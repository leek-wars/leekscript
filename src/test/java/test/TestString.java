package test;

public class TestString extends TestCommon {

	public void run() {

		section("String.charAt()");
		code("return charAt('bonjour', 1)").equals("o");

		section("String.length()");
		code("return length('bonjour')").equals("7");

		section("String.substring()");
		code("return substring('bonjour',2,3)").equals("njo");

		section("String.replace()");
		code("return replace('bonjour','onj','pro')").equals("bproour");
		code("return replace('testtest', 'test', '{id}')").equals("{id}{id}");
		code("return replace('testtest', 'test', '$id')").equals("$id$id");

		section("String.indexOf()");
		code("return indexOf('bonjour','o')").equals("1");
		code("return indexOf('bonjour','o',2)").equals("4");

		section("String.split()");
		code("return split('1:2:3:4:5',':')").equals("[1, 2, 3, 4, 5]");
		code("return split('1:2:3:4:5',':',2)").equals("[1, 2:3:4:5]");

		section("String.toLower()");
		code("return toLower('AbCDefgh')").equals("abcdefgh");

		section("String.toUpper()");
		code("return toUpper('AbCDefgh')").equals("ABCDEFGH");

		section("String.startsWith()");
		code("return startsWith('bonjour','bon')").equals("true");
		code("return startsWith('bonjour','jour')").equals("false");

		section("String.endsWith()");
		code("return endsWith('bonjour','bon')").equals("false");
		code("return endsWith('bonjour','jour')").equals("true");

		section("String.contains()");
		code("return contains('bonjour','bon')").equals("true");
		code("return contains('bonjour','jour')").equals("true");
		code("return contains('bonjour','jourr')").equals("false");

		section("String.number()");
		code("return contains('bonjour','bon')").equals("true");
		code("return contains('bonjour','jour')").equals("true");
		code("return contains('bonjour','jourr')").equals("false");
	}
}
