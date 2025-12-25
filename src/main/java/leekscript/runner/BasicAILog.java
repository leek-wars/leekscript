package leekscript.runner;

import tools.jackson.databind.node.ArrayNode;

import leekscript.AILog;
import leekscript.util.Json;

public class BasicAILog extends AILog {

	protected Stream stream;

	public BasicAILog() {
		this.stream = new Stream() {
			@Override
			public void write(ArrayNode a) {
				System.out.println(a.toString());
			}
		};
	}


	public void addSystemLog(AI ai, int type, String trace, int key, Object[] parameters) throws LeekRunException {

		if (isFull()) return;

		if (!addSize(20 + trace.length())) {
			return;
		}

		String[] parametersString = parameters != null ? new String[parameters.length] : null;

		if (parameters != null) {
			for (int p = 0; p < parameters.length; ++p) {
				var parameterString = ai.string(parameters[p]);
				if (!addSize(parameterString.length())) {
					parametersString[p] = "[...]";
				} else {
					parametersString[p] = parameterString;
				}
			}
		}

		ArrayNode obj = Json.createArray();
		obj.add(0);
		obj.add(type);
		obj.add(trace);
		obj.add(key);
		if (parametersString != null)
			obj.addPOJO(parametersString);

		stream.write(obj);
	}

	public void addLog(int type, String message) {
		message = message.replaceAll("\t", "    ");
		addLog(type, message, 0, 0, 0);
	}

	public void addLog(int type, String message, int color) {
		addLog(type, message, color, 0, 0);
	}

	public void addLog(int type, String message, int color, int ai, int line) {

		if (message == null || !addSize(20 + message.length())) {
			return;
		}
		ArrayNode obj = Json.createArray();
		obj.add(0);
		obj.add(type);
		obj.add(message);
		if (color != 0) {
			obj.add(color);
		}
		stream.write(obj);
	}


	@Override
	public void addSystemLog(int type, String trace, int key, String[] parameters) {

		if (isFull()) return;

		int paramSize = 0;
		if (parameters != null) {
			for (String p : parameters) {
				if (p != null) {
					paramSize += p.length();
				}
			}
		}
		if (!addSize(20 + paramSize)) {
			return;
		}

		ArrayNode obj = Json.createArray();
		obj.add(0);
		obj.add(type);
		obj.add(trace);
		obj.add(key);
		if (parameters != null)
			obj.addPOJO(parameters);

		stream.write(obj);
	}


	@Override
	public void setStream(Stream stream) {
		this.stream = stream;
	}
}
