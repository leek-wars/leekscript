package leekscript.common;

public enum Annotation {
	UNUSED, DEPRECATED, PURE, NODISCARD, OVERRIDE, TAILREC, TODO,
	// Framework de test (modules `*.test.leek`)
	TEST, SKIP, FAILS, BEFORE, AFTER;

	public static Annotation fromString(String name) {
		return switch (name) {
			case "unused" -> UNUSED;
			case "deprecated" -> DEPRECATED;
			case "pure" -> PURE;
			case "nodiscard" -> NODISCARD;
			case "override" -> OVERRIDE;
			case "tailrec" -> TAILREC;
			case "todo" -> TODO;
			case "test" -> TEST;
			case "skip" -> SKIP;
			case "fails" -> FAILS;
			case "before" -> BEFORE;
			case "after" -> AFTER;
			default -> null;
		};
	}

	/** Annotation du framework de test : admise seulement dans un module de test. */
	public boolean isTestAnnotation() {
		return this == TEST || this == SKIP || this == FAILS || this == BEFORE || this == AFTER;
	}
}
