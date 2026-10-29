package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;


@ExtendWith(SummaryExtension.class)
public class TestSet extends TestCommon {


		@Test
	public void testSet_constructor() throws Exception {
		section("Set.constructor()");
		code("return <1, 2>").debug().equals("<1, 2>");
		code("return <(1 > 2), (1 < 2)>").equals("<false, true>");
		code("return <\"abc\", 1>").equals("<\"abc\", 1>");
		code("return <>").equals("<>");
		DISABLED_code("return <<>>").equals("<<>>");
		code("<1, 2, 3, 4>").ops(8);
		code_v3_("Set<integer> a = new Set() return a").equals("<>");
	}

	/**
	 * Littéral set avec intervalle d'entiers `<a..b>` (#2335).
	 */
	@Test
	public void testSet_range() throws Exception {
		section("Set range literal");
		code("return <1..5>").equals("<1, 2, 3, 4, 5>");
		code("return <1..1>").equals("<1>");
		// Sens décroissant
		code("return <5..1>").equals("<5, 4, 3, 2, 1>");
		// Mélange valeurs simples et intervalles
		code("return <1, 5..8, 20>").equals("<1, 5, 6, 7, 8, 20>");
		code("return <1..3, 10, 20..22>").equals("<1, 2, 3, 10, 20, 21, 22>");
		// Bornes dynamiques
		code("var a = 3 var b = 7 return <a..b>").equals("<3, 4, 5, 6, 7>");
		// Doublons fusionnés par le set
		code("return <1..3, 2..4>").equals("<1, 2, 3, 4>");
		// Équivalences
		code("return <1..3> == <1, 2, 3>").equals("true");
		code_v1_4("return setSize(<1..100>)").equals("100");
		// Un intervalle entre crochets reste un élément intervalle (pas d'expansion)
		code("return <[1..10]>").equals("<[1..10]>");
	}

	@Test
	public void testSet_typing() throws Exception {
		section("Set typing");
		code_strict_v4_("Set i = <1, 2>; return i instanceof Set").equals("true");
		code_strict_v4_("Set<integer> i = <1, 2>; return i instanceof Set").equals("true");
		// Passing Array? to Set? parameter (was generating incompatible cast)
		code_strict_v4_("function f1(Array? arr) { return f2(arr) } function f2(Set? s) { return s == null } return f1(null)").equals("true");
	}

	@Test
	public void testSet_operator_EqualsEquals() throws Exception {
		section("Set.operator ==");
		code("return <> == <>").equals("true");
		code("return <1> == <1>").equals("true");
		code("return <'a', 'b'> == <1, 2>").equals("false");
		code("return <'a', 'b'> == <'a', 'b'>").equals("true");
		// == not deep on sets
		DISABLED_code("return <'a', <1, 2>> == <'a', <1, 2>>").equals("true");
		DISABLED_code("return <[1], [2 : 3]> == <[1], [2 : 3]>").equals("true");
		code("return <'a', 'b'> == <'a', 'b', 'c', 'd'>").equals("false");
	}

	@Test
	public void testSet_operator_Not() throws Exception {
		section("Set operator !");
		code("var i = <> return !i").equals("true");
		code("var i = <1> return !i").equals("false");
	}

	@Test
	public void testSet_operator_Add() throws Exception {
		section("Set operator +");
		// Comme un tableau : union avec un ensemble, copie augmentée d'une valeur
		code("var s = <1, 2> return s + 3").equals("<1, 2, 3>");
		code("var s = <1, 2> return s + <2, 3>").equals("<1, 2, 3>");
		code("var s = <1, 2> var t = s + 3 return s").equals("<1, 2>");
		code("var s = <1, 2> s += 3 return s").equals("<1, 2, 3>");
		code("var s = <1, 2> s += <5> return s").equals("<1, 2, 5>");
		// `+=` ajoute en place, comme sur un tableau : les autres références voient l'ajout
		code_v2_("var s = <1> var a = s s += 2 return a").equals("<1, 2>");
		code("global s = <1> s += 2 return s").equals("<1, 2>");
		code("var s = <1, 2> return s + 'a'").equals("\"<1, 2>a\"");
		code_v4_("function g(Set s, y) { return s + y } return [g(<1>, 'a'), g(<1>, 2)]").equals("[\"<1>a\", <1, 2>]");
		code_strict_v4_("Set<integer> s = <1> Set<integer> t = s + 2 return t").noWarning();
		code_v4_("var s = <> for (var i = 0; i < 1000000; ++i) s += i").max_ram(10_000).error(Error.OUT_OF_MEMORY);
		// Avec un tableau v1-v3 à droite, `+` concatène
		code_v1_3("return <1> + [2]").equals("[<1>, 2]");
	}

