package leekscript.common;

import java.util.Map;

/**
 * A type variable (generic parameter), e.g. the {@code T} in
 * {@code function<T> first(T[] list) -> T}. Each declared type variable is a
 * distinct instance with identity semantics: two {@code T}s declared in two
 * different functions never collide.
 *
 * Type variables only exist at compile time. They are resolved to concrete
 * types by {@link Type#substitute(Map)} during generic call resolution, and the
 * runtime stays fully dynamic (erasure). Introduced in language version 5.
 */
public class TypeVariable extends Type {

	private final String varName;
	/** Upper bound; defaults to {@link Type#ANY} (unbounded). */
	private final Type superType;

	public TypeVariable(String name) {
		this(name, Type.ANY);
	}

	public TypeVariable(String name, Type superType) {
		super(name, "t", "Object", "Object", "null");
		this.varName = name;
		this.superType = superType;
	}

	public String getName() {
		return varName;
	}

	@Override
	public CastType accepts(Type type) {
		if (type == this) return CastType.EQUALS;
		// Two distinct type variables are never compatible (only a variable with
		// itself is). An unbound variable that escapes inference behaves like its
		// upper bound for assignability checks (ANY by default = permissive).
		if (type instanceof TypeVariable) return CastType.INCOMPATIBLE;
		return superType.accepts(type);
	}

	@Override
	public Type substitute(Map<TypeVariable, Type> bindings) {
		var bound = bindings.get(this);
		return bound != null ? bound : this;
	}

	@Override
	public boolean containsTypeVariable() {
		return true;
	}

	@Override
	public String getCode() {
		return varName;
	}

	// Identity semantics: each declared type variable is unique. We deliberately
	// override the signature-based hashCode/equals of the base Type.
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object object) {
		return this == object;
	}
}
