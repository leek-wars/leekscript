package leekscript.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import leekscript.compiler.resolver.FileSystem;

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
		var direct = reverse.get(path);
		if (direct == null || direct.isEmpty()) {
			// path itself has no includer. It's a root only if it was reached from a
			// different starting point (via at least one step). The initial caller
			// distinguishes the two cases via visited size.
			if (visited.size() > 1) roots.add(path);
			return;
		}
		for (var includer : direct) {
			collectRoots(includer, roots, visited);
		}
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
			var direct = forward.get(queue.poll());
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
		var includeSet = new HashSet<String>();
		boolean hasUnresolved = false;
		for (var name : extractIncludes(file)) {
			try {
				var resolved = file.getFolder().resolve(name);
				includeSet.add(resolved.getPath());
			} catch (Exception e) {
				hasUnresolved = true;
			}
		}
		forward.put(file.getPath(), includeSet);
		for (var inc : includeSet) {
			reverse.computeIfAbsent(inc, k -> new HashSet<>()).add(file.getPath());
		}
		if (hasUnresolved) pathsWithUnresolved.add(file.getPath());
	}

	/**
	 * Returns the raw include names (unresolved paths) found in the file's source.
	 * Uses the cached token stream when available, otherwise runs the lexer once
	 * and stores the tokens on the AIFile so a subsequent analyze pass can reuse them.
	 *
	 * Applique PragmaParser avant le lex : sinon on parse à file.getVersion() (souvent
	 * LATEST_VERSION par défaut) et on cache des tokens dont la lex'ication ne
	 * correspond pas à `// @version:N`. IACompiler.compile réutiliserait ces tokens
	 * stale et planterait sur les keywords version-dependent (ex. AND en v2).
	 */
	private List<String> extractIncludes(AIFile file) {
		var result = new ArrayList<String>();
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
				if (tok.getType() == TokenType.INCLUDE) {
					stream.eat();
					if (!stream.hasMoreTokens()) break;
					var lparen = stream.eat();
					if (lparen.getType() != TokenType.PAR_LEFT) continue;
					if (!stream.hasMoreTokens()) break;
					var arg = stream.eat();
					if (arg.getType() == TokenType.VAR_STRING) {
						var word = arg.getWord();
						if (word.length() >= 2) {
							result.add(word.substring(1, word.length() - 1));
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
