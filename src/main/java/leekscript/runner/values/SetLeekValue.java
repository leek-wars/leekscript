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

	public Object setPut(AI ai, Object value) throws LeekRunException {
		set.add(value);
		return value;
	}

	public Object setRemove(AI ai, Object value) throws LeekRunException {
		set.remove(value);
		return value;
	}

	public SetLeekValue setClear(AI ai) throws LeekRunException {
		set.clear();
		return this;
	}

	public boolean setContains(AI ai, Object value) throws LeekRunException {
		return operatorIn(value);
	}

	public boolean operatorIn(Object value) throws LeekRunException {
		return set.contains(value);
	}

	public long setSize(AI ai) throws LeekRunException {
		return set.size();
	}

	public boolean setIsEmpty(AI ai) throws LeekRunException {
		return set.isEmpty();
	}

	public boolean setIsSubsetOf(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops(this.set.size() * 2);
		return set.set.containsAll(this.set);
	}
}
