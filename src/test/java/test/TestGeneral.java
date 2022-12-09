package test;

import leekscript.runner.LeekConstants;
import leekscript.common.Error;
import leekscript.compiler.WordParser;

public class TestGeneral extends TestCommon {

	public void run() {

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

		section("color()");
		code_v4_("color(0, 0, 0)").error(Error.REMOVED_FUNCTION_REPLACEMENT);
		code_v1_3("return color(255,0,255)").equals(String.valueOf(0xFF00FF));
		code_v1_3("return color(255,255,0)").equals(String.valueOf(0xFFFF00));
		code_v1_3("return color(0,255,255)").equals(String.valueOf(0x00FFFF));

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

		section("Variables with keywords");
		for (var word : WordParser.reservedWords) {
			if (word.equals("this")) {
				code_v1("var " + word + " = 2;").error(Error.NONE);
				code_v2("var " + word + " = 2;").error(Error.THIS_NOT_ALLOWED_HERE);
				code_v3_("var " + word + " = 2;").error(Error.VARIABLE_NAME_UNAVAILABLE);
			} else if (word.equals("instanceof")) {
				code_v1_2("var " + word + " = 2;").error(Error.VAR_NAME_EXPECTED);
				code_v3_("var " + word + " = 2;").error(Error.VAR_NAME_EXPECTED);
			} else if (word.equals("function")) {
				code_v1_2("var " + word + " = 2;").error(Error.OPENING_PARENTHESIS_EXPECTED);
				code_v3_("var " + word + " = 2;").error(Error.OPENING_PARENTHESIS_EXPECTED);
			} else if (word.equals("global")) {
				// code_v1_2("var " + word + " = 2;").error(Error.NONE); // Compilation error
				code_v3_("var " + word + " = 2;").error(Error.VARIABLE_NAME_UNAVAILABLE);
			} else {
				code_v1_2("var " + word + " = 2;").error(Error.NONE);
				code_v3_("var " + word + " = 2;").error(Error.VARIABLE_NAME_UNAVAILABLE);
			}
		}

		section("Globals with keywords");
		code_v1_2("global break = 2").error(Error.VARIABLE_NAME_UNAVAILABLE);
		for (var word : WordParser.reservedWords) {
			if (word.equals("this")) {
				code_v3_("global " + word + " = 2;").error(Error.VARIABLE_NAME_UNAVAILABLE);
			} else if (word.equals("instanceof")) {
				code_v3_("global " + word + " = 2;").error(Error.VAR_NAME_EXPECTED_AFTER_GLOBAL);
			} else if (word.equals("function")) {
				code_v3_("global " + word + " = 2;").error(Error.VARIABLE_NAME_UNAVAILABLE);
			} else {
				code_v3_("global " + word + " = 2;").error(Error.VARIABLE_NAME_UNAVAILABLE);
			}
		}

		code_v1("var new = 12 var b = @new return b").equals("12");
		code_v1("global final = 2").error(Error.NONE);

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
		code("var a = 'hello' return a = 2").equals("2");
		code("var a = 2 a = 'hello' return a").equals("\"hello\"");
		code("var a = 2 a = [1, 2] return a").equals("[1, 2]");
		code_v2_("var a = 5.5 a = {} return a").equals("{}");
		// code("var a = [5, 7] a = 7 System.print(a)").output("7\n");
		// code("var a = 7 a = [5, 12] a").equals("[5, 12]");
		// code("var a = 7 System.print(a) a = <5, 12> System.print(a)").output("7\n<5, 12>\n");
		// code("var a = 5 a = 200l").equals("200");
		// code("var a = 5 a = 200l a").equals("200");
		// code("var a = 200l a = 5").equals("5");
		// code("var a = 200l a = 5 a").equals("5");
		// code("var a = 5.5 a = 200l a").equals("200");
		code("var a = 5.5 return a = 2").equals("2");
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

		section("Assignments");
		// code("var b = 0 { b = 12 } return b").equals("12");
		// code("var i = 12 { i = 'salut' } return i").equals("salut");
		// code("var i = 12 {{{ i = 'salut' }}} return i").equals("salut");
		code("var b = 5 if (1) { b = 'salut' } return b").equals("\"salut\"");
		code("var b = 5 if (0) { b = 'salut' } return b").equals("5");
		code("var a = 12 if (1) { a = 5 a++ } else { a = 3 } return a").equals("6");
		code_v1("var a = 12 if (0) { a = 5 a++ } else { a = 5.5 } return a").equals("5,5");
		code_v2_("var a = 12 if (0) { a = 5 a++ } else { a = 5.5 } return a").equals("5.5");
		// code("var a = 12 if (0) { a = 5 a++ } else { a = 7l } return a").equals("7");
		code("var b = 5 if (1) {} else { b = 'salut' } return b").equals("5");
		code("var b = 5 if (0) {} else { b = 'salut' } return b").equals("\"salut\"");
		code("var x = 5 if (true) if (true) x = 'a' return x").equals("\"a\"");
		code("var x = 5 if (true) if (true) if (true) if (true) if (true) x = 'a' return x").equals("\"a\"");
		// code("var x = 2 var y = { if (x == 0) { return 'error' } 7 * x } return y").equals("14");
		code("var y if (false) { if (true) {;} else { y = 2 } } else { y = 5 } return y").equals("5");
		code("PI = PI + 12; return PI").error(Error.CANT_ASSIGN_VALUE);
		code_v1("var grow = []; var n = []; grow = @n; return grow").equals("[]");
		code("var PI = 3 return PI").equals("3");
		code("var a = 2 var b = 5 var c = 7; a = b = c return [a, b, c]").equals("[7, 7, 7]");

		section("Assignments with +=");
		code_v1("var a = 10 a += 0.5 return a").equals("10,5");
		code_v2_("var a = 10 a += 0.5 return a").equals("10.5");

		section("File");
		file("ai/code/trivial.leek").equals("2");

		section("number()");
		code("return number('12')").equals("12");
		code_v1("return number('12.55')").equals("12,55");
		code_v2_("return number('12.55')").equals("12.55");

		section("Variables with accents");
		code("var état = 12 return état").equals("12");
		code("var ÀÖØÝàöøýÿ = 'yo' return ÀÖØÝàöøýÿ").equals("\"yo\"");
		code("var nœud = [] return nœud").equals("[]");
	}
}
