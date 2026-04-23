package leekscript.runner;

import java.util.TreeMap;

import leekscript.compiler.LeekScript;
import leekscript.runner.values.Box;

public class Session {

	private TreeMap<String, Box<Object>> variables = new TreeMap<>();
	private int version;
	private boolean strict;

	public void rebindAll(AI ai) {
		var visited = LeekOperations.newVisitedSet();
		for (var box : variables.values()) {
			box.rebind(ai, visited);
		}
	}

	public Session() {
		this(LeekScript.LATEST_VERSION, true);
	}

	public Session(int version, boolean strict) {
		this.version = version;
		this.strict = strict;
	}

	public void setVariable(AI ai, String variable, Object value) throws LeekRunException {
		// Ne pas utiliser le constructeur Box(ai, value) : il facture 1 op,
		// ce qui gonflerait artificiellement le coût de `var x = …` dans la
		// console (le stockage en session est un détail d'implémentation du REPL).
		var box = new Box<Object>(ai);
		box.setRef(value);
		this.variables.put(variable, box);
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
