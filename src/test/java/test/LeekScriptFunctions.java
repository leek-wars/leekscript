package test;

import java.util.ArrayList;
import java.util.List;

import leekscript.ErrorManager;
import leekscript.LSException;
import leekscript.compiler.LeekScriptCompilation;
import leekscript.runner.AI;
import leekscript.runner.LeekConstants;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LeekScriptFunctions {
	
	private AI ai;
	
	@Before
	public void init() throws Exception {
		ai = new DefaultUserAI();
	}

	@Test
	public void MathFunctionsTest() throws Exception {

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test abs
		codes.add("abs(-5)");
		values.add(5);
		codes.add("abs(8)");
		values.add(8);

		// Test min
		codes.add("min(8,5)");
		values.add(5);
		codes.add("min(8,88)");
		values.add(8);

		// Test max
		codes.add("max(8,5)");
		values.add(8);
		codes.add("max(8,88)");
		values.add(88);

		// Test cos
		codes.add("cos(0)");
		values.add(1);
		codes.add("cos(2.5)");
		values.add(Math.cos(2.5));

		// Test sin
		codes.add("sin(0)");
		values.add(0);
		codes.add("sin(2.5)");
		values.add(Math.sin(2.5));

		// Test tan
		codes.add("tan(0)");
		values.add(0);
		codes.add("tan(2.5)");
		values.add(Math.tan(2.5));

		// Test toRadians
		codes.add("toRadians(180)");
		values.add(Math.PI);

		// Test toDegrees
		codes.add("toDegrees(PI)");
		values.add(180);

		// Test acos
		codes.add("acos(1)");
		values.add(0);

		// Test asin
		codes.add("asin(0)");
		values.add(0);

		// Test atan
		codes.add("atan(0)");
		values.add(0);

		// Test ceil
		codes.add("ceil(1.897)");
		values.add(2);
		codes.add("ceil(3.01)");
		values.add(4);

		// Test floor
		codes.add("floor(1.897)");
		values.add(1);
		codes.add("floor(3.01)");
		values.add(3);

		// Test round
		codes.add("round(1.897)");
		values.add(2);
		codes.add("round(3.01)");
		values.add(3);

		// Test sqrt
		codes.add("sqrt(16)");
		values.add(4);
		codes.add("sqrt(25)");
		values.add(5);

		// Test cqrt
		codes.add("cbrt(1000)");
		values.add(10);
		codes.add("cbrt(125)");
		values.add(5);

		// Test log
		codes.add("log(1)");
		values.add(0);
		codes.add("log(E)");
		values.add(1);

		// Test log10
		codes.add("log10(10)");
		values.add(1);

		// Test exp
		codes.add("exp(1)");
		values.add(Math.E);

		// Test pow
		codes.add("pow(5,3)");
		values.add(125);

		// Test signum
		codes.add("signum(85)");
		values.add(1);
		codes.add("signum(-0.5)");
		values.add(-1);

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void StringFunctionsTest() throws Exception {
		// Test AI

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test charAt
		codes.add("charAt('bonjour', 1)");
		values.add("o");

		// Test length
		codes.add("length('bonjour')");
		values.add(7);

		// Test substring
		codes.add("substring('bonjour',2,3)");
		values.add("njo");

		// Test replace
		codes.add("replace('bonjour','onj','pro')");
		values.add("bproour");

		// Test indexOf
		codes.add("indexOf('bonjour','o')");
		values.add(1);
		codes.add("indexOf('bonjour','o',2)");
		values.add(4);

		// Test split
		codes.add("string(split('1:2:3:4:5',':'))");
		values.add("[1, 2, 3, 4, 5]");
		codes.add("string(split('1:2:3:4:5',':',2))");
		values.add("[1, 2:3:4:5]");

		// Test toLower
		codes.add("toLower('AbCDefgh')");
		values.add("abcdefgh");

		// Test toUpper
		codes.add("toUpper('AbCDefgh')");
		values.add("ABCDEFGH");

		// Test startsWith
		codes.add("startsWith('bonjour','bon')");
		values.add(true);
		codes.add("startsWith('bonjour','jour')");
		values.add(false);

		// Test endsWith
		codes.add("endsWith('bonjour','bon')");
		values.add(false);
		codes.add("endsWith('bonjour','jour')");
		values.add(true);

		// Test contains
		codes.add("contains('bonjour','bon')");
		values.add(true);
		codes.add("contains('bonjour','jour')");
		values.add(true);
		codes.add("contains('bonjour','jourr')");
		values.add(false);

		// Test number
		codes.add("contains('bonjour','bon')");
		values.add(true);
		codes.add("contains('bonjour','jour')");
		values.add(true);
		codes.add("contains('bonjour','jourr')");
		values.add(false);

		// Test AI
		Assert.assertTrue(testAI(codes, values));
	}

	@Test
	public void ArrayFunctionsTest() throws Exception {
		// Test AI

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test remove
		codes.add("function(){ var r = ['a','b','c','d','e']; return remove(r,1); }()");
		values.add("b");
		codes.add("function(){ var r = ['a','b','c','d','e']; return remove(r,55); }()");
		values.add(null);
		codes.add("function(){ var r = ['a','b','c','d','e']; remove(r,1); return string(r);}()");
		values.add("[a, c, d, e]");

		// Test count
		codes.add("count(['a','b','c','d','e'])");
		values.add(5);

		// Test join
		codes.add("join(['a','b','c','d','e'],'_')");
		values.add("a_b_c_d_e");
		codes.add("join(['a','b','c','d','e'],'_-')");
		values.add("a_-b_-c_-d_-e");

		// Test insert
		codes.add("function(){ var a = ['a','b','c','d']; insert(a, 'b', 1); return string(a); }()");
		values.add("[a, b, b, c, d]");

		// Test push
		codes.add("function(){ var a = ['a','b','c','d']; push(a, 'b'); return string(a); }()");
		values.add("[a, b, c, d, b]");

		// Test unshift
		codes.add("function(){ var a = ['a','b','c','d']; unshift(a, 'b'); return string(a); }()");
		values.add("[b, a, b, c, d]");

		// Test shift
		codes.add("function(){ var a = ['a','b','c','d']; shift(a); return string(a); }()");
		values.add("[b, c, d]");

		// Test pop
		codes.add("function(){ var a = ['a','b','c','d']; pop(a); return string(a); }()");
		values.add("[a, b, c]");

		// Test removeElement
		codes.add("function(){ var a = ['a','b','c','d']; removeElement(a,'c'); return string(a); }()");
		values.add("[0 : a, 1 : b, 3 : d]");

		// Test removeKey
		codes.add("function(){ var a = ['a':'va','b':'vb','c':'vc','d':'vd']; removeKey(a,'a'); return string(a); }()");
		values.add("[b : vb, c : vc, d : vd]");

		// Test sort
		codes.add("function(){ var a = [8,6,2,3,7,1,0]; sort(a); return string(a); }()");
		values.add("[0, 1, 2, 3, 6, 7, 8]");
		codes.add("function(){ var a = [8,6,2,3,7,1,0]; sort(a, SORT_ASC); return string(a); }()");
		values.add("[0, 1, 2, 3, 6, 7, 8]");
		codes.add("function(){ var a = [8,6,2,3,7,1,0]; sort(a, SORT_DESC); return string(a); }()");
		values.add("[8, 7, 6, 3, 2, 1, 0]");

		// Test assocSort
		codes.add("function(){ var a = ['b':'vb','c':'vc','a':'va','d':'vd']; assocSort(a); return string(a); }()");
		values.add("[a : va, b : vb, c : vc, d : vd]");
		codes.add("function(){ var a = ['b':'vb','c':'vc','a':'va','d':'vd']; assocSort(a, SORT_DESC); return string(a); }()");
		values.add("[d : vd, c : vc, b : vb, a : va]");
		codes.add("function(){ var a = [8,6,2,3,7,1,0]; assocSort(a); return string(a); }()");
		values.add("[6 : 0, 5 : 1, 2 : 2, 3 : 3, 1 : 6, 4 : 7, 0 : 8]");

		// Test keySort
		codes.add("function(){ var a = ['b':'vb','c':'vc','a':'va','d':'vd']; keySort(a); return string(a); }()");
		values.add("[a : va, b : vb, c : vc, d : vd]");
		codes.add("function(){ var a = ['b':'vb','c':'vc','a':'va','d':'vd']; keySort(a, SORT_DESC); return string(a); }()");
		values.add("[d : vd, c : vc, b : vb, a : va]");
		codes.add("function(){ var a = [6 : 0, 5 : 1, 2 : 2, 3 : 3, 1 : 6, 4 : 7, 0 : 8]; keySort(a); return string(a); }()");
		values.add("[8, 6, 2, 3, 7, 1, 0]");

		// Test search
		codes.add("function(){ var a = ['a','b','c','d']; return search(a,'c'); }()");
		values.add(2);
		codes.add("function(){ var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return search(a,'c'); }()");
		values.add("cle3");
		codes.add("function(){ var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return search(a,'454'); }()");
		values.add(null);

		// Test inArray
		codes.add("function(){ var a = ['a','b','c','d']; return inArray(a,'c'); }()");
		values.add(true);
		codes.add("function(){ var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return inArray(a,'c'); }()");
		values.add(true);
		codes.add("function(){ var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return inArray(a,'cle3'); }()");
		values.add(false);
		codes.add("function(){ var a = ['cle1':'a','cle2':'b','cle3':'c','cle4':'d']; return inArray(a,'454'); }()");
		values.add(false);

		// Test reverse
		codes.add("function(){ var a = ['a','b','c','d']; reverse(a); return string(a); }()");
		values.add("[d, c, b, a]");

		// Test arrayMin
		codes.add("arrayMin([8,4,3,-1,8,44])");
		values.add(-1);
		codes.add("arrayMin([0:7,8:9,'a':2])");
		values.add(2);

		// Test arrayMax
		codes.add("arrayMax([8,4,3,-1,8,44])");
		values.add(44);
		codes.add("arrayMax([0:7,8:9,'a':2])");
		values.add(9);

		// Test sum
		codes.add("sum([1,5,7])");
		values.add(13);
		codes.add("sum([0:1,'a':5,'test':7])");
		values.add(13);
		codes.add("sum([])");
		values.add(0);

		// Test average
		codes.add("average([2,4,6])");
		values.add(4);
		codes.add("average([0:2,'a':4,'test':6])");
		values.add(4);
		codes.add("average([])");
		values.add(0);

		// Test fill
		codes.add("function(){ var a = [1,2,3]; fill(a, 'a'); return string(a); }()");
		values.add("[a, a, a]");
		codes.add("function(){ var a = [1,2,3]; fill(a, 'a',2); return string(a); }()");
		values.add("[a, a, 3]");
		codes.add("function(){ var a = []; fill(a, 'a',2); return string(a); }()");
		values.add("[a, a]");

		// Test isEmpty
		codes.add("isEmpty([2,4,6])");
		values.add(false);
		codes.add("isEmpty([2:8])");
		values.add(false);
		codes.add("isEmpty([])");
		values.add(true);

		// Test subArray
		codes.add("string(subArray([1,2,3,4,5,6,7,8],1,3))");
		values.add("[2, 3, 4]");
		codes.add("string(subArray([1,2,3,4,5,6,7,8],3,3))");
		values.add("[4]");

		// Test pushAll
		codes.add("function(){ var a = [1,2,3]; pushAll(a, [5,6,7]); return string(a); }()");
		values.add("[1, 2, 3, 5, 6, 7]");

		// Test assocReverse
		codes.add("function(){ var a = [1,2,3]; assocReverse(a); return string(a); }()");
		values.add("[2 : 3, 1 : 2, 0 : 1]");

		// Test large array
		codes.add("function(){ var a = []; for (var i = 0; i < 100000; ++i) { push(a, i); } return a[91212]; }()");
		values.add("91212");
		
		// Test arrayMap et suivant => plus loin

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
	public void testReference() throws Exception {
		String leekscript = "var t = [3,4,5]; t[3]=[1,2,3,4]; var r = @t[3]; r[4] ='prou3t'; return t;";
		LeekScriptCompilation.testScript(leekscript, new NullLeekValue());
	}

	@Test
	public void testAnonymousFunctioNSelfAccess() {
		String leekscript = "var t = function(){ return t; };";
		try {
			LeekScriptCompilation.testScript(leekscript, new NullLeekValue());
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

	private boolean testAI(List<String> mCodes, List<Object> mValues) throws Exception {
		String leekscript = "return [";
		AbstractLeekValue[] values = new AbstractLeekValue[mValues.size()];

		for (int i = 0; i < mValues.size(); i++) {
			if (i != 0)
				leekscript += ",";
			leekscript += mCodes.get(i);
			Object c = mValues.get(i);
			if (c == null)
				values[i] = new NullLeekValue();
			else if (c instanceof Integer)
				values[i] = new IntLeekValue(((Integer) mValues.get(i)));
			else if (c instanceof Double)
				values[i] = new DoubleLeekValue(((Double) mValues.get(i)));
			else if (c instanceof String)
				values[i] = new StringLeekValue(((String) mValues.get(i)));
			else if (c instanceof Boolean)
				values[i] = new BooleanLeekValue(((Boolean) mValues.get(i)));
			else if (c instanceof AbstractLeekValue)
				values[i] = (AbstractLeekValue) mValues.get(i);
		}
		leekscript += "];";
		try {
			return LeekScriptCompilation.testScript(leekscript, new ArrayLeekValue(ai, values));
		} catch (LSException e) {
			int i = e.getIndex();
			System.err.println("Erreur :\n" + mCodes.get(i));
			System.err.println("Valeur attendue :\n" + e.getThe().getString(ai));
			System.err.println("Valeur renvoyée :\n" + e.getRun().getString(ai));
			return false;
		}
	}
}
