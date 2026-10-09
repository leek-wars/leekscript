package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.compiler.Options;
import leekscript.compiler.LeekScript;

@ExtendWith(SummaryExtension.class)
public class TestElvis extends TestCommon {

	@Test
	public void testElvis() throws Exception {
		section("Opérateur ?: (a ? a : b)");
		// a vraie -> a ; a fausse -> b
		code_v4("return 5 ?: 7").equals("5");
		code_v4("return 0 ?: 7").equals("7");
		code_v4("return null ?: 7").equals("7");
		code_v4("return \"\" ?: \"défaut\"").equals("\"défaut\"");
		code_v4("return \"x\" ?: \"défaut\"").equals("\"x\"");
		code_v4("return false ?: true").equals("true");
		code_v4("return [] ?: [1]").equals("[1]");
		code_v4("return [2] ?: [1]").equals("[2]");
		// Avec ou sans espaces
		code_v4("var a = 0 return a?:3").equals("3");
		code_v4("var a = 4 return a ?: 3").equals("4");
		// a est évaluée UNE SEULE FOIS, que la branche soit prise ou non
		code_v4("global calls = 0 function f() { calls++ return 2 } var r = f() ?: 9 return [r, calls]").equals("[2, 1]");
		code_v4("global calls = 0 function f() { calls++ return 0 } var r = f() ?: 9 return [r, calls]").equals("[9, 1]");
		// b n'est évalué que si a est fausse
		code_v4("global calls = 0 function g() { calls++ return 9 } var r = 1 ?: g() return [r, calls]").equals("[1, 0]");
		code_v4("global calls = 0 function g() { calls++ return 9 } var r = 0 ?: g() return [r, calls]").equals("[9, 1]");
		// Chaînage
		code_v4("return 0 ?: null ?: 3").equals("3");
		code_v4("return 0 ?: 2 ?: 3").equals("2");
		code_v4("return 0 ?: 0 ?: 0").equals("0");
		// Dans un ternaire, et un ternaire dans ses opérandes
		code_v4("return true ? 0 ?: 5 : 6").equals("5");
		code_v4("return false ? 1 : 0 ?: 8").equals("8");
		code_v4("return (true ? 0 : 1) ?: 4").equals("4");
		code_v4("var a = 0 return a ?: true ? 1 : 2").equals("1");
		// Typé : integer et real, string
		code_v4("integer a = 0 return a ?: 12").equals("12");
		code_v4("real a = 1.5 return a ?: 12").equals("1.5");
		code_v4("integer a = 3 return a ?: 12.5").equals("3");
		code_v4("string? s = null return s ?: \"vide\"").equals("\"vide\"");
		// Dans une expression plus grande
		code_v4("var a = 0 return 10 + (a ?: 5)").equals("15");
		code_v4("var t = [0, 1] return t[0] ?: t[1]").equals("1");
		// Appels récursifs : l'opérande de gauche peut elle-même utiliser ?:
		code_v4("function fib(n) { return n < 2 ? n : (fib(n - 1) ?: 0) + (fib(n - 2) ?: 0) } return fib(10)").equals("55");
		code_v4("function r(n) { return n <= 0 ? 0 : (r(n - 1) ?: n) } return r(3)").equals("1");
		// Dans une boucle et une fonction anonyme
		code_v4("var s = 0 for (var i = 0; i < 4; i++) { s += (i % 2) ?: 10 } return s").equals("22");
		code_v4("var f = function(x) { return x ?: -1 } return [f(0), f(2)]").equals("[-1, 2]");
		// ?. et ?[ ne sont pas touchés
		code_v4("var o = null return o?.x ?: 3").equals("3");
		code_v4("var a = null return a?[0] ?: 4").equals("4");
		// Opérande gauche dont le Java est un Object malgré son type (assignation, incrément, tableau typé)
		code_v4("integer a = 0 return (a = 5) ?: 1").equals("5");
		code_v4("integer a = 0 return (a += 5) ?: 1").equals("5");
		code_v4("integer a = 0 return a++ ?: 1").equals("1");
		code_v4("integer a = 1 return a++ ?: 9").equals("1");
		code_v4("integer a = 0 var f = function() { a++ } f() return a ?: 8").equals("1");
		code_v4("Array<integer> t = [0] return (t[0] = 4) ?: 1").equals("4");
		code_v4("Array<integer> t = [0] return (t[0] += 4) ?: 1").equals("4");
		code_v4("Array<integer> t = [0] return t[0] ?: 6").equals("6");
		code_v4("real r = 0.0 return r ?: 2.5").equals("2.5");
		code_v4("boolean b = false return b ?: true").equals("true");
		// Mélangé avec || : même priorité, lecture de gauche à droite
		code_v4("return false || 0 ?: 5").equals("5");
		code_v4("return 0 ?: 1 || 2").equals("true");
		// Instruction seule et incrément de boucle
		code_v4("global n = 0 function f() { n++ return 0 } f() ?: f(); return n").equals("2");
		code_v4("global n = 0 function f() { n++ return 0 } for (var i = 0; i < 2; f() ?: f()) { i++ } return n").equals("4");
		// Dans une classe : champ et méthode
		code_v4("class A { integer x = 0 m() { return x ?: 7 } } return new A().m()").equals("7");
		code_v4("class A { x = 0 static s(v) { return v ?: 3 } } return A.s(0)").equals("3");
		// Avant la v4, `?:` n'existe pas ; le ternaire normal est inchangé
		code_v3("return 1 ? 2 : 3").equals("2");
		code_v3("return true?1:2").equals("1");
		code_v3("return 5 ?: 7").error(leekscript.common.Error.OPERATOR_UNEXPECTED);
		code_v1("return 5 ?: 7").error(leekscript.common.Error.OPERATOR_UNEXPECTED);
		// Le ternaire ne change pas en v4 : `? :` espacé, imbriqués, compacts
		code_v4("return true ? 1 : 2").equals("1");
		code_v4("return false ? 1 : true ? 3 : 4").equals("3");
		code_v4("var a = [1, 2] return true?a[0]:a[1]").equals("1");
		code_v4("var a = [1, 2] return false?[1]:[3]").equals("[3]");
	}

	@Test
	public void testElvisOperationsDisabled() throws Exception {
		// Sans comptage d'opérations (CLI), `a ?: b;` en instruction doit rester du Java valide
		assertOps("global n = 0 function f() { n++ return 0 } f() ?: f(); return n", "2");
		assertOps("var x = 0 x ?: 3; return x", "0");
		assertOps("var a = 0 var b = 5 for (var i = 0; i < 2; a ?: b) { i++ } return b", "5");
	}

	private void assertOps(String snippet, String expected) throws Exception {
		var ai = LeekScript.compileSnippet(snippet, "AI", new Options(false));
		ai.init();
		ai.staticInit();
		var v = ai.runIA();
		assertEquals(expected, ai.export(v, new HashSet<>()), snippet);
	}
}
