package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class BlankInstruction implements LeekInstruction {

	@Override
	public String getCode() {
		return "";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
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

	}

	@Override
	public int getOperations() {
		return 0;
	}
}
