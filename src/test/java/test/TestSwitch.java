package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestSwitch extends TestCommon {

		@Test
	public void testBasic_switch() throws Exception {
		section("Basic switch");
		code_v3_("var x = 1 switch (x) { case 1: return 'one' } return 'none'").equals("\"one\"");
		code_v3_("var x = 2 switch (x) { case 1: return 'one' case 2: return 'two' } return 'none'").equals("\"two\"");
		code_v3_("var x = 3 switch (x) { case 1: return 'one' case 2: return 'two' } return 'none'").equals("\"none\"");
	}

	@Test
	public void testSwitch_with_default() throws Exception {
		section("Switch with default");
		code_v3_("var x = 5 switch (x) { case 1: return 'one' default: return 'other' }").equals("\"other\"");
		code_v3_("var x = 1 switch (x) { case 1: return 'one' default: return 'other' }").equals("\"one\"");
	}

	@Test
	public void testSwitch_with_break() throws Exception {
		section("Switch with break");
		code_v3_("var x = 1 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"one\"");
		code_v3_("var x = 2 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"two\"");
		code_v3_("var x = 3 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"none\"");
	}

	@Test
	public void testSwitch_with_break_and_default() throws Exception {
		section("Switch with break and default");
		code_v3_("var x = 3 var r = '' switch (x) { case 1: r = 'one' break case 2: r = 'two' break default: r = 'default' break } return r").equals("\"default\"");
	}

	@Test
	public void testMultiple_case_values() throws Exception {
		section("Multiple case values");
		code_v3_("var x = 2 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"one or two\"");
		code_v3_("var x = 1 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"one or two\"");
		code_v3_("var x = 3 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"three\"");
	}

	@Test
	public void testSwitch_with_strings() throws Exception {
		section("Switch with strings");
		code_v3_("var x = 'hello' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("1");
		code_v3_("var x = 'world' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("2");
		code_v3_("var x = 'other' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("0");
	}

	@Test
	public void testSwitch_with_expressions() throws Exception {
		section("Switch with expressions");
		code_v3_("var x = 5 switch (x) { case 2 + 3: return 'five' default: return 'other' }").equals("\"five\"");
	}

	@Test
	public void testSwitch_with_multiple_instructions_in_case() throws Exception {
		section("Switch with multiple instructions in case");
		code_v3_("var x = 1 var a = 0 var b = 0 switch (x) { case 1: a = 10 b = 20 break case 2: a = 30 b = 40 break } return a + b").equals("30");
	}

	@Test
	public void testSwitch_in_function() throws Exception {
		section("Switch in function");
		code_v3_("function f(x) { switch (x) { case 1: return 'one' case 2: return 'two' default: return 'other' } } return f(2)").equals("\"two\"");
		code_v3_("function f(x) { switch (x) { case 1: return 'one' case 2: return 'two' default: return 'other' } } return f(5)").equals("\"other\"");
	}

	@Test
	public void testSwitch_with_null() throws Exception {
		section("Switch with null");
		code_v3_("var x = null switch (x) { case null: return 'null' default: return 'other' }").equals("\"null\"");
	}

	@Test
	public void testSwitch_with_boolean() throws Exception {
		section("Switch with boolean");
		code_v3_("var x = true switch (x) { case true: return 'yes' case false: return 'no' }").equals("\"yes\"");
	}

	@Test
	public void testNested_switch() throws Exception {
		section("Nested switch");
		code_v3_("var x = 1 var y = 2 var r = '' switch (x) { case 1: switch (y) { case 1: r = 'x1y1' break case 2: r = 'x1y2' break } break case 2: r = 'x2' break } return r").equals("\"x1y2\"");
	}

	@Test
	public void testSwitch_in_loop() throws Exception {
		section("Switch in loop");
		code_v3_("var s = 0 for (var i = 0; i < 5; i++) { switch (i) { case 0: case 1: s += 10 break default: s += 1 break } } return s").equals("23");
	}

	@Test
	public void testSwitch_with_if_inside_case() throws Exception {
		section("Switch with if inside case");
		code_v3_("var x = 1 var r = 'no' switch (x) { case 1: if (true) { r = 'yes' } break case 2: r = 'two' break } return r").equals("\"yes\"");
		code_v3_("var x = 1 var r = 'no' switch (x) { case 1: r = 'a' if (x == 1) { r = 'b' } r = r + 'c' break case 2: r = 'two' break } return r").equals("\"bc\"");
		code_v3_("var x = 2 var r = 'no' switch (x) { case 1: if (true) { r = 'one' } break case 2: if (true) { r = 'two' } break } return r").equals("\"two\"");
	}

	@Test
	public void testSwitch_with_for_inside_case() throws Exception {
		section("Switch with for inside case");
		code_v3_("var x = 1 var s = 0 switch (x) { case 1: for (var i = 0; i < 3; i++) { s += i } break } return s").equals("3");
	}

	@Test
	public void testSwitch_with_while_inside_case() throws Exception {
		section("Switch with while inside case");
		code_v3_("var x = 1 var s = 0 switch (x) { case 1: var i = 0 while (i < 3) { s += i i++ } break } return s").equals("3");
	}

	@Test
	public void testSwitch_case_with_if_and_no_break() throws Exception {
		section("Switch case with if and no break (fall-through)");
		code_v3_("var a = 0 var x = 1 switch (x) { case 1: a = 4 if (2 == 2) { return 99 } case 2: a = 12 case 3: a = 15 } return a").equals("99");
		code_v3_("var a = 0 var x = 1 switch (x) { case 1: a = 4 if (2 == 3) { return 99 } case 2: a = 12 case 3: a = 15 } return a").equals("15");
	}

	@Test
	public void testEmpty_switch() throws Exception {
		section("Empty switch");
		code_v3_("var x = 1 switch (x) {} return 'ok'").equals("\"ok\"");
	}

}
