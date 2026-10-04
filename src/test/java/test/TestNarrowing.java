package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import leekscript.common.Error;


@ExtendWith(SummaryExtension.class)
public class TestNarrowing extends TestCommon {

		@Test
	public void testInit() throws Exception {
		header("Type Narrowing");
	}

	@Test
	public void testBasic_null_check_if() throws Exception {
		section("Basic null check: if (x != null)");
		// With narrowing: abs(x) should not warn inside if (x != null)
		code_v4_("integer | null x = 5; if (x != null) { return abs(x) } return 0").noWarning();
		// Functional correctness
		code_v4_("integer | null x = 5; if (x != null) { return abs(x) } return 0").equals("5");
		code_v4_("integer | null x = -3; if (x != null) { return abs(x) } return 0").equals("3");
		code_v4_("integer | null x = null; if (x != null) { return abs(x) } return 0").equals("0");
	}

	@Test
	public void testEquality_null_check_if() throws Exception {
		section("Equality null check: if (x == null)");
		// x == null: in else branch, x is non-null
		code_v4_("integer | null x = 5; if (x == null) { return 0 } else { return abs(x) }").noWarning();
		code_v4_("integer | null x = 5; if (x == null) { return 0 } else { return abs(x) }").equals("5");
	}

	@Test
	public void testTruthy_check_if() throws Exception {
		section("Truthy check: if (x)");
		// Truthy check narrows nullable to non-null in true branch
		code_v4_("integer | null x = 5; if (x) { return abs(x) } return 0").noWarning();
		code_v4_("integer | null x = 5; if (x) { return abs(x) } return 0").equals("5");
		code_v4_("integer | null x = null; if (x) { return abs(x) } return 0").equals("0");
	}

	@Test
	public void testCase_1_AndAnd_Divide_OrOr_operators() throws Exception {
		section("Case 1: && / || operators");
		// && combines narrowings
		code_v4_("integer | null x = 5; integer | null y = 3; if (x != null && y != null) { return abs(x) + abs(y) } return 0").noWarning();
		code_v4_("integer | null x = 5; integer | null y = 3; if (x != null && y != null) { return abs(x) + abs(y) } return 0").equals("8");
		// || combines false narrowings
		code_v4_("integer | null x = null; integer | null y = null; if (x == null || y == null) { return 0 }").equals("0");
		// Short-circuit: x != null && abs(x) → x is narrowed in the right operand
		code_v4_("integer | null x = 5; if (x != null && abs(x) > 0) { return 1 } return 0").noWarning();
		code_v4_("integer | null x = 5; if (x != null && abs(x) > 0) { return 1 } return 0").equals("1");
	}

	@Test
	public void testCase_2_Early_return_narrowing() throws Exception {
		section("Case 2: Early return narrowing");
		// if (x == null) return → x is non-null after
		code_v4_("integer | null x = 5; if (x == null) return 0; return abs(x)").noWarning();
		code_v4_("integer | null x = 5; if (x == null) return 0; return abs(x)").equals("5");
		code_v4_("integer | null x = null; if (x == null) return 0; return abs(x)").equals("0");
		// if (x != null) { ... } with no else doesn't narrow after
		// (only early return narrows)
	}

	@Test
	public void testCase_3_While_loop_narrowing() throws Exception {
		section("Case 3: While loop narrowing");
		// while (x != null) → x is non-null inside loop
		code_v4_("integer | null x = 5; var r = 0; while (x != null) { r = abs(x); x = null } return r").max_ops(1000).noWarning();
		code_v4_("integer | null x = 5; var r = 0; while (x != null) { r = abs(x); x = null } return r").max_ops(1000).equals("5");
		// After reassignment (w = null), w is narrowed to null type (assignment narrowing)
		code_v4_("integer | null w = 5; var r = 0; while (w != null) { r = abs(w); w = null } return r").max_ops(1000).noWarning();
	}

	@Test
	public void testCase_3b_For_loop_narrowing() throws Exception {
		section("Case 3b: For loop narrowing");
		// for (...; a != null; ...) → a is non-null inside loop (issue #5050)
		code_v4_("integer | null a = 4; var r = 0; for (integer i = 0; i < 10 && a != null; i++) { r = abs(a); a = null } return r").max_ops(1000).noWarning();
		code_v4_("integer | null a = 4; var r = 0; for (integer i = 0; i < 10 && a != null; i++) { r = abs(a); a = null } return r").max_ops(1000).equals("4");
		// Condition seule, sans opérande supplémentaire
		code_v4_("integer | null b = 7; var r = 0; for (var i = 0; b != null; i++) { r = abs(b); b = null } return r").max_ops(1000).noWarning();
		// Le narrowing ne fuit pas après la boucle
		code_v4_("integer | null c = 4; for (var i = 0; i < 1 && c != null; i++) {} return c == null ? 0 : 1").max_ops(1000).equals("1");
	}

	@Test
	public void testCase_3c_Ternary_narrowing() throws Exception {
		section("Case 3c: Ternary narrowing");
		// a != null ? abs(a) : 0 → a is non-null in the true branch (issue #5050)
		code_v4_("integer | null a = 4; return a != null ? abs(a) : 0").noWarning();
		code_v4_("integer | null a = 4; return a != null ? abs(a) : 0").equals("4");
		code_v4_("integer | null a = null; return a != null ? abs(a) : 0").equals("0");
		// Branche fausse : a == null ? 0 : abs(a)
		code_v4_("integer | null a = -3; return a == null ? 0 : abs(a)").noWarning();
		code_v4_("integer | null a = -3; return a == null ? 0 : abs(a)").equals("3");
		// Le narrowing ne fuit pas hors du ternaire
		code_v4_("integer | null a = 4; var r = a != null ? 1 : 0; return a == null ? 0 : r").equals("1");
	}

	@Test
	public void testCase_4_instanceof_narrowing() throws Exception {
		section("Case 4: instanceof narrowing");
		// instanceof narrows the type (class types like Array)
		code_v4_("Array | string x = [1, 2]; if (x instanceof Array) { return count(x) } return 0").noWarning();
		code_v4_("Array | string x = [1, 2]; if (x instanceof Array) { return count(x) } return 0").equals("2");
	}

	@Test
	public void testCase_5_Assignment_in_condition() throws Exception {
		section("Case 5: Assignment in condition");
		// (x = getValue()) != null → x is narrowed in the if body
		code_v4_("function f() => integer | null { return 5 } integer | null x = null; if ((x = f()) != null) { return abs(x) } return 0").noWarning();
		code_v4_("function f() => integer | null { return 5 } integer | null x = null; if ((x = f()) != null) { return abs(x) } return 0").equals("5");
	}

	@Test
	public void testNegation_Not() throws Exception {
		section("Negation: !(x == null)");
		code_v4_("integer | null x = 5; if (!(x == null)) { return abs(x) } return 0").noWarning();
		code_v4_("integer | null x = 5; if (!(x == null)) { return abs(x) } return 0").equals("5");
	}

	@Test
	public void testEarly_return_in_function_with_parameter() throws Exception {
		section("Early return in function with parameter");
		// Early return narrowing should work with function parameters too
		code_v4_("function process(integer | null val) { if (val == null) return null; return abs(val) } return process(5)").noWarning();
		code_v4_("function process(integer | null val) { if (val == null) return null; return abs(val) } return process(5)").equals("5");
		// Bare return (no value) should also narrow
		code_v4_("function process(integer | null val) { if (val == null) return null; debug(abs(val)) } process(5)").noWarning();
		// Exact user pattern: return without value
		code_v4_("function process(integer | null val) { if (val == null) { return } debug(abs(val)) } process(5)").noWarning();
		// Function not called - should still narrow correctly
		code_v4_("function process(integer | null val) { if (val == null) { return } debug(abs(val)) }").noWarning();
	}

