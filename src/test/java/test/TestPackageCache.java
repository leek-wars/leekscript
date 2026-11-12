package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.PackageCache;
import leekscript.compiler.SimpleFileManager;
import leekscript.compiler.SimpleSourceFile;
import leekscript.runner.AI;

/**
 * v5 : le package est l'unité de recompilation Java et de cache. D'abord le cache seul, sur du Java
 * écrit à la main (ce qui se recompile quand un corps, une signature ou une constante change), puis
 * des programmes LeekScript à packages (un corps modifié ne recompile que son package, deux points
 * d'entrée partagent les packages qu'ils importent).
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique.
 */
@Isolated
public class TestPackageCache {

	@TempDir Path tmpRoot;
	private RamFileSystem fs;
	private static final AtomicLong UNIQUE = new AtomicLong();
	private String uniqueId;

	@BeforeEach
	void setUp() {
		uniqueId = Long.toHexString(System.nanoTime()) + "_" + UNIQUE.incrementAndGet();
		fs = new RamFileSystem(tmpRoot, LeekScript.MAX_VERSION);
		LeekScript.setFileSystem(fs);
	}

	@AfterEach
	void tearDown() {
		LeekScript.resetFileSystem();
	}

	// =========================================================================
	// Le cache seul : deux groupes, B lit A
	// =========================================================================

	private static final String B = "public class B { public static long g() { return A.f() + A.K; } }";

	private static String a(String type, String body, int constant) {
		return "public class A { public static final int K = " + constant + "; public static " + type + " f() { return " + body + "; } }";
	}

