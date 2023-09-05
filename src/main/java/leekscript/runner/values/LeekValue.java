package leekscript.runner.values;

import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.AI.NativeObjectLeekValue;
import leekscript.runner.LeekRunException;

public interface LeekValue {


	public String string(AI ai, Set<Object> visited) throws LeekRunException;

}
