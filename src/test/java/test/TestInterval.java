package test;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Interval.constructor()");
		code_v4_("return [1..2];").equals("[1..2]");
		code_v4_("return [-10..-2];").equals("[-10..-2]");
		code_v4_("return [1 * 5 .. 8 + 5];").equals("[5..13]");

		section("Interval.in");
		code_v4_("return 1 in [1..2];").equals("true");
		code_v4_("return 1 in [0..2];").equals("true");
		code_v4_("return 2 in [1..2];").equals("true");
		code_v4_("return 3 in [1..2];").equals("false");
		code_v4_("return 0 in [1..2];").equals("false");
		code_v4_("return 1 in [1..1];").equals("true");
		code_v4_("return 1 in [2..1];").equals("false");
		code_strict_v4_("boolean x = 1 in [1..1]; return x").equals("true");

		section("Interval typing");
		code_strict_v4_("Interval i = [0..10]; return i instanceof Interval").equals("true");
	}
}
