package leekscript.runner.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueComparator;
import leekscript.runner.LeekValueManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class LegacyArrayLeekValue implements Iterable<Entry<Object, Object>>, GenericArrayLeekValue, GenericMapLeekValue {

	public final static int ARRAY_CELL_ACCESS_OPERATIONS = 2;
	public final static int ARRAY_CELL_CREATE_OPERATIONS = 2; // + sqrt(size) / 5

	private final static int START_CAPACITY = 8;
	private final static int MAX_CAPACITY = 32000;

	public final static int ASC = 0;
	public final static int DESC = 1;
	public final static int RANDOM = 2;
	public final static int ASC_A = 3;
	public final static int DESC_A = 4;
	public final static int ASC_K = 5;
	public final static int DESC_K = 6;

	public static class ArrayIterator implements Iterator<Entry<Object, Object>> {

		Element mElement;

		public ArrayIterator(Element head) {
			mElement = head;
		}

		@Override
		public boolean hasNext() {
			return mElement != null;
		}

		@Override
		public Entry<Object, Object> next() {
			var v = mElement;
			if (mElement != null)
				mElement = mElement.next;
			return v;
		}

		public Object getKey(AI ai) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return mElement.getKey();
			} else {
				return LeekOperations.clone(ai, mElement.getKey());
			}
		}

		public Object key() {
			return mElement.keyObject();
		}

		public Object getValue(AI ai) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return mElement.getValue();
			} else {
				return LeekOperations.clone(ai, mElement.getValue());
			}
		}

		public Object getValue() {
			return mElement.getValue();
		}

		public Object getKeyRef() throws LeekRunException {
			return mElement.getKey();
		}

		public Object getValueBox() throws LeekRunException {
			return mElement.valueBox();
		}

		public void setValue(AI ai, Box value) throws LeekRunException {
			mElement.setValue(ai, value);
		}
	}

	private static class ElementComparatorV1 implements Comparator<Element> {

		private final int mOrder;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

		public ElementComparatorV1(int order) {
			mOrder = order;
		}

		@Override
		public int compare(Element v1, Element v2) {
			try {
				if (mOrder == SORT_ASC)
					return compareAsc(v1.value.getValue(), v2.value.getValue());
				else if (mOrder == SORT_DESC)
					return compareAsc(v2.value.getValue(), v1.value.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

		public int compareAsc(Object v1, Object v2) throws LeekRunException {
			var type1 = LeekValueManager.getV1Type(v1);
			var type2 = LeekValueManager.getV1Type(v2);
			if (type1 < type2)
				return -1;
			else if (type1 > type2)
				return 1;
			if (type1 == LeekValue.BOOLEAN_V1) {
				if ((Boolean) v1 == (Boolean) v2)
					return 0;
				else if ((Boolean) v1)
					return 1;
				else
					return -1;
			} else if (type1 == LeekValue.NUMBER_V1) {
				var d = ((Number) v2).doubleValue();
				if (((Number) v1).doubleValue() == d)
					return 0;
				else if (((Number) v1).doubleValue() < d)
					return -1;
				else
					return 1;
			} else if (type1 == LeekValue.STRING_V1) {
				return ((String) v1).compareTo((String) v2);
			} else if (type1 == LeekValue.ARRAY_V1) {
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

	private static class ElementComparator implements Comparator<Element> {

		private final int mOrder;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

		public ElementComparator(int order) {
			mOrder = order;
		}

		@Override
		public int compare(Element v1, Element v2) {
			try {
			if (mOrder == SORT_ASC)
				return compareAsc(v1.value.getValue(), v2.value.getValue());
			else if (mOrder == SORT_DESC)
				return compareAsc(v2.value.getValue(), v1.value.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

		public int compareAsc(Object v1, Object v2) throws LeekRunException {
			var type1 = LeekValueManager.getType(v1);
			var type2 = LeekValueManager.getType(v2);
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

	private static class KeyComparator implements Comparator<Element> {

		private final int mOrder;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

		public KeyComparator(int order) {
			mOrder = order;
		}

		@Override
		public int compare(Element v1, Element v2) {
			if (mOrder == SORT_ASC)
				return compareAsc(v1.key, v2.key);
			else if (mOrder == SORT_DESC)
				return compareAsc(v2.key, v1.key);
			return 0;
		}

		public int compareAsc(Object v1, Object v2) {
			if (v1 instanceof String && v2 instanceof String)
				return ((String) v1).compareTo((String) v2);
			else if (v1 instanceof Long && v2 instanceof Long)
				return ((Long) v1).compareTo((Long) v2);
			else if (v1 instanceof Long)
				return -1;
			return 1;
		}
	}

	private class ReversedPhpIterator implements Iterator<Object> {

		private Element e = mEnd;

		@Override
		public boolean hasNext() {
			return e != null;
		}
		@Override
		public Object next() {
			var v = e.value.getValue();
			if (e != null)
				e = e.prev;
			return v;
		}
		@Override
		public void remove() {}
	}

	public static class Element implements Entry<Object, Object> {

		private Object key;
		private int hash;
		private boolean numeric = false;
		private Box value = null;

		private Element next = null;
		private Element prev = null;

		private Element hashNext = null;

		public Element next() {
			return next;
		}

		@Override
		public Object getKey() {
			return key;
		}

		public Object keyObject() {
			return key;
		}

		@Override
		public Object getValue() {
			return value.getValue();
		}

		public Object valueBox() {
			return value;
		}

		public void setValue(AI ai, Object v) throws LeekRunException {
			value.set(v);
		}

		public String toString() {
			return value.getValue().toString();
		}

		@Override
		public Object setValue(Object arg0) {
			return null;
		}
	}

	private Element mHead = null;
	private Element mEnd = null;
	private long mIndex = 0;
	private int mSize = 0;
	private int capacity = 0;
	private Element[] mTable = null;

	public LegacyArrayLeekValue() {}

	public LegacyArrayLeekValue(AI ai, Object values[]) throws LeekRunException {
		this(ai, values, false);
	}

	public LegacyArrayLeekValue(AI ai, Object values[], boolean isKeyValue) throws LeekRunException {
		if (capacity > 0) {
			initTable(ai, values.length);
		}
		if (isKeyValue) {
			int i = 0;
			while (i < values.length) {
				getOrCreate(ai, values[i]).set(values[i + 1]);
				i += 2;
			}
		} else {
			for (int i = 0; i < values.length; i++) {
				pushNoClone(ai, values[i]);
			}
		}
	}

	public LegacyArrayLeekValue(AI ai, LegacyArrayLeekValue array, int level) throws LeekRunException {
		if (array.size() > 0) {
			initTable(ai, array.size());
			Element e = array.mHead;
			while (e != null) {
				if (ai.getVersion() >= 2) {
					if (level == 1) {
						set(ai, e.key, e.value.getValue());
					} else {
						set(ai, e.key, LeekOperations.clone(ai, e.value.getValue(), level - 1));
					}
				} else {
					set(ai, e.key, LeekOperations.clone(ai, e.value.getValue()));
				}
				e = e.next;
			}
		}
	}

	public Object put(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		if (ai.getVersion() == 1) {
			value = LeekOperations.clone(ai, value);
		}
		set(ai, key, value);
		return value;
	}

	public Object put_inc(AI ai, Object key) throws LeekRunException {
		return getOrCreate(ai, key).increment();
	}

	public Object put_pre_inc(AI ai, Object key) throws LeekRunException {
		return getOrCreate(ai, key).pre_increment();
	}

	public Object put_dec(AI ai, Object key) throws LeekRunException {
		return getOrCreate(ai, key).decrement();
	}

	public Object put_pre_dec(AI ai, Object key) throws LeekRunException {
		return getOrCreate(ai, key).pre_decrement();
	}

	public Object put_add_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).add_eq(value);
	}

	public Object put_sub_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).sub_eq(value);
	}

	public Object put_mul_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).mul_eq(value);
	}

	public Object put_pow_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).pow_eq(value);
	}

	public Object put_div_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).div_eq(value);
	}

	public long put_intdiv_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).intdiv_eq(value);
	}

	public Object put_mod_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).mod_eq(value);
	}

	public long put_bor_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).bor_eq(value);
	}

	public long put_band_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).band_eq(value);
	}

	public long put_bxor_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).bxor_eq(value);
	}

	public long put_shl_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).shl_eq(value);
	}

	public long put_shr_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).shr_eq(value);
	}

	public long put_ushr_eq(AI ai, Object key, Object value) throws LeekRunException {
		return getOrCreate(ai, key).ushr_eq(value);
	}

	private Object transformKey(AI ai, Object key) throws LeekRunException {
		if (key instanceof String || key instanceof ObjectLeekValue) {
			return key;
		} else {
			return ai.longint(key);
		}
	}

	public Object get(AI ai, int value) throws LeekRunException {
		return get(ai, (long) value);
	}

	public Object remove(AI ai, long index) throws LeekRunException {
		int numMoved = size() - (int) index - 1;
		ai.ops(1 + Math.max(0, numMoved));
		return removeIndex(ai, (int) index);
	}

	public Object removeKey(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + size());
		remove(ai, value);
		return null;
	}

	public Object shuffle(AI ai) throws LeekRunException {
		sort(ai, RANDOM);
		return null;
	}

	public String join(AI ai, String sep) throws LeekRunException {
		ai.ops(1 + size() * 2);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (var val : this) {
			if (!first)
				sb.append(sep);
			else
				first = false;
			sb.append(ai.string(val.getValue()));
		}
		return sb.toString();
	}

	public String export(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited, true);
	}

	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return toString(ai, visited, false);
	}

	public boolean equals(AI ai, Object comp) throws LeekRunException {
		if (comp instanceof LegacyArrayLeekValue) {
			return equals(ai, ((LegacyArrayLeekValue) comp));
		} else if (size() == 1) { // Si y'a un seul élément dans le tableau
			var firstValue = getHeadElement().getValue();
			if (firstValue == null && comp == null) {
				return ai.getVersion() == 1; // Bug in LS1, [null] == null
			}
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
		if (value instanceof LegacyArrayLeekValue) {
			var iterator = ((LegacyArrayLeekValue) value).iterator();
			while (iterator.hasNext()) {
				if (iterator.key() instanceof String || iterator.key() instanceof ObjectLeekValue)
					getOrCreate(ai, ai.string(iterator.getKey(ai))).set(iterator.getValue(ai));
				else
					push(ai, iterator.getValue(ai));
				iterator.next();
			}
		} else {
			push(ai, value);
		}
		return this;
	}

	public ArrayIterator iterator() {
		return new ArrayIterator(getHeadElement());
	}

	public Iterator<Object> getReversedIterator() {
		return reversedIterator();
	}

	public JSON toJSON(AI ai) throws LeekRunException {
		return toJSON(ai, new HashSet<>());
	}

	public JSON toJSON(AI ai, HashSet<Object> visited) throws LeekRunException {
		visited.add(this);

		if (isAssociative()) {
			JSONObject o = new JSONObject();
			var i = iterator();
			while (i.hasNext()) {
				var v = i.getValue(ai);
				if (!visited.contains(v)) {
					if (!ai.isPrimitive(v)) {
						visited.add(v);
					}
					o.put(i.key().toString(), ai.toJSON(v, visited));
				}
				i.next();
			}
			return o;
		} else {
			var a = new JSONArray();
			for (var entry : this) {
				var v = entry.getValue();
				if (!visited.contains(v)) {
					if (!ai.isPrimitive(v)) {
						visited.add(v);
					}
					a.add(ai.toJSON(v, visited));
				}
			}
			return a;
		}
	}

	@Override
	public String toString() {
		var r = "[";
		boolean first = true;
		var i = iterator();
		while (i.hasNext()) {
			if (first) first = false;
			else r += ", ";
			if (isAssociative()) {
				r += i.key().toString() + ": ";
			}
			r += i.getValue();
			i.next();
		}
		return r + "]";
	}

	private void initTable(AI ai, int capacity) throws LeekRunException {
		int realCapacity = Math.max(START_CAPACITY, capacity);
		// System.out.println("ops initTable");
		ai.opsNoCheck(realCapacity / 5);
		this.capacity = realCapacity;
		mTable = new Element[realCapacity];
	}

	private void growCapacity(AI ai) throws LeekRunException {

		if (capacity == MAX_CAPACITY) return;

		// Copy in a new array
		this.capacity = Math.min(Math.max(START_CAPACITY, capacity * 2), MAX_CAPACITY);
		mTable = new Element[this.capacity];
		Element e = mHead;
		mHead = null;
		mEnd = null;
		mSize = 0;
		while (e != null) {
			set(ai, e.key, e.value.getValue());
			e = e.next;
		}
	}

	public Element getHeadElement() {
		return mHead;
	}

	/**
	 * Retourne le nombre d'éléments dans le tableau
	 *
	 * @return Nombre d'éléments
	 */
	public int size() {
		return mSize;
	}

	public long count(AI ai) {
		return mSize;
	}

	/**
	 * Retourne la valeur à pour une clé donnée
	 *
	 * @param key
	 *            Clé dont on veut la valeur
	 * @return Valeur à la clé donnée
	 * @throws LeekRunException
	 */
	public Object get(AI ai, Object keyValue) throws LeekRunException {
		var key = transformKey(ai, keyValue);
		Element e = getElement(ai, key);
		return e == null ? null : e.value.getValue();
	}

	public Box getBox(AI ai, Object keyValue) throws LeekRunException {
		var key = transformKey(ai, keyValue);
		Element e = getElement(ai, key);
		return e == null ? new Box(ai) : e.value;
	}

	/**
	 * Vérifie si le tableau contient une valeur donnée
	 *
	 * @param value
	 *            Valeur à rechercher
	 * @return True si la valeur existe dans le tableau
	 * @throws LeekRunException
	 */
	public boolean inArray(AI ai, Object value) throws LeekRunException {
		ai.opsNoCheck(1);
		Element e = mHead;
		int i = 0;
		while (e != null) {
			if (ai.eq(e.value.getValue(), value)) {
				ai.ops(i);
				return true;
			}
			e = e.next;
			i++;
		}
		ai.ops(size());
		return false;
	}

	public Object search(AI ai, Object value) throws LeekRunException {
		ai.opsNoCheck(1);
		Element e = mHead;
		int i = 0;
		while (e != null) {
			if (ai.equals_equals(e.value.getValue(), value)) {
				ai.ops(i);
				return e.key;
			}
			e = e.next;
			i++;
		}
		ai.ops(size());
		return null;
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
	public Object search(AI ai, Object value, long pos) throws LeekRunException {
		Element e = mHead;
		int p = 0;
		while (e != null) {
			if (p >= pos && LeekValueManager.getType(e.value) == LeekValueManager.getType(value) && ai.eq(e.value.getValue(), value)) {
				return e.key;
			}
			e = e.next;
			p++;
		}
		return null;
	}

	public Object removeIndex(AI ai, int index) throws LeekRunException {
		if (index >= mSize)
			return null;
		Element e = mHead;
		int p = 0;
		while (e != null) {
			if (p == index) {
				remove(ai, e.key);
				reindex(ai);
				return e.value.getValue();
			}
			e = e.next;
			p++;
		}
		return null;
	}

	/**
	 * Supprime un objet par sa clé
	 *
	 * @param key
	 *            Clé à supprimer
	 * @throws LeekRunException
	 */
	public void remove(AI ai, Object key) throws LeekRunException {
		Element e = getElement(ai, key);
		if (e == null)
			return;

		destroyElement(e);
		// Si l'élément existe on l'enleve de la hashmap
		removeFromHashmap(ai, e);
		// Puis on refait le chainage sans l'élément
		if (e.prev == null)
			mHead = e.next;
		else
			e.prev.next = e.next;

		if (e.next == null)
			mEnd = e.prev;
		else
			e.next.prev = e.prev;
	}

	public Object arrayMin(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		if (size() > 0) {
			var comp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
			var iterator = iterator();
			Object min_c = iterator.next().getValue();
			while (iterator.hasNext()) {
				var value = iterator.next().getValue();
				if (comp.compare(value, min_c) == -1) {
					min_c = value;
				}
			}
			return LeekOperations.clone(ai, min_c);
		}
		return null;
	}


	public Object arrayMax(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		Object min_c = null;
		var mincomp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
		for (var val : this) {
			if (min_c == null)
				min_c = val.getValue();
			else if (mincomp.compare(val.getValue(), min_c) == 1)
				min_c = val.getValue();
		}
		if (min_c == null)
			return null;
		else
			return LeekOperations.clone(ai, min_c);
	}

	/**
	 * Trie le tableau
	 *
	 * @throws LeekRunException
	 */
	public Object sort(AI ai) throws LeekRunException {
		return sort(ai, ASC);
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public Object sort(AI ai, long comparator) throws LeekRunException {
		ai.ops(1 + (int) (5 * size() * Math.log(size())));
		if (mSize == 0) {
			return null;
		}
		// System.out.println("sort " + comparator);
		// création de la liste
		List<Element> liste = new ArrayList<Element>();
		Element elem = mHead;
		while (elem != null) {
			liste.add(elem);
			elem = elem.next;
		}
		// Trie de la liste
		if (comparator == RANDOM)
			Collections.shuffle(liste, new Random(ai.getRandom().getInt(0, Integer.MAX_VALUE - 1)));
		else if (comparator == ASC_K || comparator == DESC_K) {
			Collections.sort(liste, new KeyComparator((comparator == ASC_K) ? ElementComparator.SORT_ASC : ElementComparator.SORT_DESC));
		} else {
			if (ai.getVersion() == 1) {
				Collections.sort(liste, new ElementComparatorV1((comparator == ASC || comparator == ASC_A) ? ElementComparator.SORT_ASC : ElementComparator.SORT_DESC));
			} else {
				Collections.sort(liste, new ElementComparator((comparator == ASC || comparator == ASC_A) ? ElementComparator.SORT_ASC : ElementComparator.SORT_DESC));
			}
		}

		// Mise en place de la liste
		mHead = liste.get(0);
		Element prev = null;
		for (int i = 0; i < liste.size(); i++) {
			Element cur = liste.get(i);
			if (prev != null)
				prev.next = cur;
			cur.prev = prev;
			prev = cur;
		}
		prev.next = null;
		mEnd = prev;
		if (comparator == ASC || comparator == RANDOM || comparator == DESC) {
			this.reindex(ai);
		}
		return null;
	}

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public void sort(AI ai, Comparator<Element> comparator) throws LeekRunException {
		if (mSize == 0)
			return;

		Element e = mHead;
		boolean isInOrder = true;
		var value = 0l;
		while (e != null) {
			if (!(e.key instanceof Long) || (Long) e.key != value) {
				isInOrder = false;
				break;
			}
			value++;
			e = e.next;
		}

		// création de la liste
		List<Element> liste = new ArrayList<Element>();
		Element elem = mHead;
		while (elem != null) {
			liste.add(elem);
			elem = elem.next;
		}

		// Tri de la liste
		Collections.sort(liste, comparator);

		// Mise en place de la liste
		mHead = liste.get(0);
		Element prev = null;
		for (int i = 0; i < liste.size(); i++) {
			Element cur = liste.get(i);
			if (prev != null)
				prev.next = cur;
			cur.prev = prev;
			prev = cur;
		}
		prev.next = null;
		mEnd = prev;
		if (isInOrder)
			reindex(ai);
	}

	public LegacyArrayLeekValue arraySort(AI ai, final FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + (int) (5 * size() * Math.log(size())));
		try {
			int nb = function.getArgumentsCount();
			if (nb == 2) {
				var array = (LegacyArrayLeekValue) LeekOperations.clone(ai, this);
				array.sort(ai, new Comparator<Element>() {
					@Override
					public int compare(Element o1, Element o2) {
						try {
							return ai.integer(function.run(ai, null, o1.getValue(), o2.getValue()));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				return array;
			} else if (nb == 4) {
				var array = (LegacyArrayLeekValue) LeekOperations.clone(ai, this);
				array.sort(ai, new Comparator<Element>() {
					@Override
					public int compare(Element o1, Element o2) {
						try {
							return ai.integer(function.run(ai, null, o1.getKey(), o1.getValue(), o2.getKey(), o2.getValue()));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				return array;
			}
		} catch (RuntimeException e) {
			if (e.getCause() instanceof LeekRunException) {
				throw (LeekRunException) e.getCause();
			}
		}
		return null;
	}

	/**
	 * Inverse l'ordre
	 *
	 * @throws LeekRunException
	 */
	public Object reverse(AI ai) throws LeekRunException {
		ai.ops(1 + size());
		if (mSize == 0) {
			return null;
		}
		Element prev = null;
		Element current = mEnd;
		Element tmp;
		while (current != null) {
			// Fin de liste
			if (prev == null) {
				mHead = current;
			} else {
				prev.next = current;
			}
			tmp = prev;
			prev = current;
			current = current.prev;
			prev.prev = tmp;
		}
		prev.next = null;
		mEnd = prev;

		reindex(ai);
		return null;
	}

	public Object assocReverse(AI ai) throws LeekRunException {
		ai.ops(1 + size());
		if (mSize == 0)
			return null;
		Element prev = null;
		Element current = mEnd;
		Element tmp;
		while (current != null) {
			// Fin de liste
			if (prev == null) {
				mHead = current;
			} else {
				prev.next = current;
			}
			tmp = prev;
			prev = current;
			current = current.prev;
			prev.prev = tmp;
		}
		prev.next = null;
		mEnd = prev;
		return null;
	}

	public Object removeElement(AI ai, Object value) throws LeekRunException {
		ai.ops(1 + size());
		Element e = mHead;
		while (e != null) {
			if (LeekValueManager.getType(e.value) == LeekValueManager.getType(value) && ai.eq(e.value.getValue(), value)) {
				// On a notre élément à supprimer
				// On l'enleve de la HashMap
				removeFromHashmap(ai, e);
				destroyElement(e);
				// Puis on refait le chainage sans l'élément
				if (e.prev == null)
					mHead = e.next;
				else
					e.prev.next = e.next;

				if (e.next == null)
					mEnd = e.prev;
				else
					e.next.prev = e.prev;
				return null;
			}
			e = e.next;
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
	public Object push(AI ai, Object value) throws LeekRunException {
		if (ai.getVersion() == 1) {
			value = LeekOperations.clone(ai, value);
		}
		return pushNoClone(ai, value);
	}

	public Object pushNoClone(AI ai, Object value) throws LeekRunException {
		if (mSize >= capacity) {
			growCapacity(ai);
		}
		mSize++;
		var key = Long.valueOf(mIndex);
		Element e = createElement(ai, key, value);
		pushElement(e);
		return null;
	}

	public Object pushAll(AI ai, LegacyArrayLeekValue other) throws LeekRunException {
		ai.ops(1 + other.size());
		for (var value : other) {
			push(ai, value.getValue());
		}
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
		if (ai.getVersion() == 1) {
			value = LeekOperations.clone(ai, value);
		}
		if (mSize >= capacity) {
			growCapacity(ai);
		}
		mSize++;
		var key = 0l;
		Element e = createElement(ai, key, value);
		unshiftElement(e);
		reindex(ai);
		return null;
	}

	/**
	 * Mettre à jour la valeur pour une clé donnée
	 *
	 * @param key
	 *            Clé (long, Double ou String)
	 * @param value
	 *            Valeur
	 * @throws LeekRunException
	 */
	public void set(AI ai, Object key, Object value) throws LeekRunException {
		Element e = getElement(ai, key);
		// Si l'élément n'existe pas on le crée
		if (e == null) {
			if (mSize >= capacity) {
				growCapacity(ai);
			}
			mSize++;
			e = createElement(ai, key, value);
			pushElement(e);
		} else {
			e.value.set(value);
		}
	}

	public Box getOrCreate(AI ai, Object keyValue) throws LeekRunException {
		var key = transformKey(ai, keyValue);
		Element e = getElement(ai, key);
		if (e == null) {
			if (mSize >= capacity) {
				growCapacity(ai);
			}
			mSize++;
			e = createElement(ai, key, null);
			pushElement(e);
		}
		return e.value;
	}

	public Box end() {
		return mEnd == null ? null : mEnd.value;
	}

	public Box start() {
		return mHead == null ? null : mHead.value;
	}

	public Object insert(AI ai, Object value, long position) throws LeekRunException {
		int shifted = size() - (int) position;
		ai.ops(1 + Math.max(0, shifted));
		if (position < 0) {
			return null;
		} else if (position >= mSize) {
			push(ai, value);
		} else if (position == 0) {
			if (mSize >= capacity) {
				growCapacity(ai);
			}
			mSize++;
			var key = 0l;
			Element e = createElement(ai, key, value);
			unshiftElement(e);
			reindex(ai);
			return null;
		} else {
			if (mSize >= capacity) {
				growCapacity(ai);
			}
			mSize++;
			// On crée notre nouvel élément
			Element e = createElement(ai, mIndex, value);
			// On va rechercher l'élément avant lequel insérer
			Element i = mHead;
			for (int k = 0; k < position; k++)
				i = i.next;

			// On insert l'élément (normalement ça n'est ni la tête ni la queue)
			e.prev = i.prev;
			e.next = i;
			i.prev.next = e;
			i.prev = e;

			// On réindexe
			reindex(ai);
		}
		return null;
	}

	public LegacyArrayLeekValue subArray(AI ai, long start, long end) throws LeekRunException {
		ai.ops(1 + Math.max(0, (int) (end - start)));
		if (start < 0 || end < start || end >= size())
			return null;
		LegacyArrayLeekValue retour = new LegacyArrayLeekValue();
		int i = 0;
		for (var val : this) {
			if (i >= start && i <= end) {
				retour.push(ai, LeekOperations.clone(ai, val.getValue()));
				ai.ops(1);
			}
			i++;
		}
		return retour;
	}

	public boolean isEmpty(AI ai) {
		return size() == 0;
	}

	public Object fill(AI ai, Object value) throws LeekRunException {
		return fill(ai, value, size());
	}

	public Object fill(AI ai, Object value, long size) throws LeekRunException {
		for (long i = 0; i < size; i++) {
			put(ai, i, value);
			ai.ops(3);
		}
		return null;
	}

	public double average(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		double average = 0;
		for (var val : this) {
			average += ai.real(val.getValue());
		}
		if (average == 0)
			return 0.0;
		return average / size();
	}

	public double sum(AI ai) throws LeekRunException {
		ai.ops(1 + 2 * size());
		double somme = 0;
		for (var val : this) {
			somme += ai.real(val.getValue());
		}
		return somme;
	}

	public Object pop(AI ai) throws LeekRunException {
		return remove(ai, size() - 1);
	}

	public Object shift(AI ai) throws LeekRunException {
		ai.ops(1 + size());
		return removeIndex(ai, 0);
	}

	public Object arrayIter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + size());
		if (ai.getVersion() >= 2) {
			var iterator = iterator();
			if (function == null) {
				return null;
			}
			int nb = function.getArgumentsCount();
			if (nb != 1 && nb != 2)
				return null;
			while (iterator.hasNext()) {
				var value = iterator.getValue();
				if (nb == 1) {
					function.run(ai, null, value);
				} else {
					function.run(ai, null, iterator.getKey(ai), value);
				}
				iterator.next();
			}
			return null;
		} else {
			var iterator = iterator();
			if (function == null) {
				return null;
			}
			int nb = function.getArgumentsCount();
			if (nb != 1 && nb != 2)
				return null;
			while (iterator.hasNext()) {
				var value = iterator.getValueBox();
				if (nb == 1) {
					function.run(ai, null, value);
				} else {
					function.run(ai, null, iterator.getKey(ai), value);
				}
				iterator.next();
			}
			return null;
		}
	}

	public LegacyArrayLeekValue arrayConcat(AI ai, LegacyArrayLeekValue other) throws LeekRunException {
		return (LegacyArrayLeekValue) ai.add(this, other);
	}

	public LegacyArrayLeekValue arrayPartition(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		if (ai.getVersion() >= 2) {
			var list1 = new LegacyArrayLeekValue();
			var list2 = new LegacyArrayLeekValue();
			int nb = function.getArgumentsCount();
			if (nb != 1 && nb != 2)
				return new LegacyArrayLeekValue();
			ArrayIterator iterator = iterator();
			boolean b;
			while (iterator.hasNext()) {
				var value = iterator.getValue();
				if (nb == 1)
					b = ai.bool(function.run(ai, null, value));
				else
					b = ai.bool(function.run(ai, null, iterator.getKey(ai), value));
				(b ? list1 : list2).getOrCreate(ai, iterator.getKey(ai)).set(iterator.getValue(ai));
				iterator.next();
			}
			return new LegacyArrayLeekValue(ai, new Object[] { list1, list2 }, false);
		} else {
			var list1 = new LegacyArrayLeekValue();
			var list2 = new LegacyArrayLeekValue();
			int nb = function.getArgumentsCount();
			if (nb != 1 && nb != 2)
				return new LegacyArrayLeekValue();
			var iterator = iterator();
			boolean b;
			while (iterator.hasNext()) {
				var value = iterator.getValueBox();
				if (nb == 1)
					b = ai.bool(function.run(ai, null, value));
				else
					b = ai.bool(function.run(ai, null, iterator.getKey(ai), value));
				(b ? list1 : list2).getOrCreate(ai, iterator.getKey(ai)).set(iterator.getValue(ai));
				iterator.next();
			}
			return new LegacyArrayLeekValue(ai, new Object[] { list1, list2 }, false);
		}
	}

	public Object arrayFoldLeft(AI ai, FunctionLeekValue function, Object start_value) throws LeekRunException {
		ai.ops(1 + 2 * size());
		Object result = LeekOperations.clone(ai, start_value);
		for (var value : this) {
			result = function.run(ai, null, result, value.getValue());
		}
		return result;
	}

	public Object arrayFoldRight(AI ai, FunctionLeekValue function, Object start_value) throws LeekRunException {
		ai.ops(1 + 2 * size());
		Object result = LeekOperations.clone(ai, start_value);
		var it = getReversedIterator();
		while (it.hasNext()) {
			result = function.run(ai, null, it.next(), result);
		}
		return result;
	}

	public LegacyArrayLeekValue arrayMap(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		if (ai.getVersion() >= 2) {
			var retour = new LegacyArrayLeekValue();
			var iterator = iterator();
			int nb = function.getArgumentsCount();
			while (iterator.hasNext()) {
				var value = iterator.getValue();
				if (nb >= 2) {
					retour.getOrCreate(ai, iterator.getKey(ai)).set(function.run(ai, null, iterator.getKey(ai), value));
				} else {
					retour.getOrCreate(ai, iterator.getKey(ai)).set(function.run(ai, null, value));
				}
				iterator.next();
			}
			return retour;
		} else {
			var retour = new LegacyArrayLeekValue();
			var iterator = iterator();
			int nb = function.getArgumentsCount();
			while (iterator.hasNext()) {
				var value = iterator.getValueBox();
				if (nb >= 2)
					retour.getOrCreate(ai, iterator.getKey(ai)).setRef(function.run(ai, null, iterator.getKey(ai), value));
				else
					retour.getOrCreate(ai, iterator.getKey(ai)).setRef(function.run(ai, null, value));
				iterator.next();
			}
			return retour;
		}
	}

	public LegacyArrayLeekValue arrayFilter(AI ai, FunctionLeekValue function) throws LeekRunException {
		ai.ops(1 + 2 * size());
		if (ai.getVersion() >= 2) {
			var retour = new LegacyArrayLeekValue();
			var iterator = iterator();
			int nb = function.getArgumentsCount();
			if (nb != 1 && nb != 2)
				return retour;
			while (iterator.hasNext()) {
				var value = iterator.getValue();
				if (nb == 1) {
					if (ai.bool(function.run(ai, null, value))) {
						retour.push(ai, iterator.getValue(ai));
					}
				} else {
					if (ai.bool(function.run(ai, null, iterator.getKey(ai), value))) {
						retour.push(ai, iterator.getValue(ai));
					}
				}
				iterator.next();
			}
			return retour;
		} else {
			var retour = new LegacyArrayLeekValue();
			var iterator = iterator();
			int nb = function.getArgumentsCount();
			if (nb != 1 && nb != 2)
				return retour;
			while (iterator.hasNext()) {
				var value = iterator.getValueBox();
				if (nb == 1) {
					if (ai.bool(function.run(ai, null, new Object[] { value }))) {
						// In LeekScript < 1.0, arrayFilter had a bug, the result array was not reindexed
						retour.getOrCreate(ai, iterator.getKey(ai)).set(iterator.getValue(ai));
					}
				} else {
					if (ai.bool(function.run(ai, null, new Object[] { iterator.getKey(ai), value }))) {
						retour.getOrCreate(ai, iterator.getKey(ai)).set(iterator.getValue(ai));
					}
				}
				iterator.next();
			}
			return retour;
		}
	}

	public LegacyArrayLeekValue arrayFlatten(AI ai) throws LeekRunException {
		return arrayFlatten(ai, 1);
	}

	public LegacyArrayLeekValue arrayFlatten(AI ai, long depth) throws LeekRunException {
		var result = new LegacyArrayLeekValue();
		flatten_rec(ai, this, result, depth);
		return result;
	}

	private void flatten_rec(AI ai, LegacyArrayLeekValue array, LegacyArrayLeekValue result, long depth) throws LeekRunException {
		ai.ops(1 + 2 * size());
		for (var value : array) {
			if (value.getValue() instanceof LegacyArrayLeekValue && depth > 0) {
				flatten_rec(ai, (LegacyArrayLeekValue) value.getValue(), result, depth - 1);
			} else {
				result.push(ai, value.getValue());
			}
		}
	}

	public Object keySort(AI ai) throws LeekRunException {
		return keySort(ai, 0);
	}

	public Object keySort(AI ai, long comparator) throws LeekRunException {
		int type = comparator == 1 ? LegacyArrayLeekValue.DESC_K : LegacyArrayLeekValue.ASC_K;
		sort(ai, type);
		return null;
	}

	public Object assocSort(AI ai) throws LeekRunException {
		return assocSort(ai, 0);
	}

	public Object assocSort(AI ai, long comparator) throws LeekRunException {
		int type = comparator == 0 ? LegacyArrayLeekValue.ASC_A : LegacyArrayLeekValue.DESC_A;
		sort(ai, type);
		return null;
	}

	// Fonctions "briques de base"

	public void reindex(AI ai) throws LeekRunException {
		// Réindexer le tableau (Change l'index de toutes les valeurs numériques)
		Long new_index = 0l;

		ai.opsNoCheck(mSize);

		Element e = mHead;
		while (e != null) {
			if (e.numeric) {
				var new_key = new_index;
				// Changement de clé
				if (!e.key.equals(new_key)) {
					// On regarde si le hashCode change
					if (new_key.hashCode() != e.hash) {
						removeFromHashmap(ai, e);
						e.hash = new_key.hashCode();
						addToHashMap(ai, e);
					}
					e.key = new_key;
				}
				new_index++;
			}
			e = e.next;
		}
		mIndex = new_index;
	}

	private void unshiftElement(Element e) { // Ajouter un élément au début
		if (mHead == null) { // Tableau vide
			mHead = e;
			mEnd = e;
		} else { // Ajouter au début
			mHead.prev = e;
			e.next = mHead;
			mHead = e;
		}
	}

	private void pushElement(Element e) {// Ajouter un élément à la fin
		if (mEnd == null) { // Tableau vide
			mHead = e;
			mEnd = e;
		} else { // Sinon on ajoute à la fin
			mEnd.next = e;
			e.prev = mEnd;
			mEnd = e;
		}
	}

	private Element createElement(AI ai, Object key, Object value) throws LeekRunException {
		// On crée l'élément
		Element e = new Element();
		e.hash = key.hashCode();
		e.key = key;

		e.value = new Box(ai, value);
		if (key instanceof Long) {
			// On met à jour l'index suivant
			var index = (Long) key;
			if (index >= mIndex)
				mIndex = index + 1;
			e.numeric = true;
		}

		// On l'ajoute dans la hashmap
		addToHashMap(ai, e);

		// System.out.println("ops createElement");
		int operations = LegacyArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS + (int) Math.sqrt(mSize) / 3;
		ai.opsNoCheck(operations);

		return e;
	}

	private void addToHashMap(AI ai, Element e) throws LeekRunException {
		if (mTable == null) {
			initTable(ai, START_CAPACITY);
		}
		// Ajouter dans la hashmap
		int index = getIndex(e.hash);
		Element f = mTable[index];
		if (f == null)
			mTable[index] = e;
		else {
			while (f.hashNext != null) {
				f = f.hashNext;
			}
			f.hashNext = e;
		}
	}

	private void removeFromHashmap(AI ai, Element e) throws LeekRunException {
		if (mTable == null) {
			return; // nothing to to, empty array
		}
		// Remove from hash hashmap
		int index = getIndex(e.hash);
		Element f = mTable[index];
		if (f == null)
			return;
		else if (f == e) {
			mTable[index] = e.hashNext;
			e.hashNext = null;
		} else {
			while (f.hashNext != e && f.hashNext != null) {
				f = f.hashNext;
			}
			if (f.hashNext == null) {
				return;
			}
			f.hashNext = f.hashNext.hashNext;
			e.hashNext = null;
		}
	}

	private void destroyElement(Element e) throws LeekRunException {
		mSize--;
	}

	private Element getElement(AI ai, Object key) throws LeekRunException {

		// System.out.println("ops getElement");
		int operations = LegacyArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS;
		ai.opsNoCheck(operations);

		if (mTable == null) {
			return null; // empty array
		}

		int hash = key == null ? 0 : key.hashCode();
		int index = getIndex(hash);

		Element f = mTable[index];

		while (f != null) {
			if (f.hash == hash && f.key.equals(key)) {
				return f;
			}
			f = f.hashNext;
		}
		return null;
	}

	private int getIndex(int hash) {
		return hash & (capacity - 1);
	}

	public boolean some(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (var entry : this) {
			if (ai.bool(function.run(ai, null, entry.getKey(), entry.getValue(), this))) {
				return true;
			}
		}
		return false;
	}

	public boolean every(AI ai, FunctionLeekValue function) throws LeekRunException {
		for (var entry : this) {
			if (ai.bool(function.run(ai, null, entry.getKey(), entry.getValue(), this))) {
				return true;
			}
		}
		return false;
	}

	public String toString(AI ai, Set<Object> visited, boolean export) throws LeekRunException {

		ai.ops(1 + mSize * 2);

		StringBuilder sb = new StringBuilder();
		// On va regarder si le tableau est dans l'ordre
		Element e = mHead;

		boolean isInOrder = true;
		var value = 0l;
		while (e != null) {
			if (!(e.key instanceof Long) || (Long) e.key != value) {
				isInOrder = false;
				break;
			}
			value++;
			e = e.next;
		}

		sb.append("[");
		e = mHead;
		while (e != null) {
			if (e != mHead)
				sb.append(", ");
			if (!isInOrder) {
				if (export) {
					sb.append(ai.export(e.key, visited));
				} else {
					sb.append(ai.string(e.key, visited));
				}
				sb.append(" : ");
			}
			if (visited.contains(e.value.getValue())) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(e.value.getValue())) {
					visited.add(e.value.getValue());
				}
				if (export) {
					sb.append(ai.export(e.value.getValue(), visited));
				} else {
					sb.append(ai.string(e.value.getValue(), visited));
				}
			}
			e = e.next;
		}
		sb.append("]");
		return sb.toString();
	}

	public boolean isAssociative() {

		Element e = mHead;
		var value = 0l;
		while (e != null) {
			if (!(e.key instanceof Long) || (Long) e.key != value) {
				return true;
			}
			value++;
			e = e.next;
		}
		return false;
	}

	public Iterator<Object> reversedIterator() {
		return new ReversedPhpIterator();
	}

	public boolean equals(AI ai, LegacyArrayLeekValue array) throws LeekRunException {

		ai.ops(1);

		// On commence par vérifier la taille
		if (mSize != array.mSize)
			return false;
		if (mSize == 0)
			return true;

		ai.ops(mSize);

		Element e1 = mHead;
		Element e2 = array.mHead;
		// On va comparer chaque élément 1 à 1
		while (e1 != null) {
			if (!e1.key.equals(e2.key))
				return false;
			if (!ai.eq(e1.value.getValue(), e2.value.getValue()))
				return false;
			e1 = e1.next;
			e2 = e2.next;
		}
		return true;
	}
}
