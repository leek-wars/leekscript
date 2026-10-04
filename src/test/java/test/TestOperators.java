package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;


import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestOperators extends TestCommon {


		@Test
	public void testOperator_EqualsEquals() throws Exception {
		section("Operator ==");
		code("return null == null").equals("true");

		code("return false == false").equals("true");
		code("return true == true").equals("true");
		code("return false == true").equals("false");
		code("return true == false").equals("false");
		code_v1_3("return true == 'true'").equals("true");
		code_v4_("return true == 'true'").equals("false");
		code_v1_3("return false == 'false'").equals("true");
		code_v4_("return false == 'false'").equals("false");
		code("return true == 'false'").equals("false");
		code("return false == 'true'").equals("false");
		code_v1_3("return false == []").equals("true");
		code_v4_("return false == []").equals("false");
		code_v1_3("return false == 0").equals("true");
		code_v4_("return false == 0").equals("false");
		code_v1_3("return true == 1").equals("true");
		code_v4_("return true == 1").equals("false");
		code_v1_3("return false != 0").equals("false");
		code_v4_("return false != 0").equals("true");
		code_v1_3("return true != 1").equals("false");
		code_v4_("return true != 1").equals("true");
		code_v1_3("return false == ''").equals("true");
		code_v4_("return false == ''").equals("false");
		code_v1_3("return false == '0'").equals("true");
		code_v4_("return false == '0'").equals("false");
		code_v1_3("return false == []").equals("true");
		code_v4_("return false == []").equals("false");
		code_v1_3("return false == [0]").equals("true");
		code_v4_("return false == [0]").equals("false");
		code_v1_3("return true == 12").equals("true");
		code_v4_("return true == 12").equals("false");
		code_v1_3("return true == '1'").equals("true");
		code_v4_("return true == '1'").equals("false");
		code_v1_3("return true == '12'").equals("true");
		code_v4_("return true == '12'").equals("false");
		code_v1_3("return true == 'lama'").equals("true");
		code_v4_("return true == 'lama'").equals("false");
		code_v1_3("return true == [1]").equals("true");
		code_v4_("return true == [1]").equals("false");
		code_v1_3("return true == [12]").equals("true");
		code_v4_("return true == [12]").equals("false");
		code_v1_3("return true == [1, 2, 3]").equals("true");
		code_v4_("return true == [1, 2, 3]").equals("false");

		code_v1_3("return 0 == false").equals("true");
		code_v4_("return 0 == false").equals("false");
		code("return 0 == 0").equals("true");
		code_v1_3("return 0 == ''").equals("true");
		code_v4_("return 0 == ''").equals("false");
		code_v1_3("return 0 == '0'").equals("true");
		code_v4_("return 0 == '0'").equals("false");
		code_v1_3("return 0 == 'false'").equals("true");
		code_v4_("return 0 == 'false'").equals("false");
		code_v1_3("return 0 == []").equals("true");
		code_v4_("return 0 == []").equals("false");
		code_v1_3("return 0 == [0]").equals("true");
		code_v4_("return 0 == [0]").equals("false");
		code("return 0 != null").equals("true");

		code_v1_3("return 1 == true").equals("true");
		code_v4_("return 1 == true").equals("false");
		code_v1_3("return 1 == '1'").equals("true");
		code_v4_("return 1 == '1'").equals("false");
		code_v1_3("return 1 == 'true'").equals("true");
		code_v4_("return 1 == 'true'").equals("false");
		code("return 1 == 'lama'").equals("false");
		code_v1_3("return 1 == [1]").equals("true");
		code_v4_("return 1 == [1]").equals("false");
		code("return 1 == 2").equals("false");

		code_v1_3("return 12 == true").equals("true");
		code_v4_("return 12 == true").equals("false");
		code("return -1 == -5").equals("false");
		code("return 50 == 50").equals("true");
		code("return 5 == 5").equals("true");
		code("return 45 == 5").equals("false");
		code_v1_3("return 10 == '10'").equals("true");
		code_v4_("return 10 == '10'").equals("false");
		code("return 10 == '15'").equals("false");
		code("return 10 == '15'").equals("false");
		code_v1_3("return 10.8 == '10.8'").equals("true");
		code_v4_("return 10.8 == '10.8'").equals("false");
		code("return 10.8 == '10.87'").equals("false");
		code_v1_3("return 12 == 'true'").equals("true");
		code_v4_("return 12 == 'true'").equals("false");
		code("return 2 == 'false'").equals("false");
		code_v1_3("return 12 == [12]").equals("true");
		code_v4_("return 12 == [12]").equals("false");

		code("return 'Chaine1' == 'Chaine1'").equals("true");
		code("return 'Chaine1' == 'Chaine2'").equals("false");
		code_v1_3("return '1' == 1").equals("true");
		code_v4_("return '1' == 1").equals("false");
		code_v1_3("return '0' == 0").equals("true");
		code_v4_("return '0' == 0").equals("false");
		code_v1_3("return '10' == 10").equals("true");
		code_v4_("return '10' == 10").equals("false");
		code("return '15' == 10").equals("false");

		code("return [] == []").equals("true");
		code("return [0] == [0]").equals("true");
		code("return [0, 1] == [0, 1]").equals("true");
		code("return [0, 1] == [0]").equals("false");
		code("return ['Chaine1'] == ['Chaine2']").equals("false");
		code("return ['Chaine1'] == ['Chaine1']").equals("true");

		code("return function() {} == function() {}").equals("false");
		code("return endsWith == function() {}").equals("false");
		code("return endsWith == endsWith").equals("true");
		code("function test() {} return test == test").equals("true");
		code("function a() {} function b() {} return a == b").equals("false");

		String[] values1 = new String[] { "false", "true", "0", "1", "12", "''", "'0'", "'1'", "'12'", "'lama'", "'true'", "'false'",
			"[]", "[0]", "[1]", "[12]", "[1,2,3]", "null" };
		String[] equalEqual = new String[] {
			"X X  XX    XXX    ", // false
			" X XX  XXXX   XXX ", // true
			"X X  XX    XXX    ", // 0
			" X X   X  X   X   ", // 1
			" X  X   X X    X  ", // 12
			"X X  X      XX    ", // " "
			"X X   X      X    ", // "0"
			" X X   X      X   ", // "1"
			" X  X   X      X  ", // "12"
			" X       X        ", // "lama"
			" X XX     X   XXX ", // "true"
			"X X        XXX    ", // "false"
			"X X  X     XX     ", // []
			"X X  XX    X X    ", // [0]
			" X X   X  X   X   ", // [1]
			" X  X   X X    X  ", // [12]
			" X        X     X ", // [1,2,3]
			"                 X", // null
		};

		for (int i = 0; i < values1.length; i++) {
			for (int j = 0; j < values1.length; j++) {
				code_v1_3("return " + values1[i] + " == " + values1[j]).equals(String.valueOf(equalEqual[i].charAt(j) == 'X'));
				code_v4_("return " + values1[i] + " == " + values1[j]).equals(String.valueOf(i == j));
			}
		}
	}

	@Test
	public void testOther_operators() throws Exception {
		section("Other operators");
		code("var sum = 1, ops = 10 return sum < ops * 0.95 || sum > ops").equals("true");
		code("var sum = 9.8, ops = 10 return sum < ops * 0.95 || sum > ops").equals("false");
		code("var sum = 98 var ops = 100 return sum < ops * 0.95 || sum > ops").equals("false");
		code("var sum = 98 var ops = 100 if (sum < ops * 0.95 || sum > ops) {}").equals("null");
		code("var sum = 1 var ops = 10 if (sum < ops * 0.95 || sum > ops) {} return null").equals("null");
		code("var a = [] if (a != []) {}").equals("null");
		code_v1_3("return !null == 50").equals("true");
		code_v4_("return !null == 50").equals("false");
	}

	@Test
	public void testOperator_EqualsEqualsEquals() throws Exception {
		section("Operator ===");
		Object[] values = new Object[] { "0", "1",
			"12", "13", "false",
			"true", "null", "'true'",
			"'false'", "'12'",
			"'lama'",
			"[]", "['12']" };

		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values.length; j++) {
				code("return " + values[i] + " === " + values[j]).equals(String.valueOf(i == j));
			}
		}
		code("return 1 === 1.0").equals("true");
		code("return 12 === 12.0").equals("true");
		code("return null == [null]").equals("false");
		code("return null != [null]").equals("true");
		code_v1("return [null] == null").equals("true"); // Bug in LS1.0
		code_v2_("return [null] == null").equals("false"); // Fixed in 1.1
		code_v1("return [null] != null").equals("false"); // Bug in LS1.0
		code_v2_("return [null] != null").equals("true"); // Fixed in 1.1

		code("var a = 1; var result = -10 + (1- (a-1)); return result").equals("-9");
		code("var a = 1; var result = 0; result = -10 + (1- (a-1)); return result").equals("-9");

		code("return null < 3").equals("true");
		code("var a = null return a < 3").equals("true");
		code("return true < 10").equals("true");
		code("return false < 10").equals("true");
		code("return 10 < true").equals("false");
		code("return 10 < false").equals("false");
		code("return 10 > true").equals("true");
		code("return 10 > false").equals("true");
		code("return true > 10").equals("false");
		code("return false > 10").equals("false");

		code("var a = 20 if (15 > a > 11) { return true } return false").equals("false");
		code("return 15 > 14 > 11 and 150 < 200 < 250").equals("false");
		code("return 15 > 10 > 11 and 150 < 200 < 250").equals("false");
		code("return 15 > 14 > 11 and 150 < 100 < 250").equals("false");
		code("return 15 > 10 > 11 and 150 < 100 < 250").equals("false");
	}

	@Test
	public void testOperator_Plus() throws Exception {
		section("Operator +");
		code("return false + 1").equals("1");
		code("return 1 + false").equals("1");
		code("return true + 1").equals("2");
		code("return 1 + true").equals("2");
		code("return true + null").equals("1");
		code("return null + true").equals("1");
		code("return false + null").equals("0");
		code("return null + false").equals("0");
	}

	@Test
	public void testAssignment_to_itself() throws Exception {
		section("Assignment to itself");
		// TODO
		// code("var x = 2 x = x").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { x constructor(z) { x = x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { x constructor(z) { this.x = x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { x constructor(z) { this.x = this.x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { x constructor(z) { x = this.x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { static x constructor(z) { x = x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { static x constructor(z) { class.x = x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { static x constructor(z) { class.x = class.x } }").warning(Error.ASSIGN_SAME_VARIABLE);
		// code_v2_("class A { static x constructor(z) { x = class.x } }").warning(Error.ASSIGN_SAME_VARIABLE);
	}

	@Test
	public void testComparison_always_false() throws Exception {
		section("Comparison always false");
		code_v4_("5.5 == true").warning(Error.COMPARISON_ALWAYS_FALSE);
		code_v4_("count([]) == 'hello'").warning(Error.COMPARISON_ALWAYS_FALSE);
		code_v4_("[:] == {}").warning(Error.COMPARISON_ALWAYS_FALSE);
		code_v4_("(function() {}) == null").warning(Error.COMPARISON_ALWAYS_FALSE);
		code_v4_("Array == 12").warning(Error.COMPARISON_ALWAYS_FALSE);
	}

	@Test
	public void testComparison_always_true() throws Exception {
		section("Comparison always true");
		code_v4_("5.5 != true").warning(Error.COMPARISON_ALWAYS_TRUE);
		code_v4_("count([]) != 'hello'").warning(Error.COMPARISON_ALWAYS_TRUE);
		code_v4_("[:] != {}").warning(Error.COMPARISON_ALWAYS_TRUE);
		code_v4_("(function() {}) != null").warning(Error.COMPARISON_ALWAYS_TRUE);
		code_v4_("Array != 12").warning(Error.COMPARISON_ALWAYS_TRUE);
	}

	@Test
	public void testUnknown_operator() throws Exception {
		section("Unknown operator");
		code("'salut' - 2").warning(Error.UNKNOWN_OPERATOR);
		code("2 / [1, 2, 3]").warning(Error.UNKNOWN_OPERATOR);
		code_v3_("{} % 5").warning(Error.UNKNOWN_OPERATOR);
		// Un booléen vaut 0 ou 1, comme à l'exécution
		var arithmetic = code_v4_("return [1 * true, 1 / true, 1 - true, 2 ** true, 5 % true, true * false]");
		arithmetic.noWarning();
		arithmetic.equals("[1, 1.0, 0, 2, 0, 0]");
		var bitsAndComparisons = code_v4_("return [true < 2, true >= 2, true & 3, 2 | true, true << 2, 8 >> true]");
		bitsAndComparisons.noWarning();
		bitsAndComparisons.equals("[true, false, 1, 3, 4, 4]");
		code_v4_("boolean? b = true; return 2 * b").noWarning();
		code_strict_v4_("integer x = 3 * true; return x").noWarning();
		code_v4_("'salut' * true").warning(Error.UNKNOWN_OPERATOR);
	}

	@Test
	public void testAssignment_operators() throws Exception {
		section("Assignment operators");
		Object[] values2 = new Object[] {
			"null", "true", "false", "12", "5.678", "false", "true", "'lama'", "[1, 2, 3, 4, 5]"
		};
		Object[] operators = new Object[] {
			"+", "-", "*", "/", "%", "&", "^", "|", "<<", ">>", ">>>", "**",
		};
		Object[] assignmentOperators = new Object[] {
			"+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "<<=", ">>=", ">>>=", "**=",
		};
		String[] actualResults = new String[] {
			"0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "1", "-1", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "1", "-1", "0", "0", "0", "0", "0", "1", "0", "0", "0", "0", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "12", "-12", "0", "0", "0", "0", "12", "12", "0", "0", "0", "0", "12", "-12", "0", "0", "0", "0", "0", "12", "0", "0", "0", "0", "5,678", "-5,678", "0", "0", "0", "0", "5", "5", "0", "0", "0", "0", "5,678", "-5,678", "0", "0", "0", "0", "0", "5", "0", "0", "0", "0", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "1", "-1", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "1", "-1", "0", "0", "0", "0", "0", "1", "0", "0", "0", "0", "\"nulllama\"", "-4", "0", "0", "0", "0", "4", "4", "0", "0", "0", "0", "\"nulllama\"", "-4", "0", "0", "0", "0", "0", "4", "0", "0", "0", "0", "[null, 1, 2, 3, 4, 5]", "-5", "0", "0", "0", "0", "5", "5", "0", "0", "0", "0", "[null, 1, 2, 3, 4, 5]", "-5", "0", "0", "0", "0", "0", "5", "0", "0", "0", "0", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "2", "0", "1", "1", "0", "1", "0", "1", "2", "0", "0", "1", "2", "0", "1", "1", "0", "1", "1", "1", "2", "0", "0", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "13", "-11", "12", "0,083", "1", "0", "13", "13", "4096", "0", "0", "1", "13", "-11", "12", "0,083", "1", "0", "1", "13", "4096", "0", "0", "1", "6,678", "-4,678", "5,678", "0,176", "1", "1", "4", "5", "32", "0", "0", "1", "6,678", "-4,678", "5,678", "0,176", "1", "1", "1", "5", "32", "0", "0", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "2", "0", "1", "1", "0", "1", "0", "1", "2", "0", "0", "1", "2", "0", "1", "1", "0", "1", "1", "1", "2", "0", "0", "1", "\"truelama\"", "-3", "4", "0,25", "1", "0", "5", "5", "16", "0", "0", "1", "\"truelama\"", "-3", "4", "0,25", "1", "0", "1", "5", "16", "0", "0", "1", "[true, 1, 2, 3, 4, 5]", "-4", "5", "0,2", "1", "1", "4", "5", "32", "0", "0", "1", "[true, 1, 2, 3, 4, 5]", "-4", "5", "0,2", "1", "1", "1", "5", "32", "0", "0", "1", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "1", "-1", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "1", "-1", "0", "0", "0", "0", "0", "1", "0", "0", "0", "0", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "12", "-12", "0", "0", "0", "0", "12", "12", "0", "0", "0", "0", "12", "-12", "0", "0", "0", "0", "0", "12", "0", "0", "0", "0", "5,678", "-5,678", "0", "0", "0", "0", "5", "5", "0", "0", "0", "0", "5,678", "-5,678", "0", "0", "0", "0", "0", "5", "0", "0", "0", "0", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "1", "-1", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "1", "-1", "0", "0", "0", "0", "0", "1", "0", "0", "0", "0", "\"falselama\"", "-4", "0", "0", "0", "0", "4", "4", "0", "0", "0", "0", "\"falselama\"", "-4", "0", "0", "0", "0", "0", "4", "0", "0", "0", "0", "[false, 1, 2, 3, 4, 5]", "-5", "0", "0", "0", "0", "5", "5", "0", "0", "0", "0", "[false, 1, 2, 3, 4, 5]", "-5", "0", "0", "0", "0", "0", "5", "0", "0", "0", "0", "12", "12", "0", "null", "null", "0", "12", "12", "12", "12", "12", "1", "12", "12", "0", "null", "null", "0", "1", "12", "12", "12", "12", "1", "13", "11", "12", "12", "0", "0", "13", "13", "24", "6", "6", "12", "13", "11", "12", "12", "0", "0", "12", "13", "24", "6", "6", "12", "12", "12", "0", "null", "null", "0", "12", "12", "12", "12", "12", "1", "12", "12", "0", "null", "null", "0", "1", "12", "12", "12", "12", "1", "24", "0", "144", "1", "0", "12", "0", "12", "49152", "0", "0", "8916100448256", "24", "0", "144", "1", "0", "12", "8916100448256", "12", "49152", "0", "0", "8916100448256", "17,678", "6,322", "68,136", "2,113", "0,644", "4", "9", "13", "384", "0", "0", "1 341 501,353", "17,678", "6,322", "68,136", "2,113", "0,644", "4", "1 341 501,353", "13", "384", "0", "0", "1 341 501,353", "12", "12", "0", "null", "null", "0", "12", "12", "12", "12", "12", "1", "12", "12", "0", "null", "null", "0", "1", "12", "12", "12", "12", "1", "13", "11", "12", "12", "0", "0", "13", "13", "24", "6", "6", "12", "13", "11", "12", "12", "0", "0", "12", "13", "24", "6", "6", "12", "\"12lama\"", "8", "48", "3", "0", "4", "8", "12", "192", "0", "0", "20736", "\"12lama\"", "8", "48", "3", "0", "4", "20736", "12", "192", "0", "0", "20736", "[12, 1, 2, 3, 4, 5]", "7", "60", "2,4", "2", "4", "9", "13", "384", "0", "0", "248832", "[12, 1, 2, 3, 4, 5]", "7", "60", "2,4", "2", "4", "248832", "13", "384", "0", "0", "248832", "5,678", "5,678", "0", "null", "NaN", "0", "5", "5", "5", "5", "5", "1", "5,678", "5,678", "0", "null", "NaN", "0", "1", "5", "5", "5", "5", "1", "6,678", "4,678", "5,678", "5,678", "0,678", "1", "4", "5", "10", "2", "2", "5,678", "6,678", "4,678", "5,678", "5,678", "0,678", "1", "5,678", "5", "10", "2", "2", "5,678", "5,678", "5,678", "0", "null", "NaN", "0", "5", "5", "5", "5", "5", "1", "5,678", "5,678", "0", "null", "NaN", "0", "1", "5", "5", "5", "5", "1", "17,678", "-6,322", "68,136", "0,473", "5,678", "4", "9", "13", "20480", "0", "0", "1 122 909 247,194", "17,678", "-6,322", "68,136", "0,473", "5,678", "4", "1 122 909 247,194", "13", "20480", "0", "0", "1 122 909 247,194", "11,356", "0", "32,24", "1", "0", "5", "0", "5", "160", "0", "0", "19 156,732", "11,356", "0", "32,24", "1", "0", "5", "19 156,732", "5", "160", "0", "0", "19 156,732", "5,678", "5,678", "0", "null", "NaN", "0", "5", "5", "5", "5", "5", "1", "5,678", "5,678", "0", "null", "NaN", "0", "1", "5", "5", "5", "5", "1", "6,678", "4,678", "5,678", "5,678", "0,678", "1", "4", "5", "10", "2", "2", "5,678", "6,678", "4,678", "5,678", "5,678", "0,678", "1", "5,678", "5", "10", "2", "2", "5,678", "\"5,678lama\"", "1,678", "22,712", "1,419", "1,678", "4", "1", "5", "80", "0", "0", "1 039,397", "\"5,678lama\"", "1,678", "22,712", "1,419", "1,678", "4", "1 039,397", "5", "80", "0", "0", "1 039,397", "[5,678, 1, 2, 3, 4, 5]", "0,678", "28,39", "1,136", "0,678", "5", "0", "5", "160", "0", "0", "5 901,697", "[5,678, 1, 2, 3, 4, 5]", "0,678", "28,39", "1,136", "0,678", "5", "5 901,697", "5", "160", "0", "0", "5 901,697", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "1", "-1", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "1", "-1", "0", "0", "0", "0", "0", "1", "0", "0", "0", "0", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "12", "-12", "0", "0", "0", "0", "12", "12", "0", "0", "0", "0", "12", "-12", "0", "0", "0", "0", "0", "12", "0", "0", "0", "0", "5,678", "-5,678", "0", "0", "0", "0", "5", "5", "0", "0", "0", "0", "5,678", "-5,678", "0", "0", "0", "0", "0", "5", "0", "0", "0", "0", "0", "0", "0", "null", "null", "0", "0", "0", "0", "0", "0", "1", "0", "0", "0", "null", "null", "0", "1", "0", "0", "0", "0", "1", "1", "-1", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "1", "-1", "0", "0", "0", "0", "0", "1", "0", "0", "0", "0", "\"falselama\"", "-4", "0", "0", "0", "0", "4", "4", "0", "0", "0", "0", "\"falselama\"", "-4", "0", "0", "0", "0", "0", "4", "0", "0", "0", "0", "[false, 1, 2, 3, 4, 5]", "-5", "0", "0", "0", "0", "5", "5", "0", "0", "0", "0", "[false, 1, 2, 3, 4, 5]", "-5", "0", "0", "0", "0", "0", "5", "0", "0", "0", "0", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "2", "0", "1", "1", "0", "1", "0", "1", "2", "0", "0", "1", "2", "0", "1", "1", "0", "1", "1", "1", "2", "0", "0", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "13", "-11", "12", "0,083", "1", "0", "13", "13", "4096", "0", "0", "1", "13", "-11", "12", "0,083", "1", "0", "1", "13", "4096", "0", "0", "1", "6,678", "-4,678", "5,678", "0,176", "1", "1", "4", "5", "32", "0", "0", "1", "6,678", "-4,678", "5,678", "0,176", "1", "1", "1", "5", "32", "0", "0", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "1", "1", "0", "null", "null", "0", "1", "1", "1", "1", "1", "1", "2", "0", "1", "1", "0", "1", "0", "1", "2", "0", "0", "1", "2", "0", "1", "1", "0", "1", "1", "1", "2", "0", "0", "1", "\"truelama\"", "-3", "4", "0,25", "1", "0", "5", "5", "16", "0", "0", "1", "\"truelama\"", "-3", "4", "0,25", "1", "0", "1", "5", "16", "0", "0", "1", "[true, 1, 2, 3, 4, 5]", "-4", "5", "0,2", "1", "1", "4", "5", "32", "0", "0", "1", "[true, 1, 2, 3, 4, 5]", "-4", "5", "0,2", "1", "1", "1", "5", "32", "0", "0", "1", "\"lamanull\"", "4", "0", "null", "null", "0", "4", "4", "4", "4", "4", "1", "\"lamanull\"", "4", "0", "null", "null", "0", "1", "4", "4", "4", "4", "1", "\"lamatrue\"", "3", "4", "4", "0", "0", "5", "5", "8", "2", "2", "4", "\"lamatrue\"", "3", "4", "4", "0", "0", "4", "5", "8", "2", "2", "4", "\"lamafalse\"", "4", "0", "null", "null", "0", "4", "4", "4", "4", "4", "1", "\"lamafalse\"", "4", "0", "null", "null", "0", "1", "4", "4", "4", "4", "1", "\"lama12\"", "-8", "48", "0,333", "4", "4", "8", "12", "16384", "0", "0", "16777216", "\"lama12\"", "-8", "48", "0,333", "4", "4", "16777216", "12", "16384", "0", "0", "16777216", "\"lama5,678\"", "-1,678", "22,712", "0,704", "4", "4", "1", "5", "128", "0", "0", "2 621,179", "\"lama5,678\"", "-1,678", "22,712", "0,704", "4", "4", "2 621,179", "5", "128", "0", "0", "2 621,179", "\"lamafalse\"", "4", "0", "null", "null", "0", "4", "4", "4", "4", "4", "1", "\"lamafalse\"", "4", "0", "null", "null", "0", "1", "4", "4", "4", "4", "1", "\"lamatrue\"", "3", "4", "4", "0", "0", "5", "5", "8", "2", "2", "4", "\"lamatrue\"", "3", "4", "4", "0", "0", "4", "5", "8", "2", "2", "4", "\"lamalama\"", "0", "16", "1", "0", "4", "0", "4", "64", "0", "0", "256", "\"lamalama\"", "0", "16", "1", "0", "4", "256", "4", "64", "0", "0", "256", "\"lama[1, 2, 3, 4, 5]\"", "-1", "20", "0,8", "4", "4", "1", "5", "128", "0", "0", "1024", "\"lama[1, 2, 3, 4, 5]\"", "-1", "20", "0,8", "4", "4", "1024", "5", "128", "0", "0", "1024", "[1, 2, 3, 4, 5, null]", "5", "0", "null", "null", "0", "5", "5", "5", "5", "5", "1", "[1, 2, 3, 4, 5, null]", "5", "0", "null", "null", "0", "1", "5", "5", "5", "5", "1", "[1, 2, 3, 4, 5, true]", "4", "5", "5", "0", "1", "4", "5", "10", "2", "2", "5", "[1, 2, 3, 4, 5, true]", "4", "5", "5", "0", "1", "5", "5", "10", "2", "2", "5", "[1, 2, 3, 4, 5, false]", "5", "0", "null", "null", "0", "5", "5", "5", "5", "5", "1", "[1, 2, 3, 4, 5, false]", "5", "0", "null", "null", "0", "1", "5", "5", "5", "5", "1", "[1, 2, 3, 4, 5, 12]", "-7", "60", "0,417", "5", "4", "9", "13", "20480", "0", "0", "244140625", "[1, 2, 3, 4, 5, 12]", "-7", "60", "0,417", "5", "4", "244140625", "13", "20480", "0", "0", "244140625", "[1, 2, 3, 4, 5, 5,678]", "-0,678", "28,39", "0,881", "5", "5", "0", "5", "160", "0", "0", "9 305,757", "[1, 2, 3, 4, 5, 5,678]", "-0,678", "28,39", "0,881", "5", "5", "9 305,757", "5", "160", "0", "0", "9 305,757", "[1, 2, 3, 4, 5, false]", "5", "0", "null", "null", "0", "5", "5", "5", "5", "5", "1", "[1, 2, 3, 4, 5, false]", "5", "0", "null", "null", "0", "1", "5", "5", "5", "5", "1", "[1, 2, 3, 4, 5, true]", "4", "5", "5", "0", "1", "4", "5", "10", "2", "2", "5", "[1, 2, 3, 4, 5, true]", "4", "5", "5", "0", "1", "5", "5", "10", "2", "2", "5", "\"[1, 2, 3, 4, 5]lama\"", "1", "20", "1,25", "1", "4", "1", "5", "80", "0", "0", "625", "\"[1, 2, 3, 4, 5]lama\"", "1", "20", "1,25", "1", "4", "625", "5", "80", "0", "0", "625", "[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]", "0", "25", "1", "0", "5", "0", "5", "160", "0", "0", "3125", "[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]", "0", "25", "1", "0", "5", "3125", "5", "160", "0", "0", "3125"
		};
		List<String> results = new ArrayList<String>();
		int r = 0;
		String result;
		for (var value1 : values2) {
			for (var value2 : values2) {
				for (var operator : operators) {
					String expected = actualResults[r++];
					var c = code_v1("var a = " + value1 + " return a " + operator + " " + value2);
					if (expected.equals("error")) result = c.error(Error.INVALID_OPERATOR);
					else result = c.equals(expected);
					results.add("\"" + result + "\"");
				}
				for (var operator : assignmentOperators) {
					String expected = actualResults[r++];
					var c = code_v1("var a = " + value1 + " a " + operator + " " + value2 + " return a");
					if (expected.equals("error")) result = c.error(Error.INVALID_OPERATOR);
					else result = c.equals(expected);
					results.add("\"" + result + "\"");
				}
			}
		}
		System.out.println(results);
	}

	@Test
	public void testCompoundAssign_typedLocal_nonPrimitiveRHS() throws Exception {
		section("Typed local compound operators with any-typed RHS");
		// Regression: `*=` / `**=` on a typed local with a non-primitive RHS
		// produced `u_x = (Number) mul(u_x, ...)` which Java rejects when u_x is long.
		code_v2_("function f() { return 5; } integer x = 10; x += f(); return x;").equals("15");
		code_v2_("function f() { return 5; } integer x = 10; x -= f(); return x;").equals("5");
		code_v2_("function f() { return 5; } integer x = 10; x *= f(); return x;").equals("50");
		code_v2_("function f() { return 5; } real x = 10; x /= f(); return x;").equals("2.0");
		code_v2_("function f() { return 5; } integer x = 10; x %= f(); return x;").equals("0");
		code_v2_("function f() { return 5; } integer x = 2; x **= f(); return x;").equals("32");
		// Compound `Number` (integer|real) RHS via map value
		code_v2_("global cfg = ['k': 2, 'r': 1.5]; integer x = 10; x *= cfg['k']; return x;").equals("20");
		code_v2_("global cfg = ['k': 2, 'r': 1.5]; integer x = 2; x **= cfg['k']; return x;").equals("4");
	}

	@Test
	public void testIncrement_nonPrimitiveLocation() throws Exception {
		section("Increment on a location Java does not declare as a primitive number");
		// Regression: `x++` passe par add()/sub(), qui renvoient un Object. Sans conversion
		// vers le type déclaré, `integer | real x = 5; x++` émettait `u_x = add(u_x, 1l)` sur
		// un `Number u_x`, rejeté par javac : COMPILE_JAVA et l'IA ne compilait plus (#5052).
		code_v4_("integer | real x = 5; x++; return x;").equals("6");
		code_v4_("integer | real x = 5; x--; return x;").equals("4");
		code_v4_("integer | real x = 5; ++x; return x;").equals("6");
		code_v4_("integer | real x = 5; --x; return x;").equals("4");
		code_v4_("integer | real x = 5; return x++;").equals("5");
		code_v4_("integer | real x = 5; return x--;").equals("5");
		code_v4_("integer | real x = 5; return ++x;").equals("6");
		code_v4_("integer | real x = 5; return --x;").equals("4");
		code_v4_("integer | real x = 5.5; x++; return x;").equals("6.5");
		code_v4_("integer | real x = 5; return [x++, x++, x];").equals("[5, 6, 7]");

		section("Increment on a nullable local");
		code_v4_("integer | null x = 5; x++; return x;").equals("6");
		code_v4_("integer | null x = 5; return x--;").equals("5");
		code_v4_("real | null x = 5.5; ++x; return x;").equals("6.5");

		section("Increment on a Number global");
		code_v4_("integer | real y = 1; global x = y; x++; return x;").equals("2");
		code_v4_("integer | real y = 1; global x = y; return x--;").equals("1");
		code_v4_("integer | real y = 1; global x = y; return ++x;").equals("2");
		code_v4_("integer | real y = 1; global x = y; return --x;").equals("0");

		section("Increment on a typed field");
		// Récepteur implicite : seule forme qui passe par VariableType.FIELD, `this.v++`
		// étant un LeekObjectAccess avec ses propres helpers field_inc()
		code_v4_("class A { public integer | real v = 5; public m() { v++; return v } } return (new A()).m();").equals("6");
		code_v4_("class A { public integer | real v = 5; public m() { return v++ } } return (new A()).m();").equals("5");
		code_v4_("class A { public integer | real v = 5; public m() { return ++v } } return (new A()).m();").equals("6");
		code_v4_("class A { public integer | real v = 5; public m() { return --v } } return (new A()).m();").equals("4");
		// Un champ déclaré `long` / `double` en Java (TestObject couvre la matrice complète)
		code_v4_("class A { public integer v = 5; public m() { v++; return v } } return (new A()).m();").equals("6");
		code_v4_("class A { public real v = 5.5; public m() { return --v } } return (new A()).m();").equals("4.5");
		// Sur un champ déclaré `Number`, le récepteur explicite passe par LeekObjectAccess
		code_v4_("class A { public integer | real v = 5; public m() { return this.v++ } } return (new A()).m();").equals("5");
		code_v4_("class A { public integer | real v = 5; public m() { return ++this.v } } return (new A()).m();").equals("6");
		// big_integer : couvert par TestBigInt#testIncrementDecrement, dont la conversion
		// passe maintenant par openResultConversion (BigIntegerValue.valueOf)
	}

	@Test
	public void testIncrement_narrowedVariable() throws Exception {
		section("Increment on a narrowed variable");
		// Narrowée vers integer/real mais déclarée `Object` en Java : le chemin rapide
		// `u_x++` donnait « bad operand type Object for unary operator '++' », soit le
		// même COMPILE_JAVA que #5052. Même garde que compileAddEq.
		code_v4_("Array | integer x = 5; if (x instanceof Array) { return 0 } else { x++; return x }").equals("6");
		code_v4_("Array | integer x = 5; if (x instanceof Array) { return 0 } else { x--; return x }").equals("4");
		code_v4_("Array | integer x = 5; if (x instanceof Array) { return 0 } else { return ++x }").equals("6");
		code_v4_("Array | integer x = 5; if (x instanceof Array) { return 0 } else { return --x }").equals("4");
		code_v4_("Array | real x = 5.5; if (x instanceof Array) { return 0 } else { x++; return x }").equals("6.5");
		// Témoin : `x += 1` sur le même code, déjà gardé
		code_v4_("Array | integer x = 5; if (x instanceof Array) { return 0 } else { x += 1; return x }").equals("6");
		// Narrowée vers integer mais déclarée `Long` : javac incrémente une boîte, le chemin
		// rapide reste donc valable (et c'est le moins cher)
		code_v4_("integer | null x = 5; if (x != null) { x++; return x } return 0;").equals("6");
		code_v4_("integer | null x = 5; if (x != null) { return x-- } return 0;").equals("5");
		code_v4_("real | null x = 5.5; if (x != null) { return ++x } return 0;").equals("6.5");

		section("Increment on a reference-narrowed variable");
		// Convertir vers le type narrowé ferait un IMPOSSIBLE_CAST, l'emplacement Java
		// restant déclaré `Object`
		code_v4_("Map | string x = [1: 2]; if (x instanceof Map) { return x++ } return 0;").equals("1");
		code_v4_("Map | string x = [1: 2]; if (x instanceof Map) { return ++x } return 0;").equals("2");
		code_v4_("class A { public Map | string v = [1: 2]; public m() { if (v instanceof Map) { return v++ } return 0 } } return (new A()).m();").equals("1");
		code_v4_("class A { public Map | string v = [1: 2]; public m() { if (v instanceof Map) { return ++v } return 0 } } return (new A()).m();").equals("2");
	}

	@Test
	public void testBitAssign_slotTypes() throws Exception {
		section("Bitwise compound operators on a real, a boolean or a narrowed location");
		// real : `double |= long` ne compilait pas
		code_v4_("real r = 12; r |= 1; return r;").equals("13.0");
		code_v4_("real r = 12.7; r <<= 1; return r;").equals("24.0");
		code_v4_("real? r = 12; r |= 1; return r;").equals("13.0");
		code_v4_("global real g = 12; g |= 1; return g;").equals("13.0");
		// boolean : garde la vérité du résultat, comme `this.f |= v`
		code_v4_("boolean b = false; b |= true; return b;").equals("true");
		code_v4_("boolean? b = true; b &= false; return b;").equals("false");
		code_v4_("boolean b = true; b \\= 1; return b;").equals("true");
		code_v4_("class A { boolean? f = true; m() { f |= 1; return f; } } return new A().m();").equals("true");
		code_v4_("global boolean? g = false; g ^= 1; return g;").equals("true");
		code_strict_v4_("var b = true; b |= false; return b;").equals("true");
		// Narrowé vers integer sur un emplacement Object ou Number : pas d'opérateur natif
		code_v4_("integer | real x = 12; if (x instanceof Integer) { x |= 1 } return x;").equals("13");
		code_v4_("integer | real x = 12; if (x instanceof Integer) { x <<= 2 } return x;").equals("48");
		code_v4_("any x = 12; if (x instanceof Integer) { x |= 1 } return x;").equals("13");
		code_v4_("integer | string x = 12; if (x instanceof Integer) { x |= 1 } return x;").equals("13");
		code_v4_("global integer | real g = 12; if (g instanceof Integer) { g |= 1 } return g;").equals("13");
		code_v4_("function f(integer | real x) { if (x instanceof Integer) { x &= 4 } return x; } return f(12);").equals("4");
		code_v4_("integer | real x = 12; if (x instanceof Integer) { x %= 5 } return x;").equals("2");
		code_v4_("any x = 12; if (x instanceof Integer) { x %= 5 } return x;").equals("2");
		// Non-régression : integer natif, integer? narrowé
		code_v4_("integer x = 12; x |= 1; return x;").equals("13");
		code_v4_("integer? x = 12; if (x != null) { x |= 1 } return x;").equals("13");
	}

	@Test
	public void testDivAssign_nullableInteger() throws Exception {
		section("Operator /= on a nullable integer: `(Long) div(...)` did not compile");
		code_v4_("integer? h = 12; h /= 2; return h;").equals("6");
		code_v4_("integer? h = null; h /= 2; return h;").equals("0");
		code_v4_("class A { integer? f = 12; m() { f /= 2; return f; } } return new A().m();").equals("6");
		code_v4_("global integer? g = 12; g /= 2; return g;").equals("6");
		code_v4_("real? r = 3; r /= 2; return r;").equals("1.5");
	}

	@Test
	public void testCompoundAssign_nullableNumber() throws Exception {
		section("Compound operators on a nullable number: the result is never null");
		code_v4_("real? r = null; r += 5; return r;").equals("5.0");
		code_v4_("real? r = null; r -= 5; return r;").equals("-5.0");
		code_v4_("real? r = null; r++; return r;").equals("1.0");
		code_v4_("real? r = 1.5; r += 1; return r;").equals("2.5");
		code_v4_("integer? h = 3; h += 1.5; return h;").equals("4");
		code_v4_("integer? h = 3; h *= 1.5; return h;").equals("4");
		code_v4_("class A { real? f = null; m() { f += 5; return f; } } return new A().m();").equals("5.0");
		code_v4_("global real? g = null; g += 5; return g;").equals("5.0");
		code_strict_v4_("class A { integer? f = 3; m() { f += 1.5; return f; } } return new A().m();").equals("4");
		// Non-régression : opérande du même type, cast inchangé
		code_v4_("integer? h = 3; h += 2; return h;").equals("5");
		code_v4_("integer? h = null; h++; return h;").equals("1");
	}

	@Test
	public void testCompoundAssign_boolean() throws Exception {
		section("Arithmetic compound operators on a boolean (non strict): the truth of the result");
		code_v4("boolean b = true; b += 1; return b;").equals("true");
		code_v4("boolean b = true; b -= 1; return b;").equals("false");
		code_v4("boolean b = true; b *= 0; return b;").equals("false");
		code_v4("boolean b = true; b++; return b;").equals("true");
		code_v4("boolean? b = true; b += 1; return b;").equals("true");
		code_v4("class A { boolean f = true; m() { f += 1; return f; } } return new A().m();").equals("true");
		code_v4("global boolean g = true; g += 1; return g;").equals("true");
		// Narrowé vers boolean sur un emplacement any : le nombre y est rangé tel quel
		code_v4_("any x = true; if (x instanceof Boolean) { x += 1 } return x;").equals("2");
		// Refusé à l'analyse en strict
		code_strict_v4_("boolean b = true; b += 1; return b;").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
	}

	@Test
	public void testBitAssign_untypedContainers() throws Exception {
		section("Bitwise compound operators on a typed array cell, static field or captured variable");
		code_v4_("Array<boolean> a = [false]; a[0] |= true; return a[0];").equals("true");
		code_v4_("Array<boolean> a = [true]; a[0] &= false; return a[0];").equals("false");
		code_v4_("Array<boolean> a = [false, false]; var i = 0; a[i++] |= true; return [a, i];").equals("[[true, false], 1]");
		// Hors bornes : erreur journalisée sans interrompre l'IA, comme une case integer
		code_v4_("Array<boolean> a = [false]; a[5] |= true; return a;").equals("[false]");
		code_v4_("Array<integer> a = [1]; a[5] |= 1; return a;").equals("[1]");
		code_v4_("Map<string, boolean> m = ['k': false]; m['k'] |= true; return m['k'];").equals("true");
		code_v4_("Array<real> a = [1.5]; a[0] |= 1; return a[0];").equals("1.0");
		code_v4_("class A { static boolean S = false; static m() { S |= true; return S; } } return A.m();").equals("true");
		code_v4_("class A { static boolean? S = false; static m() { S ^= true; return S; } } return A.m();").equals("true");
		code_v4_("class A { static real? f = 1.5; static m() { f |= 1; return f; } } return A.m();").equals("1.0");
		code_v4_("class A { static boolean S = false; } A.S |= true; return A.S;").equals("true");
		code_v4_("class A { static boolean S = false; } class B extends A {} B.S |= true; return A.S;").equals("true");
		code_v4_("class A { static boolean S = false; } class B extends A { static m() { S |= true; return S; } } return B.m();").equals("true");
		code_v4_("class A { static boolean S = false; static m() { class.S |= true; return class.S; } } return A.m();").equals("true");
		code_v4_("boolean b = false; var g = function() { b |= true; }; g(); return b;").equals("true");
		code_v4_("real r = 1.5; var g = function() { r |= 1; }; g(); return r;").equals("1.0");
		// Non-régression : integer et big_integer gardent les helpers du runtime
		code_v4_("Array<integer> a = [12]; a[0] |= 1; return a[0];").equals("13");
		code_v4_("class A { static integer f = 12; static m() { f |= 1; return f; } } return A.m();").equals("13");
		code_v4_("integer h = 12; var g = function() { h |= 1; }; g(); return h;").equals("13");
		code_v4_("class A { boolean f = false; m() { this.f |= true; return this.f; } } return new A().m();").equals("true");
	}

}
