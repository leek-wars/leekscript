package leekscript.functions;

import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;

import leekscript.runner.LeekFunctions;

public class Functions {

	private static Map<String, VariableOperations> sVariableOperations = new TreeMap<String, VariableOperations>();

	public static void addFunctionOperations(String function, int operations) {
		LeekFunctions.getValue(function).setOperations(operations);
	}

	public static void addVariableOperations(String name, String variableOperations) {
		sVariableOperations.put(name, new VariableOperations(JSONObject.parseObject(variableOperations)));
	}

	public static VariableOperations getVariableOperations(String name) {
		return sVariableOperations.get(name);
	}
}
