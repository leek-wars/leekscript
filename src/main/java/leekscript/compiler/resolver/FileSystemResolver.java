package leekscript.compiler.resolver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import leekscript.compiler.AIFile;


public class FileSystemResolver implements Resolver<FileSystemContext> {

	@Override
	public AIFile<FileSystemContext> resolve(String path, ResolverContext basecontext) throws FileNotFoundException {

		FileSystemContext context = (FileSystemContext) basecontext;
		if (context == null) {
			context = new FileSystemContext(Paths.get(".").toFile());
		}
		try {
			Path resolvedPath = context.getFolder().toPath().resolve(path).normalize();

			var is = new FileInputStream(resolvedPath.toFile());
			String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);

			Path parent = resolvedPath.getParent();
			if (parent == null) parent = Paths.get(".");

			FileSystemContext newContext = new FileSystemContext(parent.toFile());

			long timestamp = resolvedPath.toFile().lastModified();
			int version = resolvedPath.toFile().getName().contains("v1") ? 1 : 2;

			return new AIFile<FileSystemContext>(path, code, timestamp, version, newContext, resolvedPath.toString().hashCode() & 0xfffffff);

		} catch (Exception e) {
			throw new FileNotFoundException();
		}
	}

	public ResolverContext createContext(int parameter1, int parameter2, int parameter3) {
		return new FileSystemContext(Paths.get(".").toFile());
	}
}
