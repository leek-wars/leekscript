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

	// #43 T3 : une fonction d'un module IMPORTÉ se monomorphise aussi. Le corps est re-parsé
	// depuis le flux de tokens du module (compilateur enfant), pas le flux principal.
	@Test
	public void monomorphizeImportedFunction() throws Exception {
		write("mathmod.leek", "export function dbl(x) { return x * 2 }\nexport function id(x) { return x }");
		String main = writeMain("import { dbl, id } from \"mathmod\"\nreturn dbl(21) + id(0)");
		assertEquals("42", run(main));
	}

	// Variante d'une fonction importée appelée récursivement (re-parse module + cache récursion).
	@Test
	public void monomorphizeImportedRecursive() throws Exception {
		write("recmod.leek", "export function fact(n) { if (n <= 1) { return 1 } return n * fact(n - 1) }");
		String main = writeMain("import { fact } from \"recmod\"\nreturn fact(5)");
		assertEquals("120", run(main));
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
	// Variables de module (export var / export global)
	// =========================================================================

	@Test
	public void importExportedVar() throws Exception {
		write("state.leek", "export var hp = 100");
		String main = writeMain("import { hp } from \"state\"\nreturn hp");
		assertEquals("100", run(main));
	}

	@Test
	public void importExportedGlobalAndUseInFunction() throws Exception {
		// export global lu par une fonction de l'importateur.
		write("conf.leek", "export global LIMIT = 50");
		String main = writeMain(
			"import { LIMIT } from \"conf\"\n" +
			"function ok(x) { return x < LIMIT }\n" +
			"return ok(10) and !ok(100)");
		assertEquals("true", run(main));
	}

	@Test
	public void nonExportedVarIsNotImportable() throws Exception {
		write("state.leek", "var secret = 1\nexport var shown = 2");
		String main = writeMain("import { secret } from \"state\"\nreturn 0");
		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.SYMBOL_NOT_EXPORTED), "import d'un var non exporté doit échouer, got: " + errors);
	}

	@Test
	public void sameVarNameInTwoModulesNoCollision() throws Exception {
		// state a un champ `count` (g_$m1$count) ; le principal a son propre `count`
		// (local u_count). getCount() lit CELUI de state. Aucune collision.
		write("state.leek",
			"export var count = 100\n" +
			"export function getCount() { return count }");
		String main = writeMain(
			"import { getCount } from \"state\"\n" +
			"var count = 1\n" +
			"return count + getCount()");
		assertEquals("101", run(main));
	}

	// =========================================================================
	// Classes de module (export class)
	// =========================================================================

	@Test
	public void importExportedClass() throws Exception {
		write("geo.leek", "export class Vec { public integer x\nconstructor(v) { this.x = v } }");
		String main = writeMain("import { Vec } from \"geo\"\nvar v = new Vec(7)\nreturn v.x");
		assertEquals("7", run(main));
	}

	@Test
	public void importExportedClassWithMethod() throws Exception {
		write("geo.leek",
			"export class Vec {\n" +
			"  public integer x\n" +
			"  constructor(v) { this.x = v }\n" +
			"  public doubled() { return this.x * 2 }\n" +
			"}");
		String main = writeMain("import { Vec } from \"geo\"\nreturn new Vec(8).doubled()");
		assertEquals("16", run(main));
	}

	@Test
	public void nonExportedClassIsNotImportable() throws Exception {
		write("geo.leek", "class Hidden {}\nexport class Shown {}");
		String main = writeMain("import { Hidden } from \"geo\"\nreturn 0");
		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.SYMBOL_NOT_EXPORTED), "import d'une classe non exportée doit échouer, got: " + errors);
	}

	@Test
	public void sameClassNameInTwoModulesNoCollision() throws Exception {
		// geo a une classe privée Box ; le principal a SA propre Box. Pas de collision.
		write("geo.leek",
			"class Box { public integer v\nconstructor(x) { this.v = x } }\n" +
			"export function makeBox(x) { var b = new Box(x)\nreturn b.v }");
		String main = writeMain(
			"class Box { public integer v\nconstructor(x) { this.v = x * 10 } }\n" +
			"import { makeBox } from \"geo\"\n" +
			"var b = new Box(5)\n" +
			"return b.v + makeBox(3)");
		assertEquals("53", run(main));
	}

	// =========================================================================
	// Import namespace (import * as Ns)
	// =========================================================================

	@Test
	public void namespaceFunctionCall() throws Exception {
		write("geo.leek", "export function clamp(x, lo, hi) { return x < lo ? lo : (x > hi ? hi : x) }");
		String main = writeMain("import * as Geo from \"geo\"\nreturn Geo.clamp(15, 0, 10)");
		assertEquals("10", run(main));
	}

	@Test
	public void namespaceVarAccess() throws Exception {
		write("state.leek", "export var hp = 42");
		String main = writeMain("import * as State from \"state\"\nreturn State.hp");
		assertEquals("42", run(main));
	}

	@Test
	public void namespaceGlobalUsedInFunction() throws Exception {
		write("conf.leek", "export global LIMIT = 5");
		String main = writeMain(
			"import * as Conf from \"conf\"\n" +
			"function ok(x) { return x < Conf.LIMIT }\n" +
			"return ok(3) and !ok(9)");
		assertEquals("true", run(main));
	}

	@Test
	public void namespaceClassNew() throws Exception {
		write("geo.leek", "export class Vec { public integer x\nconstructor(v) { this.x = v } }");
		String main = writeMain("import * as Geo from \"geo\"\nvar v = new Geo.Vec(9)\nreturn v.x");
		assertEquals("9", run(main));
	}

	@Test
	public void namespaceNonExportedMemberFails() throws Exception {
		write("geo.leek", "function secret() { return 1 }\nexport function shown() { return 2 }");
		String main = writeMain("import * as Geo from \"geo\"\nreturn Geo.secret()");
		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.SYMBOL_NOT_EXPORTED), "membre namespace non exporté doit échouer, got: " + errors);
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
	// Cycles entre modules (permis, cf docs/ls5-modules.md §1.2)
	// =========================================================================

	@Test
	public void cycleBetweenFunctions() throws Exception {
		write("ca.leek", "import { fb } from \"cb\"\nexport function fa(n) { return n <= 0 ? 0 : 1 + fb(n - 1) }");
		write("cb.leek", "import { fa } from \"ca\"\nexport function fb(n) { return n <= 0 ? 0 : 1 + fa(n - 1) }");
		assertEquals("5", run(writeMain("import { fa } from \"ca\"\nreturn fa(5)")));
	}

	@Test
	public void cycleBackToEntry() throws Exception {
		String main = "Main_" + uniqueId + "_cycle";
		write("cc.leek", "import { fm } from \"" + main + "\"\nexport function fc() { return fm() + 1 }");
		write(main + ".leek", "import { fc } from \"cc\"\nexport function fm() { return 10 }\nreturn fc()");
		assertEquals("11", run(main));
	}

	@Test
	public void cycleBetweenClasses() throws Exception {
		write("cs.leek", "import { Action } from \"ct\"\nexport class State { Action a = null\n static make() { var s = new State(); s.a = new Action(); return s } }");
		write("ct.leek", "import { State } from \"cs\"\nexport class Action { apply(State s) { return 7 } }");
		assertEquals("7", run(writeMain("import { State } from \"cs\"\nvar s = State.make()\nreturn s.a.apply(s)")));
	}

	@Test
	public void cycleMissingExportStillReported() throws Exception {
		write("cd.leek", "import { nope } from \"ce\"\nexport function fd() { return 1 }");
		write("ce.leek", "import { fd } from \"cd\"\nexport function fe() { return fd() }");
		var errors = compileAndCollectErrors(writeMain("import { fe } from \"ce\"\nreturn fe()"));
		assertTrue(errors.contains(Error.SYMBOL_NOT_EXPORTED), errors.toString());
	}

	// =========================================================================
	// Classes d'un module : résolues dans leur module
	// =========================================================================

	@Test
	public void classMethodSeesModuleImports() throws Exception {
		write("ka.leek", "export class Tool { v() { return 3 } }\nexport function helper() { return 4 }");
		write("kb.leek", "import { Tool, helper } from \"ka\"\nexport class User { static make() { return new Tool().v() } m() { return helper() } }");
		assertEquals("[3, 4]", run(writeMain("import { User } from \"kb\"\nreturn [User.make(), new User().m()]")));
	}

	@Test
	public void fieldTypedByImportedClass() throws Exception {
		write("kc.leek", "export class Gear { v() { return 6 } }");
		write("kd.leek", "import { Gear } from \"kc\"\nexport class Robot { Gear g = new Gear() }");
		assertEquals("6", run(writeMain("import { Robot } from \"kd\"\nreturn new Robot().g.v()")));
	}

	@Test
	public void classExtendsImportedClass() throws Exception {
		write("ke.leek", "export class Base { v() { return 2 } }");
		write("kf.leek", "import { Base } from \"ke\"\nexport class Child extends Base { w() { return this.v() + 1 } }");
		assertEquals("3", run(writeMain("import { Child } from \"kf\"\nreturn new Child().w()")));
	}

	// =========================================================================
	// Code de haut niveau d'un module : analysé dans son module, locales à lui
	// =========================================================================

	@Test
	public void moduleLocalsDoNotCollide() throws Exception {
		write("va.leek", "var x = 1\nexport function a() { return 1 }");
		write("vb.leek", "var x = 2\nexport function b() { return 2 }");
		assertEquals("[1, 2, 3]", run(writeMain("import { a } from \"va\"\nimport { b } from \"vb\"\nvar x = 3\nreturn [a(), b(), x]")));
	}

	@Test
	public void moduleLocalNotVisibleFromImporter() throws Exception {
		write("vc.leek", "var secret = 1\nexport function c() { return 1 }");
		var errors = compileAndCollectErrors(writeMain("import { c } from \"vc\"\nreturn secret"));
		assertTrue(errors.contains(Error.UNKNOWN_VARIABLE_OR_FUNCTION), errors.toString());
	}

	@Test
	public void moduleTopLevelSeesItsExportVar() throws Exception {
		write("vd.leek", "export var c = 1\nc = c + 1\nexport function gc() { return c }");
		assertEquals("2", run(writeMain("import { gc } from \"vd\"\nreturn gc()")));
	}

	@Test
	public void moduleTopLevelSeesItsImports() throws Exception {
		write("ve.leek", "export var trace = []\ntrace.push('e')\nexport function t() { return trace }");
		write("vf.leek", "import { trace } from \"ve\"\ntrace.push('f')\nexport function f() { return 0 }");
		// Ordre d'initialisation : dépendances d'abord, chaque module une seule fois
		assertEquals("[\"e\", \"f\"]", run(writeMain("import { f } from \"vf\"\nimport { t } from \"ve\"\nreturn t()")));
	}

	@Test
	public void moduleWithOlderVersionPragmaIsAnError() throws Exception {
		write("ov.leek", "// @version:4\nexport function f() { return 1 }");
		var errors = compileAndCollectErrors(writeMain("import { f } from \"ov\"\nreturn f()"));
		assertTrue(errors.contains(Error.MODULE_VERSION_MISMATCH), errors.toString());
	}

	@Test
	public void moduleWithCurrentVersionPragmaIsAccepted() throws Exception {
		write("ow.leek", "// @version:5\nexport function g() { return 2 }");
		assertEquals("2", run(writeMain("import { g } from \"ow\"\nreturn g()")));
	}

	// =========================================================================
	// Coût : découper en modules ne change pas les opérations
	// =========================================================================

	@Test
	public void modulesCostTheSameOperationsAsOneFile() throws Exception {
		String lib = "class Vec { x = 0; y = 0; constructor(x, y) { this.x = x; this.y = y } len2() { return this.x * this.x + this.y * this.y } }\n"
			+ "function sum(n) { var s = 0; for (var i = 0; i < n; i++) { s += new Vec(i, 1).len2() } return s }\n";
		String usage = "var total = 0\nfor (var k = 0; k < 20; k++) { total += sum(k) }\nreturn total";
		long oneFile = operations(writeMain(lib + usage));
		write("vecmod.leek", lib.replace("class Vec", "export class Vec").replace("function sum", "export function sum"));
		uniqueId += "_m";
		long modules = operations(writeMain("import { sum } from \"vecmod\"\n" + usage));
		assertEquals(oneFile, modules);
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
