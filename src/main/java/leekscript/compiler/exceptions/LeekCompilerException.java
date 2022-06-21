package leekscript.compiler.exceptions;

import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.common.Error;

public class LeekCompilerException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Location location;
	private final Error mError;
	private final String[] mParameters;

	public LeekCompilerException(Location location, Error error) {
		this(location, error, null);
	}

	public LeekCompilerException(Token token, Error error) {
		this(token.getLocation(), error, null);
	}

	public LeekCompilerException(Token token, Error error, String[] parameters) {
		this(token.getLocation(), error, parameters);
	}

	public LeekCompilerException(Location location, Error error, String[] parameters) {
		this.location = location;
		mError = error;
		mParameters = parameters;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public Location getLocation() {
		return location;
	}

	@Override
	public String getMessage() {
		return location.getFile().getPath() + ":" + location.getStartLine() + " : " + location.getStartColumn() + " : " + mError.name();
	}

	public Error getError() {
		return mError;
	}
}
