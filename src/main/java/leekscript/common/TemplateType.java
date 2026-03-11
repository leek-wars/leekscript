package leekscript.common;

import java.util.Map;

import leekscript.compiler.JavaWriter;

public class TemplateType extends Type {

	public TemplateType(String name) {
		super(name, "t", "Object", "Object", "null");
	}

	@Override
	public Type substitute(Map<String, Type> substitution) {
		return substitution.getOrDefault(name, this);
	}

	@Override
	public String getJavaPrimitiveName(int version) {
		return "Object";
	}

	@Override
	public String getJavaName(int version) {
		return "Object";
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "null";
	}
}
