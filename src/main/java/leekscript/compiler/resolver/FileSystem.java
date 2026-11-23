package leekscript.compiler.resolver;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.IncludeGraph;

public abstract class FileSystem {

	private final Map<Integer, IncludeGraph> graphs = new ConcurrentHashMap<>();

	public abstract Folder getRoot();

	public abstract Folder getRoot(int owner);

	public abstract Folder getRoot(int owner, int farmer);

	public abstract AIFile getFileById(int id, int farmer);

	public abstract Folder getFolderById(int id, int farmer);

	public abstract Folder findFolder(String name, Folder folder);

	public abstract AIFile findFile(String name, Folder folder) throws FileNotFoundException;

	public abstract long getAITimestamp(AIFile ai);

	public abstract void loadDependencies(AIFile ai);

	public abstract long getFolderTimestamp(Folder folder);

	/**
	 * Enumerates all files belonging to an owner: LeekScript, but also polyglot AIs and
	 * docs (callers filter what they need). Required for the in-memory include graph.
	 * Default returns an empty list — FS implementations that want include-graph
	 * support must override.
	 */
	public Iterable<AIFile> listAllFiles(int owner) {
		return List.of();
	}

	/**
	 * Chemin → mtime de tous les fichiers d'un owner (même périmètre que {@link #listAllFiles}).
	 * Le graphe d'includes le relit à chaque requête et ne charge ensuite que les fichiers dont le
	 * mtime a changé : un FileSystem qui sait lister sans lire le contenu des fichiers doit le
	 * surcharger, sinon chaque requête du graphe relit tout le dossier.
	 */
	public Map<String, Long> listAllTimestamps(int owner) {
		var timestamps = new HashMap<String, Long>();
		for (var file : listAllFiles(owner)) {
			timestamps.put(file.getPath(), getAITimestamp(file));
		}
		return timestamps;
	}

	/**
	 * Charge la liste des includes d'un AIFile depuis une source persistante (typiquement la DB).
	 * Permet à JavaCompiler.effectiveTimestamp de reconstruire file.includedAIs après un restart
	 * du process (sinon null tant qu'aucun analyze n'a tourné).
	 *
	 * Retourne null si pas de source persistante (mode embedded / tests).
	 */
	public Set<AIFile> loadIncludedAIs(AIFile file) {
		// Transitive : include chain Main → A → B → C must all contribute their mtime
		// to effectiveTimestamp(Main), otherwise modifying C alone never invalidates
		// Main's compiled cache.
		return resolveAll(file.getOwner(), graph(file.getOwner()).getTransitivelyIncluded(file.getPath()));
	}

	/**
	 * Fichiers LeekScript qui lisent le fichier {@code path} de {@code owner} par une fonction à
	 * argument fichier. Par chemin : le fichier lu peut avoir été supprimé ou renommé.
	 */
	public Set<AIFile> getFileReaders(int owner, String path) {
		return resolveAll(owner, graph(owner).getFileReaders(path));
	}

	/**
	 * Returns the root entrypoints that transitively include the given file.
	 * A root entrypoint is a file that has no includers of its own.
	 * Used to decide which entrypoint governs the analysis of a file — we want
	 * the version/pragma of the top-of-the-chain entrypoint, not an intermediate.
	 */
	public Set<AIFile> getIncluders(AIFile file) {
		return getIncluders(List.of(file));
	}

	/**
	 * Réunion des {@link #getIncluders(AIFile)} de chaque fichier.
	 * Le graphe n'est rafraîchi qu'une fois par owner, au lieu d'une fois par fichier : pour une IA
	 * qui inclut des centaines de fichiers, chaque rafraîchissement relit tout le dossier.
	 */
	public Set<AIFile> getIncluders(Collection<AIFile> files) {
		var byOwner = new LinkedHashMap<Integer, List<String>>();
		for (var file : files) {
			byOwner.computeIfAbsent(file.getOwner(), o -> new ArrayList<>()).add(file.getPath());
		}
		var result = new LinkedHashSet<AIFile>();
		for (var entry : byOwner.entrySet()) {
			result.addAll(resolveAll(entry.getKey(), graph(entry.getKey()).getRootEntrypoints(entry.getValue())));
		}
		return result;
	}

	private IncludeGraph graph(int owner) {
		return graphs.computeIfAbsent(owner, o -> new IncludeGraph(this, o));
	}

	/** Chemins du graphe → fichiers ; un fichier disparu entre-temps est sauté. */
	private Set<AIFile> resolveAll(int owner, Set<String> paths) {
		if (paths.isEmpty()) return Collections.emptySet();
		var root = getRoot(owner);
		if (root == null) return Collections.emptySet();
		var result = new HashSet<AIFile>();
		for (var path : paths) {
			var file = resolveOrNull(root, path);
			if (file != null) result.add(file);
		}
		return result;
	}

	/** Le fichier {@code path} sous {@code root}, null s'il a disparu depuis le parcours du graphe ou ne se charge pas. */
	public static AIFile resolveOrNull(Folder root, String path) {
		try {
			return root.resolve(path);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Le fichier est-il équipé sur un poireau (ou tout autre usage où il est compilé SEUL,
	 * comme point d'entrée) ? Un tel fichier est analysé comme racine même s'il est inclus
	 * ailleurs : sa propre compilation fait foi. Faux par défaut (tests, mode embarqué) ;
	 * les FileSystem adossés au serveur surchargent.
	 */
	public boolean isEquipped(AIFile file) {
		return false;
	}
}
