package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public class VariableLeekValue extends AbstractLeekValue {

	protected AbstractLeekValue mValue;
	protected AI mUAI = null;

	public VariableLeekValue(AI uai, AbstractLeekValue value) throws Exception {
		mUAI = uai;
		mUAI.addOperations(1);
		if (!(value instanceof VariableLeekValue))
			mValue = value.getValue();
		else {
			if (value.isReference())
				mValue = value.getValue();
			else
				mValue = LeekOperations.clone(uai, value.getValue());
		}
	}

	@Override
	public int getSize() throws LeekRunException {
		return mValue.getSize();
	}

	@Override
	public int getInt(AI ai) throws LeekRunException {
		return mValue.getInt(ai);
	}

	@Override
	public double getDouble(AI ai) throws LeekRunException {
		return mValue.getDouble(ai);
	}

	@Override
	public String getString(AI ai) throws LeekRunException {
		return mValue.getString(ai);
	}

	@Override
	public boolean getBoolean() {
		return mValue.getBoolean();
	}

	@Override
	public ArrayLeekValue getArray() {
		return mValue.getArray();
	}

	@Override
	public boolean isArray() {
		return mValue.isArray();
	}

	@Override
	public boolean isNull() {
		return mValue.isNull();
	}

	// Fonctions spéciales L-Values
	@Override
	public AbstractLeekValue getValue() {
		return mValue;
	}

	@Override
	public AbstractLeekValue set(AI ai, AbstractLeekValue value) throws Exception {
		ai.addOperations(1);
		if (value.isReference())
			return mValue = value.getValue();
		else
			return mValue = LeekOperations.clone(ai, value.getValue());
	}

	@Override
	public AbstractLeekValue increment(AI ai) throws Exception {
		mValue = mValue.getValue();
		if (mValue instanceof IntLeekValue) {
			ai.addOperations(1);
			int value = mValue.getInt(ai);
			mValue = LeekValueManager.getLeekIntValue(ai, value + 1, mValue);
			return LeekValueManager.getLeekIntValue(value);
		} else
			return mValue.increment(ai);
	}

	@Override
	public AbstractLeekValue decrement(AI ai) throws Exception {
		mValue = mValue.getValue();
		if (mValue instanceof IntLeekValue) {
			ai.addOperations(1);
			int value = mValue.getInt(ai);
			mValue = LeekValueManager.getLeekIntValue(ai, value - 1, mValue);
			return LeekValueManager.getLeekIntValue(value);
		} else
			return mValue.decrement(ai);
	}

	@Override
	public AbstractLeekValue pre_increment(AI ai) throws Exception {
		mValue = mValue.getValue();
		if (mValue instanceof IntLeekValue) {
			ai.addOperations(1);
			int value = mValue.getInt(ai);
			return mValue = LeekValueManager.getLeekIntValue(ai, value + 1, mValue);
		} else
			return mValue.pre_increment(ai);
	}

	@Override
	public AbstractLeekValue pre_decrement(AI ai) throws Exception {
		mValue = mValue.getValue();
		if (mValue instanceof IntLeekValue) {
			ai.addOperations(1);
			int value = mValue.getInt(ai);
			return mValue = LeekValueManager.getLeekIntValue(ai, value - 1, mValue);
		} else
			return mValue.pre_decrement(ai);
	}

	@Override
	public AbstractLeekValue opposite(AI ai) throws Exception {
		return mValue.opposite(ai);
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return mValue.equals(ai, comp);
	}

	@Override
	public boolean less(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return mValue.less(ai, comp);
	}

	@Override
	public boolean more(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return mValue.more(ai, comp);
	}

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.add(ai, val);
	}

	@Override
	public AbstractLeekValue minus(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.minus(ai, val);
	}

	@Override
	public AbstractLeekValue multiply(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.multiply(ai, val);
	}

	@Override
	public AbstractLeekValue power(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.power(ai, val);
	}

	@Override
	public AbstractLeekValue band(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.band(ai, val);
	}

	@Override
	public AbstractLeekValue bor(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.bor(ai, val);
	}

	@Override
	public AbstractLeekValue bxor(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.bxor(ai, val);
	}

	@Override
	public AbstractLeekValue bleft(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.bleft(ai, val);
	}

	@Override
	public AbstractLeekValue bright(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.bright(ai, val);
	}

	@Override
	public AbstractLeekValue brotate(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.brotate(ai, val);
	}

	@Override
	public AbstractLeekValue divide(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.divide(ai, val);
	}

	@Override
	public AbstractLeekValue modulus(AI ai, AbstractLeekValue val) throws Exception {
		return mValue = mValue.modulus(ai, val);
	}

	@Override
	public int getType() {
		return mValue.getType();
	}

	@Override
	public boolean isReference() {
		return mValue.isReference();
	}

	@Override
	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue[] values) throws Exception {
		return mValue.executeFunction(ai, values);
	}

	@Override
	public int getArgumentsCount(AI ai) throws Exception {
		return mValue.getArgumentsCount(ai);
	}

	@Override
	public Object toJSON(AI ai) throws Exception {
		return mValue.toJSON(ai);
	}
}
