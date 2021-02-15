package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;

public class ForeachKeyBlock extends AbstractLeekBlock {

	private String mIterator;
	private AbstractExpression mArray;
	private boolean mIsDeclaration = false;

	private String mKeyIterator = null;
	private boolean mIsKeyDeclaration = false;
	private boolean mKeyReference = false;
	private boolean mValueReference = false;

	public ForeachKeyBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isKeyDeclaration, boolean isValueDeclaration, int line, AIFile<?> ai, boolean keyReference, boolean valueReference) {
		super(parent, main, line, ai);
		mIsDeclaration = isValueDeclaration;
		mIsKeyDeclaration = isKeyDeclaration;
		mKeyReference = keyReference;
		mValueReference = valueReference;
	}

	public void setValueIterator(IAWord iterator, boolean declaration) {
		if(declaration) addVariable(new LeekVariable(iterator, VariableType.LOCAL));
		mIterator = iterator.getWord();
	}

	public void setKeyIterator(IAWord iterator, boolean declaration) {
		if(declaration) addVariable(new LeekVariable(iterator, VariableType.LOCAL));
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

		String key_iterator = mainblock.hasGlobal(mKeyIterator) ? ("globale_" + mKeyIterator) : ("user_" + mKeyIterator);
		String val_iterator = mainblock.hasGlobal(mIterator) ? ("globale_" + mIterator) : ("user_" + mIterator);
		writer.addCode("final AbstractLeekValue " + ar + " = ");
		mArray.writeJavaCode(mainblock, writer);
		writer.addCode(".getValue();");
		StringBuilder sb = new StringBuilder();
		sb.append("if(").append(ar).append(".isArray()){");
		//Clé
		if(mIsKeyDeclaration) sb.append("final VariableLeekValue ").append(key_iterator).append(" = new VariableLeekValue(mUAI, LeekValueManager.NULL);");
		else sb.append(key_iterator).append(".set(mUAI, LeekValueManager.NULL);");
		//Valeur
		if(mIsDeclaration) sb.append("final VariableLeekValue ").append(val_iterator).append(" = new VariableLeekValue(mUAI, LeekValueManager.NULL);");
		else sb.append(val_iterator).append(".set(mUAI, LeekValueManager.NULL);");
		//On fait le parcours
		//Déclaration de la variable
		sb.append("ArrayLeekValue.ArrayIterator ").append(var).append("=").append(ar).append(".getArray().getArrayIterator();");
		sb.append("while(!").append(var).append(".ended()){");
		//Maj des variables
		if (mKeyReference) {
			sb.append(key_iterator).append(".setRef(mUAI, ").append(var).append(".getKeyRef());");
		} else {
			sb.append(key_iterator).append(".set(mUAI, ").append(var).append(".getKeyRef());");
		}
		if (mValueReference) {
			sb.append(val_iterator).append(".setRef(mUAI, ").append(var).append(".getValueRef());");
		} else {
			sb.append(val_iterator).append(".set(mUAI, ").append(var).append(".getValueRef());");
		}
		sb.append(var).append(".next();");
		writer.addCounter(1);

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
		mArray.analyze(compiler);
		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}
}
