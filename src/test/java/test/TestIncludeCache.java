package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.resolver.FileSystem;
import leekscript.runner.AI;

/**
 * Reproduit le scénario décrit dans l'issue #3597 : Main contient
 * include("Class/sousfichier"). Quand on modifie sousfichier sans toucher Main,
 * l'exécution de Main doit refléter le nouveau code.
 *
 * SAME_THREAD : LeekScript.setFileSystem est un singleton statique partagé,
 * deux tests de cette classe ne peuvent pas coexister en parallèle sans
 * s'écraser mutuellement.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class TestIncludeCache {

	@TempDir Path tmpRoot;
	private Path mainPath;
	private Path subPath;
	// Nom unique : aiCache RAM et le .class disque sont keyés par
	// Objects.hash(owner, path) — partagés entre tests JUnit malgré tmpRoots
	// distincts. Sans nom unique, le test N réutilise la classe compilée par
	// le test N-1 et masque le comportement réel du cache.
	private String mainName;
	private TmpFileSystem fs;

	@BeforeEach
	void setUp() throws Exception {
		Files.createDirectories(tmpRoot.resolve("Class"));
		mainName = "Main_" + Long.toHexString(System.nanoTime());
		mainPath = tmpRoot.resolve(mainName + ".leek");
		subPath = tmpRoot.resolve("Class/sousfichier.leek");
		fs = new TmpFileSystem(tmpRoot);
		LeekScript.setFileSystem(fs);
	}

	@AfterEach
	void tearDown() {
		LeekScript.resetFileSystem();
	}

	@Test
	public void includeChangeIsPickedUpWithoutMainEdit() throws Exception {
		Files.writeString(subPath, "function f() { return 1; }");
		Files.writeString(mainPath, "include(\"Class/sousfichier\");\nreturn f();");
		assertEquals("1", compileAndRun(mainName));

		Files.writeString(subPath, "function f() { return 2; }");
		// Bump explicite : la résolution sub-seconde du FS peut faire que le mtime
		// ne change pas dans la même seconde.
		bump(subPath, 1000);

		assertEquals("2", compileAndRun(mainName), "Main n'a pas vu la nouvelle version de sousfichier");
	}

	/**
	 * Scénario de l'issue #3597 : sousfichier n'existe pas au moment du premier
	 * scan de Main par l'IncludeGraph → forward[Main] reste {} (include
	 * non-résolu). Quand sousfichier apparaît, le graphe doit re-scanner Main pour
	 * matérialiser l'edge — sinon effectiveTimestamp(Main) ignore à jamais le
	 * mtime de sousfichier et le cache compilé reste figé.
	 */
	@Test
	public void subFileCreatedAfterMainScan() throws Exception {
		Files.writeString(mainPath, "include(\"Class/sousfichier\");\nreturn f();");
		try {
			compileAndRun(mainName);
		} catch (Throwable expected) {
			// Compile attendu en échec : sousfichier introuvable. Peuple quand même
			// l'IncludeGraph avec un include non-résolu sur Main.
		}

		Files.writeString(subPath, "function f() { return 42; }");
		assertEquals("42", compileAndRun(mainName), "Main n'a pas vu sousfichier nouvellement créé");

		Files.writeString(subPath, "function f() { return 100; }");
		bump(subPath, 2000);
		assertEquals("100", compileAndRun(mainName),
				"Main n'a pas vu la modif de sousfichier (cache figé sur include non-résolu)");
	}

	/** Best-effort GC hint : si la SoftRef du aiCache RAM survit, on teste
	 *  juste le path standard. */
	@Test
	public void includeChangeAfterGcHint() throws Exception {
		Files.writeString(subPath, "function f() { return 1; }");
		Files.writeString(mainPath, "include(\"Class/sousfichier\");\nreturn f();");
		assertEquals("1", compileAndRun(mainName));

		Files.writeString(subPath, "function f() { return 2; }");
		bump(subPath, 1000);

		System.gc();
		Thread.sleep(100);
		System.gc();

		assertEquals("2", compileAndRun(mainName), "Post-GC, Main n'a pas vu la nouvelle version");
	}

	@Test
	public void includeChangeIsPickedUpWithMainBump() throws Exception {
		Files.writeString(subPath, "function f() { return 1; }");
		Files.writeString(mainPath, "include(\"Class/sousfichier\");\nreturn f();");
		assertEquals("1", compileAndRun(mainName));

		// Modif + bump aussi Main (simule le daemon qui propage via
		// invalidateFile(entrypoint, sousfichier.timestamp), commit f0902cb2).
		Files.writeString(subPath, "function f() { return 2; }");
		long ts = System.currentTimeMillis() + 1000;
		Files.setLastModifiedTime(subPath, FileTime.fromMillis(ts));
		Files.setLastModifiedTime(mainPath, FileTime.fromMillis(ts));

		assertEquals("2", compileAndRun(mainName), "Main n'a pas vu la nouvelle version même avec bump");
	}

	private static void bump(Path p, long offsetMs) throws Exception {
		Files.setLastModifiedTime(p, FileTime.fromMillis(System.currentTimeMillis() + offsetMs));
	}

	private String compileAndRun(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		var options = new Options(LeekScript.LATEST_VERSION, false, true, true, null, true);
		AI ai = JavaCompiler.compile(file, options);
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		return ai.string(ai.runIA());
	}

	/**
	 * Mimique le DbFileSystem worker (filesystem-backed, per-owner cache,
	 * listAllFiles via Files.walk) sans dépendance sur le serveur.
	 */
	static class TmpFileSystem extends FileSystem {
		private final Path root;
		private final Map<Integer, Folder> rootFolders = new HashMap<>();
		private final Map<String, AIFile> filesByPath = new HashMap<>();

		TmpFileSystem(Path root) {
			this.root = root;
		}

		@Override
		public synchronized Iterable<AIFile> listAllFiles(int owner) {
			var list = new ArrayList<AIFile>();
			if (!Files.exists(root)) return list;
			try (var walk = Files.walk(root)) {
				walk.filter(p -> p.toString().endsWith(".leek") && Files.isRegularFile(p)).forEach(p -> {
					var rel = root.relativize(p).toString().replace(java.io.File.separatorChar, '/');
					rel = rel.substring(0, rel.length() - ".leek".length());
					var f = getFileByPath(rel, owner);
					if (f != null) list.add(f);
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		synchronized AIFile getFileByPath(String filePath, int owner) {
			var key = owner + ":" + filePath;
			var cached = filesByPath.get(key);
			if (cached != null && getAITimestamp(cached) <= cached.getTimestamp()) return cached;
			var fsPath = root.resolve(filePath + ".leek");
			try {
				var code = Files.readString(fsPath);
				long mtime = Files.getLastModifiedTime(fsPath).toMillis();
				var rootFolder = getRoot(owner);
				var folder = resolveParent(filePath, rootFolder);
				var file = new AIFile(filePath, code, mtime, LeekScript.LATEST_VERSION, folder, owner,
						Objects.hash(owner, filePath) & 0xfffffff, false);
				folder.addFile(file);
				filesByPath.put(key, file);
				return file;
			} catch (Exception e) {
				return null;
			}
		}

		private Folder resolveParent(String filePath, Folder rootFolder) {
			var parts = filePath.split("/");
			var current = rootFolder;
			for (int i = 0; i < parts.length - 1; i++) {
				var sub = current.getFolder(parts[i]);
				if (sub == null) return current;
				current = sub;
			}
			return current;
		}

		@Override public Folder getRoot() { return null; }

		@Override
		public synchronized Folder getRoot(int owner) {
			var f = rootFolders.get(owner);
			if (f != null) return f;
			f = new Folder(owner, this);
			f.setParent(f);
			f.setRoot(f);
			rootFolders.put(owner, f);
			return f;
		}

		@Override public Folder getRoot(int owner, int farmer) { return null; }

		@Override
		public synchronized Folder findFolder(String name, Folder current) {
			var path = buildChild(current, name);
			if (!Files.isDirectory(root.resolve(path))) return null;
			return new Folder(name.hashCode(), current.getOwner(), name, current, getRoot(current.getOwner()), this,
					System.currentTimeMillis());
		}

		@Override
		public synchronized AIFile findFile(String name, Folder folder) throws FileNotFoundException {
			var filePath = buildChild(folder, name);
			var key = folder.getOwner() + ":" + filePath;
			var cached = filesByPath.get(key);
			if (cached != null && getAITimestamp(cached) <= cached.getTimestamp()) return cached;
			var fsPath = root.resolve(filePath + ".leek");
			try {
				var code = Files.readString(fsPath);
				long mtime = Files.getLastModifiedTime(fsPath).toMillis();
				var file = new AIFile(filePath, code, mtime, LeekScript.LATEST_VERSION, folder, folder.getOwner(),
						Objects.hash(folder.getOwner(), filePath) & 0xfffffff, false);
				filesByPath.put(key, file);
				return file;
			} catch (java.io.IOException e) {
				throw new FileNotFoundException(filePath);
			}
		}

		private String buildChild(Folder folder, String name) {
			var parts = new ArrayList<String>();
			parts.add(name);
			var c = folder;
			while (c != null && c.getParent() != c) {
				parts.add(0, c.getName());
				c = c.getParent();
			}
			return String.join("/", parts);
		}

		@Override public AIFile getFileById(int id, int farmer) { return null; }
		@Override public Folder getFolderById(int id, int farmer) { return getRoot(farmer); }

		@Override
		public long getAITimestamp(AIFile ai) {
			try {
				return Files.getLastModifiedTime(root.resolve(ai.getPath() + ".leek")).toMillis();
			} catch (Exception e) {
				return Long.MAX_VALUE;
			}
		}

		@Override public void loadDependencies(AIFile ai) {}
		@Override public long getFolderTimestamp(Folder folder) { return Long.MAX_VALUE; }
	}
}
