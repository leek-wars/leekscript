package leekscript.runner.values;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueComparator;

public class MapLeekValue extends HashMap<Object, Object> implements Iterable<Entry<Object, Object>>, GenericMapLeekValue {

	public MapLeekValue() {}

	public MapLeekValue(int capacity) {
		super(capacity);
	}

	public MapLeekValue(Object values[]) {
		for (int i = 0; i < values.length; i += 2) {
			put(values[i], values[i + 1]);
		}
	}

	public MapLeekValue(AI ai, MapLeekValue map, int level) throws LeekRunException {
		for (var entry : map) {
			if (level == 1) {
				put(entry.getKey(), entry.getValue());
			} else {
				put(entry.getKey(), LeekOperations.clone(ai, entry.getValue(), level - 1));
			}
		}
	}

	@Override
	public void set(AI ai, Object key, Object value) {
		put(key, value);
	}

	public Object put(AI ai, Object key, Object value) {
		put(key, value);
		return value;
	}

	public Object mapPut(AI ai, Object key, Object value) {
		put(key, value);
		return value;
	}

	public Object get(AI ai, Object index) {
		return get(index);
	}

	public Object mapGet(AI ai, Object key) {
		return get(key);
	}

	public Object mapGet(AI ai, Object key, Object defaultValue) {
		return getOrDefault(key, defaultValue);
	}

	public Object add_eq(AI ai, Object y) {
		if (y instanceof MapLeekValue) {
			for (var entry : ((MapLeekValue) y).entrySet()) {
				putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	public Object put_inc(AI ai, Object key) throws LeekRunException {
		return put(key, ai.add(get(key), 1l));
	}

	public Object put_pre_inc(AI ai, Object key) throws LeekRunException {
		return put(key, ai.add(get(key), 1l));
	}

	public Object put_add_eq(AI ai, Object key, Object value) throws LeekRunException {
		var v = ai.add(get(key), value);
		put(key, v);
		return v;
	}

	public long mapSize(AI ai) {
		return size();
	}

	public boolean mapIsEmpty(AI ai) {
		return size() == 0;
	}

	public MapLeekValue mapMap(AI ai, FunctionLeekValue function) throws LeekRunException {
		var result = new MapLeekValue(size());
		for (var entry : this.entrySet()) {
			result.put(entry.getKey(), function.run(ai, null, entry.getValue(), entry.getKey(), this));
		}
		return result;
	}

	public Object mapIter(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (var entry : this.entrySet()) {
			function.run(ai, null, entry.getValue(), entry.getKey());
		}
		return null;
	}

	public double mapSum(AI ai) throws LeekRunException {
		double sum = 0;
		for (var val : this.values()) {
			sum += ai.real(val);
		}
		return sum;
	}

	public double mapAverage(AI ai) throws LeekRunException {
		double average = 0;
		for (var val : this.values()) {
			average += ai.real(val);
		}
		if (average == 0)
			return 0.0;
		return average / size();
	}

	public Object mapMin(AI ai) {
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

	public Object mapMax(AI ai) throws LeekRunException {
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

	public Object mapSearch(AI ai, Object value) {
		for (var entry : entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public Object mapRemove(AI ai, Object key) {
		return remove(key);
	}

	public Object mapRemoveElement(AI ai, Object value) {
		for (var entry : entrySet()) {
			if (entry.getValue().equals(value)) {
				remove(entry.getKey());
			}
		}
		return null;
	}

	public ArrayLeekValue mapGetValues(AI ai) throws LeekRunException {
		return new ArrayLeekValue(values().toArray());
	}

	public ArrayLeekValue mapGetKeys(AI ai) throws LeekRunException {
		return new ArrayLeekValue(keySet().toArray());
	}

	public Object mapRemoveAll(AI ai, Object value) {
		entrySet().removeIf(entry -> entry.getValue().equals(value));
		return null;
	}

	public Object mapReplace(AI ai, Object key, Object value) {
		return replace(key, value);
	}

	public Object mapReplaceAll(AI ai, MapLeekValue map) {
		for (var entry : map) {
			replace(entry.getKey(), entry.getValue());
		}
		return null;
	}

	public Object mapFill(AI ai, Object value) {
		for (var entry : entrySet()) {
			entry.setValue(value);
		}
		return null;
	}

	public boolean mapEvery(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (var entry : entrySet()) {
			if (!ai.bool(function.run(ai, null, entry.getValue(), entry.getKey()))) return false;
		}
		return true;
	}

	public boolean mapSome(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (var entry : entrySet()) {
			if (ai.bool(function.run(ai, null, entry.getValue(), entry.getKey(), this))) return true;
		}
		return false;
	}

	public Object mapFold(AI ai, FunctionLeekValue function, Object v) throws LeekRunException {
		var result = v;
		for (var entry : entrySet()) {
			result = function.run(ai, null, result, entry.getValue(), entry.getKey(), this);
		}
		return result;
	}

	public MapLeekValue mapFilter(AI ai, FunctionLeekValue function) throws LeekRunException {
		var result = new MapLeekValue();
		for (var entry : entrySet()) {
			if (ai.bool(function.run(ai, null, entry.getValue(), entry.getKey(), this))) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	public MapLeekValue mapMerge(AI ai, MapLeekValue map) throws LeekRunException {
		ai.ops((size() + map.size()) * 2);
		var result = (MapLeekValue) this.clone();
		for (var entry : map.entrySet()) {
			result.putIfAbsent(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public Object mapClear(AI ai) {
		clear();
		return null;
	}

	public boolean mapContains(AI ai, Object value) {
		return containsValue(value);
	}

	public boolean mapContainsKey(AI ai, Object key) {
		return containsKey(key);
	}

	public Object mapRemoveKey(AI ai, Object key) {
		return remove(key);
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

			var k = entry.getKey();
			if (visited.contains(k)) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(k)) {
					visited.add(k);
				}
				sb.append(ai.getString(k, visited));
			}

			sb.append(" : ");

			var v = entry.getValue();
			if (visited.contains(v)) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(v)) {
					visited.add(v);
				}
				sb.append(ai.getString(v, visited));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Iterator<Entry<Object, Object>> iterator() {
		return entrySet().iterator();
	}

	public JSONObject toJSON(AI ai, HashSet<Object> visited) throws LeekRunException {
		visited.add(this);

		var o = new JSONObject();
		for (var entry : entrySet()) {
			var v = entry.getValue();
			if (!visited.contains(v)) {
				if (!ai.isPrimitive(v)) {
					visited.add(v);
				}
				o.put(ai.string(entry.getKey()), ai.toJSON(v, visited));
		}
		}
		return o;
	}

	public int hashCode() {
		return 0;
	}
}
