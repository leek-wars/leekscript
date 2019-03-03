package leekscript.runner;

import leekscript.runner.values.AbstractLeekValue;

public interface ILeekFunction {
	abstract public int getArguments();
	abstract public int getArgumentsMin();
	abstract public boolean isExtra();
	abstract public String getNamespace();
	abstract public int[] parameters();
	abstract public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception;
	abstract public void addOperations(AI leekIA, ILeekFunction function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception;
}
