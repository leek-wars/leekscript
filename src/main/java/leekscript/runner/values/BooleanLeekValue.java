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
	public AbstractLeekValue power(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.power(ai, this, val);
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
	public AbstractLeekValue band(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.band(ai, this, val);
	}

	@Override
	public AbstractLeekValue bor(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.bor(ai, this, val);
	}

	@Override
	public AbstractLeekValue bxor(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.bxor(ai, this, val);
	}

	@Override
	public AbstractLeekValue bleft(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.bleft(ai, this, val);
	}

	@Override
	public AbstractLeekValue bright(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.bright(ai, this, val);
	}

	@Override
	public AbstractLeekValue brotate(AI ai, AbstractLeekValue val) throws LeekRunException {
		return LeekOperations.brotate(ai, this, val);
	}

	@Override
	public int getV10Type() {
		return BOOLEAN_V10;
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

	@Override
	public boolean isPrimitive() {
		return true;
	}
}
