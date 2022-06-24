package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.Expression;

public abstract class LeekInstruction extends Expression {

	public abstract String getCode();

	public abstract void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer);

	public abstract int getEndBlock();

	public abstract boolean putCounterBefore();

	public void preAnalyze(WordCompiler compiler) {}

	public void analyze(WordCompiler compiler) {}

	public abstract Location getLocation();

	public int getOperations() {
		return 0;
	}
}
