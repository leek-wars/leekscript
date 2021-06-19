package leekscript.runner;

public interface LeekFunction {

	public Object run(Object... values) throws LeekRunException;
}