	@Test
	public void testSet_numeric_value() throws Exception {
		section("Set as a number");
		// Hors de `+`, un ensemble vaut sa taille, comme les autres conteneurs
		code("var s = <1, 2> return 1 + s").equals("3");
		code("var s = <1, 2> return s * 2").equals("4");
		code("var s = <1, 2> return s > 1").equals("true");
	}

	@Test
	public void testSet_setPut() throws Exception {
		section("Set.setPut()");
		code_v1_4("var i = <1, 2> setPut(i, 3) return i").debug().equals("<1, 2, 3>");
		code_v1_4("var i = <1, 2> setPut(i, 3) setPut(i, 4) return i").equals("<1, 2, 3, 4>");
		code_v1_4("var i = <1, 2> setPut(i, 3) setPut(i, 3) return i").equals("<1, 2, 3>");
		code_v1_4("var i = <1, 2> setPut(i, 1) return i").equals("<1, 2>");
		code_v1_4("var i = <> setPut(i, 1) return i").equals("<1>");
		code_v1_4("var i = <> setPut(i, 'okay') setPut(i, 'okay') return i").equals("<\"okay\">");
		code_v1_4("var i = <> setPut(i, 'okay') setPut(i, 1) return i").equals("<\"okay\", 1>");
		code_v1_4("var i = <1, 2> return setPut(i, 3)").equals("true");
		code_v1_4("var i = <1, 2> return setPut(i, 2)").equals("false");
	}

	@Test
	public void testSet_setPutAll() throws Exception {
		section("Set.setPutAll()");
		code_v1_4("var s = <1, 2> setPutAll(s, <2, 3>) return s").equals("<1, 2, 3>");
		code_v1_4("var s = <> setPutAll(s, <'a', 1>) return s").equals("<\"a\", 1>");
		code_v1_4("var s = <1> setPutAll(s, <>) return s").equals("<1>");
		code_v1_4("var s = <> setPutAll(s, <>) return s").equals("<>");
		code_v1_4("var s = <1, 2> setPutAll(s, s) return s").equals("<1, 2>");
		code_v1_4("var s = <1> var t = <2> setPutAll(s, t) return t").equals("<2>");
		code_v1_4("var s = <1> return setPutAll(s, <2>)").equals("null");
		// En place : les autres références voient l'ajout
		code_v2_4("var s = <1> var a = s setPutAll(s, <2, 3>) return a").equals("<1, 2, 3>");
		code_v4("function f(x, y) { setPutAll(x, y) return x } return f(<1>, <2>)").equals("<1, 2>");
		// 1 + 1 par élément de droite, comme `s += t` (qui compte en plus l'opérateur)
		code_v1_4("var s = <1, 2> var t = <3, 4, 5> setPutAll(s, t)").ops(16);
		code_v2_("var s = <1, 2> var t = <3, 4, 5> s += t").ops(17);
		code_v1_4("var s = <1, 2> var t = <3, 4, 5, 6, 7, 8> setPutAll(s, t)").ops(25);
		code_v4("var s = <> for (var i = 0; i < 1000000; ++i) setPutAll(s, <i>)").max_ram(10_000).error(Error.OUT_OF_MEMORY);
		// v5 : forme méthode
		code_v5("var s = <1, 2> s.putAll(<2, 3>) return s").equals("<1, 2, 3>");
		code_v5("var s = <1> var a = s s.putAll(<2, 3>) return a").equals("<1, 2, 3>");
		code_v5("var s = <1, 2> var t = <3, 4, 5> s.putAll(t)").ops(16); // comme la forme libre
		code_v5("return setPutAll(<1>, <2>)").error(Error.REMOVED_FUNCTION_REPLACEMENT);
		// Une fonction ou une variable du joueur du même nom reste prioritaire
		code("function setPutAll(a, b) { return 12 } return setPutAll(<1>, <2>)").equals("12");
		code_strict("function setPutAll(a, b) { return 12 } return setPutAll(<1>, <2>)").equals("12");
		code("var setPutAll = 3 return setPutAll").equals("3");
		code("global setPutAll = 5 return setPutAll").equals("5");
	}

