package leekscript.common;

import leekscript.compiler.JavaWriter;

public class LegacyArrayType extends Type {

	public LegacyArrayType() {
		super("Array", "a", "LegacyArrayLeekValue", "LegacyArrayLeekValue", "new LegacyArrayLeekValue()");
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof LegacyArrayType at) {
			return CastType.EQUALS;
		}
		return super.accepts(type);
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "new LegacyArrayLeekValue(" + writer.getAIThis() + ")";
	}

	public Type key() {
		return Type.ANY;
	}

	@Override
	public Type element() {
		return Type.ANY;
	}

	@Override
	public Type elementAccess(int version, boolean strict) {
		return Type.ANY;
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return Type.ANY;
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
