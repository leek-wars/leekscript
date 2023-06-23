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
import leekscript.compiler.exceptions.LeekCompilerException;

public class LegacyLeekArray extends Expression {

	private final ArrayList<Expression> mValues = new ArrayList<Expression>();
	public boolean mIsKeyVal = false;
	public Type type = Type.ARRAY;
	private Token openingBracket;
	private Token closingBracket;

	public LegacyLeekArray(Token openingBracket) {
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

	public void addValue(WordCompiler compiler, Expression key, Expression value) throws LeekCompilerException {
		// Clés dupliquée ?
		for (int i = 0; i < mValues.size(); i += 2) {
			if (key.equals(mValues.get(i))) {
				compiler.addError(new AnalyzeError(key.getLocation(), AnalyzeErrorLevel.WARNING, Error.MAP_DUPLICATED_KEY));
			}
		}

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
		return type;
	}

	@Override
	public String toString() {
		if (mIsKeyVal && mValues.size() == 0) return "[:]";
		String str = "[";
		for (int i = 0; i < mValues.size(); i++) {
			if (i > 0) str += ", ";
			if (mIsKeyVal) {
				str += mValues.get(i).toString() + ": ";
				i++;
			}
			str += mValues.get(i).toString();
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
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		for (var value : mValues) {
			value.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		operations = 0;
		if (mIsKeyVal) {
			Type keyType = Type.VOID;
			Type elementType = Type.VOID;
			for (int v = 0; v < mValues.size(); v += 2) {
				var value = mValues.get(v + 1);
				var key = mValues.get(v);
				key.analyze(compiler);
				value.analyze(compiler);
				operations += 2 + key.getOperations() + value.getOperations();
				keyType = Type.compound(keyType, key.getType());
				elementType = Type.compound(elementType, value.getType());
			}
			if (compiler.getVersion() >= 4) {

				this.type = Type.map(keyType, elementType);
			} else {
				this.type = Type.LEGACY_ARRAY;
			}
		} else {
			Type elementType = Type.VOID;
			for (var value : mValues) {
				value.analyze(compiler);
				operations += 2 + value.getOperations();
				elementType = Type.compound(elementType, value.getType());
			}
			if (compiler.getVersion() >= 4) {
				this.type = Type.array(elementType);
			} else {
				this.type = Type.LEGACY_ARRAY;
			}
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mValues.isEmpty()) {
			writer.addCode("new LegacyArrayLeekValue(" + writer.getAIThis() + ")");
			return;
		}

		writer.addCode("new LegacyArrayLeekValue(" + writer.getAIThis() + ", new Object[] { ");
		for (int i = 0; i < mValues.size(); i++) {
			if (i != 0) writer.addCode(", ");
			mValues.get(i).writeJavaCode(mainblock, writer);
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
