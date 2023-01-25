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

	private static final int READ_OPERATIONS = 2;
	private static final int WRITE_OPERATIONS = 3;
	private final AI ai;

	public MapLeekValue(AI ai) {
		this.ai = ai;
	}

	public MapLeekValue(AI ai, int capacity) {
		super(capacity);
		this.ai = ai;
	}

	public MapLeekValue(AI ai, Object values[]) throws LeekRunException {
		this.ai = ai;
		for (int i = 0; i < values.length; i += 2) {
			put(values[i], values[i + 1]);
		}
		ai.increaseRAM(2 * values.length);
	}

	public MapLeekValue(AI ai, MapLeekValue map) throws LeekRunException {
		this(ai, map, 1);
	}

	public MapLeekValue(AI ai, MapLeekValue map, int level) throws LeekRunException {
		this.ai = ai;
		for (var entry : map) {
			if (level == 1) {
				put(entry.getKey(), entry.getValue());
			} else {
				put(entry.getKey(), LeekOperations.clone(ai, entry.getValue(), level - 1));
			}
		}
		ai.increaseRAM(2 * size());
	}

	@Override
	public void set(AI ai, Object key, Object value) {
		put(key, value);
	}

	public Object put(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		if (!containsKey(key)) {
			ai.increaseRAM(2);
		}
		put(key, value);
		return value;
	}

	public Object mapPut(AI ai, Object key, Object value) throws LeekRunException {
		return put(ai, key, value);
	}

	public Object mapPutAll(AI ai, MapLeekValue map) throws LeekRunException {
		ai.ops(1 + 3 * map.size());
		var sizeBefore = size();
		putAll(map);
		ai.increaseRAM(2 * (size() - sizeBefore));
		return null;
	}

	public Object get(AI ai, Object index) {
		ai.opsNoCheck(MapLeekValue.READ_OPERATIONS);
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
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = get(key);
		put(key, ai.add(get(key), 1l));
		return v;
	}

	public Object put_pre_inc(AI ai, Object key) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.add(get(key), 1l);
		put(key, v);
		return v;
	}

	public Object put_dec(AI ai, Object key) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		return put(key, ai.sub(get(key), 1l));
	}

	public Object put_pre_dec(AI ai, Object key) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.sub(get(key), 1l);
		put(key, v);
		return v;
	}

	public Object put_add_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.add(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_sub_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.sub(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_mul_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.mul(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_div_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.div(get(key), value);
		put(key, v);
		return v;
	}

	public long put_intdiv_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.intdiv(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_mod_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.mod(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_pow_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.pow(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_band_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.band(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_bor_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.bor(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_bxor_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.bxor(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_shr_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.shr(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_ushr_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.ushr(get(key), value);
		put(key, v);
		return v;
	}

	public Object put_shl_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(MapLeekValue.WRITE_OPERATIONS);
		var v = ai.shl(get(key), value);
		put(key, v);
		return v;
	}

	public long mapSize(AI ai) {
		return size();
	}

	public boolean mapIsEmpty(AI ai) {
		return size() == 0;
	}

	public ArrayLeekValue mapKeys(AI ai) throws LeekRunException {
		return new ArrayLeekValue(ai, keySet().toArray());
	}

	public ArrayLeekValue mapValues(AI ai) throws LeekRunException {
		return new ArrayLeekValue(ai, values().toArray());
	}

	public MapLeekValue mapMap(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 3 * size());
		var result = new MapLeekValue(ai, size());
		for (var entry : this.entrySet()) {
			result.put(entry.getKey(), function.run(ai, null, entry.getValue(), entry.getKey(), this));
		}
		ai.increaseRAM(2 * size());
		return result;
	}

	public Object mapIter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		for (var entry : this.entrySet()) {
			function.run(ai, null, entry.getValue(), entry.getKey(), this);
		}
		return null;
	}

	public double mapSum(AI ai) throws LeekRunException {
		ai.ops(1 + 3 * size());
		double sum = 0;
		for (var val : this.values()) {
			sum += ai.real(val);
		}
		return sum;
	}

	public double mapAverage(AI ai) throws LeekRunException {
		ai.ops(1 + 3 * size());
		double average = 0;
		for (var val : this.values()) {
			average += ai.real(val);
		}
		if (average == 0)
			return 0.0;
		return average / size();
	}

	public Object mapMin(AI ai) throws LeekRunException {
		ai.ops(1 + 3 * size());
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
		ai.ops(1 + 3 * size());
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

	public Object mapSearch(AI ai, Object value) throws LeekRunException {
		ai.opsNoCheck(1);
		int i = 0;
		for (var entry : entrySet()) {
			if (ai.equals_equals(entry.getValue(), value)) {
				ai.ops(2 * i);
				return entry.getKey();
			}
		}
		ai.ops(2 * size());
		return null;
	}

	public Object mapRemove(AI ai, Object key) throws LeekRunException {
		return remove(key);
	}

	public ArrayLeekValue mapGetValues(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		return new ArrayLeekValue(ai, values().toArray());
	}

	public ArrayLeekValue mapGetKeys(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		return new ArrayLeekValue(ai, keySet().toArray());
	}

	public Object mapRemoveAll(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + 2 * size());
		var sizeBefore = size();
		entrySet().removeIf(entry -> (entry.getValue() == null && value == null) || entry.getValue().equals(value));
		ai.decreaseRAM(2 * (sizeBefore - size()));
		return null;
	}

	public Object mapReplace(AI ai, Object key, Object value) {
		return replace(key, value);
	}

	public Object mapReplaceAll(AI ai, MapLeekValue map) throws LeekRunException {
		ai.ops(1 + 2 * size());
		for (var entry : map) {
			replace(entry.getKey(), entry.getValue());
		}
		return null;
	}

	public Object mapFill(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + 2 * size());
		for (var entry : entrySet()) {
			entry.setValue(value);
		}
		return null;
	}

	public boolean mapEvery(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.opsNoCheck(1);
		int i = 0;
		for (var entry : entrySet()) {
			if (!ai.bool(function.run(ai, null, entry.getValue(), entry.getKey()))) {
				ai.ops(2 * i);
				return false;
			}
			i++;
		}
		ai.ops(2 * size());
		return true;
	}

	public boolean mapSome(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.opsNoCheck(1);
		int i = 0;
		for (var entry : entrySet()) {
			if (ai.bool(function.run(ai, null, entry.getValue(), entry.getKey(), this))) {
				ai.ops(2 * i);
				return true;
			}
			i++;
		}
		ai.ops(2 * size());
		return false;
	}

	public Object mapFold(AI ai, FunctionLeekValue function, Object v) throws LeekRunException {
		ai.ops(1 + 3 * size());
		var result = v;
		for (var entry : entrySet()) {
			result = function.run(ai, null, result, entry.getValue(), entry.getKey(), this);
		}
		return result;
	}

	public MapLeekValue mapFilter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 3 * size());
		var result = new MapLeekValue(ai);
		for (var entry : entrySet()) {
			if (ai.bool(function.run(ai, null, entry.getValue(), entry.getKey(), this))) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		ai.increaseRAM(2 * result.size());
		return result;
	}

	public MapLeekValue mapMerge(AI ai, MapLeekValue map) throws LeekRunException {
		ai.ops((size() + map.size()) * 3);
		var result = new MapLeekValue(ai, this);
		for (var entry : map.entrySet()) {
			result.putIfAbsent(entry.getKey(), entry.getValue());
		}
		ai.increaseRAM(2 * (result.size() - size()));
		return result;
	}

	public MapLeekValue mapClear(AI ai) {
		ai.decreaseRAM(2 * size());
		clear();
		return this;
	}

	public boolean mapContains(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + 2 * size());
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
				sb.append(ai.export(k, visited));
			}

			sb.append(" : ");

			var v = entry.getValue();
			if (visited.contains(v)) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(v)) {
					visited.add(v);
				}
				sb.append(ai.export(v, visited));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Iterator<Entry<Object, Object>> iterator() {
		return entrySet().iterator();
	}

	@Override
	@SuppressWarnings("deprecated")
	protected void finalize() throws Throwable {
		super.finalize();
		ai.decreaseRAM(2 * size());
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
		int hashCode = 1;
		hashCode = 31 * hashCode + size();
		for (var entry : this) {
			var k = entry.getKey();
			var kh = 0;
			if (k instanceof ArrayLeekValue) {
				kh = ((ArrayLeekValue) k).size();
			} else if (k instanceof MapLeekValue) {
				kh = ((MapLeekValue) k).size();
			} else if (k instanceof ObjectLeekValue) {
				kh = ((ObjectLeekValue) k).size();
			} else {
				kh = k == null ? 0 : k.hashCode();
			}
			hashCode = 31 * hashCode + kh;

			var eh = 0;
			var e = entry.getValue();
			if (e instanceof ArrayLeekValue) {
				eh = ((ArrayLeekValue) e).size();
			} else if (e instanceof MapLeekValue) {
				eh = ((MapLeekValue) e).size();
			} else if (e instanceof ObjectLeekValue) {
				eh = ((ObjectLeekValue) e).size();
			} else {
				eh = e == null ? 0 : e.hashCode();
			}
			hashCode = 31 * hashCode + eh;
		}
		return hashCode;
	}
}
