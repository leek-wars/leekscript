package leekscript.compiler.bytecode;

import java.lang.constant.ClassDesc;
import java.util.Map;

/** Types Java du backend bytecode : conversions du JLS (chapitre 5) sur les classes du runtime. */
public final class JTypes {

	private JTypes() {}

	/** Type du littéral `null`. */
	public static final class Null {
		private Null() {}
	}

	private static final Map<Class<?>, Class<?>> BOXES = Map.of(
		long.class, Long.class, int.class, Integer.class, double.class, Double.class,
		boolean.class, Boolean.class, float.class, Float.class, short.class, Short.class,
		byte.class, Byte.class, char.class, Character.class);

	private static final Map<Class<?>, Class<?>> UNBOXES = Map.of(
		Long.class, long.class, Integer.class, int.class, Double.class, double.class,
		Boolean.class, boolean.class, Float.class, float.class, Short.class, short.class,
		Byte.class, byte.class, Character.class, char.class);

	private static final String[] PACKAGES = { "java.lang.", "leekscript.runner.", "leekscript.runner.values.",
		"leekscript.runner.classes.", "leekscript.common." };

	public static boolean isNull(Class<?> c) {
		return c == Null.class;
	}

	public static boolean isReference(Class<?> c) {
		return !c.isPrimitive();
	}

	/** Le type boxé d'un primitif, le type lui-même sinon. */
	public static Class<?> box(Class<?> c) {
		var b = BOXES.get(c);
		return b != null ? b : c;
	}

	/** Le primitif d'un type boxé (Long → long), null si ce n'en est pas un. */
	public static Class<?> unbox(Class<?> c) {
		return UNBOXES.get(c);
	}

	/** Le primitif d'un type numérique, boxé ou non ; null si le type n'est pas numérique. */
	public static Class<?> numeric(Class<?> c) {
		var p = c.isPrimitive() ? c : unbox(c);
		if (p == null || p == boolean.class || p == void.class) return null;
		return p;
	}

	private static int rank(Class<?> p) {
		if (p == byte.class) return 1;
		if (p == short.class || p == char.class) return 2;
		if (p == int.class) return 3;
		if (p == long.class) return 4;
		if (p == float.class) return 5;
		if (p == double.class) return 6;
		return -1;
	}

	/** Conversion primitive élargissante (JLS 5.1.2), identité comprise. */
	public static boolean widensPrimitive(Class<?> from, Class<?> to) {
		if (from == to) return true;
		if (from == boolean.class || to == boolean.class) return false;
		if (to == char.class) return false;
		if (from == char.class) return rank(to) >= 3;
		int a = rank(from), b = rank(to);
		return a > 0 && b > 0 && a < b;
	}

	/** Sous-typage de références (JLS 4.10.2), null compris. */
	public static boolean subtypeRef(Class<?> from, Class<?> to) {
		if (to.isPrimitive()) return false;
		if (isNull(from)) return true;
		if (from.isPrimitive()) return false;
		return to.isAssignableFrom(from);
	}

	/** Sous-typage pour le choix de la méthode la plus spécifique : primitifs (JLS 4.10.1) et références. */
	public static boolean subtype(Class<?> a, Class<?> b) {
		if (a.isPrimitive() && b.isPrimitive()) return widensPrimitive(a, b);
		return subtypeRef(a, b);
	}

	/** Contexte d'invocation strict : identité, élargissement primitif ou de référence. */
	public static boolean strict(Class<?> from, Class<?> to) {
		if (from.isPrimitive() && to.isPrimitive()) return widensPrimitive(from, to);
		return subtypeRef(from, to);
	}

	/** Contexte d'invocation souple : strict, ou boxing puis élargissement, ou unboxing puis élargissement. */
	public static boolean loose(Class<?> from, Class<?> to) {
		if (strict(from, to)) return true;
		if (from.isPrimitive() && !to.isPrimitive()) return from != void.class && subtypeRef(box(from), to);
		if (!from.isPrimitive() && to.isPrimitive()) {
			var u = unbox(from);
			return u != null && widensPrimitive(u, to);
		}
		return false;
	}

