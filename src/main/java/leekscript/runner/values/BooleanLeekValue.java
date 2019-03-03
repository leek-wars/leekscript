package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;

public class BooleanLeekValue extends AbstractLeekValue {

	public final boolean mValue;

	public BooleanLeekValue(boolean value) {
		mValue = value;
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public boolean getBoolean() {
		return mValue;
	}

	@Override
	public int getInt(AI ai) {
		return mValue ? 1 : 0;
	}

	@Override
	public double getDouble(AI ai) {
		return mValue ? 1 : 0;
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public String getString(AI ai) {
		return mValue ? "true" : "false";
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
		return BOOLEAN;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp.getType() == NULL)
			return false;
		else if (comp.getType() == ARRAY)
			return comp.equals(ai, this);
		return mValue == comp.getBoolean();
	}

	@Override
	public Object toJSON(AI ai) {
		return mValue;
	}
}
