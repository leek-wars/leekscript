package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import leekscript.common.Error;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;

/**
 * Modules v5 (pilier 3, phase 3b) — résolution multi-fichiers : import effectif,
 * visibilité privée par défaut, codegen sans collision entre modules, imports
 * transitifs. Harness filesystem partagé {@link RamFileSystem} (cf. TestIncludeCache).
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique.
 */
@Isolated
public class TestModulesResolution {

	@TempDir Path tmpRoot;
	private RamFileSystem fs;
	private static final AtomicLong UNIQUE = new AtomicLong();
	private String uniqueId;

	@BeforeEach
	void setUp() {
		uniqueId = Long.toHexString(System.nanoTime()) + "_" + UNIQUE.incrementAndGet();
		fs = new RamFileSystem(tmpRoot, LeekScript.LATEST_VERSION);
		LeekScript.setFileSystem(fs);
	}

	@AfterEach
	void tearDown() {
		LeekScript.resetFileSystem();
	}

	// =========================================================================
	// Import de base
	// =========================================================================

	@Test
	public void importExportedFunction() throws Exception {
		write("utils.leek", "export function clamp(x, lo, hi) { return x < lo ? lo : (x > hi ? hi : x) }");
		String main = writeMain("import { clamp } from \"utils\"\nreturn clamp(15, 0, 10)");
		assertEquals("10", run(main));
	}

	@Test
	public void importMultipleNames() throws Exception {
		write("math2.leek", "export function add(a, b) { return a + b }\nexport function mul(a, b) { return a * b }");
		String main = writeMain("import { add, mul } from \"math2\"\nreturn add(2, 3) + mul(4, 5)");
		assertEquals("25", run(main));
	}

	// =========================================================================
	// Visibilité privée par défaut
	// =========================================================================

	@Test
	public void importFromMissingFile() throws Exception {
		String main = writeMain("import { clamp } from \"nonexistent\"\nreturn 1");
		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.AI_NOT_EXISTING), "import d'un fichier introuvable, got: " + errors);
	}

	@Test
	public void nonExportedFunctionIsNotImportable() throws Exception {
		write("secret.leek", "function hidden() { return 42 }\nexport function visible() { return 1 }");
		String main = writeMain("import { hidden } from \"secret\"\nreturn hidden()");
		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.SYMBOL_NOT_EXPORTED), "import d'un non-exporté doit échouer, got: " + errors);
	}

	@Test
	public void privateFunctionNotVisibleEvenWithoutImport() throws Exception {
		// `hidden` n'est ni exporté ni importé : le module principal ne doit pas le voir.
		write("secret.leek", "function hidden() { return 42 }\nexport function visible() { return 1 }");
		String main = writeMain("import { visible } from \"secret\"\nreturn hidden()");
		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.UNKNOWN_VARIABLE_OR_FUNCTION), "hidden() invisible sans export/import, got: " + errors);
	}

	@Test
	public void exportedFunctionCanCallItsPrivateHelper() throws Exception {
		// La fonction exportée appelle un helper privé de son propre module : OK.
		write("lib.leek",
			"function helper(x) { return x * 2 }\n" +
			"export function publicApi(x) { return helper(x) + 1 }");
		String main = writeMain("import { publicApi } from \"lib\"\nreturn publicApi(10)");
		assertEquals("21", run(main));
	}

	// =========================================================================
	// Pas de collision de noms entre modules (codegen préfixé)
	// =========================================================================

	@Test
	public void sameFunctionNameInTwoModulesNoCollision() throws Exception {
		// Le module principal ET lib définissent `value()`. lib exporte `get()` qui
		// appelle SON `value()`. Les deux doivent coexister sans s'écraser.
		write("lib.leek",
			"function value() { return 100 }\n" +
			"export function get() { return value() }");
		String main = writeMain(
			"import { get } from \"lib\"\n" +
			"function value() { return 1 }\n" +
			"return value() + get()");
		assertEquals("101", run(main));
	}

	// =========================================================================
	// Imports transitifs
	// =========================================================================

	@Test
	public void transitiveImport() throws Exception {
		write("c.leek", "export function base() { return 7 }");
		write("b.leek", "import { base } from \"c\"\nexport function middle() { return base() * 2 }");
		String main = writeMain("import { middle } from \"b\"\nreturn middle()");
		assertEquals("14", run(main));
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

	private String writeMain(String content) throws IOException {
		String name = "Main_" + uniqueId;
		write(name + ".leek", content);
		return name;
	}

	private String run(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		var options = new Options(LeekScript.LATEST_VERSION, false, true, true, null, true);
		AI ai = JavaCompiler.compile(file, options);
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		return ai.string(ai.runIA());
	}

	private List<Error> compileAndCollectErrors(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		var options = new Options(LeekScript.LATEST_VERSION, false, true, true, null, true);
		var errors = new ArrayList<Error>();
		try {
			JavaCompiler.compile(file, options);
		} catch (LeekCompilerException e) {
			errors.add(e.getError());
		}
		file.getErrors().forEach(e -> errors.add(e.error));
		return errors;
	}
}
