package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public interface GenericArrayLeekValue {

	public Object push(AI ai, Object value) throws LeekRunException;

	public Object pushNoClone(AI ai, Object value) throws LeekRunException;

	public int size();

	public Object get(AI ai, int i) throws LeekRunException;

}
