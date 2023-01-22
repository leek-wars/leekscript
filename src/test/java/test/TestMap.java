package test;

import leekscript.common.Error;

public class TestMap extends TestCommon {

	public void run() {
		header("Map");

		section("Constructor");
		code_v4_("return [:]").equals("[:]");
		code("return [1: 1, 2: 2]").equals("[1 : 1, 2 : 2]");
		code("return [1: 1, 2: 'a']").equals("[1 : 1, 2 : \"a\"]");
		code_v4_("var m = new Map() m[1] = 2 return m").equals("[1 : 2]");

		section("Map::to_bool()");
		// code("![:]").equals("true");
		code("return ![]").equals("true");
		code("return ![2: 2]").equals("false");
		code("if ([2: 2]) { return 12 } else { return 5 }").equals("12");

		section("Array of maps");
		code("return [[], [1: 1], [1: 2]]").equals("[[], [1 : 1], [1 : 2]]");
		// code("return [[:], [1: 1], [1: 2]]").equals("[[:], [1: 1], [1: 2]]");
		code("var m = ['a': 'b'] return [m]").equals("[[\"a\" : \"b\"]]");

		section("Infinite maps");
		code_v1("var a = [:] a[0] = a return a").equals("[[]]");
		code_v2_3("var a = [:] a[0] = a return a").equals("[<...>]");
		code_v4_("var a = [:] a[0] = a return a").equals("[0 : [0 : <...>]]");
		code_v1("var a = [:] a[a] = 1 return a").equals("[1]");
		code_v2_3("var a = [:] a[a] = 1 return a").equals("[1]");
		code_v4_("var a = [:] a[a] = 1 return a").equals("[[<...> : 1] : 1]");
		code_v1_3("var a = [3 : 4] a[a] = 2 return a").equals("[3 : 4, 1 : 2]");
		code_v1("var a = [:] a[a] = a return a").equals("[[]]");
		code_v2_3("var a = [:] a[a] = a return a").equals("[<...>]");
		code_v1("var a = [:] a[0] = [a] return a").equals("[[[]]]");
		code_v2_3("var a = [:] a[0] = [a] return a").equals("[[<...>]]");
		code_v4_("var a = [:] a[0] = [a] return a").equals("[0 : [[0 : <...>]]]");

		section("Map duplicated key");
		code_v1_3("return [1 : 2, 1 : 3]").warning(Error.MAP_DUPLICATED_KEY);
		code_v4_("return [1 : 2, 1 : 3]").error(Error.MAP_DUPLICATED_KEY);
		code_v1_3("return ['a' : 2, 'a' : 3]").warning(Error.MAP_DUPLICATED_KEY);
		code_v4_("return ['a' : 2, 'a' : 3]").error(Error.MAP_DUPLICATED_KEY);
		code_v1_3("return [true : 2, true : 3]").warning(Error.MAP_DUPLICATED_KEY);
		code_v4_("return [true : 2, true : 3]").error(Error.MAP_DUPLICATED_KEY);

		/*
		* Operators
		*/
		var maps = new String[] {"[5: 5]", "[5: 9.99]", "[5: 'abc']", "[9.99: 5]", "[9.99: 9.99]", "[9.99: 'abc']", "['abc': 5]", "['abc': 9.99]", "['abc': 'abc']"};

		section("Map.operator ==");
		code("return ['a': 'b'] == [1: 1]").equals("false");
		code("return ['a': 'b'] == ['a': 'b']").equals("true");
		code("return [1: ['a': 'b']] == [1: ['a': 'b']]").equals("true");
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
		code("var m = ['a': 'b'] return m['a']").equals("\"b\"");
		code("var m = [5: 12] return m[5]").equals("12");
		code_v1("var m = [5: 12.5] return m[5]").equals("12,5");
		code_v2_("var m = [5: 12.5] return m[5]").equals("12.5");
		code("var m = [5.5: 12] return m[5.5]").equals("12");
		code_v1("var m = [5.5: 12.5] return m[5.5]").equals("12,5");
		code_v2_("var m = [5.5: 12.5] return m[5.5]").equals("12.5");
		code("var m = ['salut': 12] return m['salut']").equals("12");
		code_v1("var m = ['salut': 12.5] return m['salut']").equals("12,5");
		code_v2_("var m = ['salut': 12.5] return m['salut']").equals("12.5");
		code("var m = ['salut': 'yolo'] return m['salut']").equals("\"yolo\"");
		code("var m = ['a': 'b'] m['a'] = 'c' return m").equals("[\"a\" : \"c\"]");
		code("var m = ['salut': 12] m['salut'] = 13 return m['salut']").equals("13");
		code("var m = ['salut': 'yo'] m['salut'] = 'ya' return m['salut']").equals("\"ya\"");
		code_v1_3("var m = [5: 12] return m[5.7]").equals("12");
		code_v4_("var m = [5: 12] return m[5.7]").equals("null");
		// code("var m = [5: 12] m['salut']").error(ls::Error::INVALID_MAP_KEY, {"'salut'", "m", env.tmp_string->to_string()});
		// code("var m = [5.7: 'hello'] m['salut']").error(ls::Error::INVALID_MAP_KEY, {"'salut'", "m", env.tmp_string->to_string()});
		code("var m = [1: 'a', 2: 'b'] m[2] = 'B' return m").equals("[1 : \"a\", 2 : \"B\"]");
		code("var m = [1: 2, 3: 4] m[5] = 6 return m").equals("[1 : 2, 3 : 4, 5 : 6]");
		code("var m = ['a': 2, 'b': 4] m['c'] = 6 return m").equals("[\"a\" : 2, \"b\" : 4, \"c\" : 6]");
		code_v1("var m = ['a': 2.5, 'b': 4.8] m['c'] = 6.9 return m").equals("[\"a\" : 2,5, \"b\" : 4,8, \"c\" : 6,9]");
		code_v2_("var m = ['a': 2.5, 'b': 4.8] m['c'] = 6.9 return m").equals("[\"a\" : 2.5, \"b\" : 4.8, \"c\" : 6.9]");
		code("var m = [1: 'a', 2: 'b'] m[3] = 'c' return m").equals("[1 : \"a\", 2 : \"b\", 3 : \"c\"]");
		code("var m = ['a': '2', 'b': '4'] m['c'] = '6' return m").equals("[\"a\" : \"2\", \"b\" : \"4\", \"c\" : \"6\"]");
		code("var m = [1: 2, 3: 4] m[3] = 6 return m").equals("[1 : 2, 3 : 6]");
		code_v1("var m = [1: 2.5, 3: 4.5] m[3] = 6.5 return m").equals("[1 : 2,5, 3 : 6,5]");
		code_v2_("var m = [1: 2.5, 3: 4.5] m[3] = 6.5 return m").equals("[1 : 2.5, 3 : 6.5]");
		code_v4_("var m = [1.5: 2, 3.5: 4] m[3.5] = 6 return m").equals("[1.5 : 2, 3.5 : 6]");
		code_v4_("var m = [1.5: 2.5, 3.5: 4.5] m[3.5] = 6.5 return m").equals("[1.5 : 2.5, 3.5 : 6.5]");
		code("var m = ['1': 2, '3': 4] m['3'] = 6 return m").equals("[\"1\" : 2, \"3\" : 6]");
		code_v4_("var m = [1.5: 'a', 2.5: 'b'] m[2.5] = 'c' return m").equals("[2.5 : \"c\", 1.5 : \"a\"]");
		code("return ['', [1: 2][1]]").equals("[\"\", 2]");
		code_v1("return ['', [1: 2.5][1]]").equals("[\"\", 2,5]");
		code_v2_("return ['', [1: 2.5][1]]").equals("[\"\", 2.5]");
		code_v1_3("var m = [] var ns = '01234566' return m[ns] = 1").equals("1");
		code_v4_("var m = [] var ns = '01234566' return m[ns] = 1").equals("null");
		code("var a = [12: 5] return a[5] = 7").equals("7");
		code("var a = [12: 5] var b = 7 return a[5] = b").equals("7");

		section("Map.operator [] left-value");
		code("var m = [1: 2] m[1]++ return m").equals("[1 : 3]");
		code("var m = ['a': 2] m['a']++ return m").equals("[\"a\" : 3]");
		code("var k = ['a', 12][0] var m = ['a': 2] m[k]++ return m").equals("[\"a\" : 3]");

		section("Operators on map element");
		code_v2_("var m = [1: 10] return --m[1]").equals("9");
		code_v2_("var m = [1: 10] m[1]-- return m[1]").equals("9");
		code_v2_("var m = [1: 10] return ++m[1]").equals("11");
		code_v2_("var m = [1: 10] m[1]++ return m[1]").equals("11");
		code_v2_("var m = [1: 10] return m[1] += 5").equals("15");
		code_v2_("var m = [1: 10] return m[1] -= 5").equals("5");
		code_v2_("var m = [1: 10] return m[1] *= 5").equals("50");
		code_v2_("var m = [1: 10] return m[1] /= 5").equals("2.0");
		code_v2_("var m = [1: 10] return m[1] \\= 3").equals("3");
		code_v2_("var m = [1: 10] return m[1] %= 5").equals("0");
		code_v2_("var m = [1: 10] return m[1] **= 5").equals("100000");
		code_v2_("var m = [1: 10] return m[1] |= 5").equals("15");
		code_v2_("var m = [1: 10] return m[1] &= 5").equals("0");
		code_v2_("var m = [1: 10] return m[1] ^= 5").equals("15");
		code_v2_("var m = [1: 10] return m[1] <<= 5").equals("320");
		code_v2_("var m = [1: 10] return m[1] >>= 5").equals("0");
		code_v2_("var m = [1: 10] return m[1] >>>= 5").equals("0");

		section("Map.operator +");
		code_v4_("return [:] + [:]").equals("[:]");
		code_v4_("return [1 : 2] + [3 : 4]").equals("[1 : 2, 3 : 4]");
		code_v4_("return [1 : 2] + [1 : 4]").equals("[1 : 2]");
		code_v4_("return ['a' : 2] + ['a' : 4]").equals("[\"a\" : 2]");
		code_v4_("return [:] + [1 : 2, 3 : 4]").equals("[1 : 2, 3 : 4]");
		code_v4_("return [1 : 2, 3 : 4] + [:]").equals("[1 : 2, 3 : 4]");

		section("Map.operator +=");
		code_v4_("var a = [:] a += [:] return a").equals("[:]");
		code_v4_("var a = [1 : 2] a += [3 : 4] return a").equals("[1 : 2, 3 : 4]");
		code_v4_("var a = [1 : 2] a += [1 : 4] return a").equals("[1 : 2]");
		code_v4_("var a = ['a' : 2] a += ['a' : 4] return a").equals("[\"a\" : 2]");
		code_v4_("var a = [:] a += [1 : 2, 3 : 4] return a").equals("[1 : 2, 3 : 4]");
		code_v4_("var a = [1 : 2, 3 : 4] a += [:] return a").equals("[1 : 2, 3 : 4]");

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

		section("Map.instanceof");
		code_v4_("return [:] instanceof Map").equals("true");

		/**
		 * Clone
		 */
		section("Map clone()");
		code("var a = [1 : 'a', 2 : 'b', 3 : 'c'] var b = clone(a) b[4] = 'd' return [a, b]").equals("[[1 : \"a\", 2 : \"b\", 3 : \"c\"], [1 : \"a\", 2 : \"b\", 3 : \"c\", 4 : \"d\"]]");

		/**
		 * JSON
		 */
		section("Map JSON");
		code_v4_("return jsonEncode([:])").equals("\"{}\"");
		code_v4_("return jsonEncode([1 : 2])").equals("\"{\"1\":2}\"");
		code_v4_("return jsonEncode([1 : 2, 3 : 4, 5 : 6])").equals("\"{\"1\":2,\"3\":4,\"5\":6}\"");
		code_v4_("return jsonEncode(['a' : 'b', 'c' : 'd', 'e' : 'f'])").equals("\"{\"a\":\"b\",\"c\":\"d\",\"e\":\"f\"}\"");
		code_v4_("var a = [1, 2, 3] return jsonEncode([1 : a, 2 : 3])").equals("\"{\"1\":[1,2,3],\"2\":3}\"");
		code_v4_("var a = [1] return jsonEncode([1 : a, 2 : a])").equals("\"{\"1\":[1]}\"");
		code_v4_("var a = [:] a[a] = a return jsonEncode(a)").equals("\"{}\"");

		/*
		* Iteration
		*/
		section("Map iteration");
		code("var s = '' for (var v in [:]) { s += v } return s").equals("\"\"");
		code("var s = '' for (var v in [1:2]) { s += v } return s").equals("\"2\"");
		code("var s = '' for (var v in [1:2,3:4]) { s += v } return s").equals("\"24\"");
		code("var s = '' for (var v in [1:2,3:4,5:6]) { s += v } return s").equals("\"246\"");
		code("var s = '' for (var v in ['a':'b']) { s += v } return s").equals("\"b\"");
		code("var s = '' for (var v in ['a':'b','c':'d']) { s += v } return s").equals("\"bd\"");
		code("var s = '' for (var k : var v in ['a':'b','c':'d','e':'f']) { s += v } return s").equals("\"bdf\"");
		code("var s = '' for (var k : var v in [:]) { s += (k + ' ' + v) } return s").equals("\"\"");
		code("var s = '' for (var k : var v in [1:2]) { s += (k + ' ' + v) } return s").equals("\"1 2\"");
		code("var s = '' for (var k : var v in [1:2,3:4]) { s += (k + ' ' + v) } return s").equals("\"1 23 4\"");
		code("var s = '' for (var k : var v in [1:2,3:4,5:6]) { s += (k + ' ' + v) } return s").equals("\"1 23 45 6\"");
		code("var s = '' for (var k : var v in ['a':'b']) { s += (k + ' ' + v) } return s").equals("\"a b\"");
		code("var s = '' for (var k : var v in ['a':'b','c':'d']) { s += (k + ' ' + v) } return s").equals("\"a bc d\"");
		code("var s = '' for (var k : var v in ['a':'b','c':'d','e':'f']) { s += (k + ' ' + v) } return s").equals("\"a bc de f\"");

		/**
		 * Méthodes
		 */
		section("Map.isEmpty");
		code_v1_3("return isEmpty([2 : 8])").equals("false");
		code_v1_3("return mapIsEmpty([2 : 8])").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("return mapIsEmpty([2 : 8])").equals("false");
		code_v4_("return mapIsEmpty([:])").equals("true");

		section("Map.clear");
		code_v1_3("var m = [:] mapClear(m) return m").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var m = [:] mapClear(m) return m").equals("[:]");
		code_v4_("var m = [1 : 2, 3 : 4] mapClear(m) return m").equals("[:]");
		code_v4_("var m = ['a' : 'b', 'c' : 'd'] mapClear(m) return m").equals("[:]");

		section("Map.get");
		code_v1_3("var m = [:] return mapGet(m, 2)").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var m = [:] return mapGet(m, 2)").equals("null");
		code_v4_("var m = [:] return mapGet(m, 2, 'default')").equals("\"default\"");
		code_v4_("var m = [1 : 2, 3 : 4] return mapGet(m, 5)").equals("null");
		code_v4_("var m = [1 : 2, 3 : 4] return mapGet(m, 3)").equals("4");
		code_v4_("var m = ['a' : 'b', 'c' : 'd'] return mapGet(m, 'c')").equals("\"d\"");
		code_v4_("var m = ['a' : 'b', 'c' : 'd'] return mapGet(m, 'c', 'default')").equals("\"d\"");
		code_v4_("var m = ['a' : 'b', 'c' : 'd'] return mapGet(m, 'b', 'default')").equals("\"default\"");

		section("Map.put");
		code_v1_3("var m = [:] mapPut(m, 2, 3) return m").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var m = [:] mapPut(m, 2, 3) return m").equals("[2 : 3]");
		code_v4_("var m = [1 : 2, 3 : 4] mapPut(m, 5, 6) return m").equals("[1 : 2, 3 : 4, 5 : 6]");
		code_v4_("var m = [1 : 2, 3 : 4] mapPut(m, 3, 6) return m").equals("[1 : 2, 3 : 6]");
		code_v4_("var m = ['a' : 'b', 'c' : 'd'] mapPut(m, 'e', 'f') return m").equals("[\"a\" : \"b\", \"c\" : \"d\", \"e\" : \"f\"]");

		section("Map.keys");
		code_v4_("return mapKeys([1: 2, 3: 4, 5: 6])").equals("[1, 3, 5]");
		code_v4_("return mapKeys(['1': 2, '3': 4, '5': 6])").equals("[\"1\", \"3\", \"5\"]");

		section("Map.values");
		code_v4_("return mapValues([1: 2, 3: 4, 5: 6])").equals("[2, 4, 6]");

		section("mapMap");
		code_v2_3("return arrayMap(['a': 1, 'b': 2], function(k, v) { return k + v })").equals("[\"a\" : \"a1\", \"b\" : \"b2\"]");
		code_v4_("return mapMap(['a': 1, 'b': 2], function(v) { return v * 10 })").equals("[\"a\" : 10, \"b\" : 20]");
		code_v4_("return mapMap(['a': 1, 'b': 2], function(v, k) { return k + v })").equals("[\"a\" : \"a1\", \"b\" : \"b2\"]");
		code_v2_3("return function() { var t = ['a': 1, 'b': 2]; arrayMap(t, function(k, v) { v = 'tomate'; k = 'ctus'; return 3; }); return t; }();").equals("[\"a\" : 1, \"b\" : 2]");
		code_v4_("return function() { var t = ['a': 1, 'b': 2]; mapMap(t, function(v, k) { v = 'tomate'; k = 'ctus'; return 3; }); return t; }();").equals("[\"a\" : 1, \"b\" : 2]");
		code_v4_("return mapMap(['a': 1, 'b': 2], function(v, k, a, b) { return v * 10 })").equals("[\"a\" : 10, \"b\" : 20]");
		code_v4_("return mapMap(['a': 1, 'b': 2], function() { return 10 })").equals("[\"a\" : 10, \"b\" : 10]");

		section("mapRemove()");
		code_v1_3("var a = ['a':'va','b':'vb','c':'vc','d':'vd']; removeKey(a,'a'); return a").equals("[\"b\" : \"vb\", \"c\" : \"vc\", \"d\" : \"vd\"]");
		code_v4_("removeKey([1, 2, 3])").error(Error.REMOVED_FUNCTION_REPLACEMENT);
		code_v4_("var a = ['a':'va','b':'vb','c':'vc','d':'vd']; mapRemove(a,'a'); return a").equals("[\"b\" : \"vb\", \"c\" : \"vc\", \"d\" : \"vd\"]");

		section("Map.removeAll()");
		code_v1_3("return mapRemoveAll([2 : 8], 8)").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [:] mapRemoveAll(a, 2) return a").equals("[:]");
		code_v4_("var a = [1 : 2] mapRemoveAll(a, 2) return a").equals("[:]");
		code_v4_("var a = [1 : 2, 2 : 2, 3 : 10] mapRemoveAll(a, 55) return a").equals("[1 : 2, 2 : 2, 3 : 10]");
		code_v4_("var a = [1 : 2, 2 : 2, 3 : 10, 4 : 2] mapRemoveAll(a, 2) return a").equals("[3 : 10]");
		code_v4_("var a = [1 : null, 2 : 2, 3 : null] mapRemoveAll(a, null) return a").equals("[2 : 2]");

		section("Map.replace()");
		code_v1_3("return mapReplace([2 : 8], 2, 10)").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [:] mapReplace(a, 2, 10) return a").equals("[:]");
		code_v4_("var a = [2 : 8] mapReplace(a, 2, 10) return a").equals("[2 : 10]");
		code_v4_("var a = [2 : 8] var b = [2 : 10] mapReplace(a, 5, 10) return a").equals("[2 : 8]");

		section("Map.replaceAll()");
		code_v1_3("return mapReplaceAll([2 : 8], [:])").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [2 : 8] var b = [2 : 10] mapReplaceAll(a, b) return a").equals("[2 : 10]");
		code_v4_("var a = [2 : 8] var b = [5 : 10] mapReplaceAll(a, b) return a").equals("[2 : 8]");
		code_v4_("var a = [:] var b = [5 : 10] mapReplaceAll(a, b) return a").equals("[:]");
		code_v4_("var a = ['a' : 'b'] var b = [:] mapReplaceAll(a, b) return a").equals("[\"a\" : \"b\"]");
		code_v4_("var a = ['a' : 'b', 'c' : 'd'] var b = ['a' : 10, 'c' : 20] mapReplaceAll(a, b) return a").equals("[\"a\" : 10, \"c\" : 20]");

		section("Map.fill()");
		code_v1_3("return mapFill([2 : 8], 5)").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [:] mapFill(a, 5) return a").equals("[:]");
		code_v4_("var a = [2 : 8] mapFill(a, 5) return a").equals("[2 : 5]");
		code_v4_("var a = [2 : 8, 4 : 5, 3: 1] mapFill(a, 'a') return a").equals("[2 : \"a\", 3 : \"a\", 4 : \"a\"]");

		section("assocSort() legacy");
		code_v1_3("var t = [2:0,1:1,0:2]; return arraySort(t,function(k1, v1, k2, v2){return (k1>k2)?(-1):(k1<k2)?1:0;})").equals("[2 : 0, 1 : 1, 0 : 2]");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; assocSort(a); return a").equals("[\"a\" : \"va\", \"b\" : \"vb\", \"c\" : \"vc\", \"d\" : \"vd\"]");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; assocSort(a, SORT_DESC); return a").equals("[\"d\" : \"vd\", \"c\" : \"vc\", \"b\" : \"vb\", \"a\" : \"va\"]");
		code_v1_3("var a = [8,6,2,3,7,1,0]; assocSort(a); return a").equals("[6 : 0, 5 : 1, 2 : 2, 3 : 3, 1 : 6, 4 : 7, 0 : 8]");
		code_v1("var a = [0, 1, 1, 1, 2, 2, 2, 2, 2, null, 3, 3, 3, 3, 3, 3, 3, 3, 3, null, 4, 4, 4, null, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, null, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, null, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6] assocSort(a) return a").equals("[0 : 0, 1 : 1, 2 : 1, 3 : 1, 4 : 2, 5 : 2, 6 : 2, 7 : 2, 8 : 2, 10 : 3, 11 : 3, 12 : 3, 13 : 3, 14 : 3, 15 : 3, 16 : 3, 17 : 3, 18 : 3, 20 : 4, 21 : 4, 22 : 4, 24 : 4, 25 : 4, 26 : 4, 27 : 4, 28 : 4, 29 : 4, 30 : 4, 31 : 4, 32 : 4, 33 : 5, 34 : 5, 35 : 5, 37 : 5, 38 : 5, 39 : 5, 40 : 5, 41 : 5, 42 : 5, 43 : 5, 44 : 5, 45 : 5, 46 : 5, 47 : 5, 48 : 5, 49 : 5, 51 : 6, 52 : 6, 53 : 6, 54 : 6, 55 : 6, 56 : 6, 57 : 6, 58 : 6, 59 : 6, 60 : 6, 61 : 6, 62 : 6, 63 : 6, 64 : 6, 65 : 6, 66 : 6, 67 : 6, 68 : 6, 69 : 6, 70 : 6, 9 : null, 19 : null, 23 : null, 36 : null, 50 : null]");
		code_v2_3("var a = [0, 1, 1, 1, 2, 2, 2, 2, 2, null, 3, 3, 3, 3, 3, 3, 3, 3, 3, null, 4, 4, 4, null, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, null, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, null, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6] assocSort(a) return a").equals("[9 : null, 19 : null, 23 : null, 36 : null, 50 : null, 0 : 0, 1 : 1, 2 : 1, 3 : 1, 4 : 2, 5 : 2, 6 : 2, 7 : 2, 8 : 2, 10 : 3, 11 : 3, 12 : 3, 13 : 3, 14 : 3, 15 : 3, 16 : 3, 17 : 3, 18 : 3, 20 : 4, 21 : 4, 22 : 4, 24 : 4, 25 : 4, 26 : 4, 27 : 4, 28 : 4, 29 : 4, 30 : 4, 31 : 4, 32 : 4, 33 : 5, 34 : 5, 35 : 5, 37 : 5, 38 : 5, 39 : 5, 40 : 5, 41 : 5, 42 : 5, 43 : 5, 44 : 5, 45 : 5, 46 : 5, 47 : 5, 48 : 5, 49 : 5, 51 : 6, 52 : 6, 53 : 6, 54 : 6, 55 : 6, 56 : 6, 57 : 6, 58 : 6, 59 : 6, 60 : 6, 61 : 6, 62 : 6, 63 : 6, 64 : 6, 65 : 6, 66 : 6, 67 : 6, 68 : 6, 69 : 6, 70 : 6]");
		code_v1("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a) return a").equals("[162 : 0, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 128 : 2, 197 : 2, 161 : 2, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6, 181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null]");
		code_v2_3("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a) return a").equals("[181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null, 162 : 0, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 128 : 2, 197 : 2, 161 : 2, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6]");
		code_v1("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a, SORT_DESC) return a").equals("[181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null, 56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 128 : 2, 197 : 2, 161 : 2, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 162 : 0]");
		code_v2_3("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a, SORT_DESC) return a").equals("[56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 128 : 2, 197 : 2, 161 : 2, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 162 : 0, 181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null]");
		code_v4_("assocSort([1 : 2])").error(Error.REMOVED_FUNCTION);
		code_v1("var a = [306 : 1610.592, 323 : 1207.944, 324 : 2013.24, 341 : 1525.741] assocSort(a, SORT_DESC) return a").equals("[324 : 2 013,24, 306 : 1 610,592, 341 : 1 525,741, 323 : 1 207,944]");
		code_v2_3("var a = [306 : 1610.592, 323 : 1207.944, 324 : 2013.24, 341 : 1525.741] assocSort(a, SORT_DESC) return a").equals("[324 : 2013.24, 306 : 1610.592, 341 : 1525.741, 323 : 1207.944]");

		section("keySort legacy");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; keySort(a); return a").equals("[\"a\" : \"va\", \"b\" : \"vb\", \"c\" : \"vc\", \"d\" : \"vd\"]");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; keySort(a, SORT_DESC); return a").equals("[\"d\" : \"vd\", \"c\" : \"vc\", \"b\" : \"vb\", \"a\" : \"va\"]");
		code_v1_3("var a = [6 : 0, 5 : 1, 2 : 2, 3 : 3, 1 : 6, 4 : 7, 0 : 8]; keySort(a); return a").equals("[8, 6, 2, 3, 7, 1, 0]");
		code_v4_("keySort([1 : 2])").error(Error.REMOVED_FUNCTION);

		section("mapSearch");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return search(a,'c')").equals("\"cle3\"");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapSearch(a,'c')").equals("\"cle3\"");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return search(a,'454')").equals("null");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapSearch(a,'454')").equals("null");
		code_v4_("var a = [1: null, 2: 5, 3: 12] return mapSearch(a, 12)").equals("3");
		code_v4_("var a = [1: [1], 2: [2], 3: [3]] return mapSearch(a, [2])").equals("2");

		section("mapContains");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return inArray(a, 'c')").equals("true");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapContains(a, 'c')").equals("true");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return inArray(a,' cle3')").equals("false");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapContains(a, 'cle3')").equals("false");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return inArray(a, '454')").equals("false");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapContains(a, '454')").equals("false");

		section("mapContainsKey");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapContainsKey(a, 'c')").equals("false");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapContainsKey(a, 'cle3')").equals("true");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapContainsKey(a, '454')").equals("false");

		section("mapMin()");
		code_v1_3("return arrayMin([0 : 7, 8 : 9, 'a' : 2])").equals("2");
		code_v4_("return mapMin([0 : 7, 8 : 9, 'a' : 2])").equals("2");
		code_v1_3("var a = [560 : null, 595 : null, 601 : 15, 566 : 13, 531 : 13] return arrayMin(a)").equals("null");
		code_v4_("var a = [560 : null, 595 : null, 601 : 15, 566 : 13, 531 : 13] return mapMin(a)").equals("null");

		section("mapMax()");
		code_v1_3("return arrayMax([0 : 7, 8 : 9, 'a' : 2])").equals("9");
		code_v4_("return mapMax([0 : 7, 8 : 9, 'a' : 2])").equals("9");
		code_v1_3("var a = [560 : null, 595 : null, 601 : 15, 566 : 13, 531 : 13] return arrayMax(a)").equals("15");
		code_v4_("var a = [560 : null, 595 : null, 601 : 15, 566 : 13, 531 : 13] return mapMax(a)").equals("15");

		section("mapSum()");
		code_v1("return sum([0:1,'a':5,'test':7])").equals("13");
		code_v2_3("return sum([0:1,'a':5,'test':7])").equals("13.0");
		code_v4_("return mapSum([0 : 1, 'a' : 5, 'test' : 7])").equals("13.0");

		section("mapAverage()");
		code_v1("return average([0: 2, 'a': 4, 'test': 6])").equals("4");
		code_v2_3("return average([0: 2, 'a': 4, 'test': 6])").equals("4.0");
		code_v4_("return mapAverage([0: 2, 'a': 4, 'test': 6])").equals("4.0");

		section("assocReverse");
		code_v1_3("var a = [1 : 2, 3 : 4, 5 : 6] assocReverse(a) return a").equals("[5 : 6, 3 : 4, 1 : 2]");
		code_v4_("assocReverse([1 : 2])").error(Error.REMOVED_FUNCTION);

		section("Map.some");
		code_v1_3("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return v == 6 })").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return v == 6 })").equals("true");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return v == 10 })").equals("false");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return k == 5 })").equals("true");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return k == 10 })").equals("false");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return v * k == 30 })").equals("true");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapSome(a, function(v, k) { return v * k == 77 })").equals("false");

		section("Map.every");
		code_v1_3("var a = [1 : 2, 3 : 4, 5 : 6] return mapEvery(a, function(v, k) { return v == 6 })").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapEvery(a, function(v, k) { return v > 1 })").equals("true");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapEvery(a, function(v, k) { return v > 3 })").equals("false");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapEvery(a, function(v, k) { return k > 0 })").equals("true");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapEvery(a, function(v, k) { return k > 2 })").equals("false");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapEvery(a, function(v, k) { return v * k == 30 })").equals("false");
		code_v4_("var a = [2 : 24, 4 : 12, 12 : 4] return mapEvery(a, function(v, k) { return v * k == 48 })").equals("true");

		section("Map.fold");
		code_v1_3("var a = [1 : 2, 3 : 4, 5 : 6] return mapFold(a, function(v, k) {}, 10)").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [:] return mapFold(a, function(r, v, k) { return r + v * k }, 10)").equals("10");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapFold(a, function(r, v, k) { return r + v * k }, 10)").equals("54");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFold(a, function(r, v, k) { return r + ' ' + v }, 'r:')").equals("\"r: a b c\"");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFold(a, function(r, v) { return r + ' ' + v }, 'r:')").equals("\"r: a b c\"");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFold(a, function(r, v, a, b) { return r + ' ' + v }, 'r:')").equals("\"r: a b c\"");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFold(a, function(r, v, k) { return r + ' ' + k }, 'r:')").equals("\"r: 1 3 5\"");

		section("Map.filter");
		code_v1_3("var a = [1 : 2, 3 : 4, 5 : 6] return mapFilter(a, function(v, k) {})").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("var a = [:] return mapFilter(a, function() { return true })").equals("[:]");
		code_v4_("var a = [:] return mapFilter(a, function(v, k) { return v > 10 })").equals("[:]");
		code_v4_("var a = [1 : 2, 3 : 4, 5 : 6] return mapFilter(a, function(v, k) { return k > 4 })").equals("[5 : 6]");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFilter(a, function(v, k) { return k >= 3 })").equals("[3 : \"b\", 5 : \"c\"]");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFilter(a, function(v) { return v === 'a' })").equals("[1 : \"a\"]");
		code_v4_("var a = [1 : 'a', 3 : 'b', 5 : 'c'] return mapFilter(a, function(v, k, a, b) { return k >= 3 })").equals("[3 : \"b\", 5 : \"c\"]");

		section("Map.merge");
		code_v1_3("return mapMerge([1 : 2], [3 : 4])").error(Error.FUNCTION_NOT_AVAILABLE);
		code_v4_("return mapMerge([:], [:])").equals("[:]");
		code_v4_("return mapMerge([1 : 2], [3 : 4])").equals("[1 : 2, 3 : 4]");
		code_v4_("return mapMerge([1 : 2], [1 : 4])").equals("[1 : 2]");
		code_v4_("return mapMerge(['a' : 2], ['a' : 4])").equals("[\"a\" : 2]");
		code_v4_("return mapMerge([:], [1 : 2, 3 : 4])").equals("[1 : 2, 3 : 4]");
		code_v4_("return mapMerge([1 : 2, 3 : 4], [:])").equals("[1 : 2, 3 : 4]");
	}
}
