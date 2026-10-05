package test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Location;
import leekscript.compiler.Options;
import leekscript.compiler.exceptions.LeekCompilerException;

@ExtendWith(SummaryExtension.class)
public class TestString extends TestCommon {


		@Test
	public void testOperator_Plus() throws Exception {
		section("Operator +");
		code("return 1+','+2").equals("\"1,2\"");
		code("return 1 + ',' + 2").equals("\"1,2\"");
		code("return 1 + ', ' + 2").equals("\"1, 2\"");
		code("return 1 + \", \" + 2").equals("\"1, 2\"");
	}

	@Test
	public void testString_interpolation() throws Exception {
		// String interpolation (v4+) : sucre syntaxique au-dessus de la concaténation.
		// "a${expr}b" est desugaré en ("a" + (expr) + "b") au niveau du lexer, donc
		// le code Java généré (et le coût en opérations) est identique à une
		// concaténation écrite à la main.
		section("String interpolation");
		code_v4_("var name = \"Bob\" return \"Hello ${name}!\"").equals("\"Hello Bob!\"");
		code_v4_("return \"${1 + 2}\"").equals("\"3\"");
		code_v4_("return \"${42}\"").equals("\"42\""); // un entier interpolé devient une chaîne
		code_v4_("var a = 2 var b = 3 return \"${a}+${b}=${a + b}\"").equals("\"2+3=5\"");
		code_v4_("var x = 5 return \"x=${x}.\"").equals("\"x=5.\"");
		code_v4_("var arr = [1, 2, 3] return \"arr=${arr}, len=${count(arr)}\"").equals("\"arr=[1, 2, 3], len=3\"");
		// Interpolation imbriquée
		code_v4_("return \"out ${ \"in ${1 + 1}\" }!\"").equals("\"out in 2!\"");
		// Une chaîne sans ${ reste un littéral simple
		code_v4_("return \"no interpolation\"").equals("\"no interpolation\"");
		// Coût identique à la concaténation manuelle
		code_v4_("var n = \"Bob\" return \"Hi ${n}!\" == \"Hi \" + n + \"!\"").equals("true");
	}

	@Test
	public void testString_interpolation_escaping() throws Exception {
		section("String interpolation escaping");
		// \$ échappe l'interpolation et produit un $ littéral
		code_v4_("var n = 1 return \"\\${n}=${n}\"").equals("\"${n}=1\"");
		code_v4_("return \"cost \\$5\"").equals("\"cost $5\"");
		// Un $ seul (non suivi de {) reste littéral
		code_v4_("return \"price: 5$\"").equals("\"price: 5$\"");
		// Les chaînes à guillemets simples ne sont jamais interpolées
		code_v4_("return '${1 + 1}'").equals("\"${1 + 1}\"");
		// Avant la v4, ${ } est un littéral (pas d'interpolation)
		code_v1_3("return \"a${1}b\"").equals("\"a${1}b\"");
		code_v1_3("return \"${\"").equals("\"${\"");
		// \$ n'échappe qu'en v4+ et entre guillemets doubles : ailleurs le backslash reste
		code_v4_("return length(\"\\$\")").equals("1");
		code_v4_("return length('\\$')").equals("2");
		code_v4_("return length('\\${1}')").equals("5");
		code_v1_3("return length(\"\\$\")").equals("2");
		// \\ puis ${ : le backslash est échappé, l'interpolation a lieu
		code_v4_("return length(\"\\\\${1}\")").equals("2");
	}

	@Test
	public void testString_interpolation_lexer() throws Exception {
		section("String interpolation lexer");
		// Une } dans une chaîne imbriquée ne ferme pas l'interpolation
		code_v4_("return \"${'}'}\"").equals("\"}\"");
		code_v4_("return \"${\"}\"}\"").equals("\"}\"");
		code_v4_("return \"${\"{\"}\"").equals("\"{\"");
		// Les accolades d'un objet ou d'un bloc s'équilibrent
		code_v4_("return \"${ {a: 1}.a }\"").equals("\"1\"");
		code_v4_("return \"${ [1: 2][1] }\"").equals("\"2\"");
		code_v4_("return \"${ function() { return 1 }() }\"").equals("\"1\"");
		// Commentaires dans l'expression
		code_v4_("return \"${ 1 /* } */ + 2 }\"").equals("\"3\"");
		// Accolades et $ hors interpolation : texte
		code_v4_("return \"}${1}{\"").equals("\"}1{\"");
		code_v4_("return \"${1}}\"").equals("\"1}\"");
		code_v4_("return \"$\"").equals("\"$\"");
		code_v4_("return \"$ {1}\"").equals("\"$ {1}\"");
		code_v4_("return \"$${1}\"").equals("\"$1\"");
		code_v4_("return \"\"").equals("\"\"");
		code_v4_("return \"${1}${2}\"").equals("\"12\"");
		code_v4_("return \"${null}\"").equals("\"null\"");
		code_v4_("return \"${true ? \"y\" : \"n\"}\"").equals("\"y\"");
		// Sur plusieurs lignes
		code_v4_("return length(\"l1\n${1 + 1}\nl3\")").equals("7");
		// Le résultat reste une expression comme une autre
		code_v4_("var x = 5 return \"${x}\"[0]").equals("\"5\"");
		code_v4_("return \"${1}\" + \"${2}\" + 3").equals("\"123\"");
		code_v4_("var m = [\"${1}\": 3] return m[\"1\"]").equals("3");
	}

