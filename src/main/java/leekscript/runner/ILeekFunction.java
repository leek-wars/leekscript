package leekscript.runner;

import leekscript.common.Type;

public interface ILeekFunction {
	abstract public void setOperations(int operations);
	abstract public int getArguments();
	abstract public int getArgumentsMin();
	abstract public boolean isExtra();
	abstract public String getNamespace();
	abstract public Type getReturnType();
	abstract public CallableVersion[] getVersions();
	abstract public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException;
	abstract public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException;

	abstract public int getMinVersion();

	abstract public int getMaxVersion();
	public abstract String getStandardClass();

	public abstract int getOperations();
	public abstract boolean isStatic();
}
