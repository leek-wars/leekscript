package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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

	private final static String IA_PATH = "ai";
	private static long id = 1;

	private static class AIClassEntry {
		Class<?> clazz;
		long timestamp;
		public AIClassEntry(Class<?> clazz, long timestamp) {
			this.clazz = clazz;
			this.timestamp = timestamp;
		}
	}

	private static Resolver<ResourceContext> defaultResolver = new ResourceResolver();
	private static Resolver<FileSystemContext> fileSystemResolver = new FileSystemResolver();
	private static Resolver<?> customResolver = null;
	private static String classpath;
	private static List<String> arguments = new ArrayList<>();
	private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private static URLClassLoader urlLoader;
	private static HashMap<String, AIClassEntry> aiCache = new HashMap<>();
	static {
		classpath = new File(LeekScript.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getPath();
		arguments.addAll(Arrays.asList("-classpath", classpath, "-nowarn"));
		try {
			urlLoader = new URLClassLoader(new URL[] { new File(IA_PATH).toURI().toURL() }, new ClassLoader() {});
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

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
		public double getDouble() {
			return random.nextDouble();
		}
	};

	public static AI compileFile(String filepath, String AIClass, boolean useClassCache) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> ai = getResolver().resolve(filepath, null);
		return compile(ai, AIClass, useClassCache);
	}

	public static AI compileFile(String filepath, String AIClass, int version) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> ai = getResolver().resolve(filepath, null);
		ai.setVersion(version);
		return compile(ai, AIClass, false);
	}

	public static AI compileFileContext(String filepath, String AIClass, ResolverContext context, boolean useClassCache) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> ai = getResolver().resolve(filepath, context);
		return compile(ai, AIClass, useClassCache);
	}

	public static AI compileSnippet(String snippet, String AIClass)	throws LeekScriptException, LeekCompilerException, IOException {
		return compileSnippet(snippet, AIClass, 2);
	}

	public static AI compileSnippet(String snippet, String AIClass, int version) throws LeekScriptException, LeekCompilerException, IOException {
		long ai_id = id++;
		AIFile<?> ai = new AIFile<FileSystemContext>("<snippet " + ai_id + ">", snippet, System.currentTimeMillis(), version, null, (int) ai_id);
		return compile(ai, AIClass, false);
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

	public static AI compile(AIFile<?> file, String AIClass, boolean useClassCache) throws LeekScriptException, LeekCompilerException {

		// System.out.println("LeekScript compile AI " + file.getPath() + " timestamp : " + file.getTimestamp());

		var root = new File(IA_PATH);
		if (!root.exists()) root.mkdir();
		String javaClassName = "AI_" + file.getId();
		String fileName = javaClassName + ".java";
		File compiled = Paths.get(IA_PATH, javaClassName + ".class").toFile();
		File java = Paths.get(IA_PATH, javaClassName + ".java").toFile();
		File lines = Paths.get(IA_PATH, javaClassName + ".lines").toFile();

		// Cache des classes en RAM d'abord
		var entry = aiCache.get(javaClassName);
		if (entry != null && entry.timestamp >= file.getTimestamp()) {
			// System.out.println("Load AI " + file.getPath() + " from RAM");
			try {
				var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
				ai.setId(file.getId());
				ai.setLinesFile(lines);
				return ai;
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}

		// Utilisation du cache de class dans le file system
		if (useClassCache && compiled.exists() && compiled.length() != 0 && compiled.lastModified() >= file.getTimestamp()) {
			// System.out.println("Load AI " + file.getPath() + " from disk");
			try {
				try {
					urlLoader = new URLClassLoader(new URL[] { new File(IA_PATH).toURI().toURL() }, new ClassLoader() {});
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				var clazz = urlLoader.loadClass(javaClassName);
				entry = new AIClassEntry(clazz, file.getTimestamp());
				aiCache.put(javaClassName, entry);
				var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
				ai.setId(file.getId());
				ai.setLinesFile(lines);
				return ai;
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}

		// On commence par la conversion LS -> Java
		// System.out.println("Re-compile AI " + file.getPath());
		long t = System.nanoTime();
		var compiledCode = new IACompiler().compile(file, javaClassName, AIClass);
		long analyze_time = System.nanoTime() - t;

		if (compiledCode.getJavaCode().isEmpty()) { // Rien ne compile, pas normal
			throw new LeekScriptException(Error.TRANSPILE_TO_JAVA, "No java generated!");
		}

		// System.out.println(compiledJava);

		// Sauvegarde du code java
		try {
			FileOutputStream javaOutput = new FileOutputStream(java);
			javaOutput.write(compiledCode.getJavaCode().getBytes(StandardCharsets.UTF_8));
			javaOutput.close();
		} catch (IOException e) {
			throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
		}

		// Sauvegarde du fichier de lignes
		try {
			FileOutputStream javaOutput = new FileOutputStream(lines);
			javaOutput.write(compiledCode.getLines().getBytes(StandardCharsets.UTF_8));
			javaOutput.close();
		} catch (IOException e) {
			throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
		}

		t = System.nanoTime();
		var fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
		var output = new StringWriter();
		var compilationUnits = Collections.singletonList(new SimpleSourceFile(fileName, compiledCode.getJavaCode()));
		var task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);

		boolean result = task.call();
		long compile_time = System.nanoTime() - t;

		if (!result) { // Java compilation failed
			throw new LeekScriptException(Error.COMPILE_JAVA, output.toString());
		}

		t = System.nanoTime();
		ClassLoader classLoader = new ClassLoader() {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				var bytes = fileManager.get(name).getCompiledBinaries();
				return defineClass(name, bytes, 0, bytes.length);
			}
		};

		// Load inner classes before
		for (var compiledClass : fileManager.getCompiled().values()) {

			if (useClassCache) { // Save bytecode
				try {
					var classFile = new FileOutputStream(Paths.get(IA_PATH, compiledClass.getName() + ".class").toFile());
					classFile.write(compiledClass.getCompiledBinaries());
					classFile.close();
				} catch (IOException e) {
					throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
				}
			}

			if (compiledClass.getName().equals(javaClassName)) continue;
			try {
				classLoader.loadClass(compiledClass.getName());
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}

		// Load the main class
		try {
			var clazz = classLoader.loadClass(javaClassName);
			var ai = (AI) clazz.getDeclaredConstructor().newInstance();
			long load_time = System.nanoTime() - t;

			ai.setId(file.getId());
			ai.setAnalyzeTime(analyze_time);
			ai.setCompileTime(compile_time);
			ai.setLoadTime(load_time);
			ai.setLinesFile(lines);

			if (useClassCache) {
				aiCache.put(javaClassName, new AIClassEntry(clazz, file.getTimestamp()));
			}
			return ai;
		} catch (Exception e) {
			throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
		}
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