package leekscript.compiler.expression;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Token;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekReal extends LeekNumber {

	private final double value;

	public LeekReal(Token token, double value) {
		super(token, Type.REAL);
		this.value = value;
	}

	@Override
	public boolean isInfinity() {
		return value == Double.POSITIVE_INFINITY;
	}

	@Override
	public String toString() {
		var formatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
		formatter.setMaximumFractionDigits(15);
		formatter.setGroupingUsed(false);
		return formatter.format(value);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (value == Double.POSITIVE_INFINITY) {
			writer.addCode("Double.POSITIVE_INFINITY");
		} else {
			writer.addCode(String.valueOf(value));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LeekReal) {
			return value == ((LeekReal) o).value;
		}
		return false;
	}
}
