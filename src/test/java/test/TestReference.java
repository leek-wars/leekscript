

package test;

public class TestReference extends TestCommon {

	public void run() throws Exception {

		section("Références");
		code("var a = [] var b = @a push(b, 1) return a").equals("[1]");
		code("var a = [1] var b = @a[0] b++ return a").equals("[1]");
		code_v1("var f = function(@a) { a++ } var b = 10 f(b) return b").equals("11");
		code_v2_("var f = function(@a) { a++ } var b = 10 f(b) return b").equals("10");
		code("var t = [3, 4, 5]; var a = @t[1] a++ return t;").equals("[3, 4, 5]");
		code("var t = [3, 4, 5]; var a = null a = @t[1] a++ return t;").equals("[3, 4, 5]");
		code("var t = 0; var f = function(a) { t = a }; f([]);").equals("null");
		code("var t = 0; var f = function(a) { t = a }; var b = []; f(b);").equals("null");
		code("var t = 0; var f = function(a) { t = a }; var b = []; f(b); push(t, 5);").equals("null");
		code_v1("var t = 0; var f = function(a) { t = a }; var b = []; f(b); push(t, 5); return [t, b];").equals("[[5], []]");
		code_v2_("var t = 0; var f = function(a) { t = a }; var b = []; f(b); push(t, 5); return [t, b];").equals("[[5], <...>]");
		code_v1("var t = 0; var f = function(a) { t = @a }; var b = []; f(b); push(t, 5); return b;").equals("[]");
		code_v2_("var t = 0; var f = function(a) { t = @a }; var b = []; f(b); push(t, 5); return b;").equals("[5]");
		code("var t = 0; var f = function(@a) { t = @a }; var b = []; f(b); push(t, 5); return b;").equals("[5]");
		code("var t = 0; var f = function(a) { t; }; f(t); return 'ok';").equals("\"ok\"");
		code_v1("function ref() { var a = 2; return @a; } return 1 / ref();").equals("0,5");
		code_v2_("function ref() { var a = 2; return @a; } return 1 / ref();").equals("0.5");
		code("var a = @[1, 2, 3]; var b = @a; return (@(b));").equals("[1, 2, 3]");
		code("var count = count([1, 2, 3]) return count").equals("3");
		code("var a = 12; var b = @a; a++; return [a, b]").equals("[13, 12]");
		code("var a = 12; var b = @a; var c = @b; a++; return [a, b, c]").equals("[13, 12, 12]");
		code("var a = [12]; var b = @a[0]; a[0]++; return [a, b]").equals("[[13], 12]");
		code("var a = @[1, 2, 3]; var b = @a; (@(b));").equals("null");
		code("var a = @[1, 2, 3]; var b = @a; return (@(b));").equals("[1, 2, 3]");
		code("var x = 7 var y = @x var f = function() { y = x } f() return y").equals("7");
		code("global x = 7 var y = @x var f = function() { y = x } f() return y").equals("7");
	}
}
