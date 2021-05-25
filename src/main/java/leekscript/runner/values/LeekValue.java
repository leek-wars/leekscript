package leekscript.runner.values;

public class LeekValue {

	public final static int NUMBER_V10 = 1;
	public final static int BOOLEAN_V10 = 2;
	public final static int ARRAY_V10 = 3;
	public final static int NULL_V10 = 4;
	public final static int STRING_V10 = 5;
	public final static int FUNCTION_V10 = 6;
	public final static int CLASS_V10 = 7;
	public final static int OBJECT_V10 = 8;

	public final static int NULL = 0;
	public final static int NUMBER = 1;
	public final static int BOOLEAN = 2;
	public final static int STRING = 3;
	public final static int ARRAY = 4;
	public final static int FUNCTION = 5;
	public final static int CLASS = 6;
	public final static int OBJECT = 7;

	public final static int ADD_COST = 1;
	public final static int MUL_COST = 5;
	public final static int DIV_COST = 5;
	public final static int MOD_COST = 5;
	public final static int POW_COST = 140;

	public static String getParamString(Object[] parameters) {
		String ret = "";
		for (int j = 0; j < parameters.length; j++) {
			if (j != 0)
				ret += ", ";
			var v = parameters[j];
			if (v == null)
				ret += "null";
			else if (v instanceof Number)
				ret += "number";
			else if (v instanceof Boolean)
				ret += "boolean";
			else if (v instanceof String)
				ret += "string";
			else if (v instanceof ArrayLeekValue)
				ret += "array";
			else if (v instanceof FunctionLeekValue)
				ret += "function";
			else if (v instanceof ObjectLeekValue)
				ret += "object";
			else
				ret += "?";
		}
		return ret;
	}
}
