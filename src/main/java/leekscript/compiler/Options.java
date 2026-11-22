package leekscript.compiler;

import leekscript.runner.Session;

/**
 * `tests` : compilation des tests d'un module de test (lanceur de tests, analyse de l'éditeur),
 * jamais d'une IA de combat. Elle seule ouvre les dépendances de test d'un package
 * (`depends test`, cf Package#getTestDepends) à une entrée de son dossier `test/`.
 */
public record Options(int version, boolean strict, boolean useCache, boolean enableOperations, Session session, boolean useExtra, boolean tests) {

	public Options(int version, boolean strict, boolean useCache, boolean enableOperations, Session session, boolean useExtra) {
		this(version, strict, useCache, enableOperations, session, useExtra, false);
	}
	public Options() {
		this(LeekScript.LATEST_VERSION, false, false, false, null, true);
	}
	public Options(boolean operations) {
		this(LeekScript.LATEST_VERSION, false, false, operations, null, true);
	}
	public Options(Session session) {
		this(LeekScript.LATEST_VERSION, false, true, true, session, true);
	}

	/** Les mêmes options, pour compiler des tests. */
	public Options forTests() {
		return tests ? this : new Options(version, strict, useCache, enableOperations, session, useExtra, true);
	}
}
