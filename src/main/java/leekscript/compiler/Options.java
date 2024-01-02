package leekscript.compiler;

import leekscript.runner.Session;

public record Options(int version, boolean strict, boolean useCache, boolean enableOperations, Session session) {

	public Options() {
		this(LeekScript.LATEST_VERSION, false, false, false, null);
	}
	public Options(boolean operations) {
		this(LeekScript.LATEST_VERSION, false, false, operations, null);
	}
	public Options(Session session) {
		this(LeekScript.LATEST_VERSION, false, true, false, session);
	}
}
