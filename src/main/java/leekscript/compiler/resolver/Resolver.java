package leekscript.compiler.resolver;

import java.io.FileNotFoundException;

import leekscript.compiler.AIFile;

public interface Resolver<C extends ResolverContext> {

	public class Result<C> {
		public final String code;
		public final C context;
		public Result(String code, C context) {
			this.code = code;
			this.context = context;
		}
	}

	/**
	 * Resolve a AI by path, like 'library.leek' or '../../test.leek'
	 * @param path AI path
	 * @param context The AI's context or null to initialize it from default
	 * @return The result with the AI's code and context
	 * @throws FileNotFoundException if the AI is not found
	 */
	abstract public AIFile<C> resolve(String path, ResolverContext context) throws FileNotFoundException;

	public abstract ResolverContext createContext(int parameter1, int parameter2, int parameter3);
}
