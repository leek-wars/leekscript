package leekscript.runner.values;

import java.util.Set;

import leekscript.AILog;
import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class IntervalLeekValue {

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

		if (isLeftBounded()) {
			sb.append(ai.export(from, visited));
		}
		sb.append("..");
		if (isRightBounded()) {
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
		return isLeftBounded() && isRightBounded();
	}

	public boolean isLeftBounded() {
		return from != Double.NEGATIVE_INFINITY;
	}

	public boolean isRightBounded() {
		return to != Double.POSITIVE_INFINITY;
	}

	public boolean operatorIn(Object value) throws LeekRunException {
		ai.ops(1);
		var valueAsReal = ai.real(value);
		return from <= valueAsReal && valueAsReal <= to;
	}

	public ArrayLeekValue intervalToArray(AI ai) throws LeekRunException {
		return intervalToArray(ai, 1);
	}

	public ArrayLeekValue intervalToArray(AI ai, double step) throws LeekRunException {
		if (!intervalIsBounded(ai)) {
			ai.addSystemLog(AILog.ERROR, Error.CANNOT_ITERATE_UNBOUNDED_INTERVAL, new Object[] { this });
			return null;
		}

		// Operations are added by the array
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
}
