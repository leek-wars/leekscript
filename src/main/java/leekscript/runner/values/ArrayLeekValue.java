package leekscript.runner.values;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.runner.PhpArray;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ArrayLeekValue implements Iterable<Box> {

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

		public Object getKey(AI ai) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return mElement.key();
			} else {
				return LeekOperations.clone(ai, mElement.key());
			}
		}

		public Object key() {
			return mElement.keyObject();
		}

		public Object getValue(AI ai) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return mElement.value();
			} else {
				return LeekOperations.clone(ai, mElement.value());
			}
		}

		public Object value() {
			return mElement.value();
		}

		public Object getKeyRef() throws LeekRunException {
			return mElement.key();
		}

		public Object getValueBox() throws LeekRunException {
			return mElement.valueBox();
		}

		public void setValue(AI ai, Box value) throws LeekRunException {
			mElement.setValue(ai, value);
		}
	}

	public ArrayLeekValue() {
		mValues = new PhpArray();
	}

	public ArrayLeekValue(AI ai, Object values[]) throws LeekRunException {
		this(ai, values, false);
	}

	public ArrayLeekValue(AI ai, Object values[], boolean isKeyValue) throws LeekRunException {
		mValues = new PhpArray(ai, values.length);
		if (isKeyValue) {
			int i = 0;
			while (i < values.length) {
				getOrCreate(ai, values[i]).set(values[i + 1]);
				i += 2;
			}
		} else {
			for (int i = 0; i < values.length; i++) {
				mValues.push(ai, values[i]);
			}
		}
	}

	public ArrayLeekValue(AI ai, ArrayLeekValue array, int level) throws LeekRunException {
		mValues = new PhpArray(ai, array.mValues, level);
	}

	public int size() {
		return mValues.size();
	}

	public Object get(AI ai, Object keyValue) throws LeekRunException {
		var key = transformKey(ai, keyValue);
		return mValues.get(ai, key);
	}

	public Box getBox(AI ai, Object keyValue) throws LeekRunException {
		var key = transformKey(ai, keyValue);
		return mValues.getBox(ai, key);
	}

	public Box getOrCreate(AI ai, Object keyValue) throws LeekRunException {
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key);
	}

	public Object put(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		if (ai.getVersion() == 1) {
			value = LeekOperations.clone(ai, value);
		}
		mValues.set(ai, key, value);
		return value;
	}

	public Object put_inc(AI ai, Object keyValue) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).increment();
	}

	public Object put_pre_inc(AI ai, Object keyValue) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).pre_increment();
	}

	public Object put_dec(AI ai, Object keyValue) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).decrement();
	}

	public Object put_pre_dec(AI ai, Object keyValue) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).pre_decrement();
	}

	public Object put_add_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).add_eq(value);
		return value;
	}

	public Object put_sub_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).sub_eq(value);
		return value;
	}

	public Object put_mul_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).mul_eq(value);
		return value;
	}

	public Object put_pow_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).pow_eq(value);
		return value;
	}

	public Object put_div_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).div_eq(value);
		return value;
	}

	public Object put_mod_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).mod_eq(value);
		return value;
	}

	public Object put_bor_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).bor_eq(value);
		return value;
	}


	public Object put_band_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		mValues.getOrCreate(ai, key).band_eq(value);
		return value;
	}


	public Object put_bxor_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).bxor_eq(value);
	}

	public Object put_shl_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).shl_eq(value);
	}

	public Object put_shr_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).shr_eq(value);
	}

	public Object put_ushr_eq(AI ai, Object keyValue, Object value) throws LeekRunException {
		// ai.ops(1);
		var key = transformKey(ai, keyValue);
		return mValues.getOrCreate(ai, key).ushr_eq(value);
	}

	private Object transformKey(AI ai, Object key) throws LeekRunException {
		if (key instanceof String || key instanceof ObjectLeekValue) {
			return key;
		} else {
			return ai.integer(key);
		}
	}

	public Box get(AI ai, int value) throws LeekRunException {
		return mValues.getOrCreate(ai, Integer.valueOf(value));
	}

	public Object remove(AI ai, int index) throws LeekRunException {
		return mValues.removeIndex(ai, index);
	}

	public void removeObject(AI ai, Object value) throws LeekRunException {
		mValues.removeObject(ai, value);
	}

	public void removeByKey(AI ai, Object value) throws LeekRunException {
		mValues.remove(ai, value);
	}

	public void shuffle(AI ai) throws LeekRunException {
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
		for (Object val : mValues) {
			if (!first)
				sb.append(sep);
			else
				first = false;
			sb.append(LeekValueManager.getString(ai, val));
		}
		return sb.toString();
	}

	public void insert(AI ai, Object value, int pos) throws LeekRunException {
		mValues.insert(ai, pos, value);
	}

	public Object search(AI ai, Object search, int pos) throws LeekRunException {
		return mValues.search(ai, search, pos);
	}

	public void sort(AI ai, int type) throws LeekRunException {
		mValues.sort(ai, type);
	}

	@Override
	public Iterator<Box> iterator() {
		return mValues.iterator();
	}

	public Object end() {
		return mValues.end().getValue();
	}

	public Object start() {
		return mValues.start().getValue();
	}

	public boolean contains(AI ai, Object value) throws LeekRunException {
		return mValues.contains(ai, value);
	}

	public void push(AI ai, Object m) throws LeekRunException {
		mValues.push(ai, m);
	}

	public String getString(AI ai, Set<Object> visited) throws LeekRunException {
		visited.add(this);
		return mValues.toString(ai, visited);
	}

	public boolean equals(AI ai, Object comp) throws LeekRunException {
		if (comp instanceof ArrayLeekValue) {
			return mValues.equals(ai, ((ArrayLeekValue) comp).mValues);
		} else if (mValues.size() == 1) { // Si y'a un seul élément dans le tableau
			var firstValue = mValues.getHeadElement().value();
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
			else if (ai.string(comp).isEmpty() && mValues.size() == 0)
				return true;
		} else if (comp instanceof Number) {
			if (mValues.size() == 0 && ai.integer(comp) == 0)
				return true;
		}
		return false;
	}

	public Object add_eq(AI ai, Object value) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			// mValues.reindex(ai);
			ArrayIterator iterator = ((ArrayLeekValue) value).getArrayIterator();
			while (!iterator.ended()) {
				if (iterator.key() instanceof String || iterator.key() instanceof ObjectLeekValue)
					mValues.getOrCreate(ai, ai.string(iterator.getKey(ai))).set(iterator.getValue(ai));
				else
					mValues.push(ai, iterator.getValue(ai));
				iterator.next();
			}
		} else {
			mValues.push(ai, value);
		}
		return this;
	}

	public ArrayIterator getArrayIterator() {
		return new ArrayIterator(mValues.getHeadElement());
	}

	public Iterator<Object> getReversedIterator() {
		return mValues.reversedIterator();
	}

	public void sort(AI ai, Comparator<PhpArray.Element> comparator) throws LeekRunException {
		mValues.sort(ai, comparator);
	}

	public JSON toJSON(AI ai) throws LeekRunException {
		return toJSON(ai, new HashSet<>());
	}

	public JSON toJSON(AI ai, HashSet<Object> visited) throws LeekRunException {
		visited.add(this);

		if (mValues.isAssociative()) {
			JSONObject o = new JSONObject();
			ArrayIterator i = getArrayIterator();
			while (!i.ended()) {
				var v = i.getValue(ai);
				if (!visited.contains(v)) {
					if (!ai.isPrimitive(v)) {
						visited.add(v);
					}
					o.put(i.key().toString(), ai.toJSON(v));
				}
				i.next();
			}
			return o;
		} else {
			JSONArray a = new JSONArray();
			for (var v : this) {
				if (!visited.contains(v)) {
					if (!ai.isPrimitive(v)) {
						visited.add(v);
					}
					a.add(ai.toJSON(v.getValue()));
				}
			}
			return a;
		}
	}

	@Override
	public String toString() {
		var r = "[";
		boolean first = true;
		ArrayIterator i = getArrayIterator();
		while (!i.ended()) {
			if (first) first = false;
			else r += ", ";
			if (mValues.isAssociative()) {
				r += i.key().toString() + ": ";
			}
			r += i.value();
			i.next();
		}
		return r + "]";
	}
}
