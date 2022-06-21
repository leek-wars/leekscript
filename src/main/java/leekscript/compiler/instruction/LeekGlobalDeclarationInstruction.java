package leekscript.compiler.instruction;

import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;

public class LeekGlobalDeclarationInstruction extends LeekInstruction {

	private final Token token;
	private final Token variableToken;
	private Expression mValue = null;

	public LeekGlobalDeclarationInstruction(Token token, Token variableToken) {
		this.token = token;
		this.variableToken = variableToken;
	}

	public void setValue(Expression value) {
		mValue = value;
	}

	public String getName() {
		return variableToken.getWord();
	}

	@Override
	public String getCode() {
		if (mValue != null) {
			return "global " + variableToken.getWord() + " = " + mValue.getString() + ";";
		} else {
			return "global " + variableToken.getWord() + ";";
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("if (!g_init_" + variableToken.getWord() + ") { ");
		if (mainblock.getWordCompiler().getVersion() >= 2) {
			writer.addCode("g_" + variableToken.getWord() + " = ");
			if (mValue != null) {
				if (mValue.getOperations() > 0) writer.addCode("ops(");
				mValue.writeJavaCode(mainblock, writer);
				if (mValue.getOperations() > 0) writer.addCode(", " + mValue.getOperations() + ")");
			}
			else writer.addCode("null");
		} else {
			writer.addCode("g_" + variableToken.getWord() + " = new Box(" + writer.getAIThis() + ", ");
			if (mValue != null) mValue.compileL(mainblock, writer);
			else writer.addCode("null");
			writer.addCode(", " + (mValue != null ? mValue.getOperations() : 0) + ")");
		}
		writer.addCode("; g_init_" + variableToken.getWord() + " = true;");
		if (mainblock.getWordCompiler().getVersion() >= 2) writer.addCode(" ops(1);");
		writer.addLine(" }", getLocation());
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la variable
		compiler.getCurrentBlock().addVariable(new LeekVariable(compiler, variableToken, VariableType.GLOBAL));
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		if (mValue != null) {
			mValue.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (mValue != null) {
			mValue.analyze(compiler);
		}
	}

	@Override
	public int getOperations() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}
}
