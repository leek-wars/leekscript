package leekscript.compiler;

import java.io.IOException;
import java.util.Random;

import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystem;
import leekscript.compiler.resolver.NativeFileSystem;
import leekscript.compiler.resolver.ResourceFileSystem;
import leekscript.runner.AI;
import leekscript.common.Error;

public class LeekScript {

	public final static int LATEST_VERSION = 4;
	private static long id = 1;

	private static ResourceFileSystem defaultFileSystem = new ResourceFileSystem();
	private static NativeFileSystem nativeFileSystem = new NativeFileSystem();
	private static FileSystem customFileSystem = null;

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

	public static AI compileFile(String filepath, String AIClass, Options options) throws LeekScriptException, LeekCompilerException, IOException {
		var file = getFileSystem().getRoot().resolve(filepath);
		file.setVersion(options.version(), options.strict());
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass(AIClass);
		return file.compile(options);
	}

	public static AI compileSnippet(String snippet, String AIClass, Options options) throws LeekScriptException, LeekCompilerException, IOException {
		long ai_id = id++;
		var file = new AIFile("<snippet " + ai_id + ">", snippet, System.currentTimeMillis(), options.version(), (int) ai_id, options.strict());
		file.setJavaClass("AI_" + ai_id);
		file.setRootClass(AIClass);
		file.setId((int) ai_id);
		return file.compile(options);
	}

	public static String mergeFile(AIFile ai) throws LeekScriptException, LeekCompilerException, IOException {
		return new IACompiler().merge(ai);
	}

	public static String runFile(String filename, Options options) throws Exception {
		AI ai = LeekScript.compileFile(filename, "AI", options);
		var v = ai.runIA();
		System.out.println(ai.string(v));
		return ai.string(v);
	}

	public static void setFileSystem(FileSystem fileSystem) {
		customFileSystem = fileSystem;
	}

	public static void resetFileSystem() {
		customFileSystem = null;
	}

	public static FileSystem getFileSystem() {
		return customFileSystem != null ? customFileSystem : defaultFileSystem;
	}

	public static RandomGenerator getRandom() {
		return defaultRandomGenerator;
	}

	public static NativeFileSystem getNativeFileSystem() {
		return nativeFileSystem;
	}
}