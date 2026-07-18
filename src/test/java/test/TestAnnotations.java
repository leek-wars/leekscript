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

		// Covariance also applies to generic container types: Array<integer> is a
		// subtype of Array<any>, so narrowing the array element type is allowed
		// (issue #4533).
		code_v4_("""
			class A {
				public Array<any> get() { return []; }
			}
			class B extends A {
				@override
				public Array<integer> get() { return [1, 2]; }
			}
			return new B().get();
			""").equals("[1, 2]");

		// Narrowing an untyped (any) parent return to a container type is covariance
		// too, like narrowing to a class.
		code_v4_("""
			class A {
				get() { return []; }
			}
			class B extends A {
				@override
				Array<integer> get() { return [1]; }
			}
			return new B().get();
			""").equals("[1]");

		// But widening the element type (Array<integer> -> Array<any>) stays rejected.
		code_v4_("""
			class A {
				Array<integer> get() { return [1]; }
			}
			class B extends A {
				@override
				Array<any> get() { return []; }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// Narrowing an untyped (any) parent return to string is covariance too:
		// string is a reference type, not a primitive.
		code_v4_("""
			class A {
				get() { return ""; }
			}
			class B extends A {
				@override
				string get() { return "x"; }
			}
			return new B().get();
			""").equals("\"x\"");

		// Narrowing `any` to boolean stays rejected: boolean is a primitive.
		code_v4_("""
			class A {
				get() { return true; }
			}
			class B extends A {
				@override
				boolean get() { return false; }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// Narrowing `any` to real stays rejected: real is a primitive.
		code_v4_("""
			class A {
				get() { return 1.5; }
			}
			class B extends A {
				@override
				real get() { return 2.5; }
			}
			return 0;
			""").error(Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE);

		// A sibling element type is not a subtype, so narrowing Array<Dog> to
		// Array<Cat> is rejected (only narrowing along the chain is allowed).
		code_v4_("""
			class Animal {}
			class Dog extends Animal {}
			class Cat extends Animal {}
			class A {
				Array<Dog> get() { return []; }
			}
			class B extends A {
				@override
				Array<Cat> get() { return []; }
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
