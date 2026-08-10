package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;

/**
 * Constantes de compilation et élimination de code mort (cf ConstantFolder) :
 * 1. un champ `static final` à initialiseur LITTÉRAL est inliné au codegen,
 *    comme les constantes moteur ; l'idiome runtime (`static final X = tune()`)
 *    n'est pas plié ;
 * 2. une condition de `if`/`else if` constante (littéraux et champs pliés
 *    combinés par `!`, `&&`, `||`) ne coûte plus aucune opération et javac
 *    élimine la branche morte ;
 * 3. un appel statiquement résolu vers une fonction VIDE (corps réduit à rien
 *    après pliage) ou CONSTANTE (unique `return <littéral>`) est supprimé ou
 *    substitué, si tous ses arguments sont purs (jamais évalués sinon).
 */
@ExtendWith(SummaryExtension.class)
public class TestConstantFolding extends TestCommon {

	@Test
	public void testInit() throws Exception {
		header("Constantes de compilation & DCE");
	}

	@Test
	public void testStatic_final_literal_inlined() throws Exception {
		section("Champ static final littéral inliné");
		code_v2_("class A { static final X = 5 } return A.X").equals("5");
		code_v2_("class A { static final X = 5 } return A.X + 1").equals("6");
		code_v2_("class A { static final X = -3 } return A.X").equals("-3");
		code_v2_("class A { static final S = 'abc' } return A.S").equals("\"abc\"");
		code_v2_("class A { static final B = true } return A.B").equals("true");
		code_v2_("class A { static final N = null } return A.N").equals("null");
		code_v2_("class A { static final integer X = 12 } return A.X + 1").equals("13");
		code_v2_("class A { static final real R = 5 } return A.R").almost(5.0);
		// Accès non qualifié depuis une méthode de la classe
		code_v2_("class A { static final X = 7 static getX() { return X } } return A.getX()").equals("7");
		// Héritage : la constante du parent est visible sur l'enfant
		code_v2_("class A { static final X = 9 } class B extends A {} return B.X").equals("9");
		// En condition ternaire (constante inlinée, ternaire non plié)
		code_v2_("class A { static final X = true } return A.X ? 1 : 2").equals("1");
	}

	@Test
	public void testNon_constant_fields_untouched() throws Exception {
		section("Champs non constants inchangés");
		// Non final : mutable, jamais inliné
		code_v2_("class A { static X = 5 } A.X = 6 return A.X").equals("6");
		// Initialiseur non littéral (idiome tune()/registre) : reste runtime
		// (fonction non constante : son corps est une expression, pas un littéral)
		code_v2_("function tune() { return 40 + 2 } class A { static final X = tune() } return A.X").equals("42");
		// Initialiseur champ → champ : non chaîné (dépendrait de l'ordre d'init)
		code_v2_("class A { static final X = 5 static final Y = A.X } return A.Y").equals("5");
	}

	@Test
	public void testConstant_conditions() throws Exception {
		section("Conditions constantes : 0 opération");
		code("if (true) { return 1 } return 2").equals("1");
		code("if (false) { return 1 } return 2").equals("2");
		code("if (true) { return 1 } return 2").ops(0);
		code("if (false) { return 1 } return 2").ops(0);
		code_v2_("class A { static final DEBUG = false } if (A.DEBUG) { return 1 } return 2").equals("2");
		code_v2_("class A { static final DEBUG = false } if (A.DEBUG) { return 1 } return 2").ops(0);
		code_v2_("class A { static final DEBUG = false } if (!A.DEBUG) { return 1 } return 2").equals("1");
		code_v2_("class A { static final DEBUG = false } if (!A.DEBUG) { return 1 } return 2").ops(0);
	}

