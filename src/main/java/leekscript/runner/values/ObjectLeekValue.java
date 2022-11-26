package leekscript.runner.values;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.common.AccessLevel;
import leekscript.common.Error;

public class ObjectLeekValue {

	public final ClassLeekValue clazz;
	public final int id;
	public final LinkedHashMap<String, ObjectVariableValue> fields = new LinkedHashMap<>();

	public ObjectLeekValue(AI ai, ClassLeekValue clazz) {
		this.clazz = clazz;
		this.id = ai.getNextObjectID();
	}

	public ObjectLeekValue(AI ai, String[] keys, Object[] values) throws LeekRunException {
		this(ai, ai.objectClass);
		for (int i = 0; i < keys.length; ++i) {
			addField(ai, keys[i], values[i], AccessLevel.PUBLIC, false);
		}
	}

	public ObjectLeekValue(AI ai, ObjectLeekValue value, int level) throws LeekRunException {
		this(ai, value.clazz);
		ai.ops(value.fields.size());
		for (var field : value.fields.entrySet()) {
			if (level == 1) {
				fields.put(field.getKey(), new ObjectVariableValue(ai, field.getValue().getValue(), field.getValue().level, field.getValue().isFinal));
			} else {
				fields.put(field.getKey(), new ObjectVariableValue(ai, LeekOperations.clone(ai, field.getValue().getValue(), level - 1), field.getValue().level, field.getValue().isFinal));
			}
		}
		ai.increaseRAM(2 * value.fields.size());
	}

	public void addField(AI ai, String field, Object value, AccessLevel level, boolean isFinal) throws LeekRunException {
		fields.put(field, new ObjectVariableValue(ai, value, level, isFinal));
		ai.increaseRAM(2);
	}

	public Object getField(String field) throws LeekRunException {
		return getField(field, clazz);
	}

	public Object getField(String field, ClassLeekValue fromClass) throws LeekRunException {
		// System.out.println("getField " + field);
		if (field.equals("class")) {
			return clazz;
		}
		// Private : Access from same class
		var result = fields.get(field);
		if (result != null) {
			if (fromClass == clazz || clazz.descendsFrom(fromClass)) {
				return result.mValue;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(clazz)) {
					if (result.level == AccessLevel.PRIVATE) {
						clazz.ai.addSystemLog(AILog.ERROR, Error.PRIVATE_FIELD, new String[] { clazz.name, field });
						return null;
					}
					return result.mValue;
				} else {
					// Public : Access from outside
					if (result.level != AccessLevel.PUBLIC) {
						clazz.ai.addSystemLog(AILog.ERROR, result.level == AccessLevel.PROTECTED ? Error.PROTECTED_FIELD : Error.PRIVATE_FIELD, new String[] { clazz.name, field });
						return null;
					}
					return result.mValue;
				}
			}
		}
		var method = clazz.genericMethods.get(field);
		if (method != null) return method;

