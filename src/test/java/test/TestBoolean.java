package test;

import leekscript.common.Error;

public class TestBoolean extends TestCommon {

	public void run() {

		section("Boolean");
		code_v1_2("return true").equals("true");
		code_v1_2("return True").equals("true");
		code_v1_2("return TRUE").equals("true");
		code_v3_("return true").equals("true");
		code_v3_("return True").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v3_("return TRUE").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);

		code_v1_2("return false").equals("false");
		code_v1_2("return False").equals("false");
		code_v1_2("return FALSE").equals("false");
		code_v3_("return false").equals("false");
		code_v3_("return False").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v3_("return FALSE").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);

		section("Boolean.operator !");
		code_v1_2("return not true").equals("false");
		code_v1_2("return not false").equals("true");
		code_v1_2("return Not true").equals("false");
		code_v1_2("return Not false").equals("true");
		code_v1_2("return NOT true").equals("false");
		code_v1_2("return NOT false").equals("true");
		code_v3_("return not true").equals("false");
		code_v3_("return not false").equals("true");
		code_v3_("return Not true").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return Not false").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return NOT true").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return NOT false").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
	}
}
