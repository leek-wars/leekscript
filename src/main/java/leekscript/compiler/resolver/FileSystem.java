package leekscript.compiler.resolver;

import java.io.FileNotFoundException;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;

public abstract class FileSystem {

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
}