	@Test
	public void testString_interpolation_errors() throws Exception {
		section("String interpolation errors");
		code_v4_("return \"${}\"").error(Error.VALUE_EXPECTED);
		code_v4_("return \"a${ }b\"").error(Error.VALUE_EXPECTED);
		code_v4_("return \"${ /* rien */ }\"").error(Error.VALUE_EXPECTED);
		code_v4_("return \"abc ${1 + 2\"").error(Error.STRING_NOT_CLOSED);
		code_v4_("return \"abc ${1 + 2").error(Error.STRING_NOT_CLOSED);
		code_v4_("return \"${\"").error(Error.STRING_NOT_CLOSED);
		code_v4_("return \"${ 1 // }\"").error(Error.STRING_NOT_CLOSED);
		code_v4_("return \"a${1}b").error(Error.STRING_NOT_CLOSED);
		code_v4_("var x = 1 return \"${x +}\"").error(Error.UNCOMPLETE_EXPRESSION);
		code_v4_("return \"${#}\"").error(Error.INVALID_CHAR);
		code_v4_("include(\"${1}\")").error(Error.AI_NAME_EXPECTED);
	}

	private static final AtomicInteger interpolationFiles = new AtomicInteger(1_000_000);

	private static AIFile interpolationFile(String code) {
		int id = interpolationFiles.incrementAndGet();
		var file = new AIFile("interpolation", code, System.currentTimeMillis(), 4, id, false);
		file.setJavaClass("AI_" + id);
		file.setRootClass("AI");
		file.setId(id);
		return file;
	}

	private static Location compileErrorLocation(String code) {
		var file = interpolationFile(code);
		return assertThrows(LeekCompilerException.class, () -> file.compile(new Options())).getLocation();
	}

	private static String position(Location location) {
		return location.getStartLine() + ":" + location.getStartColumn() + "-" + location.getEndLine() + ":" + location.getEndColumn();
	}

	/**
	 * L'éditeur souligne l'erreur à sa position : un `${` jamais fermé avale la fin du
	 * fichier, l'erreur doit pointer le `${` et pas la fin.
	 */
	@Test
	public void testString_interpolation_errorLocations() throws Exception {
		assertEquals("2:12-2:13", position(compileErrorLocation("var x = 1\nreturn \"abc ${x\"\nvar y = 2\n")));
		assertEquals("1:8-1:9", position(compileErrorLocation("return \"${\"")));
		// `${}` : le `${` et le `}`
		assertEquals("1:8-1:10", position(compileErrorLocation("return \"${}\"")));
		// Erreur dans l'expression : sur le `}` qui la termine trop tôt (le parser ajoute
		// ensuite CLOSING_PARENTHESIS_EXPECTED, comme pour `("line " + (x +) + " end")`)
		var file = interpolationFile("var x = 1\nvar s = \"line ${x +} end\"\nreturn s");
		assertThrows(LeekCompilerException.class, () -> file.compile(new Options()));
		var incomplete = file.getErrors().stream().filter(e -> e.error == Error.UNCOMPLETE_EXPRESSION).findFirst().orElseThrow();
		assertEquals("2:19-2:19", position(incomplete.location));
		// Une chaîne de plusieurs lignes ne décale pas les erreurs suivantes
		assertEquals("5:7-5:17", position(compileErrorLocation("var x = 1\nvar s = \"a\n${x}\nb\"\nreturn zzz_inconnu\n")));
	}

