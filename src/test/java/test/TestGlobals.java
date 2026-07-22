package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestGlobals extends TestCommon {


		@Test
	public void testGlobals() throws Exception {
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
	}

	@Test
	public void testGlobals_duplicateName() throws Exception {
		// #2863 : l'erreur VARIABLE_NAME_UNAVAILABLE partait sans paramètre,
		// l'éditeur affichait le gabarit brut « Ce nom de variable « {0} » est indisponible »
		section("Duplicate global name (#2863)");
		code("global test global test").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "test");
		code("global a, a").errorWith(Error.VARIABLE_NAME_UNAVAILABLE, "a");
	}

	@Test
	public void testGlobals_typedArrayAssignmentLegacy() throws Exception {
		// #4465 : en v1-3, l'affectation indexée sur un tableau typé `Array` émettait
		// `.putv4(...)`, inexistant sur LegacyArrayLeekValue -> COMPILE_JAVA.
		section("Typed Array assignment in legacy versions (#4465)");
		code_v1_3("global Array t = [] t[0] = 42 return t[0]").equals("42");
		code_v1_3("global Array t = [] t[5] = 12 return t[5]").equals("12");
		code_v1_3("global Array t = [] t['a'] = 7 return t['a']").equals("7");
		code_v1_3("Array t = [] t[1] = 12 return t[1]").equals("12");
		// Même famille : la branche Map typée de compileSetCopy (v1) émettait `.setv4(...)`,
		// qui n'existe sur aucune classe runtime. L'IMPOSSIBLE_CAST est un comportement v1
		// préexistant (déclaration typée sans valeur = Box à null) : on vérifie surtout
		// que la compilation Java passe et que l'erreur reste côté joueur.
		code_v1("global Map m m['a'] = 7 return m['a']").error(Error.IMPOSSIBLE_CAST);
		code_v1("Map m m['a'] = 7 return m['a']").error(Error.IMPOSSIBLE_CAST);
	}

	@Test
	public void testGlobals_operators() throws Exception {
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
		// Compound operators with non-primitive RHS (any type from function return)
		section("Globals compound operators with any-typed RHS");
		code_v2_("function f() { return 5; } global integer x = 10; x += f(); return x;").equals("15");
		code_v2_("function f() { return 5; } global integer x = 10; x -= f(); return x;").equals("5");
		code_v2_("function f() { return 5; } global integer x = 10; x *= f(); return x;").equals("50");
		code_v2_("function f() { return 5; } global real x = 10; x /= f(); return x;").equals("2.0");
		code_v2_("function f() { return 5; } global integer x = 10; x %= f(); return x;").equals("0");
		code_v2_("function f() { return 5; } global integer x = 2; x **= f(); return x;").equals("32");
		// Compound operators with any-typed variable on both sides
		code_v2_("function f() { return 3; } global integer x = 0; x += f(); x += f(); return x;").equals("6");
		code("global x = 12; return x == 5;").equals("false");
		code("global x = 12; return x === 5;").equals("false");
	}

	@Test
	public void testInferredGlobalCompoundInFunction() throws Exception {
		// #4339 : une globale à type inféré (strict, sans type explicite) réaffectée
		// avec `+=`/`-=`/... dans une FONCTION générait du Java invalide : le champ
		// était typé `long` mais le site d'usage croyait le type `any` (snapshot figé
		// avant l'inférence) → `g_x = add(g_x, ...)` sans cast → COMPILE_JAVA.
		section("Inferred global compound-assign inside a function (#4339)");
		code_strict_v4_("global x = 0; function f(any v) { x += v } f(5) return x").equals("5");
		code_strict_v4_("global x = 10; function f(any v) { x -= v } f(5) return x").equals("5");
		code_strict_v4_("global x = 2; function f(any v) { x *= v } f(5) return x").equals("10");
		code_strict_v4_("global x = 17; function f(any v) { x %= v } f(5) return x").equals("2");
		code_strict_v4_("global x = 10; function f(any v) { x /= v } f(5) return x").equals("2");
		// Primitive RHS reste correct
		code_strict_v4_("global x = 0; function f() { x += 5 } f() return x").equals("5");
		// Comportement identique avec un type explicite
		code_strict_v4_("global integer x = 0; function f(any v) { x += v } f(5) return x").equals("5");
		code_strict_v4_("global real x = 0.0; function f(any v) { x += v } f(5) return x").equals("5.0");
	}

	@Test
	public void testTypes() throws Exception {
		section("Types");
		code("global boolean? x = null; return x").equals("null");
		code("global boolean x; x = count([]) == 0; return x").equals("true");
		code("global boolean? x = null; x = 1 == 2; return x").equals("false");
		code("global Array<string> w = split('hello', ''); return w").equals("[\"h\", \"e\", \"l\", \"l\", \"o\"]");
		code_v4_("function f() { return [:] } global Map c = f(); return c").equals("[:]");
		code_strict("global a = [0] a[0] = 12 return a").equals("[12]");
		code_v1("global real x = 56 return x").equals("56");
		code_v2_("global real x = 56 return x").equals("56.0");
		code_v1("global real x x = 56 return x").equals("56");
		code_v2_("global real x x = 56 return x").equals("56.0");
		code_v4_("global Map x x = [:]").equals("[:]");
		code_v4_("global x global y x = (y = [:])").equals("[:]");
		code_v4_("global x global y = [1:2] x = (y[1] = [:])").equals("[:]");
		code_v4_("var z = 2 global x global y = [1:2] x = (y[z] = [:])").equals("[:]");
		code_v4_("var e = 2 var f = 4 global x global y = [1:2] x = (y[e | f << 2] = [:])").equals("[:]");
		code_v4_("global Array<integer> CELL_X = function() => Array<integer> { return [] }() CELL_X").equals("[]");
		code_v4_("global Map<integer, Map<integer, boolean>> x x = (x[1] = [:]) x").equals("[:]");
		code_v4_("global Map<integer, Map<integer, Map<integer, boolean>>> y = [:] global Map<integer, Map<integer, boolean>> x x = (y[5] = [:]) x").equals("[:]");
	}

}