package leekscript.common;

import leekscript.compiler.instruction.EnumDeclarationInstruction;

public class EnumValueType extends Type {

	private EnumDeclarationInstruction enumDeclaration;

	public EnumValueType(EnumDeclarationInstruction enumDeclaration) {
		super("Enum<" + (enumDeclaration == null ? "?" : enumDeclaration.getName()) + ">", "E", "EnumLeekValue", "EnumLeekValue", "null");
		this.enumDeclaration = enumDeclaration;
	}

	public Type member(String member) {
		if (member == null) return Type.VOID;
		if (enumDeclaration == null) return Type.VOID;
		var constant = enumDeclaration.getConstant(member);
		if (constant != null) {
			return constant.getType();
		}
		return Type.VOID;
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return member(key);
	}

	@Override
	public EnumDeclarationInstruction getEnumDeclaration() {
		return enumDeclaration;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof EnumValueType et) {
			if (this.enumDeclaration == et.enumDeclaration) return CastType.EQUALS;
			return CastType.UPCAST;
		}
		return super.accepts(type);
	}

	@Override
	public boolean isIndexable() {
		return false;
	}

	@Override
	public String getCode() {
		return enumDeclaration == null ? "Enum" : "Enum<" + enumDeclaration.getName() + ">";
	}
}
