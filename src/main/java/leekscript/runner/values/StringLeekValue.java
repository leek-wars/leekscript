package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekOperations;

public class StringLeekValue extends AbstractLeekValue {

	private String mValue;

	public StringLeekValue(String value) {
		mValue = value;
	}

	@Override
	public String getString(AI ai) {
		return mValue;
	}

	@Override
	public boolean getBoolean() {
		if (mValue.equals("false") || mValue.equals("0")) {
			return false;
		}
		return !mValue.isEmpty();
	}

	@Override
	public int getInt(AI ai) throws LeekRunException {
		if (mValue.isEmpty())
			return 0;
		if (mValue.equals("true"))
			return 1;
		if (mValue.equals("false"))
			return 0;
		ai.addOperations(mValue.length());
		try {
			return Integer.parseInt(mValue);
		} catch (Exception e) {
			return mValue.length();
		}
	}

	@Override
	public double getDouble(AI ai) throws LeekRunException {
		if (mValue.equals("true"))
			return 1;
		if (mValue.equals("false"))
			return 0;
		if (mValue.isEmpty())
			return 0;
		ai.addOperations(mValue.length());
		try {
			return Double.parseDouble(mValue);
		} catch (Exception e) {
			return mValue.length();
		}
	}

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws LeekRunException {
		String s = val.getString(ai);
		ai.addOperations(1 + s.length() + mValue.length());
		mValue += s;
		return this;
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
		return STRING_V10;
	}


	@Override
	public int getType() {
		return STRING;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp.getType() == NUMBER) {
			if (mValue.equals("false") || mValue.equals("0") || mValue.equals("")) {
				return comp.getInt(ai) == 0;
			}
			if (mValue.equals("true")) {
				return comp.getDouble(ai) != 0;
			}
			if (mValue.equals("1") && comp.getDouble(ai) == 1) {
				return true;
			}
			if (comp instanceof IntLeekValue)
				return getInt(ai) == comp.getInt(ai);
			else
				return getDouble(ai) == comp.getDouble(ai);
		} else if (comp.getType() == BOOLEAN) {
			return getBoolean() == comp.getBoolean();
		} else if (comp.getType() == STRING) {
			String s = comp.getString(ai);
			ai.addOperations(Math.min(
				s.length(),
				mValue.length()
			));
			return mValue.equals(s);
		} else if (comp.getType() == ARRAY) {
			return comp.equals(ai, this);
		}
		return false;
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
