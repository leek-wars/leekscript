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

	// `class Box<T> { ... }` shared by the instantiation-precision tests below.
	private static final String BOX = "class Box<T> { T value constructor(T v) { this.value = v } public T get() { return this.value } } ";

	@Test
	public void inferredClassTypeIsPrecise() throws Exception {
		section("Generics / classes inferred instantiation precision");
		// new Box(5) infers Box<integer> from the constructor argument, so .get() and
		// .value read back as integer (assignable to integer with no warning) — proves
		// T is no longer erased to any at the use site.
		code_strict_v5_(BOX + "integer i = new Box(5).get() return i").noWarning();
		code_strict_v5_("class Box<T> { T value constructor(T v) { this.value = v } } integer i = new Box(5).value return i").noWarning();
		// new Box('hello') infers Box<string>: .get()/.value are string, NOT assignable
		// to integer (any would be a silent unsafe downcast — this proves T = string).
		code_strict_v5_(BOX + "integer i = new Box('hello').get() return i").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict_v5_("class Box<T> { T value constructor(T v) { this.value = v } } integer i = new Box('hello').value return i").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		// Through a local variable (not only the chained form).
		code_strict_v5_(BOX + "var b = new Box(5) integer i = b.get() return i").noWarning();
		// Several type parameters: new Pair(1, 'x') infers Pair<integer, string>.
		code_strict_v5_("class Pair<A, B> { A a B b constructor(A a, B b) { this.a = a this.b = b } } string s = new Pair(1, 'x').b return s").noWarning();
		code_strict_v5_("class Pair<A, B> { A a B b constructor(A a, B b) { this.a = a this.b = b } } integer i = new Pair(1, 'x').b return i").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		// Runtime still correct (erasure unchanged): the precise type is compile-time only.
		code_v5(BOX + "var b = new Box(5) return b.get() + 1").equals("6");
		// Non-integer primitives exercise the codegen wrapper cast (Double / Boolean), not
		// just Long — both method return and field access.
		code_strict_v5_(BOX + "real r = new Box(5.5).get() return r").noWarning();
		code_v5(BOX + "return new Box(5.5).get() * 2").equals("11.0");
		code_v5("class Box<T> { T value constructor(T v) { this.value = v } } var b = new Box(5.5) return b.value + 1").equals("6.5");
		code_strict_v5_(BOX + "boolean b = new Box(true).get() return b").noWarning();
		code_strict_v5_("class Box<T> { T value constructor(T v) { this.value = v } } boolean b2 = new Box(false).value return b2").noWarning();
		code_v5(BOX + "return !new Box(true).get()").equals("false");
	}

	@Test
	public void inferredClassTypeChained() throws Exception {
		section("Generics / classes chained generic receiver (cast precedence)");
		// Chained generic receiver: Box<Box<integer>>. The erased-return cast must be
		// PARENTHESIZED — `(Long) x.get().get()` would parse as `(Long)(x.get().get())`
		// and call .get() on the erased Object. Method-then-method and method-then-field.
		code_v5(BOX + "return new Box(new Box(5)).get().get() + 1").equals("6");
		code_v5(BOX + "return new Box(new Box(5)).get().value + 1").equals("6");
		code_strict_v5_(BOX + "integer i = new Box(new Box(5)).get().get() return i").noWarning();
		// Same precedence hazard via a generic user function returning a parameterized class.
		code_v5(BOX + "function id<T>(T x) -> T { return x } return id(new Box(5)).get() + 1").equals("6");
		// And via a generic system method (array.max() -> the element, a Box).
		code_v5(BOX + "return [new Box(5)].max().get() + 1").equals("6");
	}

	@Test
	public void genericClassInheritanceAndEdges() throws Exception {
		section("Generics / classes inheritance + erasure edges");
		// Inherited member whose type is a PARENT type variable not bound by the subclass
		// instantiation (B extends A, raw): it must erase to any, NOT leak a raw T (which
		// used to raise a hard ASSIGNMENT_INCOMPATIBLE_TYPE even in non-strict v5).
		code_v5("class A<U> { U v constructor(U x) { this.v = x } public U get() { return this.v } } class B<T> extends A { constructor(T x) { super(x) } } var b = new B(5) integer i = b.get() return i").equals("5");
		// Phantom type parameter (T appears in no constructor argument): erases to any.
		code_v5("class Box<T> { integer fixed constructor() { this.fixed = 7 } public T make() { return this.fixed } } return new Box().make()").equals("7");
		// A T-typed method parameter is checked against the ERASED bound (any), so a
		// wrong-typed argument is accepted (documents the current erasure-boundary limit).
		code_v5("class Box<T> { T value constructor(T v) { this.value = v } public void set(T v) { this.value = v } public T get() { return this.value } } var b = new Box(5) b.set('x') return b.get()").equals("\"x\"");
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
