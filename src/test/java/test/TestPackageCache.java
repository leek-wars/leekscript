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

	// C lit un membre de A à travers B, qui en hérite : l'interface de B, pour ses lecteurs,
	// comprend ce qu'elle hérite d'un autre groupe.
	@Test
	public void inheritedSignatureChangeRecompilesReadersOfTheSubclass() throws Exception {
		var b = "public class B extends A { }";
		var c = "public class C { public static long g() { return new B().f(); } }";
		for (var type : new String[] { "int", "long" }) {
			var cache = new PackageCache(tmpRoot.resolve("cache").toFile(), true);
			cache.add("a", "A", "public class A { public " + type + " f() { return 7; } }");
			cache.add("b", "B", b);
			cache.add("c", "C", c);
			var result = cache.compile(TestPackageCache::javac);
			assertTrue(result.compiled.contains("C"), result.compiled.toString());
			var loader = new ClassLoader() {
				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {
					var bytes = result.classes.get(name);
					if (bytes == null) throw new ClassNotFoundException(name);
					return defineClass(name, bytes, 0, bytes.length);
				}
			};
			assertEquals(7L, loader.loadClass("C").getMethod("g").invoke(null));
		}
	}

	// Ménage : une entrée qui n'a servi à aucune compilation depuis UNUSED_DAYS jours disparaît,
	// celles qui servent restent, et un groupe dont l'entrée a disparu se recompile.
	@Test
	public void unusedEntriesAreSwept() throws Exception {
		var root = tmpRoot.resolve("cache").toFile();
		long day = 24 * 3600 * 1000L;
		long now = System.currentTimeMillis();
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
		assertEquals("[A] 21", build(a("int", "20", 1), true));
		assertEquals(3, entries(root).size());
		assertEquals(0, PackageCache.sweep(root, now + (PackageCache.UNUSED_DAYS - 1) * day));
		// 40 jours plus tard, la version 20 a resservi la veille : l'ancienne version de A part seule
		long later = now + 40 * day;
		for (var entry : entries(root)) new File(entry, "ok").setLastModified(now);
		for (var entry : entries(root)) if (!isFirstA(entry)) new File(entry, "ok").setLastModified(later - day);
		assertEquals(1, PackageCache.sweep(root, later));
		assertEquals(2, entries(root).size());
		assertEquals("[] 21", build(a("int", "20", 1), true));
		assertEquals("[A] 11", build(a("int", "10", 1), true));
		// Tout a vieilli : plus rien, et le programme se recompile
		assertEquals(3, PackageCache.sweep(root, later + 100 * day));
		assertEquals(0, entries(root).size());
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
	}

	// Une écriture interrompue laisse un dossier temporaire : il part au bout d'un jour.
	@Test
	public void interruptedWritesAreSwept() throws Exception {
		var root = tmpRoot.resolve("cache").toFile();
		assertEquals("[A, B] 11", build(a("int", "10", 1), true));
		var source = entries(root).get(0).getParentFile();
		var temporary = new File(source, "tmp123");
		assertTrue(temporary.mkdir());
		Files.writeString(temporary.toPath().resolve("A.class"), "");
		long now = System.currentTimeMillis();
		assertEquals(0, PackageCache.sweep(root, now));
		assertEquals(1, PackageCache.sweep(root, now + 2 * 24 * 3600 * 1000L));
		assertTrue(!temporary.exists());
		assertEquals("[] 11", build(a("int", "10", 1), true));
	}

	/** Les entrées de cache (dossiers `<Java>/<interfaces lues>`) sous `root`. */
	private static List<File> entries(File root) {
		var found = new ArrayList<File>();
		var sources = root.listFiles(File::isDirectory);
		if (sources == null) return found;
		for (var source : sources) {
			for (var entry : source.listFiles(File::isDirectory)) {
				if (new File(entry, "ok").exists()) found.add(entry);
			}
		}
		return found;
	}

	/** L'entrée de la première version de A (`return 10`) : la seule dont le Java la contient. */
	private static boolean isFirstA(File entry) throws IOException {
		var java = new File(entry, "java/A.java");
		return java.exists() && Files.readString(java.toPath()).contains("return 10;");
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
		forgetInMemory(main);
		assertEquals("7", run(main));
		// Pas d'analyse ni de javac : les classes viennent des dossiers des packages
		assertEquals(null, file.getCompiledCode());
	}

	// Une IA qui passe aux packages a encore sa classe d'avant, à plat : elle ne doit plus servir,
	// même quand les entrées de ses packages ont disparu.
	@Test
	public void flatClassFromBeforePackagesIsNeverReloaded() throws Exception {
		String main = writeMain("return \"sans package " + uniqueId + "\"");
		assertEquals("sans package " + uniqueId, run(main));
		writePackages("2");
		write(main + ".leek", PROGRAM);
		touch(main + ".leek");
		assertEquals("7", run(main));
		forgetInMemory(main);
		PackageCache.sweep(new File(JavaCompiler.IA_PATH, "p"), System.currentTimeMillis() + 1000L * 24 * 3600 * 1000);
		assertEquals("7", run(main));
	}

	// Chargée du disque, une IA ne lit plus rien du cache : le ménage ne lui retire aucune classe.
	@Test
	public void sweepDoesNotBreakALoadedProgram() throws Exception {
		write("geo/package.leek", "package geo\nexport class Pt { public integer x = 3; Array<integer> twice() { return [1, 2].map(v => v * x) } }\nexport function make() -> Pt { tag() return new Pt() }\nfunction tag() { return \"" + uniqueId + "\" }");
		String main = writeMain("depends geo from \"geo\"\nimport { make } from \"@geo\"\nreturn make().twice()");
		assertEquals("[3, 6]", run(main));
		forgetInMemory(main);
		AI ai = load(main);
		PackageCache.sweep(new File(JavaCompiler.IA_PATH, "p"), System.currentTimeMillis() + 1000L * 24 * 3600 * 1000);
		assertEquals("[3, 6]", ai.string(ai.runIA()));
	}

	// L'ordre des membres générés d'un package ne tient ni au hasard ni au reste du programme :
	// un point d'entrée qui déclare lui-même des fonctions partage quand même le package.
	@Test
	public void packageJavaDoesNotDependOnTheRestOfTheProgram() throws Exception {
		var sb = new StringBuilder("package lib\n");
		var values = new StringBuilder();
		for (int i = 0; i < 12; i++) {
			sb.append("export function f").append(i).append("(integer n) -> integer { return n + ").append(i).append(" }\n");
			values.append(i > 0 ? ", " : "").append("f").append(i);
		}
		sb.append("export function all() -> Array<any> { tag() return [").append(values).append("] }\nfunction tag() { return \"").append(uniqueId).append("\" }");
		write("lib/package.leek", sb.toString());
		String first = writeMain("depends lib from \"lib\"\nimport { all } from \"@lib\"\nreturn Array.count(all())");
		assertEquals("12", run(first));
		uniqueId += "_b";
		var second = new StringBuilder("depends lib from \"lib\"\nimport { all, f3 } from \"@lib\"\n");
		for (int i = 0; i < 40; i++) second.append("function own").append(i).append("() { return ").append(i).append(" }\n");
		second.append("return Array.count(all()) + f3(1) + own7()");
		String other = writeMain(second.toString());
		assertEquals("23", run(other));
		assertEquals(1, javacUnits(other).size(), javacUnits(other).toString());
	}

	// Les entrées d'un programme ont été supprimées par le ménage : il se recompile en entier.
	@Test
	public void programRecompilesAfterItsEntriesAreSwept() throws Exception {
		writePackages("2");
		String main = writeMain(PROGRAM);
		assertEquals("7", run(main));
		PackageCache.sweep(new File(JavaCompiler.IA_PATH, "p"), System.currentTimeMillis() + 1000L * 24 * 3600 * 1000);
		forgetInMemory(main);
		assertEquals("7", run(main));
		assertEquals(4, javacUnits(main).size(), javacUnits(main).toString());
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

	/** Comme un autre processus : ni classe en mémoire, ni code compilé gardé sur le fichier. */
	private void forgetInMemory(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		var cache = JavaCompiler.class.getDeclaredField("aiCache");
		cache.setAccessible(true);
		((java.util.Map<?, ?>) cache.get(null)).remove(file.getJavaClass());
		file.setCompiledCode(null);
	}

	private AI load(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		AI ai = JavaCompiler.compile(file, new Options(LeekScript.MAX_VERSION, false, true, true, null, true));
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		return ai;
	}

	private String run(String name) throws Exception {
		AI ai = load(name);
		return ai.string(ai.runIA());
	}
}
