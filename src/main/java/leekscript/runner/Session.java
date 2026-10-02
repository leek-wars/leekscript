package leekscript.runner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.TreeMap;

import leekscript.common.Type;
import leekscript.compiler.LeekScript;
import leekscript.runner.values.Box;

public class Session {

	private TreeMap<String, Box<Object>> variables = new TreeMap<>();
	// Type déclaré des variables (`integer a = 1`), pour qu'une ligne suivante ne les relise pas en `any`
	// (#5226 : `BigInteger a = 1` puis `a << 64` décalait un long). Seuls les types sans lien avec une
	// classe de l'IA compilée sont retenus, cf `keepsType`.
	private TreeMap<String, Type> types = new TreeMap<>();
	private int version;
	private boolean strict;
	// IA des lignes déjà jouées. Une fonction définie sur une ligne garde son code, et donc son
	// compteur d'opérations, dans l'IA de cette ligne : l'appeler plus tard n'est pas compté sur
	// la ligne courante. Références faibles : une ligne dont plus rien n'est appelable s'en va.
	private final ArrayList<WeakReference<AI>> previous = new ArrayList<>();
	private AI current;

	public void rebindAll(AI ai) {
		var visited = LeekOperations.newVisitedSet();
		for (var box : variables.values()) {
			box.rebind(ai, visited);
		}
		if (current != null && current != ai) {
			previous.add(new WeakReference<>(current));
		}
		current = ai;
		previous.removeIf(ref -> ref.get() == null);
		for (var ref : previous) {
			var line = ref.get();
			if (line != null) line.resetCounter();
		}
	}

	/**
	 * Coût d'une ligne : ses propres opérations depuis `before` (lu après la construction de
	 * l'IA), plus les appels qu'elle a faits aux fonctions des lignes passées.
	 */
	public long lineOperations(AI ai, long before) {
		return ai.operations() - before + previousOperations();
	}

	/**
	 * Opérations consommées dans les IA des lignes passées depuis le début de la ligne
	 * courante (appels de leurs fonctions), à ajouter à celles de la ligne elle-même.
	 */
	public long previousOperations() {
		long ops = 0;
		for (var ref : previous) {
			var line = ref.get();
			if (line != null) ops += line.operations();
		}
		return ops;
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

	public static boolean keepsType(Type type) {
		return type == Type.INT || type == Type.REAL || type == Type.BOOL || type == Type.STRING || type == Type.BIG_INT;
	}

	public void setType(String name, Type type) {
		if (keepsType(type)) types.put(name, type);
		else types.remove(name);
	}

	public Type getType(String name) {
		return types.getOrDefault(name, Type.ANY);
	}

	public int getVersion() {
		return version;
	}

	public boolean isStrict() {
		return strict;
	}
}
