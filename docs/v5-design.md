# LeekScript v5 — Design & Roadmap

> Branche `v5`. Document vivant. Objectif : faire évoluer le langage (génériques,
> bibliothèque standard objet, modules) **sans casser** la sémantique des versions
> 1 à 4. Toute nouvelle sémantique est gardée derrière `version >= 5`.

## 0. Principe de compatibilité

Le comportement v1–v4 est **gelé**. Les ~100 points de bascule existants
(`getVersion() >= 2/3/4`) forment le contrat de compatibilité ; v5 est *additif*
à ces endroits, sauf breaking change explicitement décidé et documenté ici.

Aujourd'hui les versions sont comparées par des littéraux entiers dispersés
(`>= 2`, `>= 4`, `== 1`...). Pour v5 on introduit un point unique :

```
leekscript/common/LanguageFeature.java   (nouveau)
  enum LanguageFeature { CLASSES(2), MAPS(4), GENERICS(5), MODULES(5),
                         OO_STDLIB(5), ... ; final int since; boolean enabledIn(int v) }
```

On **n'a pas besoin** de réécrire les 100 gates d'un coup : on route les
nouveaux gates v5 par `LanguageFeature`, et on migre l'existant opportunément.
Cela évite de semer des `>= 5` magiques dans tout le compilateur.

### Stratégie de dépréciation
Pour les formes supprimées en v5 (fonctions libres de la stdlib, voir §2), on
passe par un palier : **warning en v5** (`Error.DEPRECATED_IN_V5`) plutôt que
suppression sèche, tant que la migration des IA joueurs n'est pas outillée.
Suppression dure dans une v5.x ultérieure une fois la télémétrie au vert.

---

## 1. Génériques (pilier 1)

### État actuel
- `common/TemplateType.java` est un stub de 9 lignes, inutilisé.
- `Array<T>`, `Map<K,V>`, `Set<T>`, `Function` sont **monomorphes** :
  `ArrayType.type` est un `Type` concret, pas une variable.
- La résolution de surcharge système (`CallableVersion[]` + `VersionsType`,
  `LeekFunctionCall.verifyVersions` ~ligne 770) repose sur une métrique de
  distance de cast, pas sur l'unification.

