package test;

import leekscript.common.Error;

public class TestNumber extends TestCommon {

	public void run() throws Exception {

		header("Numbers");

		section("Basic numbers");
		code("return 0").equals("0");
		code("return -1").equals("-1");
		code("return -(-1)").equals("1");
		code_v1("return -1e3").equals("-1 000");
		code_v2_("return -1e3").equals("-1000.0");
		code_v1("return 1e-3").equals("0,001");
		code_v2_("return 1e-3").equals("0.001");
		code_v1("return 1e-3+5").equals("5,001");
		code_v2_("return 1e-3+5").equals("5.001");
		code_v1("return 1e+3").equals("1 000");
		code_v2_("return 1e+3").equals("1000.0");
		code_v1("return 1e+3+2").equals("1 002");
		code_v2_("return 1e+3+2").equals("1002.0");
		code_v1("return 1e+3-2").equals("998");
		code_v2_("return 1e+3-2").equals("998.0");
		code_v1("return 1.5e-3").equals("0,002");
		code_v2_("return 1.5e-3").equals("0.0015");
		// TODO special character constant
		// code("π").almost(3.141592653589793116);

		section("Lexical errors");
		code("12345r").error(Error.INVALID_NUMBER);
		code("0b011001711").error(Error.INVALID_NUMBER);
		code("0b").error(Error.INVALID_NUMBER);
		code("0x").error(Error.INVALID_NUMBER);
		code("0x+").error(Error.UNCOMPLETE_EXPRESSION);
		code("0x;").error(Error.INVALID_NUMBER);
		code("0b#").error(Error.INVALID_CHAR);
		code("0b'").error(Error.END_OF_SCRIPT_UNEXPECTED);
		code("0b\"").error(Error.END_OF_SCRIPT_UNEXPECTED);
		code("0xeazblqzd").error(Error.INVALID_NUMBER);
		code("0xPMQBTRAZ").error(Error.INVALID_NUMBER);
		code("0xffxff").error(Error.INVALID_NUMBER);
		code("0b101b010").error(Error.INVALID_NUMBER);
		code("0b101x010").error(Error.INVALID_NUMBER);
		code("0b101.010").error(Error.INVALID_NUMBER);

		section("Basic operations");
		code("return 0 + 5;").equals("5");
		code("return 5 + 5;").equals("10");
		code("return 10 - 3;").equals("7");
		code("return -2 + 3;").equals("1");
		code("return 5 * 5;").equals("25");
		code_v1("return 15 / 3;").equals("5");
		code_v2_("return 15 / 3;").equals("5.0");
		code_v1("return 15 / 2;").equals("7,5");
		code_v2_("return 15 / 2;").equals("7.5");
		code("return 12 ** 2;").equals("144");
		code("return 2 ** 5;").equals("32");
		code("return 2 < 5;").equals("true");
		code("return 12 < 5;").equals("false");
		code("return 5 == 12;").equals("false");
		code("return 12 == 12;").equals("true");
		code_v1("return 0.2 + 0.1").equals("0,3");
		code_v2_("return 0.2 + 0.1").almost(0.3);
		// TODO absolute value operator
		// DISABLED_code("return |-12|;").equals("12");
		code("return -12 * 2;").equals("-24");
		code("return (-12) * 2;").equals("-24");
		code("return -12 ** 2;").equals("144");
		code("return (-12) ** 2;").equals("144");
		code("return -12 + 2;").equals("-10");
		code("var a = [2, 'a'] return [-a[0], ~a[0]] == [-2, ~2];").equals("true");
		// TODO operator +x
		// code("var a = [2, 'a'] return [-a[0], +a[0], ~a[0]] == [-2, 2, ~2];").equals("true");

		section("Hexadecimal representation");
		code("return 0x0;").equals("0");
		code("return 0x00000000").equals("0");
		code("return 0x1").equals("1");
		code("return 0x00000001").equals("1");
		code("return 0xf").equals("15");
		code("return 0x0000000f").equals("15");
		code("return -0xf").equals("-15");
		code("return 0xff").equals("255");
		code("return 0x10").equals("16");
		code("return -0xffff").equals("-65535");
		code_v1("return -0x1.p53").equals("-9 007 199 254 740 992");
		code_v2_("return -0x1.p53").equals("-9.007199254740992E15");
		code_v1("return -0xa.bcdp-42").equals("-0");
		code_v2_("return -0xa.bcdp-42").equals("-2.4414359423019505E-12");
		code("return 0xffffffff").equals("4294967295");
		code("return 0x7FFFFFFFFFFFFFFF").equals("9223372036854775807");
		// TODO Arbitrary precision numbers
		// code("return 0x8fa6cd83e41a6f4ec").equals("165618988158544180460");
		//code("-0xa71ed8fa6cd83e41a6f4eaf4ed9dff8cc3ab1e9a4ec6baf1ea77db4fa1c").equals("-72088955549248787618860543269425825306377186794534918826231778059287068");
		//code("0xfe54c4ceabf93c4eaeafcde94eba4c79741a7cc8ef43daec6a71ed8fa6cd8b3e41a6f4ea7f4ed9dff8cc3ab61e9a4ec6baf1ea77deb4fa1c").equals("722100440055342029825617696009879717719483550913608718409456486549003139646247155371523487552495527165084677501327990299146441654073884");

		section("Binary representation");
		code("return 0b0").equals("0");
		code("return 0b00001").equals("1");
		code("return 0b1001010110").equals("598");
		code("return -0b0101101001111").equals("-2895");
		code("return 0b0111111111111111111111111111111111111111111111111111111111111111").equals("9223372036854775807");
		// TODO Arbitrary precision numbers
		// code("return 0b010101010101110101010101011111111110111110111110000000011101101010101001").equals("1574698668551521295017");
		//code("return -0b101010101011101010101010111111111101111101111100000000111011010101010010011111100000011111111111110000").equals("-3381639641241763826573319995376");

		section("Underscore delimiters");
		code("return 1_000_123").equals("1000123");
		code("return 1_000__123").error(Error.MULTIPLE_NUMERIC_SEPARATORS);
		code("return 0x_ff").equals("255");
		code("return 0_x_ff").error(Error.INVALID_NUMBER);
		code("return 0xff_ff_ff_ff").equals("4294967295");
		code("return 0b1001_0101_10").equals("598");
		code_v1("return 5.001_002_003").equals("5,001");
		code_v2_("return 5.001_002_003").equals("5.001002003");
		code("return _1_000_000").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);

		section("null must not be considered as 0");
		code("return null == 0;").equals("false");
		code("return null < 0;").equals("false");
		// code("null + 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("5 + null").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("5 / null").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("null / 12").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("null * 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("5 * null").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		section("Numbers with variables");
		code("var a = 2 return a++;").equals("2");
		code("var a = 2; return ++a;").equals("3");
		code("var a = 2 return a--;").equals("2");
		code("var a = 2; return --a;").equals("1");
		code("var a = 2 return a += 5;").equals("7");
		code("var a = 2 return a -= 5;").equals("-3");
		code("var a = 2 return a *= 5;").equals("10");
		code_v1("var a = 100 return a /= 5;").equals("20");
		code_v2_("var a = 100 return a /= 5;").equals("20.0");
		code("var a = 56 return a %= 17;").equals("5");
		code("var a = 15 return a **= 2;").equals("225");
		code_v1("var a = 1.5 return a * 0.5;").equals("0,75");
		code_v2_("var a = 1.5 return a * 0.5;").equals("0.75");
		// DISABLED_code("var i = 1m return i = i + 2m;").equals("3");
		code("var a = 10; a += 10 - 2 * 3; return a;").equals("14");

		section("multiple operations");
		code_v1("return (33 - 2) / 2;").equals("15,5");
		code_v2_("return (33 - 2) / 2;").equals("15.5");
		code("return 12 < (45 / 4);").equals("false");
		code("return 12 == (24 / 2);").equals("true");
		// code("2.5 + 4.7").almost(7.2);
		// DISABLED_code("return 2.5 × 4.7;").equals("11.75");
		code("return 5 * 2 + 3 * 4;").equals("22");

