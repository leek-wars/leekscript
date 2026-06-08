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
		// Constructeur BigInteger() -> 0
		code_v4_("return BigInteger()").equals("0");
		code_v4_("var x = BigInteger() return x instanceof BigInteger").equals("true");
		code_v4_("return (BigInteger() + 1) << 100 == 1L << 100").equals("true");
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

	@Test
	public void testBitwise() throws Exception {
		section("Opérations binaires");
		// Division entière `\` → big_integer
		code_v4_("return 15L \\ 3").equals("5");
		code_v4_("return 17L \\ 5").equals("3");
		code_v4_("var x = 15L \\ 3 return x instanceof BigInteger").equals("true");
		// ET / OU / XOR / NON
		code_v4_("return 12L & 10").equals("8");
		code_v4_("return 12L | 1").equals("13");
		code_v4_("return 12L ^ 10").equals("6");
		code_v4_("return ~5L").equals("-6");
		code_v4_("return ~(~12L)").equals("12");
		// Décalages — au-delà de 64 bits (le cœur du big_integer)
		code_v4_("return 1L << 100 == 1267650600228229401496703205376L").equals("true");
		code_v4_("big_integer x = 1 return x << 150 == 1427247692705959881058285969449495136382746624L").equals("true");
		code_v4_("return (1L << 200) >> 100 == 1L << 100").equals("true");
		code_v4_("return (1L << 64) >> 64").equals("1");
		// Combinaison
		code_v4_("return ((1L << 80) | 1) & 1").equals("1");
	}

	@Test
	public void testBuiltins() throws Exception {
		section("Fonctions built-in");
		// abs / min / max -> big_integer
		code_v4_("return abs(-5L)").equals("5");
		code_v4_("var x = abs(-5L) return x instanceof BigInteger").equals("true");
		code_v4_("return abs(5L)").equals("5");
		code_v4_("return min(5L, 3L)").equals("3");
		code_v4_("return max(5L, 3L)").equals("5");
		code_v4_("var x = max(5L, 3L) return x instanceof BigInteger").equals("true");
		// pow (fonction) -> big_integer
		code_v4_("return pow(2L, 10)").equals("1024");
		code_v4_("return pow(2L, 100) == 1L << 100").equals("true");
		// binString / hexString sans troncature au-delà de 64 bits
		code_v4_("return binString(5L)").equals("\"101\"");
		code_v4_("return hexString(255L)").equals("\"ff\"");
		code_v4_("return hexString(1L << 80)").equals("\"100000000000000000000\"");
		// LS4 inchangé (les versions integer/real restent sélectionnées)
		code_v4_("return abs(-5)").equals("5");
		code_v4_("return max(5, 3)").equals("5");
		code_v4_("return pow(5, 3)").equals("125.0");

		section("Conteneurs et clone");
		// Tableaux et maps de big_integer
		code_v4_("var a = [5L, 3L, 1L] return a[0] + a[2]").equals("6");
		code_v4_("var a = [5L, 3L, 1L] return arrayMax(a) == 5L").equals("true");
		code_v4_("var m = [:] m['x'] = 1000000000000000000000L return m['x'] == 1000000000000000000000L").equals("true");
		// clone (big_integer immutable)
		code_v4_("var a = 5L var b = clone(a) return b == 5L").equals("true");
		// JSON
		code_v4_("return jsonEncode(5L)").equals("\"5\"");
	}

	@Test
	public void testIncrementDecrement() throws Exception {
		section("Increment / decrement (#bug trouvé : ne compilait pas)");
		code_v4_("big_integer a = 5L a++ return a").equals("6");
		code_v4_("big_integer a = 5L a-- return a").equals("4");
		code_v4_("big_integer a = 5L return ++a").equals("6");
		code_v4_("big_integer a = 5L return --a").equals("4");
		// post-fixe retourne l'ancienne valeur
		code_v4_("big_integer a = 5L return a++").equals("5");
		code_v4_("big_integer a = 5L return a--").equals("5");
		// au-delà de 64 bits
		code_v4_("big_integer a = (1L << 100) a++ return a == (1L << 100) + 1").equals("true");
		code_v4_("big_integer a = (1L << 100) a-- return a == (1L << 100) - 1").equals("true");
		// boucle d'incrément
		code_v4_("big_integer a = 0L for (var i = 0; i < 1000; i++) a++ return a").equals("1000");
		// global
		code_v4_("global g = 5L g++ return g").equals("6");
	}

	@Test
	public void testBooleanContext() throws Exception {
		section("Contexte booléen (#bug trouvé : bigint toujours falsy)");
		code_v4_("if (5L) { return 1 } else { return 0 }").equals("1");
		code_v4_("if (0L) { return 1 } else { return 0 }").equals("0");
		code_v4_("if (1L << 100) { return 1 } else { return 0 }").equals("1");
		code_v4_("return !5L").equals("false");
		code_v4_("return !0L").equals("true");
		code_v4_("return 5L && true").equals("true");
		code_v4_("return 0L && true").equals("false");
		code_v4_("return 0L || true").equals("true");
		code_v4_("return 5L ? \"a\" : \"b\"").equals("\"a\"");
		code_v4_("return 0L ? \"a\" : \"b\"").equals("\"b\"");
	}

	@Test
	public void testMixedArgs() throws Exception {
		section("Arguments mixtes bigint/integer (#bug trouvé : troncature)");
		// max/min avec un bigint et un integer : l'integer est promu, le bigint
		// n'est PAS tronqué.
		code_v4_("return max(1L << 100, 5) == 1L << 100").equals("true");
		code_v4_("return max(5, 1L << 100) == 1L << 100").equals("true");
		code_v4_("return min(1L << 100, 5) == 5L").equals("true");
		code_v4_("var x = max(1L << 100, 5) return x instanceof BigInteger").equals("true");
		// pow avec exposant integer ou bigint
		code_v4_("return pow(2L, 100) == 1L << 100").equals("true");
		code_v4_("return pow(2, 10L) == 1024L").equals("true");
		code_v4_("return pow(2L, 10L) == 1024L").equals("true");
		// abs
		code_v4_("return abs(1L << 100) == 1L << 100").equals("true");
		code_v4_("return abs(-(1L << 100)) == 1L << 100").equals("true");
	}

	@Test
	public void testEdgeCases() throws Exception {
		section("Cas limites");
		// puissances signées
		code_v4_("return (-1L) ** 100 == 1L").equals("true");
		code_v4_("return (-1L) ** 101 == -1L").equals("true");
		code_v4_("return 0L ** 5").equals("0");
		code_v4_("return 5L ** 0").equals("1");
		// modulo négatif : signe du dividende (comme integer)
		code_v4_("return -7L % 3L").equals("-1");
		code_v4_("return 7L % -3L").equals("1");
		code_v4_("return -7L \\ 2L").equals("-3");
		// gros modulo
		code_v4_("return (1L << 1000) % 1000000007L").equals("688423210");
		// factorielle 50 exacte
		code_v4_("big_integer f = 1 for (var i = 2; i <= 50; i++) f = f * i return f == 30414093201713378043612608166064768844377641568960512000000000000L").equals("true");
		// clone (immutable) et conteneurs
		code_v4_("var a = [1L, [2L, 3L]] var b = clone(a, 2) return b[1][0] == 2L").equals("true");
		code_v4_("return \"x=\" + (1L << 64)").equals("\"x=18446744073709551616\"");
		// big_integer comme clé de map (hashCode/equals)
		code_v4_("var m = [(1L << 100): \"ok\"] return m[1L << 100]").equals("\"ok\"");
		// dédup dans un set
		code_v4_("return setSize(<5L, 5L, 3L>)").equals("2");
		code_v4_("return setSize(<(1L << 100), (1L << 100)>)").equals("1");
	}

	@Test
	public void testSortAndJson() throws Exception {
		section("Tri par défaut (#bug : comparaison double lossy)");
		code_v4_("return arraySort([3L, 1L, 2L])").equals("[1, 2, 3]");
		// Gros bigints différant en deçà de la précision double : doivent être
		// triés EXACTEMENT (une comparaison double les verrait égaux).
		code_v4_("return arraySort([(1L << 70) + 2, (1L << 70) + 1, (1L << 70)])[0] == 1L << 70").equals("true");
		code_v4_("var a = arraySort([(1L << 70) + 1, (1L << 70)]) return a[0] == 1L << 70").equals("true");
		code_v4_("return arraySort([(1L << 100), 5L, (1L << 50)])[0] == 5L").equals("true");

		section("jsonEncode (#bug : valeur tronquée -> JSON invalide)");
		code_v4_("return jsonEncode(5L)").equals("\"5\"");
		// Au-delà de 64 bits : le nombre COMPLET (pas la string cropée) -> JSON valide
		code_v4_("return jsonEncode(1L << 70)").equals("\"1180591620717411303424\"");
		code_v4_("return jsonEncode([5L, (1L << 80)])").equals("\"[5,1208925819614629174706176]\"");
		// round-trip : encode produit du JSON valide (le décode d'un entier
		// surdimensionné donne un real, JSON n'ayant pas de type entier arbitraire)
		code_v4_("return jsonEncode(jsonDecode(jsonEncode(123L)))").equals("\"123\"");
	}
}
