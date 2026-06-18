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

		// --- Purity checking ---

		// Pure function using only its own locals/parameters → no warning
		code_v4_("@pure function f() { var local = 0; local = 1; return local } return f()").noWarning();
		code_v4_("@pure function f() { var local = 0; local = 1; return local } return f()").equals("1");

		// Mutating a local array/map of its own is pure
		code_v4_("@pure function f() { var a = [1, 2]; a[0] = 5; return a } return f()").noWarning();

		// Writing to a global is a side effect → not pure
		code_v4_("global g = 0; @pure function f() { g = 1; return g } return f()").warning(Error.ANNOTATION_NOT_PURE);

		// Incrementing a global is a side effect → not pure
		code_v4_("global g = 0; @pure function f() { g++; return g } return f()").warning(Error.ANNOTATION_NOT_PURE);

		// Calling a side-effecting builtin (I/O) → not pure
		code_v4_("@pure function f(a) { debug(a); return a } return f(1)").warning(Error.ANNOTATION_NOT_PURE);

		// Calling an array-mutating builtin → not pure
		code_v4_("@pure function f(arr) { push(arr, 1); return arr } return f([])").warning(Error.ANNOTATION_NOT_PURE);

		// Calling a pure builtin (no mutation) → still pure
		code_v4_("@pure function f(a) { return abs(a) } return f(-3)").noWarning();
		code_v4_("@pure function f(a) { return abs(a) } return f(-3)").equals("3");

		// Calling another @pure function is fine
		code_v4_("@pure function helper(x) { return x * 2 } @pure function f(x) { return helper(x) } return f(2)").noWarning();
		code_v4_("@pure function helper(x) { return x * 2 } @pure function f(x) { return helper(x) } return f(2)").equals("4");

		// Calling an unannotated but actually-pure function is fine: purity is verified
		// transitively, not by requiring the @pure annotation everywhere
		code_v4_("function helper(x) { return x * 2 } @pure function f(x) { return helper(x) } return f(2)").noWarning();
		code_v4_("function helper(x) { return x * 2 } @pure function f(x) { return helper(x) } return f(2)").equals("4");

		// ...even through a chain of unannotated pure functions
		code_v4_("function a(x) { return b(x) } function b(x) { return x + 1 } @pure function f(x) { return a(x) } return f(1)").noWarning();
		code_v4_("function a(x) { return b(x) } function b(x) { return x + 1 } @pure function f(x) { return a(x) } return f(1)").equals("2");

		// Calling an unannotated function that IS impure → warning
		code_v4_("global g = 0; function bump() { g = 1; return g } @pure function f() { return bump() } return f()").warning(Error.ANNOTATION_NOT_PURE);

		// ...including impurity reached transitively through another function
		code_v4_("function a() { return b() } function b() { debug(1); return 0 } @pure function f() { return a() } return f()").warning(Error.ANNOTATION_NOT_PURE);

		// Recursion on a @pure function is allowed (it calls itself, which is @pure)
		code_v4_("@pure function fact(n) { return n <= 1 ? 1 : n * fact(n - 1) } return fact(5)").noWarning();
		code_v4_("@pure function fact(n) { return n <= 1 ? 1 : n * fact(n - 1) } return fact(5)").equals("120");

		// Side effects inside a lambda defined within a @pure function are NOT attributed
		// to it (the lambda is a separate scope): defining it is pure.
		code_v4_("global g = 0; @pure function f() { var fn = function() { g = 1 }; return 0 } return f()").noWarning();

		// @pure on a method: mutating the object (this) is a side effect → not pure
		code_v4_("""
			class A {
				public integer x = 0
				@pure setX() { this.x = 1; return this.x }
			}
			return new A().setX()
			""").warning(Error.ANNOTATION_NOT_PURE);
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

		section("@override — covariant return type");

		// Narrowing the return type (covariance, like Java): the child may return a
		// subtype of what the parent returns. Here self() narrows Animal → Dog, so the
		// statically-typed result can call a Dog-only method.
		code_v4_("""
			class Animal {
				Animal self() { return this; }
			}
			class Dog extends Animal {
				@override
				Dog self() { return this; }
				bark() { return "woof"; }
			}
			return new Dog().self().bark();
			""").equals("\"woof\"");

		// Covariance is limited to class types, like Java: a primitive return type
		// cannot be narrowed (Java forbids real → integer covariant returns too).
		code_v4_("""
			class A {
				real get() { return 1.5; }
			}
			class B extends A {
				@override
				integer get() { return 2; }
			}
			return new B().get();
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// Covariance over several levels of inheritance.
		code_v4_("""
			class Animal {
				Animal self() { return this; }
			}
			class Dog extends Animal {}
			class Puppy extends Dog {
				@override
				Puppy self() { return this; }
				yip() { return "yip"; }
			}
			return new Puppy().self().yip();
			""").equals("\"yip\"");

		// Identical return type is still accepted.
		code_v4_("""
			class Animal {
				Animal self() { return this; }
			}
			class Dog extends Animal {
				@override
				Animal self() { return this; }
			}
			return new Dog().self() == null ? 0 : 1;
			""").equals("1");

		// Widening the return type (supertype) is rejected.
		code_v4_("""
			class Animal {
				Dog get() { return new Dog(); }
			}
			class Dog extends Animal {
				@override
				Animal get() { return new Animal(); }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// Unrelated return type is rejected.
		code_v4_("""
			class Animal {
				integer get() { return 1; }
			}
			class Dog extends Animal {
				@override
				string get() { return "x"; }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// Parameter types remain invariant (like Java): changing them is rejected.
		code_v4_("""
			class Animal {
				set(integer x) { return x; }
			}
			class Dog extends Animal {
				@override
				set(string x) { return x; }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// Covariance also applies to an implicit override (no @override annotation):
		// the narrowed return type still lets the result call a child-only method.
		code_v4_("""
			class Animal {
				Animal self() { return this; }
			}
			class Dog extends Animal {
				Dog self() { return this; }
				bark() { return "woof"; }
			}
			return new Dog().self().bark();
			""").equals("\"woof\"");

		// A sibling subclass is not a subtype of the parent's return type, so it is
		// rejected: only narrowing along the inheritance chain is allowed.
		code_v4_("""
			class Animal {}
			class Dog extends Animal {}
			class Cat extends Animal {}
			class A {
				Dog get() { return new Dog(); }
			}
			class B extends A {
				@override
				Cat get() { return new Cat(); }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// `any` is the top type, so narrowing an untyped (any) parent return to a
		// class is covariance too: a common case where a child types a parent method.
		code_v4_("""
			class Animal {
				wrap() { return this; }
			}
			class Dog extends Animal {
				@override
				Dog wrap() { return this; }
				bark() { return "woof"; }
			}
			return new Dog().wrap().bark();
			""").equals("\"woof\"");

		// But narrowing `any` to a primitive stays rejected, consistent with the
		// primitive restriction (Java forbids Object -> int covariant returns too).
		code_v4_("""
			class A {
				get() { return 1; }
			}
			class B extends A {
				@override
				integer get() { return 2; }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);
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

	/**
	 * Couvre le sentinel NO_ANNOTATIONS (List.of()) utilisé dans classDeclaration et
	 * variableDeclaration. Le partage de l'instance immuable au lieu d'allouer un
	 * ArrayList vide à chaque membre/declaration est correct ssi (1) applyAnnotations
	 * skip si vide, (2) les comparaisons par référence avec NO_ANNOTATIONS détectent
	 * bien le cas vide pour basculer à un vrai ArrayList quand on découvre une annotation.
	 */
	@Test
	public void testClassMembersNoAnnotationSentinel() throws Exception {
		section("Class members — NO_ANNOTATIONS sentinel");
		// Classe sans aucune annotation sur ses membres — tous prennent NO_ANNOTATIONS
		code_v4_("class A { public integer x = 0 public get() { return this.x } } var a = new A() return a.get()").equals("0");
		// Classe avec annotation sur UN membre seulement : le sentinel doit bien
		// basculer vers ArrayList<> uniquement pour le membre concerné, sans
		// muter l'instance partagée
		code_v4_("class B { public integer a = 1 @deprecated public integer b = 2 public integer c = 3 public sum() { return this.a + this.c } } return new B().sum()").equals("4");
		// Plusieurs classes successives sans annotations : chacune doit avoir sa propre
		// vue propre (régression possible si NO_ANNOTATIONS était mutée)
		code_v4_("class C1 { public integer x = 10 } class C2 { public integer x = 20 } return new C1().x + new C2().x").equals("30");
	}
}
