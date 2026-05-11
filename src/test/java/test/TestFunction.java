package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestFunction extends TestCommon {


		@Test
	public void testFunction() throws Exception {
		section("Function");
		code_v1_2("return function() {}").equals("#Anonymous Function");
		code_v1_2("return Function() {}").equals("#Anonymous Function");
		code_v1_2("return FUNCTION() {}").equals("#Anonymous Function");
		code_v3_("return function() {}").equals("#Anonymous Function");
		code_v3_("return Function() {}").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return FUNCTION() {}").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
	}

	@Test
	public void testFunction_toBoolean() throws Exception {
		section("Function toBoolean");
		code("var a = function() {} return !!a").equals("true");
		code("var a = function() {} if (a) { return 12 } return null").equals("12");
	}

	@Test
	public void testFunctions_Divide_Lambdas() throws Exception {
		section("Functions / Lambdas");
		code("var f = x -> x return f(12)").equals("12");
		code("var f = x => x return f(12)").equals("12");
		code("var f = x -> x ** 2 return f(12)").equals("144");
		code("var f = x, y -> x + y return f(5, 12)").equals("17");
		code("var f = -> 12 return f()").equals("12");
		code("return (x -> x)(12)").equals("12");
		code("return (x, y -> x + y)(12, 5)").equals("17");
		code("return ( -> [])()").equals("[]");
		code("return ( -> 12)()").equals("12");
		code("var f = x -> x return f(5) + f(7)").equals("12");
		code("return [-> 12][0]()").equals("12");
		code("return [-> 12, 'toto'][0]()").equals("12");
		code_v1("return (x -> x + 12.12)(1.01)").equals("13,13");
		code_v2_("return (x -> x + 12.12)(1.01)").almost(13.13);
		code_v1("return (x -> x + 12)(1.01)").equals("13,01");
		code_v2_("return (x -> x + 12)(1.01)").almost(13.01);
		code("return [x -> x ** 2][0](12)").equals("144");
		code("return [[x -> x ** 2]][0][0](12)").equals("144");
		code("return [[[x -> x ** 2]]][0][0][0](12)").equals("144");
		code("return [[[[[[[x -> x ** 2]]]]]]][0][0][0][0][0][0][0](12)").equals("144");
		code("return (-> -> 12)()()").equals("12");
		code("return (-> -> -> 13)()()()").equals("13");
		code("return (-> -> -> -> 14)()()()()").equals("14");
		code("return (-> -> -> -> -> 15)()()()()()").equals("15");
		code("var f = -> -> 12 return f()()").equals("12");
		code("var f = x -> -> 'salut' return f(5)()").equals("\"salut\"");
		code("var f = x -> [x, x, x] return f(44)").equals("[44, 44, 44]");
		code("var f = function(x) { var r = x ** 2 return r + 1 } return f(10)").equals("101");
		code("var f = function(x) { if (x < 10) {return true} return 12 } return [f(5), f(20)]").equals("[true, 12]");
		code("var f = function(x) { if (x < 10) {return true} else {return 12} } return [f(5), f(20)]").equals("[true, 12]");
		code_v1("var f = x -> { var y = x == 0 ? 'error' : 1/x return '' + y } return [f(-2), f(0), f(2)]").equals("[\"-0,5\", \"error\", \"0,5\"]");
		code_v2_("var f = x -> { var y = x == 0 ? 'error' : 1/x return '' + y } return [f(-2), f(0), f(2)]").equals("[\"-0.5\", \"error\", \"0.5\"]");
		code("var f = i -> { return [1 2 3][i] } return f(1)").equals("2");
		code("var f = i -> { return [1 2 3][i] } return 42").equals("42");
		code("var f = a, i -> a[i] return f([1 2 3], 1)").equals("2");
		code("return [x -> x][0]").equals("#Anonymous Function");
		// code("var f = x = 2 -> x + 1 return f").equals("#Anonymous Function");
		code("var f = b -> (b) ? 2 : 3 return f(false)").equals("3");
		code("var f = b => {b = !b if (b) { return 2 } else { return 3 }} return f(false)").equals("2");
		code("return (x -> y -> x + 1)(1)(2)").equals("2");
		// code("var f = x, y -> { x += '+' y += '.' } var a = 'A', b = 'B' f(a, b) return [a, b]").equals("['A+', 'B.']");
		// code("var f = -> 12m f()").equals("12");
		// code("var f = x => x f(12m)").equals("12");
		code("return [x -> x]").equals("[#Anonymous Function]");
		code("return (x) -> x").equals("#Anonymous Function");
	}

	/**
	 * Edge cases pour la détection d'arrow function dans une déclaration `var x = ...`.
	 * Le parser doit décider si l'expression à droite du `=` est une arrow function
	 * (pour activer le contexte `inList=false` qui permet aux virgules de séparer
	 * les paramètres) ou une expression normale.
	 *
	 * Couvre les 6 cas que peut rencontrer isArrowFunctionAhead :
	 *   1. arrow inside parens : `(a, b) => a + b`
	 *   2. arrow sans parens (param unique) : `a => a * 2`
	 *   3. expression avec parens mais pas d'arrow : `(1 + 2) * 3`
	 *   4. arrow nested deeper que le premier `)` : `f((y) => y, z)`
	 *   5. function expression avec `=>` dans le return type : `function() => integer { return 1 }`
	 *   6. expression sans `(` ni `=>` : `5`
	 */
	@Test
	public void testArrowFunctionDetectionInVarDecl() throws Exception {
		section("Arrow function detection in var declaration");
		// 1. arrow inside parens
		code("var f = (a, b) => a + b return f(3, 4)").equals("7");
		code("var f = (a) => a * 2 return f(5)").equals("10");
		// 2. arrow sans parens (param unique sans parens)
		code("var f = x => x * 3 return f(4)").equals("12");
		code("var f = -> 42 return f()").equals("42");
		// 3. parens sans arrow : pas une arrow function
		code("var x = (1 + 2) * 3 return x").equals("9");
		code("var x = (5) return x").equals("5");
		// 4. arrow nested deeper qu'un `)` rencontré avant : passage d'arrow comme
		//    argument à une fonction. L'arrow apparaît APRÈS le PAR_LEFT du `f(`
		//    et le scan doit le détecter à travers le `)` du paramètre.
		code("function callIt(f, v) { return f(v) } var g = callIt(x => x * 7, 6) return g").equals("42");
		// 5. function expression avec arrow dans return type : pas une "arrow function"
		//    au sens var-declaration, mais contient `=>`
		code("var f = function() => integer { return 7 } return f()").equals("7");
		code_v2_("var f = function(integer x) => integer { return x + 1 } return f(10)").equals("11");
		// 6. expression simple, no paren no arrow
		code("var x = 5 return x").equals("5");
		code("var s = 'hello' return s").equals("\"hello\"");
		// Combinaisons piégeuses
		code("var f = (a, b) => (a + b) * 2 return f(1, 2)").equals("6");
		code("var x = ((1)) return x").equals("1");
		code("var x = (1) + (2) return x").equals("3");
	}

	/**
	 * Cas additionnels de redéfinition de fonctions système (passe 4 : isRedefinedFunction
	 * fast-path via hasRedefinedFunctions). Les patterns ++/--/+=/-= sont déjà couverts ;
	 * on ajoute ?? = / ??= (coalesce-assign) qui n'était pas explicite.
	 */
	@Test
	public void testRedefinitionAdditional() throws Exception {
		section("Redefinition — coalesce-assign and other compound ops");
		code_v4_("abs ??= 2; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v4_("abs *= 2; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v4_("abs /= 2; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v4_("function f() { return 1 } f ??= function() { return 2 } return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		// Sanity : sans redef, le check `hasRedefinedFunctions` fast-path est utilisé
		// pour TOUTES les variables. Vérifie qu'une IA propre n'émet aucun faux warning.
		code_v4_("var x = 5 return x + 1").equals("6");
	}

	@Test
	public void testRecursive() throws Exception {
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
	}

	@Test
	public void testRedefinition() throws Exception {
		section("Redefinition");
		code("var count = count([1, 2, 3]) return count;").equals("3");
		code("var d = debug d('salut')").equals("null");
		code_v1_3("abs = 2 return abs").equals("2");
		code_v4_("abs = 2 return abs").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("arrayFoldRight = 'salut' return arrayFoldRight").equals("\"salut\"");
		code_v4_("arrayFoldRight = 'salut' return arrayFoldRight").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("cos = function(x, y, z) { return x + y * z } return cos(1, 2, 3)").equals("7");
		code_v4_("cos = function(x, y, z) { return x + y * z } return cos(1, 2, 3)").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("function f(x, y, z) { return x + y * z } cos = f return cos(1, 2, 3)").equals("7");
		code_v4_("function f(x, y, z) { return x + y * z } cos = f return cos(1, 2, 3)").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("isEmpty = function() { return 'salut' } if (!isEmpty()) { return 'wrong' } return 'ok'").equals("\"ok\"");
		code_v4_("isEmpty = function() { return 'salut' } if (!isEmpty()) { return 'wrong' } return 'ok'").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("var _count = count; count = function(x) { return _count(x) } return count([1, 2, 3])").equals("3");
		code_v4_("var _count = count; count = function(x) { return _count(x) } return count([1, 2, 3])").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("var _count = count; count = function(x) { return _count(x) } return count([1, 2, 3]) > count([])").equals("true");
		code_v4_("var _count = count; count = function(x) { return _count(x) } return count([1, 2, 3]) > count([])").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("function f() { return count([1, 2, 3]) } var _count = count; count = function(x) { return _count(x) } return f()").equals("3");
		code_v4_("function f() { return count([1, 2, 3]) } var _count = count; count = function(x) { return _count(x) } return f()").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1_3("function f() { return count([1, 2, 3]) > count([]) } var _count = count; count = function(x) { return _count(x) } return f()").equals("true");
		code_v4_("function f() { return count([1, 2, 3]) > count([]) } var _count = count; count = function(x) { return _count(x) } return f()").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("function isEmpty(x) { return 12 } return !isEmpty(1) && !isEmpty(2)").equals("false");
		code("var max = 0 return max < 12").equals("true");
		code("var max = 0 if (max < 12) { max = 5 } return max").equals("5");
		code("abs++; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("abs--; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("++abs; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("abs += 1; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("function f() { return 1 } f++; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("function f() { return 1 } f += 1; return 0").error(Error.CANNOT_REDEFINE_FUNCTION);
	}

	@Test
	public void testSystem_function_as_argument() throws Exception {
		section("System function as argument");
		code_v1("function t(@f) { return function(@a) { return arrayMap(a, f); } } return t(sqrt)([1, 4, 9, 16, 25]);").equals("[1, 2, 3, 4, 5]");
		code_v2_("function t(f) { return function(a) { return arrayMap(a, f); } } return t(sqrt)([1, 4, 9, 16, 25]);").equals("[1.0, 2.0, 3.0, 4.0, 5.0]");
	}

	@Test
	public void testSingle_null_argument() throws Exception {
		section("Single null argument");
		code("function f(a) { return 12; } return f(null);").equals("12");
		code("var fa = [function(a) { return 12; }] return fa[0](null);").equals("12");
	}

	@Test
	public void testCapture_argument() throws Exception {
		section("Capture argument");
		code_v1("function f(@a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("12");
		code_v2_("function f(a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("10");
		code_v1("var f = function(@a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("12");
		code_v2_("var f = function(a) { return function() { a += 2 } }; var x = 10 f(x)() return x;").equals("10");
	}

	@Test
	public void testCapture_loop_variable() throws Exception {
		section("Capture loop variable");
		code("var sum = 0 for (var i = 0; i < 10; ++i) { sum += (function() { return i })() } return sum").equals("45");
	}

	@Test
	public void testFunction_with_references() throws Exception {
		section("Function with references");
		code_v1("function f(@x) { push(x, 12) } var a = [] f(a) return a").equals("[12]");
		code_v1("function f(x) { push(x, 12) } var a = [] f(a) return a").equals("[]");
		code_v1("function f(x) { push(x, 12) } function g(@x) { push(x, 12) } var a = [] var b = [] f(a) g(b) return [a, b]").equals("[[], [12]]");
		code_v1("function f(x) { push(x, 12) } function g(@x) { push(x, 12) } var a = [] var b = [] var t = [f, g]; t[0](a) t[1](b) return [a, b]").equals("[[], [12]]");
		code_v1("var a = [1, 2, 3] arrayMap(a, function(@x) { x += 1 }) return a").equals("[2, 3, 4]");
		code_v1("var f = function(@x) { x += 1 } var a = [1, 2, 3] arrayMap(a, f) return a").equals("[2, 3, 4]");
		code_v1("function f(@x) { x += 1 } var a = [1, 2, 3] arrayMap(a, f) return a").equals("[2, 3, 4]");
	}

	@Test
	public void testReturn_reference() throws Exception {
		section("Return reference");
		code_v1("global x = 10 function f() { return @x } var a = f() a += 5 return x").equals("10");
		code("global x = 10 function f() { return x } var a = f() a += 5 return x").equals("10");
		code_v1("var x = 10 var f = function() { return @x } var a = f() a += 5 return x").equals("10");
		code("var x = 10 var f = function() { return x } var a = f() a += 5 return x").equals("10");
		code_v1("var x = [] var f = function() { return @x } var a = f() push(a, 5) return x").equals("[5]");
		code_v1("var x = [] var f = function() { return x } var a = f() push(a, 5) return x").equals("[]");
		code_v2_("var x = [] var f = function() { return x } var a = f() push(a, 5) return x").equals("[5]");
	}

	@Test
	public void testMisc() throws Exception {
		section("Misc");
		code("function f(x) { var s = 0 s |= 12 return s } f(12);").equals("12");
		code("function te(a){ return function(){ return a**2; }; } return te(2)();").equals("4");
		code("function te(a){ return function(b){ return function(c){return a*b*c;}; }; } return te(2)(1)(2);").equals("4");
		code("var tab = [2, 3, 4, 5, 6]; var r = []; for (var i : var j in tab) { r[i] = function() { return j; }; } return 4;").equals("4");
		code_strict_v4_("var tab = [2, 3, 4, 5, 6]; var r = []; for (var i : var j in tab) { r[i] = function() { return j; }; } return 4;").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_v1("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return retour").equals("[2, 1, 2, 1, 2]");
		code_v2_("var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return retour").equals("[2.0, 1, 2.0, 1, 2.0]");
		code_v1("var r = [1, 2, 3] var f = function() { return r } var x = f() push(x, 12) return r").equals("[1, 2, 3]");
		code_v2_("var r = [1, 2, 3] var f = function() { return r } var x = f() push(x, 12) return r").equals("[1, 2, 3, 12]");
		code("function f() { return [1, 2, 3] } var x = f();").equals("null");
		code("var x = arrayMap([1, 2, 3], function(x) { return x });").equals("null");
		code("any x = arrayMap([1, 2, 3], function(x) { return x }); debug(x);").equals("null");
		code("any toto = 12; var f = function() { toto = 'salut'; }; [true, 12, f][2](); return toto").equals("\"salut\"");
		code("any toto = 12; var f = function() { toto = 'salut'; }; var g = function() { return f; }; g()() return toto").equals("\"salut\"");
		code_v1("function Coordonate(@par_x, @par_y) { var x = par_x; var y = par_y; var getX = function(){ return x; }; var getY = function(){ return y; };return @(function(@method) { if(method === 'getX'){ return getX; } if(method === 'getY'){ return getY;	} }); } var c = Coordonate(5, 12) return [c('getX')(), c('getY')()]").equals("[5, 12]");
		code_v2_("function Coordonate(par_x, par_y) { var x = par_x; var y = par_y; var getX = function(){ return x; }; var getY = function(){ return y; };return (function(method) { if(method === 'getX'){ return getX; } if(method === 'getY'){ return getY;	} }); } var c = Coordonate(5, 12) return [c('getX')(), c('getY')()]").equals("[5, 12]");
		code("function test() { var r = [1, 2, 3] return (r); } return test()").equals("[1, 2, 3]");
		code("function test() { var r = [1, 2, 3] return (r); } var a = test() return a").equals("[1, 2, 3]");
		code("function t(a) {} t([ [12], [12] ])").equals("null");
		code_v1("function t(@a) {} t([ [12], [12] ])").equals("null");
		code_v1("function t() { var a = 12 return @a } return t() + 2").equals("14");
		code("function t() { var a = [1, 2, 3] return a } var x = t() var f = function() { return x } return x").equals("[1, 2, 3]");
		code_v1_3("push = 1 return push").equals("1");
		code_v4_("push = 1 return push").error(Error.CANNOT_REDEFINE_FUNCTION);
		code_v1("function LamaSwag() {} @LamaSwag();").equals("null");
		code_v1_3("function f() { distance = 12 } function distance() { return 'salut' } return distance()").equals("\"salut\"");
		code_v4_("function f() { distance = 12 } function distance() { return 'salut' } return distance()").error(Error.CANNOT_REDEFINE_FUNCTION);
		code("getOperations()").equals("0");
		code("var a = [function() { return 12 }] return a[0]()").equals("12");
		code_v1("function push_to_array(array) { return function(element) { push(array, element); } } var arrayCurry = []; var functionToCall = push_to_array(arrayCurry); for (var i = 0; i < 5; i++) functionToCall(i); return arrayCurry").equals("[]");
		code_v2_("function push_to_array(array) { return function(element) { push(array, element); } } var arrayCurry = []; var functionToCall = push_to_array(arrayCurry); for (var i = 0; i < 5; i++) functionToCall(i); return arrayCurry").equals("[0, 1, 2, 3, 4]");
	}

	@Test
	public void testModify_argument() throws Exception {
		section("Modify argument");
		code("function test(x) { x += 10 return x } return test(5)").equals("15");
		code("var a = [1, 2, 3] function test(x) { push(x, 10) return x } return [a, test([])]").equals("[[1, 2, 3], [10]]");

		code("function f(arg, arg) { return arg } return f(1, 2)").error(Error.PARAMETER_NAME_UNAVAILABLE);
	}

	@Test
	public void testKnapsack_variants() throws Exception {
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
		code_v1("var added = [] added[1] = true;").equals("true");
		code_v1("var added = [] var new_added = added;").equals("null");
		code_v1("var added = [] var new_added = added; new_added[1] = true;").equals("true");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { var new_added = added; new_added[1] = true; }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var new_added = added; new_added[1] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var new_added = added; new_added[item_id] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { if (count(current[1])) push(all, current);	var item_count = count(items); for (var j = i; j < item_count; ++j) { var item = @items[j];	var item_id = item[0]; var cost = item[1]; if (cost > tp) continue;var new_added = added; new_added[item_id] = true; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; } }; aux([0, []], 0, 25, []);").equals("null");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var all = []; var aux; aux = function(@current, i, tp, added, last) { for (var j = i; j < 3; ++j) { if (tp < 0) continue; var new_added = added; new_added[2] = true; var copy = current; } }; aux([0, []], 0, 25, [], -1); return count(all);").equals("0");
		code_v1("var items = [[37, 3], [47, 10], [28, 5]] var aux; aux = function(@current, i, tp, added) { if (tp < 0) return; var new_added = added; new_added[2] = true; }; aux([0, []], 0, 25, []);").equals("null");
		code_v1("var add = [2: 2] var copy = add;").equals("null");
		code_v1("var add = [2: true] var copy = add;").equals("null");
		code_v1("var add = [] add[2] = true; add[2] = true;").equals("true");
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
	}

	@Test
	public void testStrings_leek_variations() throws Exception {
		section("strings.leek variations");
		code("var m = ['A', 'T', 'C', 'G'];").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) {} return abs(100 * (count / tests) - 52) < 12;").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) { var adn = '' for (var j = 0; j < 200; j++) {} } return abs(100 * (count / tests) - 52) < 12;").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var count = 0 var tests = 500 for (var k = 0; k < tests; k++) { var adn = '' for (var j = 0; j < 200; j++) {} var c = contains(adn, 'GAGA'); if (c) count++ } return abs(100 * (count / tests) - 52) < 12;").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var adn = '' adn += m[randInt(0, 4)] null").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var adn = '' for (var j = 0; j < 200; j++) { adn += m[randInt(0, 4)] }").equals("null");
		code("var adn = 'testtest' contains(adn, 'GAGA');").equals("false");
		code("var m = ['A', 'T', 'C', 'G'] var adn = 'testtest' adn += m[randInt(0, 4)] null").equals("null");
		code("var m = ['A', 'T', 'C', 'G'] var adn = 'testtest' adn += m[randInt(0, 4)] contains(adn, 'GAGA')").equals("false");
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

	@Test
	public void testSystem_function_typing() throws Exception {
		section("System function typing");
		code_v1_3("count('hello')").equals("0");
		code_v4_("count('hello')").warning(Error.WRONG_ARGUMENT_TYPE);
		code("return abs(12) < 50").equals("true");
		code("return round(abs(cos(2)) + 5)").equals("5");
		code("var a = cos return round(acos(a(2)))").equals("2");
		code_v1("return arrayMap([1, 2, 3], cos)").equals("[0,54, -0,416, -0,99]");
		code_v2_("return arrayMap([1, 2, 3], cos)").equals("[0.5403023058681398, -0.4161468365471424, -0.9899924966004454]");
		code_v1("return arrayMap([1, 2, 3], atan2)").equals("[0, 0,464, 0,588]");
		code_v2_3("return arrayMap([1, 2, 3], atan2)").equals("[0.0, 0.4636476090008061, 0.5880026035475675]");
		code_v4_("return arrayMap([1, 2, 3], atan2)").equals("[1.5707963267948966, 1.1071487177940904, 0.982793723247329]");
		code_v1("var a = [cos, sin, tan] return arrayMap(a, function(f) { return f(5) })").equals("[0,284, -0,959, -3,381]");
		code_v2_("var a = [cos, sin, tan] return arrayMap(a, function(f) { return f(5) })").equals("[0.28366218546322625, -0.9589242746631385, -3.380515006246586]");
		code("return arrayMap(split('salut', ''), length)").equals("[1, 1, 1, 1, 1]");
		code("return count(arrayFlatten([[1],[2]]))").equals("2");
		code("var a = toUpper, b = arrayMap return b(['a', 'b'], a)").equals("[\"A\", \"B\"]");
		code_v1("return sqrt()").equals("0");
		code_v2("return sqrt()").equals("0.0");
		code_v3_("return sqrt()").error(Error.INVALID_PARAMETER_COUNT);
		code_v1("return sqrt(25, 16, 9)").equals("5");
		code_v2("return sqrt(25, 16, 9)").equals("5.0");
		code_v3_("return sqrt(25, 16, 9)").error(Error.INVALID_PARAMETER_COUNT);
		code_v1("var a = sqrt return a(25, 16, 9)").equals("5");
		code_v2_("var a = sqrt return a(25, 16, 9)").equals("5.0");
		code("return count(unknown([1, 2, 3, 4, 5]))").equals("5");
		code("return count(unknown(12))").equals("0");
		code("return string(count)").equals("\"#Function count\"");
	}

	@Test
	public void testWrong_number_of_arguments() throws Exception {
		section("Wrong number of arguments");
		code("return (x => x)()").warning(Error.INVALID_PARAMETER_COUNT);
		code_strict("var f = (x) => x return f()").error(Error.INVALID_PARAMETER_COUNT);
		code("function f(x) { return x } return f()").error(Error.INVALID_PARAMETER_COUNT);
		code("function f(x) { return x } return [f][0]()").equals("null");
		code_v1("cos()").equals("1");
		code_v2("cos()").equals("1.0");
		code_v3_("cos()").error(Error.INVALID_PARAMETER_COUNT);
		code_v1("return [cos][0]()").equals("null");
	}

	@Test
	public void testTypes() throws Exception {
		section("Types");
		code_v1("function f(integer | real x) { return sqrt(x) } return f(12)").equals("3,464");
		code_v2_("function f(integer | real x) { return sqrt(x) } return f(12)").equals("3.4641016151377544");
		// Le ! ne fait rien donc pas d'erreur, à voir
		DISABLED_code("function b() => string? { return null } var a = b()! return a").error(Error.IMPOSSIBLE_CAST);
		code("function doNothingWithInt(Function<integer => any> f) {} function doNothing() {} function doNothingInt(integer a) {} function doNothingWith(Function< => any> f) {}doNothingWith(doNothing); doNothingWithInt(doNothingInt);").equals("null");
		DISABLED_code_v1("function doNothingWithInt(Function<integer => integer> f) { f(2) } function doNothing() {} function doNothingInt(integer a) {} doNothingWithInt(doNothingInt);").equals("null");
		code_v2_("function doNothingWithInt(Function<integer => integer> f) { f(2) } function doNothing() {} function doNothingInt(integer a) {} doNothingWithInt(doNothingInt);").error(Error.IMPOSSIBLE_CAST);
		code("function f() => integer { return 3; } integer i = f(); integer j = 0; j += f() as integer; j += f()").equals("6");
		DISABLED_code_v1("function f(real r) => real { return r } return f(12)").equals("12.0");
		code_v2_("function f(real r) => real { return r } return f(12)").equals("12.0");
		DISABLED_code_v1("function f(real r) => integer { return r } return f(12)").equals("12");
		code_v2_("function f(real r) => integer { return r } return f(12)").equals("12");
		code_v1("function f(real r) { return r } return f(12)").equals("12");
		code_v2_("function f(real r) { return r } return f(12)").equals("12.0");
		code_v1("function generator() => Function< =>real> { return function() => real { return 12.5 }} var level1 = generator(); var level2 = level1(); return level2").equals("12,5");
		code_v2_("function generator() => Function< =>real> { return function() => real { return 12.5 }} var level1 = generator(); var level2 = level1(); return level2").equals("12.5");
		code("function generator(Function a) { return function () {	a(); } } generator(function () {});").equals("#Anonymous Function");
		code("function f() => void { return; }").equals("null");
		code_strict("function f() => void { return null; }").error(Error.INCOMPATIBLE_TYPE);
		code("function f() => null { return null }").equals("null");
		code_strict("function f() => null { return; }").error(Error.INCOMPATIBLE_TYPE);
		code_v2_("function Functor() => Function < => string> { return function() => string { return 'yea' } } class Temp { Function < => Function> functor; Function result;constructor() { this.functor = Functor; this.result = this.functor(); } } var t = new Temp() return t.result()").equals("\"yea\"");
		DISABLED_code_v1("Function< => integer> f function test(Function< => any> _) {} test(f)").equals("null");
		code_v2_("Function< => integer> f function test(Function< => any> _) {} test(f)").equals("null");
		code("Function<integer => boolean> t = function(integer b) => boolean { return true }").equals("null");
	}

	/**
	 * Edge cases du parser de types Function. Couvre le fast-path de eatPrimaryType
	 * qui doit accepter TokenType.FUNCTION en plus de STRING : en v1/v2 le lexer est
	 * case-insensitive, donc `Function` (capital F) est tokenisé comme FUNCTION
	 * (le keyword) plutôt que STRING.
	 *
	 * Si on oublie d'ajouter FUNCTION à l'allowlist, on obtient des erreurs comme
	 * `OPENING_CURLY_BRACKET_EXPECTED` ou `PARAMETER_NAME_EXPECTED` sur ces tests.
	 */
	@Test
	public void testFunctionTypeEdgeCases() throws Exception {
		section("Function type edge cases (compound types)");
		// Function sans paramètre, juste arrow + return type
		code("function f() => Function< => integer> { return function() => integer { return 7 } } return f()()").equals("7");
		code_v2_("Function< => integer> f f = function() => integer { return 42 } return f()").equals("42");
		// Function avec plusieurs paramètres
		code_v2_("Function<integer, integer => integer> add = function(integer a, integer b) => integer { return a + b } return add(3, 4)").equals("7");
		// Function imbriqué : Function returning Function
		code_v2_("Function< => Function< => integer>> make = function() => Function< => integer> { return function() => integer { return 99 } } return make()()").equals("99");
		// Test en v3+ (case-sensitive : `Function` tokenisé en STRING, pas FUNCTION)
		code_v3_("function f() => Function< => integer> { return function() => integer { return 7 } } return f()()").equals("7");
		// Compound avec null
		code_v2_("Function< => integer> | null g = null return g").equals("null");
	}

	/**
	 * Test : un user identifier qui ressemble à un nom de type ou de keyword.
	 * Le fast-path en v3+ vérifie la première char contre les keyword-starts ; un
	 * identifiant comme `Helper`, `Killer`, `Manager` (commençant par H/J/K/M/Q/U/Z)
	 * skip carrément le HashMap.get sur KEYWORDS.
	 */
	@Test
	public void testKeywordFastPathV3() throws Exception {
		section("Keyword fast-path identifiers (v3+)");
		code_v3_("var Helper = 1 return Helper").equals("1");
		code_v3_("var Manager = 'm' return Manager").equals("\"m\"");
		code_v3_("var Quux = 42 var Zen = 7 return Quux + Zen").equals("49");
		// User identifier qui commence par un char keyword-start (a..y sauf h/j/k/m/q/u/z)
		// → passe le bitmap, fallback HashMap lookup, miss → STRING
		code_v3_("var alpha = 1 var beta = 2 return alpha + beta").equals("3");
		// Identifiant proche d'un keyword mais avec lettre différente
		code_v3_("var foreach = 5 return foreach + 1").equals("6");
		code_v3_("var ifElse = 'x' return ifElse").equals("\"x\"");
	}

	@Test
	public void testConditional_return() throws Exception {
		section("Conditional return");
		code("function f(x) { return? x return 12 } f(5)").equals("5");
		code("function f(x) { return? x return 12 } f(0)").equals("12");
		code("return? 5").equals("5");
		code("return? 'test'").equals("\"test\"");
		code("return? null 5").equals("5");
		code("return? null return? null 5").equals("5");
	}

	@Test
	public void testReturn_type_warnings() throws Exception {
		section("Return type warnings");

		// UNSAFE_DOWNCAST: returning any from typed function — no warning in non-strict
		code_v4_("function f() -> integer { any x = 5 return x } return f()").noWarning();

		// UNSAFE_DOWNCAST: returning integer|null from integer — no warning in non-strict
		code_v4_("function f(integer x) -> integer { var m = [1: 'a'] return m[x] } return f(1)").noWarning();

		// UNSAFE_DOWNCAST in strict mode — should produce warning
		code_strict_v4_("function f() -> integer { any x = 5 return x } return f()").warning(Error.DANGEROUS_CONVERSION);

		// INCOMPATIBLE: returning string from integer function — always warning
		code_v4_("function f() -> integer { return 'hello' } return f()").warning(Error.INCOMPATIBLE_TYPE);
		code_strict_v4_("function f() -> integer { return 'hello' } return f()").error(Error.INCOMPATIBLE_TYPE);

		// Correct return type — no warning
		code_v4_("function f() -> integer { return 42 } return f()").noWarning();
		code_strict_v4_("function f() -> integer { return 42 } return f()").noWarning();

		// UPCAST: returning integer from any function — no warning
		code_v4_("function f() { return 42 } return f()").noWarning();
	}

	@Test
	public void testDefault_parameters() throws Exception {
		section("Default parameters");
		// Basic default parameter
		code("function f(x = 5) { return x } return f()").equals("5");
		code("function f(x = 5) { return x } return f(12)").equals("12");
		// Multiple parameters with some defaults
		code("function f(a, b = 10) { return a + b } return f(3)").equals("13");
		code("function f(a, b = 10) { return a + b } return f(3, 7)").equals("10");
		// Multiple default parameters
		code("function f(a = 1, b = 2, c = 3) { return a + b + c } return f()").equals("6");
		code("function f(a = 1, b = 2, c = 3) { return a + b + c } return f(10)").equals("15");
		code("function f(a = 1, b = 2, c = 3) { return a + b + c } return f(10, 20)").equals("33");
		code("function f(a = 1, b = 2, c = 3) { return a + b + c } return f(10, 20, 30)").equals("60");
		// Default with expression
		code("function f(a = 3 + 4) { return a } return f()").equals("7");
		// Default parameter with string
		code("function f(s = 'hello') { return s } return f()").equals("\"hello\"");
		// Default parameter with array
		code("function f(a = [1, 2, 3]) { return a } return f()").equals("[1, 2, 3]");
		// Default array parameter followed by another parameter
		code("function f(a = [1, 2], b = 10) { return count(a) + b } return f()").equals("12");
		// Error: default parameter not at end
		code("function f(a = 5, b) { return a + b } return f(1, 2)").error(Error.DEFAULT_ARGUMENT_NOT_END);
		// Too many arguments
		code("function f(a, b = 10) { return a + b } return f(1, 2, 3)").error(Error.INVALID_PARAMETER_COUNT);
		// Too few arguments
		code("function f(a, b = 10) { return a + b } return f()").error(Error.INVALID_PARAMETER_COUNT);
	}

	/**
	 * Le fast-path canBeLambda dans readExpression skip la détection lambda quand le
	 * head ne peut clairement pas démarrer une lambda. Couvre les cas frontières :
	 *   - head=STRING + next=OPERATOR(`<`) doit garder la slow path (Array<T> typed lambda)
	 *   - head=STRING + next=OPERATOR(`+/=/...`) doit skip (expression normale)
	 *   - head=STRING + next=BRACKET_LEFT/PAR_LEFT/DOT doit skip (access/call)
	 *   - lambdas typées sans parens (`integer x => x`) doivent toujours matcher
	 *   - lambdas dans contexte liste (`map([1,2,3], x => x * 2)`) doivent matcher
	 */
	@Test
	public void testLambdaFastPathDetection() throws Exception {
		section("Lambda fast-path detection edge cases");
		// 1. Lambdas qui DOIVENT être détectées (canBeLambda=true)
		code("var f = x => x * 2 return f(5)").equals("10");                              // head=STRING, next=ARROW
		code("var f = (a, b) => a + b return f(3, 4)").equals("7");                       // head=PAR_LEFT
		code_v4_("var f = integer x => x + 1 return f(10)").equals("11");                 // head=STRING(int type), next=STRING
		// 2. Non-lambdas avec head=STRING — le fast-path doit skip la lambda detection sans casser le parse
		code("var x = 5 var y = x return y").equals("5");                                 // head=STRING, next=END_INSTRUCTION
		code("var x = 5 var y = x + 1 return y").equals("6");                             // head=STRING, next=OPERATOR(+)
		code("var x = 5 var y = x == 5 return y").equals("true");                         // head=STRING, next=OPERATOR(==)
		code("var x = 5 var y = x < 10 return y").equals("true");                         // head=STRING, next=OPERATOR(<) — false positive canBeLambda, doit retomber correctement
		code_v4_("var arr = [1, 2, 3] return arr[1]").equals("2");                        // head=STRING, next=BRACKET_LEFT
		code("function f() { return 42 } return f()").equals("42");                       // head=STRING, next=PAR_LEFT
		// 3. Lambda dans contexte liste (inList=true) — la garde `!inList || parenthesis`
		// est touchée par le fast-path
		code_v4_("var r = arrayMap([1, 2, 3], x => x * 10) return r").equals("[10, 20, 30]");
		// 4. Expression commençant par opérateur unaire ou nombre — head ≠ lambda-starter, skip entier
		code("var x = -42 return x").equals("-42");                                       // head=OPERATOR
		code("var x = !true return x").equals("false");                                   // head=OPERATOR
		code("var x = 42 return x").equals("42");                                         // head=NUMBER
		code_v4_("var x = [1, 2, 3] return x").equals("[1, 2, 3]");                       // head=BRACKET_LEFT
	}

}
