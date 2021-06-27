package leekscript.compiler.instruction;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekObjectAccess;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekTabularValue;
import leekscript.compiler.expression.LeekTernaire;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;

public class LeekExpressionInstruction implements LeekInstruction {

	private final AbstractExpression mExpression;
	private final AIFile<?> mAI;
	private final int mLine;

	public LeekExpressionInstruction(AbstractExpression expression, int line, AIFile<?> ai) {
		mExpression = expression;
		mLine = line;
		mAI = ai;
	}

	@Override
	public String getCode() {
		return mExpression.getString() + ";";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

		// Simple values are not compiled
		var trimmed = mExpression.trim();
		if (trimmed instanceof LeekExpression && ((LeekExpression) trimmed).getOperator() == Operators.REFERENCE) {
			trimmed = ((LeekExpression) trimmed).getExpression2().trim();
		}
		if (trimmed instanceof LeekNull || trimmed instanceof LeekBoolean || trimmed instanceof LeekNumber || trimmed instanceof LeekString || trimmed instanceof LeekVariable || trimmed instanceof LeekObjectAccess || trimmed instanceof LeekTabularValue || trimmed instanceof LeekAnonymousFunction) {
			return;
		}

		// Wrap an expression with a function call to avoid 'error: not a statement' error
		if (trimmed instanceof LeekTernaire || (trimmed instanceof LeekExpression && ((LeekExpression) trimmed).needsWrapper())) {
			writer.addCode("ops(");
			trimmed.writeJavaCode(mainblock, writer);
			writer.addCode(", " + trimmed.getOperations() + ")");
		} else {
			if (trimmed.getOperations() > 0) writer.addCode("ops(");
			trimmed.writeJavaCode(mainblock, writer);
			if (trimmed.getOperations() > 0) writer.addCode(", " + trimmed.getOperations() + ")");
		}
		// if (trimmed.getOperations() > 0) {
		// 	writer.addCode("; ops(" + trimmed.getOperations() + ")");
		// }
		writer.addLine(";", mLine, mAI);
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mExpression.analyze(compiler);
	}

	@Override
	public int getOperations() {
		return 0;
	}
}
