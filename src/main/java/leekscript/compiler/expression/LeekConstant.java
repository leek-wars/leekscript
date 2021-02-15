package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.runner.ILeekConstant;
import leekscript.runner.LeekFunctions;

public class LeekConstant extends AbstractExpression {

	private final String mConstantName;
	private final ILeekConstant mConstant;

	public LeekConstant(String word, ILeekConstant constant) {
		mConstantName = word;
		mConstant = constant;
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public String getString() {
		return mConstantName;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mConstant.getType() == LeekFunctions.INT) writer.addCode("LeekValueManager.getLeekIntValue(" + mConstant.getIntValue() + ")");
		else if (mConstant.getType() == LeekFunctions.DOUBLE) writer.addCode("new DoubleLeekValue(" + mConstant.getValue() + ")");
		else writer.addCode("LeekValueManager.NULL");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// La vérification se fait en amont
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
