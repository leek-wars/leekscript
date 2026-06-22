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


	@Test
	public void testParameterMonomorphization_collections() throws Exception {
		section("Parameter monomorphization — collections (#43 T3)");

		// Array/Map/Set : un paramètre non annoté appelé avec une collection crée une variante
		// typée vers le type LARGE (Array<any>/Map/Set) → méthodes en dispatch statique, sémantique
		// préservée (sum() reste real, mutation visible, accès élément correct).
		code_v5("function total(a) { return a.sum() } return total([1, 2, 3])").equals("6.0");
		code_v5("function f(a) { a.push(4) return a } return f([1, 2, 3])").equals("[1, 2, 3, 4]");
		code_v5("function first(a) { return a[0] } return first([7, 8, 9])").equals("7");
		code_v5("function sz(m) { return m.size() } return sz([1 : 10, 2 : 20])").equals("2");
		code_v5("function get(m) { return m[1] } return get([1 : 10, 2 : 20])").equals("10");
		code_v5("function has(s) { return s.contains(2) } return has(<1, 2, 3>)").equals("true");

		// Profils mixtes selon le site : array ET int → deux variantes distinctes.
		code_v5("function id(x) { return x } return id([1, 2])[0] + id(5)").equals("6");
	}



	@Test
	public void testParameterMonomorphization() throws Exception {
		section("Parameter monomorphization (#43 tranche 3)");

		// Cœur : un paramètre non annoté appelé avec un type concret crée une VARIANTE
		// spécialisée (corps re-parsé + analysé typé, 1×). Son type de retour précis se
		// PROPAGE à l'appelant → en strict v5, `integer m = dbl(5)` ne warn plus (dbl(5)
		// dispatche vers la variante dbl$i qui renvoie int). v4 (off) → x any → warn.
		code_strict_v5_("function dbl(x) { return x * 2 } integer m = dbl(5) return m").noWarning();
		code_strict_v4("function dbl(x) { return x * 2 } integer m = dbl(5) return m").warning(Error.DANGEROUS_CONVERSION_VARIABLE);
		// Runtime correct (la variante produit la même valeur).
		code_v5("function dbl(x) { return x * 2 } return dbl(21)").equals("42");
		code_v4_("function dbl(x) { return x * 2 } return dbl(21)").equals("42");

		// Argument = local typé (pas un littéral) : profil int détecté pareil.
		code_v5("function dbl(x) { return x * 2 } var n = 7 return dbl(n)").equals("14");

		// real et bool.
		code_strict_v5_("function half(x) { return x / 2.0 } real r = half(9) return r").noWarning();
		code_v5("function neg(b) { return !b } return neg(true)").equals("false");

		// Profils MULTIPLES : la même fonction appelée int ET real → DEUX variantes (id$i, id$r),
		// chacune correcte. (id$a — appel any — garderait la générique.)
		code_v5("function id(x) { return x } return id(5) + id(10)").equals("15");
		code_v5("function id(x) { return x } return [id(5), id(2.5)]").equals("[5, 2.5]");

		// String : un paramètre appelé avec une string crée une variante typée (méthodes string
		// résolues statiquement). Runtime correct. (En strict, le CORPS générique `s.length()`
		// sur `any` warn FIELD_MAY_NOT_EXIST indépendamment — strict encourage l'annotation ;
		// la conversion d'appel, elle, est bien propagée précise.)
		code_v5("function up(s) { return s.toUpper() } return up(\"hi\")").equals("\"HI\"");
		code_v5("function len(s) { return s.length() } return len(\"hello\")").equals("5");
		code_v5("function f(s) { return s + \"!\" } return f(\"hi\")").equals("\"hi!\"");
	}


	@Test
	public void testParameterMonomorphization_class() throws Exception {
		section("Parameter monomorphization — types classe (#43 T3, getHP(entity))");

		// Un paramètre non annoté appelé avec une instance de classe crée une variante typée :
		// l'accès méthode se résout statiquement (plus de dispatch dynamique par nom).
		code_v5("class A { getX() { return 7 } } function get(a) { return a.getX() } return get(new A())").equals("7");

		// Le cas phare de l'issue #43 : un helper `getHP(entity)` non typé devient typé.
		code_v5("class Entity { getLife() { return 100 } } function getHP(e) { return e.getLife() } return getHP(new Entity())").equals("100");

		// DISPATCH VIRTUEL à travers la variante : le paramètre est typé par le type STATIQUE de
		// l'argument ; une sous-classe redéfinissant la méthode appelle bien la redéfinition.
		code_v5("class A { getX() { return 1 } } class B extends A { getX() { return 2 } } function get(a) { return a.getX() } return get(new B())").equals("2");
		code_v5("class A { getX() { return 1 } } class B extends A { getX() { return 2 } } function get(a) { return a.getX() } A a = new B() return get(a)").equals("2");

		// Profils MULTIPLES : la même fonction appelée avec deux classes différentes → deux
		// variantes distinctes, chacune correcte.
		code_v5("class A { v() { return 1 } } class C { v() { return 9 } } function get(x) { return x.v() } return get(new A()) + get(new C())").equals("10");

		// Mélange primitif + classe selon le site d'appel (une variante par profil).
		code_v5("class A { v() { return 3 } } function f(x) { return x } return f(5) + f(new A()).v()").equals("8");
	}

	@Test
	public void testParameterMonomorphization_safety() throws Exception {
		section("Parameter monomorphization — sûreté (#43 T3)");

		// Récursion : l'appel récursif (n int) dispatche vers la MÊME variante (cache avant
		// analyse → pas de boucle). Runtime correct.
		code_v5("function fact(n) { if (n <= 1) { return 1 } return n * fact(n - 1) } return fact(5)").equals("120");

		// Abandon de variante : spécialiser x en int rendrait `x[0]` invalide → la variante est
		// abandonnée SANS erreur visible, l'appelant garde la générique (accès dynamique → null).
		code_v5("function f(x) { return x[0] } return f(5)").equals("null");

		// Paramètre annoté : inchangé (pas de variante, déjà typé).
		code_v5("function f(integer x) { return x * 2 } return f(9)").equals("18");

		// Fonction utilisée comme VALEUR (échappe) : l'appel dynamique via la variable utilise la
		// générique ; un appel direct concret peut quand même se spécialiser. Runtime correct.
		code_v5("function f(x) { return x * 2 } var g = f return g(5) + f(10)").equals("30");

		// Appel avec un argument any (non concret) → pas de variante, générique. Correct.
		code_v5("function dbl(x) { return x * 2 } var a = [1] return dbl(a[0])").equals("2");

		// Fermeture DANS un corps monomorphisé : la variante re-parse une closure fraîche qui
		// capture le paramètre typé. Runtime correct (pas de collision d'identité de closure).
		code_v5("function apply(x) { var f = u -> u + x return f(10) } return apply(5)").equals("15");

		// Chaîne de monomorphisation : twice(5) → twice$i dont le corps appelle inc(int) →
		// inc$i (typé), imbriqué. Propagation à travers les variantes.
		code_v5("function inc(x) { return x + 1 } function twice(x) { return inc(inc(x)) } return twice(5)").equals("7");

		// Mutuelle récursion typée : ping(int)/pong(int) créent ping$i/pong$i. Pas de boucle de
		// création (cache avant analyse). Runtime correct.
		code_v5("function ping(n) { if (n <= 0) { return 0 } return pong(n - 1) } function pong(n) { return ping(n - 1) } return ping(6)").equals("0");

		// Variante créée depuis une MÉTHODE (currentClass posée) : la fonction libre s'analyse
		// hors contexte de classe (currentClass annulé). Runtime correct.
		code_v5("function dbl(x) { return x * 2 } class A { m() { return dbl(5) } } return new A().m()").equals("10");

		// Paramètres par DÉFAUT : seul l'appel TOUTES-args monomorphise (variante sans défaut,
		// tous params requis). Un appel partiel garde la générique (qui conserve ses défauts).
		code_v5("function f(x, y = 5) { return x + y } return f(3, 7)").equals("10"); // full-arg → variante
		code_v5("function f(x, y = 5) { return x + y } return f(3)").equals("8");      // partiel → générique (défaut)
		// Propagation sur l'appel full-arg : x+y (int,int) → int → pas de warn en strict.
		code_strict_v5_("function f(x, y = 5) { return x + y } integer m = f(3, 7) return m").noWarning();
		// Les deux formes d'appel coexistent dans le même programme.
		code_v5("function f(x, y = 5) { return x + y } return f(3, 7) + f(3)").equals("18");
	}
}
