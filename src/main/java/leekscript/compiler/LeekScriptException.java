package leekscript.compiler;

import leekscript.common.Error;

public class LeekScriptException extends Exception {

	private static final long serialVersionUID = -5149423928011355230L;

	private final Error mType;
	private String mMessage = null;
	private String mLocation = null;

	public LeekScriptException(Error type) {
		mType = type;
	}

	public LeekScriptException(Error type, String message) {
		mType = type;
		mMessage = message;
	}

	public LeekScriptException(Error type, String message, String location) {
		mType = type;
		mMessage = message;
		mLocation = location;
	}

	public Error getType() {
		return mType;
	}

	@Override
	public String getMessage() {
		return mType.name() + " : " + mMessage;
	}

	public String getLocation() {
		return mLocation;
	}
}
