package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;

public class LeekScript {

	private final static String IA_PATH = "ai/";
	private static long id = 1;

	private static Resolver<FileSystemContext> defaultResolver = new FileSystemResolver();
	private static Resolver<?> customResolver = null;
	private static String classpath;
	private static List<String> arguments = new ArrayList<>();
	private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private static SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
	private static URLClassLoader urlLoader;
	private static HashMap<String, Class<?>> aiCache = new HashMap<>();
	static {
		try {
			classpath = LeekScript.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			arguments.addAll(Arrays.asList("-classpath", classpath, "-nowarn"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		try {
			urlLoader = new URLClassLoader(new URL[] { new File("ai").toURI().toURL() }, new ClassLoader() {});
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
		return compile(ai, AIClass, true);
	}

	public static AI compileFileContext(String filepath, String AIClass, ResolverContext context, boolean useClassCache) throws LeekScriptException, LeekCompilerException, IOException {
		AIFile<?> ai = getResolver().resolve(filepath, context);
		return compile(ai, AIClass, useClassCache);
	}

	public static AI compileSnippet(String snippet, String AIClass)	throws LeekScriptException, LeekCompilerException, IOException {
		return compileSnippet(snippet, AIClass, 11);
	}

	public static AI compileSnippet(String snippet, String AIClass, int version) throws LeekScriptException, LeekCompilerException, IOException {
		long ai_id = id++;
		AIFile<?> ai = new AIFile<FileSystemContext>("<snippet " + ai_id + ">", snippet, System.currentTimeMillis(), version, null, (int) ai_id);
		return compile(ai, AIClass, false);
	}


	public static AbstractLeekValue runScript(String script, boolean nocache) throws Exception {
		return LeekScript.compileSnippet(script, "AI").runIA();
	}

	public static String runFile(String filename) throws Exception {
		AI ai = LeekScript.compileFile(filename, "AI", true);
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		return v.getString(ai);
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

	public static AI compile(AIFile<?> file, String AIClass, boolean useClassCache) throws LeekScriptException, LeekCompilerException, IOException {

		new File(IA_PATH).mkdir();
		String javaClassName = "AI_" + file.getId();
		String fileName = javaClassName + ".java";
		File compiled = new File(IA_PATH + javaClassName + ".class");
		File java = new File(IA_PATH + javaClassName + ".java");

		// Utilisation du cache de class
		if (useClassCache && compiled.exists() && compiled.length() != 0 && compiled.lastModified() > file.getTimestamp()) {
			try {
				var clazz = aiCache.get(javaClassName);
				if (clazz == null) {
					clazz = urlLoader.loadClass(javaClassName);
					aiCache.put(javaClassName, clazz);
				}
				var ai = (AI) clazz.getDeclaredConstructor().newInstance();
				ai.setId(file.getId());
				return ai;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// On commence par la conversion LS->Java
		long t = System.nanoTime();
		String compiledJava = new IACompiler().compile(file, javaClassName, AIClass);
		long analyze_time = System.nanoTime() - t;

		if (compiledJava.isEmpty()) { // Rien ne compile, pas normal
			throw new LeekScriptException(LeekScriptException.CANT_COMPILE, "No java generated!");
		}

		// System.out.println(compiledJava);

		// Sauvegarde du code java
		FileOutputStream javaOutput = new FileOutputStream(java);
		javaOutput.write(compiledJava.getBytes(StandardCharsets.UTF_8));
		javaOutput.close();

		try {

			t = System.nanoTime();
			fileManager.clear();
			var output = new StringWriter();
			var compilationUnits = Collections.singletonList(new SimpleSourceFile(fileName, compiledJava));
			var task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);

			boolean result = task.call();
			long compile_time = System.nanoTime() - t;

			if (!result) { // Java compilation failed
				throw new LeekScriptException(LeekScriptException.CANT_COMPILE, output.toString());
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
					var classFile = new FileOutputStream(IA_PATH + compiledClass.getName() + ".class");
					classFile.write(compiledClass.getCompiledBinaries());
					classFile.close();
				}

				if (compiledClass.getName().equals(javaClassName)) continue;
				classLoader.loadClass(compiledClass.getName());
			}

			// Load the main class
			var clazz = classLoader.loadClass(javaClassName);
			var ai = (AI) clazz.getDeclaredConstructor().newInstance();
			long load_time = System.nanoTime() - t;

			ai.setId(file.getId());

			if (useClassCache) {
				aiCache.put(javaClassName, clazz);
			}
			return ai;

		} catch (Exception e) {
			throw new LeekScriptException(LeekScriptException.CANT_COMPILE, e.getMessage());
		}
	}

	public static void throwException(String error) throws LeekScriptException {
		if (error != null && !error.isEmpty()) {
			if (error.contains("code too large")) {
				String[] lines = error.split("\n", 3);
				if (lines.length >= 2 && lines[1].split(" ").length > 4) {
					String l = lines[1].split(" ")[2];
					if (l.length() > 4 && !l.startsWith("runIA")) {
						throw new LeekScriptException(LeekScriptException.CODE_TOO_LARGE_FUNCTION, l.substring(14, l.length() - 2));
					}
				}
				throw new LeekScriptException(LeekScriptException.CODE_TOO_LARGE);
			}
		}
		throw new LeekScriptException(LeekScriptException.CANT_COMPILE, error);
	}


}