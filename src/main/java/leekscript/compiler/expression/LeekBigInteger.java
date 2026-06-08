package leekscript.compiler.expression;

import java.math.BigInteger;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

/**
 * Littéral entier de taille arbitraire (`5L`, `1L << 1000`, ou un littéral trop
 * grand pour tenir dans un `long`). Génère un {@link leekscript.runner.values.BigIntegerValue}.
 *
 * Conçu initialement par Batary (bat_jarry@hotmail.fr).
 */
public class LeekBigInteger extends Expression {

	private final Token token;
	private final BigInteger value;

	public LeekBigInteger(Token token, BigInteger value) {
		this.token = token;
		this.value = value;
		this.token.setExpression(this);
	}

	@Override
	public int getNature() {
		return NUMBER;
	}

	@Override
	public Type getType() {
		return Type.BIG_INT;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addCode("new BigIntegerValue(" + writer.getAIThis() + ", \"" + value.toString() + "\")");
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LeekBigInteger) {
			return value.equals(((LeekBigInteger) o).value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
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
