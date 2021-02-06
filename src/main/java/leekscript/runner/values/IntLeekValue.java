package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public class IntLeekValue extends AbstractLeekValue {
	private int mValue;

	public IntLeekValue(int value) {
		mValue = value;
	}

	@Override
	public void setInt(int nb) {
		mValue = nb;
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public int getInt(AI ai) {
		return mValue;
	}

	@Override
	public double getDouble(AI ai) {
		return mValue;
	}

	@Override
	public boolean getBoolean() {
		return mValue != 0;
	}

	@Override
	public String getString(AI ai) throws LeekRunException {
		ai.addOperations(3);
		return String.valueOf(mValue);
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public AbstractLeekValue increment(AI ai) {
		return this;
	}

	@Override
	public AbstractLeekValue decrement(AI ai) {
		return this;
	}

	@Override
	public AbstractLeekValue pre_increment(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		return LeekValueManager.getLeekIntValue(mValue + 1);
	}

	@Override
	public AbstractLeekValue pre_decrement(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		return LeekValueManager.getLeekIntValue(mValue - 1);
	}

	@Override
	public boolean less(AI ai, AbstractLeekValue comp) throws LeekRunException {
		ai.addOperations(1);
		comp = comp.getValue();
		if (comp instanceof DoubleLeekValue)
			return getDouble(ai) < comp.getDouble(ai);
		return super.less(ai, comp);
	}

	@Override
	public boolean more(AI ai, AbstractLeekValue comp) throws LeekRunException {
		ai.addOperations(1);
		comp = comp.getValue();
		if (comp instanceof DoubleLeekValue)
			return getDouble(ai) > comp.getDouble(ai);
		return super.more(ai, comp);
	}

	// Assign:

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(ADD_COST);
		val = val.getValue();
		if (val instanceof DoubleLeekValue)
			return LeekValueManager.getLeekDoubleValue(mValue + val.getDouble(ai));
		return LeekValueManager.getLeekIntValue(ai, mValue + val.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue minus(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(ADD_COST);
		val = val.getValue();
		if (val instanceof DoubleLeekValue)
			return LeekValueManager.getLeekDoubleValue(mValue - val.getDouble(ai));
		return LeekValueManager.getLeekIntValue(ai, mValue - val.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue multiply(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(MUL_COST);
		val = val.getValue();
		if (val instanceof DoubleLeekValue)
			return LeekValueManager.getLeekDoubleValue(mValue * val.getDouble(ai));
		return LeekValueManager.getLeekIntValue(ai, mValue * val.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue power(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(POW_COST);
		val = val.getValue();
		if (val instanceof DoubleLeekValue)
			return new DoubleLeekValue(Math.pow(mValue, val.getDouble(ai)));
		return LeekValueManager.getLeekIntValue(ai, (int) Math.pow(mValue, val.getInt(ai)), this);
	}

	@Override
	public AbstractLeekValue band(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(ai, mValue & value.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue bor(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(ai, mValue | value.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue bxor(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(ai, mValue ^ value.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue bleft(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(ai, mValue << value.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue bright(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(ai, mValue >> value.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue brotate(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(ai, mValue >>> value.getInt(ai), this);
	}

	@Override
	public AbstractLeekValue divide(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(DIV_COST);
		return LeekOperations.divide(ai, this, val);
	}

	@Override
	public AbstractLeekValue modulus(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(MOD_COST);
		val = val.getValue();
		if (val instanceof DoubleLeekValue)
			return LeekValueManager.getLeekDoubleValue(mValue % val.getDouble(ai));
		return LeekValueManager.getLeekIntValue(ai, mValue % val.getInt(ai), this);
	}

	@Override
	public int getType() {
		return NUMBER;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp instanceof IntLeekValue) {
			return comp.getInt(ai) == mValue;
		}
		else if (comp.getType() == NUMBER) {
			return comp.getDouble(ai) == mValue;
		}
		else if (comp.getType() == BOOLEAN) {
			return comp.getBoolean() == getBoolean();
		}
		else if (comp.getType() == STRING) {
			if (comp.getString(ai).equals("true"))
				return mValue != 0;
			return mValue == comp.getInt(ai);
		}
		else if (comp.getType() == ARRAY) {
			return comp.equals(ai, this);
		}
		return false;
	}

	@Override
	public Object toJSON(AI ai) {
		return mValue;
	}
}
