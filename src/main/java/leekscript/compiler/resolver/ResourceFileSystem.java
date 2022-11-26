package leekscript.compiler.resolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;

public class ResourceFileSystem extends FileSystem {

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
	public Folder getRoot(int owner) {
		System.out.println("ResourceFileSystem.getRoot()");
		return null;
	}

	@Override
	public Folder findFolder(String name, Folder folder) {
		try {
			var root = Paths.get(folder.getName()).toFile();
			Path resolvedPath = root.toPath().resolve(name).normalize();

			Path parent = resolvedPath.getParent();
			if (parent == null) parent = Paths.get(".");

			return new Folder(0, 0, resolvedPath.toString(), folder, this.root, this, System.currentTimeMillis());

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public AIFile findFile(String name, Folder folder) {
		try {
			var root = Paths.get(folder.getName()).toFile();
			Path resolvedPath = root.toPath().resolve(name).normalize();

			var is = getClass().getClassLoader().getResourceAsStream(resolvedPath.toString());
			String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);

			Path parent = resolvedPath.getParent();
			if (parent == null) parent = Paths.get(".");

			long timestamp = resolvedPath.toFile().lastModified();

			return new AIFile(name, code, timestamp, 2, resolvedPath.toString().hashCode() & 0xfffffff);

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public AIFile getFileById(int id, int farmer) {
		return null; // Pas d'ID sur les fichiers
	}

	@Override
	public Folder getFolderById(int id, int farmer) {
		return null; // Pas d'ID sur les fichiers
	}

	@Override
	public long getAITimestamp(AIFile ai) {
		return System.currentTimeMillis(); // Toujours expiré
	}

	@Override
	public void loadDependencies(AIFile ai) {
		// Nothing to load
	}

	@Override
	public long getFolderTimestamp(Folder folder) {
		return System.currentTimeMillis(); // Toujours expiré
	}
}
