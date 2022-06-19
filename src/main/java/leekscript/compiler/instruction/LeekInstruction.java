package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public abstract class LeekInstruction {

	public abstract String getCode();

	public abstract void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer);

	public abstract int getEndBlock();

	public abstract boolean putCounterBefore();

	public void preAnalyze(WordCompiler compiler) {}

	public abstract void analyze(WordCompiler compiler);

	public abstract int getOperations();
}
