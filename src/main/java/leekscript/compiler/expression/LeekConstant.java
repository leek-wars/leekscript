package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class LeekConstant extends AbstractExpression {

	private final String mConstantName;

	public LeekConstant(String word) {
		mConstantName = word;
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
		int type = LeekConstants.getType(mConstantName);
		if(type == LeekFunctions.INT) writer.addCode("LeekValueManager.getLeekIntValue(LeekConstants." + mConstantName + ")");
		else if(type == LeekFunctions.DOUBLE) writer.addCode("new DoubleLeekValue(LeekConstants." + mConstantName + ")");
		else writer.addCode("LeekValueManager.NULL");
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		//La vérification se fait en amont
		return true;
	}

}
