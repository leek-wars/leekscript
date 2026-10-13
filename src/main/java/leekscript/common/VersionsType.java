package leekscript.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
		if (best != CastType.SAFE_DOWNCAST && best != CastType.UNSAFE_DOWNCAST) return best;
		// Aucune version ne couvre seule un argument (un Interval sans type d'élément pour
		// intervalMin, intervalToArray…) : l'appel est jugé sur le pire de ses cas concrets.
		for (int i = 0; i < types.size(); i++) {
			var cases = types.get(i).cases();
			if (cases.size() < 2) continue;
			var split = new ArrayList<>(types);
			var worst = CastType.EQUALS;
			for (var c : cases) {
				split.set(i, c);
				var r = acceptsArguments(split);
				if (r.ordinal() > worst.ordinal()) worst = r;
			}
			return worst.ordinal() < best.ordinal() ? worst : best;
		}
		return best;
	}
}
