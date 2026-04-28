package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;


@ExtendWith(SummaryExtension.class)
public class TestSet extends TestCommon {


		@Test
	public void testSet_constructor() throws Exception {
		section("Set.constructor()");
		code("return <1, 2>").debug().equals("<1, 2>");
		code("return <(1 > 2), (1 < 2)>").equals("<false, true>");
		code("return <\"abc\", 1>").equals("<1, \"abc\">");
		code("return <>").equals("<>");
		DISABLED_code("return <<>>").equals("<<>>");
		code("<1, 2, 3, 4>").ops(8);
		code_v3_("Set<integer> a = new Set() return a").equals("<>");
	}

	@Test
	public void testSet_typing() throws Exception {
		section("Set typing");
		code_strict_v4_("Set i = <1, 2>; return i instanceof Set").equals("true");
		code_strict_v4_("Set<integer> i = <1, 2>; return i instanceof Set").equals("true");
		// Passing Array? to Set? parameter (was generating incompatible cast)
		code_strict_v4_("function f1(Array? arr) { return f2(arr) } function f2(Set? s) { return s == null } return f1(null)").equals("true");
	}

	@Test
	public void testSet_operator_EqualsEquals() throws Exception {
		section("Set.operator ==");
		code("return <> == <>").equals("true");
		code("return <1> == <1>").equals("true");
		code("return <'a', 'b'> == <1, 2>").equals("false");
		code("return <'a', 'b'> == <'a', 'b'>").equals("true");
		// == not deep on sets
		DISABLED_code("return <'a', <1, 2>> == <'a', <1, 2>>").equals("true");
		DISABLED_code("return <[1], [2 : 3]> == <[1], [2 : 3]>").equals("true");
		code("return <'a', 'b'> == <'a', 'b', 'c', 'd'>").equals("false");
	}

	@Test
	public void testSet_operator_Not() throws Exception {
		section("Set operator !");
		code("var i = <> return !i").equals("true");
		code("var i = <1> return !i").equals("false");
	}

	@Test
	public void testSet_setPut() throws Exception {
		section("Set.setPut()");
		code("var i = <1, 2> setPut(i, 3) return i").debug().equals("<1, 2, 3>");
		code("var i = <1, 2> setPut(i, 3) setPut(i, 4) return i").equals("<1, 2, 3, 4>");
		code("var i = <1, 2> setPut(i, 3) setPut(i, 3) return i").equals("<1, 2, 3>");
		code("var i = <1, 2> setPut(i, 1) return i").equals("<1, 2>");
		code("var i = <> setPut(i, 1) return i").equals("<1>");
		code("var i = <> setPut(i, 'okay') setPut(i, 'okay') return i").equals("<\"okay\">");
		code("var i = <> setPut(i, 'okay') setPut(i, 1) return i").equals("<\"okay\", 1>");
		code("var i = <1, 2> return setPut(i, 3)").equals("true");
		code("var i = <1, 2> return setPut(i, 2)").equals("false");
	}

	@Test
	public void testSet_setRemove() throws Exception {
		section("Set.setRemove()");
		code("var i = <1, 2> setRemove(i, 1) return i").equals("<2>");
		code("var i = <1, 2> setRemove(i, 3) return i").equals("<1, 2>");
		code("var i = <1, 2> setRemove(i, 1) setRemove(i, 2) return i").equals("<>");
		code("var i = <1, 2> setRemove(i, 1) setRemove(i, 1) return i").equals("<2>");
		code("var i = <> setRemove(i, 1) return i").equals("<>");
		code("var i = <'okay', 1> setRemove(i, 'okay') return i").equals("<1>");
	}

	@Test
	public void testSet_setClear() throws Exception {
		section("Set.setClear()");
		code("var i = <1, 2>; setClear(i); return i").equals("<>");
		code("var i = <1, 2>; setClear(i); setClear(i); return i").equals("<>");
		code("var i = <>; setClear(i); return i").equals("<>");
		code("var i = <'okay', 1>; setClear(i); return i").equals("<>");
	}

	@Test
	public void testSet_setContains() throws Exception {
		section("Set.setContains()");
		code("var i = <1, 2>; return setContains(i, 1)").equals("true");
		code("var i = <1, 2>; return setContains(i, 3)").equals("false");
		code("var i = <>; return setContains(i, 1)").equals("false");
		code("var i = <'okay', 1>; return setContains(i, 'okay')").equals("true");
		code("var i = <'okay', 1>; return setContains(i, 'other')").equals("false");
	}

