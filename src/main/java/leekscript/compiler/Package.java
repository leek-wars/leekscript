package leekscript.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un package v5 (docs/ls5-modules.md §2) : un dossier dont le fichier `package.leek` commence par
 * `package <nom>` puis des `depends <alias> from "<dossier>"`. Les fichiers hors de tout package
 * forment le package du programme, dont l'en-tête est celui du fichier d'entrée (`depends`
 * seulement). L'en-tête se lit sur les jetons, sans compiler : le graphe des dépendances est
 * vérifié avant toute compilation.
 *
 * <p>`depends test <alias> from "<dossier>"` : dépendance des seuls modules du dossier `test/` du
 * package, hors du graphe de prod (cf {@link #getTestDepends}).
 */
public class Package {

	public static final String ROOT_FILE = "package";

	public record Dependency(Token alias, String path) {}

	private final Folder folder; // null : package du programme
	private final AIFile root;
	private final Token name; // `package <nom>`, null si absent
	private final Map<String, Dependency> depends = new LinkedHashMap<>();
	private final Map<String, Dependency> testDepends = new LinkedHashMap<>();

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

	/** Les dépendances de prod (`depends`), sans celles des tests. */
	public Map<String, Dependency> getDepends() {
		return depends;
	}

	/**
	 * Les dépendances de test (`depends test`) : seuls les modules du dossier `test/` du package
	 * les importent, et elles ne comptent dans le graphe que quand on compile ses tests. Un alias
	 * déjà déclaré par un `depends` n'y figure pas.
	 */
	public Map<String, Dependency> getTestDepends() {
		return testDepends;
	}

	/** Nom du dossier des tests d'un package, sous son dossier. */
	public static final String TEST_FOLDER = "test";

	/**
	 * Le dossier `folder` est-il le dossier `test/` de ce package ou l'un de ses sous-dossiers ?
	 * Toujours faux pour le package du programme, qui n'a pas de dossier à lui.
	 */
	public boolean isTestFolder(Folder folder) {
		if (this.folder == null) return false;
		var home = folderKey(this.folder);
		// On remonte jusqu'au dossier placé juste sous celui du package. Un nom de dossier peut être
		// un chemin complet (NativeFileSystem) : on n'en compare que le dernier élément.
		for (var current = folder; current != null && current.getParent() != null && current.getParent() != current; current = current.getParent()) {
			if (folderKey(current.getParent()).equals(home)) return TEST_FOLDER.equals(lastSegment(current.getName()));
		}
		return false;
	}

	private static String lastSegment(String name) {
		return name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf(java.io.File.separatorChar)) + 1);
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

	/**
	 * Package le plus proche en remontant de `folder` (dossier qui contient une racine), null s'il n'y
	 * en a pas (le fichier est alors dans le package du programme). `cache` (facultatif) garde le
	 * résultat de chaque dossier visité, par folderKey.
	 */
	public static Package nearest(Folder folder, Map<String, Package> cache) {
		var visited = new java.util.ArrayList<String>();
		Package found = null;
		for (var current = folder; current != null; current = current.getParent()) {
			var key = folderKey(current);
			if (cache != null && cache.containsKey(key)) {
				found = cache.get(key);
				break;
			}
			visited.add(key);
			var root = findRoot(current, "");
			if (root != null) {
				found = read(current, root);
				break;
			}
			if (current.getParent() == current) break;
		}
		if (cache != null) for (var key : visited) cache.put(key, found);
		return found;
	}

	/** Clé d'un dossier, stable d'une instance de Folder à l'autre : propriétaire et chemin. */
	private static String folderKey(Folder folder) {
		var names = new java.util.ArrayDeque<String>();
		for (var f = folder; f != null && f.getParent() != f; f = f.getParent()) names.addFirst(f.getName());
		return folder.getOwner() + ":/" + String.join("/", names);
	}

	/** Racine de la dépendance `dependency` de ce package, null si absente ; `programFolder` : dossier de l'entrée. */
	public AIFile dependencyRoot(Dependency dependency, Folder programFolder) {
		return findRoot(isProgram() ? programFolder : folder, dependency.path());
	}

	/**
	 * Racine du package `alias` déclaré par un `depends` du package de `file`, null sinon. Hors
	 * compilation (graphe des dépendances du cache des IA) : un fichier hors de tout package lit
	 * l'en-tête de son propre fichier, comme le fichier d'entrée d'un programme. Un fichier du
	 * dossier `test/` de son package voit aussi les `depends test`.
	 */
	public static AIFile resolveDependency(AIFile file, String alias) {
		var owner = nearest(file.getFolder(), null);
		if (owner == null) owner = read(null, file);
		var dependency = owner.getDepends().get(alias);
		if (dependency == null && owner.isTestFolder(file.getFolder())) dependency = owner.getTestDepends().get(alias);
		return dependency == null ? null : owner.dependencyRoot(dependency, file.getFolder());
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

	/** Lit l'en-tête de `root` : `package <nom>` (racine de package) puis les `depends` et `depends test`. */
	public static Package read(Folder folder, AIFile root) {
		// Cas courant : aucun en-tête, inutile de relire le fichier en jetons
		var code = root.getCode();
		if (code == null || !code.contains("package") && !code.contains("depends")) return new Package(folder, root, null);
		var stream = tokens(root);
		var tokens = stream == null ? java.util.List.<Token>of() : stream.getTokens();
		java.util.function.IntFunction<Token> at = i -> i < tokens.size() ? tokens.get(i) : null;
		int i = 0;
		Token name = null;
		if (isPackageLine(at, i)) {
			name = at.apply(i + 1);
			i = skipEnd(at, i + 2);
		}
		var result = new Package(folder, root, name);
		for (int length; (length = dependsLineLength(at, i)) > 0; i = skipEnd(at, i + length)) {
			var alias = at.apply(i + length - 3);
			var path = at.apply(i + length - 1).getWord();
			var dependency = new Dependency(alias, path.substring(1, path.length() - 1));
			(length == TEST_DEPENDS_LENGTH ? result.testDepends : result.depends).putIfAbsent(alias.getWord(), dependency);
		}
		// Un alias déclaré des deux façons est une dépendance de prod
		result.testDepends.keySet().removeAll(result.depends.keySet());
		return result;
	}

	private static int skipEnd(java.util.function.IntFunction<Token> at, int i) {
		var token = at.apply(i);
		return token != null && token.getType() == TokenType.END_INSTRUCTION ? i + 1 : i;
	}

	/** `package <nom>` à partir du jeton `i` (`at` : null ou jeton de fin au-delà du fichier). */
	public static boolean isPackageLine(java.util.function.IntFunction<Token> at, int i) {
		return is(at.apply(i), TokenType.PACKAGE) && is(at.apply(i + 1), TokenType.STRING);
	}

	private static final int DEPENDS_LENGTH = 4;
	public static final int TEST_DEPENDS_LENGTH = 5;

	/**
	 * Nombre de jetons de la ligne `depends <alias> from "<chemin>"` (4) ou `depends test <alias>
	 * from "<chemin>"` (5) à partir du jeton `i`, 0 si ce n'en est pas une. `test` n'est un mot de
	 * l'en-tête que suivi d'un alias : `depends test from "…"` déclare l'alias `test`.
	 */
	public static int dependsLineLength(java.util.function.IntFunction<Token> at, int i) {
		var keyword = at.apply(i);
		if (!is(keyword, TokenType.STRING) || !keyword.getWord().equals("depends")) return 0;
		if (isAliasFrom(at, i + 1)) return DEPENDS_LENGTH;
		var test = at.apply(i + 1);
		if (is(test, TokenType.STRING) && test.getWord().equals("test") && isAliasFrom(at, i + 2)) return TEST_DEPENDS_LENGTH;
		return 0;
	}

	// `<alias> from "<chemin>"` à partir du jeton `i`
	private static boolean isAliasFrom(java.util.function.IntFunction<Token> at, int i) {
		var from = at.apply(i + 1);
		return is(at.apply(i), TokenType.STRING)
			&& from != null && from.getWord().equals("from")
			&& is(at.apply(i + 2), TokenType.VAR_STRING);
	}

	private static boolean is(Token token, TokenType type) {
		return token != null && token.getType() == type;
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
