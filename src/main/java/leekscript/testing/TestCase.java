package leekscript.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import leekscript.runner.AI;
import leekscript.util.Json;

/**
 * Une fonction de la table des tests d'un module : un test (`@test`), ou une préparation
 * (`@before`) ou une clôture (`@after`) lancée autour de chaque test du module.
 *
 * @param index position dans la table, celle qu'attend {@link AI#callTestFunction}
 * @param skip raison d'un `@skip`, null si le test est lancé
 * @param fails raison d'un `@fails` (échec attendu), null sinon
 * @param options arguments nommés de `@test(…)` : `ops`, `scenario`…
 */
public record TestCase(int index, String name, int line, Kind kind, String skip, String fails, Map<String, Object> options) {

	public enum Kind { TEST, BEFORE, AFTER }

	/** La table des tests d'une IA, vide si elle n'est pas un module de test. */
	public static List<TestCase> table(AI ai) {
		var cases = new ArrayList<TestCase>();
		var json = ai.getTestTable();
		if (json == null) return cases;
		var array = Json.parseArray(json);
		for (int i = 0; i < array.size(); i++) {
			var node = array.get(i);
			var options = new LinkedHashMap<String, Object>();
			if (node.has("options")) {
				for (var option : node.get("options").properties()) {
					var value = option.getValue();
					options.put(option.getKey(), value.isString() ? value.asString() : value.isBoolean() ? (Object) value.asBoolean() : value.isIntegralNumber() ? (Object) value.asLong() : (Object) value.asDouble());
				}
			}
			cases.add(new TestCase(i, node.get("name").asString(), node.get("line").asInt(),
				Kind.valueOf(node.get("kind").asString().toUpperCase()),
				node.has("skip") ? node.get("skip").asString() : null,
				node.has("fails") ? node.get("fails").asString() : null,
				options));
		}
		return cases;
	}

	public boolean isTest() {
		return kind == Kind.TEST;
	}

	/** Plafond d'opérations demandé par `@test(ops: …)`, ou `byDefault`. */
	public long maxOperations(long byDefault) {
		return options.get("ops") instanceof Number n && n.longValue() > 0 ? n.longValue() : byDefault;
	}
}
