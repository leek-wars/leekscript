package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.AbstractExpression;

public class ConditionalBloc extends AbstractLeekBlock {

	private ConditionalBloc mParentCondition = null;
	private AbstractExpression mCondition = null;

	private boolean mPutCounterBefore = false;

	public ConditionalBloc(AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
	}

	public void setParentCondition(ConditionalBloc parent) {
		mParentCondition = parent;
	}

	public ConditionalBloc getParentCondition() {
		return mParentCondition;
	}

	public void setCondition(AbstractExpression condition) {
		mCondition = condition;
	}

	public AbstractExpression getCondition() {
		return mCondition;
	}

	@Override
	public String getCode() {
		String str = "";
		if(mParentCondition == null) str = "if (" + mCondition.getString() + ") {";
		else if(mCondition != null) str = "else if (" + mCondition.getString() + ") {";
		else str = "else {";
		str += "\n" + super.getCode();
		return str + "}";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mParentCondition == null) {
			writer.addCode("if (");
			writer.addCode("ops(");
			writer.getBoolean(mainblock, mCondition);
			writer.addCode(", " + mCondition.getOperations());
			writer.addLine(")) {", mLine, mAI);
		} else if (mCondition != null) {
			writer.addCode("else if (");
			writer.addCode("ops(");
			writer.getBoolean(mainblock, mCondition);
			writer.addCode(", " + mCondition.getOperations());
			writer.addLine(")) {", mLine, mAI);
		}
		else writer.addLine("else {", mLine, mAI);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0) writer.addCounter(1);
		writer.addLine("}");
	}

	public int getConditionEndBlock() {
		if (mEndInstruction == 0) return 0;
		if (mParentCondition != null) {
			int parent = mParentCondition.getConditionEndBlock();
			if (parent == 0) return 0;
			return parent | mEndInstruction;
		}
		return mEndInstruction;
	}

	@Override
	public int getEndBlock() {
		if (mCondition == null) {
			int r = getConditionEndBlock();
			if (r != 0) setPutCounterBefore(true);
			return r;
		}
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return mPutCounterBefore;
	}

	private void setPutCounterBefore(boolean value) {
		if (mParentCondition != null) mParentCondition.setPutCounterBefore(value);
		else mPutCounterBefore = value;
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (mCondition != null) {
			mCondition.analyze(compiler);
		}
		super.analyze(compiler);
	}
}
