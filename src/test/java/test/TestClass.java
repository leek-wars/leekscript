package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestClass extends TestCommon {


		@Test
	public void testClass_toBoolean() throws Exception {
		section("Class toBoolean");
		code_v2_("class A {} return !!A").equals("true");
		code_v2_("class A {} if (A) { return 12 } return null").equals("12");
	}

	@Test
	public void testClass_and_variable() throws Exception {
		section("Class and variable");
		code_v2_("class A {} var A = 12;").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("var A = 12; class A {}").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A {} function f(A) { A = 1 return A } return f(2)").equals("1");
		code_v2_("class A {} function f() { var A = 1 return A } return f()").equals("1");
		code_v2_("class A {} A = 12").error(Error.CANT_ASSIGN_VALUE);
	}

	@Test
	public void testClass_typed_global_defined_after() throws Exception {
		section("Class - typed global with class defined after (#2853)");
		// Une globale typée d'une classe définie plus loin : doit compiler
		code_v4_("global MyClass var1 = new MyClass(); class MyClass {} return var1 != null").equals("true");
		// Deux globales typées de la même classe définie plus loin : ne doit plus
		// faussement déclencher VARIABLE_NAME_UNAVAILABLE (#2853)
		code_v4_("global MyClass var1 = new MyClass(); global MyClass var2 = new MyClass(); class MyClass {} return var1 != null && var2 != null").equals("true");
		// Idem en déclarations séparées avec valeurs ensuite
		code_v4_("global MyClass a = new MyClass() global MyClass b = new MyClass() class MyClass { public integer x = 7 } return a.x + b.x").equals("14");
	}

	@Test
	public void testClass_name() throws Exception {
		section("Class.name");
		code_v2_("class A { public m() { return name }} return new A().m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2_("class A { public m() { return class.name }} return new A().m()").equals("\"A\"");
		code_v2_("class A { public static m() { return name }} return A.m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2_("class A { public static m() { return class.name }} return A.m()").equals("\"A\"");
		// #2619 : dans une méthode d'instance héritée, `class` doit désigner la classe
		// runtime de l'objet (late static binding), pas la classe où la méthode est définie.
		code_v2_("class A { public m() { return class.name }} class B extends A {} return new B().m()").equals("\"B\"");
		code_v2_("class A { public m() { return class }} class B extends A {} return new B().m() == B").equals("true");
		code_v2_("class A { constructor() {} public m() { return class.name }} class B extends A { constructor() {} } return new B().m()").equals("\"B\"");
	}

	@Test
	public void testStaticMethodReference_higherOrder() throws Exception {
		section("Static method reference passed to higher-order function (#11714)");
		// arrayMap appelle le callback avec (élément, index, tableau) : comme une fonction
		// classique ou un built-in (cos, atan2), une référence de méthode statique doit
		// ignorer les arguments en trop et retomber sur la surcharge de plus grande arité.
		code_v2_("class A { public static addOne(n) { return n + 1 } } return arrayMap([1, 2, 3], A.addOne)").equals("[2, 3, 4]");
		// Appel direct avec l'arité exacte inchangé
		code_v2_("class A { public static addOne(n) { return n + 1 } } return A.addOne(5)").equals("6");
		// Méthode 2 params : la référence rapporte sa vraie arité, donc les HOF legacy (v1-3)
		// lui passent (clé, valeur) au lieu d'1 seul argument. v4 passe (élément, index).
		code_v2_3("class A { public static m2(a, b) { return a * 10 + b } } return arrayMap([1, 2, 3], A.m2)").equals("[1, 12, 23]");
		code_v4_("class A { public static m2(a, b) { return a * 10 + b } } return arrayMap([1, 2, 3], A.m2)").equals("[10, 21, 32]");
		// Méthode avec plus de params que fournis : les manquants sont paddés à null (comme une
		// fonction classique), m4 renvoie son 1er argument donc on retrouve clé (v1-3) / élément (v4).
		code_v2_3("class A { public static m4(a, b, c, d) { return a } } return arrayMap([1, 2, 3], A.m4)").equals("[0, 1, 2]");
		code_v4_("class A { public static m4(a, b, c, d) { return a } } return arrayMap([1, 2, 3], A.m4)").equals("[1, 2, 3]");
	}

	@Test
	public void testMethod_call_with_nullable_argument() throws Exception {
		section("Method call with nullable argument");
		// Calling a method without this. prefix with a nullable parameter
		code_v2_("class A { private Array<integer> list = [] private add(integer | null v) { if (v != null) { push(list, v) } } public run() { add(null) add(3) return list } } return new A().run()").equals("[3]");
		// Same with this. prefix
		code_v2_("class A { private Array<integer> list = [] private add(integer | null v) { if (v != null) { push(list, v) } } public run() { this.add(null) this.add(3) return list } } return new A().run()").equals("[3]");
		// Nullable field passed to method without this.
		code_v2_("class A { private integer? val = null private get(integer? v) { return v } public run() { return get(val) } } return new A().run()").equals("null");
		code_v2_("class A { private integer? val = null private get(integer? v) { return v } public run() { val = 42 return get(val) } } return new A().run()").equals("42");
		// Class type nullable parameter
		code_v2_("class B { public integer x = 0 } class A { private check(B | null b) { return b != null ? b.x : -1 } public run() { return check(null) + ',' + check(new B()) } } return new A().run()").equals("\"-1,0\"");
	}

	@Test
	public void testMethod_call_via_array_access_typed_param() throws Exception {
		section("Issue #3719 - Array-access method call with typed parameter");
		// Untyped parameter — works
		code_v2_("class Test { private integer test_fct(a) { return 5 + a } } Test t = new Test(); return t['test_fct'](15)").equals("20");
		// Typed parameter — should also work
		code_v2_("class Test { private integer test_fct(integer a) { return 5 + a } } Test t = new Test(); return t['test_fct'](15)").equals("20");
		// Multiple typed parameters
		code_v2_("class Test { public real f(integer a, real b) { return a + b } } return new Test()['f'](2, 3.5)").equals("5.5");
		// Typed string parameter
		code_v2_("class Test { public string f(string s) { return s + '!' } } return new Test()['f']('hi')").equals("\"hi!\"");
	}

	@Test
	public void testMethod_default_parameter_with_typed_class() throws Exception {
		section("Method default parameter with typed class");
		// Untyped field as default value for typed parameter (was crashing with incompatible types)
		code_v2_("class A { public truc public void f(A arg = truc) { debug(arg) } } new A().f()").equals("null");
		// Typed field as default value for same typed parameter
		code_v2_("class A { public A other public void f(A arg = other) { debug(arg) } } new A().f()").equals("null");
		// Typed field with value as default
		code_v2_("class B { public integer x = 42 } class A { public B b = new B() public integer f(B arg = b) { return arg.x } } return new A().f()").equals("42");
	}

	@Test
	public void testField_method_same_name() throws Exception {
		section("Issue #2861 - champ et méthode homonymes");
		// Appel : `t.monNom()` vise la méthode, sans erreur trompeuse « champ privé /
		// non appelable » (le champ privé integer ne doit plus intercepter l'appel).
		code_strict_v2_("class Test { private integer monNom; public integer monNom() { return 7; } } Test t = new Test(); return t.monNom()").equals("7");
		// Lecture (sans parenthèses) : la sémantique de champ est préservée (priorité au
		// champ), on lit bien la valeur du champ et non une référence de méthode.
		code_v2_("class Test { public integer monNom = 5; public integer monNom() { return 7; } } Test t = new Test(); return t.monNom").equals("5");
		// Écriture : l'affectation cible bien le champ.
		code_v2_("class Test { public integer monNom = 5; public integer monNom() { return 7; } } Test t = new Test(); t.monNom = 9; return t.monNom").equals("9");
		// Sans collision, tout compile normalement
		code_v2_("class Test { private integer monNom = 3; public integer getMonNom() { return monNom; } } return new Test().getMonNom()").equals("3");
	}

	@Test
	public void testNonMinusexistent_field_access() throws Exception {
		section("Non-existent field access");
		// this.field where field doesn't exist: error in strict, warning in non-strict
		code_strict_v2_("class A { public m() { return this.x } } return new A().m()").error(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		code_v2_("class A { public m() { return this.x } } return new A().m()").warning(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		// Existing field should still work
		code_v2_("class A { public integer x = 42 public m() { return this.x } } return new A().m()").equals("42");
		// Implicit this field access to non-existent field
		code_strict_v2_("class A { public m() { return x } } return new A().m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		// Non-existent field on typed variable
		code_strict_v2_("class Foo { } Foo f = new Foo() return f.x").error(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		code_v2_("class Foo { } Foo f = new Foo() return f.x").warning(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		// Dynamic this.method() (non-statically-known) must compile (regression: u_this not in scope)
		code_v2_("class A { public m() { return this.unknownMethod() } } return new A().m()").equals("null");
	}

	/**
	 * #4268 : foreach sans `var` dont l'itérateur porte le nom d'un champ de classe
	 * (ici hérité). preAnalyze trouvait le champ (includeClassMembers=true) et ne
	 * signalait pas d'erreur, mais analyze résout avec false → iteratorVariable=null
	 * → NPE dans ForeachBlock.writeJavaCode. Doit produire une erreur propre, pas crasher.
	 */
	@Test
	public void testForeach_iterator_shadows_class_field() throws Exception {
		section("Issue #4268 - foreach iterator named like a class field (no var)");
		// Champ direct
		code_v2_4("class A { public field = [1, 2, 3] public m() { for (field in field) {} } } return new A().m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		// Champ hérité (cas exact du rapport : Enemy extends Leek, `cell` défini dans Leek)
		code_v2_4("class P { public cell = 0 } class C extends P { public m(any arr) { for (cell in arr) {} } } return new C().m([1, 2])").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		// Sanity : avec `var`, c'est une déclaration valide et ça itère normalement
		code_v2_4("class A { public field = [1, 2, 3] public m() { var s = 0 for (var x in field) { s += x } return s } } return new A().m()").equals("6");
	}

	/**
	 * Optional chaining `obj?.field` / `obj?.method()` (#2272) : court-circuite à null
	 * si l'objet est null, sinon accès/appel normal.
	 */
	@Test
	public void testOptional_chaining() throws Exception {
		section("Optional chaining");
		// Accès champ sur objet non null
		code_v4_("class A { public x = 42 } var a = new A() return a?.x").equals("42");
		// Appel de méthode sur objet non null
		code_v4_("class A { method m() { return 7 } } var a = new A() return a?.m()").equals("7");
		// Méthode avec arguments
		code_v4_("class A { method add(a, b) { return a + b } } var a = new A() return a?.add(3, 4)").equals("7");
		// Court-circuit : objet null → null
		code_v4_("class A { public x = 42 } A? a = null return a?.x").equals("null");
		code_v4_("class A { method m() { return 7 } } A? a = null return a?.m()").equals("null");
		// Chaîne : un maillon null court-circuite la suite
		code_v4_("class A { public next = null public v = 1 } var a = new A() return a?.next?.v").equals("null");
		// Chaîne d'appels
		code_v4_("class A { method self() { return this } public x = 9 } var a = new A() return a?.self()?.x").equals("9");
		// Objet anonyme
		code_v4_("var o = {x: 5} return o?.x").equals("5");
		code_v4_("var o = null return o?.x").equals("null");
		// Résultat utilisé dans une expression
		code_v4_("class A { public x = 10 } var a = new A() return a?.x + 5").equals("15");
		// `.` normal puis `?.`
		code_v4_("class B { public v = 3 } class A { public b = null } var a = new A() a.b = new B() return a.b?.v").equals("3");
		// Le ternaire reste non ambigu
		code_v4_("class A { public x = 7 } var a = new A() return true ? a.x : 0").equals("7");
		code_v4_("return 1 > 2 ? 10 : 20").equals("20");
		// Accès optionnel non assignable
		code_v4_("class A { public x = 1 } var a = new A() a?.x = 5 return a.x").error(Error.CANT_ASSIGN_VALUE);
		// Appel de méthode optionnel en mode strict : pas de faux warning "may not be
		// callable" (l'accès optionnel passe par le chemin dynamique null-safe) (#4204)
		code_strict_v4_("class A { a(b) { return b } compute() { return this?.a(5) } } var x = new A() return x.compute()").equals("5");
		code_strict_v4_("class A { method m() { return 7 } } var a = new A() return a?.m()").noWarning();
		code_strict_v4_("class A { method add(a, b) { return a + b } } var a = new A() return a?.add(3, 4)").noWarning();
		// `obj?.field` est typé `membre | null` (et non ANY) : le type du membre est
		// conservé, exploitable en mode strict, sans faux warning (#4204).
		code_strict_v4_("class A { public integer x = 10 } var a = new A() return a?.x + 5").equals("15");
		code_strict_v4_("class A { public integer x = 10 } var a = new A() return a?.x + 5").noWarning();
	}

	@Test
	public void testClass_clone_error_routing() throws Exception {
		section("Class clone() error routing (no stdout swallow)");
		// Deep-clone d'une instance de classe imbriquée : copies indépendantes
		code_v4_("class B { public integer x = 5 } class A { public B b = new B() } var a = new A() var c = clone(a, 3) c.b.x = 9 return [a.b.x, c.b.x]").equals("[5, 9]");
		// Un clone qui dépasse la limite RAM dans le constructeur de copie doit remonter
		// proprement OUT_OF_MEMORY (avant : avalé + craché sur stdout par le stub ErrorManager,
		// qui spammait les logs du worker).
		long low_ram = 10_000;
		code_v4("class A { public data = [] public fill() { for (var i = 0; i < 8000; ++i) push(data, i) } } var a = new A() a.fill() return clone(a, 2)").max_ram(low_ram).error(Error.OUT_OF_MEMORY);
	}

}
