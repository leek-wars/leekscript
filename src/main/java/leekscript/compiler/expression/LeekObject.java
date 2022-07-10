package leekscript.compiler.expression;

import java.util.LinkedHashMap;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekObject extends Expression {

	private final Token openingBrace;
	private Token closingBrace;
	private final LinkedHashMap<String, Expression> mValues = new LinkedHashMap<>();

	public LeekObject(Token openingBrace) {
		this.openingBrace = openingBrace;
	}

	public void addEntry(String key, Expression value) {
		mValues.put(key, value);
	}

	public void setClosingBrace(Token closingBrace) {
		this.closingBrace = closingBrace;
		this.closingBrace.setExpression(this);
		this.openingBrace.setExpression(this);
	}

	@Override
	public int getNature() {
		return OBJECT;
	}

	@Override
	public Type getType() {
		return Type.OBJECT;
	}

	@Override
	public String toString() {
		String str = "{";
		int i = 0;
		for (var entry : mValues.entrySet()) {
			if (i++ > 0) str += ", ";
			str += entry.getKey() + ": ";
			str += entry.getValue().toString();
		}
		return str + "}";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		for (var entry : mValues.entrySet()) {
			entry.getValue().validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new ObjectLeekValue(" + writer.getAIThis() + ", new String[] { ");
		int i = 0;
		for (var entry : mValues.entrySet()) {
			if (i++ != 0) writer.addCode(", ");
			writer.addCode("\"" + entry.getKey() + "\"");
		}
		writer.addCode(" }, new Object[] { ");
		i = 0;
		for (var entry : mValues.entrySet()) {
			if (i++ != 0) writer.addCode(", ");
			entry.getValue().writeJavaCode(mainblock, writer);
		}
		writer.addCode(" })");
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		for (var value : mValues.entrySet()) {
			value.getValue().preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		operations = 0;
		for (var value : mValues.entrySet()) {
			value.getValue().analyze(compiler);
			operations += value.getValue().getOperations();
		}
	}

	@Override
	public Location getLocation() {
		return new Location(openingBrace.getLocation(), closingBrace.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		return new Hover(getType(), getLocation(), toString());
	}
}
