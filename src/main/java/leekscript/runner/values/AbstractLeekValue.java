package leekscript.runner.values;

import java.util.HashSet;
import java.util.Set;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;

public abstract class AbstractLeekValue {

	public final static int NUMBER = 1;
	public final static int BOOLEAN = 2;
	public final static int ARRAY = 3;
	public final static int NULL = 4;
	public final static int STRING = 5;
	public final static int FUNCTION = 6;
	public final static int CLASS = 7;
	public final static int OBJECT = 8;

	public final static int ADD_COST = 1;
	public final static int MUL_COST = 5;
	public final static int DIV_COST = 5;
	public final static int MOD_COST = 5;
	public final static int POW_COST = 140;

	public int getInt(AI ai) throws LeekRunException {
		return 0;
	}

	public void setInt(int nb) {}

	public double getDouble(AI ai) throws LeekRunException {
		return 0;
	}

	public String getString(AI ai) throws LeekRunException {
		return getString(ai, new HashSet<>());
	}

	public String getString(AI ai, Set<Object> visited) throws LeekRunException {
		return getString(ai);
	}

	public boolean getBoolean() {
		return false;
	}

	public boolean getBooleanTernary(AI ai) throws LeekRunException {
		ai.addOperations(1);
		return getBoolean();
	}

	public boolean isNumeric() {
		return false;
	}

	public boolean isArray() {
		return false;
	}

	public boolean isArrayForIteration(AI ai) throws LeekRunException {
		// Pas itérable
		ai.addSystemLog(AILog.ERROR, AILog.NOT_ITERABLE, new String[] { getString(ai) });
		return false;
	}

	public boolean isNull() {
		return false;
	}

	public ArrayLeekValue getArray() {
		return null;
	}

	public AbstractLeekValue get(AI ai, int value) throws LeekRunException {
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue get(AI ai, AbstractLeekValue value) throws LeekRunException {
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getOrCreate(AI ai, AbstractLeekValue value) throws LeekRunException {
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getValue() {
		return this;
	}

	public AbstractLeekValue not(AI ai) {
		return LeekValueManager.getLeekBooleanValue(!getBoolean());
	}

	public AbstractLeekValue bnot(AI ai) throws LeekRunException {
		return LeekValueManager.getLeekIntValue(~getInt(ai));
	}

	public AbstractLeekValue opposite(AI ai) throws LeekRunException {
		return LeekValueManager.getLeekIntValue(-getInt(ai));
	}

	public AbstractLeekValue set(AI ai, AbstractLeekValue value) throws LeekRunException {
		return this;
	}
	public AbstractLeekValue setRef(AI ai, AbstractLeekValue value) throws LeekRunException {
		return this;
	}

	public int getArgumentsCount(AI ai) throws LeekRunException {
		return -1;
	}

	// Fonctions de comparaison
	public abstract boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException;

	public boolean notequals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return !equals(ai, comp);
	}

	public boolean less(AI ai, AbstractLeekValue comp) throws LeekRunException {
		ai.addOperations(1);
		return getInt(ai) < comp.getInt(ai);
	}

	public boolean moreequals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return !less(ai, comp);
	}

	public boolean more(AI ai, AbstractLeekValue comp) throws LeekRunException {
		ai.addOperations(1);
		return getInt(ai) > comp.getInt(ai);
	}

	public boolean lessequals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return !more(ai, comp);
	}

	// Fonctions pour L-Values
	public AbstractLeekValue increment(AI ai) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue decrement(AI ai) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue pre_increment(AI ai) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue pre_decrement(AI ai) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue add(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue minus(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue multiply(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue divide(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue modulus(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue power(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue band(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue bor(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue bxor(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue bleft(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue bright(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue brotate(AI ai, AbstractLeekValue value) throws LeekRunException {
		throw new LeekRunException(LeekRunException.INVALID_OPERATOR);
	}

	public AbstractLeekValue instanceOf(AI ai, AbstractLeekValue value) throws LeekRunException {
		ai.addOperations(2);
		var clazz = value.getValue();
		if (!(clazz instanceof ClassLeekValue)) {
			ai.addSystemLog(AILog.ERROR, AILog.INSTANCEOF_MUST_BE_CLASS);
			return LeekValueManager.getLeekBooleanValue(false);
		}
		var v = getValue();
		if (v instanceof ObjectLeekValue && ((ObjectLeekValue) v).getClazz().descendsFrom((ClassLeekValue) clazz)) {
			return LeekValueManager.getLeekBooleanValue(true);
		}
		return LeekValueManager.getLeekBooleanValue(false);
	}

	public AbstractLeekValue getField(AI ai, String field) throws LeekRunException {
		// Aucun champ
		ai.addSystemLog(AILog.ERROR, AILog.UNKNOWN_FIELD, new String[] { getString(ai), field });
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue callMethod(AI ai, String method, AbstractLeekValue... arguments) throws LeekRunException {
		// Aucune méthode
		ai.addSystemLog(AILog.ERROR, AILog.UNKNOWN_METHOD, new String[] { getString(ai), method });
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue callSuperMethod(AI ai, String method, AbstractLeekValue... arguments) throws LeekRunException {
		// Aucune méthode
		ai.addSystemLog(AILog.ERROR, AILog.UNKNOWN_METHOD, new String[] { getString(ai), method });
		return LeekValueManager.NULL;
	}

	public void callConstructor(AI ai, AbstractLeekValue thiz, AbstractLeekValue... arguments) throws LeekRunException {}

	public abstract int getType();

	public boolean isReference() {
		return false;
	}

	public abstract Object toJSON(AI ai) throws LeekRunException;

	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue... value) throws LeekRunException {
		// On ne peux pas exécuter ce type de variable
		ai.addSystemLog(AILog.ERROR, AILog.CAN_NOT_EXECUTE_VALUE, new String[] { getString(ai) });
		return LeekValueManager.NULL;
	}

	public static String getParamString(AbstractLeekValue[] parameters) {
		String ret = "";
		for (int j = 0; j < parameters.length; j++) {
			if (j != 0)
				ret += ", ";
			if (parameters[j].getValue().getType() == NUMBER)
				ret += "number";
			else if (parameters[j].getValue().getType() == BOOLEAN)
				ret += "boolean";
			else if (parameters[j].getValue().getType() == STRING)
				ret += "string";
			else if (parameters[j].getValue().getType() == ARRAY)
				ret += "array";
			else if (parameters[j].getValue().getType() == FUNCTION)
				ret += "function";
			else if (parameters[j].getValue().getType() == NULL)
				ret += "null";
			else if (parameters[j].getValue().getType() == OBJECT)
				ret += "object";
			else
				ret += "?";
		}
		return ret;
	}

	public abstract boolean isPrimitive();
}
