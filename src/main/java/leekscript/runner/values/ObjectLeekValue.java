package leekscript.runner.values;

import java.util.HashMap;
import java.util.Set;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekAnonymousFunction;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.common.AccessLevel;
import leekscript.common.Error;

public class ObjectLeekValue extends AbstractLeekValue {

	public final ClassLeekValue clazz;
	public final HashMap<String, ObjectVariableValue> fields = new HashMap<>();

	public ObjectLeekValue(ClassLeekValue clazz) {
		this.clazz = clazz;
	}

	public ObjectLeekValue(AI ai, ObjectLeekValue value, int level) throws LeekRunException {
		this.clazz = value.clazz;
		ai.addOperations(value.fields.size());
		for (var field : value.fields.entrySet()) {
			if (level == 1) {
				fields.put(field.getKey(), new ObjectVariableValue(ai, LeekOperations.clonePrimitive(ai, field.getValue()), field.getValue().level));
			} else {
				fields.put(field.getKey(), new ObjectVariableValue(ai, LeekOperations.clone(ai, field.getValue(), level - 1), field.getValue().level));
			}
		}
	}

	public void addField(AI ai, String field, AbstractLeekValue value, AccessLevel level) throws LeekRunException {
		fields.put(field, new ObjectVariableValue(ai, LeekOperations.clone(ai, value), level));
	}

	@Override
	public AbstractLeekValue getField(AI ai, String field, ClassLeekValue fromClass) throws LeekRunException {
		// System.out.println("getField " + field);
		ai.addOperations(1);
		if (field.equals("class")) {
			return clazz;
		}
		// Private : Access from same class
		var result = fields.get(field);
		if (result != null) {
			if (fromClass == clazz) {
				return result;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(clazz)) {
					if (result.level == AccessLevel.PRIVATE) {
						ai.addSystemLog(AILog.ERROR, Error.PRIVATE_FIELD, new String[] { clazz.name, field });
						return LeekValueManager.NULL;
					}
					return result;
				} else {
					// Public : Access from outside
					if (result.level != AccessLevel.PUBLIC) {
						ai.addSystemLog(AILog.ERROR, result.level == AccessLevel.PROTECTED ? Error.PROTECTED_FIELD : Error.PRIVATE_FIELD, new String[] { clazz.name, field });
						return LeekValueManager.NULL;
					}
					return result;
				}
			}
		}
		var method = clazz.genericMethods.get(field);
		if (method != null) return method;

		ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new String[] { clazz.name, field });
		return LeekValueManager.NULL;
	}

	@Override
	public AbstractLeekValue get(AI ai, AbstractLeekValue value) throws LeekRunException {
		return getField(ai, value.getString(ai), null);
	}

	@Override
	public AbstractLeekValue get(AI ai, AbstractLeekValue value, ClassLeekValue fromClass) throws LeekRunException {
		return getField(ai, value.getString(ai), fromClass);
	}

	@Override
	public AbstractLeekValue getOrCreate(AI ai, AbstractLeekValue value) throws LeekRunException {
		return getField(ai, value.getString(ai), null);
	}

	@Override
	public AbstractLeekValue getOrCreate(AI ai, AbstractLeekValue value, ClassLeekValue fromClass) throws LeekRunException {
		return getField(ai, value.getString(ai), fromClass);
	}

	@Override
	public AbstractLeekValue callMethod(AI ai, String method, ClassLeekValue fromClass, AbstractLeekValue... arguments) throws LeekRunException {
		ai.addOperations(1);
		LeekAnonymousFunction result = clazz.getMethod(ai, method, fromClass);
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
			return LeekValueManager.NULL;
		}
		// Call method with new arguments, add the object at the beginning
		return result.run(ai, this, arguments);
	}

	@Override
	public AbstractLeekValue callSuperMethod(AI ai, ClassLeekValue currentClass, String method, AbstractLeekValue... arguments) throws LeekRunException {
		ai.addOperations(1);
		LeekAnonymousFunction result = currentClass.getSuperMethod(ai, method, currentClass);
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
			return LeekValueManager.NULL;
		}
		// Call method with new arguments, add the object at the beginning
		return result.run(ai, this, arguments);
	}

	@Override
	public boolean getBoolean() {
		return fields.size() > 0;
	}

	@Override
	public int getInt(AI ai) {
		return fields.size();
	}

	@Override
	public double getDouble(AI ai) {
		return fields.size();
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public String getString(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);

		var string_method = clazz.getMethod(ai, "string_0", null);
		if (string_method != null) {
			var result = string_method.run(ai, this, new AbstractLeekValue[] {});
			if (result.getType() != STRING) {
				ai.addSystemLog(AILog.ERROR, Error.STRING_METHOD_MUST_RETURN_STRING, new String[] { clazz.name });
			} else {
				return result.getString(ai, visited);
			}
		}

		var sb = new StringBuilder(clazz.name + " {");
		boolean first = true;
		for (HashMap.Entry<String, ObjectVariableValue> field : fields.entrySet()) {
			if (first) first = false;
			else sb.append(", ");
			sb.append(field.getKey());
			sb.append(": ");
			if (visited.contains(field.getValue().getValue())) {
				sb.append("<...>");
			} else {
				if (!field.getValue().getValue().isPrimitive()) {
					visited.add(field.getValue().getValue());
				}
				sb.append(field.getValue().getValue().getString(ai, visited));
			}
		}
		sb.append("}");
		return sb.toString();
	}

	@Override
	public int getV10Type() {
		return OBJECT_V10;
	}

	@Override
	public int getType() {
		return OBJECT;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		comp = comp.getValue();
		if (comp instanceof ObjectLeekValue) {
			return this == comp;
		}
		return false;
	}

	public boolean equalsDeep(AI ai, AbstractLeekValue comp) throws LeekRunException {
		comp = comp.getValue();
		if (comp instanceof ObjectLeekValue) {
			var o = (ObjectLeekValue) comp;
			if (o.clazz != clazz) return false;
			for (var f : fields.entrySet()) {
				if (!f.getValue().getValue().equals(ai, o.fields.get(f.getKey()))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public Object toJSON(AI ai) {
		return "object";
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	public ClassLeekValue getClazz() {
		return clazz;
	}
}