		clazz.ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new String[] { clazz.name, field });
		return null;
	}

	public Box getFieldL(String field) throws LeekRunException {
		// System.out.println("getField " + field);
		var result = fields.get(field);
		if (result != null) {
			return result;
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
		// ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new String[] { clazz.name, field });
		// return null;
	}

	public Object initField(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		// Pour un objet anonyme (classe Object), on peut rajouter des proprietés à la volée
		if (result == null && clazz == clazz.ai.objectClass) {
			addField(clazz.ai, field, value, AccessLevel.PUBLIC, false);
			return value;
		}
		if (result != null) {
			result.set(value);
			return value;
		}
		clazz.ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new String[] { clazz.name, field });
		return null;
	}

	public Object setField(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		// Pour un objet anonyme (classe Object), on peut rajouter des proprietés à la volée
		if (result == null && clazz == clazz.ai.objectClass) {
			addField(clazz.ai, field, value, AccessLevel.PUBLIC, false);
			return value;
		}
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			result.set(value);
			return value;
		}
		clazz.ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new String[] { clazz.name, field });
		return null;
	}

	public Object field_inc(String field) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.increment();
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_pre_inc(String field) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.pre_increment();
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_dec(String field) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.decrement();
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_pre_dec(String field) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.pre_decrement();
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_add_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.add_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_sub_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.sub_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_mul_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.mul_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_pow_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.pow_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_div_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.div_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_intdiv_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.intdiv_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_mod_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return null;
			}
			return result.mod_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_bor_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.bor_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_bxor_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.bxor_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_band_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.band_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_shl_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.shl_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_shr_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.shr_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public long field_ushr_eq(String field, Object value) throws LeekRunException {
		var result = fields.get(field);
		if (result != null) {
			if (result.isFinal) {
				clazz.ai.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { clazz.name, field });
				return 0;
			}
			return result.ushr_eq(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Box getOrCreate(AI ai, Object value) throws LeekRunException {
		return getFieldL(ai.string(value));
	}

	public Object callAccess(String field, String method, ClassLeekValue fromClass, Object... arguments) throws LeekRunException {
		var resultM = clazz.getMethod(clazz.ai, method, fromClass);
		if (resultM == null) {
			if (method.equals("keys_0")) {
				var result = clazz.ai.newArray(fields.size());
				for (var key : fields.keySet()) {
					result.pushNoClone(clazz.ai, key);
				}
				return result;
			}
			if (method.equals("values_0")) {
				var result = clazz.ai.newArray(fields.size());
				for (var key : fields.values()) {
					result.pushNoClone(clazz.ai, key);
				}
				return result;
			}
		}
		if (resultM == null) {
			var result = fields.get(field);
			if (result != null) {
				// Private : Access from same class
				if (result.level == AccessLevel.PRIVATE && fromClass != clazz) {
					clazz.ai.addSystemLog(AILog.ERROR, Error.PRIVATE_FIELD, new String[] { clazz.name, field });
					return null;
				}
				// Protected : Access from descendant
				if (result.level == AccessLevel.PROTECTED && (fromClass != clazz && !clazz.descendsFrom(fromClass))) {
					clazz.ai.addSystemLog(AILog.ERROR, result.level == AccessLevel.PROTECTED ? Error.PROTECTED_FIELD : Error.PRIVATE_FIELD, new String[] { clazz.name, field });
					return null;
				}
				// Call the value
				return clazz.ai.execute(result.mValue, arguments);
			}
			// Pas de méthode
			var underscore = method.lastIndexOf("_");
			var argCount = Integer.parseInt(method.substring(underscore + 1));
			String methodRealName = method.substring(0, underscore) + "(";
			for (int i = 0; i < argCount; ++i) {
				if (i > 0) methodRealName += ", ";
				methodRealName += "x";
			}
			methodRealName += ")";
			clazz.ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { clazz.name, methodRealName });
			return null;
		}

		// Call method with new arguments, add the object at the beginning
		return resultM.run(clazz.ai, this, arguments);
	}

	public Object callMethod(String method, ClassLeekValue fromClass, Object... arguments) throws LeekRunException {
		var result = clazz.getMethod(clazz.ai, method, fromClass);
		if (result == null) {
			if (method.equals("keys_0")) {
				String[] values = new String[fields.size()];
				int i = 0;
				for (var key : fields.keySet()) {
					values[i++] = key;
				}
				return new LegacyArrayLeekValue(clazz.ai, values);
			}
			if (method.equals("values_0")) {
				Object[] values = new Object[fields.size()];
				int i = 0;
				for (var value : fields.values()) {
					values[i++] = value;
				}
				return new LegacyArrayLeekValue(clazz.ai, values);
			}
		}
		if (result == null) {
			int underscore = method.lastIndexOf("_");
			int argCount = Integer.parseInt(method.substring(underscore + 1));
			String methodRealName = method.substring(0, underscore) + "(";
			for (int i = 0; i < argCount; ++i) {
				if (i > 0) methodRealName += ", ";
				methodRealName += "x";
			}
			methodRealName += ")";
			clazz.ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { clazz.name, methodRealName });
			return null;
		}
		// Call method with new arguments, add the object at the beginning
		return result.run(clazz.ai, this, arguments);
	}

	public Object callSuperMethod(AI ai, String method, ClassLeekValue currentClass, Object... arguments) throws LeekRunException {
		var result = currentClass.getSuperMethod(ai, method, currentClass);
		if (result == null) {
			int underscore = method.lastIndexOf("_");
			int argCount = Integer.parseInt(method.substring(underscore + 1));
			String methodRealName = method.substring(0, underscore) + "(";
			for (int i = 0; i < argCount; ++i) {
				if (i > 0) methodRealName += ", ";
				methodRealName += "x";
			}
			methodRealName += ")";
			ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { clazz.name, methodRealName });
			return null;
		}
		// Call method with new arguments, add the object at the beginning
		return result.run(clazz.ai, this, arguments);
	}

	public int getInt(AI ai) {
		return fields.size();
	}

	public double getDouble(AI ai) {
		return fields.size();
	}

	public String export(AI ai, Set<Object> visited) throws LeekRunException {
		return getStringBase(ai, visited, true);
	}

	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		return getStringBase(ai, visited, false);
	}

	public String getStringBase(AI ai, Set<Object> visited, boolean export) throws LeekRunException {
		visited.add(this);

		var string_method = clazz.getMethod(ai, "string_0", null);
		if (string_method != null) {
			var result = string_method.run(clazz.ai, this);
			if (!(result instanceof String)) {
				ai.addSystemLog(AILog.ERROR, Error.STRING_METHOD_MUST_RETURN_STRING, new String[] { clazz.name });
			} else {
				return ai.string(result, visited);
			}
		}

		ai.ops(1 + fields.size() * 2);

		var sb = new StringBuilder();
		if (clazz != clazz.ai.objectClass) {
			sb.append(clazz.name).append(" ");
		}
		sb.append("{");
		boolean first = true;
		for (HashMap.Entry<String, ObjectVariableValue> field : fields.entrySet()) {
			if (first) first = false;
			else sb.append(", ");
			sb.append(field.getKey());
			sb.append(": ");
			if (visited.contains(field.getValue().getValue())) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(field.getValue().getValue())) {
					visited.add(field.getValue().getValue());
				}
				if (export) {
					sb.append(ai.export(field.getValue().getValue(), visited));
				} else {
					sb.append(ai.string(field.getValue().getValue(), visited));
				}
			}
		}
		sb.append("}");
		return sb.toString();
	}

	public boolean equals(AI ai, Object comp) throws LeekRunException {
		if (comp instanceof ObjectLeekValue) {
			return this == comp;
		}
		return false;
	}

	public boolean equalsDeep(AI ai, Object comp) throws LeekRunException {
		if (comp instanceof ObjectLeekValue) {
			var o = (ObjectLeekValue) comp;
			if (o.clazz != clazz) return false;
			for (var f : fields.entrySet()) {
				if (!ai.eq(f.getValue().getValue(), o.fields.get(f.getKey()))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public Object toJSON(AI ai, HashSet<Object> visited) throws LeekRunException {
		visited.add(this);

		var o = new JSONObject();
		for (var entry : fields.entrySet()) {
			var v = entry.getValue().getValue();
			if (!visited.contains(v)) {
				if (!ai.isPrimitive(v)) {
					visited.add(v);
				}
				o.put(ai.string(entry.getKey()), ai.toJSON(v, visited));
			}
		}
		return o;
	}

	public ClassLeekValue getClazz() {
		return clazz;
	}

	public int size() {
		return fields.size();
	}

	public String toString() {
		var sb = new StringBuilder();
		if (clazz != clazz.ai.objectClass) {
			sb.append(clazz.name).append(" ");
		}
		sb.append("{");
		boolean first = true;
		for (HashMap.Entry<String, ObjectVariableValue> field : fields.entrySet()) {
			if (first) first = false;
			else sb.append(", ");
			sb.append(field.getKey());
			sb.append(": ");
			sb.append(field.getValue().toString());
		}
		sb.append("}");
		return sb.toString();
	}

	@Override
	@SuppressWarnings("deprecated")
	protected void finalize() throws Throwable {
		super.finalize();
		clazz.ai.decreaseRAM(2 * size());
	}

	@Override
	public int hashCode() {
		return this.id;
	}
}
