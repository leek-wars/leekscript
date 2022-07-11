package leekscript.runner;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import leekscript.AILog;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.ClassLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.LeekValue;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.runner.values.Box;
import leekscript.common.AccessLevel;
import leekscript.common.Error;

public class LeekValueManager {

	public static Object parseJSON(Object o, AI ai) throws LeekRunException {
		if (o instanceof Boolean) {
			return o;
		}
		if (o instanceof String) {
			return o;
		}
		if (o instanceof Double) {
			return o;
		}
		if (o instanceof Integer) {
			return (long) (Integer) o;
		}
		if (o instanceof Long) {
			return o;
		}
		if (o instanceof BigInteger) {
			ai.addSystemLog(AILog.ERROR, Error.INVALID_OPERATOR, new String[] { "jsonDecode(" + ai.export(o) + ")" });
			return null;
		}
		if (o instanceof BigDecimal) {
			return ((BigDecimal) o).doubleValue();
		}
		if (o instanceof JSONArray) {
			JSONArray a = (JSONArray) o;
			var array = ai.newArray();
			for (var oo : a) {
				array.pushNoClone(ai, parseJSON(oo, ai));
			}
			return array;
		}
		if (o instanceof JSONObject) {
			JSONObject a = (JSONObject) o;

			var keys = new ArrayList<String>(a.keySet());
			Collections.sort(keys);

			if (ai.getVersion() <= 3) {
				var array = new LegacyArrayLeekValue();
				for (var key : keys) {
					array.getOrCreate(ai, key).set(parseJSON(a.get(key), ai));
				}
				return array;
			}
			var object = new ObjectLeekValue(ai, ai.objectClass);
			for (String key : keys) {
				object.addField(ai, key, parseJSON(a.get(key), ai), AccessLevel.PUBLIC, false);
			}
			return object;
		}

		return "Class " + o.getClass().getSimpleName();
	}

	public static Object getValue(Object value) {
		if (value instanceof Box) {
			return ((Box) value).getValue();
		}
		return value;
	}

	public static long bnot(AI ai, Object value) throws LeekRunException {
		return ~ai.longint(value);
	}

	public static FunctionLeekValue getFunction(AI ai, Object value) throws LeekRunException {
		var v = getValue(value);
		if (v instanceof FunctionLeekValue) {
			return (FunctionLeekValue) v;
		}
		// On ne peux pas exécuter ce type de variable
		ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_VALUE, new String[] { ai.export(value) });
		return null;
	}

	public static Box getOrCreate(AI ai, Object value, Object index) throws LeekRunException {
		if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).getOrCreate(ai, index);
		}
		throw new LeekRunException(Error.UNKNOWN_FUNCTION);
	}

	public static Box getFieldL(AI ai, Object value, String field) throws LeekRunException {
		// value = getValue(value);
		if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).getFieldL(field);
		}
		if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).getFieldL(field);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public static Object callMethod(AI ai, Object value, String method, Object... arguments) throws LeekRunException {
		// Aucune méthode
		ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { ai.export(value), method });
		return null;
	}

	public static Object callSuperMethod(AI ai, Object value, String method, Object... arguments) throws LeekRunException {
		// Aucune méthode
		ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { ai.export(value), method });
		return null;
	}

	public static int getType(Object v) {
		if (v == null) return LeekValue.NULL;
		if (v instanceof Boolean) return LeekValue.BOOLEAN;
		if (v instanceof Number) return LeekValue.NUMBER;
		if (v instanceof String) return LeekValue.STRING;
		if (v instanceof LegacyArrayLeekValue || v instanceof ArrayLeekValue) return LeekValue.ARRAY;
		if (v instanceof MapLeekValue) return LeekValue.MAP;
		if (v instanceof ObjectLeekValue) return LeekValue.OBJECT;
		if (v instanceof ClassLeekValue) return LeekValue.CLASS;
		if (v instanceof FunctionLeekValue) return LeekValue.FUNCTION;
		if (v instanceof Box) return getType(((Box) v).getValue());
		return 0;
	}

	public static int getV1Type(Object v) {
		if (v == null) return LeekValue.NULL_V1;
		if (v instanceof Boolean) return LeekValue.BOOLEAN_V1;
		if (v instanceof Number) return LeekValue.NUMBER_V1;
		if (v instanceof String) return LeekValue.STRING_V1;
		if (v instanceof LegacyArrayLeekValue) return LeekValue.ARRAY_V1;
		if (v instanceof ObjectLeekValue) return LeekValue.OBJECT_V1;
		if (v instanceof ClassLeekValue) return LeekValue.CLASS_V1;
		if (v instanceof FunctionLeekValue) return LeekValue.FUNCTION_V1;
		if (v instanceof Box) return getV1Type(((Box) v).getValue());
		return 0;
	}

	public static String toJSON(AI ai, Object value) {
		return null;
	}

	public static Object executeArrayAccess(AI ai, Object array, Object key, ClassLeekValue fromClass, Object... arguments) throws LeekRunException {
		if (array instanceof ObjectLeekValue) {
			ai.ops(1);
			return ((ObjectLeekValue) array).callMethod(ai.string(key) + "_" + arguments.length, fromClass, arguments);
		} else if (array instanceof ClassLeekValue) {
			return ((ClassLeekValue) array).callMethod(ai.string(key) + "_" + arguments.length, fromClass, arguments);
		} else {
			return ai.execute(ai.get(array, key, fromClass), arguments);
		}
	}
}
