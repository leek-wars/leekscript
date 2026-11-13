package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import leekscript.testing.TestCase;
import leekscript.testing.TestResult;
import leekscript.testing.TestResult.Status;
import leekscript.testing.TestRunner;

/**
 * Framework de test (v5) : un module de test `*.test.leek` porte des fonctions `@test`, lancées une
 * par une sur une IA neuve par {@link TestRunner}. Asserts natifs, `@skip`, `@fails`, `@before`,
 * `@after`. Un module de test n'est importé que par un module de test.
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique.
 */
@Isolated
public class TestTestFramework {

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

	// ----- découverte -----

	@Test
	public void tableListsTestsInFileOrder() throws Exception {
		var name = module("@before function prepare() {}\n@test function second() { assert(true) }\n@test @skip(\"plus tard\") function third() { assert(true) }\n@after function clean() {}\nfunction helper() { return 1 }");
		var table = TestCase.table(compile(name));
		assertEquals(List.of("prepare", "second", "third", "clean"), table.stream().map(TestCase::name).toList());
		assertEquals(List.of(TestCase.Kind.BEFORE, TestCase.Kind.TEST, TestCase.Kind.TEST, TestCase.Kind.AFTER), table.stream().map(TestCase::kind).toList());
		assertEquals(2, table.get(1).line());
		assertEquals("plus tard", table.get(2).skip());
	}

	@Test
	public void optionsOfTestAreKept() throws Exception {
		var table = TestCase.table(compile(module("@test(ops: 50_000_000, scenario: \"plateaux/couloir.json\") function big() { assert(true) }")));
		assertEquals(50_000_000L, table.get(0).maxOperations(1));
		assertEquals("plateaux/couloir.json", table.get(0).options().get("scenario"));
	}

	@Test
	public void numericOptionsReadLikeCodeLiterals() throws Exception {
		var table = TestCase.table(compile(module("@test(ops: 5e7) function real() { assert(true) }\n@test(ops: 0x1000) function hex() { assert(true) }")));
		assertEquals(50_000_000L, table.get(0).maxOperations(1));
		assertEquals(4096L, table.get(1).maxOperations(1));
	}

	@Test
	public void argumentsOnAnAnnotationThatTakesNone() throws Exception {
		assertTrue(errors(module("@unused(42) function f() { return 1 }\n@test function t() { assert(true) }")).contains(Error.ANNOTATION_ARGUMENT_INVALID));
	}

	@Test
	public void ordinaryProgramHasNoTable() throws Exception {
		write("plain_" + uniqueId + ".leek", "function f() { return 1 }\nreturn f()");
		assertTrue(TestCase.table(compile("plain_" + uniqueId)).isEmpty());
	}

	@Test
	public void staticMethodsAreTestsToo() throws Exception {
		var results = run(module("class TestTas {\n\tstatic integer base = 0\n\t@before static void prepare() { base = 40 }\n"
			+ "\t@test static void sum() { assertEquals(42, base + 2) }\n\t@test @skip(\"plus tard\") static void later() { fail() }\n\tstatic integer helper() { return 1 }\n}\n"
			+ "@test function free() { assertEquals(40, TestTas.base + 0) }"));
		assertEquals(List.of("TestTas.sum", "TestTas.later", "free"), results.stream().map(TestResult::name).toList());
		assertEquals(List.of(Status.PASSED, Status.SKIPPED, Status.PASSED), results.stream().map(TestResult::status).toList());
	}

	@Test
	public void testAnnotationNeedsAStaticMethod() throws Exception {
		assertTrue(errors(module("class A {\n\t@test void t() { assert(true) }\n}")).contains(Error.ANNOTATION_INVALID_CONTEXT));
		assertTrue(errors(module("class A {\n\t@test static void t(integer x) { assert(x > 0) }\n}")).contains(Error.TEST_FUNCTION_WITH_PARAMETERS));
	}

	// ----- verdicts -----

