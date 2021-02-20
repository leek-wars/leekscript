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

	public void declare(WordCompiler compiler) {
		// Variables interdites
		if (token.getWord().equals("this")) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, LeekCompilerException.THIS_NOT_ALLOWED_HERE));
		} else {
			// Vérification déjà existante (on vérifie les globales seulement en 1.1 car il y a un léger bug en 1.0 avec les includes)
			if ((compiler.getVersion() >= 11 && compiler.getMainBlock().hasGlobal(token.getWord())) || compiler.getMainBlock().hasUserFunction(token.getWord(), true)) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, LeekCompilerException.VARIABLE_NAME_UNAVAILABLE));
			} else {
				// On ajoute la variable
				compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.GLOBAL));
			}
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (mValue != null) {
			mValue.analyze(compiler);
		}
	}
}
