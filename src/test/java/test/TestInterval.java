package test;

import leekscript.common.Error;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Interval.constructor()");
		code_v4_("return [1..2];").debug().equals("[1..2]");
		code_v4_("return [-10..-2];").debug().equals("[-10..-2]");
		code_v4_("return [1 * 5 .. 8 + 5];").debug().equals("[5..13]");
	}
}
