package leekscript.compiler.resolver;

import java.io.File;

public class ResourceContext extends ResolverContext {

	private File folder;

	public ResourceContext(File folder) {
		this.folder = folder;
	}

	@Override
	public String toString() {
		return folder.getPath();
	}

	public File getFolder() {
		return folder;
	}

	public void setFolder(File folder) {
		this.folder = folder;
	}
}
