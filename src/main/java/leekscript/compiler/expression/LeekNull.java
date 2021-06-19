package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekNull extends AbstractExpression {

	@Override
	public int getNature() {
		return NULL;
	}

	@Override
	public Type getType() {
		return Type.NULL;
	}

	@Override
	public String getString() {
		return "null";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		//Pour une valeur null pas de soucis
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("null");
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
