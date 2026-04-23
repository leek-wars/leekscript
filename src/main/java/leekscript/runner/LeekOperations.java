package leekscript.runner;

import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Set;

import leekscript.ErrorManager;
import leekscript.runner.AI.NativeObjectLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.Box;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.runner.values.SetLeekValue;

public class LeekOperations {

	/**
	 * Rebinds a value graph to a new AI so that subsequent ops() calls from
	 * methods on MapLeekValue/ArrayLeekValue/Box/... are accounted on `ai`,
	 * not on the (possibly dead) AI that originally created the value.
	 * Used by the REPL/Session so that ops reported per snippet match the
	 * cost of operations performed in that snippet.
	 */
	public static void rebind(AI ai, Object value, Set<Object> visited) {
		if (value == null) return;
		if (value instanceof Box<?> b) b.rebind(ai, visited);
		else if (value instanceof MapLeekValue m) m.rebind(ai, visited);
		else if (value instanceof ArrayLeekValue a) a.rebind(ai, visited);
		else if (value instanceof SetLeekValue s) s.rebind(ai, visited);
		else if (value instanceof LegacyArrayLeekValue l) l.rebind(ai, visited);
		else if (value instanceof ObjectLeekValue o) o.rebind(ai, visited);
	}

	/**
	 * Identity semantics: deux collections structurellement égales
	 * (ex. [1,2,3] et un autre [1,2,3]) doivent être rebind séparément,
	 * et on ne veut pas déclencher leurs `.equals()` qui peuvent être coûteux.
	 */
	public static Set<Object> newVisitedSet() {
		return java.util.Collections.newSetFromMap(new IdentityHashMap<>());
	}

	public static Object clone(AI ai, Object value) throws LeekRunException {
		return clone(ai, value, 1);
	}

	public static Object clone(AI ai, Object value, int level) throws LeekRunException {
		if (value instanceof LegacyArrayLeekValue) {
			if (level == 0) return value;
			var array = (LegacyArrayLeekValue) value;
			ai.ops(1 + array.size() * (LegacyArrayLeekValue.ARRAY_CELL_CREATE_OPERATIONS));
			return new LegacyArrayLeekValue(ai, array, level);
		} else if (value instanceof ArrayLeekValue) {
			if (level == 0) return value;
			var array = (ArrayLeekValue) value;
			ai.ops(1 + array.size());
			return new ArrayLeekValue(ai, array, level);
		} else if (value instanceof MapLeekValue) {
			if (level == 0) return value;
			var map = (MapLeekValue) value;
			ai.ops(1 + map.size());
			return new MapLeekValue(ai, map, level);
		} else if (value instanceof ObjectLeekValue) {
			if (level == 0) return value;
			ai.ops(1);
			return new ObjectLeekValue(ai, (ObjectLeekValue) value, level);
		} else if (value instanceof SetLeekValue set) {
			if (level == 0) return value;
			ai.ops(1);
			return new SetLeekValue(ai, set, level);
		} else if (value instanceof NativeObjectLeekValue o) {
			if (level == 0) return value;

			ai.ops(1 + o.size());
			
			// Call copy constructor
			Object object = null;
			try {
				object = o.getClass().getConstructor(ai.getClass(), o.getClass(), int.class).newInstance(ai, o, level);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
				ErrorManager.exception(e1);
			}
			ai.allocateRAM(object, 2 * o.size());
			return object;
		}
		return value;
	}

	public static boolean equals_equals(AI ai, Object v1, Object v2) throws LeekRunException {
		ai.ops(1);
		if (v1 instanceof ObjectLeekValue && v2 instanceof ObjectLeekValue) {
			return v1 == v2;
		}
		if (v1 instanceof NativeObjectLeekValue && v2 instanceof NativeObjectLeekValue) {
			return v1 == v2;
		}
		return LeekValueManager.getType(v1) == LeekValueManager.getType(v2) && ai.eq(v1, v2);
	}

	public static boolean notequals_equals(AI ai, Object v1, Object v2) throws LeekRunException {
		return !equals_equals(ai, v1, v2);
	}
}