	@Test
	public void testSet_setRemove() throws Exception {
		section("Set.setRemove()");
		code_v1_4("var i = <1, 2> setRemove(i, 1) return i").equals("<2>");
		code_v1_4("var i = <1, 2> setRemove(i, 3) return i").equals("<1, 2>");
		code_v1_4("var i = <1, 2> setRemove(i, 1) setRemove(i, 2) return i").equals("<>");
		code_v1_4("var i = <1, 2> setRemove(i, 1) setRemove(i, 1) return i").equals("<2>");
		code_v1_4("var i = <> setRemove(i, 1) return i").equals("<>");
		code_v1_4("var i = <'okay', 1> setRemove(i, 'okay') return i").equals("<1>");
	}

	@Test
	public void testSet_setClear() throws Exception {
		section("Set.setClear()");
		code_v1_4("var i = <1, 2>; setClear(i); return i").equals("<>");
		code_v1_4("var i = <1, 2>; setClear(i); setClear(i); return i").equals("<>");
		code_v1_4("var i = <>; setClear(i); return i").equals("<>");
		code_v1_4("var i = <'okay', 1>; setClear(i); return i").equals("<>");
	}

	@Test
	public void testSet_setContains() throws Exception {
		section("Set.setContains()");
		code_v1_4("var i = <1, 2>; return setContains(i, 1)").equals("true");
		code_v1_4("var i = <1, 2>; return setContains(i, 3)").equals("false");
		code_v1_4("var i = <>; return setContains(i, 1)").equals("false");
		code_v1_4("var i = <'okay', 1>; return setContains(i, 'okay')").equals("true");
		code_v1_4("var i = <'okay', 1>; return setContains(i, 'other')").equals("false");
	}

	@Test
	public void testSet_in() throws Exception {
		section("Set.in");
		code("var i = <1, 2>; return 1 in i").equals("true");
		code("var i = <1, 2>; return 3 in i").equals("false");
		code("var i = <>; return 1 in i").equals("false");
		code("var i = <'okay', 1>; return 'okay' in i").equals("true");
		code("var i = <'okay', 1>; return 'other' in i").equals("false");
	}

	@Test
	public void testSet_not_in() throws Exception {
		section("Set.not_in");
		code("var i = <1, 2>; return 3 not in i").equals("true");
		code("var i = <1, 2>; return 1 not in i").equals("false");
	}

	@Test
	public void testSet_setSize() throws Exception {
		section("Set.setSize()");
		code_v1_4("var i = <1, 2>; return setSize(i)").equals("2");
		code_v1_4("var i = <1, 2>; setRemove(i, 1); return setSize(i)").equals("1");
		code_v1_4("var i = <>; return setSize(i)").equals("0");
		code_v1_4("var i = <'okay', 1>; return setSize(i)").equals("2");
	}

	@Test
	public void testSet_setIsEmpty() throws Exception {
		section("Set.setIsEmpty()");
		code_v1_4("var i = <1, 2>; setRemove(i, 1); return setIsEmpty(i)").equals("false");
		code_v1_4("var i = <1, 2>; setClear(i); return setIsEmpty(i)").equals("true");
		code_v1_4("var i = <1>; setRemove(i, 1); return setIsEmpty(i)").equals("true");
		code_v1_4("var i = <>; return setIsEmpty(i)").equals("true");
		code_v1_4("var i = <'okay', 1>; return setIsEmpty(i)").equals("false");
	}

	@Test
	public void testSet_setSubsetOf() throws Exception {
		section("Set.setSubsetOf()");
		code_v1_4("var i = <1, 2>; var j = <1, 2, 3>; return setIsSubsetOf(i, j)").equals("true");
		code_v1_4("var i = <1, 2>; var j = <1, 2, 3>; return setIsSubsetOf(j, i)").equals("false");
		code_v1_4("var i = <1, 2>; var j = <1, 2>; return setIsSubsetOf(i, j)").equals("true");
		code_v1_4("var i = <1, 2>; var j = <1, 2>; return setIsSubsetOf(j, i)").equals("true");
		code_v1_4("var i = <>; var j = <1, 2>; return setIsSubsetOf(i, j)").equals("true");
		code_v1_4("var i = <>; var j = <1, 2>; return setIsSubsetOf(j, i)").equals("false");
		code_v1_4("var i = <>; var j = <>; return setIsSubsetOf(i, j)").equals("true");
	}

	@Test
	public void testSet_iteration() throws Exception {
		section("Set iteration");
		code("var s = <1, 2, 3, 4, 5> var x = 0 for (var y in s) x += y return x").equals("15");
	}

