package test;

import leekscript.common.Error;

public class TestFunction extends TestCommon {

	public void run() {

		section("Function");
		code_v1_2("return function() {}").equals("#Anonymous Function");
		code_v1_2("return Function() {}").equals("#Anonymous Function");
		code_v1_2("return FUNCTION() {}").equals("#Anonymous Function");
		code_v3_("return function() {}").equals("#Anonymous Function");
		code_v3_("return Function() {}").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return FUNCTION() {}").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);

		section("Function toBoolean");
		code("var a = function() {} return !!a").equals("true");
		code("var a = function() {} if (a) { return 12 } return null").equals("12");

		section("Recursive");
		code("var fact = function(x) { if (x == 1) { return 1 } else { return fact(x - 1) * x } } return fact(8);").equals("40320");
		// code("var fact = function(x) { if (x == 1) { return 1m } else { return fact(x - 1) * x } } return fact(30m);").equals("265252859812191058636308480000000");
		// code("var fact = function(x) { if (x == 1) { 1m } else { fact(x - 1) * x } } return fact(30);").equals("265252859812191058636308480000000");
		code("var fact = function(x) { if (x > 1) { return fact(x - 1) * x } else { return 1 } } return fact(10);").equals("3628800");
		code("var fib = function(n) { if (n <= 1) { return n } else { return fib(n - 1) + fib(n - 2) } } return fib(25);").equals("75025");
		// code("var fact = x -> if x > 1 x * fact(x - 1) else x fact(5)").equals("120");
		// code("var test = x -> if x > 0 { test(x - 1) } else { 77 } test(4)").equals("77");
		// code("var fact = (x, a) -> { if x == 0 then return a end return fact(x - 1, x * a) } fact(10, 1)").equals("3628800");
		// code("var fact = (x, a) -> { if x == 0m then return a end return fact(x - 1, x * a) } fact(10m, 1m)").equals("3628800");
		// code("function test() { var fact = x -> if x == 1 { 1 } else { fact(x - 1) * x } fact(8) } test()").equals("40320");
		file_v1("ai/code/knapsack.leek").equals("761");
		file_v2_("ai/code/knapsack_2.leek").equals("761");
		code("function cellsInRange(i) { var areaInRange = []; if (i == 0) { return cellsInRange(10); } else { return areaInRange; } } var myRange = cellsInRange(0); return myRange").equals("[]");

		section("Redefinition");
		code("var count = count([1, 2, 3]) return count;").equals("3");
		code("var d = debug d('salut')").equals("null");

		section("System function as argument");
		code_v1("function t(@f) { return function(@a) { return arrayMap(a, f); } } return t(sqrt)([1, 4, 9, 16, 25]);").equals("[1, 2, 3, 4, 5]");
		code_v2_("function t(f) { return function(a) { return arrayMap(a, f); } } return t(sqrt)([1, 4, 9, 16, 25]);").equals("[1.0, 2.0, 3.0, 4.0, 5.0]");

		section("Single null argument");
		code("function f(a) { return 12; } return f(null);").equals("12");
		code("var fa = [function(a) { return 12; }] return fa[0](null);").equals("12");

		section("Capture argument");
		code_v1("function f(@a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("12");
		code_v2_("function f(a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("10");
		code_v1("var f = function(@a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("12");
		code_v2_("var f = function(a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("10");

		section("Capture loop variable");
		code("var sum = 0 for (var i = 0; i < 10; ++i) { sum += (function() { return i })() } return sum").equals("45");

		section("Return reference");
		code_v1("global x = 10 function f() { return @x } var a = f() a += 5 return x").equals("10");
		code("global x = 10 function f() { return x } var a = f() a += 5 return x").equals("10");
		code_v1("var x = 10 var f = function() { return @x } var a = f() a += 5 return x").equals("10");
		code("var x = 10 var f = function() { return x } var a = f() a += 5 return x").equals("10");
		code_v1("var x = [] var f = function() { return @x } var a = f() push(a, 5) return x").equals("[5]");
		code_v1("var x = [] var f = function() { return x } var a = f() push(a, 5) return x").equals("[]");
		code_v2_("var x = [] var f = function() { return x } var a = f() push(a, 5) return x").equals("[5]");

		section("Misc");
		code("function f(x) { var s = 0 s |= 12 return s } f(12);").equals("null");
		code("function te(a){ return function(){ return a**2; }; } return te(2)();").equals("4");
		code("function te(a){ return function(b){ return function(c){return a*b*c;}; }; } return te(2)(1)(2);").equals("4");
		code("var tab = [2, 3, 4, 5, 6]; var r = []; for (var i : var j in tab) { r[i] = function() { return j; }; } return 4;").equals("4");
		code_v1("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);").equals("[2, 1, 2, 1, 2]");
		code_v2_("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);").equals("[2.0, 1, 2.0, 1, 2.0]");
		code_v1("var r = [1, 2, 3] var f = function() { return r } var x = f() push(x, 12) return r").equals("[1, 2, 3]");
		code_v2_("var r = [1, 2, 3] var f = function() { return r } var x = f() push(x, 12) return r").equals("[1, 2, 3, 12]");
		code("function f() { return [1, 2, 3] } var x = f();").equals("null");
		code("var x = arrayMap([1, 2, 3], function(x) { return x });").equals("null");
		code("var x = arrayMap([1, 2, 3], function(x) { return x }); debug(x);").equals("null");
		code("var toto = 12; var f = function() { toto = 'salut'; }; [true, 12, f][2](); return toto").equals("salut");
		code("var toto = 12; var f = function() { toto = 'salut'; }; var g = function() { return f; }; g()() return toto").equals("salut");
		code_v1("function Coordonate(@par_x, @par_y) { var x = par_x; var y = par_y; var getX = function(){ return x; }; var getY = function(){ return y; };return @(function(@method) { if(method === 'getX'){ return getX; } if(method === 'getY'){ return getY;	} }); } var c = Coordonate(5, 12) return [c('getX')(), c('getY')()]").equals("[5, 12]");
		code_v2_("function Coordonate(par_x, par_y) { var x = par_x; var y = par_y; var getX = function(){ return x; }; var getY = function(){ return y; };return (function(method) { if(method === 'getX'){ return getX; } if(method === 'getY'){ return getY;	} }); } var c = Coordonate(5, 12) return [c('getX')(), c('getY')()]").equals("[5, 12]");
		code("function test() { var r = [1, 2, 3] return (r); } return test()").equals("[1, 2, 3]");
		code("function test() { var r = [1, 2, 3] return (r); } var a = test() return a").equals("[1, 2, 3]");
		code("function t(a) {} t([ [12], [12] ])").equals("null");
		code_v1("function t(@a) {} t([ [12], [12] ])").equals("null");
		code_v1("function t() { var a = 12 return @a } return t() + 2").equals("14");
		code("function t() { var a = [1, 2, 3] return a } var x = t() var f = function() { return x } return x").equals("[1, 2, 3]");
		code("push = 1 return push").equals("1");
		code_v1("function LamaSwag() {} @LamaSwag();").equals("null");
		code("function f() { distance = 12 } function distance() { return 'salut' } return distance()").equals("salut");
		code("getOperations()").equals("null");
		code("var a = [function() { return 12 }] return a[0]()").equals("12");
		code_v1("function push_to_array(array) { return function(element) { push(array, element); } } var arrayCurry = []; var functionToCall = push_to_array(arrayCurry); for (var i = 0; i < 5; i++) functionToCall(i); return arrayCurry").equals("[]");
		code_v2_("function push_to_array(array) { return function(element) { push(array, element); } } var arrayCurry = []; var functionToCall = push_to_array(arrayCurry); for (var i = 0; i < 5; i++) functionToCall(i); return arrayCurry").equals("[0, 1, 2, 3, 4]");

		section("Modify argument");
		code("function test(x) { x += 10 return x } return test(5)").equals("15");
		code("var a = [1, 2, 3] function test(x) { push(x, 10) return x } return [a, test([])]").equals("[[1, 2, 3], [10]]");

		code("function f(arg, arg) { return arg } return f(1, 2)").error(Error.PARAMETER_NAME_UNAVAILABLE);
		section("Knapsack variants");
		code("var items = [[37, 3], [47, 10], [28, 5]] var all = []; return count(all);").equals("0");
		code_v1("var aux; aux = function() {}; aux();").equals("null");
		code_v1("var aux = function(current) {}; aux([0, []]);").equals("null");
		code_v1("var aux = function(@current) {}; aux([0, []]);").equals("null");
		code_v1("var aux; aux = function(current, i, tp, added, last) {}; aux([0, []], 0, 25, [], -1);").equals("null");
		code_v1("var aux; aux = function(@current, i, tp, added, last) {}; aux([0, []], 0, 25, [], -1);").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) {}; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp) { if (count(current[1])) push(all, current); var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j]; var cost = item[1]; if (cost > tp) continue; var copy = current; push(copy[1], @[item, cost, 1]); } }; aux([0, []], 0, 25); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j]; var cost = item[1]; if (cost > tp) continue; var copy = current; push(copy[1], @[item, cost, 1]); aux(copy, j, tp - cost); } }; aux([0, []], 0, 25); return count(all);").equals("44");
		code_v1("var added = [] added[1] = true;").equals("null");
		code_v1("var added = [] var new_added = added;").equals("null");
		code_v1("var added = [] var new_added = added; new_added[1] = true;").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { var new_added = added; new_added[1] = true; }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var new_added = added; new_added[1] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var new_added = added; new_added[item_id] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; } }; aux([0, []], 0, 25, []);").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; }; aux([0, []], 0, 25, []);").equals("null");
		code_v1("var add = [2: 2] var copy = add;").equals("null");
		code_v1("var add = [2: true] var copy = add;").equals("null");
		code_v1("var add = [] add[2] = true; add[2] = true;").equals("null");
		code_v1("var add = [] add[2] = true; var copy = add;").equals("null");
		code_v1("var aux = function(tp, add) { if (tp < 0) return; aux(tp - 5, add); }; aux(25, []);").equals("null");
		code_v1("var aux = function(tp, add) { if (tp < 0) return; aux(tp - 5, add); }; aux(25, [1, 2, 3]);").equals("null");
		code_v1("var aux = function(tp, add) { if (tp < 0) return; push(add, 2); aux(tp - 5, add); }; aux(25, []);").equals("null");
		code_v1("var aux = function(tp, add) { if (tp < 0) return; add[2] = true; aux(tp - 5, add); }; aux(25, []);").equals("null");
		code_v1("var aux = function(tp, add) { if (tp < 0) return; var new_add = add; aux(tp - 5, new_add); }; aux(25, []);").equals("null");
		code_v1("var aux = function(tp, add) { if (tp < 0) return; var new_add = add; new_add[2] = true; aux(tp - 5, new_add); }; aux(25, []);").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; aux(tp - 5, new_added); }; aux(25, []);").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; aux([], i, tp - 5, new_added); }; aux([0, []], 0, 25, []);").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; aux([], i, tp - 5, new_added); }; aux([0, []], 0, 25, []); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; var copy = current; aux(copy, i, tp - 5, new_added); }; aux([0, []], 0, 25, []); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; aux(copy, j, tp - 5, new_added, 1); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; push(copy[1], @[item, cost, 1]); aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("44");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; push(copy[1], @[item, cost, 1]); copy[0] += cost; aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("44");

		section("strings.leek variations");
		code("var m = ['A', 'T', 'C', 'G'];").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) {} return abs(100 * (count / tests) - 52) < 12;").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) { var adn = '' for (var j = 0; j < 200; j++) {} } return abs(100 * (count / tests) - 52) < 12;").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) { var adn = '' for (var j = 0; j < 200; j++) {} var c = contains(adn, 'GAGA'); if (c) count++ } return abs(100 * (count / tests) - 52) < 12;").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var adn = '' adn += m[randInt(0, 4)];").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] }").equals("null");
		code("var adn = 'testtest' contains(adn, 'GAGA');").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var adn = 'testtest' adn += m[randInt(0, 4)]").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var adn = 'testtest' adn += m[randInt(0, 4)] contains(adn, 'GAGA');").equals("null");
		code("var adn = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' var c = contains(adn, 'GAGA');").equals("null");
		code("var adn = '' for (var j = 0; j < 200; j++) { adn += 'A' } var c = contains(adn, 'GAGA');").equals("null");
		code("var count = 0 var adn = '' for (var j = 0; j < 200; j++) { adn += 'A' } var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] } var c = contains(adn, 'GAGA');").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] }").equals("null");
		code("var count = 0 var adn = '' for (var j = 0; j < 200; j++) { adn += 'A' } var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var count = 0 var m = ['A', 'T', 'C', 'G'] var adn = '' for (var j = 0; j < 200; j++) { adn += m[0] } var c = false if (c) count++").equals("null");
		code("var count = 0 var m = ['A', 'T', 'C', 'G'] var adn = '' adn += m[0] var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var count = 0 var m = ['A', 'T', 'C', 'G'] var adn = '' for (var j = 0; j < 200; j++) { adn += m[0] } var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 50; j++) { adn += m[randInt(0, 4)] } var c = true; if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 50; j++) { adn += m[randInt(0, 4)] } var c = false; if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 50; j++) { adn += m[randInt(0, 4)] } var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 100; j++) { adn += m[randInt(0, 4)] } var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] } var c = contains(adn, 'GAGA'); if (c) count++").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) { var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] } var c = contains(adn, 'GAGA'); if (c) count++ }").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) { var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] } var c = contains(adn, 'GAGA'); if (c) count++ } return abs(100 * (count / tests) - 52) < 12;").equals("true");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; push(copy[1], @[item, cost, 1]); copy[0] += cost; aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("44");
	}
}
