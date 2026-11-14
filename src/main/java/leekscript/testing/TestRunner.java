package leekscript.testing;

import java.util.ArrayList;
import java.util.List;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.Options;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

/**
 * Lance les tests d'un module de test (`*.test.leek`). Chaque test tourne sur une IA neuve : ses
 * statiques et ses globales repartent de zéro, son compteur d'opérations aussi.
 *
 * Deux usages : {@link #runModule} lance un module hors de tout combat (LeekScript pur) ;
 * {@link #run} lance un test sur une IA déjà installée par l'appelant (dans un combat, par exemple).
 */
public class TestRunner {

	/** Plafond d'opérations d'un test, sauf `@test(ops: …)`. */
	public static final long DEFAULT_MAX_OPERATIONS = 20_000_000;

	/** Plafond d'opérations d'un test, même s'il demande plus (`@test(ops: …)`) : borne sa durée. */
	public static final long MAX_OPERATIONS = 500_000_000;

	/**
	 * Lance un test sur une IA neuve, prête à tourner : le code de premier niveau du module, ses
	 * `@before`, le test, ses `@after`. L'IA garde son enregistreur après l'appel.
	 */
	public static TestResult run(AI ai, TestRun run) {
		var test = run.test();
		var table = run.table();
		if (test.skip() != null) return run.skipped();
		var recorder = new TestRecorder();
		ai.setTestRecorder(recorder);
		String error = null;
		// Une erreur avant le test (code de premier niveau, `@before`) n'est jamais l'échec attendu d'un `@fails`
		boolean inTest = false;
		boolean outOfBudget = false;
		// Coût de la seule fonction du test : celui d'un bench
		long beforeTest = 0, afterTest = -1;
		long start = System.nanoTime();
		try {
			ai.runIA();
			for (var c : table) if (c.kind() == TestCase.Kind.BEFORE) ai.callTestFunction(c.index());
			inTest = true;
			beforeTest = ai.getOperations();
			ai.callTestFunction(test.index());
			afterTest = ai.getOperations();
		} catch (Throwable t) {
			rethrowIfFatal(t);
			var cause = unwrap(t);
			error = describe(ai, cause);
			outOfBudget = cause instanceof LeekRunException e && isOutOfBudget(e);
		}
		// Les `@after` tournent même après un test planté ; leur propre erreur ne cache pas la première
		try {
			for (var c : table) if (c.kind() == TestCase.Kind.AFTER) ai.callTestFunction(c.index());
		} catch (Throwable t) {
			rethrowIfFatal(t);
			if (error == null) {
				error = describe(ai, unwrap(t));
				inTest = false;
			}
		}
		long duration = System.nanoTime() - start;

		TestResult.Status status;
		String reason = null;
		if (error != null || !recorder.errors().isEmpty()) {
			status = TestResult.Status.ERROR;
		} else if (!recorder.failures().isEmpty()) {
			status = TestResult.Status.FAILED;
		} else if (recorder.asserts() == 0 && !test.isBench()) {
			// Un test qui ne vérifie rien ne prouve rien (un bench, lui, mesure)
			status = TestResult.Status.FAILED;
			reason = "no_assert";
		} else {
			status = TestResult.Status.PASSED;
		}
		// `@fails` : l'échec attendu est celui du test lui-même (assert raté, erreur dans son corps).
		// Un test vidé de ses asserts, une préparation qui plante ou un budget épuisé restent des échecs.
		if (test.fails() != null && reason == null && !outOfBudget && (error == null || inTest)) {
			reason = test.fails();
			status = status.ok ? TestResult.Status.UNEXPECTED_PASS : TestResult.Status.EXPECTED_FAILURE;
		}
		long operations = test.isBench() && afterTest >= 0 ? afterTest - beforeTest : ai.getOperations();
		return new TestResult(run.module(), test.name(), test.line(), status, reason, operations, duration, recorder.asserts(),
			List.copyOf(recorder.failures()), List.copyOf(recorder.errors()), error, run.logs() == null ? List.of() : List.copyOf(run.logs()),
			test.tolerance());
	}

	/** Une erreur de la machine virtuelle (mémoire épuisée…) n'est pas celle d'un test : elle remonte. */
	private static void rethrowIfFatal(Throwable t) {
		if (t instanceof VirtualMachineError fatal && !(t instanceof StackOverflowError)) throw fatal;
	}

	/** Plus d'opérations ou plus de mémoire : le code n'est pas allé au bout. */
	public static boolean isOutOfBudget(LeekRunException e) {
		return e.getError() == Error.TOO_MUCH_OPERATIONS || e.getError() == Error.OUT_OF_MEMORY;
	}

	/** Une erreur du joueur peut arriver emballée (fonction appelée par une native, réflexion). */
	private static Throwable unwrap(Throwable t) {
		while (!(t instanceof LeekRunException) && !(t instanceof StackOverflowError) && t.getCause() != null && t.getCause() != t) t = t.getCause();
		return t;
	}

	/** L'erreur (déballée) qui a arrêté un test, en texte : son type, ses paramètres, où elle a eu lieu. Null si le test s'est arrêté normalement. */
	private static String describe(AI ai, Throwable t) {
		if (t instanceof StackOverflowError) return Error.STACKOVERFLOW.name();
		// L'entité du test est morte pendant son tour : le test s'arrête là, sans erreur
		if (t instanceof LeekRunException e && e.getError() == Error.ENTITY_DIED) return null;
		var sb = new StringBuilder();
		try {
			if (t instanceof LeekRunException e) {
				sb.append(e.getError().name());
				if (e.getParameters() != null) {
					for (var parameter : e.getParameters()) sb.append(' ').append(parameter);
				}
			} else {
				var error = ai.throwableToError(t);
				sb.append(error.type.name());
				if (error.parameters != null) {
					for (var parameter : error.parameters) sb.append(' ').append(ai.string(parameter));
				}
			}
			var where = ai.getErrorMessage(t);
			if (!where.isEmpty()) sb.append('\n').append(where.strip());
		} catch (Throwable e) {
			if (sb.length() == 0) sb.append(t.toString());
		}
		return sb.toString();
	}

	/** Lance tous les tests d'un module hors de tout combat. */
	public static List<TestResult> runModule(AIFile file, Options options) throws Exception {
		var results = new ArrayList<TestResult>();
		// L'IA compilée pour lire la table sert au premier test lancé ; chaque suivant a la sienne
		var ai = file.compile(options);
		var table = TestCase.table(ai);
		for (var test : table) {
			if (!test.isTest()) continue;
			if (test.skip() == null) {
				if (ai == null) ai = file.compile(options);
				ai.init();
				ai.staticInit();
				ai.resetCounter();
				ai.setMaxOperations(test.maxOperations());
			}
			var logs = new ArrayList<String>();
			if (ai != null) ai.getLogs().setStream(a -> logs.add(a.toString()));
			results.add(run(ai, new TestRun(file.getPath(), table, test, logs)));
			if (test.skip() == null) ai = null;
		}
		return results;
	}
}
