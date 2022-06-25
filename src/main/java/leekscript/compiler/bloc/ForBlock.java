package leekscript.compiler.bloc;

import leekscript.common.Type;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekExpressionInstruction;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ForBlock extends AbstractLeekBlock {

	private final Token token;
	private LeekInstruction mInitialisation = null;
	private Expression mCondition = null;
	private Expression mIncrementation = null;

	public ForBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
	}

	public void setInitialisation(WordCompiler compiler, Token token, Expression value, boolean isDeclaration, boolean isGlobal) {
		if (isDeclaration) {
			LeekVariableDeclarationInstruction init = new LeekVariableDeclarationInstruction(compiler, token, compiler.getCurrentFunction());
			init.setValue(value);
			mInitialisation = init;
		} else {
			LeekExpression exp = new LeekExpression();
			exp.addExpression(isGlobal ? new LeekVariable(token, VariableType.GLOBAL) : new LeekVariable(token, VariableType.LOCAL));
			exp.addOperator(Operators.ASSIGN, token);
			exp.addExpression(value);
			mInitialisation = new LeekExpressionInstruction(exp);
		}
	}

	public void setCondition(Expression value) {
		mCondition = value;
	}

	public void setIncrementation(Expression incrementation) {
		mIncrementation = incrementation;
	}

	@Override
	public String getCode() {
		return "for (" + mInitialisation.getCode() + mCondition.toString() + "; " + mIncrementation.toString() + ") {\n" + super.getCode() + "}";
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		if (mInitialisation != null) mInitialisation.preAnalyze(compiler);
		if (mCondition != null) mCondition.preAnalyze(compiler);
		if (mIncrementation != null) mIncrementation.preAnalyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		if (mInitialisation != null) mInitialisation.analyze(compiler);
		if (mCondition != null) mCondition.analyze(compiler);
		if (mIncrementation != null) mIncrementation.analyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addCode("for (");
		mInitialisation.writeJavaCode(mainblock, writer);

		writer.addCode("ops(");
		// Prevent unreachable code error
		if (mCondition instanceof LeekBoolean) {
			writer.addCode("bool(");
			writer.getBoolean(mainblock, mCondition);
			writer.addCode(")");
		} else {
			writer.getBoolean(mainblock, mCondition);
		}
		writer.addCode(", " + mCondition.getOperations() + "); ops(");
		mIncrementation.writeJavaCode(mainblock, writer);
		writer.addLine(", " + mIncrementation.getOperations() + ")) {", getLocation());
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		writer.addLine("}");
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	@Override
	public int getEndBlock() {
		return 0;
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
