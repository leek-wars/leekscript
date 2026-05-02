package leekscript.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import leekscript.compiler.Complete;
import leekscript.compiler.Complete.CompleteCategory;
import leekscript.compiler.instruction.ClassDeclarationInstruction;

public class GenericClassType extends Type {

	private final ClassDeclarationInstruction clazz;
	private final List<Type> typeArguments;

	public GenericClassType(ClassDeclarationInstruction clazz, List<Type> typeArguments) {
		super(buildName(clazz, typeArguments), "c", "u_" + clazz.getName(), "u_" + clazz.getName(), "null");
		this.clazz = clazz;
		this.typeArguments = new ArrayList<>(typeArguments);
	}

	private static String buildName(ClassDeclarationInstruction clazz, List<Type> typeArguments) {
		if (typeArguments.isEmpty()) return clazz.getName();
		var sb = new StringBuilder(clazz.getName()).append("<");
		for (int i = 0; i < typeArguments.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append(typeArguments.get(i).toString());
		}
		sb.append(">");
		return sb.toString();
	}

	private Map<String, Type> getSubstitution() {
		var params = clazz.getTypeParameters();
		var sub = new HashMap<String, Type>();
		for (int i = 0; i < params.size() && i < typeArguments.size(); i++) {
			sub.put(params.get(i), typeArguments.get(i));
		}
		return sub;
	}

	@Override
	public Type member(String member) {
		var m = clazz.getMember(member);
		if (m != null) {
			return m.getType().substitute(getSubstitution());
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
				return m.getType().substitute(getSubstitution());
			}
			return Type.VOID;
		}
		return Type.ANY;
	}

	@Override
	public ClassDeclarationInstruction getClassDeclaration() {
		return clazz;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof GenericClassType gct) {
			if (this.clazz != gct.clazz) return CastType.INCOMPATIBLE;
			if (this.typeArguments.size() != gct.typeArguments.size()) return CastType.INCOMPATIBLE;
			for (int i = 0; i < typeArguments.size(); i++) {
				if (this.typeArguments.get(i).accepts(gct.typeArguments.get(i)) == CastType.INCOMPATIBLE) {
					return CastType.INCOMPATIBLE;
				}
			}
			return CastType.EQUALS;
		}
		if (type instanceof ClassType ct) {
			if (this.clazz != ct.getClassDeclaration()) return CastType.INCOMPATIBLE;
			return CastType.UNSAFE_DOWNCAST;
		}
		return super.accepts(type);
	}

	@Override
	public Type returnType() {
		return new GenericClassType(clazz, typeArguments);
	}

	@Override
	public Complete complete() {
		var complete = new Complete(this);
		var sub = getSubstitution();
		var current = this.clazz;
		while (current != null) {
			for (var field : current.getFields().entrySet()) {
				complete.add(CompleteCategory.FIELD, field.getKey(), field.getValue().getType().substitute(sub));
			}
			current = current.getParent();
		}
		current = this.clazz;
		while (current != null) {
			for (var method : current.getMethods().entrySet()) {
				for (var version : method.getValue().entrySet()) {
					complete.add(CompleteCategory.METHOD, method.getKey(), version.getValue().block.getType().substitute(sub));
				}
			}
			current = current.getParent();
		}
		return complete;
	}

	public List<Type> getTypeArguments() {
		return new ArrayList<>(typeArguments);
	}
}
