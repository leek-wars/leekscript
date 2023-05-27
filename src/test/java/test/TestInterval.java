package test;

import leekscript.common.Error;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Interval.constructor()");
		code_v5_("return [1..2];").equals("[1..2]");
		code_v5_("return [-10..-2];").equals("[-10..-2]");
		code_v5_("return [1 * 5 .. 8 + 5];").equals("[5..13]");
	}
}
