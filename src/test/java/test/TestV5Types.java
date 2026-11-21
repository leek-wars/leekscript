package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

/**
 * Règles de typage propres à LeekScript 5, gelées en v1-4 parce qu'elles casseraient des IA
 * existantes : chaque section fige aussi le comportement v4.
 */
@ExtendWith(SummaryExtension.class)
public class TestV5Types extends TestCommon {

	@Test
	public void nullToNumber() throws Exception {
		section("v5 : null converti en integer / real = IMPOSSIBLE_CAST (#5389)");
		// Déclarations
		code_v5_("Map m = [:] real r = m[0] return r").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map m = [:] integer i = m[0] return i").error(Error.IMPOSSIBLE_CAST);
		code_strict_v5_("Map m = [:] real r = m[0] return r").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var m = [:] integer n = m['k'] n++ return n").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var x = null integer i = x return i").error(Error.IMPOSSIBLE_CAST);
		// Affectations, globales, cases de tableau typé
		code_v5_("var m = [:] integer i = 2 i = m[0] return i").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var m = [:] real r = 2.5 r = m[0] return r").error(Error.IMPOSSIBLE_CAST);
		code_v5_("global integer g = 1 var m = [:] g = m[0] return g").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<integer> a = [1] var m = [:] a[0] = m[0] return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, real> a = ['a': 1.5] var m = [:] a['a'] = m[0] return a").error(Error.IMPOSSIBLE_CAST);
		// Paramètres et retours typés
		code_v5_("function f(integer x) { return x } var m = [:] return f(m[0])").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f(real x) { return x } var m = [:] return f(m[0])").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f() => integer { var m = [:] return m[0] } return f()").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f() => real { var m = [:] return m[0] } return f()").error(Error.IMPOSSIBLE_CAST);
		// `return?` écarte null avant de convertir
		code_strict_v5_("function f(integer? x) => integer { return? x return 12 } return [f(5), f(null), f(0)]").equals("[5, 12, 12]");
		code_v5_("function f(any x) => real { return? x return 1.5 } return [f(2.5), f(null)]").equals("[2.5, 1.5]");
		// Champs typés, chemin statique et chemin dynamique (cf TestFieldTypeRepro)
		code_v5_("class A { integer x } var m = [:] A a = new A() a.x = m[0] return a.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { real x } var m = [:] A a = new A() a.x = m[0] return a.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { integer x } var m = [:] var a = new A() a.x = m[0] return a.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { static integer x } var m = [:] A.x = m[0] return A.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { integer x constructor() { var m = [:] this.x = m[0] } } return new A().x").error(Error.IMPOSSIBLE_CAST);
		// Conversion explicite
		code_v5_("var m = [:] return m[0] as integer").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var m = [:] return m[0] as real").error(Error.IMPOSSIBLE_CAST);

		section("v5 : le reste de la conversion vers integer / real ne change pas (#5389)");
		code_v5_("var x = 2.7 integer i = x return i").equals("2");
		code_v5_("var x = true integer i = x return i").equals("1");
		code_v5_("var x = 12 real r = x return r").equals("12.0");
		code_v5_("var x = [1, 2, 3] integer i = x return i").equals("3");
		code_v5_("class A { integer x } var a = new A() a.x = 1.5 return a.x").equals("1");
		// Un type nullable garde null
		code_v5_("Map m = [:] real? r = m[0] return r").equals("null");
		code_v5_("Map m = [:] integer? i = m[0] return i").equals("null");
		code_v5_("class A { integer? x = 2 } var a = new A() a.x = null return a.x").equals("null");
		code_v5_("function f(integer? x) { return x } var m = [:] return f(m[0])").equals("null");
		// null comme opérande arithmétique vaut toujours 0 : ce n'est pas une conversion
		code_v5_("integer i = 1 var x = null i += x return i").equals("1");
		code_v5_("real r = 1.5 var x = null r *= x return r").equals("0.0");

		section("v1-4 : null converti en integer / real vaut toujours 0 / 0.0 (#5389)");
		code_v2_4("var m = [:] real r = m[0] return r").equals("0.0");
		code_v2_4("var m = [:] integer i = m[0] return i").equals("0");
		code_strict_v4("Map m = [:] real r = m[0] return r").equals("0.0");
		code_v4("var m = [:] integer n = m['k'] n++ return n").equals("1");
		code_v4("Array<integer> a = [1] var m = [:] a[0] = m[0] return a").equals("[0]");
		code_v2_4("function f(integer x) { return x } var m = [:] return f(m[0])").equals("0");
		code_v2_4("function f() => real { var m = [:] return m[0] } return f()").equals("0.0");
		code_v4("class A { integer x } var m = [:] A a = new A() a.x = m[0] return a.x").equals("0");
		code_v4("class A { static integer x } var m = [:] A.x = m[0] return A.x").equals("0");
		code_v4("var m = [:] return m[0] as integer").equals("0");
	}
}
