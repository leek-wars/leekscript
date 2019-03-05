package test;

import leekscript.LSException;
import leekscript.compiler.LeekScript;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.functions.VariableOperations;
import leekscript.runner.LeekConstants;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.game.ErrorManager;

public class TestGeneral {

	TestAI ai;
	
	@Before
	public void init() throws Exception {
		ai = new TestAI();
	}

	@Test
	public void lsOperationsTest() throws Exception {
		VariableOperations op = new VariableOperations(JSONObject.parseObject("{\"1\":\"50\",\"10\":\"100\",\"50\":\"1000\",\"100\":\"5000\"}"));
		System.out.println(op.getOperations(501));
	}

	@Test
	public void ternaireBisTest() throws Exception {
		try {
			LeekScript.testScript("return (1&2?'coucou');", new BooleanLeekValue(false));
			Assert.fail("Compilation validée...");
		} catch (LeekCompilerException exp) {
			return;
		} catch (Exception e) {
			Assert.fail("Compilation validée...");
		}
	}

	@Test
	public void incrementTest() throws Exception {
		Assert.assertTrue(testScript("var u=0; return 1*u++ +2-2;", new IntLeekValue(0)));
	}

	@Test
	public void decrementTest() throws Exception {
		Assert.assertTrue(testScript("var u=2; return 1*u--+2-2;", new IntLeekValue(2)));
	}

	@Test
	public void preIncrementTest() throws Exception {
		Assert.assertTrue(testScript("var u=0; return 1*++u+2-2;", new IntLeekValue(1)));
	}

	@Test
	public void preDecrementTest() throws Exception {
		Assert.assertTrue(testScript("var u=2; return 1*--u+2-2;", new IntLeekValue(1)));
	}

	@Test
	public void tripleEqualsTest() throws Exception {
		Assert.assertTrue(testScript("return 1 === true;", new BooleanLeekValue(false)));
	}

	@Test
	public void ternaireTest() throws Exception {
		Assert.assertTrue(testScript("var t = true; return t === 2?6:2*4;", 8));
	}

	@Test
	public void anonymousFunctionTest() throws Exception {
		Assert.assertTrue(testScript("var test = function(@a){ a = 8; }; var test2 = function(b, d){ var c = 1; d(c); return c; }; return test2(1, test);", 8));
	}

	@Test
	public void conditionTest() throws Exception {
		Assert.assertTrue(testScript("var test = 8; if(test == 7) return 1; else if(test == 8) return 2; else return 3;", new IntLeekValue(2)));
	}

	@Test
	public void foreachTest() throws Exception {
		Assert.assertTrue(testScript("var test = [0,1,2,3,4,5]; var retour = \"\"; for(var i in test){ retour += i; } return retour;", 
				new StringLeekValue("012345")));
	}

	@Test
	public void foreachGlobalTest() throws Exception {
		Assert.assertTrue(testScript("global i; var test = [0,1,2,3,4,5]; var retour = \"\"; for(i in test){ retour += i; } return retour;", 
				new StringLeekValue("012345")));
	}

	@Test
	public void foreachkeyvalTest() throws Exception {
		Assert.assertTrue(testScript("var test = ['a':5,'b':8,'c':8,9:'p']; var retour = \"\"; for(var i : var j in test){ retour += i+':'+j; } return retour;", 
				new StringLeekValue("a:5b:8c:89:p")));
	}

	@Test
	public void foreachkeyvalGlobalTest() throws Exception {
		Assert.assertTrue(testScript("global i,j; var test = ['a':5,'b':8,'c':8,9:'p']; var retour = \"\"; for(i : j in test){ retour += i+':'+j; } return retour;",
				new StringLeekValue("a:5b:8c:89:p")));
	}

	@Test
	public void forTest() throws Exception {
		Assert.assertTrue(testScript("for(var i=0;i<1000;i++){}", new NullLeekValue()));
	}

	@Test
	public void forGlobalTest() throws Exception {
		Assert.assertTrue(testScript("global i; for(i=0;i<1000;i++){}", new NullLeekValue()));
	}

	@Test
	public void functionGlobalTest() throws Exception {
		Assert.assertTrue(testScript("function testi() { var tab=[1,5]; for (i in tab) debug(i); } global i;", new NullLeekValue()));
	}

	@Test
	public void whileTest() throws Exception {
		Assert.assertTrue(testScript("var i = 0; while(i<1000){ i++; }", new NullLeekValue()));
	}

	@Test
	public void dowhileTest() throws Exception {
		Assert.assertTrue(testScript("var i = 0; do{ i++; } while(i < 1000);", new NullLeekValue()));
	}

