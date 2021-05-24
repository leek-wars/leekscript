package test;

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

		section("Globals operators");
		code("global x = 12; x++; return x;").equals("13");
		code("global x = 12; x--; return x;").equals("11");
		code("global x = 12; ++x; return x;").equals("13");
		code("global x = 12; --x; return x;").equals("11");
		code("global x = 12; x += 5; return x;").equals("17");
		code("global x = 12; x -= 5; return x;").equals("7");
		code("global x = 12; x *= 5; return x;").equals("60");
		code_v10("global x = 12; x /= 5; return x;").equals("2,4");
		code_v11("global x = 12; x /= 5; return x;").equals("2.4");
		code("global x = 12; x %= 5; return x;").equals("2");
		code("global x = 2; x **= 5; return x;").equals("32");
		code("global x = 12; x |= 5; return x;").equals("13");
		code("global x = 12; x &= 5; return x;").equals("4");
		code_v10("global x = 12; x ^= 5; return x;").equals("248832");
		code_v11("global x = 12; x ^= 5; return x;").equals("9");
		code("global x = 12; return x == 5;").equals("false");
		code("global x = 12; return x === 5;").equals("false");
	}
}