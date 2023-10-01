package test;

import leekscript.common.Error;

public class TestGlobals extends TestCommon {

	public void run() throws Exception {

		section("Globals");
		code("global x; return x;").equals("null");
		code("global x = null; return x;").equals("null");
		code("global x = 12; return x;").equals("12");
		code("global x = [1, 2, 3]; return x;").equals("[1, 2, 3]");
		code("global x; x = 12 return x;").equals("12");
		code("global x; x = [1, 2, 3]; return x;").equals("[1, 2, 3]");
		code("var r = x; global x; return r;").equals("null");
		code("var r = x; global x = 12; return r;").equals("null");
		code("global r = 2 + 2; return r").equals("4");
		code("global r = [1, 2, 3]; return r").equals("[1, 2, 3]");
		code("global r = 'salut'; return r").equals("\"salut\"");
		code("global r = ['a': 12, 'b': 5]; return r").equals("[\"a\" : 12, \"b\" : 5]");
		code_v1_3("global r = [] return r[1] = 12").equals("12");
		code_v4_("global r = [] return r[1] = 12").equals("null");
		code_strict_v4_("global any r = [] return r[1] = 12").error(Error.ARRAY_OUT_OF_BOUND);
		code_v4_("global r = [:] return r[1] = 12").equals("12");
		code_strict("global any r = [:] return r[1] = 12").equals("12");
		code("global r = [0] return r[0] += 12").equals("12");
		code_v1_3("global r = [] return r[5] += 12").equals("12");
		code_v4_("global r = [] return r[5] += 12").equals("null");
		code_v4_("global r = [:] return r[5] += 12").equals("12");
		code_strict_v4_("global r = [:] return r[5] += 12").error(Error.ASSIGNMENT_INCOMPATIBLE_TYPE);
		code_v1("global r = 12 r = @null").equals("null");
		code("global m = [] return m = m").equals("[]");
		code_v2_("global m = {} return m = m").equals("{}");
		code_v2_("global m = {a: 12} return m = m").equals("{a: 12}");

		section("Globals operators");
		code("global x = 12; x++; return x;").equals("13");
		code("global integer x = 12; x++; return x;").equals("13");
		code("global x = 12; x--; return x;").equals("11");
		code("global integer x = 12; x--; return x;").equals("11");
		code("global x = 12; ++x; return x;").equals("13");
		code("global integer x = 12; ++x; return x;").equals("13");
		code("global x = 12; --x; return x;").equals("11");
		code("global integer x = 12; --x; return x;").equals("11");
		code("global x = 12; x += 5; return x;").equals("17");
		code("global integer x = 12; x += 5; return x;").equals("17");
		code("global x = 12; x -= 5; return x;").equals("7");
		code("global integer x = 12; x -= 5; return x;").equals("7");
		code("global x = 12; x *= 5; return x;").equals("60");
		code("global integer x = 12; x *= 5; return x;").equals("60");
		code_v1("global x = 12; x /= 5; return x;").equals("2,4");
		code_v2_("global x = 12; x /= 5; return x;").equals("2.4");
		code_v2_("global real x = 12; x /= 5; return x;").equals("2.4");
		code("global x = 12; x %= 5; return x;").equals("2");
		code("global integer x = 12; x %= 5; return x;").equals("2");
		code("global x = 2; x **= 5; return x;").equals("32");
		code("global integer x = 2; x **= 5; return x;").equals("32");
		code("global x = 12; x |= 5; return x;").equals("13");
		code("global integer x = 12; x |= 5; return x;").equals("13");
		code("global x = 12; x &= 5; return x;").equals("4");
		code("global integer x = 12; x &= 5; return x;").equals("4");
		code_v1("global x = 12; x ^= 5; return x;").equals("248832");
		code_v1("global integer x = 12; x ^= 5; return x;").equals("248832");
		code_v2_("global x = 12; x ^= 5; return x;").equals("9");
		code("global x = 12; return x == 5;").equals("false");
		code("global x = 12; return x === 5;").equals("false");

		section("Types");
		code("global boolean? x = null; return x").equals("null");
		code("global boolean x; x = count([]) == 0; return x").equals("true");
		code("global boolean? x = null; x = 1 == 2; return x").equals("false");
		code("global Array<string> w = split('hello', ''); return w").equals("[\"h\", \"e\", \"l\", \"l\", \"o\"]");
		code_v4_("function f() { return [:] } global Map c = f(); return c").equals("[:]");
	}
}