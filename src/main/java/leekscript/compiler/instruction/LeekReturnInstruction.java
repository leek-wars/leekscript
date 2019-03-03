package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;

public class LeekReturnInstruction implements LeekInstruction {

	private final int mAI;
	private final int mLine;
	private AbstractExpression mExpression = null;

	// private final int mCount;

	public LeekReturnInstruction(int count, AbstractExpression exp, int line, int ai) {
		mExpression = exp;
		// mCount = count;
		mLine = line;
		mAI = ai;
	}

	@Override
	public String getCode() {
		return "return " + (mExpression == null ? "null" : mExpression.getString());
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("return ");
		if (mExpression == null)
			writer.addCode("LeekValueManager.NULL;");
		else {
			mExpression.writeJavaCode(mainblock, writer);
			writer.addLine(";", mLine, mAI);
		}
	}

	@Override
	public int getEndBlock() {
		return 1;
	}

	@Override
	public boolean putCounterBefore() {
		return true;
	}
}