	@Test
	public void testShort_circuit_folding() throws Exception {
		section("Court-circuit && et ||");
		// Opérande gauche dominant : le droit (non constant) n'aurait jamais été évalué
		code_v2_("class A { static final D = false } function f() { return true } if (A.D && f()) { return 1 } return 2").equals("2");
		code_v2_("class A { static final D = false } function f() { return true } if (A.D && f()) { return 1 } return 2").ops(0);
		code_v2_("class A { static final V = true } function f() { return false } if (A.V || f()) { return 1 } return 2").equals("1");
		// Combinaison entièrement constante
		code_v2_("class A { static final D = false static final V = true } if (A.V && !A.D) { return 1 } return 2").ops(0);
		// Opérande gauche non constant : pas plié, sémantique conservée
		code_v2_("var i = 3 if (i > 2 && true) { return 1 } return 2").equals("1");
	}

	@Test
	public void testElse_if_chains() throws Exception {
		section("Chaînes else if");
		code_v2_("class A { static final X = 2 } if (A.X == 1) { return 1 } else if (A.X) { return 2 } else { return 3 }").equals("2");
		code("if (false) { return 1 } else if (false) { return 2 } else { return 3 }").equals("3");
		code("if (false) { return 1 } else if (true) { return 2 } else { return 3 }").equals("2");
	}

	@Test
	public void testConstant_loop_conditions_compile() throws Exception {
		section("Boucles à condition constante (pas d'erreur unreachable javac)");
		code_v2_("class A { static final F = false } while (A.F) { return 1 } return 2").equals("2");
		code_v2_("class A { static final F = false } while (!A.F) { return 1 } return 2").equals("1");
		code_v2_("class A { static final F = false } var n = 0 do { n++ } while (A.F) return n").equals("1");
		code_v2_("class A { static final F = false } for (var i = 0; A.F; i++) { return 1 } return 2").equals("2");
		code("while (0) {} return 2").equals("2");
	}

	@Test
	public void testEmpty_function_call_eliminated() throws Exception {
		section("Appel de fonction vide éliminé");
		code_v2_("function foo() {} foo() return 1").equals("1");
		code_v2_("function foo() {} foo() return 1").ops(0);
		// v1 : pas d'élimination, le coût d'appel (1 op côté callee) reste
		code_v1("function foo() {} foo() return 1").ops(1);
		// Arguments purs : littéraux et identifiants simples
		code_v2_("function foo(x, y) {} foo(1, true) return 1").ops(0);
		code_v2_("function foo(x) {} var a = 1 foo(a) return 2").equals("2");
		// Champ statique en argument
		code_v2_("class A { static X = 5 } function foo(x) {} foo(A.X) return 3").equals("3");
	}

	@Test
	public void testGuard_function_pattern() throws Exception {
		section("Fonction garde : if constant-faux dans le corps");
		// Le cas cible : garde de debug/profiling à coût nul
		code_v2_("class C { static final PROFILE = false } function guard(x) { if (C.PROFILE) { debug(x) } } guard(1) return 8").equals("8");
		code_v2_("class C { static final PROFILE = false } function guard(x) { if (C.PROFILE) { debug(x) } } guard(1) return 8").ops(0);
		// Garde vraie : le corps reste, l'appel aussi
		code_v2_("class C { static final PROFILE = true } global n = 0 function guard() { if (C.PROFILE) { n++ } } guard() return n").equals("1");
		// Garde non constante : l'appel reste
		code_v2_("global on = true global n = 0 function guard() { if (on) { n++ } } guard() return n").equals("1");
	}

	@Test
	public void testConstant_function_substituted() throws Exception {
		section("Fonction constante substituée");
		code_v2_("function two() { return 2 } return two()").equals("2");
		code_v2_("function two() { return 2 } return two()").ops(0);
		code_v1("function two() { return 2 } return two()").ops(1);
		code_v2_("function two() { return 2 } return two() + 1").equals("3");
		code_v2_("function no() { return false } if (no()) { return 1 } return 2").equals("2");
		// Le littéral peut venir d'un champ static final plié
		code_v2_("class A { static final X = 4 } function x() { return A.X } return x()").equals("4");
	}

