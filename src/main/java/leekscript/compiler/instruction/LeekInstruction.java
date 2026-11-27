package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.bytecode.BytecodeWriter;
import leekscript.compiler.bytecode.Unsupported;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;

public abstract class LeekInstruction extends Expression {

	public abstract String getCode();

	public abstract void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis);

	/** writeJavaCode pour le backend bytecode ; {@link Unsupported} tant que l'instruction ne le sait pas. */
	public void writeBytecode(MainLeekBlock mainblock, BytecodeWriter writer) {
		throw new Unsupported(getClass().getSimpleName());
	}

	public abstract int getEndBlock();

	public abstract boolean putCounterBefore();

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {}

	public abstract Location getLocation();

	public int getOperations() {
		return 0;
	}
}
