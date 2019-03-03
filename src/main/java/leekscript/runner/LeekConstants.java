package leekscript.runner;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class LeekConstants {

	public final static double PI = Math.PI;
	public final static double E = Math.E;

	public final static int INSTRUCTIONS_LIMIT = 300000;
	public final static int OPERATIONS_LIMIT = 20000000;

	public final static int SORT_ASC = 0;
	public final static int SORT_DESC = 1;

	public final static int CELL_EMPTY = 0;
	public final static int CELL_PLAYER = 1;
	public final static int CELL_OBSTACLE = 2;

	public final static int COLOR_RED = 0xFF0000;
	public final static int COLOR_GREEN = 0x00FF00;
	public final static int COLOR_BLUE = 0x0000FF;

	public final static int TYPE_NULL = 0;
	public final static int TYPE_NUMBER = 1;
	public final static int TYPE_BOOLEAN = 2;
	public final static int TYPE_STRING = 3;
	public final static int TYPE_ARRAY = 4;
	public final static int TYPE_FUNCTION = 5;
	
	private static Set<String> extraConstants = new HashSet<>();
	private static String extraConstantsClass;

	public static int getType(String constant) {

		if (constant.equals("E") || constant.equals("PI")) {
			return LeekFunctions.DOUBLE;
		} else if (
				// Colors
				constant.equals("COLOR_RED")
				|| constant.equals("COLOR_GREEN")
				|| constant.equals("COLOR_BLUE")
				||
				// Types
				constant.equals("TYPE_NULL")
				|| constant.equals("TYPE_NUMBER")
				|| constant.equals("TYPE_FUNCTION")
				|| constant.equals("TYPE_ARRAY")
				|| constant.equals("TYPE_STRING")
				|| constant.equals("TYPE_BOOLEAN")
				||
				// Sort
				constant.equals("SORT_ASC") || constant.equals("SORT_DESC")
		) {
			return LeekFunctions.INT;
		}
		return 0;
	}

	public static void setExtraConstants(String extraConstantsClass) {
		LeekConstants.extraConstantsClass = extraConstantsClass;
		try {
			Class<?> extra = Class.forName(extraConstantsClass);
			for (Field constant : extra.getDeclaredFields()) {
				extraConstants.add(constant.getName());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static String getNamespace(String mConstantName) {
		if (extraConstants.contains(mConstantName)) {
			return extraConstantsClass;
		}
		return "LeekConstants";
	}
}
