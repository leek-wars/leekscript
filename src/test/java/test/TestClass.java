package test;

public class TestClass extends TestCommon {

	public void run() {

		section("Class toBoolean");
		code_v2_("class A {} return !!A").equals("true");
		code_v2_("class A {} if (A) { return 12 } return null").equals("12");
	}
}
