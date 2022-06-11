package leekscript.runner.values;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.common.Error;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class ArrayLeekValue extends ArrayList<Object> implements GenericArrayLeekValue {

	private static final int ARRAY_CELL_ACCESS_OPERATIONS = 1;

	public final static int ASC = 1;
	public final static int DESC = 2;
	public final static int RANDOM = 3;

	private static class ElementComparator implements Comparator<Object> {

		private final int mOrder;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

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
			int type1 = LeekValueManager.getType(v1);
			int type2 = LeekValueManager.getType(v2);
			if (type1 < type2)
				return -1;
			else if (type1 > type2)
				return 1;
			if (type1 == LeekValue.BOOLEAN) {
				if ((Boolean) v1 == (Boolean) v2)
					return 0;
				else if ((Boolean) v1)
					return 1;
				else
					return -1;
			} else if (type1 == LeekValue.NUMBER) {
				var d = ((Number) v2).doubleValue();
				if (((Number) v1).doubleValue() == d)
					return 0;
				else if (((Number) v1).doubleValue() < d)
					return -1;
				else
					return 1;
			} else if (type1 == LeekValue.STRING) {
				return ((String) v1).compareTo((String) v2);
			} else if (type1 == LeekValue.ARRAY) {
				var a = (LegacyArrayLeekValue) v2;
				if (((LegacyArrayLeekValue) v1).size() == a.size())
					return 0;
				else if (((LegacyArrayLeekValue) v1).size() < a.size())
					return -1;
				else
					return 1;
			} else {
				return 0;
			}
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
			var e = new AbstractMap.SimpleEntry<Object, Object>(i, array.get(i));
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

	public Object put(AI ai, Object keyValue, Object value) throws LeekRunException {
		if (value instanceof Long) {
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
		int i = ai.integer(key);
		try {
			var previous_value = get(i);
			set(i, ai.add(previous_value, 1));
			return previous_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_pre_inc(AI ai, Object key) throws LeekRunException {
		int i = ai.integer(key);
		try {
			var new_value = ai.add(get(i), 1);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_dec(AI ai, Object key) throws LeekRunException {
		int i = ai.integer(key);
		try {
			var previous_value = get(i);
			set(i, ai.sub(previous_value, 1));
			return previous_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_pre_dec(AI ai, Object key) throws LeekRunException {
		int i = ai.integer(key);
		try {
			var new_value = ai.sub(get(i), 1);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object put_add_eq(AI ai, Object key, Object value) throws LeekRunException {
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
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
		int i = ai.integer(key);
		try {
			var new_value = ai.ushr(get(i), value);
			set(i, new_value);
			return new_value;
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public void removeByKey(AI ai, Object value) throws LeekRunException {
		remove(ai, value);
	}

	public void shuffle(AI ai) throws LeekRunException {
		sort(ai, RANDOM);
	}

	public String join(AI ai, String sep) throws LeekRunException {
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

	public JSON toJSON(AI ai) throws LeekRunException {
		return toJSON(ai, new HashSet<>());
	}

	public JSON toJSON(AI ai, Set<Object> visited) throws LeekRunException {
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
		int i = ai.integer(key);
		try {
			return get(i);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	public Object getOrDefault(AI ai, Object key, Object defaultValue) throws LeekRunException {
		ai.addOperationsNoCheck(ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS);
		int i = ai.integer(key);
		try {
			return get(i);
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

	/**
	 * Retourne la clé associé à une valeur
	 *
	 * @param value
	 *            Valeur à rechercher
	 * @param pos
	 * @return Clé associée à la valeur ou null si la valeur n'existe pas
	 * @throws LeekRunException
	 */
	public Object search(AI ai, Object value, int pos) throws LeekRunException {
		for (int i = pos; i < size(); ++i) {
			if (get(i) == value) return i;
		}
		return -1;
	}

	public Object removeIndex(AI ai, int index) throws LeekRunException {
		return remove(index);
	}

	/**
	 * Supprime un objet par sa clé
	 *
	 * @param key
	 *            Clé à supprimer
	 * @throws LeekRunException
	 */
	public Object remove(AI ai, Object key) throws LeekRunException {
		int i = ai.integer(key);
		try {
			return remove(i);
		} catch (IndexOutOfBoundsException e) {
			wrongIndexError(ai, i);
			return null;
		}
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public void sort(AI ai, int comparator) throws LeekRunException {
		if (comparator == RANDOM) {
			Collections.shuffle(this, new Random(ai.getRandom().getInt(0, Integer.MAX_VALUE - 1)));
		} else {
			Collections.sort(this, new ElementComparator(comparator));
		}
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public void sort(AI ai, Comparator<Object> comparator) throws LeekRunException {
		Collections.sort(this, comparator);
	}

	public ArrayLeekValue sort(AI ai, FunctionLeekValue function) throws LeekRunException {
		var result = new ArrayLeekValue(ai, this, 1);
		Collections.sort(result, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				try {
					return ai.integer(function.execute(ai, o1, o2));
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
	public void reverse() {
		Collections.reverse(this);
	}

	public void removeObject(AI ai, Object value) throws LeekRunException {
		remove(value);
	}

	/**
	 * Ajouter un élément à la fin du array
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws LeekRunException
	 */
	@Override
	public void push(AI ai, Object value) throws LeekRunException {
		if (value instanceof Long) {
			throw new LeekRunException(LeekRunException.INVALID_VALUE, value);
		}
		add(value);
	}

	/**
	 * Ajouter un élément au début du array (décale les index numériques)
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws LeekRunException
	 */
	public void unshift(AI ai, Object value) throws LeekRunException {
		add(0, value);
	}

	public void insert(AI ai, int position, Object value) throws LeekRunException {
		add(position, value);
	}

	public void fill(Object value, int size) {
		if (size >= size()) { // Plus petit ou égal
			Collections.fill(this, value);
			int to_add = size - size();
			for (int i = 0; i < to_add; ++i) add(value);
		} else { // Plus grand
			this.ensureCapacity(size);
			for (int i = 0; i < size; ++i) {
				set(i, value);
			}
		}
	}

	public ArrayLeekValue map(AI ai, FunctionLeekValue function) throws LeekRunException {
		var result = new ArrayLeekValue(size());
		for (int i = 0; i < size(); ++i) {
			result.add(function.execute(ai, get(i), i, this));
		}
		return result;
	}

	public ArrayLeekValue subArray(int start, int end) {
		start = Math.max(0, start);
		end = Math.min(size(), end);
		int size = end - start;
		var result = new ArrayLeekValue(size);
		for (int i = start; i < end; ++i) {
			result.add(get(i));
		}
		return result;
	}

	public void iter(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (int i = 0; i < size(); ++i) {
			function.execute(ai, get(i), i, this);
		}
	}

	public Object foldLeft(AI ai, FunctionLeekValue function, Object object) throws LeekRunException {
		Object r = object;
		for (int i = 0; i < size(); ++i) {
			r = function.execute(ai, r, get(i), i, this);
		}
		return r;
	}

	public Object foldRight(AI ai, FunctionLeekValue function, Object object) throws LeekRunException {
		Object r = object;
		for (int i = size() - 1; i >= 0; --i) {
			r = function.execute(ai, get(i), r, i, this);
		}
		return r;
	}

	public Object partition(AI ai, FunctionLeekValue function) throws LeekRunException {
		var r1 = new ArrayLeekValue();
		var r2 = new ArrayLeekValue();
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			boolean b = ai.bool(function.execute(ai, v, i, this));
			if (b) {
				r1.add(v);
			} else {
				r2.add(v);
			}
		}
		return new ArrayLeekValue(new Object[] { r1, r2 });
	}

	public Object flatten(int depth) throws LeekRunException {
		var r = new ArrayLeekValue();
		flatten_rec(this, r, depth);
		return r;
	}

	public void flatten_rec(ArrayLeekValue array, ArrayLeekValue result, int depth) throws LeekRunException {
		for (var value : array) {
			if (value instanceof ArrayLeekValue && depth > 0) {
				flatten_rec((ArrayLeekValue) value, result, depth - 1);
			} else {
				result.add(value);
			}
		}
	}

	public ArrayLeekValue filter(AI ai, FunctionLeekValue function) throws LeekRunException {
		var result = new ArrayLeekValue();
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (ai.bool(function.execute(ai, v, i, this))) {
				result.add(v);
			}
		}
		return result;
	}

	public boolean some(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (ai.bool(function.execute(ai, v, i, this))) {
				return true;
			}
		}
		return false;
	}

	public boolean every(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (int i = 0; i < size(); ++i) {
			var v = get(i);
			if (!ai.bool(function.execute(ai, v, i, this))) {
				return false;
			}
		}
		return true;
	}

	public void removeAll(Object value) {
		removeIf(v -> v == value);
	}

	public MapLeekValue frequencies() {
		var frequencies = new MapLeekValue();
		for (var value : this) {
			frequencies.merge(value, 1, (x, y) -> (Integer) x + (Integer) y);
		}
		return frequencies;
	}

	public ArrayLeekValue chunk(int size) {
		size = Math.max(1, Math.min(size(), size));
		var chunks = new ArrayLeekValue();
		int n = (int) Math.ceil((float) size() / size);
		for (var c = 0; c < n; ++c) {
			int to = Math.min(size(), (c + 1) * size);
			var chunk = new ArrayLeekValue(subList(c * size, to));
			chunks.add(chunk);
		}
		return chunks;
	}

	public ArrayLeekValue unique(AI ai) {
		var set = new HashSet<Object>();
		for (var value : this) {
			set.add(value);
		}
		return new ArrayLeekValue(set.toArray());
	}

	public ArrayLeekValue random(AI ai, int count) throws LeekRunException {
		var result = (ArrayLeekValue) clone();
		shuffle(ai);
		result.removeRange(Math.max(0, Math.min(count, size())), size());
		return result;
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
