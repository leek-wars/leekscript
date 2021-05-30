package leekscript.runner.values;

import java.util.Arrays;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekAnonymousFunction;
import leekscript.runner.LeekFunctions;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.common.Error;

public class FunctionLeekValue extends AbstractLeekValue {

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

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) {
		if (comp.getType() != getType()) {
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

	@Override
	public int getV10Type() {
		return FUNCTION_V10;
	}

	@Override
	public int getType() {
		return FUNCTION;
	}

	private AbstractLeekValue[] copyValues(AI uai, AbstractLeekValue[] values, boolean[] references) throws LeekRunException {
		AbstractLeekValue[] copy = new AbstractLeekValue[values.length];
		for (int i = 0; i < values.length; i++) {
			if (!references[i] && values[i] instanceof VariableLeekValue)
				copy[i] = LeekOperations.clone(uai, values[i]);
			else
				copy[i] = values[i];
		}
		return copy;
	}

	private AbstractLeekValue[] copyPrimitiveValues(AI uai, AbstractLeekValue[] values) throws LeekRunException {
		AbstractLeekValue[] copy = new AbstractLeekValue[values.length];
		for (int i = 0; i < values.length; i++) {
			copy[i] = LeekOperations.clonePrimitive(uai, values[i]);
		}
		return copy;
	}

	private AbstractLeekValue[] prepareValues(AbstractLeekValue[] values, int count) {
		AbstractLeekValue[] retour = new AbstractLeekValue[count];
		for (int i = 0; i < count; i++) {
			retour[i] = (i >= values.length) ? LeekValueManager.NULL : values[i].getValue();
		}
		return retour;
	}

	@Override
	public int getArgumentsCount(AI ai) throws LeekRunException {
		if (mType == LEEK_FUNCTION)
			return mFunction.getArguments();
		else if (mType == USER_FUNCTION)
			return ai.userFunctionCount(mId);
		else if (mType == ANONYMOUS_FUNCTION)
			return ai.anonymousFunctionCount(mId);
		return -1;
	}

	@Override
	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue... values) throws LeekRunException {
		if (mType == LEEK_FUNCTION) {
			return LeekFunctions.executeFunction(ai, mFunction, prepareValues(values, mFunction.getArguments()), values.length);
		}
		else if (mType == USER_FUNCTION) {
			if (values.length != ai.userFunctionCount(mId)) {
				ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { AbstractLeekValue.getParamString(values), String.valueOf(ai.userFunctionCount(mId)) });
			}
			else {
				if (ai.getVersion() >= 11) {
					return ai.userFunctionExecute(mId, copyPrimitiveValues(ai, values));
				} else {
					return ai.userFunctionExecute(mId, copyValues(ai, values, ai.userFunctionReference(mId)));
				}
			}
		}
		else if (mType == ANONYMOUS_FUNCTION) {
			if (values.length != ai.anonymousFunctionCount(mId)) {
				ai.addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { AbstractLeekValue.getParamString(values), String.valueOf(ai.anonymousFunctionCount(mId)) });
			} else {
				if (ai.getVersion() >= 11) {
					return mAnonymous.run(ai, null, copyPrimitiveValues(ai, values));
				} else {
					return mAnonymous.run(ai, null, copyValues(ai, values, ai.anonymousFunctionReference(mId)));
				}
			}
		} else if (mType == METHOD) {
			return mAnonymous.run(ai, values[0], Arrays.copyOfRange(copyPrimitiveValues(ai, values), 1, values.length));
		}
		return LeekValueManager.NULL;
	}

	@Override
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

	public AbstractLeekValue cloneFunction() {
		if (mType == LEEK_FUNCTION)
			return new FunctionLeekValue(mFunction);
		else if (mType == USER_FUNCTION)
			return new FunctionLeekValue(mId);
		else
			return new FunctionLeekValue(mId, mAnonymous);
	}

	@Override
	public Object toJSON(AI ai) {
		return "<function>";
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}
}
