package leekscript.compiler.bloc;

import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpressionException;

public class DoWhileBlock extends AbstractLeekBlock {

	private Expression mCondition = null;
	private final Token token;

	public DoWhileBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
	}

	public void setCondition(Expression condition) {
		mCondition = condition;
	}

	public Expression getCondition() {
		return mCondition;
	}

	@Override
	public String getCode() {
		return "do{\n" + super.getCode() + "}while(" + (mCondition == null ? "" : mCondition.toString()) + ");";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addLine("do {");
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer, false);
		writer.addCode("} while (");
		if (writer.isOperationsEnabled()) {
			writer.addCode("ops(");
		}
		// Prevent unreachable code error
		if (mCondition instanceof LeekBoolean) {
			writer.addCode("bool(");
			writer.getBoolean(mainblock, mCondition, false);
			writer.addCode(")");
		} else {
			writer.getBoolean(mainblock, mCondition, false);
		}
		if (writer.isOperationsEnabled()) {
			writer.addCode(", " + mCondition.getOperations() + ")");
		}
		writer.addLine(");", getLocation());
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (mCondition != null) {
			mCondition.preAnalyze(compiler);
		} else {
			// Le corps a absorbé le while de fermeture (ex. corps vide « do while(c); »
			// ou corps réduit à une boucle « do while(a) while(b); »), laissant la
			// condition nulle. On émet une erreur propre plutôt que de laisser la
			// génération de code planter sur un NullPointerException.
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.WHILE_EXPECTED_AFTER_DO));
		}
		super.preAnalyze(compiler);
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		if (mCondition != null) {
			mCondition.analyze(compiler);
		}
		super.analyze(compiler);
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		return Type.VOID;
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
