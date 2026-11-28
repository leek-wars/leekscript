package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import leekscript.compiler.AIFile;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.Package;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.NativeFileSystem;
import leekscript.runner.AI;
import leekscript.testing.TestResult;
import leekscript.testing.TestResult.Status;
import leekscript.testing.TestRunner;

/**
 * Dépendances de test d'un package (`depends test <alias> from "<dossier>"`) : seuls les modules
 * du dossier `test/` du package les importent, et elles restent hors du graphe de prod. Cas type :
 * un package de fabriques `fixtures` qui dépend de `scoring`, dont les tests dépendent de `fixtures`.
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique.
 */
@Isolated
public class TestPackageTestDepends {

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

	/**
	 * `simulation` ← `scoring` ← `fixtures` (prod), et `scoring` dépend de `fixtures` pour ses
	 * tests. Le corps de `score` porte l'identifiant du test : chaque test a son propre Java, donc
	 * sa propre entrée du cache des packages.
	 */
	private void writePackages() throws IOException {
		writePackages("");
	}

	/** `scoringExtra` : lignes de plus à la fin du package.leek de `scoring` (un fichier ne se réécrit pas : le cache des fichiers suit la date). */
	private void writePackages(String scoringExtra) throws IOException {
		write("simulation/package.leek", "package simulation\nexport function damage(integer power) -> integer { return power * 2 }");
		write("scoring/package.leek", "package scoring\ndepends simulation from \"../simulation\"\ndepends test fixtures from \"../fixtures\"\nimport { damage } from \"@simulation\"\nexport function score(integer power) -> integer { tag() return damage(power) + 1 }\nfunction tag() { return \"" + uniqueId + "\" }" + scoringExtra);
		write("fixtures/package.leek", "package fixtures\ndepends simulation from \"../simulation\"\ndepends scoring from \"../scoring\"\nimport { damage } from \"@simulation\"\nimport { score } from \"@scoring\"\nexport function strongHit() -> integer { return damage(10) }\nexport function expectedScore() -> integer { return score(10) }");
	}

	// ----- syntaxe -----

	@Test
	public void headerLinesAreRead() throws Exception {
		writePackages();
		var root = file("scoring/package");
		var scoring = Package.read(root.getFolder(), root);
		assertEquals("scoring", scoring.getName().getWord());
		assertEquals(List.of("simulation"), new ArrayList<>(scoring.getDepends().keySet()));
		assertEquals(List.of("fixtures"), new ArrayList<>(scoring.getTestDepends().keySet()));
		assertEquals("../fixtures", scoring.getTestDepends().get("fixtures").path());
	}

	@Test
	public void aliasNamedTestIsAnOrdinaryDependency() throws Exception {
		// `test` n'est un mot de l'en-tête que suivi d'un alias
		write("test/package.leek", "package test\nexport function one() -> integer { return 1 }");
		var main = writeMain("depends test from \"test\"\nimport { one } from \"@test\"\nreturn one()");
		var entry = file(main);
		var program = Package.read(null, entry);
		assertEquals(List.of("test"), new ArrayList<>(program.getDepends().keySet()));
		assertTrue(program.getTestDepends().isEmpty());
		assertEquals("1", run(main));
	}

	@Test
	public void testIsStillAnIdentifier() throws Exception {
		assertEquals("4", run(writeMain("var test = 4\nreturn test")));
	}

	@Test
	public void aliasDeclaredBothWaysIsAProdDependency() throws Exception {
		write("pkg/package.leek", "package pkg\ndepends test geo from \"../geo\"\ndepends geo from \"../geo\"");
		var root = file("pkg/package");
		var pkg = Package.read(root.getFolder(), root);
		assertEquals(List.of("geo"), new ArrayList<>(pkg.getDepends().keySet()));
		assertTrue(pkg.getTestDepends().isEmpty());
	}

