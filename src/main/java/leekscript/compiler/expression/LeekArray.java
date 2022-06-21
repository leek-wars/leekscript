package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.compiler.AnalyzeError;
import leekscript.common.Error;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekArray extends Expression {

	private final ArrayList<Expression> mValues = new ArrayList<Expression>();
	public boolean mIsKeyVal = false;
	public Type type = Type.ARRAY;
	private Token openingBracket;
	private Token closingBracket;

	public LeekArray(Token openingBracket) {
		this.openingBracket = openingBracket;
	}

	public void addValue(Expression param) {
		mValues.add(param);
	}

	public void setClosingBracket(Token closingBracket) {
		this.closingBracket = closingBracket;
	}

	public void addValue(WordCompiler compiler, Expression key, Token keyToken, Expression value) {

		// Clés dupliquée ?
		for (int i = 0; i < mValues.size(); i += 2) {
			if (key.equals(mValues.get(i))) {
				var level = compiler.getVersion() >= 4 ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				compiler.addError(new AnalyzeError(keyToken, level, Error.MAP_DUPLICATED_KEY));
			}
		}

		mIsKeyVal = true;
		mValues.add(key);
		mValues.add(value);

		if (compiler.getVersion() >= 4) {
			type = Type.MAP;
		}
	}

	@Override
	public int getNature() {
		return ARRAY;
	}

	@Override
	public Type getType() {
		return type;
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
		for (Expression parameter : mValues) {
			parameter.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		for (var value : mValues) {
			value.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		operations = 0;
		for (var value : mValues) {
			value.analyze(compiler);
			operations += 2 + value.getOperations();
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mainblock.getVersion() >= 4) {
			if (mIsKeyVal) {
				if (mValues.size() == 0) writer.addCode("new MapLeekValue(" + writer.getAIThis() + ")");
				else {
					writer.addCode("new MapLeekValue(" + writer.getAIThis() + ", new Object[] { ");
					for (int i = 0; i < mValues.size(); i++) {
						if (i != 0) writer.addCode(", ");
						mValues.get(i).writeJavaCode(mainblock, writer);
					}
					writer.addCode(" })");
				}
			} else {
				if (mValues.size() == 0) writer.addCode("new ArrayLeekValue(" + writer.getAIThis() + ")");
				else {
					writer.addCode("new ArrayLeekValue(" + writer.getAIThis() + ", new Object[] { ");
					for (int i = 0; i < mValues.size(); i++) {
						if (i != 0) writer.addCode(", ");
						mValues.get(i).writeJavaCode(mainblock, writer);
					}
					writer.addCode(" })");
				}
			}
		} else {
			if (mValues.size() == 0) writer.addCode("new LegacyArrayLeekValue()");
			else {
				writer.addCode("new LegacyArrayLeekValue(" + writer.getAIThis() + ", new Object[] { ");
				for (int i = 0; i < mValues.size(); i++) {
					if (i != 0) writer.addCode(", ");
					mValues.get(i).writeJavaCode(mainblock, writer);
				}
				writer.addCode(" }, " + (mIsKeyVal ? "true" : "false") + ")");
			}
		}
	}

	@Override
	public Location getLocation() {
		return new Location(openingBracket.getLocation(), closingBracket.getLocation());
	}
}
