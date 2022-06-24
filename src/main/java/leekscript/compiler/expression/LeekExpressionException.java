package leekscript.compiler.expression;

import leekscript.common.Error;

public class LeekExpressionException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 5724420043991763088L;
	private final Expression mExpression;
	private final Error mError;

	public LeekExpressionException(Expression exp, Error error) {
		mExpression = exp;
		mError = error;
	}

	@Override
	public String getMessage() {
		return mError.name();
	}

	public String getExpression() {
		return mExpression.toString();
	}

	public Error getError() {
		return mError;
	}
}
