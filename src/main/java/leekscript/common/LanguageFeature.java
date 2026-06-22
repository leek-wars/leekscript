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
	PARAMETER_TYPE_INFERENCE(5);

	public final int since;

	LanguageFeature(int since) {
		this.since = since;
	}

	public boolean enabledIn(int version) {
		return version >= since;
	}
}
