package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.expression.AbstractExpression;

public class ForeachKeyBlock extends AbstractLeekBlock {

	private String mIterator;
	private AbstractExpression mArray;
	private boolean mIsDeclaration = false;

	private String mKeyIterator = null;
	private boolean mIsKeyDeclaration = false;

	public ForeachKeyBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isKeyDeclaration, boolean isValueDeclaration, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
		mIsDeclaration = isValueDeclaration;
		mIsKeyDeclaration = isKeyDeclaration;
	}

	public void setValueIterator(String iterator, boolean declaration) {
		if(declaration) addVariable(iterator);
		mIterator = iterator;
	}

	public void setKeyIterator(String iterator, boolean declaration) {
		if(declaration) addVariable(iterator);
		mKeyIterator = iterator;
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
		//On fait le parcour
		//Déclaration de la variable
		sb.append("ArrayLeekValue.ArrayIterator ").append(var).append("=").append(ar).append(".getArray().getArrayIterator();");
		sb.append("while(!").append(var).append(".ended()){");
		//Maj des variables
		sb.append(key_iterator).append(".set(mUAI, ").append(var).append(".getKey(mUAI));");
		sb.append(val_iterator).append(".set(mUAI, ").append(var).append(".getValue(mUAI));");
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
}
