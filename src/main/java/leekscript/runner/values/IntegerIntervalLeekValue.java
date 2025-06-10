package leekscript.runner.values;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import leekscript.AILog;
import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.RamUsage;

public class IntegerIntervalLeekValue extends IntervalLeekValue {

	public static class IntervalIterator implements Iterator<Entry<Object, Object>> {

		private IntegerIntervalLeekValue interval;
		private long i = 0;
		private long x;

		public IntervalIterator(IntegerIntervalLeekValue interval) {
			this.interval = interval;
			this.x = interval.minClosed ? interval.from : interval.from + 1;
		}

		@Override
		public boolean hasNext() {
			return interval.maxClosed ? x <= interval.to : x < interval.to;
		}

		@Override
		public Entry<Object, Object> next() {
			var e = new AbstractMap.SimpleEntry<Object, Object>(i, x);
			i++;
			x++;
			return e;
		}
	}

	private final long from;
	private final long to;

	public IntegerIntervalLeekValue(AI ai) throws LeekRunException {
		this(ai, false, 0, false, 0);
	}

	public IntegerIntervalLeekValue(AI ai, boolean minClosed, Object from, boolean maxClosed, Object to) throws LeekRunException {
		super(ai, minClosed, maxClosed);

		this.from = ai.longint(from);
		this.to = ai.longint(to);

		if (minClosed && this.from == Long.MIN_VALUE) {
			throw new LeekRunException(Error.INTERVAL_INFINITE_CLOSED, new String[] { ai.string(this.from) } );
		}
		if (maxClosed && this.to == Long.MAX_VALUE) {
			throw new LeekRunException(Error.INTERVAL_INFINITE_CLOSED, new String[] { ai.string(this.to) } );
		}
	}

