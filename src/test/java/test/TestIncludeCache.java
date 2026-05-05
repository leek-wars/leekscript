package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystem;
import leekscript.runner.AI;

/**
 * Couverture exhaustive du cache d'inclusions LeekScript et de son invalidation.
 *
 * Issue #3597 : modifier un fichier inclus sans toucher l'entrypoint doit faire
 * recompiler l'entrypoint au prochain run. Cas courants à couvrir : éditeur web
 * (mtime poussé par le daemon), git pull (mtimes simultanés ou non-monotones),
 * includes transitifs, structures en diamant, etc.
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique. Sans isolation
 * vis-à-vis des autres classes de tests, leur LeekScript.compileFile() résolvent
 * via notre TmpFileSystem et échouent en cascade.
 */
@Isolated
public class TestIncludeCache {

	@TempDir Path tmpRoot;
	private TmpFileSystem fs;
	// Nom d'entrypoint unique par test : aiCache RAM et le .class disque sont
	// keyés par Objects.hash(owner, path), partagés entre tests JUnit malgré
	// tmpRoots distincts.
	private static final AtomicLong UNIQUE = new AtomicLong();
	private String uniqueId;

	@BeforeEach
	void setUp() {
		uniqueId = Long.toHexString(System.nanoTime()) + "_" + UNIQUE.incrementAndGet();
		fs = new TmpFileSystem(tmpRoot);
		LeekScript.setFileSystem(fs);
	}

	@AfterEach
	void tearDown() {
		LeekScript.resetFileSystem();
	}

	// =========================================================================
	// Cas direct : Main → sub
	// =========================================================================

	@Test
	public void directIncludeModified() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");

		assertEquals("1", run(main));

		write("sub.leek", "function f() { return 2; }");
		bumpMtime("sub.leek", 1000);

