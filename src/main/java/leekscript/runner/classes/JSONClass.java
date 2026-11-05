package leekscript.runner.classes;

import leekscript.util.Json;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekValueManager;

public class JSONClass {

	public static String jsonEncode(AI ai, Object value) {

		try {

			String json = Json.toJson(ai.toJSON(value), AI.MAX_STRING_LENGTH);
			if (json == null) return encodeFailed(ai, "Cannot encode object: JSON longer than " + AI.MAX_STRING_LENGTH + " characters");
			ai.ops((long) json.length() * 10);
			return json;

		} catch (Exception e) {

			// Pas de value.toString() : sans borne de longueur, il peut lui-même épuiser la mémoire
			String preview = value == null || value instanceof String || value instanceof Number || value instanceof Boolean ? String.valueOf(value) : value.getClass().getSimpleName();
			if (preview.length() > 200) preview = preview.substring(0, 200) + "…";
			return encodeFailed(ai, "Cannot encode object \"" + preview + "\"");
		}
	}

	private static String encodeFailed(AI ai, String message) {
		ai.getLogs().addLog(AILog.ERROR, message);
		try {
			ai.ops(100);
		} catch (Exception e1) {}
		return "";
	}

	public static Object jsonDecode(AI ai, String json) {

		try {

			var obj = LeekValueManager.parseJSON(Json.parse(json), ai);
			ai.ops((long) json.length() * 10);
			return obj;

		} catch (Throwable e) {
			// Catch Throwable (not just Exception) so StackOverflowError or OOM from a
			// hostile payload doesn't kill the worker thread. Truncate the echoed json
			// to avoid re-allocating multi-MB strings when recovering from OOM.
			String preview = json.length() > 200 ? json.substring(0, 200) + "…" : json;
			ai.getLogs().addLog(AILog.ERROR, "Cannot parse json \"" + preview + "\"");
			try {
				ai.ops(100);
			} catch (Exception e1) {}
			return null;
		}
	}
}
