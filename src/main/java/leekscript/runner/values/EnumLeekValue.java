package leekscript.runner.values;

import java.util.HashMap;
import java.util.Map;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class EnumLeekValue {

	public final AI ai;
	public final String name;
	private final Map<String, EnumConstant> constants = new HashMap<>();
	private long nextAutoValue = 0;

	public EnumLeekValue(AI ai, String name) {
		this.ai = ai;
		this.name = name;
	}

	public void addConstant(String name, Object value) {
		long numericValue;
		if (value instanceof Number n) {
			numericValue = n.longValue();
			nextAutoValue = numericValue + 1;
		} else {
			numericValue = nextAutoValue++;
		}
		constants.put(name, new EnumConstant(this, name, numericValue));
	}

	public Object getField(String field) throws LeekRunException {
		return constants.get(field);
	}

	public static class EnumConstant {
		public final EnumLeekValue enumType;
		public final String name;
		public final long value;

		public EnumConstant(EnumLeekValue enumType, String name, long value) {
			this.enumType = enumType;
			this.name = name;
			this.value = value;
		}
	}
}
