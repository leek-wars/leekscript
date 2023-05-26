package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.common.Error;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekLegacyHybridContainer extends Expression {

	private final ArrayList<Expression> mValues = new ArrayList<Expression>();
	public boolean mIsKeyVal = false;
	public Type type = Type.ARRAY;
	private Token openingBracket;
	private Token closingBracket;

	public LeekLegacyHybridContainer(Token openingBracket) {
		this.openingBracket = openingBracket;
	}

	public void addValue(Expression param) {
		mValues.add(param);
	}

	public void setClosingBracket(Token closingBracket) {
		this.closingBracket = closingBracket;
		closingBracket.setExpression(this);
		openingBracket.setExpression(this);
	}

	public void addValue(WordCompiler compiler, Expression key, Token keyToken, Expression value) {
		// Clés dupliquée ?
		for (int i = 0; i < mValues.size(); i += 2) {
			if (key.equals(mValues.get(i))) {
				compiler.addError(new AnalyzeError(key.getLocation(), AnalyzeErrorLevel.WARNING, Error.MAP_DUPLICATED_KEY));
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
	public String toString() {
		if (mIsKeyVal && mValues.size() == 0)
			return "[:]";
		String str = "[";
		for (int i = 0; i < mValues.size(); i++) {
			if (i > 0)
				str += ", ";
			if (mIsKeyVal) {
				str += mValues.get(i).toString() + ": ";
				i++;
			}
			str += mValues.get(i).toString();
		}
		return str + "]";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainBlock) throws LeekExpressionException {
		for (Expression parameter : mValues) {
			parameter.validExpression(compiler, mainBlock);
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
	public void writeJavaCode(MainLeekBlock mainBlock, JavaWriter writer) {
		if (mValues.isEmpty()) {
			writer.addCode("new LegacyHybridContainerLeekValue()");
			return;
		}

		writer.addCode("new LegacyHybridContainerLeekValue(" + writer.getAIThis() + ", new Object[] { ");
		for (int i = 0; i < mValues.size(); i++) {
			if (i != 0)
				writer.addCode(", ");
			mValues.get(i).writeJavaCode(mainBlock, writer);
		}
		writer.addCode(" }, " + (mIsKeyVal ? "true" : "false") + ")");

	}

	@Override
	public Location getLocation() {
		return new Location(openingBracket.getLocation(), closingBracket.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		hover.setSize(mIsKeyVal ? mValues.size() / 2 : mValues.size());
		return hover;
	}
}
