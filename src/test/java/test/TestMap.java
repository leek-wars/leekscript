package test;

public class TestMap extends TestCommon {

	public void run() {
		header("Map");

		section("Constructor");
		code_v4_("return [:]").equals("[:]");
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
		code_v1_3("var m = [5: 12] return m[5.7]").equals("12");
		code_v4_("var m = [5: 12] return m[5.7]").equals("null");
		// code("var m = [5: 12] m['salut']").error(ls::Error::INVALID_MAP_KEY, {"'salut'", "m", env.tmp_string->to_string()});
		// code("var m = [5.7: 'hello'] m['salut']").error(ls::Error::INVALID_MAP_KEY, {"'salut'", "m", env.tmp_string->to_string()});
		code("var m = [1: 'a', 2: 'b'] m[2] = 'B' return m").equals("[1 : a, 2 : B]");
		code("var m = [1: 2, 3: 4] m[5] = 6 return m").equals("[1 : 2, 3 : 4, 5 : 6]");
		code("var m = ['a': 2, 'b': 4] m['c'] = 6 return m").equals("[a : 2, b : 4, c : 6]");
		code_v1("var m = ['a': 2.5, 'b': 4.8] m['c'] = 6.9 return m").equals("[a : 2,5, b : 4,8, c : 6,9]");
		code_v2_("var m = ['a': 2.5, 'b': 4.8] m['c'] = 6.9 return m").equals("[a : 2.5, b : 4.8, c : 6.9]");
		code("var m = [1: 'a', 2: 'b'] m[3] = 'c' return m").equals("[1 : a, 2 : b, 3 : c]");
		code("var m = ['a': '2', 'b': '4'] m['c'] = '6' return m").equals("[a : 2, b : 4, c : 6]");
		code("var m = [1: 2, 3: 4] m[3] = 6 return m").equals("[1 : 2, 3 : 6]");
		code_v1("var m = [1: 2.5, 3: 4.5] m[3] = 6.5 return m").equals("[1 : 2,5, 3 : 6,5]");
		code_v2_("var m = [1: 2.5, 3: 4.5] m[3] = 6.5 return m").equals("[1 : 2.5, 3 : 6.5]");
		code_v4_("var m = [1.5: 2, 3.5: 4] m[3.5] = 6 return m").equals("[1.5 : 2, 3.5 : 6]");
		code_v4_("var m = [1.5: 2.5, 3.5: 4.5] m[3.5] = 6.5 return m").equals("[1.5 : 2.5, 3.5 : 6.5]");
		code("var m = ['1': 2, '3': 4] m['3'] = 6 return m").equals("[1 : 2, 3 : 6]");
		code_v4_("var m = [1.5: 'a', 2.5: 'b'] m[2.5] = 'c' return m").equals("[2.5 : c, 1.5 : a]");
		code("return ['', [1: 2][1]]").equals("[, 2]");
		code_v1("return ['', [1: 2.5][1]]").equals("[, 2,5]");
		code_v2_("return ['', [1: 2.5][1]]").equals("[, 2.5]");
		code_v1_3("var m = [] var ns = '01234566' return m[ns] = 1").equals("1");
		code_v4_("var m = [] var ns = '01234566' return m[ns] = 1").equals("null");
		code("var a = [12: 5] return a[5] = 7").equals("7");
		code("var a = [12: 5] var b = 7 return a[5] = b").equals("7");

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
		section("Map iteration");
		code("var s = '' for (var k : var v in [:]) { s += (k + ' ' + v) } return s").equals("");
		code("var s = '' for (var k : var v in [1:2]) { s += (k + ' ' + v) } return s").equals("1 2");
		code("var s = '' for (var k : var v in [1:2,3:4]) { s += (k + ' ' + v) } return s").equals("1 23 4");
		code("var s = '' for (var k : var v in [1:2,3:4,5:6]) { s += (k + ' ' + v) } return s").equals("1 23 45 6");
		code("var s = '' for (var k : var v in ['a':'b']) { s += (k + ' ' + v) } return s").equals("a b");
		code("var s = '' for (var k : var v in ['a':'b','c':'d']) { s += (k + ' ' + v) } return s").equals("a bc d");
		code("var s = '' for (var k : var v in ['a':'b','c':'d','e':'f']) { s += (k + ' ' + v) } return s").equals("a bc de f");

		/**
		 * Méthodes
		 */
		section("Map.isEmpty legacy");
		code_v1_3("return isEmpty([2 : 8])").equals("false");

		section("mapMap");
		code_v2_3("return arrayMap(['a': 1, 'b': 2], function(k, v) { return k + v })").equals("[a : a1, b : b2]");
		code_v4_("return mapMap(['a': 1, 'b': 2], function(v) { return v * 10 })").equals("[a : 10, b : 20]");
		code_v4_("return mapMap(['a': 1, 'b': 2], function(v, k) { return k + v })").equals("[a : a1, b : b2]");
		code_v2_3("return function() { var t = ['a': 1, 'b': 2]; arrayMap(t, function(k, v) { v = 'tomate'; k = 'ctus'; return 3; }); return t; }();").equals("[a : 1, b : 2]");
		code_v4_("return function() { var t = ['a': 1, 'b': 2]; mapMap(t, function(v, k) { v = 'tomate'; k = 'ctus'; return 3; }); return t; }();").equals("[a : 1, b : 2]");

		section("mapRemoveKey()");
		code_v1_3("var a = ['a':'va','b':'vb','c':'vc','d':'vd']; removeKey(a,'a'); return a").equals("[b : vb, c : vc, d : vd]");
		code_v4_("var a = ['a':'va','b':'vb','c':'vc','d':'vd']; mapRemove(a,'a'); return a").equals("[b : vb, c : vc, d : vd]");

		section("assocSort() legacy");
		code_v1_3("var t = [2:0,1:1,0:2]; return arraySort(t,function(k1, v1, k2, v2){return (k1>k2)?(-1):(k1<k2)?1:0;})").equals("[2 : 0, 1 : 1, 0 : 2]");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; assocSort(a); return a").equals("[a : va, b : vb, c : vc, d : vd]");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; assocSort(a, SORT_DESC); return a").equals("[d : vd, c : vc, b : vb, a : va]");
		code_v1_3("var a = [8,6,2,3,7,1,0]; assocSort(a); return a").equals("[6 : 0, 5 : 1, 2 : 2, 3 : 3, 1 : 6, 4 : 7, 0 : 8]");
		code_v1("var a = [0, 1, 1, 1, 2, 2, 2, 2, 2, null, 3, 3, 3, 3, 3, 3, 3, 3, 3, null, 4, 4, 4, null, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, null, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, null, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6] assocSort(a) return a").equals("[0 : 0, 1 : 1, 2 : 1, 3 : 1, 4 : 2, 5 : 2, 6 : 2, 7 : 2, 8 : 2, 10 : 3, 11 : 3, 12 : 3, 13 : 3, 14 : 3, 15 : 3, 16 : 3, 17 : 3, 18 : 3, 20 : 4, 21 : 4, 22 : 4, 24 : 4, 25 : 4, 26 : 4, 27 : 4, 28 : 4, 29 : 4, 30 : 4, 31 : 4, 32 : 4, 33 : 5, 34 : 5, 35 : 5, 37 : 5, 38 : 5, 39 : 5, 40 : 5, 41 : 5, 42 : 5, 43 : 5, 44 : 5, 45 : 5, 46 : 5, 47 : 5, 48 : 5, 49 : 5, 51 : 6, 52 : 6, 53 : 6, 54 : 6, 55 : 6, 56 : 6, 57 : 6, 58 : 6, 59 : 6, 60 : 6, 61 : 6, 62 : 6, 63 : 6, 64 : 6, 65 : 6, 66 : 6, 67 : 6, 68 : 6, 69 : 6, 70 : 6, 9 : null, 19 : null, 23 : null, 36 : null, 50 : null]");
		code_v2_3("var a = [0, 1, 1, 1, 2, 2, 2, 2, 2, null, 3, 3, 3, 3, 3, 3, 3, 3, 3, null, 4, 4, 4, null, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, null, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, null, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6] assocSort(a) return a").equals("[9 : null, 19 : null, 23 : null, 36 : null, 50 : null, 0 : 0, 1 : 1, 2 : 1, 3 : 1, 4 : 2, 5 : 2, 6 : 2, 7 : 2, 8 : 2, 10 : 3, 11 : 3, 12 : 3, 13 : 3, 14 : 3, 15 : 3, 16 : 3, 17 : 3, 18 : 3, 20 : 4, 21 : 4, 22 : 4, 24 : 4, 25 : 4, 26 : 4, 27 : 4, 28 : 4, 29 : 4, 30 : 4, 31 : 4, 32 : 4, 33 : 5, 34 : 5, 35 : 5, 37 : 5, 38 : 5, 39 : 5, 40 : 5, 41 : 5, 42 : 5, 43 : 5, 44 : 5, 45 : 5, 46 : 5, 47 : 5, 48 : 5, 49 : 5, 51 : 6, 52 : 6, 53 : 6, 54 : 6, 55 : 6, 56 : 6, 57 : 6, 58 : 6, 59 : 6, 60 : 6, 61 : 6, 62 : 6, 63 : 6, 64 : 6, 65 : 6, 66 : 6, 67 : 6, 68 : 6, 69 : 6, 70 : 6]");
		code_v1("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a) return a").equals("[162 : 0, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 128 : 2, 197 : 2, 161 : 2, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6, 181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null]");
		code_v2_3("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a) return a").equals("[181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null, 162 : 0, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 128 : 2, 197 : 2, 161 : 2, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6]");
		code_v1("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a, SORT_DESC) return a").equals("[181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null, 56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 128 : 2, 197 : 2, 161 : 2, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 162 : 0]");
		code_v2_3("var a = [162: 0, 144: 1, 145: 1, 180: 1, 179: 1, 128: 2, 197: 2, 161: 2, 181: null, 143: 3, 110: 3, 146: 3, 214: 3, 234: null, 230: null, 199: null, 125: 4, 92: 4, 164: 4, 232: 4, 160: 4, 93: 4, 231: 4, 77: null, 252: null, 247: null, 217: null, 251: null, 74: 5, 182: 5, 250: 5, 142: 5, 75: 5, 147: 5, 249: 5, 177: 5, 76: 5, 248: 5, 212: null, 54: null, 60: null, 270: null, 235: null, 269: null, 56: 6, 200: 6, 268: 6, 57: 6, 165: 6, 267: 6, 159: 6, 130: 6, 266: 6, 194: 6, 59: 6, 265: 6, 229: null] assocSort(a, SORT_DESC) return a").equals("[56 : 6, 200 : 6, 268 : 6, 57 : 6, 165 : 6, 267 : 6, 159 : 6, 130 : 6, 266 : 6, 194 : 6, 59 : 6, 265 : 6, 74 : 5, 182 : 5, 250 : 5, 142 : 5, 75 : 5, 147 : 5, 249 : 5, 177 : 5, 76 : 5, 248 : 5, 125 : 4, 92 : 4, 164 : 4, 232 : 4, 160 : 4, 93 : 4, 231 : 4, 143 : 3, 110 : 3, 146 : 3, 214 : 3, 128 : 2, 197 : 2, 161 : 2, 144 : 1, 145 : 1, 180 : 1, 179 : 1, 162 : 0, 181 : null, 234 : null, 230 : null, 199 : null, 77 : null, 252 : null, 247 : null, 217 : null, 251 : null, 212 : null, 54 : null, 60 : null, 270 : null, 235 : null, 269 : null, 229 : null]");

		section("keySort legacy");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; keySort(a); return a").equals("[a : va, b : vb, c : vc, d : vd]");
		code_v1_3("var a = ['b':'vb','c':'vc','a':'va','d':'vd']; keySort(a, SORT_DESC); return a").equals("[d : vd, c : vc, b : vb, a : va]");
		code_v1_3("var a = [6 : 0, 5 : 1, 2 : 2, 3 : 3, 1 : 6, 4 : 7, 0 : 8]; keySort(a); return a").equals("[8, 6, 2, 3, 7, 1, 0]");

		section("mapSearch");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return search(a,'c')").equals("cle3");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapSearch(a,'c')").equals("cle3");
		code_v1_3("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return search(a,'454')").equals("null");
		code_v4_("var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return mapSearch(a,'454')").equals("null");

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
	}
}
