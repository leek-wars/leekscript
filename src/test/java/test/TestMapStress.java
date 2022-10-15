package test;

import leekscript.common.Error;

public class TestMapStress extends TestCommon {

	public void run() throws Exception {

		long fight_max_ops = 14l * 64l * 20000000l;
		System.out.println("Fight max ops = " + fight_max_ops);

		/**
		 * Stress test
		 */
		section("Map stress test general");
		code("var a = [0 : 1] for (var i = 0; i < 100; ++i) a[i] = 1").error(Error.NONE);
		code("var a = [0 : 1] for (var i = 0; i < 1000; ++i) a[i] = 1").error(Error.NONE);
		code("var a = [0 : 1] for (var i = 0; i < 100000; ++i) a[i] = 1").error(Error.NONE);
		code("var a = [0 : 1] for (var i = 0; i < 500000; ++i) a[i] = 1").error(Error.NONE);
		code_v1_3("var a = [0 : 1] for (var i = 0; i < 1000000; ++i) a[i] = 1").max_ops(20000000).error(Error.TOO_MUCH_OPERATIONS);
		code_v4_("var a = [:] for (var i = 0; i < 1000000; ++i) a[i] = 1").max_ops(20000000).error(Error.NONE);
		code_v4_("var a = [:] for (var i = 0; i < 900000; ++i) a[i] = 1").max_ops(fight_max_ops).error(Error.NONE);
		code_v4_("var a = [:] for (var i = 0; i < 100000000; ++i) a[i] = 1").error(Error.OUT_OF_MEMORY);
		code_v4_("var all = [] for (var j = 0; j < 20; ++j) { var a = [:] for (var i = 0; i < 100000; ++i) a[i] = 1 push(all, a) } return count(all)").max_ram(1000000).error(Error.OUT_OF_MEMORY);
		code_v4_("for (var j = 0; j < 20; ++j) { var a = [:] for (var i = 0; i < 100000; ++i) a[i] = 1 } return 'ok'").max_ram(1000000).equals("\"ok\"");

		/**
		 * Stress test methods
		 */
		long low_ram = 10_000;
		code_v4_("var a = [:] for (var i = 0; i < 1000000; ++i) a[i] = 1").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
		code_v4_("var a = [:] for (var i = 0; i < 1000000; ++i) mapPut(a, i, 1)").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
		code_v4_("var a = [:] for (var i = 0; i < 1000000; ++i) mapPutAll(a, [i : 1, i + 1 : 2, i + 2 : 3])").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
		code_v4_("var a = [:] for (var i = 0; i < 8000; ++i) a[i] = 1 mapMap(a, function(x) { return x })").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
		code_v4_("var a = [:] for (var i = 0; i < 8000; ++i) a[i] = 1 mapFilter(a, function(x) { return x % 2 })").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
		code_v4_("var a = [:] for (var i = 0; i < 8000; ++i) a[i] = 1 mapMerge(a, a)").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
		code_v4_("var a = [:] for (var i = 0; i < 8000; ++i) a[i] = 1 var b = clone(a)").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
	}
}
