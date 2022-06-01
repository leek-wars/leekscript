package leekscript.runner;

import java.util.Comparator;

import leekscript.runner.values.LegacyArrayLeekValue;

public class LeekValueComparator {

	public static class SortComparator implements Comparator<Object> {

		private final int mOrder;
		// private final AI ai;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

		public SortComparator(AI ai, int order) {
			mOrder = order;
			// this.ai = ai;
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

		public int compareAsc(Object v1, Object v2) throws LeekRunException {
			if (v1 == null) {
				if (v2 == null) return 0;
				return -1;
			} else if (v1 instanceof Boolean) {
				if (v2 == null) return 1;
				if (v2 instanceof Boolean) {
					if ((Boolean) v1 == (Boolean) v2)
						return 0;
					else if ((Boolean) v1)
						return 1;
					else
						return -1;
				}
				return -1;
			} else if (v1 instanceof Number) {
				if (v2 instanceof Number) {
					if (((Number) v1).doubleValue() == ((Number) v2).doubleValue())
						return 0;
					else if (((Number) v1).doubleValue() < ((Number) v2).doubleValue())
						return -1;
					else
						return 1;
				} else if (v2 instanceof Boolean || v2 == null)
					return 1;
				else
					return -1;
			} else if (v1 instanceof String) {
				if (v2 instanceof String) {
					return ((String) v1).compareTo((String) v2);
				} else if (v2 instanceof Number || v2 instanceof Boolean || v2 == null)
					return 1;
				else
					return -1;
			} else if (v1 instanceof LegacyArrayLeekValue) {
				if (v2 instanceof LegacyArrayLeekValue) {
					if (((LegacyArrayLeekValue) v1).size() == ((LegacyArrayLeekValue) v2).size())
						return 0;
					else if (((LegacyArrayLeekValue) v1).size() < ((LegacyArrayLeekValue) v2).size())
						return -1;
					else
						return 1;
				} else if (v2 == null)
					return -1;
				else
					return 1;
			} else {
				return -1;
			}
		}
	}
}
