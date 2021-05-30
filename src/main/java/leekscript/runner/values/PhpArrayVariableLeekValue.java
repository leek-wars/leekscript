package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.PhpArray;

public class PhpArrayVariableLeekValue extends VariableLeekValue {

	public PhpArrayVariableLeekValue(PhpArray array, AI uai, AbstractLeekValue value, int keySize) throws LeekRunException {
		super(uai, value.getValue());
	}

	@Override
	public AbstractLeekValue set(AI ai, AbstractLeekValue value) throws LeekRunException {
		value = value.getValue();
		super.set(ai, value);
		return mValue;
	}

	@Override
	public AbstractLeekValue setNoOps(AI ai, AbstractLeekValue value) throws LeekRunException {
		value = value.getValue();
		super.setNoOps(ai, value);
		return mValue;
	}
}
