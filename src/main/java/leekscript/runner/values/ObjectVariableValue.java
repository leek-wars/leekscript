package leekscript.runner.values;

import leekscript.common.AccessLevel;
import leekscript.common.Type;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class ObjectVariableValue extends Box {

	public AccessLevel level;
	public Type type;
	public boolean isFinal = false;

	public ObjectVariableValue(AI ai, AccessLevel level, boolean isFinal) throws LeekRunException {
		this(ai, Type.ANY, null, level, isFinal);
	}

	public ObjectVariableValue(AI ai, Object value, AccessLevel level, boolean isFinal) throws LeekRunException {
		this(ai, Type.ANY, value, level, isFinal);
	}

	public ObjectVariableValue(AI ai, Type type, Object value, AccessLevel level, boolean isFinal) throws LeekRunException {
		super(ai, value);
		this.level = level;
		this.type = type;
		this.isFinal = isFinal;
	}
}
