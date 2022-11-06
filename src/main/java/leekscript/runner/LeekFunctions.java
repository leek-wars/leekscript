package leekscript.runner;

import java.util.HashMap;
import java.util.Map;

import leekscript.common.Type;
import leekscript.compiler.LeekScript;

public class LeekFunctions {

	private static HashMap<String, LeekFunctions> functions = new HashMap<>();

	static {
		/**
		 * Fonctions Value
		 */
		method("string", "Value", 8, true, Type.STRING, new Type[] { Type.ANY });
		method("number", "Value", 10, true, Type.NUMBER, new Type[] { Type.ANY });
		method("typeOf", "Value", 8, true, Type.INT, new Type[] { Type.ANY });
		method("clone", "Value", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.INT }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("unknown", "Value", 100, true, Type.ANY, new Type[] { Type.ANY });

		/**
		 * Fonctions Number (mathématiques)
		 */
		method("abs", "Number", 2, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("min", "Number", 2, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
		});
		method("max", "Number", 2, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
		});
		method("cos", "Number", 30, true, Type.REAL, new Type[] { Type.REAL });
		method("sin", "Number", 30, true, Type.REAL, new Type[] { Type.REAL });
		method("tan", "Number", 30, true, Type.REAL, new Type[] { Type.REAL });
		method("acos", "Number", 30, true, Type.REAL, new Type[] { Type.REAL });
		method("asin", "Number", 30, true, Type.REAL, new Type[] { Type.REAL });
		method("atan", "Number", 30, true, Type.REAL, new Type[] { Type.REAL });
		method("atan2", "Number", 35, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("toRadians", "Number", 5, true, Type.REAL, new Type[] { Type.REAL });
		method("toDegrees", "Number", 5, true, Type.REAL, new Type[] { Type.REAL });
		method("ceil", "Number", 2, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("floor", "Number", 2, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("round", "Number", 2, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("sqrt", "Number", 8, true, Type.REAL, new Type[] { Type.REAL });
		method("cbrt", "Number", 62, true, Type.REAL, new Type[] { Type.REAL });
		method("log", "Number", 39, true, Type.REAL, new Type[] { Type.REAL });
		method("log2", "Number", 23, true, Type.REAL, new Type[] { Type.REAL });
		method("log10", "Number", 23, true, Type.REAL, new Type[] { Type.REAL });
		method("exp", "Number", 40, true, Type.REAL, new Type[] { Type.REAL });
		method("pow", "Number", 140, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("rand", "Number", 30, true, Type.REAL, new Type[0]);
		method("randInt", "Number", 30, true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("randFloat", "Number", 30, true, Type.REAL, new Type[] { Type.REAL, Type.REAL }).setMaxVersion(3, "randReal");
		method("randReal", "Number", 30, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("hypot", "Number", 187, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("signum", "Number", 2, true, Type.INT, new Type[] { Type.REAL });
		method("bitCount", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("trailingZeros", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("leadingZeros", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("bitReverse", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("byteReverse", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("rotateLeft", "Number", 1, true, Type.INT, new Type[] { Type.INT, Type.INT }).setMinVersion(4);
		method("rotateRight", "Number", 1, true, Type.INT, new Type[] { Type.INT, Type.INT }).setMinVersion(4);
		method("binString", "Number", 10, true, Type.STRING, new Type[] { Type.INT }).setMinVersion(4);
		method("hexString", "Number", 10, true, Type.STRING, new Type[] { Type.INT }).setMinVersion(4);
		method("realBits", "Number", 1, true, Type.INT, new Type[] { Type.REAL }).setMinVersion(4);
		method("bitsToReal", "Number", 1, true, Type.REAL, new Type[] { Type.INT }).setMinVersion(4);
		method("isFinite", "Number", 1, true, Type.BOOL, new Type[] { Type.REAL }).setMinVersion(4);
		method("isInfinite", "Number", 1, true, Type.BOOL, new Type[] { Type.REAL }).setMinVersion(4);
		method("isNaN", "Number", 1, true, Type.BOOL, new Type[] { Type.REAL }).setMinVersion(4);
		method("isPermutation", "Number", 50, true, Type.BOOL, new Type[] { Type.INT, Type.INT }).setMinVersion(4);

		/**
		 * Fonctions String
		 */
		method("charAt", "String", 8, true, Type.STRING, new Type[] { Type.STRING, Type.INT });
		method("length", "String", 15, true, Type.INT, new Type[] { Type.STRING });
		method("substring", "String", true, new CallableVersion[] {
			new CallableVersion(Type.STRING, new Type[] { Type.STRING, Type.INT, Type.INT }),
			new CallableVersion(Type.STRING, new Type[] { Type.STRING, Type.INT }),
		});
		method("replace", "String", true, Type.STRING, new Type[] { Type.STRING, Type.STRING, Type.STRING });
		method("indexOf", "String", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.STRING, Type.STRING, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.STRING, Type.STRING }),
		});
		method("split", "String", true, new CallableVersion[] {
			new CallableVersion(Type.ARRAY_STRING, new Type[] { Type.STRING, Type.STRING, Type.INT }),
			new CallableVersion(Type.ARRAY_STRING, new Type[] { Type.STRING, Type.STRING }),
		});
		method("toLower", "String", true, Type.STRING, new Type[] { Type.STRING });
		method("toUpper", "String", true, Type.STRING, new Type[] { Type.STRING });
		method("startsWith", "String", true, Type.BOOL, new Type[] { Type.STRING, Type.STRING });
		method("endsWith", "String", true, Type.BOOL, new Type[] { Type.STRING, Type.STRING });
		method("contains", "String", true, Type.BOOL, new Type[] { Type.STRING, Type.STRING });
		method("trim", "String", 10, true, Type.STRING, new Type[] { Type.STRING });
		method("codePointAt", "String", 5, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.STRING, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.STRING }),
		});

		/**
		 * Fonctions array
		 */
		method("remove", "Array", Type.ANY, new Type[] { Type.ARRAY, Type.INT });
		method("arrayRemoveAll", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }).setMinVersion(4);
		method("count", "Array", 1, Type.INT, new Type[] { Type.ARRAY });
		method("join", "Array", Type.STRING, new Type[] { Type.ARRAY, Type.STRING });
		method("insert", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY, Type.INT });
		method("push", "Array", 2, Type.VOID, new Type[] { Type.ARRAY, Type.ANY });
		method("unshift", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY });
		method("shift", "Array", Type.ANY, new Type[] { Type.ARRAY });
		method("pop", "Array", 2, Type.ANY, new Type[] { Type.ARRAY });
		method("removeElement", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY });
		method("sort", "Array", new CallableVersion[] {
			new CallableVersion(Type.VOID, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.VOID, new Type[] { Type.ARRAY }),
		});
		method("shuffle", "Array", Type.VOID, new Type[] { Type.ARRAY });
		method("search", "Array", new CallableVersion[] {
			new CallableVersion(Type.INT_OR_NULL, new Type[] { Type.ARRAY, Type.ANY, Type.INT }), // Return int | null because of V3
			new CallableVersion(Type.INT_OR_NULL, new Type[] { Type.ARRAY, Type.ANY}),
		});
		method("inArray", "Array", Type.BOOL, new Type[] { Type.ARRAY, Type.ANY });
		method("reverse", "Array", Type.VOID, new Type[] { Type.ARRAY });
		method("arrayMin", "Array", Type.ANY, new Type[] { Type.ARRAY });
		method("arrayMax", "Array", Type.ANY, new Type[] { Type.ARRAY });
		method("sum", "Array", Type.REAL, new Type[] { Type.ARRAY });
		method("average", "Array", Type.REAL, new Type[] { Type.ARRAY });
		method("fill", "Array", new CallableVersion[] {
			new CallableVersion(Type.VOID, 	new Type[] { Type.ARRAY, Type.ANY, Type.INT }),
			new CallableVersion(Type.VOID, 	new Type[] { Type.ARRAY, Type.ANY }),
		});
		method("isEmpty", "Array", 2, Type.BOOL, new Type[] { Type.ARRAY });
		method("subArray", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.INT, Type.INT }).setMaxVersion(3, "arraySlice");
		method("arraySlice", "Array", new CallableVersion[] {
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.ANY, Type.ANY, Type.INT }),
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.ANY }),
		}).setMinVersion(4);
		method("pushAll", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ARRAY });
		method("assocReverse", "Array", Type.VOID, new Type[] { Type.ARRAY }).setMaxVersion(3);
		method("arrayMap", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION });
		method("arrayFilter", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION });
		method("arrayFlatten", "Array", new CallableVersion[] {
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY }),
		});
		method("arrayFoldLeft", "Array", Type.ANY, new Type[] { Type.ARRAY, Type.FUNCTION, Type.ANY });
		method("arrayFoldRight", "Array", Type.ANY, new Type[] { Type.ARRAY, Type.FUNCTION, Type.ANY });
		method("arrayPartition", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION });
		method("arrayIter", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.FUNCTION });
		method("arrayConcat", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.ARRAY });
		method("arraySort", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION });
		method("arraySome", "Array", Type.BOOL, new Type[] { Type.ARRAY, Type.FUNCTION }).setMinVersion(4);
		method("arrayEvery", "Array", Type.BOOL, new Type[] { Type.ARRAY, Type.FUNCTION }).setMinVersion(4);
		method("arrayGet", "Array", 1, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.INT, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.INT }),
		}).setMinVersion(4);
		method("arrayRandom", "Array", Type.ANY, new Type[] { Type.ARRAY, Type.INT }).setMinVersion(4);
		method("arrayFrequencies", "Array", Type.MAP, new Type[] { Type.ARRAY }).setMinVersion(4);
		method("arrayChunk", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }).setMinVersion(4);
		method("arrayUnique", "Array", Type.ARRAY, new Type[] { Type.ARRAY }).setMinVersion(4);
		method("arrayClear", "Array", 1, Type.ARRAY, new Type[] { Type.ARRAY }).setMinVersion(4);

		/**
		 * Map functions
		 */
		method("mapSize", "Map", 1, Type.INT, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapIsEmpty", "Map", 2, Type.BOOL, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapClear", "Map", 1, Type.MAP, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapGet", "Map", 2, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.MAP, Type.ANY }),
		}).setMinVersion(4);
		method("mapValues", "Map", Type.ARRAY, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapKeys", "Map", Type.ARRAY, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapIter", "Map", Type.VOID, new Type[] { Type.MAP, Type.FUNCTION }).setMinVersion(4);
		method("mapMap", "Map", Type.MAP, new Type[] { Type.MAP, Type.FUNCTION }).setMinVersion(4);
		method("mapSum", "Map", Type.REAL, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapAverage", "Map", Type.REAL, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapMin", "Map", Type.ANY, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapMax", "Map", Type.ANY, new Type[] { Type.MAP }).setMinVersion(4);
		method("mapSearch", "Map", Type.ANY, new Type[] { Type.MAP, Type.ANY }).setMinVersion(4);
		method("mapContains", "Map", Type.BOOL, new Type[] { Type.MAP, Type.ANY }).setMinVersion(4);
		method("mapContainsKey", "Map", 2, Type.BOOL, new Type[] { Type.MAP, Type.ANY }).setMinVersion(4);
		method("mapRemove", "Map", 2, Type.ANY, new Type[] { Type.MAP, Type.ANY }).setMinVersion(4);
		method("mapRemoveAll", "Map", Type.VOID, new Type[] { Type.MAP, Type.ANY }).setMinVersion(4);
		method("mapReplace", "Map", 3, Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }).setMinVersion(4);
		method("mapReplaceAll", "Map", Type.VOID, new Type[] { Type.MAP, Type.MAP }).setMinVersion(4);
		method("mapFill", "Map", Type.VOID, new Type[] { Type.MAP, Type.ANY }).setMinVersion(4);
		method("mapEvery", "Map", Type.BOOL, new Type[] { Type.MAP, Type.FUNCTION }).setMinVersion(4);
		method("mapSome", "Map", Type.BOOL, new Type[] { Type.MAP, Type.FUNCTION }).setMinVersion(4);
		method("mapFold", "Map", Type.ANY, new Type[] { Type.MAP, Type.FUNCTION, Type.ANY }).setMinVersion(4);
		method("mapFilter", "Map", Type.ANY,  new Type[] { Type.MAP, Type.FUNCTION }).setMinVersion(4);
		method("mapMerge", "Map", Type.ANY, new Type[] { Type.MAP, Type.MAP }).setMinVersion(4);
		method("mapPut", "Map", 3, Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }).setMinVersion(4);
		method("mapPutAll", "Map", Type.VOID, new Type[] { Type.MAP, Type.MAP }).setMinVersion(4);
		method("assocSort", "Map", new CallableVersion[] {
			new CallableVersion(Type.VOID, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.VOID, new Type[] { Type.ARRAY }),
		}).setMaxVersion(3);
		method("keySort", "Map", new CallableVersion[] {
			new CallableVersion(Type.VOID, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.VOID, new Type[] { Type.ARRAY }),
		}).setMaxVersion(3);
		method("removeKey", "Map", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }).setMaxVersion(3, "mapRemove");

		/**
		 * JSON functions
		 */
		method("jsonEncode", "JSON", true, Type.STRING, new Type[] { Type.ANY });
		method("jsonDecode", "JSON", true, Type.ANY, new Type[] { Type.STRING });

		/**
		 * Color functions
		 */
		method("color", "Color", 7, true, Type.INT, new Type[] { Type.INT, Type.INT, Type.INT }).setMaxVersion(3, "getColor");
		method("getColor", "Color", 7, true, Type.INT, new Type[] { Type.INT, Type.INT, Type.INT });
		method("getRed", "Color", 2, true, Type.INT, new Type[] { Type.INT });
		method("getGreen", "Color", 2, true, Type.INT, new Type[] { Type.INT });
		method("getBlue", "Color", 1, true, Type.INT, new Type[] { Type.INT });

		/**
		 * System functions
		 */
		method("getOperations", "System", 1, true, Type.INT, new Type[0]);
		method("getMaxOperations", "System", 1, true, Type.INT, new Type[0]);
		method("getInstructionsCount", "System", 1, true, Type.INT, new Type[0]);
		method("debug", "System", 100, true, Type.VOID, new Type[] { Type.ANY });
		method("debugW", "System", 100, true, Type.VOID, new Type[] { Type.ANY });
		method("debugE", "System", 100, true, Type.VOID, new Type[] { Type.ANY });
		method("debugC", "System", 100, true, Type.VOID, new Type[] { Type.ANY, Type.INT });
		method("getUsedRAM", "System", 1, true, Type.INT, new Type[0]);
		method("getMaxRAM", "System", 1, true, Type.INT, new Type[0]);
	}

	private static LeekFunctions method(String name, String clazz, Type return_type, Type[] arguments) {
		return method(name, clazz, 0, false, return_type, arguments);
	}
	private static LeekFunctions method(String name, String clazz, boolean isStatic, Type return_type, Type[] arguments) {
		return method(name, clazz, 0, isStatic, return_type, arguments);
	}
	private static LeekFunctions method(String name, String clazz, int operations, boolean isStatic, Type return_type, Type[] arguments) {
		return method(name, clazz, operations, isStatic, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}
	private static LeekFunctions method(String name, String clazz, int operations, Type return_type, Type[] arguments) {
		return method(name, clazz, operations, false, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}
	private static LeekFunctions method(String name, String clazz, CallableVersion[] versions) {
		return method(name, clazz, 0, false, versions);
	}
	private static LeekFunctions method(String name, String clazz, boolean isStatic, CallableVersion[] versions) {
		return method(name, clazz, 0, isStatic, versions);
	}
	private static LeekFunctions method(String name, String clazz, int operations, CallableVersion[] versions) {
		return method(name, clazz, operations, false, versions);
	}
	private static LeekFunctions method(String name, String clazz, int operations, boolean isStatic, CallableVersion[] versions) {
		var function = new LeekFunctions(clazz, name, operations, isStatic, versions);
		functions.put(name, function);
		return function;
	}

	private static Map<String, LeekFunctions> extraFunctions = null;

	private static String extraFunctionsImport;

	private final String name;
	private int mArguments = Integer.MIN_VALUE;
	private int mArgumentsMin = Integer.MAX_VALUE;
	private int mOperations = 0;
	private Type return_type;
	private CallableVersion[] versions;
	private boolean direct = false;
	private String standardClass = null;
	private boolean isStatic = false;
	private int minVersion = 1;
	private int maxVersion = LeekScript.LATEST_VERSION;
	private String replacement = null;

	public LeekFunctions(String standardClass, String name, int operations, boolean isStatic, Type return_type, Type[] arguments) {
		this(standardClass, name, operations, isStatic, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}

	public LeekFunctions(String standardClass, String name, int operations, boolean isStatic, CallableVersion[] versions) {
		this.name = name;
		this.versions = versions;
		this.direct = true;
		this.mOperations = operations;
		this.isStatic = isStatic;
		this.standardClass = standardClass;
		for (var version : versions) {
			version.function = this;
			if (version.arguments.length < mArgumentsMin) mArgumentsMin = version.arguments.length;
			if (version.arguments.length > mArguments) mArguments = version.arguments.length;
		}
	}

	public boolean isDirect() {
		return direct;
	}

	public Type getReturnType() {
		return return_type;
	}

	public CallableVersion[] getVersions() {
		return versions;
	}

	public static void setExtraFunctions(Map<String, LeekFunctions> extraFunctions, String extraFunctionsImport) {
		LeekFunctions.extraFunctions = extraFunctions;
		LeekFunctions.extraFunctionsImport = extraFunctionsImport;
	}

	public static boolean isExtraFunction(String name) {
		var f = getValue(name);
		return f != null && f.isExtra();
	}

	// public static String getNamespace(String name) {
	// 	return isExtraFunction(name) ? extraFunctions : "LeekFunctions";
	// }

	public int getArguments() {
		return mArguments;
	}

	public String getNamespace() {
		return "LeekFunctions";
	}

	public int getArgumentsMin() {
		return mArgumentsMin;
	}

	public boolean isExtra() {
		return false;
	}

	public int getOperations() {
		return mOperations;
	}

	public String getStandardClass() {
		return standardClass;
	}

	public static LeekFunctions getValue(String name) {
		var f = functions.get(name);
		if (f != null) return f;
		if (extraFunctions != null) {
			return extraFunctions.get(name);
		}
		return null;
	}

	/*
	 * Lancer la fonction
	 */
	public Object run(AI ai, LeekFunctions function, Object... parameters) throws LeekRunException {
		return null;
	}

	public int cost() {
		return 1;
	}

	public int getMinVersion() {
		return this.minVersion;
	}

	public void setMinVersion(int minVersion) {
		this.minVersion = minVersion;
	}

	public int getMaxVersion() {
		return maxVersion;
	}

	public void setMaxVersion(int max_version) {
		this.maxVersion = max_version;
	}

	public void setMaxVersion(int max_version, String replacement) {
		this.maxVersion = max_version;
		this.replacement = replacement;
	}

	public void setOperations(int operations) {
		mOperations = operations;
	}

	public void addOperations(AI leekIA, LeekFunctions function, Object parameters[], Object retour) throws LeekRunException {
		leekIA.ops(getOperations());
	}

	public boolean isStatic() {
		return isStatic;
	}
	public String getName() {
		return name;
	}
	public static String getExtraFunctionsImport() {
		return extraFunctionsImport;
	}
	public String getReplacement() {
		return replacement;
	}
}
