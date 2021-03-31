package leekscript.runner;

import leekscript.AILog;
import leekscript.compiler.LeekScript;
import leekscript.compiler.RandomGenerator;
import leekscript.runner.PhpArray.Element;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;
import leekscript.runner.values.VariableLeekValue;
import leekscript.runner.values.ArrayLeekValue.ArrayIterator;

import java.util.Comparator;
import java.util.Iterator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public abstract class AI {

	public static final int ERROR_LOG_COST = 1000;

	public final static int MAX_MEMORY = 100000;

	protected long mOperations = 0;
	public final static long MAX_OPERATIONS = 20000000;

	protected JSONArray mErrorObject = null;
	protected String thisObject = null;

	protected int id;
	protected AILog logs;
	protected AI mUAI;
	protected int mInstructions;
	protected RandomGenerator randomGenerator;

	public AI() {
		mUAI = this;
		logs = new AILog();
		randomGenerator = LeekScript.getRandom();
		try {
			init();
		} catch (Exception e) {}
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	// Method that can be overriden in each AI
	protected void init() throws Exception {}

	public int getInstructions() {
		return mInstructions;
	}

	public long getOperations() {
		return mOperations;
	}

	public AILog getLogs() {
		return logs;
	}

	public void addOperations(int nb) throws LeekRunException {
		mOperations += nb;
		if (mOperations >= MAX_OPERATIONS) {
			throw new LeekRunException(LeekRunException.TOO_MUCH_OPERATIONS);
		}
	}

	public void addOperationsNoCheck(int nb) {
		mOperations += nb;
	}

	public void resetCounter() {
		mOperations = 0;
	}

	protected NullLeekValue nothing(AbstractLeekValue obj) throws LeekRunException {
		return LeekValueManager.NULL;
	}

	public String getErrorMessage(StackTraceElement[] elements) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (StackTraceElement element : elements) {
			// System.out.println(element.getClassName() + " " + element.getMethodName() + " " + element.getLineNumber());
			if (element.getClassName().startsWith("AI_")) {
				sb.append(getErrorLocalisation(element.getLineNumber())).append("\n");
				if (count++ > 50) {
					sb.append("[...]");
					break;
				}
			}
		}
		// for (StackTraceElement element : elements) {
		// 	sb.append("\t▶ " + element.getClassName() + "." + element.getMethodName() + ", line " + element.getLineNumber()).append("\n");
		// }
		return sb.toString();
	}

	public String getErrorMessage(Throwable e) {
		return getErrorMessage(e.getStackTrace());
	}

	protected String getErrorLocalisation(int line) {
		if (mErrorObject == null) {
			mErrorObject = new JSONArray();
			for (String error : getErrorString()) {
				mErrorObject.add(JSON.parseArray(error));
			}
			thisObject = getAItring();
		}
		int value = 0;
		for (int i = 0; i < mErrorObject.size(); i++) {
			if (mErrorObject.getJSONArray(i).getInteger(0) > line) {
				break;
			}
			value = i;
		}
		if (mErrorObject.size() > value) {
			JSONArray l = mErrorObject.getJSONArray(value);
			if (l != null && l.size() >= 3) {
				var files = getErrorFiles();
				var f = l.getIntValue(1);
				String file = f < files.length ? files[f] : "?";
				return "\t▶ AI " + file + ", line " + l.getString(2); // + ", java " + line;
			}
		}
		return "";
	}

	public AbstractLeekValue color(AbstractLeekValue red, AbstractLeekValue green, AbstractLeekValue blue) throws LeekRunException {
		return LeekValueManager.getLeekIntValue(((red.getInt(this) & 255) << 16) | ((green.getInt(this) & 255) << 8) | (blue.getInt(this) & 255));
	}

	public int typeOf(AbstractLeekValue value) {
		if (value.getType() == AbstractLeekValue.ARRAY)
			return (int) LeekConstants.TYPE_ARRAY.getValue();
		if (value.getType() == AbstractLeekValue.NULL)
			return (int) LeekConstants.TYPE_NULL.getValue();
		if (value.getType() == AbstractLeekValue.STRING)
			return (int) LeekConstants.TYPE_STRING.getValue();
		if (value.getType() == AbstractLeekValue.NUMBER)
			return (int) LeekConstants.TYPE_NUMBER.getValue();
		if (value.getType() == AbstractLeekValue.BOOLEAN)
			return (int) LeekConstants.TYPE_BOOLEAN.getValue();
		if (value.getType() == AbstractLeekValue.FUNCTION)
			return (int) LeekConstants.TYPE_FUNCTION.getValue();
		if (value.getType() == AbstractLeekValue.OBJECT)
			return (int) LeekConstants.TYPE_OBJECT.getValue();
		return 0;
	}

	public void arrayFlatten(ArrayLeekValue array, ArrayLeekValue retour, int depth) throws LeekRunException {
		for (AbstractLeekValue value : array) {
			if (value.getValue() instanceof ArrayLeekValue && depth > 0) {
				arrayFlatten(value.getArray(), retour, depth - 1);
			} else
				retour.push(this, LeekOperations.clone(this, value));
		}
	}

	public AbstractLeekValue arrayFoldLeft(ArrayLeekValue array, AbstractLeekValue function, AbstractLeekValue start_value) throws LeekRunException {
		AbstractLeekValue result = LeekOperations.clone(this, start_value);
		// AbstractLeekValue prev = null;
		for (AbstractLeekValue value : array) {
			result = function.executeFunction(this, new AbstractLeekValue[] { result, value });
		}
		return result;
	}

	public AbstractLeekValue arrayFoldRight(ArrayLeekValue array, AbstractLeekValue function, AbstractLeekValue start_value) throws LeekRunException {
		AbstractLeekValue result = LeekOperations.clone(this, start_value);
		// AbstractLeekValue prev = null;
		Iterator<AbstractLeekValue> it = array.getReversedIterator();
		while (it.hasNext()) {
			result = function.executeFunction(this, new AbstractLeekValue[] { it.next(), result });
		}
		return result;
	}

	public AbstractLeekValue arrayPartition(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException {
		ArrayLeekValue list1 = new ArrayLeekValue();
		ArrayLeekValue list2 = new ArrayLeekValue();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return new ArrayLeekValue();
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		ArrayIterator iterator = array.getArrayIterator();
		boolean b;
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1)
				b = function.executeFunction(this, new AbstractLeekValue[] { value }).getBoolean();
			else
				b = function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value }).getBoolean();
			iterator.setValue(this, value);
			(b ? list1 : list2).getOrCreate(this, iterator.getKey(this)).set(this, iterator.getValue(this));
			iterator.next();
		}
		return new ArrayLeekValue(this, new AbstractLeekValue[] { list1, list2 }, false);
	}

	public ArrayLeekValue arrayMap(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		ArrayIterator iterator = array.getArrayIterator();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return retour;
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1)
				retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, function.executeFunction(this, new AbstractLeekValue[] { value }));
			else
				retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value }));
			iterator.setValue(this, value);
			iterator.next();
		}
		return retour;
	}

	public ArrayLeekValue arrayFilter(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		ArrayIterator iterator = array.getArrayIterator();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return retour;
		boolean b;
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1) {
				b = function.executeFunction(this, new AbstractLeekValue[] { value }).getBoolean();
				iterator.setValue(this, value);
				if (b) {
					if (getVersion() >= 11)
						retour.push(this, iterator.getValue(this).getValue());
					else
						// In LeekScript < 1.0, arrayFilter had a bug, the result array was not reindexed
						retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, iterator.getValue(this).getValue());
				}
			} else {
				b = function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value }).getBoolean();
				iterator.setValue(this, value);
				if (b)
					if (getVersion() >= 11)
						retour.push(this, iterator.getValue(this).getValue());
					else
						retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, iterator.getValue(this).getValue());
			}
			iterator.next();
		}
		return retour;
	}

	public AbstractLeekValue arrayIter(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException {
		ArrayIterator iterator = array.getArrayIterator();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return LeekValueManager.NULL;
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1)
				function.executeFunction(this, new AbstractLeekValue[] { value });
			else
				function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value });
			iterator.setValue(this, value);
			iterator.next();
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue arraySort(ArrayLeekValue origin, final AbstractLeekValue function) throws LeekRunException {
		try {
			int nb = function.getArgumentsCount(this);
			if (nb == 2) {
				ArrayLeekValue array = LeekOperations.clone(this, origin).getArray();
				array.sort(this, new Comparator<PhpArray.Element>() {
					@Override
					public int compare(Element o1, Element o2) {
						try {
							return function.executeFunction(AI.this, new AbstractLeekValue[] { o1.value(), o2.value() }).getInt(AI.this);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				return array;
			} else if (nb == 4) {
				ArrayLeekValue array = LeekOperations.clone(this, origin).getArray();
				array.sort(this, new Comparator<PhpArray.Element>() {
					@Override
					public int compare(Element o1, Element o2) {
						try {
							return function.executeFunction(AI.this, new AbstractLeekValue[] { o1.key(), o1.value(), o2.key(), o2.value() }).getInt(AI.this);
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
		return LeekValueManager.NULL;
	}


	public AbstractLeekValue jsonEncode(AI ai, AbstractLeekValue object) {

		try {

			String json = JSON.toJSONString(object.toJSON(ai));
			addOperations(json.length() * 10);
			return new StringLeekValue(json);

		} catch (Exception e) {

			getLogs().addLog(AILog.ERROR, "Cannot encode object \"" + object.toString() + "\"");
			try {
				addOperations(100);
			} catch (Exception e1) {}
			return LeekValueManager.NULL;
		}
	}

	public AbstractLeekValue jsonDecode(String json) {

		try {

			AbstractLeekValue obj = LeekValueManager.parseJSON(JSON.parse(json), this);
			addOperations(json.length() * 10);
			return obj;

		} catch (Exception e) {

			getLogs().addLog(AILog.ERROR, "Cannot parse json \"" + json + "\"");
			try {
				addOperations(100);
			} catch (Exception e1) {}
			return LeekValueManager.NULL;
		}
	}

	public void addSystemLog(int type, String key) throws LeekRunException {
		addSystemLog(type, key, null);
	}

	public void addSystemLog(int type, String key, String[] parameters) throws LeekRunException {
		if (type == AILog.WARNING)
			type = AILog.SWARNING;
		else if (type == AILog.ERROR)
			type = AILog.SERROR;
		else if (type == AILog.STANDARD)
			type = AILog.SSTANDARD;

		logs.addSystemLog(type, getErrorMessage(Thread.currentThread().getStackTrace()), key, parameters);
	}

	protected abstract String[] getErrorString();

	protected abstract String[] getErrorFiles();

	protected abstract String getAItring();

	public abstract AbstractLeekValue runIA() throws LeekRunException;

	public abstract int userFunctionCount(int id);

	public abstract boolean[] userFunctionReference(int id);

	public abstract AbstractLeekValue userFunctionExecute(int id, AbstractLeekValue[] value) throws LeekRunException;

	public abstract int anonymousFunctionCount(int id);

	public abstract boolean[] anonymousFunctionReference(int id);

	public RandomGenerator getRandom() {
		return randomGenerator;
	}

	public abstract int getVersion();
}
