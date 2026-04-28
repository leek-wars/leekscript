package leekscript.compiler.resolver;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.LeekScript;

public class NativeFileSystem extends FileSystem {

	private Folder root = new Folder(0, this);

	@Override
	public Folder getRoot() {
		return root;
	}

	@Override
	public Folder getRoot(int owner, int farmer) {
		// Pas de permission, une seule racine
		return root;
	}

	@Override
	public AIFile getFileById(int id, int farmer) {
		// System.out.println("NativeFileSystem.getFileById() " + id + " " + farmer);
		return null;
	}

	@Override
	public Folder getRoot(int owner) {
		return root;
	}

	/**
	 * Resolve `name` under `folderRoot` and reject any path that escapes the
	 * root (e.g. `name = "../../etc/passwd"`). Used in CLI dev mode; production
	 * uses a DB-backed FileSystem so this never runs in a worker.
	 */
	private static Path resolveSafe(Path folderRoot, String name) {
		Path absoluteRoot = folderRoot.toAbsolutePath().normalize();
		Path resolved = absoluteRoot.resolve(name).normalize();
		if (!resolved.startsWith(absoluteRoot)) {
			return null;
		}
		return resolved;
	}

	@Override
	public Folder findFolder(String name, Folder folder) {
		try {
			Path folderRoot = Paths.get(folder.getName());
			Path resolvedPath = resolveSafe(folderRoot, name);
			if (resolvedPath == null) return null;

			return new Folder(0, 0, resolvedPath.toString(), folder, this.root, this, System.currentTimeMillis());

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public AIFile findFile(String name, Folder folder) throws FileNotFoundException {
		try {
			Path folderRoot = Paths.get(folder.getName());
			Path resolvedPath = resolveSafe(folderRoot, name);
			if (resolvedPath == null) throw new FileNotFoundException();

			String code = Files.readString(resolvedPath, StandardCharsets.UTF_8);

			long timestamp = resolvedPath.toFile().lastModified();

			return new AIFile(name, code, timestamp, LeekScript.LATEST_VERSION, folder, folder.getOwner(), resolvedPath.toString().hashCode() & 0xfffffff, false);

		} catch (FileNotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new FileNotFoundException();
		}
	}

	@Override
	public Folder getFolderById(int id, int farmer) {
		return root;
	}

	@Override
	public long getAITimestamp(AIFile ai) {
		return 0;
	}

	@Override
	public void loadDependencies(AIFile ai) {}

	@Override
	public long getFolderTimestamp(Folder folder) {
		return 0;
	}

}
