package leekscript.runner;

import leekscript.AILog;
import leekscript.compiler.AIFile;
import leekscript.compiler.LineMapping;
import leekscript.compiler.RandomGenerator;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.classes.StandardClass;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.ClassLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.GenericMapLeekValue;
import leekscript.runner.values.IntegerIntervalLeekValue;
import leekscript.runner.values.IntervalLeekValue;
import leekscript.runner.values.RealIntervalLeekValue;
import leekscript.runner.values.SetLeekValue;
import leekscript.runner.values.LeekValue;
import leekscript.runner.values.LeekValueType;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.runner.values.Box;
import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.Type;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import leekscript.runner.values.BigIntegerValue;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSONObject;

public abstract class AI {

	public static final int DOUBLE = 1;
	public static final int INT = 2;
	public static final int BOOLEAN = 3;
	public static final int STRING = 4;
	public static final int NULL = 5;
	public static final int LEGACY_ARRAY = 6;
	public static final int NUMBER = 7;
	public static final int FUNCTION = 8;
	public static final int ARRAY = 9;
	public static final int MAP = 10;
	public static final int INTERVAL = 11;
	public static final int SET = 12;
	public static final int BIG_INT = 13;

	public static final int ERROR_LOG_COST = 10000;

	protected long mOperations = 0;
	public final static int MAX_OPERATIONS = 20_000_000;
	public long maxOperations = MAX_OPERATIONS;

	protected volatile long mRAM = 0;
	public final static int MAX_RAM = 12_500_000; // in 64 bits "quads" = 100 Mo
	public long maxRAM = MAX_RAM;

	// references to objects in memory
	private ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
	private List<LeekReference> references = new ArrayList<>();

	protected TreeMap<Integer, LineMapping> mLinesMapping = new TreeMap<>();
	protected String thisObject = null;

	protected int id;
	protected int version;
	protected AILog logs;
	protected int mInstructions;
	protected RandomGenerator randomGenerator;
	private long analyzeTime;
	private long compileTime;
	private long loadTime;
	private File filesLines;
	private AIFile file;
	private int objectID = 0;

	public final ClassLeekValue valueClass;
	public final ClassLeekValue nullClass;
	public final ClassLeekValue booleanClass;
	public final ClassLeekValue integerClass;
	public final ClassLeekValue bigIntegerClass;
	public final ClassLeekValue realClass;
	public final ClassLeekValue numberClass;
	public final ClassLeekValue arrayClass;
	public final ClassLeekValue legacyArrayClass;
	public final ClassLeekValue mapClass;
	public final ClassLeekValue intervalClass;
	public final ClassLeekValue setClass;
	public final ClassLeekValue stringClass;
	public final ClassLeekValue objectClass;
	public final ClassLeekValue functionClass;
	public final ClassLeekValue classClass;
	public final ClassLeekValue jsonClass;
	public final ClassLeekValue systemClass;

	public class NativeObjectLeekValue implements LeekValue {

		private int id;

		public NativeObjectLeekValue() throws LeekRunException {
			this.id = AI.this.getNextObjectID();
			AI.this.allocateRAM(this, size() * 2);
		}

		@Override
		public int hashCode() {
			return this.id;
		}

		public Object u_keys() throws LeekRunException {
			var result = new ArrayLeekValue(AI.this);
			for (var field : this.getClass().getFields()) {
				result.push(AI.this, field.getName());
			}
			return result;
		}

		public int size() {
			return this.getClass().getFields().length;
		}

		public String string(AI ai, Set<Object> visited) throws LeekRunException {
			return getStringBase(visited, false);
		}

		public String export(Set<Object> visited) throws LeekRunException {
			return getStringBase(visited, true);
		}

		public String getStringBase(Set<Object> visited, boolean export) throws LeekRunException {
			visited.add(this);

			try {
				var string_method = getClass().getMethod("u_string");
				var result = string_method.invoke(this);
				if (!(result instanceof String)) {
					AI.this.addSystemLog(AILog.ERROR, Error.STRING_METHOD_MUST_RETURN_STRING, new String[] { getClass().getSimpleName() });
				} else {
					return AI.this.string(result, visited);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// addSystemLog(AILog.SERROR, e);
				// Méthode pas trouvée, pas grave
			}

			var classes = new ArrayList<Class<?>>();
			Class<?> current = getClass();
			while (current != null && current != NativeObjectLeekValue.class) {
				classes.add(0, current);
				current = current.getSuperclass();
			}

			var fields = new ArrayList<Field>();
			for (var clazz : classes) {
				for (var f : clazz.getDeclaredFields()) {
					if (f.isSynthetic()) continue;
					fields.add(f);
				}
			}

			AI.this.ops(1 + fields.size() * 2);

			var sb = new StringBuilder();
			if (!getClass().getSimpleName().equals("Object")) {
				sb.append(getClass().getSimpleName().substring(2)).append(" ");
			}
			sb.append("{");
			boolean first = true;
			for (var field : fields) {
				if (first) first = false;
				else sb.append(", ");
				sb.append(field.getName());
				sb.append(": ");
				Object v = null;
				try {
					v = field.get(this);
				} catch (IllegalArgumentException | IllegalAccessException e) {}
				if (visited.contains(v)) {
					sb.append("<...>");
				} else {
					if (!AI.this.isPrimitive(v)) {
						visited.add(v);
					}
					if (export) {
						sb.append(AI.this.export(v, visited));
					} else {
						sb.append(AI.this.string(v, visited));
					}
				}
			}
			sb.append("}");
			return sb.toString();
		}

		public Object toJSON(AI ai, HashSet<Object> visited) throws LeekRunException {
			visited.add(this);

			var fields = new ArrayList<Field>();
			Class<?> current = getClass();
			while (current != null) {
				for (var f : current.getDeclaredFields()) {
					if (f.isSynthetic()) continue;
					fields.add(f);
				}
				current = current.getSuperclass();
			}

			var o = new JSONObject();
			for (var field : fields) {
				Object v;
				try {
					v = field.get(this);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					v = null;
				}
				if (!visited.contains(v)) {
					if (!ai.isPrimitive(v)) {
						visited.add(v);
					}
					o.put(ai.string(field.getName()), ai.toJSON(v, visited));
				}
			}
			return o;
		}
	}

	public AI(int instructions, int version) {
		this(instructions, version, new BasicAILog());
	}

