package leekscript.compiler.instruction;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekObjectAccess;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekArrayAccess;
import leekscript.compiler.expression.LeekTernaire;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;

public class LeekExpressionInstruction extends LeekInstruction {

	private final Expression mExpression;

	public LeekExpressionInstruction(Expression expression) {
		mExpression = expression;
	}

	@Override
	public String getCode() {
		return mExpression.toString() + ";";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

		// Simple values are not compiled
		var trimmed = mExpression.trim();
		if (trimmed instanceof LeekExpression && ((LeekExpression) trimmed).getOperator() == Operators.REFERENCE) {
			trimmed = ((LeekExpression) trimmed).getExpression2().trim();
		}
		if (trimmed instanceof LeekNull || trimmed instanceof LeekBoolean || trimmed instanceof LeekNumber || trimmed instanceof LeekString || trimmed instanceof LeekVariable || trimmed instanceof LeekObjectAccess || trimmed instanceof LeekArrayAccess || trimmed instanceof LeekAnonymousFunction) {
			return;
		}

		// Wrap an expression with a function call to avoid 'error: not a statement' error
		if (trimmed instanceof LeekTernaire || (trimmed instanceof LeekExpression && ((LeekExpression) trimmed).needsWrapper())) {
			if (trimmed.getOperations() > 0) writer.addCode("ops(");
			else writer.addCode("nothing(");
			trimmed.writeJavaCode(mainblock, writer);
			if (trimmed.getOperations() > 0) writer.addCode(", " + trimmed.getOperations() + ")");
			else writer.addCode(")");
		} else {
			if (trimmed.getOperations() > 0) writer.addCode("ops(");
			trimmed.writeJavaCode(mainblock, writer);
			if (trimmed.getOperations() > 0) writer.addCode(", " + trimmed.getOperations() + ")");
		}
		// if (trimmed.getOperations() > 0) {
		// 	writer.addCode("; ops(" + trimmed.getOperations() + ")");
		// }
		writer.addLine(";", getLocation());
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		mExpression.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mExpression.analyze(compiler);
	}

	@Override
	public int getOperations() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return mExpression.getLocation();
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
