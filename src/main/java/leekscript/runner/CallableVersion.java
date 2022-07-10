package leekscript.runner;

import java.util.Arrays;

import leekscript.common.Type;

public class CallableVersion {

	public Type return_type;
	public Type[] arguments;
	public LeekFunctions function;

	public CallableVersion(Type return_type) {
		this(return_type, new Type[0]);
	}

	public CallableVersion(Type return_type, Type[] arguments) {
		this.return_type = return_type;
		this.arguments = arguments;
	}

	public String getParametersSignature() {
		String r = "";
		for (var argument : arguments) {
			r += argument.getSignature();
		}
		return r;
	}

	public String toString() {
		return Arrays.toString(this.arguments) + " => " + return_type;
	}
}
