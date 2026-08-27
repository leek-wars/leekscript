package leekscript.runner.values;

import java.util.Set;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.common.Error;

public class Box<T> {

	protected Object mValue;
	protected AI mUAI = null;

	public void rebind(AI ai, Set<Object> visited) {
		if (!visited.add(this)) return;
		this.mUAI = ai;
		LeekOperations.rebind(ai, mValue, visited);
	}

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

	@SuppressWarnings("unchecked")
	public T get() {
		return (T) mValue;
	}

	public Object set(Object value) throws LeekRunException {
		// mUAI.ops(1);
		// Cas commun (v2+, valeur non-Box) : assignation directe.
		// Le check de version ne se fait que si la valeur est une Box (rare).
		if (value instanceof Box<?> box) {
			Object inner = box.get();
			return mValue = (mUAI.getVersion() >= 2) ? inner : LeekOperations.clone(mUAI, inner);
		}
		return mValue = value;
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

	@SuppressWarnings("unchecked")
	public T increment() throws LeekRunException {
		if (mValue instanceof Long) {
			var value = (Long) mValue;
			mValue = value + 1;
			return (T) value;
		}
		if (mValue instanceof Double) {
			var value = (Double) mValue;
			mValue = value + 1;
			return (T) value;
		}
		if (mValue instanceof BigIntegerValue value) {
			mValue = mUAI.add(value, 1l);
			return (T) value;
		}
		// Cohérent avec ai.add(null, 1) = 1 et MapLeekValue.put_inc :
		// null en contexte numérique vaut 0. Évite un INVALID_OPERATOR + log
		// chaud (5000 fois pour `m[k]++` sur clé absente, 58× plus lent).
		if (mValue == null) {
			mValue = 1l;
			return (T) (Long) 0l;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { mUAI.export(mValue) + "++" });
		return null;
	}

	@SuppressWarnings("unchecked")
	public T decrement() throws LeekRunException {
		if (mValue instanceof Long) {
			var value = (Long) mValue;
			mValue = value - 1;
			return (T) value;
		}
		if (mValue instanceof Double) {
			var value = (Double) mValue;
			mValue = value - 1;
			return (T) value;
		}
		if (mValue instanceof BigIntegerValue value) {
			mValue = mUAI.sub(value, 1l);
			return (T) value;
		}
		if (mValue == null) {
			mValue = -1l;
			return (T) (Long) 0l;
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { mUAI.export(mValue) + "--" });
		return null;
	}

	@SuppressWarnings("unchecked")
	public T pre_increment() throws LeekRunException {
		if (mValue instanceof Long) {
			return (T) (mValue = (Long) mValue + 1);
		}
		if (mValue instanceof Double) {
			return (T) (mValue = (Double) mValue + 1);
		}
		if (mValue instanceof BigIntegerValue big) {
			return (T) (mValue = mUAI.add(big, 1l));
		}
		if (mValue == null) {
			return (T) (mValue = (Long) 1l);
		}
		mUAI.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { "++" + mUAI.export(mValue) });
		return null;
	}

	@SuppressWarnings("unchecked")
	public T pre_decrement() throws LeekRunException {
		if (mValue instanceof Long) {
			return (T) (mValue = (Long) mValue - 1);
		}
		if (mValue instanceof Double) {
			return (T) (mValue = (Double) mValue - 1);
		}
		if (mValue instanceof BigIntegerValue big) {
			return (T) (mValue = mUAI.sub(big, 1l));
		}
		if (mValue == null) {
			return (T) (mValue = (Long) (-1l));
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
		if (mValue instanceof Long l) return -(long) l;
		if (mValue instanceof Double d) return -(double) d;
		if (mValue instanceof BigIntegerValue big) return big.negate();
		return -mUAI.longint(mValue);
	}

	public Object add_eq(Object val) throws LeekRunException {
		if (mValue instanceof LegacyArrayLeekValue && !(val instanceof String)) {
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

	public Object band_eq(Object val) throws LeekRunException {
		return mValue = mUAI.bandAny(mValue, val);
	}

	public Object bor_eq(Object val) throws LeekRunException {
		return mValue = mUAI.borAny(mValue, val);
	}

	public Object bxor_eq(Object val) throws LeekRunException {
		return mValue = mUAI.bxorAny(mValue, val);
	}

	public Object shl_eq(Object val) throws LeekRunException {
		return mValue = mUAI.shlAny(mValue, val);
	}

	public Object shr_eq(Object val) throws LeekRunException {
		return mValue = mUAI.shrAny(mValue, val);
	}

	public Object ushr_eq(Object val) throws LeekRunException {
		return mValue = mUAI.ushrAny(mValue, val);
	}

	public double div_eq(Object val) throws LeekRunException {
		return (double) (mValue = mUAI.div(mValue, val));
	}

	public Object div_eq_v1(Object val) throws LeekRunException {
		return mValue = mUAI.div_v1(mValue, val);
	}

	public Object intdiv_eq(Object val) throws LeekRunException {
		return mValue = mUAI.intdivAny(mValue, val);
	}

	public Object mod_eq(Object val) throws LeekRunException {
		return mValue = mUAI.mod(mValue, val);
	}

	public Object coalesce_eq(Object val) throws LeekRunException {
		// a ??= b  =>  a = (a != null) ? a : b
		if (mValue != null) {
			return mValue;
		}
		return mValue = val;
	}

	public Object get(Object index, ClassLeekValue fromClass) throws LeekRunException {
		return mUAI.get(mValue, index, fromClass);
	}

	public Box<?> getOrCreate(Object index) throws LeekRunException {
		return LeekValueManager.getOrCreate(mUAI, mValue, index);
	}

	public Object getField(String field, ClassLeekValue fromClass) throws LeekRunException {
		if (mValue instanceof ObjectLeekValue object) {
			return object.getField(field, fromClass);
		}
		return null;
	}

	public Box<?> getFieldL(String field) throws LeekRunException {
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
