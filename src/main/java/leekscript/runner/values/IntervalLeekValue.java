package leekscript.runner.values;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import leekscript.AILog;
import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class IntervalLeekValue {

	public static class IntervalIterator implements Iterator<Entry<Object, Object>> {

		private IntervalLeekValue interval;
		private long i = 0;
		private long x;

		public IntervalIterator(IntervalLeekValue interval) {
			this.interval = interval;
			this.x = (long) interval.from;
		}

		@Override
		public boolean hasNext() {
			return x < interval.to;
		}

		@Override
		public Entry<Object, Object> next() {
			var e = new AbstractMap.SimpleEntry<Object, Object>(i, x);
			i++;
			x++;
			return e;
		}
	}

	private final AI ai;
	public final int id;

	private final double from;
	private final double to;

	public IntervalLeekValue(AI ai, Object from, Object to) throws LeekRunException {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		this.from = ai.real(from);
		this.to = ai.real(to);
	}

	@Override
	public boolean equals(Object object) {
		return object == this;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public String getString(AI ai2, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited);
	}

	public String toString(AI ai, Set<Object> visited) throws LeekRunException {
		ai.ops(1);

		StringBuilder sb = new StringBuilder("[");

		if (intervalIsLeftBounded(ai)) {
			sb.append(ai.export(from, visited));
		}
		sb.append("..");
		if (intervalIsRightBounded(ai)) {
			sb.append(ai.export(to, visited));
		}

		return sb.append("]").toString();
	}

	public double intervalLowerBound(AI ai) {
		return from;
	}

	public double intervalUpperBound(AI ai) {
		return to;
	}

	public boolean intervalIsEmpty(AI ai) {
		return to < from;
	}

	public boolean intervalIsBounded(AI ai) {
		return intervalIsLeftBounded(ai) && intervalIsRightBounded(ai);
	}

	public boolean intervalIsLeftBounded(AI ai) {
		return from != Double.NEGATIVE_INFINITY;
	}

	public boolean intervalIsRightBounded(AI ai) {
		return to != Double.POSITIVE_INFINITY;
	}

	public boolean operatorIn(Object value) throws LeekRunException {
		ai.ops(1);
		var valueAsReal = ai.real(value);
		return from <= valueAsReal && valueAsReal <= to;
	}

	public double intervalMidpoint(AI ai) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return Double.NaN;
		}

		// [a..b]
		if (intervalIsBounded(ai)) {
			return (from + to) / 2;
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

	public IntervalLeekValue intervalIntersection(AI ai, IntervalLeekValue interval) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return new IntervalLeekValue(ai, from, to);
		}

		if (interval.intervalIsEmpty(ai)) {
			return new IntervalLeekValue(ai, interval.from, interval.to);
		}

		return new IntervalLeekValue(ai, Math.max(from, interval.from), Math.min(to, interval.to));
	}

	public IntervalLeekValue intervalCombine(AI ai, IntervalLeekValue interval) throws LeekRunException {
		if (intervalIsEmpty(ai)) {
			return new IntervalLeekValue(ai, interval.from, interval.to);
		}

		if (interval.intervalIsEmpty(ai)) {
			return new IntervalLeekValue(ai, from, to);
		}

		return new IntervalLeekValue(ai, Math.min(from, interval.from), Math.max(to, interval.to));
	}

	public ArrayLeekValue intervalToArray(AI ai) throws LeekRunException {
		return intervalToArray(ai, 1);
	}

	public ArrayLeekValue intervalToArray(AI ai, double step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		var array = new ArrayLeekValue(ai);

		if (step >= 0.0) {
			for (var i = from; i <= to; i += step) {
				array.push(ai, i);
			}
		} else {
			for (var i = to; i >= from; i += step) {
				array.push(ai, i);
			}
		}

		ai.ops(array.size() * 2);

		return array;
	}

	public ArrayLeekValue range(AI ai, Object start, Object end, double step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		if (intervalIsEmpty(ai)) {
			return new ArrayLeekValue(ai);
		}

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

	public Iterator<Entry<Object, Object>> iterator() {
		return new IntervalIterator(this);
	}
}
