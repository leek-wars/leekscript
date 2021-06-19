package leekscript.runner;

import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.ObjectLeekValue;

public class LeekOperations {

	public static Object and(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.bool(v1) && ai.bool(v2);
	}

	public static Object or(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.bool(v1) || ai.bool(v2);
	}

	public static int band(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.integer(v1) & ai.integer(v2);
	}

	public static int bleft(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.integer(v1) << ai.integer(v2);
	}

	public static int bright(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.integer(v1) >> ai.integer(v2);
	}

	public static int buright(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.integer(v1) >>> ai.integer(v2);
	}

	public static boolean equals(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return ai.eq(v1, v2);
	}

	public static boolean notequals(AI ai, Object v1, Object v2) throws LeekRunException {
		// ai.ops(1);
		return !ai.eq(v1, v2);
	}

	public static Object clone(AI ai, Object value) throws LeekRunException {
		return clone(ai, value, 1);
	}

	public static Object clone(AI ai, Object value, int level) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			if (level == 0) return value;
			// System.out.println("ops Clone Array begin");
			ai.ops(1);
			var array = (ArrayLeekValue) value;
			if (array.size() > 0) {
				// System.out.println("ops Clone Array");
				ai.ops(array.size() * (ArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS));
			}
			return new ArrayLeekValue(ai, array, level);
		} else if (value instanceof ObjectLeekValue) {
			if (level == 0) return value;
			ai.ops(1);
			return new ObjectLeekValue(ai, (ObjectLeekValue) value, level);
		}
		return value;
	}

	public static boolean equals_equals(AI ai, Object v1, Object v2) throws LeekRunException {
		ai.ops(1);
		if (v1 instanceof ObjectLeekValue && v2 instanceof ObjectLeekValue) {
			return v1 == v2;
		}
		return LeekValueManager.getType(v1) == LeekValueManager.getType(v2) && ai.eq(v1, v2);
	}

	public static boolean notequals_equals(AI ai, Object v1, Object v2) throws LeekRunException {
		return !equals_equals(ai, v1, v2);
	}
}
