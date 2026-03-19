package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import leekscript.common.Error;


@ExtendWith(SummaryExtension.class)
public class TestNarrowing extends TestCommon {

	@Test
	public void run() throws Exception {

		header("Type Narrowing");

		section("Basic null check: if (x != null)");
		// With narrowing: abs(x) should not warn inside if (x != null)
		code_v4_("integer | null x = 5; if (x != null) { return abs(x) } return 0").noWarning();
		// Functional correctness
		code_v4_("integer | null x = 5; if (x != null) { return abs(x) } return 0").equals("5");
		code_v4_("integer | null x = -3; if (x != null) { return abs(x) } return 0").equals("3");
		code_v4_("integer | null x = null; if (x != null) { return abs(x) } return 0").equals("0");

		section("Equality null check: if (x == null)");
		// x == null: in else branch, x is non-null
		code_v4_("integer | null x = 5; if (x == null) { return 0 } else { return abs(x) }").noWarning();
		code_v4_("integer | null x = 5; if (x == null) { return 0 } else { return abs(x) }").equals("5");

		section("Truthy check: if (x)");
		// Truthy check narrows nullable to non-null in true branch
		code_v4_("integer | null x = 5; if (x) { return abs(x) } return 0").noWarning();
		code_v4_("integer | null x = 5; if (x) { return abs(x) } return 0").equals("5");
		code_v4_("integer | null x = null; if (x) { return abs(x) } return 0").equals("0");

		section("Case 1: && / || operators");
		// && combines narrowings
		code_v4_("integer | null x = 5; integer | null y = 3; if (x != null && y != null) { return abs(x) + abs(y) } return 0").noWarning();
		code_v4_("integer | null x = 5; integer | null y = 3; if (x != null && y != null) { return abs(x) + abs(y) } return 0").equals("8");
		// || combines false narrowings
		code_v4_("integer | null x = null; integer | null y = null; if (x == null || y == null) { return 0 }").equals("0");
		// Short-circuit: x != null && abs(x) → x is narrowed in the right operand
		code_v4_("integer | null x = 5; if (x != null && abs(x) > 0) { return 1 } return 0").noWarning();
		code_v4_("integer | null x = 5; if (x != null && abs(x) > 0) { return 1 } return 0").equals("1");

		section("Case 2: Early return narrowing");
		// if (x == null) return → x is non-null after
		code_v4_("integer | null x = 5; if (x == null) return 0; return abs(x)").noWarning();
		code_v4_("integer | null x = 5; if (x == null) return 0; return abs(x)").equals("5");
		code_v4_("integer | null x = null; if (x == null) return 0; return abs(x)").equals("0");
		// if (x != null) { ... } with no else doesn't narrow after
		// (only early return narrows)

		section("Case 3: While loop narrowing");
		// while (x != null) → x is non-null inside loop
		code_v4_("integer | null x = 5; var r = 0; while (x != null) { r = abs(x); x = null } return r").max_ops(1000).noWarning();
		code_v4_("integer | null x = 5; var r = 0; while (x != null) { r = abs(x); x = null } return r").max_ops(1000).equals("5");
		// After reassignment (w = null), narrowing is reset → abs(w) should warn in strict mode
		code_strict_v4_("integer | null w = 5; var r = 0; while (w != null) { r = abs(w); w = null; abs(w) } return r").max_ops(1000).warning(Error.DANGEROUS_CONVERSION);

		section("Case 4: instanceof narrowing");
		// instanceof narrows the type (class types like Array)
		code_v4_("Array | string x = [1, 2]; if (x instanceof Array) { return count(x) } return 0").noWarning();
		code_v4_("Array | string x = [1, 2]; if (x instanceof Array) { return count(x) } return 0").equals("2");

		section("Case 5: Assignment in condition");
		// (x = getValue()) != null → x is narrowed in the if body
		code_v4_("function f() => integer | null { return 5 } integer | null x = null; if ((x = f()) != null) { return abs(x) } return 0").noWarning();
		code_v4_("function f() => integer | null { return 5 } integer | null x = null; if ((x = f()) != null) { return abs(x) } return 0").equals("5");

		section("Negation: !(x == null)");
		code_v4_("integer | null x = 5; if (!(x == null)) { return abs(x) } return 0").noWarning();
		code_v4_("integer | null x = 5; if (!(x == null)) { return abs(x) } return 0").equals("5");

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

		section("Else-if chains");
		code_v4_("integer | null x = 5; integer | null y = 3; if (x == null) { return 0 } else if (y == null) { return abs(x) } else { return abs(x) + abs(y) }").noWarning();
		code_v4_("integer | null x = 5; integer | null y = 3; if (x == null) { return 0 } else if (y == null) { return abs(x) } else { return abs(x) + abs(y) }").equals("8");

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

		section("Class field narrowing");
		// Assignment inside narrowed branch should use declared type, not narrowed type
		code_v4_("class A { integer | null x = null; m() { if (x == null) { x = 12 } } } var a = new A(); a.m(); return a.x").noWarning();
		code_v4_("class A { integer | null x = null; m() { if (x == null) { x = 12 } } } var a = new A(); a.m(); return a.x").equals("12");
		// Static field: basic assignment (no narrowing) — should work without warning
		code_v4_("class A { static integer | null x = null; m() { class.x = 12 } } var a = new A(); a.m(); return A.x").noWarning();
		// Static field: assignment inside narrowed branch
		code_v4_("class A { static integer | null x = null; m() { if (class.x == null) { class.x = 12 } } } var a = new A(); a.m(); return A.x").noWarning();
		code_v4_("class A { static integer | null x = null; m() { if (class.x == null) { class.x = 12 } } } var a = new A(); a.m(); return A.x").equals("12");

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
}
