package test;

import leekscript.common.Error;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Construct interval");
		code_v4_("return [1..3]").equals("[1, 2, 3]");
		code_v4_("return [1..3, -5]").equals("[1, 2, 3, -5]");
		code_v4_("return [-5, 1..3]").equals("[-5, 1, 2, 3]");
		code_v4_("return [1..3, 5..7]").equals("[1, 2, 3, 5, 6, 7]");
		code_v4_("return [-2..2]").equals("[-2, -1, 0, 1, 2]");
		code_v4_("return [2..-2]").equals("[]");
		code_v4_("return [10..10]").equals("[10]");
		code_v4_("return [10..10, 10]").equals("[10, 10]");
		code_v4_("return [10..10, 10, 10..10]").equals("[10, 10, 10]");

		section("Construct interval from expression");
		code_v4_("var a = 1 return [a..3]").equals("[1, 2, 3]");
		code_v4_("var a = 1 return [a..3, a]").equals("[1, 2, 3, 1]");
		code_v4_("var a = 1 return [a..a]").equals("[1]");
		code_v4_("var a = 2 return [a * 5..a * 7]").equals("[10, 11, 12, 13, 14]");
		code_v4_("return [{a: 12}.a..15]").equals("[12, 13, 14, 15]");
		code_v4_("return [1..[10, 1, 5][0]]").error(Error.NONE);

		section("Invalid interval syntax");
		code_v4_("return [1..]").error(Error.UNCOMPLETE_EXPRESSION);
		code_v4_("return [..1]").error(Error.VALUE_EXPECTED);
		code_v4_("return [1..2..3]").error(Error.VALUE_EXPECTED);
		code_v4_("return [1..2, 3..]").error(Error.UNCOMPLETE_EXPRESSION);
		code_v4_("return [1, ..5]").error(Error.VALUE_EXPECTED);
		code_v4_("return [1, 2..]").error(Error.UNCOMPLETE_EXPRESSION);

		section("Interval access");
		code_v4_("var a = [1..3] return a[0]").equals("1");
		code_v4_("var a = [1..3] return a[1]").equals("2");
		code_v4_("var a = [1..3] return a[2]").equals("3");
		code_v4_("var a = [1..3] return a[-1]").equals("3");
		code_v4_("return [[[1..2]]][0][0][0]").equals("1");
		code_v4_("return [[-5..-2][-1]..[2..3][1]]").equals("[-2, -1, 0, 1, 2, 3]");

		section("Interval available only in v4");
		code_v1_3("return [1..3]").error(Error.UNKNOWN_OPERATOR);

	}
}
