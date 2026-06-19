package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestInference extends TestCommon {

	@Test
	public void testReturnTypeInference() throws Exception {
		section("Return type inference (#43)");

		// Retour littéral : le type de retour est inféré `integer`, donc `integer m = f()`
		// ne warn pas en strict v5. En v4 (inférence désactivée) le retour reste `any` et
		// l'affectation à un integer warn : prouve l'inférence ET son gating v5.
		code_strict_v5_("function getFive() { return 5 } integer m = getFive() return m").noWarning();
		code_strict_v4("function getFive() { return 5 } integer m = getFive() return m").warning(Error.DANGEROUS_CONVERSION_VARIABLE);
		// Runtime inchangé.
		code_v5("function getFive() { return 5 } return getFive() * 2").equals("10");
		code_v4_("function getFive() { return 5 } return getFive() * 2").equals("10");

		// Plusieurs branches du même type, toutes terminales : inféré `integer`.
		code_strict_v5_("function f(c) { if (c) { return 1 } else { return 2 } } integer m = f(true) return m").noWarning();

		// Retour d'une chaîne : inféré `string`.
		code_strict_v5_("function greet() { return \"hi\" } string s = greet() return s").noWarning();

		// Annotation explicite respectée (inférence non régressive).
		code_strict_v5_("function f() -> integer { return 5 } integer m = f() return m").noWarning();
		// `-> any` explicite BLOQUE l'inférence : le retour reste `any`, l'affectation warn.
		code_strict_v5_("function f() -> any { return 5 } integer m = f() return m").warning(Error.DANGEROUS_CONVERSION_VARIABLE);
	}

	@Test
	public void testReturnTypeInference_nullable() throws Exception {
		section("Return type inference — chute en fin de corps (#43)");

		// Le corps peut tomber en fin (pas de return sur tous les chemins) → le type inféré
		// inclut `null` : `integer m = f()` warn en strict (int | null), et f(false) renvoie
		// bien `null` (et NON 0) au runtime — c'est l'invariant de correction du retour implicite.
		code_strict_v5_("function f(c) { if (c) { return 5 } } integer m = f(true) return m").warning(Error.DANGEROUS_CONVERSION_VARIABLE);
		code_v5("function f(c) { if (c) { return 5 } } return f(true)").equals("5");
		code_v5("function f(c) { if (c) { return 5 } } return f(false)").equals("null");
		// Aucun return du tout → renvoie null.
		code_v5("function f() { var x = 5 } return f()").equals("null");
	}

	@Test
	public void testReturnTypeInference_recursion() throws Exception {
		section("Return type inference — récursion (#43)");

		// Récursion : l'appel récursif vaut `any` au moment de l'analyse → l'union retombe
		// sur `any` (pas de point fixe, pas de boucle infinie). Le runtime reste correct.
		code_v5("function fact(n) { if (n <= 1) { return 1 } return n * fact(n - 1) } return fact(5)").equals("120");
		// Récursion mutuelle : pas de crash, runtime correct.
		code_v5("function isEven(n) { if (n == 0) { return true } return isOdd(n - 1) } function isOdd(n) { if (n == 0) { return false } return isEven(n - 1) } return isEven(10)").equals("true");
	}

	@Test
	public void testReturnTypeInference_generic() throws Exception {
		section("Return type inference — corps générique non annoté (#43)");

		// Un corps générique SANS `-> T` infère le retour T (effacé en Object, re-casté au
		// site d'appel via la substitution générique). Équivaut à annoter `-> T`.
		code_v5("function f<T>(T x) { return x } return f(5)").equals("5");
		code_v5("function f<T>(T x) { return x } return f([1, 2, 3])").equals("[1, 2, 3]");
		// La substitution donne un type précis : `integer m = f(5)` ne warn pas en strict.
		code_strict_v5_("function f<T>(T x) { return x } integer m = f(5) return m").noWarning();
	}

	@Test
	public void testReturnTypeInference_propagation() throws Exception {
		section("Return type inference — propagation à travers les appels (#43 T2)");

		// Chaîne de helpers a -> b -> c -> littéral. L'analyse à la demande infère c avant b
		// avant a, quel que soit l'ordre de déclaration (a déclaré AVANT c) → a() est typé
		// `integer`, donc `integer m = a()` ne warn pas en strict.
		code_strict_v5_("function a() { return b() } function b() { return c() } function c() { return 42 } integer m = a() return m").noWarning();
		code_v5("function a() { return b() } function b() { return c() } function c() { return 42 } return a()").equals("42");

		// Récursion mutuelle via l'analyse à la demande : pas de boucle infinie ni de crash,
		// runtime correct (le type retombe sur any pour le cycle).
		code_v5("function ping(n) { if (n <= 0) { return 0 } return pong(n - 1) } function pong(n) { return ping(n - 1) } return ping(5)").equals("0");

		// Sûreté de la ré-entrance : la valeur par défaut d'un paramètre de l'appelé ne voit
		// PAS les locales de l'appelant (la résolution se fait en preAnalyze, contexte correct,
		// avant toute analyse à la demande). `leaked` reste donc inconnu dans callee.
		code_v5("function caller() { var leaked = 99 return callee() } function callee(p = leaked) { return p } return caller()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
	}
}
