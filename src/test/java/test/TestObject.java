package test;

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
		code_v11("class A { a = 10 } var a = new A(); return --a.a").equals("9");
		code_v11("class A { a = 10 } var a = new A(); a.a-- return a.a").equals("9");
		code_v11("class A { a = 10 } var a = new A(); return ++a.a").equals("11");
		code_v11("class A { a = 10 } var a = new A(); a.a++ return a.a").equals("11");
		code_v11("class A { a = 10 } var a = [new A()]; a[0].a++ return a[0].a").equals("11");
		code_v11("class A { a = 10 } var a = [new A()]; a[0].a-- return a[0].a").equals("9");
		code_v11("class A { a = 10 } var a = [new A()]; ++a[0].a return a[0].a").equals("11");
		code_v11("class A { a = 10 } var a = [new A()]; --a[0].a return a[0].a").equals("9");

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