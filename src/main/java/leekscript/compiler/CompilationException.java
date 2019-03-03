package leekscript.compiler;

public class CompilationException extends Exception {

	private static final long serialVersionUID = -954637252185851811L;

	private final String mError;

	public CompilationException(String error) {
		mError = error;
	}

	@Override
	public String getMessage() {
		return mError;
	}
}
