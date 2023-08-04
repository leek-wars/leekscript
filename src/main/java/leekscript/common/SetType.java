package leekscript.common;

public class SetType extends Type {

	private Type type;

	public SetType(Type type) {
		super(type == Type.VOID ? "Set<empty>" : type == Type.ANY ? "Set" : "Set<" + type.toString() + ">", "h", "SetLeekValue", "SetLeekValue", "new SetLeekValue()");
		this.type = type;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof SetType at) {
			if (at.type == Type.VOID) return CastType.UPCAST;
			if (this.type == Type.VOID) return CastType.UPCAST;
			var cast = this.type.accepts(at.type);
			if (cast.ordinal() >= CastType.SAFE_DOWNCAST.ordinal()) return CastType.UNSAFE_DOWNCAST;
			return cast;
		}
		return super.accepts(type);
	}

	@Override
	public Type element() {
		return type;
	}

	public boolean isSet() {
		return true;
	}

	public boolean canBeIterable() {
		return true;
	}

	public boolean isIterable() {
		return true;
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() * 31 + 1;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof SetType at) {
			return this.type.equals(at.type);
		}
		return false;
	}
}
