package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekParenthesis extends Expression {

	private final Expression mExpression;

	public LeekParenthesis(Expression exp) {
		mExpression = exp;
	}

	public Expression getExpression() {
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
	public Expression trim() {
		return mExpression.trim();
	}

	@Override
	public String toString() {
		return "(" + mExpression.toString() + ")";
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
	public void preAnalyze(WordCompiler compiler) {
		mExpression.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mExpression.analyze(compiler);
		operations = mExpression.getOperations();
	}

	@Override
	public Location getLocation() {
		return mExpression.getLocation();
	}
}
