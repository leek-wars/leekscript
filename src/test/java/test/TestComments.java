

package test;

public class TestComments extends TestCommon {

	public void run() throws Exception {

		section("Comments");
		code("// basic; return 12;").equals("null");
		code("/* basic; */ return 12;").equals("12");
		code_v10("/*// basic; */ return 12;").error();
		code_v11("/*// basic; */ return 12;").equals("12");
	}
}
