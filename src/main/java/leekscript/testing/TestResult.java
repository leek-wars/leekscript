package leekscript.testing;

import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import leekscript.util.Json;

/**
 * Le résultat d'un test.
 *
 * @param error erreur d'exécution qui a arrêté le test (texte), null sinon
 * @param errorLine ligne de cette erreur dans le module de test, 0 si inconnue
 */
public record TestResult(String module, String name, int line, Status status, String reason, long operations, long durationNs,
		int asserts, List<TestRecorder.Failure> failures, List<TestRecorder.LoggedError> loggedErrors, String error, List<String> logs) {

	public enum Status {
		/** Tous les asserts sont passés. */
		PASSED(true),
		/** Un assert a échoué, ou le test n'en a fait aucun. */
		FAILED(false),
		/** Une erreur d'exécution : elle a arrêté le test, ou le moteur l'a journalisée. */
		ERROR(false),
		/** `@skip` : pas lancé. */
		SKIPPED(true),
		/** `@fails` : le test échoue comme prévu. */
		EXPECTED_FAILURE(true),
		/** `@fails` : le test passe alors qu'il devait échouer. */
		UNEXPECTED_PASS(false);

		public final boolean ok;

		Status(boolean ok) {
			this.ok = ok;
		}
	}

	public ObjectNode toJson() {
		var json = Json.createObject();
		json.put("module", module);
		json.put("name", name);
		json.put("line", line);
		json.put("status", status.name().toLowerCase());
		if (reason != null) json.put("reason", reason);
		json.put("operations", operations);
		json.put("duration_ms", durationNs / 1_000_000.0);
		json.put("asserts", asserts);
		if (!failures.isEmpty()) {
			var array = json.putArray("failures");
			for (var failure : failures) {
				var node = array.addObject();
				if (failure.file() != null) node.put("file", failure.file());
				node.put("line", failure.line());
				if (failure.message() != null) node.put("message", failure.message());
				if (failure.expected() != null) node.put("expected", failure.expected());
				if (failure.actual() != null) node.put("actual", failure.actual());
			}
		}
		if (!loggedErrors.isEmpty()) {
			var array = json.putArray("logged_errors");
			for (var logged : loggedErrors) {
				var node = array.addObject();
				if (logged.file() != null) node.put("file", logged.file());
				node.put("line", logged.line());
				node.put("error", logged.error());
				if (logged.parameters() != null) {
					var parameters = node.putArray("parameters");
					for (var parameter : logged.parameters()) parameters.add(parameter);
				}
			}
		}
		if (error != null) json.put("error", error);
		if (!logs.isEmpty()) {
			var array = json.putArray("logs");
			for (var log : logs) array.add(log);
		}
		return json;
	}
}
