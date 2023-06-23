package leekscript.runner.values;

import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class IntervalLeekValue {

	private final AI ai;
	public final int id;

	private final Object from;
	private final Object to;

	public IntervalLeekValue(AI ai, Object from, Object to) throws LeekRunException {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		this.from = from;
		this.to = to;
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

		if (visited.contains(from)) {
			sb.append("<...>");
		} else {
			if (!ai.isPrimitive(from)) {
				visited.add(from);
			}
			sb.append(ai.export(from, visited));
		}

		sb.append("..");

		if (visited.contains(to)) {
			sb.append("<...>");
		} else {
			if (!ai.isPrimitive(to)) {
				visited.add(to);
			}
			sb.append(ai.export(to, visited));
		}

		return sb.append("]").toString();
	}
}
