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
	public int getVersion() {
		return 2;
	}

	@Override
	protected String[] getErrorFiles() {
		return null;
	}
}
