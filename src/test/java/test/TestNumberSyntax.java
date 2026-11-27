package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import leekscript.runner.NumberSyntax;

/**
 * NumberSyntax répond comme les analyseurs du JDK, et les conversions de chaîne en nombre qui
 * passent par lui rendent ce qu'elles rendaient par exception rattrapée.
 */
@ExtendWith(SummaryExtension.class)
public class TestNumberSyntax extends TestCommon {

	private static final String[] FIXED = {
		"", " ", "+", "-", ".", "1.", ".1", "1e", "1e+", "1e5", "1e5f", "1f", "1d", "1D", "0x", "0x1", "0x1p3", "0x.8p1", "0X1.8P-2d", "0x1p", "0xp1", "0x1.p1", "0x.p1",
		"NaN", "+NaN", "-Infinity", "Infinity", "Infinit", "nan", "infinity", "NaNf", "Infinityd",
		"9223372036854775807", "9223372036854775808", "-9223372036854775808", "-9223372036854775809", "2147483647", "2147483648", "-2147483648", "-2147483649",
		"+0", "-0", "00000000000000000000000000001", " 12 ", "\t1.5\n", "1 2", "1_000", "１２", "١٢", "+-1", "--1", "1e1.5", "1.2.3", "1ee1", "e1", "1e1e1", "1e-",
		"1L", "0b1", "1,5", " 1", "1 ", "\u00001", "1e99999999999", "abc", "true", "12abc",
	};

	/** Alphabets des chaînes tirées : chacun rend probables les formes proches d'un nombre valide. */
	private static final String[] ALPHABETS = {
		"0123456789", "0123456789+-", "0123456789.eE+-", "0123456789.eE+-fFdD ", "0123456789abcdefABCDEFxXpP.+-", "NaInfityn+- 0",
		"01.eE+-fFdDxXpPNaInfinity \t\n\u0000\u001f ", "0123456789٠١३１９+-.", "9", "0.", "1e+-", "0x1p.Pf", "0x1p.P+-fd9aF", "0X.p1",
	};

	private static boolean parses(Runnable parse) {
		try {
			parse.run();
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static void assertSameAsJdk(String s) {
		assertEquals(parses(() -> Integer.parseInt(s)), NumberSyntax.isInt(s), "isInt [" + s + "]");
		assertEquals(parses(() -> Long.parseLong(s)), NumberSyntax.isLong(s), "isLong [" + s + "]");
		assertEquals(parses(() -> Double.parseDouble(s)), NumberSyntax.isReal(s), "isReal [" + s + "]");
	}

	@Test
	public void sameAnswersAsTheJdkParsers() {
		for (String s : FIXED) assertSameAsJdk(s);
		assertSameAsJdk("9".repeat(400));
		assertSameAsJdk("." + "9".repeat(400));
		var random = new Random(42);
		for (int k = 0; k < 300_000; k++) {
			String alphabet = ALPHABETS[random.nextInt(ALPHABETS.length)];
			int length = random.nextInt(random.nextBoolean() ? 6 : 24);
			var sb = new StringBuilder();
			for (int i = 0; i < length; i++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
			assertSameAsJdk(sb.toString());
		}
	}

	@Test
	public void stringToNumberConversions() throws Exception {
		section("number() d'une chaîne");
		code("return number('abc')").equals("0");
		code("return number('12abc')").equals("0");
		code("return number('1e3')").equals("0");
		code("return number(' 12')").equals("0");
		code("return number('0x10')").equals("0");
		code("return number('-')").equals("0");
		code("return number('1.2.3')").equals("0");
		code("return number('99999999999999999999')").equals("0");
		code("return number('+5')").equals("5");
		code("return number('-7')").equals("-7");
		code_v1("return number(' 1.5 ')").equals("1,5");
		code_v2_("return number(' 1.5 ')").equals("1.5");
		code_v2_("return number('1.5f')").equals("1.5");
		code_v2_("return number('1.')").equals("1.0");

		section("Chaîne dans une opération : sa valeur, sinon sa longueur");
		code("return 'abc' * 2").equals("6");
		code("return '12' * 2").equals("24");
		code("return '1.5' * 2").equals("6");
		code("return 'abc' - 1").equals("2");
		code("return '12' - 1").equals("11");
		code("return ' 12' - 1").equals("2");
		code("return '1e2' - 1").equals("2");
		code("return '0x10' - 1").equals("3");
		code("return '99999999999999999999' - 1").equals("19");
		code("return 'Infinity' - 1").equals("7");
		code("return '' - 1").equals("-1");
		code("return 'true' - 1").equals("0");
		code("return -'abc'").equals("-3");
		code("return -'5'").equals("-5");
		code("return 5 % '3'").equals("2");
		code_v2_("return 5 / 'ab'").equals("2.5");
		code_v2_("return '10' / '4'").equals("2.5");
		code("return 'abc' < 2").equals("false");
		code("return 'abc' > 2").equals("true");
		code("return '1' < 2").equals("true");

		section("Chaîne comparée à un nombre");
		code("return 12 == 'abc'").equals("false");
		code("return 12.5 == 'abc'").equals("false");
		code("return 'abc' == 3").equals("false");
		code("return 12 == '12.0'").equals("false");
		code("return 10 == '1e1'").equals("false");
		code("return 12 == ' 12'").equals("false");
		code("return '0x10' == 16").equals("false");
		code_v1_2("return 12 == '12'").equals("true");
		code_v1_2("return 12.5 == '12.5'").equals("true");
		code_v1_2("return '1e1' == 10").equals("true");
		code_v1_2("return 10.0 == '1e1'").equals("true");
		code_v1_2("return ' 12' == 12").equals("true");
		code_v1_2("return 12.0 == ' 12 '").equals("true");
		code_v4_("return 12 == '12'").equals("false");
		code_v4_("return '12' == 12").equals("false");
	}
}
