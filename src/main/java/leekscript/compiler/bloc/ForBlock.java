package leekscript.compiler.bloc;

import leekscript.common.Type;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.ConstantFolder;
import leekscript.compiler.expression.LeekFunctionCall;
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

	public void setInitialisation(WordCompiler compiler, Token token, Expression value, boolean isDeclaration, boolean isGlobal, Type type) {
		if (isDeclaration) {
			LeekVariableDeclarationInstruction init = new LeekVariableDeclarationInstruction(compiler, token, compiler.getCurrentFunction(), type);
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
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		if (mInitialisation != null) mInitialisation.preAnalyze(compiler);
		if (mCondition != null) mCondition.preAnalyze(compiler);
		if (mIncrementation != null) mIncrementation.preAnalyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		if (mInitialisation != null) mInitialisation.analyze(compiler);
		if (mCondition != null) mCondition.analyze(compiler);
		if (mIncrementation != null) mIncrementation.analyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {

		writer.addCode("for (");
		mInitialisation.writeJavaCode(mainblock, writer, false);

		if (writer.isOperationsEnabled()) {
			writer.addCode("ops(");
		}
		// Prevent unreachable code error (aussi pour une condition constante après
		// pliage — cf ConstantFolder : `for (;false;)` serait rejeté par javac)
		if (ConstantFolder.isConstantEmission(mCondition, mainblock, mainblock.getWordCompiler().getCurrentClass())) {
			writer.addCode("bool(");
			writer.getBoolean(mainblock, mCondition, false);
			writer.addCode(")");
		} else {
			writer.getBoolean(mainblock, mCondition, false);
		}
		if (writer.isOperationsEnabled()) {
			writer.addCode(", " + mCondition.getOperations() + ")");
		}
		writer.addCode("; ");
		if (writer.isOperationsEnabled()) {
			writer.addCode("ops(");
			mIncrementation.writeJavaCode(mainblock, writer, false);
			writer.addCode(", " + mIncrementation.getOperations() + ")");
		} else if (!(mIncrementation.trim() instanceof LeekFunctionCall call && call.isEliminable(mainblock))) {
			// Ops désactivées (CLI) : un appel éliminé se substituerait par sa valeur
			// (« null »), qui n'est pas une statement expression valide en position
			// d'incrément — on émet un update vide, valide en Java.
			mIncrementation.writeJavaCode(mainblock, writer, false);
		}
		writer.addLine(") {", getLocation());
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer, false);
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
