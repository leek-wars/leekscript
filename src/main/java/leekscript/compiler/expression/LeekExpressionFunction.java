package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.runner.LeekFunctions;

public class LeekExpressionFunction extends AbstractExpression {

	private final ArrayList<AbstractExpression> mParameters = new ArrayList<AbstractExpression>();
	private AbstractExpression mExpression = null;

	public LeekExpressionFunction() {}

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
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if(mExpression == null || !mExpression.validExpression(compiler, mainblock)) return false;

		//Vérification de chaque paramètre
		for(AbstractExpression parameter : mParameters){
			parameter.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		boolean addComma = true;
		if (mExpression instanceof LeekObjectAccess) {
			var object = ((LeekObjectAccess) mExpression).getObject();
			object.writeJavaCode(mainblock, writer);
			writer.addCode(".callMethod(mUAI, \"" + ((LeekObjectAccess) mExpression).getField() + "_" + mParameters.size() + "\"");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.METHOD) {
			writer.addCode("u_this.callMethod(mUAI, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\"");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.STATIC_METHOD) {
			writer.addCode("u_class.callMethod(mUAI, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\"");
		} else
		if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SYSTEM_FUNCTION) {
			var variable = (LeekVariable) mExpression;
			String namespace = LeekFunctions.getNamespace(variable.getName());
			writer.addCode("LeekValueManager.getFunction(" + namespace + "." + variable.getName() + ")");
			writer.addCode(".executeFunction(mUAI");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.FUNCTION) {
			writer.addCode("user_function_");
			writer.addCode(((LeekVariable) mExpression).getName());
			writer.addCode("(");
			addComma = false;
		} else {
			mExpression.writeJavaCode(mainblock, writer);
			writer.addCode(".executeFunction(mUAI");
		}
		for (int i = 0; i < mParameters.size(); i++) {
			if (i > 0 || addComma) writer.addCode(", ");
			if (i < mParameters.size()) {
				mParameters.get(i).writeJavaCode(mainblock, writer);
				writer.addCode(".getValue()");
			} else {
				writer.addCode("LeekValueManager.NULL");
			}
		}
		writer.addCode(")");
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mExpression.analyze(compiler);
		for (AbstractExpression parameter : mParameters) {
			parameter.analyze(compiler);
		}
	}
}
