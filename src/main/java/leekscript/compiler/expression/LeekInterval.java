package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LeekInterval extends Expression {

	// mFrom and mTo may be null
	private Expression mFrom;
	private Expression mTo;
	private Token openingBracket;
	private Token closingBracket;

	public LeekInterval(Token openingBracket, Expression from, Expression to, Token closingBracket) {
		this.openingBracket = openingBracket;
		this.closingBracket = closingBracket;
		this.mFrom = from;
		this.mTo = to;

		openingBracket.setExpression(this);
		closingBracket.setExpression(this);
	}

	@Override
	public int getNature() {
		return ARRAY;
	}

	@Override
	public Type getType() {
		return Type.INTERVAL;
	}

	@Override
	public String toString() {
		return "[" + (mFrom != null ? mFrom.toString() : "") + ".." + (mTo != null ? mTo.toString() : "") + "]";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainBlock) throws LeekExpressionException {
		return (mFrom == null || mFrom.validExpression(compiler, mainBlock)) && (mTo == null || mTo.validExpression(compiler, mainBlock));
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (mFrom != null) {
			mFrom.preAnalyze(compiler);
		}
		if (mTo != null) {
			mTo.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		operations = 2;
		if (mFrom != null) {
			mFrom.analyze(compiler);
			operations += mFrom.getOperations();
		}
		if (mTo != null) {
			mTo.analyze(compiler);
			operations += mTo.getOperations();
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainBlock, JavaWriter writer) {
		writer.addCode("new IntervalLeekValue(" + writer.getAIThis() + ", ");
		if (mFrom == null) {
			writer.addCode("Double.NEGATIVE_INFINITY");
		} else {
			mFrom.writeJavaCode(mainBlock, writer);
		}
		writer.addCode(", ");
		if (mTo == null) {
			writer.addCode("Double.POSITIVE_INFINITY");
		} else {
			mTo.writeJavaCode(mainBlock, writer);
		}
		writer.addCode(")");
	}

	@Override
	public Location getLocation() {
		return new Location(openingBracket.getLocation(), closingBracket.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		return hover;
	}
}
