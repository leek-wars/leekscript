package test;

import leekscript.common.Error;

public class TestArrayStress extends TestCommon {

	public void run() throws Exception {

		long fight_max_ops = 14l * 64l * 20000000l;
		System.out.println("Fight max ops = " + fight_max_ops);

		/**
		 * Stress test
		 */
		section("Array stress test");
		code("var a = [] for (var i = 0; i < 100; ++i) push(a, i) return count(a)").max_ops(20000000).equals("100");
		code("var a = [] for (var i = 0; i < 1000; ++i) push(a, i) return count(a)").max_ops(20000000).equals("1000");
		code("var a = [] for (var i = 0; i < 100000; ++i) push(a, i) return count(a)").max_ops(20000000).equals("100000");
		code("var a = [] for (var i = 0; i < 500000; ++i) push(a, i) return count(a)").max_ops(fight_max_ops).equals("500000");
		code_v1_3("var a = [] for (var i = 0; i < 1000000; ++i) push(a, i) return count(a)").max_ops(20000000).error(Error.TOO_MUCH_OPERATIONS);
		code_v4_("var a = [] for (var i = 0; i < 1000000; ++i) push(a, i) return count(a)").max_ops(20000000).equals("1000000");
		code_v4_("var a = [] for (var i = 0; i < 2000000; ++i) push(a, i) return count(a)").max_ops(20000000).equals("2000000");
		code_v4_("var a = [] for (var i = 0; i < 9000000; ++i) push(a, i) return count(a)").max_ops(fight_max_ops).equals("9000000");
		code_v4_("var a = [] for (var i = 0; i < 100000000; ++i) push(a, i) return count(a)").error(Error.OUT_OF_MEMORY);
		code_v4_("var all = [] for (var j = 0; j < 20; ++j) { var a = [] for (var i = 0; i < 1000000; ++i) push(a, i) push(all, a) } return count(all)").error(Error.OUT_OF_MEMORY);
		code_v4_("for (var j = 0; j < 20; ++j) { var a = [] for (var i = 0; i < 1000000; ++i) push(a, i) } return 'ok'").equals("ok");
	}
}
