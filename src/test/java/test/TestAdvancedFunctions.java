package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import leekscript.LSException;
import leekscript.compiler.LeekScript;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

public class TestAdvancedFunctions {

	private AI ai;
	
	@Before
	public void init() throws Exception {
		ai = new TestAI();
	}

	// Test de retour sur des structures du leekscript
	@Test
	public void whileReturnTest() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("var t = 0; while(t<5){ t++; return t;}", new IntLeekValue(1)));
		Assert.assertTrue(LeekScript.testScript("var t = 0; while(t<5){ t++; return t;} return 0;", new IntLeekValue(1)));
	}

	@Test
	public void doWhileReturnTest() throws Exception {
		// Test AI
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
		// Test AI
		Assert.assertTrue(LeekScript.testScript("for(var i=0;i<3;i++){ return i; }", new IntLeekValue(0)));
		Assert.assertTrue(LeekScript.testScript("for(var i=0;i<3;i++){ return i; } return 2;", new IntLeekValue(0)));
	}

	@Test
	public void anonymousTest() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("function te(a){ return function(){ return a**2; }; } return te(2)();", new IntLeekValue(4)));
	}

	@Test
	public void anonymous2Test() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("function te(a){ return function(b){ return function(c){return a*b*c;}; }; } return te(2)(1)(2);", new IntLeekValue(4)));
	}

	@Test
	public void anonymous3Test() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("var tab = [2,3,4,5,6];var r = [];for(var i: var j in tab){	r[i] = function(){ return j; };}return 4;", new IntLeekValue(4)));
	}

	@Test
	public void conditionalTest() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("var test = 0; if(false) if(true) test = 3; else test = 1; return test;", new IntLeekValue(0)));
	}

	@Test
	public void forEachReturnTest() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("var tab = [0,1,2,3]; for(var i in tab){ return i; }", new IntLeekValue(0)));
		Assert.assertTrue(LeekScript.testScript("var tab = [0,1,2,3]; for(var i in tab){ return i; } return 5;", new IntLeekValue(0)));
	}

	@Test
	public void forEachKeyReturnTest() throws Exception {
		// Test AI
		Assert.assertTrue(LeekScript.testScript("var tab = [1:0,2:1,3:2,4:3]; for(var i : var j in tab){ return i; } ", new IntLeekValue(1)));
		Assert.assertTrue(LeekScript.testScript("var tab = [1:0,2:1,3:2,4:3]; for(var i : var j in tab){ return i; } return 0;", new IntLeekValue(1)));
	}

	@Test
	public void divisionByZeroTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test nombre
		codes.add("8/0");
		values.add(null);

		// Test nombre
		codes.add("8/null");
		values.add(null);

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}




	@Test
	public void arrayMapTest() throws Exception {

		// On lance le test
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayMap([1,2,3,4,5],function(e){ return e*2; })");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(2), new IntLeekValue(4), new IntLeekValue(6), new IntLeekValue(8), new IntLeekValue(10) }));

		codes.add("arrayMap([4,9,16],sqrt)");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new IntLeekValue(2), new IntLeekValue(3), new IntLeekValue(4) }));

		codes.add("arrayMap(['a':1,'b':2],function(k,v){ return k+v;})");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] {
				new StringLeekValue("a"), new StringLeekValue("a1"),
				new StringLeekValue("b"), new StringLeekValue("b2") }, true));

		codes.add("function(){ var t = ['a':1,'b':2]; arrayMap(t,function(@k,@v){ v='tomate';k='ctus'; return 3;}); return t;}()");
		values.add(new ArrayLeekValue(ai, new AbstractLeekValue[] { new StringLeekValue("a"),
						new StringLeekValue("tomate"),
						new StringLeekValue("b"),
						new StringLeekValue("tomate") }, true));

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayFilterTest() throws Exception {

		// On lance le test
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

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayFlatten() throws Exception {

		// On lance le test
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

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayFoldLeft() throws Exception {

		// On lance le test
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("arrayFoldLeft([6,7,8,9], function(a,b){return a+b;},0)");
		values.add(30);

		codes.add("arrayFoldLeft([1,0,1,2,5,7,9], function(a,b){return a+','+b;},'')");
		values.add(",1,0,1,2,5,7,9");
		// Test AI
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

		// On lance le test
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

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayConcat() throws Exception {

		// On lance le test
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("string([0]+[1,2])");
		values.add("[0, 1, 2]");

		codes.add("function(){ var a = [0,1]; a+= [3]; return string(a);}()");
		values.add("[0, 1, 3]");

		codes.add("string(arrayConcat([0],[1,2]))");
		values.add("[0, 1, 2]");

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arrayIter() throws Exception {

		// On lance le test
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

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void arraySort() throws Exception {

		// On lance le test
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// codes.add("string(arraySort([0,1,2,3], function(e, f){return
		// e>f;}))");
		codes.add("string(function(){var t = [0,1,2]; return arraySort(t,function(e, f){return (e>f)?(-1):(e<f)?1:0;});}())");
		values.add("[2, 1, 0]");

		codes.add("string(function(){var t = [2:0,1:1,0:2]; return arraySort(t,function(k1, v1, k2, v2){return (k1>k2)?(-1):(k1<k2)?1:0;});}())");
		values.add("[2 : 0, 1 : 1, 0 : 2]");

		codes.add("string(function(){var t = ['test','t']; return arraySort(t,function(k1, v1, k2, v2){return (k1>k2)?(-1):(k1<k2)?1:0;});}())");
		values.add("[t, test]");

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	@Ignore
	public void redefineFunctionTest() throws Exception {

		String leekscript = "var retour = [];for(var i=0;i<5;i++){if(i&1){var sqrt=function(e){return 1;}; push(retour, sqrt(4));}else{push(retour, sqrt(4));}}return string(retour);";

		// Test AI
		Assert.assertTrue(LeekScript.testScript(leekscript, new StringLeekValue("[2, 1, 2, 1, 2]")));
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
			int i = e.getIndex();
			System.err.println("Erreur :\n" + mCodes.get(i));
			System.err.println("Valeur attendue :\n" + e.getThe().getString(ai));
			System.err.println("Valeur renvoyée :\n" + e.getRun().getString(ai));
			return false;
		}
	}
}
