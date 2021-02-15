package leekscript.compiler.instruction;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;

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
		return token.getWord() + " = " + mValue.getString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("if(globale_" + token.getWord() + " == null){globale_" + token.getWord() + " = new VariableLeekValue(mUAI, ");
		if(mValue != null) mValue.writeJavaCode(mainblock, writer);
		else writer.addCode("LeekValueManager.NULL");
		writer.addLine(");}", mLine, mAI);
	}

	public String getJavaDeclaration() {
		return "private VariableLeekValue globale_" + token.getWord() + " = null;";
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
	public void analyze(WordCompiler compiler) {
		// if (token.getWord().equals("COLOR_ORANGE")) {
		// 	System.out.println("Add global " + token.getWord());
		// }
		// compiler.getMainBlock().addGlobal(token.getWord());
		// compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.GLOBAL));
		if (mValue != null) {
			mValue.analyze(compiler);
		}
	}
}
