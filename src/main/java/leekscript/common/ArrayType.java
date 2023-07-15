package leekscript.common;

import leekscript.compiler.JavaWriter;

public class ArrayType extends Type {

	private Type type;

	public ArrayType(Type type) {
		super(type == Type.VOID ? "Array<empty>" : type == Type.ANY ? "Array" : "Array<" + type.toString() + ">", "a", "ArrayLeekValue", "ArrayLeekValue", "new ArrayLeekValue()");
		this.type = type;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof ArrayType at) {
			if (at.type == Type.VOID) return CastType.UPCAST;
			if (this.type == Type.VOID) return CastType.UPCAST;
			var cast = this.type.accepts(at.type);
			if (cast.ordinal() >= CastType.SAFE_DOWNCAST.ordinal()) return CastType.INCOMPATIBLE;
			return cast;
		}
		return super.accepts(type);
	}

	@Override
	public String getJavaPrimitiveName(int version) {
		return version >= 4 ? "ArrayLeekValue" : "LegacyArrayLeekValue";
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return version >= 4 ? "new ArrayLeekValue(" + writer.getAIThis() + ")" : "new LegacyArrayLeekValue(" + writer.getAIThis() + ")";
	}

	@Override
	public String getJavaName(int version) {
		return version >= 4 ? "ArrayLeekValue" : "LegacyArrayLeekValue";
	}

	public Type key() {
		return Type.INT;
	}

	@Override
	public Type element() {
		return type;
	}

	@Override
	public Type elementAccess(int version, boolean strict) {
		if (strict) {
			return type;
		}
		// if (version == 1) {
		// 	return new BoxType(Type.compound(type, Type.NULL));
		// }
		return Type.compound(type, Type.NULL);
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		if (strict) {
			return type;
		}
		// if (version == 1) {
		// 	return new BoxType(Type.compound(type, Type.NULL));
		// }
		return Type.compound(type, Type.NULL);
	}

	public boolean isArray() {
		return true;
	}

	public boolean canBeIterable() {
		return true;
	}

	public boolean isIterable() {
		return true;
	}

	@Override
	public boolean isIndexable() {
		return true;
	}

	@Override
	public boolean canBeIndexable() {
		return true;
	}
}
