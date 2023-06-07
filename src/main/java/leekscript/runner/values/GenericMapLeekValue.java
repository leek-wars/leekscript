package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public interface GenericMapLeekValue {

	public void set(AI ai, Object key, Object value) throws LeekRunException;

	public int size();

	public Object get(Object key) throws LeekRunException;
}
