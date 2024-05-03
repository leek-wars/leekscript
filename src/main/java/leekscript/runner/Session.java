package leekscript.runner;

import java.util.TreeMap;

import leekscript.compiler.LeekScript;
import leekscript.runner.values.Box;

public class Session {

	private TreeMap<String, Box<Object>> variables = new TreeMap<>();
	private int version;
	private boolean strict;

	public Session() {
		this(LeekScript.LATEST_VERSION, true);
	}

	public Session(int version, boolean strict) {
		this.version = version;
		this.strict = strict;
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

	public int getVersion() {
		return version;
	}

	public boolean isStrict() {
		return strict;
	}
}