	public AI(int instructions, int version, AILog logs) {
		this.logs = logs;
		this.mInstructions = instructions;
		this.version = version;

		randomGenerator = new RandomGenerator() {
			private Random random = new Random();

			@Override
			public void seed(long seed) {
				random.setSeed(seed);
			}

			@Override
			public int getInt(int min, int max) {
				if (max - min + 1 <= 0)
					return 0;
				return min + random.nextInt(max - min + 1);
			}

			@Override
			public long getLong(long min, long max) {
				if (max - min + 1 <= 0)
					return 0;
				// return min + Math.abs(random.nextLong()) % (max - min + 1);
				return min + random.nextInt((int) max - (int) min + 1);
			}

			@Override
			public double getDouble() {
				return random.nextDouble();
			}
		};

		valueClass = new ClassLeekValue(this, "Value");
		nullClass = new ClassLeekValue(this, "Null", valueClass);
		booleanClass = new ClassLeekValue(this, "Boolean", valueClass);
		numberClass = new ClassLeekValue(this, "Number", valueClass);
		realClass = new ClassLeekValue(this, "Real", numberClass);
		integerClass = new ClassLeekValue(this, "Integer", realClass);
		bigIntegerClass = new ClassLeekValue(this, "BigInteger", numberClass);
		arrayClass = new ClassLeekValue(this, "Array", valueClass);
		legacyArrayClass = new ClassLeekValue(this, "Array", valueClass);
		mapClass = new ClassLeekValue(this, "Map", valueClass);
		intervalClass = new ClassLeekValue(this, "Interval", valueClass);
		setClass = new ClassLeekValue(this, "Set", valueClass);
		stringClass = new ClassLeekValue(this, "String", valueClass);
		objectClass = new ClassLeekValue(this, "Object", valueClass);
		functionClass = new ClassLeekValue(this, "Function", valueClass);
		classClass = new ClassLeekValue(this, "Class", valueClass);
		jsonClass = new ClassLeekValue(this, "JSON");
		systemClass = new ClassLeekValue(this, "System");
		try {
			integerClass.addStaticField(this, "MIN_VALUE", Type.INT, Long.MIN_VALUE, AccessLevel.PUBLIC, true);
			integerClass.addStaticField(this, "MAX_VALUE", Type.INT, Long.MAX_VALUE, AccessLevel.PUBLIC, true);
			realClass.addStaticField(this, "MIN_VALUE", Type.REAL, Double.MIN_VALUE, AccessLevel.PUBLIC, true);
			realClass.addStaticField(this, "MAX_VALUE", Type.REAL, Double.MAX_VALUE, AccessLevel.PUBLIC, true);
		} catch (LeekRunException e) {
			// No exception possible for LS2+
		}

	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	// Method that can be overriden in each AI
	public void init() throws Exception {}

	// Method that can be overriden in each AI
	public void staticInit() throws Exception {}

	public int getInstructions() {
		return mInstructions;
	}

	public long operations() {
		return mOperations;
	}

	public long getOperations() {
		return mOperations;
	}

	public long getMaxOperations() {
		return maxOperations;
	}

	public long getUsedRAM() {
		Reference<?> referenceFromQueue;
		while ((referenceFromQueue = referenceQueue.poll()) != null) {
		    ((LeekReference)referenceFromQueue).finalizeResources(this);
		    referenceFromQueue.clear();
		}
		return mRAM;
	}

	public long getMaxRAM() {
		return maxRAM;
	}

	public void setMaxRAM(int maxRAM) {
		this.maxRAM = maxRAM;
	}

	public void setMaxOperations(int maxOperations) {
		this.maxOperations = maxOperations;
	}

	public AILog getLogs() {
		return logs;
	}

	public void ops(int nb) throws LeekRunException {
		// System.out.println("ops " + nb);
		mOperations += nb;
		if (mOperations >= maxOperations) {
			throw new LeekRunException(Error.TOO_MUCH_OPERATIONS);
		}
	}

	public Object ops(Object x, int nb) throws LeekRunException {
		ops(nb);
		return x;
	}

	public long ops(long x, int nb) throws LeekRunException {
		ops(nb);
		return x;
	}

	public double ops(double x, int nb) throws LeekRunException {
		ops(nb);
		return x;
	}

	public boolean ops(boolean x, int nb) throws LeekRunException {
		ops(nb);
		return x;
	}

	public void opsNoCheck(int nb) {
		// System.out.println("ops " + nb);
		mOperations += nb;
	}

	public void resetCounter() {
		mOperations = 0;
	}


	public void increaseRAMDirect(int ram) {
		mRAM += ram;
	}
	
	public RamUsage allocateRAM(Object obj, int ram) throws LeekRunException {
		return allocateRAM(obj, ram, true);
	}
	
	public RamUsage allocateRAM(Object obj, int ram, boolean checkOverflow) throws LeekRunException {
		RamUsage ramRef = new RamUsage(ram);
		// subscribe object to the referenceQueue
		references.add(new LeekReference(ramRef, obj, referenceQueue));

		mRAM += ram;
		if (checkOverflow) checkRamOverflow();
		
	    return ramRef;
	}
	
	public void increaseRAM(RamUsage ramRef, int value) throws LeekRunException {
		mRAM += value;
		ramRef.add(value);
		checkRamOverflow();
	}
	
	private void checkRamOverflow() throws LeekRunException {
		if (mRAM > maxRAM) {
			long ramBefore = mRAM;
			
			// update memory usage if garbage collector has already passed (call to gc is very expensive)
			Reference<?> referenceFromQueue;
			while ((referenceFromQueue = referenceQueue.poll()) != null) {
			    ((LeekReference)referenceFromQueue).finalizeResources(this);
			    referenceFromQueue.clear();
			}
			
			if (mRAM > maxRAM) {
				System.gc();
				
				for (int i = 100; i > 0 && mRAM > maxRAM; i--) {
					try {
						// wait a bit to let garbage collector run
						Thread.sleep(0, 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					while ((referenceFromQueue = referenceQueue.poll()) != null) {
					    ((LeekReference)referenceFromQueue).finalizeResources(this);
					    referenceFromQueue.clear();
					}
				}

				if (mRAM > maxRAM) {
					getLogs().addLog(AILog.WARNING, "[RAM error] RAM before: " + ramBefore + " RAM after: " + mRAM);
					throw new LeekRunException(Error.OUT_OF_MEMORY);
				}
			}
		}
	}

	public void decreaseRAM(RamUsage ramRef, int value) {
		mRAM -= value;
		ramRef.remove(value);
	}
	
	public void freeRAM(LeekReference ref, int value) {
		mRAM -= value;
		references.remove(ref);
	}

	protected void nothing(Object obj) {

	}

	public GenericArrayLeekValue newArray() {
		if (version >= 4) {
			return new ArrayLeekValue(this);
		} else {
			return new LegacyArrayLeekValue(this);
		}
	}

	public GenericArrayLeekValue newArray(int capacity) {
		if (version >= 4) {
			return new ArrayLeekValue(this, capacity);
		} else {
			return new LegacyArrayLeekValue(this);
		}
	}

	public GenericMapLeekValue newMap(AI ai) {
		if (version >= 4) {
			return new MapLeekValue(ai);
		} else {
			return new LegacyArrayLeekValue(ai);
		}
	}

	public String getErrorMessage(StackTraceElement[] elements) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (StackTraceElement element : elements) {
			// System.out.println(element.getClassName() + " " + element.getMethodName() + " " + element.getLineNumber());
			if (element.getClassName().startsWith("AI_")) {
				sb.append(getErrorLocalisation(element.getLineNumber()));
				if (count++ > 50) {
					sb.append("[...]");
					break;
				}
			}
		}
		// Java stacktrace
		// for (StackTraceElement element : elements) {
		// 	sb.append("\t▶ " + element.getClassName() + "." + element.getMethodName() + ", line " + element.getLineNumber()).append("\n");
		// }
		return sb.toString();
	}

	public record LeekScriptPosition(int file, int line) {}

	public LeekScriptPosition getCurrentLeekScriptPosition() {
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if (element.getClassName().startsWith("AI_")) {
				var mapping = getLineMapping(element.getLineNumber());
				if (mapping != null) {
					var files = getErrorFilesID();
					var f = mapping.getAI();
					int file = f < files.length ? files[f] : 0;
					return new LeekScriptPosition(file, mapping.getLeekScriptLine());
				}
			}
		}
		return null;
	}

	public String getErrorMessage(Throwable e) {
		return getErrorMessage(e.getStackTrace());
	}

	protected LineMapping getLineMapping(int javaLine) {
		if (mLinesMapping.isEmpty() && this.filesLines != null && this.filesLines.exists()) {
			try (Stream<String> stream = Files.lines(this.filesLines.toPath())) {
				stream.forEach(l -> {
					var parts = l.split(" ");
					mLinesMapping.put(Integer.parseInt(parts[0]), new LineMapping(Integer.parseInt(parts[2]), Integer.parseInt(parts[1])));
				});
			} catch (IOException e) {}
			thisObject = getAIString();
		}
		return mLinesMapping.get(javaLine);
	}

	protected String getErrorLocalisation(int javaLine) {
		var lineMapping = getLineMapping(javaLine);
		if (lineMapping != null) {
			var files = getErrorFiles();
			var f = lineMapping.getAI();
			String file = f < files.length ? files[f] : "?";
			return "\t▶ AI " + file + ", line " + lineMapping.getLeekScriptLine() + "\n"; // + ", java " + line;
		}
		return "";
	}

	public LeekError throwableToError(Throwable throwable) {

		if (throwable instanceof InvocationTargetException) {
			return throwableToError(throwable.getCause());
		}

		LeekError error = new LeekError();

		if (throwable instanceof ClassCastException) {
			error.type = Error.IMPOSSIBLE_CAST;

			Pattern r = Pattern.compile("class (.*) cannot be cast to class (.*) \\(");
			Matcher m = r.matcher(throwable.getMessage() != null ? throwable.getMessage() : "");
			if (m.find()) {
				error.parameters = new Object[] { javaTypeToLS(m.group(1)), javaTypeToLS(m.group(2)) };
			} else {
				error.parameters = new Object[] { "?", "?" };
			}
		} else if (throwable instanceof IllegalArgumentException) {
			throwable.printStackTrace(System.out);
			error.type = Error.IMPOSSIBLE_CAST;

			Pattern r = Pattern.compile("Can not set (.*) field .* to (.*)");
			Matcher m = r.matcher(throwable.getMessage() != null ? throwable.getMessage() : "");
			if (m.find()) {
				error.parameters = new Object[] { javaTypeToLS(m.group(2)), javaTypeToLS(m.group(1)) };
			} else {
				error.parameters = new Object[] { "?", "?" };
			}
		} else if (throwable instanceof NullPointerException) {
			error.type = Error.IMPOSSIBLE_CAST;

			Pattern r = Pattern.compile("Cannot invoke \"(.*)\" because \".*\" is null");
			Matcher m = r.matcher(throwable.getMessage() != null ? throwable.getMessage() : "");
			if (m.find()) {
				var method = m.group(1);
				var clazz = method.substring(0, method.lastIndexOf("."));
				error.parameters = new Object[] { "null", javaTypeToLS(clazz) };
			} else {
				Pattern r2 = Pattern.compile("Cannot invoke \"(.*)\" because the return value of \".*\" is null");
				Matcher m2 = r2.matcher(throwable.getMessage() != null ? throwable.getMessage() : "");
				if (m2.find()) {
					error.parameters = new Object[] { "null", javaTypeToLS(m2.group(1)) };
				} else {
					Pattern r3 = Pattern.compile("Cannot read field \"(.*)\" because \".*\" is null");
					Matcher m3 = r3.matcher(throwable.getMessage() != null ? throwable.getMessage() : "");
					if (m3.find()) {
						error.type = Error.UNKNOWN_FIELD;
						error.parameters = new Object[] { "null", javaTypeToLS(m3.group(1)) };
					} else {
						error.parameters = new Object[] { "null", "?" };
					}
				}
			}
		} else {
			// Erreur inconnue
			throwable.printStackTrace(System.out);
			error.parameters = throwable == null ? null : new Object[] { throwable.toString() };
		}
		return error;
	}

	public void addSystemLog(int type, Throwable throwable) throws LeekRunException {

		var error = throwableToError(throwable);

		addSystemLog(type, error.type.ordinal(), error.parameters, throwable);
	}

	private String javaTypeToLS(String type) {
		switch (type) {
			case "boolean":
			case "java.lang.Boolean":
			case "java.lang.Boolean.booleanValue()":
				return "boolean";
			case "long":
			case "java.lang.Long":
			case "java.lang.Long.longValue()":
				return "integer";
			case "big_integer":
			case "leekscript.runner.values.BigIntegerValue": 
				return "big_integer";
			case "double":
			case "java.lang.Double":
			case "java.lang.Double.doubleValue()":
				return "real";
			case "java.lang.String": return "string";
			case "leekscript.runner.values.MapLeekValue": return "Map";
			case "leekscript.runner.values.ArrayLeekValue":
			case "leekscript.runner.values.LegacyArrayLeekValue": return "Array";
			case "leekscript.runner.values.RealIntervalLeekValue": return "Interval<real>";
			case "leekscript.runner.values.IntegerIntervalLeekValue": return "Interval<integer>";
			case "leekscript.runner.values.ObjectLeekValue": return "Object";
			case "leekscript.runner.values.FunctionLeekValue": return "Function";
		}
		// AI_331043$u_Strategy
		var prefix = "AI_" + this.id + "$u_";
		if (type.startsWith(prefix)) return type.substring(prefix.length());

		return type;
	}

	public void addSystemLog(int type, Error error) throws LeekRunException {
		addSystemLog(type, error.ordinal(), null, null);
	}

	public void addSystemLog(int type, int error) throws LeekRunException {
		addSystemLog(type, error, null, null);
	}

	public void addSystemLog(int type, Error error, Object[] parameters) throws LeekRunException {
		addSystemLog(type, error.ordinal(), parameters, null);
	}

	public void addSystemLog(int type, Error error, Object[] parameters, Throwable cause) throws LeekRunException {
		addSystemLog(type, error.ordinal(), parameters, cause);
	}

	public void addSystemLog(int type, int error, Object[] parameters, Throwable cause) throws LeekRunException {
		ops(AI.ERROR_LOG_COST);
		if (type == AILog.WARNING)
			type = AILog.SWARNING;
		else if (type == AILog.ERROR)
			type = AILog.SERROR;
		else if (type == AILog.STANDARD)
			type = AILog.SSTANDARD;

		String stacktrace;
		if (cause == null) {
			 stacktrace = getErrorMessage(Thread.currentThread().getStackTrace());
		} else {
			if (cause.getCause() != null) {
				stacktrace = getErrorMessage(cause.getCause().getStackTrace());
			} else {
				stacktrace = getErrorMessage(cause.getStackTrace());
			}
		}
		logs.addSystemLog(this, type, stacktrace, error, parameters);
	}

	protected String[] getErrorFiles() { return null; }

	protected int[] getErrorFilesID() { return null; }

	protected String getAIString() { return ""; }

	public Object runIA() throws LeekRunException {
		return runIA(null);
	}

	public abstract Object runIA(Session session) throws LeekRunException;

	public RandomGenerator getRandom() {
		return randomGenerator;
	}

	public int getVersion() { return this.version; }

	public static Object load(Object value) {
		if (value instanceof Box) {
			return ((Box) value).get();
		}
		return value;
	}

	public boolean eq(Object x, Object y) throws LeekRunException {
		// ops(1);
		if (x == null) return y == null;
		if (x instanceof Number) {
			if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
				if (x instanceof Double && (((Double) x).isNaN() || ((Double) x).isInfinite())
					|| y instanceof Double && (((Double) y).isNaN() || ((Double) y).isInfinite())) {
					return false;
				}
				if (x instanceof Double || y instanceof Double) {
					return real(x) == real(y);
				}
				return bigint(x).equals(bigint(y));
			}
			var n = ((Number) x).doubleValue();
			if (y instanceof Number) {
				return n == ((Number) y).doubleValue();
			}
			if (y instanceof Boolean) {
				if ((Boolean) y) return n != 0;
				return n == 0;
			}
			if (y instanceof String) {
				var s = (String) y;
				if (s.equals("false") || s.equals("0") || s.equals("")) return n == 0;
				if (s.equals("true")) return n != 0;
				if (s.equals("1") && n == 1) return true;
				if (x instanceof Double) {
					try {
						ops(((String) y).length());
						return n == Double.parseDouble((String) y);
					} catch (Exception e) {
						return false;
					}
				} else if (x instanceof BigIntegerValue) {
					try {
						ops(((String) y).length());
						return x.equals(BigIntegerValue.valueOf(this, (String) y));
					} catch (Exception e) {
						return false;
					}
				} else {
					try {
						ops(((String) y).length());
						return n == Integer.parseInt((String) y);
					} catch (Exception e) {
						return false;
					}
				}
			}
			if (y instanceof LegacyArrayLeekValue) {
				return ((LegacyArrayLeekValue) y).equals(this, (Number) x);
			}
			if (y == null) return false;
			return n == real(y);
		}
		if (x instanceof Boolean) {
			if (y instanceof String) {
				if (((String) y).equals("false") || ((String) y).equals("0") || ((String) y).length() == 0) return ((Boolean) x) == false;
				return ((Boolean) x) == true;
			}
			if (y instanceof LegacyArrayLeekValue) {
				return ((LegacyArrayLeekValue) y).equals(this, (Boolean) x);
			}
			if (y instanceof Number) {
				return (Boolean) x == (((Number) y).doubleValue() != 0);
			}
		}
		if (x instanceof LegacyArrayLeekValue) {
			var array = (LegacyArrayLeekValue) x;
			if (y instanceof String) {
				if (((String) y).length() == 0) return array.size() == 0 || eq(array.iterator().next().getValue(), y);
			}
			return ((LegacyArrayLeekValue) x).equals(this, y);
		}
		if (x instanceof ArrayLeekValue ax && y instanceof ArrayLeekValue ay) {
			return ax.eq(ay);
		}
		if (x instanceof MapLeekValue mx && y instanceof MapLeekValue my) {
			return mx.eq(my);
		}
		if (x instanceof SetLeekValue sx && y instanceof SetLeekValue sy) {
			return sx.eq(sy);
		}
		if (x instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) x).equals(y);
		}
		if (x instanceof String) {
			var s = (String) x;
			if (y instanceof String) {
				ops(Math.min(s.length(), ((String) y).length()));
				return x.equals(y);
			}
			if (y instanceof Number) {
				var n = ((Number) y).doubleValue();
				if (s.equals("true")) return n != 0;
				if (s.equals("false") || s.equals("0") || s.length() == 0) return n == 0;
				if (s.equals("1") && n == 1) return true;
				
				ops(s.length());
				if (y instanceof BigIntegerValue) {
					try {
						return y.equals(BigIntegerValue.valueOf(this, s));
					} catch (Exception e) {
						return false;
					}
				} else {
					try {
						return n == Double.parseDouble(s);
					} catch (Exception e) {
						return false;
					}
				}
			}
			if (y instanceof Boolean) {
				if (s.equals("false") || s.equals("0") || s.length() == 0) return ((Boolean) y) == false;
				return ((Boolean) y) == true;
			}
			if (y instanceof LegacyArrayLeekValue) {
				var array = (LegacyArrayLeekValue) y;
				if (array.size() == 0) return s.length() == 0 || s.equals("false");
				if (array.size() == 1 || s.equals("true")) return eq(((LegacyArrayLeekValue) y).iterator().next().getValue(), x);
				return false;
			}
		}
		return x.equals(y);
	}

	public boolean neq(Object x, Object y) throws LeekRunException {
		return !eq(x, y);
	}

	public boolean equals_equals(Object x, Object y) throws LeekRunException {
		if (x == null) return y == null;
		if (y == null) return x == null;
		if (x instanceof ObjectLeekValue && y instanceof ObjectLeekValue) {
			return x.equals(y);
		}
		return LeekValueManager.getType(x) == LeekValueManager.getType(y) && eq(x, y);
	}

	public boolean notequals_equals(Object x, Object y) throws LeekRunException {
		return !equals_equals(x, y);
	}

	public boolean less(Object x, Object y) throws LeekRunException {
		if (x instanceof Number && y instanceof Number) {
			if (x instanceof Long && y instanceof Long) {
				return (Long) x < (Long) y;
			}
			if (x instanceof BigIntegerValue) {
				if (y instanceof BigIntegerValue) {
					return ((BigIntegerValue) x).compareTo((BigIntegerValue) y) < 0;
				}
				if (y instanceof Long) {
					return ((BigIntegerValue) x).compareTo(BigIntegerValue.valueOf(this, (Long) y)) < 0;
				}
			} else if (y instanceof BigIntegerValue) {
				if (x instanceof Long) {
					return BigIntegerValue.valueOf(this, (Long) x).compareTo((BigIntegerValue) y) < 0;
				}
			}
			return ((Number) x).doubleValue() < ((Number) y).doubleValue();
		}
		return longint(x) < longint(y);
	}

	public boolean more(Object x, Object y) throws LeekRunException {
		if (x instanceof Number && y instanceof Number) {
			if (x instanceof Long && y instanceof Long) {
				return (Long) x > (Long) y;
			}
			if (x instanceof BigIntegerValue) {
				if (y instanceof BigIntegerValue) {
					return ((BigIntegerValue) x).compareTo((BigIntegerValue) y) > 0;
				}
				if (y instanceof Long) {
					return ((BigIntegerValue) x).compareTo(BigIntegerValue.valueOf(this, (Long) y)) > 0;
				}
			} else if (y instanceof BigIntegerValue) {
				if (x instanceof Long) {
					return BigIntegerValue.valueOf(this, (Long) x).compareTo((BigIntegerValue) y) > 0;
				}
			}
			return ((Number) x).doubleValue() > ((Number) y).doubleValue();
		}
		return longint(x) > longint(y);
	}

	public boolean lessequals(Object x, Object y) throws LeekRunException {
		if (x instanceof Number && y instanceof Number) {
			if (x instanceof Long && y instanceof Long) {
				return (Long) x <= (Long) y;
			}
			if (x instanceof BigIntegerValue) {
				if (y instanceof BigIntegerValue) {
					return ((BigIntegerValue) x).compareTo((BigIntegerValue) y) <= 0;
				}
				if (y instanceof Long) {
					return ((BigIntegerValue) x).compareTo(BigIntegerValue.valueOf(this, (Long) y)) <= 0;
				}
			} else if (y instanceof BigIntegerValue) {
				if (x instanceof Long) {
					return BigIntegerValue.valueOf(this, (Long) x).compareTo((BigIntegerValue) y) <= 0;
				}
			}
			return ((Number) x).doubleValue() <= ((Number) y).doubleValue();
		}
		return longint(x) <= longint(y);
	}

	public boolean moreequals(Object x, Object y) throws LeekRunException {
		if (x instanceof Number && y instanceof Number) {
			if (x instanceof Long && y instanceof Long) {
				return (Long) x >= (Long) y;
			}
			if (x instanceof BigIntegerValue) {
				if (y instanceof BigIntegerValue) {
					return ((BigIntegerValue) x).compareTo((BigIntegerValue) y) >= 0;
				}
				if (y instanceof Long) {
					return ((BigIntegerValue) x).compareTo(BigIntegerValue.valueOf(this, (Long) y)) >= 0;
				}
			} else if (y instanceof BigIntegerValue) {
				if (x instanceof Long) {
					return BigIntegerValue.valueOf(this, (Long) x).compareTo((BigIntegerValue) y) >= 0;
				}
			}
			return ((Number) x).doubleValue() >= ((Number) y).doubleValue();
		}
		return longint(x) >= longint(y);
	}

	public boolean bool(Object value) {
		if (value instanceof Double) {
			return (Double) value != 0;
		} else if (value instanceof Long) {
			return (Long) value != 0;
		} else if (value instanceof BigIntegerValue) {
			return !((BigIntegerValue) value).isZero();
		} else if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).size() != 0;
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.size() != 0;
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).size() != 0;
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).size() != 0;
		} else if (value instanceof SetLeekValue set) {
			return set.size() != 0;
		} else if (value instanceof IntervalLeekValue) {
			return !((IntervalLeekValue) value).intervalIsEmpty(this);
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).size() != 0;
		} else if (value instanceof RealIntervalLeekValue) {
			// TODO
			return true;
		} else if (value instanceof String) {
			var s = (String) value;
			if (s.equals("false") || s.equals("0")) {
				return false;
			}
			return !s.isEmpty();
		} else if (value instanceof FunctionLeekValue) {
			return true;
		} else if (value instanceof ClassLeekValue) {
			return true;
		} else if (value instanceof Box) {
			return bool(((Box) value).get());
		}
		return false;
	}

	public int integer(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return (int) (double) value;
		} else if (value instanceof Long) {
			return (int) (long) value;
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).intValue();	
		} else if (value instanceof Boolean) {
			return ((Boolean) value) ? 1 : 0;
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).size();
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.size();
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).size();
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).size();
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).size();
		} else if (value instanceof IntegerIntervalLeekValue interval) {
			return (int) interval.intervalSize(this);
		} else if (value instanceof RealIntervalLeekValue interval) {
			return (int) interval.intervalSize(this);
		} else if (value instanceof String) {
			var s = (String) value;
			// ops(2);
			if (s.equals("true")) return 1;
			if (s.equals("false")) return 0;
			if (s.isEmpty()) return 0;
			ops(s.length());
			try {
				return Integer.parseInt(s);
			} catch (Exception e) {
				return (int) s.length();
			}
		} else if (value instanceof Box) {
			return integer(((Box) value).get());
		} else if (value instanceof FunctionLeekValue) {
			return 0;
		} else if (value == null) {
			return 0;
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}

	public Number number(Object value) throws LeekRunException {
		if (value instanceof Number) {
			return (Number) value;
		} else if (value instanceof Boolean) {
			return ((Boolean) value) ? 1l : 0l;
		} else if (value instanceof ObjectLeekValue) {
			return (long) ((ObjectLeekValue) value).size();
		} else if (value instanceof NativeObjectLeekValue o) {
			return (long) o.size();
		} else if (value instanceof LegacyArrayLeekValue) {
			return (long) ((LegacyArrayLeekValue) value).size();
		} else if (value instanceof ArrayLeekValue) {
			return (long) ((ArrayLeekValue) value).size();
		} else if (value instanceof MapLeekValue) {
			return (long) ((MapLeekValue) value).size();
		} else if (value instanceof IntegerIntervalLeekValue interval) {
			return interval.intervalSize(this);
		} else if (value instanceof RealIntervalLeekValue interval) {
			return interval.intervalSize(this);
		} else if (value instanceof String) {
			var s = (String) value;
			if (s.equals("true")) return 1l;
			if (s.equals("false")) return 0l;
			if (s.isEmpty()) return 0l;
			ops(s.length());
			try {
				return Double.parseDouble(s);
			} catch (Exception e) {
				return (long) s.length();
			}
		} else if (value instanceof Box) {
			return number(((Box) value).get());
		} else if (value instanceof FunctionLeekValue) {
			return 0l;
		} else if (value == null) {
			return 0l;
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}

	public long longint(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return (long) (double) value;
		} else if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).longValue();
		} else if (value instanceof Boolean) {
			return ((Boolean) value) ? 1 : 0;
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).size();
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.size();
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).size();
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).size();
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).size();
		} else if (value instanceof IntegerIntervalLeekValue interval) {
			return interval.intervalSize(this);
		} else if (value instanceof RealIntervalLeekValue interval) {
			return (long) interval.intervalSize(this);
		} else if (value instanceof String) {
			var s = (String) value;
			if (s.equals("true")) return 1;
			if (s.equals("false")) return 0;
			if (s.isEmpty()) return 0;
			ops(s.length());
			try {
				return Long.parseLong(s);
			} catch (Exception e) {
				return (long) s.length();
			}
		} else if (value instanceof FunctionLeekValue) {
			return 0;
		} else if (value instanceof Box box) {
			return longint(box.get());
		} else if (value == null) {
			return 0;
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}
	
	public BigIntegerValue bigint(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return BigIntegerValue.valueOf(this, (long) (double) value);
		} else if (value instanceof Long) {
			return BigIntegerValue.valueOf(this, (Long) value);
		} else if (value instanceof BigIntegerValue) {
			return (BigIntegerValue) value;
		} else if (value instanceof Boolean) {
			return ((Boolean) value) ? BigIntegerValue.valueOf(this, 1) : BigIntegerValue.valueOf(this, 0);
		} else if (value instanceof ObjectLeekValue) {
			return BigIntegerValue.valueOf(this, ((ObjectLeekValue) value).size());
		} else if (value instanceof NativeObjectLeekValue o) {
			return BigIntegerValue.valueOf(this, o.size());
		} else if (value instanceof LegacyArrayLeekValue) {
			return BigIntegerValue.valueOf(this, ((LegacyArrayLeekValue) value).size());
		} else if (value instanceof ArrayLeekValue) {
			return BigIntegerValue.valueOf(this, ((ArrayLeekValue) value).size());
		} else if (value instanceof MapLeekValue) {
			return BigIntegerValue.valueOf(this, ((MapLeekValue) value).size());
		} else if (value instanceof IntegerIntervalLeekValue interval) {
			return BigIntegerValue.valueOf(this, interval.intervalSize(this));
		} else if (value instanceof RealIntervalLeekValue interval) {
			return BigIntegerValue.valueOf(this, (long) interval.intervalSize(this));
		} else if (value instanceof String) {
			var s = (String) value;
			if (s.equals("true")) return BigIntegerValue.valueOf(this, 1);
			if (s.equals("false")) return BigIntegerValue.valueOf(this, 0);
			if (s.isEmpty()) return BigIntegerValue.valueOf(this, 0);
			ops(s.length());
			try {
				return BigIntegerValue.valueOf(this, s);
			} catch (Exception e) {
				return BigIntegerValue.valueOf(this, s.length());
			}
		} else if (value instanceof FunctionLeekValue) {
			return BigIntegerValue.valueOf(this, 0);
		} else if (value instanceof Box box) {
			return bigint(box.get());
		} else if (value == null) {
			return BigIntegerValue.valueOf(this, 0);
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}

	public double real(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return (Double) value;
		} else if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).doubleValue();
		} else if (value instanceof Boolean) {
			return ((Boolean) value) ? 1 : 0;
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).size();
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.size();
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).size();
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).size();
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).size();
		} else if (value instanceof IntegerIntervalLeekValue interval) {
			return (double) interval.intervalSize(this);
		} else if (value instanceof RealIntervalLeekValue interval) {
			return interval.intervalSize(this);
		} else if (value instanceof String) {
			var s = (String) value;
			if (s.equals("true")) return 1l;
			if (s.equals("false")) return 0l;
			if (s.isEmpty()) return 0l;
			ops(s.length());
			try {
				return Double.parseDouble(s);
			} catch (Exception e) {
				return s.length();
			}
		} else if (value instanceof Box) {
			return real(((Box) value).get());
		} else if (value instanceof FunctionLeekValue) {
			return 0.0;
		} else if (value == null) {
			return 0.0;
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}

	public int signum(Object value) throws LeekRunException {
		// Pour un float, on utile signum sinon ils sont castés en int 0
		if (value instanceof Double) {
			return (int) Math.signum((Double) value);
		}
		if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).signum();
		}
		return integer(value);
	}

	public boolean not(Object value) throws LeekRunException {
		return !bool(value);
	}

	public Object minus(Object value) throws LeekRunException {
		if (value instanceof Double) return -((Double) value);
		if (value instanceof BigIntegerValue) return ((BigIntegerValue) value).negate();
		return -longint(value);
	}

	public Number minus(Number value) throws LeekRunException {
		if (value instanceof Double) return -((Double) value);
		if (value instanceof BigIntegerValue) return ((BigIntegerValue) value).negate();
		return -longint(value);
	}
	
	public BigIntegerValue bnot(BigIntegerValue value) throws LeekRunException {
		return ((BigIntegerValue) value).not();
	}
	
	public Number bnot(Object value) throws LeekRunException {
		if (value instanceof BigIntegerValue) return ((BigIntegerValue) value).not();
		return LeekValueManager.bnot(this, value);
	}

	public long bnot(long value) throws LeekRunException {
		return LeekValueManager.bnot(this, value);
	}

	public Number bor(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) return bigint(x).or(bigint(y));
		return longint(x) | longint(y);
	}

	public Number band(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) return bigint(x).and(bigint(y));
		return longint(x) & longint(y);
	}

	public Number bxor(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) return bigint(x).xor(bigint(y));
		return longint(x) ^ longint(y);
	}

	public Number shl(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue) return bigint(x).shiftLeft((int) longint(y));
		return longint(x) << longint(y);
	}

	public Number shr(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue) return bigint(x).shiftRight((int) longint(y)); // same as >>> due to implementation
		return longint(x) >> longint(y);
	}

	public Number ushr(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue) return bigint(x).shiftRight((int) longint(y));
		return longint(x) >>> longint(y);
	}

	public long add(long x, long y) throws LeekRunException {
		return x + y;
	}

	public long add(long x, Long y) throws LeekRunException {
		return x + longint(y);
	}

	public long add(Long x, long y) throws LeekRunException {
		return longint(x) + y;
	}

	public long add(Long x, Long y) throws LeekRunException {
		return longint(x) + longint(y);
	}

	public Object add(Object x, Object y) throws LeekRunException {

		if (x instanceof String || y instanceof String) {
			var v1_string = string(x);
			var v2_string = string(y);
			ops(v1_string.length() + v2_string.length());
			return v1_string + v2_string;
		}

		if (x instanceof ArrayLeekValue) {
			var array1 = (ArrayLeekValue) x;
			if (y instanceof ArrayLeekValue) {
				var array2 = (ArrayLeekValue) y;
				return array1.arrayConcat(this, array2);
			}

			ops(array1.size() * 2);
			var result = new ArrayLeekValue(this, array1, 1);
			result.push(this, y);
			return result;
		}

		if (x instanceof MapLeekValue) {
			var map1 = (MapLeekValue) x;
			if (y instanceof MapLeekValue) {
				var map2 = (MapLeekValue) y;

				return map1.mapMerge(this, map2);
			}
		}

		// Concatenate arrays
		if (x instanceof LegacyArrayLeekValue) {
			if (y instanceof LegacyArrayLeekValue) {
				var array1 = (LegacyArrayLeekValue) x;
				var array2 = (LegacyArrayLeekValue) y;

				ops((array1.size() + array2.size()) * 2);

				var retour = new LegacyArrayLeekValue(this);
				var iterator = array1.iterator();
				while (iterator.hasNext()) {
					if (iterator.key() instanceof String) {
						retour.getOrCreate(this, iterator.getKey(this)).set(iterator.getValue(this));
					} else {
						retour.push(this, iterator.getValue(this));
					}
					iterator.next();
				}
				iterator = array2.iterator();
				while (iterator.hasNext()) {
					if (iterator.key() instanceof String) {
						retour.getOrCreate(this, iterator.getKey(this)).set(iterator.getValue(this));
					} else {
						retour.push(this, iterator.getValue(this));
					}
					iterator.next();
				}
				return retour;
			}

			var array1 = (LegacyArrayLeekValue) x;

			ops(array1.size() * 2);

			var retour = new LegacyArrayLeekValue(this);
			var iterator = array1.iterator();

			while (iterator.hasNext()) {
				if (iterator.key() instanceof String) {
					retour.getOrCreate(this, iterator.getKey(this)).set(iterator.getValue(this));
				} else {
					retour.push(this, iterator.getValue(this));
				}
				iterator.next();
			}
			retour.push(this, y);

			return retour;
		}

		if (y instanceof LegacyArrayLeekValue) {
			var array2 = (LegacyArrayLeekValue) y;

			ops(array2.size() * 2);

			var retour = new LegacyArrayLeekValue(this);

			retour.push(this, x);

			var iterator = array2.iterator();
			while (iterator.hasNext()) {
				if (iterator.key() instanceof String) {
					retour.getOrCreate(this, iterator.getKey(this)).set(iterator.getValue(this));
				} else {
					retour.push(this, iterator.getValue(this));
				}
				iterator.next();
			}
			return retour;
		}
		
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			return bigint(x).add(bigint(y));
		}
		if (x instanceof Double || y instanceof Double) {
			return real(x) + real(y);
		}
		return longint(x) + longint(y);
	}

	public long sub(Long x, Long y) throws LeekRunException {
		return longint(x) - longint(y);
	}

	public double sub(Double x, Double y) throws LeekRunException {
		return real(x) - real(y);
	}

	public Object sub(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			return bigint(x).subtract(bigint(y));
		}
		if (x instanceof Double || y instanceof Double) {
			return real(x) - real(y);
		}
		return longint(x) - longint(y);
	}

	public Object mul(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			return bigint(x).multiply(bigint(y));
		}
		if (x instanceof Double || y instanceof Double) {
			return real(x) * real(y);
		}
		return longint(x) * longint(y);
	}

	public Object div_v1(Object x, Object y) throws LeekRunException {
		double real_y = real(y);
		if (version == 1 && real_y == 0) {
			addSystemLog(AILog.ERROR, Error.DIVISION_BY_ZERO);
			return null;
		}
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			BigIntegerValue d = bigint(y);
			if (d.isZero()) return Double.NaN;
			return bigint(x).divide(d);
		}
		return real(x) / real_y;
	}

	public Number div(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			BigIntegerValue d = bigint(y);
			if (d.isZero()) return Double.NaN;
			return bigint(x).divide(d);
		}
		return real(x) / real(y);
	}

	public Number intdiv(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			BigIntegerValue d = bigint(y);
			if (d.isZero()) return Double.NaN;
			return bigint(x).divide(d);
		}
		return longint(x) / longint(y);
	}

	public Object mod(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue || y instanceof BigIntegerValue) {
			BigIntegerValue d = bigint(y);
			if (d.isZero()) return Double.NaN;
			return bigint(x).mod(d);
		}
		if (x instanceof Double || y instanceof Double) {
			return real(x) % real(y);
		}
		var y_int = longint(y);
		if (version == 1 && y_int == 0) {
			addSystemLog(AILog.ERROR, Error.DIVISION_BY_ZERO);
			return null;
		}
		return longint(x) % y_int;
	}

	public long pow(long x, long y) throws LeekRunException {
		return (long) Math.pow(x, y);
	}

	public double pow(double x, double y) throws LeekRunException {
		return Math.pow(x, y);
	}

	public double pow(Double x, long y) throws LeekRunException {
		return Math.pow(real(x), y);
	}

	public long pow(Long x, long y) throws LeekRunException {
		return (long) Math.pow(longint(x), y);
	}

	public Number pow(Object x, Object y) throws LeekRunException {
		if (x instanceof BigIntegerValue) {
			int e = integer(y);
			if (e < 0) return (Double) Math.pow(real(x), real(y));
			return bigint(x).pow(e);
		}
		if (x instanceof Double || y instanceof Double) {
			return Math.pow(real(x), real(y));
		}
		return (long) Math.pow(longint(x), longint(y));
	}

	public Object add_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) x).add_eq(this, y);
		}
		if (x instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) x).add_eq(this, y);
		}
		if (x instanceof MapLeekValue) {
			return ((MapLeekValue) x).add_eq(this, y);
		}
		return add(x, y);
	}

	public Object sub_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).sub_eq(y);
		}
		return null;
	}

	public Object mul_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).mul_eq(y);
		}
		return null;
	}

	public Number div_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).div_eq(y);
		}
		return 0.0;
	}

	public Object div_v1_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).div_v1_eq(y);
		}
		return null;
	}

	public Object intdiv_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).intdiv_eq(y);
		}
		return null;
	}

	public Object mod_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).mod_eq(y);
		}
		return null;
	}

	public Number bor_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).bor_eq(y);
		}
		return 0;
	}

	public Number band_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).band_eq(y);
		}
		return 0;
	}

	public Number bxor_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).bxor_eq(y);
		}
		return 0;
	}

	public Object increment(Object x) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).increment();
		}
		return null;
	}

	public int intOrNull(Object value) throws LeekRunException {
		if (value == null)
			return -1;
		return integer(value);
	}

	public long longOrNull(Object value) throws LeekRunException {
		if (value == null)
			return -1;
		return longint(value);
	}

	public Object copy(Object value) throws LeekRunException {
		return LeekOperations.clone(this, value);
	}

	public Object copy(Object value, int level) throws LeekRunException {
		return LeekOperations.clone(this, value, level);
	}

	public String string(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return doubleToString(this, (Double) value);
		} else if (value instanceof Long) {
			this.ops(3);
			return String.valueOf((Long) value);
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).toString();
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof String) {
			return (String) value;
		} else if (value instanceof LeekValue leekValue) {
			return leekValue.string(this, new HashSet<Object>());
		} else if (value instanceof Box box) {
			return string(box.get());
		} else if (value == null) {
			return "null";
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}

	public String string(Object value, Set<Object> visited) throws LeekRunException {
		if (value instanceof Double) {
			return doubleToString(this, (Double) value);
		} else if (value instanceof Long) {
			this.ops(3);
			return String.valueOf((Long) value);
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).toString();
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof String) {
			return (String) value;
		} else if (value instanceof LeekValue leekValue) {
			return leekValue.string(this, visited);
		} else if (value instanceof Box) {
			return string(((Box) value).get());
		} else if (value == null) {
			return "null";
		}
		throw new RuntimeException("Valeur invalide : " + value);
	}

	public static String doubleToString(AI ai, double value) {
		ai.opsNoCheck(3);
		if (ai.getVersion() >= 2) {
			if (value == Double.POSITIVE_INFINITY) return "∞";
			if (value == Double.NEGATIVE_INFINITY) return "-∞";
			return String.valueOf((Double) value);
		} else {
			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(0);
			return df.format((Double) value);
		}
	}

	public String export(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return doubleToString(this, (Double) value);
		} else if (value instanceof Long) {
			this.opsNoCheck(3);
			return String.valueOf((Long) value);
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).toString();
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).export(this, new HashSet<Object>());
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.export(new HashSet<Object>());
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).export(this, new HashSet<Object>());
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).string(this, new HashSet<Object>());
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).string(this, new HashSet<Object>());
		} else if (value instanceof SetLeekValue set) {
			return set.string(this, new HashSet<Object>());
		} else if (value instanceof IntervalLeekValue interval) {
			return interval.string(this, new HashSet<Object>());
		} else if (value instanceof String) {
			return "\"" + value + "\"";
		} else if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).string(this);
		} else if (value instanceof FunctionLeekValue function) {
			return function.toString();
		} else if (value instanceof Box) {
			return export(((Box) value).get());
		} else if (value == null) {
			return "null";
		}
		throw new RuntimeException("Valeur invalide : " + value + " class=" + value.getClass());
	}

	public String export(Object value, Set<Object> visited) throws LeekRunException {
		if (value instanceof Double) {
			return doubleToString(this, (Double) value);
		} else if (value instanceof Long) {
			this.ops(3);
			return String.valueOf((Long) value);
		} else if (value instanceof BigIntegerValue) {
			return ((BigIntegerValue) value).toString();
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).export(this, visited);
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.export(visited);
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).export(this, visited);
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).string(this, visited);
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).string(this, visited);
		} else if (value instanceof SetLeekValue) {
			return ((SetLeekValue) value).string(this, visited);
		} else if (value instanceof IntervalLeekValue) {
			return ((IntervalLeekValue) value).string(this, visited);
		} else if (value instanceof String) {
			return "\"" + value + "\"";
		} else if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).string(this);
		} else if (value instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) value).toString();
		} else if (value instanceof Box) {
			return export(((Box) value).get());
		} else if (value == null) {
			return "null";
		}
		throw new RuntimeException("Valeur invalide : " + value + " class=" + value.getClass());
	}

	public Object toJSON(Object v) throws LeekRunException {
		return toJSON(v, new HashSet<>());
	}

	public Object toJSON(Object v, HashSet<Object> visited) throws LeekRunException {
		if (v instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) v).toJSON(this, visited);
		}
		if (v instanceof MapLeekValue) {
			return ((MapLeekValue) v).toJSON(this, visited);
		}
		if (v instanceof RealIntervalLeekValue) {
			// TODO
			return new JSONObject();
		}
		if (v instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) v).toJSON(this, visited);
		}
		if (v instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) v).toJSON(this, visited);
		}
		if (v instanceof NativeObjectLeekValue o) {
			return o.toJSON(this, visited);
		}
		return v;
	}

	public boolean isPrimitive(Object value) {
		return !(value instanceof ArrayLeekValue || value instanceof MapLeekValue || value instanceof LegacyArrayLeekValue || value instanceof SetLeekValue || value instanceof RealIntervalLeekValue || value instanceof ObjectLeekValue || value instanceof NativeObjectLeekValue);
	}

	public boolean isIterable(Object value) throws LeekRunException {
		boolean ok = value instanceof LegacyArrayLeekValue || value instanceof ArrayLeekValue || value instanceof MapLeekValue || value instanceof SetLeekValue || value instanceof IntervalLeekValue;
		if (!ok && version >= 2) {
			addSystemLog(AILog.ERROR, Error.NOT_ITERABLE, new Object[] { value });
		}
		return ok;
	}

	public Iterator<Entry<Object, Object>> iterator(Object value) {
		if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).iterator();
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).genericIterator();
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).entrySet().iterator();
		} else if (value instanceof SetLeekValue set) {
			return set.genericIterator();
		} else if (value instanceof IntervalLeekValue interval) {
			return interval.iterator();
		}
		return null;
	}

	public Object getField(Object value, String field, ClassLeekValue fromClass) throws LeekRunException {
		if (field.equals("class")) {
			return classOf(value);
		}
		if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).getField(field, fromClass);
		}
		if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).getField(field, fromClass);
		}
		if (value instanceof NativeObjectLeekValue object) {
			try {
				var f = value.getClass().getField(field);
				if (!checkFieldAccessLevel(f, value, fromClass)) {
					return null;
				}
				return f.get(value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				// Method ?
				try {
					var clazz = (ClassLeekValue) this.getClass().getField(value.getClass().getSimpleName()).get(this);
					var method = clazz.genericMethods.get(field);
					if (method != null) return method;
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
					addSystemLog(AILog.ERROR, e1);
				}
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { value, field });
		return null;
	}

	private Field getWriteableField(Object object, String field, ClassLeekValue fromClass) throws LeekRunException, NoSuchFieldException, SecurityException {
		var f = object.getClass().getField(field);
		if (!checkFieldAccessLevel(f, object, fromClass)) {
			return null;
		}
		if (f.isAnnotationPresent(Final.class)) {
			this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getSimpleName().substring(2), field });
			return null;
		}
		return f;
	}

	private boolean checkFieldAccessLevel(Field field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (field.isAnnotationPresent(Private.class)) {
			if (fromClass == null || value.getClass() != fromClass.clazz) {
				addSystemLog(AILog.ERROR, Error.PRIVATE_FIELD, new Object[] { value.getClass().getSimpleName().substring(2), field.getName() });
				return false;
			}
		} else if (field.isAnnotationPresent(Protected.class)) {
			if (fromClass == null || !value.getClass().isAssignableFrom(fromClass.clazz)) {
				addSystemLog(AILog.ERROR, Error.PROTECTED_FIELD, new Object[] { value.getClass().getSimpleName().substring(2), field.getName() });
				return false;
			}
		}
		return true;
	}

	public Object initField(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).initField(field, value);
		}
		try {
			object.getClass().getField(field).set(object, value);
			return value;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object setField(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).setField(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).setField(field, value);
		}
		if (object instanceof NativeObjectLeekValue) {
			try {
				var f = object.getClass().getField(field);
				if (!checkFieldAccessLevel(f, object, fromClass)) {
					return null;
				}
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
					return null;
				}
				f.set(object, value);
				return value;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				// addSystemLog(AILog.ERROR, e);
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_inc(Object object, String field, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_inc(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).increment();
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var previous = f.get(object);
			var v = add(previous, 1l);
			f.set(object, v);
			return previous;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}

		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_pre_inc(Object object, String field, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_pre_inc(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).pre_increment();
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = add(f.get(object), 1l);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_dec(Object object, String field, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_dec(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).decrement();
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var previous = f.get(object);
			var v = sub(previous, 1l);
			f.set(object, v);
			return previous;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_pre_dec(Object object, String field, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_pre_dec(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).pre_decrement();
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = sub(f.get(object), 1l);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_add_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_add_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).add_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = add(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_sub_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_sub_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).sub_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = sub(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_mul_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_mul_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).mul_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = mul(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_pow_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_pow_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).pow_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = pow(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_div_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_div_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).div_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = div(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_intdiv_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_intdiv_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).intdiv_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = intdiv(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_mod_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_mod_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).mod_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (!checkFieldAccessLevel(f, object, fromClass)) {
				return null;
			}
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = mod(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_bor_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_bor_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).bor_eq(value);
		}
		try {
			var f = getWriteableField(object, field, fromClass);
			if (f == null) return null;
			var v = bor(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_band_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_band_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).band_eq(value);
		}
		try {
			var f = getWriteableField(object, field, fromClass);
			if (f == null) return null;
			var v = band(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_bxor_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_bxor_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).bxor_eq(value);
		}
		try {
			var f = getWriteableField(object, field, fromClass);
			if (f == null) return null;
			var v = bxor(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_shl_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_shl_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).shl_eq(value);
		}
		try {
			var f = getWriteableField(object, field, fromClass);
			if (f == null) return null;
			var v = shl(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			addSystemLog(AILog.ERROR, e);
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_shr_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_shr_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).shr_eq(value);
		}
		if (object instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(object, field, fromClass);
			if (f == null) return null;
				var v = shr(f.get(object), value);
				f.set(object, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_ushr_eq(Object object, String field, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_ushr_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).ushr_eq(value);
		}
		if (object instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(object, field, fromClass);
				if (f == null) return null;
				var v = ushr(f.get(object), value);
				f.set(object, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}
	public Object putv4(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put(key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).putv4(key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).set(key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).setField(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).setField(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				f.set(array, value);
				return value;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put(key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put(key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).set(key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).setField(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).setField(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				f.set(array, value);
				return value;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_inc(Object array, Object key, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_inc(this, key);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_inc(this, key);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_inc(this, key);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_inc(field);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_inc(field);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = f.get(array);
				f.set(array, add(v, 1l));
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_pre_inc(Object array, Object key, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_pre_inc(this, key);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_pre_inc(this, key);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_pre_inc(this, key);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_pre_inc(field);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_pre_inc(field);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = add(f.get(array), 1l);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_dec(Object array, Object key, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_dec(this, key);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_dec(this, key);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_dec(this, key);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_dec(field);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_dec(field);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = f.get(array);
				f.set(array, sub(v, 1l));
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_pre_dec(Object array, Object key, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_pre_dec(this, key);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_pre_dec(this, key);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_pre_dec(this, key);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_pre_dec(field);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_pre_dec(field);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = sub(f.get(array), 1l);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_add_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_add_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_add_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_add_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_add_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_add_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = add(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_sub_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_sub_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_sub_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_sub_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_sub_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_sub_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = sub(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_mul_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_mul_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_mul_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_mul_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_mul_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_mul_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = mul(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_pow_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_pow_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_pow_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_pow_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_pow_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_pow_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = pow(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_mod_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_mod_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_mod_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_mod_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_mod_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_mod_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = mod(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_div_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_div_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_div_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_div_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_div_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_div_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = div(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Number put_intdiv_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_intdiv_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_intdiv_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_intdiv_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_intdiv_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_intdiv_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return 0;
				var v = intdiv(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return 0;
	}

	public Object put_bor_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_bor_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_bor_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_bor_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_bor_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_bor_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = bor(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_band_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_band_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_band_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_band_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_band_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_band_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = band(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_shl_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_shl_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_shl_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_shl_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_shl_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_shl_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = shl(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_shr_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_shr_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_shr_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_shr_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_shr_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_shr_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = shr(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_ushr_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_ushr_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_ushr_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_ushr_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_ushr_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_ushr_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = ushr(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_bxor_eq(Object array, Object key, Object value, ClassLeekValue fromClass) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put_bxor_eq(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put_bxor_eq(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put_bxor_eq(this, key, value);
		}
		if (array instanceof ObjectLeekValue) {
			var field = string(key);
			return ((ObjectLeekValue) array).field_bxor_eq(field, value);
		}
		if (array instanceof ClassLeekValue) {
			var field = string(key);
			return ((ClassLeekValue) array).field_bxor_eq(field, value);
		}
		if (array instanceof NativeObjectLeekValue) {
			try {
				var f = getWriteableField(array, string(key), fromClass);
				if (f == null) return null;
				var v = bxor(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				addSystemLog(AILog.ERROR, e);
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object set(Object variable, Object value) throws LeekRunException {
		if (variable instanceof Box) {
			return ((Box) variable).set(value);
		}
		return null;
	}

	public Object get(Object value, Object index, ClassLeekValue fromClass) throws LeekRunException {
		if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).get(index);
		}
		if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).get(this, index);
		}
		if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).get(index);
		}
		if (value instanceof ObjectLeekValue) {
			ops(1);
			return ((ObjectLeekValue) value).getField(string(index), fromClass);
		}
		if (value instanceof ClassLeekValue) {
			ops(1);
			return ((ClassLeekValue) value).getField(string(index));
		}
		if (value instanceof NativeObjectLeekValue) {
			ops(1);
			try {
				var f = value.getClass().getField(string(index));
				if (!checkFieldAccessLevel(f, value, fromClass)) {
					return null;
				}
				return f.get(value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				// Method ?
				try {
					var clazz = (ClassLeekValue) this.getClass().getField(value.getClass().getSimpleName()).get(this);
					var method = clazz.genericMethods.get(string(index));
					if (method != null) return method;
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
					addSystemLog(AILog.ERROR, e1);
				}
			}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { value });
		return null;
	}

	public Box getBox(Object value, Object index) throws LeekRunException {
		if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).getBox(this, index);
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { value });
		return null;
	}

	public ArrayLeekValue range(Object value, Object startIndex, Object endIndex) throws LeekRunException {
		return range(value, startIndex, endIndex, 1l);
	}

	public ArrayLeekValue range(Object value, Object start, Object end, Object strideObject) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			var stride = longint(strideObject);
			return ((ArrayLeekValue) value).arraySlice(this, start, end, stride);
		}
		if (value instanceof IntervalLeekValue interval) {
			return interval.range(this, start, end, strideObject);
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { value });
		return null;
	}

	public ArrayLeekValue range_start(Object value, Object endIndex) throws LeekRunException {
		return range_start(value, endIndex, 1l);
	}

	public ArrayLeekValue range_start(Object value, Object start, Object strideObject) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			var array = (ArrayLeekValue) value;
			var stride = longint(strideObject);
			return array.arraySlice(this, start, null, stride);
		}
		if (value instanceof IntervalLeekValue interval) {
			return interval.range(this, start, null, strideObject);
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { value });
		return null;
	}

	public ArrayLeekValue range_end(Object value, Object endIndex) throws LeekRunException {
		return range_end(value, endIndex, 1l);
	}

	public ArrayLeekValue range_end(Object value, Object end, Object strideObject) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			var array = (ArrayLeekValue) value;
			var stride = longint(strideObject);
			return array.arraySlice(this, null, end, stride);
		}
		if (value instanceof IntervalLeekValue interval) {
			return interval.range(this, null, end, real(strideObject));
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { value });
		return null;
	}

	public ArrayLeekValue range_all(Object value) throws LeekRunException {
		return range_all(value, 1l);
	}

	public ArrayLeekValue range_all(Object value, Object strideObject) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			var stride = longint(strideObject);
			return ((ArrayLeekValue) value).arraySlice(this, null, null, stride);
		}
		if (value instanceof IntervalLeekValue interval) {
			return interval.range(this, null, null, real(strideObject));
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { value });
		return null;
	}

	public Object callMethod(Object value, String method, ClassLeekValue fromClass, Object... args) throws LeekRunException {
		if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).callMethod(method, fromClass, args);
		}
		// if (value instanceof ClassLeekValue) {
		// 	return ((ClassLeekValue) value).callMethod(method, args);
		// }
		try {
			// var m = value.getClass().getMethod("u_" + method);
			Method m = null;
			for (var mm : value.getClass().getMethods()) {
				if (mm.getName().equals("u_" + method) && mm.getParameterTypes().length == args.length) {
					m = mm;
					break;
				}
			}
			if (m == null) throw new NoSuchMethodException(method);
			if (m.isAnnotationPresent(Private.class)) {
				if (fromClass == null || value.getClass() != fromClass.clazz) {
					addSystemLog(AILog.ERROR, Error.PRIVATE_METHOD, new Object[] { value.getClass().getSimpleName().substring(2), method });
					return null;
				}
			} else if (m.isAnnotationPresent(Protected.class)) {
				if (fromClass == null || !value.getClass().isAssignableFrom(fromClass.clazz)) {
					addSystemLog(AILog.ERROR, Error.PROTECTED_METHOD, new Object[] { value.getClass().getSimpleName().substring(2), method });
					return null;
				}
			}
			return m.invoke(value, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace(System.out);
		}
		return null;
	}

	public Object callObjectAccess(Object value, String field, String method, ClassLeekValue fromClass, Object... args) throws LeekRunException {
		if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).callMethod(method + "_" + args.length, fromClass, args);
		}
		if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).callAccess(field, method, fromClass, args);
		}
		if (value instanceof NativeObjectLeekValue) {
			try {
				// var types = new Class<?>[args.length];
				// for (int i = 0; i < args.length; ++i) {
				// 	types[i] = args[i] == null ? Object.class : args[i].getClass();
				// }
				// var m = value.getClass().getMethod(method, types);
				Method m = null;
				for (var mm : value.getClass().getMethods()) {
					if (mm.getName().equals(method) && mm.getParameterTypes().length == args.length) {
						m = mm;
						break;
					}
				}
				if (m == null) throw new NoSuchMethodException(method);
				if (m.isAnnotationPresent(Private.class)) {
					if (fromClass == null || value.getClass() != fromClass.clazz) {
						addSystemLog(AILog.ERROR, Error.PRIVATE_METHOD, new Object[] { value.getClass().getSimpleName().substring(2), field });
						return null;
					}
				} else if (m.isAnnotationPresent(Protected.class)) {
					if (fromClass == null || !value.getClass().isAssignableFrom(fromClass.clazz)) {
						addSystemLog(AILog.ERROR, Error.PROTECTED_METHOD, new Object[] { value.getClass().getSimpleName().substring(2), field });
						return null;
					}
				}
				return m.invoke(value, args);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				if (e instanceof InvocationTargetException) {
					addSystemLog(AILog.ERROR, e);
				} else {
					try {
						var f = value.getClass().getField(field);
						if (!checkFieldAccessLevel(f, value, fromClass)) {
							return null;
						}
						return execute(f.get(value), args);
					} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
						addSystemLog(AILog.ERROR, e1);
					}
				}
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { value, field });
		return null;
	}

	public Object execute(Object function, Object... args) throws LeekRunException {
		// System.out.println("[AI] execute function=" + function + " args=" + Arrays.toString(args));
		if (function instanceof ClassLeekValue) {
			return ((ClassLeekValue) function).run(this, null, args);
		}
		if (function instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) function).run(this, null, args);
		}

		// On ne peux pas exécuter ce type de variable
		addSystemLog(AILog.ERROR, Error.CAN_NOT_EXECUTE_VALUE, new Object[] { function });
		return null;
	}

	public boolean check(String functionName, int[] types, Object... arguments) throws LeekRunException {
		if (verifyParameters(types, arguments)) {
			return true;
		}
		String ret = LeekValueType.getParamString(arguments);
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FUNCTION, new String[] { functionName + "(" + ret + ")" });
		return false;
	}

	public IntervalLeekValue interval(boolean minClosed, Object min, boolean maxClosed, Object max) throws LeekRunException {
		if ((min instanceof Long || min instanceof BigIntegerValue) && (max instanceof Long || max instanceof BigIntegerValue)) {
			return new IntegerIntervalLeekValue(this, minClosed, longint(min), maxClosed, longint(max));
		}
		return new RealIntervalLeekValue(this, minClosed, min, maxClosed, max);
	}

	public ArrayLeekValue toArray(int index, Object value) throws LeekRunException {
		if (value instanceof ArrayLeekValue) {
			return (ArrayLeekValue) value;
		}
		addSystemLog(AILog.ERROR, Error.WRONG_ARGUMENT_TYPE, new Object[] {
			String.valueOf(index),
			value,
			StandardClass.getType(this, value).toString(),
			Type.ARRAY.toString() + " (V4+)"
		});
		throw new ClassCastException();
	}

	public MapLeekValue toMap(int index, Object value) throws LeekRunException {
		if (value instanceof MapLeekValue) {
			return (MapLeekValue) value;
		}
		addSystemLog(AILog.ERROR, Error.WRONG_ARGUMENT_TYPE, new Object[] {
			String.valueOf(index),
			value,
			StandardClass.getType(this, value).toString(),
			Type.MAP.toString()
		});
		throw new ClassCastException();
	}

	public FunctionLeekValue toFunction(int index, Object value) throws LeekRunException {
		if (value instanceof FunctionLeekValue) {
			return (FunctionLeekValue) value;
		}
		addSystemLog(AILog.ERROR, Error.WRONG_ARGUMENT_TYPE, new Object[] {
			String.valueOf(index),
			value,
			StandardClass.getType(this, value).toString(),
			Type.FUNCTION.toString()
		});
		throw new ClassCastException();
	}

	public LegacyArrayLeekValue toLegacyArray(int index, Object value) throws LeekRunException {
		if (value instanceof LegacyArrayLeekValue) {
			return (LegacyArrayLeekValue) value;
		}
		addSystemLog(AILog.ERROR, Error.WRONG_ARGUMENT_TYPE, new Object[] {
			String.valueOf(index),
			value,
			StandardClass.getType(this, value).toString(),
			Type.ARRAY.toString() + " (V1-3)"
		});
		return new LegacyArrayLeekValue(this);
	}

	public static boolean verifyParameters(int[] types, Object... parameters) {
		if (types.length != parameters.length) return false;
		for (int i = 0; i < types.length; i++) {
			if (types[i] == -1) continue;
			if (i >= parameters.length || !isType(parameters[i], types[i])) {
				return false;
			}
		}
		return true;
	}

	public static boolean isType(Object value, int type) {
		// value = LeekValueManager.getValue(value);
		switch (type) {
			case BOOLEAN: return value instanceof Boolean;
			case INT: return value instanceof Long;
			case BIG_INT: return value instanceof BigIntegerValue;
			case DOUBLE: return value instanceof Double;
			case STRING: return value instanceof String;
			case NULL: return value == null;
			case LEGACY_ARRAY: return value instanceof LegacyArrayLeekValue;
			case ARRAY: return value instanceof LegacyArrayLeekValue || value instanceof ArrayLeekValue;
			case MAP: return value instanceof MapLeekValue;
			case FUNCTION: return value instanceof FunctionLeekValue;
			case NUMBER: return value instanceof Long || value instanceof Double;
			case INTERVAL: return value instanceof IntervalLeekValue;
			case SET: return value instanceof SetLeekValue;
		}
		return true;
	}

	public ClassLeekValue classOf(Object value) {
		if (value == null) return nullClass;
		if (value instanceof Long) return integerClass;
		if (value instanceof BigIntegerValue) return bigIntegerClass;
		if (value instanceof Double) return realClass;
		if (value instanceof Boolean) return booleanClass;
		if (value instanceof LegacyArrayLeekValue) return legacyArrayClass;
		if (value instanceof ArrayLeekValue) return arrayClass;
		if (value instanceof MapLeekValue) return mapClass;
		if (value instanceof IntervalLeekValue) return intervalClass;
		if (value instanceof SetLeekValue) return setClass;
		if (value instanceof String) return stringClass;
		if (value instanceof ObjectLeekValue) return ((ObjectLeekValue) value).clazz;
		if (value instanceof NativeObjectLeekValue)
			try {
				// System.out.println("get class " + ((NativeObjectLeekValue) value).getClass().getSimpleName());
				return (ClassLeekValue) this.getClass().getField(((NativeObjectLeekValue) value).getClass().getSimpleName()).get(this);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				return valueClass;
			}
		if (value instanceof ClassLeekValue) return classClass;
		if (value instanceof FunctionLeekValue) return functionClass;
		return valueClass;
	}

	public boolean instanceOf(Object value, Object clazz) throws LeekRunException {
		ops(2);
		if (!(clazz instanceof ClassLeekValue)) {
			addSystemLog(AILog.ERROR, Error.INSTANCEOF_MUST_BE_CLASS);
			return false;
		}
		var v = load(value);
		var vClass = classOf(v);
		if (vClass.descendsFrom((ClassLeekValue) clazz)) {
			return true;
		}
		return false;
	}

	public boolean operatorIn(Object container, Object value) throws LeekRunException {
		if (container instanceof RealIntervalLeekValue realInterval) {
			return realInterval.operatorIn(value);
		} else if (container instanceof IntegerIntervalLeekValue interval) {
			return interval.operatorIn(value);
		} else if (container instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) container).operatorIn(value);
		} else if (container instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) container).operatorIn(value);
		} else if (container instanceof MapLeekValue) {
			return ((MapLeekValue) container).operatorIn(value);
		} else if (container instanceof SetLeekValue) {
			return ((SetLeekValue) container).operatorIn(value);
		}

		ops(1);
		addSystemLog(AILog.ERROR, Error.OPERATOR_IN_ON_INVALID_CONTAINER, new Object[] { value });
		return false;
	}

	public long getAnalyzeTime() {
		return analyzeTime;
	}

	public long getCompileTime() {
		return compileTime;
	}

	public void setAnalyzeTime(long analyze_time) {
		this.analyzeTime = analyze_time;
	}

	public void setCompileTime(long compile_time) {
		this.compileTime = compile_time;
	}

	public void setLoadTime(long load_time) {
		this.loadTime = load_time;
	}

	public long getLoadTime() {
		return loadTime;
	}

	public void setLinesFile(File lines) {
		this.filesLines = lines;
	}

	public void setFile(AIFile file) {
		this.file = file;
	}

	public AIFile getFile() {
		return file;
	}

	public int getNextObjectID() {
		return objectID++;
	}

	public Object new_nullClass() {
		return null;
	}

	public boolean new_booleanClass() {
		return false;
	}

	public long new_integerClass() {
		return 0l;
	}
	
	public BigIntegerValue new_bigIntegerClass() throws LeekRunException {
		return new BigIntegerValue(this, 0);
	}

	public double new_realClass() {
		return 0;
	}

	public double new_numberClass() {
		return 0.0;
	}

	public ArrayLeekValue new_arrayClass() {
		return new ArrayLeekValue(this);
	}

	public LegacyArrayLeekValue new_legacyArrayClass() {
		return new LegacyArrayLeekValue(this);
	}

	public MapLeekValue new_mapClass() {
		return new MapLeekValue(this);
	}

	public SetLeekValue new_setClass() throws LeekRunException {
		return new SetLeekValue(this);
	}

	public IntervalLeekValue new_intervalClass() throws LeekRunException {
		return new RealIntervalLeekValue(this);
	}

	public ObjectLeekValue new_objectClass() throws LeekRunException {
		return new ObjectLeekValue(this, this.objectClass);
	}

	public boolean xor(boolean x, boolean y) {
		return (x && !y) || (!x && y);
	}

	public Date getDate() {
		return new Date();
	}
}
