package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekBoolean extends AbstractExpression {

	private final boolean mValue;

	public LeekBoolean(boolean value) {
		mValue = value;
	}

	@Override
	public int getType() {
		return BOOLEAN;
	}

	@Override
	public String getString() {
		return mValue ? "true" : "false";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("LeekValueManager." + (mValue ? "TRUE" : "FALSE"));
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

}
