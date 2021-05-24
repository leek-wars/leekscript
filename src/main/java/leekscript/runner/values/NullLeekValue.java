package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;

public class NullLeekValue extends AbstractLeekValue {
	@Override
	public boolean isNull() {
		return true;
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
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.add(ai, this, val);
	}

	@Override
	public AbstractLeekValue minus(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.minus(ai, this, val);
	}

	@Override
	public AbstractLeekValue multiply(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.multiply(ai, this, val);
	}

	@Override
	public AbstractLeekValue divide(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.divide(ai, this, val);
	}

	@Override
	public AbstractLeekValue modulus(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.modulus(ai, this, val);
	}

	@Override
	public AbstractLeekValue power(AI ai, AbstractLeekValue value) throws LeekRunException {
		return LeekOperations.power(ai, this, value);
	}

	@Override
	public AbstractLeekValue bor(AI ai, AbstractLeekValue value) throws LeekRunException {
		return LeekOperations.bor(ai, this, value);
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

	@Override
	public boolean isPrimitive() {
		return true;
	}
}
