package leekscript.runner.values;

import java.math.BigInteger;
import java.util.Set;

import leekscript.AILog;
import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

/**
 * Entier de taille arbitraire (limité uniquement par la mémoire et le coût en
 * opérations). Implémentation actuelle : wrapper immutable sur {@link BigInteger}.
 *
 * Le modèle de coût (ops / RAM) est proportionnel à la taille en bits du nombre :
 * une multiplication ou un décalage sur un très grand nombre coûte cher, ce qui
 * garantit l'équité en combat.
 *
 * Conçu initialement par Batary (bat_jarry@hotmail.fr), réimplémenté proprement
 * sur la base actuelle.
 *
 * @see BigInteger
 */
public class BigIntegerValue extends Number implements LeekValue {

	private final BigInteger value;
	private final AI ai;

	// nombre de chiffres conservés au début et à la fin lors de la conversion en
	// chaîne d'un très grand nombre (évite des conversions lentes/énormes)
	private final static int STRING_CROP_LIMIT = 10;
	private final static BigInteger STRING_CROP_VALUE = BigInteger.TEN.pow(STRING_CROP_LIMIT);
	private final static double BASE_FACTOR = Math.log10(2); // conversion base 2 -> base 10
	private final static double BIT_LIMIT = STRING_CROP_LIMIT * 2 / BASE_FACTOR; // bits correspondant au crop limit

	// Taille maximale (en bits) d'un big_integer. ~1 million de bits = ~315 000
	// chiffres décimaux : largement suffisant pour tout usage légitime, mais borne
	// le coût CPU/RAM d'une opération unique (une multiplication de deux nombres de
	// cette taille reste de l'ordre de la dizaine de ms). Empêche qu'un calcul comme
	// `n *= n` en boucle ne génère un nombre gigantesque qui ferait ramer le serveur.
	public final static long MAX_BITLENGTH = 1L << 20;

	public BigIntegerValue(AI ai, String val, int radix) throws LeekRunException {
		this.ai = ai;
		this.value = new BigInteger(val, radix);
		register();
	}

	public BigIntegerValue(AI ai, String val) throws LeekRunException {
		this.ai = ai;
		this.value = new BigInteger(val);
		register();
	}

	public BigIntegerValue(AI ai, double val) throws LeekRunException {
		this.ai = ai;
		// BigDecimal.valueOf() puis troncature : préserve la partie entière même
		// pour les grands réels (un simple `(long) val` saturerait à Long.MAX/MIN).
		// Infinity/NaN ne sont pas représentables : on garde la saturation `(long)`.
		this.value = Double.isFinite(val) ? java.math.BigDecimal.valueOf(val).toBigInteger() : BigInteger.valueOf((long) val);
		register();
	}

	public BigIntegerValue(AI ai, long val) throws LeekRunException {
		this.ai = ai;
		this.value = BigInteger.valueOf(val);
		register();
	}

