package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekBreakInstruction implements LeekInstruction {

	// private final int mCount;
	private final int mLine;
	private final int mAI;

	public LeekBreakInstruction(int count, int line, int ai) {
		// mCount = count;
		mLine = line;
		mAI = ai;
	}

	@Override
	public String getCode() {
		return "break";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCounter(1);
		writer.addLine("break;", mLine, mAI);
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