	/**
	 * Une chaîne interpolée est découpée en tokens synthétiques (`"a${x}b"` devient
	 * `( "a" + ( x ) + "b" )`). `atLocation` cherche par dichotomie : si leurs positions
	 * ne sont pas triées, le survol d'un caractère tombe sur un autre token, ou sur rien.
	 */
	@Test
	public void testString_interpolation_hover() throws Exception {
		var code = "var abc = 5 var y = 2 return \"${abc}longtail ${y}\" + abc";
		var file = interpolationFile(code);
		file.compile(new Options());
		var tokens = file.getTokenStream();
		int quote = code.indexOf('"');
		var expected = new String[] { "(", "+", "(", "abc", "abc", "abc", ")",
			"\"longtail \"", "\"longtail \"", "\"longtail \"", "\"longtail \"", "\"longtail \"", "\"longtail \"", "\"longtail \"", "\"longtail \"", "\"longtail \"",
			"+", "(", "y", ")", ")" };
		for (int i = 0; i < expected.length; i++) {
			var token = tokens.atLocation(1, quote + i);
			assertNotNull(token, "colonne " + (quote + i));
			assertEquals(expected[i], token.getWord(), "colonne " + (quote + i));
		}
		// La variable interpolée est bien analysée : son survol donne sa définition
		var hover = file.hover(1, code.indexOf("abc}"));
		assertNotNull(hover);
		assertNotNull(hover.defined);
		assertEquals(1, hover.defined.getStartLine());
		assertEquals(code.indexOf("abc"), hover.defined.getStartColumn());
	}

	/** Le survol tient partout sur une interpolation cassée (le code ne compile pas). */
	@Test
	public void testString_interpolation_hoverOnBrokenCode() throws Exception {
		for (var code : new String[] {
			"var x = 1\nreturn \"abc ${x\"\nvar y = 2\n",
			"return \"a${}b\"\n",
			"var x = 1\nreturn \"a${x +}b\"\n",
			"var x = 1\nreturn \"a\n${x}\nb${x}\"\n",
		}) {
			var file = interpolationFile(code);
			try {
				file.compile(new Options());
			} catch (LeekCompilerException e) {
				// Attendu pour les trois premiers
			}
			var lines = code.split("\n", -1);
			for (int l = 1; l <= lines.length; l++) {
				for (int c = 0; c <= lines[l - 1].length(); c++) {
					final int line = l, column = c;
					assertDoesNotThrow(() -> file.hover(line, column), code + " ligne " + line + " colonne " + column);
				}
			}
		}
	}

	private static long operations(String code) throws Exception {
		var ai = LeekScript.compileSnippet(code, "AI", new Options(4, false, false, true, null, true));
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		ai.runIA();
		return ai.operations();
	}

	@Test
	public void testString_interpolation_operations() throws Exception {
		assertEquals(operations("var n = 'Bob' return 'Hi ' + n + '!'"), operations("var n = 'Bob' return \"Hi ${n}!\""));
		assertEquals(operations("var a = 2 var b = 3 return '' + a + '+' + b + '=' + (a + b)"), operations("var a = 2 var b = 3 return \"${a}+${b}=${a + b}\""));
	}

	@Test
	public void testString_charAt() throws Exception {
		section("String.charAt()");
		code_v1_4("return charAt('bonjour', 1)").equals("\"o\"");
	}

	@Test
	public void testString_length() throws Exception {
		section("String.length()");
		code_v1_4("return length('bonjour')").equals("7");
	}

	@Test
	public void testString_substring() throws Exception {
		section("String.substring()");
		code_v1_4("return substring('bonjour',2,3)").equals("\"njo\"");
	}

	@Test
	public void testString_replace() throws Exception {
		section("String.replace()");
		code_v1_4("return replace('bonjour','onj','pro')").equals("\"bproour\"");
		code_v1_4("return replace('testtest', 'test', '{id}')").equals("\"{id}{id}\"");
		code_v1_4("return replace('testtest', 'test', '$id')").equals("\"$id$id\"");
	}

	@Test
	public void testString_replaceAmplificationIsBilled() throws Exception {
		// Memory amplification protection: replace(string, "", longRepl) inserts
		// longRepl between each char, so output = string.length() * longRepl.length().
		// The ops cost must reflect that upper bound, otherwise a hostile AI could
		// OOM the worker faster than its ops budget can stop it.
		section("String.replace() amplification billing");
		// 50 chars * 50 chars = 2500 ops billed; expansion produces 50 * 51 = 2550 chars.
		code_v1_4("var big = '' var rep = '' for (var i = 0; i < 50; i++) { big = big + 'X' rep = rep + 'Y' } return length(replace(big, '', rep)) >= 2500").equals("true");
	}

