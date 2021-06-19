package leekscript.common;

public enum AccessLevel {
	PUBLIC, PROTECTED, PRIVATE;

	public static AccessLevel fromString(String word) {
		switch (word) {
			case "public": return AccessLevel.PUBLIC;
			case "protected": return AccessLevel.PROTECTED;
			case "private": return AccessLevel.PRIVATE;
		}
		return AccessLevel.PUBLIC;
	}
}