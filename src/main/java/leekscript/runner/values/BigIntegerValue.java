package leekscript.runner.values;

import java.math.BigInteger;
import java.util.Set;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

/**
 * Represents an integer number with size limited only by memory and operations
 * cost. Current implementation is a wrapper on BigInteger. It allows easy swap
 * for other versions. Untested mutable version to be considered (although it does not have built-in processor optimizations) :
 * https://github.com/bwakell/Huldra/blob/master/src/main/java/org/huldra/math/BigInt.java
 * 
 * @see BigInteger
 */
public class BigIntegerValue extends Number implements LeekValue {

	private final BigInteger value;
	private final AI ai;
	
	private final static int STRING_CROP_LIMIT = 10; // number of digits to keep at the beginning and the end when converting to string
	private final static BigInteger STRING_CROP_VALUE = BigInteger.TEN.pow(STRING_CROP_LIMIT);
	private final static double BASE_FACTOR = Math.log10(2); // used to convert from base 2 to 10
	private final static double BIT_LIMIT = STRING_CROP_LIMIT * 2 / BASE_FACTOR; // number of bits (base 2) corresponding to crop limit (base 10)

	public BigIntegerValue(AI ai, String val, int radix) throws LeekRunException {
		this.ai = ai;
		this.value = new BigInteger(val, radix);
		ops(4);
		int ram = value.bitLength() / 64;
		ai.allocateRAM(this, ram);
	}

	public BigIntegerValue(AI ai, String val) throws LeekRunException {
		this.ai = ai;
		this.value = new BigInteger(val);
		ops(4);
		int ram = value.bitLength() / 64;
		ai.allocateRAM(this, ram);
	}

	public BigIntegerValue(AI ai, double val) throws LeekRunException {
		this.ai = ai;
		this.value = BigInteger.valueOf((long) val);
		ops(4);
		int ram = value.bitLength() / 64;
		ai.allocateRAM(this, ram);
	}

	public BigIntegerValue(AI ai, long val) throws LeekRunException {
		this.ai = ai;
		this.value = BigInteger.valueOf(val);
		ops(4);
		int ram = value.bitLength() / 64;
		ai.allocateRAM(this, ram);
	}

	public BigIntegerValue(AI ai, BigInteger val) throws LeekRunException {
		this.ai = ai;
		this.value = val;
		ops(4);
		int ram = value.bitLength() / 64;
		ai.allocateRAM(this, ram);
	}

	// benchmarks :
	// nothing : 20s
	// finalize : 60s
	// phantomRef : 25s
	// cleaner : 40s
	// cleaner + autocloseable : 45s
	// weak reference : 25s

	public BigIntegerValue add(BigIntegerValue val) throws LeekRunException {
		ops();
		val.ops();
		return new BigIntegerValue(ai, value.add(val.value));
	}

	public BigIntegerValue subtract(BigIntegerValue val) throws LeekRunException {
		ops();
		val.ops();
		return new BigIntegerValue(ai, value.subtract(val.value));
	}

	public BigIntegerValue multiply(BigIntegerValue val) throws LeekRunException {
		if (value.bitLength() < 5000) {
			ops(value.bitLength() / 100);
		} else {
			ops(value.bitLength() / 50);
		}

		if (val.value.bitLength() < 5000) {
			val.ops(val.value.bitLength() / 100);
		} else {
			val.ops(val.value.bitLength() / 50);
		}
		return new BigIntegerValue(ai, value.multiply(val.value));
	}

	public BigIntegerValue divide(BigIntegerValue val) throws LeekRunException {
		ops(15);
		val.ops();
		return new BigIntegerValue(ai, value.divide(val.value));
	}

	public BigIntegerValue pow(int exponent) throws LeekRunException {
		ops(value.bitLength() / 2000 * exponent);
		return new BigIntegerValue(ai, value.pow(exponent));
	}

	public BigIntegerValue abs() throws LeekRunException {
		return new BigIntegerValue(ai, value.abs());
	}

	public BigIntegerValue negate() throws LeekRunException {
		return new BigIntegerValue(ai, value.negate());
	}

	public BigIntegerValue mod(BigIntegerValue m) throws LeekRunException {
		ops(value.bitLength() / 50);
		m.ops();
		return new BigIntegerValue(ai, value.mod(m.value));
	}

	public BigIntegerValue shiftLeft(int n) throws LeekRunException {
		binaryShiftOps(n);
		return new BigIntegerValue(ai, value.shiftLeft(n));
	}

	public BigIntegerValue shiftRight(int n) throws LeekRunException {
		binaryShiftOps(n);
		return new BigIntegerValue(ai, value.shiftRight(n));
	}

