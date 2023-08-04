package test;

public class TestSet extends TestCommon {

	public void run() throws Exception {

		section("Set.constructor()");
		code_v4_("return <1, 2>").debug().equals("<1, 2>");
		code_v4_("return <(1 > 2), (1 < 2)>").equals("<false, true>");
		code_v4_("return <\"abc\", 1>").equals("<1, \"abc\">");
		code_v4_("return <>").equals("<>");
		DISABLED_code_v4_("return <<>>").equals("<<>>");
		code_v4_("<1, 2, 3, 4>").ops(8);

		section("Set typing");
		code_strict_v4_("Set i = <1, 2>; return i instanceof Set").equals("true");
		code_strict_v4_("Set<integer> i = <1, 2>; return i instanceof Set").equals("true");
	}
}
