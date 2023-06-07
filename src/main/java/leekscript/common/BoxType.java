package leekscript.common;

import leekscript.compiler.JavaWriter;

public class BoxType extends Type {

	private Type type;

	public BoxType(Type type) {
		super(type == Type.ANY ? "Box" : "Box<" + type.toString() + ">", "x", "Box", "Box", "new Box()");
		this.type = type;
	}

	@Override
	public String getJavaPrimitiveName(int version) {
		return "Box<" + this.type.getJavaName(version) + ">";
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "new Box<Object>(null)";
	}

	@Override
	public String getJavaName(int version) {
		return "Box<" + this.type.getJavaName(version) + ">";
	}

	@Override
	public Type element() {
		return this.type.element();
	}

	@Override
	public Type key() {
		return this.type.key();
	}

	public boolean isArray() {
		return this.type.isArray();
	}

	public boolean canBeIterable() {
		return this.type.canBeIterable();
	}

	public boolean isIterable() {
		return this.type.isIterable();
	}

	@Override
	public boolean isIndexable() {
		return this.type.isIndexable();
	}

	@Override
	public boolean canBeIndexable() {
		return this.type.canBeIndexable();
	}
}
