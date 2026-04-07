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

}
