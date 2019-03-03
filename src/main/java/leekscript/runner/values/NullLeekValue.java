package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;

public class NullLeekValue extends AbstractLeekValue {
	@Override
	public boolean isNull() {
		return true;
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public String getString(AI ai) {
		return "null";
	}

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws Exception {
		return LeekOperations.add(ai, this, val);
	}

	@Override
	public AbstractLeekValue multiply(AI ai, AbstractLeekValue val) throws Exception {
		return LeekOperations.multiply(ai, this, val);
	}

	@Override
	public AbstractLeekValue divide(AI ai, AbstractLeekValue val) throws Exception {
		return LeekOperations.divide(ai, this, val);
	}

	@Override
	public AbstractLeekValue modulus(AI ai, AbstractLeekValue val) throws Exception {
		return LeekOperations.modulus(ai, this, val);
	}

	@Override
	public int getType() {
		return NULL;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) {
		return comp.getType() == NULL;
	}

	@Override
	public Object toJSON(AI ai) {
		return null;
	}
}
