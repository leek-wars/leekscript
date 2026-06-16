package leekscript.common;

/**
 * Named language features and the version they were introduced in. Centralizes
 * version gates so the compiler stops scattering magic {@code version >= N}
 * literals. New v5 gates route through here; the ~100 pre-existing v2/v3/v4
 * gates are migrated opportunistically (see docs/v5-design.md §0).
 */
public enum LanguageFeature {

	/** Generic user functions and type variables: {@code function name<T>(...)}. */
	GENERICS(5);

	public final int since;

	LanguageFeature(int since) {
		this.since = since;
	}

	public boolean enabledIn(int version) {
		return version >= since;
	}
}
