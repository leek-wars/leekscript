package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekTernaire;
import leekscript.compiler.expression.LeekVariable;

public class LeekExpressionInstruction implements LeekInstruction {

	private final AbstractExpression mExpression;
	private final int mAI;
	private final int mLine;

	public LeekExpressionInstruction(AbstractExpression expression, int line, int ai) {
		mExpression = expression;
		mLine = line;
		mAI = ai;
	}

	@Override
	public String getCode() {
		return mExpression.getString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mExpression.trim() instanceof LeekTernaire || mExpression.trim() instanceof LeekNull || mExpression.trim() instanceof LeekBoolean) {
			writer.addCode("nothing(");
			mExpression.writeJavaCode(mainblock, writer);
			writer.addLine(");", mLine, mAI);
		}
		else {
			if (mExpression instanceof LeekVariable) {
				// We don't write a code like "variable;", useless and not
				// authorized by Java
			} else {
				mExpression.writeJavaCode(mainblock, writer);
				writer.addLine(";", mLine, mAI);
			}
		}
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}
}
