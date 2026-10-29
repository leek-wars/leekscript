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
 * Packages v5 (docs/ls5-modules.md §2) : un dossier avec un package.leek (`package <nom>`, des
 * `depends`), importé par `"@nom"`, sans cycle entre packages.
 *
 * Ancien en-tête du harnais copié de TestModulesResolution :
 * Modules v5 (pilier 3, phase 3b) — résolution multi-fichiers : import effectif,
 * visibilité privée par défaut, codegen sans collision entre modules, imports
 * transitifs. Harness filesystem partagé {@link RamFileSystem} (cf. TestIncludeCache).
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique.
 */
@Isolated
public class TestPackages {

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

	@Test
	public void importPackageApi() throws Exception {
		write("los/package.leek", "package los\nexport { visible } from \"./core\"");
		write("los/core.leek", "export function visible(integer n) -> integer { return n + 1 }\nfunction helper() { return 0 }");
		assertEquals("4", run(writeMain("depends los from \"los\"\nimport { visible } from \"@los\"\nreturn visible(3)")));
	}

	@Test
	public void packageDependsOnPackage() throws Exception {
		write("astar/package.leek", "package astar\nexport function twice(integer n) -> integer { return n * 2 }");
		write("los/package.leek", "package los\ndepends astar from \"../astar\"\nimport { twice } from \"@astar\"\nexport function f(integer n) -> integer { return twice(n) + 1 }");
		assertEquals("7", run(writeMain("depends los from \"los\"\nimport { f } from \"@los\"\nreturn f(3)")));
	}

	@Test
	public void packageNamespaceImport() throws Exception {
		write("geo/package.leek", "package geo\nexport function dist(integer a, integer b) -> integer { return b - a }");
		assertEquals("5", run(writeMain("depends geo from \"geo\"\nimport * as Geo from \"@geo\"\nreturn Geo.dist(2, 7)")));
	}

	@Test
	public void undeclaredPackage() throws Exception {
		write("los/package.leek", "package los\nexport function v() -> integer { return 1 }");
		assertTrue(errors(writeMain("import { v } from \"@los\"\nreturn v()")).contains(Error.PACKAGE_NOT_DECLARED));
	}

	@Test
	public void relativePathIntoAnotherPackage() throws Exception {
		write("los/package.leek", "package los\nexport { v } from \"./core\"");
		write("los/core.leek", "export function v() -> integer { return 1 }");
		assertTrue(errors(writeMain("import { v } from \"los/core\"\nreturn v()")).contains(Error.PACKAGE_PATH_CROSSES_BOUNDARY));
	}

	@Test
	public void deepImportIntoPackage() throws Exception {
		write("los/package.leek", "package los\nexport { v } from \"./core\"");
		write("los/core.leek", "export function v() -> integer { return 1 }");
		assertTrue(errors(writeMain("depends los from \"los\"\nimport { v } from \"@los/core\"\nreturn v()")).contains(Error.PACKAGE_PATH_CROSSES_BOUNDARY));
	}

	@Test
	public void packageCannotReachProgramFiles() throws Exception {
		write("shared.leek", "export function s() { return 1 }");
		write("los/package.leek", "package los\nimport { s } from \"../shared\"\nexport function v() -> integer { return 1 }");
		assertTrue(errors(writeMain("depends los from \"los\"\nimport { v } from \"@los\"\nreturn v()")).contains(Error.PACKAGE_PATH_CROSSES_BOUNDARY));
	}

	@Test
	public void cycleBetweenPackages() throws Exception {
		write("pa/package.leek", "package pa\ndepends pb from \"../pb\"\nexport function a() -> integer { return 1 }");
		write("pb/package.leek", "package pb\ndepends pa from \"../pa\"\nexport function b() -> integer { return 2 }");
		assertTrue(errors(writeMain("depends pa from \"pa\"\nimport { a } from \"@pa\"\nreturn a()")).contains(Error.PACKAGE_CYCLE));
	}

	@Test
	public void packageNameMismatch() throws Exception {
		write("los/package.leek", "package los\nexport function v() -> integer { return 1 }");
		assertTrue(errors(writeMain("depends sight from \"los\"\nimport { v } from \"@sight\"\nreturn v()")).contains(Error.PACKAGE_NAME_MISMATCH));
	}

