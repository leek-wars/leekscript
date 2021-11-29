package test;

public class TestMap extends TestCommon {

	public void run() {
		header("Map");

		section("Constructor");
		// code("[:]").equals("[:]");
		code("return []").equals("[]");
		code("return [1: 1, 2: 2]").equals("[1 : 1, 2 : 2]");
		code("return [1: 1, 2: 'a']").equals("[1 : 1, 2 : a]");
		code("return ['1': 'a', '1': 'b', '1': 'c']").equals("[1 : c]");

		section("Map::to_bool()");
		// code("![:]").equals("true");
		code("return ![]").equals("true");
		code("return ![2: 2]").equals("false");
		code("if ([2: 2]) { return 12 } else { return 5 }").equals("12");

		section("Array of maps");
		code("return [[], [1: 1], [1: 2]]").equals("[[], [1 : 1], [1 : 2]]");
		// code("return [[:], [1: 1], [1: 2]]").equals("[[:], [1: 1], [1: 2]]");
		code("var m = ['a': 'b'] return [m]").equals("[[a : b]]");

		/*
		* Operators
		*/
		var maps = new String[] {"[5: 5]", "[5: 9.99]", "[5: 'abc']", "[9.99: 5]", "[9.99: 9.99]", "[9.99: 'abc']", "['abc': 5]", "['abc': 9.99]", "['abc': 'abc']"};

		section("Map.operator ==");
		code("return ['a': 'b'] == [1: 1]").equals("false");
		code("return ['a': 'b'] == ['a': 'b']").equals("true");
		code("return ['a': 'b'] == ['a': 'b', 'c': 'd']").equals("false");
		// code("var x = ['a' : 'b'] var y = [1 : 1] return x.clear() == y.clear()").equals("true");
		for (var m1 : maps)
			for (var m2 : maps)
				code("return " + m1 + " == " + m2).equals(m1 == m2 ? "true" : "false");
		code("return [12.5: 9.99] == 'hello'").equals("false");

		// section("Map.operator <");
		// code("return ['a': 1, 'b': 2] < ['a': 1, 'b': 3]").equals("true");
		// code("return [1: 1, 2: 0] < [1: 1, 2: true]").equals("false");
		// code("return [1: 1, 2: 0.5] < [1: 1, 2: true]").equals("false");
		// code("return [1: 1, 2: 'a'] < [1: 1.5, 2: 5.5]").equals("true");
		// for (var i = 0; i < maps.length; ++i)
		// 	for (var j = 0; j < maps.length; ++j)
		// 		code("return " + maps[i] + " < " + maps[j]).equals(i < j ? "true" : "false");

		// section("Map.operator in");
		// code("var m = ['salut': 12] return 'salut' in m").equals("true");
		// code("var m = ['salut': 12] return 'salum' in m").equals("false");
		// code("var m = ['salut': 12] return 12 in m.values()").equals("true");

		section("Map.operator []");
		code("var m = [1: 1] return m[1]").equals("1");
		code("var m = ['a': 'b'] return m['a']").equals("b");
		code("var m = [5: 12] return m[5]").equals("12");
		code_v1("var m = [5: 12.5] return m[5]").equals("12,5");
		code_v2_("var m = [5: 12.5] return m[5]").equals("12.5");
		code("var m = [5.5: 12] return m[5.5]").equals("12");
		code_v1("var m = [5.5: 12.5] return m[5.5]").equals("12,5");
		code_v2_("var m = [5.5: 12.5] return m[5.5]").equals("12.5");
		code("var m = ['salut': 12] return m['salut']").equals("12");
		code_v1("var m = ['salut': 12.5] return m['salut']").equals("12,5");
		code_v2_("var m = ['salut': 12.5] return m['salut']").equals("12.5");
		code("var m = ['salut': 'yolo'] return m['salut']").equals("yolo");
		code("var m = ['a': 'b'] m['a'] = 'c' return m").equals("[a : c]");
		code("var m = ['salut': 12] m['salut'] = 13 return m['salut']").equals("13");
		code("var m = ['salut': 'yo'] m['salut'] = 'ya' return m['salut']").equals("ya");
		code("var m = [5: 12] return m[5.7]").equals("12");
		// code("var m = [5: 12] m['salut']").error(ls::Error::INVALID_MAP_KEY, {"'salut'", "m", env.tmp_string->to_string()});
		// code("var m = [5.7: 'hello'] m['salut']").error(ls::Error::INVALID_MAP_KEY, {"'salut'", "m", env.tmp_string->to_string()});
		code("var m = [1: 'a', 2: 'b'] m[2] = 'B' return m").equals("[1 : a, 2 : B]");
		// code("var m = [1: 'a', 2: 'b'] m[3]").exception(ls::vm::Exception::ARRAY_OUT_OF_BOUNDS);
		code("var m = [1: 2, 3: 4] m[5] = 6 return m").equals("[1 : 2, 3 : 4, 5 : 6]");
		code("var m = ['a': 2, 'b': 4] m['c'] = 6 return m").equals("[a : 2, b : 4, c : 6]");
		code_v1("var m = ['a': 2.5, 'b': 4.8] m['c'] = 6.9 return m").equals("[a : 2,5, b : 4,8, c : 6,9]");
		code_v2_("var m = ['a': 2.5, 'b': 4.8] m['c'] = 6.9 return m").equals("[a : 2.5, b : 4.8, c : 6.9]");
		code("var m = [1: 'a', 2: 'b'] m[3] = 'c' return m").equals("[1 : a, 2 : b, 3 : c]");
		code("var m = ['a': '2', 'b': '4'] m['c'] = '6' return m").equals("[a : 2, b : 4, c : 6]");
		code("var m = [1: 2, 3: 4] m[3] = 6 return m").equals("[1 : 2, 3 : 6]");
		code_v1("var m = [1: 2.5, 3: 4.5] m[3] = 6.5 return m").equals("[1 : 2,5, 3 : 6,5]");
		code_v2_("var m = [1: 2.5, 3: 4.5] m[3] = 6.5 return m").equals("[1 : 2.5, 3 : 6.5]");
		// code_v1("var m = [1.5: 2, 3.5: 4] m[3.5] = 6 return m").equals("[1,5 : 2, 3,5 : 6]");
		// code_v2_("var m = [1.5: 2, 3.5: 4] m[3.5] = 6 return m").equals("[1.5 : 2, 3.5 : 6]");
		// code_v1("var m = [1.5: 2.5, 3.5: 4.5] m[3.5] = 6.5 return m").equals("[1,5 : 2,5, 3,5 : 6,5]");
		// code_v2_("var m = [1.5: 2.5, 3.5: 4.5] m[3.5] = 6.5 return m").equals("[1.5 : 2.5, 3.5 : 6.5]");
		code("var m = ['1': 2, '3': 4] m['3'] = 6 return m").equals("[1 : 2, 3 : 6]");
		// code_v1("var m = [1.5: 'a', 2.5: 'b'] m[2.5] = 'c' return m").equals("[1,5 : a, 2,5 : c]");
		// code_v2_("var m = [1.5: 'a', 2.5: 'b'] m[2.5] = 'c' return m").equals("[1.5 : a, 2.5 : c]");
		code("return ['', [1: 2][1]]").equals("[, 2]");
		code_v1("return ['', [1: 2.5][1]]").equals("[, 2,5]");
		code_v2_("return ['', [1: 2.5][1]]").equals("[, 2.5]");
		code("var m = [] var ns = '01234566' return m[ns] = 1").equals("1");

		section("Map.operator [] left-value");
		code("var m = [1: 2] m[1]++ return m").equals("[1 : 3]");
		code("var m = ['a': 2] m['a']++ return m").equals("[a : 3]");
		code("var k = ['a', 12][0] var m = ['a': 2] m[k]++ return m").equals("[a : 3]");

		// section("Map.operator [] on unknown maps");
		// code("var m = ptr(['a': '2']) m['c'] = '6' return m").equals("['a': '2', 'c': '6']");
		// code("var m = ptr([2: 'a']) m[3] = 'b' return m").equals("[2: 'a', 3: 'b']");
		// code("var m = ptr([2.5: 'a']) m[3.6] = 'b' return m").equals("[2.5: 'a', 3.6: 'b']");
		// code("var m = ptr(['a': 2]) m['c'] = 6 m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr(['a': 2.2]) m['c'] = 6.6 m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr([2: 2]) m[3] = 6 m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr([2.5: 2]) m[3.5] = 6 m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr([2.5: 2.8]) m[3.5] = 6 m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr([2: 2.8]) m[3] = 6 m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr([2: 'a']) m['toto'] = 'b' m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var m = ptr([2.5: 'a']) m['toto'] = 'b' m").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		/*
		* Iteration
		*/
		// section("Map iteration");
		// code("for k, v in [:] { System.print(k + ' ' + v) }").output("");
		// code("for k, v in [1:2] { System.print(k + ' ' + v) }").output("1 2\n");
		// code("for k, v in [1:2,3:4] { System.print(k + ' ' + v) }").output("1 2\n3 4\n");
		// code("for k, v in [1:2,3:4,5:6] { System.print(k + ' ' + v) }").output("1 2\n3 4\n5 6\n");
		// code("for k, v in ['a':'b'] { System.print(k + ' ' + v) }").output("a b\n");
		// code("for k, v in ['a':'b','c':'d'] { System.print(k + ' ' + v) }").output("a b\nc d\n");
		// code("for k, v in ['a':'b','c':'d','e':'f'] { System.print(k + ' ' + v) }").output("a b\nc d\ne f\n");
	}
}
