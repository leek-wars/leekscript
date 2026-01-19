package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LeekParenthesis extends Expression {

	private final Expression mExpression;
	private final Token leftParenthesis;
	private final Token rightParenthesis;

	public LeekParenthesis(Expression exp, Token leftParenthesis, Token rightParenthesis) {
		mExpression = exp;
		this.leftParenthesis = leftParenthesis;
		this.rightParenthesis = rightParenthesis;
		leftParenthesis.setExpression(this);
		rightParenthesis.setExpression(this);
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
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addCode("(");
		mExpression.writeJavaCode(mainblock, writer, false);
		writer.addCode(")");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return mExpression.validExpression(compiler, mainblock);
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.analyze(compiler);
		operations = mExpression.getOperations();
	}

	@Override
	public Location getLocation() {
		return new Location(this.leftParenthesis.getLocation(), this.rightParenthesis.getLocation());
	}
}
