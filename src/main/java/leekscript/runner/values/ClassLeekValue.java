package leekscript.runner.values;

import java.util.HashMap;
import java.util.Map.Entry;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.runner.LeekAnonymousFunction;

public class ClassLeekValue extends AbstractLeekValue {

	public String name;
	public AbstractLeekValue parent;
	public HashMap<String, AbstractLeekValue> fields = new HashMap<>();
	public HashMap<String, VariableLeekValue> staticFields = new HashMap<>();
	public HashMap<Integer, LeekAnonymousFunction> constructors = new HashMap<>();
	public HashMap<String, LeekAnonymousFunction> methods = new HashMap<>();
	public HashMap<String, AbstractLeekValue> genericMethods = new HashMap<>();
	public HashMap<String, LeekAnonymousFunction> staticMethods = new HashMap<>();
	public HashMap<String, AbstractLeekValue> genericStaticMethods = new HashMap<>();

	private ArrayLeekValue fieldsArray;
	private ArrayLeekValue methodsArray;

	public ClassLeekValue(String name) {
		this.name = name;
		this.parent = LeekValueManager.NULL;
	}

	public void setParent(AbstractLeekValue parent) {
		this.parent = parent;
	}

	public void addConstructor(int arg_count, LeekAnonymousFunction function) {
		constructors.put(arg_count, function);
	}

	public void addField(AI ai, String field, AbstractLeekValue value) {
		fields.put(field, value);
	}

	public void addStaticField(AI ai, String field, AbstractLeekValue value) throws LeekRunException {
		staticFields.put(field, new VariableLeekValue(ai, value));
	}

	public void addMethod(String method, int argCount, LeekAnonymousFunction function) {
		methods.put(method + "_" + argCount, function);
	}

	public void addGenericMethod(String method, LeekAnonymousFunction function) {
		genericMethods.put(method, new FunctionLeekValue(function));
	}

	public void addStaticMethod(String method, int argCount, LeekAnonymousFunction function) {
		staticMethods.put(method + "_" + argCount, function);
	}

	public void addGenericStaticMethod(String method, LeekAnonymousFunction function) {
		genericStaticMethods.put(method, new FunctionLeekValue(function));
	}

	@Override
	public AbstractLeekValue getField(AI ai, String field) throws LeekRunException {
		AbstractLeekValue result = staticFields.get(field);
		if (result == null) {
			if (field.equals("fields")) {
				return getFieldsArray(ai);
			} else if (field.equals("methods")) {
				return getMethodsArray(ai);
			} else if (field.equals("parent")) {
				return parent;
			}
			return LeekValueManager.NULL;
		}
		return result;
	}

	@Override
	public AbstractLeekValue callMethod(AI ai, String method, AbstractLeekValue... arguments) throws LeekRunException {
		LeekAnonymousFunction result = staticMethods.get(method);
		if (result == null) {
			int underscore = method.lastIndexOf("_");
			int argCount = Integer.parseInt(method.substring(underscore + 1));
			String methodRealName = method.substring(0, underscore) + "(";
			for (int i = 0; i < argCount; ++i) {
				if (i > 0) methodRealName += ", ";
				methodRealName += "x";
			}
			methodRealName += ")";
			ai.addSystemLog(AILog.ERROR, AILog.UNKNOWN_STATIC_METHOD, new String[] { name, methodRealName });
			return LeekValueManager.NULL;
		}
		// Call method with new arguments, add the object at the beginning
		return result.run(ai, null, arguments);
	}

	/**
	 * Constructors
	 */
	@Override
	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue... arguments) throws LeekRunException {

		// Create the actual object
		ObjectLeekValue object = new ObjectLeekValue(this);
		// Add fields
		for (Entry<String, AbstractLeekValue> field : fields.entrySet()) {
			object.addField(ai, field.getKey(), field.getValue());
		}

		int arg_count = arguments.length;
		if (constructors.containsKey(arg_count)) {
			return constructors.get(arg_count).run(ai, object, arguments);
		} else {
			return object;
		}
	}

	private ArrayLeekValue getFieldsArray(AI ai) throws LeekRunException {
		if (fieldsArray == null) {
			AbstractLeekValue[] values = new AbstractLeekValue[fields.size()];
			int i = 0;
			for (var f : fields.entrySet()) {
				values[i++] = new StringLeekValue(f.getKey());
			}
			fieldsArray = new ArrayLeekValue(ai, values);
		}
		return fieldsArray;
	}

	private ArrayLeekValue getMethodsArray(AI ai) throws LeekRunException {
		if (methodsArray == null) {
			AbstractLeekValue[] values = new AbstractLeekValue[genericMethods.size()];
			int i = 0;
			for (var f : genericMethods.entrySet()) {
				values[i++] = new StringLeekValue(f.getKey());
			}
			methodsArray = new ArrayLeekValue(ai, values);
		}
		return methodsArray;
	}

	@Override
	public boolean getBoolean() {
		return true;
	}

	@Override
	public int getInt(AI ai) {
		return 0;
	}

	@Override
	public double getDouble(AI ai) {
		return 0;
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

	@Override
	public String getString(AI ai) {
		return "<class " + name + ">";
	}

	@Override
	public int getType() {
		return CLASS;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp instanceof ClassLeekValue) {
			return ((ClassLeekValue) comp).name.equals(this.name);
		}
		return false;
	}

	@Override
	public Object toJSON(AI ai) {
		return name;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}
}
