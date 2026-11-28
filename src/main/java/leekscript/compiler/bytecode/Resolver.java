package leekscript.compiler.bytecode;

import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Résolution des surcharges comme javac (JLS 15.12.2) sur les classes du runtime, par
 * réflexion : phase 1 sans boxing, phase 2 avec, phase 3 en arité variable, puis la méthode
 * la plus spécifique. Les génériques sont lus par leur effacement ; le type de retour `T`
 * d'une méthode générique est déduit des arguments déclarés `T`. Un cas que cette
 * approximation ne tranche pas sûrement lève {@link Unsupported} : javac reprend la main.
 */
public final class Resolver {

	private Resolver() {}

	/** Une méthode choisie, la phase qui l'a retenue et le type statique de son résultat. */
	public record Resolved(Executable executable, boolean varargs, Class<?> returnType) {

		public Class<?>[] parameters() {
			return executable.getParameterTypes();
		}

		/** Type effacé laissé sur la pile par l'appel. */
		public Class<?> erasedReturn() {
			return executable instanceof Method m ? m.getReturnType() : executable.getDeclaringClass();
		}

		/** Type du paramètre qui reçoit l'argument `i` (le composant du tableau en arité variable). */
		public Class<?> parameterFor(int i) {
			var p = parameters();
			if (varargs && i >= p.length - 1) return p[p.length - 1].getComponentType();
			return p[i];
		}
	}

	private record Key(Class<?> owner, String name, List<Class<?>> args, List<Class<?>> classLiterals, boolean statics) {}

	private static final ConcurrentHashMap<Key, Resolved> cache = new ConcurrentHashMap<>();

	/** `classLiterals` : pour chaque argument `X.class`, X (null sinon, ou liste null) ; il déduit un `T` déclaré `Class<T>`. */
	public static Resolved method(Class<?> owner, String name, List<Class<?>> args, List<Class<?>> classLiterals, boolean statics) {
		var key = new Key(owner, name, List.copyOf(args), classLiterals, statics);
		// Les talons d'une compilation ne vont pas dans les caches : ils retiendraient leur chargeur
		boolean cacheable = cacheable(owner, args);
		var r = cacheable ? cache.get(key) : null;
		if (r == null) {
			var candidates = new ArrayList<Executable>();
			for (var m : methods(owner).getOrDefault(name, List.of())) {
				// Un appel sans receveur voit aussi les méthodes statiques
				if (!statics || Modifier.isStatic(m.getModifiers())) candidates.add(m);
			}
			r = select(candidates, args, classLiterals, owner + "." + name);
			if (cacheable) cache.put(key, r);
		}
		return r;
	}

	public static Resolved constructor(Class<?> owner, List<Class<?>> args) {
		var key = new Key(owner, "<init>", List.copyOf(args), null, false);
		boolean cacheable = cacheable(owner, args);
		var r = cacheable ? cache.get(key) : null;
		if (r == null) {
			var candidates = new ArrayList<Executable>();
			for (var c : owner.getDeclaredConstructors()) {
				if (Modifier.isPublic(c.getModifiers()) || Modifier.isProtected(c.getModifiers())) candidates.add(c);
			}
			r = select(candidates, args, null, owner + ".<init>");
			if (cacheable) cache.put(key, r);
		}
		return r;
	}

	/** La classe (ou ses parents) a-t-elle une méthode accessible de ce nom ? */
	public static boolean hasMethod(Class<?> owner, String name) {
		return methods(owner).containsKey(name);
	}

	/** Méthodes accessibles depuis une classe générée hors package : publiques et protégées, sans les ponts. */
	private static boolean cacheable(Class<?> owner, List<Class<?>> args) {
		if (ClassStubs.isStub(owner)) return false;
		for (var a : args) if (!a.isPrimitive() && ClassStubs.isStub(a)) return false;
		return true;
	}

	// Par classe, dans la classe elle-même (ClassValue) : un talon emporte son entrée quand il est ramassé
	private static final ClassValue<HashMap<String, List<Method>>> METHODS = new ClassValue<>() {
		@Override
		protected HashMap<String, List<Method>> computeValue(Class<?> type) {
			return collectMethods(type);
		}
	};

	private static HashMap<String, List<Method>> methods(Class<?> owner) {
		return METHODS.get(owner);
	}

