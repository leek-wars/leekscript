package leekscript.compiler.expression;

public class LeekExpressionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5724420043991763088L;
	private final AbstractExpression mExpression;
	private final String mMessage;

	public LeekExpressionException(AbstractExpression exp, String message) {
		mExpression = exp;
		mMessage = message;
	}

	@Override
	public String getMessage() {
		return mMessage;
	}

	public String getExpression() {
		return mExpression.getString();
	}
}
