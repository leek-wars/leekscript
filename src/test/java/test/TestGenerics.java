package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

/**
 * Generic user functions (`function name<T>(...)`), introduced in language version 5.
 */
@ExtendWith(SummaryExtension.class)
public class TestGenerics extends TestCommon {

	@Test
	public void identity() throws Exception {
		section("Generics / identity");
		// A type parameter binds to whatever is passed.
		code_v5("function id<T>(T x) { return x } return id(5)").equals("5");
		code_v5("function id<T>(T x) { return x } return id('hello')").equals("\"hello\"");
		code_v5("function id<T>(T x) -> T { return x } return id(5)").equals("5");
		code_v5("function id<T>(T x) -> T { return x } return id([1, 2, 3])").equals("[1, 2, 3]");
		// Multiple type parameters.
		code_v5("function second<A, B>(A a, B b) -> B { return b } return second(1, 'x')").equals("\"x\"");
	}

	@Test
	public void inferenceFromContainers() throws Exception {
		section("Generics / inference from containers");
		// T inferred from the array element type.
		code_v5("function first<T>(Array<T> a) { return a[0] } return first([10, 20, 30])").equals("10");
		code_v5("function first<T>(Array<T> a) -> T { return a[0] } return first(['a', 'b'])").equals("\"a\"");
		// The inferred return type is concrete enough to be used arithmetically.
		code_v5("function first<T>(Array<T> a) -> T { return a[0] } return first([7, 8]) + 1").equals("8");
	}

	@Test
	public void inferredReturnTypeIsPrecise() throws Exception {
		section("Generics / inferred return type precision");
		// id(5) -> integer : assignable to integer with no warning (proves T was
		// resolved to integer, not left as any).
		code_strict_v5_("function id<T>(T x) -> T { return x } integer i = id(5) return i").noWarning();
		code_strict_v5_("function first<T>(Array<T> a) -> T { return a[0] } integer i = first([1, 2]) return i").noWarning();
		// id('hello') -> string : NOT assignable to integer (proves T resolved to
		// string, not any — any would be a silent unsafe downcast).
		code_strict_v5_("function id<T>(T x) -> T { return x } integer i = id('hello') return i").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
	}

	@Test
	public void genericClasses() throws Exception {
		section("Generics / classes");
		// A generic class: the type parameter is usable as a field type and a
		// constructor parameter type. Works via erasure (runtime stays dynamic).
		code_v5("class Box<T> { T value constructor(T v) { this.value = v } } var b = new Box(5) return b.value").equals("5");
		code_v5("class Box<T> { T value constructor(T v) { this.value = v } } var b = new Box('hello') return b.value").equals("\"hello\"");
		// Type parameter as a method return type.
		code_v5("class Box<T> { T value constructor(T v) { this.value = v } public T get() { return this.value } } return new Box(42).get()").equals("42");
		// Several type parameters.
		code_v5("class Pair<A, B> { A a B b constructor(A a, B b) { this.a = a this.b = b } } var p = new Pair(1, 'x') return p.b").equals("\"x\"");
	}

	@Test
	public void genericClassTypeChecksInStrict() throws Exception {
		section("Generics / classes strict");
		// Assigning the constructor param (type T) to a field (type T) and returning
		// a T-typed field from a T-returning method must not warn in strict mode.
		code_strict_v5_("class Box<T> { T value constructor(T v) { this.value = v } public T get() { return this.value } } return new Box(1).get()").noWarning();
	}

	@Test
	public void genericSystemFunctions() throws Exception {
		section("Generics / system functions");
		// arrayUnique(Array<T>) -> Array<T> : the element type is preserved.
		code_v5("var a = [1, 1, 2, 3, 3].unique() return a").equals("[1, 2, 3]");
		// arrayMax(Array<T>) -> T : the inferred return type is the element type, so
		// it can be assigned to a concrete typed variable with no warning (proves the
		// erased Object return is cast back to the inferred type at the call site).
		code_strict_v5_("integer m = [3, 1, 4, 1, 5].max() return m").noWarning();
		code_v5("return [3, 1, 4, 1, 5].max() + 1").equals("6");
		code_strict_v5_("string s = ['a', 'c', 'b'].max() return s").noWarning();
		// v4 must keep the old loose typing (no generic inference): arrayMax returns
		// any, so assigning to integer is an unsafe downcast (warning), not silent.
		code_strict_v4("integer m = arrayMax([3, 1, 4]) return m").warning(Error.DANGEROUS_CONVERSION_VARIABLE);
	}

	@Test
	public void genericHigherOrderSystemFunctions() throws Exception {
		section("Generics / higher-order system functions");
		// Runtime behaviour is unchanged.
		code_v5("return [1, 2, 3].map(function(v) { return v * 2 })").equals("[2, 4, 6]");
		code_v5("return [1, 2, 3, 4].filter(function(v) { return v > 2 })").equals("[3, 4]");
		// arrayFilter(Array<T>, predicate) -> Array<T> : the element type comes ONLY
		// from the array, not from the (untyped) callback parameter. This proves the
		// contravariant inference fix — T stays integer, not integer|any, so the
		// result is assignable to Array<integer> with no warning.
		// arrayFilter(Array<T>, predicate) -> Array<T> : the element type comes from the
		// array, so the result is assignable to Array<integer> with no warning (typed
		// predicate so its bool return matches cleanly).
		code_strict_v5_("function keep(integer n) -> boolean { return n > 2 } Array<integer> a = [1, 2, 3, 4].filter(keep) return a").noWarning();
		// arrayMap(Array<T>, (T) => R) -> Array<R> : R is taken from a typed callback.
		code_strict_v5_("function dbl(integer n) -> integer { return n * 2 } Array<integer> a = [1, 2, 3].map(dbl) return a").noWarning();
	}

	@Test
	public void onlyAvailableInV5() throws Exception {
		section("Generics / version gate");
		// `function name<T>` is a version-5 feature; in v4 the `<` after the name is
		// unexpected (a parenthesis is required).
		code_v4("function id<T>(T x) { return x } return id(5)").error(Error.OPENING_PARENTHESIS_EXPECTED);
	}
}
