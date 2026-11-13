package leekscript.runner;

// Fichier GÉNÉRÉ par tools.GenerateAIModule (src/test/java) : ne pas le modifier à la main.

/**
 * Base de la classe Java d'un module v5. Un module n'est pas une IA : il garde celle qui
 * l'exécute et lui relaie les méthodes que le code généré appelle sans les qualifier.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AIModule {

	protected final AI ai;
	public final leekscript.runner.values.ClassLeekValue arrayClass;
	public final leekscript.runner.values.ClassLeekValue bigintegerClass;
	public final leekscript.runner.values.ClassLeekValue booleanClass;
	public final leekscript.runner.values.ClassLeekValue classClass;
	public final leekscript.runner.values.ClassLeekValue functionClass;
	public final leekscript.runner.values.ClassLeekValue integerClass;
	public final leekscript.runner.values.ClassLeekValue intervalClass;
	public final leekscript.runner.values.ClassLeekValue jsonClass;
	public final leekscript.runner.values.ClassLeekValue legacyArrayClass;
	public final leekscript.runner.values.ClassLeekValue mapClass;
	public final leekscript.runner.values.ClassLeekValue mathClass;
	public final leekscript.runner.values.ClassLeekValue nullClass;
	public final leekscript.runner.values.ClassLeekValue numberClass;
	public final leekscript.runner.values.ClassLeekValue objectClass;
	public final leekscript.runner.values.ClassLeekValue realClass;
	public final leekscript.runner.values.ClassLeekValue setClass;
	public final leekscript.runner.values.ClassLeekValue stringClass;
	public final leekscript.runner.values.ClassLeekValue systemClass;
	public final leekscript.runner.values.ClassLeekValue valueClass;

	/** `name` : le nom du module, sous lequel les autres modules du programme le retrouvent. */
	protected AIModule(AI ai, String name) {
		this.ai = ai;
		ai.registerModule(name, this);
		this.arrayClass = ai.arrayClass;
		this.bigintegerClass = ai.bigintegerClass;
		this.booleanClass = ai.booleanClass;
		this.classClass = ai.classClass;
		this.functionClass = ai.functionClass;
		this.integerClass = ai.integerClass;
		this.intervalClass = ai.intervalClass;
		this.jsonClass = ai.jsonClass;
		this.legacyArrayClass = ai.legacyArrayClass;
		this.mapClass = ai.mapClass;
		this.mathClass = ai.mathClass;
		this.nullClass = ai.nullClass;
		this.numberClass = ai.numberClass;
		this.objectClass = ai.objectClass;
		this.realClass = ai.realClass;
		this.setClass = ai.setClass;
		this.stringClass = ai.stringClass;
		this.systemClass = ai.systemClass;
		this.valueClass = ai.valueClass;
	}

	public final long add(java.lang.Long a0, java.lang.Long a1) throws leekscript.runner.LeekRunException {
		return ai.add(a0, a1);
	}

	public final long add(java.lang.Long a0, long a1) throws leekscript.runner.LeekRunException {
		return ai.add(a0, a1);
	}

	public final java.lang.Object add(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.add(a0, a1);
	}

	public final long add(long a0, java.lang.Long a1) throws leekscript.runner.LeekRunException {
		return ai.add(a0, a1);
	}

	public final long add(long a0, long a1) throws leekscript.runner.LeekRunException {
		return ai.add(a0, a1);
	}

	public final void addSystemLog(int a0, int a1) throws leekscript.runner.LeekRunException {
		ai.addSystemLog(a0, a1);
	}

	public final void addSystemLog(int a0, int a1, java.lang.Object[] a2, java.lang.Throwable a3) throws leekscript.runner.LeekRunException {
		ai.addSystemLog(a0, a1, a2, a3);
	}

	public final void addSystemLog(int a0, java.lang.Throwable a1) throws leekscript.runner.LeekRunException {
		ai.addSystemLog(a0, a1);
	}

	public final void addSystemLog(int a0, leekscript.common.Error a1) throws leekscript.runner.LeekRunException {
		ai.addSystemLog(a0, a1);
	}

	public final void addSystemLog(int a0, leekscript.common.Error a1, java.lang.Object[] a2) throws leekscript.runner.LeekRunException {
		ai.addSystemLog(a0, a1, a2);
	}

	public final void addSystemLog(int a0, leekscript.common.Error a1, java.lang.Object[] a2, java.lang.Throwable a3) throws leekscript.runner.LeekRunException {
		ai.addSystemLog(a0, a1, a2, a3);
	}

	public final java.lang.Object add_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.add_eq(a0, a1);
	}

	public final leekscript.runner.AI.RamUsage allocateRAM(java.lang.Object a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.allocateRAM(a0, a1);
	}

	public final java.lang.Object arithmetic(leekscript.runner.AI.ArithmeticOperation a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.arithmetic(a0, a1, a2);
	}

	public final long band(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.band(a0, a1);
	}

	public final java.lang.Object bandAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bandAny(a0, a1);
	}

	public final java.lang.Object band_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.band_eq(a0, a1);
	}

	public final leekscript.runner.values.BigIntegerValue bigAnd(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bigAnd(a0, a1);
	}

	public final leekscript.runner.values.BigIntegerValue bigIntdiv(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bigIntdiv(a0, a1);
	}

	public final leekscript.runner.values.BigIntegerValue bigNot(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.bigNot(a0);
	}

	public final leekscript.runner.values.BigIntegerValue bigOr(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bigOr(a0, a1);
	}

	public final leekscript.runner.values.BigIntegerValue bigShl(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bigShl(a0, a1);
	}

	public final leekscript.runner.values.BigIntegerValue bigShr(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bigShr(a0, a1);
	}

	public final leekscript.runner.values.BigIntegerValue bigXor(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bigXor(a0, a1);
	}

	public final java.lang.Object bitAny(leekscript.runner.AI.BitOperation a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.bitAny(a0, a1, a2);
	}

	public final long bnot(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.bnot(a0);
	}

	public final java.lang.Object bnotAny(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.bnotAny(a0);
	}

	public final boolean bool(java.lang.Object a0) {
		return ai.bool(a0);
	}

	public final long bor(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bor(a0, a1);
	}

	public final java.lang.Object borAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.borAny(a0, a1);
	}

	public final java.lang.Object bor_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bor_eq(a0, a1);
	}

	public final long bxor(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bxor(a0, a1);
	}

	public final java.lang.Object bxorAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bxorAny(a0, a1);
	}

	public final java.lang.Object bxor_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.bxor_eq(a0, a1);
	}

	public final java.lang.Object callMethod(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2, java.lang.Object... a3) throws leekscript.runner.LeekRunException {
		return ai.callMethod(a0, a1, a2, a3);
	}

	public final java.lang.Object callObjectAccess(java.lang.Object a0, java.lang.String a1, java.lang.String a2, leekscript.runner.values.ClassLeekValue a3, java.lang.Object... a4) throws leekscript.runner.LeekRunException {
		return ai.callObjectAccess(a0, a1, a2, a3, a4);
	}

	public final java.lang.Object callObjectAccessNullSafe(java.lang.Object a0, java.lang.String a1, java.lang.String a2, leekscript.runner.values.ClassLeekValue a3, java.lang.Object... a4) throws leekscript.runner.LeekRunException {
		return ai.callObjectAccessNullSafe(a0, a1, a2, a3, a4);
	}

	public final void callTestFunction(int a0) throws leekscript.runner.LeekRunException {
		ai.callTestFunction(a0);
	}

	protected static java.lang.StackTraceElement[] captureAITrace() {
		return AI.captureAITrace();
	}

	public final void chargeCodeRAM(long a0) {
		ai.chargeCodeRAM(a0);
	}

	public final boolean check(java.lang.String a0, int[] a1, java.lang.Object... a2) throws leekscript.runner.LeekRunException {
		return ai.check(a0, a1, a2);
	}

	public final void checkStringLength(int a0) throws leekscript.runner.LeekRunException {
		ai.checkStringLength(a0);
	}

	public final leekscript.runner.values.ClassLeekValue classOf(java.lang.Object a0) {
		return ai.classOf(a0);
	}

	public final java.lang.Object convertTo(java.lang.Object a0, leekscript.common.Type a1) throws leekscript.runner.LeekRunException {
		return ai.convertTo(a0, a1);
	}

	public final java.lang.Object copy(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.copy(a0);
	}

	public final java.lang.Object copy(java.lang.Object a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.copy(a0, a1);
	}

	public final void decreaseRAM(leekscript.runner.AI.RamUsage a0, int a1) {
		ai.decreaseRAM(a0, a1);
	}

	public final double div(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.div(a0, a1);
	}

	public final double div_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.div_eq(a0, a1);
	}

	public final java.lang.Object div_eq_v1(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.div_eq_v1(a0, a1);
	}

	public final java.lang.Object div_v1(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.div_v1(a0, a1);
	}

	public static java.lang.String doubleToString(leekscript.runner.AI a0, double a1) {
		return AI.doubleToString(a0, a1);
	}

	public final boolean eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.eq(a0, a1);
	}

	public final boolean equals_equals(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.equals_equals(a0, a1);
	}

	public final java.lang.Object execute(java.lang.Object a0, java.lang.Object... a1) throws leekscript.runner.LeekRunException {
		return ai.execute(a0, a1);
	}

	public final java.lang.String export(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.export(a0);
	}

	public final java.lang.String export(java.lang.Object a0, java.util.Set<java.lang.Object> a1) throws leekscript.runner.LeekRunException {
		return ai.export(a0, a1);
	}

	public final java.lang.Object field_add_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_add_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_band_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_band_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_bor_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_bor_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_bxor_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_bxor_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_coalesce_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_coalesce_eq(a0, a1, a2, a3);
	}

	public final boolean field_coalesce_needed(java.lang.Object a0, java.lang.String a1) {
		return ai.field_coalesce_needed(a0, a1);
	}

	public final java.lang.Object field_dec(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.field_dec(a0, a1, a2);
	}

	public final java.lang.Object field_div_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_div_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_inc(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.field_inc(a0, a1, a2);
	}

	public final java.lang.Object field_intdiv_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_intdiv_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_mod_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_mod_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_mul_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_mul_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_pow_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_pow_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_pre_dec(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.field_pre_dec(a0, a1, a2);
	}

	public final java.lang.Object field_pre_inc(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.field_pre_inc(a0, a1, a2);
	}

	public final java.lang.Object field_shl_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_shl_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_shr_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_shr_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_sub_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_sub_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object field_ushr_eq(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.field_ushr_eq(a0, a1, a2, a3);
	}

	public static java.lang.reflect.Method findMethod(java.lang.Class<?> a0, java.lang.String a1, int a2) {
		return AI.findMethod(a0, a1, a2);
	}

	public final java.lang.Object get(java.lang.Object a0, java.lang.Object a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.get(a0, a1, a2);
	}

	protected final java.lang.String getAIString() {
		return ai.getAIString();
	}

	public final long getAnalyzeTime() {
		return ai.getAnalyzeTime();
	}

	public final leekscript.runner.values.Box getBox(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.getBox(a0, a1);
	}

	public final long getBytecodeSize() {
		return ai.getBytecodeSize();
	}

	public final long getCodeRAM() {
		return ai.getCodeRAM();
	}

	public final long getCompileTime() {
		return ai.getCompileTime();
	}

	public final leekscript.runner.AI.LeekScriptPosition getCurrentLeekScriptPosition() {
		return ai.getCurrentLeekScriptPosition();
	}

	public final java.util.Date getDate() {
		return ai.getDate();
	}

	protected final java.lang.String[] getErrorFiles() {
		return ai.getErrorFiles();
	}

	protected final int[] getErrorFilesID() {
		return ai.getErrorFilesID();
	}

	protected final java.lang.String getErrorLocalisation(int a0) {
		return ai.getErrorLocalisation(a0);
	}

	protected final java.lang.String getErrorLocalisation(java.lang.String a0, int a1) {
		return ai.getErrorLocalisation(a0, a1);
	}

	public final java.lang.String getErrorMessage(java.lang.StackTraceElement[] a0) {
		return ai.getErrorMessage(a0);
	}

	public final java.lang.String getErrorMessage(java.lang.Throwable a0) {
		return ai.getErrorMessage(a0);
	}

	public final java.lang.Object getField(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.getField(a0, a1, a2);
	}

	public static java.lang.reflect.Field getFieldCached(java.lang.Class<?> a0, java.lang.String a1) throws java.lang.NoSuchFieldException {
		return AI.getFieldCached(a0, a1);
	}

	public final java.lang.Object getFieldNullSafe(java.lang.Object a0, java.lang.String a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.getFieldNullSafe(a0, a1, a2);
	}

	public static java.lang.reflect.Field[] getFieldsCached(java.lang.Class<?> a0) {
		return AI.getFieldsCached(a0);
	}

	public final leekscript.compiler.AIFile getFile() {
		return ai.getFile();
	}

	public final int getId() {
		return ai.getId();
	}

	public final int getInstructions() {
		return ai.getInstructions();
	}

	protected final leekscript.compiler.LineMapping getLineMapping(int a0) {
		return ai.getLineMapping(a0);
	}

	protected final leekscript.compiler.LineMapping getLineMapping(java.lang.String a0, int a1) {
		return ai.getLineMapping(a0, a1);
	}

	public final long getLoadTime() {
		return ai.getLoadTime();
	}

	public final leekscript.AILog getLogs() {
		return ai.getLogs();
	}

	public final long getMaxOperations() {
		return ai.getMaxOperations();
	}

	public final long getMaxRAM() {
		return ai.getMaxRAM();
	}

	public final leekscript.runner.AIModule getModule(java.lang.String a0) {
		return ai.getModule(a0);
	}

	public final int getNextObjectID() {
		return ai.getNextObjectID();
	}

	public final long getOperations() {
		return ai.getOperations();
	}

	public final leekscript.compiler.RandomGenerator getRandom() {
		return ai.getRandom();
	}

	public final java.lang.String getString(java.lang.String a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.getString(a0, a1);
	}

	public final leekscript.testing.TestRecorder getTestRecorder() {
		return ai.getTestRecorder();
	}

	public final java.lang.String getTestTable() {
		return ai.getTestTable();
	}

	public final long getUsedRAM() {
		return ai.getUsedRAM();
	}

	public final int getVersion() {
		return ai.getVersion();
	}

	public final void increaseRAM(leekscript.runner.AI.RamUsage a0, int a1) throws leekscript.runner.LeekRunException {
		ai.increaseRAM(a0, a1);
	}

	public final void increaseRAMDirect(int a0) {
		ai.increaseRAMDirect(a0);
	}

	public final java.lang.Object increment(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.increment(a0);
	}

	public final void init() throws java.lang.Exception {
		ai.init();
	}

	public final java.lang.Object initField(java.lang.Object a0, java.lang.String a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.initField(a0, a1, a2);
	}

	public final boolean instanceOf(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.instanceOf(a0, a1);
	}

	public final int intOrNull(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.intOrNull(a0);
	}

	public final long intdiv(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.intdiv(a0, a1);
	}

	public final java.lang.Object intdivAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.intdivAny(a0, a1);
	}

	public final java.lang.Object intdiv_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.intdiv_eq(a0, a1);
	}

	public final int integer(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.integer(a0);
	}

	public final leekscript.runner.values.IntervalLeekValue interval(boolean a0, java.lang.Object a1, boolean a2, java.lang.Object a3) throws leekscript.runner.LeekRunException {
		return ai.interval(a0, a1, a2, a3);
	}

	public final boolean isIterable(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.isIterable(a0);
	}

	public final boolean isPrimitive(java.lang.Object a0) {
		return ai.isPrimitive(a0);
	}

	public static boolean isType(java.lang.Object a0, int a1) {
		return AI.isType(a0, a1);
	}

	public final java.util.Iterator<java.util.Map.Entry<java.lang.Object, java.lang.Object>> iterator(java.lang.Object a0) {
		return ai.iterator(a0);
	}

	public final boolean less(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.less(a0, a1);
	}

	public final boolean lessequals(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.lessequals(a0, a1);
	}

	public static java.lang.Object load(java.lang.Object a0) {
		return AI.load(a0);
	}

	public final long longOrNull(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.longOrNull(a0);
	}

	public final long longint(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.longint(a0);
	}

	public final java.lang.Long longintOrNull(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.longintOrNull(a0);
	}

	public final java.lang.Number minus(java.lang.Number a0) throws leekscript.runner.LeekRunException {
		return ai.minus(a0);
	}

	public final java.lang.Object minus(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.minus(a0);
	}

	public final java.lang.Object mod(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.mod(a0, a1);
	}

	public final java.lang.Object mod_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.mod_eq(a0, a1);
	}

	public final boolean more(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.more(a0, a1);
	}

	public final boolean moreequals(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.moreequals(a0, a1);
	}

	public final java.lang.Object mul(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.mul(a0, a1);
	}

	public final java.lang.Object mul_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.mul_eq(a0, a1);
	}

	public final boolean neq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.neq(a0, a1);
	}

	public final leekscript.runner.values.GenericArrayLeekValue newArray() throws leekscript.runner.LeekRunException {
		return ai.newArray();
	}

	public final leekscript.runner.values.GenericArrayLeekValue newArray(int a0) throws leekscript.runner.LeekRunException {
		return ai.newArray(a0);
	}

	public final leekscript.runner.values.GenericMapLeekValue newMap(leekscript.runner.AI a0) throws leekscript.runner.LeekRunException {
		return ai.newMap(a0);
	}

	public final leekscript.runner.values.ArrayLeekValue new_arrayClass() throws leekscript.runner.LeekRunException {
		return ai.new_arrayClass();
	}

	public final leekscript.runner.values.BigIntegerValue new_bigintegerClass() throws leekscript.runner.LeekRunException {
		return ai.new_bigintegerClass();
	}

	public final boolean new_booleanClass() {
		return ai.new_booleanClass();
	}

	public final long new_integerClass() {
		return ai.new_integerClass();
	}

	public final leekscript.runner.values.IntervalLeekValue new_intervalClass() throws leekscript.runner.LeekRunException {
		return ai.new_intervalClass();
	}

	public final leekscript.runner.values.LegacyArrayLeekValue new_legacyArrayClass() throws leekscript.runner.LeekRunException {
		return ai.new_legacyArrayClass();
	}

	public final leekscript.runner.values.MapLeekValue new_mapClass() throws leekscript.runner.LeekRunException {
		return ai.new_mapClass();
	}

	public final java.lang.Object new_nullClass() {
		return ai.new_nullClass();
	}

	public final double new_numberClass() {
		return ai.new_numberClass();
	}

	public final leekscript.runner.values.ObjectLeekValue new_objectClass() throws leekscript.runner.LeekRunException {
		return ai.new_objectClass();
	}

	public final double new_realClass() {
		return ai.new_realClass();
	}

	public final leekscript.runner.values.SetLeekValue new_setClass() throws leekscript.runner.LeekRunException {
		return ai.new_setClass();
	}

	public final java.lang.String new_stringClass() {
		return ai.new_stringClass();
	}

	public final boolean not(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.not(a0);
	}

	public final boolean notequals_equals(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.notequals_equals(a0, a1);
	}

	protected final void nothing(java.lang.Object a0) {
		ai.nothing(a0);
	}

	public final java.lang.Number number(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.number(a0);
	}

	public final long operations() {
		return ai.operations();
	}

	public final boolean operatorIn(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.operatorIn(a0, a1);
	}

	public final <T> T ops(T a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.ops(a0, a1);
	}

	public final boolean ops(boolean a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.ops(a0, a1);
	}

	public final double ops(double a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.ops(a0, a1);
	}

	public final void ops(int a0) throws leekscript.runner.LeekRunException {
		ai.ops(a0);
	}

	public final void ops(long a0) throws leekscript.runner.LeekRunException {
		ai.ops(a0);
	}

	public final long ops(long a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.ops(a0, a1);
	}

	public final <T> T opsAny(T a0, int a1) throws leekscript.runner.LeekRunException {
		return ai.opsAny(a0, a1);
	}

	public final void opsNoCheck(int a0) {
		ai.opsNoCheck(a0);
	}

	public final double pow(double a0, double a1) throws leekscript.runner.LeekRunException {
		return ai.pow(a0, a1);
	}

	public final double pow(java.lang.Double a0, long a1) throws leekscript.runner.LeekRunException {
		return ai.pow(a0, a1);
	}

	public final long pow(java.lang.Long a0, long a1) throws leekscript.runner.LeekRunException {
		return ai.pow(a0, a1);
	}

	public final java.lang.Number pow(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.pow(a0, a1);
	}

	public final long pow(long a0, long a1) throws leekscript.runner.LeekRunException {
		return ai.pow(a0, a1);
	}

	public final java.lang.Object put(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put(a0, a1, a2, a3);
	}

	public final java.lang.Object put_add_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_add_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_arithmetic_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.AI.ArithmeticOperation a3, leekscript.common.Type a4, leekscript.runner.values.ClassLeekValue a5) throws leekscript.runner.LeekRunException {
		return ai.put_arithmetic_eq(a0, a1, a2, a3, a4, a5);
	}

	public final java.lang.Object put_band_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_band_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_bit_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.AI.BitOperation a3, leekscript.common.Type a4, leekscript.runner.values.ClassLeekValue a5) throws leekscript.runner.LeekRunException {
		return ai.put_bit_eq(a0, a1, a2, a3, a4, a5);
	}

	public final java.lang.Object put_bor_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_bor_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_bxor_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_bxor_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_coalesce_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_coalesce_eq(a0, a1, a2, a3);
	}

	public final boolean put_coalesce_needed(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.put_coalesce_needed(a0, a1);
	}

	public final java.lang.Object put_dec(java.lang.Object a0, java.lang.Object a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.put_dec(a0, a1, a2);
	}

	public final java.lang.Object put_div_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_div_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_inc(java.lang.Object a0, java.lang.Object a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.put_inc(a0, a1, a2);
	}

	public final java.lang.Object put_intdiv_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_intdiv_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_mod_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_mod_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_mul_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_mul_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_pow_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_pow_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_pre_dec(java.lang.Object a0, java.lang.Object a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.put_pre_dec(a0, a1, a2);
	}

	public final java.lang.Object put_pre_inc(java.lang.Object a0, java.lang.Object a1, leekscript.runner.values.ClassLeekValue a2) throws leekscript.runner.LeekRunException {
		return ai.put_pre_inc(a0, a1, a2);
	}

	public final java.lang.Object put_shl_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_shl_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_shr_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_shr_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_sub_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_sub_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object put_ushr_eq(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.put_ushr_eq(a0, a1, a2, a3);
	}

	public final java.lang.Object putv4(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.putv4(a0, a1, a2, a3);
	}

	public final leekscript.runner.values.ArrayLeekValue range(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.range(a0, a1, a2);
	}

	public final leekscript.runner.values.ArrayLeekValue range(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, java.lang.Object a3) throws leekscript.runner.LeekRunException {
		return ai.range(a0, a1, a2, a3);
	}

	public final java.lang.Object rangeDynamic(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic(a0, a1, a2);
	}

	public final java.lang.Object rangeDynamic(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2, java.lang.Object a3) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic(a0, a1, a2, a3);
	}

	public final java.lang.Object rangeDynamic_all(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic_all(a0);
	}

	public final java.lang.Object rangeDynamic_all(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic_all(a0, a1);
	}

	public final java.lang.Object rangeDynamic_end(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic_end(a0, a1);
	}

	public final java.lang.Object rangeDynamic_end(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic_end(a0, a1, a2);
	}

	public final java.lang.Object rangeDynamic_start(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic_start(a0, a1);
	}

	public final java.lang.Object rangeDynamic_start(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.rangeDynamic_start(a0, a1, a2);
	}

	public final java.lang.String rangeString(java.lang.String a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.rangeString(a0, a1, a2);
	}

	public final java.lang.String rangeString(java.lang.String a0, java.lang.Object a1, java.lang.Object a2, java.lang.Object a3) throws leekscript.runner.LeekRunException {
		return ai.rangeString(a0, a1, a2, a3);
	}

	public final java.lang.String rangeString_all(java.lang.String a0) throws leekscript.runner.LeekRunException {
		return ai.rangeString_all(a0);
	}

	public final java.lang.String rangeString_all(java.lang.String a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.rangeString_all(a0, a1);
	}

	public final java.lang.String rangeString_end(java.lang.String a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.rangeString_end(a0, a1);
	}

	public final java.lang.String rangeString_end(java.lang.String a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.rangeString_end(a0, a1, a2);
	}

	public final java.lang.String rangeString_start(java.lang.String a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.rangeString_start(a0, a1);
	}

	public final java.lang.String rangeString_start(java.lang.String a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.rangeString_start(a0, a1, a2);
	}

	public final leekscript.runner.values.ArrayLeekValue range_all(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.range_all(a0);
	}

	public final leekscript.runner.values.ArrayLeekValue range_all(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.range_all(a0, a1);
	}

	public final leekscript.runner.values.ArrayLeekValue range_end(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.range_end(a0, a1);
	}

	public final leekscript.runner.values.ArrayLeekValue range_end(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.range_end(a0, a1, a2);
	}

	public final leekscript.runner.values.ArrayLeekValue range_start(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.range_start(a0, a1);
	}

	public final leekscript.runner.values.ArrayLeekValue range_start(java.lang.Object a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.range_start(a0, a1, a2);
	}

	public final double real(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.real(a0);
	}

	public static java.lang.Double realBox(java.lang.Object a0) {
		return AI.realBox(a0);
	}

	public final java.lang.Double realOrNull(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.realOrNull(a0);
	}

	public final void registerModule(java.lang.String a0, leekscript.runner.AIModule a1) {
		ai.registerModule(a0, a1);
	}

	public final void registerModuleClass(leekscript.runner.values.ClassLeekValue a0) {
		ai.registerModuleClass(a0);
	}

	public final void resetCounter() {
		ai.resetCounter();
	}

	public final java.lang.Object runIA() throws leekscript.runner.LeekRunException {
		return ai.runIA();
	}

	public final java.lang.Object set(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.set(a0, a1);
	}

	public final void setAnalyzeTime(long a0) {
		ai.setAnalyzeTime(a0);
	}

	public final void setCompileTime(long a0) {
		ai.setCompileTime(a0);
	}

	public final java.lang.Object setField(java.lang.Object a0, java.lang.String a1, java.lang.Object a2, leekscript.runner.values.ClassLeekValue a3) throws leekscript.runner.LeekRunException {
		return ai.setField(a0, a1, a2, a3);
	}

	public final void setFile(leekscript.compiler.AIFile a0) {
		ai.setFile(a0);
	}

	public final void setId(int a0) {
		ai.setId(a0);
	}

	public final void setLinesFile(java.io.File a0) {
		ai.setLinesFile(a0);
	}

	public final leekscript.runner.values.SetLeekValue setLiteralAdd(leekscript.runner.values.SetLeekValue a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.setLiteralAdd(a0, a1);
	}

	public final leekscript.runner.values.SetLeekValue setLiteralRange(leekscript.runner.values.SetLeekValue a0, java.lang.Object a1, java.lang.Object a2) throws leekscript.runner.LeekRunException {
		return ai.setLiteralRange(a0, a1, a2);
	}

	public final void setLoadTime(long a0) {
		ai.setLoadTime(a0);
	}

	public final void setMaxOperations(int a0) {
		ai.setMaxOperations(a0);
	}

	public final void setMaxRAM(int a0) {
		ai.setMaxRAM(a0);
	}

	public final void setTestRecorder(leekscript.testing.TestRecorder a0) {
		ai.setTestRecorder(a0);
	}

	public final void shareCodeRAM(int a0) {
		ai.shareCodeRAM(a0);
	}

	public final long shl(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.shl(a0, a1);
	}

	public final java.lang.Object shlAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.shlAny(a0, a1);
	}

	public final long shr(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.shr(a0, a1);
	}

	public final java.lang.Object shrAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.shrAny(a0, a1);
	}

	public final int signum(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.signum(a0);
	}

	public final void staticInit() throws java.lang.Exception {
		ai.staticInit();
	}

	public final java.lang.String string(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.string(a0);
	}

	public final java.lang.String string(java.lang.Object a0, java.util.Set<java.lang.Object> a1) throws leekscript.runner.LeekRunException {
		return ai.string(a0, a1);
	}

	public final java.lang.String stringSlice(java.lang.String a0, java.lang.Object a1, java.lang.Object a2, long a3) throws leekscript.runner.LeekRunException {
		return ai.stringSlice(a0, a1, a2, a3);
	}

	public final double sub(java.lang.Double a0, java.lang.Double a1) throws leekscript.runner.LeekRunException {
		return ai.sub(a0, a1);
	}

	public final long sub(java.lang.Long a0, java.lang.Long a1) throws leekscript.runner.LeekRunException {
		return ai.sub(a0, a1);
	}

	public final java.lang.Object sub(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.sub(a0, a1);
	}

	public final java.lang.Object sub_eq(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.sub_eq(a0, a1);
	}

	public final leekscript.runner.LeekError throwableToError(java.lang.Throwable a0) {
		return ai.throwableToError(a0);
	}

	public final leekscript.runner.values.ArrayLeekValue toArray(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toArray(a0, a1);
	}

	public final leekscript.runner.values.ArrayLeekValue toArrayOrNull(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toArrayOrNull(a0, a1);
	}

	public final leekscript.runner.values.FunctionLeekValue toFunction(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toFunction(a0, a1);
	}

	public final java.lang.Object toJSON(java.lang.Object a0) throws leekscript.runner.LeekRunException {
		return ai.toJSON(a0);
	}

	public final java.lang.Object toJSON(java.lang.Object a0, java.util.HashSet<java.lang.Object> a1) throws leekscript.runner.LeekRunException {
		return ai.toJSON(a0, a1);
	}

	public final leekscript.runner.values.LegacyArrayLeekValue toLegacyArray(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toLegacyArray(a0, a1);
	}

	public final leekscript.runner.values.LegacyArrayLeekValue toLegacyArrayOrNull(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toLegacyArrayOrNull(a0, a1);
	}

	public static java.lang.Long toLongOrNull(java.lang.Object a0) {
		return AI.toLongOrNull(a0);
	}

	public final leekscript.runner.values.MapLeekValue toMap(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toMap(a0, a1);
	}

	public final leekscript.runner.values.MapLeekValue toMapOrNull(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toMapOrNull(a0, a1);
	}

	public final leekscript.runner.values.SetLeekValue toSet(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toSet(a0, a1);
	}

	public final leekscript.runner.values.SetLeekValue toSetOrNull(int a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.toSetOrNull(a0, a1);
	}

	public static java.lang.String userClassName(java.lang.Class<?> a0) {
		return AI.userClassName(a0);
	}

	public final java.lang.Object userClassOwner(java.lang.Class<?> a0) {
		return ai.userClassOwner(a0);
	}

	public final long ushr(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.ushr(a0, a1);
	}

	public final java.lang.Object ushrAny(java.lang.Object a0, java.lang.Object a1) throws leekscript.runner.LeekRunException {
		return ai.ushrAny(a0, a1);
	}

	public static boolean verifyParameters(int[] a0, java.lang.Object... a1) {
		return AI.verifyParameters(a0, a1);
	}

	public final boolean xor(boolean a0, boolean a1) {
		return ai.xor(a0, a1);
	}
}