	@Test
	public void testElseMinusif_chains() throws Exception {
		section("Else-if chains");
		code_v4_("integer | null x = 5; integer | null y = 3; if (x == null) { return 0 } else if (y == null) { return abs(x) } else { return abs(x) + abs(y) }").noWarning();
		code_v4_("integer | null x = 5; integer | null y = 3; if (x == null) { return 0 } else if (y == null) { return abs(x) } else { return abs(x) + abs(y) }").equals("8");
	}

	@Test
	public void testSwitch_narrowing() throws Exception {
		section("Switch narrowing");
		// case null → variable is null, other cases → variable is non-null
		code_v4_("integer | null x = 5; switch (x) { case null: return 0; default: return abs(x) }").noWarning();
		code_v4_("integer | null x = 5; switch (x) { case null: return 0; default: return abs(x) }").equals("5");
		code_v4_("integer | null x = null; switch (x) { case null: return 0; default: return abs(x) }").equals("0");
		// Non-null case value: variable is non-null
		code_v4_("integer | null x = 5; switch (x) { case null: return 0; case 5: return abs(x); default: return abs(x) }").noWarning();
		code_v4_("integer | null x = 5; switch (x) { case null: return 0; case 5: return abs(x); default: return abs(x) }").equals("5");
		// Without case null, default can't be narrowed
		code_v4_("integer | null x = 5; switch (x) { case 5: return abs(x); default: return 0 }").noWarning();
	}

	@Test
	public void testClass_field_narrowing() throws Exception {
		section("Class field narrowing");
		// Assignment inside narrowed branch should use declared type, not narrowed type
		code_v4_("class A { integer | null x = null; m() { if (x == null) { x = 12 } } } var a = new A(); a.m(); return a.x").noWarning();
		code_v4_("class A { integer | null x = null; m() { if (x == null) { x = 12 } } } var a = new A(); a.m(); return a.x").equals("12");
		// Static field: basic assignment (no narrowing) — should work without warning
		code_v4_("class A { static integer | null x = null; m() { class.x = 12 } } var a = new A(); a.m(); return A.x").noWarning();
		// Static field: assignment inside narrowed branch
		code_v4_("class A { static integer | null x = null; m() { if (class.x == null) { class.x = 12 } } } var a = new A(); a.m(); return A.x").noWarning();
		code_v4_("class A { static integer | null x = null; m() { if (class.x == null) { class.x = 12 } } } var a = new A(); a.m(); return A.x").equals("12");

		// Class-typed field: assignment inside narrowed block
		code_v4_("class B { integer x = 0 } class A { B | null config = null; m() { if (config == null) { config = new B() } } } var a = new A(); a.m(); return a.config").noWarning();
		// Class-typed field: assignment to class-typed field with narrowing
		code_v4_("class Cell { integer v = 42 } class A { Cell | null c = null; m() { if (c == null) { c = new Cell() } } } var a = new A(); a.m(); return a.c.v").equals("42");
		// Class-typed local variable assigned from narrowed variable
		code_v4_("class Cell { integer v = 0 } Cell | null x = new Cell(); if (x != null) { var y = x; y = new Cell(); return y.v } return -1").equals("0");

		// Class-typed nullable field: assignment inside null-check block
		code_v4_("class B { integer x = 0 } class A { B? config = null; m() { if (config == null) { config = new B() } } } var a = new A(); a.m(); return a.config.x").equals("0");
		code_v4_("class B { integer x = 0 } class A { B? config = null; m() { if (config == null) { config = new B() } } } var a = new A(); a.m(); return a.config.x").noWarning();
		// Non-null branch: assignment to nullable class field
		code_v4_("class B { integer x = 0 } class A { B? c = new B(); m() { if (c != null) { c = new B() } } } var a = new A(); a.m(); return a.c.x").equals("0");
	}

	@Test
	public void testInstanceof_narrowing_with_compound_types() throws Exception {
		section("Instanceof narrowing with compound types (Java cast)");
		// Map | integer narrowed to Map → .get() needs a cast in Java
		code_v4_("Map | integer x = [1 : 'a', 2 : 'b']; if (x instanceof Map) { return x[1] } return 0").noWarning();
		code_v4_("Map | integer x = [1 : 'a', 2 : 'b']; if (x instanceof Map) { return x[1] } return 0").equals("\"a\"");
		// Array | string narrowed to Array → count() and subscript work
		code_v4_("Array | string x = [1, 2, 3]; if (x instanceof Array) { return count(x) } return 0").noWarning();
		code_v4_("Array | string x = [1, 2, 3]; if (x instanceof Array) { return count(x) } return 0").equals("3");
		code_v4_("Array | string x = [10, 20, 30]; if (x instanceof Array) { return x[1] } return 0").noWarning();
		code_v4_("Array | string x = [10, 20, 30]; if (x instanceof Array) { return x[1] } return 0").equals("20");
		// Map access after instanceof inside && short-circuit
		code_v4_("Map | integer x = [1 : 'a']; if (x instanceof Map && x[1] == 'a') { return 1 } return 0").noWarning();
		code_v4_("Map | integer x = [1 : 'a']; if (x instanceof Map && x[1] == 'a') { return 1 } return 0").equals("1");
		// Multiple map accesses after instanceof
		code_v4_("Map | integer x = [1 : 10, 2 : 20]; if (x instanceof Map) { return x[1] + x[2] } return 0").noWarning();
		code_v4_("Map | integer x = [1 : 10, 2 : 20]; if (x instanceof Map) { return x[1] + x[2] } return 0").equals("30");
		// real | null | integer narrowed to real → doubleValue() needs a cast
		code_v4_("real | null | integer x = 3.14; real y = 0; if (x != null) { y = x } return y").noWarning();
		code_v4_("real | null | integer x = 3.14; real y = 0; if (x != null) { y = x } return y").equals("3.14");
		// Narrowing with assignment after instanceof (reset then re-narrow)
		code_v4_("Map | integer x = [1 : 'a']; if (x instanceof Map) { var v = x[1]; x = [2 : 'b']; return v } return 0").noWarning();
		code_v4_("Map | integer x = [1 : 'a']; if (x instanceof Map) { var v = x[1]; x = [2 : 'b']; return v } return 0").equals("\"a\"");
		// Compound assignment with narrowed primitive in else branch (mpLeft -= cellArray)
		code_v4_("Map | integer x = 5; integer y = 10; if (x instanceof Map) { return 0 } else { y -= x; return y }").noWarning();
		code_v4_("Map | integer x = 5; integer y = 10; if (x instanceof Map) { return 0 } else { y -= x; return y }").equals("5");
		code_v4_("Map | integer x = 3; integer y = 10; if (x instanceof Map) { return 0 } else { y += x; return y }").equals("13");
		code_v4_("Map | integer x = 2; integer y = 10; if (x instanceof Map) { return 0 } else { y *= x; return y }").equals("20");
	}

