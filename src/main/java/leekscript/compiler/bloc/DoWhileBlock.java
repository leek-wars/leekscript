package leekscript.compiler.bloc;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.expression.AbstractExpression;

public class DoWhileBlock extends AbstractLeekBlock {

	private AbstractExpression mCondition = null;

	public DoWhileBlock(AbstractLeekBlock parent, MainLeekBlock main) {
		super(parent, main, 0, 0);
	}

	public void setLineAI(int line, int ai) {
		mLine = line;
		mAI = ai;
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
}
