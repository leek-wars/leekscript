package leekscript.runner.classes;

import leekscript.runner.AI;

public class NumberClass {

	public static long abs(AI ai, long x) {
		return Math.abs(x);
	}

	public static double abs(AI ai, double x) {
		return Math.abs(x);
	}

	public static long min(AI ai, long x, long y) {
		return Math.min(x, y);
	}

	public static double min(AI ai, double x, double y) {
		return Math.min(x, y);
	}

	public static long max(AI ai, long x, long y) {
		return Math.max(x, y);
	}

	public static double max(AI ai, double x, double y) {
		return Math.max(x, y);
	}

	public static double cos(AI ai, double x) {
		return Math.cos(x);
	}

	public static double acos(AI ai, double x) {
		return Math.acos(x);
	}

	public static double sin(AI ai, double x) {
		return Math.sin(x);
	}

	public static double asin(AI ai, double x) {
		return Math.asin(x);
	}

	public static double tan(AI ai, double x) {
		return Math.tan(x);
	}

	public static double atan(AI ai, double x) {
		return Math.atan(x);
	}

	public static double atan2(AI ai, double y, double x) {
		return Math.atan2(y, x);
	}

	public static double toRadians(AI ai, double x) {
		return x * Math.PI / 180;
	}

	public static double toDegrees(AI ai, double x) {
		return x * 180 / Math.PI;
	}

	public static long ceil(AI ai, long x) {
		return x;
	}

	public static long ceil(AI ai, double x) {
		return (long) Math.ceil(x);
	}

	public static long floor(AI ai, long x) {
		return x;
	}

	public static long floor(AI ai, double x) {
		return (long) Math.floor(x);
	}

	public static long round(AI ai, long x) {
		return x;
	}

	public static long round(AI ai, double x) {
		return Math.round(x);
	}

	public static double sqrt(AI ai, double x) {
		return Math.sqrt(x);
	}

	public static double cbrt(AI ai, double x) {
		return Math.cbrt(x);
	}

	public static double log(AI ai, double x) {
		return Math.log(x);
	}

	public static double log2(AI ai, double x) {
		return Math.log(x) / Math.log(2);
	}

	public static double log10(AI ai, double x) {
		return Math.log10(x);
	}

	public static double exp(AI ai, double x) {
		return Math.exp(x);
	}

	public static double pow(AI ai, double x, double y) {
		return Math.pow(x, y);
	}

	public static double rand(AI ai) {
		return ai.getRandom().getDouble();
	}

	public static long randInt(AI ai, long a, long b) {
		if (a > b)
			return (long) ai.getRandom().getInt((int) a, (int) b - 1);
		else
			return (long) ai.getRandom().getInt((int) a, (int) b - 1);
	}

	public static double randFloat(AI ai, double a, double b) {
		if (a > b)
			return b + ai.getRandom().getDouble() * (a - b);
		else
			return a + ai.getRandom().getDouble() * (b - a);
	}

	public static double randReal(AI ai, double a, double b) {
		if (a > b)
			return b + ai.getRandom().getDouble() * (a - b);
		else
			return a + ai.getRandom().getDouble() * (b - a);
	}

	public static double hypot(AI ai, double x, double y) {
		return Math.hypot(x, y);
	}

	public static long signum(AI ai, double x) {
		return (long) Math.signum(x);
	}

	public static long bitCount(AI ai, long x) {
		return Long.bitCount(x);
	}

	public static long trailingZeros(AI ai, long x) {
		return Long.numberOfTrailingZeros(x);
	}

	public static long leadingZeros(AI ai, long x) {
		return Long.numberOfLeadingZeros(x);
	}

	public static long bitReverse(AI ai, long x) {
		return Long.reverse(x);
	}

	public static long byteReverse(AI ai, long x) {
		return Long.reverseBytes(x);
	}

	public static long rotateLeft(AI ai, long x, long y) {
		return Long.rotateLeft(x, (int) y);
	}

	public static long rotateRight(AI ai, long x, long y) {
		return Long.rotateRight(x, (int) y);
	}

	public static String binString(AI ai, long x) {
		return Long.toBinaryString(x);
	}

	public static String hexString(AI ai, long x) {
		return Long.toHexString(x);
	}

	public static long realBits(AI ai, double x) {
		return Double.doubleToRawLongBits(x);
	}

	public static double bitsToReal(AI ai, long x) {
		return Double.longBitsToDouble(x);
	}

	public static boolean isFinite(AI ai, double x) {
		return Double.isFinite(x);
	}

	public static boolean isInfinite(AI ai, double x) {
		return Double.isInfinite(x);
	}

	public static boolean isNaN(AI ai, double x) {
		return Double.isNaN(x);
	}

	public static boolean isPermutation(AI ai, long x, long y) {
		var c = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		while (x != 0) { c[(int) (x % 10)]++; x /= 10; }
		while (y != 0) { c[(int) (y % 10)]--; y /= 10; }
		int res = 1;
		for (int i = 0; i < 10; i++) res &= (c[i] == 0 ? 1 : 0);
		return res != 0;
	}
}
