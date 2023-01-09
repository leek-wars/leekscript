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

	private static final int READ_OPERATIONS = 1;
	private static final int WRITE_OPERATIONS = 2;
	private static final int MAX_SIZE = 10_000_000;

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

	private final AI ai;

	public ArrayLeekValue(AI ai) {
		this.ai = ai;
	}

	public ArrayLeekValue(AI ai, int capacity) {
		super(Math.min(MAX_SIZE, capacity));
		this.ai = ai;
	}

	public ArrayLeekValue(AI ai, Object values[]) throws LeekRunException {
		this.ai = ai;
		for (var value : values) {
			add(value);
		}
		ai.increaseRAM(values.length);
	}

	public ArrayLeekValue(AI ai, List<Object> values) throws LeekRunException {
		super(values);
		this.ai = ai;
		ai.increaseRAM(values.size());
	}

	public ArrayLeekValue(AI ai, ArrayLeekValue array) throws LeekRunException {
		this(ai, array, 1);
	}

	public ArrayLeekValue(AI ai, ArrayLeekValue array, int level) throws LeekRunException {
		this.ai = ai;
		ai.increaseRAM(array.size());
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = ai.integer(keyValue);
		if (i < 0) i += size();
		try {
			set(i, value);
			return value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_inc(AI ai, Object key) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
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
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.div(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public long put_intdiv_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.intdiv(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
		}
	}

	public Object put_mod_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.mod(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public long put_bor_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.bor(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
		}
	}

	public long put_band_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.band(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
		}
	}

	public long put_bxor_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.bxor(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
		}
	}

	public long put_shl_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.shl(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
		}
	}

	public long put_shr_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.shr(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
		}
	}

	public long put_ushr_eq(AI ai, Object key, Object value) throws LeekRunException {
		ai.opsNoCheck(ArrayLeekValue.WRITE_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			var new_value = ai.ushr(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return 0;
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
			sb.append(ai.string(val));
		}
		return sb.toString();
	}

	public String getString(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited);
	}

	public Object add_eq(AI ai, Object value) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			pushAll(ai, (ArrayLeekValue) value);
		} else {
			push(ai, value);
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
		ai.opsNoCheck(ArrayLeekValue.READ_OPERATIONS);
		int i = (int) ai.integer(key);
		if (i < 0) i += size();
		try {
			return get(i);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object arrayGet(AI ai, long index) throws LeekRunException {
		return arrayGet(ai, index, null);
	}

	public Object arrayGet(AI ai, long index, Object defaultValue) throws LeekRunException {
		if (index < 0) index += size();
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
		ai.opsNoCheck(ArrayLeekValue.READ_OPERATIONS);
		if (index < 0) index += size();
		try {
			return get(index);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, index);
			return null;
		}
	}

	public long search(AI ai, Object value) throws LeekRunException {
		ai.opsNoCheck(1);
		for (var i = 0; i < size(); ++i) {
			var e = get(i);
			if (ai.equals_equals(value, e)) {
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
		ai.opsNoCheck(1);
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
			var result = remove((int) key);
			ai.decreaseRAM(1);
			return result;
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
					return ai.signum(function.run(ai, null, o1, o2));
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
		if (remove(value)) {
			ai.decreaseRAM(1);
		}
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
 		ai.increaseRAM(1);
		add(value);
		return null;
	}

	public Object pushNoClone(AI ai, Object value) throws LeekRunException {
		ai.increaseRAM(1);
		add(value);
		return null;
	}

	public Object pushAll(AI ai, ArrayLeekValue other) throws LeekRunException {
		ai.increaseRAM(other.size());
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
		ai.increaseRAM(1);
		ai.ops(1 + size());
		add(0, value);
		return null;
	}

	public Object insert(AI ai, Object value, long position) throws LeekRunException {
		if (position < 0) position += size();
		try {
			int shifted = size() - (int) position;
			ai.ops(1 + Math.max(0, shifted));
			add((int) position, value);
			ai.increaseRAM(1);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, (int) position);
		}
		return null;
	}

	public Object fill(AI ai, Object value) throws LeekRunException {
		return fill(ai, value, size());
	}

	public Object fill(AI ai, Object value, long size) throws LeekRunException {
		ai.ops(1, Math.max(0, (int) size));
		if (size >= size()) { // Agrandissement
			var to_add = (int) size - size();
			ai.increaseRAM(to_add);
			Collections.fill(this, value);
			ensureCapacity((int) size);
			for (int i = 0; i < to_add; ++i) {
				add(value);
			}
		} else {
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
		var result = new ArrayLeekValue(ai, size());
		for (int i = 0; i < size(); ++i) {
			result.add(function.run(ai, null, get(i), (long) i, this));
		}
		ai.increaseRAM(size());
		return result;
	}

	public ArrayLeekValue arraySlice(AI ai, Object start) throws LeekRunException {
		return arraySlice(ai, start, null, 1);
	}

	public ArrayLeekValue arraySlice(AI ai, Object start, Object end) throws LeekRunException {
		return arraySlice(ai, start, end, 1);
	}

	public ArrayLeekValue arraySlice(AI ai, Object startValue, Object endValue, long stride) throws LeekRunException {
		if (stride == 0) stride = 1;
		int start, end;
		if (startValue == null) {
			start = stride > 0 ? 0 : size() - 1;
		} else {
			start = ai.integer(startValue);
			if (start < 0) start += size();
			if (stride > 0) {
				start = Math.max(0, start);
			} else {
				start = Math.min(size() - 1, start);
			}
		}
		if (endValue == null) {
			end = stride > 0 ? size() : -1;
		} else {
			end = ai.integer(endValue);
			if (end < 0) end += size();
			if (stride > 0) {
				end = Math.min(size(), end);
			} else {
				end = Math.max(-1, end);
			}
		}
		int size = (int) Math.abs(end - start) / (int) Math.abs(stride);
		ai.ops(1 + size);
		var result = new ArrayLeekValue(ai, size);
		// System.out.println("slice start=" + start + " end=" + end + " stride=" + stride + " size=" + size);
		if (stride > 0) {
			for (int i = (int) start; i < end; i += stride) {
				result.add(get(i));
			}
		} else {
			for (int i = (int) start; i > end; i += stride) {
				result.add(get(i));
			}
		}
		ai.increaseRAM(size);
		assert(size == result.size());
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
		var r1 = new ArrayLeekValue(ai);
		var r2 = new ArrayLeekValue(ai);
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			boolean b = ai.bool(function.run(ai, null, v, (long) i, this));
			if (b) {
				r1.add(v);
			} else {
				r2.add(v);
			}
		}
		ai.increaseRAM(size());
		return new ArrayLeekValue(ai, new Object[] { r1, r2 });
	}

	public ArrayLeekValue arrayFlatten(AI ai) throws LeekRunException {
		return arrayFlatten(ai, 1);
	}

	public ArrayLeekValue arrayFlatten(AI ai, long depth) throws LeekRunException {
		var r = new ArrayLeekValue(ai);
		flatten_rec(ai, this, r, depth);
		return r;
	}

	public void flatten_rec(AI ai, ArrayLeekValue array, ArrayLeekValue result, long depth) throws LeekRunException {
		ai.ops(1 + 2 * size());
		for (var value : array) {
			if (value instanceof ArrayLeekValue && depth > 0) {
				flatten_rec(ai, (ArrayLeekValue) value, result, depth - 1);
			} else {
				result.push(ai, value);
			}
		}
	}

	public ArrayLeekValue arrayFilter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		var result = new ArrayLeekValue(ai);
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (ai.bool(function.run(ai, null, v, (long) i, this))) {
				result.add(v);
			}
		}
		ai.increaseRAM(result.size());
		return result;
	}

	public boolean arraySome(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.opsNoCheck(1);
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
		ai.opsNoCheck(1);
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
		var sizeBefore = size();
		removeIf(v -> value == null ? v == value : value.equals(v));
		ai.decreaseRAM(sizeBefore - size());
		return null;
	}

	public MapLeekValue arrayFrequencies(AI ai) throws LeekRunException {
		ai.ops(size() * 3);
		var frequencies = new MapLeekValue(ai);
		for (var value : this) {
			frequencies.merge(value, 1l, (x, y) -> (Long) x + (Long) y);
		}
		ai.increaseRAM(frequencies.size());
		return frequencies;
	}

	public ArrayLeekValue arrayChunk(AI ai, long size) throws LeekRunException {
		ai.ops(size() * 3);
		int isize = (int) Math.max(1, Math.min(size(), size));
		var chunks = new ArrayLeekValue(ai);
		int n = (int) Math.ceil((float) size() / isize);
		for (var c = 0; c < n; ++c) {
			int to = Math.min(size(), (c + 1) * isize);
			var chunk = new ArrayLeekValue(ai, subList(c * isize, to));
			chunks.push(ai, chunk);
		}
		return chunks;
	}

	public ArrayLeekValue arrayUnique(AI ai) throws LeekRunException {
		ai.ops(size() * 3);
		var set = new HashSet<Object>();
		for (var value : this) {
			set.add(value);
		}
		return new ArrayLeekValue(ai, set.toArray());
	}

	public ArrayLeekValue arrayRandom(AI ai, long count) throws LeekRunException {
		ai.ops(5 + size());
		var result = new ArrayLeekValue(ai, this);
		result.shuffle(ai);
		var finalCount = Math.max(0, Math.min((int) count, size()));
		result.removeRange(finalCount, size());
		ai.decreaseRAM(size() - finalCount);
		return result;
	}

	public boolean inArray(AI ai, Object value) throws LeekRunException {
		ai.opsNoCheck(1);
		for (int i = 0; i < size(); ++i) {
			if (ai.equals_equals(get(i), value)) {
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
		ai.decreaseRAM(1);
		return remove(size() - 1);
	}

	public Object shift(AI ai) throws LeekRunException {
		ai.ops(1 + size());
		if (size() == 0) {
			wrongIndexError(ai, 0);
			return null;
		}
		ai.decreaseRAM(1);
		return remove(0);
	}

	public ArrayLeekValue arrayConcat(AI ai, ArrayLeekValue other) throws LeekRunException {
		var result = new ArrayLeekValue(ai, size() + other.size());
		result.pushAll(ai, this);
		result.pushAll(ai, other);
		return result;
	}

	public ArrayLeekValue arrayClear(AI ai) {
		ai.decreaseRAM(size());
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
					// ai.ops(10);
					visited.add(value);
				}
				sb.append(ai.export(value, visited));
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
	@SuppressWarnings("deprecated")
	protected void finalize() throws Throwable {
		super.finalize();
		ai.decreaseRAM(size());
	}

	@Override
	public boolean equals(Object object) {
		return object == this;
	}

	public int hashCode() {
		int hashCode = 1;
		hashCode = 31 * hashCode + size();
		for (var e : this) {
			var eh = 0;
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
