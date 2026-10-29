package test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.resolver.FileSystem;

/**
 * FileSystem de test adossé à un dossier temporaire sur disque (mime le DbFileSystem
 * du worker : cache par owner, listAllFiles via Files.walk) sans dépendre du serveur.
 * Partagé par {@link TestIncludeCache} et {@link TestModulesResolution} ; la version
 * LeekScript des AIFile produits est passée au constructeur (include = v≤4, modules = v5).
 */
class RamFileSystem extends FileSystem {

	// Les tests de modules et de packages compilent en `// @version:5`
	static {
		TestCommon.openUnreleasedVersions();
	}

	private final Path root;
	private final int version;
	private final Map<Integer, Folder> rootFolders = new HashMap<>();
	private final Map<String, AIFile> filesByPath = new HashMap<>();

	RamFileSystem(Path root, int version) {
		this.root = root;
		this.version = version;
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
		if (cached != null && getAITimestamp(cached) == cached.getTimestamp()) return cached;
		var fsPath = root.resolve(filePath + ".leek");
		try {
			var code = Files.readString(fsPath);
			long mtime = Files.getLastModifiedTime(fsPath).toMillis();
			var rootFolder = getRoot(owner);
			var folder = resolveParent(filePath, rootFolder);
			var file = new AIFile(filePath, code, mtime, version, folder, owner,
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
		if (cached != null && getAITimestamp(cached) == cached.getTimestamp()) return cached;
		var fsPath = root.resolve(filePath + ".leek");
		try {
			var code = Files.readString(fsPath);
			long mtime = Files.getLastModifiedTime(fsPath).toMillis();
			var file = new AIFile(filePath, code, mtime, version, folder, folder.getOwner(),
					Objects.hash(folder.getOwner(), filePath) & 0xfffffff, false);
			filesByPath.put(key, file);
			return file;
		} catch (IOException e) {
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
