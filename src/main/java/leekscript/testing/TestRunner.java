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

	/**
	 * Lance un test sur une IA neuve, prête à tourner : le code de premier niveau du module, ses
	 * `@before`, le test, ses `@after`. L'IA garde son enregistreur après l'appel.
	 */
	public static TestResult run(AI ai, String module, List<TestCase> table, TestCase test, List<String> logs) {
		if (test.skip() != null) {
			return new TestResult(module, test.name(), test.line(), TestResult.Status.SKIPPED, test.skip(), 0, 0, 0, List.of(), List.of(), null, List.of());
		}
		var recorder = new TestRecorder();
		ai.setTestRecorder(recorder);
		String error = null;
		long start = System.nanoTime();
		try {
			ai.runIA();
			for (var c : table) if (c.kind() == TestCase.Kind.BEFORE) ai.callTestFunction(c.index());
			ai.callTestFunction(test.index());
			for (var c : table) if (c.kind() == TestCase.Kind.AFTER) ai.callTestFunction(c.index());
		} catch (Throwable t) {
			error = describe(ai, t);
		}
		long duration = System.nanoTime() - start;

		TestResult.Status status;
		String reason = null;
		if (error != null || !recorder.errors().isEmpty()) {
			status = TestResult.Status.ERROR;
		} else if (!recorder.failures().isEmpty()) {
			status = TestResult.Status.FAILED;
		} else if (recorder.asserts() == 0) {
			// Un test qui ne vérifie rien ne prouve rien
			status = TestResult.Status.FAILED;
			reason = "no_assert";
		} else {
			status = TestResult.Status.PASSED;
		}
		if (test.fails() != null) {
			reason = test.fails();
			status = status.ok ? TestResult.Status.UNEXPECTED_PASS : TestResult.Status.EXPECTED_FAILURE;
		}
		return new TestResult(module, test.name(), test.line(), status, reason, ai.getOperations(), duration, recorder.asserts(),
			List.copyOf(recorder.failures()), List.copyOf(recorder.errors()), error, logs == null ? List.of() : List.copyOf(logs));
	}

	/** L'erreur qui a arrêté un test, en texte : son type, ses paramètres, où elle a eu lieu. Null si le test s'est arrêté normalement. */
	private static String describe(AI ai, Throwable t) {
		// Une erreur du joueur peut arriver emballée (fonction appelée par une native, réflexion)
		while (!(t instanceof LeekRunException) && !(t instanceof StackOverflowError) && t.getCause() != null && t.getCause() != t) t = t.getCause();
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
		var table = TestCase.table(file.compile(options));
		for (var test : table) {
			if (!test.isTest()) continue;
			var ai = file.compile(options);
			var logs = new ArrayList<String>();
			ai.getLogs().setStream(a -> logs.add(a.toString()));
			ai.init();
			ai.staticInit();
			ai.resetCounter();
			ai.setMaxOperations((int) Math.min(Integer.MAX_VALUE, test.maxOperations(DEFAULT_MAX_OPERATIONS)));
			results.add(run(ai, file.getPath(), table, test, logs));
		}
		return results;
	}
}
