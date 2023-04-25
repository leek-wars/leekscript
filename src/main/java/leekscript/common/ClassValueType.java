package leekscript.common;

import java.util.List;

import leekscript.compiler.instruction.ClassDeclarationInstruction;

public class ClassValueType extends Type {

	private ClassDeclarationInstruction clazz;

	public ClassValueType(ClassDeclarationInstruction clazz) {
		super("Class<" + (clazz == null ? "?" : clazz.getName()) + ">", "c", "ClassLeekValue", "ClassLeekValue", "null");
		this.clazz = clazz;
	}

	public Type member(String member) {
		if (member.equals("name")) return Type.STRING;
		if (member.equals("super")) return this.clazz != null && this.clazz.getParent() != null ? this.clazz.getParent().getClassValueType() : Type.CLASS;
		if (member.equals("fields")) return Type.ARRAY_STRING;
		if (member.equals("staticFields")) return Type.ARRAY_STRING;
		if (member.equals("methods")) return Type.ARRAY_STRING;
		if (member.equals("staticMethods")) return Type.ARRAY_STRING;
		if (clazz == null) return Type.VOID;
		var m = clazz.getStaticMember(member);
		if (m != null) {
			return m.getType();
		}
		return Type.VOID;
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return member(key);
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return clazz;
	}

	public CastType accepts(Type type) {
		if (type instanceof ClassValueType c) {
			// Equals
			if (this.clazz == c.clazz) return CastType.EQUALS;

			return CastType.UPCAST;
			// // this = Animal, type = Dog
			// if (c.clazz.descendsFrom(this.clazz)) return CastType.UPCAST;
			// // this = Dog, type = Animal
			// if (this.clazz != null && this.clazz.descendsFrom(c.clazz)) return CastType.UNSAFE_DOWNCAST;
			// // Incompatible
			// return CastType.INCOMPATIBLE;
		}
		return super.accepts(type);
	}

	@Override
	public CastType acceptsArguments(List<Type> types) {
		if (types.size() == 0 || clazz == null || clazz.getConstructor(types.size()) != null) {
			return CastType.EQUALS;
		}
		return CastType.INCOMPATIBLE;
	}

	@Override
	public boolean canBeCallable() {
		return true;
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	@Override
	public Type key() {
		return Type.STRING;
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
	public boolean isIndexable() {
		return true;
	}

	@Override
	public boolean canBeIndexable() {
		return true;
	}
}
