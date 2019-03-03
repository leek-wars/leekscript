package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;

public class LeekVariableDeclarationInstruction implements LeekInstruction {

	private final String mName;
	private final int mLine;
	private final int mAI;
	private AbstractExpression mValue = null;
	private boolean mMustSepare = false;

	public LeekVariableDeclarationInstruction(String name, int line, int ai) {
		mName = name;
		mLine = line;
		mAI = ai;
	}

	public void mustSepare() {
		mMustSepare = true;
	}

	public void setValue(AbstractExpression value) {
		mValue = value;
	}

	public String getName() {
		return mName;
	}

	@Override
	public String getCode() {
		if(mValue == null) return "var " + mName;
		return "var " + mName + " = " + mValue.getString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if(!mMustSepare){
			writer.addCode("final VariableLeekValue user_" + mName + " = new VariableLeekValue(mUAI, ");
			if(mValue != null) mValue.writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
			writer.addLine(");", mLine, mAI);
		}
		else{
			writer.addCode("final VariableLeekValue user_" + mName + " = new VariableLeekValue(mUAI, LeekValueManager.NULL); user_" + mName + ".set(mUAI, ");
			if(mValue != null) mValue.writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
			writer.addLine(");", mLine, mAI);
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
