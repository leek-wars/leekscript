package leekscript;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public abstract class AILog {

	public final static int STANDARD = 1;
	public final static int WARNING = 2;
	public final static int ERROR = 3;
	public final static int SSTANDARD = 6;
	public final static int SWARNING = 7;
	public final static int SERROR = 8;

	private int mSize = 0;
	private final static int MAX_LENGTH = 500000;

	public abstract void addSystemLog(int type, String trace, int key, String[] parameters);

	public abstract void addSystemLog(AI ai, int type, String trace, int key, Object[] parameters) throws LeekRunException;

	public abstract void addLog(int type, String message);

	public abstract void addLog(int type, String message, int color);

	public boolean addSize(int size) {
		if (mSize + size > MAX_LENGTH) {
			mSize = MAX_LENGTH;
			return false;
		}
		mSize += size;
		return true;
	}

	public boolean isFull() {
		return mSize >= MAX_LENGTH;
	}
}
