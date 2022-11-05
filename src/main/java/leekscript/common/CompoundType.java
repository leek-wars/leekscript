package leekscript.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompoundType extends Type {

	private List<Type> types;

	public CompoundType(Type... types) {
		super(String.join(" | ", Arrays.asList(types).stream().map(t -> t.name).collect(Collectors.toList())), "x", "Object", "Object", "null");
		// super("compound", "", "", "", "");
		this.types = Arrays.asList(types);
	}

	@Override
	public CastType accepts(Type type) {
		var best = CastType.INCOMPATIBLE;
		var worst = CastType.EQUALS;
		for (var t : types) {
			var r = t.accepts(this);
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

	public List<Type> getTypes() {
		return types;
	}
}
