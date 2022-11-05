package leekscript.common;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
		var result = CastType.INCOMPATIBLE;
		for (var t : types) {
			var r = t.accepts(type);
			if (r.ordinal() < result.ordinal()) {
				result = r;
			}
		}
		return result;
	}

	@Override
	public boolean isNumber() {
		return this.types.stream().allMatch(t -> t.isNumber());
	}

	public List<Type> getTypes() {
		return types;
	}
}
