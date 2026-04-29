package leekscript.compiler.resolver;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
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
	 * Enumerates all .leek files belonging to an owner. Required for the in-memory
	 * include graph. Default returns an empty list — FS implementations that want
	 * include-graph support must override.
	 */
	public Iterable<AIFile> listAllFiles(int owner) {
		return List.of();
	}

	/**
	 * Charge la liste des includes d'un AIFile depuis une source persistante (typiquement la DB).
	 * Permet à JavaCompiler.effectiveTimestamp de reconstruire file.includedAIs après un restart
	 * du process (sinon null tant qu'aucun analyze n'a tourné).
	 *
	 * Retourne null si pas de source persistante (mode embedded / tests).
	 */
	public Set<AIFile> loadIncludedAIs(AIFile file) {
		var graph = graphs.computeIfAbsent(file.getOwner(), o -> new IncludeGraph(this, o));
		// Transitive : include chain Main → A → B → C must all contribute their mtime
		// to effectiveTimestamp(Main), otherwise modifying C alone never invalidates
		// Main's compiled cache.
		var paths = graph.getTransitivelyIncluded(file.getPath());
		var root = getRoot(file.getOwner());
		if (root == null) return Collections.emptySet();
		var result = new HashSet<AIFile>();
		for (var path : paths) {
			try {
				var inc = root.resolve(path);
				if (inc != null) result.add(inc);
			} catch (Exception e) {
				// skip
			}
		}
		return result;
	}

	/**
	 * Returns the root entrypoints that transitively include the given file.
	 * A root entrypoint is a file that has no includers of its own.
	 * Used to decide which entrypoint governs the analysis of a file — we want
	 * the version/pragma of the top-of-the-chain entrypoint, not an intermediate.
	 */
	public Set<AIFile> getIncluders(AIFile file) {
		var graph = graphs.computeIfAbsent(file.getOwner(), o -> new IncludeGraph(this, o));
		var paths = graph.getRootEntrypoints(file.getPath());
		if (paths.isEmpty()) return Collections.emptySet();
		var result = new HashSet<AIFile>();
		var root = getRoot(file.getOwner());
		if (root == null) return result;
		for (var path : paths) {
			try {
				var inc = root.resolve(path);
				if (inc != null) result.add(inc);
			} catch (Exception e) {
				// file disappeared between graph build and resolve — skip
			}
		}
		return result;
	}
}
