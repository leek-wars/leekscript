package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekArray extends AbstractExpression {

	private final ArrayList<AbstractExpression> mValues = new ArrayList<AbstractExpression>();

	private boolean mIsKeyVal = false;

	public void addValue(AbstractExpression param) {
		mValues.add(param);
	}

	public void addValue(AbstractExpression key, AbstractExpression value) {
		mIsKeyVal = true;
		mValues.add(key);
		mValues.add(value);
	}

	@Override
	public int getType() {
		return ARRAY;
	}

	@Override
	public String getString() {
		String str = "[";
		for(int i = 0; i < mValues.size(); i++){
			if(i > 0) str += ", ";
			str += mValues.get(i).getString();
		}
		return str + "]";
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		for(AbstractExpression parameter : mValues){
			parameter.validExpression(mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if(mValues.size() == 0) writer.addCode("new ArrayLeekValue()");
		else{
			writer.addCode("new ArrayLeekValue(mUAI, new AbstractLeekValue[]{");
			for(int i = 0; i < mValues.size(); i++){
				if(i != 0) writer.addCode(",");
				mValues.get(i).writeJavaCode(mainblock, writer);
			}
			writer.addCode("}, " + (mIsKeyVal ? "true" : "false") + ")");
		}
	}
}
