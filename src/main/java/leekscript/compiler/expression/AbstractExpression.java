package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public abstract class AbstractExpression {

	public final static int EXPRESSION = 1;
	public final static int NUMBER = 2;
	public final static int STRING = 3;
	public final static int BOOLEAN = 4;
	public final static int TABULAR_VALUE = 5;
	public final static int VARIABLE = 6;
	public final static int NULL = 7;
	public final static int FUNCTION = 8;
	public final static int GLOBAL = 9;
	public final static int ARRAY = 10;
	public final static int OBJECT = 11;
	public final static int OBJECT_ACCESS = 12;

	protected int operations = 0;

	public abstract int getNature();

	public abstract Type getType();

	public abstract String getString();

	public abstract void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer);

	public abstract boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException;

	public AbstractExpression trim() {
		return this;
	}

	public boolean isLeftValue() {
		return false;
	}

	public boolean nullable() {
		return true;
	}

	public abstract void analyze(WordCompiler compiler);

	public int getOperations() {
		return operations;
	}
}
