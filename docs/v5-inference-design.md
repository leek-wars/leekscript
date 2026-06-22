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

Le gros gain de l'issue (débloque `getHP(entity)` → `hp * 2` typé chez l'appelant), mais
**fondamentalement plus lourd** que T1/T2 : le type d'un paramètre n'est PAS déductible de
la fonction seule, il dépend des **sites d'appel**. Or l'architecture analyse chaque corps
**une seule fois** (preAnalyze fige la résolution, analyze fige les types).

#### Expérience menée (2026-06-22) : la piste « ré-analyse » est UNSOUND — abandonnée

On a implémenté et mesuré la **piste A** sous sa forme la plus propre : une analyse v5 en
**deux passes** — passe 1 silencieuse (collecte des sites d'appel + types d'arguments, aucune
erreur émise), puis on fige les types de paramètres spécialisés (= union des arguments observés,
fonction non échappée, validée par ré-analyse + rollback), puis passe 2 **autoritaire** qui
ré-analyse tout avec les paramètres spécialisés pour **propager les types de retour affinés aux
appelants**. Détection d'échappement propre : `FunctionBlock.variable.getUsageCount()` (incrémenté
1×/référence en preAnalyze) `==` nombre de sites d'appel directs ⇒ pas de fuite comme valeur.

**Résultat : ça MARCHE fonctionnellement.** `function dbl(x){return x*2}` appelé seulement avec
des int → en strict v5, `integer m = dbl(5)` ne warn plus (x inféré int → retour int → propagé) ;
v4 warn toujours. Rollback, échappement, récursion, union compound-conservatrice : OK. Suite
**17061/17065**, et **v1-4 byte-identique** (le gate tient).

