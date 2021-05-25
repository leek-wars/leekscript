package leekscript;

public class LSException extends Exception {

	private static final long serialVersionUID = -8672880192298794957L;

	private final int mIndex;
	private final Object mRun;
	private final Object mThe;

	public LSException(int i, Object run, Object the) {
		mIndex = i;
		mRun = run;
		mThe = the;
	}

	public int getIndex() {
		return mIndex;
	}

	public Object getRun() {
		return mRun;
	}

	public Object getThe() {
		return mThe;
	}

}
