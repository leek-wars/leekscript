package test;

public class TestOperators extends TestCommon {

	public void run() throws Exception {

		section("Operator ==");
		code("return null == null").equals("true");

		code("return false == false").equals("true");
		code("return true == true").equals("true");
		code("return false == true").equals("false");
		code("return true == false").equals("false");
		code("return true == 'true'").equals("true");
		code("return false == 'false'").equals("true");
		code("return true == 'false'").equals("false");
		code("return false == 'true'").equals("false");
		code("return false == []").equals("true");
		code("return false == 0").equals("true");
		code("return true == 1").equals("true");
		code("return false != 0").equals("false");
		code("return true != 1").equals("false");
		code("return false == ''").equals("true");
		code("return false == '0'").equals("true");
		code("return false == []").equals("true");
		code("return false == [0]").equals("true");
		code("return true == 12").equals("true");
		code("return true == '1'").equals("true");
		code("return true == '12'").equals("true");
		code("return true == 'lama'").equals("true");
		code("return true == [1]").equals("true");
		code("return true == [12]").equals("true");
		code("return true == [1, 2, 3]").equals("true");

		code("return 0 == false").equals("true");
		code("return 0 == 0").equals("true");
		code("return 0 == ''").equals("true");
		code("return 0 == '0'").equals("true");
		code("return 0 == 'false'").equals("true");
		code("return 0 == []").equals("true");
		code("return 0 == [0]").equals("true");
		code("return 0 != null").equals("true");

		code("return 1 == true").equals("true");
		code("return 1 == '1'").equals("true");
		code("return 1 == 'true'").equals("true");
		code("return 1 == 'lama'").equals("true");
		code("return 1 == [1]").equals("true");
		code("return 1 == 2").equals("false");

		code("return 12 == true").equals("true");
		code("return -1 == -5").equals("false");
		code("return 50 == 50").equals("true");
		code("return 5 == 5").equals("true");
		code("return 45 == 5").equals("false");
		code("return 10 == '10'").equals("true");
		code("return 10 == '10'").equals("true");
		code("return 10 == '15'").equals("false");
		code("return 10 == '15'").equals("false");
		code("return 10.8 == '10.8'").equals("true");
		code("return 10.8 == '10.87'").equals("false");
		code("return 12 == 'true'").equals("true");
		code("return 2 == 'false'").equals("false");
		code("return 12 == [12]").equals("true");

		code("return 'Chaine1' == 'Chaine1'").equals("true");
		code("return 'Chaine1' == 'Chaine2'").equals("false");
		code("return '1' == 1").equals("true");
		code("return '0' == 0").equals("true");
		code("return '10' == 10").equals("true");
		code("return '15' == 10").equals("false");

		code("return [] == []").equals("true");
		code("return [0] == [0]").equals("true");
		code("return [0, 1] == [0, 1]").equals("true");
		code("return [0, 1] == [0]").equals("false");
		code("return ['Chaine1'] == ['Chaine2']").equals("false");
		code("return ['Chaine1'] == ['Chaine1']").equals("true");

		code("return function() {} == function() {}").equals("false");
		code("return endsWith == function() {}").equals("false");
		code("return endsWith == endsWith").equals("true");

		String[] values1 = new String[] { "false", "true", "0", "1", "12", "''", "'0'", "'1'", "'12'", "'lama'", "'true'", "'false'",
			"[]", "[0]", "[1]", "[12]", "[1,2,3]", "null" };
		String[] equalEqual = new String[] {
			"X X  XX    XXX    ", // false
			" X XX  XXXX   XXX ", // true
			"X X  XX    XXX    ", // 0
			" X X   X XX   X   ", // 1
			" X  X   X X    X  ", // 12
			"X X  X      XX    ", // " "
			"X X   X      X    ", // "0"
			" X X   X      X   ", // "1"
			" X  X   X      X  ", // "12"
			" X X     X    X   ", // "lama"
			" X XX     X   XXX ", // "true"
			"X X        XXX    ", // "false"
			"X X  X     XX     ", // []
			"X X  XX    X X    ", // [0]
			" X X   X XX   X   ", // [1]
			" X  X   X X    X  ", // [12]
			" X        X     X ", // [1,2,3]
			"                 X", // null
		};

		for (int i = 0; i < values1.length; i++) {
			for (int j = 0; j < values1.length; j++) {
				code("return " + values1[i] + " == " + values1[j]).equals(String.valueOf(equalEqual[i].charAt(j) == 'X'));
			}
		}

		section("Other operators");
		code("var sum = 1, ops = 10 return sum < ops * 0.95 || sum > ops").equals("true");
		code("var sum = 9.8, ops = 10 return sum < ops * 0.95 || sum > ops").equals("false");
		code("var sum = 98 var ops = 100 return sum < ops * 0.95 || sum > ops").equals("false");
		code("var sum = 98 var ops = 100 if (sum < ops * 0.95 || sum > ops) {}").equals("null");
		code("var sum = 1 var ops = 10 if (sum < ops * 0.95 || sum > ops) {} return null").equals("null");
		code("var a = [] if (a != []) {}").equals("null");
		code("return !null == 50").equals("true");

		section("Operator ===");
		Object[] values = new Object[] { "0", "1",
			"12", "13", "false",
			"true", "null", "'true'",
			"'false'", "'12'",
			"'lama'",
			"[]", "['12']" };

		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values.length; j++) {
				code("return " + values[i] + " === " + values[j]).equals(String.valueOf(i == j));
			}
		}
		code("return 1 === 1.0").equals("true");
		code("return 12 === 12.0").equals("true");

		code("var a = 1; var result = -10 + (1- (a-1)); return result").equals("-9");
		code("var a = 1; var result = 0; result = -10 + (1- (a-1)); return result").equals("-9");

		code("return null < 3").equals("true");
		code("var a = null return a < 3").equals("true");
		code("return true < 10").equals("true");
		code("return false < 10").equals("true");
		code("return 10 < true").equals("false");
		code("return 10 < false").equals("false");
		code("return 10 > true").equals("true");
		code("return 10 > false").equals("true");
		code("return true > 10").equals("false");
		code("return false > 10").equals("false");

		code("var a = 20 if (15 > a > 11) { return true } return false").equals("false");
		code("return 15 > 14 > 11 and 150 < 200 < 250").equals("false");
		code("return 15 > 10 > 11 and 150 < 200 < 250").equals("false");
		code("return 15 > 14 > 11 and 150 < 100 < 250").equals("false");
		code("return 15 > 10 > 11 and 150 < 100 < 250").equals("false");

		section("Operator +");
		code("return false + 1").equals("1");
		code("return 1 + false").equals("1");
		code("return true + 1").equals("2");
		code("return 1 + true").equals("2");
		code("return true + null").equals("1");
		code("return null + true").equals("1");
		code("return false + null").equals("0");
		code("return null + false").equals("0");
	}
}
