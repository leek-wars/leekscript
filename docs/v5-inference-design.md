# Design : inférence de types inter-procédurale (#43)

> Document de design. Branche `v5`, Phase 6.
> Statut : **Tranches 1-2 IMPLÉMENTÉES** (suite 17047/0, zéro régression v1-4) ; tranche 3 PROPOSÉE.

## 1. Problème

Une fonction utilisateur sans annotation de retour (`-> Type`) a le type de retour
`any`. Les appelants reçoivent donc `any` et retombent sur le **dispatch dynamique**
runtime pour toute opération sur le résultat.

```leekscript
function getFive() { return 5 }     // type de retour inféré aujourd'hui : any
var x = getFive()                   // x : any  -> `x * 2` = appel runtime mul(Object, Object)
```

Objectif : inférer le type de retour à partir du corps et le propager aux appelants,
pour générer du code spécialisé (primitifs, pas de boxing). Issue #43 (impact estimé
15-40 % sur du code structuré avec des helpers).

## 2. Machinerie actuelle (relevé)

- `FunctionType.return_type` (défaut `Type.ANY`) ; posé **uniquement** par l'annotation
  explicite `-> Type` (`FunctionBlock.setReturnType`). Aucune inférence depuis le corps.
- `MainLeekBlock.preAnalyze` puis `analyze` bouclent sur `mFunctions.values()`
  (`HashMap`, ordre d'insertion en pratique). **`preAnalyze` fige les `CallableVersion[]`
  avec `this.type.getReturnType()` = ANY**, avant l'analyse des corps.
- Appelant : `LeekFunctionCall.verifyVersions` lit `version.return_type` → `this.type`
  (type de l'expression d'appel).
- Codegen (après analyze) :
  - `FunctionBlock.writeJavaCodeForArity` : signature Java
    `<returnType.getJavaPrimitiveName()> f_<nom>(...)` (ANY → `Object`, INT → `long`…).
  - `LeekReturnInstruction.writeJavaCode` : `compileConvert(expr -> returnType)`.
  - **Retour implicite** (chute en fin de corps, `mEndInstruction == 0`) :
    `return <returnType.getDefaultValue()>` → **piège** (voir §4.2).
- Récursion / graphe d'appels : aucun. `resolvedFunction` posé pendant `analyze`.

## 3. Invariants de correction

1. **Sur-type.** Le type inféré doit être un **sur-type** de chaque valeur retournée :
   tout `return expr` est alors un upcast sûr (pas de perte de données au `compileConvert`).
   Donc type inféré = **union (`compound`)** des types de tous les `return`.
2. **Chute en fin de corps = `null`.** Si la fonction peut se terminer sans `return`
   (`mEndInstruction == 0`) ou contient un `return` nu (sans expression), elle renvoie
   `null` au runtime → le type inféré **doit inclure `NULL`** (sinon le retour implicite
   émet la valeur par défaut du type, ex. `0L` pour `int`, au lieu de `null` = bug).
3. **Récursion → ANY (gratuit).** Un appel récursif est analysé *avant* que le type de
   la fonction soit inféré → il vaut ANY → l'union retombe sur ANY. Pas de point fixe,
   pas de boucle infinie. (Vrai aussi pour la récursion mutuelle si l'appelé n'est pas
   encore inféré.)
4. **Annotation explicite gagne.** Ne pas inférer si la fonction a un `-> Type` explicite
   (nécessite un drapeau `returnTypeExplicit`, car on ne distingue pas aujourd'hui
   `-> any` explicite du défaut ANY).
5. **v5 uniquement.** Le changement modifie le codegen → gardé `LanguageFeature` ;
   **v1-4 byte-identique**.

## 4. Tranches

### Tranche 1 ✅ — inférence du type de retour (sans réordonnancement)

> **FAIT.** `LanguageFeature.RETURN_TYPE_INFERENCE(5)` ; `FunctionBlock` collecte ses
> `return` (`registerReturn`) et, après `super.analyze`, infère l'union (+ NULL si
> `mEndInstruction == 0`), pose le type, reconstruit les `CallableVersion` (`buildVersions`)
> et **réaligne le `returnType` de chaque `return`** (`setInferredReturnType`) pour que la
> signature Java et la conversion des `return` coïncident. Drapeau `returnTypeExplicit`
> (posé par `setReturnType`) bloque l'inférence si annotation explicite (`-> Type`,
> type guard). Tests `TestInference`. Zéro régression (gate v5).

