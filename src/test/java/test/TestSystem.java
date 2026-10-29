package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestSystem extends TestCommon {


	@Test
	public void run() throws Exception {

		section("debug()");
		code("return debug(null)").equals("null");

	}

	@Test
	public void testSourcePosition() throws Exception {
		section("getSourceFile()");
		// Un extrait compilé seul porte un nom à numéro (« <snippet N> ») : seul le « / » de tête est stable
		code("return substring(getSourceFile(), 0, 1)").equals("\"/\"");
		code("return getSourceFile()").ops(1);
		code("function getSourceFile() { return 'mine' } return getSourceFile()").equals("\"mine\"");
		code("var getSourceFile = 12 return getSourceFile").equals("12");
		code_v3_("getSourceFile(1)").error(Error.INVALID_PARAMETER_COUNT);

		section("getSourceLine()");
		code("return getSourceLine()").equals("1");
		code("\n\nreturn getSourceLine()").equals("3");
		code("var a = getSourceLine()\nvar b = getSourceLine()\n\nreturn [a, b, getSourceLine()]").equals("[1, 2, 4]");
		code("return getSourceLine(\n)").equals("1");
		code("getSourceLine()\nreturn getSourceLine() + 1").equals("3");
		code("return [getSourceLine(), getSourceLine() + 1, -getSourceLine(), 'l' + getSourceLine(), getSourceLine() == 1]").equals("[1, 2, -1, \"l1\", true]");
		code("var n = 0\nfor (var i = 0; i < 2; getSourceLine()) { i++ n++ }\nreturn n").equals("2");
		code_v1("function f(@x) { return x }\nreturn f(getSourceLine())").equals("2");
		code("function f() {\n\treturn getSourceLine()\n}\nreturn f()").equals("2");
		code_v2_("var f = () -> getSourceLine()\n\nreturn f()").equals("1");
		code_v2_("class A {\n\tm() {\n\t\treturn getSourceLine()\n\t}\n\tstatic s() { return getSourceLine() }\n}\nreturn [new A().m(), A.s()]").equals("[3, 5]");
		code_strict_v2_("integer l = getSourceLine() return l").equals("1");
		code_strict_v2_("real r = getSourceLine() return r").equals("1.0");
		code_v2_("return sqrt(getSourceLine() + 3)").equals("2.0");
		code_v2_("function f(integer x) { return x * 10 } return f(getSourceLine())").equals("10");
		code("return getSourceLine()").ops(1);
		code_v3_("getSourceLine(1)").error(Error.INVALID_PARAMETER_COUNT);
		// Prise comme valeur, la ligne de l'appel est cherchée dans la pile, bien plus cher (cf TestIncludeCache)
		code_v2_("var f = getSourceLine f()").ops(102);
		code("var f = getSourceLine return f()").error(Error.NONE);
		code_strict("var f = getSourceLine return f()").error(Error.NONE);
		// Une fonction, une variable ou une méthode du joueur du même nom reste prioritaire
		code("function getSourceLine() { return 'mine' } return getSourceLine()").equals("\"mine\"");
		code_strict("function getSourceLine() { return 'mine' } return getSourceLine()").equals("\"mine\"");
		code("function getSourceLine(x) { return x * 2 } return getSourceLine(21)").equals("42");
		code("var getSourceLine = 12 return getSourceLine").equals("12");
		code_strict("var getSourceLine = 12 return getSourceLine").equals("12");
		code("global getSourceLine = 7 return getSourceLine").equals("7");
		code("function f(getSourceLine) { return getSourceLine } return f(3)").equals("3");
		code_v1_3("getSourceLine = function() { return 'redefined' } return getSourceLine()").equals("\"redefined\"");
		code_v2_("class A { getSourceLine() { return 'method' } m() { return getSourceLine() } } return new A().m()").equals("\"method\"");
		code_v2_("class A { static getSourceLine() { return 'static' } static m() { return getSourceLine() } } return A.m()").equals("\"static\"");
	}
}
