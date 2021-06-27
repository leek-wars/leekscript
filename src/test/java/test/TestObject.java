package test;

import leekscript.common.Error;

public class TestObject extends TestCommon {

	public void run() {

		section("Objects");
		// code("return Object()").equals("{}");
		// code("return new Object").equals("{}");
		// code("return new Object()").equals("{}");
		// code("return {}").equals("{}");
		// code("return {a: 12}").equals("{a: 12}");
		// code("return {a: 12, b: 5}").equals("{a: 12, b: 5}");
		// code("return {a: {}, b: []}").equals("{a: {}, b: []}");
		// code("var a = {} return a").equals("{}");
		// code("var a = {b: 12, c: 5} return a").equals("{b: 12, c: 5}");

		section("Objects with functions");
		// code("var f = obj -> obj.a return f({a: 'foo'})").equals("'foo'");
		// code("var f = obj -> obj.a return [f({a: 'foo'}), f({a: 'bar'})]").equals("['foo', 'bar']");
		// code("var f = obj -> obj.a [f(12), f({a: 'bar'})]").error(ls::Error::NO_SUCH_ATTRIBUTE, {"a", "Number"});

		section("No commas");
		// code("return {a: 12 b: 5}").equals("{a: 12, b: 5}");
		// code("return {a: 12 - 2 yo: -6}").equals("{a: 10, yo: -6}");
		// code("return {a: 12 b: 'yo' c: true d: [1 2 3]}").equals("{a: 12, b: 'yo', c: true, d: [1, 2, 3]}");

		section("Classes");
		code_v11("class A { } return new A();").equals("A {}");
		code_v11("class A { a = 10 } var a = [new A()]; a[0].a++ return a[0].a").equals("11");
		code_v11("class A { a = 10 } var a = [new A()]; a[0].a-- return a[0].a").equals("9");
		code_v11("class A { a = 10 } var a = [new A()]; ++a[0].a return a[0].a").equals("11");
		code_v11("class A { a = 10 } var a = [new A()]; --a[0].a return a[0].a").equals("9");
		code_v11("class A { a = 10 m() { return 12 } } var a = new A(); return a.m()").equals("12");
		code_v11("class A { a = 10 m() { return 13 } } var a = new A(); return a['m']()").equals("13");
		code_v11("class A { a = 10 m() { return 13 } } var a = new A(); var m = 'm' return a[m]()").equals("13");
		code_v11("class A { a = 10 m() { return a } } var a = new A(); var array = [a.m] return array[0](a)").equals("10");
		code_v11("class A { a = 10 m() { return a } } var a = new A(); var array = [a['m']] return array[0](a)").equals("10");

		section("Static fields");
		code_v11("class A { static x }").equals("null");
		code_v11("class A { static x } return A.x").equals("null");
		code_v11("class A { static x = 10 } return A.x").equals("10");
		code_v11("class A { static x = 'hello' } return A.x").equals("hello");
		code_v11("class A { static x = [1, 2, 3] } return A.x").equals("[1, 2, 3]");
		code_v11("class A { static x = null } return A.x").equals("null");
		code_v11("class Affiche { static COULEUR = getColor(42, 125, 78) } return Affiche.COULEUR").equals("");

		section("Operators on field");
		code_v11("class A { a = 10 } var a = new A(); return --a.a").equals("9");
		code_v11("class A { a = 10 } var a = new A(); a.a-- return a.a").equals("9");
		code_v11("class A { a = 10 } var a = new A(); return ++a.a").equals("11");
		code_v11("class A { a = 10 } var a = new A(); a.a++ return a.a").equals("11");
		code_v11("class A { a = 10 } var a = new A(); return a.a += 5").equals("15");
		code_v11("class A { a = 10 } var a = new A(); return a.a -= 5").equals("5");
		code_v11("class A { a = 10 } var a = new A(); return a.a *= 5").equals("50");
		code_v11("class A { a = 10 } var a = new A(); return a.a /= 5").equals("2.0");
		code_v11("class A { a = 10 } var a = new A(); return a.a %= 5").equals("0");
		code_v11("class A { a = 10 } var a = new A(); return a.a **= 5").equals("100000");
		code_v11("class A { a = 10 } var a = new A(); return a.a |= 5").equals("15");
		code_v11("class A { a = 10 } var a = new A(); return a.a &= 5").equals("0");
		code_v11("class A { a = 10 } var a = new A(); return a.a ^= 5").equals("15");
		code_v11("class A { a = 10 } var a = new A(); return a.a <<= 5").equals("320");
		code_v11("class A { a = 10 } var a = new A(); return a.a >>= 5").equals("0");
		code_v11("class A { a = 10 } var a = new A(); return a.a >>>= 5").equals("0");

		section("Operators on field in method");
		code_v11("class A { a = 10 m() { return --a } } return new A().m()").equals("9");
		code_v11("class A { a = 10 m() { a-- return a } } return new A().m()").equals("9");
		code_v11("class A { a = 10 m() { return ++a } } return new A().m()").equals("11");
		code_v11("class A { a = 10 m() { a++ return a } } return new A().m()").equals("11");
		code_v11("class A { a = 10 m() { return a += 5 } } return new A().m()").equals("15");
		code_v11("class A { a = 10 m() { return a -= 5 } } return new A().m()").equals("5");
		code_v11("class A { a = 10 m() { return a *= 5 } } return new A().m()").equals("50");
		code_v11("class A { a = 10 m() { return a /= 5 } } return new A().m()").equals("2.0");
		code_v11("class A { a = 10 m() { return a %= 5 } } return new A().m()").equals("0");
		code_v11("class A { a = 10 m() { return a **= 5 } } return new A().m()").equals("100000");
		code_v11("class A { a = 10 m() { return a |= 5 } } return new A().m()").equals("15");
		code_v11("class A { a = 10 m() { return a &= 5 } } return new A().m()").equals("0");
		code_v11("class A { a = 10 m() { return a ^= 5 } } return new A().m()").equals("15");
		code_v11("class A { a = 10 m() { return a <<= 5 } } return new A().m()").equals("320");
		code_v11("class A { a = 10 m() { return a >>= 5 } } return new A().m()").equals("0");
		code_v11("class A { a = 10 m() { return a >>>= 5 } } return new A().m()").equals("0");

		section("Operators on static field");
		code_v11("class A { static a = 10 } return --A.a").equals("9");
		code_v11("class A { static a = 10 } A.a-- return A.a").equals("9");
		code_v11("class A { static a = 10 } return ++A.a").equals("11");
		code_v11("class A { static a = 10 } A.a++ return A.a").equals("11");
		code_v11("class A { static a = 10 } return A.a += 5").equals("15");
		code_v11("class A { static a = 10 } return A.a -= 5").equals("5");
		code_v11("class A { static a = 10 } return A.a *= 5").equals("50");
		code_v11("class A { static a = 10 } return A.a /= 5").equals("2.0");
		code_v11("class A { static a = 10 } return A.a %= 5").equals("0");
		code_v11("class A { static a = 10 } return A.a **= 5").equals("100000");
		code_v11("class A { static a = 10 } return A.a |= 5").equals("15");
		code_v11("class A { static a = 10 } return A.a &= 5").equals("0");
		code_v11("class A { static a = 10 } return A.a ^= 5").equals("15");
		code_v11("class A { static a = 10 } return A.a <<= 5").equals("320");
		code_v11("class A { static a = 10 } return A.a >>= 5").equals("0");
		code_v11("class A { static a = 10 } return A.a >>>= 5").equals("0");

		section("Operators on static field in method");
		code_v11("class A { static a = 10 static m() { return --a } } return A.m()").equals("9");
		code_v11("class A { static a = 10 static m() { a-- return a } } return A.m()").equals("9");
		code_v11("class A { static a = 10 static m() { return ++a } } return A.m()").equals("11");
		code_v11("class A { static a = 10 static m() { a++ return a } } return A.m()").equals("11");
		code_v11("class A { static a = 10 static m() { return a += 5 } } return A.m()").equals("15");
		code_v11("class A { static a = 10 static m() { return a -= 5 } } return A.m()").equals("5");
		code_v11("class A { static a = 10 static m() { return a *= 5 } } return A.m()").equals("50");
		code_v11("class A { static a = 10 static m() { return a /= 5 } } return A.m()").equals("2.0");
		code_v11("class A { static a = 10 static m() { return a %= 5 } } return A.m()").equals("0");
		code_v11("class A { static a = 10 static m() { return a **= 5 } } return A.m()").equals("100000");
		code_v11("class A { static a = 10 static m() { return a |= 5 } } return A.m()").equals("15");
		code_v11("class A { static a = 10 static m() { return a &= 5 } } return A.m()").equals("0");
		code_v11("class A { static a = 10 static m() { return a ^= 5 } } return A.m()").equals("15");
		code_v11("class A { static a = 10 static m() { return a <<= 5 } } return A.m()").equals("320");
		code_v11("class A { static a = 10 static m() { return a >>= 5 } } return A.m()").equals("0");
		code_v11("class A { static a = 10 static m() { return a >>>= 5 } } return A.m()").equals("0");

		section("Inheritance");
		code_v11("class A { x = 10 } class B extends A {} var a = new B() return a.x").equals("10");
		code_v11("class A { m() { return 'ok' } } class B extends A { m() { return super.m() } } var a = new B() return a.m()").equals("ok");
		code_v11("class A { x = 10 } class B extends A {} class C extends B {} var a = new C() return a.x").equals("10");
		code_v11("class A { m() { return 'ok' } } class B extends A {} class C extends B {} var a = new C() return a.m()").equals("ok");
		code_v11("class A { m() { return 'ok' } } class B extends A { m() { return super.m() }} class C extends B { m() { return super.m() } } var a = new C() return a.m()").equals("ok");
		code_v11("class A { m() { return 'ok' } } class B extends A {} class C extends B { m() { return super.m() } } var a = new C() return a.m()").equals("ok");
		code_v11("class A { m() { return 'okA' } } class B extends A { m() { return super.m() + 'B' }} class C extends B { m() { return super.m() + 'C' } } var a = new C()return a.m()").equals("okABC");
		code_v11("class A { items } class B extends A { constructor() { this.items = [] } } var x = new B() return x").equals("B {items: []}");
		code_v11("class A { items } class B extends A { constructor() { this.items = [] super() } } var x = new B() return x").equals("B {items: []}");
		code_v11("class A { m() { return 'parent' } t() { return this.m() } } class B extends A { m() { return 'enfant' } } return new B().t()").equals("enfant");
		code_v11("class A { m() { return 'parent' } t() { return m() } } class B extends A { m() { return 'enfant' } } return new B().t()").equals("enfant");
		code_v11("class A {	public id; } class W extends A {} class H extends W { constructor(id){ this.id=id } }").equals("null");

		section("Access levels: fields");
		code_v11("class A { x = 10 } var a = new A() return a.x").equals("10");
		code_v11("class A { public x = 10 } var a = new A() return a.x").equals("10");
		code_v11("class A { protected x = 10 } var a = new A() return a.x").equals("null");
		code_v11("class A { private x = 10 } var a = new A() return a.x").equals("null");
		code_v11("class A { private x = 10 m() { return x } } var a = new A() return a.m()").equals("10");
		code_v11("class A { private x = 10 } class B extends A {} var a = new B() return a.x").equals("null");
		code_v11("class A { protected x = 10 } class B extends A {} var a = new B() return a.x").equals("null");
		code_v11("class A { protected x = 10 } class B extends A { m() { return x } } var a = new B() return a.m()").equals("10");
		code_v11("class A { private x = 10 constructor() { x = 15 } } var a = new A() return a").equals("A {x: 15}");
		code_v11("class A {	private x; constructor() { this.x = []; } } return new A()").equals("A {x: []}");
		code_v11("class Parent { private chaine = 'Nawak'; public get_chaine_parent() { return this.chaine; } } class Enfant extends Parent { public get_chaine_enfant() { return this.get_chaine_parent() } } var e = Enfant() return [e.get_chaine_parent(), e.get_chaine_enfant() ]").equals("[Nawak, Nawak]");
		code_v11("class Parent { protected chaine = 'Nawak'; public get_chaine_parent() { return this.chaine; } } class Enfant extends Parent { public get_chaine_enfant() { return this.get_chaine_parent() } } var e = Enfant() return [e.get_chaine_parent(), e.get_chaine_enfant() ]").equals("[Nawak, Nawak]");
		code_v11("class A { private x; constructor() { this.x = [] push(this.x, 10); } } return new A()").equals("A {x: [10]}");

		code_v11("class A { private x = 10 m() { return x } } var a = new A() return a.m()").equals("10");
		section("Access levels: static fields");
		code_v11("class A { static x = 10 } return A.x").equals("10");
		code_v11("class A { public static x = 10 } return A.x").equals("10");
		code_v11("class A { protected static x = 10 } return A.x").equals("null");
		code_v11("class A { private static x = 10 } return A.x").equals("null");
		code_v11("class A { private static x = 10 static m() { return x } } return A.m()").equals("10");
		code_v11("class A { private static x = 10 } class B extends A {} return B.x").equals("null");
		code_v11("class A { protected static x = 10 } class B extends A {} return B.x").equals("null");
		code_v11("class A { protected static x = 10 } class B extends A { static m() { return x } } return B.m()").equals("10");
		code_v11("class A { private static x = 10 static m() { return A.x } } return A.m()").equals("10");

		section("Access levels: methods");
		code_v11("class A { m() { return 10 } } var a = new A() return a.m()").equals("10");
		code_v11("class A { public m() { return 10 } } var a = new A() return a.m()").equals("10");
		code_v11("class A { protected m() { return 10 } } var a = new A() return a.m()").equals("null");
		code_v11("class A { private m() { return 10 } } var a = new A() return a.m()").equals("null");
		code_v11("class A { public m() { return 10 } } class B extends A {} var a = new B() return a.m()").equals("10");
		code_v11("class A { protected m() { return 10 } } class B extends A {} var a = new B() return a.m()").equals("null");
		code_v11("class A { private m() { return 10 } } class B extends A {} var a = new B() return a.m()").equals("null");
		code_v11("class A { protected m() { return 10 } } class B extends A { m() { return super.m() } } var a = new B() return a.m()").equals("10");

		section("Access levels: constructors");
		code_v11("class A { constructor() { } } return new A()").equals("A {}");
		code_v11("class A { public constructor() { } } return new A()").equals("A {}");
		code_v11("class A { protected constructor() { } } return new A()").error(Error.PROTECTED_CONSTRUCTOR);
		code_v11("class A { private constructor() { } } return new A()").error(Error.PRIVATE_CONSTRUCTOR);
		code_v11("class A { public constructor() { } } class B extends A {} return new B()").equals("B {}");
		code_v11("class A { x protected constructor() { x = 10 } } class B extends A { constructor() { super() } } return new B().x").equals("10");
		code_v11("class A { x private constructor() { x = 10 } } class B extends A { constructor() { super() } } return new B().x").error(Error.PRIVATE_CONSTRUCTOR);
		code_v11("class A { private constructor() { } } class B extends A {} return new B()").equals("B {}");
		code_v11("class A { private constructor() {} static getInstance() { return new A() } } return A.getInstance()").equals("A {}");

		section("Access levels: static methods");
		code_v11("class A { static m() { return 10 } } return A.m()").equals("10");
		code_v11("class A { public static m() { return 10 } } return A.m()").equals("10");
		code_v11("class A { protected static m() { return 10 } } return A.m()").error(Error.PROTECTED_STATIC_METHOD);
		code_v11("class A { private static m() { return 10 } } return A.m()").error(Error.PRIVATE_STATIC_METHOD);
		code_v11("class A { public static m() { return 10 } } class B extends A {} return B.m()").equals("10");
		code_v11("class A { protected static m() { return 10 } } class B extends A {} return B.m()").error(Error.PROTECTED_STATIC_METHOD);
		code_v11("class A { private static m() { return 10 } } class B extends A {} return B.m()").error(Error.PRIVATE_STATIC_METHOD);

		section("Initialization of fields");
		code_v11("class A { x = [1, 2, 3] } var a = new A() return a.x").equals("[1, 2, 3]");
		code_v11("class A { x = [1, 2, 3] } var a = new A() push(a.x, 4) var b = new A() return b.x").equals("[1, 2, 3]");
		code_v11("class B { y = 10 } class A { x = new B() } var a = new A() return a.x").equals("B {y: 10}");
		code_v11("class B { y = 10 } class A { x = new B() } var a = new A() return a.x.y").equals("10");
		code_v11("class B { y = 10 } class A { static x = new B() } return A.x").equals("B {y: 10}");

		section("Method is a system method");
		code_v11("class A { sqrt() { return sqrt(25) } }").equals("null");
		code_v11("class A { sqrt() { return sqrt(25) } } return new A().sqrt()").equals("5");

		/*
		* Operators
		*/
		section("Object.operator !");
		// code("return !{}").equals("true");
		// code("return !{a: 32}").equals("false");

		section("Object.operator | |");
		// code("var a = {a: 32, b: 'toto', c: false}; return |a|").equals("3");

		section("Object.operator in ()");
		// code("return 12 in {x: 5, y: 12}").equals("true");
		// code("return 12 in {x: 5, y: 'yo'}").equals("false");

		section("Object.operator . ()");
		// code("return { v: 12 }.v").equals("12");
		// code("var a = {b: 12, c: 5} return a.b").equals("12");
		// code("var a = {v: 5} return a.v = 12").equals("12");
		// code("var a = {v: 5} a.v = 12 return a").equals("{v: 12}");
		// code("var a = {v: 5} return a.v = 'salut'").equals("'salut'");
		// code("var a = {v: 5} a.v = 'salut' return a").equals("{v: 'salut'}");
		// code("var a = {b: 12} return a.b += 10").equals("22");
		// code("var a = {b: 12} return a.b -= 10").equals("2");
		// code("var a = {b: 12} return a.b *= 10").equals("120");
		// code("var a = {b: 12} return a.b /= 10").almost(1.2);
		// code("var a = {b: 12} return a.b %= 10").equals("2");
		// code("var o = {} o.new_val = 12 return o").equals("{new_val: 12}");
		// code("var o = {a: 'a'} o.b = 'b' return o").equals("{a: 'a', b: 'b'}");
		// DISABLED_code("Object.readonly.v = 5").exception(ls::vm::Exception::CANT_MODIFY_READONLY_OBJECT);
		// code("var o = [{}, ''][0] return o.values").equals("<function>");
		// code("var pq = [{p: 22, v: 55}] return pq[0].p").equals("22");
		// code("var pq = [{p: 22, v: 55}] var o = pq[0] return o.v").equals("55");

		section("Object.operator ==");
		// code("class A {} return {} == new A").equals("false");
		code_v11("class A { }; class B { }; return new A() == new B();").equals("false");
		// code("class A {} return new A == new A").equals("true");
		// code("return {a: 2} == {}").equals("false");
		// code("return {a: 2} == {a: 1}").equals("false");
		// code("return {a: 2} == {b: 2}").equals("false");
		// code("return {a: 2} == {a: 2}").equals("true");

		section("Object.operator <");
		// code("return {} < {}").equals("false");
		// code("return {a: 2} < {a: 3}").equals("true");
		// code("return {a: 2} < {a: 1}").equals("false");
		// code("return {a: 'b'} < {a: 'c'}").equals("true");
		// code("return {a: 'b'} < {a: 'a'}").equals("false");
		// code("return {b: 2} < {c: 2}").equals("true");
		// code("return {b: 2} < {a: 2}").equals("false");
		// code("return {a: 1} < {a: 1, b: 2}").equals("true");
		// code("return {a: 1, b: 2} < {a: 1}").equals("false");
		// code("return {a: 0, b: 2} < {a: 1}").equals("true");
		// code("class A {} class B {} return new A < new B").equals("true");
		// code("class A {} class B {} return new B < new A").equals("false");
		// code("class A {} return {} < new A").equals("true");
		// code("class A {} return new A < {}").equals("false");

		/*
		* Iteration
		*/
		// code("var s = '' for v in {a: 5, b: 'hello'} { s += v } s").error(ls::Error::Type::VALUE_NOT_ITERABLE, {"{a: 5, b: 'hello'}", env.tmp_object->to_string()}); // TODO .equals("'5hello'");

		/*
		* Methods
		*/
		section("Object.keys()");
		// code("return {}.keys()").equals("[]");
		// code("return {a: 5, b: 'toto', c: true, d: -> 5}.keys()").equals("['a', 'b', 'c', 'd']");
		// code("return 'x' in {x: 5, y: 'yo'}.keys()").equals("true");
		// code("return 'x' in {a: 5, y: 'yo'}.keys()").equals("false");

		section("Object.values()");
		// code("return {}.values()").equals("[]");
		// code("return {a: 1}.values()").equals("[1]");
		// code("return {a: 1, b: 1}.values()").equals("[1, 1]");
		// code("return {a: 5, b: 'toto', c: true, d: -> 5}.values()").equals("[5, 'toto', true, <function>]");

		section("Object.isTrue()");
		// code("if ({x: 12}) { return 5 } else { return 12 }").equals("5");
		// code("if ({}) { return 5 } else { return 12 }").equals("12");

		section("Object.clone()");
		// code("var a = {v: 12} return [a]").equals("[{v: 12}]");

		section("Object.map()");
		// code("return {}.map(x -> x + 1)").equals("{}");
		// code("return {x: 12, y: 5}.map(x -> x + 1)").equals("{x: 13, y: 6}");
		// code("return {x: 'a', y: 'b'}.map(x -> x + ' !')").equals("{x: 'a !', y: 'b !'}");
	}
}
