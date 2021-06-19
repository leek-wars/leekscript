package leekscript.runner.values;

import java.util.HashMap;
import java.util.Map.Entry;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.runner.LeekAnonymousFunction;
import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.compiler.bloc.AnonymousFunctionBlock;

public class ClassLeekValue extends AbstractLeekValue {

	public static class ClassField {
		AbstractLeekValue value;
		AccessLevel level;
		public ClassField(AbstractLeekValue value, AccessLevel level) {
			this.value = value;
			this.level = level;
		}
	};

	public static class ClassMethod {
		LeekAnonymousFunction value;
		AccessLevel level;
		public ClassMethod(LeekAnonymousFunction value, AccessLevel level) {
			this.value = value;
			this.level = level;
		}
	};

	public String name;
	public AbstractLeekValue parent;
	public HashMap<String, ClassField> fields = new HashMap<>();
	public HashMap<String, ObjectVariableValue> staticFields = new HashMap<>();
	public HashMap<Integer, LeekAnonymousFunction> constructors = new HashMap<>();
	public HashMap<String, ClassMethod> methods = new HashMap<>();
	public HashMap<String, AbstractLeekValue> genericMethods = new HashMap<>();
	public HashMap<String, ClassMethod> staticMethods = new HashMap<>();
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

	public void addField(AI ai, String field, AccessLevel level) {
		fields.put(field, new ClassField(null, level));
	}

	public void addStaticField(AI ai, String field, AbstractLeekValue value, AccessLevel level) throws LeekRunException {
		staticFields.put(field, new ObjectVariableValue(ai, value, level));
	}

	public void addMethod(String method, int argCount, LeekAnonymousFunction function, AccessLevel level) {
		methods.put(method + "_" + argCount, new ClassMethod(function, level));
	}

	public void addGenericMethod(String method) {
		genericMethods.put(method, new FunctionLeekValue(new LeekAnonymousFunction() {
			public AbstractLeekValue run(AI ai, AbstractLeekValue thiz, AbstractLeekValue... arguments) throws LeekRunException {
				final var methodCode = method + "_" + arguments.length;
				final var m = methods.get(methodCode);
				if (m != null) {
					return m.value.run(ai, thiz, arguments);
				}
				ai.addSystemLog(leekscript.AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { name, createMethodError(methodCode) });
				return LeekValueManager.NULL;
			}
		}));
	}

	public void addStaticMethod(String method, int argCount, LeekAnonymousFunction function, AccessLevel level) {
		staticMethods.put(method + "_" + argCount, new ClassMethod(function, level));
	}

	public void addGenericStaticMethod(String method) {
		genericMethods.put(method, new FunctionLeekValue(new LeekAnonymousFunction() {
			public AbstractLeekValue run(AI ai, AbstractLeekValue thiz, AbstractLeekValue... arguments) throws LeekRunException {
				final var methodCode = method + "_" + arguments.length;
				final var m = methods.get(methodCode);
				if (m != null) {
					return m.value.run(ai, null, arguments);
				}
				ai.addSystemLog(leekscript.AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { name, createMethodError(methodCode) });
				return LeekValueManager.NULL;
			}
		}));
	}