	@Test
	public void testElimination_guards() throws Exception {
		section("Garde-fous de l'élimination");
		// Argument impur : l'appel reste, l'effet de bord aussi
		code_v2_("var i = 0 function foo(x) {} foo(i++) return i").equals("1");
		code_v2_("global g = 0 function foo(x) {} foo(g = 5) return g").equals("5");
		// Accès tableau en argument : pas éliminé (peut lever une erreur)
		code_v2_("function foo(x) {} var a = [1] foo(a[0]) return 7").equals("7");
		// Valeur par défaut impure (évaluée côté callee) : pas de classification
		code_v2_("global c = 0 function f(x = c++) {} f() return c").equals("1");
		// Valeur par défaut littérale : éliminé
		code_v2_("function f(x = 5) {} f() return 2").equals("2");
		// Fonction redéfinie (legacy ≤ 3) : jamais substituée
		code_v2_3("function f() { return 1 } function g() { return 2 } f = g return f()").equals("2");
		// Référence de fonction : f_ toujours généré, appel dynamique intact
		code_v2_("function foo() {} var h = foo return h()").equals("null");
		// Appel en dernière instruction du main (return implicite)
		code_v2_("function foo() {} foo()").equals("null");
		// Corps avec return dans une branche vivante : pas classifiable
		code_v2_("global t = 0 function f() { if (t == 0) { return 1 } return 2 } return f()").equals("1");
	}

	@Test
	public void testStatic_final_assignment_guards() throws Exception {
		section("Affectation d'un champ static final : l'erreur reste le filet de l'inline");
		code_strict_v2_("class A { static final a = 12 } A.a = 15 return A.a").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		// Non qualifiée : l'analyse ne voit que FIELD, le filet est setField au runtime
		code_v2_("class A { static final X = 5 static m() { X = 6 } } A.m() return A.X").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		// Dans un constructeur : l'exemption d'analyse ne discrimine pas la cible
		code_v2_("class A { static final X = 5 constructor() { A.X = 9 } } new A() return A.X").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		// Incrément (chemin isIncrement, distinct de l'affectation)
		code_v2_("class A { static final a = 12 } A.a++ return A.a").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
	}

	@Test
	public void testNegative_literal_parenthesized() throws Exception {
		section("Littéraux négatifs : parenthésage de l'inline");
		code_v2_("class A { static final X = -1 } var a = 5 return a - A.X").equals("6");
		code_v2_("class A { static final X = -1 static m() { return 10 - X } } return A.m()").equals("11");
		code_v2_("class A { static final real R = -3 } return A.R").almost(-3.0);
	}

	@Test
	public void testType_matrix_conditions() throws Exception {
		section("Matrice de types en condition");
		// Chaînes : champ inliné mais truthiness PAS pliée (conservateur)
		code_v2_("class A { static final S = 'abc' } if (A.S) { return 1 } return 2").equals("1");
		code_v2_("class A { static final S = '' } if (A.S) { return 1 } return 2").equals("2");
		code("if ('') { return 1 } return 2").equals("2");
		code_v2_("class A { static final N = null } if (A.N) { return 1 } return 2").equals("2");
		code_v2_("class A { static final N = null } if (A.N) { return 1 } return 2").ops(0);
		code_v2_("class A { static final B = true } if (A.B) { return 1 } return 2").ops(0);
		code_v2_("class A { static final real R = 0 } if (A.R) { return 1 } return 2").equals("2");
		code_v2_("class A { static final real R = 5 } return A.R + 1").almost(6.0);
		// xor : non plié, chemin runtime conservé
		code_v2_("class A { static final D = true } if (A.D xor true) { return 1 } return 2").equals("2");
	}

	@Test
	public void testTyped_return_defaults() throws Exception {
		section("Fonctions typées : valeur substituée = défaut du type de retour");
		code_v4_("function f() -> integer {} return f()").equals("0");
		code_v4_("function f() -> real { return 2 } real x = f() return x").almost(2.0);
		// return nu : EMPTY seulement si le retour est any
		code_v2_("function f() { return } f() return 1").ops(0);
	}

