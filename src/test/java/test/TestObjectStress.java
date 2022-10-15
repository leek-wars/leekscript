package test;

import leekscript.common.Error;

public class TestObjectStress extends TestCommon {

	public void run() throws Exception {

		/**
		 * Stress test
		 */
		section("Object stress test general");
		code_v2_("var o = {} for (var i = 0; i < 100; ++i) o[i] = 1 return count(o.values())").max_ops(20000000).equals("100");
		code_v2_("var o = {} for (var i = 0; i < 1000; ++i) o[i] = 1 return count(o.values())").max_ops(20000000).equals("1000");
		code_v2_("var o = {} for (var i = 0; i < 100000; ++i) o[i] = 1 return count(o.values())").max_ops(20000000).equals("100000");
		code_v2_("var o = {} for (var i = 0; i < 500000; ++i) o[i] = 1 return count(o.values())").max_ops(999999999999l).equals("500000");
		code_v2_3("var o = {} for (var i = 0; i < 1000000; ++i) o[i] = 1 return count(o.values())").max_ops(20000000).error(Error.TOO_MUCH_OPERATIONS);
		code_v4_("var o = {} for (var i = 0; i < 1000000; ++i) o[i] = 1 return count(o.values())").max_ops(20000000).max_ram(1500000).error(Error.OUT_OF_MEMORY);
		code_v4_("for (var j = 0; j < 5; ++j) { var o = {} for (var i = 0; i < 1000000; ++i) o[i] = 1 } return 'ok'").equals("\"ok\"");

	}
}
