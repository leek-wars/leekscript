package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestEnum extends TestCommon {

	@Test
	public void run() throws Exception {

		section("Enum numeric defaults");
		code_v4_("enum Color { RED, GREEN, BLUE } integer r = Color.RED as integer, g = Color.GREEN as integer, b = Color.BLUE as integer return [r, g, b]").equals("[0, 1, 2]");

		section("Enum with explicit values");
		code_v4_("enum Color { RED = 1, GREEN = 2, BLUE = 3 } return Color.RED as integer").equals("1");
		code_v4_("enum Color { RED = 1, GREEN = 2, BLUE = 3 } return Color.GREEN as integer").equals("2");
		code_v4_("enum Color { RED = 1, GREEN = 2, BLUE = 3 } return Color.BLUE as integer").equals("3");

		section("Enum mixed default and explicit values");
		code_v4_("enum Mixed { A, B = 2, C } return Mixed.A as integer").equals("0");
		code_v4_("enum Mixed { A, B = 2, C } return Mixed.B as integer").equals("2");
		code_v4_("enum Mixed { A, B = 2, C } return Mixed.C as integer").equals("3");

		section("Enum with non-integer values");
		code_v4_("enum Status { OK = 200, FAIL = 500 } return Status.OK as integer").equals("200");
		code_v4_("enum Status { OK = 200, FAIL = 500 } return Status.FAIL as integer").equals("500");

		section("Enum in variable");
		code_v4_("enum Direction { UP, DOWN, LEFT, RIGHT } integer d = Direction.UP as integer return d").equals("0");
		code_v4_("enum Direction { UP, DOWN } integer d = Direction.DOWN as integer return d").equals("1");

		section("Enum type in type annotation");
		code_v4_("enum State { ON, OFF } State x = 0 as State integer i = x as integer return i").equals("0");

		section("Enum equality and switch");
		code_v4_("enum Color { RED, GREEN } var c = Color.RED return c == Color.RED").equals("true");
		code_v4_("enum Color { RED, GREEN } var c = Color.GREEN return c == Color.RED").equals("false");
		code_v4_("enum Color { RED = 1, GREEN = 2, BLUE = 3 } var c = Color.GREEN switch (c) { case Color.RED: return 1 case Color.GREEN: return 2 default: return 3 }").equals("2");

		section("Enum comparison warnings");
		code_v4_("enum Color { RED, GREEN } return Color.RED == 0").warning(Error.COMPARISON_ALWAYS_FALSE);
		code_v4_("enum Color { RED, GREEN } return Color.RED != 0").warning(Error.COMPARISON_ALWAYS_TRUE);

		section("Enum incomplete switch (no default)");
		code_v4_("enum Color { RED, GREEN, BLUE } var c = Color.RED switch (c) { case Color.RED: return 1 case Color.GREEN: return 2 } return 3").equals("1");
		code_v4_("enum Color { RED, GREEN, BLUE } var c = Color.BLUE switch (c) { case Color.RED: return 1 case Color.GREEN: return 2 } return 3").equals("3");

		section("Enum switch warnings");
		code_v4_("enum Color { RED, GREEN, BLUE } var c = Color.RED switch (c) { case Color.RED: return 1 case Color.GREEN: return 2 } return 3").warning(Error.INCOMPLETE_ENUM_SWITCH);
		code_v4_("enum Color { RED, GREEN, BLUE } var c = Color.GREEN switch (c) { case Color.RED: return 1 case Color.GREEN: return 2 } return 3").warning(Error.INCOMPLETE_ENUM_SWITCH);

		section("Enum in arithmetic and typeOf");
		code_v4_("enum Num { ONE = 1 } return Num.ONE + 1").equals("2");
		code_v4_("enum Num { ONE = 1 } return typeOf(Num.ONE)").equals("\"Num\"");

		section("Enum string()");
		code_v4_("enum Color { RED, GREEN } return string(Color.RED)").equals("\"RED\"");
		code_v4_("enum Status { OK = 200, FAIL = 500 } return string(Status.FAIL)").equals("\"FAIL\"");

		section("Enum instanceof");
		code_v4_("enum Color { RED, GREEN } var c = Color.RED return c instanceof Color").equals("true");
		code_v4_("enum Color { RED, GREEN } var i = 0 return i instanceof Color").equals("false");

		section("Enum and functions");
		code_v4_("enum State { ON, OFF } function isOn(State s) { return s == State.ON } return isOn(State.ON)").equals("true");
		code_v4_("enum State { ON, OFF } function isOn(State s) { return s == State.ON } return isOn(State.OFF)").equals("false");

		section("Enum cast round-trip");
		code_v4_("enum State { ON, OFF } return (0 as State) as integer").equals("0");
		code_v4_("enum Mixed { A, B = 2, C } Mixed x = Mixed.C integer i = x as integer return i").equals("3");

		section("Enum typeOf details");
		code_v4_("enum State { ON, OFF } return typeOf(State.ON)").equals("\"State\"");

		section("Enum string() and values");
		code_v4_("enum Color { RED = 1, GREEN = 2 } return string(Color.GREEN)").equals("\"GREEN\"");

		section("Enum invalid member access");
		code_v4_("enum Color { RED, GREEN } return Color.BLUE").error(Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);
	}
}
