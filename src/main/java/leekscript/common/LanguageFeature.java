package leekscript.common;

/**
 * Named language features and the version they were introduced in. Centralizes
 * version gates so the compiler stops scattering magic {@code version >= N}
 * literals. New v5 gates route through here; the ~100 pre-existing v2/v3/v4
 * gates are migrated opportunistically (see docs/v5.md §0).
 */
public enum LanguageFeature {

	/** Generic user functions and type variables: {@code function name<T>(...)}. */
	GENERICS(5),

	/** {@code @tailrec} vérifié : un appel récursif hors position terminale est une erreur (v1-4 : annotation sans effet). */
	TAILREC_CHECK(5),

	/** Object-oriented standard library: methods on built-in values, {@code array.map(f)}. */
	OO_STDLIB(5),

	/** Modules: {@code import}/{@code export}, one file = one module, private-by-default. */
	MODULES(5),

	/** Framework de test : annotations {@code @test}, {@code @skip}… dans un module {@code *.test.leek}, arguments d'annotation. */
	TEST_FRAMEWORK(5),

	/** Type guard functions: {@code function f(any x) -> x is Type} narrows the argument. */
	TYPE_GUARDS(5),

	/** Inter-procedural inference of non-annotated function return types (#43). */
	RETURN_TYPE_INFERENCE(5),

	/** Monomorphisation : inférence des types de PARAMÈTRES non annotés via spécialisation
	 *  par profil d'arguments (variantes re-parsées, dispatch statique) (#43, tranche 3). */
	PARAMETER_TYPE_INFERENCE(5),

	/** Une override ne peut pas réduire la visibilité (LSP) : erreur dure en v5, warning en v1-4 (#3781). */
	STRICT_OVERRIDE_VISIBILITY(5),

	/** Une variable locale peut masquer une fonction/globale du même nom dans son scope (#4237). */
	VARIABLE_SHADOWING(5),

	/** Boucle courte `for <collection> { ... }` avec variable de boucle implicite `it` (#358). */
	FOR_IMPLICIT_IT(5),

	/** Expression `match (e) { v -> r, _ -> r }` (pattern matching par valeur) (#2404). */
	MATCH(5),

	/** Surcharge de fonctions utilisateur par signature : dispatch statique au site d'appel (#4243). */
	FUNCTION_OVERLOADING(5),

	/** Bloc nu {@code { ... }} comme instruction, avec sa propre portée. */
	SCOPE_BLOCK(5),

	/** L'affinage du type d'un champ ne survit pas à un appel qui a pu le réassigner. */
	SAFE_FIELD_NARROWING(5),

	/** Un constructeur qui appelle {@code super(...)} n'appelle pas en plus le constructeur sans argument du parent. */
	EXPLICIT_SUPER_CONSTRUCTOR(5),

	/** {@code &}, {@code ^} et {@code |} passent avant les comparaisons, comme en Rust : {@code a & b == c} vaut {@code (a & b) == c}. */
	BITWISE_PRECEDENCE(5),

	/** En mode strict, une affectation composée ({@code m[k] += x}, {@code m[k]++}…) sur une clé absente d'une map est une erreur. */
	STRICT_MISSING_MAP_KEY(5),

	/** Un {@code !} posé directement en condition (if, while, for, ternaire) ne coûte pas d'opération. */
	FREE_NEGATION_IN_CONDITION(5),

	/** {@code x += y} étend un conteneur en place quel que soit l'emplacement de {@code x} (globale, champ, élément, locale capturée), comme une locale (#5655). */
	IN_PLACE_ADD_EQ(5),

	/** Dans une méthode, un membre de la classe passe devant une globale ou une fonction du programme, comme devant une fonction de Leek Wars. */
	MEMBERS_BEFORE_OUTER_NAMES(5),

	/** Lire ou écrire un champ statique hors de sa visibilité est une erreur à l'analyse, et l'écriture est refusée à l'exécution (#2904). */
	STATIC_FIELD_VISIBILITY(5),

	/** Un {@code return} en fin de ligne ne prend pas la ligne suivante pour valeur (#4778). */
	RETURN_ENDS_AT_NEWLINE(5),

	/** Convertir null vers integer ou real lève IMPOSSIBLE_CAST au lieu de rendre 0 / 0.0 (#5389). */
	NULL_TO_NUMBER_ERROR(5),

	/** Un champ de classe typé sans initialiseur reçoit la valeur par défaut de son type, comme une locale (#4635). */
	FIELD_TYPE_DEFAULT(5),

	/** Insérer null dans un conteneur typé dont l'élément (ou la clé) exclut null lève IMPOSSIBLE_CAST (#5349). */
	NON_NULL_ELEMENTS(5),

	/** Une valeur any convertie en Array / Map / Set typés voit son contenu vérifié à la conversion (#5384). */
	TYPED_CONTENT_CHECK(5),

	/** Null passé à un paramètre integer ou real d'une fonction système ne vaut plus 0 : l'appel rend sa valeur par défaut. */
	NULL_ARGUMENT_NOT_ZERO(5),

	/** En mode strict, un conteneur aux éléments incompatibles ({@code Set<string> s = <1>}) est une erreur, comme {@code string s = 1}. */
	STRICT_CONTAINER_ELEMENTS(5);

	public final int since;

	LanguageFeature(int since) {
		this.since = since;
	}

	public boolean enabledIn(int version) {
		return version >= since;
	}
}
