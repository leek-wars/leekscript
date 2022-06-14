package leekscript.runner;

import leekscript.functions.Functions;
import leekscript.functions.VariableOperations;
import leekscript.common.Type;
import leekscript.compiler.LeekScript;

public enum LeekFunctions implements ILeekFunction {

	/**
	 * Fonctions Value
	 */
	string("Value", true, Type.STRING, new Type[] { Type.ANY }),
	number("Value", true, Type.NUMBER, new Type[] { Type.ANY }),
	typeOf("Value", true, Type.INT, new Type[] { Type.ANY }),
	clone("Value", true, new CallableVersion[] {
		new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.INT }),
		new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
	}),
	unknown("Value", true, Type.ANY, new Type[] { Type.ANY }),

	/**
	 * Fonctions Number (mathématiques)
	 */
	abs("Number", 2, true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.INT }),
		new CallableVersion(Type.REAL, new Type[] { Type.REAL })
	}),
	min("Number", 2, true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
		new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
	}),
	max("Number", true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
		new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
	}),
	cos("Number", 30, true, Type.REAL, new Type[] { Type.REAL }),
	sin("Number", 30, true, Type.REAL, new Type[] { Type.REAL }),
	tan("Number", 30, true, Type.REAL, new Type[] { Type.REAL }),
	acos("Number", 30, true, Type.REAL, new Type[] { Type.REAL }),
	asin("Number", 30, true, Type.REAL, new Type[] { Type.REAL }),
	atan("Number", 30, true, Type.REAL, new Type[] { Type.REAL }),
	atan2("Number", 35, true, Type.REAL, new Type[] { Type.REAL, Type.REAL }),
	toRadians("Number", 5, true, Type.REAL, new Type[] { Type.REAL }),
	toDegrees("Number", 5, true, Type.REAL, new Type[] { Type.REAL }),
	ceil("Number", 2, true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.REAL }),
		new CallableVersion(Type.INT, new Type[] { Type.INT }),
	}),
	floor("Number", 2, true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.REAL }),
		new CallableVersion(Type.INT, new Type[] { Type.INT }),
	}),
	round("Number", 2, true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.REAL }),
		new CallableVersion(Type.INT, new Type[] { Type.INT }),
	}),
	sqrt("Number", true, Type.REAL, new Type[] { Type.REAL }),
	cbrt("Number", true, Type.REAL, new Type[] { Type.REAL }),
	log("Number", true, Type.REAL, new Type[] { Type.REAL }),
	log2("Number", true, Type.REAL, new Type[] { Type.REAL }),
	log10("Number", true, Type.REAL, new Type[] { Type.REAL }),
	exp("Number", true, Type.REAL, new Type[] { Type.REAL }),
	pow("Number", true, Type.REAL, new Type[] { Type.REAL, Type.REAL }),
	rand("Number", true, Type.REAL, new Type[0]),
	randInt("Number", true, Type.INT, new Type[] { Type.INT, Type.INT }),
	randFloat("Number", true, Type.REAL, new Type[] { Type.REAL, Type.REAL }),
	hypot("Number", true, Type.REAL, new Type[] { Type.REAL, Type.REAL }),
	signum("Number", true, Type.INT, new Type[] { Type.REAL }),

	/**
	 * Fonctions String
	 */
	charAt("String", true, Type.STRING, new Type[] { Type.STRING, Type.INT }),
	length("String", true, Type.INT, new Type[] { Type.STRING }),
	substring("String", true, new CallableVersion[] {
		new CallableVersion(Type.STRING, new Type[] { Type.STRING, Type.INT, Type.INT }),
		new CallableVersion(Type.STRING, new Type[] { Type.STRING, Type.INT }),
	}),
	replace("String", true, Type.STRING, new Type[] { Type.STRING, Type.STRING, Type.STRING }),
	indexOf("String", true, new CallableVersion[] {
		new CallableVersion(Type.INT, new Type[] { Type.STRING, Type.STRING }),
		new CallableVersion(Type.INT, new Type[] { Type.STRING, Type.STRING, Type.INT }),
	}),
	split("String", true, new CallableVersion[] {
		new CallableVersion(Type.ARRAY, new Type[] { Type.STRING, Type.STRING }),
		new CallableVersion(Type.ARRAY, new Type[] { Type.STRING, Type.STRING, Type.INT }),
	}),
	toLower("String", true, Type.STRING, new Type[] { Type.STRING }),
	toUpper("String", true, Type.STRING, new Type[] { Type.STRING }),
	startsWith("String", true, Type.BOOL, new Type[] { Type.STRING, Type.STRING }),
	endsWith("String", true, Type.BOOL, new Type[] { Type.STRING, Type.STRING }),
	contains("String", true, Type.BOOL, new Type[] { Type.STRING, Type.STRING }),
	trim("String", true, Type.BOOL, new Type[] { Type.STRING }),

	/**
	 * Fonctions array
	 */
	remove("Array", Type.ANY, new Type[] { Type.ARRAY, Type.INT }),
	arrayRemoveAll("Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }),
	count("Array", Type.INT, new Type[] { Type.ARRAY }),
	join("Array", Type.STRING, new Type[] { Type.ARRAY, Type.STRING }),
	insert("Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY, Type.INT }),
	push("Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }),
	unshift("Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }),
	shift("Array", Type.ANY, new Type[] { Type.ARRAY }),
	pop("Array", Type.ANY, new Type[] { Type.ARRAY }),
	removeElement("Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }),
	sort("Array", new CallableVersion[] {
		new CallableVersion(Type.VOID, new Type[] { Type.ARRAY }),
		new CallableVersion(Type.VOID, new Type[] { Type.ARRAY, Type.INT }),
	}),
	shuffle("Array", Type.VOID, new Type[] { Type.ARRAY }),
	search("Array", new CallableVersion[] {
		new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.ANY}),
		new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.ANY, Type.INT }),
	}),
	inArray("Array", Type.BOOL, new Type[] { Type.ARRAY, Type.ANY }),
	reverse("Array", Type.VOID, new Type[] { Type.ARRAY }),
	arrayMin("Array", Type.ANY, new Type[] { Type.ARRAY }),
	arrayMax("Array", Type.ANY, new Type[] { Type.ARRAY }),
	sum("Array", Type.REAL, new Type[] { Type.ARRAY }),
	average("Array", Type.REAL, new Type[] { Type.ARRAY }),
	fill("Array", new CallableVersion[] {
		new CallableVersion(Type.VOID, 	new Type[] { Type.ARRAY, Type.ANY }),
		new CallableVersion(Type.VOID, 	new Type[] { Type.ARRAY, Type.ANY, Type.INT }),
	}),
	isEmpty("Array", Type.BOOL, new Type[] { Type.ARRAY }),
	subArray("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.INT, Type.INT }),
	pushAll("Array", Type.VOID, new Type[] { Type.ARRAY, Type.ARRAY }),
	assocReverse("Array", Type.VOID, new Type[] { Type.ARRAY }) {
		@Override
		public int getMaxVersion() { return 3; }
	},
	arrayMap("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arrayFilter("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arrayFlatten("Array", new CallableVersion[] {
		new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY }),
		new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }),
	}),
	arrayFoldLeft("Array", Type.ANY, new Type[] { Type.ARRAY, Type.FUNCTION, Type.ANY }),
	arrayFoldRight("Array", Type.ANY, new Type[] { Type.ARRAY, Type.FUNCTION, Type.ANY }),
	arrayPartition("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arrayIter("Array", Type.VOID, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arrayConcat("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.ARRAY }),
	arraySort("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arraySome("Array", Type.BOOL, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arrayEvery("Array", Type.BOOL, new Type[] { Type.ARRAY, Type.FUNCTION }),
	arrayGet("Array", new CallableVersion[] {
		new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.INT }),
		new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.INT, Type.ANY }),
	}) {
		@Override
		public int getMinVersion() { return 4; }
	},
	arrayRandom("Array", Type.ANY, new Type[] { Type.ARRAY, Type.INT }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	arrayFrequencies("Array", Type.MAP, new Type[] { Type.ARRAY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	arrayChunk("Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	arrayUnique("Array", Type.ARRAY, new Type[] { Type.ARRAY }) {
		@Override
		public int getMinVersion() { return 4; }
	},

	/**
	 * Map functions
	 */
	mapSize("Map", Type.INT, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapIsEmpty("Map", Type.BOOL, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapClear("Map", Type.VOID, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapGet("Map", new CallableVersion[] {
		new CallableVersion(Type.ANY, new Type[] { Type.MAP, Type.ANY }),
		new CallableVersion(Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }),
	}) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapValues("Map", Type.ARRAY, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapKeys("Map", Type.ARRAY, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapIter("Map", Type.VOID, new Type[] { Type.MAP, Type.FUNCTION }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapMap("Map", Type.MAP, new Type[] { Type.MAP, Type.FUNCTION }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapSum("Map", Type.REAL, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapAverage("Map", Type.REAL, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapMin("Map", Type.ANY, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapMax("Map", Type.ANY, new Type[] { Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapSearch("Map", Type.ANY, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapContains("Map", Type.BOOL, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapContainsKey("Map", Type.BOOL, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapRemove("Map", Type.ANY, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapRemoveElement("Map", Type.ANY, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapRemoveAll("Map", Type.VOID, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapReplace("Map", Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapReplaceAll("Map", Type.VOID, new Type[] { Type.MAP, Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapFill("Map", Type.VOID, new Type[] { Type.MAP, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapEvery("Map", Type.BOOL, new Type[] { Type.MAP, Type.FUNCTION }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapSome("Map", Type.BOOL, new Type[] { Type.MAP, Type.FUNCTION }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapFold("Map", Type.ANY, new Type[] { Type.MAP, Type.FUNCTION, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapFilter("Map", Type.ANY,  new Type[] { Type.MAP, Type.FUNCTION }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapMerge("Map", Type.ANY, new Type[] { Type.MAP, Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapPut("Map", Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	mapPutAll("Map", Type.VOID, new Type[] { Type.MAP, Type.MAP }) {
		@Override
		public int getMinVersion() { return 4; }
	},
	assocSort("Map", new CallableVersion[] {
		new CallableVersion(Type.VOID, new Type[] { Type.ARRAY }),
		new CallableVersion(Type.VOID, new Type[] { Type.ARRAY, Type.INT }),
	}) {
		@Override
		public int getMaxVersion() { return 3; }
	},
	keySort("Map", new CallableVersion[] {
		new CallableVersion(Type.VOID, new Type[] { Type.ARRAY }),
		new CallableVersion(Type.VOID, new Type[] { Type.ARRAY, Type.INT }),
	}) {
		@Override
		public int getMaxVersion() { return 3; }
	},
	removeKey("Map", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }) {
		@Override
		public int getMaxVersion() { return 3; }
	},

	/**
	 * JSON functions
	 */
	jsonEncode("JSON", true, Type.STRING, new Type[] { Type.ANY }),
	jsonDecode("JSON", true, Type.ANY, new Type[] { Type.STRING }),

	/**
	 * Color functions
	 */
	color("Color", true, Type.INT, new Type[] { Type.INT, Type.INT, Type.INT }) {
		@Override
		public int getMaxVersion() { return 3; }
	},
	getColor("Color", true, Type.INT, new Type[] { Type.INT, Type.INT, Type.INT }),
	getRed("Color", true, Type.INT, new Type[] { Type.INT }),
	getGreen("Color", true, Type.INT, new Type[] { Type.INT }),
	getBlue("Color", true, Type.INT, new Type[] { Type.INT }) ,

	/**
	 * System functions
	 */
	getOperations("System", true, Type.INT, new Type[0]),
	getInstructionsCount("System", true, Type.INT, new Type[0]),
	debug("System", true, Type.VOID, new Type[] { Type.ANY }),
	debugW("System", true, Type.VOID, new Type[] { Type.ANY }),
	debugE("System", true, Type.VOID, new Type[] { Type.ANY }),
	debugC("System", true, Type.VOID, new Type[] { Type.ANY, Type.INT }),

	;

	private static String extraFunctions = null;

	private int mArguments = Integer.MIN_VALUE;
	private int mArgumentsMin = Integer.MAX_VALUE;
	private int mOperations = 0;
	protected VariableOperations mVariableOperations = null;
	private Type return_type;
	private CallableVersion[] versions;
	private boolean direct = false;
	private String standardClass = null;
	private boolean isStatic = false;

	LeekFunctions(String standardClass, CallableVersion[] versions) {
		this(standardClass, 0, false, versions);
	}

	LeekFunctions(String standardClass, Type return_version, Type[] arguments) {
		this(standardClass, 0, false, return_version, arguments);
	}

	LeekFunctions(String standardClass, boolean isStatic, Type return_type, Type[] arguments) {
		this(standardClass, 0, isStatic, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}

	LeekFunctions(String standardClass, int operations, boolean isStatic, Type return_type, Type[] arguments) {
		this(standardClass, operations, isStatic, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}

	LeekFunctions(String standardClass, boolean isStatic, CallableVersion[] versions) {
		this(standardClass, 0, isStatic, versions);
	}

	LeekFunctions(String standardClass, int operations, boolean isStatic, CallableVersion[] versions) {
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

	public static void setExtraFunctions(String extraFunctions) {
		LeekFunctions.extraFunctions = extraFunctions;
	}

	public static int isFunction(String name) {
		ILeekFunction f = getValue(name);
		if (f == null)
			return -1;
		return f.getArguments();
	}

	public static boolean isExtraFunction(String name) {
		ILeekFunction f = getValue(name);
		return f != null && f.isExtra();
	}

	public static String getNamespace(String name) {
		return isExtraFunction(name) ? extraFunctions : "LeekFunctions";
	}

	@Override
	public int getArguments() {
		return mArguments;
	}

	@Override
	public String getNamespace() {
		return "LeekFunctions";
	}


	@Override
	public int getArgumentsMin() {
		return mArgumentsMin;
	}

	@Override
	public boolean isExtra() {
		return false;
	}

	public int getOperations() {
		return mOperations;
	}

	@Override
	public String getStandardClass() {
		return standardClass;
	}

	public boolean hasVariableOperations() {
		if (mVariableOperations == null) {
			mVariableOperations = Functions.getVariableOperations(name());
		}
		return mVariableOperations != null;
	}

	public static ILeekFunction getValue(String name) {
		try {
			return LeekFunctions.valueOf(name);
		} catch (Exception e) {}
		if (extraFunctions != null) {
			try {
				Class<?> extra = Class.forName(extraFunctions);
				for (Object func : extra.getEnumConstants()) {
					if (("" + func).equals(name)) {
						return (ILeekFunction) func;
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	public static Object[] getExtraFunctions() {
		if (extraFunctions != null) {
			try {
				Class<?> extra = Class.forName(extraFunctions);
				return extra.getEnumConstants();
			} catch (ClassNotFoundException e) {
				return new Object[] {};
			}

		}
		return new Object[] {};
	}

	/*
	 * Lancer la fonction
	 */
	public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
		return null;
	}

	public int cost() {
		return 1;
	}

	public int getMinVersion() {
		return 1;
	}

	public int getMaxVersion() {
		return LeekScript.LATEST_VERSION;
	}

	public void setOperations(int operations) {
		mOperations = operations;
	}

	public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
		leekIA.ops(getOperations());
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}
}
