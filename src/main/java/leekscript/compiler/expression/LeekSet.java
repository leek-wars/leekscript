package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LeekSet extends Expression {

	private final ArrayList<Expression> mValues = new ArrayList<Expression>();
	private Token openingToken;
	private Token closingToken;

	public Type type = Type.SET;

	public LeekSet(Token openingToken) {
		this.openingToken = openingToken;
	}

	public void addValue(Expression param) {
		mValues.add(param);
	}

	public void setClosingToken(Token closingToken) {
		this.closingToken = closingToken;
		closingToken.setExpression(this);
		openingToken.setExpression(this);
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
		return mValues.stream().map(value -> value.toString()).collect(Collectors.joining(", ", "<", ">"));
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

		var types = new HashSet<Type>();
		for (var value : mValues) {
			value.analyze(compiler);
			operations += 2 + value.getOperations();
			types.add(value.getType());
		}

		this.type = Type.set(types.size() == 0 ? Type.VOID : Type.compound(types));
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new SetLeekValue(" + writer.getAIThis() + ", new Object[] { ");
		for (int i = 0; i < mValues.size(); i++) {
			if (i != 0) writer.addCode(", ");
			mValues.get(i).writeJavaCode(mainblock, writer);
		}
		writer.addCode(" })");
	}

	@Override
	public Location getLocation() {
		return new Location(openingToken.getLocation(), closingToken.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		hover.setSize(mValues.size());
		return hover;
	}
}
