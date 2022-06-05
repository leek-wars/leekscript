package leekscript.runner;

import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.ObjectLeekValue;

public class LeekOperations {

	public static Object clone(AI ai, Object value) throws LeekRunException {
		return clone(ai, value, 1);
	}

	public static Object clone(AI ai, Object value, int level) throws LeekRunException {
		if (value instanceof LegacyArrayLeekValue) {
			if (level == 0) return value;
			var array = (LegacyArrayLeekValue) value;
			ai.ops(1 + array.size() * (LegacyArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS));
			return new LegacyArrayLeekValue(ai, array, level);
		} else if (value instanceof ArrayLeekValue) {
			if (level == 0) return value;
			var array = (ArrayLeekValue) value;
			ai.ops(1 + array.size());
			return new ArrayLeekValue(ai, array, level);
		} else if (value instanceof MapLeekValue) {
			if (level == 0) return value;
			var map = (MapLeekValue) value;
			ai.ops(1 + map.size());
			return new MapLeekValue(ai, map, level);
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
