package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekVariable extends AbstractExpression {

	private final String mVariable;

	public LeekVariable(String variable) {
		mVariable = variable;
	}

	@Override
	public int getType() {
		return VARIABLE;
	}

	@Override
	public String getString() {
		return mVariable;
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("user_" + mVariable);
	}
}
