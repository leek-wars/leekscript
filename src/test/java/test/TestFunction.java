package test;

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

		code("function f(x) { var s = 0 s |= 12 return s } f(12);").equals("null");
		code("function te(a){ return function(){ return a**2; }; } return te(2)();").equals("4");
		code("function te(a){ return function(b){ return function(c){return a*b*c;}; }; } return te(2)(1)(2);").equals("4");
		code("var tab = [2, 3, 4, 5, 6]; var r = []; for (var i : var j in tab) { r[i] = function() { return j; }; } return 4;").equals("4");
		code_v10("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);").equals("[2, 1, 2, 1, 2]");
		code_v11("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);").equals("[2.0, 1, 2.0, 1, 2.0]");

		section("Modify argument");
		code("function test(x) { x += 10 return x } return test(5)").equals("15");
		code("var a = [1, 2, 3] function test(x) { push(x, 10) return x } return [a, test([])]").equals("[[1, 2, 3], [10]]");
	}
}