	// Test simples d'opérateurs
	@Test
	public void additionTest() throws Exception {
		Assert.assertTrue(testScript("var test = 1 + 8; test += 3; test++; var a = 7; var b = 1; return [test, a+b, (1+1)+9.5, 'test'+8];", 
				new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(13), new IntLeekValue(8), new DoubleLeekValue(11.5), 
						new StringLeekValue("test8") })));
	}

	@Test
	public void soustractionTest() throws Exception {
		Assert.assertTrue(testScript("var test = 20 - 8; test -= 3; test--; var a = 7; var b = 1; return [test, a-b, (1-1)-9.5];", 
				new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(8), new IntLeekValue(6), new DoubleLeekValue(-9.5) })));
	}

	@Test
	public void multiplicationTest() throws Exception {
		Assert.assertTrue(testScript("var test = 4; test *= 3; var c = 7;return [test, 8*9, 2*c];", 
				new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(12),
				new IntLeekValue(72), new DoubleLeekValue(14) })));
	}

	@Test
	public void modulusTest() throws Exception {
		Assert.assertTrue(testScript("var test = 4; test %= 3; var c = 7;return [test, 8%9, 8%c];", 
				new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(1),
				new IntLeekValue(8), new DoubleLeekValue(1) })));
	}

	@Test
	public void divisionTest() throws Exception {
		Assert.assertTrue(testScript("var test = 7; test /= 2; var c = 7;return [test, 12/6, 14/c];", 
				new ArrayLeekValue(ai, new AbstractLeekValue[] { new DoubleLeekValue(3.5),
				new IntLeekValue(2), new DoubleLeekValue(2) })));
	}

	@Test
	public void operatorsTest() throws Exception {
		AbstractLeekValue[] values = new AbstractLeekValue[] { new BooleanLeekValue(true), new BooleanLeekValue(false), 
				new BooleanLeekValue(true), new BooleanLeekValue(false),

		new BooleanLeekValue(false), new BooleanLeekValue(true), new BooleanLeekValue(false) };
		Assert.assertTrue(testScript("return [true && true, true && false, true || false, false || false, 1 > 3, 1 < 3, 4 == 7];", 
				new ArrayLeekValue(ai, values)));
	}

	@Test
	public void prioritiesTest() throws Exception {
		AbstractLeekValue[] values = new AbstractLeekValue[] {
				// 1+6*2 - 3*2
				new IntLeekValue(7),
				// 7*8 - 1
				new IntLeekValue(55),
				// 3*3 - 8 > 0
				new BooleanLeekValue(true),
				// 7*8 == 56 && 33 -8
				new BooleanLeekValue(true) };
		Assert.assertTrue(testScript("return [1+6*2-3*2,7*8-1, 3*3-8>0, 7*8 == 56 && 33-8 ];", 
				new ArrayLeekValue(ai, values)));
	}

	// Test un peu plus poussés
	@Test
	public void functionTest() throws Exception {
		Assert.assertTrue(testScript("function test(a){ return a+2; } return test(7);", 9));
	}
	
	@Test
	public void divisionByZeroTest() throws Exception {
		Assert.assertTrue(testScript("return 8/0;", null));
		Assert.assertTrue(testScript("return 8/null;", null));
	}
	
	@Test
	public void whileReturnTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("var t = 0; while(t<5){ t++; return t;}", new IntLeekValue(1)));
		Assert.assertTrue(LeekScript.testScript("var t = 0; while(t<5){ t++; return t;} return 0;", new IntLeekValue(1)));
	}

	@Test
	public void doWhileReturnTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("var t = 0; do{ t++; return t;}while(t<5);", new IntLeekValue(1)));
		try {
			LeekScript.testScript("var t = 0; do{ t++; return t;}while(t<5); return 2;", new IntLeekValue(1));
			Assert.fail("Compilation validée...");
		} catch (LeekCompilerException e) {
			return;
		} catch (Exception e) {
			Assert.fail("Compilation validée...");
		}
	}

	@Test
	public void forReturnTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("for(var i=0;i<3;i++){ return i; }", new IntLeekValue(0)));
		Assert.assertTrue(LeekScript.testScript("for(var i=0;i<3;i++){ return i; } return 2;", new IntLeekValue(0)));
	}

	@Test
	public void anonymousTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("function te(a){ return function(){ return a**2; }; } return te(2)();", new IntLeekValue(4)));
	}

	@Test
	public void anonymous2Test() throws Exception {
		Assert.assertTrue(LeekScript.testScript("function te(a){ return function(b){ return function(c){return a*b*c;}; }; } return te(2)(1)(2);", new IntLeekValue(4)));
	}

	@Test
	public void anonymous3Test() throws Exception {
		Assert.assertTrue(LeekScript.testScript("var tab = [2,3,4,5,6];var r = [];for(var i: var j in tab){	r[i] = function(){ return j; };}return 4;", new IntLeekValue(4)));
	}

	@Test
	public void conditionalTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("var test = 0; if(false) if(true) test = 3; else test = 1; return test;", new IntLeekValue(0)));
	}

	@Test
	public void forEachReturnTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("var tab = [0,1,2,3]; for(var i in tab){ return i; }", new IntLeekValue(0)));
		Assert.assertTrue(LeekScript.testScript("var tab = [0,1,2,3]; for(var i in tab){ return i; } return 5;", new IntLeekValue(0)));
	}

	@Test
	public void forEachKeyReturnTest() throws Exception {
		Assert.assertTrue(LeekScript.testScript("var tab = [1:0,2:1,3:2,4:3]; for(var i : var j in tab){ return i; } ", new IntLeekValue(1)));
		Assert.assertTrue(LeekScript.testScript("var tab = [1:0,2:1,3:2,4:3]; for(var i : var j in tab){ return i; } return 0;", new IntLeekValue(1)));
	}
	

	@Test
	public void arrayMapTest() throws Exception {

		Assert.assertTrue(LeekScript.testScript("return arrayMap([1,2,3,4,5],function(e){ return e*2; });", new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(2), new IntLeekValue(4), new IntLeekValue(6), new IntLeekValue(8), new IntLeekValue(10) })));

		Assert.assertTrue(LeekScript.testScript("return arrayMap([4,9,16],sqrt);", new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(2), new IntLeekValue(3), new IntLeekValue(4) })));

		Assert.assertTrue(LeekScript.testScript("return arrayMap(['a':1,'b':2],function(k,v){ return k+v;});", new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new StringLeekValue("a"), new StringLeekValue("a1"),
				new StringLeekValue("b"), new StringLeekValue("b2") }, true)));

		Assert.assertTrue(LeekScript.testScript("return function(){ var t = ['a':1,'b':2]; arrayMap(t,function(@k,@v){ v='tomate';k='ctus'; return 3;}); return t;}();", new ArrayLeekValue(ai, new AbstractLeekValue[] { new StringLeekValue("a"),
						new StringLeekValue("tomate"),
						new StringLeekValue("b"),
						new StringLeekValue("tomate") }, true)));
	}
	

	@Test
	public void arrayFilterTest() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayFilter([1,2,3,4,5,6,7,8,9],function(e){ return e>5; })");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(5), new IntLeekValue(6), new IntLeekValue(6),
				new IntLeekValue(7), new IntLeekValue(7),
				new IntLeekValue(8), new IntLeekValue(8), new IntLeekValue(9) }, true));

		codes.add("arrayFilter([4,5,6,'test',8,9],function(e){ return e=='test'; })");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(3), new StringLeekValue("test") }, true));

		codes.add("string(arrayFilter(['a','b','c','d'],function(k,v){ return k==3; }))");
		values.add("[3 : d]");

		codes.add("string(function(){ var t = ['a','b','c','d']; arrayFilter(t,function(k,@v){ v=4; return k==3; }); return t;}())");
		values.add("[4, 4, 4, 4]");

		codes.add("string(arrayFilter(['a','b','c','d'],function(k,@v){ v=4; return k==3; }))");
		values.add("[3 : 4]");

		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayFlatten() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayFlatten([6,7,[8,9]],99)");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(6), new IntLeekValue(7), new IntLeekValue(8), new IntLeekValue(9) }));

		codes.add("arrayFlatten([6,[[7]],[8,9]],2)");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(6), new IntLeekValue(7), new IntLeekValue(8), new IntLeekValue(9) }));

		codes.add("arrayFlatten([6,[[7]],[8,9]])");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(6), new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(7) }),
				new IntLeekValue(8), new IntLeekValue(9) }));

		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayFoldLeft() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayFoldLeft([6,7,8,9], function(a,b){return a+b;},0)");
		values.add(30);

		codes.add("arrayFoldLeft([1,0,1,2,5,7,9], function(a,b){return a+','+b;},'')");
		values.add(",1,0,1,2,5,7,9");
	
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayFoldRight() throws Exception {

		// On lance le test
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayFoldRight([6,7,8,9], function(a,b){return a+b;},0)");
		values.add(30);

		codes.add("arrayFoldRight([1,0,1,2,5,7,9], function(a,b){return a+','+b;},'')");
		values.add("1,0,1,2,5,7,9,");

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayPartition() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayPartition([6,7,8,9], function(a){return a&1;})");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(1), new IntLeekValue(7), new IntLeekValue(3), new IntLeekValue(9) }, true),
				new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0), new IntLeekValue(6), new IntLeekValue(2), new IntLeekValue(8) }, true) }));

		codes.add("string(arrayPartition([6,7,8,9], function(k,v){return k;}))");
		values.add("[[1 : 7, 2 : 8, 3 : 9], [6]]");

		codes.add("string(arrayPartition([4,3,2,1], function(k,v){return k<v;}))");
		values.add("[[4, 3], [2 : 2, 3 : 1]]");

		codes.add("string(function(){var t=[1,2,3]; arrayPartition(t, function(@v){ v=3; }); return t;}())");
		values.add("[3, 3, 3]");

		codes.add("string(function(){var t=[1,2,3]; arrayPartition(t, function(k, @v){ v=3; }); return t;}())");
		values.add("[3, 3, 3]");

		codes.add("string(arrayPartition([4,3,2,1], function(k,@v){ v=3; return k<v;}))");
		values.add("[[3, 3, 3], [3 : 3]]");

		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayConcat() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("string([0]+[1,2])");
		values.add("[0, 1, 2]");

		codes.add("function(){ var a = [0,1]; a+= [3]; return string(a);}()");
		values.add("[0, 1, 3]");

		codes.add("string(arrayConcat([0],[1,2]))");
		values.add("[0, 1, 2]");

		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayIter() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("string(function(){ var t = [1,2,3,4]; arrayIter(t, function(v){ v=2; }); return t; }())");
		values.add("[1, 2, 3, 4]");

		codes.add("string(function(){ var t = [1,2,3,4]; arrayIter(t, function(@v){ v=2; }); return t; }())");
		values.add("[2, 2, 2, 2]");

		codes.add("string(function(){ var t = [1,2,3,4]; arrayIter(t, function(k, @v){ v=k; }); return t; }())");
		values.add("[0, 1, 2, 3]");

		codes.add("string(function(){ var t = [1,2,3,4]; arrayIter(t, function(k, v){ v=k; }); return t; }())");
		values.add("[1, 2, 3, 4]");

		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arraySort() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("string(function(){var t = [0,1,2]; return arraySort(t,function(e, f){return (e>f)?(-1):(e<f)?1:0;});}())");
		values.add("[2, 1, 0]");

		codes.add("string(function(){var t = [2:0,1:1,0:2]; return arraySort(t,function(k1, v1, k2, v2){return (k1>k2)?(-1):(k1<k2)?1:0;});}())");
		values.add("[2 : 0, 1 : 1, 0 : 2]");

		codes.add("string(function(){var t = ['test','t']; return arraySort(t,function(k1, v1, k2, v2){return (k1>k2)?(-1):(k1<k2)?1:0;});}())");
		values.add("[t, test]");

		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	@Ignore
	public void redefineFunctionTest() throws Exception {

		String leekscript = "var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);";

		Assert.assertTrue(LeekScript.testScript(leekscript, new StringLeekValue("[2, 1, 2, 1, 2]")));
	}
	


	@Test
	public void testReference() throws Exception {
		String leekscript = "var t = [3,4,5]; t[3]=[1,2,3,4]; var r = @t[3]; r[4] ='prou3t'; return t;";
		LeekScript.testScript(leekscript, new NullLeekValue());
	}

	@Test
	public void testAnonymousFunctioNSelfAccess() {
		String leekscript = "var t = function(){ return t; };";
		try {
			LeekScript.testScript(leekscript, new NullLeekValue());
		} catch (Exception e) {

			ErrorManager.exception(e);
		}
	}

	@Test
	public void testIfIfNot() {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("function(){ var a = 1; if(a is 1) return 2; else return 0;}()");
		values.add(2);

		codes.add("function(){ var a = 1; if(a is 2) return 2; else return 0;}()");
		values.add(0);

		codes.add("function(){ var a = 1; if(a is not 2) return 2; else return 0;}()");
		values.add(2);

		codes.add("function(){ var a = 1; if(a is not 1) return 2; else return 0;}()");
		values.add(0);

		codes.add("function(){ var a = true; if(not a) return 2; else return 0;}()");
		values.add(0);

		// Test AI
		try {
			Assert.assertTrue(testAI(codes, values));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testOperators() {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("function(){ var a = 1; var result = -10 + (1- (a-1)); return result;}()");
		values.add(-9);

		codes.add("function(){ var a = 1; var result = 0; result = -10 + (1- (a-1)); return result;}()");
		values.add(-9);
		// Test AI
		try {
			Assert.assertTrue(testAI(codes, values));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void sortTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test sort
		codes.add("function(){ var t =[null,null,4,8,9]; sort(t); return t;}()");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(4), new IntLeekValue(8), new IntLeekValue(9), new NullLeekValue(), new NullLeekValue() }));

		// Test sort
		codes.add("function(){ var t =[4, null, 4, null, 4]; sort(t); return t;}()");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(4), new IntLeekValue(4), new IntLeekValue(4), new NullLeekValue(), new NullLeekValue() }));

		// Test sort desc
		codes.add("function(){ var t =[4, null, 5, null, 8]; sort(t,SORT_DESC); return t;}()");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] { new NullLeekValue(), new NullLeekValue(), new IntLeekValue(8), new IntLeekValue(5), new IntLeekValue(4) }));

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}
	
	@Test
	public void colorTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test color
		codes.add("color(255,0,255)");
		values.add(0xFF00FF);
		// Test color
		codes.add("color(255,255,0)");
		values.add(0xFFFF00);
		// Test color
		codes.add("color(0,255,255)");
		values.add(0x00FFFF);

		// Red
		codes.add("getRed(" + 0xAE0000 + ")");
		values.add(174);
		// Green
		codes.add("getGreen(" + 0xAF00 + ")");
		values.add(175);
		// Blue
		codes.add("getBlue(" + 0xAD + ")");
		values.add(173);

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}
	

	@Test
	public void typeOfTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test nombre
		codes.add("typeOf(255)");
		values.add(LeekConstants.TYPE_NUMBER);
		codes.add("typeOf(255.8)");
		values.add(LeekConstants.TYPE_NUMBER);
		// Test string
		codes.add("typeOf('coucou')");
		values.add(LeekConstants.TYPE_STRING);
		// Test boolean
		codes.add("typeOf(false)");
		values.add(LeekConstants.TYPE_BOOLEAN);
		// Test array
		codes.add("typeOf([1,false])");
		values.add(LeekConstants.TYPE_ARRAY);
		// Test fonction
		codes.add("typeOf(function(){ return null; })");
		values.add(LeekConstants.TYPE_FUNCTION);
		// Test null
		codes.add("typeOf(null)");
		values.add(LeekConstants.TYPE_NULL);
		// Test piège
		codes.add("typeOf(function(){ return 4; }())");
		values.add(LeekConstants.TYPE_NUMBER);

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void stringTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test nombre
		codes.add("'\\\\'");
		values.add("\\\\");

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	private boolean testAI(List<String> mCodes, List<Object> mValues) throws Exception {
		String leekscript = "return [";
		AbstractLeekValue[] values = new AbstractLeekValue[mValues.size()];

		for (int i = 0; i < mValues.size(); i++) {
			if (i != 0)
				leekscript += ",";
			leekscript += mCodes.get(i);
			Object c = mValues.get(i);
			if (c instanceof Integer)
				values[i] = new IntLeekValue(((Integer) mValues.get(i)));
			else if (c instanceof Double)
				values[i] = new DoubleLeekValue(((Double) mValues.get(i)));
			else if (c instanceof String)
				values[i] = new StringLeekValue(((String) mValues.get(i)));
			else if (c instanceof Boolean)
				values[i] = new BooleanLeekValue(((Boolean) mValues.get(i)));
			else if (c instanceof AbstractLeekValue)
				values[i] = (AbstractLeekValue) mValues.get(i);
			else
				values[i] = new NullLeekValue();
		}

		leekscript += "];";
		try {
			return LeekScript.testScript(leekscript, new ArrayLeekValue(ai, values));
		} catch (LSException e) {
			System.err.println("Erreur :\n" + leekscript);
			System.err.println("Valeur attendue :\n" + e.getThe().getString(ai));
			System.err.println("Valeur renvoyée :\n" + e.getRun().getString(ai));
			return false;
		}
	}

	private boolean testScript(String leekscript, AbstractLeekValue value) throws Exception {
		if (value == null)
			value = new NullLeekValue();
		
		return LeekScript.testScript(leekscript, value);
	}

	private boolean testScript(String leekscript, int value) throws Exception {
		return testScript(leekscript, new IntLeekValue(value));
	}
}