	@Test
	public void testDependsInAProgramEntry() throws Exception {
		// Le package du programme n'a pas de dossier `test/` : `depends test` n'y a pas de sens
		writePackages();
		var main = writeMain("depends test fixtures from \"fixtures\"\nimport { strongHit } from \"@fixtures\"\nreturn strongHit()");
		var errors = errors(main);
		assertTrue(errors.contains(Error.PACKAGE_HEADER_MISPLACED), errors.toString());
		// Pas de 183 à l'import, qu'aucun dossier ne pourrait satisfaire : l'alias n'est pas déclaré
		assertFalse(errors.contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS), errors.toString());
		assertTrue(errors.contains(Error.PACKAGE_NOT_DECLARED), errors.toString());
		var program = Package.read(null, file(main));
		assertTrue(program.getTestDepends().isEmpty());
	}

	@Test
	public void testDependsAfterAnInstruction() throws Exception {
		write("pkg/package.leek", "package pkg\nexport function v() -> integer { return 1 }\ndepends test geo from \"../geo\"");
		assertTrue(errors(writeMain("depends pkg from \"pkg\"\nimport { v } from \"@pkg\"\nreturn v()")).contains(Error.PACKAGE_HEADER_MISPLACED));
	}

	// ----- portée : le dossier test/ du package -----

	@Test
	public void testModuleImportsATestDependency() throws Exception {
		writePackages();
		write("scoring/test/score.test.leek", "import { score } from \"../package\"\nimport { strongHit, expectedScore } from \"@fixtures\"\n@test function scores_a_strong_hit() { assertEquals(expectedScore(), score(10))\nassertEquals(20, strongHit()) }");
		assertPassed(runTests("scoring/test/score.test"));
	}

	@Test
	public void ordinaryModuleOfTheTestFolderImportsATestDependency() throws Exception {
		// Tout le dossier test/, sous-dossiers compris, et pas seulement les `*.test.leek`
		writePackages();
		write("scoring/test/helpers/hits.leek", "import { strongHit } from \"@fixtures\"\nexport function twoHits() -> integer { return 2 * strongHit() }");
		write("scoring/test/hits.test.leek", "import { twoHits } from \"./helpers/hits\"\n@test function two_hits() { assertEquals(40, twoHits()) }");
		assertPassed(runTests("scoring/test/hits.test"));
	}

	@Test
	public void prodModuleCannotImportATestDependency() throws Exception {
		// Pas même quand on compile les tests du package
		writePackages();
		write("scoring/extra.leek", "import { strongHit } from \"@fixtures\"\nexport function extra() -> integer { return strongHit() }");
		write("scoring/test/extra.test.leek", "import { extra } from \"../extra\"\n@test function t() { assertEquals(20, extra()) }");
		var errors = testErrors("scoring/test/extra.test");
		// Un code à lui : la dépendance est déclarée, mais pour les tests (PACKAGE_NOT_DECLARED
		// conseillerait un `depends` ordinaire, qui la ferait passer en prod)
		assertTrue(errors.contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS), errors.toString());
		assertFalse(errors.contains(Error.PACKAGE_NOT_DECLARED));
	}

	@Test
	public void prodModuleCannotImportATestDependencyInAFightAI() throws Exception {
		writePackages("\nexport { extra } from \"./extra\"");
		write("scoring/extra.leek", "import { strongHit } from \"@fixtures\"\nexport function extra() -> integer { return strongHit() }");
		assertTrue(errors(writeMain("depends scoring from \"scoring\"\nimport { extra } from \"@scoring\"\nreturn extra()")).contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
	}

	@Test
	public void testFolderOfAnotherPackageDoesNotCount() throws Exception {
		// Un dossier `test` qui n'est pas directement sous le package : rien de spécial
		writePackages();
		write("scoring/lib/test/deep.leek", "import { strongHit } from \"@fixtures\"\nexport function deep() -> integer { return strongHit() }");
		write("scoring/test/deep.test.leek", "import { deep } from \"../lib/test/deep\"\n@test function t() { assertEquals(20, deep()) }");
		assertTrue(testErrors("scoring/test/deep.test").contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
	}

	@Test
	public void prodModuleImportingATestFolderModuleFromAFightAI() throws Exception {
		// Un module de prod qui importe un module ordinaire du dossier test/ : l'IA de combat ne
		// compile pas les tests du package, la dépendance de test reste invisible
		writePackages("\nexport { helper } from \"./test/helper\"");
		write("scoring/test/helper.leek", "import { strongHit } from \"@fixtures\"\nexport function helper() -> integer { return strongHit() }");
		assertTrue(errors(writeMain("depends scoring from \"scoring\"\nimport { helper } from \"@scoring\"\nreturn helper()")).contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
	}

	@Test
	public void testsOfAnotherPackageDoNotSeeTheTestDependencies() throws Exception {
		// Les tests de `fixtures` ne compilent pas les tests de `scoring` : ses `depends test` ne comptent pas
		writePackages();
		write("fixtures/test/hit.test.leek", "import { strongHit } from \"../package\"\n@test function t() { assertEquals(20, strongHit()) }");
		assertPassed(runTests("fixtures/test/hit.test"));
	}

	// ----- hors du graphe de prod -----

	@Test
	public void fightAIIgnoresTheTestDependencyCycle() throws Exception {
		// fixtures → scoring → (test) fixtures : pas un cycle pour l'IA de combat
		writePackages();
		var main = writeMain("depends scoring from \"scoring\"\nimport { score } from \"@scoring\"\nreturn score(3)");
		assertEquals(List.of(), errors(main));
		assertEquals("7", run(main));
	}

	@Test
	public void fightAIIgnoresAMissingTestDependency() throws Exception {
		write("scoring/package.leek", "package scoring\ndepends test fixtures from \"../nowhere\"\ndepends test other from \"../simulation\"\nexport function score(integer power) -> integer { return power + 1 }");
		write("simulation/package.leek", "package simulation\nexport function damage(integer power) -> integer { return power * 2 }");
		var main = writeMain("depends scoring from \"scoring\"\nimport { score } from \"@scoring\"\nreturn score(3)");
		assertEquals(List.of(), errors(main));
		assertEquals("4", run(main));
	}

	@Test
	public void testFolderEntryCompiledAsAFightAI() throws Exception {
		// Une entrée du dossier test/ choisie comme IA de combat : pas de mode test, pas de fixtures
		writePackages();
		write("scoring/test/helper.leek", "import { strongHit } from \"@fixtures\"\nreturn strongHit()");
		write("scoring/test/score.test.leek", "import { strongHit } from \"@fixtures\"\n@test function t() { assertEquals(20, strongHit()) }");
		assertEquals(List.of(), testErrors("scoring/test/helper"));
		assertTrue(errors("scoring/test/helper").contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
		assertTrue(errors("scoring/test/score.test").contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
	}

	@Test
	public void testModeDoesNotLeakIntoTheFightCache() throws Exception {
		// Compilé d'abord pour ses tests (cache disque et mémoire) : l'IA de combat ne le recharge pas
		writePackages();
		write("scoring/test/score.test.leek", "import { strongHit } from \"@fixtures\"\n@test function t() { assertEquals(20, strongHit()) }");
		assertPassed(runTests("scoring/test/score.test"));
		assertTrue(errors("scoring/test/score.test").contains(Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
		assertPassed(runTests("scoring/test/score.test"));
	}

	@Test
	public void editorAnalysisOfATestModule() throws Exception {
		// L'analyse (éditeur) ne fait pas d'IA de combat : elle lit un module de test comme ses tests.
		// Elle part de la dernière version publique : la v5 se déclare.
		header = "// @version:" + LeekScript.MAX_VERSION + "\n";
		writePackages();
		write("scoring/test/score.test.leek", "import { strongHit } from \"@fixtures\"\n@test function t() { assertEquals(20, strongHit()) }");
		var entry = file("scoring/test/score.test");
		new leekscript.compiler.IACompiler().analyze(entry);
		assertEquals(List.of(), entry.getErrors().stream().map(e -> e.error).toList());
	}

	@Test
	public void testDependencyOnItself() throws Exception {
		// Comme `depends scoring from "."` : un cycle, même par une arête de test
		write("scoring/package.leek", "package scoring\ndepends test scoring from \".\"\nexport function score(integer power) -> integer { return power + 1 }");
		write("scoring/test/score.test.leek", "import { score } from \"../package\"\n@test function t() { assertEquals(2, score(1)) }");
		assertTrue(testErrors("scoring/test/score.test").contains(Error.PACKAGE_CYCLE));
		write("solo/package.leek", "package solo\ndepends solo from \".\"\nexport function one() -> integer { return 1 }");
		assertTrue(errors(writeMain("depends solo from \"solo\"\nimport { one } from \"@solo\"\nreturn one()")).contains(Error.PACKAGE_CYCLE));
	}

	@Test
	public void testDependencyCycleThroughItsOwnEdgeIsAccepted() throws Exception {
		writePackages();
		write("scoring/test/score.test.leek", "import { score } from \"../package\"\nimport { expectedScore } from \"@fixtures\"\n@test function t() { assertEquals(expectedScore(), score(10)) }");
		assertEquals(List.of(), testErrors("scoring/test/score.test"));
		assertPassed(runTests("scoring/test/score.test"));
	}

	@Test
	public void missingTestDependencyOfTheTestedPackage() throws Exception {
		write("scoring/package.leek", "package scoring\ndepends test fixtures from \"../nowhere\"\nexport function score(integer power) -> integer { return power + 1 }");
		write("scoring/test/score.test.leek", "import { score } from \"../package\"\n@test function t() { assertEquals(2, score(1)) }");
		assertTrue(testErrors("scoring/test/score.test").contains(Error.PACKAGE_NOT_FOUND));
	}

	@Test
	public void misnamedTestDependencyOfTheTestedPackage() throws Exception {
		writePackages();
		write("rating/package.leek", "package rating\ndepends test fakes from \"../fixtures\"\nexport function score(integer power) -> integer { return power + 1 }");
		write("rating/test/score.test.leek", "import { score } from \"../package\"\n@test function t() { assertEquals(2, score(1)) }");
		assertTrue(testErrors("rating/test/score.test").contains(Error.PACKAGE_NAME_MISMATCH));
	}

	@Test
	public void cycleInsideTheTestDependencyGraphIsStillACycle() throws Exception {
		// Un vrai cycle de prod atteint par l'arête de test : fixtures → helpers → fixtures
		write("scoring/package.leek", "package scoring\ndepends test fixtures from \"../fixtures\"\nexport function score(integer power) -> integer { return power + 1 }");
		write("fixtures/package.leek", "package fixtures\ndepends helpers from \"../helpers\"\nexport function one() -> integer { return 1 }");
		write("helpers/package.leek", "package helpers\ndepends fixtures from \"../fixtures\"\nexport function two() -> integer { return 2 }");
		write("scoring/test/score.test.leek", "import { one } from \"@fixtures\"\n@test function t() { assertEquals(1, one()) }");
		assertTrue(testErrors("scoring/test/score.test").contains(Error.PACKAGE_CYCLE));
	}

	@Test
	public void prodCycleOfTheTestedPackageIsStillACycle() throws Exception {
		write("scoring/package.leek", "package scoring\ndepends fixtures from \"../fixtures\"\nexport function score(integer power) -> integer { return power + 1 }");
		write("fixtures/package.leek", "package fixtures\ndepends scoring from \"../scoring\"\nexport function one() -> integer { return 1 }");
		write("scoring/test/score.test.leek", "import { score } from \"../package\"\n@test function t() { assertEquals(2, score(1)) }");
		assertTrue(testErrors("scoring/test/score.test").contains(Error.PACKAGE_CYCLE));
	}

	@Test
	public void ordinaryDependsUnchanged() throws Exception {
		// Non-régression : un `depends` ordinaire est vu de partout, tests compris
		writePackages();
		write("scoring/test/sim.test.leek", "import { damage } from \"@simulation\"\n@test function t() { assertEquals(6, damage(3)) }");
		assertPassed(runTests("scoring/test/sim.test"));
		// … et un package qui n'est pas déclaré du tout reste PACKAGE_NOT_DECLARED, même depuis test/
		write("scoring/test/other.test.leek", "import { one } from \"@other\"\n@test function t() { assertEquals(1, one()) }");
		assertTrue(testErrors("scoring/test/other.test").contains(Error.PACKAGE_NOT_DECLARED));
	}

	// ----- cache des packages et graphe des imports -----

	@Test
	public void testsAndFightAIShareTheCache() throws Exception {
		// Avec le cache disque : les tests (groupes scoring et fixtures qui se lisent l'un l'autre)
		// puis l'IA de combat, deux fois chacun
		writePackages();
		write("scoring/test/helper.leek", "import { strongHit } from \"@fixtures\"\nexport function helper() -> integer { return strongHit() }");
		write("scoring/test/score.test.leek", "import { score } from \"../package\"\nimport { helper } from \"./helper\"\nimport { expectedScore } from \"@fixtures\"\n@test function t() { assertEquals(expectedScore(), score(10))\nassertEquals(20, helper()) }");
		var main = writeMain("depends scoring from \"scoring\"\nimport { score } from \"@scoring\"\nreturn score(3)");
		for (int i = 0; i < 2; i++) {
			assertPassed(runTests("scoring/test/score.test"));
			assertEquals("7", run(main));
		}
	}

	@Test
	public void includeGraphFollowsTestDependenciesFromTheTestFolder() throws Exception {
		writePackages();
		write("scoring/test/score.test.leek", "@test function t() { assert(true) }");
		write("scoring/core.leek", "export function c() -> integer { return 1 }");
		var fixturesRoot = Package.resolveDependency(file("scoring/test/score.test"), "fixtures");
		assertNotNull(fixturesRoot);
		assertEquals(file("fixtures/package").getPath(), fixturesRoot.getPath());
		assertNull(Package.resolveDependency(file("scoring/core"), "fixtures"));
		assertNotNull(Package.resolveDependency(file("scoring/core"), "simulation"));
	}

	@Test
	public void nativeFileSystem() throws Exception {
		// Le système de fichiers du générateur (`--test --root=…`) : un dossier y porte son chemin
		// complet pour nom
		var root = tmpRoot.resolve("native");
		tmpRoot = root;
		// Ses fichiers sont à la dernière version publique : la v5 se déclare
		header = "// @version:" + LeekScript.MAX_VERSION + "\n";
		writePackages();
		write("scoring/test/essai-fixtures.test.leek", "import { expectedScore } from \"@fixtures\"\nimport { score } from \"../package.leek\"\n@test function t() { assertEquals(expectedScore(), score(10)) }");
		write("scoring/core.leek", "import { strongHit } from \"@fixtures\"\nexport function c() -> integer { return strongHit() }");
		write("scoring/lib/test/deep.leek", "export function d() -> integer { return 1 }");
		LeekScript.setFileSystem(new NativeFileSystem(root.toString(), 1));
		var disk = LeekScript.getFileSystem().getRoot();
		var entry = disk.resolve("scoring/test/essai-fixtures.test.leek");
		var scoring = Package.nearest(entry.getFolder(), null);
		assertTrue(scoring.isTestFolder(entry.getFolder()));
		assertEquals("test", entry.getFolder().getBaseName());
		assertTrue(scoring.isTestFolder(disk.resolve("scoring/./test/essai-fixtures.test.leek").getFolder()));
		assertFalse(scoring.isTestFolder(disk.resolve("scoring/core.leek").getFolder()));
		assertFalse(scoring.isTestFolder(disk.resolve("scoring/lib/test/deep.leek").getFolder()));
		entry.setJavaClass("AI_" + entry.getId());
		entry.setRootClass("AI");
		assertPassed(TestRunner.runModule(entry, options()));
		assertNull(Package.resolveDependency(disk.resolve("scoring/core.leek"), "fixtures"));
	}

	// =========================================================================
	// Aides
	// =========================================================================

	private static void assertPassed(List<TestResult> results) {
		assertFalse(results.isEmpty());
		for (var result : results) assertEquals(Status.PASSED, result.status(), result.toString());
	}

	// Écrit en tête de chaque fichier
	private String header = "";

	private Path write(String relative, String content) throws IOException {
		var p = tmpRoot.resolve(relative);
		Files.createDirectories(p.getParent());
		Files.writeString(p, header + content);
		return p;
	}

	private String writeMain(String content) throws IOException {
		String name = "Main_" + uniqueId;
		write(name + ".leek", content);
		return name;
	}

	private static Options options() {
		return new Options(LeekScript.MAX_VERSION, false, true, true, null, true);
	}

	private AIFile file(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		return file;
	}

	private String run(String name) throws Exception {
		AI ai = JavaCompiler.compile(file(name), options());
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		return ai.string(ai.runIA());
	}

	private List<TestResult> runTests(String name) throws Exception {
		BackendDiff.compareTests(file(name), "AI");
		return TestRunner.runModule(file(name), options());
	}

	/** Erreurs de la compilation de `name` en IA de combat. */
	private List<Error> errors(String name) throws Exception {
		return errors(name, options());
	}

	/** Erreurs de la compilation de `name` en mode test, celle du lanceur de tests. */
	private List<Error> testErrors(String name) throws Exception {
		return errors(name, options().forTests());
	}

	private List<Error> errors(String name, Options options) throws Exception {
		var file = file(name);
		var errors = new ArrayList<Error>();
		try {
			JavaCompiler.compile(file, options);
		} catch (LeekCompilerException e) {
			errors.add(e.getError());
		} catch (Exception e) {
			// erreurs d'analyse : relevées sur le fichier ci-dessous
		}
		for (var e : file.getErrors()) errors.add(e.error);
		return errors;
	}
}
