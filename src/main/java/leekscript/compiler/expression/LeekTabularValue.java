package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekTabularValue extends AbstractExpression {

	private AbstractExpression mTabular;
	private AbstractExpression mCase;
	private boolean mLeftValue = false;

	public void setTabular(AbstractExpression tabular) {
		mTabular = tabular;
	}

	public void setCase(AbstractExpression caseexp) {
		mCase = caseexp;
	}

	public AbstractExpression getTabular() {
		return mTabular;
	}

	public AbstractExpression getCase() {
		return mCase;
	}

	@Override
	public int getNature() {
		return TABULAR_VALUE;
	}

	@Override
	public Type getType() {
		return Type.ANY;
	}

	@Override
	public String getString() {
		return (mTabular == null ? "null" : mTabular.getString()) + "[" + (mCase == null ? "null" : mCase.getString()) + "]";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// On doit vérifier qu'on a affaire : soit à une expression tabulaire, soit à une variable, soit à une globale
		// throw new LeekExpressionException(this, "Ce n'est pas un tableau valide");
		if (!mTabular.isLeftValue()) {
			mLeftValue = false;
		}

		// Sinon on valide simplement les deux expressions
		mTabular.validExpression(compiler, mainblock);
		mCase.validExpression(compiler, mainblock);
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mLeftValue) {
			mTabular.writeJavaCode(mainblock, writer);
			writer.addCode(".getValue().getOrCreate(mUAI, ");
			mCase.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			mTabular.writeJavaCode(mainblock, writer);
			writer.addCode(".getValue().get(mUAI, ");
			mCase.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		}
	}

	public void setLeftValue(boolean b) {
		mLeftValue = b;
	}

	@Override
	public boolean isLeftValue() {
		return true;
	}

	@Override
	public boolean nullable() {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mTabular.analyze(compiler);
		mCase.analyze(compiler);
		operations = mTabular.getOperations() + mCase.getOperations();
	}
}
