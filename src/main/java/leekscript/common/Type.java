package leekscript.common;

import leekscript.compiler.JavaWriter;

public class Type {

	public static Type ANY = new Type("any", "x", "Object", "Object", "null");
	public static Type NULL = new Type("null", "u", "Object", "Object", "null");
	public static Type BOOL = new Type("bool", "b", "boolean", "Boolean", "false");
	public static Type NUMBER = new Type("number", "n", "Number", "Number", "0.0");
	public static Type INT = new Type("int", "i", "long", "Long", "0l");
	public static Type REAL = new Type("real", "r", "double", "Double", "0.0");
	public static Type STRING = new Type("string", "s", "String", "String", "\"\"");
	public static Type ARRAY = new Type("Array", "a", "ArrayLeekValue", "ArrayLeekValue", "new ArrayLeekValue()");
	public static Type OBJECT = new Type("Object", "o", "ObjectLeekValue", "ObjectLeekValue", "new ObjectLeekValue()");
	public static Type FUNCTION = new Type("Function", "f", "FunctionLeekValue", "FunctionLeekValue", "new FunctionLeekValue(-1)");
	public static Type MAP = new Type("Map", "m", "MapLeekValue", "MapLeekValue", "new MapLeekValue()");
	public static Type CLASS = new Type("Class", "c", "ClassLeekValue", "ClassLeekValue", "new ClassLeekValue()");
	public static Type VOID = new Type("void", "v", "Object", "Object", "null");

	public static enum CastType {
		EQUALS,
		UPCAST,
		SAFE_DOWNCAST,
		UNSAFE_DOWNCAST,
		INCOMPATIBLE
	}

	public String name;
	public String signature;
	private String javaPrimitiveName;
	private String javaName;
	private String defaultValue;

	public Type(String name, String signature, String javaPrimitiveName, String javaName, String defaultValue) {
		this.name = name;
		this.signature = signature;
		this.javaPrimitiveName = javaPrimitiveName;
		this.javaName = javaName;
		this.defaultValue = defaultValue;
	}

	public CastType accepts(Type type) {
		if (type == this) return CastType.EQUALS;
		if (this == ANY) return CastType.UPCAST;
		if (type == ANY) return CastType.UNSAFE_DOWNCAST;
		if (this == NUMBER) {
			if (type == INT || type == REAL) return CastType.UPCAST;
		}
		if (this == REAL) {
			if (type == INT) {
				return CastType.UPCAST;
			}
			if (type == NUMBER) {
				return CastType.UNSAFE_DOWNCAST;
			}
		}
		if (this == INT) {
			if (type == REAL) {
				return CastType.SAFE_DOWNCAST;
			}
			if (type == NUMBER) {
				return CastType.UNSAFE_DOWNCAST;
			}
		}
		if (this == FUNCTION) {
			if (type == Type.CLASS) {
				return CastType.UPCAST;
			}
		}
		return CastType.INCOMPATIBLE;
	}

	public boolean isNumber() {
		return this == NUMBER || this == INT || this == REAL;
	}

	public boolean isPrimitiveNumber() {
		return this == INT || this == REAL;
	}

	public String toString() {
		return name;
	}

	public String getSignature() {
		return signature;
	}

	public String getJavaPrimitiveName(int version) {
		if (this == Type.ARRAY) {
			return version >= 4 ? "ArrayLeekValue" : "LegacyArrayLeekValue";
		}
		return javaPrimitiveName;
	}

	public String getJavaName(int version) {
		if (this == Type.ARRAY) {
			return version >= 4 ? "ArrayLeekValue" : "LegacyArrayLeekValue";
		}
		return javaName;
	}

	public String getDefaultValue(JavaWriter writer, int version) {
		if (this == Type.ARRAY) {
			return version >= 4 ? "new ArrayLeekValue(" + writer.getAIThis() + ")" : "new LegacyArrayLeekValue()";
		}
		if (this == Type.MAP) {
			return "new MapLeekValue(" + writer.getAIThis() + ")";
		}
		return defaultValue;
	}

	public Type union(Type type) {
		if (this == Type.VOID) return type;
		if (type == Type.VOID) return this;
		if (this == Type.INT) {
			if (type == Type.REAL) {
				return Type.NUMBER;
			}
		}
		if (this == Type.REAL) {
			if (type == Type.INT) {
				return Type.NUMBER;
			}
		}
		return Type.ANY;
	}

	public Object toJSON() {
		return this.name;
	}

	public boolean isArray() {
		return this == Type.ARRAY;
	}
}
