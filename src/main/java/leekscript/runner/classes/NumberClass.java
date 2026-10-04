package leekscript.runner.classes;

import java.math.BigDecimal;
import java.math.RoundingMode;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.BigIntegerValue;

public class NumberClass {

	public static long abs(AI ai, long x) {
		return Math.abs(x);
	}

	public static double abs(AI ai, double x) {
		return Math.abs(x);
	}

	public static BigIntegerValue abs(AI ai, BigIntegerValue x) throws LeekRunException {
		return x.abs();
	}

	public static long min(AI ai, long x, long y) {
		return Math.min(x, y);
	}

	public static double min(AI ai, double x, double y) {
		return Math.min(x, y);
	}

	public static BigIntegerValue min(AI ai, BigIntegerValue x, BigIntegerValue y) throws LeekRunException {
		return x.min(y);
	}

	public static long max(AI ai, long x, long y) {
		return Math.max(x, y);
	}

	public static double max(AI ai, double x, double y) {
		return Math.max(x, y);
	}

	public static BigIntegerValue max(AI ai, BigIntegerValue x, BigIntegerValue y) throws LeekRunException {
		return x.max(y);
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
		return Math.toRadians(x);
	}

	public static double toDegrees(AI ai, double x) {
		return Math.toDegrees(x);
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

	public static double sqrt(AI ai, Number x) {
		return Math.sqrt(x.doubleValue());
	}

	public static double cbrt(AI ai, double x) {
		return Math.cbrt(x);
	}

	public static double log(AI ai, double x) {
		return Math.log(x);
	}

	private static final double LN2 = Math.log(2);

	public static double log2(AI ai, double x) {
		return Math.log(x) / LN2;
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

	public static BigIntegerValue pow(AI ai, BigIntegerValue x, BigIntegerValue y) throws LeekRunException {
		long exp = y.longValue();
		if (exp < 0) return BigIntegerValue.valueOf(ai, 0L);
		return x.pow((int) Math.min(exp, Integer.MAX_VALUE));
	}

	public static double rand(AI ai) {
		return ai.getRandom().getDouble();
	}

	public static long randInt(AI ai, long a, long b) {
		if (a > b)
			return (long) ai.getRandom().getInt((int) b, (int) a - 1);
		else
			return (long) ai.getRandom().getInt((int) a, (int) b - 1);
	}

	public static double randFloat(AI ai, double a, double b) {
		return randReal(ai, a, b);
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

	public static long signum(AI ai, long x) {
		return Long.signum(x);
	}

	public static long signum(AI ai, double x) {
		return (long) Math.signum(x);
	}

	public static long signum(AI ai, BigIntegerValue x) {
		return x.signum();
	}

	public static long bitCount(AI ai, long x) {
		return Long.bitCount(x);
	}

	public static long bitCount(AI ai, BigIntegerValue x) throws LeekRunException {
		ai.ops(1 + x.bitLength() / 256); // O(n) sur le nombre de mots, comme xor
		return x.bitCount();
	}

	public static long trailingZeros(AI ai, long x) {
		return Long.numberOfTrailingZeros(x);
	}

	public static long trailingZeros(AI ai, BigIntegerValue x) throws LeekRunException {
		ai.ops(1 + x.bitLength() / 512); // scan des mots de poids faible, comme or
		return x.getLowestSetBit();
	}

	public static long bitLength(AI ai, long x) {
		return 64 - Long.numberOfLeadingZeros(x);
	}

	public static long bitLength(AI ai, BigIntegerValue x) {
		return x.bitLength();
	}

	// setBit/testBit sur long : `1L << pos` (et non `1 << pos`) pour ne pas déborder à pos >= 31.
	public static long setBit(AI ai, long x, long pos, boolean val) {
		return val ? (x | (1L << pos)) : (x & ~(1L << pos));
	}

	public static long setBit(AI ai, long x, long pos, long val) {
		return setBit(ai, x, pos, val != 0);
	}

	public static long setBit(AI ai, long x, long pos) {
		return setBit(ai, x, pos, true);
	}

	public static BigIntegerValue setBit(AI ai, BigIntegerValue x, long pos, boolean val) throws LeekRunException {
		ai.ops(1 + x.bitLength() / 128); // reconstruit le tableau de mots, comme and
		return x.setBit((int) pos, val);
	}

	public static BigIntegerValue setBit(AI ai, BigIntegerValue x, long pos, long val) throws LeekRunException {
		return x.setBit((int) pos, val != 0);
	}

	public static BigIntegerValue setBit(AI ai, BigIntegerValue x, long pos) throws LeekRunException {
		return x.setBit((int) pos, true);
	}

	public static boolean testBit(AI ai, long x, long pos) {
		return (x & (1L << pos)) != 0;
	}

	public static boolean testBit(AI ai, BigIntegerValue x, long pos) {
		return x.testBit((int) pos);
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

	public static String binString(AI ai, BigIntegerValue x) throws LeekRunException {
		return x.toString(2);
	}

	public static String hexString(AI ai, long x) {
		return Long.toHexString(x);
	}

	public static String hexString(AI ai, BigIntegerValue x) throws LeekRunException {
		return x.toString(16);
	}

	// Arrondi sur l'écriture la plus courte du réel, celle de string() : toFixed(1.005, 2) vaut
	// "1.01" (JS donne "1.00"). Décimales bornées à 0..100, jamais d'exposant ni de "-0.00".
	public static String toFixed(AI ai, double x, long digits) throws LeekRunException {
		if (Double.isNaN(x)) return "NaN";
		if (Double.isInfinite(x)) return x > 0 ? "∞" : "-∞";
		int scale = fixedScale(digits);
		var decimal = BigDecimal.valueOf(x);
		// N'arrondir que s'il y a des chiffres en trop : un setScale qui ajoute des zéros multiplie la
		// mantisse (par 10^400 pour toFixed(1e300, 100)), alors que les zéros s'écrivent tout de suite
		if (decimal.scale() > scale) decimal = decimal.setScale(scale, RoundingMode.HALF_UP);
		return withDecimals(ai, decimal.toPlainString(), Math.max(0, decimal.scale()), scale);
	}

	// Entiers : exacts au-delà de 2^53, où un passage par le réel perdrait des chiffres
	public static String toFixed(AI ai, long x, long digits) throws LeekRunException {
		return withDecimals(ai, String.valueOf(x), 0, fixedScale(digits));
	}

	public static String toFixed(AI ai, BigIntegerValue x, long digits) throws LeekRunException {
		return withDecimals(ai, x.toString(10), 0, fixedScale(digits));
	}

	// Complète `plain`, qui porte `decimals` chiffres après le point, jusqu'à `scale` chiffres
	private static String withDecimals(AI ai, String plain, int decimals, int scale) throws LeekRunException {
		var result = decimals >= scale ? plain : plain + (decimals == 0 ? "." : "") + "0".repeat(scale - decimals);
		ai.ops(result.length() / 3);
		return result;
	}

	private static int fixedScale(long digits) {
		return (int) Math.max(0, Math.min(100, digits));
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
		// Le reste d'un négatif est négatif : on compte les chiffres sans le signe
		// (Math.abs sur le reste, pas sur x, qui déborderait pour Long.MIN_VALUE)
		while (x != 0) { c[(int) Math.abs(x % 10)]++; x /= 10; }
		while (y != 0) { c[(int) Math.abs(y % 10)]--; y /= 10; }
		int res = 1;
		for (int i = 0; i < 10; i++) res &= (c[i] == 0 ? 1 : 0);
		return res != 0;
	}
}
