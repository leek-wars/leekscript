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
import java.util.Map;
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
		long signature;
		long bytecodeSize;
		public AIClassEntry(Class<?> clazz, long signature, long bytecodeSize) {
			this.clazz = clazz;
			this.signature = signature;
			this.bytecodeSize = bytecodeSize;
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
	 * Hash stable de (entrypoint mtime, transitively-included [path, mtime]). Toute
	 * modification — mtime forward, mtime backward, fichier ajouté, fichier supprimé,
	 * include renommé — produit une valeur différente, donc le cache se valide par
	 * égalité plutôt que par max-comparaison. Un max() perd l'info quand un include
	 * disparaît ou recule en mtime alors que l'entrypoint domine déjà le max.
	 *
	 * Always asks the FileSystem for the include set rather than reusing
	 * AIFile.includedAIs : that field can be stale (set to {} when an include was
	 * unresolvable, then never refreshed) which silently wedges cache invalidation.
	 */
	/** Signature du cache disque et taille du bytecode, notée sur la 2e ligne. */
	private record CachedSignature(long signature, long bytecodeSize) {}

	/** Null si le fichier manque, est illisible ou n'a pas la taille du bytecode : le cache disque ne sert pas. */
	private static CachedSignature readSignatureFile(File f) {
		if (!f.exists()) return null;
		try {
			var lines = java.nio.file.Files.readString(f.toPath()).trim().split("\n");
			return lines.length > 1 ? new CachedSignature(Long.parseLong(lines[0].trim()), Long.parseLong(lines[1].trim())) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static void writeSignatureFile(File f, long sig, long bytecodeSize) {
		try {
			java.nio.file.Files.writeString(f.toPath(), sig + "\n" + bytecodeSize);
		} catch (Exception e) {
			// best effort : disk cache won't validate next run, but RAM cache still works
		}
	}

	private static long signature(AIFile file) {
		var fs = LeekScript.getFileSystem();
		long sig = file.getTimestamp();
		var includes = fs != null ? fs.loadIncludedAIs(file) : null;
		if (includes == null || fs == null) return sig;
		// Polynomial hash, position-dependent : un git pull qui pousse main.mtime ==
		// include.mtime ne se cancel pas (XOR le ferait). Sort des includes par path
		// car l'itération d'un Set n'est pas déterministe.
		var sorted = new ArrayList<>(includes);
		sorted.sort(java.util.Comparator.comparing(AIFile::getPath));
		for (var inc : sorted) {
			sig = sig * 31 + inc.getPath().hashCode();
			sig = sig * 31 + fs.getAITimestamp(inc);
		}
		return sig;
	}

	/** Sous-dossier d'IA_PATH où se rangent les groupes compilés des programmes à packages. */
	private static final String PACKAGES_PATH = "p";

	/** Les dossiers de cache d'un programme à packages ; null sans manifeste, ou si l'un d'eux manque. */
	private static List<File> readManifest(File manifest) {
		if (!manifest.exists()) return null;
		try {
			var directories = new ArrayList<File>();
			for (var line : java.nio.file.Files.readAllLines(manifest.toPath())) {
				if (line.isEmpty()) continue;
				var directory = new File(line);
				if (!directory.isDirectory()) return null;
				directories.add(directory);
			}
			return directories;
		} catch (IOException e) {
			return null;
		}
	}

	/** `directories` null : le programme n'a pas de package, ses classes sont à plat dans IA_PATH. */
	private static void writeManifest(File manifest, List<File> directories) throws LeekScriptException {
		try {
			if (directories == null) {
				manifest.delete();
			} else {
				java.nio.file.Files.writeString(manifest.toPath(), directories.stream().map(File::getPath).collect(java.util.stream.Collectors.joining("\n")));
			}
		} catch (IOException e) {
			throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
		}
	}

	/**
	 * Compile des sources Java en mémoire, les dossiers de `classpath` en plus du classpath du
	 * compilateur : nom de classe -> bytecode. Une erreur de javac est rapportée sur la ligne
	 * LeekScript de la première classe fautive, qu'elle soit celle de l'IA ou celle d'un module.
	 */
	private static Map<String, byte[]> javac(AIFile file, List<PackageCache.Unit> units, List<File> classpath) throws LeekScriptException {
		SimpleFileManager fileManager;
		var output = new StringWriter();
		boolean result;
		try {
			compileSemaphore.acquire();
			try {
				fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
				var compilationUnits = new ArrayList<SimpleSourceFile>();
				for (var unit : units) {
					compilationUnits.add(new SimpleSourceFile(unit.className() + ".java", unit.javaCode()));
				}
				var options = arguments;
				if (!classpath.isEmpty()) {
					var path = new StringBuilder(JavaCompiler.classpath);
					for (var directory : classpath) path.append(File.pathSeparator).append(directory.getPath());
					options = Arrays.asList("-classpath", path.toString(), "-nowarn");
				}
				var task = compiler.getTask(output, fileManager, null, options, null, compilationUnits);
				result = task.call();
			} finally {
				compileSemaphore.release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LeekScriptException(Error.COMPILE_JAVA, "Compilation interrupted");
		}

		if (!result) {
			var errors = output.toString();
			int javaLine = -1;
			// La première erreur, dans la classe de l'IA ou dans celle d'un de ses modules
			var javaCode = file.getCompiledCode().getJavaCode();
			var linesMap = file.getCompiledCode().getLinesMap();
			Pattern r = Pattern.compile("(" + Pattern.quote(file.getJavaClass()) + "\\w*|" + JavaWriter.PACKAGE_CLASS_PREFIX + "\\w+)\\.java:(\\d+):");
			Matcher m = r.matcher(errors);
			if (m.find()) {
				javaLine = Integer.parseInt(m.group(2));
				for (var module : file.getCompiledCode().getModules()) {
					if (module.className().equals(m.group(1))) {
						javaCode = module.javaCode();
						linesMap = module.lines();
					}
				}
			}
			var mapping = linesMap.get(javaLine);
			String location = null;
			if (mapping != null) {
				location = file.getCompiledCode().getFiles().get(mapping.getAI()) + ":" + mapping.getLeekScriptLine();
			}

			if (JVM_LIMIT_ERRORS.stream().anyMatch(errors::contains)) {
				throw new LeekScriptException(Error.CODE_TOO_LARGE, errors, location);
			} else {
				throw new LeekScriptException(Error.COMPILE_JAVA, errors, location)
					.details(javaExcerpt(javaCode, javaLine));
			}
		}
		var classes = new java.util.HashMap<String, byte[]>();
		for (var compiledClass : fileManager.getCompiled().values()) {
			classes.put(compiledClass.getName(), compiledClass.getCompiledBinaries());
		}
		return classes;
	}

	private static AI loadFromRamCache(AIFile file, File lines) throws LeekScriptException {
		var ref = aiCache.get(file.getJavaClass());
		var entry = ref != null ? ref.get() : null;
		if (ref != null && entry == null) {
			System.out.println("[SoftRef] Class " + file.getJavaClass() + " was garbage collected, reloading");
		}
		if (entry == null || file.getTimestamp() <= 0 || entry.signature != signature(file)) {
			return null;
		}
		try {
			var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
			ai.setId(file.getId());
			ai.setLinesFile(lines);
			ai.chargeCodeRAM(entry.bytecodeSize);
			return ai;
		} catch (Exception e) {
			throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
		}
	}

	/**
	 * Limites de la JVM atteintes par le code du joueur, dans les messages de javac : méthode de plus de
	 * 64 Ko, fonction de plus de 255 emplacements de paramètres (un integer ou un real en prend deux),
	 * classe de plus de 65 535 constantes, chaîne littérale de plus de 65 535 caractères ou octets en
	 * UTF-8. Une erreur de l'IA (CODE_TOO_LARGE), pas du serveur.
	 */
	private static final List<String> JVM_LIMIT_ERRORS = List.of("code too large", "too many parameters",
			"too many constants", "constant string too long", "too long for the constant pool");

	/** Nombre de lignes de Java généré conservées de part et d'autre de la ligne fautive. */
	private static final int EXCERPT_CONTEXT = 12;
	private static final int EXCERPT_MAX_LINE = 4000;

	/**
	 * Extrait numéroté du Java généré autour de la ligne rejetée par javac. Joint au
	 * rapport d'erreur : sans lui, un COMPILE_JAVA n'est pas diagnosticable après coup
	 * (le message javac ne montre que la ligne fautive, et le source LeekScript stocké
	 * avec l'erreur est re-téléchargé donc potentiellement différent de ce qui a été
	 * compilé). Les lignes générées peuvent être très longues : on les tronque.
	 */
	public static String javaExcerpt(String javaCode, int javaLine) {
		if (javaCode == null || javaLine <= 0) return null;
		var lines = javaCode.split("\n", -1);
		if (javaLine > lines.length) return null;
		int from = Math.max(0, javaLine - 1 - EXCERPT_CONTEXT);
		int to = Math.min(lines.length, javaLine + EXCERPT_CONTEXT);
		var sb = new StringBuilder("Java généré (lignes " + (from + 1) + "-" + to + ", `>` = ligne rejetée par javac) :\n");
		for (int i = from; i < to; i++) {
			var line = lines[i];
			if (line.length() > EXCERPT_MAX_LINE) line = line.substring(0, EXCERPT_MAX_LINE) + " […]";
			sb.append(i + 1 == javaLine ? "> " : "  ").append(i + 1).append(": ").append(line).append('\n');
		}
		return sb.toString();
	}

	public static AI compile(AIFile file, Options options) throws LeekScriptException, LeekCompilerException {

		var root = new File(IA_PATH);
		if (!root.exists()) root.mkdir();

		String fileName = file.getJavaClass() + ".java";
		File compiled = Paths.get(IA_PATH, file.getJavaClass() + ".class").toFile();
		File java = Paths.get(IA_PATH, file.getJavaClass() + ".java").toFile();
		File lines = Paths.get(IA_PATH, file.getJavaClass() + ".lines").toFile();
		// Sidecar storing the signature of the source set captured at compile time.
		// Disk cache validity = byte-equal sig file (signature is a non-monotonic
		// hash, can't be compared via lastModified).
		File sigFile = Paths.get(IA_PATH, file.getJavaClass() + ".sig").toFile();
		// Programme v5 à packages : les dossiers de cache de ses groupes, un par ligne
		File manifest = Paths.get(IA_PATH, file.getJavaClass() + ".pkgs").toFile();

		AI cached = loadFromRamCache(file, lines);
		if (cached != null) return cached;

		Object lock = compilationLocks.computeIfAbsent(file.getJavaClass(), k -> new Object());
		synchronized (lock) {

			cached = loadFromRamCache(file, lines);
			if (cached != null) return cached;

			long sig = signature(file);
			// Programme v5 à packages : ses classes sont dans les dossiers de ses groupes
			var packages = options.useCache() ? readManifest(manifest) : null;
			var onDisk = options.useCache() && file.getTimestamp() > 0 && (packages != null || compiled.exists() && compiled.length() != 0) ? readSignatureFile(sigFile) : null;
			if (onDisk != null && onDisk.signature() == sig) {
				try {
					var urls = new ArrayList<URL>();
					if (packages == null) urls.add(new File(IA_PATH).toURI().toURL());
					else for (var directory : packages) urls.add(directory.toURI().toURL());
					// ClassLoader éphémère par AI pour permettre le GC des classes ;
					// ne pas le close, ça invaliderait la classe chargée.
					@SuppressWarnings("resource")
					var classLoader = new URLClassLoader(urls.toArray(new URL[0]), JavaCompiler.class.getClassLoader());
					var clazz = classLoader.loadClass(file.getJavaClass());
					var entry = new AIClassEntry(clazz, sig, onDisk.bytecodeSize());
					aiCache.put(file.getJavaClass(), new AIClassSoftReference(file.getJavaClass(), entry, refQueue));
					var ai = (AI) entry.clazz.getDeclaredConstructor().newInstance();
					ai.setId(file.getId());
					ai.setFile(file);
					ai.setLinesFile(lines);
					ai.chargeCodeRAM(entry.bytecodeSize);
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
					for (var module : file.getCompiledCode().getModules()) {
						try (var moduleOutput = new FileOutputStream(Paths.get(IA_PATH, module.className() + ".java").toFile())) { moduleOutput.write(module.javaCode().getBytes(StandardCharsets.UTF_8)); }
					}
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
			var code = file.getCompiledCode();
			Map<String, byte[]> classes;
			List<File> packageDirectories = List.of();
			if (code.hasPackages()) {
				// v5 : chaque package se compile et se cache à part (cf PackageCache)
				var cache = new PackageCache(new File(IA_PATH, PACKAGES_PATH), options.useCache());
				cache.add(PackageCache.PROGRAM_GROUP, file.getJavaClass(), code.getJavaCode());
				for (var module : code.getModules()) {
					cache.add(module.group(), module.className(), module.javaCode());
				}
				var result = cache.compile((units, classpath) -> javac(file, units, classpath));
				classes = result.classes;
				packageDirectories = result.directories;
				code.setJavacUnits(result.compiled);
			} else {
				var units = new ArrayList<PackageCache.Unit>();
				units.add(new PackageCache.Unit(file.getJavaClass(), code.getJavaCode()));
				// v5 : une unité par classe de module, compilées ensemble (elles se lisent l'une l'autre)
				for (var module : code.getModules()) {
					units.add(new PackageCache.Unit(module.className(), module.javaCode()));
				}
				classes = javac(file, units, List.of());
				code.setJavacUnits(units.stream().map(PackageCache.Unit::className).toList());
			}
			long compile_time = System.nanoTime() - t;

			t = System.nanoTime();
			var loaded = classes;
			ClassLoader classLoader = new ClassLoader() {
				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {
					var bytes = loaded.get(name);
					if (bytes == null) throw new ClassNotFoundException(name);
					return defineClass(name, bytes, 0, bytes.length);
				}
			};

			long bytecodeSize = 0;
			for (var compiledClass : classes.entrySet()) {

				var bytes = compiledClass.getValue();
				bytecodeSize += bytes.length;
				// (les classes d'un programme à packages sont déjà dans les dossiers de ses groupes)
				if (options.useCache() && !code.hasPackages()) {
					try {
						var classFile = new FileOutputStream(Paths.get(IA_PATH, compiledClass.getKey() + ".class").toFile());
						classFile.write(bytes);
						classFile.close();
					} catch (IOException e) {
						throw new LeekScriptException(Error.CANNOT_WRITE_AI, e.getMessage());
					}
				}

				if (compiledClass.getKey().equals(file.getJavaClass())) continue;
				try {
					classLoader.loadClass(compiledClass.getKey());
				} catch (Exception e) {
					throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
				}
			}
			if (options.useCache()) writeManifest(manifest, code.hasPackages() ? packageDirectories : null);

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
				ai.chargeCodeRAM(bytecodeSize);

				if (options.useCache()) {
					long currentSig = signature(file);
					writeSignatureFile(sigFile, currentSig, bytecodeSize);
					aiCache.put(file.getJavaClass(), new AIClassSoftReference(file.getJavaClass(), new AIClassEntry(clazz, currentSig, bytecodeSize), refQueue));
				}
				return ai;
			} catch (Exception e) {
				throw new LeekScriptException(Error.CANNOT_LOAD_AI, e.getMessage());
			}
		}
	}
}
