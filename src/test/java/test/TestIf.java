package test;

public class TestIf extends TestCommon {

	public void run() throws Exception {
		/*
		* Conditions
		*/
		header("Conditions");
		// code("if true then 12 else 5 end").equals("12");
		// code("if false then 12 else 5 end").equals("5");
		code("if (true) { return 12 } else { return 5 }").equals("12");
		code("if (false) { return 12 } else { return 5 }").equals("5");
		// code("let a = if (false) { 12 } else { 5 } a").equals("5");
		// code("let a = if (true) { 'yo' } else { 'hello' } a").equals("'yo'");
		// code("let a = if (true) { 12 } else { 'hello' } a").equals("12");
		// code("let a = if (true) { 'hello' } else { 12 } a").equals("'hello'");
		// code("if (true) {} else {}").equals("{}");
		code("if (true) {;} else {}").equals("null");
		// code("if (true) { {} } else {}").equals("{}");
		code("if (true) null else {}").equals("null");
		// code("if true").error(ls::Error::UNEXPECTED_TOKEN, {""});
		// code("if true else").error(ls::Error::UNEXPECTED_TOKEN, {"else"});
		code_v2_("if (true) { return {a: 12} } else { return {b: 5} }").equals("{a: 12}");
		code("if (true) return 12 else return 5;").equals("12");
		code("if (false) return 12 else return 5;").equals("5");
		code("if (true) return 12;").equals("12");
		code("if (false) return 12;").equals("null");
		// code("if true then 12 end").equals("12");
		// code("if false then 12 end").equals("null");
		// code("if (true) { 5 } else { return 2 }").equals("5");
		code("if (true) { return 5 } else { 2 }").equals("5");
		code("if (false) { 5 } else { return 2 }").equals("2");
		// code("if (false) { return 5 } else { 2 }").equals("2");
		// code("let a = 5m if true { a } else { 2m }").equals("5");
		// code("let a = 5m if true { a } else { a }").equals("5");
		// code("if true then 1 else 2 end").equals("1");
		// code("if true then if false then 1 else 2 end end").equals("2");
		// code("if true then if false then 1 end else 2 end").equals("null");
		code("if (false) { return 12 } return 5;").equals("5");
		code("var k = '121212' if (false) { return 12 } return 5;").equals("5");
		code("var L = 5 if (L < 1) {;}").equals("null");
		code("var L = 5 if (L > 1) {;}").equals("null");
		code("if (false) { return 'hello' }").equals("null");
		// code("let x = { if 1 2 else 3 } let y = { if x == 0 { 'error' } else { 8 * x } } y").equals("16");
		code("var test = 0; if(false) if(true) test = 3; else test = 1; return test;").equals("0");

		code("var a = 1; if(a is 1) return 2; else return 0").equals("2");
		code("var a = 1; if(a is 2) return 2; else return 0").equals("0");
		code("var a = 1; if(a is not 2) return 2; else return 0").equals("2");
		code("var a = 1; if(a is not 1) return 2; else return 0").equals("0");
		code("var a = true; if(not a) return 2; else return 0").equals("0");
		code("var Bob = 12 if (Bob = 75);").equals("null");
		code("var cell = 1 if (cell != null) return 12").equals("12");
		code("var cell = null if (cell != null) return 12 return 5").equals("5");
		code("function t(c) { var cell = c if (cell!=null ) 1; } return t(300);").equals("null");
		code_v1("function t(@c) { var cell = c if (cell!=null ) 1; } return t(300);").equals("null");
		code_v1("function t(@c) { var cell = c } for (var i = 0; i < 10; ++i) return t(i);").equals("null");
		code_v1("function t(@c) { var cell = c if (cell!=null ) 1; } for (var i = 0; i < 10; ++i) return t(i);").equals("null");
		code_v1("function t(@c) { var cell = c cell != null } for (var i = 0; i < 10; ++i) return t(i);").equals("null");
		code_v1("function t(@c) { var cell = c return cell != null } for (var i = 0; i < 10; ++i) return t(i);").equals("true");

		section("Conditions with other types");
		code("if (1212) { return 'ok' } else { return 5 }").equals("\"ok\"");
		code("if (['str', true][0]) { return 12 } else { return 5 }").equals("12");
		code("if (null) { return 12 } else { return 5 }").equals("5");

		section("Different branch types");
		code("if (1) return ['a'] else if (0) return [2] else return [5.5];").equals("[\"a\"]");
		code("if (0) return ['a'] else if (1) return [2] else return [5.5];").equals("[2]");
		code_v1("if (0) return ['a'] else if (0) return [2] else return [5.5];").equals("[5,5]");
		code_v2_("if (0) return ['a'] else if (0) return [2] else return [5.5];").equals("[5.5]");

		section("Ternary conditions");
		code("return true ? 5 : 12;").equals("5");
		code("return false ? 5 : 12;").equals("12");
		code("return true ? 'a' : 'b';").equals("\"a\"");
		code("return false ? 'a' : 'b';").equals("\"b\"");
		code("return true ? 'a' : 5;").equals("\"a\"");
		code("return false ? 'a' : 5;").equals("5");
		code("return true ? 5 : 'b';").equals("5");
		code("return false ? 5 : 'b';").equals("\"b\"");
		code("return 'good' ? 5 : 12;").equals("5");
		code("return '' ? 5 : 12;").equals("12");
		code("return 'good' ? 'a' : 'b';").equals("\"a\"");
		code("return true ? true ? 5 : 12 : 7;").equals("5");
		code("return true ? false ? 5 : 12 : 7;").equals("12");
		code("return false ? false ? 5 : 12 : 7;").equals("7");
		code("return false ? true ? 5 : 12 : 7;").equals("7");
		code("return true ? true ? true ? 5 : 12 : 7 : 8;").equals("5");
		code("return true ? true ? false ? 5 : 12 : 7 : 8;").equals("12");
		code("return true ? false ? false ? 5 : 12 : 7 : 8;").equals("7");
		code("return (5 > 10) ? 'a' : (4 == 2 ** 2) ? 'yes' : 'no';").equals("\"yes\"");
	}
}