package leekscript.runner;

import java.util.HashMap;

public enum LeekConstants implements ILeekConstant {

	PI(Math.PI, LeekFunctions.DOUBLE),
	E(Math.E, LeekFunctions.DOUBLE),

	INSTRUCTIONS_LIMIT(300000, LeekFunctions.INT),
	OPERATIONS_LIMIT(20000000, LeekFunctions.INT),

	SORT_ASC(0, LeekFunctions.INT),
	SORT_DESC(1, LeekFunctions.INT),

	CELL_EMPTY(0, LeekFunctions.INT),
	CELL_PLAYER(1, LeekFunctions.INT),
	CELL_OBSTACLE(2, LeekFunctions.INT),

	COLOR_RED(0xFF0000, LeekFunctions.INT),
	COLOR_GREEN(0x00FF00, LeekFunctions.INT),
	COLOR_BLUE(0x0000FF, LeekFunctions.INT),

	TYPE_NULL(0, LeekFunctions.INT),
	TYPE_NUMBER(1, LeekFunctions.INT),
	TYPE_BOOLEAN(2, LeekFunctions.INT),
	TYPE_STRING(3, LeekFunctions.INT),
	TYPE_ARRAY(4, LeekFunctions.INT),
	TYPE_FUNCTION(5, LeekFunctions.INT),
	TYPE_OBJECT(6, LeekFunctions.INT);

	private static HashMap<String, ILeekConstant> extraConstants = new HashMap<String, ILeekConstant>();

	private double value;
	private int type;

	LeekConstants(double value, int type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public double getValue() {
		return value;
	}
	@Override
	public int getIntValue() {
		return (int) value;
	}
	@Override
	public int getType() {
		return type;
	}

	public static void setExtraConstants(String extraConstantsClass) {
		try {
			Class<?> extra = Class.forName(extraConstantsClass);
			for (Object constant : extra.getEnumConstants()) {
				extraConstants.put(constant.toString(), (ILeekConstant) constant);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static ILeekConstant get(String name) {
		for (LeekConstants constant : LeekConstants.values()) {
			if (constant.name().equals(name))
				return constant;
		}
		if (extraConstants.containsKey(name)) {
			return extraConstants.get(name);
		}
		return null;
	}
}
