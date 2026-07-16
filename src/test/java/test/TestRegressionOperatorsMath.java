package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

/**
 * Tests de caractérisation du comportement LS4 des opérateurs et des fonctions
 * Math, conçus pour DÉTECTER toute régression introduite par la feature
 * big_integer. Volontairement centrés sur les opérandes/arguments de type `any`
 * (`[v, "x"][0]` force le type any), car c'est exactement là que passent les
 * helpers runtime (add/sub/mul/eq/less/minus/real/longint) et le dispatcher
 * générique des fonctions système que big_integer a modifiés. Les opérations
 * typées (int+int) compilent en Java direct et ne sont pas affectées.
 *
 * Ce fichier doit passer À L'IDENTIQUE sur master (sans big_integer) et sur la
 * branche big_integer.
 */
@ExtendWith(SummaryExtension.class)
public class TestRegressionOperatorsMath extends TestCommon {

	@Test
	public void testInit() throws Exception {
		header("Régression opérateurs & Math (any-typed)");
	}

	// Helpers : `any(v)` = un v de type any ; opère via les helpers runtime.
	private static String I(long v) { return "[" + v + ", \"x\"][0]"; }
	private static String R(String v) { return "[" + v + ", \"x\"][0]"; }

	@Test
	public void testArithmeticAny() throws Exception {
		section("Arithmétique sur any (helpers runtime add/sub/mul/mod/pow)");
		// addition
		code_v4_("var a = " + I(5) + " var b = " + I(3) + " return a + b").equals("8");
		code_v4_("var a = " + I(5) + " var b = " + R("2.5") + " return a + b").equals("7.5");
		code_v4_("var a = " + R("1.5") + " var b = " + R("2.5") + " return a + b").equals("4.0");
		code_v4_("var a = " + I(-5) + " var b = " + I(3) + " return a + b").equals("-2");
		// soustraction
		code_v4_("var a = " + I(10) + " var b = " + I(3) + " return a - b").equals("7");
		code_v4_("var a = " + R("1.5") + " var b = " + I(1) + " return a - b").almost(0.5);
		// multiplication
		code_v4_("var a = " + I(6) + " var b = " + I(7) + " return a * b").equals("42");
		code_v4_("var a = " + R("2.5") + " var b = " + I(4) + " return a * b").equals("10.0");
		// division flottante
		code_v4_("var a = " + I(7) + " var b = " + I(2) + " return a / b").equals("3.5");
		code_v4_("var a = " + I(6) + " var b = " + I(3) + " return a / b").equals("2.0");
		// division entière
		code_v4_("var a = " + I(7) + " var b = " + I(2) + " return a \\ b").equals("3");
		code_v4_("var a = " + I(-7) + " var b = " + I(2) + " return a \\ b").equals("-3");
		// modulo
		code_v4_("var a = " + I(17) + " var b = " + I(5) + " return a % b").equals("2");
		code_v4_("var a = " + I(-7) + " var b = " + I(3) + " return a % b").equals("-1");
		// puissance
		code_v4_("var a = " + I(2) + " var b = " + I(10) + " return a ** b").equals("1024");
		code_v4_("var a = " + I(0) + " var b = " + I(0) + " return a ** b").equals("1");
		// négation
		code_v4_("var a = " + I(5) + " return -a").equals("-5");
		code_v4_("var a = " + R("2.5") + " return -a").equals("-2.5");
	}

	@Test
	public void testComparisonsAny() throws Exception {
		section("Comparaisons sur any (less/more/lessequals/moreequals/eq)");
		code_v4_("var a = " + I(2) + " var b = " + I(5) + " return a < b").equals("true");
		code_v4_("var a = " + I(5) + " var b = " + I(2) + " return a < b").equals("false");
		code_v4_("var a = " + I(5) + " var b = " + I(2) + " return a > b").equals("true");
		code_v4_("var a = " + I(2) + " var b = " + I(2) + " return a >= b").equals("true");
		code_v4_("var a = " + I(2) + " var b = " + I(2) + " return a <= b").equals("true");
		code_v4_("var a = " + I(3) + " var b = " + I(2) + " return a <= b").equals("false");
		code_v4_("var a = " + R("2.5") + " var b = " + I(2) + " return a > b").equals("true");
		code_v4_("var a = " + I(5) + " var b = " + I(5) + " return a == b").equals("true");
		code_v4_("var a = " + I(5) + " var b = " + R("5.0") + " return a == b").equals("true");
		code_v4_("var a = " + I(5) + " var b = " + I(6) + " return a != b").equals("true");
		// Long > 2^53 : précision exacte (le fast-path Long==Long et la comparaison
		// doivent rester exacts)
		code_v4_("var a = " + I(9007199254740993L) + " var b = " + I(9007199254740992L) + " return a > b").equals("true");
	}

