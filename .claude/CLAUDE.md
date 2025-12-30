# LeekScript - Guide pour Claude

## Vue d'ensemble

**LeekScript** est le langage de programmation custom créé pour Leek Wars. Les joueurs l'utilisent pour coder les IA de leurs poireaux qui combattent ensuite dans des arènes.

### Liens
- Repo public: https://github.com/leek-wars/leekscript
- Documentation utilisateur: https://leekwars.com (section Encyclopedia)
- Repo parent (generator): https://github.com/leek-wars/leek-wars-generator

## Architecture technique

### Compilation LeekScript -> Java

LeekScript n'est **pas interprété** mais **compilé vers Java** :

1. **Parsing** : Code LeekScript -> AST (Abstract Syntax Tree)
2. **Compilation** : AST -> Code Java généré (classe `AI_XXXXX`)
3. **Compilation Java** : Code Java -> Bytecode JVM (en mémoire)
4. **Exécution** : Bytecode exécuté dans la JVM sandboxée

### Structure du projet

```
leekscript/
├── src/main/java/leekscript/
│   ├── compiler/           # Parsing et compilation LS -> Java
│   │   ├── LexicalParser.java      # Tokenizer
│   │   ├── LeekScript.java         # Point d'entrée compilation
│   │   ├── JavaWriter.java         # Générateur de code Java
│   │   ├── JavaCompiler.java       # Compilation Java en mémoire
│   │   ├── bloc/                   # Blocs (if, while, for, etc.)
│   │   ├── expression/             # Expressions (opérateurs, etc.)
│   │   └── instruction/            # Instructions
│   ├── runner/             # Exécution runtime
│   │   ├── AI.java                 # Classe de base des IAs générées
│   │   ├── LeekRunException.java   # Exceptions runtime LS
│   │   ├── LeekOperations.java     # Opérations (clone, equals, etc.)
│   │   ├── values/                 # Types de valeurs
│   │   │   ├── ArrayLeekValue.java
│   │   │   ├── MapLeekValue.java
│   │   │   ├── ObjectLeekValue.java
│   │   │   ├── FunctionLeekValue.java
│   │   │   └── ...
│   │   └── classes/                # Classes standard (String, Number, etc.)
│   └── common/             # Types et erreurs communes
│       ├── Type.java
│       ├── Error.java
│       └── ...
├── src/test/java/test/     # Tests unitaires
├── ai/                     # IAs de test générées
└── leekscript.jar          # JAR compilé
```

## Sandbox et sécurité

LeekScript est **complètement sandboxé** par la JVM :

### Pas d'accès dangereux
- Pas d'accès au système de fichiers
- Pas d'accès réseau
- Pas de réflexion Java
- Pas de threads
- Isolation complète du code utilisateur

### Limites d'opérations
- **Opérations** : Chaque instruction LS consomme des "opérations"
- Limite par tour de combat (déterministe, pas basé sur le temps)
- Garantit l'équité entre joueurs
- Méthode `ai.ops(n)` pour comptabiliser

### Limites mémoire (RAM)
- Arrays, Maps, Objects comptabilisés
- `ai.increaseRAM(n)` / `ai.decreaseRAM(n)`
- Empêche les attaques par consommation mémoire
- Erreur `OUT_OF_MEMORY` si dépassement

## Features du langage (v4)

### Types de base
- `null` - Valeur nulle
- `boolean` - `true` / `false`
- `integer` - Entiers 64 bits (Long Java)
- `real` - Flottants 64 bits (Double Java)
- `string` - Chaînes de caractères

### Structures de données
- `Array<T>` - Tableaux dynamiques
- `Map<K, V>` - Dictionnaires
- `Set<T>` - Ensembles
- `Interval` - Intervalles (`1..10`, `1.5..10.5`)

### POO
```leekscript
class Item {
    public value = 0
    constructor(v) {
        this.value = v
    }
    method getValue() {
        return this.value
    }
}
var item = new Item(42)
```

### Fonctions
```leekscript
function add(a, b) {
    return a + b
}
// Fonctions anonymes
var f = (x) -> x * 2
// Paramètres par défaut
function greet(name = "World") {
    return "Hello " + name
}
```

### Typage optionnel
```leekscript
var x: integer = 42
function add(a: integer, b: integer): integer {
    return a + b
}
```

## Build et test

### Build le JAR
```bash
cd /home/pierre/dev/leek-wars/generator/leekscript
gradle clean jar
# Produit: leekscript.jar
```

### Exécuter un fichier LeekScript
```bash
java -jar leekscript.jar fichier.leek
```

### Lancer les tests
```bash
gradle test
```

### Tests existants
Les tests sont dans `src/test/java/test/` :
- `TestArray.java` - Tests sur les tableaux
- `TestMap.java` - Tests sur les maps
- `TestClass.java` - Tests POO
- `TestFunction.java` - Tests fonctions
- `TestLoops.java` - Tests boucles
- `TestOperators.java` - Tests opérateurs
- `TestString.java` - Tests chaînes
- etc.

## Gestion des erreurs

### Classe Error (leekscript/common/Error.java)
Enumération de tous les types d'erreurs LeekScript :
- `UNKNOWN_ERROR` - Erreur inconnue
- `TOO_MUCH_OPERATIONS` - Trop d'opérations
- `OUT_OF_MEMORY` - Dépassement RAM
- `IMPOSSIBLE_CAST` - Cast impossible
- `UNKNOWN_FIELD` - Champ inconnu
- `STACKOVERFLOW` - Stack overflow
- etc.

### LeekRunException
Exception runtime pour les erreurs utilisateur. Contient :
- `Error type` - Type d'erreur
- `Object[] parameters` - Paramètres pour le message

### Conversion Throwable -> Error
La méthode `AI.throwableToError(Throwable)` convertit les exceptions Java en erreurs LeekScript :
- `NullPointerException` -> `IMPOSSIBLE_CAST` avec message "null vers X"
- `ClassCastException` -> `IMPOSSIBLE_CAST`
- `RuntimeException` -> Regarde la cause récursivement
- etc.

## Points d'attention

### Callbacks dans Collections.sort()
Quand on utilise `Collections.sort()` avec un Comparator custom (ex: `arraySort` avec fonction utilisateur), les exceptions doivent être wrappées dans `RuntimeException` car le Comparator ne peut pas déclarer `throws`. Le système de gestion d'erreurs (`throwableToError`) déroule la cause des `RuntimeException` pour trouver l'erreur originale.

### Code généré
Les IAs compilées héritent de `AI.java` et s'appellent `AI_XXXXX` où XXXXX est l'ID de l'IA. Exemple dans `ai/AI_6750.java`.

### Versions
- Version actuelle: 4.x
- Migration vers 5.x prévue

## Conventions de code

### Java
- Packages: `leekscript.compiler`, `leekscript.runner`, etc.
- Classes: PascalCase
- Méthodes/variables: camelCase
- Constantes: UPPER_SNAKE_CASE

### Préfixes dans le code généré
- `u_` : Variables/méthodes utilisateur (ex: `u_myVar`, `u_MyClass`)
- `f_` : Fonctions utilisateur
- Évite les collisions avec le code Java

## Relation avec les autres projets

### Generator
LeekScript est un sous-module du projet Generator. Le Generator l'utilise pour :
- Compiler les IAs des joueurs
- Exécuter les combats

### Worker (server)
Le Worker utilise le Generator (et donc LeekScript) pour :
- Générer les combats en arrière-plan
- Exécuter le code des joueurs de manière sandboxée

### Client
Le client affiche les erreurs LeekScript aux joueurs avec des messages localisés basés sur les codes d'erreur.
