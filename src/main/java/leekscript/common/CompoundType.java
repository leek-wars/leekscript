package leekscript.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import leekscript.compiler.Complete;

public class CompoundType extends Type {

	protected HashSet<Type> types;

	public CompoundType(HashSet<Type> types) {
		super(String.join(" | ", types.stream().map(t -> t.toString()).collect(Collectors.toList())), "x", "Object", "Object", "null");
		this.types = types;
	}

	public CompoundType(Type... types) {
		super(String.join(" | ", Arrays.asList(types).stream().map(t -> t.toString()).collect(Collectors.toList())), "x", "Object", "Object", "null");
		this.types = new HashSet<Type>(Arrays.asList(types));
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
		return this.types.stream().allMatch(t -> t.isNumber());
	}

	@Override
	public Type element() {
		return Type.compound(this.types.stream().map(t -> t.element()).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public Type elementAccess(int version, boolean strict) {
		return Type.compound(this.types.stream().map(t -> t.elementAccess(version, strict)).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return Type.compound(this.types.stream().map(t -> t.elementAccess(version, strict, key)).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public Type key() {
		return Type.compound(this.types.stream().map(t -> t.key()).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public Type member(String member) {
		return Type.compound(this.types.stream().map(t -> t.member(member)).collect(Collectors.toCollection(HashSet::new)));
	}

	public HashSet<Type> getTypes() {
		return types;
	}

	public boolean isArrayOrNull() {
		return types.stream().anyMatch(t -> t.isArray()) && types.stream().anyMatch(t -> t == Type.NULL);
	}

	public boolean canBeIterable() {
		return this.types.stream().anyMatch(t -> t.canBeIterable());
	}

	public boolean isIterable() {
		return this.types.stream().allMatch(t -> t.isIterable());
	}

	@Override
	public boolean isIndexable() {
		return this.types.stream().allMatch(t -> t.isIndexable());
	}

	@Override
	public boolean canBeIndexable() {
		return this.types.stream().anyMatch(t -> t.canBeIndexable());
	}

	public boolean canBeCallable() {
		return this.types.stream().anyMatch(t -> t.canBeCallable());
	}

	public boolean isCallable() {
		return this.types.stream().allMatch(t -> t.isCallable());
	}

	@Override
	public boolean canBeNull() {
		return this.types.stream().anyMatch(t -> t.canBeNull());
	}

	@Override
	public int getMinArguments() {
		return this.types.stream().map(t -> t.getMinArguments()).min(Comparator.naturalOrder()).get();
	}

	@Override
	public int getMaxArguments() {
		return this.types.stream().map(t -> t.getMaxArguments()).max(Comparator.naturalOrder()).get();
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
		return Type.compound(this.types.stream().map(t -> t.getArgument(a)).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public Type getArgument(int argumentCount, int a) {
		return Type.compound(this.types.stream().map(t -> t.getArgument(argumentCount, a)).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public Type returnType() {
		return Type.compound(this.types.stream().map(t -> t.returnType()).collect(Collectors.toCollection(HashSet::new)));
	}

	public Type assertNotNull() {
		var remaining = this.types.stream().filter(t -> t != Type.NULL).collect(Collectors.toCollection(HashSet::new));
		if (remaining.size() > 1) {
			return Type.compound(remaining);
		}
		return remaining.iterator().next();
	}

	@Override
	public String toString() {
		if (types.size() == 2 && types.stream().anyMatch(t -> t == Type.NULL)) {
			for (var t : types) {
				if (t != Type.NULL) return t.toString() + "?";
			}
		}
		return name;
	}

	public boolean isWarning() {
		return this.types.stream().anyMatch(t -> t.isWarning());
	}


	public boolean isCompoundNumber() {
		return types.size() >= 2 && types.stream().allMatch(t -> t.isNumber());
	}

	public String getJavaPrimitiveName(int version) {
		var set = new HashSet<String>();
		for (var t : types) {
			set.add(t.getJavaPrimitiveName(version));
		}
		if (set.size() == 1) return set.iterator().next();
		if (types.size() == 2) {
			if (types.stream().anyMatch(t -> t == Type.NULL)) {
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
		}
		if (isCompoundNumber()) return "Number";
		return "Object";
	}

	public String getJavaName(int version) {
		var set = new HashSet<String>();
		for (var t : types) {
			set.add(t.getJavaName(version));
		}
		if (set.size() == 1) return set.iterator().next();
		if (types.size() == 2) {
			if (types.stream().anyMatch(t -> t == Type.NULL)) {
				for (var t : types)
					if (t != Type.NULL)
						return t.getJavaName(version);
			}
		}
		if (isCompoundNumber()) return "Number";
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
