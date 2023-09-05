package test;

public class TestSet extends TestCommon {

	public void run() throws Exception {

		section("Set.constructor()");
		code("return <1, 2>").debug().equals("<1, 2>");
		code("return <(1 > 2), (1 < 2)>").equals("<false, true>");
		code("return <\"abc\", 1>").equals("<1, \"abc\">");
		code("return <>").equals("<>");
		DISABLED_code("return <<>>").equals("<<>>");
		code("<1, 2, 3, 4>").ops(8);

		section("Set typing");
		code_strict_v4_("Set i = <1, 2>; return i instanceof Set").equals("true");
		code_strict_v4_("Set<integer> i = <1, 2>; return i instanceof Set").equals("true");

		section("Set.setPut()");
		code("var i = <1, 2> setPut(i, 3) return i").debug().equals("<1, 2, 3>");
		code("var i = <1, 2> setPut(i, 3) setPut(i, 4) return i").equals("<1, 2, 3, 4>");
		code("var i = <1, 2> setPut(i, 3) setPut(i, 3) return i").equals("<1, 2, 3>");
		code("var i = <1, 2> setPut(i, 1) return i").equals("<1, 2>");
		code("var i = <> setPut(i, 1) return i").equals("<1>");
		code("var i = <> setPut(i, 'okay') setPut(i, 'okay') return i").equals("<\"okay\">");
		code("var i = <> setPut(i, 'okay') setPut(i, 1) return i").equals("<\"okay\", 1>");

		section("Set.setRemove()");
		code("var i = <1, 2> setRemove(i, 1) return i").equals("<2>");
		code("var i = <1, 2> setRemove(i, 3) return i").equals("<1, 2>");
		code("var i = <1, 2> setRemove(i, 1) setRemove(i, 2) return i").equals("<>");
		code("var i = <1, 2> setRemove(i, 1) setRemove(i, 1) return i").equals("<2>");
		code("var i = <> setRemove(i, 1) return i").equals("<>");
		code("var i = <'okay', 1> setRemove(i, 'okay') return i").equals("<1>");

		section("Set.setClear()");
		code("var i = <1, 2>; setClear(i); return i").equals("<>");
		code("var i = <1, 2>; setClear(i); setClear(i); return i").equals("<>");
		code("var i = <>; setClear(i); return i").equals("<>");
		code("var i = <'okay', 1>; setClear(i); return i").equals("<>");

		section("Set.setContains()");
		code("var i = <1, 2>; return setContains(i, 1)").equals("true");
		code("var i = <1, 2>; return setContains(i, 3)").equals("false");
		code("var i = <>; return setContains(i, 1)").equals("false");
		code("var i = <'okay', 1>; return setContains(i, 'okay')").equals("true");
		code("var i = <'okay', 1>; return setContains(i, 'other')").equals("false");

		section("Set.in");
		code("var i = <1, 2>; return 1 in i").equals("true");
		code("var i = <1, 2>; return 3 in i").equals("false");
		code("var i = <>; return 1 in i").equals("false");
		code("var i = <'okay', 1>; return 'okay' in i").equals("true");
		code("var i = <'okay', 1>; return 'other' in i").equals("false");

		section("Set.setSize()");
		code("var i = <1, 2>; return setSize(i)").equals("2");
		code("var i = <1, 2>; setRemove(i, 1); return setSize(i)").equals("1");
		code("var i = <>; return setSize(i)").equals("0");
		code("var i = <'okay', 1>; return setSize(i)").equals("2");

		section("Set.setIsEmpty()");
		code("var i = <1, 2>; setRemove(i, 1); return setIsEmpty(i)").equals("false");
		code("var i = <1, 2>; setClear(i); return setIsEmpty(i)").equals("true");
		code("var i = <1>; setRemove(i, 1); return setIsEmpty(i)").equals("true");
		code("var i = <>; return setIsEmpty(i)").equals("true");
		code("var i = <'okay', 1>; return setIsEmpty(i)").equals("false");

		section("Set.setSubsetOf()");
		code("var i = <1, 2>; var j = <1, 2, 3>; return setIsSubsetOf(i, j)").equals("true");
		code("var i = <1, 2>; var j = <1, 2, 3>; return setIsSubsetOf(j, i)").equals("false");
		code("var i = <1, 2>; var j = <1, 2>; return setIsSubsetOf(i, j)").equals("true");
		code("var i = <1, 2>; var j = <1, 2>; return setIsSubsetOf(j, i)").equals("true");
		code("var i = <>; var j = <1, 2>; return setIsSubsetOf(i, j)").equals("true");
		code("var i = <>; var j = <1, 2>; return setIsSubsetOf(j, i)").equals("false");
		code("var i = <>; var j = <>; return setIsSubsetOf(i, j)").equals("true");

		section("Set iteration");
		code("var s = <1, 2, 3, 4, 5> var x = 0 for (var y in s) x += y return x").equals("15");

		section("Set.union()");
		code("var s1 = <1, 2, 3> var s2 = <4, 5, 6> return setUnion(s1, s2)").equals("<1, 2, 3, 4, 5, 6>");

		section("Set.intersection()");
		code("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setIntersection(s1, s2)").debug().equals("<2, 3>");

		section("Set.difference()");
		code("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setDifference(s1, s2)").debug().equals("<1>");

		section("Set.disjunction()");
		code("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setDisjunction(s1, s2)").debug().equals("<1, 4>");

		section("Set.toArray()");
		code("var s = <1, 2, 3> return setToArray(s)").equals("[1, 2, 3]");
	}
}
