package leekscript.runner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import leekscript.common.FunctionType;
import leekscript.common.Type;
import leekscript.common.TypeVariable;
import leekscript.compiler.LeekScript;

public class LeekFunctions {

	private static HashMap<String, LeekFunctions> functions = new HashMap<>();
	// standardClass -> nom de méthode -> fonction (méthodes appelables en `valeur.méthode()`).
	// Déclaré AVANT le bloc static d'enregistrement (ordre d'init statique).
	private static final Map<String, Map<String, LeekFunctions>> methodsByClass = new HashMap<>();

	static {
		/**
		 * Fonctions Value
		 */
		method("string", "Value", 6, true, Type.STRING, new Type[] { Type.ANY });
		method("number", "Value", 3, true, Type.INT_OR_REAL, new Type[] { Type.ANY });
		method("typeOf", "Value", 1, true, Type.INT, new Type[] { Type.ANY });
		method("clone", "Value", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.INT }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("unknown", "Value", 100, true, Type.ANY, new Type[] { Type.ANY });

		/**
		 * Fonctions Number (mathématiques)
		 */
		// Ordre : la version REAL sert de fallback (conversion totale via real()),
		// INT et BIG_INT sont sélectionnées par instanceof dans le dispatcher générique.
		method("abs", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT }),
		});
		method("min", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT, Type.BIG_INT }),
		});
		method("max", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT, Type.BIG_INT }),
		});
		method("cos", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("sin", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("tan", "Number", 4, true, Type.REAL, new Type[] { Type.REAL });
		method("acos", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("asin", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("atan", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("atan2", "Number", 4, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("toRadians", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("toDegrees", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("ceil", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("floor", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("round", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("sqrt", "Number", 3, true, Type.REAL, new Type[] { Type.REAL });
		method("cbrt", "Number", 5, true, Type.REAL, new Type[] { Type.REAL });
		method("log", "Number", 4, true, Type.REAL, new Type[] { Type.REAL });
		method("log2", "Number", 5, true, Type.REAL, new Type[] { Type.REAL });
		method("log10", "Number", 5, true, Type.REAL, new Type[] { Type.REAL });
		method("exp", "Number", 2, true, Type.REAL, new Type[] { Type.REAL });
		method("pow", "Number", 7, true, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL, Type.REAL }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT, Type.BIG_INT }),
		});
		method("rand", "Number", 3, true, Type.REAL, new Type[0]);
		method("randInt", "Number", 2, true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("randFloat", "Number", 3, true, Type.REAL, new Type[] { Type.REAL, Type.REAL }).setMaxVersion(3, "randReal");
		method("randReal", "Number", 3, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("hypot", "Number", 2, true, Type.REAL, new Type[] { Type.REAL, Type.REAL });
		method("signum", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.REAL }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.BIG_INT }),
		});
		method("bitCount", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.BIG_INT }),
		}).setMinVersion(4);
		method("trailingZeros", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.BIG_INT }),
		}).setMinVersion(4);
		method("bitLength", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.BIG_INT }),
		}).setMinVersion(4);
		method("setBit", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT, Type.BOOL }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT, Type.INT }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT, Type.INT, Type.INT }),
			new CallableVersion(Type.BIG_INT, new Type[] { Type.BIG_INT, Type.INT, Type.BOOL }),
		}).setMinVersion(4);
		method("testBit", "Number", 1, true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.BOOL, new Type[] { Type.BIG_INT, Type.INT }),
		}).setMinVersion(4);
		method("leadingZeros", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("bitReverse", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("byteReverse", "Number", 1, true, Type.INT, new Type[] { Type.INT }).setMinVersion(4);
		method("rotateLeft", "Number", 1, true, Type.INT, new Type[] { Type.INT, Type.INT }).setMinVersion(4);
		method("rotateRight", "Number", 1, true, Type.INT, new Type[] { Type.INT, Type.INT }).setMinVersion(4);
		method("binString", "Number", 9, true, new CallableVersion[] {
			new CallableVersion(Type.STRING, new Type[] { Type.INT }),
			new CallableVersion(Type.STRING, new Type[] { Type.BIG_INT }),
		}).setMinVersion(4);
		method("hexString", "Number", 6, true, new CallableVersion[] {
			new CallableVersion(Type.STRING, new Type[] { Type.INT }),
			new CallableVersion(Type.STRING, new Type[] { Type.BIG_INT }),
		}).setMinVersion(4);
		method("toFixed", "Number", 40, true, new CallableVersion[] {
			new CallableVersion(Type.STRING, new Type[] { Type.REAL, Type.INT }),
			new CallableVersion(Type.STRING, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.STRING, new Type[] { Type.BIG_INT, Type.INT }),
		});
		method("realBits", "Number", 1, true, Type.INT, new Type[] { Type.REAL }).setMinVersion(4);
		method("bitsToReal", "Number", 3, true, Type.REAL, new Type[] { Type.INT }).setMinVersion(4);
		method("isFinite", "Number", 1, true, Type.BOOL, new Type[] { Type.REAL }).setMinVersion(4);
		method("isInfinite", "Number", 1, true, Type.BOOL, new Type[] { Type.REAL }).setMinVersion(4);
		method("isNaN", "Number", 1, true, Type.BOOL, new Type[] { Type.REAL }).setMinVersion(4);
		method("isPermutation", "Number", 10, true, Type.BOOL, new Type[] { Type.INT, Type.INT }).setMinVersion(4);

		/**
		 * Fonctions String
		 */
		method("charAt", "String", 4, true, Type.STRING, new Type[] { Type.STRING, Type.INT });
		method("length", "String", 1, true, Type.INT, new Type[] { Type.STRING });
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
		method("trim", "String", 5, true, Type.STRING, new Type[] { Type.STRING });
		method("codePointAt", "String", 1, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.STRING, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.STRING }),
		});
		// Sans receveur (elle fabrique une chaîne) : en v5, la forme qui la remplace est
		// `String.fromCodePoint(x)`, pas une méthode `s.fromCodePoint()`.
		method("fromCodePoint", "String", 6, true, Type.STRING, new Type[] { Type.INT }).setReplacement("String.fromCodePoint");

		/**
		 * Fonctions array
		 */
		method("remove", "Array", Type.ANY, new Type[] { Type.ARRAY, Type.INT });
		method("arrayRemoveAll", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ANY }).alias("removeAll").setMinVersion(4);
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
			new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.ANY, Type.INT }), // Return int | null because of V3
			new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.ANY}),
		});
		method("inArray", "Array", Type.BOOL, new Type[] { Type.ARRAY, Type.ANY });
		method("reverse", "Array", Type.VOID, new Type[] { Type.ARRAY });
		{ var t = new TypeVariable("T"); method("arrayMin", "Array", t, new Type[] { Type.array(t) }).alias("min"); }
		{ var t = new TypeVariable("T"); method("arrayMax", "Array", t, new Type[] { Type.array(t) }).alias("max"); }
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
		}).alias("slice").setMinVersion(4);
		method("pushAll", "Array", Type.VOID, new Type[] { Type.ARRAY, Type.ARRAY });
		method("assocReverse", "Array", Type.VOID, new Type[] { Type.ARRAY }).setMaxVersion(3);
		// arrayMap(Array, (any, int, Array) => R) -> Array<R> : R se lie depuis le retour
		// du callback. Les params du callback restent `any` : LeekScript ne type pas
		// contextuellement les lambdas, et y substituer un type le rendrait incompatible
		// avec un lambda non typé (params vérifiés de façon covariante).
		{ var r = new TypeVariable("R"); method("arrayMap", "Array", Type.array(r), new Type[] { Type.ARRAY, new FunctionType(r, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("map"); }
		// arrayFilter(Array<T>, (any, int, Array) => bool) -> Array<T> : type d'élément préservé.
		{ var t = new TypeVariable("T"); method("arrayFilter", "Array", Type.array(t), new Type[] { Type.array(t), new FunctionType(Type.BOOL, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("filter"); }
		method("arrayFind", "Array", Type.ANY, new Type[] { Type.ARRAY, new FunctionType(Type.BOOL, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("find").setMinVersion(4);
		method("arrayFlatten", "Array", new CallableVersion[] {
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY }),
		}).alias("flatten");
		method("arrayFoldLeft", "Array", Type.ANY, new Type[] { Type.ARRAY, new FunctionType(Type.ANY, 0, Type.ANY, Type.ANY, Type.INT, Type.ARRAY), Type.ANY }).alias("foldLeft");
		method("arrayFoldRight", "Array", Type.ANY, new Type[] { Type.ARRAY, new FunctionType(Type.ANY, 0, Type.ANY, Type.ANY, Type.INT, Type.ARRAY), Type.ANY }).alias("foldRight");
		method("arrayPartition", "Array", Type.ARRAY, new Type[] { Type.ARRAY, new FunctionType(Type.BOOL, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("partition");
		method("arrayIter", "Array", Type.VOID, new Type[] { Type.ARRAY, new FunctionType(Type.ANY, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("iter");
		method("arrayConcat", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.ARRAY }).alias("concat");
		// `sorted` et non `sort` : contrairement à `sort()` qui trie EN PLACE, arraySort renvoie
		// un NOUVEAU tableau. Même opposition que sorted()/sort() en Python et toSorted()/sort()
		// en JS. Sans cet alias, arraySort serait la seule fonction Array sans forme OO propre,
		// et son message de suppression v5 pointerait vers `arraySort()` — un nom qui n'a rien
		// de canonique.
		method("arraySort", "Array", new CallableVersion[] {
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, new FunctionType(Type.INT_OR_REAL, Type.ANY, Type.ANY) }),
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY })
		}).alias("sorted");
		method("arraySome", "Array", Type.BOOL, new Type[] { Type.ARRAY, new FunctionType(Type.BOOL, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("some").setMinVersion(4);
		method("arrayEvery", "Array", Type.BOOL, new Type[] { Type.ARRAY, new FunctionType(Type.BOOL, 0, Type.ANY, Type.INT, Type.ARRAY) }).alias("every").setMinVersion(4);
		method("arrayGet", "Array", 1, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.INT, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ARRAY, Type.INT }),
		}).alias("get").setMinVersion(4);
		method("arrayRandom", "Array", new CallableVersion[] {
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.ARRAY, new Type[] { Type.ARRAY }),
		}).alias("random").setMinVersion(4);
		method("arrayFrequencies", "Array", Type.MAP, new Type[] { Type.ARRAY }).alias("frequencies").setMinVersion(4);
		method("arrayChunk", "Array", Type.ARRAY, new Type[] { Type.ARRAY, Type.INT }).alias("chunk").setMinVersion(4);
		{ var t = new TypeVariable("T"); method("arrayUnique", "Array", Type.array(t), new Type[] { Type.array(t) }).alias("unique").setMinVersion(4); }
		{ var t = new TypeVariable("T"); method("arrayRotate", "Array", Type.array(t), new Type[] { Type.array(t), Type.INT }).alias("rotate").setMinVersion(4); }
		method("arrayClear", "Array", 1, Type.ARRAY, new Type[] { Type.ARRAY }).alias("clear").setMinVersion(4);
		method("arrayToSet", "Array", Type.SET, new Type[] { Type.ARRAY }).alias("toSet");

		/**
		 * Map functions
		 */
		method("mapSize", "Map", 1, Type.INT, new Type[] { Type.MAP }).alias("size").setMinVersion(4);
		method("mapIsEmpty", "Map", 2, Type.BOOL, new Type[] { Type.MAP }).alias("isEmpty").setMinVersion(4);
		method("mapClear", "Map", 1, Type.MAP, new Type[] { Type.MAP }).alias("clear").setMinVersion(4);
		method("mapGet", "Map", 2, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.MAP, Type.ANY }),
		}).alias("get").setMinVersion(4);
		method("mapValues", "Map", Type.ARRAY, new Type[] { Type.MAP }).alias("values").setMinVersion(4);
		method("mapKeys", "Map", Type.ARRAY, new Type[] { Type.MAP }).alias("keys").setMinVersion(4);
		method("mapKeySet", "Map", Type.SET, new Type[] { Type.MAP }).alias("keySet").setMinVersion(4);
		method("mapIter", "Map", Type.VOID, new Type[] { Type.MAP, new FunctionType(Type.ANY, 0, Type.ANY, Type.ANY, Type.MAP) }).alias("iter").setMinVersion(4);
		method("mapMap", "Map", Type.MAP, new Type[] { Type.MAP, new FunctionType(Type.ANY, 0, Type.ANY, Type.ANY, Type.MAP) }).alias("map").setMinVersion(4);
		method("mapSum", "Map", Type.REAL, new Type[] { Type.MAP }).alias("sum").setMinVersion(4);
		method("mapAverage", "Map", Type.REAL, new Type[] { Type.MAP }).alias("average").setMinVersion(4);
		method("mapMin", "Map", Type.ANY, new Type[] { Type.MAP }).alias("min").setMinVersion(4);
		method("mapMax", "Map", Type.ANY, new Type[] { Type.MAP }).alias("max").setMinVersion(4);
		method("mapSearch", "Map", Type.ANY, new Type[] { Type.MAP, Type.ANY }).alias("search").setMinVersion(4);
		method("mapContains", "Map", Type.BOOL, new Type[] { Type.MAP, Type.ANY }).alias("contains").setMinVersion(4);
		method("mapContainsKey", "Map", 1, Type.BOOL, new Type[] { Type.MAP, Type.ANY }).alias("containsKey").setMinVersion(4);
		method("mapRemove", "Map", 2, Type.ANY, new Type[] { Type.MAP, Type.ANY }).alias("remove").setMinVersion(4);
		method("mapRemoveAll", "Map", Type.VOID, new Type[] { Type.MAP, Type.ANY }).alias("removeAll").setMinVersion(4);
		method("mapReplace", "Map", 3, Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }).alias("replace").setMinVersion(4);
		method("mapReplaceAll", "Map", Type.VOID, new Type[] { Type.MAP, Type.MAP }).alias("replaceAll").setMinVersion(4);
		method("mapFill", "Map", Type.VOID, new Type[] { Type.MAP, Type.ANY }).alias("fill").setMinVersion(4);
		method("mapEvery", "Map", Type.BOOL, new Type[] { Type.MAP, new FunctionType(Type.ANY, 1, Type.ANY, Type.ANY, Type.MAP) }).alias("every").setMinVersion(4);
		method("mapSome", "Map", Type.BOOL, new Type[] { Type.MAP, new FunctionType(Type.ANY, 1, Type.ANY, Type.ANY, Type.MAP) }).alias("some").setMinVersion(4);
		method("mapFold", "Map", Type.ANY, new Type[] { Type.MAP, new FunctionType(Type.ANY, 2, Type.ANY, Type.ANY, Type.ANY, Type.MAP), Type.ANY }).alias("fold").setMinVersion(4);
		method("mapFilter", "Map", Type.MAP, new Type[] { Type.MAP, new FunctionType(Type.BOOL, 0, Type.ANY, Type.ANY, Type.MAP) }).alias("filter").setMinVersion(4);
		method("mapMerge", "Map", Type.ANY, new Type[] { Type.MAP, Type.MAP }).alias("merge").setMinVersion(4);
		method("mapPut", "Map", 3, Type.ANY, new Type[] { Type.MAP, Type.ANY, Type.ANY }).alias("put").setMinVersion(4);
		method("mapPutAll", "Map", Type.VOID, new Type[] { Type.MAP, Type.MAP }).alias("putAll").setMinVersion(4);
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
		 * Set functions
		 */
		method("setPut", "Set", 3, Type.BOOL, new Type[] { Type.SET, Type.ANY }).alias("put");
		method("setPutAll", "Set", Type.VOID, new Type[] { Type.SET, Type.SET }).alias("putAll");
		method("setRemove", "Set", 2, Type.BOOL, new Type[] { Type.SET, Type.ANY }).alias("remove");
		method("setClear", "Set", 1, Type.SET, new Type[] { Type.SET }).alias("clear");
		// 1 op comme l'opérateur `in`, qui fait exactement la même recherche par hachage (#2613)
		method("setContains", "Set", 1, Type.BOOL, new Type[] { Type.SET, Type.ANY }).alias("contains");
		method("setSize", "Set", 1, Type.INT, new Type[] { Type.SET }).alias("size");
		// Lecture de champ : ne peut pas coûter plus que setContains, qui hache et compare
		method("setIsEmpty", "Set", 1, Type.BOOL, new Type[] { Type.SET }).alias("isEmpty");
		method("setIsSubsetOf", "Set", Type.BOOL, new Type[] { Type.SET, Type.SET }).alias("isSubsetOf");
		method("setUnion", "Set", Type.SET, new Type[] { Type.SET, Type.SET }).alias("union");
		method("setIntersection", "Set", Type.SET, new Type[] { Type.SET, Type.SET }).alias("intersection");
		method("setDifference", "Set", Type.SET, new Type[] { Type.SET, Type.SET }).alias("difference");
		method("setDisjunction", "Set", Type.SET, new Type[] { Type.SET, Type.SET }).alias("disjunction");
		method("setFilter", "Set", Type.SET, new Type[] { Type.SET, new FunctionType(Type.BOOL, 0, Type.ANY, Type.SET) }).alias("filter");
		method("setToArray", "Set", Type.ARRAY, new Type[] { Type.SET }).alias("toArray");
		// Helpers de réduction + conversions (#24), alignés sur Map (gated v4 pour éviter toute collision avec d'anciennes IAs).
		method("setSum", "Set", Type.REAL, new Type[] { Type.SET }).alias("sum").setMinVersion(4);
		method("setAverage", "Set", Type.REAL, new Type[] { Type.SET }).alias("average").setMinVersion(4);
		method("setMin", "Set", Type.ANY, new Type[] { Type.SET }).alias("min").setMinVersion(4);
		method("setMax", "Set", Type.ANY, new Type[] { Type.SET }).alias("max").setMinVersion(4);
		method("setEvery", "Set", Type.BOOL, new Type[] { Type.SET, new FunctionType(Type.BOOL, 0, Type.ANY, Type.SET) }).alias("every").setMinVersion(4);
		method("setSome", "Set", Type.BOOL, new Type[] { Type.SET, new FunctionType(Type.BOOL, 0, Type.ANY, Type.SET) }).alias("some").setMinVersion(4);
		method("setFold", "Set", Type.ANY, new Type[] { Type.SET, new FunctionType(Type.ANY, 0, Type.ANY, Type.ANY, Type.SET), Type.ANY }).alias("fold").setMinVersion(4);
		method("setIter", "Set", Type.VOID, new Type[] { Type.SET, new FunctionType(Type.ANY, 0, Type.ANY, Type.SET) }).alias("iter").setMinVersion(4);
		method("setToMap", "Set", Type.MAP, new Type[] { Type.SET }).alias("toMap").setMinVersion(4);

		/**
		 * Interval functions
		 */
		// Alias OO v5 (interval.min(), interval.size()...) : forme méthode cohérente avec Array/Map/Set/String/Number.
		// Le dispatch méthode est gardé v5 ; les fonctions libres intervalXxx restent disponibles à leurs versions.
		method("intervalMin", "Interval", 1, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL_INTERVAL}),
			new CallableVersion(Type.INT, new Type[] { Type.INTEGER_INTERVAL }),
		}).alias("min");
		method("intervalMax", "Interval", 1, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL_INTERVAL}),
			new CallableVersion(Type.INT, new Type[] { Type.INTEGER_INTERVAL }),
		}).alias("max");
		// Pas d'alias OO `size` : intervalSize renvoie `from - to` (span négatif, bug de signe
		// préexistant non testé) — on ne l'expose pas via l'API méthode tant qu'il n'est pas corrigé.
		method("intervalSize", "Interval", 1, new CallableVersion[] {
			new CallableVersion(Type.REAL, new Type[] { Type.REAL_INTERVAL}),
			new CallableVersion(Type.INT, new Type[] { Type.INTEGER_INTERVAL }),
		});
		method("intervalIsEmpty", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isEmpty");
		method("intervalIsBounded", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isBounded");
		method("intervalIsRightBounded", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isRightBounded");
		method("intervalIsLeftBounded", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isLeftBounded");
		method("intervalIsClosed", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isClosed");
		method("intervalIsRightClosed", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isRightClosed");
		method("intervalIsLeftClosed", "Interval", 1, Type.BOOL, new Type[] { Type.INTERVAL }).alias("isLeftClosed");
		method("intervalContains", "Interval", 2, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.INTERVAL, Type.INT }),
			new CallableVersion(Type.BOOL, new Type[] { Type.INTERVAL, Type.REAL }),
			new CallableVersion(Type.BOOL, new Type[] { Type.INTERVAL, Type.INT_OR_REAL }),
		}).alias("contains");
		method("intervalAverage", "Interval", 3, Type.REAL, new Type[] { Type.INTERVAL }).alias("average");
		method("intervalIntersection", "Interval", 3, new CallableVersion[] {
			new CallableVersion(Type.REAL_INTERVAL, new Type[] { Type.REAL_INTERVAL, Type.REAL_INTERVAL }),
			new CallableVersion(Type.REAL_INTERVAL, new Type[] { Type.INTEGER_INTERVAL, Type.REAL_INTERVAL }),
			new CallableVersion(Type.REAL_INTERVAL, new Type[] { Type.REAL_INTERVAL, Type.INTEGER_INTERVAL }),
			new CallableVersion(Type.INTEGER_INTERVAL, new Type[] { Type.INTEGER_INTERVAL, Type.INTEGER_INTERVAL }),
		}).alias("intersection");
		method("intervalCombine", "Interval", 3, new CallableVersion[] {
			new CallableVersion(Type.REAL_INTERVAL, new Type[] { Type.REAL_INTERVAL, Type.REAL_INTERVAL }),
			new CallableVersion(Type.REAL_INTERVAL, new Type[] { Type.INTEGER_INTERVAL, Type.REAL_INTERVAL }),
			new CallableVersion(Type.REAL_INTERVAL, new Type[] { Type.REAL_INTERVAL, Type.INTEGER_INTERVAL }),
			new CallableVersion(Type.INTEGER_INTERVAL, new Type[] { Type.INTEGER_INTERVAL, Type.INTEGER_INTERVAL }),
		}).alias("combine");
		method("intervalToArray", "Interval", new CallableVersion[] {
			new CallableVersion(Type.ARRAY_REAL, new Type[] { Type.REAL_INTERVAL, Type.REAL }),
			new CallableVersion(Type.ARRAY_REAL, new Type[] { Type.REAL_INTERVAL}),
			new CallableVersion(Type.ARRAY_REAL, new Type[] { Type.INTEGER_INTERVAL, Type.REAL }),
			new CallableVersion(Type.ARRAY_INT, new Type[] { Type.INTEGER_INTERVAL, Type.INT }),
			new CallableVersion(Type.ARRAY_INT, new Type[] { Type.INTEGER_INTERVAL}),
		}).alias("toArray");
		method("intervalToSet", "Interval", new CallableVersion[] {
			new CallableVersion(Type.SET_REAL, new Type[] { Type.REAL_INTERVAL, Type.REAL }),
			new CallableVersion(Type.SET_REAL, new Type[] { Type.REAL_INTERVAL}),
			new CallableVersion(Type.SET_REAL, new Type[] { Type.INTEGER_INTERVAL, Type.REAL }),
			new CallableVersion(Type.SET_INT, new Type[] { Type.INTEGER_INTERVAL, Type.INT }),
			new CallableVersion(Type.SET_INT, new Type[] { Type.INTEGER_INTERVAL}),
		}).alias("toSet");

		/**
		 * JSON functions
		 */
		method("jsonEncode", "JSON", true, Type.STRING, new Type[] { Type.ANY }).alias("encode");
		method("jsonDecode", "JSON", true, Type.ANY, new Type[] { Type.STRING }).alias("decode");

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
		method("getDate", "System", 50, true, Type.STRING, new Type[0]);
		method("getTime", "System", 50, true, Type.STRING, new Type[0]);
		method("getTimestamp", "System", 5, true, Type.INT, new Type[0]);
		method("fileRead", "System", 1, true, Type.STRING, new Type[] { Type.STRING }).setFileArgument();
		method("getSourceLine", "System", 1, true, Type.INT, new Type[0]).setCallSite(CallSite.LINE);
		method("getSourceFile", "System", 1, true, Type.STRING, new Type[0]).setCallSite(CallSite.FILE);

		// v5, OO stdlib : les fonctions à receveur (Array/Map/Set/String) n'ont plus de
		// forme libre en v5. `arrayMap(a, f)` / `push(a, x)` / `length(s)` -> erreur dure
		// (REMOVED_FUNCTION) pointant vers la forme méthode `a.map(f)` / `a.push(x)` /
		// `s.length()`. La forme méthode passe par resolveBuiltinMethod (ne vérifie pas
		// maxVersion), donc capper la forme libre à v4 ne la casse pas. Value/JSON/System/
		// Color gardent leur forme libre.
		for (var f : functions.values()) {
			if (("Array".equals(f.standardClass) || "Map".equals(f.standardClass)
					|| "Set".equals(f.standardClass) || "String".equals(f.standardClass))
					&& f.maxVersion >= 5) {
				f.maxVersion = 4;
				f.ooDemoted = true;
				if (f.replacement == null) {
					f.replacement = (f.methodName != null ? f.methodName : f.name) + "()";
				}
			} else if ("Number".equals(f.standardClass) && f.minVersion < 4 && f.maxVersion >= 5) {
				// Fonctions MATH -> erreur dure pointant vers `Math.sqrt(x)`. La distinction
				// math vs utilitaire est portée par minVersion À LA DÉCLARATION (pas une liste
				// de noms séparée à maintenir) : les fonctions math sont les historiques (v1-3),
				// les utilitaires bit/entier de Number (binString, bitLength, isNaN, bitReverse,
				// leadingZeros...) ont été ajoutés en v4 (minVersion>=4) et restent libres.
				// La forme namespace `Math.f` reste valide (gardée par !is_namespace_function).
				f.maxVersion = 4;
				f.ooDemoted = true;
				if (f.replacement == null) {
					f.replacement = "Math." + f.name;
				}
			}
		}
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
		// Index par type pour la résolution des méthodes `valeur.méthode()` (v5,
		// pilier OO). Permet `Array.filter` et `Map.filter` de coexister par un nom
		// propre, là où la table plate `functions` n'autorise qu'un seul `filter`.
		if (clazz != null) methodsByClass.computeIfAbsent(clazz, k -> new HashMap<>()).put(name, function);
		return function;
	}

	/** Méthode système exposée sur un type natif (ex. "Array","map"), ou null. */
	public static LeekFunctions getMethod(String standardClass, String name) {
		var byName = methodsByClass.get(standardClass);
		return byName == null ? null : byName.get(name);
	}

	/**
	 * Méthode de bibliothèque standard appelable en `valeur.méthode()` sur un type
	 * natif (Array/Map/Set, dont les méthodes sont des méthodes d'instance Java sur
	 * la value class), ou null. Source unique de vérité pour le compilateur.
	 */
	public static LeekFunctions methodFor(Type type, String name) {
		String standardClass = type instanceof leekscript.common.ArrayType ? "Array"
			: type instanceof leekscript.common.MapType ? "Map"
			: type instanceof leekscript.common.SetType ? "Set"
			: type == Type.STRING ? "String"
			: type.isNumber() ? "Number"
			: null;
		return standardClass == null ? null : getMethod(standardClass, name);
	}

	/**
	 * Classe standard sous-jacente d'un namespace built-in écrit dans le code (v5).
	 * Le namespace `Math` expose les fonctions mathématiques, qui portent la classe
	 * standard "Number" en interne (`NumberClass.sqrt(ai, x)`). Mécanisme général :
	 * d'autres namespaces pourront mapper leur nom vers une classe standard ici.
	 */
	public static String namespaceStandardClass(String namespaceName) {
		switch (namespaceName) {
			// `Math` regroupe les fonctions de la classe standard "Number".
			case "Math": return "Number";
			// Les autres namespaces portent le nom de leur classe standard.
			// `Interval` et `Value` ont leur classe interne déclarée depuis la v3 : les exposer
			// ne réserve aucun identifiant nouveau, et referme deux trous de la grille v5
			// (« plus de fonctions qui traînent seules »). `Color`, lui, n'a pas de classe
			// interne : l'exposer demanderait d'en déclarer une, cf. §2.3.
			case "Number": case "String": case "Array": case "Map":
			case "Set": case "JSON": case "System": case "Interval": case "Value":
				return namespaceName;
			default: return null;
		}
	}

	/**
	 * Fonction d'un namespace built-in appelée en forme qualifiée `Namespace.func(...)`
	 * (v5), ou null. Résolue exactement comme l'ancienne forme libre `func(...)` :
	 * `Math.sqrt(x)` == `sqrt(x)`, `Math.rand()` == `rand()`.
	 */
	public static LeekFunctions namespaceFunction(String namespaceName, String field) {
		var standardClass = namespaceStandardClass(namespaceName);
		return standardClass == null ? null : getMethod(standardClass, field);
	}

	private static Map<String, LeekFunctions> extraFunctions = null;

	private static String extraFunctionsImport;

	/**
	 * Top-level user functions that the host runtime invokes itself (e.g. lifecycle
	 * hooks called by reflection), so nothing in the script calls them. They must not
	 * be reported as unused in strict mode. Populated by the embedder, like
	 * {@link #setExtraFunctions}.
	 */
	private static java.util.Set<String> entryPointFunctions = java.util.Collections.emptySet();

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
	private boolean fileArgument = false;
	private CallSite callSite = null;
	// Nom de la méthode Java à appeler (sur la value class ou la *Class statique).
	// Découple le nom leekscript du nom Java : permet d'exposer la méthode `map`
	// (v5) tout en appelant la méthode Java existante `arrayMap`. Défaut = name.
	private String javaName = null;
	// Nom de la méthode propre équivalente, pour le message d'erreur quand la forme
	// libre est supprimée en v5. Défaut = name ; mis à jour par alias().
	private String methodName = null;
	// Vrai quand le cap `maxVersion = 4` vient de la migration OO v5 (§2.2/§2.4) et non d'un
	// retrait réel du langage. Distinction porteuse : dans le premier cas la fonction existe
	// toujours, seule sa forme LIBRE disparaît (la forme méthode/namespace est justement le
	// remplacement) ; dans le second (`subArray`, `assocSort`, `removeKey`, `randFloat`...
	// retirés en v3) elle ne doit revenir par AUCUNE forme.
	private boolean ooDemoted = false;

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

	public static void setEntryPointFunctions(java.util.Set<String> names) {
		LeekFunctions.entryPointFunctions = names == null ? java.util.Collections.emptySet() : names;
	}

	public static boolean isEntryPointFunction(String name) {
		return entryPointFunctions.contains(name);
	}

	public static boolean isExtraFunction(String name) {
		var f = getValue(name, true);
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

	public static LeekFunctions getValue(String name, boolean useExtra) {
		var f = functions.get(name);
		if (f != null) return f;
		if (useExtra && extraFunctions != null) {
			return extraFunctions.get(name);
		}
		return null;
	}

	/** Fonctions de la bibliotheque standard (hors fonctions "extra" comme le combat). */
	public static Map<String, LeekFunctions> getStandardFunctions() {
		return functions;
	}

	/*
	 * Lancer la fonction
	 */
	public Object run(AI ai, LeekFunctions function, Object... parameters) throws LeekRunException {
		return null;
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

	public LeekFunctions setReplacement(String replacement) {
		this.replacement = replacement;
		return this;
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

	/**
	 * Le 1er argument est le chemin d'un fichier du joueur, écrit en littéral : le compilateur
	 * le résout comme un include (relatif au fichier appelant) et passe son CONTENU à la fonction.
	 */
	public LeekFunctions setFileArgument() {
		this.fileArgument = true;
		return this;
	}

	public boolean hasFileArgument() {
		return fileArgument;
	}

	/** Ce que vaut l'appel, connu à la compilation à l'endroit où il est écrit : sa ligne ou son fichier. */
	public enum CallSite { LINE, FILE }

	public LeekFunctions setCallSite(CallSite callSite) {
		this.callSite = callSite;
		return this;
	}

	public CallSite getCallSite() {
		return callSite;
	}
	public String getName() {
		return name;
	}
	/** Nom de la méthode Java à appeler (défaut = nom leekscript). */
	public String getJavaName() {
		return javaName != null ? javaName : name;
	}

	/**
	 * Expose cette fonction sous un nom de méthode propre, appelable uniquement en
	 * `valeur.méthode()` (pilier OO v5) : `arrayMap` devient `map`. L'alias est une
	 * méthode-only (pas une fonction globale libre) qui appelle la même méthode Java.
	 */
	public LeekFunctions alias(String methodName) {
		// Copies propres des CallableVersion : le constructeur fait `version.function =
		// this`, donc partager le tableau écraserait le `.function` de l'original
		// (cassant la forme libre legacy). Cf. régression arrayMap/arrayFilter.
		var copyVersions = new CallableVersion[versions.length];
		for (int i = 0; i < versions.length; i++) {
			copyVersions[i] = new CallableVersion(versions[i].return_type, versions[i].arguments);
		}
		var copy = new LeekFunctions(standardClass, methodName, mOperations, isStatic, copyVersions);
		copy.javaName = this.name;
		copy.minVersion = 5;
		methodsByClass.computeIfAbsent(standardClass, k -> new HashMap<>()).put(methodName, copy);
		this.methodName = methodName; // pour le message d'erreur de suppression v5
		return this;
	}
	public static String getExtraFunctionsImport() {
		return extraFunctionsImport;
	}
	public String getReplacement() {
		return replacement;
	}

	/**
	 * Vrai si la forme libre a été retirée par la migration OO v5 (§2.2/§2.4), la fonction
	 * restant disponible en forme méthode/namespace. Faux pour un retrait réel du langage,
	 * qui doit fermer TOUTES les formes.
	 */
	public boolean isOoDemoted() {
		return ooDemoted;
	}

	/**
	 * Nom de remplacement à afficher pour une compilation en version {@code version}.
	 *
	 * Une fonction retirée en v3/v4 pointe vers le nom qui l'a remplacée à l'époque
	 * (`subArray` -> `arraySlice`), mais ce nom peut lui-même avoir perdu sa forme libre
	 * en v5 : le joueur était alors renvoyé vers un identifiant qui ne compile pas non
	 * plus. On suit la chaîne tant que la cible est elle aussi retirée à la version
	 * demandée, pour atterrir sur la forme réellement valide : `subArray` -> `slice()`
	 * en v5, mais toujours `arraySlice` en v4.
	 */
	public String getReplacement(int version) {
		var current = replacement;
		var seen = new HashSet<String>();
		while (current != null && seen.add(current)) {
			// Les formes canoniques v5 (`slice()`, `Math.abs`) ne sont pas des noms de
			// fonction libre : `functions.get` renvoie null et la chaîne s'arrête là.
			var target = functions.get(current);
			if (target == null || target == this || version <= target.maxVersion || target.replacement == null) {
				break;
			}
			current = target.replacement;
		}
		return current;
	}
}
