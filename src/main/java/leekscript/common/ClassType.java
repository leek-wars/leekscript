package leekscript.common;

import leekscript.compiler.instruction.ClassDeclarationInstruction;

public class ClassType extends Type {

	private ClassDeclarationInstruction clazz;

	public ClassType(ClassDeclarationInstruction clazz) {
		super(clazz.getName(), "c", "u_" + clazz.getName(), "u_" + clazz.getName(), "null");
		this.clazz = clazz;
	}

	public Type member(String member) {
		var m = clazz.getMember(member);
		if (m != null) {
			return m.getType();
		}
		return Type.ERROR;
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
	public Type key() {
		return Type.STRING;
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		if (key != null) {
			var m = clazz.getMember(key);
			if (m != null) {
				return m.getType();
			}
			return Type.VOID;
		}
		return Type.ANY;
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return clazz;
	}

	public CastType accepts(Type type) {
		if (type instanceof ClassType c) {
			// Equals
			if (this.clazz == c.clazz) return CastType.EQUALS;
			// this = Animal, type = Dog
			if (c.clazz.descendsFrom(this.clazz)) return CastType.UPCAST;
			// this = Dog, type = Animal
			if (this.clazz.descendsFrom(c.clazz)) return CastType.UNSAFE_DOWNCAST;
			// Incompatible
			return CastType.INCOMPATIBLE;
		}
		return super.accepts(type);
	}

	@Override
	public Type returnType() {
		return this.clazz.getType();
	}
}
