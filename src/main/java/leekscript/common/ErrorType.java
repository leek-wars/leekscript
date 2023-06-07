package leekscript.common;

public class ErrorType extends Type {

	public ErrorType() {
		super("error", "e", "", "", "");
	}

	@Override
	public boolean isWarning() {
		return true;
	}
}