	@Test
	public void testBitwiseAny() throws Exception {
		section("Opérations binaires sur any (band/bor/bxor/bnot/shl/shr/ushr/intdiv)");
		code_v4_("var a = " + I(12) + " var b = " + I(10) + " return a & b").equals("8");
		code_v4_("var a = " + I(12) + " var b = " + I(1) + " return a | b").equals("13");
		code_v4_("var a = " + I(12) + " var b = " + I(10) + " return a ^ b").equals("6");
		code_v4_("var a = " + I(5) + " return ~a").equals("-6");
		code_v4_("var a = " + I(1) + " var b = " + I(10) + " return a << b").equals("1024");
		code_v4_("var a = " + I(1024) + " var b = " + I(2) + " return a >> b").equals("256");
		code_v4_("var a = " + I(-1) + " var b = " + I(60) + " return a >>> b").equals("15");
	}

	@Test
	public void testCompoundAssign() throws Exception {
		section("Assignations composées (typées)");
		code_v4_("var a = 10 a += 4 return a").equals("14");
		code_v4_("var a = 10 a -= 4 return a").equals("6");
		code_v4_("var a = 10 a *= 4 return a").equals("40");
		code_v4_("var a = 10 a /= 4 return a").equals("2.5");
		code_v4_("var a = 10 a \\= 3 return a").equals("3");
		code_v4_("var a = 17 a %= 5 return a").equals("2");
		code_v4_("var a = 2 a **= 10 return a").equals("1024");
		code_v4_("var a = 12 a &= 10 return a").equals("8");
		code_v4_("var a = 12 a |= 1 return a").equals("13");
		code_v4_("var a = 12 a ^= 10 return a").equals("6");
		code_v4_("var a = 1 a <<= 10 return a").equals("1024");
		code_v4_("var a = 1024 a >>= 2 return a").equals("256");
	}

	@Test
	public void testMathFunctionsTyped() throws Exception {
		section("Fonctions Math (arguments typés)");
		code_v4_("return abs(-5)").equals("5");
		code_v4_("return abs(-5.5)").equals("5.5");
		code_v4_("return min(3, 7)").equals("3");
		code_v4_("return max(3, 7)").equals("7");
		code_v4_("return min(3.5, 2.0)").equals("2.0");
		code_v4_("return max(3.5, 2.0)").equals("3.5");
		code_v4_("return pow(2, 3)").equals("8.0");
		code_v4_("return floor(3.7)").equals("3");
		code_v4_("return ceil(3.2)").equals("4");
		code_v4_("return round(3.5)").equals("4");
		code_v4_("return signum(-3)").equals("-1");
		code_v4_("return binString(5)").equals("\"101\"");
		code_v4_("return hexString(255)").equals("\"ff\"");
		code("return sqrt(16.0)").almost(4.0);
		code("return cbrt(27.0)").almost(3.0);
		code("return cos(0.0)").almost(1.0);
		code("return sin(0.0)").almost(0.0);
		code("return exp(0.0)").almost(1.0);
		code("return log(1.0)").almost(0.0);
		code("return hypot(3.0, 4.0)").almost(5.0);
		code("return atan2(0.0, 1.0)").almost(0.0);
	}