	@Test
	public void testBreak_continue_in_constant_ifs() throws Exception {
		section("break/continue dans des if constants en boucle");
		code("var n = 0 while (n < 5) { if (false) { break } n++ } return n").equals("5");
		code("var n = 0 while (true) { if (true) { break } n++ } return n").equals("0");
		code("var s = 0 for (var i = 0; i < 5; i++) { if (true) { continue } s += i } return s").equals("0");
		code_v2_("class A { static final D = false } var n = 0 while (n < 3) { if (A.D) { debug(n) } n++ } return n").equals("3");
	}

	@Test
	public void testConstant_true_loops() throws Exception {
		section("Boucles à condition constante vraie");
		code_v2_("class A { static final T = true } var n = 0 do { n++ if (n == 3) { break } } while (A.T) return n").equals("3");
		code_v2_("class A { static final T = true } var n = 0 for (var i = 0; A.T; i++) { n++ if (n == 3) { break } } return n").equals("3");
	}

	@Test
	public void testElse_if_priority_preserved() throws Exception {
		section("else if constant-vrai : la priorité de la chaîne est conservée");
		code("var x = 1 if (x == 1) { return 1 } else if (true) { return 2 } else { return 3 }").equals("1");
		code("var x = 5 if (x == 1) { return 1 } else if (true) { return 2 } else { return 3 }").equals("2");
	}

	@Test
	public void testNo_elimination_of_methods() throws Exception {
		section("Pas d'élimination hors fonctions globales");
		code_v2_("class A { static empty() {} } A.empty() return 1").equals("1");
	}

	@Test
	public void testNested_and_multi_arity_elimination() throws Exception {
		section("Appels imbriqués et multi-arités");
		code_v2_("function two() { return 2 } return abs(two())").equals("2");
		code_v2_("function two() { return 2 } function inc(x) { return x + 1 } return inc(two())").equals("3");
		code_v2_("function f(x, y = 2) {} f(1) f(1, 3) return 9").equals("9");
		code_v2_("function f(x, y = 2) {} f(1) f(1, 3) return 9").ops(0);
		code_v2_("class A { static final X = 5 } function f(y = A.X) {} f() return 2").ops(0);
	}

	@Test
	public void testOps_pinned() throws Exception {
		section("Comptage d'ops figé");
		code_v1("if (true) { return 1 } return 2").ops(0);
		code_v2_("function f(x = 5) {} f() return 2").ops(0);
		code_v2_("function two() { return 2 } return two() + 1").ops(1);
		code_v2_("function no() { return false } if (no()) { return 1 } return 2").ops(1);
	}

	@Test
	public void testDynamic_reads_see_runtime_field() throws Exception {
		section("Lectures dynamiques et non-scalaires : champ runtime intact");
		code_v2_("class A { static final X = 5 } return A['X']").equals("5");
		code_v4_("class A { static final M = [1: 2] } return A.M[1]").equals("2");
	}

	@Test
	public void testAccess_levels_respected() throws Exception {
		section("Niveaux d'accès : miroir exact du runtime (pas de fuite de constante)");
		// private lu de l'extérieur : pas d'inline, le runtime renvoie null + log
		code_v2_("class A { private static final SECRET = 42 } return A.SECRET").equals("null");
		// private hérité lu depuis la sous-classe : refusé aussi
		code_v2_("class P { private static final X = 5 } class B extends P { static m() { return X } } return B.m()").equals("null");
		// private lu depuis SA classe : autorisé, inliné
		code_v2_("class A { private static final X = 7 static m() { return X } } return A.m()").equals("7");
		// protected depuis un descendant : autorisé
		code_v2_("class P { protected static final X = 9 } class B extends P { static m() { return X } } return B.m()").equals("9");
		// protected de l'extérieur : refusé
		code_v2_("class P { protected static final X = 9 } return P.X").equals("null");
		// argument privé d'un appel éliminable : l'appel (et son log d'erreur) reste
		code_v2_("class A { private static final X = 5 } function foo(x) {} foo(A.X) return 1").equals("1");
	}

