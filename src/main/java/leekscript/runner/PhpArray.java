package leekscript.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.PhpArrayVariableLeekValue;
import leekscript.runner.values.StringLeekValue;

public class PhpArray implements Iterable<AbstractLeekValue> {

	private final static int START_CAPACITY = 16;
	private final static int MAX_CAPACITY = 32000;

	public final static int ASC = 1;
	public final static int DESC = 2;
	public final static int RANDOM = 3;
	public final static int ASC_A = 4;
	public final static int DESC_A = 5;
	public final static int ASC_K = 6;
	public final static int DESC_K = 7;

	private static int RAM_LIMIT = 1000000;

	private class ElementComparator implements Comparator<Element> {

		private final int mOrder;
		private final AI ai;

		public final static int SORT_ASC = 1;
		public final static int SORT_DESC = 2;

		public ElementComparator(AI ai, int order) {
			mOrder = order;
			this.ai = ai;
		}

		@Override
		public int compare(Element v1, Element v2) {
			try {
			if (mOrder == SORT_ASC)
				return compareAsc(v1.value.getValue(), v2.value.getValue());
			else if (mOrder == SORT_DESC)
				return compareAsc(v2.value.getValue(), v1.value.getValue());
			} catch (Exception e) {}
			return 0;
		}

		public int compareAsc(AbstractLeekValue v1, AbstractLeekValue v2) throws LeekRunException {
			if (v1.getType() < v2.getType())
				return -1;
			else if (v1.getType() > v2.getType())
				return 1;
			if (v1.getType() == AbstractLeekValue.BOOLEAN) {
				if (v1.getBoolean() == v2.getBoolean())
					return 0;
				else if (v1.getBoolean())
					return 1;
				else
					return -1;
			} else if (v1.getType() == AbstractLeekValue.NUMBER) {
				if (v1.getDouble(ai) == v2.getDouble(ai))
					return 0;
				else if (v1.getDouble(ai) < v2.getDouble(ai))
					return -1;
				else
					return 1;
			} else if (v1.getType() == AbstractLeekValue.STRING) {
				return v1.getString(ai).compareTo(v2.getString(ai));
			} else if (v1.getType() == AbstractLeekValue.ARRAY) {
				if (v1.getArray().size() == v2.getArray().size())
					return 0;
				else if (v1.getArray().size() < v2.getArray().size())
					return -1;
				else
					return 1;
			} else if (v1.getType() == AbstractLeekValue.NULL)
				return 0;
			else
				return -1;
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

	private class PhpIterator implements Iterator<AbstractLeekValue> {
		private Element e = mHead;

		@Override
		public boolean hasNext() {
			return e != null;
		}
		@Override
		public AbstractLeekValue next() {
			AbstractLeekValue v = e.value;
			if (e != null)
				e = e.next;
			return v;
		}
		@Override
		public void remove() {}
	}

	private class ReversedPhpIterator implements Iterator<AbstractLeekValue> {
		private Element e = mEnd;

		@Override
		public boolean hasNext() {
			return e != null;
		}
		@Override
		public AbstractLeekValue next() {
			AbstractLeekValue v = e.value;
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
		private PhpArrayVariableLeekValue value = null;

		private Element next = null;
		private Element prev = null;

		private Element hashNext = null;

		public Element next() {
			return next;
		}

		public AbstractLeekValue key() {
			if (key instanceof Integer)
				return LeekValueManager.getLeekIntValue(((Integer) key).intValue());
			else
				return new StringLeekValue(key.toString());
		}

		public Object keyObject() {
			return key;
		}

		public AbstractLeekValue value() {
			return value.getValue();
		}

		public void setValue(AI ai, AbstractLeekValue v) throws Exception {
			value.set(ai, v.getValue());
		}
	}

	private Element mHead = null;
	private Element mEnd = null;

	private int mIndex = 0;

	private int mSize = 0;
	private int capacity = 0;

	// Calcul optimisé de la ram utilisée (Optimisé niveau UC, pas niveau RAM)
	private int mTotalSize = 0;
	private PhpArrayVariableLeekValue mParent = null;

	private Element[] mTable = null;

	public PhpArray() {}
	
	public PhpArray(AI ai, int capacity) throws LeekRunException {
		initTable(ai, capacity);
	}
	
	public PhpArray(AI ai, PhpArray phpArray) throws Exception {
		if (phpArray.size() > 0) {
			initTable(ai, phpArray.size());
			Element e = phpArray.mHead;
			while (e != null) {
				set(ai, e.key, LeekOperations.clone(ai, e.value.getValue()));
				e = e.next;
			}
		}
	}
	
	private void initTable(AI ai, int capacity) throws LeekRunException {
		ai.addOperations(capacity / 5);
		this.capacity = capacity;
		mTable = new Element[capacity];
	}
	
	private void growCapacity(AI ai) throws Exception {

		if (capacity == MAX_CAPACITY) return;
		
		capacity = Math.min(capacity * 2, MAX_CAPACITY);
		
		// Copy in a new array
		PhpArray newArray = new PhpArray(ai, capacity);
		Element e = mHead;
		while (e != null) {
			newArray.set(ai, e.key, e.value.getValue());
			e = e.next;
		}
		// Use the table of this new array
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
	public AbstractLeekValue get(AI ai, Object key) throws LeekRunException {
		Element e = getElement(ai, key);
		return e == null ? LeekValueManager.NULL : e.value;
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
	public boolean contains(AI ai, AbstractLeekValue value) throws LeekRunException {
		Element e = mHead;
		while (e != null) {
			if (e.value.equals(ai, value))
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
	public AbstractLeekValue search(AI ai, AbstractLeekValue value, int pos) throws LeekRunException {
		Element e = mHead;
		int p = 0;
		while (e != null) {
			if (p >= pos && e.value.getType() == value.getType() && e.value.equals(ai, value)) {
				if (e.key instanceof Integer)
					return LeekValueManager.getLeekIntValue(((Integer) e.key).intValue());
				else
					return new StringLeekValue(e.key.toString());
			}
			e = e.next;
			p++;
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue removeIndex(AI ai, int index) throws LeekRunException {
		if (index >= mSize)
			return LeekValueManager.NULL;
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
		return LeekValueManager.NULL;
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
		// création de la liste
		List<Element> liste = new ArrayList<Element>();
		Element elem = mHead;
		while (elem != null) {
			liste.add(elem);
			elem = elem.next;
		}
		// Trie de la liste
		if (comparator == RANDOM)
			Collections.shuffle(liste);
		else if (comparator == ASC_K || comparator == DESC_K) {
			Collections.sort(liste, new KeyComparator(
					(comparator == ASC_K) ? ElementComparator.SORT_ASC
							: ElementComparator.SORT_DESC));
		} else
			Collections.sort(liste, new ElementComparator(ai,
					(comparator == ASC || comparator == ASC_A) ? ElementComparator.SORT_ASC
							: ElementComparator.SORT_DESC));

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
	 * @throws Exception
	 */
	public void sort(AI ai, Comparator<Element> comparator) throws Exception {
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

	public void removeObject(AI ai, AbstractLeekValue value) throws LeekRunException {
		Element e = mHead;
		while (e != null) {
			if (e.value.getType() == value.getType() && e.value.equals(ai, value)) {
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
	 * @throws Exception
	 */
	public void push(AI ai, AbstractLeekValue value) throws Exception {
		Integer key = Integer.valueOf(mIndex);
		Element e = createElement(ai, key, value);
		pushElement(e);
		mSize++;
		if (mSize > capacity) {
			growCapacity(ai);
		}
	}

	/**
	 * Ajouter un élément au début du array (décale les index numériques)
	 *
	 * @param value
	 *            Element à ajouter
	 * @throws Exception
	 */
	public void unshift(AI ai, AbstractLeekValue value) throws Exception {
		Integer key = 0;
		Element e = createElement(ai, key, value);
		unshiftElement(e);
		reindex(ai);
		mSize++;
		if (mSize > capacity) {
			growCapacity(ai);
		}
	}

	/**
	 * Mettre à jour la valeur pour une clé donnée
	 *
	 * @param key
	 *            Clé (Integer, Double ou String)
	 * @param value
	 *            Valeur
	 * @throws Exception
	 */
	public void set(AI ai, Object key, AbstractLeekValue value) throws Exception {

		Element e = getElement(ai, key);
		// Si l'élément n'existe pas on le crée
		if (e == null) {
			e = createElement(ai, key, value);
			pushElement(e);
			mSize++;
			if (mSize > capacity) {
				growCapacity(ai);
			}
		} else {
			e.value.set(ai, value);
		}
	}

	public AbstractLeekValue getOrCreate(AI ai, Object key) throws Exception {
		Element e = getElement(ai, key);
		if (e == null) {
			e = createElement(ai, key, LeekValueManager.NULL);
			pushElement(e);
			mSize++;
			if (mSize > capacity) {
				growCapacity(ai);
				e = getElement(ai, key);
			}
		}
		return e.value;
	}

	public void set(int key, AbstractLeekValue value) throws Exception {
		set(Integer.valueOf(key), value);
	}

	public AbstractLeekValue end() {
		return mEnd == null ? LeekValueManager.NULL : mEnd.value;
	}

	public AbstractLeekValue start() {
		return mHead == null ? LeekValueManager.NULL : mHead.value;
	}

	public void insert(AI ai, int position, AbstractLeekValue value) throws Exception {
		if (position < 0) {
			return;
		} else if (position >= mSize) {
			push(ai, value);
		} else if (position == 0) {
			unshift(ai, value);
		} else {
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
			
			mSize++;
			if (mSize > capacity) {
				growCapacity(ai);
			}
		}
	}

	// Fonctions "briques de base"

	public void reindex(AI ai) throws LeekRunException {
		// Réindexer le tableau (Change l'index de toutes les valeurs
		// numériques)
		int new_index = 0;
		
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

	private void unshiftElement(Element e) {// Ajouter un élément au début
		if (mHead == null) {// Tableau vide
			mHead = e;
			mEnd = e;
		} else {// Ajouter au début
			mHead.prev = e;
			e.next = mHead;
			mHead = e;
		}
	}

	private void pushElement(Element e) {// Ajouter un élément à la fin
		if (mEnd == null) {// Tableau vide
			mHead = e;
			mEnd = e;
		} else {// Sinon on ajoute à la fin
			mEnd.next = e;
			e.prev = mEnd;
			mEnd = e;
		}
	}

	private Element createElement(AI ai, Object key, AbstractLeekValue value) throws Exception {
		// On crée l'élément
		Element e = new Element();
		e.hash = key.hashCode();
		e.key = key;

		// On ajoute la taille de la clé
		int keySize = 1;
		
		e.value = new PhpArrayVariableLeekValue(this, ai, value, keySize);
		if (key instanceof Integer) {
			// On met à jour l'index suivant
			int index = ((Integer) key).intValue();
			if (index >= mIndex)
				mIndex = index + 1;
			e.numeric = true;
		}

		// On l'ajoute dans la hashmap
		addToHashMap(ai, e);

		int operations = ArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS + (int) Math.sqrt(mSize) / 3;
		ai.addOperations(operations);

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
		if (e.key instanceof Integer)
			e.value.removeFromTable(1);
		else
			e.value.removeFromTable(((String) e.key).length());
	}

	private Element getElement(AI ai, Object key) throws LeekRunException {
		if (mTable == null) {
			return null; // empty array
		}
		int operations = ArrayLeekValue.ARRAY_CELL_ACCESS_OPERATIONS;
		ai.addOperations(operations);

		int hash = getHash(key);
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

	private int getHash(Object key) {
		return key.hashCode();
	}

	private int getIndex(int hash) {
		return hash & (capacity - 1);
	}

	public String toString(AI ai) throws LeekRunException {

		ai.addOperations(1 + mSize * 2);

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
			if (!isInOrder)
				sb.append(e.key).append(" : ");
			sb.append(e.value.getString(ai));
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
	public Iterator<AbstractLeekValue> iterator() {
		return new PhpIterator();
	}

	public Iterator<AbstractLeekValue> reversedIterator() {
		return new ReversedPhpIterator();
	}

	public boolean equals(AI ai, PhpArray array) throws LeekRunException {
		
		ai.addOperations(1);
		
		// On commence par vérifier la taille
		if (mSize != array.mSize)
			return false;
		if (mSize == 0)
			return true;

		ai.addOperations(mSize);
		
		Element e1 = mHead;
		Element e2 = array.mHead;
		// On va comparer chaque élément 1 à 1
		while (e1 != null) {
			if (!e1.key.equals(e2.key))
				return false;
			if (!e1.value.getValue().equals(ai, e2.value.getValue()))
				return false;
			e1 = e1.next;
			e2 = e2.next;
		}
		return true;
	}

	public void setParent(PhpArrayVariableLeekValue parent) {
		mParent = parent;
	}

	public void updateArraySize(int delta) throws LeekRunException {
		if (delta == 0) {
			return;
		}
		mTotalSize += delta;
		if (mTotalSize >= RAM_LIMIT) {
			throw new LeekRunException(LeekRunException.OUT_OF_MEMORY);
		}
		if (mParent != null) {
			mParent.updateSize(delta);
		}
	}

	public int getSize() {
		return mTotalSize;
	}
}
