package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Token;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekInteger extends LeekNumber {

	private final long value;

	public LeekInteger(Token token, long value) {
		super(token, Type.INT);
		this.value = value;
	}

	@Override
	public boolean isInfinity() {
		return false;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode(String.valueOf(value) + "l");
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LeekInteger) {
			return value == ((LeekInteger) o).value;
		}
		return false;
	}
}
