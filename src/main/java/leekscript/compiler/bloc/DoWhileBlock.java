package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.AbstractExpression;

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
		writer.addLine("do{");
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		writer.addCode("}while(");
		mCondition.writeJavaCode(mainblock, writer);
		writer.addLine(".getBoolean());", mLine, mAI);
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
