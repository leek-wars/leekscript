package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestSwitch extends TestCommon {

		@Test
	public void testBasic_switch() throws Exception {
		section("Basic switch");
		code_v3_("var x = 1 switch (x) { case 1: return 'one' } return 'none'").equals("\"one\"");
		code_v3_("var x = 2 switch (x) { case 1: return 'one' case 2: return 'two' } return 'none'").equals("\"two\"");
		code_v3_("var x = 3 switch (x) { case 1: return 'one' case 2: return 'two' } return 'none'").equals("\"none\"");
	}

	@Test
	public void testSwitch_with_default() throws Exception {
		section("Switch with default");
		code_v3_("var x = 5 switch (x) { case 1: return 'one' default: return 'other' }").equals("\"other\"");
		code_v3_("var x = 1 switch (x) { case 1: return 'one' default: return 'other' }").equals("\"one\"");
	}

	@Test
	public void testSwitch_with_break() throws Exception {
		section("Switch with break");
		code_v3_("var x = 1 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"one\"");
		code_v3_("var x = 2 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"two\"");
		code_v3_("var x = 3 var r = 'none' switch (x) { case 1: r = 'one' break case 2: r = 'two' break } return r").equals("\"none\"");
	}

	@Test
	public void testSwitch_with_break_and_default() throws Exception {
		section("Switch with break and default");
		code_v3_("var x = 3 var r = '' switch (x) { case 1: r = 'one' break case 2: r = 'two' break default: r = 'default' break } return r").equals("\"default\"");
	}

	@Test
	public void testMultiple_case_values() throws Exception {
		section("Multiple case values");
		code_v3_("var x = 2 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"one or two\"");
		code_v3_("var x = 1 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"one or two\"");
		code_v3_("var x = 3 switch (x) { case 1: case 2: return 'one or two' case 3: return 'three' } return 'none'").equals("\"three\"");
	}

	@Test
	public void testSwitch_with_strings() throws Exception {
		section("Switch with strings");
		code_v3_("var x = 'hello' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("1");
		code_v3_("var x = 'world' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("2");
		code_v3_("var x = 'other' switch (x) { case 'hello': return 1 case 'world': return 2 } return 0").equals("0");
	}

	@Test
	public void testSwitch_with_expressions() throws Exception {
		section("Switch with expressions");
		code_v3_("var x = 5 switch (x) { case 2 + 3: return 'five' default: return 'other' }").equals("\"five\"");
	}

	@Test
	public void testSwitch_with_multiple_instructions_in_case() throws Exception {
		section("Switch with multiple instructions in case");
		code_v3_("var x = 1 var a = 0 var b = 0 switch (x) { case 1: a = 10 b = 20 break case 2: a = 30 b = 40 break } return a + b").equals("30");
	}

	@Test
	public void testSwitch_in_function() throws Exception {
		section("Switch in function");
		code_v3_("function f(x) { switch (x) { case 1: return 'one' case 2: return 'two' default: return 'other' } } return f(2)").equals("\"two\"");
		code_v3_("function f(x) { switch (x) { case 1: return 'one' case 2: return 'two' default: return 'other' } } return f(5)").equals("\"other\"");
	}

	@Test
	public void testSwitch_with_null() throws Exception {
		section("Switch with null");
		code_v3_("var x = null switch (x) { case null: return 'null' default: return 'other' }").equals("\"null\"");
	}

	@Test
	public void testSwitch_with_boolean() throws Exception {
		section("Switch with boolean");
		code_v3_("var x = true switch (x) { case true: return 'yes' case false: return 'no' }").equals("\"yes\"");
	}

	@Test
	public void testNested_switch() throws Exception {
		section("Nested switch");
		code_v3_("var x = 1 var y = 2 var r = '' switch (x) { case 1: switch (y) { case 1: r = 'x1y1' break case 2: r = 'x1y2' break } break case 2: r = 'x2' break } return r").equals("\"x1y2\"");
	}

	@Test
	public void testSwitch_in_loop() throws Exception {
		section("Switch in loop");
		code_v3_("var s = 0 for (var i = 0; i < 5; i++) { switch (i) { case 0: case 1: s += 10 break default: s += 1 break } } return s").equals("23");
	}

	@Test
	public void testSwitch_with_if_inside_case() throws Exception {
		section("Switch with if inside case");
		code_v3_("var x = 1 var r = 'no' switch (x) { case 1: if (true) { r = 'yes' } break case 2: r = 'two' break } return r").equals("\"yes\"");
		code_v3_("var x = 1 var r = 'no' switch (x) { case 1: r = 'a' if (x == 1) { r = 'b' } r = r + 'c' break case 2: r = 'two' break } return r").equals("\"bc\"");
		code_v3_("var x = 2 var r = 'no' switch (x) { case 1: if (true) { r = 'one' } break case 2: if (true) { r = 'two' } break } return r").equals("\"two\"");
	}

	@Test
	public void testSwitch_with_for_inside_case() throws Exception {
		section("Switch with for inside case");
		code_v3_("var x = 1 var s = 0 switch (x) { case 1: for (var i = 0; i < 3; i++) { s += i } break } return s").equals("3");
	}

	@Test
	public void testSwitch_with_while_inside_case() throws Exception {
		section("Switch with while inside case");
		code_v3_("var x = 1 var s = 0 switch (x) { case 1: var i = 0 while (i < 3) { s += i i++ } break } return s").equals("3");
	}

	@Test
	public void testSwitch_case_with_if_and_no_break() throws Exception {
		section("Switch case with if and no break (fall-through)");
		code_v3_("var a = 0 var x = 1 switch (x) { case 1: a = 4 if (2 == 2) { return 99 } case 2: a = 12 case 3: a = 15 } return a").equals("99");
		code_v3_("var a = 0 var x = 1 switch (x) { case 1: a = 4 if (2 == 3) { return 99 } case 2: a = 12 case 3: a = 15 } return a").equals("15");
	}

	@Test
	public void testEmpty_switch() throws Exception {
		section("Empty switch");
		code_v3_("var x = 1 switch (x) {} return 'ok'").equals("\"ok\"");
	}

	@Test
	public void testSwitch_all_paths_abrupt_with_fallthrough() throws Exception {
		section("Switch with all paths abrupt through fall-through (#4892)");
		// Un case qui fallthrough dans un default qui return : javac prouve que le
		// switch ne se termine jamais normalement, et le code émis après (le return
		// implicite de la fonction) doit quand même compiler.
		code_v3_("function f(x) { var a = 7 switch (x) { case 1: a = 8 default: return a } } return f(1)").equals("8");
		code_v3_("function f(x) { var a = 7 switch (x) { case 1: a = 8 default: return a } } return f(2)").equals("7");
		// Même chose dans le bloc principal
		code_v3_("var a = 1 switch (a) { case 1: a = 2 default: return a }").equals("2");
		// Chemin chaîne de comparaisons (label non constant)
		code_v3_("function f(x) { var a = 7 switch (x) { case 1 + 0: a = 8 default: return a } } return f(1)").equals("8");
		// Tous les groupes abrupts via continue : le code après le switch est mort
		// à l'exécution mais doit rester compilable
		code_v3_("var s = 0 for (var i = 0; i < 3; i++) { switch (i) { case 0: continue default: continue } s += 1 } return s").equals("0");
	}

	@Test
	public void testSwitch_conditional_break_in_returning_cases() throws Exception {
		section("Switch with conditional break in all-returning cases");
		// Un break caché dans un if fait sortir du switch : il ne « retourne » pas
		// toujours, le code après doit être accepté et un return final émis.
		code_v3_("function f(x) { switch (x) { case 1: if (x == 1) break return 'r1' default: return 'r2' } return 'after' } return f(1)").equals("\"after\"");
		code_v3_("function f(x) { switch (x) { case 1: if (x == 1) break return 'r1' default: return 'r2' } } return f(1)").equals("null");
		code_v3_("function f(x) { switch (x) { case 1: if (x == 1) break return 'r1' default: return 'r2' } } return f(2)").equals("\"r2\"");
		// Break enfoui deux niveaux de if plus bas
		code_v3_("function f(x) { switch (x) { case 1: if (x >= 1) { if (x == 1) break } return 'r1' default: return 'r2' } } return f(1)").equals("null");
	}

	@Test
	public void testSwitch_shield_in_all_function_contexts() throws Exception {
		section("Switch fall-through shield in method, constructor, anonymous function");
		// Méthode, constructeur et fonction anonyme émettent leur return implicite
		// comme les fonctions globales : mêmes cas limites d'atteignabilité javac.
		code_v3_("class A { public m(x) { var a = 1 switch (x) { case 1: a = 2 default: return a } } } return new A().m(1)").equals("2");
		code_v3_("class A { public v = 0 constructor(x) { switch (x) { case 1: this.v = 2 default: return } } } return new A(1).v").equals("2");
		code_v3_("var f = function(x) { var a = 1 switch (x) { case 1: a = 2 default: return a } } return f(1)").equals("2");
	}

	@Test
	public void testSwitch_null_case_merged_with_default() throws Exception {
		section("Switch : case null: partageant le corps du default");
		// Le corps est celui du default : il s'exécute pour TOUTE valeur, on ne peut
		// donc pas y réduire le sujet à null (sans quoi le code émis remplaçait la
		// variable par des constantes null — résultats faux, sans aucune erreur).
		code_v3_("integer | null x = 5 switch (x) { case null: default: return x ** 2 }").equals("25");
		code_v3_("integer | null x = null switch (x) { case null: default: return 7 }").equals("7");
		// Le narrowing normal reste actif quand les deux labels sont séparés
		code_v3_("integer | null x = 5 switch (x) { case null: return 1 default: return x ** 2 }").equals("25");
		code_v3_("integer | null x = null switch (x) { case null: return 1 default: return 2 }").equals("1");
	}

	@Test
	public void testSwitch_duplicate_default() throws Exception {
		section("Switch : un seul default autorisé");
		// Deux default: donnaient un `duplicate default label` javac (erreur interne)
		code_v3_("var x = 1 switch (x) { default: return 1 default: return 2 }").error(Error.SWITCH_DUPLICATE_DEFAULT);
		code_v3_("var x = 1 switch (x) { default: default: return 1 }").error(Error.SWITCH_DUPLICATE_DEFAULT);
	}

	@Test
	public void testSwitch_continue_needs_enclosing_loop() throws Exception {
		section("Switch : continue exige une boucle englobante");
		// Le switch capture le break mais jamais le continue : hors boucle, le Java
		// émis était rejeté par javac au lieu d'une erreur d'analyse propre.
		code_v3_("var x = 1 switch (x) { case 1: continue } return 2").error(Error.CONTINUE_OUT_OF_LOOP);
		code_v3_("var f = function(x) { switch (x) { case 1: continue } } return f(1)").error(Error.CONTINUE_OUT_OF_LOOP);
		// Dans une boucle, il vise cette boucle et reste légal
		code_v3_("var s = 0 for (var i = 0; i < 3; i++) { switch (i) { case 0: continue default: s += 1 } } return s").equals("2");
		code_v3_("var s = 0 var i = 0 while (i < 3) { i++ switch (i) { case 1: continue default: s += 1 } } return s").equals("2");
	}

	@Test
	public void testSwitch_unclosed() throws Exception {
		section("Switch sans accolade fermante");
		// Le reste du fichier était avalé par le dernier case, sans un mot
		code_v3_("var x = 5 switch (x) { case 1: x = 10\nreturn x").error(Error.OPEN_BLOC_REMAINING);
		code_v3_("var x = 5 switch (x) { case 1: x = 10 }\nreturn x").equals("5");
	}

	@Test
	public void testSwitch_dead_code_after_returning_switch() throws Exception {
		section("Dead code after an all-returning switch");
		// Verrouille getEndBlock() == 1 : sans lui, ce code mort serait accepté.
		code_v3_("function f(x) { switch (x) { case 1: return 1 default: return 2 } return 3 } return f(1)").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		// Un break dans une boucle imbriquée vise la boucle, pas le switch : le
		// switch retourne toujours et le code qui suit reste du code mort.
		code_v3_("function f(x) { switch (x) { case 1: for (var i = 0; i < 3; i++) { if (x == 1) break } return 'r1' default: return 'r2' } return 3 } return f(1)").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("function f(x) { switch (x) { case 1: for (var i = 0; i < 3; i++) { if (x == 1) break } return 'r1' default: return 'r2' } } return f(1)").equals("\"r1\"");
	}

}