		section("String conversions");
		// DISABLED_code("65.char()").equals("'A'");
		// DISABLED_code("char(65)").equals("'A'");
		// DISABLED_code("126.char()").equals("'~'");
		// DISABLED_code("char(128040)").equals("'🐨'");
		// DISABLED_code("126.784.char()").equals("'~'");
		// DISABLED_code("char([126.784, 'hello'][0])").equals("'~'");
		// DISABLED_code("let c = 65 (c.char())").equals("'A'");
		// DISABLED_code("let c = 65 (c.char() + '!')").equals("'A!'");
		// DISABLED_code("0x2764.char()").equals("'❤'");

		section("Multiple precision numbers");
		// DISABLED_code("12344532132423").equals("12344532132423");
		// DISABLED_code("var a = 10m a").equals("10");
		// DISABLED_code("0m").equals("0");
		// DISABLED_code("0xf45eab5c9d13aab44376beff").equals("75628790656539575381594128127");
		// TODO floating-point multiple precision numbers
		// TODO code("123456.78910m").equals("123456.7891");
		// TODO code("123456789123456789123456789.5").equals("");
		// TODO code("1234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567891234567883459720303390827584524332795121111123456788999999999999999999999999999999999.5").equals("");
		// DISABLED_code("let a = 1209876543789765432456765432087654321 a").equals("1209876543789765432456765432087654321");
		// DISABLED_code("let a = { 1209876543789765432456765432087654321 } a").equals("1209876543789765432456765432087654321");
		// DISABLED_code("var a = 5m a = 12m").equals("12");
		// DISABLED_code("var a = 5m a = 12m a").equals("12");
		// DISABLED_code("let f = -> 12m f().string()").equals("'12'");

		section("Integer division by zero");
		// code("1 \\ 0").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// code("1 % 0").exception(ls::vm::Exception::DIVISION_BY_ZERO);

		/*
		 * Number standard library
		 */
		// header("Number standard library");
		section("Constructor");
		code_v3_("return Number").equals("<class Number>");
		// DISABLED_code("Number()").equals("0");
		// DISABLED_code("Number(12)").equals("12");
		// DISABLED_code("Number(12.5)").equals("12.5");
		// DISABLED_code("Number(12l)").equals("12");
		// DISABLED_code("Number(12m)").equals("12");
		// DISABLED_code("[Number(), 'str']").equals("[0, 'str']");
		// DISABLED_code("new Number").equals("0");
		// DISABLED_code("new Number()").equals("0");
		// DISABLED_code("new Number(12)").equals("12");
		// DISABLED_code("['', new Number()]").equals("['', 0]");
		// DISABLED_code("['', new Number]").equals("['', 0]");
		// DISABLED_code("['', Number()]").equals("['', 0]");
		// DISABLED_code("['', new Number(12)]").equals("['', 12]");
		// DISABLED_code("['', Number(12)]").equals("['', 12]");

		section("Constants");
		code_v1("return PI").equals("3,142");
		code_v2_("return PI").almost(3.141592653589793116);
		code_v1("return ['', PI]").equals("[\"\", 3,142]");
		code_v2_("return ['', PI]").equals("[\"\", 3.141592653589793]");
		code_v1("return 2 * PI").equals("6,283");
		code_v2_("return 2 * PI").almost(6.283185307179586232);
		code_v1("return E").equals("2,718");
		code_v2_("return E").almost(2.718281828459045091);
		// code("phi").almost(1.618033988749894903);
		// code("epsilon").almost(0.000000000000000222);
		// code("let pi = 3 pi").equals("3");
		// code("{ let pi = 3 } pi").almost(3.141592653589793116);
		code_v1("return Infinity").equals("∞");
		code_v2_("return Infinity").equals("Infinity");
		code("return NaN").equals("NaN");
		code("return NaN === NaN").equals("false");
		code("return 0 / 0 === NaN").equals("false");

