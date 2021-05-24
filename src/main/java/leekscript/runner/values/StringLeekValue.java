package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

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
		ai.addOperations(2);
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
			return 1;
		}
	}

	@Override
	public double getDouble(AI ai) throws LeekRunException {
		ai.addOperations(2);
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
			return 1;
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
			if (mValue.equals("true"))
				return comp.getDouble(ai) != 0;
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
