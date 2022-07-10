package leekscript.runner;

import java.util.HashMap;

import leekscript.common.Type;

public enum LeekConstants implements ILeekConstant {

	PI(Math.PI, Type.REAL),
	E(Math.E, Type.REAL),
	Infinity(Double.POSITIVE_INFINITY, Type.REAL),
	NaN(Double.NaN, Type.REAL),

	INSTRUCTIONS_LIMIT(300000, Type.INT),
	OPERATIONS_LIMIT(20000000, Type.INT),

	SORT_ASC(0, Type.INT),
	SORT_DESC(1, Type.INT),

	CELL_EMPTY(0, Type.INT),
	CELL_PLAYER(1, Type.INT),
	CELL_OBSTACLE(2, Type.INT),

	COLOR_RED(0xFF0000, Type.INT),
	COLOR_GREEN(0x00FF00, Type.INT),
	COLOR_BLUE(0x0000FF, Type.INT),

	TYPE_NULL(0, Type.INT),
	TYPE_NUMBER(1, Type.INT),
	TYPE_BOOLEAN(2, Type.INT),
	TYPE_STRING(3, Type.INT),
	TYPE_ARRAY(4, Type.INT),
	TYPE_FUNCTION(5, Type.INT),
	TYPE_CLASS(6, Type.INT),
	TYPE_OBJECT(7, Type.INT),
	TYPE_MAP(8, Type.INT);

	private static HashMap<String, ILeekConstant> extraConstants = new HashMap<String, ILeekConstant>();

	private double value;
	private Type type;

	LeekConstants(double value, Type type) {
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
	public Type getType() {
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
