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

		section("Set.setPut()");
		code_v4_("Set i = <1, 2>; setPut(i, 3); return i").equals("<1, 2, 3>");
		code_v4_("Set i = <1, 2>; setPut(i, 3); setPut(i, 4); return i").equals("<1, 2, 3, 4>");
		code_v4_("Set i = <1, 2>; setPut(i, 3); setPut(i, 3); return i").equals("<1, 2, 3>");
		code_v4_("Set i = <1, 2>; setPut(i, 1); return i").equals("<1, 2>");
		code_v4_("Set i = <>; setPut(i, 1); return i").equals("<1>");
		code_v4_("Set i = <>; setPut(i, 'okay'); setPut(i, 'okay'); return i").equals("<\"okay\">");
		code_v4_("Set i = <>; setPut(i, 'okay'); setPut(i, 1); return i").equals("<\"okay\", 1>");

		section("Set.setRemove()");
		code_v4_("Set i = <1, 2>; setRemove(i, 1); return i").equals("<2>");
		code_v4_("Set i = <1, 2>; setRemove(i, 3); return i").equals("<1, 2>");
		code_v4_("Set i = <1, 2>; setRemove(i, 1); setRemove(i, 2); return i").equals("<>");
		code_v4_("Set i = <1, 2>; setRemove(i, 1); setRemove(i, 1); return i").equals("<2>");
		code_v4_("Set i = <>; setRemove(i, 1); return i").equals("<>");
		code_v4_("Set i = <'okay', 1>; setRemove(i, 'okay'); return i").equals("<1>");

		section("Set.setClear()");
		code_v4_("Set i = <1, 2>; setClear(i); return i").equals("<>");
		code_v4_("Set i = <1, 2>; setClear(i); setClear(i); return i").equals("<>");
		code_v4_("Set i = <>; setClear(i); return i").equals("<>");
		code_v4_("Set i = <'okay', 1>; setClear(i); return i").equals("<>");
	}
}
