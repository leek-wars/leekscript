package leekscript.compiler;

import leekscript.runner.Session;

public record Options(int version, boolean strict, boolean useCache, boolean enableOperations, Session session, boolean useExtra) {

	public Options() {
		this(LeekScript.LATEST_VERSION, false, false, false, null, true);
	}
	public Options(boolean operations) {
		this(LeekScript.LATEST_VERSION, false, false, operations, null, true);
	}
	public Options(Session session) {
		this(LeekScript.LATEST_VERSION, false, true, true, session, true);
	}
}
