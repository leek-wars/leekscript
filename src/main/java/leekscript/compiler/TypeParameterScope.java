package leekscript.compiler;

import leekscript.common.TypeVariable;

/**
 * A declaration that introduces generic type parameters into scope: a generic
 * function ({@code function f<T>(...)}) or a generic class ({@code class Box<T>}).
 * The parser walks the enclosing scopes to resolve a type-parameter name to its
 * {@link TypeVariable}. Introduced with language-version-5 generics.
 */
public interface TypeParameterScope {

	/** The type variable declared under {@code name} in this scope, or null. */
	TypeVariable getTypeParameter(String name);
}
