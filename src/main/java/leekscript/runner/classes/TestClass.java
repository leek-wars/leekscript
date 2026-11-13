package leekscript.runner.classes;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.testing.TestRecorder;

/**
 * Asserts du framework de test : `assert`, `assertEquals`, `assertNear`, `assertError`, `fail`.
 * Chacun rend son verdict et n'arrête pas le test. Hors d'un lancement de tests (pas de
 * {@link TestRecorder} sur l'IA), un assert raté écrit une erreur au journal.
 */
public class TestClass {

	private static boolean record(AI ai, boolean ok, Object message, String expected, String actual) throws LeekRunException {
		var text = message == null ? null : ai.string(message);
		var recorder = ai.getTestRecorder();
		if (recorder != null) return recorder.check(ai, ok, text, expected, actual);
		if (!ok) {
			var line = new StringBuilder("assert");
			if (text != null) line.append(" : ").append(text);
			if (expected != null) line.append(" (attendu ").append(expected).append(", obtenu ").append(actual).append(")");
			ai.getLogs().addLog(AILog.ERROR, line.toString());
		}
		return ok;
	}

	// `assert` est un mot réservé de Java : la fonction LeekScript `assert` appelle `check`
	public static boolean check(AI ai, Object condition) throws LeekRunException {
		return check(ai, condition, null);
	}

	public static boolean check(AI ai, Object condition, Object message) throws LeekRunException {
		return record(ai, ai.bool(condition), message, null, null);
	}

	public static boolean assertEquals(AI ai, Object expected, Object actual) throws LeekRunException {
		return assertEquals(ai, expected, actual, null);
	}

	public static boolean assertEquals(AI ai, Object expected, Object actual, Object message) throws LeekRunException {
		boolean ok = TestRecorder.same(ai, expected, actual);
		return record(ai, ok, message, ok ? null : ai.export(expected), ok ? null : ai.export(actual));
	}

	public static boolean assertNear(AI ai, Object expected, Object actual, Object delta) throws LeekRunException {
		return assertNear(ai, expected, actual, delta, null);
	}

	public static boolean assertNear(AI ai, Object expected, Object actual, Object delta, Object message) throws LeekRunException {
		boolean ok = expected instanceof Number e && actual instanceof Number a && delta instanceof Number d
			&& Math.abs(e.doubleValue() - a.doubleValue()) <= d.doubleValue();
		return record(ai, ok, message, ok ? null : ai.export(expected) + " ± " + ai.export(delta), ok ? null : ai.export(actual));
	}

	public static boolean assertError(AI ai, Object function) throws LeekRunException {
		return assertError(ai, function, null);
	}

	/**
	 * La fonction doit provoquer une erreur d'exécution : une erreur qui arrête le code, ou une
	 * erreur que le moteur journalise en continuant (lecture hors bornes). Hors d'un lancement de
	 * tests rien n'est rattrapé : ce n'est pas un try/catch pour une IA de combat.
	 */
	public static boolean assertError(AI ai, Object function, Object message) throws LeekRunException {
		if (!(function instanceof FunctionLeekValue f)) {
			return record(ai, false, message, "une fonction", ai.export(function));
		}
		var recorder = ai.getTestRecorder();
		if (recorder == null) {
			f.run(ai, null);
			return record(ai, false, message, "une erreur", "aucune erreur");
		}
		int before = recorder.expectedErrorsSeen();
		recorder.beginExpectedError();
		boolean thrown = false;
		try {
			f.run(ai, null);
		} catch (LeekRunException e) {
			if (stopsTheTest(e)) throw e;
			thrown = true;
		} catch (RuntimeException e) {
			// Une erreur du joueur arrive parfois emballée (fonction appelée par une native)
			if (e.getCause() instanceof LeekRunException cause && stopsTheTest(cause)) throw cause;
			thrown = true;
		} finally {
			recorder.endExpectedError();
		}
		boolean ok = thrown || recorder.expectedErrorsSeen() > before;
		return record(ai, ok, message, ok ? null : "une erreur", ok ? null : "aucune erreur");
	}

	/** Plus d'opérations, plus de mémoire, entité morte : ce n'est pas l'erreur attendue, le tour s'arrête. */
	private static boolean stopsTheTest(LeekRunException e) {
		return e.getError() == leekscript.common.Error.TOO_MUCH_OPERATIONS || e.getError() == leekscript.common.Error.OUT_OF_MEMORY
			|| e.getError() == leekscript.common.Error.ENTITY_DIED;
	}

	public static boolean fail(AI ai) throws LeekRunException {
		return fail(ai, null);
	}

	public static boolean fail(AI ai, Object message) throws LeekRunException {
		return record(ai, false, message, null, null);
	}
}
