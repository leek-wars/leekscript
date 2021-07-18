package leekscript.compiler;

import leekscript.common.Error;

public class LeekScriptException extends Exception {

	private static final long serialVersionUID = -5149423928011355230L;

	private final Error mType;
	private String mMessage = null;

	public LeekScriptException(Error type) {
		mType = type;
	}

	public LeekScriptException(Error type, String message) {
		mType = type;
		mMessage = message;
	}

	public Error getType() {
		return mType;
	}

	@Override
	public String getMessage() {
		return mType.name() + " : " + mMessage;
	}
}
