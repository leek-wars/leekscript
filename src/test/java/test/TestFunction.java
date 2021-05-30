package test;

import leekscript.common.Error;

public class TestFunction extends TestCommon {

	public void run() {

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
		file_v10("ai/code/knapsack.leek").equals("761");
		file_v11("ai/code/knapsack_11.leek").equals("761");

		section("Redefinition");
		code("var count = count([1, 2, 3]) return count;").equals("3");

		section("System function as argument");
		code_v10("function t(@f) { return function(@a) { return arrayMap(a, f); } } return t(sqrt)([1, 4, 9, 16, 25]);").equals("[1, 2, 3, 4, 5]");
		code_v11("function t(f) { return function(a) { return arrayMap(a, f); } } return t(sqrt)([1, 4, 9, 16, 25]);").equals("[1.0, 2.0, 3.0, 4.0, 5.0]");

		section("Single null argument");
		code("function f(a) { return 12; } return f(null);").equals("12");
		code("var fa = [function(a) { return 12; }] return fa[0](null);").equals("12");

		section("Capture argument");
		code_v10("function f(@a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("12");
		code_v11("function f(a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("10");
		code_v10("var f = function(@a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("12");
		code_v11("var f = function(a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("10");

		section("Capture loop variable");
		code("var sum = 0 for (var i = 0; i < 10; ++i) { sum += (function() { return i })() } return sum").equals("45");

		section("Misc");
		code("function f(x) { var s = 0 s |= 12 return s } f(12);").equals("null");
		code("function te(a){ return function(){ return a**2; }; } return te(2)();").equals("4");
		code("function te(a){ return function(b){ return function(c){return a*b*c;}; }; } return te(2)(1)(2);").equals("4");
		code("var tab = [2, 3, 4, 5, 6]; var r = []; for (var i : var j in tab) { r[i] = function() { return j; }; } return 4;").equals("4");
		code_v10("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);").equals("[2, 1, 2, 1, 2]");
		code_v11("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);").equals("[2.0, 1, 2.0, 1, 2.0]");

		section("Modify argument");
		code("function test(x) { x += 10 return x } return test(5)").equals("15");
		code("var a = [1, 2, 3] function test(x) { push(x, 10) return x } return [a, test([])]").equals("[[1, 2, 3], [10]]");

		code("function f(arg, arg) { return arg } return f(1, 2)").error(Error.PARAMETER_NAME_UNAVAILABLE);
		section("Knapsack variants");
		code("var items = [[37, 3], [47, 10], [28, 5]] var all = []; return count(all);").equals("0");
		code_v10("var aux; aux = function() {}; aux();").equals("null");
		code_v10("var aux = function(current) {}; aux([0, []]);").equals("null");
		code_v10("var aux = function(@current) {}; aux([0, []]);").equals("null");
		code_v10("var aux; aux = function(current, i, tp, added, last) {}; aux([0, []], 0, 25, [], -1);").equals("null");
		code_v10("var aux; aux = function(@current, i, tp, added, last) {}; aux([0, []], 0, 25, [], -1);").equals("null");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) {}; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp) { if (count(current[1])) push(all, current); var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j]; var cost = item[1]; if (cost > tp) continue; var copy = current; push(copy[1], @[item, cost, 1]); } }; aux([0, []], 0, 25); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j]; var cost = item[1]; if (cost > tp) continue; var copy = current; push(copy[1], @[item, cost, 1]); aux(copy, j, tp - cost); } }; aux([0, []], 0, 25); return count(all);").equals("44");
		code_v10("var added = [] added[1] = true;").equals("null");
		code_v10("var added = [] var new_added = added;").equals("null");
		code_v10("var added = [] var new_added = added; new_added[1] = true;").equals("null");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { var new_added = added; new_added[1] = true; }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var new_added = added; new_added[1] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var new_added = added; new_added[item_id] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; } }; aux([0, []], 0, 25, []);").equals("null");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; }; aux([0, []], 0, 25, []);").equals("null");
		code_v10("var add = [2: 2] var copy = add;").equals("null");
		code_v10("var add = [2: true] var copy = add;").equals("null");
		code_v10("var add = [] add[2] = true; add[2] = true;").equals("null");
		code_v10("var add = [] add[2] = true; var copy = add;").equals("null");
		code_v10("var aux = function(tp, add) { if (tp < 0) return; aux(tp - 5, add); }; aux(25, []);").equals("null");
		code_v10("var aux = function(tp, add) { if (tp < 0) return; aux(tp - 5, add); }; aux(25, [1, 2, 3]);").equals("null");
		code_v10("var aux = function(tp, add) { if (tp < 0) return; push(add, 2); aux(tp - 5, add); }; aux(25, []);").equals("null");
		code_v10("var aux = function(tp, add) { if (tp < 0) return; add[2] = true; aux(tp - 5, add); }; aux(25, []);").equals("null");
		code_v10("var aux = function(tp, add) { if (tp < 0) return; var new_add = add; aux(tp - 5, new_add); }; aux(25, []);").equals("null");
		code_v10("var aux = function(tp, add) { if (tp < 0) return; var new_add = add; new_add[2] = true; aux(tp - 5, new_add); }; aux(25, []);").equals("null");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; aux(tp - 5, new_added); }; aux(25, []);").equals("null");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; aux([], i, tp - 5, new_added); }; aux([0, []], 0, 25, []);").equals("null");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; aux([], i, tp - 5, new_added); }; aux([0, []], 0, 25, []); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; var copy = current; aux(copy, i, tp - 5, new_added); }; aux([0, []], 0, 25, []); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; aux(copy, j, tp - 5, new_added, 1); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; push(copy[1], @[item, cost, 1]); aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("44");
		code_v10("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; var copy = current; push(copy[1], @[item, cost, 1]); copy[0] += cost; aux(copy, j, tp - cost, new_added, item_id); } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("44");
	}
}
