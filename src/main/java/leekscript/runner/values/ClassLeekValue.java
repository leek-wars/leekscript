package leekscript.runner.values;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

import leekscript.runner.values.BigIntegerValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import leekscript.AILog;
import leekscript.ErrorManager;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.Type;

public class ClassLeekValue extends FunctionLeekValue<Object> {

	public static class ClassField {
		Object value;
		AccessLevel level;
		boolean isFinal;
		public ClassField(Object value, AccessLevel level, boolean isFinal) {
			this.value = value;
			this.level = level;
			this.isFinal = isFinal;
		}
	};

	public static class ClassMethod {
		FunctionLeekValue value;
		AccessLevel level;
		public ClassMethod(FunctionLeekValue value, AccessLevel level) {
			this.value = value;
			this.level = level;
		}
	};

	public static class ClassStaticMethod {
		FunctionLeekValue value;
		AccessLevel level;
		public ClassStaticMethod(FunctionLeekValue value, AccessLevel level) {
			this.value = value;
			this.level = level;
		}
	};

	public AI ai;
	public String name;
	public ClassLeekValue parent;
	public Type type;
	public LinkedHashMap<String, ClassField> fields = new LinkedHashMap<>();
	public LinkedHashMap<String, ObjectVariableValue> staticFields = new LinkedHashMap<>();
	public HashMap<Integer, ClassMethod> constructors = new HashMap<>();
	public HashMap<String, ClassMethod> methods = new HashMap<>();
	public HashMap<String, Object> genericMethods = new HashMap<>();
	public HashMap<String, ClassStaticMethod> staticMethods = new HashMap<>();
	public HashMap<String, Object> genericStaticMethods = new HashMap<>();
	public FunctionLeekValue initFields = null;
	public Class<?> clazz;

	private Object fieldsArray;
	private Object staticFieldsArray;
	private Object methodsArray;
	private Object staticMethodsArray;

	public ClassLeekValue(AI ai, String name) {
		this(ai, name, null, null);
	}

	public ClassLeekValue(AI ai, String name, ClassLeekValue parent) {
		this(ai, name, parent, null);
	}

	public ClassLeekValue(AI ai, String name, ClassLeekValue parent, Class<?> clazz) {
		super(0);
		// this.mAnonymous = new LeekAnonymousFunction() {
		// 	@Override
		// 	public Object run(ObjectLeekValue thiz, Object... values) throws LeekRunException {
		// 		return execute(ai, values);
		// 	}
		// };
		this.ai = ai;
		this.name = name;
		this.parent = parent;
		this.type = new Type(name, "c", "ClassLeekValue", "ClassLeekValue", "new ClassLeekValue()");
		this.clazz = clazz;

		if (ai.getVersion() >= 4) {
			this.methodsArray = new ArrayLeekValue(ai);
			this.staticMethodsArray = new ArrayLeekValue(ai);
		} else {
			this.methodsArray = new LegacyArrayLeekValue(ai);
			this.staticMethodsArray = new LegacyArrayLeekValue(ai);
		}
	}

	public void setParent(ClassLeekValue parent) throws LeekRunException {
		this.parent = parent;
		if (ai.getVersion() >= 4) {
			for (var method : (ArrayLeekValue) parent.methodsArray) {
				((ArrayLeekValue) this.methodsArray).add(method);
			}
			for (var method : (ArrayLeekValue) parent.staticMethodsArray) {
				((ArrayLeekValue) this.staticMethodsArray).add(method);
			}
		} else {
			for (var method : (LegacyArrayLeekValue) parent.methodsArray) {
				((LegacyArrayLeekValue) this.methodsArray).push(ai, method.getValue());
			}
			for (var method : (LegacyArrayLeekValue) parent.staticMethodsArray) {
				((LegacyArrayLeekValue) this.staticMethodsArray).push(ai, method.getValue());
			}
		}
	}

	public void addConstructor(int arg_count, FunctionLeekValue function, AccessLevel level) {
		constructors.put(arg_count, new ClassMethod(function, level));
	}

	public void addField(String field, AccessLevel level, boolean isFinal) {
		fields.put(field, new ClassField(null, level, isFinal));
	}

