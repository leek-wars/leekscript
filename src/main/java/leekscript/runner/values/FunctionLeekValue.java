package leekscript.runner.values;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekAnonymousFunction;
import leekscript.runner.LeekFunctions;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekValueManager;

public class FunctionLeekValue extends AbstractLeekValue {

	private final static int LEEK_FUNCTION = 1;
	private final static int USER_FUNCTION = 2;
	private final static int ANONYMOUS_FUNCTION = 3;

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
	public int getType() {
		return AbstractLeekValue.FUNCTION;
	}

	private AbstractLeekValue[] copyValues(AI uai, AbstractLeekValue[] values, boolean[] references) throws Exception {
		AbstractLeekValue[] copy = new AbstractLeekValue[values.length];
		for (int i = 0; i < values.length; i++) {
			if (!references[i])
				copy[i] = LeekOperations.clone(uai, values[i]);
			else
				copy[i] = values[i];
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
	public int getArgumentsCount(AI ai) throws Exception {
		if (mType == LEEK_FUNCTION)
			return mFunction.getArguments();
		else if (mType == USER_FUNCTION)
			return ai.userFunctionCount(mId);
		else if (mType == ANONYMOUS_FUNCTION)
			return ai.anonymousFunctionCount(mId);
		return -1;
	}

	@Override
	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue[] values) throws Exception {
		if (mType == LEEK_FUNCTION) {
			return LeekFunctions.executeFunction(ai, mFunction, prepareValues(values, mFunction.getArguments()), values.length);
		}
		else if (mType == USER_FUNCTION) {
			if (values.length != ai.userFunctionCount(mId)) {

				ai.addOperations(AI.ERROR_LOG_COST);
				ai.addSystemLog(AILog.ERROR, AILog.CAN_NOT_EXECUTE_WITH_ARGUMENTS, new String[] { AbstractLeekValue.getParamString(values), String.valueOf(ai.userFunctionCount(mId)) });
			}
			else
				return ai.userFunctionExecute(mId, copyValues(ai, values, ai.userFunctionReference(mId)));
		}
		else if (mType == ANONYMOUS_FUNCTION) {
			if (values.length != ai.anonymousFunctionCount(mId)) {

				ai.addOperations(AI.ERROR_LOG_COST);
				ai.addSystemLog(AILog.ERROR, AILog.CAN_NOT_EXECUTE_WITH_ARGUMENTS,
						new String[] { AbstractLeekValue.getParamString(values), String.valueOf(ai.anonymousFunctionCount(mId)) });
			}
			else
				return mAnonymous.run(ai, copyValues(ai, values, ai.anonymousFunctionReference(mId)));

		}
		return LeekValueManager.NULL;
	}

	@Override
	public String getString(AI ai) {
		if (mType == LEEK_FUNCTION)
			return "#Function " + mFunction;
		else if (mType == USER_FUNCTION)
			return "#User Function";
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
}
