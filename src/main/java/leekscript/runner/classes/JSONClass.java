package leekscript.runner.classes;

import leekscript.util.Json;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekValueManager;

public class JSONClass {

	public static String jsonEncode(AI ai, Object value) {

		try {

			String json = Json.toJson(ai.toJSON(value));
			ai.ops(json.length() * 10);
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
			ai.ops(json.length() * 10);
			return obj;

		} catch (Exception e) {

			ai.getLogs().addLog(AILog.ERROR, "Cannot parse json \"" + json + "\"");
			try {
				ai.ops(100);
			} catch (Exception e1) {}
			return null;
		}
	}
}
