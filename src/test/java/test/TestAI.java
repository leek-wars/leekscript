package test;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.AbstractLeekValue;

public class TestAI extends AI {

	public TestAI() throws Exception {
		super();
	}

	@Override
	protected String[] getErrorString() {
		return null;
	}

	@Override
	protected String getAItring() {
		return null;
	}

	@Override
	public AbstractLeekValue runIA() throws LeekRunException {
		return null;
	}

	@Override
	public int userFunctionCount(int id) {
		return 0;
	}

	@Override
	public boolean[] userFunctionReference(int id) {
		return null;
	}

	@Override
	public AbstractLeekValue userFunctionExecute(int id, AbstractLeekValue[] value) throws LeekRunException {
		return null;
	}

	@Override
	public int anonymousFunctionCount(int id) {
		return 0;
	}

	@Override
	public boolean[] anonymousFunctionReference(int id) {
		return null;
	}

	@Override
	public int getVersion() {
		return 11;
	}
}