	@Override
	public boolean equals(Object object) {
		return object == this;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited);
	}

	public String toString(AI ai, Set<Object> visited) throws LeekRunException {
		ai.ops(1);

		StringBuilder sb = new StringBuilder(minClosed ? "[" : "]");

		if (intervalIsLeftBounded(ai)) {
			sb.append(ai.export(from, visited));
		} else {
			sb.append("-∞");
		}
		sb.append("..");
		if (intervalIsRightBounded(ai)) {
			sb.append(ai.export(to, visited));
		} else {
			sb.append("∞");
		}

		return sb.append(maxClosed ? "]" : "[").toString();
	}

	public boolean intervalMinClosed(AI ai) {
		return minClosed;
	}

	public long intervalMin(AI ai) {
		return from;
	}

	public boolean intervalMaxClosed(AI ai) {
		return maxClosed;
	}

	public long intervalMax(AI ai) {
		return to;
	}

	public long intervalSize(AI ai) {
		if (from == Long.MIN_VALUE || to == Long.MAX_VALUE) {
			return Long.MAX_VALUE;
		}
		return from - to;
	}

	public boolean intervalIsEmpty(AI ai) {
		return to < from || (from == to && !maxClosed && !maxClosed);
	}


	public boolean intervalIsBounded(AI ai) {
		return intervalIsLeftBounded(ai) && intervalIsRightBounded(ai);
	}

	public boolean intervalIsLeftBounded(AI ai) {
		return from != Long.MIN_VALUE;
	}

	public boolean intervalIsRightBounded(AI ai) {
		return to != Long.MAX_VALUE;
	}

	@Override
	public boolean intervalIsClosed(AI ai) {
		return maxClosed && minClosed;
	}

	@Override
	public boolean intervalIsLeftClosed(AI ai) {
		return minClosed;
	}

	@Override
	public boolean intervalIsRightClosed(AI ai) {
		return maxClosed;
	}

	public boolean operatorIn(Object value) throws LeekRunException {
		ai.ops(1);
		if (value instanceof Long l) {
			return intervalContains(ai, l);
		}
		return intervalContains(ai, ai.real(value));
	}

	@Override
	public boolean intervalContains(AI ai, long x) throws LeekRunException {
		if (x == Long.MIN_VALUE && from == x) return true;
		if (x == Long.MAX_VALUE && to == x) return true;
		return (minClosed ? from <= x : from < x) && (maxClosed ? x <= to : x < to);
	}

	@Override
	public boolean intervalContains(AI ai, double x) throws LeekRunException {
		if (x == Double.NEGATIVE_INFINITY && from == Long.MIN_VALUE) return true;
		if (x == Double.POSITIVE_INFINITY && to == Long.MAX_VALUE) return true;
		return (minClosed ? from <= x : from < x) && (maxClosed ? x <= to : x < to);
	}

	@Override
	public double intervalAverage(AI ai) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return Double.NaN;
		}
		// [a..b]
		if (intervalIsBounded(ai)) {
			return ((minClosed ? from : from + 1) + (maxClosed ? to : to - 1)) / 2.0;
		}
		// [a..]
		if (intervalIsLeftBounded(ai)) {
			return Double.POSITIVE_INFINITY;
		}
		// [..b]
		if (intervalIsRightBounded(ai)) {
			return Double.NEGATIVE_INFINITY;
		}
		// [..]
		return Double.NaN;
	}

	public IntegerIntervalLeekValue intervalIntersection(AI ai, IntegerIntervalLeekValue interval) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return new IntegerIntervalLeekValue(ai, minClosed, from, maxClosed, to);
		}

		if (interval.intervalIsEmpty(ai)) {
			return new IntegerIntervalLeekValue(ai, interval.minClosed, interval.from, interval.maxClosed, interval.to);
		}

		var unionMinClosed = from < interval.from ? interval.minClosed : minClosed;
		var unionMaxClosed = to < interval.to ? maxClosed : interval.maxClosed;
		return new IntegerIntervalLeekValue(ai, unionMinClosed, Math.max(from, interval.from), unionMaxClosed, Math.min(to, interval.to));
	}

	public RealIntervalLeekValue intervalIntersection(AI ai, RealIntervalLeekValue interval) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return new RealIntervalLeekValue(ai, minClosed, from, maxClosed, to);
		}

		if (interval.intervalIsEmpty(ai)) {
			return new RealIntervalLeekValue(ai, interval.minClosed, interval.getFrom(), interval.maxClosed, interval.getTo());
		}

		var unionMinClosed = from < interval.getFrom() ? interval.minClosed : minClosed;
		var unionMaxClosed = to < interval.getTo() ? maxClosed : interval.maxClosed;
		return new RealIntervalLeekValue(ai, unionMinClosed, Math.max(from, interval.getFrom()), unionMaxClosed, Math.min(to, interval.getTo()));
	}

	public IntegerIntervalLeekValue intervalCombine(AI ai, IntegerIntervalLeekValue interval) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return new IntegerIntervalLeekValue(ai, interval.minClosed, interval.from, interval.maxClosed, interval.to);
		}

		if (interval.intervalIsEmpty(ai)) {
			return new IntegerIntervalLeekValue(ai, minClosed, from, maxClosed, to);
		}

		var unionMinClosed = from < interval.from ? minClosed : interval.minClosed;
		var unionMaxClosed = to < interval.to ? interval.maxClosed : maxClosed;
		return new IntegerIntervalLeekValue(ai, unionMinClosed, Math.min(from, interval.from), unionMaxClosed, Math.max(to, interval.to));
	}

	public RealIntervalLeekValue intervalCombine(AI ai, RealIntervalLeekValue interval) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return new RealIntervalLeekValue(ai, interval.minClosed, interval.getFrom(), interval.maxClosed, interval.getTo());
		}

		if (interval.intervalIsEmpty(ai)) {
			return new RealIntervalLeekValue(ai, minClosed, from, maxClosed, to);
		}

		var unionMinClosed = from < interval.getFrom() ? minClosed : interval.minClosed;
		var unionMaxClosed = to < interval.getTo() ? interval.maxClosed : maxClosed;
		return new RealIntervalLeekValue(ai, unionMinClosed, Math.min(from, interval.getFrom()), unionMaxClosed, Math.max(to, interval.getTo()));
	}

	public ArrayLeekValue intervalToArray(AI ai) throws LeekRunException {
		return intervalToArray(ai, 1);
	}

	public ArrayLeekValue intervalToArray(AI ai, long step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var array = new ArrayLeekValue(ai);

		if (step >= 0) {
			var start = minClosed ? from : from + 1;
			var end = maxClosed ? to : to - 1;
			for (var i = start; i <= end; i += step) {
				array.push(ai, i);
			}
		} else {
			var start = maxClosed ? to : to - 1;
			var end = minClosed ? from : from + 1;
			for (var i = start; i >= end; i += step) {
				array.push(ai, i);
			}
		}

		ai.ops(array.size() * 2);

		return array;
	}

	public LegacyArrayLeekValue intervalToArray_v1_3(AI ai) throws LeekRunException {
		return intervalToArray_v1_3(ai, 1);
	}

	public LegacyArrayLeekValue intervalToArray_v1_3(AI ai, long step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var array = new LegacyArrayLeekValue(ai);

		if (step >= 0) {
			var start = minClosed ? from : from + 1;
			var end = maxClosed ? to : to - 1;
			for (var i = start; i <= end; i += step) {
				array.push(ai, i);
			}
		} else {
			var start = maxClosed ? to : to - 1;
			var end = minClosed ? from : from + 1;
			for (var i = start; i >= end; i += step) {
				array.push(ai, i);
			}
		}

		ai.ops(array.size() * 2);

		return array;
	}

	public ArrayLeekValue intervalToArray(AI ai, double step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var array = new ArrayLeekValue(ai);

		if (step >= 0) {
			var start = minClosed ? from : from + 1;
			var end = maxClosed ? to : to - 1;
			for (double i = start; i <= end; i += step) {
				array.push(ai, i);
			}
		} else {
			var start = maxClosed ? to : to - 1;
			var end = minClosed ? from : from + 1;
			for (double i = start; i >= end; i += step) {
				array.push(ai, i);
			}
		}

		ai.ops(array.size() * 2);

		return array;
	}

	public LegacyArrayLeekValue intervalToArray_v1_3(AI ai, double step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var array = new LegacyArrayLeekValue(ai);

		if (step >= 0) {
			for (double i = from; i <= to; i += step) {
				array.push(ai, i);
			}
		} else {
			for (double i = to; i >= from; i += step) {
				array.push(ai, i);
			}
		}

		ai.ops(array.size() * 2);

		return array;
	}

	public SetLeekValue intervalToSet(AI ai) throws LeekRunException {
		return intervalToSet(ai, 1);
	}

	public SetLeekValue intervalToSet(AI ai, long step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var set = new SetLeekValue(ai);

		if (step >= 0) {
			var start = minClosed ? from : from + 1;
			var end = maxClosed ? to : to - 1;
			for (var i = start; i <= end; i += step) {
				set.setPut(ai, i);
			}
		} else {
			var start = maxClosed ? to : to - 1;
			var end = minClosed ? from : from + 1;
			for (var i = start; i >= end; i += step) {
				set.setPut(ai, i);
			}
		}

		ai.ops(set.size() * 2);

		return set;
	}

	public SetLeekValue intervalToSet(AI ai, double step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var set = new SetLeekValue(ai);

		if (step >= 0) {
			var start = minClosed ? from : from + 1;
			var end = maxClosed ? to : to - 1;
			for (double i = start; i <= end; i += step) {
				set.setPut(ai, i);
			}
		} else {
			var start = maxClosed ? to : to - 1;
			var end = minClosed ? from : from + 1;
			for (double i = start; i >= end; i += step) {
				set.setPut(ai, i);
			}
		}

		ai.ops(set.size() * 2);

		return set;
	}

	public ArrayLeekValue range(AI ai, Object start, Object end, Object strideObject) throws LeekRunException {

		if (intervalIsEmpty(ai)) {
			return new ArrayLeekValue(ai);
		}

		var step = ai.real(strideObject);
		if (step == 0.0) {
			step = 1.0;
		}

		int maxSize = (int) ((to - from) / Math.abs(step)) + 1;

		var startAsInteger = start == null ? 0 : ai.integer(start);
		var endAsInteger = end == null ? maxSize : ai.integer(end);

		int minIdx = Math.max(0, startAsInteger < 0 ? maxSize + startAsInteger : startAsInteger);
		int maxIdx = Math.min(maxSize, endAsInteger < 0 ? maxSize + endAsInteger : endAsInteger);

		var array = new ArrayLeekValue(ai);

		for (var i = minIdx; i < maxIdx; ++i) {
			if (step >= 0) {
				array.push(ai, from + i * step);
			} else {
				array.push(ai, to + i * step);
			}
		}

		ai.ops(array.size() * 2);

		return array;
	}

	@Override
	public Iterator<Entry<Object, Object>> iterator() {
		return new IntervalIterator(this);
	}

	public long getFrom() {
		return from;
	}

	public long getTo() {
		return to;
	}
}
