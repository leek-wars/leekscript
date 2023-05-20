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

public class LeekArray extends Expression {

	private final ArrayList<LeekArrayElement> mElements = new ArrayList<LeekArrayElement>();
	public boolean mIsKeyVal = false;
	public Type type = Type.ARRAY;
	private Token openingBracket;
	private Token closingBracket;

	public LeekArray(Token openingBracket) {
		this.openingBracket = openingBracket;
	}

	public void addValue(Expression value) {
		mElements.add(new LeekArrayValue(value));
	}

	public void setClosingBracket(Token closingBracket) {
		this.closingBracket = closingBracket;
		closingBracket.setExpression(this);
		openingBracket.setExpression(this);
	}

	public void addValue(WordCompiler compiler, Expression key, Token keyToken, Expression value) {
		verifyDuplicatedKeys(compiler, key, keyToken);

		mElements.add(new LeekMapKeyValue(key, value));

		mIsKeyVal = true;
		if (compiler.getVersion() >= 4) {
			type = Type.MAP;
		}
	}

	private void verifyDuplicatedKeys(WordCompiler compiler, Expression key, Token keyToken) {
		for (var element : mElements) {
			if (element instanceof LeekMapKeyValue && key.equals(((LeekMapKeyValue) element).getKey())) {
				var level = compiler.getVersion() >= 4 ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				compiler.addError(new AnalyzeError(keyToken.getLocation(), level, Error.MAP_DUPLICATED_KEY));
			}
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
		if (mIsKeyVal && mElements.size() == 0)
			return "[:]";

		String str = "[";
		var first = true;
		for (var element : mElements) {
			if (!first)
				str += ", ";
			str += element.toString();
			first = false;
		}
		return str + "]";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		var allValid = true;
		for (var element : mElements) {
			allValid = allValid && element.validExpression(compiler, mainblock);
		}
		return allValid;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		for (var element : mElements) {
			element.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		operations = 0;
		for (var element : mElements) {
			element.analyze(compiler);
			operations += element.getOperations();
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		boolean passIsKeyValAsParameter = mainblock.getVersion() < 4;
		String klass = mainblock.getVersion() < 4 ? "LegacyArrayLeekValue"
				: (mIsKeyVal ? "MapLeekValue" : "ArrayLeekValue");

		writer.addCode("new " + klass + "(" + writer.getAIThis() + ", new Object[] { ");

		boolean first = true;
		for (var element : mElements) {
			if (!first)
				writer.addCode(", ");
			element.writeJavaCode(mainblock, writer);
			first = false;
		}

		if (passIsKeyValAsParameter) {
			writer.addCode(" }, " + (mIsKeyVal ? "true" : "false") + ")");
		} else {
			writer.addCode(" })");
		}
	}

	@Override
	public Location getLocation() {
		return new Location(openingBracket.getLocation(), closingBracket.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		hover.setSize(mElements.size());
		return hover;
	}

	private abstract class LeekArrayElement {
		public abstract String toString();

		public abstract void preAnalyze(WordCompiler compiler);

		public abstract void analyze(WordCompiler compiler);

		public abstract int getOperations();

		public abstract boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock)
				throws LeekExpressionException;

		public abstract void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer);
	}

	private final class LeekArrayValue extends LeekArrayElement {
		private final Expression value;

		private LeekArrayValue(Expression value) {
			this.value = value;
		}

		public String toString() {
			return value.toString();
		}

		public void preAnalyze(WordCompiler compiler) {
			value.preAnalyze(compiler);
		}

		public void analyze(WordCompiler compiler) {
			value.analyze(compiler);
		}

		public int getOperations() {
			return 2 + value.getOperations();
		}

		public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
			return value.validExpression(compiler, mainblock);
		}

		public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
			value.writeJavaCode(mainblock, writer);
		}
	}

	private final class LeekMapKeyValue extends LeekArrayElement {
		private final Expression key;
		private final Expression value;

		private LeekMapKeyValue(Expression key, Expression value) {
			this.key = key;
			this.value = value;
		}

		public Expression getKey() {
			return key;
		}

		public String toString() {
			return key.toString() + ": " + value.toString();
		}

		public void preAnalyze(WordCompiler compiler) {
			key.preAnalyze(compiler);
			value.preAnalyze(compiler);
		}

		public void analyze(WordCompiler compiler) {
			key.analyze(compiler);
			value.analyze(compiler);
		}

		public int getOperations() {
			return 4 + key.getOperations() + value.getOperations();
		}

		public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
			return key.validExpression(compiler, mainblock) && value.validExpression(compiler, mainblock);
		}

		public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
			key.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			value.writeJavaCode(mainblock, writer);
		}
	}
}