	@Test
	public void testSet_union() throws Exception {
		section("Set.union()");
		code_v1_4("var s1 = <1, 2, 3> var s2 = <4, 5, 6> return setUnion(s1, s2)").equals("<1, 2, 3, 4, 5, 6>");
	}

	@Test
	public void testSet_intersection() throws Exception {
		section("Set.intersection()");
		code_v1_4("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setIntersection(s1, s2)").debug().equals("<2, 3>");
	}

	@Test
	public void testSet_difference() throws Exception {
		section("Set.difference()");
		code_v1_4("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setDifference(s1, s2)").debug().equals("<1>");
	}

	@Test
	public void testSet_disjunction() throws Exception {
		section("Set.disjunction()");
		code_v1_4("var s1 = <1, 2, 3> var s2 = <2, 3, 4> return setDisjunction(s1, s2)").debug().equals("<1, 4>");
	}

	@Test
	public void testSet_filter() throws Exception {
		section("Set.filter()");
		code_v1_4("var s = <1, 2, 3, 4, 5> return setFilter(s, x -> x > 2)").equals("<3, 4, 5>");
		code_v1_4("var s = <1, 2, 3> return setFilter(s, x -> false)").equals("<>");
		code_v1_4("var s = <1, 2, 3> return setFilter(s, x -> true)").equals("<1, 2, 3>");
		code_v1_4("var s = <> return setFilter(s, x -> true)").equals("<>");
	}

	@Test
	public void testSet_unclosed() throws Exception {
		section("Unclosed set");
		// Un `)` ou un `;` après le dernier élément : c'est la fermeture du set qui manque
		code_v4_("var a = 1 if (a in <1, 2, 3) return [] return [a]").compileError(Error.CLOSING_CHEVRON_EXPECTED);
		code_v4_("var s = <1, 2; return s").compileError(Error.CLOSING_CHEVRON_EXPECTED);
		code_v4_("var a = 1 if (a in <1, 2, 3>) return [] return [a]").equals("[]");
	}

	@Test
	public void testSet_toArray() throws Exception {
		section("Set.toArray()");
		code_v1_4("var s = <1, 2, 3> return setToArray(s)").equals("[1, 2, 3]");
	}

	@Test
	public void testSet_clone() throws Exception {
		section("Set clone");
		code_v1_4("var s = <1, 2, 3> var s2 = clone(s) setClear(s) return s2").equals("<1, 2, 3>");
		code_v1_4("var s = <1, 2, 3> var s2 = clone(s) setPut(s, 4) return s2").equals("<1, 2, 3>");
	}

	@Test
	public void testSet_helpers() throws Exception {
		section("Set helpers (#24)");
		// Réductions — forme méthode (v5) et forme libre (v4), gated v4
		code_v5("return <1, 2, 3>.sum()").equals("6.0");
		code_v1_4("return setSum(<1, 2, 3>)").error(Error.FUNCTION_NOT_AVAILABLE); // v5 seulement, en forme méthode
		code_v5("return <1, 2, 3, 4>.average()").equals("2.5");
		code_v5("return <>.average()").equals("0.0");
		code_v5("var s = <3, 1, 2> return s.min()").equals("1");
		code_v5("return <3, 1, 2>.max()").equals("3");
		code_v5("return <>.min()").equals("null");
		code_v5("return <>.max()").equals("null");
		code_v5("return <2, 4, 6>.every(x -> x % 2 == 0)").equals("true");
		code_v5("return <2, 4, 5>.every(x -> x % 2 == 0)").equals("false");
		code_v5("return <1, 2, 3>.some(x -> x > 2)").equals("true");
		code_v5("return <1, 2, 3>.some(x -> x > 5)").equals("false");
		code_v5("return <1, 2, 3>.fold((r, v) -> r + v, 10)").equals("16");
		code_v1_4("return setFold(<1, 2, 3>, function(r, v) { return r + v }, 0)").error(Error.FUNCTION_NOT_AVAILABLE);
		// iter (effet de bord via une globale capturée ; set dans une var pour éviter l'ambiguïté 0 < 1)
		code_v5("global total = 0 var s = <1, 2, 3> s.iter(function(v) { total += v }) return total").equals("6");
		// Conversions
		code_v5("return <10, 20, 30>.toMap()").equals("[0 : 10, 1 : 20, 2 : 30]");
		code_v5("return [1 : 10, 2 : 20].keySet()").equals("<1, 2>");
		// Gating : indisponibles en v1-3 (forme libre)

	}

}
