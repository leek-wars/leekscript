package leekscript.compiler.resolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

			String code = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);

			Path parent = resolvedPath.getParent();
			if (parent == null) parent = Paths.get(".");

			FileSystemContext newContext = new FileSystemContext(parent.toFile());

			long timestamp = resolvedPath.toFile().lastModified();

			return new AIFile<FileSystemContext>(path, code, timestamp, 11, newContext, resolvedPath.hashCode() & 0xfffffff);

		} catch (IOException e) {
			throw new FileNotFoundException();
		}
	}

	public ResolverContext createContext(int parameter1, int parameter2) {
		return new FileSystemContext(Paths.get(".").toFile());
	}
}
