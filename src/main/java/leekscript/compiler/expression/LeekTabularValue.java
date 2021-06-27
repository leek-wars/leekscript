package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.ClassMethodBlock;
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
		writer.addCode("get(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		writer.addCode(writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null");
		writer.addCode(")");
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		assert(mLeftValue && !mTabular.nullable());
		writer.addCode("getBox(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileSetCopy(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);

		// writer.compileClone(mainblock, expr);
		writer.addCode(")");
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_inc(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_pre_inc(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_dec(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_pre_dec(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_add_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_sub_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_mul_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_mod_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_div_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_pow_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_bor_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_band_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_bxor_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_shl_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_shr_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_ushr_eq(");
		mTabular.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
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