	public BigIntegerValue(AI ai, BigInteger val) throws LeekRunException {
		this.ai = ai;
		this.value = val;
		register();
	}

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
		long bitsA = value.bitLength();
		long bitsB = val.value.bitLength();
		// Refuse de produire un nombre dépassant la taille max AVANT de lancer la
		// multiplication (sinon le calcul Java, très coûteux en CPU et en mémoire
		// transitoire, aurait déjà eu lieu au moment de construire le résultat).
		checkResultSize(bitsA + bitsB);
		// Coût ~ produit des tailles en mots : borne haute du coût réel (Java reste
		// sous-quadratique au-delà de quelques milliers de bits). Indispensable car un
		// coût LINÉAIRE laissait `n *= n` (carrés successifs, taille qui double à chaque
		// tour) atteindre des nombres de plusieurs centaines de Mo avant que la limite
		// d'opérations ne réagisse.
		ai.ops(mulCost(bitsA, bitsB));
		return new BigIntegerValue(ai, value.multiply(val.value));
	}

	public BigIntegerValue divide(BigIntegerValue val) throws LeekRunException {
		// La division d'un grand nombre par un autre est aussi coûteuse qu'une
		// multiplication : on la facture pareil (le résultat ne grandit pas).
		ai.ops(mulCost(value.bitLength(), val.value.bitLength()));
		return new BigIntegerValue(ai, value.divide(val.value));
	}

	public BigIntegerValue pow(int exponent) throws LeekRunException {
		long baseBits = value.bitLength();
		// base ∈ {-1, 0, 1} ou exposant ≤ 0 : résultat trivial (instantané), on garde
		// le comportement existant (value.pow(exponent) lève si exponent < 0).
		if (baseBits <= 1 || exponent <= 0) {
			ops(1);
			return new BigIntegerValue(ai, value.pow(exponent));
		}
		// Taille du résultat ~ exponent * bits de la base. On refuse les résultats
		// trop grands AVANT le calcul, et on facture le coût du dernier carré
		// (qui domine l'exponentiation rapide) plutôt qu'un coût linéaire qui rendait
		// `2L ** 1000000` quasi gratuit.
		long resultBits = (long) exponent * baseBits;
		checkResultSize(resultBits);
		ai.ops(mulCost(resultBits / 2, resultBits / 2));
		return new BigIntegerValue(ai, value.pow(exponent));
	}

	public BigIntegerValue abs() throws LeekRunException {
		return new BigIntegerValue(ai, value.abs());
	}

	public BigIntegerValue negate() throws LeekRunException {
		return new BigIntegerValue(ai, value.negate());
	}

	public BigIntegerValue mod(BigIntegerValue m) throws LeekRunException {
		ai.ops(mulCost(value.bitLength(), m.value.bitLength()));
		return new BigIntegerValue(ai, value.mod(m.value));
	}

	/**
	 * Reste de la division (opérateur `%`) : le signe suit le dividende, comme
	 * l'opérateur `%` sur les entiers. À distinguer de {@link #mod} (toujours positif).
	 */
	public BigIntegerValue remainder(BigIntegerValue m) throws LeekRunException {
		ai.ops(mulCost(value.bitLength(), m.value.bitLength()));
		return new BigIntegerValue(ai, value.remainder(m.value));
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
		return val; // BigInteger immutable : sûr
	}

	public static BigIntegerValue valueOf(AI ai, Object val) throws LeekRunException {
		if (val instanceof Double)
			return new BigIntegerValue(ai, (Double) val);
		if (val instanceof Long)
			return new BigIntegerValue(ai, (Long) val);
		if (val instanceof BigIntegerValue)
			return (BigIntegerValue) val;
		if (val instanceof Boolean)
			return new BigIntegerValue(ai, ((Boolean) val) ? 1L : 0L);
		if (val instanceof String)
			return new BigIntegerValue(ai, (String) val);
		ai.getLogs().addLog(AILog.ERROR, "Cannot cast \"" + val + "\" to BigInteger");
		return new BigIntegerValue(ai, 0);
	}

	public BigInteger getValue() {
		return value;
	}

	/** Vrai si la valeur est un entier exact (Long, big_integer ou booléen). */
	public static boolean isIntegerLike(Object v) {
		return v instanceof Long || v instanceof BigIntegerValue || v instanceof Boolean;
	}

	/** Vue BigInteger d'une valeur entière exacte (sans AI, pour la comparaison/tri). */
	public static BigInteger toBigInteger(Object v) {
		if (v instanceof BigIntegerValue bv) return bv.value;
		if (v instanceof Long l) return BigInteger.valueOf(l);
		if (v instanceof Boolean b) return b ? BigInteger.ONE : BigInteger.ZERO;
		return BigInteger.ZERO;
	}

	/** Comparaison EXACTE de deux entiers exacts (cf. {@link #isIntegerLike}). */
	public static int compareIntegers(Object v1, Object v2) {
		return toBigInteger(v1).compareTo(toBigInteger(v2));
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
		return value.signum() == 0;
	}

	@Override
	public String toString() {
		ai.opsNoCheck(10);
		if (value.bitLength() <= BIT_LIMIT) {
			return value.toString();
		} else {
			ai.opsNoCheck(100);
			// n'affiche que les premiers et derniers chiffres pour éviter une
			// conversion en chaîne énorme et lente
			int digitLength = (int) Math.floor(value.bitLength() * BASE_FACTOR);
			String firstDigits = value.divide(BigInteger.TEN.pow(digitLength - STRING_CROP_LIMIT)).toString()
					.substring(0, STRING_CROP_LIMIT - Math.min(0, value.signum()));
			ai.opsNoCheck((int) (value.bitLength() / 128));
			return firstDigits + "..." + String.format("%0" + STRING_CROP_LIMIT + "d", value.abs().mod(STRING_CROP_VALUE));
		}
	}

	public String toString(int radix) throws LeekRunException {
		ops(2 + (int) Math.pow(value.bitLength(), 2) / 400 / radix);
		return value.toString(radix);
	}

	@Override
	public String string(AI ai, Set<Object> visited) throws LeekRunException {
		return this.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BigIntegerValue && value.equals(((BigIntegerValue) obj).value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	private void binaryShiftOps(int n) throws LeekRunException {
		ops(n < 4000 ? 1 : n / 2000);
	}

	/**
	 * Coût d'une multiplication (ou division/modulo) de deux nombres de tailles
	 * données, proportionnel au produit des tailles en mots de 64 bits. C'est une
	 * borne HAUTE du coût réel (Java passe en Karatsuba/Toom-Cook au-delà de quelques
	 * milliers de bits, donc moins cher), ce qui garantit qu'on ne sous-facture jamais.
	 * Le facteur 8 laisse de la marge pour les usages légitimes (ex : exponentiation
	 * modulaire sur quelques milliers de bits) tout en stoppant net les carrés successifs.
	 */
	private static long mulCost(long bitsA, long bitsB) {
		long wa = Math.max(1, bitsA / 64);
		long wb = Math.max(1, bitsB / 64);
		return 1 + wa * wb / 8;
	}

	/** Refuse, AVANT le calcul, une opération qui produirait un nombre trop grand. */
	private void checkResultSize(long resultBits) throws LeekRunException {
		if (resultBits > MAX_BITLENGTH) {
			ai.getLogs().addLog(AILog.WARNING, "[BigInteger] résultat trop grand : " + resultBits + " bits (max " + MAX_BITLENGTH + ")");
			throw new LeekRunException(Error.OUT_OF_MEMORY);
		}
	}

	/** Enregistre un nouveau big_integer : vérifie la taille puis facture ops + RAM. */
	private void register() throws LeekRunException {
		checkResultSize(value.bitLength());
		ops(4);
		ai.allocateRAM(this, value.bitLength() / 64);
	}

	private void ops() throws LeekRunException {
		ops(1);
	}

	private void ops(int nb) throws LeekRunException {
		int size = (int) (value.bitLength() / 1000);
		ai.ops(nb + size);
	}
}
