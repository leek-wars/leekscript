package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public abstract class FunctionLeekValue<T> {

	protected int mParametersCount = -1;
	protected String name;

	public FunctionLeekValue(int parametersCount) {
		this(parametersCount, "#Anonymous Function");
	}

	public FunctionLeekValue(int parametersCount, String name) {
		mParametersCount = parametersCount;
		this.name = name;
	}

	public int getArgumentsCount() {
		return mParametersCount;
	}

	public abstract T run(AI ai, Object thiz, Object... values) throws LeekRunException;

	public Object toJSON(AI ai) {
		return "<function>";
	}

	public String toString() {
		return this.name;
	}

	public String getString(AI ai) {
		return this.name;
	}
}
