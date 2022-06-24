package leekscript.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystemContext;
import leekscript.compiler.resolver.FileSystemResolver;
import leekscript.compiler.resolver.Resolver;
import leekscript.compiler.resolver.ResolverContext;
import leekscript.compiler.resolver.ResourceContext;
import leekscript.compiler.resolver.ResourceResolver;
import leekscript.runner.AI;
import leekscript.common.Error;

public class LeekScript {

	public final static int LATEST_VERSION = 4;
	private static long id = 1;

	private static Resolver<ResourceContext> defaultResolver = new ResourceResolver();
	private static Resolver<FileSystemContext> fileSystemResolver = new FileSystemResolver();
	private static Resolver<?> customResolver = null;

	private static RandomGenerator defaultRandomGenerator = new RandomGenerator() {
		private Random random = new Random();

		@Override
		public void seed(long seed) {
			random.setSeed(seed);
		}

		@Override
		public int getInt(int min, int max) {
			if (max - min + 1 <= 0)
				return 0;
			return min + random.nextInt(max - min + 1);
		}

		@Override
		public long getLong(long min, long max) {
			if (max - min + 1 <= 0)
				return 0;
			// return min + Math.abs(random.nextLong()) % (max - min + 1);
			return min + random.nextInt((int) max - (int) min + 1);
		}

		@Override
		public double getDouble() {
			return random.nextDouble();
		}
	};

	public static boolean isLoaded(int id) {
		return getResolver().isLoaded(id);
	}

	public static Hover hover(int id, int line, int column) throws FileNotFoundException {
		AIFile<?> file = getResolver().getById(id);
		if (file == null) {
			throw new FileNotFoundException();
		}
		return file.hover(line, column);
	}

	public static AI compileFile(String filepath, String AIClass, boolean useClassCache) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> file = getResolver().resolve(filepath, null);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass(AIClass);
		return file.compile(useClassCache);
	}

	public static AI compileFile(String filepath, String AIClass, int version) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> file = getResolver().resolve(filepath, null);
		file.setVersion(version);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass(AIClass);
		return file.compile(false);
	}

	public static AI compileFileContext(String filepath, String AIClass, ResolverContext context, boolean useClassCache) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> file = getResolver().resolve(filepath, context);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass(AIClass);
		return file.compile(useClassCache);
	}

	public static AI compileSnippet(String snippet, String AIClass)	throws LeekScriptException, LeekCompilerException, IOException {
		return compileSnippet(snippet, AIClass, 2);
	}

	public static AI compileSnippet(String snippet, String AIClass, int version) throws LeekScriptException, LeekCompilerException, IOException {
		long ai_id = id++;
		AIFile<?> file = new AIFile<FileSystemContext>("<snippet " + ai_id + ">", snippet, System.currentTimeMillis(), version, null, (int) ai_id);
		file.setJavaClass("AI_" + ai_id);
		file.setRootClass(AIClass);
		return file.compile(false);
	}

	public static String mergeFile(String filepath, ResolverContext context) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> ai = getResolver().resolve(filepath, context);

		return new IACompiler().merge(ai);
	}

	public static Object runScript(String script, boolean nocache) throws Exception {
		return LeekScript.compileSnippet(script, "AI").runIA();
	}

	public static String runFile(String filename) throws Exception {
		AI ai = LeekScript.compileFile(filename, "AI", true);
		var v = ai.runIA();
		System.out.println(ai.string(v));
		return ai.string(v);
	}

	public static void setResolver(Resolver<?> resolver) {
		customResolver = resolver;
	}

	public static void resetResolver() {
		customResolver = null;
	}

	public static Resolver<?> getResolver() {
		return customResolver != null ? customResolver : defaultResolver;
	}

	public static RandomGenerator getRandom() {
		return defaultRandomGenerator;
	}

	public static void throwException(String error) throws LeekScriptException {
		if (error != null && !error.isEmpty()) {
			if (error.contains("code too large")) {
				String[] lines = error.split("\n", 3);
				if (lines.length >= 2 && lines[1].split(" ").length > 4) {
					String l = lines[1].split(" ")[2];
					if (l.length() > 4 && !l.startsWith("runIA")) {
						throw new LeekScriptException(Error.CODE_TOO_LARGE_FUNCTION, l.substring(14, l.length() - 2));
					}
				}
				throw new LeekScriptException(Error.CODE_TOO_LARGE);
			}
		}
		throw new LeekScriptException(Error.COMPILE_JAVA, error);
	}

	public static Resolver<?> getFileSystemResolver() {
		return fileSystemResolver;
	}


}