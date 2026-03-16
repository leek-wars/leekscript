package leekscript.common;

import leekscript.compiler.instruction.EnumDeclarationInstruction;

public class EnumType extends Type {

	private EnumDeclarationInstruction enumDeclaration;

	public EnumType(EnumDeclarationInstruction enumDeclaration) {
		super(enumDeclaration.getName(), "e", "Object", "Object", "null");
		this.enumDeclaration = enumDeclaration;
	}

	@Override
	public EnumDeclarationInstruction getEnumDeclaration() {
		return enumDeclaration;
	}

	@Override
	public CastType accepts(Type type) {
		if (type == this) return CastType.EQUALS;
		if (this == ANY) return CastType.UPCAST;
		if (type == ANY) return CastType.UNSAFE_DOWNCAST;
		if (type instanceof EnumType et) {
			if (this.enumDeclaration == et.enumDeclaration) return CastType.EQUALS;
			return CastType.INCOMPATIBLE;
		}
		return super.accepts(type);
	}

	@Override
	public String getCode() {
		return enumDeclaration.getName();
	}
}