	/** Compile A (groupe « a ») et B (groupe « b ») : les classes passées à javac, puis B.g(). */
	private String build(String a, boolean disk) throws Exception {
		var cache = new PackageCache(tmpRoot.resolve("cache").toFile(), disk);
		cache.add("a", "A", a);
		cache.add("b", "B", B);
		var result = cache.compile(TestPackageCache::javac);
		var loader = new ClassLoader() {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				var bytes = result.classes.get(name);
				if (bytes == null) throw new ClassNotFoundException(name);
				return defineClass(name, bytes, 0, bytes.length);
			}
		};
		var compiled = new ArrayList<>(result.compiled);
		java.util.Collections.sort(compiled);
		return compiled + " " + loader.loadClass("B").getMethod("g").invoke(null);
	}

	private static Map<String, byte[]> javac(List<PackageCache.Unit> units, List<File> classpath) {
		var compiler = ToolProvider.getSystemJavaCompiler();
		var fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
		var sources = new ArrayList<SimpleSourceFile>();
		for (var unit : units) sources.add(new SimpleSourceFile(unit.className() + ".java", unit.javaCode()));
		var path = new StringBuilder(System.getProperty("java.class.path"));
		for (var directory : classpath) path.append(File.pathSeparator).append(directory.getPath());
		var output = new StringWriter();
		if (!compiler.getTask(output, fileManager, null, List.of("-classpath", path.toString(), "-nowarn"), null, sources).call()) {
			throw new IllegalStateException(output.toString());
		}
		var classes = new HashMap<String, byte[]>();
		for (var compiled : fileManager.getCompiled().values()) classes.put(compiled.getName(), compiled.getCompiledBinaries());
		return classes;
	}

	@Test
	public void bodyChangeRecompilesOnlyItsGroup() throws Exception {
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
		assertEquals("[] 11", build(a("int", "10", 1), true));
		assertEquals("[A] 21", build(a("int", "20", 1), true));
	}

	// B ne change pas d'une lettre, mais son bytecode appelle `A.f()I` : une signature qui change
	// le recompile, sinon il planterait à l'exécution (NoSuchMethodError).
	@Test
	public void signatureChangeRecompilesItsReaders() throws Exception {
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
		assertEquals("[A, B] 11", build(a("long", "10", 1), true));
	}

	// javac recopie une constante chez ceux qui la lisent : sa valeur fait partie de l'interface.
	@Test
	public void constantChangeRecompilesItsReaders() throws Exception {
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
		assertEquals("[A, B] 15", build(a("int", "10", 5), true));
	}

	// Une entrée de cache n'est jamais réécrite : revenir à une version déjà compilée ne coûte rien,
	// y compris à ceux qui la lisaient.
	@Test
	public void earlierVersionsStayCached() throws Exception {
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
		assertEquals("[A, B] 11", build(a("long", "10", 1), true));
		assertEquals("[] 11", build(a("int", "10", 1), true));
		assertEquals("[] 11", build(a("long", "10", 1), true));
	}

	@Test
	public void withoutDiskEverythingCompilesAtOnce() throws Exception {
		assertEquals("[A, B] 11", build(a("int", "10", 1), false));
		assertEquals("[A, B] 11", build(a("int", "10", 1), false));
		assertTrue(!Files.exists(tmpRoot.resolve("cache")));
	}

	// =========================================================================
	// Programmes LeekScript à packages
	// =========================================================================

	/** Deux packages, `los` qui lit `astar`. Leur contenu porte `uniqueId` : rien n'est déjà en cache. */
	private void writePackages(String factor) throws IOException {
		write("astar/package.leek", "package astar\nexport { twice } from \"./core\"");
		write("astar/core.leek", "export function twice(integer n) -> integer { tag() return n * " + factor + " }\nfunction tag() { return \"" + uniqueId + "\" }");
		write("los/package.leek", "package los\ndepends astar from \"../astar\"\nimport { twice } from \"@astar\"\nexport function f(integer n) -> integer { tag() return twice(n) + 1 }\nfunction tag() { return \"" + uniqueId + "\" }");
	}

	private static final String PROGRAM = "depends los from \"los\"\nimport { f } from \"@los\"\nreturn f(3)";

	@Test
	public void bodyChangeRecompilesOnlyItsPackage() throws Exception {
		writePackages("2");
		String main = writeMain(PROGRAM + " // " + uniqueId);
		assertEquals("7", run(main));
		// L'IA, les deux racines de package et le module de `astar`
		assertEquals(4, javacUnits(main).size(), javacUnits(main).toString());

		writePackages("3");
		touch("astar/core.leek");
		assertEquals("10", run(main));
		var again = javacUnits(main);
		assertEquals(2, again.size(), again.toString());
		assertTrue(again.stream().allMatch(c -> c.startsWith("AI_P_core_") || c.startsWith("AI_P_package_")), again.toString());
	}

	@Test
	public void twoEntryPointsShareTheirPackages() throws Exception {
		writePackages("2");
		String first = writeMain(PROGRAM);
		assertEquals("7", run(first));
		assertEquals(4, javacUnits(first).size(), javacUnits(first).toString());

		uniqueId += "_b";
		String second = writeMain("depends los from \"los\"\nimport { f } from \"@los\"\nreturn f(4)");
		assertEquals("9", run(second));
		// Seule la classe du second programme passe par javac
		assertEquals(1, javacUnits(second).size(), javacUnits(second).toString());
	}

	// Le Java d'un package ne dépend pas de ceux qui l'appellent : deux programmes qui lui passent
	// des valeurs de types différents partagent quand même ses classes.
	@Test
	public void callersDoNotShapeAPackage() throws Exception {
		write("box/package.leek", "package box\nexport function wrap(any x) -> Array<any> { tag() return [x, x] }\nexport function size(Array<any> xs) -> integer { return Array.count(xs) }\nfunction tag() { return \"" + uniqueId + "\" }");
		String first = writeMain("depends box from \"box\"\nimport { wrap, size } from \"@box\"\nreturn size(wrap(12))");
		assertEquals("2", run(first));
		uniqueId += "_b";
		String second = writeMain("depends box from \"box\"\nimport { wrap, size } from \"@box\"\nvar w = wrap(\"a\")\nreturn string(w) + size(w) + size([1.5])");
		assertEquals("[\"a\", \"a\"]21", run(second));
		assertEquals(1, javacUnits(second).size(), javacUnits(second).toString());
	}

	// Rechargé du disque (autre processus), le programme retrouve les classes de ses packages.
	@Test
	public void packagesReloadFromDiskCache() throws Exception {
		writePackages("2");
		String main = writeMain(PROGRAM);
		assertEquals("7", run(main));
		var file = fs.getRoot(0).resolve(main);
		var cache = JavaCompiler.class.getDeclaredField("aiCache");
		cache.setAccessible(true);
		((java.util.Map<?, ?>) cache.get(null)).remove(file.getJavaClass());
		file.setCompiledCode(null);
		assertEquals("7", run(main));
		// Pas d'analyse ni de javac : les classes viennent des dossiers des packages
		assertEquals(null, file.getCompiledCode());
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private Path write(String relative, String content) throws IOException {
		var p = tmpRoot.resolve(relative);
		Files.createDirectories(p.getParent());
		Files.writeString(p, content);
		return p;
	}

	private void touch(String relative) throws IOException {
		Files.setLastModifiedTime(tmpRoot.resolve(relative), FileTime.fromMillis(System.currentTimeMillis() + 5000));
	}

	private String writeMain(String content) throws IOException {
		String name = "Main_" + uniqueId;
		write(name + ".leek", content);
		return name;
	}

	private List<String> javacUnits(String name) throws Exception {
		return fs.getRoot(0).resolve(name).getCompiledCode().getJavacUnits();
	}

	private String run(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		AI ai = JavaCompiler.compile(file, new Options(LeekScript.MAX_VERSION, false, true, true, null, true));
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		return ai.string(ai.runIA());
	}
}
