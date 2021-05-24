package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekNumber extends AbstractExpression {

	private final double mValue;
	private final boolean floating;

	public LeekNumber(double value, boolean floating) {
		mValue = value;
		this.floating = floating;
	}

	@Override
	public int getType() {
		return NUMBER;
	}

	@Override
	public String getString() {
		return String.valueOf(mValue);
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		//Pour un nombre pas de soucis
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (floating || mValue != (int) mValue) {
			writer.addCode("new DoubleLeekValue(" + mValue + ")");
		} else {
			writer.addCode("LeekValueManager.getLeekIntValue(" + ((int) mValue) + ")");
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
