package leekscript.runner;

import leekscript.common.Type;

public class CallableVersion {

	public Type return_type;
	public Type[] arguments;

	public CallableVersion(Type return_type, Type[] arguments) {
		this.return_type = return_type;
		this.arguments = arguments;
	}
}
