package leekscript.common;

public class ObjectType extends Type {

	public ObjectType() {
		super("Object", "o", "ObjectLeekValue", "ObjectLeekValue", "new ObjectLeekValue()");
	}

	@Override
	public Type member(String member) {
		if (member.equals("keys")) {
			return Type.function(Type.ARRAY_STRING, new Type[0]);
		}
		return super.member(member);
	}

	public Type element() {
		return Type.ANY;
	}

	@Override
	public Type key() {
		return Type.STRING;
	}

	public Type elementAccess(int version) {
		return Type.ANY;
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return Type.ANY;
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