	public BigIntegerValue and(BigIntegerValue val) throws LeekRunException {
		ops(value.bitLength() / 128);
		val.ops(val.value.bitLength() / 128);
		return new BigIntegerValue(ai, value.and(val.value));
	}

	public BigIntegerValue or(BigIntegerValue val) throws LeekRunException {
		ops(value.bitLength() / 512);
		val.ops(val.value.bitLength() / 512);
		return new BigIntegerValue(ai, value.or(val.value));
	}

	public BigIntegerValue xor(BigIntegerValue val) throws LeekRunException {
		ops(value.bitLength() / 256);
		val.ops(val.value.bitLength() / 256);
		return new BigIntegerValue(ai, value.xor(val.value));
	}

	public BigIntegerValue not() throws LeekRunException {
		ops(value.bitLength() / 256);
		return new BigIntegerValue(ai, value.not());
	}

	public BigIntegerValue setBit(int n, boolean val) throws LeekRunException {
		if (val) {
			return new BigIntegerValue(ai, value.setBit(n));
		} else {
			return new BigIntegerValue(ai, value.clearBit(n));
		}
	}

	public boolean testBit(int n) {
		return value.testBit(n);
	}

	public BigIntegerValue min(BigIntegerValue val) throws LeekRunException {
		return new BigIntegerValue(ai, value.min(val.value));
	}

	public BigIntegerValue max(BigIntegerValue val) throws LeekRunException {
		return new BigIntegerValue(ai, value.max(val.value));
	}

	public static BigIntegerValue valueOf(AI ai, String val) throws LeekRunException {
		return new BigIntegerValue(ai, val);
	}

	public static BigIntegerValue valueOf(AI ai, double val) throws LeekRunException {
		return new BigIntegerValue(ai, val);
	}

	public static BigIntegerValue valueOf(AI ai, long val) throws LeekRunException {
		return new BigIntegerValue(ai, val);
	}

	public static BigIntegerValue valueOf(AI ai, BigInteger val) throws LeekRunException {
		return new BigIntegerValue(ai, val);
	}

	public static BigIntegerValue valueOf(AI ai, BigIntegerValue val) {
		return val; // BigInteger is not mutable so this is safe
	}

	public static BigIntegerValue valueOf(AI ai, Object val) throws LeekRunException {
		if (val instanceof Double)
			return new BigIntegerValue(ai, (Double) val);
		if (val instanceof Long)
			return new BigIntegerValue(ai, (Long) val);
		if (val instanceof BigIntegerValue)
			return (BigIntegerValue) val;
		if (val instanceof String)
			return new BigIntegerValue(ai, (String) val);
		ai.getLogs().addLog(AILog.ERROR, "Cannot cast \"" + val.toString() + "\" to BigInteger");
		return new BigIntegerValue(ai, 0);
	}

	public int signum() {
		return value.signum();
	}

	public long bitLength() {
		return value.bitLength();
	}

	public long bitCount() {
		return value.bitCount();
	}

	public long getLowestSetBit() {
		return value.getLowestSetBit();
	}

	public int compareTo(BigIntegerValue y) {
		return value.compareTo(y.value);
	}

	@Override
	public int intValue() {
		return value.intValue();
	}

	@Override
	public long longValue() {
		return value.longValue();
	}

	@Override
	public float floatValue() {
		return value.floatValue();
	}

	@Override
	public double doubleValue() {
		return value.doubleValue();
	}

	public boolean isZero() {
		return value.equals(BigInteger.ZERO);
	}

	public String toString() {
		ai.opsNoCheck(10);
		if (value.bitLength() <= BIT_LIMIT) {
			return value.toString();
		} else {
			ai.opsNoCheck(100);
			// print only first and last digits to avoid overflow and slow full String response time
			int digitLength = (int) Math.floor(value.bitLength() * BASE_FACTOR);
			String firstDigits = value.divide(BigInteger.TEN.pow(digitLength - STRING_CROP_LIMIT)).toString()
					.substring(0, STRING_CROP_LIMIT - Math.min(0, value.signum()));
			ai.opsNoCheck(value.bitLength() / 128);
			return firstDigits + "..." + String.format("%0" + STRING_CROP_LIMIT + "d", value.abs().mod(STRING_CROP_VALUE));
		}
	}

	public String toString(int i) throws LeekRunException {
		ops(2 + (int) Math.pow(value.bitLength(), 2) / 400 / i);
		return value.toString(i);
	}

	@Override
	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		return this.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BigIntegerValue && value.equals(((BigIntegerValue) obj).value);
	}

	private void binaryShiftOps(int n) throws LeekRunException {
		ops(n < 4000 ? 1 : n / 2000);
	}

	private void ops() throws LeekRunException {
		ops(1);
	}

	private void ops(int nb) throws LeekRunException {
		int size = (int) (value.bitLength() / 1000);
		ai.ops(nb + size);
	}

}