### Cible v5
1. **`TypeVariable extends Type`** (remplace `TemplateType`) : `name`, `bounds`
   (borne sup, défaut `any`), `variance` (INVARIANT par défaut), identité par
   référence (`equals`/`hashCode` sur l'instance, pas le nom).
2. **`Type.substitute(Map<TypeVariable, Type>)`** sur toute la hiérarchie
   (`ArrayType`, `MapType`, `SetType`, `FunctionType`, `CompoundType`,
   `ClassType`...). `TypeVariable.substitute` renvoie le binding ou soi-même.
3. **Fonctions utilisateur génériques** ✅ (implémenté) :
   `function first<T>(Array<T> list) -> T { return list[0] }`
   - Les paramètres de type vont **après le nom** (`name<T, U>`), pas après
     `function` : `function<` est déjà réservé au type `Function<...>` côté
     expression (conflit de grammaire). Cf. `WordCompiler.eatTypeParameters`.
   - `T` résolu en `TypeVariable` pendant le parse des types (portée = chaîne des
     `FunctionBlock` englobants, `WordCompiler.findTypeParameter`).
   - `FunctionBlock` stocke `TypeVariable[]` ; les `CallableVersion` générés
     portent les variables.
   - À l'appel (`LeekFunctionCall.verifyVersions`) : inférence structurelle
     (`inferTypeVariables`) param formel ↔ arg effectif → `substitute` dans le
     type de retour. Réutilise la résolution par distance existante ; no-op pour
     les versions non génériques (zéro impact sur l'existant).
   - Codegen : retour effacé en `Object`, re-casté vers le type inféré au site
     d'appel.
4. **Classes utilisateur génériques** : `class Box<T> { T value; ... }`,
   instanciées en `ParameterizedClassType` (ClassType + bindings).
5. **Signatures système génériques** : remplacer les `Type[]` figés de
   `CallableVersion` par des signatures pouvant référencer des `TypeVariable`.
   Ex : `Array.map<T,R>(self: T[], f: T => R) -> R[]`. Bénéfice immédiat :
   `map`/`filter`/`reduce` enfin typés précisément au lieu de `any`.

### Codegen : effacement (erasure)
Les génériques sont **purement compile-time**. On compile vers Java en
effaçant les types (le runtime LeekScript reste dynamique). **Aucune
modification du runtime** (`runner/values/*`) n'est nécessaire pour les
génériques : énorme réduction de risque. Pas de monomorphisation.

### Variance
Démarrer **invariant** partout (sûr, = comportement actuel d'`ArrayType.accepts`).
Ajouter la covariance en position lecture seule plus tard (itération).

### Découpage
- 1a. `TypeVariable` + `substitute` + affichage (`toString`/`getCode`).
- 1b. Fonctions utilisateur génériques (parse + inférence + fallback distance).
- 1c. Classes génériques.
- 1d. Signatures système génériques (dépend de §2 pour `map`/`filter`).

---

## 2. Bibliothèque standard objet (pilier 2)

### État actuel
- Table plate `LeekFunctions` : chaque fonction porte un `standardClass`
  (`"Number"`, `"String"`, `"Array"`, `"System"`, `"JSON"`, `"Color"`...) et un
  flag `isStatic`.
- Deux formes de codegen existent déjà (`LeekFunctionCall` ~287-298) :
  - statique : `NumberClass.sqrt(ai, x)`
  - instance : `((String) s).length(ai)` → `StringClass.length(ai, s)`
- Mais **aucune valeur de namespace n'est exposée** : pas de `Math`, pas de
  `Array.from(...)`. Tout est fonction globale.

### Cible v5
Exposer de vraies **valeurs de namespace/classe built-in** comme globales en v5 :
`Math`, `Number`, `String`, `Array`, `Map`, `Set`, `JSON`, `Color`, `System`.

- **Méthodes statiques** : `Math.sqrt(x)`, `Array.from(set)`, `JSON.encode(v)`.
  Le compilo résout `Math.sqrt` comme accès de champ sur le built-in `Math`,
  puis réutilise le codegen statique existant (`<StdClass>Class.<name>`).
- **Méthodes d'instance** : `array.push(x)`, `string.length()` — déjà supportées
  via `isStatic=false`, on conserve.
- Le `standardClass` déjà présent sur chaque `LeekFunctions` sert de source :
  on génère pour chaque classe standard une valeur built-in regroupant ses
  méthodes statiques + instance, enregistrée comme global (gardée v≥5).

### Politique v5 « plus de fonctions qui traînent »
- Les fonctions **purement bibliothèque** appelées en forme libre
  (`sqrt(x)`, `jsonEncode(v)`, `abs(x)`...) deviennent **dépréciées en v5**
  (warning) → à terme erreur. Forme canonique : `Math.sqrt(x)`, `JSON.encode(v)`.
- Les **méthodes d'instance** sur les valeurs (`string.length()`,
  `array.push()`) restent la forme idiomatique.
- Les fonctions **métier** du générateur (`getCell`, etc., injectées via
  `extraFunctions`) sont **hors périmètre** du cœur leekscript ; leur
  regroupement objet sera traité côté générateur séparément.

### Découpage
- 2a. Infrastructure : valeur built-in de namespace + résolution `Math.x`.
- 2b. Enregistrement des namespaces depuis `standardClass` (gardé v≥5).
- 2c. Warnings de dépréciation sur les formes libres en v5.

---

## 3. Système de modules (pilier 3)

### État actuel
- `include("path")` **fusionne à plat** tous les symboles dans un unique
  `MainLeekBlock`, sans namespace ; version héritée du fichier incluant.
- `resolver/` + `IncludeGraph` tracent déjà le DAG d'inclusion (forward/reverse,
  mtimes) — bonne base.

### Cible v5
Vrais modules. Syntaxe proposée :

```
// utils.lk
export function clamp(x, lo, hi) { ... }
export class Vec { ... }
function private_helper() { ... }   // non exporté = privé au module

// main.lk
import { clamp, Vec } from "utils"
import * as Utils from "geometry"
```

- **Fichier = module.** Les symboles non `export` sont privés (plus de fuite
  globale comme avec `include`).
- Implémentation : introduire un `ModuleScope` entre le scope global et le
  fichier ; tables de symboles **par module** au lieu de la fusion unique.
  Codegen : toujours une seule classe Java, mais noms qualifiés par module
  (`module$utils$clamp`) pour éviter les collisions.
- Réutiliser `IncludeGraph` (déjà un DAG) ; cycles gérés via le double-pass
  existant.
- **Compat** : `include()` reste pour v≤4 et comme échappatoire ; v5 privilégie
  `import`/`export`.

### Découpage
- 3a. Parse `export` / `import ... from` (gardé v≥5).
- 3b. `ModuleScope` + résolution qualifiée + codegen renommé.
- 3c. `import * as Ns` (namespace de module).

---

## 4. Roadmap

| Phase | Contenu | Dépend de |
|-------|---------|-----------|
| **0** | Branche v5, bump `LATEST_VERSION=5`, ce doc, `LanguageFeature` | — |
| **1** | Génériques cœur : `TypeVariable`, `substitute`, fonctions génériques | 0 |
| **2** | Classes génériques | 1 |
| **3** | Stdlib objet : namespaces `Math`/`Array`/... + dépréc. formes libres | 0 |
| **4** | Signatures système génériques (`map`/`filter`/`reduce` typés) | 1, 3 |
| **5** | Modules : `import`/`export` | 0 |
| **6** | Gains v5 issus des issues : #42 type guards, #44 @tailrec, #43 perf inférence | 1 |

Chaque phase = compilable + testée + non régressive sur la suite v1–v4 existante.

## 5. Invariants de non-régression
- La suite de tests `src/test/java/test/` doit rester verte à chaque commit.
- Toute nouvelle règle stricte v5 est inopérante en v1–v4.
- Préférer la dégradation gracieuse (warning) à l'erreur dure sur le code qui
  compilait avant (cf. politique de dépréciation §0).