**Cœur.** Après `super.analyze()` dans `FunctionBlock.analyze` (corps analysé, donc les
expressions de `return` ont un type), si v5 ET pas d'annotation explicite :

```
inféré = union des types des expressions de chaque `return`
         (return nu -> NULL)
si mEndInstruction == 0 (peut chuter)        -> union avec NULL
si aucun return du tout                       -> NULL
poser this.type.returnType = inféré
reconstruire les CallableVersion[] avec ce return_type
```

**Collecte des `return`.** Mirroir du mécanisme `@tailrec` : `LeekReturnInstruction.analyze`
enregistre son expression sur la fonction courante (`compiler.getCurrentFunction()`), ou
`FunctionBlock` les expose. (Ne PAS descendre dans les fonctions anonymes imbriquées :
`getCurrentFunction()` = l'anonyme dans son corps, donc l'exclusion est gratuite.)

**Bénéfice.** (a) Le codegen de la fonction elle-même devient précis (signature `long`,
pas de boxing du retour) ; (b) les appelants analysés *ensuite* (ordre `mFunctions`)
reçoivent le type précis via les versions reconstruites.

**Limites.** Une fonction à paramètre non typé qui renvoie une valeur dérivée du
paramètre reste ANY (`function dbl(x){ return x*2 }` : `x` est ANY → `x*2` ANY). Seuls
les retours littéraux / d'expressions typées / d'appels déjà inférés bénéficient. La
propagation aux appelants est *dépendante de l'ordre* (déterministe par JVM, mais un
appelant analysé avant l'appelé voit encore ANY — sûr, juste moins précis).

**Sûreté.** Aucune valeur de retour ne déborde le type inféré (invariant §3.1) ; le
retour implicite reste `null`-correct (§3.2) ; appel d'un `long f()` là où l'appelant
croit `Object` = autoboxing Java (correct). Donc même propagation partielle = sûre.

**Risque.** Churn des tests **v5** à comptage d'ops (`.ops(N)`) et à warnings
(`DANGEROUS_CONVERSION` quand un retour passe de ANY à un type précis). Les tests v1-4
sont intacts (gate). Plan : lancer la suite, inspecter chaque diff, corriger les
comptes attendus là où le nouveau codegen est correct.

### Tranche 2 ✅ — propagation déterministe par analyse à la demande

> **FAIT.** Approche révisée : **analyse paresseuse à la demande**, plus simple et sûre que
> le tri topologique explicite initialement prévu (pas de graphe d'appels). Tout appelant
> voit le type inféré de l'appelé, quel que soit l'ordre de déclaration.

`FunctionBlock.analyze` devient **idempotent** (drapeau `analysisStarted` ; un appel
ré-entrant ou répété retourne aussitôt). Quand `LeekFunctionCall.analyze` résout un appelé
utilisateur (gardé v5), il déclenche `f.analyze()` **à la demande** AVANT de lire son type
(`verifyVersions`) → l'appelé est inféré juste à temps. La demande fixe l'ordre effectif ;
les cycles sont cassés par le drapeau (un appelé en cours d'analyse vaut ANY → l'union
retombe sur ANY, sound, pas de boucle infinie).

**Sûreté de la ré-entrance** : la résolution des variables (y compris les valeurs par défaut
des paramètres) se fait en **preAnalyze** — passe unique sur toutes les fonctions, AVANT
toute `analyze` — donc dans le bon contexte ; l'analyse à la demande ne re-déclenche que
`analyze` (types + inférence), où `currentBlock` / `currentFunction` / `module` sont
sauvegardés-restaurés (FunctionBlock + AbstractLeekBlock.analyze). Aucune fuite de portée
(testé : la valeur par défaut d'un appelé ne voit pas les locales de l'appelant). **v1-4
byte-identique** : le déclencheur est gardé v5 et, sans lui, chaque fonction n'est analysée
qu'une fois par la boucle → le drapeau est inerte. Tests `TestInference` (propagation
`a→b→c`, récursion mutuelle, sûreté de portée).

