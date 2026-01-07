package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public abstract class LeekNumber extends Expression {

	private final Token token;
	private Type type;

	public LeekNumber(Token token, Type type) {
		this.token = token;
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

	public abstract boolean isInfinity();

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// Pour un nombre pas de soucis
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {

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
