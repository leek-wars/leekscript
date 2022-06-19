package leekscript.runner.values;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueComparator;
import leekscript.runner.LeekValueManager;
import leekscript.common.Error;

public class ArrayLeekValue extends ArrayList<Object> implements GenericArrayLeekValue {

	private static final int ARRAY_CELL_ACCESS_OPERATIONS = 1;

	public final static int ASC = 0;
	public final static int DESC = 1;
	public final static int RANDOM = 2;

	private static class ElementComparator implements Comparator<Object> {

		private final int mOrder;

		public final static int SORT_ASC = 0;
		public final static int SORT_DESC = 1;

		public ElementComparator(int order) {
			mOrder = order;
		}

		@Override
		public int compare(Object v1, Object v2) {
			try {
				if (mOrder == SORT_ASC)
					return compareAsc(v1, v2);
				else if (mOrder == SORT_DESC)
					return compareAsc(v2, v1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

		public int compareAsc(Object v1, Object v2) throws LeekRunException {
			var type1 = LeekValueManager.getType(v1);
			var type2 = LeekValueManager.getType(v2);
			if (type1 != type2)
				return type1 - type2;

			if (type1 == LeekValue.BOOLEAN) {
				if ((Boolean) v1 == (Boolean) v2)
					return 0;
				else if ((Boolean) v1)
					return 1;
				else
					return -1;
			} else if (type1 == LeekValue.NUMBER) {
				var d1 = ((Number) v1).doubleValue();
				var d2 = ((Number) v2).doubleValue();
				return Double.compare(d1, d2);
			} else if (type1 == LeekValue.STRING) {
				return ((String) v1).compareTo((String) v2);
			} else if (type1 == LeekValue.ARRAY) {
				return ((LegacyArrayLeekValue) v1).size() - ((LegacyArrayLeekValue) v2).size();
			}
			return 0;
		}
	}

	public static class ArrayIterator implements Iterator<Entry<Object, Object>> {

		private ArrayLeekValue array;
		private int i = 0;

		public ArrayIterator(ArrayLeekValue array) {
			this.array = array;
		}

		@Override
		public boolean hasNext() {
			return i < array.size();
		}

		@Override
		public Entry<Object, Object> next() {
			var e = new AbstractMap.SimpleEntry<Object, Object>((long) i, array.get(i));
			i++;
			return e;
		}
	}

	public ArrayLeekValue() {

	}

	public ArrayLeekValue(int capacity) {
		super(capacity);
	}

	public ArrayLeekValue(Object values[]) {
		for (var value : values) {
			add(value);
		}
	}

	public ArrayLeekValue(List<Object> values) {
		super(values);
	}

	public ArrayLeekValue(AI ai, ArrayLeekValue array, int level) throws LeekRunException {
		for (var value : array) {
			if (level == 1) {
				add(value);
			} else {
				add(LeekOperations.clone(ai, value, level - 1));
			}
		}
	}

	public long count(AI ai) {
		return size();
	}

	public Object put(AI ai, Object keyValue, Object value) throws LeekRunException {
		if (value instanceof Integer) {
			throw new LeekRunException(LeekRunException.INVALID_VALUE, value);
		}
		int i = ai.integer(keyValue);
		try {
			set(i, value);
			return value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_inc(AI ai, Object key) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var previous_value = get(i);
			set(i, ai.add(previous_value, 1l));
			return previous_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_pre_inc(AI ai, Object key) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.add(get(i), 1l);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_dec(AI ai, Object key) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var previous_value = get(i);
			set(i, ai.sub(previous_value, 1l));
			return previous_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_pre_dec(AI ai, Object key) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.sub(get(i), 1l);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_add_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.add(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_sub_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.sub(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_mul_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.mul(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_pow_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.pow(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_div_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.div(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_mod_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.mod(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_bor_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.bor(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_band_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.band(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_bxor_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.bxor(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_shl_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.shl(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_shr_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.shr(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_ushr_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = (int) ai.integer(key);
		try {
			var new_value = ai.ushr(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object shuffle(AI ai) throws LeekRunException {
		sort(ai, RANDOM);
		return null;
	}

	public String join(AI ai, String sep) throws LeekRunException {
		ai.ops(1 + size() * 2);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object val : this) {
			if (!first)
				sb.append(sep);
			else
				first = false;
			sb.append(LeekValueManager.getString(ai, val));
		}
		return sb.toString();
	}

	public String getString(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited);
	}

	public boolean equals(AI ai, Object comp) throws LeekRunException {
		if (comp instanceof LegacyArrayLeekValue) {
			return equals(ai, ((LegacyArrayLeekValue) comp));
		} else if (size() == 1) { // Si y'a un seul élément dans le tableau
			var firstValue = get(0);
			return ai.eq(firstValue, comp);
		} else if (comp instanceof Boolean) {
			return ai.bool(comp) == ai.bool(this);
		} else if (comp instanceof String) {
			if (ai.string(comp).equals("false") && ai.bool(this) == false)
				return true;
			else if (ai.string(comp).equals("true") && ai.bool(this) == true)
				return true;
			else if (ai.string(comp).isEmpty() && size() == 0)
				return true;
		} else if (comp instanceof Number) {
			if (size() == 0 && ai.integer(comp) == 0)
				return true;
		}
		return false;
	}

	public Object add_eq(AI ai, Object value) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			addAll((ArrayLeekValue) value);
		} else {
			add(value);
		}
		return this;
	}

	public JSONArray toJSON(AI ai) throws LeekRunException {
		return toJSON(ai, new HashSet<>());
	}

	public JSONArray toJSON(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);

		JSONArray a = new JSONArray();
		for (var v : this) {
			if (!visited.contains(v)) {
				if (!ai.isPrimitive(v)) {
					visited.add(v);
				}
				a.add(ai.toJSON(v));
			}
		}
		return a;
	}

	/**
	 * Retourne la valeur à pour une clé donnée
	 *
	 * @param key
	 *            Clé dont on veut la valeur
	 * @return Valeur à la clé donnée
	 * @throws LeekRunException
	 */
	public Object get(AI ai, Object key) throws LeekRunException {
		ai.addOperationsNoCheck(ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS);
		int i = (int) ai.integer(key);
		try {
			return get(i);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object arrayGet(AI ai, long index) throws LeekRunException {
		ai.addOperationsNoCheck(ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS);
		try {
			return get((int) index);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, (int) index);
			return null;
		}
	}

	public Object arrayGet(AI ai, long index, Object defaultValue) throws LeekRunException {
		ai.addOperationsNoCheck(ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS);
		try {
			return get((int) index);
		} catch (IndexOutOfBoundsException e) {
			return defaultValue;
		}
	}

	private void wrongIndexError(AI ai, int i) throws LeekRunException {
		ai.addSystemLog(AILog.ERROR, Error.ARRAY_OUT_OF_BOUND, new String[] {
			String.valueOf(i),
			String.valueOf(size())
		});
	}

	public Object get(AI ai, int index) throws LeekRunException {
		ai.addOperationsNoCheck(ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS);
		try {
			return get(index);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, index);
			return null;
		}
	}

	public long search(AI ai, Object value) throws LeekRunException {
		ai.addOperationsNoCheck(1);
		for (var i = 0; i < size(); ++i) {
			var e = get(i);
			if (value == null ? e == value : value.equals(e)) {
				ai.ops(i);
				return (long) i;
			}
		}
		ai.ops(size());
		return -1l;
	}

	/**
	 * Retourne la clé associé à une valeur
	 *
	 * @param value
	 *            Valeur à rechercher
	 * @param pos
	 * @return Clé associée à la valeur ou null si la valeur n'existe pas
	 * @throws LeekRunException
	 */
	public long search(AI ai, Object value, long pos) throws LeekRunException {
		ai.addOperationsNoCheck(1);
		for (var i = (int) pos; i < size(); ++i) {
			var e = get(i);
			if (value == null ? e == value : value.equals(e)) {
				ai.ops(i);
				return (long) i;
			}
		}
		ai.ops(size());
		return -1l;
	}

	/**
	 * Supprime un objet par sa clé
	 *
	 * @param key
	 *            Clé à supprimer
	 * @throws LeekRunException
	 */
	public Object remove(AI ai, long key) throws LeekRunException {
		int numMoved = size() - (int) key - 1;
		ai.ops(1 + Math.max(0, numMoved));
		try {
			return remove((int) key);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, (int) key);
			return null;
		}
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public Object sort(AI ai) throws LeekRunException {
		ai.ops(1 + (int) (5 * size() * Math.log(size())));
		Collections.sort(this, new ElementComparator(ASC));
		return null;
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public Object sort(AI ai, long comparator) throws LeekRunException {
		ai.ops(1 + (int) (5 * size() * Math.log(size())));
		if (comparator == RANDOM) {
			Collections.shuffle(this, new Random(ai.getRandom().getInt(0, Integer.MAX_VALUE - 1)));
		} else {
			Collections.sort(this, new ElementComparator((int) comparator));
		}
		return null;
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public void sort(AI ai, Comparator<Object> comparator) throws LeekRunException {
		ai.ops(1 + (int) (5 * size() * Math.log(size())));
		Collections.sort(this, comparator);
	}

	public ArrayLeekValue arraySort(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + (int) (5 * size() * Math.log(size())));
		var result = new ArrayLeekValue(ai, this, 1);
		Collections.sort(result, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				try {
					return (int) ai.integer(function.run(ai, null, o1, o2));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		return result;
	}

	/**
	 * Inverse l'ordre
	 *
	 * @throws LeekRunException
	 */
	public Object reverse(AI ai) throws LeekRunException {
		ai.ops(1 + size());
		Collections.reverse(this);
		return null;
	}

	public Object removeElement(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + size());
		remove(value);
		return null;
	}

	/**
	 * Ajouter un élément à la fin du array
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws LeekRunException
	 */
	@Override
	public Object push(AI ai, Object value) throws LeekRunException {
		if (value instanceof Integer) {
			throw new LeekRunException(LeekRunException.INVALID_VALUE, value);
		}
		add(value);
		return null;
	}

	public Object pushNoClone(AI ai, Object value) throws LeekRunException {
		if (value instanceof Integer) {
			throw new LeekRunException(LeekRunException.INVALID_VALUE, value);
		}
		add(value);
		return null;
	}

	public Object pushAll(AI ai, ArrayLeekValue other) throws LeekRunException {
		ai.ops(1 + other.size());
		addAll(other);
		return null;
	}

	/**
	 * Ajouter un élément au début du array (décale les index numériques)
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws LeekRunException
	 */
	public Object unshift(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + size());
		add(0, value);
		return null;
	}

	public Object insert(AI ai, Object value, long position) throws LeekRunException {
		int shifted = size() - (int) position;
		ai.ops(1 + Math.max(0, shifted));
		add((int) position, value);
		return null;
	}

	public Object fill(AI ai, Object value) throws LeekRunException {
		return fill(ai, value, size());
	}

	public Object fill(AI ai, Object value, long size) throws LeekRunException {
		ai.ops(1, Math.max(0, (int) size));
		if (size >= size()) { // Plus petit ou égal
			Collections.fill(this, value);
			var to_add = size - size();
			for (int i = 0; i < to_add; ++i) add(value);
		} else { // Plus grand
			this.ensureCapacity((int) size);
			for (int i = 0; i < size; ++i) {
				set(i, value);
			}
		}
		return null;
	}

	public double average(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		double average = 0;
		for (var val : this) {
			average += ai.real(val);
		}
		if (average == 0)
			return 0.0;
		return average / size();
	}

	public double sum(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		double somme = 0;
		for (var val : this) {
			somme += ai.real(val);
		}
		return somme;
	}

	public Object arrayMin(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		if (size() == 0) return null;
		Object min_value = get(0);
		var mincomp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
		for (int i = 1; i < size(); ++i) {
			var val = get(i);
			if (mincomp.compare(val, min_value) == -1)
				min_value = val;
		}
		return min_value;
	}

	public Object arrayMax(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		Object max_value = null;
		var mincomp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
		for (var val : this) {
			if (max_value == null)
				max_value = val;
			else if (mincomp.compare(val, max_value) == 1)
				max_value = val;
		}
		return max_value;
	}

	public ArrayLeekValue arrayMap(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		var result = new ArrayLeekValue(size());
		for (int i = 0; i < size(); ++i) {
			result.add(function.run(ai, null, get(i), (long) i, this));
		}
		return result;
	}

	public ArrayLeekValue subArray(AI ai, long start, long end) throws LeekRunException {
		start = Math.max(0, start);
		end = Math.min(size(), end);
		int size = (int) (end - start);
		ai.ops(1 + size);
		var result = new ArrayLeekValue(size);
		for (int i = (int) start; i < end; ++i) {
			result.add(get(i));
		}
		return result;
	}

	public boolean isEmpty(AI ai) {
		return size() == 0;
	}

	public Object arrayIter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + size());
		for (int i = 0; i < size(); ++i) {
			function.run(ai, null, get(i), (long) i, this);
		}
		return null;
	}

	public Object arrayFoldLeft(AI ai, FunctionLeekValue function, Object object) throws LeekRunException {
		ai.ops(1 + 2 * size());
		Object r = object;
		for (int i = 0; i < size(); ++i) {
			r = function.run(ai, null, r, get(i), (long) i, this);
		}
		return r;
	}

	public Object arrayFoldRight(AI ai, FunctionLeekValue function, Object object) throws LeekRunException {
		ai.ops(1 + 2 * size());
		Object r = object;
		for (int i = size() - 1; i >= 0; --i) {
			r = function.run(ai, null, get(i), r, (long) i, this);
		}
		return r;
	}

	public ArrayLeekValue arrayPartition(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		var r1 = new ArrayLeekValue();
		var r2 = new ArrayLeekValue();
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			boolean b = ai.bool(function.run(ai, null, v, (long) i, this));
			if (b) {
				r1.add(v);
			} else {
				r2.add(v);
			}
		}
		return new ArrayLeekValue(new Object[] { r1, r2 });
	}

	public ArrayLeekValue arrayFlatten(AI ai) throws LeekRunException {
		return arrayFlatten(ai, 1);
	}

	public ArrayLeekValue arrayFlatten(AI ai, long depth) throws LeekRunException {
		var r = new ArrayLeekValue();
		flatten_rec(ai, this, r, depth);
		return r;
	}

	public void flatten_rec(AI ai, ArrayLeekValue array, ArrayLeekValue result, long depth) throws LeekRunException {
		ai.ops(1 + 2 * size());
		for (var value : array) {
			if (value instanceof ArrayLeekValue && depth > 0) {
				flatten_rec(ai, (ArrayLeekValue) value, result, depth - 1);
			} else {
				result.add(value);
			}
		}
	}

	public ArrayLeekValue arrayFilter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		var result = new ArrayLeekValue();
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (ai.bool(function.run(ai, null, v, (long) i, this))) {
				result.add(v);
			}
		}
		return result;
	}

	public boolean arraySome(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.addOperationsNoCheck(1);
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (ai.bool(function.run(ai, null, v, (long) i, this))) {
				ai.ops(i);
				return true;
			}
		}
		ai.ops(size());
		return false;
	}

	public boolean arrayEvery(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.addOperationsNoCheck(1);
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (!ai.bool(function.run(ai, null, v, (long) i, this))) {
				ai.ops(i);
				return false;
			}
		}
		ai.ops(size());
		return true;
	}

	public Object arrayRemoveAll(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + size());
		removeIf(v -> value == null ? v == value : value.equals(v));
		return null;
	}

	public MapLeekValue arrayFrequencies(AI ai) throws LeekRunException {
		ai.ops(size() * 3);
		var frequencies = new MapLeekValue();
		for (var value : this) {
			frequencies.merge(value, 1l, (x, y) -> (Long) x + (Long) y);
		}
		return frequencies;
	}

	public ArrayLeekValue arrayChunk(AI ai, long size) throws LeekRunException {
		ai.ops(size() * 3);
		int isize = (int) Math.max(1, Math.min(size(), size));
		var chunks = new ArrayLeekValue();
		int n = (int) Math.ceil((float) size() / isize);
		for (var c = 0; c < n; ++c) {
			int to = Math.min(size(), (c + 1) * isize);
			var chunk = new ArrayLeekValue(subList(c * isize, to));
			chunks.add(chunk);
		}
		return chunks;
	}

	public ArrayLeekValue arrayUnique(AI ai) throws LeekRunException {
		ai.ops(size() * 3);
		var set = new HashSet<Object>();
		for (var value : this) {
			set.add(value);
		}
		return new ArrayLeekValue(set.toArray());
	}

	public ArrayLeekValue arrayRandom(AI ai, long count) throws LeekRunException {
		ai.ops(size());
		var result = (ArrayLeekValue) clone();
		shuffle(ai);
		result.removeRange(Math.max(0, Math.min((int) count, size())), size());
		return result;
	}

	public boolean inArray(AI ai, Object value) throws LeekRunException {
		ai.addOperationsNoCheck(1);
		for (int i = 0; i < size(); ++i) {
			var e = get(i);
			if (value == null ? e == value : value.equals(e)) {
				ai.ops(i);
				return true;
			}
		}
		ai.ops(size());
		return false;
	}

	public Object pop(AI ai) throws LeekRunException {
		if (size() == 0) {
			wrongIndexError(ai, 0);
			return null;
		}
		return remove(size() - 1);
	}

	public Object shift(AI ai) throws LeekRunException {
		ai.ops(1 + size());
		if (size() == 0) {
			wrongIndexError(ai, 0);
			return null;
		}
		return remove(0);
	}

	public ArrayLeekValue arrayConcat(AI ai, ArrayLeekValue other) throws LeekRunException {
		return (ArrayLeekValue) ai.add(this, other);
	}

	public ArrayLeekValue arrayClear(AI ai) {
		clear();
		return this;
	}

	public String toString(AI ai, Set<Object> visited) throws LeekRunException {

		ai.ops(1 + size() * 2);

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < size(); ++i) {
			var value = get(i);
			if (i > 0)
				sb.append(", ");
			if (visited.contains(value)) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(value)) {
					visited.add(value);
				}
				sb.append(ai.getString(value, visited));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public boolean eq(AI ai, ArrayLeekValue array) throws LeekRunException {

		ai.ops(1);

		// On commence par vérifier la taille
		if (size() != array.size())
			return false;
		if (size() == 0)
			return true;

		ai.ops(size());

		// On va comparer chaque élément 1 à 1
		for (int i = 0; i < size(); ++i) {
			if (!ai.eq(get(i), array.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public boolean equals(Object array) {
		return this == array;
	}

	public int hashCode() {
		return 0;
	}

	public int hashCodeRec(ArrayLeekValue array) {
		int hashCode = 1;
		for (var e : this) {
			var eh = 0;
			if (e != this && e instanceof ArrayLeekValue) {

			}
			hashCode = 31 * hashCode + eh;
		}
		return hashCode;
	}

	public Iterator<Entry<Object, Object>> genericIterator() {
		return new ArrayIterator(this);
	}
}
