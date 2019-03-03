package leekscript.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;

public class Functions {

	private static boolean sReady = false;
	private static List<Function> sFunctions = new ArrayList<Function>();
	private static Map<String, VariableOperations> sVariableOperations = new TreeMap<String, VariableOperations>();

	public static void addFunction(Function function) {
		sFunctions.add(function);
	}

	public static void addVariableOperations(String name, String variableOperations) {
		sVariableOperations.put(name, new VariableOperations(JSONObject.parseObject(variableOperations)));
	}

	public static VariableOperations getVariableOperations(String name) {
		return sVariableOperations.get(name);
	}

	public static boolean isReady() {

		return sReady;
	}

	public static void setReady(boolean ready) {
		sReady = ready;
	}

	public static Function getFunction(String name, int params) {
		for (Function f : sFunctions) {
			if (f.getName().equals(name) && f.countArguments() == params)
				return f;
		}
		return null;
	}

	public static Integer getOperations(String name) {
		for (Function f : sFunctions) {
			if (f.getName().equals(name)) {
				return f.getOperations();
			}
		}
		return 1;
	}
}
