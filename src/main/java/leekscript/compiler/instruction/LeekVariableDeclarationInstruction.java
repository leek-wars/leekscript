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
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
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
		this.box = compiler.getVersion() <= 1;
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
		return "var " + token.getWord() + " = " + mValue.getString() + ";";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (this.captured) {
			if (mValue != null && mValue.trim() instanceof LeekAnonymousFunction) {
				writer.addCode("final Wrapper u_" + token.getWord() + " = new Wrapper(new Box(" + writer.getAIThis() + ", null)); u_" + token.getWord() + ".set(");
				mValue.writeJavaCode(mainblock, writer);
				writer.addLine(");", mLine, mAI);
			} else if (mValue instanceof LeekExpression && ((LeekExpression) mValue).getOperator() == Operators.REFERENCE) {
				var e = ((LeekExpression) mValue).getExpression2();
				if (e.isLeftValue()) {
					writer.addCode("final Wrapper u_" + token.getWord() + " = new Wrapper(");
					e.compileL(mainblock, writer);
					writer.addLine(", " + e.getOperations() + ")");
				} else {
					writer.addCode("final var u_" + token.getWord() + " = new Wrapper(new Box(" + writer.getAIThis() + ", ");
					e.writeJavaCode(mainblock, writer);
					writer.addLine("), " + e.getOperations() + ")");
				}
				writer.addLine(";", mLine, mAI);
			} else if (mainblock.getWordCompiler().getVersion() <= 1) {
				writer.addCode("final var u_" + token.getWord() + " = new Wrapper(new Box(" + writer.getAIThis() + ", ");
				if (mValue != null) mValue.compileL(mainblock, writer);
				else writer.addCode("null");
				writer.addLine(")");
				if (mValue != null && mValue.getOperations() > 0) {
					writer.addCode(", " + mValue.getOperations());
				}
				writer.addLine(");", mLine, mAI);
			} else {
				writer.addCode("final var u_" + token.getWord() + " = new Wrapper(new Box(" + writer.getAIThis() + ", ");
				if (mValue != null) mValue.writeJavaCode(mainblock, writer);
				else writer.addCode("null");
				writer.addLine(")");
				if (mValue != null && mValue.getOperations() > 0) {
					writer.addCode(", " + mValue.getOperations());
				}
				writer.addLine(");", mLine, mAI);
			}
		} else {
			if (mainblock.getWordCompiler().getVersion() <= 1) {
				if (mValue instanceof LeekExpression && ((LeekExpression) mValue).getOperator() == Operators.REFERENCE) {
					var e = ((LeekExpression) mValue).getExpression2();
					if (e.isLeftValue()) {
						// writer.addCode("Box u_" + token.getWord() + " = ");
						// e.compileL(mainblock, writer);
						writer.addCode("var u_" + token.getWord() + " = new Box(" + writer.getAIThis() + ", ");
						// e.compileL(mainblock, writer);
						e.writeJavaCode(mainblock, writer);
						if (mValue.getOperations() > 0) {
							writer.addCode(", " + mValue.getOperations());
						}
						writer.addLine(")");
					} else {
						writer.addCode("var u_" + token.getWord() + " = new Box(" + writer.getAIThis() + ", ");
						// e.writeJavaCode(mainblock, writer);
						mValue.compileL(mainblock, writer);
						if (mValue.getOperations() > 0) {
							writer.addCode(", " + mValue.getOperations());
						}
						writer.addLine(")");
					}
					writer.addLine(";", mLine, mAI);
				} else {
					writer.addCode("var u_" + token.getWord() + " = new Box(" + writer.getAIThis() + ", ");
					if (mValue != null) {
						if (mValue.isLeftValue()) {
							writer.compileClone(mainblock, mValue);
					 	} else {
							mValue.compileL(mainblock, writer);
							// mValue.writeJavaCode(mainblock, writer);
						}
						if (mValue.getOperations() > 0) {
							writer.addCode(", " + mValue.getOperations());
						}
					} else {
						writer.addCode("null");
					}
					writer.addLine(");", mLine, mAI);
				}
			} else {
				writer.addCode("Object u_" + token.getWord() + " = ");
				writer.addCode("ops(");
				if (mValue != null) mValue.writeJavaCode(mainblock, writer);
				else writer.addCode("null");
				writer.addCode(", " + (1 + (mValue == null ? 0 : mValue.getOperations())) + ")");
				writer.addLine(";", mLine, mAI);
			}
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
		if (compiler.getVersion() >= 2 && token.getWord().equals("this")) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.THIS_NOT_ALLOWED_HERE));
		} else {
			// Vérification déjà existante (on vérifie les globales et fonctions seulement en 1.1 car il y a un léger bug en 1.0 avec les includes)
			if ((compiler.getVersion() >= 2 && (compiler.getMainBlock().hasGlobal(token.getWord()) || compiler.getMainBlock().hasUserFunction(token.getWord(), true))) || compiler.getCurrentBlock().hasVariable(token.getWord())) {
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
