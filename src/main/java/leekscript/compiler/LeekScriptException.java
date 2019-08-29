package leekscript.compiler;

public class LeekScriptException extends Exception {

	private static final long serialVersionUID = -5149423928011355230L;

	public final static int CANT_COMPILE = 0;
	public final static int CODE_TOO_LARGE = 1;
	public final static int CODE_TOO_LARGE_FUNCTION = 2;

	private final int mType;
	private String mMessage = null;

	public LeekScriptException(int type) {
		mType = type;
	}

	public LeekScriptException(int type, String message) {
		mType = type;
		mMessage = message;
	}

	public int getType() {
		return mType;
	}

	@Override
	public String getMessage() {
		return mMessage;
	}
}
