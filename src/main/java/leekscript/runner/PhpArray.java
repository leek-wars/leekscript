package leekscript.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LeekValue;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.runner.values.Box;

public class PhpArray implements Iterable<Box> {

	private final static int START_CAPACITY = 8;
	private final static int MAX_CAPACITY = 32000;

	public final static int ASC = 1;
	public final static int DESC = 2;
	public final static int RANDOM = 3;
	public final static int ASC_A = 4;
	public final static int DESC_A = 5;
	public final static int ASC_K = 6;
	public final static int DESC_K = 7;

	private class ElementComparatorV1 implements Comparator<Element> {

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
			int type1 = LeekValueManager.getV1Type(v1);
			int type2 = LeekValueManager.getV1Type(v2);
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
				var a = (ArrayLeekValue) v2;
				if (((ArrayLeekValue) v1).size() == a.size())
					return 0;
				else if (((ArrayLeekValue) v1).size() < a.size())
					return -1;
				else
					return 1;
			} else {
				return 0;
			}
		}
	}

	private class ElementComparator implements Comparator<Element> {

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
				var a = (ArrayLeekValue) v2;
				if (((ArrayLeekValue) v1).size() == a.size())
					return 0;
				else if (((ArrayLeekValue) v1).size() < a.size())
					return -1;
				else
					return 1;
			} else {
				return 0;
			}
		}
	}

	private class KeyComparator implements Comparator<Element> {

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
			else if (v1 instanceof Integer && v2 instanceof Integer)
				return ((Integer) v1).compareTo((Integer) v2);
			else if (v1 instanceof Integer)
				return -1;
			return 1;
		}
	}

	private class PhpIterator implements Iterator<Box> {
		private Element e = mHead;

		@Override
		public boolean hasNext() {
			return e != null;
		}

		@Override
		public Box next() {
			var v = e.value;
			if (e != null)
				e = e.next;
			return v;
		}

		@Override
		public void remove() {}
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

	public class Element {

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

		public Object key() {
			return key;
		}

		public Object keyObject() {
			return key;
		}

		public Object value() {
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
	}

	private Element mHead = null;
	private Element mEnd = null;
	private int mIndex = 0;
	private int mSize = 0;
	private int capacity = 0;
	private Element[] mTable = null;

	public PhpArray() {}

	public PhpArray(AI ai, int capacity) throws LeekRunException {
		if (capacity > 0) {
			initTable(ai, capacity);
		}
	}

	public PhpArray(AI ai, PhpArray phpArray, int level) throws LeekRunException {
		if (phpArray.size() > 0) {
			initTable(ai, phpArray.size());
			Element e = phpArray.mHead;
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

	private void initTable(AI ai, int capacity) throws LeekRunException {
		int realCapacity = Math.max(START_CAPACITY, capacity);
		// System.out.println("ops initTable");
		ai.addOperationsNoCheck(realCapacity / 5);
		this.capacity = realCapacity;
		mTable = new Element[realCapacity];
	}

	private void growCapacity(AI ai) throws LeekRunException {

		if (capacity == MAX_CAPACITY) return;

		// Copy in a new array
		int new_capacity = Math.min(Math.max(START_CAPACITY, capacity * 2), MAX_CAPACITY);
		PhpArray newArray = new PhpArray(ai, new_capacity);
		Element e = mHead;
		while (e != null) {
			newArray.set(ai, e.key, e.value.getValue());
			e = e.next;
		}
		// Use the table of this new array
		capacity = new_capacity;
		mTable = newArray.mTable;
		mHead = newArray.mHead;
		mEnd = newArray.mEnd;
		mSize = newArray.mSize;
		mIndex = newArray.mIndex;
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

	/**
	 * Retourne la valeur à pour une clé donnée
	 *
	 * @param key
	 *            Clé dont on veut la valeur
	 * @return Valeur à la clé donnée
	 * @throws LeekRunException
	 */
	public Object get(AI ai, Object key) throws LeekRunException {
		Element e = getElement(ai, key);
		return e == null ? null : e.value.getValue();
	}

	public Box getBox(AI ai, Object key) throws LeekRunException {
		Element e = getElement(ai, key);
		return e == null ? new Box(ai) : e.value;
	}

	/**
	 * Vérifie si une clé se trouve bien dans le tableau
	 *
	 * @param key
	 *            Clé à rechercher
	 * @return True si la clé existe
	 * @throws LeekRunException
	 */
	public boolean containsKey(AI ai, Object key) throws LeekRunException {
		return getElement(ai, key) != null;
	}

	/**
	 * Vérifie si le tableau contient une valeur donnée
	 *
	 * @param value
	 *            Valeur à rechercher
	 * @return True si la valeur existe dans le tableau
	 * @throws LeekRunException
	 */
	public boolean contains(AI ai, Object value) throws LeekRunException {
		Element e = mHead;
		while (e != null) {
			if (ai.eq(e.value.getValue(), value))
				return true;
			e = e.next;
		}
		return false;
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

	/**
	 * Trie le tableau
	 *
	 * @param comparator
	 * @throws LeekRunException
	 */
	public void sort(AI ai, int comparator) throws LeekRunException {
		if (mSize == 0) {
			return;
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
			Collections.sort(liste, new KeyComparator(
					(comparator == ASC_K) ? ElementComparator.SORT_ASC
							: ElementComparator.SORT_DESC));
		} else {
			if (ai.getVersion() == 1) {
				Collections.sort(liste, new ElementComparatorV1(
					(comparator == ASC || comparator == ASC_A) ? ElementComparator.SORT_ASC
							: ElementComparator.SORT_DESC));
			} else {
				Collections.sort(liste, new ElementComparator(
				(comparator == ASC || comparator == ASC_A) ? ElementComparator.SORT_ASC
						: ElementComparator.SORT_DESC));
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
		int value = 0;
		while (e != null) {
			if (!(e.key instanceof Integer) || (Integer) e.key != value) {
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

	/**
	 * Inverse l'ordre
	 *
	 * @throws LeekRunException
	 */
	public void reverse(AI ai) throws LeekRunException {
		if (mSize == 0) {
			return;
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
	}

	public void assocReverse() {
		if (mSize == 0)
			return;
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
	}

	public void removeObject(AI ai, Object value) throws LeekRunException {
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
				return;
			}
			e = e.next;
		}
	}

	/**
	 * Ajouter un élément à la fin du array
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws LeekRunException
	 */
	public void push(AI ai, Object value) throws LeekRunException {
		if (mSize >= capacity) {
			growCapacity(ai);
		}
		mSize++;
		Integer key = Integer.valueOf(mIndex);
		Element e = createElement(ai, key, value);
		pushElement(e);
	}

	/**
	 * Ajouter un élément au début du array (décale les index numériques)
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws LeekRunException
	 */
	public void unshift(AI ai, Object value) throws LeekRunException {
		if (mSize >= capacity) {
			growCapacity(ai);
		}
		mSize++;
		Integer key = 0;
		Element e = createElement(ai, key, value);
		unshiftElement(e);
		reindex(ai);
	}

	/**
	 * Mettre à jour la valeur pour une clé donnée
	 *
	 * @param key
	 *            Clé (Integer, Double ou String)
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

	public Box getOrCreate(AI ai, Object key) throws LeekRunException {
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

	public void set(int key, Object value) throws LeekRunException {
		set(Integer.valueOf(key), value);
	}

	public Box end() {
		return mEnd == null ? null : mEnd.value;
	}

	public Box start() {
		return mHead == null ? null : mHead.value;
	}

	public void insert(AI ai, int position, Object value) throws LeekRunException {
		if (position < 0) {
			return;
		} else if (position >= mSize) {
			push(ai, value);
		} else if (position == 0) {
			unshift(ai, value);
		} else {
			if (mSize >= capacity) {
				growCapacity(ai);
			}
			mSize++;
			// On crée notre nouvel élément
			Element e = createElement(ai, Integer.valueOf(mIndex), value);
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
	}

	// Fonctions "briques de base"

	public void reindex(AI ai) throws LeekRunException {
		// Réindexer le tableau (Change l'index de toutes les valeurs numériques)
		int new_index = 0;

		ai.addOperationsNoCheck(mSize);

		Element e = mHead;
		while (e != null) {
			if (e.numeric) {
				Integer new_key = new_index;
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
		if (key instanceof Integer) {
			// On met à jour l'index suivant
			int index = ((Integer) key).intValue();
			if (index >= mIndex)
				mIndex = index + 1;
			e.numeric = true;
		}

		// On l'ajoute dans la hashmap
		addToHashMap(ai, e);

		// System.out.println("ops createElement");
		int operations = ArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS + (int) Math.sqrt(mSize) / 3;
		ai.addOperationsNoCheck(operations);

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
		int operations = ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS;
		ai.addOperationsNoCheck(operations);

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

	public String toString(AI ai, Set<Object> visited) throws LeekRunException {

		ai.ops(1 + mSize * 2);

		StringBuilder sb = new StringBuilder();
		// On va regarder si le tableau est dans l'ordre
		Element e = mHead;

		boolean isInOrder = true;
		int value = 0;
		while (e != null) {
			if (!(e.key instanceof Integer) || (Integer) e.key != value) {
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
				if (e.key instanceof ObjectLeekValue) {
					sb.append(ai.getString((ObjectLeekValue) e.key, visited));
				} else {
					sb.append(e.key);
				}
				sb.append(" : ");
			}
			if (visited.contains(e.value.getValue())) {
				sb.append("<...>");
			} else {
				if (!ai.isPrimitive(e.value.getValue())) {
					visited.add(e.value.getValue());
				}
				sb.append(ai.getString(e.value.getValue(), visited));
			}
			e = e.next;
		}
		sb.append("]");
		return sb.toString();
	}

	public boolean isAssociative() {

		Element e = mHead;
		int value = 0;
		while (e != null) {
			if (!(e.key instanceof Integer) || (Integer) e.key != value) {
				return true;
			}
			value++;
			e = e.next;
		}
		return false;
	}

	@Override
	public Iterator<Box> iterator() {
		return new PhpIterator();
	}

	public Iterator<Object> reversedIterator() {
		return new ReversedPhpIterator();
	}

	public boolean equals(AI ai, PhpArray array) throws LeekRunException {

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
