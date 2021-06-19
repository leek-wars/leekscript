package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekBoolean;

public class WhileBlock extends AbstractLeekBlock {

	private AbstractExpression mCondition = null;

	public WhileBlock(AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
	}

	public void setCondition(AbstractExpression condition) {
		mCondition = condition;
	}

	public AbstractExpression getCondition() {
		return mCondition;
	}

	@Override
	public String getCode() {
		return "while(" + mCondition.getString() + "){\n" + super.getCode() + "}";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		// writer.addCounter(1);
		writer.addCode("while (ops(");
		// Prevent unreachable code error
		if (mCondition instanceof LeekBoolean) {
			writer.addCode("bool(");
			writer.getBoolean(mainblock, mCondition);
			writer.addCode(")");
		} else {
			writer.getBoolean(mainblock, mCondition);
		}
		writer.addCode(", " + (mCondition.getOperations()) + ")");
		writer.addLine(") {");
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		writer.addLine("}");
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	public void analyze(WordCompiler compiler) {
		if (mCondition != null) {
			mCondition.analyze(compiler);
		}
		super.analyze(compiler);
	}
}
