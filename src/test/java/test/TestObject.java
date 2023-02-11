package test;

import leekscript.common.Error;

public class TestObject extends TestCommon {

	public void run() {

		section("Objects");
		code_v3_("return Object()").equals("{}");
		code_v3_("return new Object").equals("{}");
		code_v3_("return new Object()").equals("{}");
		code_v2_("return {}").equals("{}");
		code_v2_("return {a: 12}").equals("{a: 12}");
		code_v2_("return {a: 12, b: 5}").equals("{a: 12, b: 5}");
		code_v2_("return {a: {}, b: []}").equals("{a: {}, b: []}");
		code_v2_("var a = {} return a").equals("{}");
		code_v2_("var a = {b: 12, c: 5} return a").equals("{b: 12, c: 5}");

		section("Objects with functions");
		code_v2_("var f = function(obj) { return obj.a } return f({a: 'foo'})").equals("\"foo\"");
		code_v2_("var f = function(obj) { return obj.a } return [f({a: 'foo'}), f({a: 'bar'})]").equals("[\"foo\", \"bar\"]");
		//code("var f = function(obj) { return obj.a } [f(12), f({a: 'bar'})]").error(ls::Error::NO_SUCH_ATTRIBUTE, {"a", "Number"});

		section("No commas");
		code_v2_("return {a: 12 b: 5}").equals("{a: 12, b: 5}");
		code_v2_("return {a: 12 - 2 yo: -6}").equals("{a: 10, yo: -6}");
		code_v2_("return {a: 12 b: 'yo' c: true d: [1 2 3]}").equals("{a: 12, b: \"yo\", c: true, d: [1, 2, 3]}");

		section("Object in object");
		code_v2_("var a = {x: 1} var b = {v: a} return b").equals("{v: {x: 1}}");
		code_v2_("var a = {x: 1} var b = {v: a}.v return a == b").equals("true");

		section("Classes");
		code_v2_("class A { } return new A();").equals("A {}");
		code_v2_("class A { a = 10 } var a = [new A()]; a[0].a++ return a[0].a").equals("11");
		code_v2_("class A { a = 10 } var a = [new A()]; a[0].a-- return a[0].a").equals("9");
		code_v2_("class A { a = 10 } var a = [new A()]; ++a[0].a return a[0].a").equals("11");
		code_v2_("class A { a = 10 } var a = [new A()]; --a[0].a return a[0].a").equals("9");
		code_v2_("class A { a = 10 m() { return 12 } } var a = new A(); return a.m()").equals("12");
		code_v2_("class A { a = 10 m() { return 13 } } var a = new A(); return a['m']()").equals("13");
		code_v2_("class A { a = 10 m() { return 13 } } var a = new A(); var m = 'm' return a[m]()").equals("13");
		code_v2_("class A { a = 10 m() { return a } } var a = new A(); var array = [a.m] return array[0](a)").equals("10");
		code_v2_("class A { a = 10 m() { return a } } var a = new A(); var array = [a['m']] return array[0](a)").equals("10");

		section("Reserved fields");
		code_v2("class A { for while if var this }").error(Error.NONE);
		code_v3_("class A { for }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { if }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { while }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { var }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { this }").error(Error.VARIABLE_NAME_UNAVAILABLE);

		section("Constructors");
		code_v2_("class A {} var a = new A() return a").equals("A {}");
		code_v2_("class A { constructor() {} } var a = new A() return a").equals("A {}");
		code_v2_("class A { constructor() { return; } } var a = new A() return a").equals("A {}");

		section("Default arguments on constructors");
		code_v2_3("class A { constructor(x, y) {} constructor(x, y, z = 2) {} }").error(Error.NONE);
		code_v4_("class A { constructor(x, y) {} constructor(x, y, z = 2) {} }").error(Error.DUPLICATED_CONSTRUCTOR);
		code_v2_("class A { constructor(x) {} constructor(x, y, z = 2) {} }").error(Error.NONE);
		code_v2_("class A { f constructor(x = 2) { f = x } } return A().f").equals("2");
		code_v2_("class A { f constructor(x = 2) { f = x } } return A(9).f").equals("9");
		code_v2_("class A { f constructor(x, y = 2) { f = x * y } } return A(9).f").equals("18");
		code_v2_("class A { f constructor(x = 5, y = 7) { f = x * y } } return A().f").equals("35");
		code_v2_("class A { f constructor(x = 5, y = 7, z = 10) { f = x * y * z } } return A().f").equals("350");
		code_v2_("class A { f constructor(x = 5, y = 7, z = 10) { f = x * y * z } } return A(4).f").equals("280");
		code_v2_("class A { f static v() { return 55 } constructor(x, y = v()) { f = x * y } } return A(9).f").equals("495");
		code_v2_("class A { f constructor(x, y = x) { f = x * y } } return A(9).f").equals("81");
		code_v2_("class A { f constructor(x, y = x, z = y) { f = x * y * z } } return A(9).f").equals("729");
		code_v2_("class A { f constructor(x = 12) { f = arrayMap([1, 1, 1], (_) => x) } } return new A().f").equals("[12, 12, 12]");
		code_v2_("class A { f constructor(x = 12, y = x) { f = arrayMap([1, 1, 1], (_) => x + y) } } return new A().f").equals("[24, 24, 24]");

		section("Static fields");
		code_v2_("class A { static x }").equals("null");
		code_v2_("class A { static x } return A.x").equals("null");
		code_v2_("class A { static x = 10 } return A.x").equals("10");
		code_v2_("class A { static x = 'hello' } return A.x").equals("\"hello\"");
		code_v2_("class A { static x = [1, 2, 3] } return A.x").equals("[1, 2, 3]");
		code_v2_("class A { static x = null } return A.x").equals("null");
		code_v2_("class Affiche { static COULEUR = getColor(42, 125, 78) } return Affiche.COULEUR").equals("2784590");
		code_v2_("class A { static b } A.c").error(Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);
		code_v2_("class A { static b static m() { class.c } }").error(Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);

		section("Reserved static fields");
		code_v2("class A { static for static while static if static var static this }").error(Error.NONE);
		code_v3_("class A { static for }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { static if }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { static while }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { static var }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v3_("class A { static this }").error(Error.VARIABLE_NAME_UNAVAILABLE);

		section("Static field dependencies on other static fields");
		code_v2_("class A { private static x = 1 static y = A.x } return A.y").equals("1");
		code_v2_("class A { private static x = 1 static y = class.x } return A.y").equals("1");
		code_v2_("class A { private static x = 1 static y = x } return A.y").equals("1");

		section("Operators on field");
		code_v2_("class A { a = 10 } var a = new A(); return --a.a").equals("9");
		code_v2_("class A { a = 10 } var a = new A(); a.a-- return a.a").equals("9");
		code_v2_("class A { a = 10 } var a = new A(); return ++a.a").equals("11");
		code_v2_("class A { a = 10 } var a = new A(); a.a++ return a.a").equals("11");
		code_v2_("class A { a = 10 } var a = new A(); return a.a += 5").equals("15");
		code_v2_("class A { a = 10 } var a = new A(); return a.a -= 5").equals("5");
		code_v2_("class A { a = 10 } var a = new A(); return a.a *= 5").equals("50");
		code_v2_("class A { a = 10 } var a = new A(); return a.a /= 5").equals("2.0");
		code_v2_("class A { a = 10 } var a = new A(); return a.a \\= 3").equals("3");
		code_v2_("class A { a = 10 } var a = new A(); return a.a %= 5").equals("0");
		code_v2_("class A { a = 10 } var a = new A(); return a.a **= 5").equals("100000");
		code_v2_("class A { a = 10 } var a = new A(); return a.a |= 5").equals("15");
		code_v2_("class A { a = 10 } var a = new A(); return a.a &= 5").equals("0");
		code_v2_("class A { a = 10 } var a = new A(); return a.a ^= 5").equals("15");
		code_v2_("class A { a = 10 } var a = new A(); return a.a <<= 5").equals("320");
		code_v2_("class A { a = 10 } var a = new A(); return a.a >>= 5").equals("0");
		code_v2_("class A { a = 10 } var a = new A(); return a.a >>>= 5").equals("0");

		section("Operators on field in method");
		code_v2_("class A { a = 10 m() { return --a } } return new A().m()").equals("9");
		code_v2_("class A { a = 10 m() { a-- return a } } return new A().m()").equals("9");
		code_v2_("class A { a = 10 m() { return ++a } } return new A().m()").equals("11");
		code_v2_("class A { a = 10 m() { a++ return a } } return new A().m()").equals("11");
		code_v2_("class A { a = 10 m() { return a += 5 } } return new A().m()").equals("15");
		code_v2_("class A { a = 10 m() { return a -= 5 } } return new A().m()").equals("5");
		code_v2_("class A { a = 10 m() { return a *= 5 } } return new A().m()").equals("50");
		code_v2_("class A { a = 10 m() { return a /= 5 } } return new A().m()").equals("2.0");
		code_v2_("class A { a = 10 m() { return a %= 5 } } return new A().m()").equals("0");
		code_v2_("class A { a = 10 m() { return a **= 5 } } return new A().m()").equals("100000");
		code_v2_("class A { a = 10 m() { return a |= 5 } } return new A().m()").equals("15");
		code_v2_("class A { a = 10 m() { return a &= 5 } } return new A().m()").equals("0");
		code_v2_("class A { a = 10 m() { return a ^= 5 } } return new A().m()").equals("15");
		code_v2_("class A { a = 10 m() { return a <<= 5 } } return new A().m()").equals("320");
		code_v2_("class A { a = 10 m() { return a >>= 5 } } return new A().m()").equals("0");
		code_v2_("class A { a = 10 m() { return a >>>= 5 } } return new A().m()").equals("0");

		section("Operators on static field");
		code_v2_("class A { static a = 10 } return --A.a").equals("9");
		code_v2_("class A { static a = 10 } A.a-- return A.a").equals("9");
		code_v2_("class A { static a = 10 } return ++A.a").equals("11");
		code_v2_("class A { static a = 10 } A.a++ return A.a").equals("11");
		code_v2_("class A { static a = 10 } return A.a += 5").equals("15");
		code_v2_("class A { static a = 10 } return A.a -= 5").equals("5");
		code_v2_("class A { static a = 10 } return A.a *= 5").equals("50");
		code_v2_("class A { static a = 10 } return A.a /= 5").equals("2.0");
		code_v2_("class A { static a = 10 } return A.a %= 5").equals("0");
		code_v2_("class A { static a = 10 } return A.a **= 5").equals("100000");
		code_v2_("class A { static a = 10 } return A.a |= 5").equals("15");
		code_v2_("class A { static a = 10 } return A.a &= 5").equals("0");
		code_v2_("class A { static a = 10 } return A.a ^= 5").equals("15");
		code_v2_("class A { static a = 10 } return A.a <<= 5").equals("320");
		code_v2_("class A { static a = 10 } return A.a >>= 5").equals("0");
		code_v2_("class A { static a = 10 } return A.a >>>= 5").equals("0");

		section("Operators on static field in method");
		code_v2_("class A { static a = 10 static m() { return --a } } return A.m()").equals("9");
		code_v2_("class A { static a = 10 static m() { a-- return a } } return A.m()").equals("9");
		code_v2_("class A { static a = 10 static m() { return ++a } } return A.m()").equals("11");
		code_v2_("class A { static a = 10 static m() { a++ return a } } return A.m()").equals("11");
		code_v2_("class A { static a = 10 static m() { return a += 5 } } return A.m()").equals("15");
		code_v2_("class A { static a = 10 static m() { return a -= 5 } } return A.m()").equals("5");
		code_v2_("class A { static a = 10 static m() { return a *= 5 } } return A.m()").equals("50");
		code_v2_("class A { static a = 10 static m() { return a /= 5 } } return A.m()").equals("2.0");
		code_v2_("class A { static a = 10 static m() { return a %= 5 } } return A.m()").equals("0");
		code_v2_("class A { static a = 10 static m() { return a **= 5 } } return A.m()").equals("100000");
		code_v2_("class A { static a = 10 static m() { return a |= 5 } } return A.m()").equals("15");
		code_v2_("class A { static a = 10 static m() { return a &= 5 } } return A.m()").equals("0");
		code_v2_("class A { static a = 10 static m() { return a ^= 5 } } return A.m()").equals("15");
		code_v2_("class A { static a = 10 static m() { return a <<= 5 } } return A.m()").equals("320");
		code_v2_("class A { static a = 10 static m() { return a >>= 5 } } return A.m()").equals("0");
		code_v2_("class A { static a = 10 static m() { return a >>>= 5 } } return A.m()").equals("0");

		section("Operators on static field in method with class.");
		code_v2_("class A { static a = 10 static m() { return --class.a } } return A.m()").equals("9");
		code_v2_("class A { static a = 10 static m() { class.a-- return class.a } } return A.m()").equals("9");
		code_v2_("class A { static a = 10 static m() { return ++class.a } } return A.m()").equals("11");
		code_v2_("class A { static a = 10 static m() { class.a++ return class.a } } return A.m()").equals("11");
		code_v2_("class A { static a = 10 static m() { return class.a += 5 } } return A.m()").equals("15");
		code_v2_("class A { static a = 10 static m() { return class.a -= 5 } } return A.m()").equals("5");
		code_v2_("class A { static a = 10 static m() { return class.a *= 5 } } return A.m()").equals("50");
		code_v2_("class A { static a = 10 static m() { return class.a /= 5 } } return A.m()").equals("2.0");
		code_v2_("class A { static a = 10 static m() { return class.a %= 5 } } return A.m()").equals("0");
		code_v2_("class A { static a = 10 static m() { return class.a **= 5 } } return A.m()").equals("100000");
		code_v2_("class A { static a = 10 static m() { return class.a |= 5 } } return A.m()").equals("15");
		code_v2_("class A { static a = 10 static m() { return class.a &= 5 } } return A.m()").equals("0");
		code_v2_("class A { static a = 10 static m() { return class.a ^= 5 } } return A.m()").equals("15");
		code_v2_("class A { static a = 10 static m() { return class.a <<= 5 } } return A.m()").equals("320");
		code_v2_("class A { static a = 10 static m() { return class.a >>= 5 } } return A.m()").equals("0");
		code_v2_("class A { static a = 10 static m() { return class.a >>>= 5 } } return A.m()").equals("0");

		section("Call of static field");
		code_v2_("class A { static a = -> 12 } return A.a()").equals("12");
		code_v2_("class A { private static a = -> 12 } return A.a()").error(Error.PRIVATE_STATIC_FIELD);
		code_v2_("class A { static a = -> 12 } return A.b()").error(Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);

		section("Final fields");
		code_v2_("class A { final a = 12 } var a = new A() a.a = 15 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a += 10 return a.a").equals("12");
		code_v2_("class A { final a = [] } var a = new A() a.a += 10 return a.a").equals("[]");
		code_v2_("class A { final a = 12 } var a = new A() a['a'] = 15 return a['a']").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a['a'] += 15 return a['a']").equals("12");
		code_v2_("class A { final a m(x) { this.a = x } } return new A(12)").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { final a m(x) { a = x } } return new A(12)").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { final a m(x) { if (2) { this.a = x } } } return new A(12)").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { final a m(x) { if (2) { a = x } } } return new A(12)").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { final a constructor(x) { this.a = x } } return new A(12)").equals("A {a: 12}");
		code_v2_("class A { final a constructor(x) { if (1) { this.a = x } } } return new A(12)").equals("A {a: 12}");
		code_v2_("class A { final a constructor(x) { a = x } } return new A(12)").equals("A {a: 12}");
		code_v2_("class A { final a constructor(x) { if (1) { a = x } } } return new A(12)").equals("A {a: 12}");
		code_v2_("class A { final a = 12 } var a = new A() a.a++ return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a-- return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A(); ++a.a return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A(); --a.a return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a += 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a -= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a *= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a **= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a /= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a %= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a |= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a &= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a ^= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a >>= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a >>>= 10 return a.a").equals("12");
		code_v2_("class A { final a = 12 } var a = new A() a.a <<= 10 return a.a").equals("12");

