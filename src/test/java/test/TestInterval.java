package test;

public class TestInterval extends TestCommon {

	public void run() throws Exception {

		section("Interval.constructor()");
		code_v4_("return [1..2];").equals("[1.0..2.0]");
		code_v4_("return [-10..-2];").equals("[-10.0..-2.0]");
		code_v4_("return [1 * 5 .. 8 + 5];").equals("[5.0..13.0]");
		code_v4_("[1..2]").ops(2);

		section("Interval.intervalLowerBound");
		code_v4_("return intervalLowerBound([1..2]);").equals("1.0");
		code_v4_("return intervalLowerBound([-10..-2]);").equals("-10.0");
		code_v4_("return intervalLowerBound([1 * 5 .. 8 + 5]);").equals("5.0");
		code_v4_("return intervalLowerBound([1..1]);").equals("1.0");
		code_v4_("return intervalLowerBound([1..0]);").equals("1.0");

		section("Interval.intervalUpperBound");
		code_v4_("return intervalUpperBound([1..2]);").equals("2.0");
		code_v4_("return intervalUpperBound([-10..-2]);").equals("-2.0");
		code_v4_("return intervalUpperBound([1 * 5 .. 8 + 5]);").equals("13.0");
		code_v4_("return intervalUpperBound([1..1]);").equals("1.0");
		code_v4_("return intervalUpperBound([1..0]);").equals("0.0");

		section("Interval.intervalIsEmpty");
		code_v4_("return intervalIsEmpty([1..2]);").equals("false");
		code_v4_("return intervalIsEmpty([-10..-2]);").equals("false");
		code_v4_("return intervalIsEmpty([1..1]);").equals("false");
		code_v4_("return intervalIsEmpty([1..0]);").equals("true");

		section("Interval as boolean");
		code_v4_("return !![1..2];").equals("true");
		code_v4_("return !![-10..-2];").equals("true");
		code_v4_("return !![1..1];").equals("true");
		code_v4_("return !![1..0];").debug().equals("false");

		section("Interval.in");
		code_v4_("return 1 in [1..2];").equals("true");
		code_v4_("return 1 in [0..2];").equals("true");
		code_v4_("return 2 in [1..2];").equals("true");
		code_v4_("return 3 in [1..2];").equals("false");
		code_v4_("return 0 in [1..2];").equals("false");
		code_v4_("return 1 in [1..1];").equals("true");
		code_v4_("return 1 in [2..1];").equals("false");
		code_strict_v4_("boolean x = 1 in [1..1]; return x").equals("true");
		code_v4_("1 in [1..2]").ops(4);

		section("Interval typing");
		code_strict_v4_("Interval i = [0..10]; return i instanceof Interval").equals("true");

		section("Interval.intervalToArray()");
		code_v4_("return intervalToArray([1..2]);").equals("[1.0, 2.0]");
		code_v4_("return intervalToArray([-2..2]);").equals("[-2.0, -1.0, 0.0, 1.0, 2.0]");
		code_v4_("return intervalToArray([1..1]);").equals("[1.0]");
		code_v4_("return intervalToArray([1..0]);").equals("[]");
		code_v4_("intervalToArray([1..2])").ops(6);

		section("Interval.intervalToArray(<step>)");
		code_v4_("return intervalToArray([1..2], 0.8);").equals("[1.0, 1.8]");
		code_v4_("return intervalToArray([1..2], 2);").equals("[1.0]");
		code_v4_("return intervalToArray([-10..10], 5);").equals("[-10.0, -5.0, 0.0, 5.0, 10.0]");
		code_v4_("return intervalToArray([1..1], 7);").equals("[1.0]");
		code_v4_("return intervalToArray([1..0], 2);").equals("[]");

		section("Interval.intervalToArray(<negative step>)");
		code_v4_("return intervalToArray([1..2], -0.8);").equals("[2.0, 1.2]");
		code_v4_("return intervalToArray([1..2], -2);").equals("[2.0]");
		code_v4_("return intervalToArray([-10..10], -5);").equals("[10.0, 5.0, 0.0, -5.0, -10.0]");
		code_v4_("return intervalToArray([1..1], -7);").equals("[1.0]");
		code_v4_("return intervalToArray([1..0], -2);").equals("[]");
	}
}
