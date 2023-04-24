package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;

public class LeekTypeExpression extends LeekExpression {

	public Token token;

	public LeekTypeExpression(Token token, Type type) {
		this.token = token;
		this.type = type;
		token.setExpression(this);
	}

	@Override
	public Hover hover(Token token) {
		var clazz = this.type.getClassDeclaration();
		return new Hover(getType(), this.token.getLocation(), clazz != null ? clazz.getLocation() : null);
	}
}
