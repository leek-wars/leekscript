package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekNumber extends AbstractExpression {

	private final double mValue;

	public LeekNumber(double value) {
		mValue = value;
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
		if(mValue == (int) mValue){
			writer.addCode("LeekValueManager.getLeekIntValue(" + ((int) mValue) + ")");
		}
		else{
			writer.addCode("new DoubleLeekValue(" + mValue + ")");
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
