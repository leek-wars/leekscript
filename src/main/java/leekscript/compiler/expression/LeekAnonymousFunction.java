package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekAnonymousFunction extends AbstractExpression {

	private final AnonymousFunctionBlock mBlock;

	public LeekAnonymousFunction(AnonymousFunctionBlock block) {
		mBlock = block;
	}

	@Override
	public int getType() {
		return FUNCTION;
	}

	@Override
	public String getString() {
		return "#Anonymous" + mBlock.getId();
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new FunctionLeekValue(" + mBlock.getId() + ", ");
		mBlock.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

}