	@Test
	public void passingTest() throws Exception {
		var r = only("@test function sum() { assertEquals(4, 2 + 2) assert(1 < 2, \"ordre\") }");
		assertEquals(Status.PASSED, r.status());
		assertEquals(2, r.asserts());
		assertTrue(r.operations() > 0);
	}

	@Test
	public void failingAssertGivesLineAndValues() throws Exception {
		var r = only("@test function sum() {\n\tassertEquals(5, 2 + 2, \"somme\")\n\tassert(true)\n}");
		assertEquals(Status.FAILED, r.status());
		assertEquals(2, r.asserts(), "un assert raté n'arrête pas le test");
		assertEquals(1, r.failures().size());
		var failure = r.failures().get(0);
		assertEquals(2, failure.line());
		assertEquals("somme", failure.message());
		assertEquals("5", failure.expected());
		assertEquals("4", failure.actual());
	}

	@Test
	public void assertReturnsItsVerdict() throws Exception {
		var r = only("@test function t() { if (!assert(false)) { return null } fail(\"pas atteint\") }");
		assertEquals(1, r.asserts());
	}

	@Test
	public void testWithoutAssertFails() throws Exception {
		var r = only("@test function nothing() { var x = 1 }");
		assertEquals(Status.FAILED, r.status());
		assertEquals("no_assert", r.reason());
	}

	@Test
	public void skippedTestIsNotRun() throws Exception {
		var r = only("@test @skip(\"abandonné\") function t() { fail() }");
		assertEquals(Status.SKIPPED, r.status());
		assertEquals("abandonné", r.reason());
		assertEquals(0, r.asserts());
	}

	@Test
	public void expectedFailure() throws Exception {
		var r = only("@test @fails(\"bug ouvert\") function t() { assertEquals(1, 2) }");
		assertEquals(Status.EXPECTED_FAILURE, r.status());
		assertEquals("bug ouvert", r.reason());
		assertTrue(r.status().ok);
	}

	@Test
	public void unexpectedPass() throws Exception {
		var r = only("@test @fails(\"bug ouvert\") function t() { assertEquals(1, 1) }");
		assertEquals(Status.UNEXPECTED_PASS, r.status());
		assertFalse(r.status().ok);
	}

	@Test
	public void failsDoesNotExcuseATestThatChecksNothing() throws Exception {
		var r = only("@test @fails(\"bug ouvert\") function t() { var x = 1 }");
		assertEquals(Status.FAILED, r.status());
		assertEquals("no_assert", r.reason());
	}

	@Test
	public void failsDoesNotExcuseABrokenSetup() throws Exception {
		var r = only("function deep(integer n) -> integer { return deep(n + 1) + 1 }\n@before function prepare() { deep(0) }\n@test @fails(\"bug ouvert\") function t() { assertEquals(1, 2) }");
		assertEquals(Status.ERROR, r.status());
	}

	@Test
	public void failsDoesNotExcuseAnOperationOverrun() throws Exception {
		var r = only("@test(ops: 5000) @fails(\"bug ouvert\") function t() { var i = 0\nwhile (true) { i++ } }");
		assertEquals(Status.ERROR, r.status());
	}

	@Test
	public void failsAcceptsATestThatCrashes() throws Exception {
		var r = only("function deep(integer n) -> integer { return deep(n + 1) + 1 }\n@test @fails(\"bug ouvert\") function t() { deep(0) }");
		assertEquals(Status.EXPECTED_FAILURE, r.status());
	}

	@Test
	public void afterRunsEvenWhenTheTestCrashes() throws Exception {
		var r = only("function deep(integer n) -> integer { return deep(n + 1) + 1 }\n@after function clean() { assert(false, \"after a tourné\") }\n@test function t() { deep(0) }");
		assertEquals(Status.ERROR, r.status());
		assertEquals(1, r.failures().size(), "l'assert du @after est enregistré");
	}

	// ----- asserts -----

