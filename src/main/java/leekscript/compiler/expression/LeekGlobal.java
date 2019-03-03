package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekGlobal extends AbstractExpression {

	private final String mVariable;

	public LeekGlobal(String variable) {
		mVariable = variable;
	}

	@Override
	public int getType() {
		return GLOBAL;
	}

	@Override
	public String getString() {
		return mVariable;
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		//Pour une globale, la vérification est faite avant l'ajout donc pas besoin de refaire
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("(globale_" + mVariable + "==null?LeekValueManager.NULL:globale_" + mVariable + ")");
	}

}
