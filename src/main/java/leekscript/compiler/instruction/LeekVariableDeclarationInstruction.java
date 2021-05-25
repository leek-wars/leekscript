package leekscript.compiler.instruction;

import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.common.Type;

public class LeekVariableDeclarationInstruction implements LeekInstruction {

	private final IAWord token;
	private final int mLine;
	private final AIFile<?> mAI;
	private AbstractExpression mValue = null;
	private boolean captured = false;
	private AbstractLeekBlock function;
	private boolean box = false;

	public LeekVariableDeclarationInstruction(WordCompiler compiler, IAWord token, int line, AIFile<?> ai, AbstractLeekBlock function) {
		this.token = token;
		mLine = line;
		mAI = ai;
		this.function = function;
		this.box = compiler.getVersion() <= 10;
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

	public boolean isBox() {
		return this.box || this.captured;
	}

	public boolean isWrapper() {
		return this.captured;
	}

	@Override
	public String getCode() {
		if (mValue == null) return "var " + token.getWord();
		return "var " + token.getWord() + " = " + mValue.getString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (!captured || !(mValue instanceof LeekAnonymousFunction)) {
			writer.addCode("final VariableLeekValue user_" + token.getWord() + " = new VariableLeekValue(mUAI, ");
			if (mValue != null) mValue.writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
			writer.addLine(");", mLine, mAI);
		} else {
			writer.addCode("final VariableLeekValue user_" + token.getWord() + " = new VariableLeekValue(mUAI); user_" + token.getWord() + ".init(mUAI, ");
			if (mValue != null) mValue.writeJavaCode(mainblock, writer);
			else writer.addCode("LeekValueManager.NULL");
			writer.addLine(");", mLine, mAI);
		}
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	public AbstractLeekBlock getFunction() {
		return this.function;
	}

	public boolean isCaptured() {
		return captured;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	@Override
	public void analyze(WordCompiler compiler) {
		function = compiler.getCurrentFunction();
		if (mValue != null && mValue.getType() == Type.FUNCTION) {
			registerVariable(compiler);
			mValue.analyze(compiler);
		} else {
			if (mValue != null) mValue.analyze(compiler);
			registerVariable(compiler);
		}
	}

	private void registerVariable(WordCompiler compiler) {
		// Variables interdites
		if (compiler.getVersion() >= 11 && token.getWord().equals("this")) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.THIS_NOT_ALLOWED_HERE));
		} else {
			// Vérification déjà existante (on vérifie les globales et fonctions seulement en 1.1 car il y a un léger bug en 1.0 avec les includes)
			if ((compiler.getVersion() >= 11 && (compiler.getMainBlock().hasGlobal(token.getWord()) || compiler.getMainBlock().hasUserFunction(token.getWord(), true))) || compiler.getCurrentBlock().hasVariable(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			} else {
				// On ajoute la variable
				compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.LOCAL, this));
			}
		}
	}

	public void setCaptured() {
		this.captured = true;
	}

	public void setFunction(AbstractLeekBlock function) {
		this.function = function;
	}

	@Override
	public int getOperations() {
		return 0;
	}
}
