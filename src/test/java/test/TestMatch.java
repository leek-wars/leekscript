package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestMatch extends TestCommon {

	@Test
	public void testMatch() throws Exception {
		section("Match expression (#2404)");
		// Motifs littéraux + joker, en tant qu'EXPRESSION (renvoie une valeur)
		code_v5("return match (1) { 1 -> \"a\", 2 -> \"b\", _ -> \"c\" }").equals("\"a\"");
		code_v5("return match (2) { 1 -> \"a\", 2 -> \"b\", _ -> \"c\" }").equals("\"b\"");
		code_v5("return match (9) { 1 -> \"a\", 2 -> \"b\", _ -> \"c\" }").equals("\"c\"");
		// Plusieurs valeurs par branche
		code_v5("return match (3) { 1, 2, 3 -> \"low\", _ -> \"high\" }").equals("\"low\"");
		code_v5("return match (5) { 1, 2, 3 -> \"low\", _ -> \"high\" }").equals("\"high\"");
		// Pas de défaut : null si aucune branche ne correspond
		code_v5("return match (9) { 1 -> \"a\" }").equals("null");
		// Affecté à une variable
		code_v5("var x = match (2) { 1 -> 10, 2 -> 20, _ -> 0 } return x").equals("20");
		// Sujet évalué UNE SEULE FOIS (effet de bord compté une fois)
		code_v5("global calls = 0 function f() { calls++ return 2 } var r = match (f()) { 1 -> \"a\", 2 -> \"b\", 3 -> \"c\" } return [r, calls]").equals("[\"b\", 1]");
		// Sujet chaîne
		code_v5("return match (\"x\") { \"a\" -> 1, \"x\" -> 2, _ -> 3 }").equals("2");
		// Match imbriqué (valeur de branche contenant un match)
		code_v5("return match (1) { 1 -> match (2) { 2 -> \"ok\", _ -> \"no\" }, _ -> \"no\" }").equals("\"ok\"");
		// Match au milieu d'une expression
		code_v5("return 10 + match (1) { 1 -> 5, _ -> 0 }").equals("15");
		// Sujet variable, branches calculées
		code_v5("var n = 4 return match (n % 2) { 0 -> \"pair\", _ -> \"impair\" }").equals("\"pair\"");
		// Motif = identifiant nu (matche la VALEUR de la variable, pas une lambda) — revue #2404
		code_v5("var y = 1 return match (1) { y -> \"a\", _ -> \"b\" }").equals("\"a\"");
		code_v5("var y = 9 return match (1) { y -> \"a\", _ -> \"b\" }").equals("\"b\"");
		// Match avec seulement un défaut : le sujet est tout de même évalué (effet de bord) — revue #2404
		code_v5("global c = 0 function ff() { c = c + 1 return 2 } var r = match (ff()) { _ -> 99 } return [r, c]").equals("[99, 1]");
		// En v1-4, `match` reste un identifiant ordinaire (byte-identique)
		code_v4("var match = 7 return match").equals("7");
		code_v4("function match() { return 3 } return match()").equals("3");
	}
}
