package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ForeachKeyBlock extends AbstractLeekBlock {

	private IAWord mIterator;
	private AbstractExpression mArray;
	private boolean mIsDeclaration = false;

	private IAWord mKeyIterator = null;
	private boolean mIsKeyDeclaration = false;
	private boolean mKeyReference = false;
	private boolean mValueReference = false;
	private LeekVariableDeclarationInstruction iteratorDeclaration;
	private LeekVariableDeclarationInstruction iteratorKeyDeclaration;

	public ForeachKeyBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isKeyDeclaration, boolean isValueDeclaration, int line, AIFile<?> ai, boolean keyReference, boolean valueReference) {
		super(parent, main, line, ai);
		mIsDeclaration = isValueDeclaration;
		mIsKeyDeclaration = isKeyDeclaration;
		mKeyReference = keyReference;
		mValueReference = valueReference;
	}

	public void setValueIterator(WordCompiler compiler, IAWord iterator, boolean declaration) {
		if (declaration) {
			iteratorDeclaration = new LeekVariableDeclarationInstruction(compiler, iterator, 0, null, compiler.getCurrentFunction());
			addVariable(new LeekVariable(iterator, VariableType.ITERATOR, iteratorDeclaration));
		}
		mIterator = iterator.getWord();
	}

	public void setKeyIterator(WordCompiler compiler, IAWord iterator, boolean declaration) {
		if (declaration) {
			iteratorKeyDeclaration = new LeekVariableDeclarationInstruction(compiler, iterator, 0, null, compiler.getCurrentFunction());
			addVariable(new LeekVariable(iterator, VariableType.ITERATOR, iteratorKeyDeclaration));
		}
		mKeyIterator = iterator.getWord();
	}

	public void setArray(AbstractExpression exp) {
		mArray = exp;
	}

	@Override
	public String getCode() {
		return "for(" + mIterator + " in " + mArray.getString() + "){\n" + super.getCode() + "}";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		//On prend un nombre unique pour les noms de variables temporaires
		int block_count = getCount();
		String var = "i" + block_count;
		String ar = "ar" + block_count;

		String key_iterator = mainblock.hasGlobal(mKeyIterator.getWord()) ? ("globale_" + mKeyIterator) : ("user_" + mKeyIterator);
		String val_iterator = mainblock.hasGlobal(mIterator.getWord()) ? ("globale_" + mIterator) : ("user_" + mIterator);
		writer.addCode("final AbstractLeekValue " + ar + " = ");
		mArray.writeJavaCode(mainblock, writer);
		writer.addCode(".getValue();");
		StringBuilder sb = new StringBuilder();
		sb.append("if(").append(ar).append(".isArrayForIteration(mUAI)){");
		//Clé
		if(mIsKeyDeclaration) sb.append("final VariableLeekValue ").append(key_iterator).append(" = new VariableLeekValue(mUAI, LeekValueManager.NULL);");
		else sb.append(key_iterator).append(".set(mUAI, LeekValueManager.NULL);");
		//Valeur
		if(mIsDeclaration) sb.append("final VariableLeekValue ").append(val_iterator).append(" = new VariableLeekValue(mUAI, LeekValueManager.NULL);");
		else sb.append(val_iterator).append(".set(mUAI, LeekValueManager.NULL);");
		//On fait le parcours
		//Déclaration de la variable
		sb.append("ArrayLeekValue.ArrayIterator ").append(var).append("=").append(ar).append(".getArray().getArrayIterator();");
		sb.append("while(!").append(var).append(".ended()){ mUAI.addOperations(1); ");
		//Maj des variables
		if (mKeyReference || mainblock.getCompiler().getCurrentAI().getVersion() >= 11) {
			sb.append(key_iterator).append(".setRef(mUAI, ").append(var).append(".getKeyRef());");
		} else {
			sb.append(key_iterator).append(".set(mUAI, ").append(var).append(".getKeyRef());");
		}
		if (mValueReference || mainblock.getCompiler().getCurrentAI().getVersion() >= 11) {
			sb.append(val_iterator).append(".setRef(mUAI, ").append(var).append(".getValueRef());");
		} else {
			sb.append(val_iterator).append(".set(mUAI, ").append(var).append(".getValueRef());");
		}
		sb.append(var).append(".next();");

		writer.addLine(sb.toString(), mLine, mAI);
		//Instructions
		super.writeJavaCode(mainblock, writer);

		//Fin
		writer.addLine("}}");
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

		// Si c'est une déclaration on vérifie que le nom est disponnible
		if (mIsKeyDeclaration) {
			if ((compiler.getVersion() >= 11 && (compiler.getMainBlock().hasGlobal(mKeyIterator.getWord()) || compiler.getMainBlock().hasUserFunction(mKeyIterator.getWord(), true))) || compiler.getCurrentBlock().hasVariable(mKeyIterator.getWord())) {
				compiler.addError(new AnalyzeError(mKeyIterator, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			} else {
				this.addVariable(new LeekVariable(mKeyIterator, VariableType.LOCAL));
			}
		} else {
			var v = compiler.getCurrentBlock().getVariable(mKeyIterator.getWord(), true);
			if (v == null) {
				compiler.addError(new AnalyzeError(mKeyIterator, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
			}
		}
		// Si c'est une déclaration on vérifie que le nom est disponnible
		if (mIsDeclaration) {
			if ((compiler.getVersion() >= 11 && (compiler.getMainBlock().hasGlobal(mIterator.getWord()) || compiler.getMainBlock().hasUserFunction(mIterator.getWord(), true))) || compiler.getCurrentBlock().hasVariable(mIterator.getWord())) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			} else {
				this.addVariable(new LeekVariable(mIterator, VariableType.LOCAL));
			}
		} else {
			var v = compiler.getCurrentBlock().getVariable(mIterator.getWord(), true);
			if (v == null) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
			}
		}
		if (iteratorDeclaration != null)
			iteratorDeclaration.setFunction(compiler.getCurrentFunction());
		if (iteratorKeyDeclaration != null)
			iteratorKeyDeclaration.setFunction(compiler.getCurrentFunction());

		mArray.analyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}
}
