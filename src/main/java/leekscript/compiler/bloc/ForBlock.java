package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekExpressionInstruction;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ForBlock extends AbstractLeekBlock {

	private LeekInstruction mInitialisation = null;
	private AbstractExpression mCondition = null;
	private AbstractExpression mIncrementation = null;

	public ForBlock(AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
	}

	public void setInitialisation(WordCompiler compiler, IAWord token, AbstractExpression value, boolean isDeclaration, boolean isGlobal) {
		if (isDeclaration) {
			LeekVariableDeclarationInstruction init = new LeekVariableDeclarationInstruction(compiler, token, mLine, mAI, compiler.getCurrentFunction());
			init.setValue(value);
			mInitialisation = init;
		} else {
			LeekExpression exp = new LeekExpression();
			exp.addExpression(isGlobal ? new LeekVariable(token, VariableType.GLOBAL) : new LeekVariable(token, VariableType.LOCAL));
			exp.addOperator(Operators.ASSIGN, token);
			exp.addExpression(value);
			mInitialisation = new LeekExpressionInstruction(exp, mLine, mAI);
		}
	}

	public void setCondition(AbstractExpression value) {
		mCondition = value;
	}

	public void setIncrementation(AbstractExpression incrementation) {
		mIncrementation = incrementation;
	}

	@Override
	public String getCode() {
		return "for(" + mInitialisation.getCode() + ";" + mCondition.getString() + ";" + mIncrementation.getString() + "){\n" + super.getCode() + "}";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addLine("for(", mLine, mAI);
		mInitialisation.writeJavaCode(mainblock, writer);
		mCondition.writeJavaCode(mainblock, writer);
		writer.addCode(".getBoolean();");
		mIncrementation.writeJavaCode(mainblock, writer);
		writer.addLine("){", mLine, mAI);
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

	public void analyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		if (mInitialisation != null) mInitialisation.analyze(compiler);
		if (mCondition != null) mCondition.analyze(compiler);
		if (mIncrementation != null) mIncrementation.analyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}
}