### Tranche 3 — inférence des types de paramètres / spécialisation (CHANTIER DÉDIÉ)

Le gros gain de l'issue (débloque `getHP(entity)`), mais **fondamentalement plus lourd**
que T1/T2 : le type d'un paramètre n'est PAS déductible de la fonction seule, il dépend
des **sites d'appel**. Or l'architecture actuelle analyse chaque corps **une seule fois**
(preAnalyze fige la résolution, analyze fige les types). Deux approches réelles, aucune ne
rentre proprement comme une tranche contenue :

**Piste A — point fixe whole-program (union des types d'arguments).** Inférer le type d'un
paramètre = union des types d'arguments observés sur tous les sites d'appel, puis ré-analyser
le corps avec ces types. Sound (le paramètre reçoit bien l'un de ces types). MAIS :
- les types d'arguments exigent l'analyse des corps APPELANTS (pour typer les args), qui
  exige les types de retour des appelés (T1/T2) → dépendance circulaire = **itération à
  point fixe** sur tout le programme ;
- ré-analyser les corps **duplique les erreurs** → il faut une passe de collecte *sans
  émission d'erreur* distincte de la passe finale, ou un mécanisme de déduplication ;
- fragile : un seul appel avec un argument `any` (ou un appel dynamique / via variable)
  fait retomber le paramètre sur `any`.

**Piste B — spécialisation / monomorphisation.** Générer une version Java spécialisée par
profil de types d'arguments observé (`getHP_Entity`, `getHP_any`…) + dispatch au site
d'appel. La plus **sound** (chaque version est correcte pour son profil) et la plus
performante (multiplicatif avec les autres optimisations de types), mais **gros changement
codegen** : émission de N corps, sélection de la bonne variante par site, explosion
potentielle du nombre de variantes, interaction avec la récursion et les fermetures.

**Pourquoi pas une tranche contenue.** Contrairement à T1/T2 (greffées sur la passe analyze
existante sans rien réordonner de structurel), T3 impose soit un moteur de point fixe
multi-passes avec gestion des erreurs, soit un étage de monomorphisation au codegen. Les
deux sont des projets à part entière, avec un vrai risque d'**unsoundness** si bâclés
(violation du principe « jamais régresser »). **Recommandation : chantier dédié**, à concevoir
et implémenter séparément, pas en fin de session. T1+T2 livrent déjà la **solution 1 de
l'issue #43** (inférence inter-procédurale des types de retour) ; T3 correspond aux
solutions 2-3 (annotations encouragées / spécialisation).

## 5. Points de décision (à valider)

1. **Périmètre session** : tranche 1 seule (sûre, recommandée) vs 1+2 (propagation
   complète, plus risquée).
2. **Churn v5 accepté** : on assume de mettre à jour les comptes d'ops des tests v5
   impactés (le nouveau codegen étant correct), ou on cherche à minimiser le churn (ex.
   n'inférer que quand toutes les branches s'accordent sur un type concret unique) ?
3. **`-> any` explicite** : doit-il bloquer l'inférence (drapeau `returnTypeExplicit`) ?
   Proposé : oui (respecter l'annotation).
4. **Anonymes / méthodes** : tranche 1 = fonctions nommées d'abord ; étendre ensuite ?

## 6. Plan de test (tranche 1)

- `TestInference` (v5) : retour littéral (`function f(){return 5}` → appelant voit
  `int`, ops réduits), retour multi-branches (`int | real`), chute en fin (`int | null`),
  `return` nu, récursion (→ ANY, pas de crash), annotation explicite respectée,
  non-régression v4 (même code en v4 = ANY, byte-identique).
- Suite complète : 0 régression v1-4 ; diffs v5 inspectés un par un.
