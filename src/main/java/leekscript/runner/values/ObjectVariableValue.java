package leekscript.runner.values;

import leekscript.common.AccessLevel;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class ObjectVariableValue extends Box {

	public AccessLevel level;

	public ObjectVariableValue(AI ai, AccessLevel level) throws LeekRunException {
		super(ai);
		this.level = level;
	}

	public ObjectVariableValue(AI ai, Object value, AccessLevel level) throws LeekRunException {
		super(ai, value);
		this.level = level;
	}
}
