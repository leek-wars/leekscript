package leekscript.compiler.resolver;

public interface Resolver {
	
	/**
	 * Resolve a AI by path, like 'library.leek' or '../../test.leek'
	 * @param path AI path
	 * @return The AI's code, or null if not found
	 */
	String resolve(String path);
}
