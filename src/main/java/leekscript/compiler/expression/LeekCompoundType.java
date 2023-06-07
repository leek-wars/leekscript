package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Location;
import leekscript.compiler.Token;

public class LeekCompoundType extends LeekType {

	public LeekType type1;
	public LeekType type2;

	public LeekCompoundType(LeekType type1, LeekType type2, Token pipe) {
		super(pipe);
		this.type1 = type1;
		this.type2 = type2;
		pipe.setExpression(this);
		this.type = Type.compound(type1.getType(), type2.getType());
	}

	@Override
	public String toString() {
		return type.toString();
	}

	@Override
	public Location getLocation() {
		return new Location(this.type1.getLocation(), this.type2.getLocation());
	}
}
