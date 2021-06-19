package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
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
	public int getNature() {
		return ARRAY;
	}

	@Override
	public Type getType() {
		return Type.ARRAY;
	}

	@Override
	public String getString() {
		String str = "[";
		for(int i = 0; i < mValues.size(); i++){
			if (i > 0) str += ", ";
			if (mIsKeyVal) {
				str += mValues.get(i).getString() + ": ";
				i++;
			}
			str += mValues.get(i).getString();
		}
		return str + "]";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		for(AbstractExpression parameter : mValues){
			parameter.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mValues.size() == 0) writer.addCode("new ArrayLeekValue()");
		else {
			writer.addCode("new ArrayLeekValue(" + writer.getAIThis() + ", new Object[] { ");
			for (int i = 0; i < mValues.size(); i++) {
				if (i != 0) writer.addCode(", ");
				mValues.get(i).writeJavaCode(mainblock, writer);
			}
			writer.addCode(" }, " + (mIsKeyVal ? "true" : "false") + ")");
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		operations = 0;
		for (var value : mValues) {
			value.analyze(compiler);
			operations += value.getOperations();
		}
	}
}
