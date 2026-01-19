package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.stream.Collectors;

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

public class LeekMap extends Expression {

	private final class Entry {
		public Expression key;
		public Expression value;

		public Entry(Expression key, Expression value) {
			this.key = key;
			this.value = value;
		}
	}

	private final ArrayList<Entry> mEntries = new ArrayList<Entry>();
	private Token openingBracket;
	private Token closingBracket;

	public Type type = Type.MAP;

	public LeekMap(Token openingBracket) {
		this.openingBracket = openingBracket;
	}

	public void setClosingBracket(Token closingBracket) {
		this.closingBracket = closingBracket;
		closingBracket.setExpression(this);
		openingBracket.setExpression(this);
	}

	public void addValue(WordCompiler compiler, Expression key, Expression value) throws LeekCompilerException {
		if (mEntries.stream().anyMatch(entry -> key.equals(entry.key))) {
			compiler.addError(new AnalyzeError(key.getLocation(), AnalyzeErrorLevel.ERROR, Error.MAP_DUPLICATED_KEY));
		}

		mEntries.add(new Entry(key, value));
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
		if (mEntries.isEmpty()) {
			return "[:]";
		}

		String str = mEntries.stream().map(entry -> entry.key + ": " + entry.value).collect(Collectors.joining(", ", "[", "]"));

		return str;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		for (var entry : mEntries) {
			entry.key.validExpression(compiler, mainblock);
			entry.value.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		for (var entry : mEntries) {
			entry.key.preAnalyze(compiler);
			entry.value.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		operations = 0;

		Type keyType = Type.VOID;
		Type elementType = Type.VOID;

		for (var entry : mEntries) {
			entry.key.analyze(compiler);
			entry.value.analyze(compiler);

			operations += 2 + entry.key.getOperations() + entry.value.getOperations();

			keyType = Type.compound(keyType, entry.key.getType());
			elementType = Type.compound(elementType, entry.value.getType());
		}
		this.type = Type.map(keyType, elementType);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (mEntries.isEmpty()) {
			writer.addCode("new MapLeekValue(" + writer.getAIThis() + ")");
			return;
		}

		writer.addCode("new MapLeekValue(" + writer.getAIThis() + ", new Object[] { ");
		for (int i = 0; i < mEntries.size(); i++) {
			if (i > 0) writer.addCode(", ");

			mEntries.get(i).key.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			mEntries.get(i).value.writeJavaCode(mainblock, writer, false);
		}
		writer.addCode(" })");
	}

	@Override
	public Location getLocation() {
		return new Location(openingBracket.getLocation(), closingBracket.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		hover.setSize(mEntries.size());
		return hover;
	}
}
