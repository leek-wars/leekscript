package leekscript.runner;

import leekscript.LeekLog;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.ArrayLeekValue.ArrayIterator;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.StringLeekValue;

public class LeekOperations {

	public static AbstractLeekValue add(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {

		v1 = v1.getValue();
		v2 = v2.getValue();

		if (v1.isNumeric() && v2.isNumeric()) {
			ai.addOperations(1);
			if (v1 instanceof DoubleLeekValue || v2 instanceof DoubleLeekValue) {
				return LeekValueManager.getLeekDoubleValue(v1.getDouble(ai) + v2.getDouble(ai));
			} else {
				return LeekValueManager.getLeekIntValue(v1.getInt(ai) + v2.getInt(ai));
			}
		}

		// Concatenate arrays
		if (v1 instanceof ArrayLeekValue && v2 instanceof ArrayLeekValue) {
			
			ai.addOperations(1 + (v1.getArray().size() + v2.getArray().size()) * 2);
			
			ArrayLeekValue retour = new ArrayLeekValue();
			ArrayIterator iterator = v1.getArray().getArrayIterator();

			while (!iterator.ended()) {
				if (iterator.key() instanceof String) {
					retour.getOrCreate(ai, iterator.getKey(ai)).set(ai, iterator.getValue(ai));
				} else {
					retour.push(ai, iterator.getValue(ai));
				}
				iterator.next();
			}
			iterator = v2.getArray().getArrayIterator();
			while (!iterator.ended()) {
				if (iterator.key() instanceof String) {
					retour.getOrCreate(ai, iterator.getKey(ai)).set(ai, iterator.getValue(ai));
				} else {
					retour.push(ai, iterator.getValue(ai));
				}
				iterator.next();
			}
			return retour;
		}

		String v1_string = v1.getString(ai);
		String v2_string = v2.getString(ai);
		ai.addOperations(1 + v1_string.length() + v2_string.length());
		return new StringLeekValue(v1_string + v2_string);
	}

	public static AbstractLeekValue minus(AI ai, AbstractLeekValue v1, AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		v1 = v1.getValue();
		v2 = v2.getValue();
		if (v1.isNumeric() && v2.isNumeric()) {
			if (v1 instanceof DoubleLeekValue || v2 instanceof DoubleLeekValue)
				return LeekValueManager.getLeekDoubleValue(v1.getDouble(ai) - v2.getDouble(ai));
			else
				return LeekValueManager.getLeekIntValue(v1.getInt(ai) - v2.getInt(ai));
		}
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public static AbstractLeekValue power(AI ai, AbstractLeekValue v1, AbstractLeekValue v2) throws Exception {
		ai.addOperations(AbstractLeekValue.POW_COST);
		v1 = v1.getValue();
		v2 = v2.getValue();
		if (v1.isNumeric() && v2.isNumeric()) {
			if (v1 instanceof DoubleLeekValue || v2 instanceof DoubleLeekValue) {
				double result = Math.pow(v1.getDouble(ai), v2.getDouble(ai));
				if (Double.isNaN(result))
					return LeekValueManager.NULL;
				return LeekValueManager.getLeekDoubleValue(result);
			} else {
				double result = Math.pow(v1.getInt(ai), v2.getInt(ai));
				if (Double.isNaN(result))
					return LeekValueManager.NULL;
				return LeekValueManager.getLeekIntValue((int) result);
			}
		}
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public static AbstractLeekValue multiply(AI ai, AbstractLeekValue v1, AbstractLeekValue v2) throws Exception {
		ai.addOperations(AbstractLeekValue.MUL_COST);
		v1 = v1.getValue();
		v2 = v2.getValue();
		if (v1.isNumeric() && v2.isNumeric()) {
			if (v1 instanceof DoubleLeekValue || v2 instanceof DoubleLeekValue) {
				return LeekValueManager.getLeekDoubleValue(v1.getDouble(ai) * v2.getDouble(ai));
			} else {
				return LeekValueManager.getLeekIntValue(v1.getInt(ai) * v2.getInt(ai));
			}
		}
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public static AbstractLeekValue divide(AI ai, AbstractLeekValue v1, AbstractLeekValue v2) throws Exception {
		
		ai.addOperations(AbstractLeekValue.DIV_COST);

		v1 = v1.getValue();
		v2 = v2.getValue();
		if (v1.isNumeric() && v2.isNumeric()) {
			if (v2.getDouble(ai) == 0) {
				ai.addOperations(AI.ERROR_LOG_COST);
				ai.addSystemLog(LeekLog.ERROR, LeekLog.DIVISION_BY_ZERO);
				return LeekValueManager.NULL;
			}
			if (v1 instanceof DoubleLeekValue || v2 instanceof DoubleLeekValue) {
				return LeekValueManager.getLeekDoubleValue(v1.getDouble(ai) / v2.getDouble(ai));
			} else {
				if (v1.getInt(ai) % v2.getInt(ai) != 0)
					return new DoubleLeekValue(v1.getDouble(ai) / v2.getDouble(ai));
				else
					return LeekValueManager.getLeekIntValue(v1.getInt(ai) / v2.getInt(ai));
			}
		}
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public static AbstractLeekValue modulus(AI ai, AbstractLeekValue v1, AbstractLeekValue v2) throws Exception {
		
		ai.addOperations(AbstractLeekValue.MOD_COST);
		v1 = v1.getValue();
		v2 = v2.getValue();
		if (v1.isNumeric() && v2.isNumeric()) {
			if (v2.getDouble(ai) == 0) {
				ai.addOperations(AI.ERROR_LOG_COST);
				ai.addSystemLog(LeekLog.ERROR, LeekLog.DIVISION_BY_ZERO);
				return LeekValueManager.NULL;
			}
			if (v1 instanceof DoubleLeekValue || v2 instanceof DoubleLeekValue)
				return LeekValueManager.getLeekDoubleValue(v1.getDouble(ai) % v2.getDouble(ai));
			else
				return LeekValueManager.getLeekIntValue(v1.getInt(ai) % v2.getInt(ai));
		}
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public static AbstractLeekValue and(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.getBoolean() && v2.getBoolean());
	}

	public static AbstractLeekValue or(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.getBoolean() || v2.getBoolean());
	}

	public static AbstractLeekValue bor(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(v1.getInt(ai) | v2.getInt(ai));
	}

	public static AbstractLeekValue band(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(v1.getInt(ai) & v2.getInt(ai));
	}

	public static AbstractLeekValue bxor(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(v1.getInt(ai) ^ v2.getInt(ai));
	}

	public static AbstractLeekValue bleft(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(v1.getInt(ai) << v2.getInt(ai));
	}

	public static AbstractLeekValue bright(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(v1.getInt(ai) >> v2.getInt(ai));
	}

	public static AbstractLeekValue brotate(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(v1.getInt(ai) >>> v2.getInt(ai));
	}

	public static AbstractLeekValue equals(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.equals(ai, v2));
	}

	public static AbstractLeekValue notequals(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.notequals(ai, v2));
	}

	public static AbstractLeekValue less(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.less(ai, v2));
	}

	public static AbstractLeekValue more(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.more(ai, v2));
	}