	@Test
	public void testMathFunctionsAny() throws Exception {
		section("Fonctions Math (arguments any — dispatcher générique)");
		// Les fonctions dont big_integer a ajouté une version (abs/min/max/pow/
		// binString/hexString) : vérifier que le dispatcher choisit toujours la
		// bonne version integer/real pour des valeurs integer/real.
		code_v4_("var a = " + I(-5) + " return abs(a)").equals("5");
		code_v4_("var a = " + R("-5.5") + " return abs(a)").equals("5.5");
		code_v4_("var a = " + I(3) + " var b = " + I(7) + " return min(a, b)").equals("3");
		code_v4_("var a = " + I(3) + " var b = " + I(7) + " return max(a, b)").equals("7");
		code_v4_("var a = " + I(3) + " var b = " + R("7.5") + " return max(a, b)").equals("7.5");
		code_v4_("var a = " + R("2.5") + " var b = " + I(1) + " return min(a, b)").equals("1.0");
		code_v4_("var a = " + I(2) + " var b = " + I(10) + " return pow(a, b)").equals("1024.0");
		code_v4_("var a = " + I(255) + " return hexString(a)").equals("\"ff\"");
		code_v4_("var a = " + I(5) + " return binString(a)").equals("\"101\"");
		// Autres fonctions multi-versions (non touchées mais passant par le même
		// dispatcher généralisé) : régression possible si le dispatcher était cassé.
		code_v4_("var a = " + R("3.7") + " return floor(a)").equals("3");
		code_v4_("var a = " + R("3.2") + " return ceil(a)").equals("4");
		code_v4_("var a = " + R("3.5") + " return round(a)").equals("4");
		code_v4_("var s = [\"abc\", 1][0] return substring(s, 1)").equals("\"bc\"");
		code_v4_("var s = [\"hello\", 1][0] return indexOf(s, \"l\")").equals("2");
		code_v4_("var x = [[3, 1, 2], 0][0] return arrayMax(x)").equals("3");
		code_v4_("var x = [5, \"x\"][0] var arr = [3, 1, 5, 2] return inArray(arr, x)").equals("true");
	}

	@Test
	public void testInstanceofNumbers() throws Exception {
		section("instanceof sur nombres (classOf)");
		code_v4_("var a = " + I(5) + " return a instanceof Integer").equals("true");
		code_v4_("var a = " + I(5) + " return a instanceof Number").equals("true");
		code_v4_("var a = " + R("5.5") + " return a instanceof Real").equals("true");
		code_v4_("var a = " + R("5.5") + " return a instanceof Number").equals("true");
		code_v4_("var a = " + R("5.5") + " return a instanceof Integer").equals("false");
		code_v4_("var a = " + I(5) + " return a instanceof Real").equals("true");
		code_v4_("var a = [\"x\", 1][0] return a instanceof String").equals("true");
	}

	/**
	 * forum #11415 / issue #2744 — affectation composée sur une variable integer
	 * avec un opérande boxé (ternaire int/real, any) : le cast Java primitif du
	 * résultat de add()/mul()/... déboxait au mauvais type (ClassCastException
	 * Double → Long) et crashait l'IA. Corrigé via longint()/real().
	 */
	@Test
	public void testCompoundAssignBoxedOperand() throws Exception {
		section("Affectation composée avec opérande boxé (ternaire int/real, any)");
		code_v2_("integer a = 10 a *= (false ? 1 : 0.5) return a").equals("5");
		code_v2_("integer a = 10 a *= (true ? 1 : 0.5) return a").equals("10");
		code_v2_("integer a = 10 boolean b = false a *= (b ? 1 : 0.5) return a").equals("5");
		code_v2_("integer a = 10 a += (false ? 1 : 0.5) return a").equals("10");
		code_v2_("integer a = 10 a -= (false ? 1 : 0.5) return a").equals("9");
		code_v2_("integer a = 10 a /= (false ? 1 : 0.5) return a").equals("20");
		code_v4("integer a = 10 any b = 0.5 a *= b return a").equals("5");
		code_v4("real a = 10.0 a *= (false ? 1 : 0.5) return a").equals("5.0");
		code_v4("global integer g = 10 g *= (false ? 1 : 0.5) return g").equals("5");
		code_v4("integer a = 10 a %= (false ? 3 : 4.0) return a").equals("2");
		code_v4("integer a = 10 a **= (false ? 1 : 2.0) return a").equals("100");
	}
}
