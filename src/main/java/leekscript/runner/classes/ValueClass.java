package leekscript.runner.classes;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public class ValueClass {

	public static Object unknown(AI ai, Object value) {
		return value;
	}

	public static String string(AI ai, Object value) throws LeekRunException {
		if (value instanceof String) {
			return (String) value;
		}
		if (ai.getVersion() <= 3) {
			return ai.string(value);
		}
		return ai.export(value);
	}

	public static Number number(AI ai, Object value) {
		if (value instanceof Number)
			return (Number) value;
		if (value instanceof String) {
			var s = (String) value;
			try {
				if (s.contains(".")) {
					return Double.parseDouble(s);
				} else {
					return Long.parseLong(s);
				}
			} catch (Exception e) {}
		}
		return 0l;
	}

	public static long typeOf(AI ai, Object value) throws LeekRunException {
		return (long) LeekValueManager.getType(value);
	}

	public static Object clone(AI ai, Object value) throws LeekRunException {
		return LeekOperations.clone(ai, value);
	}

	public static Object clone(AI ai, Object value, long depth) throws LeekRunException {
		return LeekOperations.clone(ai, value, (int) depth);
	}
}
