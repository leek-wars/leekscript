package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekBoolean extends AbstractExpression {

	private final boolean mValue;

	public LeekBoolean(boolean value) {
		mValue = value;
	}

	@Override
	public int getNature() {
		return BOOLEAN;
	}

	@Override
	public Type getType() {
		return Type.BOOL;
	}

	@Override
	public String getString() {
		return mValue ? "true" : "false";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode(mValue ? "true" : "false");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
