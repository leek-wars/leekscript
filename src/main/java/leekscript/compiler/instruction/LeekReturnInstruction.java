package leekscript.compiler.instruction;

import leekscript.common.Type;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;

public class LeekReturnInstruction extends LeekInstruction {

	private final Token token;
	private final Expression expression;

	public LeekReturnInstruction(Token token, Expression exp) {
		this.token = token;
		this.expression = exp;
	}

	@Override
	public String getCode() {
		return "return " + (expression == null ? "null" : expression.toString()) + ";";
	}


	@Override
	public void preAnalyze(WordCompiler compiler) {
		if (expression != null) {
			expression.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (expression != null) {
			expression.analyze(compiler);
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("return ");
		if (expression == null) {
			writer.addCode("null;");
		} else {
			if (expression.getOperations() > 0) writer.addCode("ops(");
			var finalExpression = expression.trim();
			if (mainblock.getWordCompiler().getVersion() == 1) {
				finalExpression.compileL(mainblock, writer);
			} else {
				finalExpression.writeJavaCode(mainblock, writer);
			}
			if (finalExpression.getOperations() > 0) writer.addCode(", " + finalExpression.getOperations() + ")");
			writer.addLine(";", getLocation());
		}
	}

	@Override
	public int getEndBlock() {
		return 1;
	}

	@Override
	public boolean putCounterBefore() {
		return true;
	}

	@Override
	public int getOperations() {
		return 0;
	}

	public Location getLocation() {
		return new Location(token.getLocation(), expression.getLocation());
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
