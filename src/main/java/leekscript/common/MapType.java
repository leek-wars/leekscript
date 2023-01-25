package leekscript.common;

import leekscript.compiler.JavaWriter;

public class MapType extends Type {

	private Type key;
	private Type value;

	public MapType(Type key, Type value) {
		super(key == Type.ANY && value == Type.ANY ? "Map" : "Map<" + key.name + ", " + value.name + ">", "m", "MapLeekValue", "MapLeekValue", "new MapLeekValue()");
		this.key = key;
		this.value = value;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof MapType) {
			var keycast = this.key.accepts(((MapType) type).key);
			var valuecast = this.value.accepts(((MapType) type).value);
			// On retourne le pire cast
			return CastType.values()[Math.max(keycast.ordinal(), valuecast.ordinal())];
		}
		return super.accepts(type);
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "new MapLeekValue(" + writer.getAIThis() + ")";
	}

	public Type element() {
		return Type.compound(value, Type.NULL);
	}

	@Override
	public boolean isMap() {
		return true;
	}

	public boolean canBeIterable() {
		return true;
	}
}
