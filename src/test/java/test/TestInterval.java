package test;

import leekscript.common.Error;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Interval.constructor()");
		code("return [..]").equals("[..]");
		code("return [1..2]").equals("[1..2]");
		code_v1("return [1.0..2.0]").equals("[1..2]");
		code_v2_("return [1.0..2.0]").equals("[1.0..2.0]");
		code("return [-10..-2]").equals("[-10..-2]");
		code("return [1 * 5 .. 8 + 5]").equals("[5..13]");
		code("return ]-∞..5]").equals("]-∞..5]");
		code("return ]-∞..∞[").equals("]-∞..∞[");
		code("return ]-Infinity..Infinity[").equals("]-∞..∞[");
		code_v1("return ]-Infinity..1]").equals("]-∞..1]");
		code_v2_("return ]-Infinity..1]").equals("]-∞..1.0]");
		code_v1("return [1..Infinity[").equals("[1..∞[");
		code_v2_("return [1..Infinity[").equals("[1.0..∞[");
		code("return ]..[").equals("]-∞..∞[");
		code("return ]..1]").equals("]-∞..1]");
		code("return [1..[").equals("[1..∞[");
		code_v1("return ]..1.0]").equals("]-∞..1]");
		code_v2_("return ]..1.0]").equals("]-∞..1.0]");
		code_v1("return [1.0 ..[").equals("[1..∞[");
		code_v2_("return [1.0 ..[").equals("[1.0..∞[");
		code("[1..2]").ops(2);
		code_v3_("Interval a = new Interval() return a").equals("[..]");
		code("var a = [12] ]0..a[0][").error(Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		code("var a = [12] ]0..(a[0])[").equals("]0..12[");
		code("]5..12[ 0").equals("0");
		code("]5..12[ var x = 2").equals("null");

		section("Interval.intervalMin");
		code("return intervalMin([1..2])").equals("1");
		code("return intervalMin([1.0 .. 2.0])").almost(1.0);
		code("return intervalMin([-10..-2])").equals("-10");
		code("return intervalMin([-10.0 .. -2.0])").almost(-10.0);
		code("return intervalMin([1 * 5 .. 8 + 5])").equals("5");
		code("return intervalMin([1..1])").equals("1");
		code("return intervalMin([1..0])").equals("1");
		code("return intervalMin(]..[)").equals("-∞");
		code("intervalMin([1..2])").ops(3);
		code_v3_("Interval inter = [1..10] integer min = -intervalMin(inter) return min").equals("-1");

		section("Interval.intervalMax");
		code("return intervalMax([1..2])").equals("2");
		code("return intervalMax([1.0 .. 2.0])").almost(2.0);
		code("return intervalMax([-10..-2])").equals("-2");
		code("return intervalMax([-10.0 .. -2.0])").almost(-2.0);
		code("return intervalMax([1 * 5 .. 8 + 5])").equals("13");
		code("return intervalMax([1..1])").equals("1");
		code("return intervalMax([1..0])").equals("0");
		code("return intervalMax(]..[)").equals("∞");
		code("intervalMax([1..2])").ops(3);

		section("Interval.intervalIsEmpty");
		code("return intervalIsEmpty([..])").equals("true");
		code("return intervalIsEmpty([1..2])").equals("false");
		code("return intervalIsEmpty([-10..-2])").equals("false");
		code("return intervalIsEmpty([1..1])").equals("false");
		code("return intervalIsEmpty([1..0])").equals("true");
		code("return intervalIsEmpty([1..[)").equals("false");
		code("return intervalIsEmpty(]..[)").equals("false");
		code("intervalIsEmpty([1..2])").ops(3);

		section("Interval as boolean");
		code("return !![1..2]").equals("true");
		code("return !![-10..-2]").equals("true");
		code("return !![1..1]").equals("true");
		code("return !![1..0]").equals("false");
		code("return !![1..[").equals("true");

		section("Interval.intervalIsBounded");
		code("return intervalIsBounded([1..2])").equals("true");
		code("return intervalIsBounded(]..-2])").equals("false");
		code("return intervalIsBounded([1..[)").equals("false");
		code("return intervalIsBounded(]..[)").equals("false");
		code("intervalIsBounded([1..2])").ops(3);

		section("Interval.intervalIsLeftBounded");
		code("return intervalIsLeftBounded([1..[)").equals("true");
		code("return intervalIsLeftBounded(]..2])").equals("false");
		code("intervalIsLeftBounded([1..2])").ops(3);

		section("Interval.intervalIsRightBounded");
		code("return intervalIsRightBounded([1..[)").equals("false");
		code("return intervalIsRightBounded(]..2])").equals("true");
		code("intervalIsRightBounded([1..2])").ops(3);

		section("Interval.in");
		code("return 1 in [1..2]").equals("true");
		code("return 1 in [-1..[").equals("true");
		code("return 1 in ]..[").equals("true");
		code("return 1 in [..]").equals("false");
		code("return Infinity in ]..[").equals("true");
		code("return -Infinity in ]..[").equals("true");
		code("return 1 in [0..2]").equals("true");
		code("return 2 in [1..2]").equals("true");
		code("return 3 in [1..2]").equals("false");
		code("return 0 in [1..2]").equals("false");
		code("return 1 in [1..1]").equals("true");
		code("return 1 in [2..1]").equals("false");
		code_strict_v4_("boolean x = 1 in [1..1] return x").equals("true");
		code("1 in [1..2]").ops(4);
		code("return 2 in [1..2[").equals("false");
		code("return 1 in ]1..2[").equals("false");
		code("return 2.0 in [1.0 ..2.0[").equals("false");
		code("return 1.0 in ]1.0 ..2.0[").equals("false");
		code("return Infinity in [0..[").equals("true");
		code_v3_("return Integer.MAX_VALUE in [0.0 ..[").equals("true");

		section("Interval typing");
		code_strict_v4_("Interval i = [0..[ return i instanceof Interval").equals("true");
		code_v2_("return [0..1].class").equals("<class Interval>");

		section("Interval.intervalAverage");
		code_v1("return intervalAverage([1..2]);").equals("1,5");
		code_v2_("return intervalAverage([1..2]);").almost(1.5);
		code("return intervalAverage([-10..10]);").almost(0.0);
		code("return intervalAverage([1..1]);").almost(1.0);
		code("return intervalAverage([1..0]);").equals("NaN");
		code("return intervalAverage(]..1]);").equals("-∞");
		code("return intervalAverage([1..[)").equals("∞");
		code("return intervalAverage(]..[)").equals("NaN");
		code("return intervalAverage(]0..5]);").almost(3.0);
		code("intervalAverage([1..2])").ops(5);

		section("Interval.intervalIntersection");
		code("return intervalIntersection([1..2], [1..2])").equals("[1..2]");
		code("return intervalIntersection([1..2], [1..3])").equals("[1..2]");
		code("return intervalIntersection([1..2], [0..1])").equals("[1..1]");
		code("return intervalIntersection([1..2], [-1..0])").equals("[1..0]");
		code("return intervalIntersection([-1..2], [1..[)").equals("[1..2]");
		code("return intervalIntersection([1..2], ]..1])").equals("[1..1]");
		code_v1("return intervalIntersection([1..2], ]..[)").equals("[1..2]");
		code_v2_("return intervalIntersection([1..2], ]..[)").equals("[1.0..2.0]");
		code("return intervalIntersection(]..2], [1..[)").equals("[1..2]");
		code("intervalIntersection([1..2], [1..2])").ops(7);

		section("Interval.intervalCombine");
		code("return intervalCombine([1..2], [1..2])").equals("[1..2]");
		code_v1("return intervalCombine([1.0 .. 2.0], [1.0 .. 2.0])").equals("[1..2]");
		code_v2_("return intervalCombine([1.0 .. 2.0], [1.0 .. 2.0])").equals("[1.0..2.0]");
		code("return intervalCombine([1..2], [1..3])").equals("[1..3]");
		code("return intervalCombine([1..2], [0..1])").equals("[0..2]");
		code("return intervalCombine([1..2], [-1..0])").equals("[-1..2]");
		code("return intervalCombine([-1..2], [1..[)").equals("[-1..∞[");
		code("return intervalCombine([1..2], ]..1])").equals("]-∞..2]");
		code("return intervalCombine([1..2], ]..[)").equals("]-∞..∞[");
		code("return intervalCombine(]..2], [1..[)").equals("]-∞..∞[");
		code("intervalCombine([1..2], [1..2])").ops(7);

		section("Interval.intervalToArray()");
		code_v1("return intervalToArray([1.0 ..2.0])").equals("[1, 2]");
		code_v2_("return intervalToArray([1.0 ..2.0])").equals("[1.0, 2.0]");
		code_v1("return intervalToArray([-2.0 ..2.0])").equals("[-2, -1, 0, 1, 2]");
		code_v2_("return intervalToArray([-2.0 ..2.0])").equals("[-2.0, -1.0, 0.0, 1.0, 2.0]");
		code_v1("return intervalToArray([1.0 ..1.0])").equals("[1]");
		code_v2_("return intervalToArray([1.0 ..1.0])").equals("[1.0]");
		code("return intervalToArray([1.0 ..0.0])").equals("[]");
		code("return intervalToArray([1.0 ..[)").equals("null");
		code("return intervalToArray([1..2])").equals("[1, 2]");
		code("return intervalToArray([-2..2])").equals("[-2, -1, 0, 1, 2]");
		code("return intervalToArray([1..1])").equals("[1]");
		code("return intervalToArray([1..0])").equals("[]");
		code("return intervalToArray([1..[)").equals("null");
		code("return intervalToArray([1..5[)").equals("[1, 2, 3, 4]");
		code("return intervalToArray(]1..5[)").equals("[2, 3, 4]");
		code_v1_3("intervalToArray([1..2])").ops(12);
		code_v4_("intervalToArray([1..2])").ops(6);

		section("Interval.intervalToArray(<step>)");
		code_v1("return intervalToArray([1.0 ..2.0], 0.8);").equals("[1, 1,8]");
		code_v2_("return intervalToArray([1.0 ..2.0], 0.8);").equals("[1.0, 1.8]");
		code_v1("return intervalToArray([1.0 ..2.0], 2);").equals("[1]");
		code_v2_("return intervalToArray([1.0 ..2.0], 2);").equals("[1.0]");
		code_v1("return intervalToArray([-10.0 ..10.0], 5);").equals("[-10, -5, 0, 5, 10]");
		code_v2_("return intervalToArray([-10.0 ..10.0], 5);").equals("[-10.0, -5.0, 0.0, 5.0, 10.0]");
		code_v1("return intervalToArray([1.0 ..1.0], 7);").equals("[1]");
		code_v2_("return intervalToArray([1.0 ..1.0], 7);").equals("[1.0]");
		code("return intervalToArray([1.0 ..0.0], 2);").equals("[]");
		code("return intervalToArray([1.0 ..[, 2);").equals("null");
		code_v1("return intervalToArray([1..2], 0.8);").equals("[1, 1,8]");
		code_v2_("return intervalToArray([1..2], 0.8);").equals("[1.0, 1.8]");
		code("return intervalToArray([1..2], 2);").equals("[1]");
		code("return intervalToArray([-10..10], 5);").equals("[-10, -5, 0, 5, 10]");
		code("return intervalToArray([1..1], 7);").equals("[1]");
		code("return intervalToArray([1..0], 2);").equals("[]");
		code("return intervalToArray([1..[, 2);").equals("null");

		section("Interval.intervalToArray(<negative step>)");
		code_v1("return intervalToArray([1.0 ..2.0], -0.8);").equals("[2, 1,2]");
		code_v2_("return intervalToArray([1.0 ..2.0], -0.8);").equals("[2.0, 1.2]");
		code_v1("return intervalToArray([1.0 ..2.0], -2);").equals("[2]");
		code_v2_("return intervalToArray([1.0 ..2.0], -2);").equals("[2.0]");
		code_v1("return intervalToArray([-10.0 ..10.0], -5);").equals("[10, 5, 0, -5, -10]");
		code_v2_("return intervalToArray([-10.0 ..10.0], -5);").equals("[10.0, 5.0, 0.0, -5.0, -10.0]");
		code_v1("return intervalToArray([1.0 ..1.0], -7);").equals("[1]");
		code_v2_("return intervalToArray([1.0 ..1.0], -7);").equals("[1.0]");
		code("return intervalToArray([1.0 ..0.0], -2);").equals("[]");
		code_v1("return intervalToArray([1..2], -0.8);").equals("[2, 1,2]");
		code_v2_("return intervalToArray([1..2], -0.8);").equals("[2.0, 1.2]");
		code("return intervalToArray([1..2], -2);").equals("[2]");
		code("return intervalToArray([-10..10], -5);").equals("[10, 5, 0, -5, -10]");
		code("return intervalToArray([1..1], -7);").equals("[1]");
		code("return intervalToArray([1..0], -2);").equals("[]");
		code("return intervalToArray([0..5], -1);").equals("[5, 4, 3, 2, 1, 0]");
		code("return intervalToArray([0..5[, -1);").equals("[4, 3, 2, 1, 0]");
		code("return intervalToArray(]0..5[, -1);").equals("[4, 3, 2, 1]");


		section("Interval.intervalToSet()");
		code_v4_("return intervalToSet([1.0..2.0])").equals("<1.0, 2.0>");
		code_v4_("return intervalToSet([-2.0..2.0])").equals("<-2.0, -1.0, 0.0, 1.0, 2.0>");
		code_v4_("return intervalToSet([1.0..1.0])").equals("<1.0>");
		code_v4_("return intervalToSet([1.0..0.0])").equals("<>");
		code_v4_("return intervalToSet([1.0..[)").equals("null");
		code_v4_("return intervalToSet([1..2])").equals("<1, 2>");
		code_v4_("return intervalToSet([-2..2])").equals("<-1, 0, -2, 1, 2>");
		code_v4_("return intervalToSet([1..1])").equals("<1>");
		code_v4_("return intervalToSet([1..0])").equals("<>");
		code_v4_("return intervalToSet([1..[)").equals("null");
		code_v4_("return intervalToSet([1..5[)").equals("<1, 2, 3, 4>");
		code_v4_("return intervalToSet(]1..5[)").equals("<2, 3, 4>");
		code_v4_("intervalToSet([1..2])").ops(6);

		section("Interval.intervalToSet(<step>)");
		code_v4_("return intervalToSet([1.0..2.0], 0.8);").equals("<1.0, 1.8>");
		code_v4_("return intervalToSet([1.0..2.0], 2);").equals("<1.0>");
		code_v4_("return intervalToSet([-10.0..10.0], 5);").equals("<0.0, -10.0, -5.0, 5.0, 10.0>");
		code_v4_("return intervalToSet([1.0..1.0], 7);").equals("<1.0>");
		code_v4_("return intervalToSet([1.0..0.0], 2);").equals("<>");
		code_v4_("return intervalToSet([1.0..[, 2);").equals("null");
		code_v4_("return intervalToSet([1..2], 0.8);").equals("<1.0, 1.8>");
		code_v4_("return intervalToSet([1..2], 2);").equals("<1>");
		code_v4_("return intervalToSet([-10..10], 5);").equals("<0, -5, 5, -10, 10>");
		code_v4_("return intervalToSet([1..1], 7);").equals("<1>");
		code_v4_("return intervalToSet([1..0], 2);").equals("<>");
		code_v4_("return intervalToSet([1..[, 2);").equals("null");

		section("Interval.intervalToSet(<negative step>)");
		code_v4_("return intervalToSet([1.0..2.0], -0.8);").equals("<2.0, 1.2>");
		code_v4_("return intervalToSet([1.0..2.0], -2);").equals("<2.0>");
		code_v4_("return intervalToSet([-10.0..10.0], -5);").equals("<0.0, 10.0, 5.0, -5.0, -10.0>");
		code_v4_("return intervalToSet([1.0..1.0], -7);").equals("<1.0>");
		code_v4_("return intervalToSet([1.0..0.0], -2);").equals("<>");
		code_v4_("return intervalToSet([1..2], -0.8);").equals("<2.0, 1.2>");
		code_v4_("return intervalToSet([1..2], -2);").equals("<2>");
		code_v4_("return intervalToSet([-10..10], -5);").equals("<0, -5, 5, -10, 10>");
		code_v4_("return intervalToSet([1..1], -7);").equals("<1>");
		code_v4_("return intervalToSet([1..0], -2);").equals("<>");
		code_v4_("return intervalToSet([0..5], -1);").equals("<0, 1, 2, 3, 4, 5>");
		code_v4_("return intervalToSet([0..5[, -1);").equals("<0, 1, 2, 3, 4>");
		code_v4_("return intervalToSet(]0..5[, -1);").equals("<1, 2, 3, 4>");

		section("Interval.[start:end:step]");
		code_v4_("return [1..10][2:4:2]").equals("[5.0, 7.0]");
		code_v4_("return [1..3][2:4:2]").equals("[]");
		code_v4_("return [1..3][::2]").equals("[1.0, 3.0]");
		code_v4_("return [1..3][::-1.5]").equals("[3.0, 1.5]");
		code_v4_("return [1..10][2:4:-2]").equals("[6.0, 4.0]");
		code_v4_("return [1..4][::]").equals("[1.0, 2.0, 3.0, 4.0]");
		code_v4_("return [1..4][-1::]").equals("[4.0]");
		code_v4_("return [1..4][:-1:]").equals("[1.0, 2.0, 3.0]");
		code_v4_("return [1..4][2::]").equals("[3.0, 4.0]");
		code_v4_("return [1..4][:2:]").equals("[1.0, 2.0]");

		section("Interval iteration");
		code("var i = [1..5] var x = 0 for (var y in i) x += y return x").equals("15");
		code("var i = [1..5[ var x = 0 for (var y in i) x += y return x").equals("10");
		code("var i = ]1..5] var x = 0 for (var y in i) x += y return x").equals("14");
		code("var i = ]1..5[ var x = 0 for (var y in i) x += y return x").equals("9");
		code_v2_("var i = [1.0..5.0] var x = 0.0 for (var y in i) x += y return x").equals("15.0");
		code_v2_("var i = [1.0..5.0[ var x = 0.0 for (var y in i) x += y return x").equals("10.0");
		code_v2_("var i = ]1.0..5.0] var x = 0.0 for (var y in i) x += y return x").equals("14.0");
		code_v2_("var i = ]1.0..5.0[ var x = 0.0 for (var y in i) x += y return x").equals("9.0");
	}
}
