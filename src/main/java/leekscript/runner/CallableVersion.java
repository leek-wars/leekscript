package leekscript.runner;

import java.util.Arrays;

import leekscript.common.Type;

public class CallableVersion {

	public Type return_type;
	public Type[] arguments;
	public LeekFunctions function;
	public Type type;
	private final boolean[] nullKept;
	private final boolean generic;

	public CallableVersion(Type return_type) {
		this(return_type, new Type[0]);
	}

	public CallableVersion(Type return_type, Type[] arguments) {
		this.return_type = return_type;
		this.arguments = arguments;
		this.type = Type.function(return_type, arguments);
		this.nullKept = new boolean[arguments.length];
		boolean g = return_type.containsTypeVariable();
		if (!g) for (var a : arguments) if (a.containsTypeVariable()) { g = true; break; }
		this.generic = g;
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

	/**
	 * True if this signature mentions any type variable (generic version). Generic
	 * versions trigger type-variable inference + substitution at the call site
	 * ({@code LeekFunctionCall.verifyVersions}).
	 */
	public boolean isGeneric() {
		return generic;
	}

	/**
	 * Type de la signature, variables de type effacées vers leur borne (any) : celui que voit un
	 * programme sans génériques (v1-4), identique à l'ancienne signature non générique.
	 */
	public Type getErasedType() {
		if (!generic) return type;
		var variables = new java.util.HashSet<leekscript.common.TypeVariable>();
		collectTypeVariables(type, variables);
		var bindings = new java.util.HashMap<leekscript.common.TypeVariable, Type>();
		for (var variable : variables) bindings.put(variable, Type.ANY);
		return type.substitute(bindings);
	}

	/** Variables de type mentionnées dans `type` (éléments, clés, signatures, unions). */
	public static void collectTypeVariables(Type type, java.util.Set<leekscript.common.TypeVariable> out) {
		if (type instanceof leekscript.common.TypeVariable variable) {
			out.add(variable);
		} else if (type instanceof leekscript.common.ArrayType || type instanceof leekscript.common.SetType || type instanceof leekscript.common.IntervalType) {
			collectTypeVariables(type.element(), out);
		} else if (type instanceof leekscript.common.MapType) {
			collectTypeVariables(type.key(), out);
			collectTypeVariables(type.element(), out);
		} else if (type instanceof leekscript.common.FunctionType function) {
			collectTypeVariables(function.returnType(), out);
			for (var a : function.getArguments()) collectTypeVariables(a, out);
		} else if (type instanceof leekscript.common.CompoundType compound) {
			for (var t : compound.getTypes()) collectTypeVariables(t, out);
		}
	}
}