		section("Final static fields");
		code_v2_("class A { static final a = 12 } A.a = 15").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { static final a = 12 } A.a += 10 return A.a").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { static final a = [] } A.a += 10 return A.a").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v4_("class A { static final a = [1, 2, 3] } arrayClear(A.a) return A.a").equals("[]");
		code_v2_("class A { static final a = 12 } A['a'] = 15 return A['a']").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v2_("class A { static final a = 12 } A['a'] += 15 return A['a']").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE++").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE--").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("++Real.MAX_VALUE").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("--Real.MAX_VALUE").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE += 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE -= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE *= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE /= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE %= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE **= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE |= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE &= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE ^= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE >>= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE <<= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE >>>= 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		/**
		 * Methods
		 */
		section("Static methods");
		code_v2_("class A { static a() { return 12 } } return A.a()").equals("12");
		code_v2_("class A { static a(x) { return 12 } } return A.a()").error(Error.INVALID_PARAMETER_COUNT);
		code_v2_("class A { static a() { return 12 } } return A.b()").error(Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);
		code_v2_("class A { static f(x) {} static g() { f(1) } }").error(Error.NONE);
		code_v2_("class A { static f(x) {} static g() { f() } }").error(Error.INVALID_PARAMETER_COUNT);
		code_v2_("class A { static f(x) {} static g() { class.f() } }").error(Error.INVALID_PARAMETER_COUNT);

		section("Duplicated static method");
		code_v2_3("class A { static m() {} static m() {} }").error(Error.NONE); // OK in LS2-3
		code_v4_("class A { static m() {} static m() {} }").error(Error.DUPLICATED_METHOD); // Error in LS4+
		code_v4_("class A { m() {} static m() {} }").error(Error.DUPLICATED_METHOD);
		code_v2_("class A { static m() {} static m(x) {} }").error(Error.NONE);

		section("Static method calls with with class.");
		code_v2_("class A { static m() { return 'x' } t() { return class.m() } } var a = new A() return a.t()").equals("\"x\"");
		code_v2_("class A { static m() { return 'x' } t() { return class.zz() } } var a = new A() return a.t()").error(Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);

		section("Default arguments on static methods");
		code_v2_3("class A { static m(x, y) {} static m(x, y, z = 2) {} }").error(Error.NONE);
		code_v4_("class A { static m(x, y) {} static m(x, y, z = 2) {} }").error(Error.DUPLICATED_METHOD);
		code_v2_("class A { static m(x) {} static m(x, y, z = 2) {} }").error(Error.NONE);
		code_v2_("class A { static m(x = 2) { return x } } return A.m()").equals("2");
		code_v2_("class A { static m(x = 2) { return x } } return A.m(9)").equals("9");
		code_v2_("class A { static m(x, y = 2) { return x * y } } return A.m(9)").equals("18");
		code_v2_("class A { static m(x = 5, y = 7) { return x * y } } return A.m()").equals("35");
		code_v2_("class A { static m(x = 5, y = 7, z = 10) { return x * y * z } } return A.m()").equals("350");
		code_v2_("class A { static m(x = 5, y = 7, z = 10) { return x * y * z } } return A.m(4)").equals("280");
		code_v2_("class A { static v() { return 55 } static m(x, y = v()) { return x * y } } return A.m(9)").equals("495");
		code_v2_("class A { static m(x, y = x) { return x * y } } return A.m(9)").equals("81");
		code_v2_("class A { static m(x, y = x, z = y) { return x * y * z } } return A.m(9)").equals("729");
		code_v2_("class A { static m(x = 12) { return arrayMap([1, 1, 1], (_) => x) } } return A.m()").equals("[12, 12, 12]");
		code_v2_("class A { static m(x = 12, y = x) { return arrayMap([1, 1, 1], (_) => x + y) } } return A.m()").equals("[24, 24, 24]");

		section("Static method null argument");
		code_v2_("class Debug { static log(x) { debug(x) } } Debug.log(null)").equals("null");

		section("Methods");
		code_v2_("class A { a(x) { b(x) } b(x, y) {} }").error(Error.INVALID_PARAMETER_COUNT);
		code_v2_("class A { a(x) { this.b(x) } b(x, y) {} }").error(Error.INVALID_PARAMETER_COUNT);

		section("Duplicated method");
		code_v2_3("class A { m() {} m() {} }").error(Error.NONE); // OK in LS2-3
		code_v4_("class A { m() {} m() {} }").error(Error.DUPLICATED_METHOD); // Error in LS4+
		code_v4_("class A { static m() {} m() {} }").error(Error.DUPLICATED_METHOD);
		code_v2_("class A { m() {} m(x) {} }").error(Error.NONE);

		section("Default arguments on methods");
		code_v2_3("class A { m(x, y) {} m(x, y, z = 2) {} }").error(Error.NONE);
		code_v4_("class A { m(x, y) {} m(x, y, z = 2) {} }").error(Error.DUPLICATED_METHOD);
		code_v2_("class A { m(x) {} m(x, y, z = 2) {} }").error(Error.NONE);
		code_v2_("class A { m(x = 2) { return x } } return new A().m()").equals("2");
		code_v2_("class A { m(x = 2) { return x } } return new A().m(9)").equals("9");
		code_v2_("class A { m(x, y = 2) { return x * y } } return new A().m(9)").equals("18");
		code_v2_("class A { m(x = 5, y = 7) { return x * y } } return new A().m()").equals("35");
		code_v2_("class A { m(x = 5, y = 7, z = 10) { return x * y * z } } return new A().m()").equals("350");
		code_v2_("class A { m(x = 5, y = 7, z = 10) { return x * y * z } } return new A().m(4)").equals("280");
		code_v2_("class A { v() { return 55 } m(x, y = v()) { return x * y } } return new A().m(9)").equals("495");
		code_v2_("class A { m(x, y = x) { return x * y } } return new A().m(9)").equals("81");
		code_v2_("class A { m(x, y = x, z = y) { return x * y * z } } return new A().m(9)").equals("729");
		code_v2_("class A { m(x = 12) { return arrayMap([1, 1, 1], (_) => x) } } return new A().m()").equals("[12, 12, 12]");
		code_v2_("class A { m(x = 12, y = x) { return arrayMap([1, 1, 1], (_) => x + y) } } return new A().m()").equals("[24, 24, 24]");

		section("Duplicated method argument");
		code_v2_("class A { m(x, x) {} }").error(Error.DUPLICATED_ARGUMENT);
		code_v2_("class A { static m(x, x) {} }").error(Error.DUPLICATED_ARGUMENT);
		code_v2_("class A { constructor(x, x) {} }").error(Error.DUPLICATED_ARGUMENT);

		section("Field access by array access");
		code_v2_("var test = {} test['a'] = 8 return test").equals("{a: 8}");
		code_v2_("var test = {} test['a'] = 8 test['b'] = 12 return test").equals("{a: 8, b: 12}");
		code_v2_("class Test { a b c } var test2 = new Test() test2['a'] = 8 return test2").equals("Test {a: 8, b: null, c: null}");
		code_v2_("class Test { a b c } var test2 = new Test() for (var field in Test.fields) { test2[field] = 8 } return test2").equals("Test {a: 8, b: 8, c: 8}");
		code_v2_("class Test { a b c constructor(a, b, c) { this.a = a this.b = b this.c = c } } var test1 = new Test(1, 2, 3) var test2 = new Test() for (var field in test1.class.fields) { test2[field] = test1[field] } return test2").equals("Test {a: 1, b: 2, c: 3}");
		code_v2_("class Test { a b c constructor(a, b, c) { this.a = a this.b = b this.c = c } } var test1 = new Test(1, 2, 3) var test2 = new Test() for (var field in test2.class.fields) { test2[field] = test1[field] } return test2").equals("Test {a: 1, b: 2, c: 3}");
		code_v2_("class A { a = 6 m() { return this['a'] } } return new A().m()").equals("6");

		section("Operators on field by array access");
		code_v2_("class A { a = 10 } var a = new A(); return --a['a']").equals("9");
		code_v2_("class A { a = 10 } var a = new A(); a['a']-- return a['a']").equals("9");
		code_v2_("class A { a = 10 } var a = new A(); return ++a['a']").equals("11");
		code_v2_("class A { a = 10 } var a = new A(); a['a']++ return a['a']").equals("11");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] += 5").equals("15");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] -= 5").equals("5");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] *= 5").equals("50");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] /= 5").equals("2.0");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] %= 5").equals("0");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] **= 5").equals("100000");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] |= 5").equals("15");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] &= 5").equals("0");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] ^= 5").equals("15");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] <<= 5").equals("320");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] >>= 5").equals("0");
		code_v2_("class A { a = 10 } var a = new A(); return a['a'] >>>= 5").equals("0");

		section("Static field access by array access");
		code_v2_("class Test { static a static b static c } Test['a'] = 12 Test['b'] = 15 Test['c'] = 20 return [Test.a, Test.b, Test.c]").equals("[12, 15, 20]");
		code_v2_("class A { static a = 6 static m() { return class['a'] } } return A.m()").equals("6");

		section("Operators on static field by array access");
		code_v2_("class A { static a = 10 } return --A['a']").equals("9");
		code_v2_("class A { static a = 10 } A['a']-- return A['a']").equals("9");
		code_v2_("class A { static a = 10 } return ++A['a']").equals("11");
		code_v2_("class A { static a = 10 } A['a']++ return A['a']").equals("11");
		code_v2_("class A { static a = 10 } return A['a'] += 5").equals("15");
		code_v2_("class A { static a = 10 } return A['a'] -= 5").equals("5");
		code_v2_("class A { static a = 10 } return A['a'] *= 5").equals("50");
		code_v2_("class A { static a = 10 } return A['a'] /= 5").equals("2.0");
		code_v2_("class A { static a = 10 } return A['a'] %= 5").equals("0");
		code_v2_("class A { static a = 10 } return A['a'] **= 5").equals("100000");
		code_v2_("class A { static a = 10 } return A['a'] |= 5").equals("15");
		code_v2_("class A { static a = 10 } return A['a'] &= 5").equals("0");
		code_v2_("class A { static a = 10 } return A['a'] ^= 5").equals("15");
		code_v2_("class A { static a = 10 } return A['a'] <<= 5").equals("320");
		code_v2_("class A { static a = 10 } return A['a'] >>= 5").equals("0");
		code_v2_("class A { static a = 10 } return A['a'] >>>= 5").equals("0");

		section("Assign this/class/super");
		code_v2_("class A { m() { this = 12 } }").error(Error.CANT_ASSIGN_VALUE);
		code_v2_("class A { m() { class = 12 } }").error(Error.CANT_ASSIGN_VALUE);
		code_v2_("class B {} class A extends B { m() { super = 12 } }").error(Error.CANT_ASSIGN_VALUE);

		section("Inheritance");
		code_v2_("class A { x = 10 } class B extends A {} var a = new B() return a.x").equals("10");
		code_v2_("class A { m() { return 'ok' } } class B extends A { m() { return super.m() } } var a = new B() return a.m()").equals("\"ok\"");
		code_v2_("class A { x = 10 } class B extends A {} class C extends B {} var a = new C() return a.x").equals("10");
		code_v2_("class A { m() { return 'ok' } } class B extends A {} class C extends B {} var a = new C() return a.m()").equals("\"ok\"");
		code_v2_("class A { m() { return 'ok' } } class B extends A { m() { return super.m() }} class C extends B { m() { return super.m() } } var a = new C() return a.m()").equals("\"ok\"");
		code_v2_("class A { m() { return 'ok' } } class B extends A {} class C extends B { m() { return super.m() } } var a = new C() return a.m()").equals("\"ok\"");
		code_v2_("class A { m() { return 'okA' } } class B extends A { m() { return super.m() + 'B' }} class C extends B { m() { return super.m() + 'C' } } var a = new C()return a.m()").equals("\"okABC\"");
		code_v2_("class A { items } class B extends A { constructor() { this.items = [] } } var x = new B() return x").equals("B {items: []}");
		code_v2_("class A { items } class B extends A { constructor() { this.items = [] super() } } var x = new B() return x").equals("B {items: []}");
		code_v2_("class A { m() { return 'parent' } t() { return this.m() } } class B extends A { m() { return 'enfant' } } return new B().t()").equals("\"enfant\"");
		code_v2_("class A { m() { return 'parent' } t() { return m() } } class B extends A { m() { return 'enfant' } } return new B().t()").equals("\"enfant\"");
		code_v2_("class A {	public id; } class W extends A {} class H extends W { constructor(id){ this.id=id } }").equals("null");
		code_v2_("class A { public x(a) { return a } } class B extends A { public x(a, b) { return x(a + b) } } return new B().x(5, 7)").equals("12");

		section("Access levels: fields");
		code_v2_("class A { x = 10 } var a = new A() return a.x").equals("10");
		code_v2_("class A { public x = 10 } var a = new A() return a.x").equals("10");
		code_v2_("class A { protected x = 10 } var a = new A() return a.x").equals("null");
		code_v2_("class A { private x = 10 } var a = new A() return a.x").equals("null");
		code_v2_("class A { private x = 10 m() { return x } } var a = new A() return a.m()").equals("10");
		code_v2_("class A { private x = 10 } class B extends A {} var a = new B() return a.x").equals("null");
		code_v2_("class A { protected x = 10 } class B extends A {} var a = new B() return a.x").equals("null");
		code_v2_("class A { protected x = 10 } class B extends A { m() { return x } } var a = new B() return a.m()").equals("10");
		code_v2_("class A { private x = 10 constructor() { x = 15 } } var a = new A() return a").equals("A {x: 15}");
		code_v2_("class A {	private x; constructor() { this.x = []; } } return new A()").equals("A {x: []}");
		code_v2_("class Parent { private chaine = 'Nawak'; public get_chaine_parent() { return this.chaine; } } class Enfant extends Parent { public get_chaine_enfant() { return this.get_chaine_parent() } } var e = Enfant() return [e.get_chaine_parent(), e.get_chaine_enfant() ]").equals("[\"Nawak\", \"Nawak\"]");
		code_v2_("class Parent { protected chaine = 'Nawak'; public get_chaine_parent() { return this.chaine; } } class Enfant extends Parent { public get_chaine_enfant() { return this.get_chaine_parent() } } var e = Enfant() return [e.get_chaine_parent(), e.get_chaine_enfant() ]").equals("[\"Nawak\", \"Nawak\"]");
		code_v2_("class A { private x; constructor() { this.x = [] push(this.x, 10); } } return new A()").equals("A {x: [10]}");

		code_v2_("class A { private x = 10 m() { return x } } var a = new A() return a.m()").equals("10");
		section("Access levels: static fields");
		code_v2_("class A { static x = 10 } return A.x").equals("10");
		code_v2_("class A { public static x = 10 } return A.x").equals("10");
		code_v2_("class A { protected static x = 10 } return A.x").equals("null");
		code_v2_("class A { private static x = 10 } return A.x").equals("null");
		code_v2_("class A { private static x = 10 static m() { return x } } return A.m()").equals("10");
		code_v2_("class A { private static x = 10 } class B extends A {} return B.x").equals("null");
		code_v2_("class A { protected static x = 10 } class B extends A {} return B.x").equals("null");
		code_v2_("class A { protected static x = 10 } class B extends A { static m() { return x } } return B.m()").equals("10");
		code_v2_("class A { private static x = 10 static m() { return A.x } } return A.m()").equals("10");

		section("Access levels: methods");
		code_v2_("class A { m() { return 10 } } var a = new A() return a.m()").equals("10");
		code_v2_("class A { public m() { return 10 } } var a = new A() return a.m()").equals("10");
		code_v2_("class A { protected m() { return 10 } } var a = new A() return a.m()").equals("null");
		code_v2_("class A { private m() { return 10 } } var a = new A() return a.m()").equals("null");
		code_v2_("class A { public m() { return 10 } } class B extends A {} var a = new B() return a.m()").equals("10");
		code_v2_("class A { protected m() { return 10 } } class B extends A {} var a = new B() return a.m()").equals("null");
		code_v2_("class A { private m() { return 10 } } class B extends A {} var a = new B() return a.m()").equals("null");
		code_v2_("class A { protected m() { return 10 } } class B extends A { m() { return super.m() } } var a = new B() return a.m()").equals("10");

		section("Access levels: constructors");
		code_v2_("class A { constructor() { } } return new A()").equals("A {}");
		code_v2_("class A { public constructor() { } } return new A()").equals("A {}");
		code_v2_("class A { protected constructor() { } } return new A()").error(Error.PROTECTED_CONSTRUCTOR);
		code_v2_("class A { private constructor() { } } return new A()").error(Error.PRIVATE_CONSTRUCTOR);
		code_v2_("class A { public constructor() { } } class B extends A {} return new B()").equals("B {}");
		code_v2_("class A { x protected constructor() { x = 10 } } class B extends A { constructor() { super() } } return new B().x").equals("10");
		code_v2_("class A { x private constructor() { x = 10 } } class B extends A { constructor() { super() } } return new B().x").error(Error.PRIVATE_CONSTRUCTOR);
		code_v2_("class A { private constructor() { } } class B extends A {} return new B()").equals("B {}");
		code_v2_("class A { private constructor() {} static getInstance() { return new A() } } return A.getInstance()").equals("A {}");

		section("Inheritance fields");
		code_v2_("class A { a = 7 } class B extends A { } return new B().a").equals("7");
		code_v2_("class A { a = 7 } class B extends A { } return new B()['a']").equals("7");
		code_v2_("class A { a = 7 } class B extends A { } return new B()['a']++").equals("7");

		section("Inheritance static fields");
		code_v2_("class A { static a = 7 } class B extends A { } return B.a").equals("7");
		code_v2_("class A { static a = 7 } class B extends A { } return B['a']").equals("7");
		code_v2_("class A { static a = 7 } class B extends A { } return B['a']++").equals("7");

		section("Constructor as function");
		code_v2_("class A { x constructor(x) { this.x = x } } var f = A var o = {c: f} return o.c('a')").equals("A {x: \"a\"}");
		code_v2_("class A { x constructor(x) { this.x = x } } var a = [1, 2, 3, 4] return arrayMap(a, A)").equals("[A {x: 1}, A {x: 2}, A {x: 3}, A {x: 4}]");

		section("Access levels: static methods");
		code_v2_("class A { static m() { return 10 } } return A.m()").equals("10");
		code_v2_("class A { public static m() { return 10 } } return A.m()").equals("10");
		code_v2_("class A { protected static m() { return 10 } } return A.m()").error(Error.PROTECTED_STATIC_METHOD);
		code_v2_("class A { private static m() { return 10 } } return A.m()").error(Error.PRIVATE_STATIC_METHOD);
		code_v2_("class A { public static m() { return 10 } } class B extends A {} return B.m()").equals("10");
		code_v2_("class A { protected static m() { return 10 } } class B extends A {} return B.m()").error(Error.PROTECTED_STATIC_METHOD);
		code_v2_("class A { private static m() { return 10 } } class B extends A {} return B.m()").error(Error.PRIVATE_STATIC_METHOD);

		section("Initialization of fields");
		code_v2_("class A { x = [1, 2, 3] } var a = new A() return a.x").equals("[1, 2, 3]");
		code_v2_("class A { x = [1, 2, 3] } var a = new A() push(a.x, 4) var b = new A() return b.x").equals("[1, 2, 3]");
		code_v2_("class B { y = 10 } class A { x = new B() } var a = new A() return a.x").equals("B {y: 10}");
		code_v2_("class B { y = 10 } class A { x = new B() } var a = new A() return a.x.y").equals("10");
		code_v2_("class B { y = 10 } class A { static x = new B() } return A.x").equals("B {y: 10}");

		section("Initialization of static fields");
		code_v2_("class A { public static x = arrayMap([1, 3, 5], function(y) { return y ** 3 }) } return A.x").equals("[1, 27, 125]");
		code_v2_("class A { static x = arrayMap([1, 3, 5], function(y) { return y ** 3 }) } return A.x").equals("[1, 27, 125]");
		code_v2_3("class Map { public static obstacles = toUpper('hello') } return Map.obstacles").equals("\"HELLO\"");
		code_v4_("class Map { public static obstacles = toUpper('hello') } return Map.obstacles").error(Error.VARIABLE_NAME_UNAVAILABLE);

		section("Method is a system method");
		code_v2_("class A { sqrt() { return sqrt(25) } }").equals("null");
		code_v2("return sqrt(25, 12)").error(Error.NONE);
		code_v3_("return sqrt(25, 12)").error(Error.INVALID_PARAMETER_COUNT);
		code_v2_("class A {} return new A().sqrt()").equals("null");
		code_v2_("class A { sqrt() { return sqrt(25) } } return new A().sqrt()").equals("5.0");
		code_v2_("class A { sqrt() { return sqrt(10, 15) } sqrt(x, y) { return sqrt(x + y) } } return new A().sqrt()").equals("5.0");

		section("Static method is a system method");
		code_v2_("class A { static sqrt() { return sqrt(25) } }").equals("null");
		code_v2_("class A {} return new A().sqrt()").equals("null");
		code_v2_("class A { static sqrt() { return sqrt(25) } } return A.sqrt()").equals("5.0");
		code_v2_("class A { static sqrt() { return sqrt(10, 15) } static sqrt(x, y) { return sqrt(x + y) } } return A.sqrt()").equals("5.0");

		section("Method as value");
		code_v2_("class A { x = 12 m() { return x } } var o = new A() var r = [A.m] return r[0](o)").equals("12");
		code_v2_("class A { m() { return 12 } } var o = new A() var r = {x: A.m} return r.x(o)").equals("12");
		code_v2_("class A { m() { return 12 } } var o = new A() var r = [A.m] var m = r[0] return m(o)").equals("12");
		code_v2_("class A { m() { return [1, 2, 3] } } var o = new A() var r = [A.m] var m = r[0] return m(o)").equals("[1, 2, 3]");
		code_v2_("class A { m(x, y) { return x * y } } class B { x = A.m } return new B().x(new A(), 5, 12)").equals("60");
		code_v2_("class A { m(x, y) { return x * y } } var f = A.m return f(new A(), 5, 12)").equals("60");
		code_v2_("class A { m(x, y) { return x * y } } var f = new A().m return f(new A(), 5, 12)").equals("60");
		code_v2_("class A { m(x, y) { return x * y } } var f = A.m return f(new A(), 5)").equals("null");

		section("Assign to method");
		code_v2_("class A { m() {} } A.m = 12").error(Error.CANT_ASSIGN_VALUE);

		section("Static method as value");
		code_v2_("class A { static m() { return 12 } } var r = [A.m] return r[0]()").equals("12");
		code_v2_("class A { static m() { return 12 } } var r = {x: A.m} return r.x()").equals("12");
		code_v2_("class A { static m() { return 12 } } var r = [A.m] var m = r[0] return m()").equals("12");
		code_v2_("class A { static m() { return [1, 2, 3] } } var r = [A.m] var m = r[0] return m()").equals("[1, 2, 3]");
		code_v2_("class a { static method() { return '42' } } class b { toto constructor() { this.toto = a.method } } return new b().toto()").equals("\"42\"");
		code_v2_("class a { static method() { return '42' } } class b { toto constructor() { this.toto = a.method } } var o = new b() return o.toto()").equals("\"42\"");
		code_v2_("class a { static method() { return '42' } } class b { toto constructor() { this.toto = a.method } m() { return this.toto() } } var o = new b() return o.m()").equals("\"42\"");
		code_v2_("class Test { private static method_1() { return 4 } private static method_2() { return 9 } public static array = [1: Test.method_1, 2: Test.method_2] } return [Test.array[1](), Test.array[2]()]").equals("[4, 9]");
		code_v2_3("class Test { private static method_1() { return 4 } private static method_2() { return 9 } public static array = [1: Test.method_1, 2: Test.method_2] } return arrayMap(Test.array, function(x) { return x() })").equals("[1 : 4, 2 : 9]");
		code_v4_("class Test { private static method_1() { return 4 } private static method_2() { return 9 } public static map = [1: Test.method_1, 2: Test.method_2] } return mapMap(Test.map, function(x) { return x() })").equals("[1 : 4, 2 : 9]");
		code_v2_("class A { a a() { return 12 } } return new A().a()").equals("12");
		code_v2_("class A { static m(v) { return v } } var f = A.m return [A.m(5), A['m'](6), f(7)]").equals("[5, 6, 7]");

		section("Return of field");
		code_v2_3("class R { f = [] m(k, r) { return this.f[k] = r } } var x = new R() return x.m(1, 2)").equals("2");
		code_v4_("class R { f = [:] m(k, r) { return this.f[k] = r } } var x = new R() return x.m(1, 2)").equals("2");
		code_v2_3("class R { private f = [] m(k, r) { return this.f[k] = r } } var x = new R() return x.m(1, 'hello')").equals("\"hello\"");
		code_v4_("class R { private f = [:] m(k, r) { return this.f[k] = r } } var x = new R() return x.m(1, 'hello')").equals("\"hello\"");

		section("Constant in static field");
		code_v2_("class A { static bulbsNameChip = ['puny_bulb': PI] } return A.bulbsNameChip").equals("[\"puny_bulb\" : 3.141592653589793]");

		section("Misc");
		code_v2_("class A { static x() {} static m(item) { return x == item } } return A.m(12)").equals("false");
		code_v2_("class A { static x() {} static m(item) { return x == item } } return A.m(A.x)").equals("true");
		code_v2_("class A { static f(x) { x = 2 var g = function() { f(x) } } }").equals("null");
		code_v2_("class A { f(x) { x = 2 var g = function() { f(x) } } }").equals("null");
		code_v2_("class A { f(x) {} constructor(x) { x = 2 var g = function() { f(x) } } }").equals("null");

		section("Base classes");
		code_v3_("return Value").equals("<class Value>");
		code_v3_("return Null").equals("<class Null>");
		code_v3_("return Number").equals("<class Number>");
		code_v3_("return Integer").equals("<class Integer>");
		code_v3_("return Real").equals("<class Real>");
		code_v3_("return String").equals("<class String>");
		code_v3_("return Array").equals("<class Array>");
		code_v3_("return Object").equals("<class Object>");
		code_v3_("return Function").equals("<class Function>");
		code_v3_("return Class").equals("<class Class>");
		code_v3_("return JSON").equals("<class JSON>");
		code_v3_("return System").equals("<class System>");

		code_v2("return Array").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2("class Array {} return Array").equals("<class Array>");

		section(".class");
		code_v2("return null.class").equals("null");
		code_v3_("return null.class").equals("<class Null>");
		code_v2("return true.class").equals("null");
		code_v3_("return true.class").equals("<class Boolean>");
		code_v2("return (12).class").equals("null");
		code_v3_("return (12).class").equals("<class Integer>");
		code_v2("return (12.5).class").equals("null");
		code_v3_("return (12.5).class").equals("<class Real>");
		code_v2("return 'salut'.class").equals("null");
		code_v3_("return 'salut'.class").equals("<class String>");
		code_v2("return [].class").equals("null");
		code_v3_("return [].class").equals("<class Array>");
		code_v2_("return {}.class").equals("<class Object>");
		code_v2("return (function() {}).class").equals("null");
		code_v3_("return (function() {}).class").equals("<class Function>");
		code_v2("class A {} return A.class").equals("null");
		code_v3_("class A {} return A.class").equals("<class Class>");
		code_v2_("class A {} return new A().class").equals("<class A>");

		section("Class.class");
		code_v2_("class A { class }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A { class() {} }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A { } return new A().class").equals("<class A>");
		code_v2("class A { } return A.class").equals("null");
		code_v3_("class A { } return A.class").equals("<class Class>");

		section("Class.super");
		code_v2_("class A { super }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A { super() {} }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A { } class B extends A {} return B.super").equals("<class A>");
		code_v2_("class A { } class B extends A {} return B.super.name").equals("\"A\"");
		code_v2_("class A { } class B extends A {} return new B().class.super.name").equals("\"A\"");
		code_v2("class A { } return A.class.super").equals("null");
		code_v3_("class A { } return A.class.super").equals("<class Value>");

		section("Class.name");
		code_v2_("class A { static name }").error(Error.FIELD_ALREADY_EXISTS);
		code_v2_("class A { static name() {} }").error(Error.NONE);
		code_v2_("class A {} return A.name").equals("\"A\"");
		code_v2_("class A {} return new A().class.name").equals("\"A\"");
		code_v2_("class A { } A.name = 'toto'").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		section("Class.fields");
		code_v2_("class A { static fields }").error(Error.FIELD_ALREADY_EXISTS);
		code_v2_("class A { } return A.fields").equals("[]");
		code_v2_("class A { x y z } return A.fields").equals("[\"x\", \"y\", \"z\"]");
		code_v2_("class A { z y x } return A.fields").equals("[\"z\", \"y\", \"x\"]");
		code_v2_("class A { a b c static d m() {} n() {} o() {} } return A.fields").equals("[\"a\", \"b\", \"c\"]");
		code_v2_("class A { } A.fields = [1, 2]").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		section("Class.staticFields");
		code_v2_("class A { static staticFields }").error(Error.FIELD_ALREADY_EXISTS);
		code_v2_("class A { } return A.staticFields").equals("[]");
		code_v2_("class A { static x static y static z } return A.staticFields").equals("[\"x\", \"y\", \"z\"]");
		code_v2_("class A { static z static y static x } return A.staticFields").equals("[\"z\", \"y\", \"x\"]");
		code_v2_("class A { static a static b c d m() {} n() {} o() {} } return A.staticFields").equals("[\"a\", \"b\"]");
		code_v2_("class A { } A.staticFields = 'toto'").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		section("Class.methods");
		code_v2_("class A { static methods }").error(Error.FIELD_ALREADY_EXISTS);
		code_v2_("class A { a() {} b() {} static c() {} } return A.methods").equals("[\"a\", \"b\"]");
		code_v2_("class A { } A.methods = 'toto'").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		section("Class.staticMethods");
		code_v2_("class A { static staticMethods }").error(Error.FIELD_ALREADY_EXISTS);
		code_v2_("class A { a() {} b() {} static c() {} static d() {} } return A.staticMethods").equals("[\"c\", \"d\"]");
		code_v2_("class A { } A.staticMethods = 'toto'").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		section("Object.string()");
		code_v2_("return string({x: 1, y: 2, z: 3})").equals("\"{x: 1, y: 2, z: 3}\"");
		code_v2_("return string({z: 1, y: 2, x: 3})").equals("\"{z: 1, y: 2, x: 3}\"");
		code_v2_("class A { z = 1 y = 2 x = 3 } return string(new A())").equals("\"A {z: 1, y: 2, x: 3}\"");
		code_v2_("class A { x = 1 y = 2 z = 3 } return string(new A())").equals("\"A {x: 1, y: 2, z: 3}\"");

		/*
		* Operators
		*/
		section("Object.operator !");
		code_v2_("return !{}").equals("true");
		code_v2_("return !{a: 32}").equals("false");

		section("Object.operator | |");
		// code_v11("var a = {a: 32, b: 'toto', c: false}; return |a|").equals("3");

		section("Object.operator in ()");
		// code_v11("return 12 in {x: 5, y: 12}").equals("true");
		// code_v11("return 12 in {x: 5, y: 'yo'}").equals("false");

		section("Object.operator . ()");
		code_v2_("return { v: 12 }.v").equals("12");
		code_v2_("var a = {b: 12, c: 5} return a.b").equals("12");
		code_v2_("var a = {v: 5} return a.v = 12").equals("12");
		code_v2_("var a = {v: 5} a.v = 12 return a").equals("{v: 12}");
		code_v2_("var a = {v: 5} return a.v = 'salut'").equals("\"salut\"");
		code_v2_("var a = {v: 5} a.v = 'salut' return a").equals("{v: \"salut\"}");
		code_v2_("var a = {b: 12} return a.b += 10").equals("22");
		code_v2_("var a = {b: 12} return a.b -= 10").equals("2");
		code_v2_("var a = {b: 12} return a.b *= 10").equals("120");
		code_v2_("var a = {b: 12} return a.b /= 10").almost(1.2);
		code_v2_("var a = {b: 12} return a.b %= 10").equals("2");
		code_v2_("var o = {} o.new_val = 12 return o").equals("{new_val: 12}");
		code_v2_("var o = {a: 'a'} o.b = 'b' return o").equals("{a: \"a\", b: \"b\"}");
		// DISABLED_code("Object.readonly.v = 5").exception(ls::vm::Exception::CANT_MODIFY_READONLY_OBJECT);
		// code_v11("var o = [{}, ''][0] return o.values").equals("<function>");
		code_v2_("var pq = [{p: 22, v: 55}] return pq[0].p").equals("22");
		code_v2_("var pq = [{p: 22, v: 55}] var o = pq[0] return o.v").equals("55");

		section("Object.operator ==");
		code_v2_("class A {} return {} == new A").equals("false");
		code_v2_("class A { }; class B { }; return new A() == new B();").equals("false");
		code_v2_("class A {} return new A == new A").equals("false");
		code_v2_("return {a: 2} == {}").equals("false");
		code_v2_("return {a: 2} == {a: 1}").equals("false");
		code_v2_("return {a: 2} == {b: 2}").equals("false");
		code_v2_("return {a: 2} == {a: 2}").equals("false");

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

		section("Operator instanceof");
		code_v3_("return null instanceof Null").equals("true");
		code_v3_("return true instanceof Boolean").equals("true");
		code_v3_("return false instanceof Boolean").equals("true");
		code_v3_("return 12 instanceof Integer").equals("true");
		code_v3_("return 12 instanceof Real").equals("true");
		code_v3_("return 12.5 instanceof Integer").equals("false");
		code_v3_("return 'hello' instanceof String").equals("true");
		code_v3_("return [] instanceof Array").equals("true");
		code_v3_("return {} instanceof Object").equals("true");
		code_v3_("return Number instanceof Class").equals("true");
		code_v3_("return function() {} instanceof Function").equals("true");

		/*
		* Iteration
		*/
		// code("var s = '' for v in {a: 5, b: 'hello'} { s += v } s").error(ls::Error::Type::VALUE_NOT_ITERABLE, {"{a: 5, b: 'hello'}", env.tmp_object->to_string()}); // TODO .equals("'5hello'");

		/*
		* Methods
		*/
		section("Object.keys()");
		code_v3_("return {}.keys()").equals("[]");
		code_v3_("return {a: 5, b: 'toto', c: true, d: function() { return 5 } }.keys()").equals("[\"a\", \"b\", \"c\", \"d\"]");
		code_v3_("class A { x y z } return new A().keys()").equals("[\"x\", \"y\", \"z\"]");
		code_v3_("class A { z y x } return new A().keys()").equals("[\"z\", \"y\", \"x\"]");
		// code_v2_("return 'x' in {x: 5, y: 'yo'}.keys()").equals("true");
		// code_v2_("return 'x' in {a: 5, y: 'yo'}.keys()").equals("false");

		section("Object.values()");
		code_v3_("return {}.values()").equals("[]");
		code_v3_("return {a: 1}.values()").equals("[1]");
		code_v3_("return {a: 1, b: 1}.values()").equals("[1, 1]");
		code_v3_("return {a: 5, b: 'toto', c: true, d: function() { return 5 } }.values()").equals("[5, \"toto\", true, #Anonymous Function]");
		code_v3_("return {c: 5, a: 'toto', d: true, b: function() { return 5 } }.values()").equals("[5, \"toto\", true, #Anonymous Function]");
		code_v3_("return count({a: 1, b: 1}.values())").equals("2");

		section("Object.isTrue()");
		code_v2_("if ({x: 12}) { return 5 } else { return 12 }").equals("5");
		code_v2_("if ({}) { return 5 } else { return 12 }").equals("12");

		section("Object.clone()");
		code_v2_("var a = {v: 12} return [a]").equals("[{v: 12}]");

		section("Object.map()");
		// code("return {}.map(x -> x + 1)").equals("{}");
		// code("return {x: 12, y: 5}.map(x -> x + 1)").equals("{x: 13, y: 6}");
		// code("return {x: 'a', y: 'b'}.map(x -> x + ' !')").equals("{x: 'a !', y: 'b !'}");
	}
}