**MAIS la ré-analyse n'est PAS idempotente** — 4 échecs v5 irréductibles, cause racine confirmée :
1. **Compteur d'opérations.** `ConditionalBloc.analyze` fait `mCondition.operations++` (ligne ~82,
   « +1 op pour le if »). Les feuilles (littéraux, variables) ne RÉINITIALISENT pas leur
   `operations` dans leur propre `analyze` → un `++` externe **s'accumule** d'une passe à l'autre.
   `if (1) {}` passe de 1 à 2 ops. Le compte d'ops est lu au codegen (`writer.addCounter`) → faux
   au runtime (l'ops/tour est une mécanique de jeu). Les boucles (while/for) ont le même motif.
2. **État de narrowing des champs.** `class A { static integer|null x = null; ... }` : après que la
   passe 1 a vu `class.x = 12`, le `lastAssignedType`/`mNarrowedPropertyTypes` du champ **persiste**
   en passe 2 → `if (class.x == null)` devient `COMPARISON_ALWAYS_FALSE` et l'affectation saute.
   Cet état vit dans ~4 fichiers (ConditionalBloc, AbstractLeekBlock, LeekVariable, LeekExpression).

**Conclusion : `analyze` n'est pas rejouable.** Rendre l'analyse idempotente (sortir les `++`
d'ops du chemin analyze + reset du narrowing entre passes) est du whack-a-mole à couverture de
test incomplète → risque de bugs v5 subtils (le compteur d'ops corrompu ne lève AUCUNE erreur).
Toute approche par ré-analyse (2 passes OU mono-passe avec ré-analyse de corps) hérite du défaut.
**Décision user (2026-06-22) : abandonner la ré-analyse, faire la monomorphisation.**

#### Piste retenue — monomorphisation par RE-PARSE (chaque variante analysée 1×)

Générer une **méthode Java spécialisée par profil de types d'arguments** observé
(`f_dbl$i(long)`, `f_dbl$s(String)`, + générique `f_dbl(Object)` pour les sites any/dynamiques) ;
chaque variante est analysée **exactement une fois** → aucune ré-analyse, aucun problème
d'idempotence ni d'ops. Le dispatch est **statique** (le site d'appel connaît le profil de ses
arguments au compile-time → il appelle directement la bonne variante, zéro coût runtime). Chaque
variante a son propre type de retour précis → la propagation aux appelants est **sound par
construction** (le site lit le retour de la variante, analysé une seule fois).

**Faisabilité du corps frais — RE-PARSE plutôt que deep-clone d'AST** (pas d'infra de clone dans
le code ; écrire `clone()` sur toute la hiérarchie expression/instruction = énorme et fragile) :
- Le flux de tokens supporte `mTokens.getPosition()` / `setPosition()` (WordCompiler:636/645).
- Une fonction est parsée par `functionBlock()` (en-tête : params + type retour), puis le `{`
  (WordCompiler:914) ouvre le corps que la **boucle principale `compileWord` parse dans
  `mCurentBlock = block`** jusqu'au `}` apparié.
- Donc : enregistrer `block.bodyStartPosition = mTokens.getPosition()` juste après le `{`, et pour
  chaque variante repositionner le flux à `bodyStartPosition`, créer un FunctionBlock frais avec
  les params **pré-typés au profil**, re-parser le corps (boucle bornée par la profondeur
  d'accolades) puis preAnalyze + analyze ce bloc frais **une seule fois** (AST propre, résolution
  des symboles correcte dans le contexte de compilation courant).

**Algorithme (worklist).**
1. Analyse normale (T1/T2) : tous les sites d'appel résolus, args typés. Collecter par fonction
   les **profils de types d'arguments** distincts des sites directs (réutiliser la collecte de
   sites + l'échappement de l'expérience 2-passes ; `usageCount == sites` ⇒ non échappée).
2. Worklist de `(fonction, profil)` où le profil a ≥1 type concret et la fonction est éligible
   (non échappée, non point d'entrée, non redéfinie, non générique, pas de type guard, pas via
   namespace, params non-référence). Pour chaque : re-parser+analyser une variante (params pinned
   au profil). Une variante peut produire de nouveaux profils internes → enfiler. Dédup par
   `(fonction, profil)`. Borne = fonctions × profils distincts (petit en pratique ; `log()` si on
   cape).
3. Codegen : émettre la générique + toutes les variantes ; chaque site d'appel émet l'appel de SA
   variante (par profil) ou la générique.

#### 1ʳᵉ TRANCHE IMPLÉMENTÉE ✅ (2026-06-22, suite 17063/0, v1-4 byte-identique)

Monomorphisation **à la demande** (style T2), pas de post-pass : pendant l'analyse d'un appelant,
`LeekFunctionCall.analyze` (branche `VariableType.FUNCTION`) calcule le profil de types des
arguments et appelle `FunctionBlock.monomorphicVariantFor(compiler, argTypes)`. Si un paramètre non
annoté est appelé avec int/real/bool, une **variante** est créée+analysée **1×** et mise en cache :

- **Re-parse** du corps (`WordCompiler.reparseFunctionBody`) dans un `FunctionBlock` frais aux
  paramètres pré-typés (`setParameterType` met à jour mTypes + FunctionType + déclaration + var
  ARGUMENT). `functionBlock()` mémorise `bodyStart = mTokens.getPosition()` juste après le `{` ;
  la boucle de re-parse est bornée (s'arrête quand le `}` referme le bloc frais), sauve/restaure
  position+bloc+fonction+classe → appelable en pleine analyse (le flux de tokens est libre en phase
  d'analyse). La variante est analysée 1× → **aucune ré-analyse, aucun problème d'idempotence**.
- **Propagation** : `this.functionType`/`this.type` du site = ceux de la variante (retour précis
  inféré par T1 sur le corps typé) → l'appelant voit le type précis (testé : strict v5
  `integer m = dbl(5)` ne warn plus). **Dispatch statique** au codegen : `f_<internalName>$<profil>`.
- **Codegen** : `FunctionBlock.writeJavaCode` émet la générique PUIS ses variantes (chacune un
  `FunctionBlock` à part avec son suffixe `$i`/`$r`/`$b` + corps typé).
- **Récursion** : variante ajoutée au cache AVANT son analyse → l'appel récursif de même profil la
  retrouve (pas de boucle). Mutuelle OK.
- **Abandon sûr** : la variante est analysée en mode SILENCIEUX (pile d'erreurs, la création peut
  cascader) ; si la spécialisation rend le corps invalide (ex. `x[0]` sur un int), erreurs ≥1 →
  variante jetée + profil mémorisé `failedProfiles` → l'appelant garde la générique, AUCUNE erreur
  visible.

**Profils supportés :** int/real/bool **et classes utilisateur** (le `getHP(entity)` de l'issue :
l'accès méthode/champ se résout statiquement, dispatch virtuel préservé pour les sous-classes —
le paramètre est typé par le type STATIQUE de l'argument). Profil par paramètre = `specializableProfileType`
(int/real/bool, ou `ClassValueType` non interne). Clé de cache `|`-délimitée (`C<internalName>` pour
une classe) ; suffixe Java lisible `$i`/`$ib` pour un profil tout-primitif, `$v<n>` séquentiel dès
qu'une classe est en jeu (pas de jeton court injectif pour un nom de classe). Les classes internes
(Array/Map/Set → ArrayType/… pas ClassValueType) et génériques (`Box<int>` → ParameterizedClassType)
ne sont pas spécialisées (conservateur).

**Limites de cette tranche (extensions futures) :** Array/Map/Set/string pas encore (leurs valeurs
ont un type dédié, pas ClassValueType) ; arité fixe (pas de paramètres par défaut) ; **fonctions du
module PRINCIPAL uniquement** (les modules importés sont parsés par un WordCompiler enfant → leur
`bodyStart` n'est pas dans le flux principal ; à re-parser via leur compilateur enfant) ; pas
générique / pas type guard / `@référence` jamais spécialisé. Tests
`TestInference.testParameterMonomorphization(_class/_safety)`.

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
