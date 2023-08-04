package leekscript.runner.values;

import java.util.HashSet;
import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class SetLeekValue {

	private final AI ai;
	public final int id;

	private final HashSet<Object> set;

	public SetLeekValue(AI ai, Object[] values) throws LeekRunException {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		this.set = new HashSet<Object>();
		for (Object value : values) {
			this.set.add(value);
		}
	}

	public SetLeekValue(AI ai) throws LeekRunException {
		this(ai, new Object[0]);
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

		StringBuilder sb = new StringBuilder("<");

		boolean first = true;

		for (Object value : set) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}

			if (visited.contains(value)) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(value)) {
					visited.add(value);
				}
				sb.append(ai.export(value, visited));
			}
		}

		return sb.append(">").toString();
	}
}
