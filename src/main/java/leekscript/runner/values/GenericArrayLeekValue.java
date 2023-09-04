package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public interface GenericArrayLeekValue extends LeekValue {

	public Object push(AI ai, Object value) throws LeekRunException;

	public Object pushNoClone(AI ai, Object value) throws LeekRunException;

	public int size();

	public Object get(int i) throws LeekRunException;

}