	@Test
	public void testThrowing_conversions_keep_call() throws Exception {
		section("Conversions d'arguments/retour throwantes : l'appel reste");
		code_v4_("function vide(Array a) {} var x = 5 vide(x) return 1").error(Error.IMPOSSIBLE_CAST);
		code_v4_("function vide(Array a) {} any x = 5 vide(x) return 1").error(Error.IMPOSSIBLE_CAST);
		// paramètre typé + argument compatible : éliminé normalement
		code_v4_("function vide(integer a) {} vide(5) return 3").equals("3");
		code_v4_("function vide(integer a) {} vide(5) return 3").ops(0);
	}

	@Test
	public void testStatic_init_phase_coherent() throws Exception {
		section("Init statique : les constantes existent dès la création");
		// Ordre inversé dans la classe : la constante est visible avant son initField
		code_v2_("class A { static Y = A.X static final X = 5 } return A.Y").equals("5");
		// Inter-classes : B initialisée avant A
		code_v2_("class B { static Y = A.X } class A { static final X = 5 } return B.Y").equals("5");
		// Via un appel substitué dans un initialiseur
		code_v2_("class B { static Y = x() } class A { static final X = 5 } function x() { return A.X } return B.Y").equals("5");
	}

	@Test
	public void testUnary_minus_arg_not_eliminated() throws Exception {
		section("Argument à coût non nul (moins unaire) : facturation identique partout");
		code_v2_("function foo(x) {} foo(-5) return 1").equals("1");
		code_v2_("function foo(x) {} foo(-5) return 1").ops(2);
		code_v2_("function two(x) { return 2 } return two(-5)").equals("2");
		code_v2_("function two(x) { return 2 } return two(-5)").ops(2);
	}

	@Test
	public void testNegative_infinity_literal() throws Exception {
		section("Littéral -Infinity : émission Java valide");
		code_v2_("class A { static final X = -1e999 } if (A.X < 0) { return 1 } return 2").equals("1");
		code_v2_("function f() { return -1e999 } if (f() < 0) { return 1 } return 2").equals("1");
	}

	/**
	 * Chemins ops DÉSACTIVÉES (CLI leekscript.jar / embarqué) : le compteur ops(...)
	 * n'enveloppe plus les émissions, un littéral substitué peut donc devenir une
	 * constante javac nue. Vérifie : appel éliminé en initialisation et en
	 * incrément de for (statement expression), et appels constants typés boolean
	 * en condition de for/do-while/while (garde bool() anti-unreachable).
	 */
	@Test
	public void testOps_disabled_paths() throws Exception {
		section("Ops désactivées (CLI) : émission valide");
		assertOpsDisabled("var s = 0 function foo() {} for (var i = 0; i < 3; foo()) { i++ s++ } return s", "3");
		assertOpsDisabled("function no() -> boolean { return false } for (var i = 0; no(); i++) { return 1 } return 2", "2");
		assertOpsDisabled("function yes() -> boolean { return true } var n = 0 do { n++ if (n == 3) { break } } while (yes()) return n", "3");
		assertOpsDisabled("function no() -> boolean { return false } while (no()) { return 1 } return 2", "2");
		assertOpsDisabled("function foo() {} foo() return 4", "4");
		assertOpsDisabled("for (var i = 0; false; i++) { return 1 } return 2", "2");
	}

	private void assertOpsDisabled(String snippet, String expected) throws Exception {
		var ai = LeekScript.compileSnippet(snippet, "AI", new Options(false));
		ai.init();
		ai.staticInit();
		var v = ai.runIA();
		assertEquals(expected, ai.export(v, new HashSet<>()), snippet);
	}
}
