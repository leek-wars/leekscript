package test;

import static org.junit.Assert.fail;

import org.junit.Before;

import leekscript.runner.AI;
import leekscript.runner.LeekFunctions;
import leekscript.runner.LeekOperations;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

import org.junit.Test;

public class Operators {
	
	AI ai;
	
	@Before
	public void init() throws Exception {
		ai = new DefaultUserAI();
	}

	@Test
	public void simpleEqualsOperators() throws Exception {
		try {
			AI uai = new DefaultUserAI();
			if (!LeekOperations.equals(uai, new StringLeekValue("Chaine1"), new StringLeekValue("Chaine1")).getBoolean())
				fail("Chaine1 != Chaine1");
			if (LeekOperations.equals(uai, new StringLeekValue("Chaine1"), new StringLeekValue("Chaine2")).getBoolean())
				fail("Chaine1 == Chaine2");
			if (!LeekOperations.equals(uai, new BooleanLeekValue(false), new BooleanLeekValue(false)).getBoolean())
				fail("false != false");
			if (!LeekOperations.equals(uai, new BooleanLeekValue(true), new BooleanLeekValue(true)).getBoolean())
				fail("true != true");
			if (LeekOperations.equals(uai, new BooleanLeekValue(false), new BooleanLeekValue(true)).getBoolean())
				fail("false == true");
			if (LeekOperations.equals(uai, new BooleanLeekValue(true), new BooleanLeekValue(false)).getBoolean())
				fail("true == false");
			if (LeekOperations.equals(uai, new IntLeekValue(1), new IntLeekValue(2)).getBoolean())
				fail("1 == 2");
			if (LeekOperations.equals(uai, new IntLeekValue(-1), new IntLeekValue(-5)).getBoolean())
				fail("-1 == -5");
			if (!LeekOperations.equals(uai, new IntLeekValue(50), new IntLeekValue(50)).getBoolean())
				fail("50 != 50");
			if (!LeekOperations.equals(uai, new IntLeekValue(0), new IntLeekValue(0)).getBoolean())
				fail("0 != 0");
			if (!LeekOperations.equals(uai, new DoubleLeekValue(0), new DoubleLeekValue(0)).getBoolean())
				fail("0.0 != 0.0");
			if (!LeekOperations.equals(uai, new DoubleLeekValue(5), new DoubleLeekValue(5)).getBoolean())
				fail("5.0 != 5.0");
			if (LeekOperations.equals(uai, new DoubleLeekValue(45), new DoubleLeekValue(5)).getBoolean())
				fail("45.0 == 5.0");
			if (!LeekOperations.equals(uai, new NullLeekValue(), new NullLeekValue()).getBoolean())
				fail("null != null");
			if (!LeekOperations.equals(uai, new ArrayLeekValue(), new ArrayLeekValue()).getBoolean())
				fail("[] != []");
			if (!LeekOperations.equals(uai, new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0) }), 
					new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0) }))
					.getBoolean())
				fail("[0] != [0]");
			if (!LeekOperations.equals(uai, new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0), new IntLeekValue(1) }),
					new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0), new IntLeekValue(1) })).getBoolean())
				fail("[0,1] != [0,1]");
			if (LeekOperations.equals(uai, new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0), new IntLeekValue(1) }),
					new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0) })).getBoolean())
				fail("[0,1] == [0]");

			if (LeekOperations.equals(uai, new ArrayLeekValue(ai, new AbstractLeekValue[] { new StringLeekValue("Chaine1") }),
					new ArrayLeekValue(ai, new AbstractLeekValue[] { new StringLeekValue("Chaine2") })).getBoolean())
				fail("[\"Chaine1\"] == [\"Chaine2\"]");
			if (!LeekOperations.equals(uai, new ArrayLeekValue(ai, new AbstractLeekValue[] { new StringLeekValue("Chaine1") }),
					new ArrayLeekValue(ai, new AbstractLeekValue[] { new StringLeekValue("Chaine1") })).getBoolean())
				fail("[\"Chaine1\"] != [\"Chaine1\"]");

			if (!LeekOperations.equals(uai, new FunctionLeekValue(1), new FunctionLeekValue(1)).getBoolean())
				fail("#F1 != #F1");
			if (LeekOperations.equals(uai, new FunctionLeekValue(1), new FunctionLeekValue(2)).getBoolean())
				fail("#F1 == #AF1");
			if (LeekOperations.equals(uai, new FunctionLeekValue(LeekFunctions.endsWith), new FunctionLeekValue(1)).getBoolean())
				fail("#endsWith == #AF1");
			if (!LeekOperations.equals(uai, new FunctionLeekValue(LeekFunctions.endsWith), new FunctionLeekValue(LeekFunctions.endsWith)).getBoolean())
				fail("#endsWith != #endsWith");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void advancedEqualsOperators() {
		try {
			// String-Intger
			AI uai = new DefaultUserAI();
			if (!LeekOperations.equals(uai, new StringLeekValue("1"), new IntLeekValue(1)).getBoolean())
				fail("\"1\" != 1");
			if (!LeekOperations.equals(uai, new StringLeekValue("0"), new IntLeekValue(0)).getBoolean())
				fail("\"0\" != 0");
			if (!LeekOperations.equals(uai, new StringLeekValue("10"), new IntLeekValue(10)).getBoolean())
				fail("\"10\" != 10");
			if (LeekOperations.equals(uai, new StringLeekValue("15"), new IntLeekValue(10)).getBoolean())
				fail("\"15\" == 10");
			if (!LeekOperations.equals(uai, new IntLeekValue(1), new StringLeekValue("1")).getBoolean())
				fail("1 != \"1\"");
			if (!LeekOperations.equals(uai, new IntLeekValue(0), new StringLeekValue("0")).getBoolean())
				fail("0 != \"0\"");
			if (!LeekOperations.equals(uai, new DoubleLeekValue(0), new StringLeekValue("0")).getBoolean())
				fail("0.0 != \"0\"");
			if (!LeekOperations.equals(uai, new IntLeekValue(10), new StringLeekValue("10")).getBoolean())
				fail("10 != \"10\"");
			if (!LeekOperations.equals(uai, new DoubleLeekValue(10), new StringLeekValue("10")).getBoolean())
				fail("10.0 != \"10\"");
			if (LeekOperations.equals(uai, new IntLeekValue(10), new StringLeekValue("15")).getBoolean())
				fail("10 == \"15\"");
			if (LeekOperations.equals(uai, new DoubleLeekValue(10), new StringLeekValue("15")).getBoolean())
				fail("10.0 == \"15\"");
			if (!LeekOperations.equals(uai, new DoubleLeekValue(10.8), new StringLeekValue("10.8")).getBoolean())
				fail("10.8 != \"10.8\"");
			if (LeekOperations.equals(uai, new DoubleLeekValue(10.8), new StringLeekValue("10.87")).getBoolean())
				fail("10.8 == \"10.87\"");

			if (!LeekOperations.equals(uai, new BooleanLeekValue(true), new StringLeekValue("true")).getBoolean())
				fail("true != \"true\"");
			if (!LeekOperations.equals(uai, new BooleanLeekValue(false), new StringLeekValue("false")).getBoolean())
				fail("false != \"false\"");
			if (LeekOperations.equals(uai, new BooleanLeekValue(true), new StringLeekValue("false")).getBoolean())
				fail("true == \"false\"");
			if (LeekOperations.equals(uai, new BooleanLeekValue(false), new StringLeekValue("true")).getBoolean())
				fail("false == \"true\"");

			if (!LeekOperations.equals(uai, new IntLeekValue(1), new StringLeekValue("true")).getBoolean())
				fail("1 != \"true\"");
			if (!LeekOperations.equals(uai, new IntLeekValue(0), new StringLeekValue("false")).getBoolean())
				fail("0 != \"false\"");
			if (!LeekOperations.equals(uai, new IntLeekValue(12), new StringLeekValue("true")).getBoolean())
				fail("12 != \"true\"");
			if (LeekOperations.equals(uai, new IntLeekValue(2), new StringLeekValue("false")).getBoolean())
				fail("2 == \"false\"");

			if (!LeekOperations.equals(uai, new IntLeekValue(0), new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(0) })).getBoolean())
				fail("0 != [0]");
			if (!LeekOperations.equals(uai, new IntLeekValue(1), new ArrayLeekValue(ai, new AbstractLeekValue[] { new IntLeekValue(1) })).getBoolean())
				fail("1 != [1]");
			if (!LeekOperations.equals(uai, new IntLeekValue(0), new ArrayLeekValue()).getBoolean())
				fail("0 != []");
			if (!LeekOperations.equals(uai, new BooleanLeekValue(false), new ArrayLeekValue()).getBoolean())
				fail("0 != []");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void tripleEqualsTest() throws Exception {

		AbstractLeekValue[] values = new AbstractLeekValue[] { new IntLeekValue(0), new IntLeekValue(1), 
				new IntLeekValue(12), new DoubleLeekValue(13), new BooleanLeekValue(false),
				new BooleanLeekValue(true), new NullLeekValue(), new StringLeekValue("true"), 
				new StringLeekValue("false"), new StringLeekValue("12"), 
				new StringLeekValue("lama"),
				new ArrayLeekValue(), new ArrayLeekValue(ai, new AbstractLeekValue[] { 
						new StringLeekValue("12") }) };

		int i, j;
		for (i = 0; i < values.length; i++) {
			for (j = 0; j < values.length; j++) {
				if (LeekOperations.equals_equals(ai, values[i], values[j]).getBoolean() != (i == j))
					fail(values[i].getString(ai) + ((i == j) ? "!==" : "===") + values[j].getString(ai));
			}
		}

		if (!LeekOperations.equals_equals(ai, new IntLeekValue(1), new DoubleLeekValue(1)).getBoolean())
			fail("1 !== 1.0");
		if (!LeekOperations.equals_equals(ai, new DoubleLeekValue(12), new IntLeekValue(12)).getBoolean())
			fail("12.0 !== 12");
	}
}
