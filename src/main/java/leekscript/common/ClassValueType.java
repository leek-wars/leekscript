package leekscript.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import leekscript.compiler.instruction.ClassDeclarationInstruction;

public class ClassValueType extends Type {

	private ClassDeclarationInstruction clazz;

	public ClassValueType(ClassDeclarationInstruction clazz) {
		super("Class<" + (clazz == null ? "?" : clazz.getName()) + ">", "c", "ClassLeekValue", "ClassLeekValue", "null");
		this.clazz = clazz;
	}

	public Type member(String member) {
		if (member == null) return Type.VOID;
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
		}
		return super.accepts(type);
	}

	@Override
	public CastType acceptsArguments(List<Type> types) {
		if (types.size() == 0 || clazz == null) {
			return CastType.EQUALS;
		}
		// Vérification constructeurs
		var constructor = clazz.getConstructor(types.size());
		if (constructor == null) return CastType.INCOMPATIBLE;

		return constructor.block.getType().acceptsArguments(types);
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

	@Override
	public Type getArgument(int a) {
		if (this.clazz == null) return Type.ANY;
		HashSet<Type> types = new HashSet<Type>();
		for (var construct : this.clazz.getConstructors().entrySet()) {
			if (construct.getValue().block != null) {
				types.add(construct.getValue().block.getType().getArgument(a));
			}
		}
		return Type.compound(types);
	}

	@Override
	public Type getArgument(int argumentCount, int a) {
		if (this.clazz == null) return Type.ANY;

		var constructor = this.clazz.getConstructor(argumentCount);
		if (constructor == null) return Type.ANY;

		return constructor.block.getType().getArgument(a);
	}

	@Override
	public List<Type> getArguments() {
		if (this.clazz == null) return new ArrayList<>();
		var types = new ArrayList<Type>();
		for (var construct : this.clazz.getConstructors().entrySet()) {
			if (construct.getValue().block != null) {
				for (int a = 0; a < construct.getValue().block.getType().getMaxArguments(); ++a) {
					if (a < types.size()) {
						types.set(a, Type.compound(types.get(a), construct.getValue().block.getType().getArgument(a)));
					} else {
						types.add(construct.getValue().block.getType().getArgument(a));
					}
				}
			}
		}
		return types;
	}

	@Override
	public List<Type> getArguments(int argumentCount) {

		if (this.clazz == null) return new ArrayList<>();

		var constructor = this.clazz.getConstructor(argumentCount);
		if (constructor == null) return new ArrayList<>();

		return constructor.block.getType().getArguments();
	}

	@Override
	public String getCode() {
		return this.clazz == null ? "Class" : "Class<" + this.clazz.getName() + ">";
	}
}
