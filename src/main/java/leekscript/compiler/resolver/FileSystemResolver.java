package leekscript.compiler.resolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import leekscript.compiler.AIFile;


public class FileSystemResolver implements Resolver<FileSystemContext> {

	@Override
	public AIFile<FileSystemContext> resolve(String path, ResolverContext basecontext) {
		
		FileSystemContext context = (FileSystemContext) basecontext;
		if (context == null) {
			context = new FileSystemContext(Paths.get(".").toFile());
		}
		try {
			Path resolvedPath = context.getFolder().toPath().resolve(path).normalize();
			
			String code = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);
			
			FileSystemContext newContext = new FileSystemContext(resolvedPath.getParent().toFile());
			return new AIFile<FileSystemContext>(path, code, newContext);
			
		} catch (IOException e) {
			return null;
		}
	}

	public ResolverContext createContext(int parameter) {
		return new FileSystemContext(Paths.get(".").toFile());
	}
}
