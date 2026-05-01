package leekscript.runner.classes;

import leekscript.util.Json;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekValueManager;

public class JSONClass {

	public static String jsonEncode(AI ai, Object value) {

		try {

			String json = Json.toJson(ai.toJSON(value));
			ai.ops((long) json.length() * 10);
			return json;

		} catch (Exception e) {

			ai.getLogs().addLog(AILog.ERROR, "Cannot encode object \"" + value.toString() + "\"");
			try {
				ai.ops(100);
			} catch (Exception e1) {}
			return "";
		}
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
