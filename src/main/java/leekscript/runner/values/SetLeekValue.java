package leekscript.runner.values;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class SetLeekValue extends HashSet<Object> implements LeekValue {

	public static class SetIterator implements Iterator<Entry<Object, Object>> {

		private Iterator<Object> it;
		private long i = 0;

		public SetIterator(SetLeekValue set) {
			this.it = set.iterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Entry<Object, Object> next() {
			var e = new AbstractMap.SimpleEntry<Object, Object>(i, it.next());
			i++;
			return e;
		}
	}

	private final AI ai;
	public final int id;


	public SetLeekValue(AI ai) throws LeekRunException {
		this(ai, new Object[0]);
	}

	public SetLeekValue(AI ai, Object[] values) throws LeekRunException {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		for (Object value : values) {
			this.add(value);
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

	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited);
	}

	public String toString(AI ai, Set<Object> visited) throws LeekRunException {
		ai.ops(1);

		StringBuilder sb = new StringBuilder("<");

		boolean first = true;

		for (var value : this) {
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
		add(value);
		return value;
	}

	public Object setRemove(AI ai, Object value) throws LeekRunException {
		remove(value);
		return value;
	}

	public SetLeekValue setClear(AI ai) throws LeekRunException {
		clear();
		return this;
	}

	public boolean setContains(AI ai, Object value) throws LeekRunException {
		return operatorIn(value);
	}

	public boolean operatorIn(Object value) throws LeekRunException {
		return contains(value);
	}

	public long setSize(AI ai) throws LeekRunException {
		return size();
	}

	public boolean setIsEmpty(AI ai) throws LeekRunException {
		return isEmpty();
	}

	public boolean setIsSubsetOf(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops(this.size() * 2);
		return set.containsAll(this);
	}

	public Iterator<Entry<Object, Object>> genericIterator() {
		return new SetIterator(this);
	}

	public SetLeekValue setUnion(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 2);
		var r = new SetLeekValue(ai);
		r.addAll(this);
		r.addAll(set);
		return r;
	}

	public SetLeekValue setIntersection(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 2);
		var r = new SetLeekValue(ai);
		r.addAll(this);
		r.retainAll(set);
		return r;
	}
}
