package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestAnnotations extends TestCommon {

	@Test
	public void testUnused() throws Exception {
		section("@unused");

		// @unused suppresses UNUSED_VARIABLE warning in strict mode
		code_strict_v4_("@unused var x = 5; return 0").noWarning();

		// Variable still accessible when annotated
		code_strict_v4_("@unused var x = 5; return x").noWarning();
		code_strict_v4_("@unused var x = 5; return x").equals("5");

		// Without @unused, strict mode warns
		code_strict_v4_("var x = 5; return 0").warning(Error.UNUSED_VARIABLE);

		// @unused on function suppresses UNUSED_FUNCTION warning
		code_strict_v4_("@unused function foo() { return 1; } return 0").noWarning();
		code_strict_v4_("function foo() { return 1; } return 0").warning(Error.UNUSED_FUNCTION);

		// @unused on multi-var declaration applies to all declared variables
		code_strict_v4_("@unused var x, y = 0; return 0").noWarning();

		// _ prefix still works independently of @unused
		code_strict_v4_("var _x = 5; return 0").noWarning();
	}

	@Test
	public void testDeprecated() throws Exception {
		section("@deprecated");

		// Declaration alone (never used) produces no warning
		code_v4_("@deprecated var x = 5; return 0").noWarning();

		// Using a @deprecated variable warns at the usage site
		code_v4_("@deprecated var x = 5; return x").warning(Error.ANNOTATION_DEPRECATED_CALL);

		// Using a @deprecated function warns at the call site
		code_v4_("@deprecated function foo() { return 1; } return foo()").warning(Error.ANNOTATION_DEPRECATED_CALL);

		// @deprecated function still executes correctly
		code_v4_("@deprecated function foo() { return 42; } return foo()").equals("42");

		// @deprecated does NOT suppress unused warning in strict mode
		code_strict_v4_("@deprecated var x = 5; return 0").warning(Error.UNUSED_VARIABLE);
	}

	@Test
	public void testPure() throws Exception {
		section("@pure");

		// Result discarded → warning
		code_v4_("@pure function add(a, b) { return a + b; } add(1, 2); return 0").warning(Error.ANNOTATION_NODISCARD);

		// Result used → no warning
		code_v4_("@pure function add(a, b) { return a + b; } return add(1, 2)").noWarning();
		code_v4_("@pure function add(a, b) { return a + b; } return add(1, 2)").equals("3");

		// @pure is visible by @unused so both can be combined
		code_strict_v4_("@pure @unused function helper() { return 0; } return 0").noWarning();
	}

	@Test
	public void testNodeiscard() throws Exception {
		section("@nodiscard");

		// Result discarded → warning
		code_v4_("@nodiscard function compute() { return 42; } compute(); return 0").warning(Error.ANNOTATION_NODISCARD);

		// Result used → no warning
		code_v4_("@nodiscard function compute() { return 42; } return compute()").noWarning();
		code_v4_("@nodiscard function compute() { return 42; } return compute()").equals("42");
	}

	@Test
	public void testTodo() throws Exception {
		section("@todo");

		// @todo always warns regardless of strict mode
		code_v4_("@todo function stub() { return 0; } return stub()").warning(Error.ANNOTATION_TODO);
		code_strict_v4_("@todo function stub() { return 0; } return stub()").warning(Error.ANNOTATION_TODO);
		code_v4_("@todo var x = 0; return x").warning(Error.ANNOTATION_TODO);

		// @todo on a class method
		code_v4_("""
			class Stub {
				@todo
				compute() { return 0; }
			}
			return new Stub().compute();
			""").warning(Error.ANNOTATION_TODO);
	}

	@Test
	public void testOverride() throws Exception {
		section("@override");

		// @override on method that correctly overrides parent → no error, correct result
		code_v4_("""
			class Animal {
				speak() { return "..."; }
			}
			class Dog extends Animal {
				@override
				speak() { return "woof"; }
			}
			return new Dog().speak();
			""").equals("\"woof\"");

		// @override on method without any parent class → error
		code_v4_("""
			class Dog {
				@override
				speak() { return "woof"; }
			}
			return 0;
			""").error(Error.ANNOTATION_OVERRIDE_NO_PARENT);

		// @override on method that doesn't exist in parent → error
		code_v4_("""
			class Animal {}
			class Dog extends Animal {
				@override
				speak() { return "woof"; }
			}
			return 0;
			""").error(Error.ANNOTATION_OVERRIDE_NO_PARENT);
	}

	@Test
	public void testUnknownAnnotation() throws Exception {
		section("Unknown annotation");

		// Unknown annotations produce a warning but parsing continues
		code_v4_("@foo var x = 5; return x").warning(Error.ANNOTATION_UNKNOWN);
		code_v4_("@foo var x = 5; return x").equals("5");
		code_v4_("@bar function f() { return 1; } return f()").warning(Error.ANNOTATION_UNKNOWN);
	}

	@Test
	public void testMultipleAnnotations() throws Exception {
		section("Multiple annotations");

		// @unused combined with another annotation
		code_strict_v4_("@unused @deprecated var x = 5; return 0").noWarning();

		// @pure and @nodiscard both trigger the same warning when result is discarded
		code_v4_("@pure @nodiscard function f() { return 1; } f(); return 0").warning(Error.ANNOTATION_NODISCARD);

		// @todo with @unused: both annotations apply independently
		code_strict_v4_("@todo @unused function stub() { return 0; } return 0").warning(Error.ANNOTATION_TODO);
	}
}
