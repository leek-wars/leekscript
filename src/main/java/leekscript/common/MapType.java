package leekscript.common;

import leekscript.compiler.JavaWriter;

public class MapType extends Type {

	private final Type key;
	private final Type value;

	public MapType(Type key, Type value) {
		super(key == Type.VOID && value == Type.VOID ? "Map<empty>" : key == Type.ANY && value == Type.ANY ? "Map" : "Map<" + key.toString() + ", " + value.toString() + ">", "m", "MapLeekValue", "MapLeekValue", "new MapLeekValue()");
		this.key = key;
		this.value = value;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof MapType mt) {
			if (mt.key == Type.VOID && mt.value == Type.VOID) return CastType.UPCAST;
			var keycast = this.key.accepts(mt.key);
			var valuecast = this.value.accepts(mt.value);
			// On retourne le pire cast
			var cast = CastType.values()[Math.max(keycast.ordinal(), valuecast.ordinal())];
			if (cast.ordinal() >= CastType.SAFE_DOWNCAST.ordinal()) return CastType.UNSAFE_DOWNCAST;
			return cast;
		}
		return super.accepts(type);
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "new MapLeekValue(" + writer.getAIThis() + ")";
	}

	public Type key() {
		return key;
	}

	public Type element() {
		return value;
	}

	public Type elementAccess(int version) {
		return Type.compound(value, Type.NULL);
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return Type.compound(value, Type.NULL);
	}

	@Override
	public boolean isMap() {
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

	@Override
	public int hashCode() {
		return (this.key.hashCode() * 31 + this.value.hashCode()) * 31 + 2;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof MapType mt) {
			return this.key.equals(mt.key) && this.value.equals(mt.value);
		}
		return false;
	}
}
