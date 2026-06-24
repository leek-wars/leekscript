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

	/** Object-oriented standard library: methods on built-in values, {@code array.map(f)}. */
	OO_STDLIB(5),

	/** Modules: {@code import}/{@code export}, one file = one module, private-by-default. */
	MODULES(5),

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
	FUNCTION_OVERLOADING(5);

	public final int since;

	LanguageFeature(int since) {
		this.since = since;
	}

	public boolean enabledIn(int version) {
		return version >= since;
	}
}