	@Test
	public void equalityBetweenNumbersOnly() throws Exception {
		// Les huit cas du harnais d'origine de l'IA pilote, plus la comparaison en profondeur
		var r = only("@test function t() {\n"
			+ "assertEquals(1, 1.0)\n"                  // 2
			+ "assertEquals(0.3, 0.1 + 0.2)\n"          // 3
			+ "assertEquals(null, null)\n"              // 4
			+ "assertEquals([1, [2, 3]], [1, [2, 3]])\n" // 5
			+ "assertEquals([0.3], [0.1 + 0.2])\n"      // 6
			+ "assertEquals([\"a\": 1], [\"a\": 1])\n"  // 7
			+ "assertEquals(0, null)\n"                 // 8
			+ "assertEquals(0.0, null)\n"               // 9
			+ "assertEquals(1, true)\n"                 // 10
			+ "assertEquals(0, false)\n"                // 11
			+ "assertEquals(3, [1, 2, 3])\n"            // 12
			+ "assertEquals([1, 2], [1, 2, 3])\n"       // 13
			+ "}");
		assertEquals(12, r.asserts());
		assertEquals(List.of(8, 9, 10, 11, 12, 13), r.failures().stream().map(f -> f.line()).toList());
	}

	@Test
	public void assertNear() throws Exception {
		var r = only("@test function t() {\nassertNear(10, 10.4, 0.5)\nassertNear(10, 11, 0.5)\nassertNear(10, null, 0.5)\n}");
		assertEquals(List.of(3, 4), r.failures().stream().map(f -> f.line()).toList());
	}

	@Test
	public void loggedErrorMakesTheTestAnError() throws Exception {
		// Lecture hors bornes : le moteur journalise et continue
		var r = only("@test function t() {\nvar a = [1, 2]\nvar x = a[5]\nassert(true)\n}");
		assertEquals(Status.ERROR, r.status());
		assertEquals(1, r.asserts(), "le code a continué");
		assertEquals(1, r.loggedErrors().size());
		assertEquals(3, r.loggedErrors().get(0).line());
	}

	@Test
	public void assertErrorCatchesALoggedError() throws Exception {
		var r = only("@test function t() {\nvar a = [1, 2]\nassertError(function() { return a[5] })\n}");
		assertEquals(Status.PASSED, r.status());
		assertTrue(r.loggedErrors().isEmpty());
	}

	@Test
	public void assertErrorFailsWithoutError() throws Exception {
		var r = only("@test function t() {\nassertError(function() { return 1 })\n}");
		assertEquals(Status.FAILED, r.status());
		assertEquals("no error", r.failures().get(0).actual());
	}

	@Test
	public void runtimeErrorStopsOnlyItsTest() throws Exception {
		var results = run(module("function deep(integer n) -> integer { return deep(n + 1) + 1 }\n@test function first() { deep(0)\nassert(true) }\n@test function second() { assert(true) }"));
		assertEquals(Status.ERROR, results.get(0).status());
		assertNotNull(results.get(0).error());
		assertEquals(Status.PASSED, results.get(1).status());
	}

	@Test
	public void operationLimitIsPerTest() throws Exception {
		var results = run(module("@test(ops: 5000) function loop() { var i = 0\nwhile (true) { i++ }\nassert(true) }\n@test function after() { assert(true) }"));
		assertEquals(Status.ERROR, results.get(0).status());
		assertTrue(results.get(0).error().startsWith("TOO_MUCH_OPERATIONS"), results.get(0).error());
		assertEquals(Status.PASSED, results.get(1).status());
	}

	@Test
	public void assertErrorDoesNotInvalidateTheBudget() throws Exception {
		// Une boucle sans fin dans assertError n'est pas « l'erreur attendue » : le test s'arrête
		var r = only("@test(ops: 5000) function t() { assertError(function() { var i = 0\nwhile (true) { i++ } }) }");
		assertEquals(Status.ERROR, r.status());
		assertTrue(r.error().startsWith("TOO_MUCH_OPERATIONS"), r.error());
	}

