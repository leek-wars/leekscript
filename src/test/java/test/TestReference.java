

package test;

public class TestReference extends TestCommon {

	public void run() throws Exception {

		section("Références");
		code_v10("var t = [3, 4, 5]; var a = @t[1] a++ return t;").equals("[3, 4, 5]");
		code_v10("var t = [3, 4, 5]; var a = null a = @t[1] a++ return t;").equals("[3, 4, 5]");
		code_v10("var t = 0; var f = function(a) { t = a }; var b = []; f(b); push(t, 5); return [t, b];").equals("[[5], []]");
		code_v10("var t = 0; var f = function(a) { t = @a }; var b = []; f(b); push(t, 5); return b;").equals("[]");
		code_v10("var t = 0; var f = function(@a) { t = @a }; var b = []; f(b); push(t, 5); return b;").equals("[5]");
		code_v10("var t = 0; var f = function(a) { t; }; f(t); return 'ok';").equals("ok");
		code_v10("function ref() { var a = 2; return @a; } return 1 / ref();").equals("0,5");
		code_v10("var a = @[1, 2, 3]; var b = @a; return (@(b));").equals("[1, 2, 3]");
		code_v10("var count = count([1, 2, 3]) return count").equals("3");
		code_v10("var a = 12; var b = @a; a++; return [a, b]").equals("[13, 12]");
		code_v10("var a = 12; var b = @a; var c = @b; a++; return [a, b, c]").equals("[13, 12, 12]");
		code_v10("var a = [12]; var b = @a[0]; a[0]++; return [a, b]").equals("[[13], 12]");
	}
}