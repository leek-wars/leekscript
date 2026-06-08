package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

/**
 * Tests des entiers de taille arbitraire `big_integer` (#bigint).
 * Conçu initialement par Batary (bat_jarry@hotmail.fr).
 *
 * M1 (tranche verticale) : type, littéraux `L`, déclaration/affectation,
 * conversions, string(), égalité. L'arithmétique et les opérations binaires
 * sont testées dans les milestones suivants.
 */
@ExtendWith(SummaryExtension.class)
public class TestBigInt extends TestCommon {

	@Test
	public void testInit() throws Exception {
		header("Big Integers");
	}

	@Test
	public void testLiterals() throws Exception {
		section("Littéraux L");
		code_v4_("return 5L").equals("5");
		code_v4_("return 0L").equals("0");
		// Hexadécimal et binaire
		code_v4_("return 0xFFL").equals("255");
		code_v4_("return 0b101L").equals("5");
		// Séparateurs
		code_v4_("return 1_000_000L").equals("1000000");
		// Au-delà des 64 bits d'un long
		code_v4_("return 1000000000000000000L").equals("1000000000000000000");
		code_v4_("return 100000000000000000000000L == 100000000000000000000000L").equals("true");
		// Très grand nombre : affichage tronqué (début ... fin) pour éviter une
		// conversion en chaîne énorme
		code_v4_("return string(123456789012345678901234567890L)").equals("\"1234567890...1234567890\"");
	}

	@Test
	public void testDeclaration() throws Exception {
		section("Déclaration / affectation");
		code_v4_("big_integer a = 5L return a").equals("5");
		// Inférence depuis un littéral L
		code_v4_("var a = 5L return a").equals("5");
		// Affectation entre big_integer
		code_v4_("big_integer a = 5L big_integer b = a return b").equals("5");
		// Réaffectation
		code_v4_("big_integer a = 5L a = 9L return a").equals("9");
		// Valeur par défaut
		code_v4_("big_integer a return a").equals("0");
	}

	@Test
	public void testConversions() throws Exception {
		section("Conversions");
		// integer -> big_integer (élargissement sans perte)
		code_v4_("big_integer a = 5 return a").equals("5");
		// big_integer -> integer (downcast)
		code_v4_("integer a = 5L return a").equals("5");
		// real -> big_integer (troncature de la partie décimale)
		code_v4_("big_integer a = 5.7 return a").equals("5");
		// real -> big_integer pour un grand réel (ne doit pas saturer à Long.MAX)
		code_v4_("big_integer a = 1000000000000000000000.0 return string(a)").equals("\"1000000000...0000000000\"");
		// big_integer -> real
		code_v4_("real a = 5L return a").equals("5.0");
		// big_integer -> string
		code_v4_("return string(5L)").equals("\"5\"");
		code_v4_("big_integer a = 42L return string(a)").equals("\"42\"");
	}

	@Test
	public void testEquality() throws Exception {
		section("Égalité");
		code_v4_("return 5L == 5L").equals("true");
		code_v4_("return 5L == 6L").equals("false");
		// Égalité mixte big_integer <-> integer
		code_v4_("return 5L == 5").equals("true");
		code_v4_("return 5 == 5L").equals("true");
		code_v4_("return 5L != 6").equals("true");
		code_v4_("big_integer a = 100L return a == 100").equals("true");
		// Grands nombres égaux
		code_v4_("return 123456789012345678901234567890L == 123456789012345678901234567890L").equals("true");
		code_v4_("return 123456789012345678901234567890L == 123456789012345678901234567891L").equals("false");
	}

	@Test
	public void testArithmetic() throws Exception {
		section("Arithmétique");
		// + - * : résultat big_integer (promotion)
		code_v4_("return 2L + 5").equals("7");
		code_v4_("return 5L + 5").equals("10");
		code_v4_("return 10 - 3L").equals("7");
		code_v4_("return 5 * 5L").equals("25");
		code_v4_("var x = 5L + 5 return x instanceof BigInteger").equals("true");
		code_v4_("var x = 5 * 5L return x instanceof BigInteger").equals("true");
		// `/` = division flottante → real
		code_v4_("return 15L / 3").equals("5.0");
		code_v4_("return 15L / 2").equals("7.5");
		code_v4_("var x = 15L / 3 return x instanceof Real").equals("true");
		// `%` = reste → big_integer
		code_v4_("return 15L % 4").equals("3");
		code_v4_("return 17L % 5").equals("2");
		// `**` puissance → big_integer
		code_v4_("return 12L ** 2").equals("144");
		code_v4_("return 2 ** 10L").equals("1024");
		// Négation
		code_v4_("return -5L").equals("-5");
		code_v4_("return -(-1L)").equals("1");
		code_v4_("return -12L * 2").equals("-24");
		code_v4_("return -12L ** 2").equals("144");
		code_v4_("return -12L + 2").equals("-10");
		// Au-delà de 2^63 (le gros intérêt du big_integer)
		code_v4_("return 9223372036854775807L + 1 == 9223372036854775808L").equals("true");
		code_v4_("big_integer a = 1000000000000L return a * a == 1000000000000000000000000L").equals("true");
	}

	@Test
	public void testComparisons() throws Exception {
		section("Comparaisons");
		code_v4_("return 2L < 5").equals("true");
		code_v4_("return 12 < 5L").equals("false");
		code_v4_("return 5L > 2").equals("true");
		code_v4_("return 2L >= 2").equals("true");
		code_v4_("return 2L <= 2").equals("true");
		code_v4_("return 3L <= 2").equals("false");
		// Exactitude au-delà de 2^53 (où une comparaison en double échouerait)
		code_v4_("return 9007199254740993L > 9007199254740992L").equals("true");
		code_v4_("return 100000000000000000000L + 1 > 100000000000000000000L").equals("true");
		code_v4_("return 1267650600228229401496703205376L > 1267650600228229401496703205375L").equals("true");
		// instanceof
		code_v4_("return 5L instanceof BigInteger").equals("true");
		code_v4_("return 5L instanceof Number").equals("true");
		code_v4_("return 5 instanceof BigInteger").equals("false");
	}
}
