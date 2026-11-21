package leekscript.runner;

import leekscript.common.Error;
import leekscript.runner.classes.StandardClass;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BigIntegerValue;
import leekscript.runner.values.ClassLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.IntervalLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.SetLeekValue;

/**
 * v5 : le type déclaré d'un Array / Map / Set, vérifié sur son contenu quand une valeur any lui
 * est convertie (#5384). Une valeur convient quand le code généré peut la lire sous ce type : la
 * lecture d'une case `Array<integer>` caste en Long, un réel y échouerait donc plus loin, à la
 * lecture ; il est refusé ici, à la conversion. Un `real` accepte aussi un entier (realBox),
 * null n'est accepté que par un type qui le contient (cf #5349). Construit par le code généré
 * (JavaWriter.contentTypeCode) ; les types sans contrôle possible (any, Object, variable de type)
 * y deviennent ANY.
 */
public abstract class ContentType {

	private final String name;

	protected ContentType(String name) {
		this.name = name;
	}

	/** La valeur a-t-elle ce type ? Une opération par élément de conteneur parcouru. */
	public abstract boolean fits(AI ai, Object value) throws LeekRunException;

	/** Comme fits, mais lève IMPOSSIBLE_CAST sur l'élément fautif le plus profond. */
	public void check(AI ai, Object value) throws LeekRunException {
		if (!fits(ai, value)) throw mismatch(ai, value);
	}

	@Override
	public String toString() {
		return name;
	}

	LeekRunException mismatch(AI ai, Object value) {
		String actual;
		try {
			actual = value == null ? "null" : StandardClass.getType(ai, value).toString();
		} catch (Exception e) {
			actual = "?";
		}
		return new LeekRunException(Error.IMPOSSIBLE_CAST, new String[] { actual, name });
	}

	private static ContentType leaf(String name, Class<?> javaClass) {
		return new ContentType(name) {
			public boolean fits(AI ai, Object value) {
				return javaClass.isInstance(value);
			}
		};
	}

	public static final ContentType ANY = new ContentType("any") {
		public boolean fits(AI ai, Object value) {
			return true;
		}
	};
	public static final ContentType INT = leaf("integer", Long.class);
	public static final ContentType BIG_INT = leaf("big_integer", BigIntegerValue.class);
	public static final ContentType BOOL = leaf("boolean", Boolean.class);
	public static final ContentType STRING = leaf("string", String.class);
	public static final ContentType FUNCTION = leaf("Function", FunctionLeekValue.class);
	public static final ContentType CLASS = leaf("Class", ClassLeekValue.class);
	public static final ContentType INTERVAL = leaf("Interval", IntervalLeekValue.class);
	public static final ContentType NULL = new ContentType("null") {
		public boolean fits(AI ai, Object value) {
			return value == null;
		}
	};
	public static final ContentType REAL = new ContentType("real") {
		public boolean fits(AI ai, Object value) {
			return value instanceof Double || value instanceof Long;
		}
	};

	/** Instance d'une classe utilisateur (ou d'une sous-classe), sans ses paramètres génériques. */
	public static ContentType instance(Class<?> javaClass, String name) {
		return leaf(name, javaClass);
	}

	public static ContentType union(String name, ContentType... types) {
		return new ContentType(name) {
			public boolean fits(AI ai, Object value) throws LeekRunException {
				for (var type : types) {
					if (type.fits(ai, value)) return true;
				}
				return false;
			}
		};
	}

	public static ContentType array(ContentType element) {
		return collection("Array<" + element + ">", ArrayLeekValue.class, element);
	}

	public static ContentType set(ContentType element) {
		return collection("Set<" + element + ">", SetLeekValue.class, element);
	}

	/** Tableau ou ensemble : chaque élément est vérifié, sauf pour un élément any (rien à parcourir). */
	private static ContentType collection(String name, Class<? extends java.util.Collection<?>> javaClass, ContentType element) {
		if (element == ANY) return leaf(name, javaClass);
		return new ContentType(name) {
			public boolean fits(AI ai, Object value) throws LeekRunException {
				if (!javaClass.isInstance(value)) return false;
				var collection = (java.util.Collection<?>) value;
				ai.ops(collection.size());
				for (var e : collection) {
					if (!element.fits(ai, e)) return false;
				}
				return true;
			}
			public void check(AI ai, Object value) throws LeekRunException {
				if (!javaClass.isInstance(value)) throw mismatch(ai, value);
				var collection = (java.util.Collection<?>) value;
				ai.ops(collection.size());
				for (var e : collection) element.check(ai, e);
			}
		};
	}

	public static ContentType map(ContentType key, ContentType value) {
		if (key == ANY && value == ANY) return leaf("Map<" + key + ", " + value + ">", MapLeekValue.class);
		return new ContentType("Map<" + key + ", " + value + ">") {
			public boolean fits(AI ai, Object object) throws LeekRunException {
				if (!(object instanceof MapLeekValue map)) return false;
				ai.ops(map.size());
				for (var entry : map.entrySet()) {
					if (!key.fits(ai, entry.getKey()) || !value.fits(ai, entry.getValue())) return false;
				}
				return true;
			}
			public void check(AI ai, Object object) throws LeekRunException {
				if (!(object instanceof MapLeekValue map)) throw mismatch(ai, object);
				ai.ops(map.size());
				for (var entry : map.entrySet()) {
					key.check(ai, entry.getKey());
					value.check(ai, entry.getValue());
				}
			}
		};
	}
}
