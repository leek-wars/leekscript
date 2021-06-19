package leekscript.runner;

import leekscript.runner.values.ObjectLeekValue;

public interface LeekAnonymousFunction {

	public Object run(ObjectLeekValue thiz, Object... values) throws LeekRunException;
}
