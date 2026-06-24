package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestOverloading extends TestCommon {

	@Test
	public void testOverloading() throws Exception {
		section("Function overloading (#4243)");
		// Surcharge par arité
		code_v5("function f() { return 0 } function f(x) { return x } return f() + f(5)").equals("5");
		code_v5("function g(a) { return a } function g(a, b) { return a + b } return g(3) * 10 + g(1, 2)").equals("33");
		// Surcharge par type : le bon CORPS est exécuté
		code_v5("function area(integer x) { return x * 2 } function area(string x) { return x + \"!\" } return area(5)").equals("10");
		code_v5("function area(integer x) { return x * 2 } function area(string x) { return x + \"!\" } return area(\"hi\")").equals("\"hi!\"");
		// Dispatch : la surcharge la plus précise gagne sur la version any
		code_v5("function d(x) { return \"any\" } function d(integer x) { return \"int\" } return d(5)").equals("\"int\"");
		code_v5("function d(x) { return \"any\" } function d(integer x) { return \"int\" } return d(\"s\")").equals("\"any\"");
		// Trois surcharges
		code_v5("function t(integer x) { return 1 } function t(string x) { return 2 } function t(real x) { return 3 } return t(1) * 100 + t(\"a\") * 10 + t(1.5)").equals("123");
		// Ambiguïté : deux surcharges à distance égale
		code_v5("function k(integer a, real b) { return 1 } function k(real a, integer b) { return 2 } return k(1, 2)").error(Error.AMBIGUOUS_OVERLOAD);
		// v1-4 : un nom de fonction dupliqué reste interdit (byte-identique)
		code_v2_4("function f() { return 1 } function f() { return 2 } return f()").error(Error.FUNCTION_NAME_UNAVAILABLE);
		code_v2_4("function f(x) { return 1 } function f(x, y) { return 2 } return f(1)").error(Error.FUNCTION_NAME_UNAVAILABLE);
		// En v5, un conflit avec une globale reste interdit
		code_v5("global f = 5 function f() { return 1 } return f").error(Error.FUNCTION_NAME_UNAVAILABLE);
		// Fonction simple non surchargée : inchangée
		code_v5("function single(x) { return x + 1 } return single(41)").equals("42");
	}
}
