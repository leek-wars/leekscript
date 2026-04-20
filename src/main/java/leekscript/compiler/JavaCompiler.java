package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
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

	// SoftReference avec nom de classe pour le logging
	private static class AIClassSoftReference extends SoftReference<AIClassEntry> {
		final String className;
		public AIClassSoftReference(String className, AIClassEntry entry, ReferenceQueue<AIClassEntry> queue) {
			super(entry, queue);
			this.className = className;
		}
	}

	public final static String IA_PATH = "ai";
	private static javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private static final Semaphore compileSemaphore = new Semaphore(4);
	private static String classpath;
	private static List<String> arguments = new ArrayList<>();
	private static ConcurrentHashMap<String, AIClassSoftReference> aiCache = new ConcurrentHashMap<>();
	private static ReferenceQueue<AIClassEntry> refQueue = new ReferenceQueue<>();
	// AIFile contient des champs mutables (errors, tokens, compiledCode) partagés via le cache global
	// DbFileSystem ; sans lock, deux threads compilant le même AI en parallèle racent dessus.
	// Borné par le nombre d'AIs uniques jamais compilés (cleanup non-trivial sans ref counting).
	private static final ConcurrentHashMap<String, Object> compilationLocks = new ConcurrentHashMap<>();

	public static int getCacheSize() {
		return aiCache.size();
	}

	static {
		classpath = System.getProperty("java.class.path");
		arguments.addAll(Arrays.asList("-classpath", classpath, "-nowarn"));

		// Thread de monitoring des classes libérées par le GC
		Thread monitor = new Thread(() -> {
			while (true) {
				try {
					// Bloque jusqu'à ce qu'une référence soit collectée
					var ref = (AIClassSoftReference) refQueue.remove();
					System.out.println("[SoftRef GC] Class " + ref.className + " was garbage collected");
					// Nettoie l'entrée orpheline du cache
					aiCache.remove(ref.className);
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "AICache-GC-Monitor");
		monitor.setDaemon(true);
		monitor.start();
	}

	/**
	 * Timestamp effectif = max(timestamp du fichier, mtime disque de chaque include).
	 * Permet d'invalider le cache si un include a changé même quand le fichier principal n'a pas bougé
	 * (ex: switch de branche git qui ne modifie qu'un fichier inclus).
	 *
	 * Sur un AIFile fraîchement chargé (ex: premier compile post-restart), getIncludedAIs() est null
	 * tant qu'aucun analyze n'a tourné dans ce process. On demande alors au FileSystem de charger la
	 * liste depuis sa source persistante (côté worker : table ai_include_path). Une fois mémoïsé sur
	 * le AIFile, les appels suivants évitent la requête.
	 */
	private static long effectiveTimestamp(AIFile file) {
		var fs = LeekScript.getFileSystem();
		var includes = file.getIncludedAIs();
		if (includes == null && fs != null) {
			includes = fs.loadIncludedAIs(file);
			if (includes != null) file.setIncludedAIs(includes);
		}
		long max = file.getTimestamp();
		if (includes == null || fs == null) return max;
		for (var inc : includes) {
			long t = fs.getAITimestamp(inc);
			if (t > max) max = t;
		}
		return max;
	}

	private static AI loadFromRamCache(AIFile file, File java, File lines) throws LeekScriptException {
		var ref = aiCache.get(file.getJavaClass());
		var entry = ref != null ? ref.get() : null;
		if (ref != null && entry == null) {
			System.out.println("[SoftRef] Class " + file.getJavaClass() + " was garbage collected, reloading");
		}
		if (entry == null || file.getTimestamp() <= 0 || entry.timestamp < effectiveTimestamp(file)) {
			return null;
		}
		try {
			var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
			ai.setId(file.getId());
			ai.setLinesFile(lines);
			ai.increaseRAMDirect((int) (java.length() * 10));
			return ai;
		} catch (Exception e) {
			throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
		}
	}

	public static AI compile(AIFile file, Options options) throws LeekScriptException, LeekCompilerException {

		var root = new File(IA_PATH);
		if (!root.exists()) root.mkdir();

		String fileName = file.getJavaClass() + ".java";
		File compiled = Paths.get(IA_PATH, file.getJavaClass() + ".class").toFile();
		File java = Paths.get(IA_PATH, file.getJavaClass() + ".java").toFile();
		File lines = Paths.get(IA_PATH, file.getJavaClass() + ".lines").toFile();

		AI cached = loadFromRamCache(file, java, lines);
		if (cached != null) return cached;

		Object lock = compilationLocks.computeIfAbsent(file.getJavaClass(), k -> new Object());
		synchronized (lock) {

			cached = loadFromRamCache(file, java, lines);
			if (cached != null) return cached;

			long effectiveTs = effectiveTimestamp(file);
			if (options.useCache() && file.getTimestamp() > 0 && compiled.exists() && compiled.length() != 0 && compiled.lastModified() >= effectiveTs) {
				try {
					// ClassLoader éphémère par AI pour permettre le GC des classes ;
					// ne pas le close, ça invaliderait la classe chargée.
					@SuppressWarnings("resource")
					var classLoader = new URLClassLoader(new URL[] { new File(IA_PATH).toURI().toURL() }, JavaCompiler.class.getClassLoader());
					var clazz = classLoader.loadClass(file.getJavaClass());
					var entry = new AIClassEntry(clazz, effectiveTs);
					aiCache.put(file.getJavaClass(), new AIClassSoftReference(file.getJavaClass(), entry, refQueue));
					var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
					ai.setId(file.getId());
					ai.setFile(file);
					ai.setLinesFile(lines);
					ai.increaseRAMDirect((int) (java.length() * 10));
					return ai;
				} catch (Exception e) {
					throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
				}
			}

			long t = System.nanoTime();
			var lsCompiler = new IACompiler();
			file.setCompiledCode(lsCompiler.compile(file, file.getJavaClass(), options));
			long analyze_time = System.nanoTime() - t;

			if (file.getCompiledCode().getJavaCode().isEmpty()) {
				throw new LeekScriptException(Error.TRANSPILE_TO_JAVA, "No java generated!");
			}

			if (options.useCache()) {
				try {
					FileOutputStream javaOutput = new FileOutputStream(java);
					javaOutput.write(file.getCompiledCode().getJavaCode().getBytes(StandardCharsets.UTF_8));
					javaOutput.close();
				} catch (IOException e) {
					throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
				}

				try {
					FileOutputStream javaOutput = new FileOutputStream(lines);
					javaOutput.write(file.getCompiledCode().getLines().getBytes(StandardCharsets.UTF_8));
					javaOutput.close();
				} catch (IOException e) {
					throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
				}
			}

			t = System.nanoTime();
			SimpleFileManager fileManager;
			var output = new StringWriter();
			boolean result;
			try {
				compileSemaphore.acquire();
				try {
					fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
					var compilationUnits = Collections.singletonList(new SimpleSourceFile(fileName, file.getCompiledCode().getJavaCode()));
					var task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
					result = task.call();
				} finally {
					compileSemaphore.release();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new LeekScriptException(Error.COMPILE_JAVA, "Compilation interrupted");
			}
			long compile_time = System.nanoTime() - t;

			if (!result) {
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

			for (var compiledClass : fileManager.getCompiled().values()) {

				if (options.useCache()) {
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
				ai.increaseRAMDirect((int) (java.length() * 10));

				if (options.useCache()) {
					// Après compile : includedAIs vient d'être rempli par IACompiler, donc effectiveTimestamp
					// reflète bien l'état réel du fichier + de ses dépendances.
					aiCache.put(file.getJavaClass(), new AIClassSoftReference(file.getJavaClass(), new AIClassEntry(clazz, effectiveTimestamp(file)), refQueue));
				}
				return ai;
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}
	}
}
