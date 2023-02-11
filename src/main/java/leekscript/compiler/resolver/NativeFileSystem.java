package leekscript.compiler.resolver;

import java.io.FileNotFoundException;
import java.io.FileReader;
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

	@Override
	public Folder findFolder(String name, Folder folder) {
		// System.out.println("NativeFileSystem.findFolder() " + name + " " + folder);
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
	public AIFile findFile(String name, Folder folder) throws FileNotFoundException {
		// System.out.println("NativeFileSystem.findFile() " + name + " " + folder);
		try {
			var root = Paths.get(folder.getName()).toFile();
			Path resolvedPath = root.toPath().resolve(name).normalize();

			String code = Files.readString(resolvedPath, StandardCharsets.UTF_8);

			Path parent = resolvedPath.getParent();
			if (parent == null) parent = Paths.get(".");

			long timestamp = resolvedPath.toFile().lastModified();

			return new AIFile(name, code, timestamp, LeekScript.LATEST_VERSION, folder, folder.getOwner(), resolvedPath.toString().hashCode() & 0xfffffff);

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
