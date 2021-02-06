package leekscript.runner;

import leekscript.runner.values.AbstractLeekValue;

public interface LeekAnonymousFunction {

	public AbstractLeekValue run(AI ai, AbstractLeekValue... values) throws LeekRunException;
}
