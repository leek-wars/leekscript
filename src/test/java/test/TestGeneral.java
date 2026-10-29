package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;


import leekscript.runner.LeekConstants;
import leekscript.common.Error;
import leekscript.compiler.LexicalParser;

@ExtendWith(SummaryExtension.class)
public class TestGeneral extends TestCommon {


		@Test
	public void testNull() throws Exception {
		section("null");
		code_v1_2("return null").equals("null");
		code_v1_2("return Null").equals("null");
		code_v1_2("return NULL").equals("null");
		code_v3_("return null").equals("null");
		code_v3_("return Null").equals("<class Null>");
		code_v3_("return NULL").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);

		header("Variables");
		// code("var a").equals("(void)");
		// code("var a = 2").equals("(void)");
		code("var a = 2 return a").equals("2");
		code("var a, b, c = 3 return c").equals("3");
		code("var a = 1, b = 2, c = 3 return c").equals("3");
		code("var a = 1, b = 2, c return c").equals("null");
		// code("var a = 1, b, c = 3").equals("(void)");
		// code("var a, b, c = 3").equals("(void)");
		// code("var a, b, c").equals("(void)");
		code("var a return a").equals("null");
		code("var a a = 12 return a").equals("12");
		code("var a = 5 a = 13 return a").equals("13");
		code("var a = 1 var b = (a = 12) return b").equals("12");
		// code("var s = 'hello'").equals("(void)");
		code("var s = 'hello' return s").equals("\"hello\"");
		// code("var état = 12 return état").equals("12");
		// code("var 韭 = 'leek' return 韭").equals("'leek'");
		// code("var ♫☯🐖👽 = 5 var 🐨 = 2 return ♫☯🐖👽 ** 🐨").equals("25");
		code("var a = 2 return [a = 10]").equals("[10]");
		code("var a = 2 return ['a', a = 10]").equals("[\"a\", 10]");
	}

	@Test
	public void testTypeOf() throws Exception {
		section("typeOf()");
		// Test nombre
		code("return typeOf(255)").equals(String.valueOf(LeekConstants.TYPE_NUMBER.getIntValue()));
		code("return typeOf(255.8)").equals(String.valueOf(LeekConstants.TYPE_NUMBER.getIntValue()));
		// Test string
		code("return typeOf('coucou')").equals(String.valueOf(LeekConstants.TYPE_STRING.getIntValue()));
		// Test boolean
		code("return typeOf(false)").equals(String.valueOf(LeekConstants.TYPE_BOOLEAN.getIntValue()));
		// Test array
		code("return typeOf([1,false])").equals(String.valueOf(LeekConstants.TYPE_ARRAY.getIntValue()));
		// Test fonction
		code("return typeOf(function(){ return null; })").equals(String.valueOf(LeekConstants.TYPE_FUNCTION.getIntValue()));
		// Test null
		code("return typeOf(null)").equals(String.valueOf(LeekConstants.TYPE_NULL.getIntValue()));
		// Test piège
		code("return typeOf(function(){ return 4; }())").equals(String.valueOf(LeekConstants.TYPE_NUMBER.getIntValue()));
		code_v4_("return typeOf(<1, 2, 3>)").equals(String.valueOf(LeekConstants.TYPE_SET.getIntValue()));
		code_v4_("return typeOf([1..10])").equals(String.valueOf(LeekConstants.TYPE_INTERVAL.getIntValue()));
	}

	@Test
	public void testColor() throws Exception {
		section("color()");
		code_v4_("color(0, 0, 0)").error(Error.REMOVED_FUNCTION_REPLACEMENT);
		code_v1_3("return color(255,0,255)").equals(String.valueOf(0xFF00FF));
		code_v1_3("return color(255,255,0)").equals(String.valueOf(0xFFFF00));
		code_v1_3("return color(0,255,255)").equals(String.valueOf(0x00FFFF));
	}

	@Test
	public void testGetColor() throws Exception {
		section("getColor()");
		code("return getColor(255,0,255)").equals(String.valueOf(0xFF00FF));
		code("return getColor(255,255,0)").equals(String.valueOf(0xFFFF00));
		code("return getColor(0,255,255)").equals(String.valueOf(0x00FFFF));

		// Red
		code("return getRed(" + 0xAE0000 + ")").equals("174");
		// Green
		code("return getGreen(" + 0xAF00 + ")").equals("175");
		// Blue
		code("return getBlue(" + 0xAD + ")").equals("173");
	}

	@Test
	public void testVariables_with_keywords() throws Exception {
		section("Variables with keywords");
		String[] reservedWordsV1 = new String[] { "as", "continue", "break", "do", "else", "false", "for", "function", "global", "if", "in", "not", "null", "return", "true", "var", "while", "and", "or", "xor" };
		String[] reservedWordsV2 = new String[] { "class", "constructor", "extends", "instanceof", "new", "private", "protected", "public", "static", "super", "this" };
		String[] reservedWordsV3 = new String[] { "abstract", "await", "byte", "case", "catch", "char", "const", "default", "double", "enum", "eval", "export", "final", "finally", "float", "goto", "implements", "import", "int", "interface", "let", "long", "native", "package", "short", "switch", "synchronized", "throw", "throws", "transient", "try", "typeof", "void", "volatile", "with", "yield" };
		for (var word : reservedWordsV1) {
			// and et or arrivent déjà traduits en && et || : pas de nom à citer
			if (word.equals("and") || word.equals("or")) {
				code("var " + word + " = 2;").error(Error.VAR_NAME_EXPECTED);
			} else {
				code("var " + word + " = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, word);
			}
		}
		for (var word : reservedWordsV2) {
			code_v1("var " + word + " = 2;").error(Error.NONE);
			code_v2_("var " + word + " = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, word);
		}
		for (var word : reservedWordsV3) {
			code_v1_2("var " + word + " = 2;").error(Error.NONE);
			code_v3_("var " + word + " = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, word);
		}
		// Forum 11969 : include aussi, que la première passe prenait pour une inclusion
		code("var include = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "include");
		code("var a = 1, include = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "include");
		code_v1_2("var Include = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "Include");
		code("var 12 = 2;").error(Error.VAR_NAME_EXPECTED);
		code_v1_4("include 'a'").error(Error.OPENING_PARENTHESIS_EXPECTED);
	}

	@Test
	public void testGlobals_with_keywords() throws Exception {
		section("Globals with keywords");
		code_v1_2("global break = 2").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "break");
		for (var word : LexicalParser.reservedWords) {
			// void est lu comme le type de la globale
			if (word.equals("and") || word.equals("or") || word.equals("void")) {
				code_v3_("global " + word + " = 2;").error(Error.VAR_NAME_EXPECTED_AFTER_GLOBAL);
			} else {
				code_v3_("global " + word + " = 2;").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, word);
			}
		}
		code("global a = 1, include = 2").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "include");
		code("global 12 = 2").error(Error.VAR_NAME_EXPECTED_AFTER_GLOBAL);

		code_v1("var new = 12 var b = @new return b").equals("12");
		code_v1("global final = 2").error(Error.NONE);
	}

	@Test
	public void testType_changes() throws Exception {
		section("Type changes");
		code("var a return a = 12").equals("12");
		code("var a a = 12 return a").equals("12");
		code_v1("var a return a = 12.5").equals("12,5");
		code_v2_("var a return a = 12.5").equals("12.5");
		code_v1("var a a = 12.5 return a").equals("12,5");
		code_v2_("var a a = 12.5 return a").equals("12.5");
		code("var a return a = 'a'").equals("\"a\"");
		code("var a a = 'a' return a").equals("\"a\"");
		// code("var a return a = 12m").equals("12");
		// code("var a a = 12m return a").equals("12");
		code("var a = 2 return a = 'hello'").equals("\"hello\"");
		code_strict("var a = 2 return a = 'hello'").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict("any a = 2 return a = 'hello'").equals("\"hello\"");
		code("var a = 'hello' return a = 2").equals("2");
		code_strict("var a = 'hello' return a = 2").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict("any a = 'hello' return a = 2").equals("2");
		code("var a = 2 a = 'hello' return a").equals("\"hello\"");
		code_strict("var a = 2 a = 'hello' return a").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict("any a = 2 a = 'hello' return a").equals("\"hello\"");
		code("var a = 2 a = [1, 2] return a").equals("[1, 2]");
		code_strict("var a = 2 a = [1, 2] return a").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict("any a = 2 a = [1, 2] return a").equals("[1, 2]");
		code_v2_("var a = 5.5 a = {} return a").equals("{}");
		code_strict_v2_("var a = 5.5 a = {} return a").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict_v2_("any a = 5.5 a = {} return a").equals("{}");
		// code("var a = [5, 7] a = 7 System.print(a)").output("7\n");
		// code("var a = 7 a = [5, 12] a").equals("[5, 12]");
		// code("var a = 7 System.print(a) a = <5, 12> System.print(a)").output("7\n<5, 12>\n");
		// code("var a = 5 a = 200l").equals("200");
		// code("var a = 5 a = 200l a").equals("200");
		// code("var a = 200l a = 5").equals("5");
		// code("var a = 200l a = 5 a").equals("5");
		// code("var a = 5.5 a = 200l a").equals("200");
		code("var a = 5.5 return a = 2").equals("2");
		code_strict_v1("var a = 5.5 return a = 2").equals("2");
		code_strict_v2_("var a = 5.5 return a = 2").equals("2.0");
		// code("var a = 5.5 a = 1000m").equals("1000");
		// code("var a = 5.5 a = 2m ** 100").equals("1267650600228229401496703205376");
		// code("var a = 2m return a = 5").equals("5");
		// code("var a = 5.5 System.print(a) a = 2 System.print(a) a = 200l System.print(a) a = 1000m System.print(a) a = 'hello' System.print(a)").output("5.5\n2\n200\n1000\nhello\n");
		// code("var a = [] a = 5m").equals("5");

		// section("Value.copy()");
		// code("2.copy()").equals("2");
		// code("2.5.copy()").equals("2.5");
		// code("12l.copy()").equals("12");
		// code("100m").equals("100");
		// code("'abc'.copy()").equals("'abc'");
		// code("[].copy()").equals("[]");
		// code("[1, 2, 3].copy()").equals("[1, 2, 3]");
		// code("[1.5, 2.5, 3.5].copy()").equals("[1.5, 2.5, 3.5]");
		// code("[1..1000].copy()").equals("[1..1000]");
		// code("[:].copy()").equals("[:]");
		// code("{}.copy()").equals("{}");
		// code("(x -> x).copy()").equals("<function>");
		// code("Number.copy()").equals("<class Number>");
	}

	@Test
	public void testScopeBlocks() throws Exception {
		section("Scope blocks");
		code_v5_("var b = 0 { b = 12 } return b").equals("12");
		code_v5_("var i = 12 { i = 'salut' } return i").equals("\"salut\"");
		code_v5_("var i = 12 {{{ i = 'salut' }}} return i").equals("\"salut\"");
		code_v5_("{ var a = 1 } { var a = 2 } return 0").equals("0");
		code_v5_("{ integer i = 1 } { integer i = 2 } return 0").equals("0");
		code_v5_("var r = 0 { var a = 5 r = a } { var a = 10 r += a } return r").equals("15");
		code_v5_("{ } return 1").equals("1");

		// Portée : une variable du bloc n'existe plus après lui
		code_v5_("{ var a = 1 } return a").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v5_("{ var a = 1 } { var b = a } return 0").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v5_("var x = 1 { x++ } return x").equals("2");
		code_v5_("var r = 0 if (true) { var a = 1 { var b = a + 1 r = b } } return r").equals("2");
		code_v5_("class A { m() { var r = 0 { var a = 2 r = a } { var a = 3 r += a } return r } } return new A().m()").equals("5");

		// Masquage : mêmes règles que dans un if
		code_v5_("var a = 1 { var a = 2 } return a").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v5_("var a = 1 if (true) { var a = 2 } return a").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v5_("{ var a = 1 { var a = 2 } } return 0").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v5_("global g = 1 { var g = 2 return g }").equals("2");
		code_v5_("function foo() { return 1 } { var foo = 2 return foo }").equals("2");

		// Fermeture dans une boucle : une variable fraîche par tour
		code_v5_("var fs = [] for (var i = 0; i < 3; i++) { { var j = i * 10 fs.push(function() { return j }) } } return [fs[0](), fs[1](), fs[2]()]").equals("[0, 10, 20]");

		// break / continue / return dans un bloc nu
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { { if (i == 2) continue } s += i } return s").equals("8");
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { s += i { if (i == 2) break } } return s").equals("3");
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { s += i { continue } } return s").equals("10");
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { s += i { break } } return s").equals("0");
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { s += i { if (i == 2) { break } else { continue } } } return s").equals("3");
		code_v5_("var s = 0 while (s < 10) { s++ { if (s == 4) break } } return s").equals("4");
		code_v5_("var s = 0 do { s++ { if (s == 4) break } } while (s < 10) return s").equals("4");
		code_v5_("var s = 0 for (var x in [1, 2, 3]) { { if (x == 2) continue } s += x } return s").equals("4");
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { switch (i) { case 1: { continue } default: { s += i } } } return s").equals("9");
		code_v5_("var x = 2 switch (x) { case 2: { var y = 5 return y } default: return 0 }").equals("5");
		code_v5_("function f() { { return 1 } } return f()").equals("1");
		code_v5_("function f(x) { { if (x) return 1 else return 2 } } return [f(true), f(false)]").equals("[1, 2]");
		code_v5_("var f = x => { { return x * 2 } } return f(4)").equals("8");
		code_v5_("{ return 7 }").equals("7");
		code_v5_("{ return }").equals("null");
		code_strict_v5_("integer s = 0 for (integer? x in [1, null, 3]) { if (x == null) { { continue } } integer y = x s += y } return s").noWarning();
		code_v5_("{ break }").error(Error.BREAK_OUT_OF_LOOP);
		code_v5_("{ continue }").error(Error.CONTINUE_OUT_OF_LOOP);

		// Code mort après une sortie dans un bloc nu : refusé comme ailleurs
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { continue s += i } return s").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { { continue } s += i } return s").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { { break } s += i } return s").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { { { continue } } s += i } return s").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { { if (i == 2) { break } else { continue } } s++ } return s").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("var s = 0 for (var i = 0; i < 5; i++) { switch (i) { case 1: { break } s++ default: { s += i } } } return s").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("{ return 1 } return 2").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("{ { return 1 } } return 2").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v5_("function f() { { return 1 } return 2 } return f()").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);

		// Blocs vides et imbriqués
		code_v5_("{ { } { { } } } return 3").equals("3");
		code_v5_("{ var x = 1 { var y = x + 1 { return y + 1 } } }").equals("3");
		code_v5_("var a = 1 {} a += 1 return a").equals("2");
		code_v5_("{}").equals("null");
		code_v5_("{ }").equals("null");

		// Bloc mal formé
		code_v5_("{ var a = 1").error(Error.OPEN_BLOC_REMAINING);
		code_v5_("{ { var a = 1 }").error(Error.OPEN_BLOC_REMAINING);
		code_v5_("{ } }").error(Error.NO_BLOC_TO_CLOSE);
		code_v5_("if (true) {} {} else {} return 0").error(Error.NO_IF_BLOCK);

		// Un `{` en début d'instruction reste un littéral objet, en v4 comme en v5, s'il est
		// suivi de `}` puis d'un opérateur, `.`, `[` ou `(`, ou de `clé :`
		code_v2_("{} % 5").equals("0");
		code_v2_("{a: 1}.a").equals("1");
		code_v2_("{a: 1, b: 2}.b").equals("2");
		code_v2_("var o = 1 {a: 1} return o").equals("1");
		code_v2_("var x = 1;{a: 1}.a").equals("1");
		code_v2_("{a: 1}").equals("{a: 1}");
		code_v2_("{} == {}").equals("false");
		code_v2_("{}['a']").equals("null");
		code_v2_("{}.a").equals("null");
		code_v2_("{} + 1").equals("1");
		code_v4_("{} instanceof Object").equals("true");
		code_v2_("{} in [1]").equals("false");
		code_v2_("{ 'a': 1 }").error(Error.VARIABLE_NAME_EXPECTED);
		code_v2_("{ 1: 2 }").error(Error.VARIABLE_NAME_EXPECTED);
		code_v2_("return {}").equals("{}");

		// v1-v4 : pas de bloc nu, rien ne change
		code_v2_4("{}").equals("{}");
		code_v2_4("var x = 1;{}").equals("{}");
		code_v1("var b = 0 { b = 12 } return b").error(Error.VALUE_EXPECTED);
		code_v2_4("var b = 0 { b = 12 } return b").error(Error.UNCOMPLETE_EXPRESSION);
		code_v2_4("{ var a = 1 } return 0").error(Error.KEYWORD_UNEXPECTED);
		code_v2_4("{ } return 1").equals("1");
	}

	@Test
	public void testAssignments() throws Exception {
		section("Assignments");
		code("var b = 5 if (1) { b = 'salut' } return b").equals("\"salut\"");
		code_strict("var b = 5 if (1) { b = 'salut' } return b").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict("any b = 5 if (1) { b = 'salut' } return b").equals("\"salut\"");
		code("var b = 5 if (0) { b = 'salut' } return b").equals("5");
		code_strict("var b = 5 if (0) { b = 'salut' } return b").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_strict("any b = 5 if (0) { b = 'salut' } return b").equals("5");
		code("var a = 12 if (1) { a = 5 a++ } else { a = 3 } return a").equals("6");
		code_v1("var a = 12 if (0) { a = 5 a++ } else { a = 5.5 } return a").equals("5,5");
		code_v2_("var a = 12 if (0) { a = 5 a++ } else { a = 5.5 } return a").equals("5.5");
		code_strict_v2_("real a = 12 if (0) { a = 5 a++ } else { a = 5.5 } return a").equals("5.5");
		// code("var a = 12 if (0) { a = 5 a++ } else { a = 7l } return a").equals("7");
		code("var b = 5 if (1) {} else { b = 'salut' } return b").equals("5");
		code_strict("any b = 5 if (1) {} else { b = 'salut' } return b").equals("5");
		code("var b = 5 if (0) {} else { b = 'salut' } return b").equals("\"salut\"");
		code_strict("any b = 5 if (0) {} else { b = 'salut' } return b").equals("\"salut\"");
		code("var x = 5 if (true) if (true) x = 'a' return x").equals("\"a\"");
		code_strict("any x = 5 if (true) if (true) x = 'a' return x").equals("\"a\"");
		code("var x = 5 if (true) if (true) if (true) if (true) if (true) x = 'a' return x").equals("\"a\"");
		code_strict("any x = 5 if (true) if (true) if (true) if (true) if (true) x = 'a' return x").equals("\"a\"");
		// code("var x = 2 var y = { if (x == 0) { return 'error' } 7 * x } return y").equals("14");
		code("var y if (false) { if (true) {;} else { y = 2 } } else { y = 5 } return y").equals("5");
		code("PI = PI + 12; return PI").error(Error.CANT_ASSIGN_VALUE);
		code("var a = 10 a % 3 \\= 0; return a").error(Error.CANT_ASSIGN_VALUE);
		code("var a = 10 a % 3 += 1; return a").error(Error.CANT_ASSIGN_VALUE);
		code_v1("var grow = []; var n = []; grow = @n; return grow").equals("[]");
		code("var PI = 3 return PI").equals("3");
		code("var a = 2 var b = 5 var c = 7; a = b = c return [a, b, c]").equals("[7, 7, 7]");
	}

	@Test
	public void testAssignments_with_PlusEquals() throws Exception {
		section("Assignments with +=");
		code_v1("var a = 10 a += 0.5 return a").equals("10,5");
		code_v2_("var a = 10 a += 0.5 return a").equals("10.5");
		code_strict_v2_("var a = 10 a += 0.5 return a").equals("10");
		code_strict_v2_("var a = 10 a += 4.5 return a").equals("14");
	}

	@Test
	public void testFile() throws Exception {
		section("File");
		file("ai/code/trivial.leek").equals("2");
	}

	@Test
	public void testNumber() throws Exception {
		section("number()");
		code("return number('12')").equals("12");
		code_v1("return number('12.55')").equals("12,55");
		code_v2_("return number('12.55')").equals("12.55");
	}

	@Test
	public void testVariables_with_accents() throws Exception {
		section("Variables with accents");
		code("var état = 12 return état").equals("12");
		code("var ÀÖØÝàöøýÿ = 'yo' return ÀÖØÝàöøýÿ").equals("\"yo\"");
		code("var nœud = [] return nœud").equals("[]");
	}

	@Test
	public void testVariables_and_types() throws Exception {
		section("Variables and types");
		DISABLED_code_v1("real a = 1.1 integer b = a return b").equals("1");
		code_v2_("real a = 1.1 integer b = a return b").equals("1");
		DISABLED_code_v1("real a = 1.9 integer b = a return b").equals("1");
		code_v2_("real a = 1.9 integer b = a return b").equals("1");
		DISABLED_code_v1("integer|real a = 1.999; integer b = a; return b").equals("1");
		code_v2_("integer|real a = 1.999; integer b = a; return b").equals("1");
		// Un type composé peut commencer par null, y compris en tête d'instruction
		code("null | integer x = 5; return x").equals("5");
		code("null | integer x = 5; x = null; return x").equals("null");
		code_v4_("null | integer x; return x").equals("null");
		code_v4_("null | Function<null | integer => null | integer> | Function< => integer> getter = null; return getter").equals("null");
		code_v4_("class A {} null | A x = new A(); return x").equals("A {}");
		// … sans changer une expression qui commence par null
		code("var a = 3; null | a; return a").equals("3");
		code("var a = 3\nnull | a\nreturn a").equals("3");
		code("null | 2; return 1").equals("1");
	}

	@Test
	public void testUnusedVariable() throws Exception {
		section("Unused variables (strict mode only)");
		// Unused variable in strict mode => warning
		code_strict_v4_("var x = 5; return 0").warning(Error.UNUSED_VARIABLE);
		// Used variable in strict mode => no warning
		code_strict_v4_("var x = 5; return x").noWarning();
		// Unused variable in non-strict mode => no warning
		code_v4_("var x = 5; return 0").noWarning();
		// Underscore prefix suppresses warning
		code_strict_v4_("var _x = 5; return 0").noWarning();
		// Unused function parameter in strict mode
		code_strict_v4_("function f(a) { return 0 } return f(1)").warning(Error.UNUSED_VARIABLE);
		// Used function parameter
		code_strict_v4_("function f(a) { return a } return f(1)").noWarning();
	}

	@Test
	public void testUnusedFunction() throws Exception {
		section("Unused functions (strict mode only)");
		// Unused function in strict mode => warning
		code_strict_v4_("function f() { return 0 } return 0").warning(Error.UNUSED_FUNCTION);
		// Used function in strict mode => no warning
		code_strict_v4_("function f() { return 0 } return f()").noWarning();
		// Unused function in non-strict mode => no warning
		code_v4_("function f() { return 0 } return 0").noWarning();
		// Underscore prefix suppresses warning
		code_strict_v4_("function _f() { return 0 } return 0").noWarning();
		// Function passed as a callback counts as used
		code_strict_v4("function f(x) { return x } return arrayMap([1, 2], f)").noWarning();
		// Function called only from another function (not from main code)
		code_strict_v4_("function helper() { return 1 } function main_fn() { return helper() } return main_fn()").noWarning();
		// Mutual recursion: both functions count as used
		code_strict_v4_("function even(n) { if (n == 0) return true return odd(n - 1) } function odd(n) { if (n == 0) return false return even(n - 1) } return even(4)").noWarning();
		// Function called from a nested block inside another function
		code_strict_v4_("function helper() -> integer { return 1 } function f() -> integer { integer r = 0 for (var i = 0; i < 3; i++) { r += helper() } return r } return f()").noWarning();
		// Entry-point functions (invoked by the host runtime, e.g. hooks) are never
		// flagged as unused even though nothing in the script calls them (issue #4165).
		leekscript.runner.LeekFunctions.setEntryPointFunctions(java.util.Set.of("beforeFight", "afterFight"));
		try {
			code_strict_v4_("function afterFight() { return 0 } return 0").noWarning();
			// A regular unused function is still flagged when entry points are registered.
			code_strict_v4_("function f() { return 0 } return 0").warning(Error.UNUSED_FUNCTION);
		} finally {
			leekscript.runner.LeekFunctions.setEntryPointFunctions(null);
		}
	}

	@Test
	public void testUnusedGlobal() throws Exception {
		section("Unused globals (strict mode only)");
		code_strict_v4_("global g = 5; return 0").warning(Error.UNUSED_VARIABLE);
		code_strict_v4_("global g = 5; return g").noWarning();
		code_v4_("global g = 5; return 0").noWarning();
		code_strict_v4_("global _g = 5; return 0").noWarning();
		code_strict_v4_("global g = 5; function f() { return g } return f()").noWarning();
		code_strict_v4_("global g = 5; function f() { return g } return 0").warning(Error.UNUSED_FUNCTION);
	}

}
