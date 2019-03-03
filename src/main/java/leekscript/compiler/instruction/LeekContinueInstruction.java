package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekContinueInstruction implements LeekInstruction {

	// private final int mCount;
	private final int mLine;
	private final int mAI;

	public LeekContinueInstruction(int count, int line, int ai) {
		// mCount = count;
		mLine = line;
		mAI = ai;
	}

	@Override
	public String getCode() {
		return "continue";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCounter(1);
		writer.addLine("continue;", mLine, mAI);
	}

	@Override
	public int getEndBlock() {
		return 2;
	}

	@Override
	public boolean putCounterBefore() {
		return true;
	}
}
