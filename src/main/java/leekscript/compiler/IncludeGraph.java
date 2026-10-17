package leekscript.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import leekscript.compiler.resolver.FileSystem;
import leekscript.runner.LeekFunctions;

/**
 * In-memory include graph for a single owner.
 *
 * Built lazily by walking the owner's AIFile list and extracting {@code include("...")}
 * calls via the LexicalParser. Re-scans only files whose mtime changed since last
 * refresh, so the incremental cost is proportional to churn, not graph size.
 */
public class IncludeGraph {

	private final FileSystem fs;
	private final int owner;

	// includer path -> set of included paths
	private final Map<String, Set<String>> forward = new HashMap<>();
	// included path -> set of includer paths
	private final Map<String, Set<String>> reverse = new HashMap<>();
	// reader path -> files read by its file-argument calls (`getLoadout("x.json")`). Hors de
	// forward/reverse : ce ne sont que des dépendances de compilation, jamais des racines d'analyse.
	private final Map<String, Set<String>> fileDeps = new HashMap<>();
	// path -> last seen mtime (used to detect adds/removes/modifies)
	private final Map<String, Long> seenMtimes = new HashMap<>();
	// includer paths whose last scan failed to resolve at least one include name.
	// When a new file appears, only these need to be rescanned — their unresolved
	// names might now resolve to the new file.
	private final Set<String> pathsWithUnresolved = new HashSet<>();

	public IncludeGraph(FileSystem fs, int owner) {
		this.fs = fs;
		this.owner = owner;
	}

	public synchronized Set<String> getDirectIncluders(String path) {
		refresh();
		var s = reverse.get(path);
		return s != null ? Collections.unmodifiableSet(s) : Set.of();
	}

	/**
	 * Returns the root entrypoints that transitively include {@code path}.
	 * A root entrypoint has no includer itself. If {@code path} has no
	 * includers at all, returns an empty set (caller should treat {@code path}
	 * as its own entrypoint).
	 */
	public synchronized Set<String> getRootEntrypoints(String path) {
		refresh();
		var roots = new HashSet<String>();
		var visited = new HashSet<String>();
		collectRoots(path, roots, visited);
		return roots;
	}

	private void collectRoots(String path, Set<String> roots, Set<String> visited) {
		if (!visited.add(path)) return;
		boolean included = false;
		for (var includer : reverse.getOrDefault(path, Set.of())) {
			if (!isRealIncluder(includer)) continue;
			included = true;
			collectRoots(includer, roots, visited);
		}
		// path itself has no real includer. It's a root only if it was reached from a
		// different starting point (via at least one step), and if it's LeekScript.
		if (!included && visited.size() > 1 && isLeekScriptPath(path)) roots.add(path);
	}

	/**
	 * Une doc qui cite `include("…")` en exemple n'inclut rien pour de vrai : un fichier
	 * qui n'est pas du LeekScript ne compte comme includer que s'il est lui-même inclus.
	 * Sinon la doc deviendrait la racine des analyses du fichier cité, et tout son texte
	 * serait compilé comme du LeekScript à chacune. (`reverse` ne garde aucun ensemble vide.)
	 */
	private boolean isRealIncluder(String includer) {
		return isLeekScriptPath(includer) || reverse.containsKey(includer);
	}

	/**
	 * Fichiers rangés avec les IA sans être du LeekScript : docs, données, IA JavaScript,
	 * TypeScript ou Python. Jamais compilés seuls, ils ne sont jamais la racine d'une analyse.
	 * Même liste que l'API et l'éditeur, qui ne les compilent pas non plus : les garder en phase.
	 */
	private static final Set<String> NON_LEEKSCRIPT_EXTENSIONS = Set.of("md", "txt", "json", "yml", "yaml", "js", "mjs", "ts", "mts", "py");

