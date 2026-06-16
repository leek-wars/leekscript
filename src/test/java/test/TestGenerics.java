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
	public void onlyAvailableInV5() throws Exception {
		section("Generics / version gate");
		// `function name<T>` is a version-5 feature; in v4 the `<` after the name is
		// unexpected (a parenthesis is required).
		code_v4("function id<T>(T x) { return x } return id(5)").error(Error.OPENING_PARENTHESIS_EXPECTED);
	}
}
