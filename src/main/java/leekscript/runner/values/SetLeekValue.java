package leekscript.runner.values;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.RamUsage;

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
	private RamUsage ram;

	public SetLeekValue(AI ai) throws LeekRunException {
		this(ai, new Object[0]);
	}

	public SetLeekValue(AI ai, Object[] values) throws LeekRunException {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		for (Object value : values) {
			this.add(value);
		}
		this.ram = ai.allocateRAM(this, values.length);
	}

	public SetLeekValue(AI ai, SetLeekValue set, int level) throws LeekRunException {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		this.ram = ai.allocateRAM(this, set.size());
		for (var value : set) {
			if (level == 1) {
				this.add(value);
			} else {
				this.add(LeekOperations.clone(ai, value, level - 1));
			}
		}
	}

	public boolean eq(SetLeekValue set) throws LeekRunException {

		ai.ops(1);

		// On commence par vérifier la taille
		if (size() != set.size())
			return false;
		if (size() == 0)
			return true;

		ai.ops(2 * size());

		// On va comparer chaque élément 1 à 1
		for (var value : this) {
			if (!set.contains(value)) return false;
		}
		return true;
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

	public boolean setPut(AI ai, Object value) throws LeekRunException {
		boolean added = add(value);
		if (added) ai.increaseRAM(ram, 1);
		return added;
	}

	public boolean setRemove(AI ai, Object value) throws LeekRunException {
		boolean removed = remove(value);
		if (removed) ai.decreaseRAM(ram, 1);
		return removed;
	}

	public SetLeekValue setClear(AI ai) throws LeekRunException {
		ai.decreaseRAM(ram, size());
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
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setIntersection(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 2);
		var r = new SetLeekValue(ai);
		r.addAll(this);
		r.retainAll(set);
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setDifference(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 2);
		var r = new SetLeekValue(ai);
		r.addAll(this);
		r.removeAll(set);
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setDisjunction(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 4);
		var r = new SetLeekValue(ai);
		for (var e : this) {
			if (!set.contains(e)) r.add(e);
		}
		for (var e : set) {
			if (!this.contains(e)) r.add(e);
		}
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public ArrayLeekValue setToArray(AI ai) throws LeekRunException {
		ai.ops(this.size() * 2);
		var r = new ArrayLeekValue(ai, this.toArray());
		return r;
	}

	public LegacyArrayLeekValue setToArray_v1_3(AI ai) throws LeekRunException {
		ai.ops(this.size() * 2);
		var r = new LegacyArrayLeekValue(ai, this.toArray());
		return r;
	}
}
