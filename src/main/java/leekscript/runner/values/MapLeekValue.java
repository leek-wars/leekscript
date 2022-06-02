package leekscript.runner.values;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueComparator;

public class MapLeekValue extends HashMap<Object, Object> implements Iterable<Entry<Object, Object>> {

	public MapLeekValue() {}

	public MapLeekValue(int capacity) {
		super(capacity);
	}

	public MapLeekValue(Object values[]) {
		for (int i = 0; i < values.length; i += 2) {
			put(values[i], values[i + 1]);
		}
	}

	public Object put(AI ai, Object key, Object value) {
		put(key, value);
		return value;
	}

	public Object get(AI ai, Object index) {
		return get(index);
	}

	public Object put_inc(AI ai, Object key) throws LeekRunException {
		return put(key, ai.add(get(key), 1));
	}

	public Object put_pre_inc(AI ai, Object key) throws LeekRunException {
		return put(key, ai.add(get(key), 1));
	}

	public Object put_add_eq(AI ai, Object key, Object value) throws LeekRunException {
		var v = ai.add(get(key), value);
		put(key, v);
		return v;
	}

	public MapLeekValue map(AI ai, FunctionLeekValue function) throws LeekRunException {
		int nb = function.getArgumentsCount(ai);
		if (nb != 1 && nb != 2) {
			return null;
		}
		var result = new MapLeekValue(size());
		for (var entry : this.entrySet()) {
			if (nb == 1)
				result.put(entry.getKey(), function.execute(ai, entry.getValue()));
			else
				result.put(entry.getKey(), function.execute(ai, entry.getValue(), entry.getKey()));
		}
		return result;
	}

	public double sum(AI ai) throws LeekRunException {
		double sum = 0;
		for (var val : this.values()) {
			sum += ai.real(val);
		}
		return sum;
	}

	public double average(AI ai) throws LeekRunException {
		double average = 0;
		for (var val : this.values()) {
			average += ai.real(val);
		}
		if (average == 0)
			return 0.0;
		return average / size();
	}

	public Object min(AI ai) {
		if (size() == 0) return null;
		var it = entrySet().iterator();
		Object min_value = it.next().getValue();
		var mincomp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
		while (it.hasNext()) {
			var val = it.next().getValue();
			if (mincomp.compare(val, min_value) == -1)
				min_value = val;
		}
		return min_value;
	}

	public Object max(AI ai) throws LeekRunException {
		Object max_value = null;
		var mincomp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
		for (var val : this.values()) {
			if (max_value == null)
				max_value = val;
			else if (mincomp.compare(val, max_value) == 1)
				max_value = val;
		}
		return max_value;
	}

	public Object search(Object value) {
		for (var entry : entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public Object removeElement(Object value) {
		for (var entry : entrySet()) {
			if (entry.getValue().equals(value)) {
				remove(entry.getKey());
			}
		}
		return null;
	}

	public String getString(AI ai, Set<Object> visited) throws LeekRunException {

		ai.ops(1 + size() * 2);

		if (size() == 0) {
			return "[:]";
		}

		StringBuilder sb = new StringBuilder("[");

		boolean first = true;
		for (var entry : this.entrySet()) {
			if (!first)
				sb.append(", ");
			else
				first = false;

			if (entry.getKey() instanceof ObjectLeekValue) {
				sb.append(ai.getString((ObjectLeekValue) entry.getKey(), visited));
			} else {
				sb.append(entry.getKey());
			}
			sb.append(" : ");

			if (visited.contains(entry.getValue())) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(entry.getValue())) {
					visited.add(entry.getValue());
				}
				sb.append(ai.getString(entry.getValue(), visited));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Iterator<Entry<Object, Object>> iterator() {
		return entrySet().iterator();
	}
}
