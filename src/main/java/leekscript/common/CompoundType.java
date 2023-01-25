package leekscript.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class CompoundType extends Type {

	private HashSet<Type> types;

	public CompoundType(HashSet<Type> types) {
		super(String.join(" | ", types.stream().map(t -> t.name).collect(Collectors.toList())), "x", "Object", "Object", "null");
		this.types = types;
	}

	public CompoundType(Type... types) {
		super(String.join(" | ", Arrays.asList(types).stream().map(t -> t.name).collect(Collectors.toList())), "x", "Object", "Object", "null");
		this.types = new HashSet<Type>(Arrays.asList(types));
	}

	@Override
	public CastType accepts(Type type) {
		var best = CastType.INCOMPATIBLE;
		var worst = CastType.EQUALS;
		for (var t : types) {
			var r = t.accepts(type);
			if (r.ordinal() > worst.ordinal()) worst = r;
			if (r.ordinal() < best.ordinal()) best = r;
		}
		// Si un est compatible, le tout est compatible
		if (worst == CastType.INCOMPATIBLE && best != CastType.INCOMPATIBLE) return CastType.UNSAFE_DOWNCAST;
		// Sinon on prend le pire
		return worst;
	}

	@Override
	public boolean isNumber() {
		return this.types.stream().allMatch(t -> t.isNumber());
	}

	@Override
	public Type element() {
		return Type.compound(this.types.stream().map(t -> t.element()).collect(Collectors.toCollection(HashSet::new)));
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
}
