package leekscript.runner.classes;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.AI.NativeObjectLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.ClassLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.LegacyHybridContainerLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.common.Type;

public class StandardClass {

	public static Type getType(AI ai, Object value) throws LeekRunException {
		if (value == null) return Type.NULL;
		if (value instanceof Long) return Type.INT;
		if (value instanceof Double) return Type.REAL;
		if (value instanceof Boolean) return Type.BOOL;
		if (value instanceof LegacyHybridContainerLeekValue || value instanceof ArrayLeekValue) return Type.ARRAY;
		if (value instanceof MapLeekValue) return Type.MAP;
		if (value instanceof String) return Type.STRING;
		if (value instanceof ObjectLeekValue o) return o.clazz.getType();
		if (value instanceof NativeObjectLeekValue o) return ai.classOf(o).getType();
		if (value instanceof ClassLeekValue) return Type.CLASS;
		if (value instanceof FunctionLeekValue) return Type.FUNCTION;
		throw new RuntimeException("Valeur invalide : " + value);
	}
}
