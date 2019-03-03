package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekExpressionFunction extends AbstractExpression {

	private final ArrayList<AbstractExpression> mParameters = new ArrayList<AbstractExpression>();
	private AbstractExpression mExpression = null;

	public LeekExpressionFunction() {
	}

	public void setExpression(AbstractExpression expression) {
		mExpression = expression;
	}

	public void addParameter(AbstractExpression param) {
		mParameters.add(param);
	}

	@Override
	public int getType() {
		return FUNCTION;
	}

	@Override
	public String getString() {
		String str = mExpression.getString() + "(";
		for(int i = 0; i < mParameters.size(); i++){
			if(i > 0) str += ", ";
			str += mParameters.get(i).getString();
		}
		return str + ")";
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		if(mExpression == null || !mExpression.validExpression(mainblock)) return false;

		//Vérification de chaque paramètre
		for(AbstractExpression parameter : mParameters){
			parameter.validExpression(mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		mExpression.writeJavaCode(mainblock, writer);
		writer.addCode(".executeFunction(mUAI, new AbstractLeekValue[]{");
		for(int i = 0; i < mParameters.size(); i++){
			if(i > 0) writer.addCode(", ");
			if(i < mParameters.size()) mParameters.get(i).writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
		}
		writer.addCode("})");
	}
}
