package leekscript.common;

import leekscript.compiler.JavaWriter;

public class BigIntegerType extends Type {

	public BigIntegerType() {
		super("big_integer", "l", "BigIntegerValue", "BigIntegerValue", "new BigIntegerValue()");
	}
	
	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "new BigIntegerValue(" + writer.getAIThis() + ", 0)";
	}
	
	@Override
	public boolean canBeIterable() {
		return false;
	}

	@Override
	public boolean isIterable() {
		return false;
	}
	
	@Override
	public boolean canBeCallable() {
		return false;
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
