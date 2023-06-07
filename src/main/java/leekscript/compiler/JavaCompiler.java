package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import leekscript.common.Error;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;

import javax.tools.ToolProvider;

public class JavaCompiler {

	private static class AIClassEntry {
		Class<?> clazz;
		long timestamp;
		public AIClassEntry(Class<?> clazz, long timestamp) {
			this.clazz = clazz;
			this.timestamp = timestamp;
		}
	}

	private static javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	public final static String IA_PATH = "ai";
	private static String classpath;
	private static List<String> arguments = new ArrayList<>();
	private static URLClassLoader urlLoader;
	private static HashMap<String, AIClassEntry> aiCache = new HashMap<>();

	static {
		classpath = new File(LeekScript.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getPath();
		classpath += ":/home/pierre/dev/leek-wars/generator/bin/main";
		classpath += ":/home/pierre/dev/leek-wars/generator/leek-wars-env/bin/main";
		arguments.addAll(Arrays.asList("-classpath", classpath, "-nowarn"));
		try {
			urlLoader = new URLClassLoader(new URL[] { new File(IA_PATH).toURI().toURL() }, new ClassLoader() {});
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public static AI compile(AIFile file, boolean useClassCache, boolean enableOperations) throws LeekScriptException, LeekCompilerException {

		var root = new File(IA_PATH);
		if (!root.exists()) root.mkdir();

		String fileName = file.getJavaClass() + ".java";
		File compiled = Paths.get(IA_PATH, file.getJavaClass() + ".class").toFile();
		File java = Paths.get(IA_PATH, file.getJavaClass() + ".java").toFile();
		File lines = Paths.get(IA_PATH, file.getJavaClass() + ".lines").toFile();

		// Cache des classes en RAM d'abord
		var entry = aiCache.get(file.getJavaClass());
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
				var clazz = urlLoader.loadClass(file.getJavaClass());
				entry = new AIClassEntry(clazz, file.getTimestamp());
				aiCache.put(file.getJavaClass(), entry);
				var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
				ai.setId(file.getId());
				ai.setFile(file);
				ai.setLinesFile(lines);
				return ai;
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}

		// On commence par la conversion LS -> Java
		// System.out.println("Re-compile AI " + file.getPath());
		long t = System.nanoTime();
		var lsCompiler = new IACompiler();
		file.setCompiledCode(lsCompiler.compile(file, file.getJavaClass(), enableOperations));
		long analyze_time = System.nanoTime() - t;

		if (file.getCompiledCode().getJavaCode().isEmpty()) { // Rien ne compile, pas normal
			throw new LeekScriptException(Error.TRANSPILE_TO_JAVA, "No java generated!");
		}

		// System.out.println(compiledJava);

		if (useClassCache) {
			// Sauvegarde du code java
			try {
				FileOutputStream javaOutput = new FileOutputStream(java);
				javaOutput.write(file.getCompiledCode().getJavaCode().getBytes(StandardCharsets.UTF_8));
				javaOutput.close();
			} catch (IOException e) {
				throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
			}

			// Sauvegarde du fichier de lignes
			try {
				FileOutputStream javaOutput = new FileOutputStream(lines);
				javaOutput.write(file.getCompiledCode().getLines().getBytes(StandardCharsets.UTF_8));
				javaOutput.close();
			} catch (IOException e) {
				throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
			}
		}

		t = System.nanoTime();
		var fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
		var output = new StringWriter();
		var compilationUnits = Collections.singletonList(new SimpleSourceFile(fileName, file.getCompiledCode().getJavaCode()));
		var task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);

		boolean result = task.call();
		long compile_time = System.nanoTime() - t;

		if (!result) { // Java compilation failed

			// On récupère la ligne
			int javaLine = -1;
			Pattern r = Pattern.compile("AI_" + file.getId() + ".java:(.*?):");
			Matcher m = r.matcher(output.toString());
			if (m.find()) {
				javaLine = Integer.parseInt(m.group(1));
			}
			var mapping = file.getCompiledCode().getLinesMap().get(javaLine);
			String location = null;
			if (mapping != null) {
				location = file.getCompiledCode().getFiles().get(mapping.getAI()) + ":" + mapping.getLeekScriptLine();
			}

			if (output.toString().contains("code too large")) {
				throw new LeekScriptException(Error.CODE_TOO_LARGE, output.toString(), location);
			} else {
				throw new LeekScriptException(Error.COMPILE_JAVA, output.toString(), location);
			}
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

			if (compiledClass.getName().equals(file.getJavaClass())) continue;
			try {
				classLoader.loadClass(compiledClass.getName());
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}

		// Load the main class
		try {
			var clazz = classLoader.loadClass(file.getJavaClass());
			var ai = (AI) clazz.getDeclaredConstructor().newInstance();
			long load_time = System.nanoTime() - t;

			ai.setFile(file);
			ai.setId(file.getId());
			ai.setAnalyzeTime(analyze_time);
			ai.setCompileTime(compile_time);
			ai.setLoadTime(load_time);
			ai.setLinesFile(lines);

			if (useClassCache) {
				aiCache.put(file.getJavaClass(), new AIClassEntry(clazz, file.getTimestamp()));
			}
			return ai;
		} catch (Exception e) {
			throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
		}
	}
}
