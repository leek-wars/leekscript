package leekscript.common;

import java.util.HashSet;

import leekscript.compiler.Complete;
import leekscript.compiler.Complete.CompleteCategory;
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

	@Override
	public Complete complete() {
		// System.out.println("ClassType " + clazz.getName() + " complete");
		var complete = new Complete(this);
		// Un nom par catégorie : une méthode redéfinie à chaque niveau d'héritage, ou
		// surchargée, ne fait qu'une proposition. La classe la plus dérivée passe en premier.
		var fields = new HashSet<String>();
		var current = this.clazz;
		while (current != null) {
			for (var field : current.getFields().entrySet()) {
				if (fields.add(field.getKey())) {
					complete.add(CompleteCategory.FIELD, field.getKey(), field.getValue().getType());
				}
			}
			current = current.getParent();
		}
		var methods = new HashSet<String>();
		current = this.clazz;
		while (current != null) {
			for (var method : current.getMethods().entrySet()) {
				if (!methods.add(method.getKey())) continue;
				method.getValue().values().stream().findFirst()
					.ifPresent(version -> complete.add(CompleteCategory.METHOD, method.getKey(), version.block.getType()));
			}
			current = current.getParent();
		}
		// System.out.println("complete = " + complete);
		return complete;
	}
}
