package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestClass extends TestCommon {


	@Test
	public void run() throws Exception {

		section("Class toBoolean");
		code_v2_("class A {} return !!A").equals("true");
		code_v2_("class A {} if (A) { return 12 } return null").equals("12");

		section("Class and variable");
		code_v2_("class A {} var A = 12;").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("var A = 12; class A {}").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A {} function f(A) { A = 1 return A } return f(2)").equals("1");
		code_v2_("class A {} function f() { var A = 1 return A } return f()").equals("1");
		code_v2_("class A {} A = 12").error(Error.CANT_ASSIGN_VALUE);

		section("Class.name");
		code_v2_("class A { public m() { return name }} return new A().m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2_("class A { public m() { return class.name }} return new A().m()").equals("\"A\"");
		code_v2_("class A { public static m() { return name }} return A.m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2_("class A { public static m() { return class.name }} return A.m()").equals("\"A\"");

	}
}
