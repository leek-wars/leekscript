package leekscript.common;

public enum Annotation {
	UNUSED, DEPRECATED, PURE, NODISCARD, OVERRIDE, TAILREC, TODO;

	public static Annotation fromString(String name) {
		return switch (name) {
			case "unused" -> UNUSED;
			case "deprecated" -> DEPRECATED;
			case "pure" -> PURE;
			case "nodiscard" -> NODISCARD;
			case "override" -> OVERRIDE;
			case "tailrec" -> TAILREC;
			case "todo" -> TODO;
			default -> null;
		};
	}
}
