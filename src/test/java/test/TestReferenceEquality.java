package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

/**
 * `==` / `!=` entre deux références d'objet sont émis en comparaison d'adresse
 * directe au lieu de passer par equals_equals()/eq(). Ces tests verrouillent
 * l'équivalence : identité pour les objets, égalité structurelle intacte pour
 * les tableaux, maps et sets qui doivent rester hors du chemin optimisé.
 */
@ExtendWith(SummaryExtension.class)
public class TestReferenceEquality extends TestCommon {

	@Test
	public void testClass_instances_identity() throws Exception {
		section("Identité entre instances de classe");
		code_v4_("class A {} var a = new A() return a == a").equals("true");
		code_v4_("class A {} var a = new A() var b = new A() return a == b").equals("false");
		code_v4_("class A {} var a = new A() var b = a return a == b").equals("true");
		code_v4_("class A {} var a = new A() var b = new A() return a != b").equals("true");
		code_v4_("class A {} var a = new A() return a != a").equals("false");
		// Mêmes champs, instances distinctes : toujours faux.
		code_v4_("class A { public v = 1 } var a = new A() var b = new A() return a == b").equals("false");
	}

	@Test
	public void testTyped_class_variables() throws Exception {
		section("Variables typées par la classe");
		code_v4_("class A {} A a = new A() A b = new A() return a == b").equals("false");
		code_v4_("class A {} A a = new A() A b = a return a == b").equals("true");
		code_v4_("class A {} A a = new A() A b = a return a != b").equals("false");
		code_v4_("class A {} function f(A x, A y) { return x == y } var a = new A() return f(a, a)").equals("true");
		code_v4_("class A {} function f(A x, A y) { return x == y } return f(new A(), new A())").equals("false");
	}

	@Test
	public void testUnrelated_classes() throws Exception {
		section("Classes sans lien de parenté");
		// Java refuserait `u_Dog == u_Cat` sans le cast en Object.
		code_v4_("class Dog {} class Cat {} Dog d = new Dog() Cat c = new Cat() return d == c").equals("false");
		code_v4_("class Dog {} class Cat {} Dog d = new Dog() Cat c = new Cat() return d != c").equals("true");
	}

	@Test
	public void testInheritance() throws Exception {
		section("Héritage");
		code_v4_("class A {} class B extends A {} A a = new B() var b = a return a == b").equals("true");
		code_v4_("class A {} class B extends A {} A a = new B() A b = new B() return a == b").equals("false");
		code_v4_("class A {} class B extends A {} B b = new B() A a = b return a == b").equals("true");
	}

	@Test
	public void testNull_operand() throws Exception {
		section("Opérande null");
		code_v4_("class A {} var a = new A() return a == null").equals("false");
		code_v4_("class A {} var a = new A() return a != null").equals("true");
		code_v4_("class A {} var a = null return a == null").equals("true");
		code_v4_("class A {} function f(A x) { return x == null } return f(null)").equals("true");
		code_v4_("class A {} function f(A x) { return x == null } return f(new A())").equals("false");
	}

	@Test
	public void testObject_literals() throws Exception {
		section("Objets littéraux");
		code_v4_("var o = {} return o == o").equals("true");
		code_v4_("return {} == {}").equals("false");
		code_v4_("var a = {x: 1} var b = {x: 1} return a == b").equals("false");
		code_v4_("var a = {x: 1} var b = a return a == b").equals("true");
		// Objet littéral face à une instance de classe.
		code_v4_("class A {} var a = new A() var o = {} return a == o").equals("false");
	}

	@Test
	public void testStructural_equality_untouched() throws Exception {
		section("Égalité structurelle préservée");
		// Tableaux, maps et sets ne doivent PAS basculer sur l'identité.
		code_v4_("return [1, 2] == [1, 2]").equals("true");
		code_v4_("return [1, 2] == [1, 3]").equals("false");
		code_v4_("var a = [1, 2] var b = [1, 2] return a == b").equals("true");
		code_v4_("return [1, 2] != [1, 3]").equals("true");
		code_v4_("return ['a': 1] == ['a': 1]").equals("true");
		code_v4_("var a = [1, 2] var b = [1, 2] return a != b").equals("false");
	}

	@Test
	public void testMixed_with_dynamic() throws Exception {
		section("Mélange avec des types dynamiques");
		// Un côté `any` : pas de chemin rapide, equals_equals reste seul juge.
		code_v4_("class A {} var a = new A() var x = 1 return a == x").equals("false");
		code_v4_("class A {} var a = new A() var x = 'hello' return a == x").equals("false");
		code_v4_("class A {} function f(x) { var a = new A() return a == x } var a2 = new A() return f(a2)").equals("false");
		code_v4_("class A {} function f(x) { return x == x } return f(new A())").equals("true");
	}

	@Test
	public void testTriple_equals() throws Exception {
		section("Opérateurs === et !==");
		code_v3_("class A {} var a = new A() return a === a").equals("true");
		code_v3_("class A {} var a = new A() var b = new A() return a === b").equals("false");
		code_v3_("class A {} var a = new A() var b = new A() return a !== b").equals("true");
		code_v3_("class A {} var a = new A() return a !== a").equals("false");
	}

	@Test
	public void testVersion_3_loose_equality() throws Exception {
		section("v3 : `==` lâche, identité pour les objets");
		code_v3_("class A {} var a = new A() return a == a").equals("true");
		code_v3_("class A {} var a = new A() var b = new A() return a == b").equals("false");
		code_v3_("class A {} var a = new A() var b = new A() return a != b").equals("true");
		code_v3_("class A {} var a = new A() return a == null").equals("false");
	}
}