		section("Constants in class");
		code_v3_("return Real.MIN_VALUE").equals("4.9E-324");
		code_v3_("return Real.MIN_VALUE.class").equals("<class Real>");
		code_v3_("return Real.MAX_VALUE").equals("1.7976931348623157E308");
		code_v3_("return Real.MAX_VALUE.class").equals("<class Real>");
		code_v3_("return Real.MAX_VALUE + Real.MIN_VALUE").equals("1.7976931348623157E308");
		code_v3_("return Integer.MIN_VALUE").equals("-9223372036854775808");
		code_v3_("return Integer.MIN_VALUE.class").equals("<class Integer>");
		code_v3_("return Integer.MAX_VALUE").equals("9223372036854775807");
		code_v3_("return Integer.MIN_VALUE + Integer.MAX_VALUE").equals("-1");
		code_v3_("return Integer.MAX_VALUE.class").equals("<class Integer>");
		code_v3_("Integer.MIN_VALUE = 0").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Integer.MIN_VALUE += 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Integer.MAX_VALUE = 0").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Integer.MAX_VALUE += 10").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MIN_VALUE = 0").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MIN_VALUE += 0").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE = 0").error(Error.CANNOT_ASSIGN_FINAL_FIELD);
		code_v3_("Real.MAX_VALUE += 0").error(Error.CANNOT_ASSIGN_FINAL_FIELD);

		/*
		 * Operators
		 */
		section("Number.operator unary -");
		code("var a = [12, ''] var b = a[0]; return -b;").equals("-12");
		code("return -(12 ** 2);").equals("-144");
		// DISABLED_code("return -(12m ** 2);").equals("-144");
		// DISABLED_code("-100m").equals("-100");

		section("Number.operator unary !");
		code("var a = [12, ''] var b = a[0]; return !b;").equals("false");

		section("Number.operator unary ~");
		code("var a = [12, ''] var b = a[0]; return ~b;").equals("-13");
		code("var a = 12 return ['', ~a];").equals("[\"\", -13]");

		section("Number.operator ++x");
		code("var a = 20; return ++a;").equals("21");
		code("var a = 30; ++a return a;").equals("31");
		// DISABLED_code("var a = 20m; return ++a;").equals("21");
		// DISABLED_code("var a = 20m; ++a return a;").equals("21");
		// DISABLED_code("var a = 20m; let b = ++a return b;").equals("21");
		// code("++5").error(ls::Error::Type::VALUE_MUST_BE_A_LVALUE, {"5"});
		code("var a = 5 return ['', ++a];").equals("[\"\", 6]");

		section("Number.operator --x");
		code("var a = 20; return --a;").equals("19");
		code("var a = 30; --a return a;").equals("29");
		// code("--5").error(ls::Error::Type::VALUE_MUST_BE_A_LVALUE, {"5"});
		code("var a = 5 return ['', --a];").equals("[\"\", 4]");

		section("Number.operator x++");
		code("var a = 20; return a++;").equals("20");
		code("var a = 20; a++ return a;").equals("21");
		code("var a = 20; var b = a++ return b;").equals("20");
		// DISABLED_code("var a = 20m; a++").equals("20");
		// DISABLED_code("var a = 20m; a++ a").equals("21");
		// DISABLED_code("var a = 20m; var b = a++ b").equals("20");
		// code("5++").error(ls::Error::Type::VALUE_MUST_BE_A_LVALUE, {"5"});

		section("Number.operator x--");
		code("var a = 20; return a--;").equals("20");
		code("var a = 20; a-- return a;").equals("19");
		code("var a = 20; var b = a-- return b;").equals("20");
		// DISABLED_code("var a = 20m; a--").equals("20");
		// DISABLED_code("var a = 20m; a-- a").equals("19");
		// DISABLED_code("var a = 20m; var b = a-- b").equals("20");
		// code("5--").error(ls::Error::Type::VALUE_MUST_BE_A_LVALUE, {"5"});

		section("Number.operator in");
		// TODO idea : a in b returns true if a is a divisor of b
		// code("2 in 12").error(ls::Error::Type::VALUE_MUST_BE_A_CONTAINER, {"12"});

		section("Number.operator =");
		// DISABLED_code("var a = 1m, b = 4m; a = b").equals("4");

		section("Number.operator ==");
		code("return 12 == 12;").equals("true");
		code("return 13 == 12;").equals("false");
		// DISABLED_code("12m == 12m").equals("true");
		// DISABLED_code("13m == 12m").equals("false");
		code("return 12 ** 5 == 12 ** 5;").equals("true");
		code("return 12 ** 5 == (3 * 4) ** 5;").equals("true");
		code("return 12 ** 5 == 248832;").equals("true");
		code("return 248832 == 12 ** 5;").equals("true");
		// DISABLED_code("12m ** 5m == 12m ** 5m").equals("true");
		// DISABLED_code("12m ** 5m == (3m * 4m) ** 5m").equals("true");
		// DISABLED_code("12m ** 5m == 248832").equals("true");
		// DISABLED_code("248832 == 12m ** 5m").equals("true");

		section("Number.operator +");
		code("return 1 + 2;").equals("3");
		code("return 1 + (2 + 3);").equals("6");
		code("return (1 + 2) + 3;").equals("6");
		code("return (1 + 2) + (3 + 4);").equals("10");
		// DISABLED_code("1m + 2m").equals("3");
		// DISABLED_code("1m + (2m + 3m)").equals("6");
		// DISABLED_code("(1m + 2m) + 3m").equals("6");
		// DISABLED_code("(1m + 2m) + (3m + 4m)").equals("10");
		code("return 15 + false;").equals("15");
		code("return 15 + true;").equals("16");
		code("var a = 15 return a + true;").equals("16");
		// DISABLED_code("10000m + 15").equals("10015");
		// DISABLED_code("let a = ['a', 12321111111111111111111111111111111321321321999999] a[1] + 123456789").equals("12321111111111111111111111111111111321321445456788");
		code("return 10000 + (-15);").equals("9985");
		// DISABLED_code("10000m + (-15)").equals("9985");
		code("return null + 2;").equals("2");
		code("return 2 + null;").equals("2");
		code("return null + null;").equals("0");

		section("Number.operator +=");
		code("var a = 15 a += true return a;").equals("16");
		// code("var a = 15$ a += []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = 15$ a += [] a").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		code("var a = 10 return a += 4;").equals("14");
		code("var a = 10 a += 4 return a;").equals("14");
		code("var a = 15 return ['', a += 7];").equals("[\"\", 22]");
		code("var a = 10 a += 5 return a;").equals("15");
		code("var a = 10 a += 78 return a;").equals("88");
		code("var a = 10 a += (-6) return a;").equals("4");
		// DISABLED_code("var a = 10m return a += 4m;").equals("14");
		// DISABLED_code("var a = 10m a += 4m return a;").equals("14");
		// DISABLED_code("var a = 15 return ['', a += 7];").equals("['', 22]");
		// DISABLED_code("var a = 10m a += 5 return a;").equals("15");
		// DISABLED_code("var a = 10m a += 78m return a;").equals("88");
		// DISABLED_code("var a = 10m a += (-6) return a;").equals("4");

		section("Number.operator -");
		code("return -12").equals("-12");
		code("return -0").equals("0");
		code("return -null").equals("0");
		code("return 1 - 2;").equals("-1");
		code("return 1 - (2 - 3);").equals("2");
		code("return (1 - 2) - 3;").equals("-4");
		code("return (1 - 2) - (3 - 4);").equals("0");
		code("return (10 + 10) - 1;").equals("19");
		// DISABLED_code("return 1m - 2m;").equals("-1");
		// DISABLED_code("return 1m - (2m - 3m);").equals("2");
		// DISABLED_code("return (1m - 2m) - 3m;").equals("-4");
		// DISABLED_code("return (1m - 2m) - (3m - 4m);").equals("0");
		// DISABLED_code("return (10m + 10m) - 1").equals("19");
		code("return 15 - 3;").equals("12");
		// DISABLED_code("1000m - 12").equals("988");
		// DISABLED_code("1000m - (-12)").equals("1012");
		code("return 15 - false;").equals("15");
		code("return 15 - true;").equals("14");
		code("var a = 15 return a - true;").equals("14");
		// code("12$ - []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		code("var a = 100 return a - 20;").equals("80");
		code("return null - null;").equals("0");
		code("return 12 - null;").equals("12");
		code("return null - 12;").equals("-12");

		section("Number.operator -=");
		code("var a = 15 a -= true return a;").equals("14");
		// code("var a = 15$ a -= []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = 15$ a -= [] a").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		code("var a = 15 return ['', a -= 6];").equals("[\"\", 9]");

		section("Number.operator *");
		code("return 3 * 4;").equals("12");
		code("return 10 + 3 * 4;").equals("22");
		code("return (5 + 2) * (16 * 2);").equals("224");
		// DISABLED_code("3m * 4m").equals("12");
		// DISABLED_code("10m + 3m * 4m").equals("22");
		// DISABLED_code("(5m + 2m) * (16m * 2m)").equals("224");
		code("return 12 * false;").equals("0");
		code("var a = 13; return a * false;").equals("0");
		code("return 13 * true;").equals("13");
		code("return 7 * 2;").equals("14");
		code("var a = 6; return a * 3;").equals("18");
		// code("14$ * []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// DISABLED_code("return 12344532132423 * 987657897613412;").equals("12192174652930109838844857276");
		// DISABLED_code("12344532132423m * 987657897613412m").equals("12192174652930109838844857276");
		// DISABLED_code("5 * 'yo'").equals("'yoyoyoyoyo'");
		// DISABLED_code("50m * 10").equals("500");
		// DISABLED_code("50 * 10m").equals("500");
		// DISABLED_code("let a = ['a', 12321111111111111111111111111111111321321321999999] a[1] * 123456789").equals("1521124814690000000000000000000000025951877651354934543211");
		code("return null * 2;").equals("0");
		code("return 2 * null;").equals("0");
		code("return null * null;").equals("0");

		section("Number.operator *=");
		code("var a = 15 a *= true return a;").equals("15");
		code("var a = 15 a *= false return a;").equals("0");
		code("var a = 15 a *= null return a;").equals("0");
		// code("var a = 15$ a *= []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = 15$ a *= [] a").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		code("var a = 15; return ['', a *= 2];").equals("[\"\", 30]");
		code("var a = 5 a *= 0 return a;").equals("0");
		code("var a = 5 a *= 12 return a;").equals("60");
		code("var a = 5 a *= 5 return a;").equals("25");
		code("var a = null a *= 5 return a;").equals("0");
		code("var a = null a *= null return a;").equals("0");
		code("var a = null return a *= 5").equals("0");
		code("var a = null return a *= null").equals("0");
		// DISABLED_code("var a = 5m a *= 0 a").equals("0");
		// DISABLED_code("var a = 5m a *= 12 a").equals("60");
		// DISABLED_code("var a = 5m a *= 5m a").equals("25");
		// DISABLED_code("var a = 91591785496891278315799124157189514175m a *= 157854689278315792457851475m a").equals("14458192840057923568549758280294876918394393505787702519557158125");
		// DISABLED_code("var a = 78m a *= true a").equals("78");

		section("Number.operator **");
		code("return 14 ** 3;").equals("2744");
		code("return 14 ** null;").equals("1");
		code("return null ** 2;").equals("0");
		code("return null ** null;").equals("1");
		code("return 0 ** 0;").equals("1");
		// DISABLED_code("return 14 ** true;").equals("14");
		// DISABLED_code("return 14 ** false;").equals("1");
		// DISABLED_code("let a = 14 return a ** false;").equals("1");
		// code("14$ ** []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// DISABLED_code("return 2 ** 50;").equals("1.125899906842624E15");
		// DISABLED_code("return 2l ** 50;").equals("1125899906842624");
		// DISABLED_code("257l ** 20").equals("-9223372036854775808"); // overflow
		// DISABLED_code("257m ** 20").equals("1580019571820317063568778786121273112555213952001");
		// DISABLED_code("2m ** 50").equals("1125899906842624");
		// DISABLED_code("(5m + 2m) ** (16m * 2m)").equals("1104427674243920646305299201");
		// code("123m ** 1900").exception(ls::vm::Exception::NUMBER_OVERFLOW);
		code("var s = 0 s = 5 ** 2 return s;").equals("25");

		section("Number.operator **=");
		code("var a = 5; a **= 4 return a").equals("625");
		code("var a = 5; return a **= 4").equals("625");
		code("var a = 5; return a **= true").equals("5");
		code("var a = null a **= 5 return a").equals("0");
		code("var a = null return a **= 5").equals("0");
		// DISABLED_code("var a = 5$; a **= false").equals("1");
		// code("var a = 5$; a **= []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// DISABLED_code("var a = 5; ['', a **= 4]").equals("['', 625]");

		section("Number.operator %");
		code("return 721 % 57;").equals("37");
		code("return false % 3;").equals("0");
		code("return true % 3;").equals("1");
		code("var a = 721 return a % 57;").equals("37");
		code("var a = null return a % 57;").equals("0");
		// code("let a = 721$ a % []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// DISABLED_code("721 % true").equals("0");
		// code("721$ % false").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// code("let a = 721$ a % false").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// DISABLED_code("let a = 721$ a % true").equals("0");
		// DISABLED_code("123456789123456789m % 234567m").equals("221463");
		// DISABLED_code("(12m ** 40m) % 234567m").equals("228798");
		// DISABLED_code("100000m % (12m ** 3m)").equals("1504");
		// DISABLED_code("(100000m * 10m) % (12m ** 3m)").equals("1216");
		// code("['salut', 123][0] % 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// TODO should have semantic error
		// code("['salut', 'a'][0] % 5").error(ls::Error::NO_SUCH_OPERATOR, {});

		section("Number.operator %=");
		code("var a = 721 return a %= 17;").equals("7");
		// DISABLED_code("var a = 721 a %= true").equals("0");
		// code("var a = 721$ a %= []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		// section("Number.operator %%");
		// DISABLED_code("0 %% 1").equals("0");
		// DISABLED_code("2 %% 5").equals("2");
		// DISABLED_code("(-2) %% 5").equals("3");
		// DISABLED_code("(-12) %% 5").equals("3");
		// DISABLED_code("721 %% 57").equals("37");
		// DISABLED_code("(-721) %% 57").equals("20");
		// DISABLED_code("(-721$) %% 57$").equals("20");

		// section("Number.operator %%=");
		// DISABLED_code("var a = 0 a %%= 1").equals("0");
		// DISABLED_code("var a = 2 a %%= 5").equals("2");
		// DISABLED_code("var a = -2 a %%= 5").equals("3");
		// DISABLED_code("var a = -12 a %%= 5").equals("3");
		// DISABLED_code("var a = 721 a %%= 57").equals("37");
		// DISABLED_code("var a = -721 a %%= 57").equals("20");
		// DISABLED_code("var a = -721$ a %%= 57$").equals("20");

		section("Number.operator /");
		code("8 / 0").equals("null");
		code("8 / null").equals("null");
		code("null / 5").equals("null");
		// code("12$ / false").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// code("let a = 13$; a / false").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		code_v1("return 13 / true;").equals("13");
		code_v2_("return 13 / true;").equals("13.0");
		code_v1("return 14 / 2;").equals("7");
		code_v2_("return 14 / 2;").equals("7.0");
		code_v1("var a = 18; return a / 3;").equals("6");
		code_v2_("var a = 18; return a / 3;").equals("6.0");
		// code("14$ / []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		code_v1("var a = 17, b = 5 return a / b;").equals("3,4");
		code_v2_("var a = 17, b = 5 return a / b;").equals("3.4");

		section("Number.operator /=");
		code_v1("var a = 12 a /= 3 return a;").equals("4");
		code_v2_("var a = 12 a /= 3 return a;").equals("4.0");
		code_v1("var a = 12 a /= 0.5 return a;").equals("24");
		code_v2_("var a = 12 a /= 0.5 return a;").equals("24.0");
		code_v1("var a = 12 a /= true return a;").equals("12");
		code_v2_("var a = 12 a /= true return a;").equals("12.0");
		code_v1("var a = null a /= 5 return a;").equals("0");
		code_v2_("var a = null a /= 5 return a;").equals("0.0");
		// code("var a = 12 a /= false return a;").equals("nan");
		// code("var a = 12$ a /= []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = 12$ a /= [] a").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		code_v1("var a = 15; return ['', a /= 2];").equals("[\"\", 7,5]");
		code_v2_("var a = 15; return ['', a /= 2];").equals("[\"\", 7.5]");

		section("Number.operator <");
		code("return 5 < 2;").equals("false");
		code("return 2 < 5;").equals("true");
		code("return 5.1 < 2.1;").equals("false");
		code("return 2.1 < 5.1;").equals("true");
		// code("3m < 4m").equals("true");
		// code("10m < (3m * 4m)").equals("true");
		// code("(5m + 5m) < (3m * 4m)").equals("true");
		// code("(5m + 5m) < 12m").equals("true");
		// code("3m < 4").equals("true");

		section("Number.operator <=");
		code("return 5 <= 2;").equals("false");
		code("return 2 <= 5;").equals("true");
		code("return 5.1 <= 2.1;").equals("false");
		code("return 2.1 <= 5.1;").equals("true");
		code("return 3 <= 4;").equals("true");
		code("return 3 <= [];").equals("false");

		section("Number.operator >");
		code("return 5 > 2;").equals("true");
		code("return 2 > 5;").equals("false");
		code("return 5.1 > 2.1;").equals("true");
		code("return 2.1 > 5.1;").equals("false");
		// code("12 > 5m").equals("true");
		code("return [] > true;").equals("false");
		// code("-100m > 0").equals("false");

		section("Number.operator >=");
		code("return 5 >= 2;").equals("true");
		code("return 2 >= 5;").equals("false");
		code("return 5.1 >= 2.1;").equals("true");
		code("return 2.1 >= 5.1;").equals("false");

		// section("Number.operator \\");
		// code("10 \\ 2").equals("5");
		// code("10 \\ 4").equals("2");
		// code("2432431 \\ 2313").equals("1051");
		// code("let a = 420987$ a \\ 546$").equals("771");
		// code("420987$ \\ 12").equals("35082");
		// code("12345678912345l \\ 1234").equals("10004602035");
		// code("12$ \\ false").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// code("let a = 13$; a \\ false").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// code("13$ \\ true").equals("13");
		// code("17$ \\ 4").equals("4");
		// code("let a = 10.7$; a \\ true").equals("10");
		// code("let a = 10$; a \\ 4").equals("2");
		// code("14$ \\ []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("67.89$ \\ 1").equals("67");
		// code("['', 10 \\ 2]").equals("['', 5]");
		// code("['', 10$ \\ 2]").equals("['', 5]");

		// section("Number.operator \\=");
		// code("var a = 12 a \\= 5").equals("2");
		// code("var a = 12$ a \\= 5").equals("2");
		// code("var a = 30$ a \\= 4 a").equals("7");
		// code("var a = 12$ a \\= true a").equals("12");
		// code("var a = 12$ a \\= false a").exception(ls::vm::Exception::DIVISION_BY_ZERO);
		// code("var a = 12$ a \\= []").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = 12$ a \\= [] a").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = 12 ['', a \\= 5]").equals("['', 2]");

		section("Number.operator &");
		code("return 0 & 0;").equals("0");
		code("return 1 & 0;").equals("0");
		code("return 1 & 1;").equals("1");
		code("return 5 & 12;").equals("4");
		code("return 87619 & 18431;").equals("17987");
		code("return 87619 & [18431, ''][0];").equals("17987");
		code("var a = 87619 return a &= 18431;").equals("17987");
		code("var a = 87619 a &= 18431 return a;").equals("17987");
		code("return 87619 & 18431;").equals("17987");
		// code("87619$ &= 18431").error(ls::Error::VALUE_MUST_BE_A_LVALUE, {"87619"});
		code("var a = 87619 a &= 18431 return a;").equals("17987");
		// code("[12, 'hello'][1] & 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);
		// code("var a = [12, 'hello'][1] a &= 18431 a").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		section("Number.operator |");
		code("return 0 | 0;").equals("0");
		code("return 1 | 0;").equals("1");
		code("return 1 | 1;").equals("1");
		code("return 5 | 12;").equals("13");
		code("return [5, ''][0] | [12, ''][0];").equals("13");
		code("return 87619 | 18431;").equals("88063");
		code("var a = 87619 return a |= 18431;").equals("88063");
		code("var a = 87619 a |= 18431 return a;").equals("88063");
		code("return [87619, ''][0] | 18431;").equals("88063");
		// code("87619$ |= 18431").error(ls::Error::VALUE_MUST_BE_A_LVALUE, {"87619"});
		code("var a = 87619 a |= 18431 return a;").equals("88063");
		// code("[12, 'hello'][1] | 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		section("Number.operator ^");
		code("return 0 ^ 0;").equals("0");
		code("return 1 ^ 0;").equals("1");
		code("return 1 ^ 1;").equals("0");
		code("return 5 ^ 12;").equals("9");
		code("return 87619 ^ 18431;").equals("70076");
		code("return [87619, ''][0] ^ [18431, ''][0];").equals("70076");
		code_v1("var a = 5 a ^= 2 return a;").equals("25"); // In LS 1.0, ^= was power equals
		code_v2_("var a = 87619 return a ^= 18431;").equals("70076");
		code_v2_("var a = 87619 a ^= 18431 return a;").equals("70076");
		code("return [87619, ''][0] ^ 18431;").equals("70076");
		// code("87619$ ^= 18431").error(ls::Error::VALUE_MUST_BE_A_LVALUE, {"87619"});
		code_v2_("var a = 87619 a ^= 18431 return a;").equals("70076");
		// code("[12, 'hello'][1] ^ 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		section("Number.operator <<");
		code("return 0 << 0;").equals("0");
		code("return 1 << 0;").equals("1");
		code("return 123456 << 0;").equals("123456");
		code("return 0 << 1;").equals("0");
		code("return 0 << 12;").equals("0");
		code("return 1 << 8;").equals("256");
		code("return 123 << 12;").equals("503808");
		code("return [123, ''][0] << 12;").equals("503808");
		code("var a = 123 return a <<= 11;").equals("251904");
		code("var a = 123 a <<= 13 return a;").equals("1007616");
		code("var a = [123, ''] return a[0] <<= 13;").equals("1007616");
		code("var a = 123 return ['', a <<= 13];").equals("[\"\", 1007616]");
		// code("'salut' << 5").exception(ls::vm::Exception::NO_SUCH_OPERATOR);

		section("Number.operator >>");
		code("return 0 >> 0;").equals("0");
		code("return 1 >> 0;").equals("1");
		code("return 123456 >> 0;").equals("123456");
		code("return 0 >> 1;").equals("0");
		code("return 0 >> 12;").equals("0");
		code("return 155 >> 3;").equals("19");
		code("return -155 >> 3;").equals("-20");
		code("return 12345 >> 8;").equals("48");
		code("return 123123123 >> 5;").equals("3847597");
		code("return [123123123, ''][0] >> 5;").equals("3847597");
		code("var a = 123123123 return a >>= 6;").equals("1923798");
		code("var a = 123123123 a >>= 7 return a;").equals("961899");
		code("var a = [123123123, ''] return a[0] >>= 7;").equals("961899");
		code("var a = 12345 return ['', a >>= 8];").equals("[\"\", 48]");
		// code("'salut' >> 5").error(ls::Error::NO_SUCH_OPERATOR, {env.tmp_string->to_string(), ">>", env.integer->to_string()});

		section("Number.operator >>>");
		code("return 155 >>> 3;").equals("19");
		code("return -155 >>> 3;").equals("2305843009213693932");
		code("return [-155, ''][0] >>> 3;").equals("2305843009213693932");
		code("var a = -155 return a >>>= 4;").equals("1152921504606846966");
		code("var a = -155 a >>>= 5 return a;").equals("576460752303423483");
		code("var a = [-155, ''] return a[0] >>>= 5;").equals("576460752303423483");
		code("var a = -155 return ['', a >>>= 5];").equals("[\"\", 576460752303423483]");
		// code("'salut' >>> 5").error(ls::Error::NO_SUCH_OPERATOR, {env.tmp_string->to_string(), ">>>", env.integer->to_string()});

		section("Not a statement errors");
		code("null; return null;").equals("null");
		code("(null); return null;").equals("null");
		code("((null)); return null;").equals("null");
		code("true; return null;").equals("null");
		code("false; return null;").equals("null");
		code("'salut'; return null;").equals("null");
		code("var a; a; return null;").equals("null");
		code("12; return null;").equals("null");
		code("12 && 5; return null;").equals("null");
		code("12 + 5; return null;").equals("null");
		code("12 - 5; return null;").equals("null");
		code("12 * 5; return null;").equals("null");
		code("12 / 5; return null;").equals("null");
		code("12 % 5; return null;").equals("null");
		code("12 ** 5; return null;").equals("null");
		code("12 ^ 5; return null;").equals("null");
		code("12 & 5; return null;").equals("null");
		code("12 | 5; return null;").equals("null");
		code("12 < 5; return null;").equals("null");
		code("12 > 5; return null;").equals("null");
		code("12 <= 5; return null;").equals("null");
		code("12 >= 5; return null;").equals("null");
		code("12 == 5; return null;").equals("null");
		code("12 === 5; return null;").equals("null");
		code("(12 && 5); return null;").equals("null");
		code("true ? 1 : 2; return null;").equals("null");
		code("(true ? 1 : 2); return null;").equals("null");

		/*
		* Methods
		*/
		section("Number.abs()");
		// code("return abs;").equals("<function>");
		code("return abs(-12);").equals("12");
		code_v1("return abs(-19.5);").equals("19,5");
		code_v2_("return abs(-19.5);").equals("19.5");
		code("return abs(12);").equals("12");
		// code("return abs(-16436435l)").equals("16436435");
		code_v1("return abs(-12.67);").equals("12,67");
		code_v2_("return abs(-12.67);").equals("12.67");
		code("return abs(['a', -15][1]);").equals("15");
		// code("return (-17).abs()").equals("17");
		// code("return (-19.5).abs()").equals("19.5");
		// code("return 12.abs").equals("<function>");
		// code("abs([1, 'salut'][1])").exception(ls::vm::Exception::WRONG_ARGUMENT_TYPE);
		code_v1("return abs(null)").equals("0");
		code_v2_("return abs(null)").equals("0.0");

		section("Number.exp()");
		code_v1("return exp(0)").equals("1");
		code_v2_("return exp(0)").equals("1.0");
		code_v1("return exp(1)").equals("2,718");
		code_v2_("return exp(1)").almost(Math.E);
		code_v1("return exp(4)").equals("54,598");
		code_v2_("return exp(4)").almost(54.598150033144236204);
		code_v1("return exp(4.89)").equals("132,954");
		code_v2_("return exp(4.89)").almost(132.953574051282743085);
		code_v1("return exp(-2.97)").equals("0,051");
		code_v2_("return exp(-2.97)").almost(0.051303310331919108);
		code_v1("return exp(['a', 7.78][1])").equals("2 392,275");
		code_v2_("return exp(['a', 7.78][1])").almost(2392.274820537377763685);
		// code("return 0.exp();").equals("1");
		// code("return 1.exp();").almost(Math.E);
		// code("return 7.exp();").almost(1096.633158428458500566);
		// code("return (-7).exp();").almost(0.000911881965554516);
		// code("return (-3.33).exp();").almost(0.035793105067655297);
		code_v1("return E ** 5;").equals("148,413");
		code_v2_("return E ** 5;").almost(148.413159102576571513);

		section("Number.floor()");
		code("return floor(5.9);").equals("5");
		code("var a = 5 return floor(a);").equals("5");
		code("var a = 5.4 return floor(a);").equals("5");
		code("return floor(['a', -14.7][1]);").equals("-15");
		code("return floor(5.5);").equals("5");
		code("return floor(1.897)").equals("1");
		code("return floor(3.01)").equals("3");

		section("Number.round()");
		code("return round(5.7)").equals("6");
		code("return round(5.4)").equals("5");
		code("return round(['a', -15.89][1])").equals("-16");
		code("return round(12)").equals("12");
		code("return round(-1000)").equals("-1000");
		code("return round(1.897)").equals("2");
		code("return round(3.01)").equals("3");

		section("Number.ceil()");
		code("return ceil(5.1)").equals("6");
		code("return ceil(188)").equals("188");
		code("return ceil(1.897)").equals("2");
		code("return ceil(3.01)").equals("4");

		section("Number.max()");
		code("return max(8, 5)").equals("8");
		code("return max(8, 88)").equals("88");
		code("return max(5, 12);").equals("12");
		code_v1("return max(5.0, 12);").equals("12");
		code_v2_("return max(5.0, 12);").equals("12.0");
		code_v1("return max(75.7, 12);").equals("75,7");
		code_v2_("return max(75.7, 12);").almost(75.7);
		code_v1("return max(5, 12.451);").equals("12,451");
		code_v2_("return max(5, 12.451);").almost(12.451);
		code("return max([5, 'a'][0], 4);").equals("5");
		code("return max([5, 'a'][0], 76);").equals("76");
		code("return max(4, [5, 'a'][0]);").equals("5");
		code("return max(77, [5, 'a'][0]);").equals("77");
		code("return max([55, 'a'][0], [5, 'a'][0]);").equals("55");
		code_v1("return max(5, 12.8);").equals("12,8");
		code_v2_("return max(5, 12.8);").equals("12.8");
		code_v1("return max(15.7, 12.8);").equals("15,7");
		code_v2_("return max(15.7, 12.8);").equals("15.7");
		code_v1("return max([15.7, ''][0], 12.8);").equals("15,7");
		code_v2_("return max([15.7, ''][0], 12.8);").equals("15.7");
		code_v1("return max(5.5, [12.8, ''][0]);").equals("12,8");
		code_v2_("return max(5.5, [12.8, ''][0]);").equals("12.8");
		code_v1("var a = 0.8 return max(0, a)").equals("0,8");
		code_v2_("var a = 0.8 return max(0, a)").equals("0.8");
		// code("return 2.max([7.5, ''][0]);").equals("7.5");
		// code("return [2, ''][0].max([7.5, ''][0]);").equals("7.5");
		// code("2.max([7.5, ''][1])").exception(ls::vm::Exception::WRONG_ARGUMENT_TYPE);
		// code("return max(5l, 10.5)").equals("10.5");
		// code("return max(5l, 10)").equals("10");
		// code("return max(true, 10l)").equals("10");
		// code("max('string', 12)").error(ls::Error::METHOD_NOT_FOUND, {"max(" + env.tmp_string->to_string() + ", " + env.integer->to_string() + ")"});

		section("Number.min()");
		code("return min(8, 5)").equals("5");
		code("return min(8, 88)").equals("8");
		code("return min(5, 12)").equals("5");
		code_v1("return min(75.7, 12)").equals("12");
		code_v2_("return min(75.7, 12)").almost(12.0);
		code_v1("return min(5, 12.451)").equals("5");
		code_v2_("return min(5, 12.451)").almost(5.0);
		code("return min([5, 'a'][0], 4)").equals("4");
		code("return min([5, 'a'][0], 76)").equals("5");
		code("return min(4, [5, 'a'][0])").equals("4");
		code("return min(77, [5, 'a'][0])").equals("5");
		code("return min([55, 'a'][0], [5, 'a'][0])").equals("5");
		code_v1("return min(5, 12.8)").equals("5");
		code_v2_("return min(5, 12.8)").equals("5.0");
		code_v1("return min(15.7, 12.8)").equals("12,8");
		code_v2_("return min(15.7, 12.8)").equals("12.8");
		code_v1("return min([15.7, ''][0], 12.8)").equals("12,8");
		code_v2_("return min([15.7, ''][0], 12.8)").equals("12.8");
		code_v1("return min(5.5, [12.8, ''][0])").equals("5,5");
		code_v2_("return min(5.5, [12.8, ''][0])").equals("5.5");
		// code("return min(5l, 10.5);").equals("5");
		// code("return min(5l, 10);").equals("5");
		// code("return min(true, 10l);").equals("1");
		// code("min('string', 12)").error(ls::Error::METHOD_NOT_FOUND, {"min(" + env.tmp_string->to_string() + ", " + env.integer->to_string() + ")"});

		section("Number.cos()");
		code_v1("return cos(0)").equals("1");
		code_v2_("return cos(0)").equals("1.0");
		code_v1("return cos(2.5)").equals("-0,801");
		code_v2_("return cos(2.5)").almost(Math.cos(2.5));
		code_v1("return cos(PI)").equals("-1");
		code_v2_("return cos(PI)").equals("-1.0");
		code_v1("return cos(PI / 2)").equals("0");
		code_v2_("return cos(PI / 2)").almost(0.0);
		// code("return π.cos()").equals("-1");
		// code("return ['', π][1].cos()").equals("-1");
		code_v1("return cos(['', PI][1]);").equals("-1");
		code_v2_("return cos(['', PI][1]);").equals("-1.0");
		code_v1("return cos(PI);").equals("-1");
		code_v2_("return cos(PI);").equals("-1.0");

		section("Number.acos()");
		code_v1("return acos(1)").equals("0");
		code_v2_("return acos(1)").equals("0.0");
		code_v1("return acos(-1)").equals("3,142");
		code_v2_("return acos(-1)").almost(Math.PI);
		code_v1("return acos(0)").equals("1,571");
		code_v2_("return acos(0)").almost(Math.PI / 2);
		// code("return (-0.33).acos()").almost(1.907099901948877019);
		code_v1("return acos(['y', 0][1])").equals("1,571");
		code_v2_("return acos(['y', 0][1])").almost(Math.PI / 2);

		section("Number.sin()");
		code_v1("return sin(0)").equals("0");
		code_v2_("return sin(0)").equals("0.0");
		code_v1("return sin(2.5)").equals("0,598");
		code_v2_("return sin(2.5)").almost(Math.sin(2.5));
		code_v1("return sin(PI)").equals("0");
		code_v2_("return sin(PI)").almost(0.0);
		code_v1("return sin(PI / 2)").equals("1");
		code_v2_("return sin(PI / 2)").equals("1.0");
		code_v1("return sin(- PI / 2)").equals("-1");
		code_v2_("return sin(- PI / 2)").equals("-1.0");
		code_v1("return sin(['', PI / 2][1])").equals("1");
		code_v2_("return sin(['', PI / 2][1])").equals("1.0");
		code_v1("return sin(PI / 2)").equals("1");
		code_v2_("return sin(PI / 2)").equals("1.0");

		section("Number.tan()");
		code_v1("return tan(0)").equals("0");
		code_v2_("return tan(0)").equals("0.0");
		code_v1("return tan(2.5)").equals("-0,747");
		code_v2_("return tan(2.5)").almost(Math.tan(2.5));
		code_v1("return tan(PI)").equals("-0");
		code_v2_("return tan(PI)").almost(0.0);
		code_v1("return tan(PI / 4)").equals("1");
		code_v2_("return tan(PI / 4)").almost(1.0);
		code_v1("return tan(- PI / 4)").equals("-1");
		code_v2_("return tan(- PI / 4)").almost(-1.0);
		code_v1("return tan(['', PI / 4][1])").equals("1");
		code_v2_("return tan(['', PI / 4][1])").almost(1.0);

		section("Number.asin()");
		code_v1("return asin(0)").equals("0");
		code_v2_("return asin(0)").equals("0.0");
		code_v1("return asin(-1)").equals("-1,571");
		code_v2_("return asin(-1)").almost(-Math.PI / 2);
		code_v1("return asin(1)").equals("1,571");
		code_v2_("return asin(1)").almost(Math.PI / 2);
		// code("return 0.33.asin()").almost(0.33630357515398035);
		code_v1("return asin(['y', -1][1])").equals("-1,571");
		code_v2_("return asin(['y', -1][1])").almost(-Math.PI / 2);

		section("Number.atan()");
		code_v1("return atan(1)").equals("0,785");
		code_v2_("return atan(1)").almost(Math.PI / 4);
		code_v1("return atan(-1)").equals("-0,785");
		code_v2_("return atan(-1)").almost(-Math.PI / 4);
		code_v1("return atan(0.5)").equals("0,464");
		code_v2_("return atan(0.5)").almost(0.463647609000806094);
		// code("return 0.atan()").equals("0");
		code_v1("return atan(['y', 0.5][1])").equals("0,464");
		code_v2_("return atan(['y', 0.5][1])").almost(0.463647609000806094);

		section("Number.atan2()");
		code_v1("return atan2(1, 1)").equals("0,785");
		code_v2_("return atan2(1, 1)").almost(Math.PI / 4);
		code_v1("return atan2(150.78, 150.78)").equals("0,785");
		code_v2_("return atan2(150.78, 150.78)").almost(Math.PI / 4);
		code_v1("return atan2(1, 0)").equals("1,571");
		code_v2_("return atan2(1, 0)").almost(Math.PI / 2);
		code_v1("return atan2(-1, 0)").equals("-1,571");
		code_v2_("return atan2(-1, 0)").almost(-Math.PI / 2);
		code_v1("return atan2(0, 1)").equals("0");
		code_v2_("return atan2(0, 1)").equals("0.0");
		code_v1("return atan2(0, -1)").equals("3,142");
		code_v2_("return atan2(0, -1)").almost(Math.PI);
		code_v1("return atan2(12.12, 42.42)").equals("0,278");
		code_v2_("return atan2(12.12, 42.42)").almost(0.278299659005111333);
		// code("return 1.atan2(1)").almost(Math.PI / 4);
		// code("return ['', -1][1].atan2(1)").almost(-Math.PI / 4);
		// code("return 1.atan2(['', -1][1])").almost(3 * Math.PI / 4);
		// code("return ['', -1][1].atan2(['', -1][1])").almost(-3 * Math.PI / 4);
		code_v1("return atan2(1, 1)").equals("0,785");
		code_v2_("return atan2(1, 1)").almost(Math.PI / 4);
		code_v1("return atan2(['', -1][1], 1)").equals("-0,785");
		code_v2_("return atan2(['', -1][1], 1)").almost(-Math.PI / 4);
		code_v1("return atan2(1, ['', -1][1])").equals("2,356");
		code_v2_("return atan2(1, ['', -1][1])").almost(3 * Math.PI / 4);
		code_v1("return atan2(['', -1][1], ['', -1][1])").equals("-2,356");
		code_v2_("return atan2(['', -1][1], ['', -1][1])").almost(-3 * Math.PI / 4);
		// code("return atan2(1, true)").almost(Math.PI / 4);
		// code("return atan2(true, false)").almost(Math.PI / 2);

		section("Number.cbrt()");
		code("return cbrt(125)").almost(5.0);
		code("return cbrt(1000)").almost(10.0);
		code("return cbrt(1728)").almost(12.0, 1e-14);
		// code("return 1728.cbrt()").almost(12.0, 0.00000000000001);
		code("return cbrt(['', 1728][1])").almost(12.0, 0.00000000000001);
		// code("return ['', 1728][1].cbrt()").almost(12.0, 0.00000000000001);

		// section("Number.int()");
		// code("int(15.1)").equals("15");
		// code("int(15.5)").equals("15");
		// code("int(15.9)").equals("15");

		// section("Number.isInteger()");
		// code("isInteger(12)").equals("true");
		// code("isInteger(0)").equals("true");
		// code("isInteger(-5)").equals("true");
		// code("isInteger(12.9)").equals("false");
		// code("isInteger(-5.2)").equals("false");
		// code("isInteger(π)").equals("false");
		// code("12.isInteger()").equals("true");
		// code("12.5.isInteger()").equals("false");
		// code("[12, 0][0].isInteger()").equals("true");
		// code("[12.5, 0][0].isInteger()").equals("false");

		// section("Number.fold");
		// code("1234567.fold((x, y) -> x + y, 0)").equals("28");
		// code("1234567.fold((x, y) -> x + y, 1000)").equals("1028");
		// code("1234567.fold((x, y) -> x * y, 1)").equals("5040");
		// code("1234567.fold((x, y) -> x + y ** 2, 0)").equals("140");

		section("Number.hypot");
		code_v1("return hypot(3, 4)").equals("5");
		code_v2_("return hypot(3, 4)").equals("5.0");
		// code("return 3.hypot(4)").equals("5");
		code_v1("return hypot(34, 74)").equals("81,437");
		code_v2_("return hypot(34, 74)").almost(81.437092286);
		code_v1("return hypot([34, ''][0], 74)").equals("81,437");
		code_v2_("return hypot([34, ''][0], 74)").almost(81.437092286);

		section("Number.signum");
		// code("0.signum()").equals("0");
		// code("-0.signum()").equals("0");
		// code("12.signum()").equals("1");
		// code("12.5.signum()").equals("1");
		// code("-12.signum()").equals("-1");
		// code("-12.5.signum()").equals("-1");
		code("return signum(0)").equals("0");
		code("return signum(12)").equals("1");
		code("return signum(-17)").equals("-1");
		code("return signum(-12.5)").equals("-1");
		code("return signum(85)").equals("1");

		section("Number.sqrt");
		code_v1("return sqrt(2)").equals("1,414");
		code_v2_("return sqrt(2)").almost(Math.sqrt(2));
		// code("return sqrt(123456789123456789123456789)").equals("11111111066111");
		// code("return sqrt(55m ** 20m)").equals("253295162119140625");
		// code("return sqrt(12m + 5m)").equals("4");
		// code("return var n = 12; n.sqrt()").equals("3.4641016151");
		// code("return let f = sqrt f(5)").equals("2.2360679775");
		code_v1("return sqrt(16)").equals("4");
		code_v2_("return sqrt(16)").equals("4.0");
		code_v1("return sqrt(25)").equals("5");
		code_v2_("return sqrt(25)").equals("5.0");

		section("Number.toDegrees");
		// code("return π.toDegrees()").equals("180");
		// code("return (π / 2).toDegrees()").equals("90");
		// code("return (-π / 2).toDegrees()").equals("-90");
		// code("return 0.toDegrees()").equals("0");
		code_v1("return toDegrees(PI)").equals("180");
		code_v2_("return toDegrees(PI)").equals("180.0");
		code_v1("return toDegrees(PI / 2)").equals("90");
		code_v2_("return toDegrees(PI / 2)").equals("90.0");
		code_v1("return toDegrees(-PI / 2)").equals("-90");
		code_v2_("return toDegrees(-PI / 2)").equals("-90.0");
		code_v1("return toDegrees(0)").equals("0");
		code_v2_("return toDegrees(0)").equals("0.0");

		section("Number.toRadians");
		// code("return 180.toRadians()").almost(Math.PI);
		// code("return 90.toRadians()").almost(Math.PI / 2);
		// code("return (-90).toRadians()").almost(-Math.PI / 2);
		// code("return 0.toRadians()").equals("0");
		code_v1("return toRadians(180)").equals("3,142");
		code_v2_("return toRadians(180)").almost(Math.PI);
		code_v1("return toRadians(90)").equals("1,571");
		code_v2_("return toRadians(90)").almost(Math.PI / 2);
		code_v1("return toRadians(-90)").equals("-1,571");
		code_v2_("return toRadians(-90)").almost(-Math.PI / 2);
		code_v1("return toRadians(0)").equals("0");
		code_v2_("return toRadians(0)").equals("0.0");

		section("Number.log");
		// code("1.log()").equals("0");
		code_v1("return log(1)").equals("0");
		code_v2_("return log(1)").equals("0.0");
		code_v1("return log(E)").equals("1");
		code_v2_("return log(E)").equals("1.0");
		// code("123456.log()").equals("11.7236400963");
		code_v1("return log(654321)").equals("13,391");
		code_v2_("return log(654321)").almost(13.3913533357);
		code_v1("return log([55555, ''][0])").equals("10,925");
		code_v2_("return log([55555, ''][0])").almost(10.9251288);

		section("Number.log10");
		code_v1("return log10(10)").equals("1");
		code_v2_("return log10(10)").equals("1.0");
		// code("return 1.log10()").equals("0");
		// code("return 123456.log10()").equals("5.0915122016");
		code_v1("return log10(654321)").equals("5,816");
		code_v2_("return log10(654321)").almost(5.8157908589);
		code_v1("return log10([55555, ''][0])").equals("4,745");
		code_v2_("return log10([55555, ''][0])").almost(4.7447231519);

		section("Number.pow");
		// code("2.pow(10)").equals("1024");
		code_v1("return pow(5, 3)").equals("125");
		code_v2_("return pow(5, 3)").equals("125.0");
		code_v1("return pow(2, 10)").equals("1 024");
		code_v2_("return pow(2, 10)").equals("1024.0");
		// code("pow([10, ''][0], 5)").equals("100000");
		// code("3000.pow(3)").equals("2147483648");
		// code("return pow(3000, 3)").equals("2147483648");
		// code("3000l.pow(3)").equals("27000000000");

		// section("Object-like calls");
		// code("(-12).abs()").equals("12");
		// code("π.cos()").equals("-1");
		// code("(π / 2).sin()").equals("1");
		// code("12.sqrt()").almost(3.464101615137754386);
		// code("12.8.floor()").equals("12");
		// code("-12.8.floor()").equals("-12");
		// code("(-12.8).floor()").equals("-13");
		// code("12.2.ceil()").equals("13");
		// code("12.8.round()").equals("13");
		// code("-12.8.round()").equals("-13");
		// code("2.pow(10)").equals("1024");
		// code("0.isInteger()").equals("true");
		// code("56.7.isInteger()").equals("false");
		// code("(-56.7).isInteger()").equals("false");
		// code("3.max(5)").equals("5");
		// code("5.max(3)").equals("5");

		// section("Combinated");
		// code("3.max(5).min(2)").equals("2");
		// code("3.max(5).max(10).max(12)").equals("12");
		// code("10.max(5).max(8.7).max(-3.91)").equals("10");
		// code("10.sqrt().cos()").almost(-0.99978607287932586);

		// section("Number.isPrime()");
		// code("0.isPrime()").equals("false");
		// code("1.isPrime()").equals("false");
		// code("2.isPrime()").equals("true");
		// code("3.isPrime()").equals("true");
		// code("4.isPrime()").equals("false");
		// code("5.isPrime()").equals("true");
		// code("1993.isPrime()").equals("true");
		// code("3972049.isPrime()").equals("false");
		// code("(1993l).isPrime()").equals("true");
		// code("4398042316799.isPrime()").equals("true");
		// code("(4398042316799m).isPrime() >= 1").equals("true");
		// code("359334085968622831041960188598043661065388726959079837.isPrime()").equals("1");
		// code("(146m ** 45m).isPrime()").equals("0");
		// code("1993l.isPrime()").equals("true");
		// code("1993m.isPrime()").equals("2");

		section("Number.rand()");
		code("var a = rand() return a >= 0 and a <= 1").equals("true");
		code("var a = rand() return a > 1").equals("false");
		code("var a = randInt(2067, 2070) return a >= 2067 and a < 2070").equals("true");
		code_v1_3("var a = randFloat(500, 510) return a >= 500 and a < 510").equals("true");
		code_v4_("var a = randReal(500, 510) return a >= 500 and a < 510").equals("true");

		section("Number.bitCount()");
		code_v4_("return bitCount(0)").equals("0");
		code_v4_("return bitCount(0b11001110011)").equals("7");
		code_v4_("return bitCount(0b111100111001111)").equals("11");
		code_v4_("return bitCount(0xff)").equals("8");

		section("Number.trailingZeros()");
		code_v4_("return trailingZeros(0)").equals("64");
		code_v4_("return trailingZeros(0b00001100110000)").equals("4");
		code_v4_("return trailingZeros(0b100000000000)").equals("11");
		code_v4_("return trailingZeros(0xff00)").equals("8");

		section("Number.leadingZeros()");
		code_v4_("return leadingZeros(0)").equals("64");
		code_v4_("return leadingZeros(0b0000110011)").equals("58");
		code_v4_("return leadingZeros(0b000000001)").equals("63");
		code_v4_("return leadingZeros(0b11111111111111111111111111111111111111111111111110000110011)").equals("5");
		code_v4_("return leadingZeros(0xff)").equals("56");

		section("Number.bitReverse()");
		code_v4_("return binString(bitReverse(0))").equals("\"0\"");
		code_v4_("return binString(bitReverse(0b0000110011))").equals("\"1100110000000000000000000000000000000000000000000000000000000000\"");
		code_v4_("return binString(bitReverse(0b000000001))").equals("\"1000000000000000000000000000000000000000000000000000000000000000\"");
		code_v4_("return binString(bitReverse(0b11111111111111111111111111111111111111111111111110000110011))").equals("\"1100110000111111111111111111111111111111111111111111111111100000\"");
		code_v4_("return binString(bitReverse(0xff))").equals("\"1111111100000000000000000000000000000000000000000000000000000000\"");

		section("Number.byteReverse()");
		code_v4_("return hexString(byteReverse(0))").equals("\"0\"");
		code_v4_("return hexString(byteReverse(0xaabbccddeeff))").equals("\"ffeeddccbbaa0000\"");
		code_v4_("return hexString(byteReverse(0xabcdef))").equals("\"efcdab0000000000\"");
		code_v4_("return hexString(byteReverse(0xfffaaafff))").equals("\"ffafaaff0f000000\"");

		section("Number.binString()");
		code_v4_("return binString(0)").equals("\"0\"");
		code_v4_("return binString(0b0000110011)").equals("\"110011\"");
		code_v4_("return binString(0b000000001)").equals("\"1\"");
		code_v4_("return binString(0b11001111000111001011101101110000110011)").equals("\"11001111000111001011101101110000110011\"");
		code_v4_("return binString(0xff)").equals("\"11111111\"");

		section("Number.hexString()");
		code_v4_("return hexString(0)").equals("\"0\"");
		code_v4_("return hexString(0xAABBCCDDEEFF)").equals("\"aabbccddeeff\"");
		code_v4_("return hexString(0xABCDEF00FEDCBA)").equals("\"abcdef00fedcba\"");
		code_v4_("return hexString(0xAAAAAAA0000000)").equals("\"aaaaaaa0000000\"");
		code_v4_("return hexString(0xFF)").equals("\"ff\"");

		section("Number.rotateLeft");
		code_v4_("return rotateLeft(0, 0)").equals("0");
		code_v4_("return rotateLeft(0, 5)").equals("0");
		code_v4_("return rotateLeft(0, -5)").equals("0");
		code_v4_("return rotateLeft(12345678, 10)").equals("12641974272");
		code_v4_("return rotateLeft(99999999999, 40)").equals("8568097191560746824");

		section("Number.rotateRight");
		code_v4_("return rotateRight(0, 0)").equals("0");
		code_v4_("return rotateRight(0, 5)").equals("0");
		code_v4_("return rotateRight(0, -5)").equals("0");
		code_v4_("return rotateRight(12345678, 10)").equals("6016809102166994712");
		code_v4_("return rotateRight(99999999999, 40)").equals("1677721599983222784");
		code_v4_("return rotateRight(99999999999, 40) === rotateLeft(99999999999, -40)").equals("true");

		section("Number.realBits");
		code_v4_("return realBits(0.0)").equals("0");
		code_v4_("return realBits(1.0)").equals("4607182418800017408");
		code_v4_("return realBits(-1.0)").equals("-4616189618054758400");
		code_v4_("return realBits(PI)").equals("4614256656552045848");
		code_v4_("return realBits(5.12345)").equals("4617454510305100746");
		code_v4_("return realBits(5434323.213213)").equals("4707593071093958692");

		section("Number.bitsToReal");
		code_v4_("return bitsToReal(0)").equals("0.0");
		code_v4_("return bitsToReal(4607182418800017408)").equals("1.0");
		code_v4_("return bitsToReal(-4616189618054758400)").equals("-1.0");
		code_v4_("return bitsToReal(4614256656552045848)").equals("3.141592653589793");
		code_v4_("return bitsToReal(4617454510305100746)").equals("5.12345");
		code_v4_("return bitsToReal(4707593071093958692)").equals("5434323.213213");

		section("Number.isFinite");
		code_v4_("return isFinite(0)").equals("true");
		code_v4_("return isFinite(12)").equals("true");
		code_v4_("return isFinite(42143.344324)").equals("true");
		code_v4_("return isFinite(1 / 0)").equals("false");

		section("Number.isInfinite");
		code_v4_("return isInfinite(0)").equals("false");
		code_v4_("return isInfinite(12)").equals("false");
		code_v4_("return isInfinite(42143.344324)").equals("false");
		code_v4_("return isInfinite(1 / 0)").equals("true");

		section("Number.isNaN");
		code_v4_("return isNaN(0)").equals("false");
		code_v4_("return isNaN(12)").equals("false");
		code_v4_("return isNaN(42143.344324)").equals("false");
		code_v4_("return isNaN(0 / 0)").equals("true");
		code_v4_("return isNaN(NaN)").equals("true");

		section("Number.isPermutation");
		code_v4_("return isPermutation(0, 0)").equals("true");
		code_v4_("return isPermutation(1, 0)").equals("false");
		code_v4_("return isPermutation(12345678, 51762384)").equals("true");
		code_v4_("return isPermutation(11112222, 22221111)").equals("true");
		code_v4_("return isPermutation(123456, 12345678)").equals("false");
	}
}
