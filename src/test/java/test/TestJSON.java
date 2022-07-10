package test;

public class TestJSON extends TestCommon {

	public void run() throws Exception {

		section("jsonEncode()");
		// code("jsonEncode()").error(ls::Error::Type::WRONG_ARGUMENT_COUNT, {"jsonEncode", "1", "0"});
		// integer
		code("return jsonEncode(0)").equals("\"0\"");
		code("return jsonEncode(12)").equals("\"12\"");
		code("return jsonEncode(-589)").equals("\"-589\"");
		// real
		code("return jsonEncode(54.123)").equals("\"54.123\"");
		code("return jsonEncode(-65.89)").equals("\"-65.89\"");
		// long
		// code("return jsonEncode(1l)").equals("1");
		// code("return jsonEncode(1234567890987)").equals("1234567890987");
		// mpz
		// code("return jsonEncode(1m)").equals("1");
		// code("return jsonEncode(123456789098712345678909871234567890987m)").equals("'123456789098712345678909871234567890987'");
		// code("return jsonEncode(15m ** 5)").equals("'759375'");
		// boolean
		code("return jsonEncode(true)").equals("\"true\"");
		code("return jsonEncode(false)").equals("\"false\"");
		code("return jsonEncode(12 > 5)").equals("\"true\"");
		// string
		code("return jsonEncode('')").equals("\"\"\"\"");
		code("return jsonEncode('hello')").equals("\"\"hello\"\"");
		// array
		code("return jsonEncode([])").equals("\"[]\"");
		code("return jsonEncode([1, 2, 3])").equals("\"[1,2,3]\"");
		// object
		code_v3_("return jsonEncode({})").equals("\"{}\"");
		code_v3_("return jsonEncode({a: 1, b: 2, c: 3})").equals("\"{\"a\":1,\"b\":2,\"c\":3}\"");
		code_v3_("return jsonEncode({hello: [], b: {d: 12}, ccccc: [1, 2, [], 4]})").equals("\"{\"b\":{\"d\":12},\"ccccc\":[1,2,[],4],\"hello\":[]}\"");
		// function : not transformable into JSON
		// code("return jsonEncode(x -> x)").equals("''");
		// code("return jsonEncode([1, x -> x, 3])").equals("'[1, 3]'");
		code_v4_("var m = ['20000': {L:5}, '10000': {L:5}, '50000': {L:5}] return m").equals("[\"50000\" : {L: 5}, \"20000\" : {L: 5}, \"10000\" : {L: 5}]");
		DISABLED_code_v4_("var m = ['20000':['L':5], '10000':['L':5], '50000':['L':5]] return m").equals("[\"50000\" : [\"L\" : 5], \"20000\" : <...>, \"10000\" : <...>]");
		DISABLED_code_v4_("var m = ['20000':['L':5], '10000':['L':5], '50000':['L':5]] return jsonEncode(m)").equals("{\"50000\":{\"L\":5}}");
		DISABLED_code_v4_("var m = ['20000':['L':5], '10000':['L':5], '50000':['L':5]] var o = {m: m} return jsonEncode(o)").equals("{\"m\":{\"50000\":{\"L\":5}}}");

		// section("Value.json()");
		// // null
		// code("null.json()").equals("'null'");
		// // integer
		// code("0.json()").equals("'0'");
		// code("12.json()").equals("'12'");
		// code("(-589).json()").equals("'-589'");
		// // real
		// code("54.123.json()").equals("'54.123'");
		// code("(-65.89).json()").equals("'-65.89'");
		// // long
		// code("(1l).json()").equals("'1'");
		// code("1234567890987.json()").equals("'1234567890987'");
		// // boolean
		// code("true.json()").equals("'true'");
		// code("false.json()").equals("'false'");
		// code("(12 > 5).json()").equals("'true'");
		// // string
		// code("''.json()").equals("'\"\"'");
		// code("'hello'.json()").equals("'\"hello\"'");
		// // array
		// code("[].json()").equals("'[]'");
		// code("[1, 2, 3].json()").equals("'[1, 2, 3]'");
		// code("['a', 'b', 'c'].json()").equals("'[\"a\", \"b\", \"c\"]'");
		// // set
		// code("<1, 2, 3>.json()").equals("'[1, 2, 3]'");
		// code("<9.99>.json()").equals("'[9.990000]'");
		// code("<'a', 'b', 'c'>.json()").equals("'[\"a\", \"b\", \"c\"]'");
		// // map
		// code("[1: 1].json()").equals("'{\"1\": 1}'");
		// code("['1': 1].json()").equals("'{\"1\": 1}'");
		// code("['a': 'b'].json()").equals("'{\"a\": \"b\"}'");
		// // object
		// code("{}.json()").equals("'{}'");
		// code("{a: 1, b: 2, c: 3}.json()").equals("'{\"a\":1,\"b\":2,\"c\":3}'");
		// code("{hello: [], b: {d: 12}, ccccc: [1, 2, [], 4]}.json()").equals("'{\"b\":{\"d\":12},\"ccccc\":[1, 2, [], 4],\"hello\":[]}'");
		// // class
		// code("Number.json()").equals("'\"<class Number>\"'");

		section("jsonDecode()");
		code("return jsonDecode('')").equals("null");
		code("return jsonDecode('null')").equals("null");
		code("return jsonDecode('true')").equals("true");
		code("return jsonDecode('false')").equals("false");

		code("return jsonDecode('12')").equals("12");
		code("return jsonDecode('-589')").equals("-589");
		code_v1("return jsonDecode('54.123')").equals("54,123");
		code_v2_("return jsonDecode('54.123')").equals("54.123");
		code_v1("return jsonDecode('-65.89')").equals("-65,89");
		code_v2_("return jsonDecode('-65.89')").equals("-65.89");
		code("return jsonDecode('1234567890987')").equals("1234567890987");

		code("return jsonDecode('\"\"')").equals("\"\"");
		code("return jsonDecode('\"hello\"')").equals("\"hello\"");

		code("return jsonDecode('[]')").equals("[]");
		code("return jsonDecode('[1,2,3]')").equals("[1, 2, 3]");
		code_v1("return jsonDecode('[1.6,2.1,3.77]')").equals("[1,6, 2,1, 3,77]");
		code_v2_("return jsonDecode('[1.6,2.1,3.77]')").equals("[1.6, 2.1, 3.77]");
		code("return jsonDecode('[\"a\",\"b\",\"c\"]')").equals("[\"a\", \"b\", \"c\"]");
		code("return jsonDecode('[[],[[],[]],[]]')").equals("[[], [[], []], []]");
		code_v1("return average(jsonDecode('[1, 2, 3, 4, 5]'))").equals("3");
		code_v2_("return average(jsonDecode('[1, 2, 3, 4, 5]'))").equals("3.0");

		code_v1_3("return jsonDecode('{}')").equals("[]");
		code_v1_3("return jsonDecode('{\"a\":1,\"b\":2,\"c\":3}')").equals("[\"a\" : 1, \"b\" : 2, \"c\" : 3]");
		code_v1_3("return jsonDecode('{\"c\":1,\"b\":2,\"a\":3}')").equals("[\"a\" : 3, \"b\" : 2, \"c\" : 1]");
		code_v1_3("return jsonDecode('{\"b\":{\"d\":12},\"ccccc\":[1,2,[],4],\"hello\":[]}')").equals("[\"b\" : [\"d\" : 12], \"ccccc\" : [1, 2, [], 4], \"hello\" : []]");
		code_v4_("return jsonDecode('{}')").equals("{}");
		code_v4_("return jsonDecode('{\"a\":1,\"b\":2,\"c\":3}')").equals("{a: 1, b: 2, c: 3}");
		code_v4_("return jsonDecode('{\"b\":{\"d\":12},\"ccccc\":[1,2,[],4],\"hello\":[]}')").equals("{b: {d: 12}, ccccc: [1, 2, [], 4], hello: []}");

		section("Combinations");
		code("var v = 'salut' return jsonDecode(jsonEncode(v)) == v").equals("true");
		code_v3("var v = {b: {d: 12}, cc: [[], 4], h: []} return string(jsonDecode(jsonEncode(v))) == string(v)").equals("false");
		code_v4_("var v = {b: {d: 12}, cc: [[], 4], h: []} return string(jsonDecode(jsonEncode(v))) == string(v)").equals("true");
		code("var v = 'salut' return jsonEncode(jsonEncode(v))").equals("\"\"\\\"salut\\\"\"\"");
	}
}
