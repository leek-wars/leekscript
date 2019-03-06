package leekscript.compiler.instruction;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;

public class LeekGlobalDeclarationInstruction implements LeekInstruction {

	private final String mName;
	private AbstractExpression mValue = null;
	private final int mLine;
	private final AIFile<?> mAI;

	public LeekGlobalDeclarationInstruction(String name, int line, AIFile<?> ai) {
		mName = name;
		mLine = line;
		mAI = ai;
	}

	public void setValue(AbstractExpression value) {
		mValue = value;
	}

	public String getName() {
		return mName;
	}

	@Override
	public String getCode() {
		return mName + " = " + mValue.getString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("if(globale_" + mName + " == null){globale_" + mName + " = new VariableLeekValue(mUAI, ");
		if(mValue != null) mValue.writeJavaCode(mainblock, writer);
		else writer.addCode("LeekValueManager.NULL");
		writer.addLine(");}", mLine, mAI);
	}

	public String getJavaDeclaration() {
		return "private VariableLeekValue globale_" + mName + " = null;";
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
