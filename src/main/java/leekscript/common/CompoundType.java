package leekscript.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import leekscript.compiler.Complete;

public class CompoundType extends Type {

	protected HashSet<Type> types;

	public CompoundType(HashSet<Type> types) {
		super(buildName(types), "x", "Object", "Object", "null");
		this.types = types;
	}

	public CompoundType(Type... types) {
		super(buildName(Arrays.asList(types)), "x", "Object", "Object", "null");
		this.types = new HashSet<Type>(Arrays.asList(types));
	}

	private static String buildName(Iterable<Type> types) {
		var sb = new StringBuilder();
		boolean first = true;
		for (var t : types) {
			if (!first) sb.append(" | ");
			sb.append(t.toString());
			first = false;
		}
		return sb.toString();
	}

	@Override
	public CastType accepts(Type type) {

		if (type instanceof CompoundType ct) {
			var worst = CastType.EQUALS;
			boolean ok = false;
			boolean nok = false;
			for (var t2 : ct.types) {
				var best = CastType.INCOMPATIBLE;
				for (var t1 : types) {
					var r = t1.accepts(t2);
					if (r.ordinal() < best.ordinal()) best = r;
				}
				if (best != CastType.INCOMPATIBLE) ok = true;
				if (best != CastType.EQUALS) nok = true;
				if (best.ordinal() > worst.ordinal()) worst = best;
			}
			if (worst == CastType.INCOMPATIBLE && ok) return CastType.UNSAFE_DOWNCAST;
			if (worst == CastType.EQUALS && nok) return CastType.UPCAST;
			return worst;
		}

		var best = CastType.INCOMPATIBLE;
		boolean ok = false;
		boolean nok = false;
		for (var t : types) {
			var r = t.accepts(type);
			if (r.ordinal() < best.ordinal()) best = r;
			if (r != CastType.INCOMPATIBLE) ok = true;
			if (r != CastType.EQUALS) nok = true;
		}
		if (best == CastType.INCOMPATIBLE && ok) return CastType.UNSAFE_DOWNCAST;
		if (best == CastType.EQUALS && nok) return CastType.UPCAST;
		return best;
	}

	@Override
	public boolean isNumber() {
		for (var t : this.types) if (!t.isNumber()) return false;
		return true;
	}