	@Test
	public void testInstanceof_narrowing_with_property_access() throws Exception {
		section("Instanceof narrowing with property access");
		// instanceof + && should narrow property type and generate correct Java cast
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } class Holder { Item item = new Chip() } var h = new Holder(); if (h.item instanceof Chip && h.item.flag) { return 1 } return 0").equals("1");
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } class Holder { Item item = new Chip() } var h = new Holder(); if (h.item instanceof Chip && h.item.flag) { return 1 } return 0").noWarning();
		// instanceof on a local variable
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } Item item = new Chip(); if (item instanceof Chip && item.flag) { return 1 } return 0").equals("1");
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } Item item = new Chip(); if (item instanceof Chip && item.flag) { return 1 } return 0").noWarning();
		// instanceof on a global variable
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } global Item item = new Chip(); if (item instanceof Chip && item.flag) { return 1 } return 0").equals("1");
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } global Item item = new Chip(); if (item instanceof Chip && item.flag) { return 1 } return 0").noWarning();
		// instanceof on an instance field (this.field)
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } class Holder { Item item = new Chip(); test() { if (item instanceof Chip && item.flag) { return 1 } return 0 } } return new Holder().test()").equals("1");
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } class Holder { Item item = new Chip(); test() { if (item instanceof Chip && item.flag) { return 1 } return 0 } } return new Holder().test()").noWarning();
		// instanceof on a static field (class.field)
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } class Holder { static Item item = new Chip(); test() { if (class.item instanceof Chip && class.item.flag) { return 1 } return 0 } } return new Holder().test()").equals("1");
		code_v4_("class Item {} class Chip extends Item { boolean flag = true } class Holder { static Item item = new Chip(); test() { if (class.item instanceof Chip && class.item.flag) { return 1 } return 0 } } return new Holder().test()").noWarning();
	}

	/**
	 * Appel de MÉTHODE sur un champ narrowé (#4933). Le champ Java garde son type déclaré :
	 * sans cast, javac ne trouve pas la méthode du type narrowé et l'IA ne compile plus
	 * (« Votre IA n'a pas pu être compilée correctement »). Un accès à un CHAMP passait
	 * déjà, d'où un bug longtemps invisible.
	 */
	@Test
	public void testInstanceof_narrowing_method_call_on_field() throws Exception {
		section("Instanceof narrowing: method call on a narrowed field");
		// Champ d'instance désigné sans `this`
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new B(); test() { if (m instanceof B) { return m.go() } return 0 } } return new C().test()").equals("7");
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new B(); test() { if (m instanceof B) { return m.go() } return 0 } } return new C().test()").noWarning();
		// Même champ désigné par `this`
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new B(); test() { if (this.m instanceof B) { return this.m.go() } return 0 } } return new C().test()").equals("7");
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new B(); test() { if (this.m instanceof B) { return this.m.go() } return 0 } } return new C().test()").noWarning();
		// Branche else d'un `!(x instanceof T)`
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new B(); test() { if (!(m instanceof B)) { return 0 } else { return m.go() } } } return new C().test()").equals("7");
		// Un champ qui n'est pas du type testé prend bien la branche else
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new A(); test() { if (m instanceof B) { return m.go() } return 0 } } return new C().test()").equals("0");
		// Champ d'une autre instance (déjà couvert par le cast de receveur, non-régression)
		code_v4_("class A {} class B extends A { integer go() { return 7 } } class C { A m = new B() } var c = new C(); if (c.m instanceof B) { return c.m.go() } return 0").equals("7");
	}

	/**
	 * Un `instanceof` sans lien avec le type déclaré du champ (donc toujours faux) narrowe quand
	 * même vers le type testé : le cast serait illégal en Java (« incompatible types ») et ferait
	 * échouer la compilation d'une IA qui passait très bien. On n'émet donc le cast que s'il est
	 * légal, et le code mort reste compilable.
	 */
	@Test
	public void testInstanceof_narrowing_incompatible_type_no_cast() throws Exception {
		section("Instanceof narrowing: pas de cast quand le type testé est incompatible");
		// Classes sans lien de parenté
		code_v4_("class A {} class D {} class C { A m = new A(); test() { if (m instanceof D) { return 1 } return 0 } } return new C().test()").equals("0");
		// Champ primitif testé contre une classe, avec et sans `this`
		code_v4_("class B {} class C { integer m = 1; test() { if (m instanceof B) { return 1 } return 0 } } return new C().test()").equals("0");
		code_v4_("class B {} class C { integer m = 1; test() { if (this.m instanceof B) { return 1 } return 0 } } return new C().test()").equals("0");
		code_v4_("class B {} class C { integer m = 1 } var c = new C(); if (c.m instanceof B) { return 1 } return 0").equals("0");
		// Types conteneurs incompatibles entre eux
		code_v4_("class C { Array m = [1]; test() { if (m instanceof Map) { return 1 } return 0 } } return new C().test()").equals("0");
		code_v4_("class C { Map m = [1 : 2]; test() { if (m instanceof Array) { return 1 } return 0 } } return new C().test()").equals("0");
		code_v4_("class C { Set m = <1>; test() { if (m instanceof Array) { return 1 } return 0 } } return new C().test()").equals("0");
		code_v4_("class C { string m = 'x'; test() { if (m instanceof Array) { return 1 } return 0 } } return new C().test()").equals("0");
	}

	/**
	 * Le cas FIELD passe désormais par le même chemin que les variables locales, donc aussi par la
	 * conversion vers un type PRIMITIF (`longint(...)` / `real(...)`) et pas seulement par le cast.
	 * Surface nouvelle : on la verrouille ici.
	 */
	@Test
	public void testInstanceof_narrowing_field_to_primitive() throws Exception {
		section("Instanceof narrowing: champ narrowé vers un type primitif");
		// Champ d'union narrowé vers integer dans la branche else, puis utilisé en arithmétique
		code_v4_("class C { Map | integer m = 5; test() { if (m instanceof Map) { return 0 } else { return m + 1 } } } return new C().test()").equals("6");
		code_v4_("class C { Map | integer m = 5; test() { if (m instanceof Map) { return 0 } else { return m + 1 } } } return new C().test()").noWarning();
		// NB : la même chose écrite `this.m` ne compile PAS, et ne compilait déjà pas avant ce
		// correctif (vérifié par un build de la révision précédente). LeekObjectAccess n'a pas de
		// chemin de conversion vers un primitif, seulement le cast de classe : `this.m + 1` émet
		// `m + 1l` avec m en Object. Bug distinct, non traité ici.
		// Et le cas où la branche est bien prise
		code_v4_("class C { Map | integer m = [1 : 2]; test() { if (m instanceof Map) { return 1 } else { return m + 1 } } } return new C().test()").equals("1");
	}

	@Test
	public void testInstanceof_primitive_narrowing_in_else_branch() throws Exception {
		section("Instanceof narrowing to primitive type in else branch");
		// Union type narrowed to integer in else branch (the instanceof-excluded type)
		// The Java variable is Object but needs to be passed as long
		code_v4_("class Cell { integer id = 5 } Cell | integer x = 3; if (x instanceof Cell) { return x.id } else { return abs(x) }").equals("3");
		code_v4_("class Cell { integer id = 5 } Cell | integer x = 3; if (x instanceof Cell) { return x.id } else { return abs(x) }").noWarning();
		code_v4_("class Cell { integer id = 5 } Cell | integer x = new Cell(); if (x instanceof Cell) { return x.id } else { return abs(x) }").equals("5");
		// Narrowed to integer and used in arithmetic
		code_v4_("Array | integer x = 7; if (x instanceof Array) { return count(x) } else { return x + 1 }").equals("8");
		code_v4_("Array | integer x = [1, 2]; if (x instanceof Array) { return count(x) } else { return x + 1 }").equals("2");
		// Narrowed to integer and passed to a function expecting integer
		code_v4_("Map | integer x = 42; if (x instanceof Map) { return 0 } else { return abs(x) }").equals("42");
		code_v4_("Map | integer x = 42; if (x instanceof Map) { return 0 } else { return abs(x) }").noWarning();
		// Narrowed to real in else branch
		code_v4_("Array | real x = 3.14; if (x instanceof Array) { return 0 } else { return floor(x) }").equals("3");
		code_v4_("Array | real x = 3.14; if (x instanceof Array) { return 0 } else { return floor(x) }").noWarning();
		// Variable used as function argument in else branch
		code_v4_("class A { integer v = 0 } A | integer x = 10; function add(integer a, integer b) { return a + b } if (x instanceof A) { return x.v } else { return add(x, 5) }").equals("15");
		code_v4_("class A { integer v = 0 } A | integer x = 10; function add(integer a, integer b) { return a + b } if (x instanceof A) { return x.v } else { return add(x, 5) }").noWarning();
	}

	@Test
	public void testInstanceof_narrowing_wrapper_box_variable() throws Exception {
		section("Instanceof narrowing on wrapper/box variables (closures)");
		// Variable captured in closure becomes a wrapper (box). After instanceof
		// narrowing, .get() returns Object and needs a cast to the narrowed type.
		// Class method call on narrowed wrapper
		code_v4_("class A { integer val = 42; getVal() { return val } } var x = new A(); var f = function() { x = new A() }; if (x instanceof A) { return x.getVal() } return 0").equals("42");
		code_v4_("class A { integer val = 42; getVal() { return val } } var x = new A(); var f = function() { x = new A() }; if (x instanceof A) { return x.getVal() } return 0").noWarning();
		// Field access on narrowed wrapper
		code_v4_("class B { integer v = 10 } var x = new B(); var f = function() { x = new B() }; if (x instanceof B) { return x.v } return 0").equals("10");
		code_v4_("class B { integer v = 10 } var x = new B(); var f = function() { x = new B() }; if (x instanceof B) { return x.v } return 0").noWarning();
		// Narrowing to primitive on wrapper variable (instanceof else branch)
		code_v4_("class C { integer id = 1 } C | integer x = 5; var f = function() { x = 10 }; if (x instanceof C) { return x.id } else { return x + 1 }").equals("6");
		code_v4_("class C { integer id = 1 } C | integer x = 5; var f = function() { x = 10 }; if (x instanceof C) { return x.id } else { return x + 1 }").noWarning();
		// Null check narrowing on wrapper variable
		code_v4_("class D { integer v = 7 } D | null x = new D(); var f = function() { x = null }; if (x != null) { return x.v } return 0").equals("7");
		code_v4_("class D { integer v = 7 } D | null x = new D(); var f = function() { x = null }; if (x != null) { return x.v } return 0").noWarning();
		// Multiple uses of narrowed wrapper in same branch
		code_v4_("class E { integer a = 3; integer b = 4 } var x = new E(); var f = function() { x = new E() }; if (x instanceof E) { return x.a + x.b } return 0").equals("7");
	}

	@Test
	public void testAs_cast_with_instanceof_narrowing_in_and() throws Exception {
		section("'as' cast must emit Java cast even when narrowing makes types match");
		// Bug: inside && after instanceof, the narrowing changes the analysis type,
		// making compileConvert think the 'as' cast is unnecessary. But the Java
		// code still needs the cast because the runtime type is the parent class.
		code_v4_("class Item { } class Chip extends Item { boolean ready = true } class Action { Item item } var a = new Action(); a.item = new Chip(); if (a.item instanceof Chip && (a.item as Chip).ready) { return 1 } return 0").equals("1");
		// Same with field access on the casted object
		code_v4_("class Base { } class Sub extends Base { integer val = 42 } class Container { Base obj } var c = new Container(); c.obj = new Sub(); if (c.obj instanceof Sub && (c.obj as Sub).val == 42) { return 1 } return 0").equals("1");
		// 'as' cast without && narrowing (should also work)
		code_v4_("class Base { } class Sub extends Base { integer val = 7 } Base b = new Sub(); return (b as Sub).val").equals("7");
	}

	@Test
	public void testAs_real_on_union() throws Exception {
		section("'as real' on integer|real union must not warn (#2450)");
		// `a` a le type union integer|real ; le caster en real est sûr (upcast),
		// aucune conversion dangereuse ne doit être signalée.
		code_strict_v4_("integer|real a = 2; real b = a as real; return b").noWarning();
		code_strict_v4_("integer|real a = 2; real b = a as real; return b").almost(2.0);
		code_strict_v4_("real|integer a = 2; real b = a as real; return b").noWarning();
	}

	@Test
	public void testCompound_type_with_map_and_integer_instanceof() throws Exception {
		section("Compound type integer|Map? with instanceof Map narrowing");
		// Bug: isMapOrNull() returned true for integer|Map? causing toMapOrNull()
		// to be generated for assignments, which fails when value is an integer.
		code_v4_("integer|Map? x = [1: 2]; if (x instanceof Map) { x = x[1] } return x").equals("2");
		code_v4_("integer|Map? x = 5; if (x instanceof Map) { x = x[1] } return x").equals("5");
		code_v4_("Map<integer, integer|Map> m = [0: 3, 1: [2: 10]]; integer|Map? v = m[1]; if (v instanceof Map) { v = v[2] } return v").equals("10");
		code_v4_("Map<integer, integer|Map> m = [0: 3, 1: [2: 10]]; integer|Map? v = m[0]; if (v instanceof Map) { v = v[2] } return v").equals("3");
	}

	@Test
	public void testGlobal_variable_assignment_inside_null_check() throws Exception {
		section("Global variable assignment inside null check");
		// global var assigned inside if (x == null) block
		code_v4_("class A { integer x = 42 } global obj = null; if (obj == null) { obj = new A() } return obj.x").equals("42");
		code_v4_("class A { integer x = 42 } global obj = null; if (obj == null) { obj = new A() } return obj.x").noWarning();
		// Multiple assignments in if/else-if chain
		code_v4_("class A { integer x = 1 } class B extends A { } global obj = null; if (obj == null) { obj = new A() } return obj.x").equals("1");
	}

	@Test
	public void testInstanceof_narrowing_nullable_property_access() throws Exception {
		section("Instanceof narrowing on nullable property access (obj.field instanceof Subclass)");
		// Nullable field (Item?) with instanceof subclass check (Weapon)
		// The field should be narrowed to the subclass type in the true branch
		code_v4_("class Item { integer id } class Weapon extends Item { integer cost = 5 } class Move { Item? item = null } var m = new Move(); m.item = new Weapon(); if (m.item instanceof Weapon) { return m.item.cost } return 0").equals("5");
		code_v4_("class Item { integer id } class Weapon extends Item { integer cost = 5 } class Move { Item? item = null } var m = new Move(); m.item = new Weapon(); if (m.item instanceof Weapon) { return m.item.cost } return 0").noWarning();
		// Passing narrowed property to a function expecting the subclass type
		code_v4_("class Item { integer id } class Weapon extends Item { integer dmg = 10 } class Move { Item? item = null } function useWeapon(Weapon w) { return w.dmg } var m = new Move(); m.item = new Weapon(); if (m.item instanceof Weapon) { return useWeapon(m.item) } return 0").equals("10");
		code_v4_("class Item { integer id } class Weapon extends Item { integer dmg = 10 } class Move { Item? item = null } function useWeapon(Weapon w) { return w.dmg } var m = new Move(); m.item = new Weapon(); if (m.item instanceof Weapon) { return useWeapon(m.item) } return 0").noWarning();
	}

	@Test
	public void testAssignment_narrowing() throws Exception {
		section("Assignment narrowing: variable narrowed to assigned expression type");
		// After if (x == null) { x = nonNull }, x is non-null
		// Simple local variable
		code_v4_("integer | null x = null; if (x == null) { x = 5 } return abs(x)").noWarning();
		code_v4_("integer | null x = null; if (x == null) { x = 5 } return abs(x)").equals("5");
		// Class field with this.field syntax (user-reported pattern)
		code_v4_("class T { T? a = null; T a_() { if (this.a == null) { this.a = new T() } return this.a } } return new T().a_()").noWarning();
		// Class field without this prefix
		code_v4_("class T { T? a = null; T a_() { if (a == null) { a = new T() } return a } } return new T().a_()").noWarning();
		// Assignment narrows type: after x = nonNull, x is non-null
		code_v4_("integer | null x = null; x = 42; return abs(x)").noWarning();
		code_v4_("integer | null x = null; x = 42; return abs(x)").equals("42");
		// Assignment to null narrows type to null (stale narrowing reset)
		code_strict_v4_("integer | null w = 5; var r = 0; while (w != null) { r = abs(w); w = null } return r").max_ops(1000).noWarning();
		// Stale lastAssignedType must not cause false positive:
		// x = 42 before the if must not make if (x == null) { debug() } narrow x
		code_strict_v4_("function f(integer | null x) { x = 42; x = null; if (x == null) { debug(0) } return abs(x) }").warning(Error.DANGEROUS_CONVERSION);
		// Field with method call after assignment: must NOT narrow (side effects could nullify)
		code_strict_v4_("class T { T? a = null; reset() { a = null } T? a_() { if (a == null) { a = new T(); reset() } return a } }").noWarning();
		// Field with single instruction: must narrow
		code_v4_("class T { T? a = null; T a_() { if (a == null) { a = new T() } return a } } return new T().a_()").noWarning();
		// if (x != null) { x = nonNull } must NOT apply false narrowing (x → null)
		// This pattern differs from if (x == null) { x = nonNull } — here the false
		// branch means x was null, so applying it would incorrectly narrow x to null.
		code_v4_("integer | null x = null; if (x != null) { x = 42 } if (x == null) { x = 0 } return abs(x)").noWarning();
		code_v4_("integer | null x = null; if (x != null) { x = 42 } if (x == null) { x = 0 } return abs(x)").equals("0");
		code_v4_("integer | null x = 5; if (x != null) { x = 42 } if (x == null) { x = 0 } return abs(x)").equals("42");
	}

	@Test
	public void testNarrowing_truthy_property() throws Exception {
		section("Narrowing: truthy check on property (if (this.field))");
		// if (this.field) should narrow this.field from T? to T inside the block
		code_v4_("class T { integer v = 42 } class C { T? item = null; integer run() { item = new T(); if (this.item) { return this.item.v } return 0 } } return new C().run()").noWarning().equals("42");
		// with an and condition
		code_v4_("class T { integer v = 42 } class C { T? item = null; integer run() { item = new T(); if (this.item and 1) { return this.item.v } return 0 } } return new C().run()").noWarning().equals("42");
	}

	@Test
	public void testNarrowing_instanceof_property_as_function_argument() throws Exception {
		section("Narrowing: instanceof on property passed as function argument");
		// Property narrowed via instanceof should be castable when passed as argument
		code_v4_("class A {} class B extends A { integer v = 42 } class C { A? item } function f(B b) { return b.v } var c = new C(); c.item = new B(); if (c.item instanceof B) { return f(c.item) } return 0").equals("42");
	}

	@Test
	public void testNarrowing_function_argument_after_null_check_strict() throws Exception {
		section("Narrowing: function argument after null check (strict mode)");
		// Non-strict
		code_v4_("function f(integer x) { return x * 2 } integer | null id = 5; if (id == null) return 0; return f(id)").noWarning();
		// Strict mode
		code_strict_v4_("function f(integer x) { return x * 2 } integer | null id = 5; if (id == null) return 0; return f(id)").noWarning();
		// Strict: static method in class (FIELD_MAY_NOT_EXIST on .id is expected since fromXY returns Cell?)
		code_strict_v4_("class Cell { integer id; static Cell get(integer i) { var c = new Cell(); c.id = i; return c } static Cell? fromXY(integer x, integer y) { integer? id = x > 0 ? x + y : null; if (id == null) return null; return Cell.get(id) } } return Cell.fromXY(1, 2)!.id").noWarning();
		// Strict: var inference from nullable function
		code_strict_v4_("function getCellFromXY(integer x, integer y) => integer? { return x + y } class Cell { integer id; static Cell get(integer i) { var c = new Cell(); c.id = i; return c } static Cell? fromXY(integer x, integer y) { var id = getCellFromXY(x, y); if (id == null) return null; return Cell.get(id) } } return Cell.fromXY(1, 2)!.id").noWarning();
	}

	/**
	 * Edge cases du parcours de NarrowingInfo après la refonte lazy des 5 HashMaps
	 * internes. Vérifie que les chemins où certaines branches du `merge` ont des
	 * sources null/vides n'allouent rien et donnent le bon résultat.
	 */
	@Test
	public void testNarrowing_NoOpAndEmptyBranches() throws Exception {
		section("Narrowing: conditions without narrowing + empty merge branches");
		// Condition sans aucune narrowing : NarrowingInfo reste tout-null
		code_v4_("function getFoo() => boolean { return true } if (getFoo()) { return 1 } return 0").noWarning().equals("1");
		// AND avec une seule branche narrowante : l'autre branche skip dans mergeInto
		code_v4_("integer | null x = 5; function getFoo() => boolean { return true } if (x != null && getFoo()) { return abs(x) } return 0").noWarning().equals("5");
		code_v4_("integer | null x = 5; function getFoo() => boolean { return true } if (getFoo() && x != null) { return abs(x) } return 0").noWarning().equals("5");
		// OR avec une seule branche narrowante : pareil
		code_v4_("integer | null x = 5; integer | null y = null; if (x == null || y == null) { return 0 } return abs(x) + abs(y)").noWarning().equals("0");
		// NOT inversion d'une condition sans narrowing
		code_v4_("function getFoo() => boolean { return true } if (!getFoo()) { return 0 } return 1").noWarning().equals("1");
		// NOT inversion d'un null check : trueNarrowings ↔ falseNarrowings
		code_v4_("integer | null x = 5; if (!(x != null)) { return 0 } return abs(x)").noWarning().equals("5");
		// AND combinant deux null checks sur des variables différentes
		code_v4_("integer | null x = 5; integer | null y = 3; if (x != null && y != null) { return abs(x) + abs(y) } return 0").noWarning().equals("8");
		// Imbrication NOT(AND)
		code_v4_("integer | null x = 5; integer | null y = 3; if (!(x == null && y == null)) { return 1 } return 0").noWarning().equals("1");
	}

	/**
	 * Edge case du fast-path `applyParentFalseNarrowings` qui retourne null direct
	 * quand `mParentCondition` est null (cas hot des premiers if).
	 */
	@Test
	public void testNarrowing_FirstIfNoParentChain() throws Exception {
		section("Narrowing: first-if without parent chain (fast path)");
		// Premier if sans else-if : mParentCondition est null, applyParentFalseNarrowings → null
		code_v4_("integer | null x = 5; if (x != null) { return abs(x) } return 0").noWarning().equals("5");
		// Premier if isolé, plusieurs séquentiels (chacun avec mParentCondition = null)
		code_v4_("integer | null x = 5; if (x != null) { x = 10 } integer | null y = 3; if (y != null) { return abs(y) + abs(x!) } return 0").noWarning().equals("13");
		// if-then-without-else : pas de chain
		code_v4_("integer | null x = 5; if (x == null) { return 0 } return abs(x)").noWarning().equals("5");
	}

	/**
	 * Couvre la garde `!isEmpty()` ajoutée dans ConditionalBloc.analyze pour ne pas
	 * allouer un MapNIterator quand le narrowingInfo n'a aucune narrowing à clear.
	 * Vérifie que les `if` sans narrowing (cas dominant) restent corrects, et que
	 * les chains de else-if avec narrowing à un seul niveau propagent bien.
	 */
	@Test
	public void testNarrowing_EmptyIterGuard() throws Exception {
		section("Narrowing: empty narrowing maps (isEmpty guard)");
		// if avec condition non narrowable (appel de fonction, pas de == null / != null)
		// → narrowingInfo.getFalseNarrowings() retourne Map.of() vide, l'iter doit être skip
		code_v4_("function isOK() { return true } if (isOK()) { return 1 } return 0").equals("1");
		// if (arithmetic comparison non-null) → narrowingInfo a falseNarrowings vides
		code_v4_("var x = 10 if (x > 5) { return 1 } return 0").equals("1");
		// if-else-if chain où aucune branche ne narrow rien
		code_v4_("var x = 1 if (x == 0) { return 0 } else if (x > 100) { return 100 } else { return x }").equals("1");
		// if avec narrowing actif mélangé avec une assignation qui doit invalider lastAssignedType
		// (la garde isEmpty protège le clear ; on vérifie que ça reste correct quand non-empty)
		code_v4_("integer | null x = null; if (x == null) { x = 42 } return abs(x)").noWarning().equals("42");
	}


	/**
	 * Narrowing d'une propriété à droite d'un && / || et dans un ternaire, quand l'objet
	 * est précisé (`this.x`, `A.X`, `class.X`, `a.x`) (#5303). Le narrowing des propriétés
	 * ne passait que par les blocs (`if (this.x != null) { ... }`).
	 */
	@Test
	public void testProperty_narrowing_in_expressions() throws Exception {
		section("Property narrowing in && / || / ternary");
		code_strict_v4_("class A { string? str = \"ab\"; boolean m() { return this.str != null && startsWith(this.str, \"a\") } } return new A().m()").noWarning();
		code_v4_("class A { string? str = \"ab\"; boolean m() { return this.str != null && startsWith(this.str, \"a\") } } return new A().m()").equals("true");
		code_strict_v4_("class A { static string? STR = \"ab\"; static boolean m() { return A.STR != null && startsWith(A.STR, \"a\") } } return A.m()").noWarning();
		code_v4_("class A { static string? STR = \"ab\"; static boolean m() { return A.STR != null && startsWith(A.STR, \"a\") } } return A.m()").equals("true");
		code_strict_v4_("class A { static string? STR = \"ab\"; static boolean m() { return class.STR != null && startsWith(class.STR, \"a\") } } return A.m()").noWarning();
		code_v4_("class A { static string? STR = \"ab\"; static boolean m() { return class.STR != null && startsWith(class.STR, \"a\") } } return A.m()").equals("true");
		code_strict_v4_("class A { string? str = \"ab\" } var a = new A(); return a.str != null && startsWith(a.str, \"a\")").noWarning();
		code_v4_("class A { string? str = \"ab\" } var a = new A(); return a.str != null && startsWith(a.str, \"a\")").equals("true");
		code_strict_v4_("class A { string? str = \"ab\"; boolean m() { return this.str == null || startsWith(this.str, \"a\") } } return new A().m()").noWarning();
		code_v4_("class A { string? str = \"ab\"; boolean m() { return this.str == null || startsWith(this.str, \"a\") } } return new A().m()").equals("true");
		code_strict_v4_("class A { string? str = \"ab\"; integer m() { return this.str != null ? length(this.str) : 0 } } return new A().m()").noWarning();
		code_v4_("class A { string? str = \"ab\"; integer m() { return this.str != null ? length(this.str) : 0 } } return new A().m()").equals("2");
		code_strict_v4_("class A { static string? STR = null; static integer m() { return A.STR == null ? 0 : length(A.STR) } } return A.m()").noWarning();
		code_v4_("class A { static string? STR = null; static integer m() { return A.STR == null ? 0 : length(A.STR) } } return A.m()").equals("0");
		// Le narrowing ne fuit pas après l'expression
		code_strict_v4_("class A { string? str = null; void m() { var b = this.str != null && startsWith(this.str, \"a\"); startsWith(this.str, \"a\") } }").warning(Error.DANGEROUS_CONVERSION);
	}

	/**
	 * Un champ `integer | boolean | null` narrowé par sa véracité vaut `boolean | integer` :
	 * `x!` dessus ne doit pas le réduire à `boolean` (chemin rapide de
	 * `CompoundType.assertNotNull`, qui prenait toute union de deux types pour `T | null`).
	 * Cas de prod : `this.on ? [this.on! : true] : …` rendait `[true : true]`.
	 */
	@Test
	public void testProperty_narrowing_keeps_two_type_union() throws Exception {
		section("Property narrowing: non-null assertion on a two-type union");
		code_v4_("class A { integer | boolean | null on = 42; m() { return this.on ? [this.on! : true] : [:] } } return new A().m()").equals("[42 : true]");
		code_v4_("class A { integer | boolean | null on = 42; m() { return this.on ? this.on! : 0 } } return new A().m()").equals("42");
		code_v4_("integer | boolean x = 42; return [x! : true]").equals("[42 : true]");
	}

	/**
	 * Un champ `real?` narrowé (`a.x != null`) reste un Double dans le Java. Lu dans une branche
	 * de ternaire enveloppée par le ops(T, int) générique, il rendait ambigu le ops du ternaire
	 * entier : l'IA ne compilait plus (« reference to ops is ambiguous »). Idem pour un champ
	 * integer ou boolean.
	 */
	@Test
	public void testNarrowed_nullable_field_in_ternary_branch() throws Exception {
		section("Narrowed nullable field in a ternary branch");
		code_v4_("class A { real? s = 2.5; real m() { real r = this.s != null ? this.s! : 0.0; return r } } return new A().m()").equals("2.5");
		code_v4_("class A { real? s = 2.5; real m() { real r = this.s == null ? 0.0 : this.s!; return r } } return new A().m()").equals("2.5");
		code_v4_("class A { real? s = 2.5 } A a = new A(); real r = a.s != null ? a.s! : 0.0; return r").equals("2.5");
		code_v4_("class A { real? s = null } A a = new A(); real r = a.s != null ? a.s! : 0.0; return r").equals("0.0");
		code_v4_("class A { real? s = 2.5 } A a = new A(); real r = a.s != null ? a.s : 0.0; return r").equals("2.5");
		code_v4_("class A { real? s = 2.5 } A a = new A(); var r = a.s == null ? 0.0 : a.s!; return r").equals("2.5");
		code_v4_("class A { real? s = 2.5 } A a = new A(); real r = (a.s != null ? a.s! : 0.0) + 1; return r").equals("3.5");
		code_v4_("class A { integer? n = 3 } A a = new A(); integer r = a.n != null ? a.n! : 0; return r").equals("3");
		code_v4_("class A { boolean? b = true } A a = new A(); boolean r = a.b != null ? a.b! : false; return r").equals("true");
		code_strict_v4_("class A { real? s = 2.5 } A a = new A(); real r = a.s != null ? a.s! : 0.0; return r").equals("2.5");
		// Ternaire imbriqué dont les deux branches sont boxées : lui-même boxé
		code_v4_("class A { real? s = 2.5 } A a = new A(); boolean c = true; if (a.s != null) { real r = c ? (c ? a.s : a.s) : 1.0; return r } return 0").equals("2.5");
		code_v4_("class A { real? s = 2.5 } A a = new A(); A b = new A(); boolean c = false; real r = (a.s != null && b.s != null) ? (c ? a.s! : b.s!) : 1.0 + 2; return r").equals("2.5");
		// Hors ternaire, la lecture garde null si le champ est remis à null sous le narrowing
		code_v4_("class A { real? s = 2.5 } A a = new A(); if (a.s != null) { a.s = null; return a.s } return -1").equals("null");
	}

	/**
	 * `as` vers le type déjà narrowé, ou `??` après un narrowing non null, ne convertit rien : la
	 * branche reste boxée, comme avec `!` (cf testNarrowed_nullable_field_in_ternary_branch).
	 */
	@Test
	public void testNarrowed_nullable_field_as_or_coalesce_in_ternary_branch() throws Exception {
		section("Narrowed nullable field through `as` or `??` in a ternary branch");
		code_v4_("class A { integer? n = 3; integer c = 7; integer m(A o) { integer r = (this.n == null) ? o.c : this.n as integer; return r } } return new A().m(new A())").equals("3");
		code_v4_("class A { integer? n = 3 } A a = new A(); A b = new A(); integer r = (a.n == null) ? b.n! + 1 : a.n as integer; return r").equals("3");
		code_v4_("class A { real? s = 2.5 } A a = new A(); real r = a.s != null ? a.s as real : 0.0; return r").equals("2.5");
		code_v4_("class A { boolean? b = true } A a = new A(); boolean r = a.b != null ? a.b as boolean : false; return r").equals("true");
		code_strict_v4_("class A { integer? n = 3 } A a = new A(); A b = new A(); integer r = (a.n == null) ? b.n! + 1 : a.n as integer; return r").equals("3");
		// Deux branches boxées aux ops égaux : le ternaire est lui-même boxé
		code_v4_("class A { real? s = 2.5 } A a = new A(); boolean c = true; if (a.s != null) { real r = c ? (c ? a.s as real : a.s as real) : 1.0; return r } return 0").equals("2.5");
		code_v4_("class A { integer? n = 3 } A a = new A(); A b = new A(); integer r = (a.n == null) ? b.n! + 1 : (a.n ?? 0); return r").equals("3");
	}

	/**
	 * Deux champs `integer?` narrowés sont deux Long en Java : `==` comparait leurs références,
	 * donc faux au-delà du cache des Long (127).
	 */
	@Test
	public void testNarrowed_nullable_fields_equality() throws Exception {
		section("Equality of two narrowed nullable fields");
		code_v4_("class A { integer? n = 1000 } A a = new A(); A b = new A(); if (a.n != null && b.n != null) { return a.n == b.n } return null").equals("true");
		code_v4_("class A { integer? n = 1000; boolean same(A o) { return this.n != null && o.n != null && this.n == o.n } } return new A().same(new A())").equals("true");
		code_v4_("class A { integer? n } A a = new A(); a.n = 1000; A b = new A(); b.n = 1001; if (a.n != null && b.n != null) { return a.n == b.n } return null").equals("false");
		code_v4_("class A { integer? n = 1000 } A a = new A(); A b = new A(); boolean c = true; if (a.n != null && b.n != null) { return (c ? a.n : b.n) == (c ? b.n : a.n) } return null").equals("true");
		code_v4_("global integer? G = 1000; global integer? H = 1000; if (G != null && H != null) { return G == H } return null").equals("true");
		code_v4_("class A { integer? n = 1000 } A a = new A(); A b = new A(); if (a.n != null && b.n != null) { return (a.n as integer) == (b.n as integer) } return null").equals("true");
		code_v4_("class A { integer? n = 1000 } A a = new A(); A b = new A(); if (a.n != null && b.n != null) { return (a.n ?? 0) == (b.n ?? 0) } return null").equals("true");
	}

	/**
	 * Même cas qu'un champ `real?` pour une variable `boolean?` (writeNarrowed ne convertit
	 * qu'integer et real) et pour une globale `T?` (lue sans writeNarrowed) : lue telle quelle,
	 * donc boxée, dans une branche de ternaire comptée.
	 */
	@Test
	public void testNarrowed_nullable_variable_in_ternary_branch() throws Exception {
		section("Narrowed nullable boolean variable or global in a ternary branch");
		code_v4_("boolean? b = true; boolean r = b != null ? b! : false; return r").equals("true");
		code_v4_("boolean? b = null; boolean r = b != null ? b! : false; return r").equals("false");
		code_strict_v4_("boolean? b = true; boolean r = b != null ? b! : false; return r").equals("true");
		code_v4_("boolean? b = true; var f = function() { b = true }; boolean r = b != null ? b! : false; return r").equals("true");
		code_v4_("function g(boolean? b) { boolean r = b != null ? b! : false; return r } return g(true)").equals("true");
		code_v4_("class A { boolean? f = true; m() { boolean r = f != null ? f! : false; return r } } return new A().m()").equals("true");
		code_v4_("global boolean? G = true; boolean r = G != null ? G! : false; return r").equals("true");
		code_v4_("global real? G = 2.5; real r = G != null ? G! : 0.0; return r").equals("2.5");
		code_v4_("global integer? G = 1000; integer r = G == null ? 0 : G!; return r").equals("1000");
		code_v4_("boolean? b = true; boolean c = true; if (b != null) { boolean r = c ? (c ? b : b) : false; return r } return null").equals("true");
		// Hors ternaire, la lecture garde null si la variable est remise à null sous le narrowing
		code_v4_("global real? G = 2.5; function z() { G = null } if (G != null) { z(); return [G] } return null").equals("[null]");
	}

	/**
	 * Une affectation ou un incrément natif rend le type Java de son emplacement : un Long pour une
	 * variable ou un champ `integer?`, ou pour un itérateur typé. `==` y comparait deux Long par
	 * référence (faux au-delà de 127), et un ternaire comptant la branche ne compilait pas.
	 */
	@Test
	public void testAssignment_value_on_boxed_slot() throws Exception {
		section("Assignment value on a boxed slot");
		code_v4_("integer? x = 0; integer? y = 0; return (x = 1000) == (y = 1000)").equals("true");
		code_v4_("integer? x = 0; integer? y = 0; return (x = 1000) == (y = 1001)").equals("false");
		code_v4_("function f(integer? x, integer? y) { return (x = 1000) == (y = 1000) } return f(0, 0)").equals("true");
		code_v4_("global integer? G = 0; global integer? H = 0; return (G = 1000) == (H = 1000)").equals("true");
		code_v4_("class A { integer? f = 0; m(A o) { return (this.f = 1000) == (o.f = 1000) } } return new A().m(new A())").equals("true");
		code_v4_("class A { integer? f = 0; m() { return (f = 1000) == (f = 1000) } } return new A().m()").equals("true");
		code_v4_("integer? x = 999; integer? y = 999; if (x != null && y != null) { return ++x == ++y } return null").equals("true");
		code_v4_("integer? x = 999; integer? y = 999; if (x != null && y != null) { return (x += 1) == (y += 1) } return null").equals("true");
		code_v4_("for (integer a in [1000]) { for (integer b in [1000]) { return a++ == b++ } } return null").equals("true");
		// Incrément d'un champ : field_inc casté vers le type boîte
		code_v4_("class A { integer f = 1000 } A a = new A(); A b = new A(); return a.f++ == b.f++").equals("true");
		code_v4_("class A { integer f = 999; m(A o) { return ++this.f == ++o.f } } return new A().m(new A())").equals("true");
		code_v4_("boolean c = true; integer? x = 0; integer r = c ? (c ? (x = 5) : 1) : 0; return r").equals("5");
		code_v4_("boolean c = true; for (integer a in [1000]) { integer r = c ? (c ? a++ : a) : 0; return r } return null").equals("1000");
	}

	/**
	 * Une variable ou un champ any (ou union) narrowé vers un primitif par `instanceof` est lu tel
	 * quel, en Object : un boolean (writeNarrowed ne convertit qu'integer et real), une globale ou
	 * un champ. Tout consommateur primitif (`if (x)`, `!x`, `boolean b = x`, `a.f + 1`) ne
	 * compilait pas ; il le convertit désormais comme une valeur any.
	 */
	@Test
	public void testInstanceof_narrowing_on_object_slot() throws Exception {
		section("Instanceof narrowing on an Object slot");
		code_v4_("var x = true; if (x instanceof Boolean) { if (x) { return 1 } } return 0").equals("1");
		code_v4_("var x = true; if (x instanceof Boolean) { return !x } return 0").equals("false");
		code_v4_("integer | boolean x = true; if (x instanceof Boolean) { boolean b = x; return b } return 0").equals("true");
		code_v4_("var x = true; boolean r = x instanceof Boolean ? x : false; return r").equals("true");
		code_v4_("function g(x) { if (x instanceof Boolean) { return !x } return 0 } return g(true)").equals("false");
		code_v4_("var x = true; var f = function() { return x }; if (x instanceof Boolean) { return !x } return 0").equals("false");
		code_v4_("class A { any f = 5 } A a = new A(); if (a.f instanceof Integer) { return a.f + 1 } return null").equals("6");
		code_v4_("class A { any f = true; m() { if (this.f instanceof Boolean) { return !this.f } return 0 } } return new A().m()").equals("false");
		code_v4_("class A { any f = true; m() { if (f instanceof Boolean) { return !f } return 0 } } return new A().m()").equals("false");
		code_v4_("global G = 5; if (G instanceof Integer) { integer b = G; return b + 1 } return 0").equals("6");
		code_v4_("global G = 1000; global H = 1000; if (G instanceof Integer && H instanceof Integer) { return G == H } return 0").equals("true");
		// Argument d'une native : le choix de la surcharge se fait pendant l'analyse, sous narrowing
		code_v4_("class A { any f = -5 } A a = new A(); if (a.f instanceof Integer) { return abs(a.f) } return null").equals("5");
		code_v4_("var b = true; if (b instanceof Boolean) { return setBit(0, 1, b) } return null").equals("2");
		code_v4_("class A { any b = true; m() { if (b instanceof Boolean) { return setBit(0, 1, b) } return null } } return new A().m()").equals("2");
		// La lecture n'est pas convertie : null reste null si la valeur est remise à null sous le narrowing
		code_v4_("class A { any f = 5 } A a = new A(); if (a.f instanceof Integer) { a.f = null; return a.f } return -1").equals("null");
		code_v4_("global G = 5; function z() { G = null } if (G instanceof Integer) { z(); return [G] } return -1").equals("[null]");
	}

	/**
	 * Une écriture à travers `!` (`x! += 1`, `t[k]! -= 1`, `++o.f!`) écrit dans la cible :
	 * seul `=` compilait, les assignations composées et `++x!` faisaient échouer la
	 * compilation de l'IA en combat (« Abstract method »).
	 */
	@Test
	public void testNon_null_assertion_as_assignment_target() throws Exception {
		section("Non-null assertion as assignment target");
		code_strict_v4_("Map<integer, integer> m = new Map() as Map<integer, integer>; integer key = 666; m[key]! = 1; return m").equals("[666 : 1]");
		code_v4_("Map<integer, Array<integer>> m = new Map() as Map<integer, Array<integer>>; m[666]! = arraySort([3, 1, 2], function (integer a, integer b) => integer { return a - b }) as Array<integer>; return m").equals("[666 : [1, 2, 3]]");
		code_v4_("integer | null x = 5; x! += 1; return x").equals("6");
		code_v4_("integer | null x = 5; x! -= 1; return x").equals("4");
		code_v4_("integer | null x = 5; x! *= 2; return x").equals("10");
		code_v4_("integer | null x = 5; x! ??= 2; return x").equals("5");
		code_v4_("integer | null x = 5; integer y = (x! += 1); return y").equals("6");
		code_v4_("integer | null x = 5; integer y = (x! = 3); return y").equals("3");
		code_v4_("integer | null x = 5; ++x!; return x").equals("6");
		code_v4_("integer | null x = 5; --x!; x!--; return x").equals("3");
		code_v4_("integer | null x = 5; return x!++").equals("5");
		code_v4_("Map<integer, integer> m = [1: 2]; m[1]! += 1; return m").equals("[1 : 3]");
		code_v4_("Map<integer, integer> m = [1: 2]; integer y = (m[1]! += 1); return y").equals("3");
		code_v4_("Array<integer> a = [1, 2]; a[0]! += 5; return a").equals("[6, 2]");
		code_v4_("var a = [1, 2]; a[0]! += 5; return a").equals("[6, 2]");
		code_v4_("class A { integer | null x = 1 } A a = new A(); a.x! += 3; return a.x").equals("4");
		code_v4_("class A { integer | null x = 1 } var a = new A(); a.x! += 3; return a.x").equals("4");
		code_strict_v4_("integer | null x = 5; x! += 1; return x").equals("6");
		code_strict_v4_("Map<integer, integer> m = [1: 2]; m[1]! += 1; return m").equals("[1 : 3]");
		// Mêmes avertissements et erreurs qu'avant
		code_v4_("integer | null x = 5; x! += 1; return x").noWarning();
		code_v4_("integer x = 5; x! += 3; return x").warning(Error.USELESS_NON_NULL_ASSERTION);
		code_v4_("function f() { return 1 } f()! += 1; return 0").compileError(Error.CANT_ASSIGN_VALUE);
		code_v4_("function f() { return 1 } f()! = 1; return 0").compileError(Error.CANT_ASSIGN_VALUE);
		code_v4_("function f() { return 1 } f! += 1; return 0").compileError(Error.CANNOT_REDEFINE_FUNCTION);
		code_v4_("function f() { return 1 } f! = 1; return 0").compileError(Error.CANNOT_REDEFINE_FUNCTION);
		code_v4_("abs! = 1; return 0").compileError(Error.CANNOT_REDEFINE_FUNCTION);
		code_v4_("function f() { return 1 } f!++; return 0").compileError(Error.CANNOT_REDEFINE_FUNCTION);

		// `f!()` appelle la valeur assertée non-null : plus d'avertissement « peut ne pas être appelable »
		code_strict_v4_("function g() { return 1 } Function | null renamed = g; return renamed!()").noWarning();
		code_strict_v4_("Map<integer, Function> a = [0: function () { return 1 }]; return a[0]!()").noWarning();
		code_strict_v4_("Map<integer, Function> a = [0: function () { return 1 }]; return a[0]!()").equals("1");
		code_strict_v4_("Map<integer, Function> a = [0: function () { return 1 }]; return a[0]()").warning(Error.MAY_NOT_BE_CALLABLE);
	}
}
