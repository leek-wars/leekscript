package leekscript.runner.values;

import java.text.DecimalFormat;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public class DoubleLeekValue extends AbstractLeekValue {

	private double mValue;

	public DoubleLeekValue(double value) {
		mValue = value;
	}

	@Override
	public int getSize() {
		return 2;
	}

	@Override
	public int getInt(AI ai) {
		return (int) mValue;
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
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(0);
		return df.format(mValue);
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public AbstractLeekValue increment(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		mValue++;
		return new DoubleLeekValue(mValue - 1d);
	}

	@Override
	public AbstractLeekValue decrement(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		mValue--;
		return new DoubleLeekValue(mValue + 1d);
	}

	@Override
	public AbstractLeekValue pre_increment(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		mValue++;
		return new DoubleLeekValue(mValue);
	}

	@Override
	public AbstractLeekValue pre_decrement(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		mValue--;
		return new DoubleLeekValue(mValue);
	}

	@Override
	public boolean less(AI ai, AbstractLeekValue comp) throws LeekRunException {
		ai.addOperations(1);
		return getDouble(ai) < comp.getDouble(ai);
	}

	@Override
	public boolean more(AI ai, AbstractLeekValue comp) throws LeekRunException {
		ai.addOperations(1);
		return getDouble(ai) > comp.getDouble(ai);
	}

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(ADD_COST);
		mValue += val.getDouble(ai);
		return this;
	}

	@Override
	public AbstractLeekValue minus(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(ADD_COST);
		mValue -= val.getDouble(ai);
		return this;
	}

	@Override
	public AbstractLeekValue multiply(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(MUL_COST);
		mValue *= val.getDouble(ai);
		return this;
	}

	@Override
	public AbstractLeekValue divide(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(DIV_COST);
		mValue /= val.getDouble(ai);
		return this;
	}

	@Override
	public AbstractLeekValue modulus(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(MOD_COST);
		mValue %= val.getDouble(ai);
		return this;
	}

	@Override
	public AbstractLeekValue opposite(AI ai) throws LeekRunException {
		ai.addOperations(ADD_COST);
		return new DoubleLeekValue(-getDouble(ai));
	}

	@Override
	public AbstractLeekValue power(AI ai, AbstractLeekValue val) throws LeekRunException {
		ai.addOperations(POW_COST);
		mValue = Math.pow(mValue, val.getDouble(ai));
		return this;
	}

	@Override
	public AbstractLeekValue band(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(getInt(ai) & value.getInt(ai));
	}

	@Override
	public AbstractLeekValue bor(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(getInt(ai) | value.getInt(ai));
	}

	@Override
	public AbstractLeekValue bxor(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(getInt(ai) ^ value.getInt(ai));
	}

	@Override
	public AbstractLeekValue bleft(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(getInt(ai) << value.getInt(ai));
	}

	@Override
	public AbstractLeekValue bright(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(getInt(ai) >> value.getInt(ai));
	}

	@Override
	public AbstractLeekValue brotate(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(1);
		return LeekValueManager.getLeekIntValue(getInt(ai) >>> value.getInt(ai));
	}

	@Override
	public int getType() {
		return NUMBER;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp.getType() == NUMBER) {
			return comp.getDouble(ai) == mValue;
		} else if (comp.getType() == BOOLEAN) {
			return comp.getBoolean() == getBoolean();
		} else if (comp.getType() == STRING) {
			if (comp.getString(ai).equals("true"))
				return mValue != 0;
			return mValue == comp.getDouble(ai);
		} else if (comp.getType() == ARRAY) {
			return comp.equals(ai, this);
		}
		return false;
	}

	@Override
	public Object toJSON(AI ai) {
		return mValue;
	}
}
