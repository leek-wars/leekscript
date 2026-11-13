package leekscript.testing;

import java.util.ArrayList;
import java.util.List;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.MapLeekValue;

/**
 * Ce qu'un test a vérifié pendant son exécution : le nombre d'asserts, ceux qui ont échoué (avec
 * leur ligne), et les erreurs que le moteur a journalisées sans arrêter le code. Posé sur l'IA par
 * le lanceur ({@link AI#setTestRecorder}) ; sans lui, un assert raté n'est qu'une ligne d'erreur
 * au journal.
 */
public class TestRecorder {

	/** Tolérance d'{@code assertEquals} entre deux nombres. */
	public static final double EPSILON = 1e-8;

	public record Failure(String file, int line, String message, String expected, String actual) {}

	public record LoggedError(String file, int line, int error, String[] parameters) {}

	private int asserts = 0;
	private final List<Failure> failures = new ArrayList<>();
	private final List<LoggedError> errors = new ArrayList<>();
	// Profondeur d'assertError en cours : les erreurs journalisées y sont attendues
	private int expectingError = 0;
	private int expectedErrorsSeen = 0;

	public int asserts() {
		return asserts;
	}

	public List<Failure> failures() {
		return failures;
	}

	public List<LoggedError> errors() {
		return errors;
	}

	/** Enregistre un assert ; rend son verdict. */
	public boolean check(AI ai, boolean ok, String message, String expected, String actual) {
		asserts++;
		if (!ok) {
			var position = ai.getCurrentLeekScriptPosition();
			failures.add(new Failure(position == null ? null : position.file(), position == null ? 0 : position.line(), message, expected, actual));
		}
		return ok;
	}

	/** Une erreur journalisée par le moteur (lecture hors bornes…) : le code continue, le test échoue. */
	public void loggedError(AI ai, int error, Object[] parameters) {
		if (expectingError > 0) {
			expectedErrorsSeen++;
			return;
		}
		var position = ai.getCurrentLeekScriptPosition();
		String[] texts = null;
		if (parameters != null) {
			texts = new String[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				try {
					texts[i] = ai.string(parameters[i]);
				} catch (LeekRunException e) {
					texts[i] = "?";
				}
			}
		}
		errors.add(new LoggedError(position == null ? null : position.file(), position == null ? 0 : position.line(), error, texts));
	}

	public void beginExpectedError() {
		expectingError++;
	}

	/** Fin d'un assertError : rend le nombre d'erreurs journalisées pendant l'appel. */
	public int endExpectedError(int seenBefore) {
		expectingError--;
		return expectedErrorsSeen - seenBefore;
	}

	public int expectedErrorsSeen() {
		return expectedErrorsSeen;
	}

	/**
	 * Égalité d'assertEquals. Entre deux nombres, à {@link #EPSILON} près ; un nombre n'égale qu'un
	 * nombre (ni null, ni true). Tableaux et maps comparés en profondeur avec la même règle.
	 */
	public static boolean same(AI ai, Object expected, Object actual) throws LeekRunException {
		if (expected instanceof Number || actual instanceof Number) {
			if (!(expected instanceof Number e) || !(actual instanceof Number a)) return false;
			if (e instanceof Long && a instanceof Long) return e.longValue() == a.longValue();
			if ((e instanceof Long || e instanceof Double) && (a instanceof Long || a instanceof Double)) {
				double x = e.doubleValue(), y = a.doubleValue();
				return x == y || Math.abs(x - y) < EPSILON;
			}
			return ai.eq(expected, actual);
		}
		if (expected instanceof ArrayLeekValue e && actual instanceof ArrayLeekValue a) {
			if (e.size() != a.size()) return false;
			for (int i = 0; i < e.size(); i++) {
				if (!same(ai, e.get(i), a.get(i))) return false;
			}
			return true;
		}
		if (expected instanceof MapLeekValue e && actual instanceof MapLeekValue a) {
			if (e.size() != a.size()) return false;
			for (var entry : e.entrySet()) {
				if (!a.containsKey(entry.getKey())) return false;
				if (!same(ai, entry.getValue(), a.get(entry.getKey()))) return false;
			}
			return true;
		}
		return ai.eq(expected, actual);
	}
}