	@Test
	public void deepComparisonCostsOperations() throws Exception {
		var small = only("@test function t() { assertEquals([1], [1]) }").operations();
		var big = only("@test function t() { var a = [] var b = []\nfor (var i = 0; i < 500; ++i) { Array.push(a, i) Array.push(b, i) }\nvar o = getOperations()\nassertEquals(a, b)\nassert(getOperations() - o >= 500) }");
		assertEquals(Status.PASSED, big.status());
		assertTrue(big.operations() > small);
	}

	@Test
	public void aStructureThatContainsItselfDoesNotLoop() throws Exception {
		var r = only("@test function t() { var a = [1]\nArray.push(a, a)\nvar b = [1]\nArray.push(b, b)\nassertEquals(a, a)\nassertEquals(a, b) }");
		assertEquals(Status.FAILED, r.status(), String.valueOf(r.error()));
		assertEquals(List.of(6), r.failures().stream().map(f -> f.line()).toList());
	}

	@Test
	public void testAnnotationsStayUnknownBeforeV5() throws Exception {
		// Une IA v4 qui portait déjà un `@test` garde son avertissement, sans erreur
		write("old_" + uniqueId + ".leek", "// @version:4\n@test function t() { return 1 }\nreturn t()");
		var errors = errors("old_" + uniqueId);
		assertTrue(errors.contains(Error.ANNOTATION_UNKNOWN), errors.toString());
		assertFalse(errors.contains(Error.TEST_ANNOTATION_OUTSIDE_TEST_MODULE));
	}

	@Test
	public void malformedAnnotationArgumentsKeepTheDeclaration() throws Exception {
		var errors = errors(module("@test(ops: ) function t() { assert(true) }\n@test function u() { assert(true) }"));
		// Une seule erreur (relevée à chaque passe) : la déclaration qui suit n'est pas emportée
		assertEquals(List.of(Error.ANNOTATION_ARGUMENT_INVALID), errors.stream().distinct().toList());
	}

	// ----- isolation -----

	@Test
	public void eachTestStartsFromAFreshProgram() throws Exception {
		var results = run(module("global counter = 0\nclass Box { static integer value = 0 }\n"
			+ "@test function one() { counter++ Box.value++ assertEquals(1, counter) assertEquals(1, Box.value) }\n"
			+ "@test function two() { counter++ Box.value++ assertEquals(1, counter) assertEquals(1, Box.value) }"));
		assertEquals(List.of(Status.PASSED, Status.PASSED), results.stream().map(TestResult::status).toList());
	}

	@Test
	public void beforeAndAfterRunAroundEachTest() throws Exception {
		var results = run(module("global trace = []\n@before function prepare() { Array.push(trace, \"before\") }\n@after function clean() { assertEquals([\"before\", \"test\"], trace) }\n"
			+ "@test function one() { Array.push(trace, \"test\") assertEquals([\"before\", \"test\"], trace) }"));
		assertEquals(Status.PASSED, results.get(0).status());
		assertEquals(2, results.get(0).asserts());
	}

	// ----- un module de test dans son package -----

	@Test
	public void testModuleReadsPackageInternals() throws Exception {
		write("heap/package.leek", "package heap\nexport { top } from \"./core\"");
		write("heap/core.leek", "export function top(Array<integer> a) -> integer { return hidden(a) }\nexport function hidden(Array<integer> a) -> integer { return a[0] }");
		// `hidden` n'est pas dans l'API du package : le module de test, qui en fait partie, le lit
		write("heap/test/core.test.leek", "import { hidden } from \"../core\"\n@test function reads_internal() { assertEquals(7, hidden([7, 1])) }");
		assertEquals(Status.PASSED, run("heap/test/core.test").get(0).status());
	}

	// ----- règles de compilation -----

	@Test
	public void ordinaryModuleCannotImportATestModule() throws Exception {
		write("helpers.test.leek", "export function h() { return 1 }");
		assertTrue(errors(writeMain("import { h } from \"helpers.test\"\nreturn h()")).contains(Error.TEST_MODULE_IMPORTED));
	}

