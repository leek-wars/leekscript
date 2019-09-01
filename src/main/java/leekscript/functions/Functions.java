package leekscript.functions;

import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;

import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekFunctions;

public class Functions {

	private static Map<String, VariableOperations> sVariableOperations = new TreeMap<String, VariableOperations>();

	public static void addFunctionOperations(String function, int operations, String variableOperations) {
		ILeekFunction f = LeekFunctions.getValue(function);
		if (f != null) {
			f.setOperations(Math.max(1, operations));
		}
		if (variableOperations != null) {
			sVariableOperations.put(function, new VariableOperations(JSONObject.parseObject(variableOperations)));
		}
	}

	public static VariableOperations getVariableOperations(String name) {
		return sVariableOperations.get(name);
	}
}
