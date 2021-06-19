package leekscript.runner;

import java.util.Comparator;

import leekscript.runner.values.AbstractLeekValue;

public class LeekValueComparator {

	public static class SortComparator implements Comparator<AbstractLeekValue> {

		private final int mOrder;
		private final AI ai;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

		public SortComparator(AI ai, int order) {
			mOrder = order;
			this.ai = ai;
		}

		@Override
		public int compare(AbstractLeekValue v1, AbstractLeekValue v2) {
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

		public int compareAsc(AbstractLeekValue v1, AbstractLeekValue v2) throws LeekRunException {
			if (LeekFunctions.isType(v1, LeekFunctions.BOOLEAN)) {
				if (LeekFunctions.isType(v2, LeekFunctions.NULL)) {
					return 1;
				}
				if (LeekFunctions.isType(v2, LeekFunctions.BOOLEAN)) {
					if (v1.getBoolean() == v2.getBoolean())
						return 0;
					else if (v1.getBoolean())
						return 1;
					else
						return -1;
				}
				return -1;
			} else if (LeekFunctions.isType(v1, LeekFunctions.NUMBER)) {
				if (LeekFunctions.isType(v2, LeekFunctions.NUMBER)) {
					if (v1.getDouble(ai) == v2.getDouble(ai))
						return 0;
					else if (v1.getDouble(ai) < v2.getDouble(ai))
						return -1;
					else
						return 1;
				} else if (LeekFunctions.isType(v2, LeekFunctions.BOOLEAN) || LeekFunctions.isType(v2, LeekFunctions.NULL))
					return 1;
				else
					return -1;
			} else if (LeekFunctions.isType(v1, LeekFunctions.STRING)) {
				if (LeekFunctions.isType(v2, LeekFunctions.STRING)) {
					return v1.getString(ai).compareTo(v2.getString(ai));
				} else if (LeekFunctions.isType(v2, LeekFunctions.NUMBER) || LeekFunctions.isType(v2, LeekFunctions.BOOLEAN) || LeekFunctions.isType(v2, LeekFunctions.NULL))
					return 1;
				else
					return -1;
			} else if (LeekFunctions.isType(v1, LeekFunctions.ARRAY)) {
				if (LeekFunctions.isType(v2, LeekFunctions.ARRAY)) {
					if (v1.getArray().size() == v2.getArray().size())
						return 0;
					else if (v1.getArray().size() < v2.getArray().size())
						return -1;
					else
						return 1;
				} else if (LeekFunctions.isType(v2, LeekFunctions.NULL))
					return -1;
				else
					return 1;
			} else
				return -1;
		}
	}
}