	@Test
	public void testModuleImportsAnotherTestModule() throws Exception {
		write("helpers.test.leek", "export function h() { return 1 }");
		assertEquals(Status.PASSED, only("import { h } from \"helpers.test\"\n@test function t() { assertEquals(1, h()) }").status());
	}

	@Test
	public void testAnnotationOutsideATestModule() throws Exception {
		assertTrue(errors(writeMain("@test function t() { assert(true) }\nreturn 1")).contains(Error.TEST_ANNOTATION_OUTSIDE_TEST_MODULE));
	}

	@Test
	public void testFunctionTakesNoParameter() throws Exception {
		assertTrue(errors(module("@test function t(integer x) { assert(x > 0) }")).contains(Error.TEST_FUNCTION_WITH_PARAMETERS));
	}

	@Test
	public void skipNeedsAReason() throws Exception {
		assertTrue(errors(module("@test @skip function t() { assert(true) }")).contains(Error.ANNOTATION_ARGUMENT_INVALID));
		assertTrue(errors(module("@test @skip(\"\") function t() { assert(true) }")).contains(Error.ANNOTATION_ARGUMENT_INVALID));
	}

	@Test
	public void skipWithoutTest() throws Exception {
		assertTrue(errors(module("@skip(\"raison\") function t() { assert(true) }")).contains(Error.ANNOTATION_INVALID_CONTEXT));
	}

	@Test
	public void strictModeDoesNotCallATestUnused() throws Exception {
		assertFalse(errors(module("// @strict\n@test function t() { assert(true) }")).contains(Error.UNUSED_FUNCTION));
	}

	@Test
	public void testNamedLikeALoopVariableOfAnImportedModule() throws Exception {
		// Un test se nomme librement : son nom ne doit pas gêner l'itérateur d'un module importé
		write("lib.leek", "export function total(Array<integer> a) -> integer { var s = 0\nfor (var item in a) { s += item }\nfor (var k : var item in a) { s += k * 0 }\nreturn s }");
		assertEquals(Status.PASSED, only("import { total } from \"lib\"\n@test function item() { assertEquals(3, total([1, 2])) }").status());
	}

	@Test
	public void testNamedLikeAClassFieldOfAnImportedModule() throws Exception {
		// `cache` nu dans une méthode statique d'un module importé désigne le champ de sa classe
		write("store.leek", "export class Store { static Map<string, integer> cache = [:]\nstatic integer put(string k) { cache[k] = 1\nreturn Map.size(cache) } }");
		assertEquals(Status.PASSED, only("import { Store } from \"store\"\n@test function cache() { assertEquals(1, Store.put(\"a\")) }").status());
	}

	// =========================================================================
	// Aides
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

	/** Écrit un module de test et rend son nom. */
	private String module(String content) throws IOException {
		String name = "suite_" + uniqueId + "_" + UNIQUE.incrementAndGet() + ".test";
		write(name + ".leek", content);
		return name;
	}

	private static Options options() {
		return new Options(LeekScript.MAX_VERSION, false, true, true, null, true);
	}

	private leekscript.compiler.AIFile file(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		return file;
	}

	private leekscript.runner.AI compile(String name) throws Exception {
		return JavaCompiler.compile(file(name), options());
	}

	private List<TestResult> run(String name) throws Exception {
		return TestRunner.runModule(file(name), options());
	}

	/** Le résultat de l'unique test d'un module. */
	private TestResult only(String content) throws Exception {
		var results = run(module(content));
		assertEquals(1, results.size());
		return results.get(0);
	}

	private List<Error> errors(String name) throws Exception {
		var file = file(name);
		var errors = new ArrayList<Error>();
		try {
			JavaCompiler.compile(file, options());
		} catch (LeekCompilerException e) {
			errors.add(e.getError());
		} catch (Exception e) {
			// erreurs d'analyse : relevées sur le fichier ci-dessous
		}
		for (var e : file.getErrors()) errors.add(e.error);
		return errors;
	}
}
