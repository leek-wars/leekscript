package leekscript;

import com.alibaba.fastjson.JSONArray;

public class AILog {

	public final static int STANDARD = 1;
	public final static int WARNING = 2;
	public final static int ERROR = 3;
	public final static int SSTANDARD = 6;
	public final static int SWARNING = 7;
	public final static int SERROR = 8;

	public interface Stream {
		public void write(JSONArray a);
	}

	private int mSize = 0;
	private final static int MAX_LENGTH = 500000;
	protected Stream stream;

	public AILog() {
		this.stream = new Stream() {
			@Override
			public void write(JSONArray a) {
				System.out.println(a.toString());
			}
		};
	}

	public void addSystemLog(int type, String trace, int key, String[] parameters) {

		int paramSize = 0;
		if (parameters != null) {
			for (String p : parameters) {
				paramSize += p.length();
			}
		}

		if (!addSize(20 + trace.length() + paramSize)) {
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

	public boolean addSize(int size) {
		if (mSize + size > MAX_LENGTH) {
			return false;
		}
		mSize += size;
		return true;
	}
}