	/** Promotion numérique binaire (JLS 5.6.2) ; null si un des opérandes n'est pas numérique. */
	public static Class<?> promote(Class<?> a, Class<?> b) {
		var pa = numeric(a);
		var pb = numeric(b);
		if (pa == null || pb == null) return null;
		if (pa == double.class || pb == double.class) return double.class;
		if (pa == float.class || pb == float.class) return float.class;
		if (pa == long.class || pb == long.class) return long.class;
		return int.class;
	}

	/** Promotion numérique unaire (JLS 5.6.1). */
	public static Class<?> promote(Class<?> a) {
		var p = numeric(a);
		if (p == null) return null;
		return rank(p) < 3 ? int.class : p;
	}

	public static ClassDesc desc(Class<?> c) {
		if (isNull(c)) return ClassDesc.of("java.lang.Object");
		return c.describeConstable().orElseThrow(() -> new Unsupported("type sans descripteur " + c));
	}

	// Noms de types déjà résolus : chaque essai raté de Class.forName lève une exception
	private static final java.util.concurrent.ConcurrentHashMap<String, Class<?>> resolved = new java.util.concurrent.ConcurrentHashMap<>();

	/** La classe d'un nom de type écrit par JavaWriter (`long`, `Object`, `ArrayLeekValue`…). */
	public static Class<?> forName(String name) {
		var cached = resolved.get(name);
		if (cached != null) return cached;
		var c = lookup(name);
		resolved.put(name, c);
		return c;
	}

	private static Class<?> lookup(String name) {
		switch (name) {
			case "long": return long.class;
			case "int": return int.class;
			case "double": return double.class;
			case "boolean": return boolean.class;
			case "void": return void.class;
		}
		if (name.contains("<") || name.contains("[")) throw new Unsupported("type " + name);
		var extra = leekscript.runner.LeekFunctions.getExtraFunctionsImport();
		if (extra != null && extra.endsWith(".*") && !name.contains(".")) {
			try {
				return Class.forName(extra.substring(0, extra.length() - 1) + name, false, JTypes.class.getClassLoader());
			} catch (ClassNotFoundException e) {}
		}
		for (var p : PACKAGES) {
			try {
				return Class.forName(name.contains(".") ? name : p + name, false, JTypes.class.getClassLoader());
			} catch (ClassNotFoundException e) {
				if (name.contains(".")) break;
			}
		}
		throw new Unsupported("type inconnu " + name);
	}

	/**
	 * La valeur d'un littéral chaîne Java tel que JavaWriter l'écrit (guillemets compris) :
	 * les seuls échappements qu'il produit sont décodés, et une chaîne que javac refuserait
	 * (fin de ligne brute, plus de 65 535 octets en UTF-8 modifié) lève {@link Unsupported}.
	 */
	public static String javaStringValue(String literal) {
		if (literal.length() < 2 || literal.charAt(0) != '"' || literal.charAt(literal.length() - 1) != '"') throw new Unsupported("littéral " + literal);
		var sb = new StringBuilder(literal.length());
		for (int i = 1; i < literal.length() - 1; i++) {
			char c = literal.charAt(i);
			if (c == '\\') {
				char n = literal.charAt(++i);
				switch (n) {
					case 'n' -> sb.append('\n');
					case 't' -> sb.append('\t');
					case '"' -> sb.append('"');
					case '\\' -> sb.append('\\');
					default -> throw new Unsupported("échappement \\" + n);
				}
			} else if (c == '\n' || c == '\r') {
				throw new Unsupported("fin de ligne dans un littéral");
			} else {
				sb.append(c);
			}
		}
		var value = sb.toString();
		long utf8 = 0;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			utf8 += c != 0 && c < 0x80 ? 1 : c < 0x800 ? 2 : 3;
		}
		if (utf8 > 65535) throw new Unsupported("chaîne de plus de 65 535 octets");
		return value;
	}
}
