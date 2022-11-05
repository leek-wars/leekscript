package test;

import leekscript.common.Error;

public class TestLoops extends TestCommon {

	public void run() {

		/*
		* While loops
		*/
		section("While loops");
		code("var i = 0 while (i < 10) { i++ } return i;").equals("10");
		code("var i = 0 var s = 0 while (i < 10) { s += i i++ } return s;").equals("45");
		code("var i = 0 while (i < 100) { i++ if (i == 50) break } return i;").equals("50");
		code("var i = 0 var a = 0 while (i < 10) { i++ if (i < 8) continue a++ } return a;").equals("3");
		code("while (true) { break }").equals("null");
		code("var i = 10 while (['hello', i][1]) { i-- } return i;").equals("0");
		code("var i = 0 while (i < 10) i++ return i;").equals("10");
		// code("var i = 5 while (i-- > 0) { System.print(i) }").output("4\n3\n2\n1\n0\n");
		code("while (true) { return 12 }").equals("12");
		code("var n = 5 var a = [] while (n--) { push(a, 1) }").equals("null");
		code("var n = 5 var a = [] while (n--) { push(a, 1) } return a;").equals("[1, 1, 1, 1, 1]");
		code("var n = 5 var a = [] while (n--) { push(a, 1) } return a;").equals("[1, 1, 1, 1, 1]");
		code("var mp = 10, grow = [100] while (mp--) { grow = [1] } return grow;").equals("[1]");
		// DISABLED_code("var a = [] while |a += 'a'| < 5 { a += 'b' } return a;").equals("['a', 'b', 'a', 'b', 'a']");
		code("var s = 0 var j = [] while (count(j) < 10) { push(j, 'a') s++ } return s;").equals("10");
		// DISABLED_code("var s = [] var i = 10 while (i--) { if i < 5 { s += i } else { s += ('a'.code() + i).char() } } return s;").equals("['j', 'i', 'h', 'g', 'f', 4, 3, 2, 1, 0]");
		// DISABLED_code("var s = <> var i = 10 while (i--) { if i < 5 { s += i } else { s += ('a'.code() + i).char() } } return s;").equals("<0, 1, 2, 3, 4, 'f', 'g', 'h', 'i', 'j'>");
		// DISABLED_code("var s = [:] var i = 10 while (i--) { if i < 5 { s[i] = 5.5 } else { s['a'] = i } } return s;").equals("[0: 5.5, 1: 5.5, 2: 5.5, 3: 5.5, 4: 5.5, 'a': 5]");
		// DISABLED_code("var s = [:] var i = 10 while (i--) { if i < 5 { s[i] = 12 } else { s[1 / i] = 7 } } return s;").equals("[0: 12, 0.111111: 7, 0.125: 7, 0.142857: 7, 0.166667: 7, 0.2: 7, 1: 12, 2: 12, 3: 12, 4: 12]");
		// DISABLED_code("var s = [:] var i = 10 while (i--) { if i < 5 { s[i] = 12 } else { s[12] = i } } return s;").equals("[0: 12, 1: 12, 2: 12, 3: 12, 4: 12, 12: 5]");
		// DISABLED_code("var a = 5 while (a-- > 0);").equals("null");
		code("var i = 10 while (i--); return i;").equals("-1");
		code("var i = 10 while (--i); return i;").equals("0");
		code("var t = 0; while(t<5){ t++; return t;}").equals("1");
		code("var t = 0; while(t<5){ t++; return t;} return 0;").equals("1");

		section("Double while loops");
		code("var s = [] var i = 0 while (i < 2) { i++ var j = 0 while (j < 3) { j++ push(s, j) }} return s;").equals("[1, 2, 3, 1, 2, 3]");
		code("var s = [] var i = 0 var j = 0 while (i < 2) { i++ j = 0 while (j < 3) { j++ push(s, j) }} return s;").equals("[1, 2, 3, 1, 2, 3]");
		code("var s = [] var i = 0 var j = 0 while (i < 4) { j = i i++ while (j < 4) { j++ push(s, j) }} return s;").equals("[1, 2, 3, 4, 2, 3, 4, 3, 4, 4]");
		code("var s = [] var i = 0 while (i < 2) { i++ var j = 0 while (j < 2) { j++ var k = 0 while (k < 2) { k++ push(s, k) }}} return s;").equals("[1, 2, 1, 2, 1, 2, 1, 2]");
		code_v1("var s = [] var i = 0 while (i < 2) { i++ push(s, 0.5) var j = 0 while (j < 3) { j++ push(s, j) }} return s;").equals("[0,5, 1, 2, 3, 0,5, 1, 2, 3]");
		code_v2_("var s = [] var i = 0 while (i < 2) { i++ push(s, 0.5) var j = 0 while (j < 3) { j++ push(s, j) }} return s;").equals("[0.5, 1, 2, 3, 0.5, 1, 2, 3]");
		// DISABLED_code("var s = [] var i = 0 while (i < 2) { i++ s.push([]) var j = 0 while j < 3 { j++ s[|s| - 1] += 1 }} s").equals("[[1, 1, 1], [1, 1, 1]]");
		// DISABLED_code("var s = [] var i = 0 while (i < 2) { i++ s.push([]) var j = 0 while j < 3 { j++ s[|s| - 1] += ('a'.code() + 3 * (i - 1) + j - 1).char() }} s").equals("[['a', 'b', 'c'], ['d', 'e', 'f']]");
		// file("test/code/loops/lot_of_whiles_int.leek").equals("30030");
		// file("test/code/loops/lot_of_whiles_array.leek").equals("30030");

		section("Do while");
		code("var i = 0 do { i++ } while (i < 10); return i;").equals("10");
		code("var i = 0 var s = 0 do { s += i i++ } while (i < 10); return s;").equals("45");
		code("var i = 0 do { i++ if (i == 50) break } while (i < 100); return i;").equals("50");
		code("function f(A) { var i = 0 do { i++ } while (i < A); return i } return f(10);").equals("10");
		code("var i = 0 var a = 0 do { i++ if (i < 8) continue a++ } while (i < 10); return a;").equals("3");
		code("do { break } while (true);").equals("null");
		code("do { if (true) return 12 } while (true); return 1;").equals("12");
		code("var t = 0; do { t++; return t; } while (t < 5);").equals("1");
		// TODO catch error
		// code("var t = 0; do { t++; return t;} while (t < 5); return 2;").equals("1");

		// header("For loops");
		// code("for var i = 0; ; i++ {}").ops_limit(1000).exception(ls::vm::Exception::OPERATION_LIMIT_EXCEEDED);
		code("for (var i = 0; false; i++) {}").equals("null");
		code("for (var i = 0; i < 10; i++) {}").equals("null");
		// DISABLED_code("var s = 0 for (var i = 0; i < 5; i++) do s += i end s").equals("10");
		// DISABLED_code("var s = 0 for (var i = 0; i < 10; i += 2) do s += i end s").equals("20");
		code("var s = 0 for (var i = 0; i < 5; i++) { s += i } return s;").equals("10");
		code("var s = 0 for (var i = 0; i < 10; i += 2) { s += i } return s;").equals("20");
		code("var a = 0 for (var i = 0; i < 10; i++) { a++ } return a;").equals("10");
		code("var a = 0 for (var i = 0; i < 10; i++) { if (i < 5) { continue } a++ } return a;").equals("5");
		code("var a = 0 for (var i = 0; i < 10; i++) { if (i > 5) { break } a++ } return a;").equals("6");
		code("var c = 0 for (var t = []; count(t) < 10; push(t, 'x')) { c++ } return c;").equals("10");
		// DISABLED_code("var s = 0 for (var m = [1: 3, 2: 2, 3: 1]; m; var l = 0 for (k, x in m) { l = k } m.erase(l)) { for (x in m) { s += x } } return s;").equals("14");
		code("for (var i = 0; ['', i < 10][1]; i++) {}").equals("null");
		// DISABLED_code("var i = ['', 1][1] for (; i < 10; i <<= 1) {}").equals("null");
		// code("for (var i = 0, j = 0; i < 5; i++, j++) { System.print(i + ', ' + j) }").output("0, 0\n1, 1\n2, 2\n3, 3\n4, 4\n");
		// code("for (var i = 0, j = 10; i < 5; i++, j += 2) { System.print(i + ', ' + j) }").output("0, 10\n1, 12\n2, 14\n3, 16\n4, 18\n");
		// DISABLED_code("for (var i = 0, j = 1, k = 2, l = 3; i < 5; i++, j++, k++, l++) { System.print([i j k l]) }").output("[0, 1, 2, 3]\n[1, 2, 3, 4]\n[2, 3, 4, 5]\n[3, 4, 5, 6]\n[4, 5, 6, 7]\n");
		// code("for var i = 0m; i < 10m; i++ {}").equals("(void)");
		// code("var s = 0m for var i = 0m; i < 10m; i++ { s += i } s").equals("45");
		// code("var s = 0m for var i = 0m; i < 10m; i += 2m { s += i } s").equals("20");

		section("For variable defined before the loop");
		// DISABLED_code("var i = 0 for (; i < 10; i++) { } return i;").equals("10");
		code("var i = 0 for (i = 0; i < 10; i++) { } return i;").equals("10");
		code("var i = 0 for (i = 0; i < 10; i++) { if (i == 5) { break } } return i;").equals("5");
		code_v1("var i var c = 0 for (i = 0; i < 20; i += 0.573) { c++ } return i;").equals("20,055");
		code_v2_("var i var c = 0 for (i = 0; i < 20; i += 0.573) { c++ } return i;").equals("20.05500000000001");
		code("var i = 's' var c = 0 for (i = []; count(i) < 8; push(i, 1)) { c++ } return i;").equals("[1, 1, 1, 1, 1, 1, 1, 1]");
		// DISABLED_code("var i = 0 for (; i < 10; i += 0.5) { } return i;").equals("10");
		// code("var i = 0 for (i = 2l; i < 10; i += 0.5) { } return i;").equals("10");

		section("For whitout braces");
		code("var s = 0 for (var i = 0; i < 10; i++) s += i return s;").equals("45");

		section("For loops with returns");
		// DISABLED_code("for (return 12; true; null) {}").equals("12");
		// DISABLED_code("for (;; return 'hello') {}").equals("'hello'");
		code("for(var i=0;i<3;i++){ return i; }").equals("0");
		code("for(var i=0;i<3;i++){ return i; } return 2;").equals("0");

		section("Nested for loops");
		code("var s = 0 for (var i = 0; i < 10; ++i) { for (var j = 0; j < 10; ++j) { s++ }} return s;").equals("100");
		code("var s = 0 for (var i = 0; i < 5; ++i) { for (var j = 0; j < 5; ++j) { for (var k = 0; k < 5; ++k) { s++ }}} return s;").equals("125");
		code("var s = 0 for (var i = 0; i < 10; i += 1) { for (var j = 0; j < 10; j += 1) { s++ }} return s;").equals("100");
		// DISABLED_code("var s = 0 for (var i = 0; i < 10; i += 1) { var j = 0 for (; j < 10; j += 1) { s++ }} return s;").equals("100");
		// file("test/code/loops/lot_of_fors_int.leek").equals("15015");
		// file("test/code/loops/lot_of_fors_array.leek").equals("15015");
		code_v1_3("var tabmulti=[]; for (var i = 0; i < 8; ++i) tabmulti[i]=1; return tabmulti").equals("[1, 1, 1, 1, 1, 1, 1, 1]");
		code_v4_("var tabmulti=[]; for (var i = 0; i < 8; ++i) tabmulti[i]=1; return tabmulti").equals("[]");
		code_v1_3("var tabmulti=[]; for (var i = 0; i < 9; ++i) tabmulti[i]=1; return tabmulti").equals("[1, 1, 1, 1, 1, 1, 1, 1, 1]");
		code_v4_("var tabmulti=[]; for (var i = 0; i < 9; ++i) tabmulti[i]=1; return tabmulti").equals("[]");
		code_v1_3("var tabmulti=[]; for (var i = 0; i < 50; ++i) tabmulti[i]=1; return tabmulti").equals("[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]");
		code_v4_("var tabmulti=[]; for (var i = 0; i < 50; ++i) tabmulti[i]=1; return tabmulti").equals("[]");
		code_v1_3("var tabmulti=[[],[],[],[],[]]; var i = 3, j = -2 tabmulti[i][j]=i*j; return tabmulti").equals("[[], [], [], [-2 : -6], []]");
		code_v4_("var tabmulti=[[],[],[],[],[]]; var i = 3, j = -2 tabmulti[i][j]=i*j; return tabmulti").equals("[[], [], [], [], []]");
		code("var tabmulti=[[],[],[],[],[]]; var vPM=4; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ }} return tabmulti").equals("[[], [], [], [], []]");
		code_v1_3("var tabmulti=[[],[],[],[],[]]; var vPM=1; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-1 : 0, 0 : 0, 1 : 0], [-1 : -1, 0 : 0, 1 : 1], [], [], []]");
		code_v4_("var tabmulti=[[:],[:],[:],[:],[:]]; var vPM=1; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-1 : 0, 0 : 0, 1 : 0], [-1 : -1, 0 : 0, 1 : 1], [:], <...>, <...>]");
		code_v1_3("var tabmulti=[[],[],[],[],[]]; var vPM=2; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-2 : 0, -1 : 0, 0 : 0, 1 : 0, 2 : 0], [-2 : -2, -1 : -1, 0 : 0, 1 : 1, 2 : 2], [-2 : -4, -1 : -2, 0 : 0, 1 : 2, 2 : 4], [], []]");
		code_v4_("var tabmulti=[[:],[:],[:],[:],[:]]; var vPM=2; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-1 : 0, 0 : 0, -2 : 0, 1 : 0, 2 : 0], [-1 : -1, 0 : 0, -2 : -2, 1 : 1, 2 : 2], [-1 : -2, 0 : 0, -2 : -4, 1 : 2, 2 : 4], [:], <...>]");
		code_v1_3("var tabmulti=[[],[],[],[],[]]; var vPM=3; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-3 : 0, -2 : 0, -1 : 0, 0 : 0, 1 : 0, 2 : 0, 3 : 0], [-3 : -3, -2 : -2, -1 : -1, 0 : 0, 1 : 1, 2 : 2, 3 : 3], [-3 : -6, -2 : -4, -1 : -2, 0 : 0, 1 : 2, 2 : 4, 3 : 6], [-3 : -9, -2 : -6, -1 : -3, 0 : 0, 1 : 3, 2 : 6, 3 : 9], []]");
		code_v4_("var tabmulti=[[:],[:],[:],[:],[:]]; var vPM=3; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-1 : 0, 0 : 0, -2 : 0, 1 : 0, -3 : 0, 2 : 0, 3 : 0], [-1 : -1, 0 : 0, -2 : -2, 1 : 1, -3 : -3, 2 : 2, 3 : 3], [-1 : -2, 0 : 0, -2 : -4, 1 : 2, -3 : -6, 2 : 4, 3 : 6], [-1 : -3, 0 : 0, -2 : -6, 1 : 3, -3 : -9, 2 : 6, 3 : 9], [:]]");
		code_v1_3("var tabmulti=[[],[],[],[],[]]; var vPM=4; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-4 : 0, -3 : 0, -2 : 0, -1 : 0, 0 : 0, 1 : 0, 2 : 0, 3 : 0, 4 : 0], [-4 : -4, -3 : -3, -2 : -2, -1 : -1, 0 : 0, 1 : 1, 2 : 2, 3 : 3, 4 : 4], [-4 : -8, -3 : -6, -2 : -4, -1 : -2, 0 : 0, 1 : 2, 2 : 4, 3 : 6, 4 : 8], [-4 : -12, -3 : -9, -2 : -6, -1 : -3, 0 : 0, 1 : 3, 2 : 6, 3 : 9, 4 : 12], [-4 : -16, -3 : -12, -2 : -8, -1 : -4, 0 : 0, 1 : 4, 2 : 8, 3 : 12, 4 : 16]]");
		code_v4_("var tabmulti=[[:],[:],[:],[:],[:]]; var vPM=4; for(var i=0;i<=vPM;i++){ for(var j=-vPM;j<=vPM;j++){ tabmulti[i][j]=i*j;}} return tabmulti").equals("[[-1 : 0, 0 : 0, -2 : 0, 1 : 0, -3 : 0, 2 : 0, -4 : 0, 3 : 0, 4 : 0], [-1 : -1, 0 : 0, -2 : -2, 1 : 1, -3 : -3, 2 : 2, -4 : -4, 3 : 3, 4 : 4], [-1 : -2, 0 : 0, -2 : -4, 1 : 2, -3 : -6, 2 : 4, -4 : -8, 3 : 6, 4 : 8], [-1 : -3, 0 : 0, -2 : -6, 1 : 3, -3 : -9, 2 : 6, -4 : -12, 3 : 9, 4 : 12], [-1 : -4, 0 : 0, -2 : -8, 1 : 4, -3 : -12, 2 : 8, -4 : -16, 3 : 12, 4 : 16]]");

		section("Mix for and while loops");
		code("var s = 0 for (var i = 0; i < 10; i += 1) { var j = 10 while (j--) { s++ }} return s;").equals("100");
		code("var s = [] for (var i = 0; i < 2; i += 1) { var j = 0 while (j < 3) { j++ push(s, 1) }} return s;").equals("[1, 1, 1, 1, 1, 1]");
		code("var s = 0 for (var i = 0; i < 10; i += 1) { var j = [] while (count(j) < 10) { push(j, 'a') s++ }} return s;").equals("100");
		code("var s = [] var i = 3 while (i--) { for (var j = 0; j < 2; ++j) { push(s, j) }} return s;").equals("[0, 1, 0, 1, 0, 1]");
		code("var s = [] var i = 3 var j while (i--) { for (j = 0; j < 2; ++j) { push(s, j) }} return s;").equals("[0, 1, 0, 1, 0, 1]");
		code("for (var x in [1, 2, 3]) { for (var x = 0; x < 10; ++x) { } }").error(Error.VARIABLE_NAME_UNAVAILABLE);

		header("Foreach loops");
		section("Empty containers");
		code("for (var v in []) {}").equals("null");
		// code("for (v in new Array) {}").equals("(void)");
		code("var s = 0; var a; for (a in [1, 2, 3]) { s += a } return s").equals("6");
		code("var s = 0; var a; for (var k : a in [1, 2, 3]) { s += a } return s").equals("6");
		code("var s = 0; var k; for (k : var a in [1, 2, 3, 4, 5]) { s += k } return s").equals("10");
		// DISABLED_code("for (var x in 12) {}").error(Error.NOT_ITERABLE);

		section("Normal containers");
		code("for (var v in [1, 2, 3, 4]) {}").equals("null");
		// code("for (var v in [1, 2, 3, 4]) do end").equals("(void)");
		code("var s = 0 for (var v in [1, 2, 3, 4]) { s += v } return s;").equals("10");
		// code("var s = 0 for v in [1l, 2l, 3l, 4l] { s += v } return s;").equals("10");
		// code("var s = 0.0 for v in [1.2, 2, 3.76, 4.01] { s += v } s").almost(10.97);
		// code("var s = 0 for v in [1.2, 2, 3.76, 4.01] { s += v } s").almost(10.97);
		code("var s = '' for (var v in ['salut ', 'ça ', 'va ?']) { s += v } return s;").equals("\"salut ça va ?\"");
		code("var a = 0 var x = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] for (var i in x) { if (i < 5) { continue } a++ } return a;").equals("5");
		code("var s = 0 for (var k : var v in [1, 2, 3, 4]) { s += k * v } return s;").equals("20");
		// code("var s = '' for (var k : var v in ['a': 1, 'b': 2, 'c': 3, 'd': 4]) { s += v * k } return s;").equals("abbcccdddd");
		code_v1("return (function (a) { var s = 0.0; for (var x in a) { s += x } return s; })([1, 2, 3, 4.25]);").equals("10,25");
		code_v2_("return (function (a) { var s = 0.0; for (var x in a) { s += x } return s; })([1, 2, 3, 4.25]);").equals("10.25");
		// DISABLED_code("var y = '' for k, x in { var x = [] x.push(4) x } { y += k + ':' + x + ' ' } y").equals("'0:4 '");
		// DISABLED_code("var y = '' for k, x in { var x = [1: 2] x.insert(3, 4) x } { y += k + ':' + x + ' ' } y").equals("'1:2 3:4 '");
		// DISABLED_code("var y = '' for k, x in { var x = [1: 2.5] x.insert(3, 4) x } { y += k + ':' + x + ' ' } y").equals("'1:2.5 3:4 '");
		// DISABLED_code("var y = '' for k, x in { var x = [1: '2'] x.insert(3, 4) x } { y += k + ':' + x + ' ' } y").equals("'1:2 3:4 '");
		// code("var y = 'test' for (var x in 1) { y = x } y").equals("1");
		// code("var y = 'test' for (var x in 'salut') { y = x } y").equals("'t'");
		code("var x = 'test' for (var x in [1]) {} return x;").error(Error.VARIABLE_NAME_UNAVAILABLE);
		// code("var y = '' for k, x in { var x = <> x.insert(4) x } { y += k + ':' + x } y").equals("'0:4'");
		// DISABLED_code("var fs = [] fs.push(s -> {var sum = 0 for v in s {sum += v} sum}) fs[0](<1, 2>)").equals("3"); // TODO issue #243
		// DISABLED_code("var fs = [] fs.push(s -> {[for v in s {v}]}) fs[0](<2,1>)").equals("[1, 2]"); // TODO issue #243
		// code("var s = 0l for i in [0..1000] { s += i ** 2 } s").equals("333833500");
		// code("var S = 'salut' var N = [] for var s in S.split('') { N += String.code(s) } N").equals("[115, 97, 108, 117, 116]");
		code("var tab = [0,1,2,3]; for(var i in tab){ return i; }").equals("0");
		code("var tab = [0,1,2,3]; for(var i in tab){ return i; } return 5;").equals("0");
		code("var tab = [1:0,2:1,3:2,4:3]; for(var i : var j in tab){ return i; } ").equals("1");
		code("var tab = [1:0,2:1,3:2,4:3]; for(var i : var j in tab){ return i; } return 0;").equals("1");

		section("Foreach - no braces");
		code("var s = 0 for (var v in [1, 2, 3, 4]) s += v return s;").equals("10");

		section("Foreach - double");
		code("var r = [] for (var x in [1, 2, 3]) { for (var y in [4, 5, 6]) { push(r, x * y) }} return r;").equals("[4, 5, 6, 8, 10, 12, 12, 15, 18]");
		// code("var r = [] for x in ['a', 'b', 'c'] { for y in [4, 5, 6] { r += x * y }} r").equals("['aaaa', 'aaaaa', 'aaaaaa', 'bbbb', 'bbbbb', 'bbbbbb', 'cccc', 'ccccc', 'cccccc']");
		code("for (var x in [1, 2, 3]) { for (var x in [4, 5, 6]) { } }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code("for (var x : var y1 in [1, 2, 3]) { for (var x : var y2 in [4, 5, 6]) { } }").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code("for (var x1 : var y in [1, 2, 3]) { for (var x2 : var y in [4, 5, 6]) { } }").error(Error.VARIABLE_NAME_UNAVAILABLE);

		section("Foreach - mix");
		// code("var n = 3 while (n--) { var r = [] for x in [1, 2, 3] { r += x } print(r) }").output("[1, 2, 3]\n[1, 2, 3]\n[1, 2, 3]\n");

		section("Foreach - references");
		code_v1("var s = 0 for (var @v in [1, 2, 3, 4]) { s += v } return s;").equals("10");
		code_v1("var s = 0 for (var @k : var @v in [1, 2, 3, 4]) { s += k * v } return s;").equals("20");

		section("Foreach - captures");
		code("for (var e in [1, 2, 3]) { (function() { return e })() } return null;").equals("null");
		code("for (var i : var e in [1, 2, 3]) { (function() { return e })() } return null;").equals("null");
		code_v1("var a = [1, 2, 3] for (var @e in a) { (function() { e = 5 })() } return a;").equals("[1, 2, 3]");
		code_v1("var a = [1, 2, 3] for (var i : var @e in a) { (function() { e = 5 })() } return a;").equals("[1, 2, 3]");
		code("var a = [1, 2, 3] for (var i : var v in a) { (function() { i += 1 })() } return a;").equals("[1, 2, 3]");

		section("Foreach - return");
		code("for (var x in [1]) { return 12 }").equals("12");
		// code("for (var x in 'salut') { return 13 }").equals("13");
		// code("for (var x in 123) { return 14 }").equals("14");

		section("Foreach - argument");
		code("function main(r) { for (var x in [1, 2, 3]) { for (var y in [4, 5, 6]) { push(r, x * y) }} return r } return main([]);").equals("[4, 5, 6, 8, 10, 12, 12, 15, 18]");

		section("Foreach - variable used in container");
		code("for (var x in x) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x in [x]) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x in arrayMap(x, => 12)) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x : var y in x) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x : var y in [x]) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x : var y in arrayMap(x, => 12)) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x : var y in y) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x : var y in [y]) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code("for (var x : var y in arrayMap(y, => 12)) {}").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);

		// header("Foreach - unknown container");
		// code("for x in ['hello', 12345][0] { print(x) }").equals("h\ne\nl\nl\no\n");

		// header("Foreach - not iterable");
		// code("for x in null {}").error(ls::Error::Type::VALUE_NOT_ITERABLE, {"null", env.null->to_string()});
		// code("for x in true {}").error(ls::Error::Type::VALUE_NOT_ITERABLE, {"true", env.boolean->to_string()});
		// code("for x in Number {}").error(ls::Error::Type::VALUE_NOT_ITERABLE, {"Number", env.const_class()->to_string()});

		// header("Array For");
		// code("[for var i = 0; i < 5; ++i { i }]").equals("[0, 1, 2, 3, 4]");
		// code("[for var i = 1; i <= 10; ++i { [for var j = 1; j <= 3; ++j { if i == 3 break 2 i * j}] }]").equals("[[1, 2, 3], [2, 4, 6]]");
		// code("[for x in [1, 2, 3] { x }]").equals("[1, 2, 3]");
		// code("let a = ['a': 'b', 'c': 'd'] [for k, x in a { k + x }]").equals("['ab', 'cd']");
		// code("[for x in [1, 2, 3] {[ for y in [1, 2, 3] { if y == 2 continue x * y }] }]").equals("[[1, 3], [2, 6], [3, 9]]");
		// code("let sorted = [for x in <5, 2, 4, 1, 3> { x }] sorted").equals("[1, 2, 3, 4, 5]");
		// code("[for i in [1..10] { i }]").equals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
		// DISABLED_code("function attrs(o) { [for k : v in o {v}] } attrs(['a'])").equals("['a']");
		// DISABLED_code("function attrs(o) { [for k : v in o {v}] } attrs([1])").equals("[1]");
		// DISABLED_code("function attrs(o) { [for k : v in o {v}] } attrs([])").equals("[]");
		// DISABLED_code("function f() { [for x in [1, 2, 3] { x }] } f()").equals("[1, 2, 3]");
		// DISABLED_code("function f() { for x in [1, 2, 3] { print(x) } } f()").output("1\n2\n3\n");

		header("Breaks and Continues");
		code("break").error(Error.BREAK_OUT_OF_LOOP);
		code("continue").error(Error.CONTINUE_OUT_OF_LOOP);
		// code("while (true) { break 2 }").error(ls::Error::Type::BREAK_MUST_BE_IN_LOOP, {});
		// code("while (true) { continue 2 }").error(ls::Error::Type::CONTINUE_MUST_BE_IN_LOOP, {});
		// code("var r = 0 for (var x in [1, 2]) { for (var y in [3, 4]) { r = 10 * x + y if (x + y) >= 5 break 2 }} r").equals("14");
		// code("var r = 0 for (var x in [1, 2]) { for (var y in [3, 4]) { r = 10 * x + y continue 2 } r = 0 } r").equals("23");
		// code("for (var x in ['a']) { var a = 'a' { var b = 'b' break } var d = 'd' } return 0;").equals("0");
		code("for (var x in ['a']) { var a = 'a' for (var y in ['a']) { var b = 'b' break } var d = 'd' } return 0;").equals("0");
		// code("for (var x in ['a']) { var a = 'a' for y in ['a'] { var b = 'b' break 2 let c = 'c' } let d = 'd' } 0").equals("0");
		// code("for (var x = 0; x < 2; ++x) { var a = 'a' { var b = 'b' break } let d = 'd' } 0").equals("0");
		code("for (var x = 0; x < 2; ++x) { var a = 'a' for (var y = 0; y < 2; ++y) { var b = 'b' break } var d = 'd' } return 0;").equals("0");
		// code("for (var x = 0; x < 2; ++x) { var a = 'a' for (var y = 0; y < 2; ++y) { var b = 'b' break 2 var c = 'c' } var d = 'd' } return 0;").equals("0");
		// code("while (true) { break 0 }").error(ls::Error::Type::BREAK_LEVEL_ZERO, {});
		// code("while (true) { continue 0 }").error(ls::Error::Type::CONTINUE_LEVEL_ZERO, {});
		code("arrayMap([1, 2, 3], function(arr) { if (1) continue })").error(Error.CONTINUE_OUT_OF_LOOP);
		code("for (var i in [1, 2, 3]) { arrayMap([1, 2, 3], function(arr) { if (1) continue }) }").error(Error.CONTINUE_OUT_OF_LOOP);
	}
}