	public static AbstractLeekValue lessequals(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.lessequals(ai, v2));
	}

	public static AbstractLeekValue moreequals(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.moreequals(ai, v2));
	}

	public static AbstractLeekValue clone(AI ai, AbstractLeekValue value)
			throws Exception {
		value = value.getValue();
		ai.addOperations(1);
		if (value instanceof StringLeekValue)
			return new StringLeekValue(value.getString(ai));
		else if (value instanceof FunctionLeekValue)
			return ((FunctionLeekValue) value).cloneFunction();
		else if (value instanceof BooleanLeekValue)
			return LeekValueManager.getLeekBooleanValue(value.getBoolean());
		else if (value instanceof IntLeekValue)
			return LeekValueManager.getLeekIntValue(value.getInt(ai));
		else if (value instanceof DoubleLeekValue)
			return new DoubleLeekValue(value.getDouble(ai));
		else if (value instanceof ArrayLeekValue) {
			if (value.getArray().size() > 0) {
				ai.addOperations(value.getArray().size() * (ArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS));
			}
			return new ArrayLeekValue(ai, value.getArray());
		} else
			return LeekValueManager.NULL;
	}

	public static AbstractLeekValue equals_equals(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.getType() == v2.getType() && v1.equals(ai, v2));
	}

	public static AbstractLeekValue notequals_equals(AI ai, AbstractLeekValue v1,
			AbstractLeekValue v2) throws Exception {
		ai.addOperations(1);
		return LeekValueManager.getLeekBooleanValue(v1.getType() != v2.getType()
				|| v1.notequals(ai, v2));
	}
}
