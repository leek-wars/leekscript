package leekscript.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class VersionsType extends CompoundType {

	public VersionsType(HashSet<Type> types) {
		super(types);
	}

	public VersionsType(Type... types) {
		super(types);
	}

	@Override
	public CastType acceptsArguments(List<Type> types) {
		// On prend le meilleur car il s'agit de plusieurs versions
		var best = CastType.INCOMPATIBLE;
		for (var t : this.types) {
			var r = t.acceptsArguments(types);
			if (r.ordinal() < best.ordinal()) best = r;
		}
		return best;
	}
}