	@Test
	public void testString_indexOf() throws Exception {
		section("String.indexOf()");
		code_v1_4("return indexOf('bonjour','o')").equals("1");
		code_v1_4("return indexOf('bonjour','o',2)").equals("4");
	}

	@Test
	public void testString_split() throws Exception {
		section("String.split()");
		code_v1_4("return split('1:2:3:4:5',':')").equals("[\"1\", \"2\", \"3\", \"4\", \"5\"]");
		code_v1_4("return split('1:2:3:4:5',':',2)").equals("[\"1\", \"2:3:4:5\"]");
		code_v1_4("var a = split('a b c d e f', ' ') return count(a)").equals("6");
	}

	@Test
	public void testString_toLower() throws Exception {
		section("String.toLower()");
		code_v1_4("return toLower('AbCDefgh')").equals("\"abcdefgh\"");
	}

	@Test
	public void testString_toUpper() throws Exception {
		section("String.toUpper()");
		code_v1_4("return toUpper('AbCDefgh')").equals("\"ABCDEFGH\"");
	}

	@Test
	public void testString_startsWith() throws Exception {
		section("String.startsWith()");
		code_v1_4("return startsWith('bonjour','bon')").equals("true");
		code_v1_4("return startsWith('bonjour','jour')").equals("false");
	}

	@Test
	public void testString_endsWith() throws Exception {
		section("String.endsWith()");
		code_v1_4("return endsWith('bonjour','bon')").equals("false");
		code_v1_4("return endsWith('bonjour','jour')").equals("true");
	}

	@Test
	public void testString_contains() throws Exception {
		section("String.contains()");
		code_v1_4("return contains('bonjour','bon')").equals("true");
		code_v1_4("return contains('bonjour','jour')").equals("true");
		code_v1_4("return contains('bonjour','jourr')").equals("false");
	}

	@Test
	public void testString_string() throws Exception {
		section("String string()");
		code("return string('hello')").equals("\"hello\"");
		code_v1_3("return string(['a', 'b', 'c'])").equals("\"[a, b, c]\"");
		code_v4_("return string(['a', 'b', 'c'])").equals("\"[\"a\", \"b\", \"c\"]\"");
		code_v1_3("return string(['a', ['b', 'c']])").equals("\"[a, [b, c]]\"");
		code_v4_("return string(['a', ['b', 'c']])").equals("\"[\"a\", [\"b\", \"c\"]]\"");
		code_v1_3("return string(['a' : 'b', 'c' : 'd'])").equals("\"[a : b, c : d]\"");
		code_v4_("return string(['a' : 'b', 'c' : 'd'])").equals("\"[\"a\" : \"b\", \"c\" : \"d\"]\"");
		code_v3("return string({ a: 'a', b: 'b' })").equals("\"{a: a, b: b}\"");
		code_v4_("return string({ a: 'a', b: 'b' })").equals("\"{a: \"a\", b: \"b\"}\"");
	}

	@Test
	public void testString_codePointAt() throws Exception {
		section("String codePointAt()");
		code_v1_4("return codePointAt('A', 0)").equals("65");
		code_v1_4("return codePointAt('ABC', 2)").equals("67");
		code_v1_4("return codePointAt('©', 0)").equals("169");
		code_v1_4("return codePointAt('é', 0)").equals("233");
		code_v1_4("return codePointAt('♫', 0)").equals("9835");
		code_v1_4("return codePointAt('🐨🐨', 2)").equals("128040");
	}

	@Test
	public void testString_fromCodePoint() throws Exception {
		section("String fromCodePoint()");
		code("return fromCodePoint(65)").equals("\"A\"");
		code("return fromCodePoint(233)").equals("\"é\"");
		code("return fromCodePoint(9835)").equals("\"♫\"");
		code("return fromCodePoint(128040)").equals("\"🐨\"");
		code("return fromCodePoint(codePointAt('🐨'))").equals("\"🐨\"");
		code("return length(fromCodePoint(128040))").equals("2");
		code("return fromCodePoint(-1)").equals("null");
		code("return fromCodePoint(0x110000)").equals("null");
		code("return fromCodePoint(0xD800)").equals("null");
		code("var fromCodePoint = 1 return fromCodePoint").equals("1");
	}

