package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestSwitch extends TestCommon {

	@Test
	public void run() throws Exception {

		section("Basic switch");
		code_v3_("var x = 1 switch (x) { case 1: return 'one' } return 'none'").equals("\"one\"");
		code_v3_("var x = 2 switch (x) { case 1: return 'one' case 2: return 'two' } return 'none'").equals("\"two\"");
		code_v3_("var x = 3 switch (x) { case 1: return 'one' case 2: return 'two' } return 'none'").equals("\"none\"");

		section("Switch with default");
		code_v3_("var x = 5 switch (x) { case 1: return 'one' default: return 'other' }").equals("\"other\"");
		code_v3_("var x = 1 switch (x) { case 1: return 'one' default: return 'other' }").equals("\"one\"");

		section("Switch with break");
		code_v3_("var x = 1 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"one\"");
		code_v3_("var x = 2 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"two\"");
		code_v3_("var x = 3 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"none\"");

		section("Switch with break and default");
		code_v3_("var x = 3 var r = '' switch (x) { case 1: r = 'one' break case 2: r = 'two' break default: r = 'default' break } return r").equals("\"default\"");

		section("Multiple case values");
		code_v3_("var x = 2 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"one or two\"");
		code_v3_("var x = 1 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"one or two\"");
		code_v3_("var x = 3 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"three\"");

		section("Switch with strings");
		code_v3_("var x = 'hello' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("1");
		code_v3_("var x = 'world' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("2");
		code_v3_("var x = 'other' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("0");

		section("Switch with expressions");
		code_v3_("var x = 5 switch (x) { case 2 + 3: return 'five' default: return 'other' }").equals("\"five\"");

		section("Switch with multiple instructions in case");
		code_v3_("var x = 1 var a = 0 var b = 0 switch (x) { case 1: a = 10 b = 20 break case 2: a = 30 b = 40 break } return a + b").equals("30");

		section("Switch in function");
		code_v3_("function f(x) { switch (x) { case 1: return 'one' case 2: return 'two' default: return 'other' } } return f(2)").equals("\"two\"");
		code_v3_("function f(x) { switch (x) { case 1: return 'one' case 2: return 'two' default: return 'other' } } return f(5)").equals("\"other\"");

		section("Switch with null");
		code_v3_("var x = null switch (x) { case null: return 'null' default: return 'other' }").equals("\"null\"");

		section("Switch with boolean");
		code_v3_("var x = true switch (x) { case true: return 'yes' case false: return 'no' }").equals("\"yes\"");

		section("Nested switch");
		code_v3_("var x = 1 var y = 2 var r = '' switch (x) { case 1: switch (y) { case 1: r = 'x1y1' break case 2: r = 'x1y2' break } break case 2: r = 'x2' break } return r").equals("\"x1y2\"");

		section("Switch in loop");
		code_v3_("var s = 0 for (var i = 0; i < 5; i++) { switch (i) { case 0: case 1: s += 10 break default: s += 1 break } } return s").equals("23");

		section("Empty switch");
		code_v3_("var x = 1 switch (x) {} return 'ok'").equals("\"ok\"");
	}
}
