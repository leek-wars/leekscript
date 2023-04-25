package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.List;

import leekscript.compiler.Location;
import leekscript.compiler.Token;

public class LeekParameterType extends LeekType {

	public Token token;
	public List<LeekType> parameters = new ArrayList<>();
	public Token chevronRight;

	public LeekParameterType(Token token, Token chevronLeft) {
		super(token);
		this.token = token;
		chevronLeft.setExpression(this);
	}

	@Override
	public String toString() {
		return type.toString();
	}

	public void close(Token chevronRight) {
		this.chevronRight = chevronRight;
		chevronRight.setExpression(this);
	}

	@Override
	public Location getLocation() {
		return new Location(this.token.getLocation(), this.chevronRight.getLocation());
	}
}
