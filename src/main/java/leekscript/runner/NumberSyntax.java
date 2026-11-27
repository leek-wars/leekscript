package leekscript.runner;

/**
 * Une chaîne se lit-elle comme un nombre ? Mêmes réponses que les analyseurs du JDK, sans exception :
 * une conversion qui échoue (`number('abc')`, `'abc' * 2`) ne coûte qu'une lecture de la chaîne, là
 * où une NumberFormatException rattrapée remplit sa pile à chaque appel.
 */
public final class NumberSyntax {

	private NumberSyntax() {}

	/** Vrai si et seulement si {@link Integer#parseInt(String)} réussit. */
	public static boolean isInt(String s) {
		return isInteger(s, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	/** Vrai si et seulement si {@link Long#parseLong(String)} réussit. */
	public static boolean isLong(String s) {
		return isInteger(s, Long.MIN_VALUE, Long.MAX_VALUE);
	}

	/** Signe facultatif puis chiffres (tout chiffre Unicode, comme le JDK), dans [min, max] : cumul en négatif, lui aussi comme le JDK. */
	private static boolean isInteger(String s, long min, long max) {
		int length = s.length();
		if (length == 0) return false;
		int i = 0;
		long limit = -max;
		char first = s.charAt(0);
		if (first == '-' || first == '+') {
			if (first == '-') limit = min;
			if (length == 1) return false;
			i++;
		}
		long multmin = limit / 10;
		long result = 0;
		while (i < length) {
			int digit = Character.digit(s.charAt(i++), 10);
			if (digit < 0 || result < multmin) return false;
			result *= 10;
			if (result < limit + digit) return false;
			result -= digit;
		}
		return true;
	}

	/**
	 * Vrai si et seulement si {@link Double#parseDouble(String)} réussit : blancs ignorés aux bouts,
	 * signe, puis NaN, Infinity, un décimal (chiffres, point, exposant) ou un hexadécimal à exposant
	 * binaire, et un suffixe de type facultatif (f, F, d, D).
	 */
	public static boolean isReal(String s) {
		int start = 0, end = s.length();
		while (start < end && s.charAt(start) <= ' ') start++;
		while (end > start && s.charAt(end - 1) <= ' ') end--;
		if (start == end) return false;
		char c = s.charAt(start);
		if (c == '+' || c == '-') {
			if (++start == end) return false;
			c = s.charAt(start);
		}
		if (c == 'N') return end - start == 3 && s.startsWith("NaN", start);
		if (c == 'I') return end - start == 8 && s.startsWith("Infinity", start);
		int i = start, digits = 0;
		boolean hexadecimal = c == '0' && end - start > 1 && (s.charAt(start + 1) == 'x' || s.charAt(start + 1) == 'X');
		if (hexadecimal) i += 2;
		while (i < end && isDigit(s.charAt(i), hexadecimal)) { i++; digits++; }
		if (i < end && s.charAt(i) == '.') {
			i++;
			while (i < end && isDigit(s.charAt(i), hexadecimal)) { i++; digits++; }
		}
		if (digits == 0) return false;
		// Exposant : obligatoire en hexadécimal (p), facultatif en décimal (e)
		boolean exponent = i < end && (hexadecimal ? s.charAt(i) == 'p' || s.charAt(i) == 'P' : s.charAt(i) == 'e' || s.charAt(i) == 'E');
		if (exponent) {
			i++;
			if (i < end && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
			int first = i;
			while (i < end && isDigit(s.charAt(i), false)) i++;
			if (i == first) return false;
		} else if (hexadecimal) {
			return false;
		}
		if (i < end && "fFdD".indexOf(s.charAt(i)) >= 0) i++;
		return i == end;
	}

	private static boolean isDigit(char c, boolean hexadecimal) {
		return (c >= '0' && c <= '9') || (hexadecimal && ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')));
	}
}
