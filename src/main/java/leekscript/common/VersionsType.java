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
		// Un Interval sans type d'élément est entier ou réel : les versions entière et réelle
		// (intervalMin, intervalToArray…) le couvrent à elles deux, l'appel n'est pas dangereux.
		for (int i = 0; i < types.size(); i++) {
			if (types.get(i) instanceof IntervalType it && it.element() == Type.ANY) {
				var cases = new ArrayList<>(types);
				cases.set(i, Type.INTEGER_INTERVAL);
				var integer = acceptsArguments(cases);
				cases.set(i, Type.REAL_INTERVAL);
				var real = acceptsArguments(cases);
				var worst = integer.ordinal() > real.ordinal() ? integer : real;
				return worst.ordinal() < best.ordinal() ? worst : best;
			}
		}
		return best;
	}
}
