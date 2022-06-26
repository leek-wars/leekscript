package leekscript.runner.classes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;

public class StringClass {

	public static String charAt(AI ai, String string, long index) {
		if (index < 0 || index >= string.length())
			return null;
		return String.valueOf(string.charAt((int) index));
	}

	public static long codePointAt(AI ai, String string) {
		if (string.length() == 0)
			return 0;
		return string.codePointAt(0);
	}

	public static long codePointAt(AI ai, String string, long index) {
		if (index < 0 || index >= string.length())
			return 0;
		return string.codePointAt((int) index);
	}

	public static long length(AI ai, String string) throws LeekRunException {
		ai.ops(1);
		return string.length();
	}

	public static String substring(AI ai, String string, long index) throws LeekRunException {
		ai.ops(1 + Math.max(0, string.length() - (int) index) / 10);
		if (string.length() <= index || index < 0) {
			return null;
		}
		return string.substring((int) index, string.length());
	}

	public static String substring(AI ai, String string, long index, long length) throws LeekRunException {
		ai.ops(1 + Math.max(0, (int) length) / 10);
		if (string.length() <= index || index < 0 || index + length > string.length() || length < 0) {
			return null;
		}
		return string.substring((int) index, (int) index + (int) length);
	}

	public static String replace(AI ai, String string, String search, String replace) throws LeekRunException {
		ai.ops(Math.max(1, string.length() * 2));
		return string.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(replace));
	}

	public static long indexOf(AI ai, String string, String needle) throws LeekRunException {
		ai.ops(1 + string.length() / 10);
		return string.indexOf(needle);
	}

	public static long indexOf(AI ai, String string, String needle, long from) throws LeekRunException {
		ai.ops(1 + string.length() / 10);
		return string.indexOf(needle, (int) from);
	}

	public static boolean contains(AI ai, String string, String needle) throws LeekRunException {
		ai.ops(1 + string.length() / 10);
		return string.contains(needle);
	}

	public static LegacyArrayLeekValue split_v1_3(AI ai, String string, String delimiter) throws LeekRunException {
		return split_v1_3(ai, string, delimiter, 0);
	}

	public static LegacyArrayLeekValue split_v1_3(AI ai, String string, String delimiter, long limit) throws LeekRunException {
		ai.ops(1 + string.length());
		var result = new LegacyArrayLeekValue();
		for (var element : string.split(Pattern.quote(delimiter), (int) limit)) {
			result.pushNoClone(ai, element);
		}
		return result;
	}

	public static ArrayLeekValue split(AI ai, String string, String delimiter) throws LeekRunException {
		return split(ai, string, delimiter, 0);
	}

	public static ArrayLeekValue split(AI ai, String string, String delimiter, long limit) throws LeekRunException {
		ai.ops(1 + string.length());
		var result = new ArrayLeekValue(ai);
		for (var element : string.split(Pattern.quote(delimiter), (int) limit)) {
			result.pushNoClone(ai, element);
		}
		return result;
	}

	public static String toLower(AI ai, String string) throws LeekRunException {
		ai.ops(1 + string.length());
		return string.toLowerCase();
	}

	public static String toUpper(AI ai, String string) throws LeekRunException {
		ai.ops(1 + string.length());
		return string.toUpperCase();
	}

	public static boolean startsWith(AI ai, String string, String prefix) throws LeekRunException {
		ai.ops(1 + string.length());
		return string.startsWith(prefix);
	}

	public static boolean endsWith(AI ai, String string, String prefix) throws LeekRunException {
		ai.ops(1 + string.length());
		return string.endsWith(prefix);
	}

	public static String trim(AI ai, String string) {
		return string.trim();
	}
}
