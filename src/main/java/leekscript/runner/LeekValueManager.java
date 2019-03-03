package leekscript.runner;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.TreeMap;

import leekscript.ErrorManager;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class LeekValueManager {

	static {
		init();
	}

	public final static NullLeekValue NULL = new NullLeekValue();
	public final static BooleanLeekValue TRUE = new BooleanLeekValue(true);
	public final static BooleanLeekValue FALSE = new BooleanLeekValue(false);
	public final static int MIN_INT = -50;
	public final static int MAX_INT = 800;

	private static TreeMap<Integer, IntLeekValue> mIntegers;
	private static TreeMap<LeekFunctions, FunctionLeekValue> mFunctions;

	public static void init() {
		mIntegers = new TreeMap<Integer, IntLeekValue>();
		for (int i = MIN_INT; i <= MAX_INT; i++) {
			try {
				mIntegers.put(i, new IntLeekValue(i));
			} catch (Exception e) {
				ErrorManager.exception(e);
			}
		}
		mFunctions = new TreeMap<LeekFunctions, FunctionLeekValue>();
		for (LeekFunctions function : LeekFunctions.values()) {
			mFunctions.put(function, new FunctionLeekValue(function));
		}
	}

	public static FunctionLeekValue getFunction(LeekFunctions function) {
		return mFunctions.get(function);
	}

	public static AbstractLeekValue getLeekDoubleValue(double nb) {
		if (nb == 0)
			return getLeekIntValue(0);
		else if (nb == 1)
			return getLeekIntValue(1);
		else if (nb == -1)
			return getLeekIntValue(-1);
		else
			return new DoubleLeekValue(nb);
	}

	public static AbstractLeekValue getLeekIntValue(int nb) {
		// Si c'est une valeur en cache on la retourne
		if (MIN_INT <= nb && nb <= MAX_INT) {
			return mIntegers.get(nb);
		}
		return new IntLeekValue(nb);
	}

	public static AbstractLeekValue getLeekIntValue(AI ai, int nb, AbstractLeekValue mValue) throws LeekRunException {
		// Si c'est une valeur en cache on la retourne
		if (MIN_INT <= nb && nb <= MAX_INT)
			return mIntegers.get(nb);
		// Si l'ancienne valeur est une valeur en cache on la modifie pas
		if (MIN_INT <= mValue.getInt(ai) && mValue.getInt(ai) <= MAX_INT)
			return new IntLeekValue(nb);
		// Sinon on modifie direct la valeur
		mValue.setInt(nb);
		return mValue;
	}

	public static AbstractLeekValue getLeekBooleanValue(boolean b) {
		return b ? TRUE : FALSE;
	}

	public static AbstractLeekValue parseJSON(Object o, AI ai) throws Exception {

		if (o instanceof Boolean) {
			return new BooleanLeekValue((Boolean) o);
		}
		if (o instanceof String) {
			return new StringLeekValue((String) o);
		}
		if (o instanceof Integer) {
			return new IntLeekValue((Integer) o);
		}
		if (o instanceof BigInteger) {
			throw new Exception();
		}
		if (o instanceof BigDecimal) {
			return new DoubleLeekValue(((BigDecimal) o).doubleValue());
		}
		if (o instanceof JSONArray) {
			JSONArray a = (JSONArray) o;
			ArrayLeekValue array = new ArrayLeekValue();
			for (Object oo : a) {
				array.push(ai, parseJSON(oo, ai));
			}
			return array;
		}
		if (o instanceof JSONObject) {
			JSONObject a = (JSONObject) o;
			ArrayLeekValue array = new ArrayLeekValue();
			for (String key : a.keySet()) {
				array.getOrCreate(ai, new StringLeekValue(key)).set(ai, parseJSON(a.get(key), ai));
			}
			return array;
		}

		return new StringLeekValue("Class " + o.getClass().getSimpleName());
	}
}
