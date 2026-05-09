package leekscript.runner.values;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.AI.RamUsage;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;

/**
 * Implémentation du type Set de LeekScript.
 *
 * Étend LinkedHashSet (et non HashSet) pour garantir un ordre d'itération
 * déterministe = ordre d'insertion (cohérent avec MapLeekValue).
 */
public class SetLeekValue extends LinkedHashSet<Object> implements LeekValue {

	public static class SetIterator implements Iterator<Entry<Object, Object>>, Entry<Object, Object> {

		private Iterator<Object> it;
		private long i = 0;
		private long currentKey;
		private Object currentValue;

		public SetIterator(SetLeekValue set) {
			this.it = set.iterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		/**
		 * L'iterator se sert lui-même d'Entry pour éviter d'allouer un
		 * SimpleEntry par itération. L'Entry retournée est invalidée par
		 * le prochain appel à {@code next()} ; copier via
		 * {@code Map.entry(e.getKey(), e.getValue())} si besoin de la conserver.
		 */
		@Override
		public Entry<Object, Object> next() {
			currentKey = i;
			currentValue = it.next();
			i++;
			return this;
		}

		@Override
		public Object getKey() { return currentKey; }

		@Override
		public Object getValue() { return currentValue; }

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}
	}

	private AI ai;
	public final int id;
	private RamUsage ram;

	public SetLeekValue(AI ai) throws LeekRunException {
		this(ai, new Object[0]);
	}

	public SetLeekValue(AI ai, int expectedSize) throws LeekRunException {
		// Capacity = N / loadFactor (+1) pour éviter le resize quand on ajoute N entrées.
		super((int) (expectedSize / 0.75f) + 1);
		this.ai = ai;
		this.id = ai.getNextObjectID();
		this.ram = ai.allocateRAM(this);
	}

	public SetLeekValue(AI ai, Object[] values) throws LeekRunException {
		// Capacity = N / loadFactor (+1) pour éviter le resize quand on ajoute N entrées.
		super((int) (values.length / 0.75f) + 1);
		this.ai = ai;
		this.id = ai.getNextObjectID();
		for (Object value : values) {
			this.add(value);
		}
		this.ram = ai.allocateRAM(this, values.length);
	}

	public void rebind(AI ai, Set<Object> visited) {
		if (!visited.add(this)) return;
		this.ai = ai;
		for (var value : this) {
			LeekOperations.rebind(ai, value, visited);
		}
	}

	public SetLeekValue(AI ai, SetLeekValue set, int level) throws LeekRunException {
		super((int) (set.size() / 0.75f) + 1);
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
		var r = new SetLeekValue(ai, this.size() + set.size());
		r.addAll(this);
		r.addAll(set);
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setIntersection(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 2);
		var r = new SetLeekValue(ai, this.size());
		r.addAll(this);
		r.retainAll(set);
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setDifference(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 2);
		var r = new SetLeekValue(ai, this.size());
		r.addAll(this);
		r.removeAll(set);
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setDisjunction(AI ai, SetLeekValue set) throws LeekRunException {
		ai.ops((this.size() + set.size()) * 4);
		var r = new SetLeekValue(ai, this.size() + set.size());
		for (var e : this) {
			if (!set.contains(e)) r.add(e);
		}
		for (var e : set) {
			if (!this.contains(e)) r.add(e);
		}
		ai.increaseRAM(ram, r.size());
		return r;
	}

	public SetLeekValue setFilter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 3 * size());
		var r = new SetLeekValue(ai, this.size());
		for (var v : this) {
			if (ai.bool(function.run(ai, null, v, this))) {
				r.add(v);
			}
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
