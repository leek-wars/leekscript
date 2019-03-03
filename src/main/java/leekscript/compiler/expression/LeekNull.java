package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekNull extends AbstractExpression {

	@Override
	public int getType() {
		return NULL;
	}

	@Override
	public String getString() {
		return "null";
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		//Pour une valeur null pas de soucis
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("LeekValueManager.NULL");
	}

}