	public void addStaticField(AI ai, String field, Object value, AccessLevel level, boolean isFinal) throws LeekRunException {
		staticFields.put(field, new ObjectVariableValue(ai, value, level, isFinal));
	}

	public void addStaticField(AI ai, String field, Type type, Object value, AccessLevel level, boolean isFinal) throws LeekRunException {
		staticFields.put(field, new ObjectVariableValue(ai, type, value, level, isFinal));
	}

	public void addMethod(String method, int argCount, FunctionLeekValue function, AccessLevel level) throws LeekRunException {
		methods.put(method + "_" + argCount, new ClassMethod(function, level));
		if (ai.getVersion() >= 4) {
			((ArrayLeekValue) this.methodsArray).add(method);
		} else {
			((LegacyArrayLeekValue) this.methodsArray).push(ai, method);
		}
	}

	public void addGenericMethod(String method) {
		genericMethods.put(method, new FunctionLeekValue<Object>(1) {
			public Object run(AI ai, Object thiz, Object... arguments) throws LeekRunException {

				if (arguments.length == 0) {
					ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { LeekValueType.getParamString(arguments), "1+" });
				} else if (arguments[0].getClass() != clazz) {
					ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { LeekValueType.getParamString(arguments), "object" });
				}

				final var methodCode = method + "_" + (arguments.length - 1);
				final var m = methods.get(methodCode);
				if (m != null) {
					return m.value.run(ai, arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
				}
				ai.addSystemLog(leekscript.AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { name, createMethodError(methodCode) });
				return null;
			}
		});
	}

	public void addStaticMethod(String method, int argCount, FunctionLeekValue function, AccessLevel level) throws LeekRunException {
		staticMethods.put("u_" + method + "_" + argCount, new ClassStaticMethod(function, level));
		if (ai.getVersion() >= 4) {
			((ArrayLeekValue) this.staticMethodsArray).add(method);
		} else {
			((LegacyArrayLeekValue) this.staticMethodsArray).push(ai, method);
		}
	}

	public void addGenericStaticMethod(String method) {
		genericStaticMethods.put(method, new FunctionLeekValue(0) {
			public Object run(AI ai, Object thiz, Object... arguments) throws LeekRunException {
				final var methodCode = "u_" + method + "_" + arguments.length;
				final var m = staticMethods.get(methodCode);
				if (m != null) {
					return m.value.run(ai, null, arguments);
				}
				ai.addSystemLog(leekscript.AILog.ERROR, Error.UNKNOWN_METHOD, new String[] { name, createMethodError(methodCode) });
				return null;
			}
		});
	}

	public Object getField(String field) throws LeekRunException {
		return getField(field, this);
	}

	public Object getField(String field, ClassLeekValue fromClass) throws LeekRunException {
		if (field.equals("fields")) {
			return getFieldsArray();
		} else if (field.equals("staticFields")) {
			return getStaticFieldsArray();
		} else if (field.equals("methods")) {
			return getMethodsArray();
		} else if (field.equals("staticMethods")) {
			return getStaticMethodsArray();
		} else if (field.equals("name")) {
			return name;
		} else if (field.equals("super")) {
			return parent;
		}
		// Private
		var result = getStaticField(field, fromClass);
		if (result != null) {
			return result.get();
		}
		var generic = genericMethods.get(field);
		if (generic != null) return generic;
		generic = genericStaticMethods.get(field);
		if (generic != null) return generic;

		if (parent instanceof ClassLeekValue) {
			return parent.getField(field, fromClass);
		}
		return null;
	}

	public ObjectVariableValue getStaticField(String field, ClassLeekValue fromClass) throws LeekRunException {
		var result = staticFields.get(field);
		if (result != null) {
			if (fromClass == this) {
				return result;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(this)) {
					if (result.level == AccessLevel.PRIVATE) {
						ai.addSystemLog(AILog.ERROR, Error.PRIVATE_STATIC_FIELD, new String[] { this.name, field });
						return null;
					}
					return result;
				} else {
					// Public : Access from outside
					if (result.level != AccessLevel.PUBLIC) {
						ai.addSystemLog(AILog.ERROR, result.level == AccessLevel.PROTECTED ? Error.PROTECTED_STATIC_FIELD : Error.PRIVATE_STATIC_FIELD, new String[] { this.name, field });
						return null;
					}
					return result;
				}
			}
		}
		return null;
	}

	public Box getFieldL(String field) throws LeekRunException {
		var result = staticFields.get(field);
		if (result != null) {
			if (result.isFinal) {
				throw new LeekRunException(Error.CANNOT_ASSIGN_FINAL_FIELD);
			}
			return result;
		}
		if (parent instanceof ClassLeekValue) {
			return parent.getFieldL(field);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object initField(String field, Object value) throws LeekRunException {
		var result = staticFields.get(field);
		if (result != null) {
			return result.set(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object setField(String field, Object value) throws LeekRunException {
		var result = staticFields.get(field);
		if (result != null) {
			if (result.isFinal) {
				throw new LeekRunException(Error.CANNOT_ASSIGN_FINAL_FIELD);
			}
			return result.set(value);
		}
		throw new LeekRunException(Error.UNKNOWN_FIELD);
	}

	public Object field_inc(String field) throws LeekRunException {
		var result = getFieldL(field);
		return result.increment();
	}

	public Object field_pre_inc(String field) throws LeekRunException {
		var result = getFieldL(field);
		return result.pre_increment();
	}

	public Object field_dec(String field) throws LeekRunException {
		var result = getFieldL(field);
		return result.decrement();
	}

	public Object field_pre_dec(String field) throws LeekRunException {
		var result = getFieldL(field);
		return result.pre_decrement();
	}

	public Object field_add_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.add_eq(value);
	}

	public Object field_sub_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.sub_eq(value);
	}

	public Object field_mul_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.mul_eq(value);
	}

	public Object field_pow_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.pow_eq(value);
	}

	public Object field_div_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.div_eq(value);
	}

	public Number field_intdiv_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.intdiv_eq(value);
	}

	public Object field_mod_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.mod_eq(value);
	}

	public Number field_bor_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.bor_eq(value);
	}

	public Number field_bxor_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.bxor_eq(value);
	}

	public Number field_band_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.band_eq(value);
	}

	public Number field_shl_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.shl_eq(value);
	}

	public Number field_shr_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.shr_eq(value);
	}

	public Number field_ushr_eq(String field, Object value) throws LeekRunException {
		var result = getFieldL(field);
		return result.ushr_eq(value);
	}

	public Object callMethod(String method, ClassLeekValue fromClass, Object... arguments) throws LeekRunException {
		ai.ops(1);
		var result = getStaticMethod(ai, method, fromClass);
		if (result == null) {
			ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_STATIC_METHOD, new String[] { name, createMethodError(method) });
			return null;
		}

		// Call method with new arguments, add the object at the beginning
		return result.run(ai, null, arguments);
	}

	public Object callStaticField(String field, ClassLeekValue fromClass, Object... arguments) throws LeekRunException {
		ai.ops(1);
		// Already check statically
		var result = staticFields.get(field);

		// Call the static field
		return ai.execute(result.get(), arguments);
	}

	public Object callConstructor(ObjectLeekValue thiz, Object... arguments) throws LeekRunException {
		if (!constructors.containsKey(arguments.length)) {
			ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { name, String.valueOf(arguments.length) });
			return thiz;
		}
		constructors.get(arguments.length).value.run(ai, thiz, arguments);
		return thiz;
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
	public Object run(AI ai, Object thiz, Object... arguments) throws LeekRunException {
		// System.out.println("Class " + name + " execute " + Arrays.toString(arguments));
		if (this == ai.valueClass || this == ai.jsonClass || this == ai.systemClass || this == ai.functionClass || this == ai.classClass || this == ai.intervalClass) {
			ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { name, String.valueOf(arguments.length) });
			return null;
		}
		if (this == ai.nullClass) return null;
		if (this == ai.booleanClass) return false;
		if (this == ai.integerClass) return 0l;
		if (this == ai.bigIntegerClass) return new BigIntegerValue(ai, BigInteger.ZERO);
		if (this == ai.realClass || this == ai.numberClass) return 0.0;
		if (this == ai.stringClass) return "";
		if (this == ai.legacyArrayClass) {
			return new LegacyArrayLeekValue(ai);
		}
		if (this == ai.arrayClass) {
			return new ArrayLeekValue(ai);
		}
		if (this == ai.mapClass) {
			return new MapLeekValue(ai);
		}
		if (this == ai.setClass) {
			return new SetLeekValue(ai);
		}
		if (this == ai.objectClass) return new ObjectLeekValue(ai, ai.objectClass);

		// Create the actual object
		ai.ops(1);
		Object object = null;
		try {
			object = this.clazz.getConstructor(ai.getClass()).newInstance(ai);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
			ErrorManager.exception(e1);
		}

		// Recherche d'un constructeur à N arguments puis N - 1, N - 2 etc.
		int arg_count = arguments.length;
		for (var a = arg_count; a >= 0; --a) {
			try {
				// var types = new Class<?>[a];
				var args = new Object[a];
				for (int i = 0; i < a; ++i) {
				// 	types[i] = Object.class;
					args[i] = arguments[i];
				}
				// var m = this.clazz.getMethod("init", types)
				Method m = null;
				for (var mm : this.clazz.getMethods()) {
					if (mm.getName().equals("init")) {
						// System.out.println(m);
						if (mm.getParameterTypes().length == args.length) {
							m = mm;
							break;
						}
					}
				}
				if (m != null) {
					m.invoke(object, args);
					return object;
				}
			} catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
				if (e instanceof IllegalArgumentException || e instanceof NoSuchMethodException) {
					ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { name, String.valueOf(arguments.length) });
				} else {
					ai.addSystemLog(AILog.ERROR, e);
				}
			}
		}
		ai.addSystemLog(AILog.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { name, String.valueOf(arguments.length) });
		return object;
	}

	private Object getFieldsArray() throws LeekRunException {
		if (fieldsArray == null) {
			if (clazz == null) {
				if (ai.getVersion() >= 4) {
					fieldsArray = new ArrayLeekValue(ai);
				} else {
					fieldsArray = new LegacyArrayLeekValue(ai);
				}
			} else {
				if (ai.getVersion() >= 4) {
					var r = new ArrayLeekValue(ai, clazz.getFields().length);
					for (var f : clazz.getFields()) {
						r.add(f.getName());
					}
					fieldsArray = r;
				} else {
					Object[] values = new Object[clazz.getFields().length];
					int i = 0;
					for (var f : clazz.getFields()) {
						values[i++] = f.getName();
					}
					fieldsArray = new LegacyArrayLeekValue(ai, values);
				}
			}
		}
		return fieldsArray;
	}

	private Object getStaticFieldsArray() throws LeekRunException {
		if (staticFieldsArray == null) {
			if (ai.getVersion() >= 4) {
				var r = new ArrayLeekValue(ai, staticFields.size());
				for (var f : staticFields.entrySet()) {
					r.add(f.getKey());
				}
				staticFieldsArray = r;
			} else {
				Object[] values = new Object[staticFields.size()];
				int i = 0;
				for (var f : staticFields.entrySet()) {
					values[i++] = f.getKey();
				}
				staticFieldsArray = new LegacyArrayLeekValue(ai, values);
			}
		}
		return staticFieldsArray;
	}

	private Object getMethodsArray() throws LeekRunException {
		return methodsArray;
	}

	private Object getStaticMethodsArray() throws LeekRunException {
		return staticMethodsArray;
	}

	public FunctionLeekValue getMethod(AI ai, String method, ClassLeekValue fromClass) throws LeekRunException {
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

	public FunctionLeekValue getStaticMethod(AI ai, String method, ClassLeekValue fromClass) throws LeekRunException {
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

	public FunctionLeekValue getSuperMethod(AI ai, String method, ClassLeekValue fromClass) throws LeekRunException {
		if (parent instanceof ClassLeekValue) {
			return ((ClassLeekValue) parent).getMethod(ai, method, fromClass);
		}
		return null;
	}

	public String string(AI ai) throws LeekRunException {
		return "<class " + name + ">";
	}

	@Override
	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		return "<class " + name + ">";
	}

	public boolean equals(AI ai, Object comp) throws LeekRunException {
		if (comp instanceof ClassLeekValue) {
			return ((ClassLeekValue) comp).name.equals(this.name);
		}
		return false;
	}

	public Object toJSON(AI ai) {
		return name;
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

	public int getArgumentsCount(AI ai) throws LeekRunException {
		return 1;
	}

	public Type getType() {
		return type;
	}
}
