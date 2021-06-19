package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekParenthesis extends AbstractExpression {

	private final AbstractExpression mExpression;

	public LeekParenthesis(AbstractExpression exp) {
		mExpression = exp;
	}

	public AbstractExpression getExpression() {
		return mExpression;
	}

	@Override
	public int getNature() {
		return 0;
	}

	@Override
	public Type getType() {
		return mExpression.getType();
	}

	@Override
	public AbstractExpression trim() {
		return mExpression.trim();
	}

	@Override
	public String getString() {
		return "(" + mExpression.getString() + ")";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("(");
		mExpression.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return mExpression.validExpression(compiler, mainblock);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mExpression.analyze(compiler);
		operations = mExpression.getOperations();
	}
}