	private static HashMap<String, List<Method>> collectMethods(Class<?> owner) {
		{
			var c = owner;
			var byName = new HashMap<String, List<Method>>();
			var signatures = new java.util.HashSet<String>();
			var all = new ArrayList<Method>();
			for (var k = c; k != null; k = k.getSuperclass()) {
				for (var m : k.getDeclaredMethods()) {
					int mod = m.getModifiers();
					if (m.isBridge() || m.isSynthetic()) continue;
					// Les méthodes privées d'une classe générée ne s'appellent que depuis son nid
					if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod) && !(Modifier.isPrivate(mod) && ClassStubs.isStub(k))) continue;
					all.add(m);
				}
			}
			for (var m : c.getMethods()) {
				if (!m.isBridge() && !m.isSynthetic()) all.add(m);
			}
			for (var m : all) {
				// La déclaration la plus dérivée d'une signature masque les autres
				var sig = m.getName() + java.util.Arrays.toString(m.getParameterTypes());
				if (signatures.add(sig)) byName.computeIfAbsent(m.getName(), n -> new ArrayList<>()).add(m);
			}
			return byName;
		}
	}

	private static Resolved select(List<Executable> candidates, List<Class<?>> args, List<Class<?>> classLiterals, String what) {
		for (int phase = 1; phase <= 3; phase++) {
			var applicable = new ArrayList<Executable>();
			for (var m : candidates) {
				if (applicable(m, args, phase)) applicable.add(m);
			}
			if (applicable.isEmpty()) continue;
			var best = mostSpecific(applicable, args, phase == 3);
			if (best == null) throw new Unsupported("surcharge ambiguë " + what + args);
			return new Resolved(best, phase == 3, returnType(best, args, classLiterals, phase == 3));
		}
		throw new Unsupported("aucune méthode " + what + args);
	}

	private static boolean applicable(Executable m, List<Class<?>> args, int phase) {
		var params = m.getParameterTypes();
		if (phase < 3) {
			if (params.length != args.size()) return false;
			for (int i = 0; i < params.length; i++) {
				if (!(phase == 1 ? JTypes.strict(args.get(i), params[i]) : JTypes.loose(args.get(i), params[i]))) return false;
			}
			return true;
		}
		if (!m.isVarArgs() || args.size() < params.length - 1) return false;
		for (int i = 0; i < args.size(); i++) {
			var p = i < params.length - 1 ? params[i] : params[params.length - 1].getComponentType();
			if (!JTypes.loose(args.get(i), p)) return false;
		}
		return true;
	}

	private static Class<?> param(Executable m, int i, boolean varargs) {
		var params = m.getParameterTypes();
		if (varargs && i >= params.length - 1) return params[params.length - 1].getComponentType();
		return params[i];
	}

	/** m1 au moins aussi spécifique que m2 (JLS 15.12.2.5, sur les types effacés). */
	private static boolean moreSpecific(Executable m1, Executable m2, int n, boolean varargs) {
		int count = varargs ? Math.max(n, Math.max(m1.getParameterCount(), m2.getParameterCount())) : n;
		for (int i = 0; i < count; i++) {
			if (!JTypes.subtype(param(m1, i, varargs), param(m2, i, varargs))) return false;
		}
		return true;
	}

	private static Executable mostSpecific(List<Executable> applicable, List<Class<?>> args, boolean varargs) {
		Executable best = null;
		for (var m : applicable) {
			boolean max = true;
			for (var o : applicable) {
				if (o != m && !moreSpecific(m, o, args.size(), varargs)) { max = false; break; }
			}
			if (max) {
				if (best != null) return null;
				best = m;
			}
		}
		return best;
	}

	/** Type statique du résultat : le retour effacé, ou pour un `T` de la méthode, le type des arguments déclarés `T`. */
	private static Class<?> returnType(Executable e, List<Class<?>> args, List<Class<?>> classLiterals, boolean varargs) {
		if (!(e instanceof Method m)) return e.getDeclaringClass();
		Type generic = m.getGenericReturnType();
		if (generic instanceof Class<?> c) return c;
		if (generic instanceof ParameterizedType p) return (Class<?>) p.getRawType();
		// (equals : la réflexion rend une copie de la méthode, jamais la même instance)
		if (generic instanceof TypeVariable<?> tv && tv.getGenericDeclaration().equals(m)) {
			var params = m.getGenericParameterTypes();
			Class<?> inferred = null;
			for (int i = 0; i < args.size(); i++) {
				int pi = Math.min(i, params.length - 1);
				Type p = params[pi];
				if (varargs && pi == params.length - 1 && p instanceof GenericArrayType ga) p = ga.getGenericComponentType();
				Class<?> t;
				if (p instanceof ParameterizedType pt && pt.getRawType() == Class.class && pt.getActualTypeArguments()[0].equals(tv)
						&& classLiterals != null && classLiterals.get(i) != null) {
					t = classLiterals.get(i);
				} else if (p.equals(tv) && !JTypes.isNull(args.get(i))) {
					t = JTypes.box(args.get(i));
				} else {
					continue;
				}
				if (inferred == null || JTypes.subtypeRef(inferred, t)) inferred = t;
				else if (!JTypes.subtypeRef(t, inferred)) throw new Unsupported("inférence de " + tv + " dans " + m);
			}
			return inferred != null ? inferred : m.getReturnType();
		}
		return m.getReturnType();
	}
}
