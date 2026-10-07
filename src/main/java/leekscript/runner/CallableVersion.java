package leekscript.runner;

import java.util.Arrays;

import leekscript.common.Type;

public class CallableVersion {

	public Type return_type;
	public Type[] arguments;
	public LeekFunctions function;
	public Type type;
	private final boolean[] nullKept;

	public CallableVersion(Type return_type) {
		this(return_type, new Type[0]);
	}

	public CallableVersion(Type return_type, Type[] arguments) {
		this.return_type = return_type;
		this.arguments = arguments;
		this.type = Type.function(return_type, arguments);
		this.nullKept = new boolean[arguments.length];
	}

	/**
	 * L'argument garde son type pour l'analyse, mais le wrapper générique (argument any, LS1-3,
	 * fonction en valeur) passe la valeur brute, null compris, au lieu de la convertir (null vaudrait
	 * 0 pour un integer) : la méthode Java doit avoir une surcharge Object à cette position.
	 */
	public CallableVersion keepNull(int argument) {
		nullKept[argument] = true;
		return this;
	}

	public boolean keepsNull(int argument) {
		return nullKept[argument];
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

	public Type getType() {
		return type;
	}
}
