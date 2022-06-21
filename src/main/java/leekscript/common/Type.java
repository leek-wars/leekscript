package leekscript.common;

import leekscript.compiler.JavaWriter;

public class Type {

	public static Type ANY = new Type("any", "Object", "null");
	public static Type NULL = new Type("null", "Object", "null");
	public static Type BOOL = new Type("bool", "boolean", "false");
	public static Type NUMBER = new Type("number", "Number", "0.0");
	public static Type INT = new Type("int", "long", "0l");
	public static Type REAL = new Type("real", "double", "0.0");
	public static Type STRING = new Type("string", "String", "\"\"");
	public static Type ARRAY = new Type("array", "ArrayLeekValue", "new ArrayLeekValue()");
	public static Type OBJECT = new Type("object", "ObjectLeekValue", "new ObjectLeekValue()");
	public static Type FUNCTION = new Type("function", "FunctionLeekValue", "new FunctionLeekValue(-1)");
	public static Type MAP = new Type("map", "MapLeekValue", "new MapLeekValue()");
	public static Type CLASS = new Type("class", "ClassLeekValue", "new ClassLeekValue()");
	public static Type VOID = new Type("void", "Object", "null");

	public static enum CastType {
		EQUALS,
		UPCAST,
		SAFE_DOWNCAST,
		UNSAFE_DOWNCAST,
		INCOMPATIBLE
	}

	public String name;
	private String javaName;
	private String defaultValue;

	public Type(String name, String javaName, String defaultValue) {
		this.name = name;
		this.javaName = javaName;
		this.defaultValue = defaultValue;
	}

	public CastType compare(Type type) {
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
		}
		if (this == INT) {
			if (type == REAL) {
				return CastType.SAFE_DOWNCAST;
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
}
