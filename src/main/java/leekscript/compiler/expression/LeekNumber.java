package leekscript.compiler.expression;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekNumber extends Expression {

	private final Token token;
	private final double doubleValue;
	private final long longValue;
	private Type type;

	public LeekNumber(Token token, double doubleValue, long longValue, Type type) {
		this.token = token;
		this.doubleValue = doubleValue;
		this.longValue = longValue;
		this.type = type;
		this.token.setExpression(this);
	}

	@Override
	public int getNature() {
		return NUMBER;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		if (type == Type.REAL) {
			var formatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
			formatter.setMaximumFractionDigits(15);
			formatter.setGroupingUsed(false);
			return formatter.format(doubleValue);
		} else {
			return String.valueOf(longValue);
		}
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// Pour un nombre pas de soucis
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == Type.INT) {
			writer.addCode(String.valueOf(longValue) + "l");
		} else {
			writer.addCode(String.valueOf(doubleValue));
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}

	public boolean equals(Object o) {
		if (o instanceof LeekNumber) {
			var n = (LeekNumber) o;
			if (type != n.type) return false;
			if (type == Type.INT) return longValue == n.longValue;
			return doubleValue == n.doubleValue;
		}
		return false;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public Hover hover(Token token) {
		return new Hover(getType(), getLocation(), toString());
	}
}
