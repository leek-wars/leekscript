package leekscript.compiler.instruction;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;

public class LeekGlobalDeclarationInstruction implements LeekInstruction {

	private final IAWord token;
	private AbstractExpression mValue = null;
	private final int mLine;
	private final AIFile<?> mAI;

	public LeekGlobalDeclarationInstruction(IAWord token, int line, AIFile<?> ai) {
		this.token = token;
		mLine = line;
		mAI = ai;
	}

	public void setValue(AbstractExpression value) {
		mValue = value;
	}

	public String getName() {
		return token.getWord();
	}

	@Override
	public String getCode() {
		if (mValue != null) {
			return "global " + token.getWord() + " = " + mValue.getString() + ";";
		} else {
			return "global " + token.getWord() + ";";
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("if (!g_init_" + token.getWord() + ") { ");
		if (mainblock.getWordCompiler().getVersion() >= 2) {
			writer.addCode("g_" + token.getWord() + " = ");
			if (mValue != null) {
				if (mValue.getOperations() > 0) writer.addCode("ops(");
				mValue.writeJavaCode(mainblock, writer);
				if (mValue.getOperations() > 0) writer.addCode(", " + mValue.getOperations() + ")");
			}
			else writer.addCode("null");
		} else {
			writer.addCode("g_" + token.getWord() + " = new Box(" + writer.getAIThis() + ", ");
			if (mValue != null) mValue.compileL(mainblock, writer);
			else writer.addCode("null");
			writer.addCode(", " + (mValue != null ? mValue.getOperations() : 0) + ")");
		}
		writer.addCode("; g_init_" + token.getWord() + " = true;");
		if (mainblock.getWordCompiler().getVersion() >= 2) writer.addCode(" ops(1);");
		writer.addLine(" }", mLine, mAI);
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
		compiler.getCurrentBlock().addVariable(new LeekVariable(compiler, token, VariableType.GLOBAL));
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
}
