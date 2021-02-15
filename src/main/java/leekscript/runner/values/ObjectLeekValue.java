package leekscript.runner.values;

import java.util.HashMap;
import java.util.Set;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekAnonymousFunction;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public class ObjectLeekValue extends AbstractLeekValue {

	public final ClassLeekValue clazz;
	public final HashMap<String, VariableLeekValue> fields = new HashMap<>();

	public ObjectLeekValue(ClassLeekValue clazz) {
		this.clazz = clazz;
	}

	public void addField(AI ai, String field, AbstractLeekValue value) throws LeekRunException {
		fields.put(field, new VariableLeekValue(ai, value));
	}

	@Override
	public AbstractLeekValue getField(AI ai, String field) throws LeekRunException {
		// System.out.println("getField " + field);
		if (field.equals("class")) {
			return clazz;
		}
		AbstractLeekValue result = fields.get(field);
		if (result != null) {
			return result;
		}
		var method = clazz.genericMethods.get(field);
		if (method != null) {
			return method;
		}
		ai.addSystemLog(AILog.ERROR, AILog.UNKNOWN_FIELD, new String[] { clazz.name, field });
		return LeekValueManager.NULL;
	}

	@Override
	public AbstractLeekValue get(AI ai, AbstractLeekValue value) throws LeekRunException {
		return getField(ai, value.getString(ai));
	}

	@Override
	public AbstractLeekValue callMethod(AI ai, String method, AbstractLeekValue... arguments) throws LeekRunException {
		LeekAnonymousFunction result = clazz.methods.get(method);
		if (result == null) {
			int underscore = method.lastIndexOf("_");
			int argCount = Integer.parseInt(method.substring(underscore + 1));
			String methodRealName = method.substring(0, underscore) + "(";
			for (int i = 0; i < argCount; ++i) {
				if (i > 0) methodRealName += ", ";
				methodRealName += "x";
			}
			methodRealName += ")";
			ai.addSystemLog(AILog.ERROR, AILog.UNKNOWN_METHOD, new String[] { clazz.name, methodRealName });
			return LeekValueManager.NULL;
		}
		// Call method with new arguments, add the object at the beginning
		return result.run(ai, this, arguments);
	}

	@Override
	public int getSize() {
		return fields.size();
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

		if (clazz.methods.containsKey("string_0")) {
			var result = clazz.methods.get("string_0").run(ai, this, new AbstractLeekValue[] {});
			if (result.getType() != STRING) {
				ai.addSystemLog(AILog.ERROR, AILog.STRING_METHOD_MUST_RETURN_STRING, new String[] { clazz.name });
			} else {
				return result.getString(ai, visited);
			}
		}

		var sb = new StringBuilder(clazz.name + " {");
		boolean first = true;
		for (HashMap.Entry<String, VariableLeekValue> field : fields.entrySet()) {
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
	public int getType() {
		return OBJECT;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp instanceof ObjectLeekValue) {

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

}
