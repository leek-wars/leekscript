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
}
