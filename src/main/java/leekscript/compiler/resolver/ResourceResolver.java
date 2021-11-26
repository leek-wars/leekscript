package leekscript.compiler.resolver;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import leekscript.compiler.AIFile;


public class ResourceResolver implements Resolver<ResourceContext> {

	@Override
	public AIFile<ResourceContext> resolve(String path, ResolverContext basecontext) throws FileNotFoundException {

		ResourceContext context = (ResourceContext) basecontext;
		if (context == null) {
			context = new ResourceContext(Paths.get(".").toFile());
		}
		try {
			Path resolvedPath = context.getFolder().toPath().resolve(path).normalize();

			var is = getClass().getClassLoader().getResourceAsStream(resolvedPath.toString());
			String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);

			Path parent = resolvedPath.getParent();
			if (parent == null) parent = Paths.get(".");

			var newContext = new ResourceContext(parent.toFile());

			long timestamp = resolvedPath.toFile().lastModified();

			return new AIFile<ResourceContext>(path, code, timestamp, 2, newContext, resolvedPath.toString().hashCode() & 0xfffffff);

		} catch (Exception e) {
			throw new FileNotFoundException();
		}
	}

	public ResolverContext createContext(int parameter1, int parameter2, int parameter3) {
		return new FileSystemContext(Paths.get(".").toFile());
	}
}
