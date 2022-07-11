package leekscript.runner.values;

public class LeekValue {

	public final static int NUMBER_V1 = 1;
	public final static int BOOLEAN_V1 = 2;
	public final static int ARRAY_V1 = 3;
	public final static int NULL_V1 = 4;
	public final static int STRING_V1 = 5;
	public final static int FUNCTION_V1 = 6;
	public final static int CLASS_V1 = 7;
	public final static int OBJECT_V1 = 8;

	public final static int NULL = 0;
	public final static int NUMBER = 1;
	public final static int BOOLEAN = 2;
	public final static int STRING = 3;
	public final static int ARRAY = 4;
	public final static int FUNCTION = 5;
	public final static int CLASS = 6;
	public final static int OBJECT = 7;
	public final static int MAP = 8;
	public final static int SET = 9;
	public final static int INTERVAL = 10;

	public final static int ADD_COST = 1;
	public final static int MUL_COST = 2;
	public final static int DIV_COST = 5;
	public final static int MOD_COST = 5;
	public final static int POW_COST = 40;

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
			else if (v instanceof ArrayLeekValue || v instanceof LegacyArrayLeekValue)
				ret += "array";
			else if (v instanceof MapLeekValue)
				ret += "map";
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
