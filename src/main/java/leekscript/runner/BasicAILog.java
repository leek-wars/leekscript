package leekscript.runner;

import com.alibaba.fastjson.JSONArray;

import leekscript.AILog;

public class BasicAILog extends AILog {

	protected Stream stream;

	public BasicAILog() {
		this.stream = new Stream() {
			@Override
			public void write(JSONArray a) {
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

		JSONArray obj = new JSONArray();
		obj.add(0);
		obj.add(type);
		obj.add(trace);
		obj.add(key);
		if (parametersString != null)
			obj.add(parametersString);

		stream.write(obj);
	}

	public void addLog(int type, String message) {
		message = message.replaceAll("\t", "    ");
		addLog(type, message, 0);
	}

	public void addLog(int type, String message, int color) {

		if (message == null || !addSize(20 + message.length())) {
			return;
		}
		JSONArray obj = new JSONArray();
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

		JSONArray obj = new JSONArray();
		obj.add(0);
		obj.add(type);
		obj.add(trace);
		obj.add(key);
		if (parameters != null)
			obj.add(parameters);

		stream.write(obj);
	}


	@Override
	public void setStream(Stream stream) {
		this.stream = stream;
	}
}
