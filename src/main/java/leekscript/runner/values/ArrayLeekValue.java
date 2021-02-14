package leekscript.runner.values;

import java.util.Comparator;
import java.util.Iterator;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.PhpArray;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ArrayLeekValue extends AbstractLeekValue implements Iterable<AbstractLeekValue> {

	private final PhpArray mValues;

	public final static int ARRAY_CELL_ACCESS_OPERATIONS = 2;
	public final static int ARRAY_CELL_CREATE_OPERATIONS = 2; // + sqrt(size) / 5

	public class ArrayIterator {

		PhpArray.Element mElement;

		public ArrayIterator(PhpArray.Element head) {
			mElement = head;
		}

		public boolean ended() {
			return mElement == null;
		}

		public void next() {
			mElement = mElement.next();
		}

		public AbstractLeekValue getKey(AI ai) throws LeekRunException {
			if (ai.getVersion() >= 11) {
				return LeekOperations.clonePrimitive(ai, mElement.key());
			} else {
				return LeekOperations.clone(ai, mElement.key());
			}
		}

		public AbstractLeekValue getKeyRef() throws LeekRunException {
			return mElement.key();
		}

		public Object key() {
			return mElement.keyObject();
		}

		public AbstractLeekValue getValue(AI ai) throws LeekRunException {
			if (ai.getVersion() >= 11) {
				return LeekOperations.clonePrimitive(ai, mElement.value());
			} else {
				return LeekOperations.clone(ai, mElement.value());
			}
		}

		public AbstractLeekValue getValueRef() throws LeekRunException {
			return mElement.value();
		}

		public AbstractLeekValue getKeyReference() throws LeekRunException {
			return mElement.key();
		}

		public AbstractLeekValue getValueReference() throws LeekRunException {
			return mElement.value();
		}

		public void setValue(AI ai, VariableLeekValue value) throws LeekRunException {
			mElement.setValue(ai, value);
		}
	}

	public ArrayLeekValue() {
		mValues = new PhpArray();
	}

	public ArrayLeekValue(AI ai, AbstractLeekValue values[]) throws LeekRunException {
		this(ai, values, false);
	}

	public ArrayLeekValue(AI ai, AbstractLeekValue values[], boolean isKeyValue) throws LeekRunException {
		mValues = new PhpArray(ai, values.length);
		if (isKeyValue) {
			int i = 0;
			while (i < values.length) {
				getOrCreate(ai, values[i].getValue()).set(ai, values[i + 1].getValue());
				i += 2;
			}
		} else {
			for (int i = 0; i < values.length; i++) {
				mValues.push(ai, values[i]);
			}
		}
	}

	/*
	 * public ArrayLeekValue() { mValues = new PhpArray(); }
	 */

	public ArrayLeekValue(AI ai, ArrayLeekValue array, int level) throws LeekRunException {
		mValues = new PhpArray(ai, array.mValues, level);
	}


	public int size() {
		return mValues.size();
	}

	@Override
	public AbstractLeekValue get(AI ai, AbstractLeekValue value) throws LeekRunException {
		Object key;
		value = value.getValue();
		if (value instanceof StringLeekValue)
			key = value.getString(ai);
		else
			key = Integer.valueOf(value.getInt(ai));
		return mValues.get(ai, key);
	}

	@Override
	public AbstractLeekValue getOrCreate(AI ai, AbstractLeekValue value) throws LeekRunException {
		Object key;
		value = value.getValue();
		if (value instanceof StringLeekValue) {
			key = value.getString(ai);
		} else {
			key = Integer.valueOf(value.getInt(ai));
		}
		return mValues.getOrCreate(ai, key);
	}

	@Override
	public AbstractLeekValue get(AI ai, int value) throws LeekRunException {
		return mValues.getOrCreate(ai, Integer.valueOf(value));
	}

	public AbstractLeekValue remove(AI ai, int index) throws LeekRunException {
		return mValues.removeIndex(ai, index);
	}

	public void removeObject(AI ai, AbstractLeekValue value) throws LeekRunException {
		mValues.removeObject(ai, value);
	}

	public void removeByKey(AI ai, AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == STRING)
			mValues.remove(ai, value.getString(ai));
		else if (value.getType() == NUMBER)
			mValues.remove(ai, value.getInt(ai));
	}

	public void shuffle(AI ai) throws LeekRunException {
		// Collections.shuffle(mValues);
		mValues.sort(ai, PhpArray.RANDOM);
	}

	public void reverse(AI ai) throws LeekRunException {
		mValues.reverse(ai);
	}

	public void assocReverse() {
		mValues.assocReverse();
	}

	public String join(AI ai, String sep) throws LeekRunException {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (AbstractLeekValue val : mValues) {
			if (!first)
				sb.append(sep);
			else
				first = false;
			sb.append(val.getValue().getString(ai));
		}
		return sb.toString();
	}

	public void insert(AI ai, AbstractLeekValue value, int pos) throws LeekRunException {
		mValues.insert(ai, pos, value);
	}

	public AbstractLeekValue search(AI ai, AbstractLeekValue search, int pos) throws LeekRunException {
		return mValues.search(ai, search, pos);
	}

	public void sort(AI ai, int type) throws LeekRunException {
		// Collections.sort(mValues, new
		// LeekValueComparator.SortComparator(type));
		mValues.sort(ai, type);
	}

	@Override
	public Iterator<AbstractLeekValue> iterator() {
		return mValues.iterator();
	}

	public AbstractLeekValue end() {
		return mValues.end().getValue();
	}

	public AbstractLeekValue start() {
		return mValues.start().getValue();
	}

	public boolean contains(AI ai, AbstractLeekValue value) throws LeekRunException {
		return mValues.contains(ai, value);
	}

	public void push(AI ai, AbstractLeekValue m) throws LeekRunException {
		mValues.push(ai, m);
	}

	@Override
	public int getType() {
		return ARRAY;
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public ArrayLeekValue getArray() {
		return this;
	}

	public String getString(AI ai) throws LeekRunException {
		return mValues.toString(ai);
	}

	@Override
	public boolean getBoolean() {
		return mValues.size() != 0;
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		if (comp.getType() == ARRAY) {
			return mValues.equals(ai, comp.getArray().mValues);
		} else if (mValues.size() == 1) { // Si y'a un seul élément dans le tableau
			return mValues.getHeadElement().value().equals(ai, comp);
		} else if (comp.getType() == BOOLEAN) {
			return comp.getBoolean() == getBoolean();
		} else if (comp.getType() == STRING) {
			if (comp.getString(ai).equals("false") && getBoolean() == false)
				return true;
			else if (comp.getString(ai).equals("true") && getBoolean() == true)
				return true;
			else if (comp.getString(ai).isEmpty() && mValues.size() == 0)
				return true;
		} else if (comp.getType() == NUMBER) {
			if (mValues.size() == 0 && comp.getInt(ai) == 0)
				return true;
		}
		return false;
	}

	@Override
	public int getSize() throws LeekRunException {
		return mValues.getSize();
	}

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue value) throws LeekRunException {
		value = value.getValue();
		if (value instanceof ArrayLeekValue) {
			// mValues.reindex(ai);
			ArrayIterator iterator = value.getArray().getArrayIterator();
			while (!iterator.ended()) {
				if (iterator.key() instanceof String)
					mValues.getOrCreate(ai, iterator.getKey(ai).getString(ai)).set(ai, iterator.getValue(ai));
				else
					mValues.push(ai, iterator.getValue(ai));
				iterator.next();
			}
		}
		return this;
	}

	public ArrayIterator getArrayIterator() {
		return new ArrayIterator(mValues.getHeadElement());
	}

	public Iterator<AbstractLeekValue> getReversedIterator() {
		return mValues.reversedIterator();
	}

	public void sort(AI ai, Comparator<PhpArray.Element> comparator) throws LeekRunException {
		mValues.sort(ai, comparator);
	}

	public void setParent(PhpArrayVariableLeekValue parent) {
		mValues.setParent(parent);
	}

	@Override
	public Object toJSON(AI ai) throws LeekRunException {

		if (mValues.isAssociative()) {
			JSONObject o = new JSONObject();
			ArrayIterator i = getArrayIterator();
			while (!i.ended()) {
				o.put(i.key().toString(), i.getValue(ai).toJSON(ai));
				i.next();
			}
			return o;
		} else {
			JSONArray a = new JSONArray();
			for (AbstractLeekValue v : this) {
				a.add(v.toJSON(ai));
			}
			return a;
		}
	}
}
