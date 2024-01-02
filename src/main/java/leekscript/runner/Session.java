package leekscript.runner;

import java.util.TreeMap;

import leekscript.runner.values.Box;

public class Session {

	private TreeMap<String, Box<Object>> variables = new TreeMap<>();

	public Session() {

	}

	public void setVariable(AI ai, String variable, Object value) throws LeekRunException {
		this.variables.put(variable, new Box<Object>(ai, value));
	}

	public TreeMap<String, Box<Object>> getVariables() {
		return variables;
	}

	public Box<Object> getVariable(String name) {
		return variables.get(name);
	}
}
