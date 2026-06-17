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
	public void onlyInV5() throws Exception {
		section("Methods / version gate");
		// In v1-4 the method form does not resolve (the value-method OO library is a
		// version-5 feature): the call is a silent no-op, the array is unchanged.
		code_v4("var a = [3, 1, 2] a.sort() return a").equals("[3, 1, 2]");
	}
}