		assertEquals("2", run(main), "modif d'un include direct doit invalider le cache de Main");
	}

	@Test
	public void directIncludeModifiedWithEntrypointTouched() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");
		assertEquals("1", run(main));

		// Comportement du daemon prod après commit f0902cb2 : invalidateFile bump
		// la mtime de l'entrypoint en plus de celle de l'include.
		long ts = System.currentTimeMillis() + 1000;
		write("sub.leek", "function f() { return 2; }");
		setMtime("sub.leek", ts);
		setMtime(mainName(main), ts);

		assertEquals("2", run(main));
	}

	/** Issue #3597 : sub n'existe pas au moment du premier scan, créé après. */
	@Test
	public void includeCreatedAfterFirstScanThenModified() throws Exception {
		String main = writeMain("include(\"sub\");\nreturn f();");
		try {
			run(main); // throw : sub introuvable
		} catch (Throwable expected) {}

		write("sub.leek", "function f() { return 42; }");
		assertEquals("42", run(main), "include nouvellement créé non détecté");

		write("sub.leek", "function f() { return 100; }");
		bumpMtime("sub.leek", 2000);
		assertEquals("100", run(main), "modif après création non détectée (cache figé sur include non-résolu)");
	}

	// =========================================================================
	// Includes transitifs : Main → A → B
	// =========================================================================

	@Test
	public void transitiveIncludeModified() throws Exception {
		write("B.leek", "function g() { return 7; }");
		write("A.leek", "include(\"B\");\nfunction f() { return g(); }");
		String main = writeMain("include(\"A\");\nreturn f();");
		assertEquals("7", run(main));

		write("B.leek", "function g() { return 9; }");
		bumpMtime("B.leek", 1000);

		assertEquals("9", run(main), "modif d'un include transitif (B inclus par A inclus par Main) non propagée");
	}

	@Test
	public void intermediateIncludeModified() throws Exception {
		write("B.leek", "function g() { return 7; }");
		write("A.leek", "include(\"B\");\nfunction f() { return g() + 1; }");
		String main = writeMain("include(\"A\");\nreturn f();");
		assertEquals("8", run(main));

		write("A.leek", "include(\"B\");\nfunction f() { return g() + 2; }");
		bumpMtime("A.leek", 1000);

		assertEquals("9", run(main), "modif de l'intermédiaire A non propagée à Main");
	}

	// =========================================================================
	// Diamant : Main → A et Main → B, A et B → C
	// =========================================================================

	@Test
	public void diamondCommonIncludeModified() throws Exception {
		write("C.leek", "function k() { return 3; }");
		write("A.leek", "include(\"C\");\nfunction a() { return k(); }");
		write("B.leek", "include(\"C\");\nfunction b() { return k() * 2; }");
		String main = writeMain("include(\"A\");\ninclude(\"B\");\nreturn a() + b();");
		assertEquals("9", run(main));

		write("C.leek", "function k() { return 5; }");
		bumpMtime("C.leek", 1000);

		assertEquals("15", run(main), "modif d'un include partagé en diamant non propagée");
	}

	// =========================================================================
	// Multi-includes : Main → A, B, C, modifier C
	// =========================================================================

	@Test
	public void oneOfManyIncludesModified() throws Exception {
		write("A.leek", "function a() { return 1; }");
		write("B.leek", "function b() { return 2; }");
		write("C.leek", "function c() { return 3; }");
		String main = writeMain("include(\"A\");\ninclude(\"B\");\ninclude(\"C\");\nreturn a() + b() + c();");
		assertEquals("6", run(main));

		write("C.leek", "function c() { return 30; }");
		bumpMtime("C.leek", 1000);

		assertEquals("33", run(main), "modif d'un include parmi plusieurs non détectée");
	}

	// =========================================================================
	// Chaîne profonde : Main → A → B → C → D
	// =========================================================================

	@Test
	public void deepChainLeafModified() throws Exception {
		write("D.leek", "function d() { return 1; }");
		write("C.leek", "include(\"D\");\nfunction c() { return d(); }");
		write("B.leek", "include(\"C\");\nfunction b() { return c(); }");
		write("A.leek", "include(\"B\");\nfunction a() { return b(); }");
		String main = writeMain("include(\"A\");\nreturn a();");
		assertEquals("1", run(main));

		write("D.leek", "function d() { return 100; }");
		bumpMtime("D.leek", 1000);

		assertEquals("100", run(main), "modif de la feuille D (4 niveaux de profondeur) non propagée");
	}

	// =========================================================================
	// Git pull : mtimes simultanées, non-monotones
	// =========================================================================

	@Test
	public void gitPullSimultaneousMtimes() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");
		assertEquals("1", run(main));

		// Simule git pull : tous les fichiers modifiés ont la même mtime exacte.
		long ts = System.currentTimeMillis() + 1000;
		write("sub.leek", "function f() { return 2; }");
		setMtime("sub.leek", ts);
		setMtime(mainName(main), ts);

		assertEquals("2", run(main), "git pull avec mtimes simultanées : modif non détectée");
	}

	@Test
	public void gitPullMtimeBackwards() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");
		assertEquals("1", run(main));

		// Simule git checkout d'une ancienne révision : mtime du sub recule. Le
		// cache du Folder utilisait `<=` ce qui rendait le AIFile cached valide
		// "tant que le mtime disque ≤ celui qu'on a chargé", masquant le rollback.
		write("sub.leek", "function f() { return 2; }");
		setMtime("sub.leek", System.currentTimeMillis() - 86_400_000L); // 24h en arrière

		assertEquals("2", run(main), "git checkout vers ancien (mtime backwards) non détecté");
	}

	@Test
	public void gitPullChainModified() throws Exception {
		write("B.leek", "function g() { return 1; }");
		write("A.leek", "include(\"B\");\nfunction f() { return g(); }");
		String main = writeMain("include(\"A\");\nreturn f();");
		assertEquals("1", run(main));

		// Simule git pull qui modifie A et B en même temps avec la même mtime.
		long ts = System.currentTimeMillis() + 1000;
		write("B.leek", "function g() { return 5; }");
		write("A.leek", "include(\"B\");\nfunction f() { return g() * 2; }");
		setMtime("A.leek", ts);
		setMtime("B.leek", ts);

		assertEquals("10", run(main), "git pull modifiant A + B simultanément non détecté");
	}

	// =========================================================================
	// Ajout d'une directive include à un Main existant
	// =========================================================================

	@Test
	public void includeDirectiveAddedToExistingMain() throws Exception {
		String main = writeMain("return 1;");
		assertEquals("1", run(main));

		// L'utilisateur ajoute un include à Main et crée le sub.
		write("sub.leek", "function f() { return 42; }");
		long ts = System.currentTimeMillis() + 1000;
		write(mainName(main), "include(\"sub\");\nreturn f();");
		setMtime(mainName(main), ts);

		assertEquals("42", run(main), "ajout d'une directive include non détecté");

		// Maintenant on modifie juste le sub : le mtime de Main n'a pas bougé,
		// mais le cache doit être invalidé via la chaîne d'includes.
		write("sub.leek", "function f() { return 100; }");
		bumpMtime("sub.leek", 2000);

		assertEquals("100", run(main), "modif du sub après ajout de directive include non détectée");
	}

	// =========================================================================
	// Sous-fichiers
	// =========================================================================

	@Test
	public void subInSubFolderModified() throws Exception {
		Files.createDirectories(tmpRoot.resolve("Class"));
		write("Class/util.leek", "function u() { return 1; }");
		String main = writeMain("include(\"Class/util\");\nreturn u();");
		assertEquals("1", run(main));

		write("Class/util.leek", "function u() { return 2; }");
		bumpMtime("Class/util.leek", 1000);

		assertEquals("2", run(main), "include via sous-dossier non détecté");
	}

	// =========================================================================
	// Multi-entrypoints partageant un include
	// =========================================================================

	@Test
	public void multipleEntrypointsShareIncludeAllInvalidated() throws Exception {
		write("shared.leek", "function s() { return 1; }");
		String main1Name = "Main1_" + uniqueId;
		String main2Name = "Main2_" + uniqueId;
		write(main1Name + ".leek", "include(\"shared\");\nreturn s();");
		write(main2Name + ".leek", "include(\"shared\");\nreturn s() + 10;");

		assertEquals("1", run(main1Name));
		assertEquals("11", run(main2Name));

		write("shared.leek", "function s() { return 5; }");
		bumpMtime("shared.leek", 1000);

		assertEquals("5", run(main1Name), "Main1 doit voir la nouvelle version de shared");
		assertEquals("15", run(main2Name), "Main2 doit voir la nouvelle version de shared");
	}

	// =========================================================================
	// Cas pathologiques : cycles d'include
	// =========================================================================

	/**
	 * Inclusion circulaire A → B → A. Le compilateur LeekScript a un guard
	 * (mIncluded.contains check) qui évite la récursion infinie. On vérifie que
	 * IncludeGraph.getTransitivelyIncluded ne tourne pas en boucle non plus.
	 */
	@Test
	public void circularIncludeDoesNotHang() throws Exception {
		write("A.leek", "include(\"B\");\nfunction a() { return 1; }");
		write("B.leek", "include(\"A\");\nfunction b() { return 2; }");
		String main = writeMain("include(\"A\");\nreturn a();");

		// Doit compiler sans hang ni StackOverflow.
		try {
			run(main);
		} catch (Throwable t) {
			// Rejet du compiler acceptable, on vérifie juste l'absence de hang.
		}

		// Modifier B doit invalider le cache de Main via la chaîne A → B sans
		// hanger sur le cycle B → A.
		write("B.leek", "include(\"A\");\nfunction b() { return 99; }");
		bumpMtime("B.leek", 1000);

		try {
			run(main);
		} catch (Throwable t) {
			// idem
		}
	}

	// =========================================================================
	// Suppression et recréation d'un include
	// =========================================================================

	@Test
	public void includeDeletedThenRecreated() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");
		assertEquals("1", run(main));

		// Supprimer le sub : le prochain compile doit échouer (include introuvable).
		Files.delete(tmpRoot.resolve("sub.leek"));
		try {
			run(main);
			throw new AssertionError("compile devrait échouer après suppression du sub");
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable expected) {
			// attendu
		}

		// Recréer le sub avec un nouveau contenu.
		write("sub.leek", "function f() { return 99; }");
		assertEquals("99", run(main), "recréation du sub après suppression non détectée");
	}

	// =========================================================================
	// Renommage d'un sous-fichier
	// =========================================================================

	@Test
	public void includeRenamed() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");
		assertEquals("1", run(main));

		// L'utilisateur renomme sub → sub2 et adapte Main.
		Files.move(tmpRoot.resolve("sub.leek"), tmpRoot.resolve("sub2.leek"));
		long ts = System.currentTimeMillis() + 1000;
		write(mainName(main), "include(\"sub2\");\nreturn f();");
		setMtime(mainName(main), ts);

		assertEquals("1", run(main), "renommage de sub en sub2 + adaptation de Main non détectés");

		// Modifier sub2 doit invalider Main.
		write("sub2.leek", "function f() { return 7; }");
		bumpMtime("sub2.leek", 2000);

		assertEquals("7", run(main), "modif de sub2 après renommage non propagée");
	}

	// =========================================================================
	// Restart simulé : aiCache vidé entre runs (SoftRef GC), .class disque
	// présent, AIFile re-construit
	// =========================================================================

	@Test
	public void simulatedWorkerRestartReusesDiskCacheCorrectly() throws Exception {
		write("sub.leek", "function f() { return 1; }");
		String main = writeMain("include(\"sub\");\nreturn f();");
		assertEquals("1", run(main));

		// Best-effort : forcer GC pour évincer la SoftRef du aiCache RAM. Au prochain
		// compile, le worker doit charger depuis le .class disque, pas re-compiler.
		// On modifie sub avant le GC pour bien tester l'invalidation post-restart.
		write("sub.leek", "function f() { return 2; }");
		bumpMtime("sub.leek", 1000);

		System.gc();
		Thread.sleep(100);
		System.gc();

		assertEquals("2", run(main), "post-restart simulé, modif du sub non détectée");
	}

	// =========================================================================
	// Modification du contenu sans changement structurel : valide-t-on que les
	// tokens ré-extraits donnent le bon graph ?
	// =========================================================================

	/**
	 * L'IncludeGraph cache le tokenStream sur l'AIFile via setTokenStream. Si le
	 * fichier change (mtime updated) mais que l'AIFile sous-jacent est réutilisé
	 * sans être rechargé, le tokenStream cached pourrait fournir l'ancien set
	 * d'includes. On vérifie que l'ajout d'une directive include (= changement
	 * d'une directive include, mais elle apparaît) est bien pris en compte.
	 */
	@Test
	public void includeDirectiveChangedInExistingFile() throws Exception {
		write("subA.leek", "function fa() { return 1; }");
		write("subB.leek", "function fb() { return 99; }");
		String main = writeMain("include(\"subA\");\nreturn fa();");
		assertEquals("1", run(main));

		// L'utilisateur change subA pour subB dans Main.
		long ts = System.currentTimeMillis() + 1000;
		write(mainName(main), "include(\"subB\");\nreturn fb();");
		setMtime(mainName(main), ts);

		assertEquals("99", run(main), "changement de cible d'include non détecté");

		// Modifier subB doit invalider Main, modifier subA ne doit PLUS l'invalider.
		write("subB.leek", "function fb() { return 7; }");
		bumpMtime("subB.leek", 2000);
		assertEquals("7", run(main), "modif de subB (nouvel include) non propagée");

		write("subA.leek", "function fa() { return 99999; }");
		bumpMtime("subA.leek", 3000);
		assertEquals("7", run(main), "modif de subA (ancien include) ne devrait PAS invalider");
	}

	// =========================================================================
	// Multi-include du même fichier (idempotence)
	// =========================================================================

	@Test
	public void sameIncludeListedTwice() throws Exception {
		write("sub.leek", "function f() { return 3; }");
		String main = writeMain("include(\"sub\");\ninclude(\"sub\");\nreturn f();");
		assertEquals("3", run(main));

		write("sub.leek", "function f() { return 5; }");
		bumpMtime("sub.leek", 1000);

		assertEquals("5", run(main), "double include du même fichier : modif non détectée");
	}

	// =========================================================================
	// Pragma version vs token cache : extractIncludes ne doit pas figer les tokens
	// à une version pré-pragma. Sinon un fichier avec `// @version:2` qui utilise
	// un keyword version-dependent (ex. AND, case-insensitive en v≤2 uniquement)
	// plante en CLOSING_PARENTHESIS_EXPECTED parce que les tokens cached en v=4
	// (file.getVersion() par défaut côté DbFileSystem) traitent AND comme identifier.
	// =========================================================================

	@Test
	public void pragmaVersion2KeywordCompiles() throws Exception {
		// Repro du crash Valoutre : "// @version:2" + AND. Sans la fix, le lexer
		// d'extractIncludes parse à v=4 (file.getVersion() par défaut), cache les
		// tokens AND-as-identifier sur l'AIFile, et IACompiler.compile les réutilise
		// après avoir résolu @version:2 → erreur de parsing au premier AND.
		String main = writeMain("// @version:2\nvar a = 1; var b = 2; if (a == 1 AND b == 2) { return 1; } else { return 0; }");
		assertEquals("1", run(main), "@version:2 + AND : tokens stale en v=4 si extractIncludes parse avant PragmaParser");
	}

	@Test
	public void pragmaVersion2InIncludeCompiles() throws Exception {
		// Même bug mais l'AND est dans un include avec son propre @version:2.
		// extractIncludes scanne TOUS les fichiers du graph (pas que l'entrypoint),
		// donc l'include se fait également cacher des tokens à la mauvaise version.
		write("sub.leek", "// @version:2\nfunction f() { var a = 1; var b = 2; if (a == 1 AND b == 2) { return 42; } return 0; }");
		String main = writeMain("// @version:2\ninclude(\"sub\");\nreturn f();");
		assertEquals("42", run(main));
	}

	@Test
	public void pragmaVersion2WithModification() throws Exception {
		// Une fois la fix appliquée, les tokens cachés sont à la bonne version.
		// On vérifie qu'une modification (mtime bumped) recompile bien et que les
		// nouveaux tokens sont aussi cachés à la bonne version.
		String main = writeMain("// @version:2\nreturn 1;");
		assertEquals("1", run(main));

		write(mainName(main), "// @version:2\nvar a = 1; if (a == 1 AND true) { return 7; } return 0;");
		bumpMtime(mainName(main), 1000);
		assertEquals("7", run(main));
	}

	// =========================================================================
	// Reporting d'erreurs à travers la frontière d'include : les LeekCompilerException
	// levées dans firstPass/secondPass d'un include étaient silencieusement avalées,
	// l'utilisateur ne voyait qu'un UNKNOWN_VARIABLE_OR_FUNCTION trompeur en aval.
	// =========================================================================

	@Test
	public void duplicateFunctionInIncludeReportsErrorFromInclude() throws Exception {
		write("lib.leek",
			"function f() { return 1; }\n" +
			"function dup() { return 2; }\n" +
			"function dup() { return 3; }\n" +
			"function g() { return 4; }\n");
		String main = writeMain("include(\"lib\");\nreturn f() + g();");

		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.FUNCTION_NAME_UNAVAILABLE), "got: " + errors);
	}

	@Test
	public void duplicateClassInIncludeReportsErrorFromInclude() throws Exception {
		write("lib.leek", "class Foo {}\nclass Foo {}\n");
		String main = writeMain("include(\"lib\");\nreturn 0;");

		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.VARIABLE_NAME_UNAVAILABLE), "got: " + errors);
	}

	@Test
	public void errorInDeepIncludeChainStillReported() throws Exception {
		// Main → A → B : un doublon dans B doit remonter jusqu'à Main.
		write("B.leek",
			"function dup() { return 1; }\n" +
			"function dup() { return 2; }\n");
		write("A.leek", "include(\"B\");\nfunction h() { return 0; }");
		String main = writeMain("include(\"A\");\nreturn h();");

		var errors = compileAndCollectErrors(main);
		assertTrue(errors.contains(Error.FUNCTION_NAME_UNAVAILABLE), "got: " + errors);
	}

	@Test
	public void cleanIncludeProducesNoSpuriousErrors() throws Exception {
		write("lib.leek", "function f() { return 1; }\nfunction g() { return 2; }\n");
		String main = writeMain("include(\"lib\");\nreturn f() + g();");

		var errors = compileAndCollectErrors(main);
		assertEquals(0, errors.size(), "got: " + errors);
		assertEquals("3", run(main));
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

	private String mainName(String main) {
		return main + ".leek";
	}

	private void setMtime(String relative, long millis) throws IOException {
		Files.setLastModifiedTime(tmpRoot.resolve(relative), FileTime.fromMillis(millis));
	}

	private void bumpMtime(String relative, long offsetMs) throws IOException {
		setMtime(relative, System.currentTimeMillis() + offsetMs);
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

	/** Union des codes d'erreur accumulés sur l'AIFile et de l'éventuelle exception
	 *  terminale levée par le compile. */
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

	// =========================================================================
	// FileSystem mimant le worker DbFileSystem (filesystem-backed, per-owner
	// cache, listAllFiles via Files.walk) sans dépendance sur le serveur.
	// =========================================================================

	static class TmpFileSystem extends FileSystem {
		private final Path root;
		private final Map<Integer, Folder> rootFolders = new HashMap<>();
		private final Map<String, AIFile> filesByPath = new HashMap<>();

		TmpFileSystem(Path root) {
			this.root = root;
		}

		@Override
		public synchronized Iterable<AIFile> listAllFiles(int owner) {
			var list = new ArrayList<AIFile>();
			if (!Files.exists(root)) return list;
			try (var walk = Files.walk(root)) {
				walk.filter(p -> p.toString().endsWith(".leek") && Files.isRegularFile(p)).forEach(p -> {
					var rel = root.relativize(p).toString().replace(java.io.File.separatorChar, '/');
					rel = rel.substring(0, rel.length() - ".leek".length());
					var f = getFileByPath(rel, owner);
					if (f != null) list.add(f);
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		synchronized AIFile getFileByPath(String filePath, int owner) {
			var key = owner + ":" + filePath;
			var cached = filesByPath.get(key);
			if (cached != null && getAITimestamp(cached) == cached.getTimestamp()) return cached;
			var fsPath = root.resolve(filePath + ".leek");
			try {
				var code = Files.readString(fsPath);
				long mtime = Files.getLastModifiedTime(fsPath).toMillis();
				var rootFolder = getRoot(owner);
				var folder = resolveParent(filePath, rootFolder);
				var file = new AIFile(filePath, code, mtime, LeekScript.LATEST_VERSION, folder, owner,
						Objects.hash(owner, filePath) & 0xfffffff, false);
				folder.addFile(file);
				filesByPath.put(key, file);
				return file;
			} catch (Exception e) {
				return null;
			}
		}

		private Folder resolveParent(String filePath, Folder rootFolder) {
			var parts = filePath.split("/");
			var current = rootFolder;
			for (int i = 0; i < parts.length - 1; i++) {
				var sub = current.getFolder(parts[i]);
				if (sub == null) return current;
				current = sub;
			}
			return current;
		}

		@Override public Folder getRoot() { return null; }

		@Override
		public synchronized Folder getRoot(int owner) {
			var f = rootFolders.get(owner);
			if (f != null) return f;
			f = new Folder(owner, this);
			f.setParent(f);
			f.setRoot(f);
			rootFolders.put(owner, f);
			return f;
		}

		@Override public Folder getRoot(int owner, int farmer) { return null; }

		@Override
		public synchronized Folder findFolder(String name, Folder current) {
			var path = buildChild(current, name);
			if (!Files.isDirectory(root.resolve(path))) return null;
			return new Folder(name.hashCode(), current.getOwner(), name, current, getRoot(current.getOwner()), this,
					System.currentTimeMillis());
		}

		@Override
		public synchronized AIFile findFile(String name, Folder folder) throws FileNotFoundException {
			var filePath = buildChild(folder, name);
			var key = folder.getOwner() + ":" + filePath;
			var cached = filesByPath.get(key);
			if (cached != null && getAITimestamp(cached) == cached.getTimestamp()) return cached;
			var fsPath = root.resolve(filePath + ".leek");
			try {
				var code = Files.readString(fsPath);
				long mtime = Files.getLastModifiedTime(fsPath).toMillis();
				var file = new AIFile(filePath, code, mtime, LeekScript.LATEST_VERSION, folder, folder.getOwner(),
						Objects.hash(folder.getOwner(), filePath) & 0xfffffff, false);
				filesByPath.put(key, file);
				return file;
			} catch (IOException e) {
				throw new FileNotFoundException(filePath);
			}
		}

		private String buildChild(Folder folder, String name) {
			var parts = new ArrayList<String>();
			parts.add(name);
			var c = folder;
			while (c != null && c.getParent() != c) {
				parts.add(0, c.getName());
				c = c.getParent();
			}
			return String.join("/", parts);
		}

		@Override public AIFile getFileById(int id, int farmer) { return null; }
		@Override public Folder getFolderById(int id, int farmer) { return getRoot(farmer); }

		@Override
		public long getAITimestamp(AIFile ai) {
			try {
				return Files.getLastModifiedTime(root.resolve(ai.getPath() + ".leek")).toMillis();
			} catch (Exception e) {
				return Long.MAX_VALUE;
			}
		}

		@Override public void loadDependencies(AIFile ai) {}
		@Override public long getFolderTimestamp(Folder folder) { return Long.MAX_VALUE; }
	}
}
