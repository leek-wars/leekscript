package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekType extends Expression {

	public Token token;
	public Type type;

	public LeekType(Token token) {
		this.token = token;
		token.setExpression(this);
	}

	public LeekType(Token token, Type type) {
		this.token = token;
		this.type = type;
		token.setExpression(this);
	}

	@Override
	public Hover hover(Token token) {
		var clazz = this.type.getClassDeclaration();
		return new Hover(getType(), getLocation(), clazz != null ? clazz.getLocation() : null);
	}

	@Override
	public String toString() {
		return type.getCode();
	}

	@Override
	public Location getLocation() {
		return this.token.getLocation();
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public int getNature() {
		return Expression.TYPE;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addCode(type.getJavaPrimitiveName(mainblock.getVersion()));
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
