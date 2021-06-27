package leekscript.runner.values;

import java.util.Arrays;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekAnonymousFunction;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.common.Error;

public class FunctionLeekValue {

	private final static int LEEK_FUNCTION = 1;
	private final static int USER_FUNCTION = 2;
	private final static int ANONYMOUS_FUNCTION = 3;
	private final static int METHOD = 4;

	private final int mType;
	private final int mId;
	private ILeekFunction mFunction;
	private LeekAnonymousFunction mAnonymous;

	public FunctionLeekValue(int id) {
		mType = USER_FUNCTION;
		mId = id;
	}

	public FunctionLeekValue(int id, LeekAnonymousFunction fonction) {
		mAnonymous = fonction;
		mType = ANONYMOUS_FUNCTION;
		mId = id;
	}

	public FunctionLeekValue(ILeekFunction fonction) {
		mFunction = fonction;
		mType = LEEK_FUNCTION;
		mId = 0;
	}

	public FunctionLeekValue(LeekAnonymousFunction fonction) {
		mAnonymous = fonction;
		mType = METHOD;
		mId = 0;
	}

	public boolean equals(AI ai, Object comp) {
		if (LeekValueManager.getType(comp) != LeekValue.FUNCTION) {
			return false;
		}
		if (!(comp instanceof FunctionLeekValue)) {
			return false;
		}
		if (mType != ((FunctionLeekValue) comp).mType) {
			return false;
		}
		if (mType == LEEK_FUNCTION) {
			return mFunction == ((FunctionLeekValue) comp).mFunction;
		} else {
			return mId == ((FunctionLeekValue) comp).mId;
		}
	}

	private Object[] prepareValues(Object[] values, int count) {
		Object[] retour = new Object[count];
		for (int i = 0; i < count; i++) {
			retour[i] = (i >= values.length) ? null : LeekValueManager.getValue(values[i]);
		}
		return retour;
	}

	private Object[] copyValues(AI uai, Object[] values, boolean[] references) throws LeekRunException {
		Object[] copy = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			if (!references[i] && values[i] instanceof Box)
				copy[i] = LeekOperations.clone(uai, values[i]);
			else
				copy[i] = values[i];
		}
		return copy;
	}

	public int getArgumentsCount(AI ai) throws LeekRunException {
		if (mType == LEEK_FUNCTION)
			return mFunction.getArguments();
		else if (mType == USER_FUNCTION)
			return ai.userFunctionCount(mId);
		else if (mType == ANONYMOUS_FUNCTION)
			return ai.anonymousFunctionCount(mId);
		return -1;
	}

	public Object execute(AI ai, Object... values) throws LeekRunException {
		if (mType == LEEK_FUNCTION) {
			return ai.sysexec(mFunction, prepareValues(values, mFunction.getArguments()));
		}
		else if (mType == USER_FUNCTION) {
			if (values.length != ai.userFunctionCount(mId)) {
				ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { LeekValue.getParamString(values), String.valueOf(ai.userFunctionCount(mId)) });
			}
			else {
				if (ai.getVersion() >= 11) {
					return ai.userFunctionExecute(mId, values);
				} else {
					return ai.userFunctionExecute(mId, copyValues(ai, values, ai.userFunctionReference(mId)));
				}
			}
		} else if (mType == ANONYMOUS_FUNCTION) {
			if (values.length != ai.anonymousFunctionCount(mId)) {
				ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { LeekValue.getParamString(values), String.valueOf(ai.anonymousFunctionCount(mId)) });
			} else {
				return mAnonymous.run(null, values);
			}
		} else if (mType == METHOD) {
			if (values.length == 0) {
				ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { "", "1+" });
			} else {
				return mAnonymous.run((ObjectLeekValue) values[0], Arrays.copyOfRange(values, 1, values.length));
			}
		}
		return null;
	}

	public String getString(AI ai) {
		if (mType == LEEK_FUNCTION)
			return "#Function " + mFunction;
		else if (mType == USER_FUNCTION)
			return "#User Function";
		else if (mType == METHOD)
			return "#Method Function";
		else
			return "#Anonymous Function";
	}

	public FunctionLeekValue cloneFunction() {
		if (mType == LEEK_FUNCTION)
			return new FunctionLeekValue(mFunction);
		else if (mType == USER_FUNCTION)
			return new FunctionLeekValue(mId);
		else
			return new FunctionLeekValue(mId, mAnonymous);
	}

	public Object toJSON(AI ai) {
		return "<function>";
	}
}
