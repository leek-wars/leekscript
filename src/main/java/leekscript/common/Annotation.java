package leekscript.common;

public enum Annotation {
	UNUSED, DEPRECATED, PURE, NODISCARD, OVERRIDE, TAILREC, TODO,
	// Framework de test (modules `*.test.leek`)
	TEST, SKIP, FAILS, BEFORE, AFTER, BENCH;

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
			case "bench" -> BENCH;
			default -> null;
		};
	}

	/** Ce que le lanceur de tests appelle lui-même : un test, un bench, leur préparation, leur clôture. */
	public static final java.util.Set<Annotation> TEST_ENTRIES = java.util.Collections.unmodifiableSet(java.util.EnumSet.of(TEST, BENCH, BEFORE, AFTER));

	/** Annotation du framework de test : admise seulement dans un module de test. */
	public boolean isTestAnnotation() {
		return this == TEST || this == SKIP || this == FAILS || this == BEFORE || this == AFTER || this == BENCH;
	}
}
