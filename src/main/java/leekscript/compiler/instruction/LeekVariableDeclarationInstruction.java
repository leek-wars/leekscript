package leekscript.compiler.instruction;

import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;

public class LeekVariableDeclarationInstruction implements LeekInstruction {

	private final IAWord token;
	private final int mLine;
	private final AIFile<?> mAI;
	private AbstractExpression mValue = null;
	private boolean mMustSepare = false;

	public LeekVariableDeclarationInstruction(IAWord token, int line, AIFile<?> ai) {
		this.token = token;
		mLine = line;
		mAI = ai;
	}

	public void mustSepare() {
		mMustSepare = true;
	}

	public void setValue(AbstractExpression value) {
		mValue = value;
	}

	public String getName() {
		return token.getWord();
	}

	public IAWord getToken() {
		return this.token;
	}

	@Override
	public String getCode() {
		if(mValue == null) return "var " + token.getWord();
		return "var " + token.getWord() + " = " + mValue.getString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if(!mMustSepare){
			writer.addCode("final VariableLeekValue user_" + token.getWord() + " = new VariableLeekValue(mUAI, ");
			if(mValue != null) mValue.writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
			writer.addLine(");", mLine, mAI);
		}
		else{
			writer.addCode("final VariableLeekValue user_" + token.getWord() + " = new VariableLeekValue(mUAI, LeekValueManager.NULL); user_" + token.getWord() + ".set(mUAI, ");
			if(mValue != null) mValue.writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
			writer.addLine(");", mLine, mAI);
		}
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
		// Variables interdites
		if (token.getWord().equals("this")) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, LeekCompilerException.THIS_NOT_ALLOWED_HERE));
		} else {
			// On ajoute la variable
			if (compiler.getMainBlock().hasGlobal(token.getWord()) || compiler.getMainBlock().hasUserFunction(token.getWord(), true) || compiler.getCurrentBlock().hasVariable(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, LeekCompilerException.VARIABLE_NAME_UNAVAILABLE));
			} else {
				compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.LOCAL));
			}
		}
		if (mValue != null) {
			mValue.analyze(compiler);
		}
	}
}
