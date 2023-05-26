package leekscript.common;

import leekscript.compiler.JavaWriter;

public class ArrayType extends Type {

	private Type type;

	public ArrayType(Type type) {
		super(type == Type.ANY ? "Array" : "Array<" + type.name + ">", "a", "ArrayLeekValue", "ArrayLeekValue",
				"new ArrayLeekValue()");
		this.type = type;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof ArrayType) {
			return this.type.accepts(((ArrayType) type).type);
		}
		return super.accepts(type);
	}

	@Override
	public String getJavaPrimitiveName(int version) {
		return version >= 4 ? "ArrayLeekValue" : "HybridContainerLeekValue";
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return version >= 4 ? "new ArrayLeekValue(" + writer.getAIThis() + ")" : "new HybridContainerLeekValue()";
	}

	@Override
	public String getJavaName(int version) {
		return version >= 4 ? "ArrayLeekValue" : "HybridContainerLeekValue";
	}

	public Type element() {
		return Type.compound(type, Type.NULL);
	}

	public boolean isArray() {
		return true;
	}

	public boolean canBeIterable() {
		return true;
	}
}
