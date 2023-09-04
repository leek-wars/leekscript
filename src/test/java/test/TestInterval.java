package test;

import leekscript.common.Error;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Interval.constructor()");
		code_v4_("return [1..2];").equals("[1.0..2.0]");
		code_v4_("return [-10..-2];").equals("[-10.0..-2.0]");
		code_v4_("return [1 * 5 .. 8 + 5];").equals("[5.0..13.0]");
		code_v4_("return [-Infinity..Infinity];").equals("[..]");
		code_v4_("return [-Infinity..1];").equals("[..1.0]");
		code_v4_("return [1..Infinity];").equals("[1.0..]");
		code_v4_("return [..];").equals("[..]");
		code_v4_("return [..1];").equals("[..1.0]");
		code_v4_("return [1..];").equals("[1.0..]");
		code_v4_("[1..2]").ops(2);

		section("Interval.intervalLowerBound");
		code_v4_("return intervalLowerBound([1..2]);").equals("1.0");
		code_v4_("return intervalLowerBound([-10..-2]);").equals("-10.0");
		code_v4_("return intervalLowerBound([1 * 5 .. 8 + 5]);").equals("5.0");
		code_v4_("return intervalLowerBound([1..1]);").equals("1.0");
		code_v4_("return intervalLowerBound([1..0]);").equals("1.0");
		code_v4_("return intervalLowerBound([..]);").equals("-Infinity");
		code_v4_("intervalLowerBound([1..2])").ops(3);

		section("Interval.intervalUpperBound");
		code_v4_("return intervalUpperBound([1..2]);").equals("2.0");
		code_v4_("return intervalUpperBound([-10..-2]);").equals("-2.0");
		code_v4_("return intervalUpperBound([1 * 5 .. 8 + 5]);").equals("13.0");
		code_v4_("return intervalUpperBound([1..1]);").equals("1.0");
		code_v4_("return intervalUpperBound([1..0]);").equals("0.0");
		code_v4_("return intervalUpperBound([..]);").equals("Infinity");
		code_v4_("intervalUpperBound([1..2])").ops(3);

		section("Interval.intervalIsEmpty");
		code_v4_("return intervalIsEmpty([1..2]);").equals("false");
		code_v4_("return intervalIsEmpty([-10..-2]);").equals("false");
		code_v4_("return intervalIsEmpty([1..1]);").equals("false");
		code_v4_("return intervalIsEmpty([1..0]);").equals("true");
		code_v4_("return intervalIsEmpty([1..]);").equals("false");
		code_v4_("return intervalIsEmpty([..]);").equals("false");
		code_v4_("intervalIsEmpty([1..2])").ops(3);

		section("Interval as boolean");
		code_v4_("return !![1..2];").equals("true");
		code_v4_("return !![-10..-2];").equals("true");
		code_v4_("return !![1..1];").equals("true");
		code_v4_("return !![1..0];").equals("false");
		code_v4_("return !![1..];").equals("true");

		section("Interval.intervalIsBounded");
		code_v4_("return intervalIsBounded([1..2]);").equals("true");
		code_v4_("return intervalIsBounded([..-2]);").equals("false");
		code_v4_("return intervalIsBounded([1..]);").equals("false");
		code_v4_("return intervalIsBounded([..]);").equals("false");
		code_v4_("intervalIsBounded([1..2])").ops(3);

		section("Interval.intervalIsLeftBounded");
		code_v4_("return intervalIsLeftBounded([1..]);").equals("true");
		code_v4_("return intervalIsLeftBounded([..2]);").equals("false");
		code_v4_("intervalIsLeftBounded([1..2])").ops(3);

		section("Interval.intervalIsRightBounded");
		code_v4_("return intervalIsRightBounded([1..]);").equals("false");
		code_v4_("return intervalIsRightBounded([..2]);").equals("true");
		code_v4_("intervalIsRightBounded([1..2])").ops(3);

		section("Interval.in");
		code_v4_("return 1 in [1..2];").equals("true");
		code_v4_("return 1 in [-1..];").equals("true");
		code_v4_("return 1 in [..];").equals("true");
		code_v4_("return Infinity in [..];").equals("true");
		code_v4_("return -Infinity in [..];").equals("true");
		code_v4_("return 1 in [0..2];").equals("true");
		code_v4_("return 2 in [1..2];").equals("true");
		code_v4_("return 3 in [1..2];").equals("false");
		code_v4_("return 0 in [1..2];").equals("false");
		code_v4_("return 1 in [1..1];").equals("true");
		code_v4_("return 1 in [2..1];").equals("false");
		code_strict_v4_("boolean x = 1 in [1..1]; return x").equals("true");
		code_v4_("1 in [1..2]").ops(4);

		section("Interval typing");
		code_strict_v4_("Interval i = [0..]; return i instanceof Interval").equals("true");

		section("Interval.intervalMidpoint");
		code_v4_("return intervalMidpoint([1..2]);").equals("1.5");
		code_v4_("return intervalMidpoint([-10..10]);").equals("0.0");
		code_v4_("return intervalMidpoint([1..1]);").equals("1.0");
		code_v4_("return intervalMidpoint([1..0]);").equals("NaN");
		code_v4_("return intervalMidpoint([..1]);").equals("-Infinity");
		code_v4_("return intervalMidpoint([1..]);").equals("Infinity");
		code_v4_("return intervalMidpoint([..]);").equals("NaN");
		code_v4_("intervalMidpoint([1..2])").ops(5);

		section("Interval.intervalIntersection");
		code_v4_("return intervalIntersection([1..2], [1..2]);").equals("[1.0..2.0]");
		code_v4_("return intervalIntersection([1..2], [1..3]);").equals("[1.0..2.0]");
		code_v4_("return intervalIntersection([1..2], [0..1]);").equals("[1.0..1.0]");
		code_v4_("return intervalIntersection([1..2], [-1..0]);").equals("[1.0..0.0]");
		code_v4_("return intervalIntersection([-1..2], [1..]);").equals("[1.0..2.0]");
		code_v4_("return intervalIntersection([1..2], [..1]);").equals("[1.0..1.0]");
		code_v4_("return intervalIntersection([1..2], [..]);").equals("[1.0..2.0]");
		code_v4_("return intervalIntersection([..2], [1..]);").equals("[1.0..2.0]");
		code_v4_("intervalIntersection([1..2], [1..2])").ops(7);

		section("Interval.intervalCombine");
		code_v4_("return intervalCombine([1..2], [1..2]);").equals("[1.0..2.0]");
		code_v4_("return intervalCombine([1..2], [1..3]);").equals("[1.0..3.0]");
		code_v4_("return intervalCombine([1..2], [0..1]);").equals("[0.0..2.0]");
		code_v4_("return intervalCombine([1..2], [-1..0]);").equals("[-1.0..2.0]");
		code_v4_("return intervalCombine([-1..2], [1..]);").equals("[-1.0..]");
		code_v4_("return intervalCombine([1..2], [..1]);").equals("[..2.0]");
		code_v4_("return intervalCombine([1..2], [..]);").equals("[..]");
		code_v4_("return intervalCombine([..2], [1..]);").equals("[..]");
		code_v4_("intervalCombine([1..2], [1..2])").ops(7);

		section("Interval.intervalToArray()");
		code_v4_("return intervalToArray([1..2]);").equals("[1.0, 2.0]");
		code_v4_("return intervalToArray([-2..2]);").equals("[-2.0, -1.0, 0.0, 1.0, 2.0]");
		code_v4_("return intervalToArray([1..1]);").equals("[1.0]");
		code_v4_("return intervalToArray([1..0]);").equals("[]");
		code_v4_("return intervalToArray([1..]);").equals("null");
		code_v4_("intervalToArray([1..2])").ops(6);

		section("Interval.intervalToArray(<step>)");
		code_v4_("return intervalToArray([1..2], 0.8);").equals("[1.0, 1.8]");
		code_v4_("return intervalToArray([1..2], 2);").equals("[1.0]");
		code_v4_("return intervalToArray([-10..10], 5);").equals("[-10.0, -5.0, 0.0, 5.0, 10.0]");
		code_v4_("return intervalToArray([1..1], 7);").equals("[1.0]");
		code_v4_("return intervalToArray([1..0], 2);").equals("[]");
		code_v4_("return intervalToArray([1..], 2);").equals("null");

		section("Interval.intervalToArray(<negative step>)");
		code_v4_("return intervalToArray([1..2], -0.8);").equals("[2.0, 1.2]");
		code_v4_("return intervalToArray([1..2], -2);").equals("[2.0]");
		code_v4_("return intervalToArray([-10..10], -5);").equals("[10.0, 5.0, 0.0, -5.0, -10.0]");
		code_v4_("return intervalToArray([1..1], -7);").equals("[1.0]");
		code_v4_("return intervalToArray([1..0], -2);").equals("[]");

		section("Interval.[start:end:step]");
		code_v4_("return [1..10][2:4:2];").equals("[5.0, 7.0]");
		code_v4_("return [1..3][2:4:2];").equals("[]");
		code_v4_("return [1..3][::2];").equals("[1.0, 3.0]");
		code_v4_("return [1..3][::-1.5];").equals("[3.0, 1.5]");
		code_v4_("return [1..10][2:4:-2];").equals("[6.0, 4.0]");
		code_v4_("return [1..4][::];").equals("[1.0, 2.0, 3.0, 4.0]");
		code_v4_("return [1..4][-1::];").equals("[4.0]");
		code_v4_("return [1..4][:-1:];").equals("[1.0, 2.0, 3.0]");
		code_v4_("return [1..4][2::];").equals("[3.0, 4.0]");
		code_v4_("return [1..4][:2:];").equals("[1.0, 2.0]");

		section("Interval iteration");
		code_v4_("var i = [0..6] var x = 0 for (var y in i) x += y return x").equals("15");
	}
}
