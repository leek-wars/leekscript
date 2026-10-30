package leekscript.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un package v5 (docs/ls5-modules.md §2) : un dossier dont le fichier `package.leek` commence par
 * `package <nom>` puis des `depends <alias> from "<dossier>"`. Les fichiers hors de tout package
 * forment le package du programme, dont l'en-tête est celui du fichier d'entrée (`depends`
 * seulement). L'en-tête se lit sur les jetons, sans compiler : le graphe des dépendances est
 * vérifié avant toute compilation.
 */
public class Package {

	public static final String ROOT_FILE = "package";

	public record Dependency(Token alias, String path) {}

	private final Folder folder; // null : package du programme
	private final AIFile root;
	private final Token name; // `package <nom>`, null si absent
	private final Map<String, Dependency> depends = new LinkedHashMap<>();

	private Package(Folder folder, AIFile root, Token name) {
		this.folder = folder;
		this.root = root;
		this.name = name;
	}

	public boolean isProgram() {
		return folder == null;
	}

	public Folder getFolder() {
		return folder;
	}

	public AIFile getRoot() {
		return root;
	}

	public Token getName() {
		return name;
	}

	public Map<String, Dependency> getDepends() {
		return depends;
	}

	/**
	 * Racine du package du dossier `folder` + `path` (`path` vide : `folder` lui-même), null s'il n'y
	 * en a pas. Les fichiers d'IA portent en général leur extension : `package.leek` d'abord.
	 */
	public static AIFile findRoot(Folder folder, String path) {
		var prefix = path.isEmpty() ? "" : path + "/";
		for (var name : new String[] { ROOT_FILE + ".leek", ROOT_FILE }) {
			try {
				return folder.resolve(prefix + name);
			} catch (java.io.FileNotFoundException e) {
				// nom suivant
			}
		}
		return null;
	}

	/** `package.leek` (avec ou sans extension) : la racine d'un package. */
	public static boolean isRootFile(AIFile file) {
		var name = file.getName();
		name = name.substring(name.lastIndexOf('/') + 1);
		return name.equals(ROOT_FILE) || name.equals(ROOT_FILE + ".leek");
	}

	// Un même package peut être lu depuis deux instances de son dossier : identité = sa racine
	@Override
	public boolean equals(Object other) {
		return other instanceof Package p && p.root.getKey().equals(root.getKey());
	}

	@Override
	public int hashCode() {
		return root.getKey().hashCode();
	}

	/** Lit l'en-tête de `root` : `package <nom>` (racine de package) puis les `depends`. */
	public static Package read(Folder folder, AIFile root) {
		// Cas courant : aucun en-tête, inutile de relire tout le fichier en jetons
		var code = root.getCode();
		if (code == null || !code.contains("package") && !code.contains("depends")) return new Package(folder, root, null);
		var stream = tokens(root);
		Token name = null;
		var tokens = new java.util.ArrayList<Token>();
		if (stream != null) {
			// Le fichier peut être en cours de compilation : on rend le curseur tel quel
			var position = stream.getPosition();
			try {
				stream.reset();
				while (stream.hasMoreTokens()) tokens.add(stream.eat());
			} finally {
				stream.setPosition(position);
			}
		}
		int i = 0;
		if (i + 1 < tokens.size() && tokens.get(i).getType() == TokenType.PACKAGE && tokens.get(i + 1).getType() == TokenType.STRING) {
			name = tokens.get(i + 1);
			i += 2;
			if (i < tokens.size() && tokens.get(i).getType() == TokenType.END_INSTRUCTION) i++;
		}
		var result = new Package(folder, root, name);
		while (isDepends(tokens, i)) {
			var alias = tokens.get(i + 1);
			var path = tokens.get(i + 3).getWord();
			result.depends.putIfAbsent(alias.getWord(), new Dependency(alias, path.substring(1, path.length() - 1)));
			i += 4;
			if (i < tokens.size() && tokens.get(i).getType() == TokenType.END_INSTRUCTION) i++;
		}
		return result;
	}

	/** `depends <alias> from "<chemin>"` à partir du jeton `i`. */
	static boolean isDepends(java.util.List<Token> tokens, int i) {
		return i + 3 < tokens.size()
			&& tokens.get(i).getType() == TokenType.STRING && tokens.get(i).getWord().equals("depends")
			&& tokens.get(i + 1).getType() == TokenType.STRING
			&& tokens.get(i + 2).getWord().equals("from")
			&& tokens.get(i + 3).getType() == TokenType.VAR_STRING;
	}

	// Jetons lus pour l'en-tête seulement : ni gardés sur le fichier ni ses erreurs de lexique
	// retenues, sa compilation les relira et signalera ses erreurs elle-même.
	private static LexicalParserTokenStream tokens(AIFile file) {
		if (file.hasBeenParsed()) return file.getTokenStream();
		try {
			var declared = PragmaParser.declaredVersion(file);
			return new LexicalParser(file, declared != null ? declared : file.getVersion()).parse(error -> {});
		} catch (Exception e) {
			return null;
		}
	}
}
