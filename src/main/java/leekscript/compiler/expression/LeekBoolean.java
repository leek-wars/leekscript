package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekBoolean extends Expression {

	private final Token token;
	private final boolean mValue;

	public LeekBoolean(Token token, boolean value) {
		this.token = token;
		mValue = value;
	}

	@Override
	public int getNature() {
		return BOOLEAN;
	}

	@Override
	public Type getType() {
		return Type.BOOL;
	}

	@Override
	public String getString() {
		return mValue ? "true" : "false";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode(mValue ? "true" : "false");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}

	public boolean equals(Object o) {
		if (o instanceof LeekBoolean) {
			return mValue == ((LeekBoolean) o).mValue;
		}
		return false;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}
}
