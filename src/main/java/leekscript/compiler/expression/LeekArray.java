package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.stream.Collectors;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekArray extends Expression {

	private final ArrayList<Expression> mValues = new ArrayList<Expression>();
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
		closingBracket.setExpression(this);
		openingBracket.setExpression(this);
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
	public String toString() {
		return mValues.stream()
				.map(value -> value.toString())
				.collect(Collectors.joining(", ", "[", "]"));

	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainBlock) throws LeekExpressionException {
		for (Expression value : mValues) {
			value.validExpression(compiler, mainBlock);
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
			writer.addCode("new ArrayLeekValue(" + writer.getAIThis() + ")");
			return;
		}

		writer.addCode("new ArrayLeekValue(" + writer.getAIThis() + ", new Object[] { ");
		for (int i = 0; i < mValues.size(); i++) {
			if (i > 0)
				writer.addCode(", ");
			mValues.get(i).writeJavaCode(mainBlock, writer);
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
		hover.setSize(mValues.size());
		return hover;
	}
}