	@Override
	public AbstractLeekValue getField(AI ai, String field, ClassLeekValue fromClass) throws LeekRunException {
		// Private
		var result = staticFields.get(field);
		if (result != null) {
			if (fromClass == this) {
				return result;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(this)) {
					if (result.level == AccessLevel.PRIVATE) {
						ai.addSystemLog(AILog.ERROR, Error.PRIVATE_STATIC_FIELD, new String[] { this.name, field });
						return LeekValueManager.NULL;
					}
					return result;
				} else {
					// Public : Access from outside
					if (result.level != AccessLevel.PUBLIC) {
						ai.addSystemLog(AILog.ERROR, result.level == AccessLevel.PROTECTED ? Error.PROTECTED_STATIC_FIELD : Error.PRIVATE_STATIC_FIELD, new String[] { this.name, field });
						return LeekValueManager.NULL;
					}
					return result;
				}
			}
		}
		if (field.equals("name")) {
			return new StringLeekValue(name);
		} else if (field.equals("fields")) {
			return getFieldsArray(ai);
		} else if (field.equals("methods")) {
			return getMethodsArray(ai);
		} else if (field.equals("parent")) {
			return parent;
		}
		if (parent instanceof ClassLeekValue) {
			return parent.getField(ai, field, fromClass);
		}
		return LeekValueManager.NULL;
	}

	@Override
	public AbstractLeekValue callMethod(AI ai, String method, ClassLeekValue fromClass, AbstractLeekValue... arguments) throws LeekRunException {
		ai.addOperations(1);
		var result = getStaticMethod(ai, method, fromClass);
		if (result == null) {
			ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_STATIC_METHOD, new String[] { name, createMethodError(method) });
			return LeekValueManager.NULL;
		}

		// Call method with new arguments, add the object at the beginning
		return result.run(ai, null, arguments);
	}

	public void callConstructor(AI ai, AbstractLeekValue thiz, AbstractLeekValue... arguments) throws LeekRunException {
		ai.addOperations(1);
		if (!constructors.containsKey(arguments.length)) {
			ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { name, String.valueOf(arguments.length) });
			return;
		}
		constructors.get(arguments.length).run(ai, thiz, arguments);
	}

	public static String createMethodError(String method) {
		int underscore = method.lastIndexOf("_");
		int argCount = Integer.parseInt(method.substring(underscore + 1));
		String methodRealName = method.substring(0, underscore) + "(";
		for (int i = 0; i < argCount; ++i) {
			if (i > 0) methodRealName += ", ";
			methodRealName += "x";
		}
		methodRealName += ")";
		return methodRealName;
	}

	/**
	 * Constructors
	 */
	@Override
	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue... arguments) throws LeekRunException {
		ai.addOperations(1);
		// Create the actual object
		ObjectLeekValue object = new ObjectLeekValue(this);

		int arg_count = arguments.length;
		if (constructors.containsKey(arg_count)) {
			return constructors.get(arg_count).run(ai, object, arguments);
		} else {
			if (arg_count > 0) {
				ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { name, String.valueOf(arguments.length) });
			}
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

	public LeekAnonymousFunction getMethod(AI ai, String method, ClassLeekValue fromClass) throws LeekRunException {
		var m = methods.get(method);
		if (m != null) {
			// Private : Access from same class
			if (fromClass == this) {
				return m.value;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(this)) {
					if (m.level == AccessLevel.PRIVATE) {
						ai.addSystemLog(AILog.ERROR, Error.PRIVATE_METHOD, new String[] { this.name, method });
						return null;
					}
					return m.value;
				} else {
					// Public : Access from outside
					if (m.level != AccessLevel.PUBLIC) {
						ai.addSystemLog(AILog.ERROR, m.level == AccessLevel.PROTECTED ? Error.PROTECTED_METHOD : Error.PRIVATE_METHOD, new String[] { this.name, method });
						return null;
					}
					return m.value;
				}
			}
		}
		if (parent instanceof ClassLeekValue) {
			return ((ClassLeekValue) parent).getMethod(ai, method, fromClass);
		}
		return null;
	}

	public LeekAnonymousFunction getStaticMethod(AI ai, String method, ClassLeekValue fromClass) throws LeekRunException {
		var m = staticMethods.get(method);
		if (m != null) {
			// Private : Access from same class
			if (fromClass == this) {
				return m.value;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(this)) {
					if (m.level == AccessLevel.PRIVATE) {
						ai.addSystemLog(AILog.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { this.name, method });
						return null;
					}
					return m.value;
				} else {
					// Public : Access from outside
					if (m.level != AccessLevel.PUBLIC) {
						ai.addSystemLog(AILog.ERROR, m.level == AccessLevel.PROTECTED ? Error.PROTECTED_STATIC_METHOD : Error.PRIVATE_STATIC_METHOD, new String[] { this.name, method });
						return null;
					}
					return m.value;
				}
			}
		}
		if (parent instanceof ClassLeekValue) {
			return ((ClassLeekValue) parent).getStaticMethod(ai, method, fromClass);
		}
		return null;
	}

	public LeekAnonymousFunction getSuperMethod(AI ai, String method, ClassLeekValue fromClass) throws LeekRunException {
		if (parent instanceof ClassLeekValue) {
			return ((ClassLeekValue) parent).getMethod(ai, method, fromClass);
		}
		return null;
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
	public int getV10Type() {
		return CLASS_V10;
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

	public boolean descendsFrom(ClassLeekValue clazz) {
		var current = this;
		while (current != null) {
			if (current == clazz) return true;
			if (current.parent instanceof ClassLeekValue) {
				current = (ClassLeekValue) current.parent;
			} else {
				return false;
			}
		}
		return false;
	}
}
