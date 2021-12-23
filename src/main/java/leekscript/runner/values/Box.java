package leekscript.runner.values;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.common.Error;

public class Box {

	protected Object mValue;
	protected AI mUAI = null;

	public Box(AI ai) throws LeekRunException {
		mUAI = ai;
		mValue = null;
	}

	public Box(AI ai, Object value) throws LeekRunException {
		mUAI = ai;
		// System.out.println("ops Box");
		ai.ops(1);
		if (ai.getVersion() >= 2) {
			mValue = value;
		} else if (value instanceof Box) {
			mValue = LeekOperations.clone(ai, ((Box) value).getValue());
		} else {
			mValue = value;
		}
	}

	public Box(AI ai, Object value, int ops) throws LeekRunException {
		this(ai, value);
		ai.ops(ops);
	}

	public Object getValue() {
		return mValue;
	}

	public Object set(Object value) throws LeekRunException {
		// mUAI.ops(1);
		if (mUAI.getVersion() >= 2) {
			return mValue = LeekValueManager.getValue(value);
		} else {
			if (value instanceof Box) {
				return mValue = LeekOperations.clone(mUAI, ((Box) value).getValue());
			} else {
				return mValue = value;
			}
		}
	}

	public Object setRef(Object value) throws LeekRunException {
		return mValue = LeekValueManager.getValue(value);
	}

	public void initGlobal(Object value) throws LeekRunException {
		if (value instanceof Box) {
			if (mUAI.getVersion() >= 2) {
				mValue = value;
			} else {
				mValue = LeekOperations.clone(mUAI, value);
			}
		} else {
			mValue = value;
		}
	}

	public Object increment() throws LeekRunException {
		if (mValue instanceof Integer) {
			int value = (Integer) mValue;
			mValue = value + 1;
			return value;
		}
		if (mValue instanceof Double) {
			double value = (Double) mValue;
			mValue = value + 1;
			return value;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { LeekValueManager.getString(mUAI, mValue), "++" });
		return null;
	}

	public Object decrement() throws LeekRunException {
		if (mValue instanceof Integer) {
			int value = (Integer) mValue;
			mValue = value - 1;
			return value;
		}
		if (mValue instanceof Double) {
			double value = (Double) mValue;
			mValue = value - 1;
			return value;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { LeekValueManager.getString(mUAI, mValue) + "--" });
		return null;
	}

	public Object pre_increment() throws LeekRunException {
		if (mValue instanceof Integer) {
			return mValue = (Integer) mValue + 1;
		}
		if (mValue instanceof Double) {
			return mValue = (Double) mValue + 1;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { "++" + LeekValueManager.getString(mUAI, mValue) });
		return null;
	}

	public Object pre_decrement() throws LeekRunException {
		if (mValue instanceof Integer) {
			return mValue = (Integer) mValue - 1;
		}
		if (mValue instanceof Double) {
			return mValue = (Double) mValue - 1;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { "--" + LeekValueManager.getString(mUAI, mValue) });
		return null;
	}

	public Object not() throws LeekRunException {
		// mUAI.ops(1);
		return !mUAI.bool(mValue);
	}

	public Object opposite() throws LeekRunException {
		// mUAI.ops(1);
		if (mValue instanceof Double) {
			return -(Double) mValue;
		}
		return -mUAI.integer(mValue);
	}

	public Object add_eq(Object val) throws LeekRunException {
		if (mValue instanceof ArrayLeekValue && !(val instanceof String)) {
			return mValue = mUAI.add_eq(mValue, val);
		}
		return mValue = mUAI.add(mValue, val);
	}

	public Object sub_eq(Object val) throws LeekRunException {
		return mValue = mUAI.sub(mValue, val);
	}

	public Object mul_eq(Object val) throws LeekRunException {
		return mValue = mUAI.mul(mValue, val);
	}

	public Object pow_eq(Object val) throws LeekRunException {
		return mValue = mUAI.pow(mValue, val);
	}

	public int band_eq(Object val) throws LeekRunException {
		return (int) (mValue = mUAI.band(mValue, val));
	}

	public int bor_eq(Object val) throws LeekRunException {
		return (int) (mValue = mUAI.bor(mValue, val));
	}

	public int bxor_eq(Object val) throws LeekRunException {
		return (int) (mValue = mUAI.bxor(mValue, val));
	}

	public int shl_eq(Object val) throws LeekRunException {
		return (int) (mValue = mUAI.shl(mValue, val));
	}

	public int shr_eq(Object val) throws LeekRunException {
		return (int) (mValue = mUAI.shr(mValue, val));
	}

	public int ushr_eq(Object val) throws LeekRunException {
		return (int) (mValue = mUAI.ushr(mValue, val));
	}

	public Object div_eq(Object val) throws LeekRunException {
		return mValue = mUAI.div(mValue, val);
	}

	public Object mod_eq(Object val) throws LeekRunException {
		return mValue = mUAI.mod(mValue, val);
	}

	public Object get(Object index, ClassLeekValue fromClass) throws LeekRunException {
		return mUAI.get(mValue, index, fromClass);
	}

	public Box getOrCreate(Object index) throws LeekRunException {
		return LeekValueManager.getOrCreate(mUAI, mValue, index);
	}

	public Object getField(String field, ClassLeekValue fromClass) throws LeekRunException {
		if (mValue instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) mValue).getField(field, fromClass);
		}
		return null;
	}

	public Box getFieldL(String field) throws LeekRunException {
		if (mValue instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) mValue).getFieldL(field);
		}
		throw new LeekRunException(LeekRunException.UNKNOWN_FIELD);
	}

	public Object put(AI ai, Object key, Object value) throws LeekRunException {
		if (mValue instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) mValue).put(ai, key, value);
		}
		throw new LeekRunException(LeekRunException.UNKNOWN_FIELD);
	}

	public AI getAI() {
		return mUAI;
	}

	public Object execute(Object... arguments) throws LeekRunException {
		return LeekValueManager.execute(mUAI, mValue, arguments);
	}

	@Override
	public String toString() {
		return mValue != null ? mValue.toString() : "null";
	}
}
