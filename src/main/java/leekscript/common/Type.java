package leekscript.common;

public class Type {

	public static Type ANY = new Type("any", '?');
	public static Type NULL = new Type("null", 'u');
	public static Type BOOL = new Type("bool", 'b');
	public static Type NUMBER = new Type("number", 'n');
	public static Type INT = new Type("int", 'i');
	public static Type REAL = new Type("real", 'r');
	public static Type STRING = new Type("string", 's');
	public static Type ARRAY = new Type("array", 'a');
	public static Type OBJECT = new Type("object", 'o');
	public static Type FUNCTION = new Type("function", 'f');

	public String name;
	public char signature;

	public Type(String name, char signature) {
		this.name = name;
		this.signature = signature;
	}

	public boolean accepts(Type type) {
		if (type == this) return true;
		if (this == ANY) return true;
		if (this == NUMBER) {
			if (type == BOOL || type == INT || type == REAL) return true;
		}
		if (this == REAL) {
			if (type == INT) return true;
		}
		if (this == BOOL) return true;
		return false;
	}

	public boolean isNumber() {
		return this == NUMBER || this == INT || this == REAL;
	}

	public char getSignature() {
		return signature;
	}
}
