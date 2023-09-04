package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.common.Error;

public class LeekInterval extends Expression {

	// mFrom and mTo may be null
	private Expression mFrom;
	private Expression mTo;
	private Token openingBracket;
	private Token closingBracket;
	private Type type;
	private final boolean minClosed;
	private final boolean maxClosed;

	public LeekInterval(Token openingBracket, Expression from, Expression to, Token closingBracket) {
		this.openingBracket = openingBracket;
		this.closingBracket = closingBracket;
		this.minClosed = openingBracket.getWord().equals("[");
		this.maxClosed = closingBracket.getWord().equals("]");
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
		return type;
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
		var fromInfinity = mFrom == null || mFrom.isInfinity();
		var toInfinity = mTo == null || mTo.isInfinity();
		var fromType = fromInfinity ? Type.VOID : mFrom.getType();
		var toType = toInfinity ? Type.VOID : mTo.getType();
		if (minClosed && maxClosed && mFrom == null && mTo == null) {
			type = Type.EMPTY_INTERVAL;
		} else if (fromType == Type.REAL || toType == Type.REAL || (fromType == Type.VOID && toType == Type.VOID)) {
			type = Type.REAL_INTERVAL;
		} else if (fromType == Type.INT || toType == Type.INT) {
			type = Type.INTEGER_INTERVAL;
		} else {
			type = Type.INTERVAL;
		}

		if (type != Type.EMPTY_INTERVAL) {
			if (minClosed && fromInfinity) {
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.INTERVAL_INFINITE_CLOSED, new String[] {
					mFrom == null ? "∞" : mFrom.toString()
				}));
			}
			if (maxClosed && toInfinity) {
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.INTERVAL_INFINITE_CLOSED, new String[] {
					mTo == null ? "∞" : mTo.toString()
				}));
			}
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainBlock, JavaWriter writer) {
		if (type == Type.EMPTY_INTERVAL) {
			writer.addCode("new RealIntervalLeekValue(" + writer.getAIThis() + ", false, 0.0, false, 0.0)");
		} else {

			if (type == Type.INTEGER_INTERVAL) {
				writer.addCode("new IntegerIntervalLeekValue(" + writer.getAIThis() + ", ");
			} else if (type == Type.REAL_INTERVAL) {
				writer.addCode("new RealIntervalLeekValue(" + writer.getAIThis() + ", ");
			} else {
				writer.addCode("interval(");
			}
			writer.addCode(minClosed + ", ");
			if (mFrom == null) {
				if (type == Type.INTEGER_INTERVAL) {
					writer.addCode("Long.MIN_VALUE");
				} else {
					writer.addCode("Double.NEGATIVE_INFINITY");
				}
			} else {
				mFrom.writeJavaCode(mainBlock, writer);
			}
			writer.addCode(", ");
			writer.addCode(maxClosed + ", ");
			if (mTo == null) {
				if (type == Type.INTEGER_INTERVAL) {
					writer.addCode("Long.MAX_VALUE");
				} else {
					writer.addCode("Double.POSITIVE_INFINITY");
				}
			} else {
				mTo.writeJavaCode(mainBlock, writer);
			}
			writer.addCode(")");
		}
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