	private static boolean isLeekScriptPath(String path) {
		int dot = path.lastIndexOf('.');
		if (dot <= path.lastIndexOf('/')) return true; // sans extension : LeekScript
		return !NON_LEEKSCRIPT_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	public synchronized Set<String> getIncluded(String path) {
		refresh();
		var s = forward.get(path);
		return s != null ? Collections.unmodifiableSet(s) : Set.of();
	}

	/**
	 * Returns all paths transitively included from {@code path} (excluding {@code path}
	 * itself). Used by JavaCompiler.effectiveTimestamp to invalidate the compiled cache
	 * when ANY file in the include chain changes — including grand-children and beyond.
	 *
	 * Without transitivity, modifying a leaf in Main → A → B → C only bumps C's mtime;
	 * Main's effectiveTimestamp only walks {A} and misses C entirely.
	 */
	public synchronized Set<String> getTransitivelyIncluded(String path) {
		refresh();
		var result = new HashSet<String>();
		var queue = new java.util.ArrayDeque<String>();
		queue.add(path);
		while (!queue.isEmpty()) {
			var current = queue.poll();
			result.addAll(fileDeps.getOrDefault(current, Set.of()));
			var direct = forward.get(current);
			if (direct == null) continue;
			for (var inc : direct) {
				if (result.add(inc)) queue.add(inc);
			}
		}
		return result;
	}

	private void refresh() {
		var current = new HashMap<String, AIFile>();
		for (var file : fs.listAllFiles(owner)) {
			current.put(file.getPath(), file);
		}

		// Files that disappeared since last scan
		var removed = new HashSet<String>(seenMtimes.keySet());
		removed.removeAll(current.keySet());
		for (var path : removed) dropFile(path);

		// New files since last scan: rescan only files that had unresolved includes,
		// since their unresolved names might now resolve to a newly-added file. Drop
		// their seenMtimes so the loop below picks them up regardless of mtime.
		if (current.size() > seenMtimes.size() && !pathsWithUnresolved.isEmpty()) {
			pathsWithUnresolved.forEach(seenMtimes::remove);
		}

		// Files new or whose mtime changed
		for (var entry : current.entrySet()) {
			var path = entry.getKey();
			var file = entry.getValue();
			long mtime = fs.getAITimestamp(file);
			var seen = seenMtimes.get(path);
			if (seen != null && seen == mtime) continue;
			rescanFile(file);
			seenMtimes.put(path, mtime);
		}
	}

	private void dropFile(String path) {
		fileDeps.remove(path);
		var oldForward = forward.remove(path);
		if (oldForward != null) {
			for (var inc : oldForward) {
				var includers = reverse.get(inc);
				if (includers != null) {
					includers.remove(path);
					if (includers.isEmpty()) reverse.remove(inc);
				}
			}
		}
		seenMtimes.remove(path);
		pathsWithUnresolved.remove(path);
	}

	private void rescanFile(AIFile file) {
		dropFile(file.getPath());
		var refs = extractReferences(file);
		var includeSet = resolveAll(file, refs.includes());
		var fileSet = resolveAll(file, refs.files());
		boolean hasUnresolved = includeSet.size() < refs.includes().size() || fileSet.size() < refs.files().size();
		if (!fileSet.isEmpty()) fileDeps.put(file.getPath(), fileSet);
		forward.put(file.getPath(), includeSet);
		for (var inc : includeSet) {
			reverse.computeIfAbsent(inc, k -> new HashSet<>()).add(file.getPath());
		}
		if (hasUnresolved) pathsWithUnresolved.add(file.getPath());
	}

	private record References(List<String> includes, List<String> files) {}

	private static Set<String> resolveAll(AIFile file, List<String> names) {
		var paths = new HashSet<String>();
		for (var name : names) {
			try {
				paths.add(file.getFolder().resolve(name).getPath());
			} catch (Exception e) {
				// non résolu : compté par l'appelant
			}
		}
		return paths;
	}

	/**
	 * Returns the raw include names and file-argument paths (unresolved) found in the file's source.
	 * Uses the cached token stream when available, otherwise runs the lexer once
	 * and stores the tokens on the AIFile so a subsequent analyze pass can reuse them.
	 *
	 * Applique PragmaParser avant le lex : sinon on parse à file.getVersion() (souvent
	 * LATEST_VERSION par défaut) et on cache des tokens dont la lex'ication ne
	 * correspond pas à `// @version:N`. IACompiler.compile réutiliserait ces tokens
	 * stale et planterait sur les keywords version-dependent (ex. AND en v2).
	 */
	private References extractReferences(AIFile file) {
		var result = new References(new ArrayList<>(), new ArrayList<>());
		LexicalParserTokenStream stream;
		if (file.hasBeenParsed()) {
			stream = file.getTokenStream();
		} else {
			try {
				PragmaParser.apply(file);
				var parser = new LexicalParser(file, file.getVersion());
				stream = parser.parse(err -> {}); // ignore lex errors here
				file.setTokenStream(stream);
			} catch (Exception e) {
				return result;
			}
		}
		int savedCursor = -1;
		try {
			// Save/restore cursor so we don't disturb a concurrent analyze
			var pos = stream.getPosition();
			savedCursor = pos.cursor();
			stream.reset();
			while (stream.hasMoreTokens()) {
				var tok = stream.get();
				// Le fichier lu par une fonction à argument fichier (`getLoadout("x.json")`) : le modifier
				// doit invalider l'IA compilée. Le jeton seul ne distingue pas une méthode ou une fonction
				// homonyme du joueur : sans effet, la dépendance ne sert qu'au cache.
				var function = tok.getType() == TokenType.STRING ? LeekFunctions.getValue(tok.getWord(), true) : null;
				boolean fileArgument = function != null && function.hasFileArgument();
				if (tok.getType() == TokenType.INCLUDE || fileArgument) {
					stream.eat();
					if (!stream.hasMoreTokens()) break;
					var lparen = stream.eat();
					if (lparen.getType() != TokenType.PAR_LEFT) continue;
					if (!stream.hasMoreTokens()) break;
					var arg = stream.eat();
					if (arg.getType() == TokenType.VAR_STRING) {
						var word = arg.getWord();
						if (word.length() >= 2) {
							(fileArgument ? result.files() : result.includes()).add(word.substring(1, word.length() - 1));
						}
					}
				} else {
					stream.eat();
				}
			}
		} finally {
			if (savedCursor >= 0) {
				stream.setPosition(new LexicalParserTokenStream.LexicalParserTokenStreamPosition(savedCursor));
			}
		}
		return result;
	}
}
