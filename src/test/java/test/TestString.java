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

}
