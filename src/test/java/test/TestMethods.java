package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

/**
 * Object-oriented standard library (version 5): calling standard-library functions
 * as methods on the value, `array.sort()` instead of `sort(array)`.
 */
@ExtendWith(SummaryExtension.class)
public class TestMethods extends TestCommon {

	@Test
	public void arrayMethodsVarReceiver() throws Exception {
		section("Methods / array (var receiver, runtime dispatch)");
		// `var` gives the receiver a static type of `any`, so the method is resolved
		// at runtime. In-place mutators must mutate the actual receiver.
		code_v5("var a = [3, 1, 2] a.sort() return a").equals("[1, 2, 3]");
		code_v5("var a = [1, 2] a.push(3) return a").equals("[1, 2, 3]");
		code_v5("var a = [3, 1, 2] a.reverse() return a").equals("[2, 1, 3]");
		code_v5("var a = [1, 2, 3] var x = a.pop() return [a, x]").equals("[[1, 2], 3]");
		// Read-only methods.
		code_v5("var a = [11, 22, 33] return a.count()").equals("3");
		code_v5("var a = [1, 2, 3] return a.sum()").equals("6.0");
	}

	@Test
	public void arrayMethodsTypedReceiver() throws Exception {
		section("Methods / array (typed receiver, static dispatch)");
		// A precise static type resolves the method at compile time.
		code_v5("Array<integer> a = [3, 1, 2] a.sort() return a").equals("[1, 2, 3]");
		code_v5("Array<integer> a = [1, 2] a.push(3) return a").equals("[1, 2, 3]");
		code_strict_v5_("Array<integer> a = [11, 22, 33] integer n = a.count() return n").noWarning();
		// On a literal receiver.
		code_v5("return [5, 1, 9].count()").equals("3");
		code_v5("return [3, 1, 2].sum()").equals("6.0");
	}

	@Test
	public void cleanMethodNames() throws Exception {
		section("Methods / clean names (arrayMap -> map, etc.)");
		// The `arrayXxx` library functions are exposed under clean method names in v5.
		code_v5("return [1, 2, 3].map(x -> x * 2)").equals("[2, 4, 6]");
		code_v5("var a = [1, 2, 3] return a.map(x -> x * 2)").equals("[2, 4, 6]");
		code_v5("Array<integer> a = [1, 2, 3] return a.map(x -> x * 2)").equals("[2, 4, 6]");
		code_v5("return [1, 2, 3, 4].filter(x -> x > 2)").equals("[3, 4]");
		code_v5("return [3, 1, 4, 1, 5].max()").equals("5");
		code_v5("return [3, 1, 4, 1, 5].min()").equals("1");
		code_v5("return [1, 1, 2, 3, 3].unique()").equals("[1, 2, 3]");
		code_v5("return [[1, 2], [3, 4]].flatten()").equals("[1, 2, 3, 4]");
		code_v5("return [1, 2, 3].concat([4, 5])").equals("[1, 2, 3, 4, 5]");
		// Inferred return type of the generic method is usable.
		code_strict_v5_("Array<integer> a = [1, 2, 3] integer m = a.max() return m").noWarning();
	}

	@Test
	public void mapMethods() throws Exception {
		section("Methods / map");
		code_v5("var m = [1: 10, 2: 20] return m.size()").equals("2");
		code_v5("var m = [1: 10, 2: 20] return m.keys()").equals("[1, 2]");
		code_v5("var m = [1: 10, 2: 20] return m.values()").equals("[10, 20]");
		code_v5("var m = [1: 10, 2: 20] return m.contains(10)").equals("true");
		code_v5("var m = [1: 10, 2: 20] return m.containsKey(2)").equals("true");
		code_v5("Map<integer, integer> m = [1: 10, 2: 20] return m.size()").equals("2"); // typed
		// mapMap callback is (value, key, map).
		code_v5("var m = [1: 10, 2: 20] return m.map((value, key) -> value * 2)").equals("[1 : 20, 2 : 40]");
	}

	@Test
	public void setMethods() throws Exception {
		section("Methods / set");
		code_v5("var s = <1, 2, 3> return s.size()").equals("3");
		code_v5("var s = <1, 2, 3> return s.contains(2)").equals("true");
		code_v5("var s = <1, 2, 3> return s.toArray()").equals("[1, 2, 3]");
		code_v5("var s = <1, 2> return s.union(<2, 3>)").equals("<1, 2, 3>");
		code_v5("var s = <1, 2, 3, 4> return s.filter(x -> x > 2)").equals("<3, 4>");
	}

	@Test
	public void stringMethods() throws Exception {
		section("Methods / string (static helper shape)");
		// String methods live in the static StringClass helper, not as instance methods
		// on the Java String — the codegen branches on isStatic.
		code_v5("return \"hello\".length()").equals("5");
		code_v5("var s = \"hello\" return s.length()").equals("5");
		code_v5("string s = \"hello\" return s.length()").equals("5");
		code_v5("return \"hello\".charAt(1)").equals("\"e\"");
		code_v5("return \"hello world\".split(\" \")").equals("[\"hello\", \"world\"]");
		code_v5("return \"Hello\".toUpper()").equals("\"HELLO\"");
		code_v5("return \"hello\".contains(\"ell\")").equals("true");
		code_v5("return \"  hi  \".trim()").equals("\"hi\"");
		// substring(index, length).
		code_v5("return \"hello\".substring(1, 3)").equals("\"ell\"");
		code_v5("return \"hello\".replace(\"l\", \"L\")").equals("\"heLLo\"");
		code_strict_v5_("string s = \"abc\" integer n = s.length() return n").noWarning();
	}

	@Test
	public void numberMethods() throws Exception {
		section("Methods / number (static helper, overload selection)");
		// Number methods are static NumberClass helpers with int/real/big_integer
		// overloads; the runtime dispatch selects the overload matching the actual type
		// (reflection does no widening). Literal receivers (5.abs()) are avoided: the
		// lexer would read `5.` as a real, so a typed/var receiver is used.
		code_v5("integer x = -5 return x.abs()").equals("5");
		code_v5("var x = -5 return x.abs()").equals("5");
		code_v5("real x = 9.0 return x.sqrt()").equals("3.0");
		code_v5("var x = 9.0 return x.sqrt()").equals("3.0");
		code_v5("integer x = 9 return x.sqrt()").equals("3.0");
		code_v5("var x = 9 return x.sqrt()").equals("3.0");
		code_v5("integer x = 5 return x.min(3)").equals("3");
		code_v5("var x = 7 return x.max(10)").equals("10");
		code_v5("real x = 2.5 return x.floor()").equals("2");
		code_strict_v5_("integer x = -5 integer a = x.abs() return a").noWarning();
	}

	@Test
	public void onlyInV5() throws Exception {
		section("Methods / version gate");
		// In v1-4 the method form does not resolve (the value-method OO library is a
		// version-5 feature): the call is a silent no-op, the array is unchanged.
		code_v4("var a = [3, 1, 2] a.sort() return a").equals("[3, 1, 2]");
		// The clean name is method-only: it does not become a free global function.
		code_v5("return map([1, 2, 3], x -> x * 2)").error(leekscript.common.Error.UNKNOWN_VARIABLE_OR_FUNCTION);
	}
}
