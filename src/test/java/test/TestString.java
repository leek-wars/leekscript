package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

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
	}

	@Test
	public void testString_charAt() throws Exception {
		section("String.charAt()");
		code("return charAt('bonjour', 1)").equals("\"o\"");
	}

	@Test
	public void testString_length() throws Exception {
		section("String.length()");
		code("return length('bonjour')").equals("7");
	}

	@Test
	public void testString_substring() throws Exception {
		section("String.substring()");
		code("return substring('bonjour',2,3)").equals("\"njo\"");
	}

	@Test
	public void testString_replace() throws Exception {
		section("String.replace()");
		code("return replace('bonjour','onj','pro')").equals("\"bproour\"");
		code("return replace('testtest', 'test', '{id}')").equals("\"{id}{id}\"");
		code("return replace('testtest', 'test', '$id')").equals("\"$id$id\"");
	}

	@Test
	public void testString_replaceAmplificationIsBilled() throws Exception {
		// Memory amplification protection: replace(string, "", longRepl) inserts
		// longRepl between each char, so output = string.length() * longRepl.length().
		// The ops cost must reflect that upper bound, otherwise a hostile AI could
		// OOM the worker faster than its ops budget can stop it.
		section("String.replace() amplification billing");
		// 50 chars * 50 chars = 2500 ops billed; expansion produces 50 * 51 = 2550 chars.
		code("var big = '' var rep = '' for (var i = 0; i < 50; i++) { big = big + 'X' rep = rep + 'Y' } return length(replace(big, '', rep)) >= 2500").equals("true");
	}

	@Test
	public void testString_indexOf() throws Exception {
		section("String.indexOf()");
		code("return indexOf('bonjour','o')").equals("1");
		code("return indexOf('bonjour','o',2)").equals("4");
	}

	@Test
	public void testString_split() throws Exception {
		section("String.split()");
		code("return split('1:2:3:4:5',':')").equals("[\"1\", \"2\", \"3\", \"4\", \"5\"]");
		code("return split('1:2:3:4:5',':',2)").equals("[\"1\", \"2:3:4:5\"]");
		code("var a = split('a b c d e f', ' ') return count(a)").equals("6");
	}

	@Test
	public void testString_toLower() throws Exception {
		section("String.toLower()");
		code("return toLower('AbCDefgh')").equals("\"abcdefgh\"");
	}

	@Test
	public void testString_toUpper() throws Exception {
		section("String.toUpper()");
		code("return toUpper('AbCDefgh')").equals("\"ABCDEFGH\"");
	}

	@Test
	public void testString_startsWith() throws Exception {
		section("String.startsWith()");
		code("return startsWith('bonjour','bon')").equals("true");
		code("return startsWith('bonjour','jour')").equals("false");
	}

	@Test
	public void testString_endsWith() throws Exception {
		section("String.endsWith()");
		code("return endsWith('bonjour','bon')").equals("false");
		code("return endsWith('bonjour','jour')").equals("true");
	}

	@Test
	public void testString_contains() throws Exception {
		section("String.contains()");
		code("return contains('bonjour','bon')").equals("true");
		code("return contains('bonjour','jour')").equals("true");
		code("return contains('bonjour','jourr')").equals("false");
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
		code("return codePointAt('A', 0)").equals("65");
		code("return codePointAt('ABC', 2)").equals("67");
		code("return codePointAt('©', 0)").equals("169");
		code("return codePointAt('é', 0)").equals("233");
		code("return codePointAt('♫', 0)").equals("9835");
		code("return codePointAt('🐨🐨', 2)").equals("128040");
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
		code_v2_("return length(\"abc\\\"def\")").equals("7");
		code_v2_("return \"a\\\"b\" == 'a\"b'").equals("true");
		// v1 (legacy) : `\"` n'était pas une vraie séquence d'échappement, le `\` était préservé
		code_v1("return length(\"abc\\\"def\")").equals("8");
		// String simple — sanity check qu'aucune régression silencieuse n'est introduite
		code("return length('hello')").equals("5");
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

}
