package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.expression.AbstractExpression;

public class ForeachBlock extends AbstractLeekBlock {

	private String mIterator;
	private AbstractExpression mArray;
	private boolean mIsDeclaration = false;
	private boolean mReference = false;

	public ForeachBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isDeclaration, int line, AIFile<?> ai, boolean reference) {
		super(parent, main, line, ai);
		mIsDeclaration = isDeclaration;
		mReference = reference;
	}

	public void setIterator(String iterator, boolean declaration) {
		if(declaration) addVariable(iterator);
		mIterator = iterator;
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
		String iterator_name = mainblock.hasGlobal(mIterator) ? ("globale_" + mIterator) : "user_" + mIterator;

		writer.addCode("final AbstractLeekValue " + ar + " = ");
		mArray.writeJavaCode(mainblock, writer);
		writer.addLine(".getValue();", mLine, mAI);
		writer.addLine("if(" + ar + ".isArray()){");
		if(mIsDeclaration) writer.addLine("final VariableLeekValue " + iterator_name + " = new VariableLeekValue(mUAI, LeekValueManager.NULL);");
		else writer.addLine(iterator_name + ".set(mUAI, LeekValueManager.NULL);");
		if (mReference) {
			writer.addLine("for(AbstractLeekValue " + var + " : " + ar + ".getArray()){ " + iterator_name + ".setRef(mUAI, " + var + ");");
		} else {
			writer.addLine("for(AbstractLeekValue " + var + " : " + ar + ".getArray()){ " + iterator_name + ".set(mUAI, " + var + ".getValue());");
		}
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);

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
