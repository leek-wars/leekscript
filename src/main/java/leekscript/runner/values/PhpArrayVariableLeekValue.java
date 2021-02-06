package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.PhpArray;

public class PhpArrayVariableLeekValue extends VariableLeekValue {

	private final PhpArray mArray;
	private int mTotalSize = 0;

	public PhpArrayVariableLeekValue(PhpArray array, AI uai, AbstractLeekValue value, int keySize) throws LeekRunException {
		super(uai, value.getValue());
		mArray = array;
		mTotalSize = value.getSize();
		mArray.updateArraySize(mTotalSize + keySize);
		if (mValue instanceof ArrayLeekValue) {
			mValue.getArray().setParent(this);
		}
	}

	@Override
	public AbstractLeekValue set(AI ai, AbstractLeekValue value) throws LeekRunException {
		value = value.getValue();
		int size = value.getSize();
		mArray.updateArraySize(size - mTotalSize);
		mTotalSize = size;
		super.set(ai, value);
		if (mValue instanceof ArrayLeekValue)
			mValue.getArray().setParent(this);
		return mValue;
	}

	public void removeFromTable(int keySize) throws LeekRunException {
		mArray.updateArraySize(-mTotalSize - keySize);
	}

	public void updateSize(int delta) throws LeekRunException {
		mTotalSize += delta;
		mArray.updateArraySize(delta);
	}
}