	/**
	 * Edge cases du lexer tryParseString — la refonte fait un scan direct sur
	 * `content` et track manuellement lineCounter pour les newlines internes.
	 * Si on rate le tracking, les erreurs ultérieures pointent vers la mauvaise ligne.
	 */
	@Test
	public void testString_lexerEdgeCases() throws Exception {
		section("String lexer edge cases");
		// Quote simple à l'intérieur de double quotes (pas d'escape nécessaire)
		code("return \"it's\"").equals("\"it's\"");
		// String vide — les deux délimiteurs
		code("return ''").equals("\"\"");
		code("return \"\"").equals("\"\"");
		// String non fermée à EOF → error (le lexer doit reporter STRING_NOT_CLOSED)
		code("return 'unclosed").any_error();
		code("return \"unclosed").any_error();
		// `\"` est traité comme séquence d'échappement → 7 chars `abc"def` en v2+
		code_v2_4("return length(\"abc\\\"def\")").equals("7");
		code_v2_("return \"a\\\"b\" == 'a\"b'").equals("true");
		// v1 (legacy) : `\"` n'était pas une vraie séquence d'échappement, le `\` était préservé
		code_v1("return length(\"abc\\\"def\")").equals("8");
		// String simple — sanity check qu'aucune régression silencieuse n'est introduite
		code_v1_4("return length('hello')").equals("5");
	}

	/**
	 * Accès indexé sur les chaînes (#3138) : caractère, index négatif, slices, comme
	 * pour les listes. Fonctionne avec une variable typée `string`, non typée (`var`),
	 * `any` ou directement sur un littéral.
	 */
	@Test
	public void testString_indexAccess() throws Exception {
		section("String index access");
		// Caractère
		code_v4_("var s = 'hello' return s[0]").equals("\"h\"");
		code_v4_("var s = 'hello' return s[1]").equals("\"e\"");
		code_v4_("string s = 'hello' return s[4]").equals("\"o\"");
		code_v4_("any s = 'hello' return s[1]").equals("\"e\"");
		code_v4_("return 'hello'[1]").equals("\"e\"");
		// Index négatif (depuis la fin)
		code_v4_("var s = 'hello' return s[-1]").equals("\"o\"");
		code_v4_("var s = 'hello' return s[-5]").equals("\"h\"");
		// Hors-bornes → null
		code_v4_("var s = 'hello' return s[10]").equals("null");
		code_v4_("var s = '' return s[0]").equals("null");
		// Unicode
		code_v4_("var s = 'café' return s[3]").equals("\"é\"");
		// Concaténation de caractères
		code_v4_("var s = 'hello' return s[0] + s[-1]").equals("\"ho\"");
		code_v4_("string s = 'abc' var r = '' for (var i = 0; i < 3; i++) r += s[i] return r").equals("\"abc\"");

		section("String slices");
		code_v4_("var s = 'hello' return s[1:4]").equals("\"ell\"");
		code_v4_("string s = 'hello' return s[1:4]").equals("\"ell\"");
		code_v4_("any s = 'hello' return s[1:4]").equals("\"ell\"");
		code_v4_("return 'hello'[1:4]").equals("\"ell\"");
		// Bornes ouvertes
		code_v4_("var s = 'hello' return s[:3]").equals("\"hel\"");
		code_v4_("var s = 'hello' return s[2:]").equals("\"llo\"");
		code_v4_("var s = 'hello' return s[:]").equals("\"hello\"");
		// Index négatifs
		code_v4_("var s = 'hello' return s[-3:]").equals("\"llo\"");
		code_v4_("var s = 'hello' return s[:-2]").equals("\"hel\"");
		// Pas (stride)
		code_v4_("var s = 'hello' return s[::2]").equals("\"hlo\"");
		code_v4_("var s = 'hello' return s[1:4:2]").equals("\"el\"");
		// Pas négatif → inversion
		code_v4_("var s = 'hello' return s[::-1]").equals("\"olleh\"");
		code_v4_("string s = 'hello' return s[::-1]").equals("\"olleh\"");
		// Slice vide
		code_v4_("var s = 'hello' return s[3:1]").equals("\"\"");
	}

	@Test
	public void testSplit_defaultLimit() throws Exception {
		section("split à limite -1 par défaut en LS5 (#3419)");
		// LS5 : limite par défaut -1 → conserve les chaînes vides en fin.
		code_v5("return 'a,b,'.split(',')").equals("[\"a\", \"b\", \"\"]");
		code_v5("return 'a,,b'.split(',')").equals("[\"a\", \"\", \"b\"]");
		// v4 (gelé) : limite 0 → Java supprime les chaînes vides de fin.
		code_v4("return split('a,b,', ',')").equals("[\"a\", \"b\"]");
		// Une limite explicite reste honorée dans les deux cas.
		code_v5("return 'a,b,c'.split(',', 2)").equals("[\"a\", \"b,c\"]");
	}

}