	@Test
	public void testSet_in() throws Exception {
		section("Set.in");
		code("var i = <1, 2>; return 1 in i").equals("true");
		code("var i = <1, 2>; return 3 in i").equals("false");
		code("var i = <>; return 1 in i").equals("false");
		code("var i = <'okay', 1>; return 'okay' in i").equals("true");
		code("var i = <'okay', 1>; return 'other' in i").equals("false");
	}

	@Test
	public void testSet_not_in() throws Exception {
		section("Set.not_in");
		code("var i = <1, 2>; return 3 not in i").equals("true");
		code("var i = <1, 2>; return 1 not in i").equals("false");
	}

	@Test
	public void testSet_setSize() throws Exception {
		section("Set.setSize()");
		code("var i = <1, 2>; return setSize(i)").equals("2");
		code("var i = <1, 2>; setRemove(i, 1); return setSize(i)").equals("1");
		code("var i = <>; return setSize(i)").equals("0");
		code("var i = <'okay', 1>; return setSize(i)").equals("2");
	}

	@Test
	public void testSet_setIsEmpty() throws Exception {
		section("Set.setIsEmpty()");
		code("var i = <1, 2>; setRemove(i, 1); return setIsEmpty(i)").equals("false");
		code("var i = <1, 2>; setClear(i); return setIsEmpty(i)").equals("true");
		code("var i = <1>; setRemove(i, 1); return setIsEmpty(i)").equals("true");
		code("var i = <>; return setIsEmpty(i)").equals("true");
		code("var i = <'okay', 1>; return setIsEmpty(i)").equals("false");
	}

	@Test
	public void testSet_setSubsetOf() throws Exception {
		section("Set.setSubsetOf()");
		code("var i = <1, 2>; var j = <1, 2, 3>; return setIsSubsetOf(i, j)").equals("true");
		code("var i = <1, 2>; var j = <1, 2, 3>; return setIsSubsetOf(j, i)").equals("false");
		code("var i = <1, 2>; var j = <1, 2>; return setIsSubsetOf(i, j)").equals("true");
		code("var i = <1, 2>; var j = <1, 2>; return setIsSubsetOf(j, i)").equals("true");
		code("var i = <>; var j = <1, 2>; return setIsSubsetOf(i, j)").equals("true");
		code("var i = <>; var j = <1, 2>; return setIsSubsetOf(j, i)").equals("false");
		code("var i = <>; var j = <>; return setIsSubsetOf(i, j)").equals("true");
	}

	@Test
	public void testSet_iteration() throws Exception {
		section("Set iteration");
		code("var s = <1, 2, 3, 4, 5> var x = 0 for (var y in s) x += y return x").equals("15");
	}

	@Test
	public void testSet_union() throws Exception {
		section("Set.union()");
		code("var s1 = <1, 2, 3> var s2 = <4, 5, 6> return setUnion(s1, s2)").equals("<1, 2, 3, 4, 5, 6>");
	}

	@Test
	public void testSet_intersection() throws Exception {
		section("Set.intersection()");
		code("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setIntersection(s1, s2)").debug().equals("<2, 3>");
	}

	@Test
	public void testSet_difference() throws Exception {
		section("Set.difference()");
		code("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setDifference(s1, s2)").debug().equals("<1>");
	}

	@Test
	public void testSet_disjunction() throws Exception {
		section("Set.disjunction()");
		code("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setDisjunction(s1, s2)").debug().equals("<1, 4>");
	}

	@Test
	public void testSet_filter() throws Exception {
		section("Set.filter()");
		code("var s = <1, 2, 3, 4, 5> return setFilter(s, x -> x > 2)").equals("<3, 4, 5>");
		code("var s = <1, 2, 3> return setFilter(s, x -> false)").equals("<>");
		code("var s = <1, 2, 3> return setFilter(s, x -> true)").equals("<1, 2, 3>");
		code("var s = <> return setFilter(s, x -> true)").equals("<>");
	}

	@Test
	public void testSet_toArray() throws Exception {
		section("Set.toArray()");
		code("var s = <1, 2, 3> return setToArray(s)").equals("[1, 2, 3]");
	}

	@Test
	public void testSet_clone() throws Exception {
		section("Set clone");
		code("var s = <1, 2, 3> var s2 = clone(s) setClear(s) return s2").equals("<1, 2, 3>");
		code("var s = <1, 2, 3> var s2 = clone(s) setPut(s, 4) return s2").equals("<1, 2, 3>");
	}

}