	@Test
	public void packageNotFound() throws Exception {
		assertTrue(errors(writeMain("depends nope from \"nope\"\nreturn 1")).contains(Error.PACKAGE_NOT_FOUND));
	}

	@Test
	public void packageLineOutsideRoot() throws Exception {
		assertTrue(errors(writeMain("package los\nreturn 1")).contains(Error.PACKAGE_HEADER_MISPLACED));
	}

	@Test
	public void dependsAfterAnInstruction() throws Exception {
		write("los/package.leek", "package los\nexport function v() -> integer { return 1 }");
		assertTrue(errors(writeMain("var x = 1\ndepends los from \"los\"\nreturn x")).contains(Error.PACKAGE_HEADER_MISPLACED));
	}

	@Test
	public void dependsIsStillAnIdentifier() throws Exception {
		assertEquals("3", run(writeMain("var depends = 3\nreturn depends")));
	}

	@Test
	public void untypedParameterInApi() throws Exception {
		write("los/package.leek", "package los\nexport function v(n) -> integer { return n }");
		assertTrue(errors(writeMain("depends los from \"los\"\nimport { v } from \"@los\"\nreturn v(1)")).contains(Error.PACKAGE_EXPORT_UNTYPED));
	}

	@Test
	public void missingReturnTypeInApi() throws Exception {
		write("los/package.leek", "package los\nexport function v(integer n) { return n }");
		assertTrue(errors(writeMain("depends los from \"los\"\nimport { v } from \"@los\"\nreturn v(1)")).contains(Error.PACKAGE_EXPORT_UNTYPED));
	}

	@Test
	public void untypedReexportedFunction() throws Exception {
		write("los/package.leek", "package los\nexport { v } from \"./core\"");
		write("los/core.leek", "export function v(n) { return n }");
		assertTrue(errors(writeMain("depends los from \"los\"\nimport { v } from \"@los\"\nreturn v(1)")).contains(Error.PACKAGE_EXPORT_UNTYPED));
	}

	@Test
	public void untypedClassMembersInApi() throws Exception {
		write("geo/package.leek", "package geo\nexport class Cell { public x = 0\n integer get() { return 1 } }");
		assertTrue(errors(writeMain("depends geo from \"geo\"\nimport { Cell } from \"@geo\"\nreturn new Cell().get()")).contains(Error.PACKAGE_EXPORT_UNTYPED));
	}

	@Test
	public void untypedExportedVarInApi() throws Exception {
		write("geo/package.leek", "package geo\nexport var size = 3");
		assertTrue(errors(writeMain("depends geo from \"geo\"\nimport { size } from \"@geo\"\nreturn size")).contains(Error.PACKAGE_EXPORT_UNTYPED));
	}

	@Test
	public void fullyTypedApiAndUntypedInternals() throws Exception {
		// Seule l'API est contrainte : les modules internes et les membres privés restent libres
		write("geo/package.leek", "package geo\nexport { Cell, dist } from \"./cell\"\nexport integer size = 3");
		write("geo/cell.leek", "export class Cell { public integer x = 0\n private y = 1\n constructor(integer x) { this.x = x }\n integer get() { return this.x + helper(this.y) } }\nfunction helper(n) { return n }\nexport function dist(Cell a, Cell b) -> integer { return b.x - a.x }");
		assertEquals("[4, 3]", run(writeMain("depends geo from \"geo\"\nimport { Cell, dist, size } from \"@geo\"\nreturn [dist(new Cell(1), new Cell(5)), size]")));
	}

	@Test
	public void genericApiIsTyped() throws Exception {
		write("box/package.leek", "package box\nexport function first<T>(Array<T> a) -> T { return a[0] }");
		assertEquals("7", run(writeMain("depends box from \"box\"\nimport { first } from \"@box\"\nreturn first([7, 8])")));
	}

	private List<Error> errors(String main) throws Exception {
		return compileAndCollectErrors(main);
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

	/** Opérations consommées par un tour de l'IA. */
	private long operations(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		AI ai = JavaCompiler.compile(file, new Options(LeekScript.LATEST_VERSION, false, true, true, null, true));
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		ai.runIA();
		return ai.getOperations();
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
