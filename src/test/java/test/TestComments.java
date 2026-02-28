
package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestComments extends TestCommon {


	@Test
	public void run() throws Exception {

		section("Comments");
		code("// basic; return 12;").equals("null");
		code("/* basic; */ return 12;").equals("12");
		code_v1("/*// basic; */ return 12;").error(Error.OPERATOR_UNEXPECTED);
		code_v2_("/*// basic; */ return 12;").equals("12");
		code_v1("/*/ return 1").equals("1");

	}
}