	@Override
	public Type element() {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.element());
		return Type.compound(result);
	}

	@Override
	public Type elementAccess(int version, boolean strict) {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.elementAccess(version, strict));
		return Type.compound(result);
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.elementAccess(version, strict, key));
		return Type.compound(result);
	}

	@Override
	public Type key() {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.key());
		return Type.compound(result);
	}

	@Override
	public Type member(String member) {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.member(member));
		return Type.compound(result);
	}

	public HashSet<Type> getTypes() {
		return types;
	}

	public boolean isArrayOrNull() {
		if (types.size() != 2) return false;
		boolean hasArray = false, hasNull = false;
		for (var t : types) {
			if (t.isArray()) hasArray = true;
			if (t == Type.NULL) hasNull = true;
		}
		return hasArray && hasNull;
	}

	public boolean isMapOrNull() {
		if (types.size() != 2) return false;
		boolean hasMap = false, hasNull = false;
		for (var t : types) {
			if (t.isMap()) hasMap = true;
			if (t == Type.NULL) hasNull = true;
		}
		return hasMap && hasNull;
	}

	public boolean isSetOrNull() {
		if (types.size() != 2) return false;
		boolean hasSet = false, hasNull = false;
		for (var t : types) {
			if (t.isSet()) hasSet = true;
			if (t == Type.NULL) hasNull = true;
		}
		return hasSet && hasNull;
	}

	public boolean canBeIterable() {
		for (var t : this.types) if (t.canBeIterable()) return true;
		return false;
	}

	public boolean isIterable() {
		for (var t : this.types) if (!t.isIterable()) return false;
		return true;
	}

	@Override
	public boolean isIndexable() {
		for (var t : this.types) if (!t.isIndexable()) return false;
		return true;
	}

	@Override
	public boolean canBeIndexable() {
		for (var t : this.types) if (t.canBeIndexable()) return true;
		return false;
	}

	public boolean canBeCallable() {
		for (var t : this.types) if (t.canBeCallable()) return true;
		return false;
	}

	public boolean isCallable() {
		for (var t : this.types) if (!t.isCallable()) return false;
		return true;
	}

	/**
	 * True ssi Type.NULL est explicitement dans le compound. Différent de
	 * canBeNull() qui retourne true pour ANY ou tout type avec canBeNull=true.
	 * Évite l'allocation stream pour le check de nullabilité explicite.
	 */
	public boolean containsNull() {
		for (var t : this.types) if (t == Type.NULL) return true;
		return false;
	}

	@Override
	public boolean canBeNull() {
		// Boucle indexée au lieu de stream + lambda — appelé fréquemment pendant
		// analyze() pour les checks de nullabilité (assignations, narrowings, etc.).
		for (var t : this.types) {
			if (t.canBeNull()) return true;
		}
		return false;
	}

	@Override
	public int getMinArguments() {
		int min = Integer.MAX_VALUE;
		for (var t : this.types) {
			int v = t.getMinArguments();
			if (v < min) min = v;
		}
		return min;
	}

	@Override
	public int getMaxArguments() {
		int max = Integer.MIN_VALUE;
		for (var t : this.types) {
			int v = t.getMaxArguments();
			if (v > max) max = v;
		}
		return max;
	}

	@Override
	public CastType acceptsArguments(List<Type> types) {
		var best = CastType.INCOMPATIBLE;
		for (var t : this.types) {
			var r = t.acceptsArguments(types);
			if (r.ordinal() < best.ordinal()) best = r;
		}
		return best;
	}

	@Override
	public Type getArgument(int a) {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.getArgument(a));
		return Type.compound(result);
	}

	@Override
	public Type getArgument(int argumentCount, int a) {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.getArgument(argumentCount, a));
		return Type.compound(result);
	}

	@Override
	public Type returnType() {
		var result = new HashSet<Type>();
		for (var t : this.types) result.add(t.returnType());
		return Type.compound(result);
	}

	public Type removeType(Type target) {
		// Fast path : 2 éléments, on extrait le non-target sans alloc de HashSet+stream.
		if (types.size() == 2) {
			Type kept = null;
			boolean found = false;
			for (var t : types) {
				if (t == target) found = true;
				else kept = t;
			}
			if (!found) return this;
			return kept != null ? kept : this;
		}
		var remaining = new HashSet<Type>();
		for (var t : this.types) if (t != target) remaining.add(t);
		if (remaining.isEmpty()) return this;
		if (remaining.size() == 1) return remaining.iterator().next();
		return Type.compound(remaining);
	}

	public Type assertNotNull() {
		// Fast path : T | NULL (cas dominant), on retourne T directement.
		if (types.size() == 2) {
			for (var t : types) {
				if (t != Type.NULL) return t;
			}
		}
		var remaining = new HashSet<Type>();
		for (var t : this.types) if (t != Type.NULL) remaining.add(t);
		if (remaining.size() > 1) {
			return Type.compound(remaining);
		}
		return remaining.iterator().next();
	}

	@Override
	public String toString() {
		if (types.size() == 2 && containsNull()) {
			for (var t : types) {
				if (t != Type.NULL) return t.toString() + "?";
			}
		}
		return name;
	}

	public boolean isWarning() {
		for (var t : this.types) if (t.isWarning()) return true;
		return false;
	}


	public boolean isIntOrReal() {
		return types.size() == 2 && types.contains(Type.INT) && types.contains(Type.REAL);
	}

	public String getJavaPrimitiveName(int version) {
		var set = new HashSet<String>();
		for (var t : types) {
			set.add(t.getJavaPrimitiveName(version));
		}
		if (set.size() == 1) return set.iterator().next();
		if (types.size() == 2) {
			if (containsNull()) {
				for (var t : types) {
					if (t != Type.NULL) {
						if (t == Type.BOOL || t == Type.INT || t == Type.REAL) {
							return t.getJavaName(version);
						} else {
							return t.getJavaPrimitiveName(version);
						}
					}
				}
			}
			if (types.contains(Type.INT) && types.contains(Type.REAL)) {
				return "Number";
			}
		}
		return "Object";
	}

	public String getJavaName(int version) {
		var set = new HashSet<String>();
		for (var t : types) {
			set.add(t.getJavaName(version));
		}
		if (set.size() == 1) return set.iterator().next();
		if (types.size() == 2) {
			if (containsNull()) {
				for (var t : types)
					if (t != Type.NULL)
						return t.getJavaName(version);
			}
			if (types.contains(Type.INT) && types.contains(Type.REAL)) {
				return "Number";
			}
		}
		return "Object";
	}

	@Override
	public Complete complete() {
		var complete = new Complete(this);
		for (var type : types) {
			complete.addAll(type.complete());
		}
		return complete;
	}


	@Override
	public int hashCode() {
		int hashCode = 7;
		for (var t : types) {
			hashCode = hashCode * 31 + t.hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof CompoundType mt) {
			return this.types.equals(mt.types);
		}
		return false;
	}
}
