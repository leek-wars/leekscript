package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

/**
 * Dispatch O(1) du switch : quand tous les labels sont des constantes entières ou
 * des littéraux chaîne, le compilateur émet un vrai switch Java au lieu de la
 * chaîne de comparaisons. Ces tests verrouillent l'équivalence sémantique avec
 * l'ancien comportement, en particulier l'égalité lâche de eq().
 */
@ExtendWith(SummaryExtension.class)
public class TestSwitchOpt extends TestCommon {

	@Test
	public void testLoose_equality_preserved() throws Exception {
		section("Switch: égalité lâche préservée");
		// eq("1", 1) est vrai : le sujet n'est pas un Long, on doit retomber sur la chaîne.
		code_v3_("var x = '1' switch (x) { case 1: return 'match' default: return 'no' }").equals("\"match\"");
		code_v3_("var x = 1 switch (x) { case '1': return 'match' default: return 'no' }").equals("\"match\"");
		code_v3_("var x = true switch (x) { case 1: return 'match' default: return 'no' }").equals("\"match\"");
		code_v3_("var x = false switch (x) { case 0: return 'match' default: return 'no' }").equals("\"match\"");
		code_v3_("var x = 1.0 switch (x) { case 1: return 'match' default: return 'no' }").equals("\"match\"");
		code_v3_("var x = '' switch (x) { case 0: return 'match' default: return 'no' }").equals("\"match\"");
	}

	@Test
	public void testInt_range_guard() throws Exception {
		section("Switch: garde de troncature int");
		// 2^32 + 1 tronqué en int vaut 1 : ne doit surtout pas matcher `case 1`.
		code_v3_("var x = 4294967297 switch (x) { case 1: return 'BAD' default: return 'ok' }").equals("\"ok\"");
		code_v3_("integer x = 4294967297 switch (x) { case 1: return 'BAD' default: return 'ok' }").equals("\"ok\"");
		code_v3_("var x = -4294967295 switch (x) { case 1: return 'BAD' default: return 'ok' }").equals("\"ok\"");
		// Label hors plage int : repli sur la chaîne, mais le résultat doit rester juste.
		code_v3_("var x = 4294967297 switch (x) { case 4294967297: return 'ok' default: return 'no' }").equals("\"ok\"");
		code_v3_("integer x = 4294967297 switch (x) { case 4294967297: return 'ok' default: return 'no' }").equals("\"ok\"");
	}

	@Test
	public void testSentinel_collisions() throws Exception {
		section("Switch: sentinelles");
		// Integer.MIN_VALUE est un label : la sentinelle doit prendre une autre valeur.
		code_v3_("integer x = 5 switch (x) { case -2147483648: return 'min' default: return 'other' }").equals("\"other\"");
		code_v3_("integer x = -2147483648 switch (x) { case -2147483648: return 'min' default: return 'other' }").equals("\"min\"");
		code_v3_("integer x = 2147483647 switch (x) { case 2147483647: return 'max' default: return 'other' }").equals("\"max\"");
		// Sujet chaîne nul : aucun label ne doit matcher.
		code_v3_("var x = null switch (x) { case 'a': return 'a' default: return 'other' }").equals("\"other\"");
	}

	@Test
	public void testNegative_labels() throws Exception {
		section("Switch: labels négatifs");
		code_v3_("var x = -2 switch (x) { case -1: return 'a' case -2: return 'b' default: return 'c' }").equals("\"b\"");
		code_v3_("integer x = -1 switch (x) { case -1: return 'a' case -2: return 'b' default: return 'c' }").equals("\"a\"");
		code_v3_("var x = 0 switch (x) { case -1: return 'a' case 0: return 'b' default: return 'c' }").equals("\"b\"");
	}

	@Test
	public void testDuplicate_labels() throws Exception {
		section("Switch: labels dupliqués");
		// Un switch Java refuserait de compiler : on reste sur la chaîne, premier gagne.
		code_v3_("var x = 1 switch (x) { case 1: return 'first' case 1: return 'second' }").equals("\"first\"");
		code_v3_("var x = 'a' switch (x) { case 'a': return 'first' case 'a': return 'second' }").equals("\"first\"");
		code_v3_("integer x = 2 switch (x) { case 1: case 2: return 'first' case 2: return 'second' }").equals("\"first\"");
	}

