package leekscript.runner;

import java.util.Comparator;

import leekscript.runner.values.LegacyArrayLeekValue;

public class LeekValueComparator {

	public final static int SORT_ASC = 1;
	public final static int SORT_DESC = 2;

	public static final SortComparator ASC = new SortComparator(SORT_ASC);
	public static final SortComparator DESC = new SortComparator(SORT_DESC);

	public static int compareAsc(Object v1, Object v2) throws LeekRunException {
		if (v1 == null) {
			return v2 == null ? 0 : -1;
		} else if (v1 instanceof Boolean b1) {
			if (v2 == null) return 1;
			if (v2 instanceof Boolean b2) return Boolean.compare(b1, b2);
			return -1;
		} else if (v1 instanceof Number n1) {
			if (v2 instanceof Number n2) {
				if (v1 instanceof Long l1 && v2 instanceof Long l2) {
					return Long.compare(l1, l2);
				}
				// big_integer : comparaison EXACTE au-delà de 2^53. #bigint
				if ((v1 instanceof leekscript.runner.values.BigIntegerValue || v2 instanceof leekscript.runner.values.BigIntegerValue)
						&& leekscript.runner.values.BigIntegerValue.isIntegerLike(v1) && leekscript.runner.values.BigIntegerValue.isIntegerLike(v2)) {
					return leekscript.runner.values.BigIntegerValue.compareIntegers(v1, v2);
				}
				return Double.compare(n1.doubleValue(), n2.doubleValue());
			} else if (v2 instanceof Boolean || v2 == null)
				return 1;
			else
				return -1;
		} else if (v1 instanceof String s1) {
			if (v2 instanceof String s2) {
				return s1.compareTo(s2);
			} else if (v2 instanceof Number || v2 instanceof Boolean || v2 == null)
				return 1;
			else
				return -1;
		} else if (v1 instanceof LegacyArrayLeekValue a1) {
			if (v2 instanceof LegacyArrayLeekValue a2) {
				return Integer.compare(a1.size(), a2.size());
			} else if (v2 == null)
				return -1;
			else
				return 1;
		} else {
			return -1;
		}
	}

	public static class SortComparator implements Comparator<Object> {

		private final int mOrder;

		private SortComparator(int order) {
			mOrder = order;
		}

		@Override
		public int compare(Object v1, Object v2) {
			try {
				if (mOrder == SORT_ASC)
					return compareAsc(v1, v2);
				else if (mOrder == SORT_DESC)
					return compareAsc(v2, v1);
			} catch (LeekRunException e) {
				// The operation limit may be exceeded here, but it's not too long
			}
			return 0;
		}
	}
}
