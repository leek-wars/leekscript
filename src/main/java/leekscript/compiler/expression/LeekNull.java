package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekNull extends Expression {

	private final Token token;

	public LeekNull(Token token) {
		this.token = token;
		token.setExpression(this);
	}

	@Override
	public int getNature() {
		return NULL;
	}

	@Override
	public Type getType() {
		return Type.NULL;
	}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		//Pour une valeur null pas de soucis
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("null");
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
