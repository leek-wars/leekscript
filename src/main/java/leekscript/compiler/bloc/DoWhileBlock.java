package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekBoolean;

public class DoWhileBlock extends AbstractLeekBlock {

	private AbstractExpression mCondition = null;

	public DoWhileBlock(AbstractLeekBlock parent, MainLeekBlock main, AIFile<?> ai) {
		super(parent, main, 0, ai);
	}

	public void setCondition(AbstractExpression condition) {
		mCondition = condition;
	}

	public AbstractExpression getCondition() {
		return mCondition;
	}

	@Override
	public String getCode() {
		return "do{\n" + super.getCode() + "}while(" + mCondition.getString() + ");";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addLine("do {");
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		writer.addCode("} while (ops(");
		// Prevent unreachable code error
		if (mCondition instanceof LeekBoolean) {
			writer.addCode("bool(");
			writer.getBoolean(mainblock, mCondition);
			writer.addCode(")");
		} else {
			writer.getBoolean(mainblock, mCondition);
		}
		writer.addCode(", " + mCondition.getOperations() + ")");
		writer.addLine(");", mLine, mAI);
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	public void analyze(WordCompiler compiler) {
		if (mCondition != null) {
			mCondition.analyze(compiler);
		}
		super.analyze(compiler);
	}
}