	@Test
	public void testFallthrough_optimized() throws Exception {
		section("Switch: fallthrough sur le chemin optimisé");
		code_v3_("var x = 1 var r = '' switch (x) { case 1: r += 'a' case 2: r += 'b' case 3: r += 'c' } return r").equals("\"abc\"");
		code_v3_("integer x = 2 var r = '' switch (x) { case 1: r += 'a' case 2: r += 'b' case 3: r += 'c' } return r").equals("\"bc\"");
		code_v3_("string x = 'b' var r = '' switch (x) { case 'a': r += 'a' case 'b': r += 'b' case 'c': r += 'c' } return r").equals("\"bc\"");
		// default au milieu : la position dans le fallthrough doit être conservée.
		code_v3_("var x = 9 var r = '' switch (x) { case 1: r += 'a' default: r += 'd' case 2: r += 'b' } return r").equals("\"db\"");
		code_v3_("integer x = 9 var r = '' switch (x) { case 1: r += 'a' default: r += 'd' case 2: r += 'b' } return r").equals("\"db\"");
		code_v3_("integer x = 1 var r = '' switch (x) { case 1: r += 'a' default: r += 'd' case 2: r += 'b' } return r").equals("\"adb\"");
	}

	@Test
	public void testDefault_merged_with_case() throws Exception {
		section("Switch: case et default sur le même corps");
		code_v3_("var x = 5 switch (x) { case 1: default: return 'd' }").equals("\"d\"");
		code_v3_("integer x = 1 switch (x) { case 1: default: return 'd' }").equals("\"d\"");
		code_v3_("integer x = 2 switch (x) { case 1: default: return 'd' case 2: return 'two' }").equals("\"two\"");
	}

	@Test
	public void testSubject_evaluated_once() throws Exception {
		section("Switch: sujet évalué une seule fois");
		code_v3_("global n = 0 function f() { n++ return 3 } switch (f()) { case 1: case 2: case 3: break } return n").equals("1");
		code_v3_("global n = 0 function f() { n++ return 9 } switch (f()) { case 1: case 2: case 3: break } return n").equals("1");
	}

	@Test
	public void testStrings_optimized() throws Exception {
		section("Switch: chaînes");
		code_v3_("string x = 'world' switch (x) { case 'hello': return 1 case 'world': return 2 default: return 0 }").equals("2");
		code_v3_("var x = 'nope' switch (x) { case 'hello': return 1 case 'world': return 2 default: return 0 }").equals("0");
		// Labels avec échappements : le littéral Java doit rester correct.
		code_v3_("var x = 'a\\nb' switch (x) { case 'a\\nb': return 'ok' default: return 'no' }").equals("\"ok\"");
		code_v3_("var x = '' switch (x) { case '': return 'empty' default: return 'no' }").equals("\"empty\"");
	}

	@Test
	public void testNon_constant_labels_still_work() throws Exception {
		section("Switch: labels non constants (repli sur la chaîne)");
		code_v3_("var x = 5 switch (x) { case 2 + 3: return 'five' default: return 'other' }").equals("\"five\"");
		code_v3_("var y = 7 var x = 7 switch (x) { case y: return 'y' default: return 'other' }").equals("\"y\"");
		code_v3_("var x = 2 switch (x) { case 1: return 'a' case 1 + 1: return 'b' default: return 'c' }").equals("\"b\"");
	}

	@Test
	public void testOperations_count() throws Exception {
		section("Switch: coût en opérations");
		// Le dispatch coûte 1 op quel que soit le nombre de cas : 1 (var) + 1 (dispatch) + 1 (corps).
		code_v3_("var x = 8 switch (x) { case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: return 1 } return 0").ops(3);
		code_v3_("integer x = 8 switch (x) { case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: return 1 } return 0").ops(3);
		// Les chaînes longues ne facturent plus min(len) par comparaison.
		code_v3_("string x = 'delta' switch (x) { case 'alpha': case 'bravo': case 'charlie': case 'delta': return 1 } return 0").ops(3);
	}
}
