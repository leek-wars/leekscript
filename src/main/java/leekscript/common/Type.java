package leekscript.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import leekscript.compiler.JavaWriter;

public class Type {

	public static final Map<HashSet<Type>, Type> compoundTypes = new HashMap<>();

	public static final Type ANY = new Type("any", "x", "Object", "Object", "null");
	public static final Type NULL = new Type("null", "u", "Object", "Object", "null");
	public static final Type BOOL = new Type("bool", "b", "boolean", "Boolean", "false");
	public static final Type NUMBER = new Type("number", "n", "Number", "Number", "0.0");
	public static final Type INT = new Type("int", "i", "long", "Long", "0l");
	public static final Type REAL = new Type("real", "r", "double", "Double", "0.0");
	public static final Type STRING = new Type("string", "s", "String", "String", "\"\"");
	public static final Type ARRAY = new Type("Array", "a", "ArrayLeekValue", "ArrayLeekValue", "new ArrayLeekValue()");
	public static final Type OBJECT = new Type("Object", "o", "ObjectLeekValue", "ObjectLeekValue", "new ObjectLeekValue()");
	public static final Type FUNCTION = new Type("Function", "f", "FunctionLeekValue", "FunctionLeekValue", "new FunctionLeekValue(-1)");
	public static final Type MAP = new Type("Map", "m", "MapLeekValue", "MapLeekValue", "new MapLeekValue()");
	public static final Type CLASS = new Type("Class", "c", "ClassLeekValue", "ClassLeekValue", "new ClassLeekValue()");
	public static final Type VOID = new Type("void", "v", "Object", "Object", "null");
	public static final Type INT_OR_NULL = compound(Type.INT, Type.NULL);
	public static final Type ARRAY_OR_NULL = compound(Type.ARRAY, Type.NULL);
	public static final Type STRING_OR_NULL = compound(Type.STRING, Type.NULL);

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

		if (type instanceof CompoundType) {
			var best = CastType.INCOMPATIBLE;
			var worst = CastType.EQUALS;
			for (var t : ((CompoundType) type).getTypes()) {
				var r = this.accepts(t);
				if (r.ordinal() > worst.ordinal()) worst = r;
				if (r.ordinal() < best.ordinal()) best = r;
			}
			// Si un est compatible, le tout est compatible
			if (worst == CastType.INCOMPATIBLE && best != CastType.INCOMPATIBLE) return CastType.UNSAFE_DOWNCAST;
			// Sinon on prend le pire
			return worst;
		}

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

	public static Type compound(Type... types) {
		var set = new HashSet<Type>(Arrays.asList(types));
		var cached = compoundTypes.get(set);
		if (cached != null) return cached;
		var type = new CompoundType(types);
		compoundTypes.put(set, type);
		return type;
	}
}
