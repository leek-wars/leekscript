package leekscript;

import leekscript.runner.values.AbstractLeekValue;

public class LSException extends Exception {

	private static final long serialVersionUID = -8672880192298794957L;

	private final int mIndex;
	private final AbstractLeekValue mRun;
	private final AbstractLeekValue mThe;

	public LSException(int i, AbstractLeekValue run, AbstractLeekValue the) {
		mIndex = i;
		mRun = run;
		mThe = the;
	}

	public int getIndex() {
		return mIndex;
	}

	public AbstractLeekValue getRun() {
		return mRun;
	}

	public AbstractLeekValue getThe() {
		return mThe;
	}

}
