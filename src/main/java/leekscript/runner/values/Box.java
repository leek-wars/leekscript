package leekscript.runner.values;

import java.math.BigInteger;

import leekscript.AILog;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public class Box<T> {

	protected Object mValue;
	protected AI mUAI = null;

	public Box(AI ai) {
		mUAI = ai;
		mValue = null;
	}

	public Box(AI ai, Object value) throws LeekRunException {
		mUAI = ai;
		ai.ops(1);
		if (ai.getVersion() >= 2) {
			mValue = value;
		} else if (value instanceof Box) {
			mValue = LeekOperations.clone(ai, ((Box) value).get());
		} else {
			mValue = value;
		}
	}

	public Box(AI ai, Object value, int ops) throws LeekRunException {
		this(ai, value);
		ai.ops(ops);
	}

	public T get() {
		return (T) mValue;
	}

	public Object set(Object value) throws LeekRunException {
		// mUAI.ops(1);
		if (mUAI.getVersion() >= 2) {
			if (value instanceof Box) {
				return mValue = ((Box) value).get();
			} else {
				return mValue = value;
			}
		} else {
			if (value instanceof Box) {
				return mValue = LeekOperations.clone(mUAI, ((Box) value).get());
			} else {
				return mValue = value;
			}
		}
	}

	public Object setRef(Object value) throws LeekRunException {
		if (value instanceof Box box) {
			return mValue = box.get();
		} else {
			return mValue = value;
		}
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

	public T increment() throws LeekRunException {
		if (mValue instanceof Long) {
			var value = (Long) mValue;
			mValue = value + 1;
			return (T) value;
		}
		if (mValue instanceof BigIntegerValue) {
			var value = (BigIntegerValue) mValue;
			mValue = value.add(new BigIntegerValue(mUAI, BigInteger.ONE));
			return (T) value;
		}
		if (mValue instanceof Double) {
			var value = (Double) mValue;
			mValue = value + 1;
			return (T) value;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { mUAI.export(mValue) + "++" });
		return null;
	}

	public T decrement() throws LeekRunException {
		if (mValue instanceof Long) {
			var value = (Long) mValue;
			mValue = value - 1;
			return (T) value;
		}
		if (mValue instanceof BigIntegerValue) {
			var value = (BigIntegerValue) mValue;
			mValue = value.subtract(new BigIntegerValue(mUAI, BigInteger.ONE));
			return (T) value;
		}
		if (mValue instanceof Double) {
			var value = (Double) mValue;
			mValue = value - 1;
			return (T) value;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { mUAI.export(mValue) + "--" });
		return null;
	}

	public T pre_increment() throws LeekRunException {
		if (mValue instanceof Long) {
			return (T) (mValue = (Long) mValue + 1);
		}
		if (mValue instanceof BigIntegerValue) {
			return (T) (mValue = ((BigIntegerValue) mValue).add(new BigIntegerValue(mUAI, BigInteger.ONE)));
		}
		if (mValue instanceof Double) {
			return (T) (mValue = (Double) mValue + 1);
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { "++" + mUAI.export(mValue) });
		return null;
	}

	public T pre_decrement() throws LeekRunException {
		if (mValue instanceof Long) {
			return (T) (mValue = (Long) mValue - 1);
		}
		if (mValue instanceof BigIntegerValue) {
			return (T) (mValue = ((BigIntegerValue) mValue).subtract(new BigIntegerValue(mUAI, BigInteger.ONE)));
		}
		if (mValue instanceof Double) {
			return (T) (mValue = (Double) mValue - 1);
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { "--" + mUAI.export(mValue) });
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
		if (mValue instanceof BigIntegerValue) {
			return ((BigIntegerValue) mValue).negate();
		}
		return -mUAI.longint(mValue);
	}

	public Object add_eq(Object val) throws LeekRunException {
		if (mValue instanceof LegacyArrayLeekValue && !(val instanceof String)) {
			return mValue = mUAI.add_eq(mValue, val);
		}
		Object ret = mUAI.add(mValue, val);
		return apply_eq(ret);
	}

	public Object sub_eq(Object val) throws LeekRunException {
		Object ret = mUAI.sub(mValue, val);
		return apply_eq(ret);
	}

	public Object mul_eq(Object val) throws LeekRunException {
		Object ret = mUAI.mul(mValue, val);
		return apply_eq(ret);
	}

	public Object pow_eq(Object val) throws LeekRunException {
		Object ret = mUAI.pow(mValue, val);
		if (mUAI.getVersion() < 4) return mValue = ret;
		return apply_eq(ret);
	}

	public Number band_eq(Object val) throws LeekRunException {
		Number ret = mUAI.band(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Number bor_eq(Object val) throws LeekRunException {
		Number ret = mUAI.bor(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Number bxor_eq(Object val) throws LeekRunException {
		Number ret = mUAI.bxor(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Number shl_eq(Object val) throws LeekRunException {
		Number ret = mUAI.shl(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Number shr_eq(Object val) throws LeekRunException {
		Number ret = mUAI.shr(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Number ushr_eq(Object val) throws LeekRunException {
		Number ret = mUAI.ushr(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Number div_eq(Object val) throws LeekRunException {
		Number ret = mUAI.div(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Object div_v1_eq(Object val) throws LeekRunException {
		return mValue = mUAI.div_v1(mValue, val);
	}

	public Number intdiv_eq(Object val) throws LeekRunException {
		Number ret = mUAI.intdiv(mValue, val);
		return (Number) apply_eq(ret);
	}

	public Object mod_eq(Object val) throws LeekRunException {
		Object ret = mUAI.mod(mValue, val);
		return apply_eq(ret);
	}
	
	private Object apply_eq(Object ret) {
		if (mUAI.getVersion() >= 4 && mValue != null && mValue.getClass() != ret.getClass() && ret instanceof BigIntegerValue) {
			if (mValue instanceof Long) {
				return mValue = ((BigIntegerValue) ret).longValue();
			} else if (mValue instanceof Double) {
				return mValue = ((BigIntegerValue) ret).doubleValue();
			}
		}
		return mValue = ret;
	}

	public Object get(Object index, ClassLeekValue fromClass) throws LeekRunException {
		return mUAI.get(mValue, index, fromClass);
	}

	public Box getOrCreate(Object index) throws LeekRunException {
		return LeekValueManager.getOrCreate(mUAI, mValue, index);
	}

	public Object getField(String field, ClassLeekValue fromClass) throws LeekRunException {
		if (mValue instanceof ObjectLeekValue object) {
			return object.getField(field, fromClass);
		}
		return null;
	}

	public Box getFieldL(String field) throws LeekRunException {
		if (mValue instanceof ObjectLeekValue object) {
			return object.getFieldL(field);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object put(AI ai, Object key, Object value) throws LeekRunException {
		if (mValue instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) mValue).put(key, value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public AI getAI() {
		return mUAI;
	}

	public Object execute(Object... arguments) throws LeekRunException {
		return mUAI.execute(mValue, arguments);
	}

	@Override
	public String toString() {
		return "Box(" + (mValue != null ? mValue.toString() : "null") + ")";
	}
}
