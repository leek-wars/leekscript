package leekscript.compiler.bloc;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;

public class ConditionalBloc extends AbstractLeekBlock {

	private ConditionalBloc mParentCondition = null;
	private Expression mCondition = null;
	private final Token token;
	private boolean mPutCounterBefore = false;

	public ConditionalBloc(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
	}

	public void setParentCondition(ConditionalBloc parent) {
		mParentCondition = parent;
	}

	public ConditionalBloc getParentCondition() {
		return mParentCondition;
	}

	public void setCondition(Expression condition) {
		mCondition = condition;
	}

	public Expression getCondition() {
		return mCondition;
	}

	@Override
	public String getCode() {
		String str = "";
		if(mParentCondition == null) str = "if (" + mCondition.toString() + ") {";
		else if(mCondition != null) str = "else if (" + mCondition.toString() + ") {";
		else str = "else {";
		str += "\n" + super.getCode();
		return str + "}";
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		if (mCondition != null) {
			mCondition.preAnalyze(compiler);
		}
		super.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (mCondition != null) {
			mCondition.analyze(compiler);
		}
		super.analyze(compiler);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mParentCondition == null) {
			writer.addCode("if (");
			if (mCondition.getOperations() > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mCondition);
			if (mCondition.getOperations() > 0) {
				writer.addCode(", " + mCondition.getOperations() + ")");
			}
			writer.addLine(") {", getLocation());
		} else if (mCondition != null) {
			writer.addCode("else if (");
			if (mCondition.getOperations() > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mCondition);
			if (mCondition.getOperations() > 0) {
				writer.addCode(", " + mCondition.getOperations() + ")");
			}
			writer.addLine(") {", getLocation());
		}
		else writer.addLine("else {", getLocation());
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0) writer.addCounter(1);
		writer.addLine("}");
	}

	public int getConditionEndBlock() {
		if (mEndInstruction == 0) return 0;
		if (mParentCondition != null) {
			int parent = mParentCondition.getConditionEndBlock();
			if (parent == 0) return 0;
			return parent | mEndInstruction;
		}
		return mEndInstruction;
	}

	@Override
	public int getEndBlock() {
		if (mCondition == null) {
			int r = getConditionEndBlock();
			if (r != 0) setPutCounterBefore(true);
			return r;
		}
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return mPutCounterBefore;
	}

	private void setPutCounterBefore(boolean value) {
		if (mParentCondition != null) mParentCondition.setPutCounterBefore(value);
		else mPutCounterBefore = value;
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
