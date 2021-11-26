package test;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class TestAI extends AI {

	public TestAI() throws Exception {
		super(0, 2);
	}

	@Override
	protected String getAIString() {
		return null;
	}

	@Override
	public Object runIA() throws LeekRunException {
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
	public Object userFunctionExecute(int id, Object[] value) throws LeekRunException {
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
		return 2;
	}

	@Override
	protected String[] getErrorFiles() {
		return null;
	}
}
