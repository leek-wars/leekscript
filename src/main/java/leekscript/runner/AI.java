package leekscript.runner;

import leekscript.AILog;
import leekscript.ErrorManager;
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
import leekscript.runner.values.LeekValue;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.runner.values.Box;
import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.Type;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Native;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
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

	public static final int ERROR_LOG_COST = 10000;

	protected long mOperations = 0;
	public final static int MAX_OPERATIONS = 20_000_000;
	public long maxOperations = MAX_OPERATIONS;

	protected long mRAM = 0;
	public final static int MAX_RAM = 12_500_000; // in 64 bits "quads" = 100 Mo
	public long maxRAM = MAX_RAM;

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
	public final ClassLeekValue realClass;
	public final ClassLeekValue numberClass;
	public final ClassLeekValue arrayClass;
	public final ClassLeekValue mapClass;
	public final ClassLeekValue stringClass;
	public final ClassLeekValue objectClass;
	public final ClassLeekValue functionClass;
	public final ClassLeekValue classClass;
	public final ClassLeekValue jsonClass;
	public final ClassLeekValue systemClass;

	public class NativeObjectLeekValue {

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

		public String string(Set<Object> visited) throws LeekRunException {
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
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {}


			var classes = new ArrayList<Class<?>>();
			Class<?> current = getClass();
			while (current != null) {
				classes.add(0, current);
				current = current.getSuperclass();
			}

			var fields = new ArrayList<Field>();
			for (var clazz : classes) {
				for (var f : clazz.getDeclaredFields()) {
					if (f.isSynthetic()) continue;
					f.setAccessible(true);
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
					f.setAccessible(true);
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

		@Override
		@SuppressWarnings("deprecated")
		protected void finalize() throws Throwable {
			super.finalize();
			decreaseRAM(2 * size());
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
		arrayClass = new ClassLeekValue(this, "Array", valueClass);
		mapClass = new ClassLeekValue(this, "Map", valueClass);
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
		// System.gc();
		// try {
		// 	Thread.sleep(0, 1);
		// } catch (InterruptedException e) {
		// 	e.printStackTrace();
		// }
		return mRAM;
	}

	public long getMaxRAM() {
		return maxRAM;
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

	public String ops(String x, int nb) throws LeekRunException {
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

	public void increaseRAM(int ram) throws LeekRunException {
		mRAM += ram;
		if (mRAM > maxRAM) {
			// System.out.println("RAM before = " + mRAM);
			long ramBefore = mRAM;
			System.gc();
			try {
				Thread.sleep(0, 1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Runtime.getRuntime().runFinalization();
			// System.out.println("RAM after  = " + mRAM);
			if (mRAM > maxRAM) {
				getLogs().addLog(AILog.WARNING, "[RAM error] RAM before: " + ramBefore + " RAM after: " + mRAM);
				throw new LeekRunException(Error.OUT_OF_MEMORY);
			}
		}
	}

	public void decreaseRAM(int ram) {
		mRAM -= ram;
	}

	protected void nothing(Object obj) {

	}

	public GenericArrayLeekValue newArray() {
		if (version >= 4) {
			return new ArrayLeekValue(this);
		} else {
			return new LegacyArrayLeekValue();
		}
	}

	public GenericArrayLeekValue newArray(int capacity) {
		if (version >= 4) {
			return new ArrayLeekValue(this, capacity);
		} else {
			return new LegacyArrayLeekValue();
		}
	}

	public GenericMapLeekValue newMap(AI ai) {
		if (version >= 4) {
			return new MapLeekValue(ai);
		} else {
			return new LegacyArrayLeekValue();
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

	public String getErrorMessage(Throwable e) {
		return getErrorMessage(e.getStackTrace());
	}

	protected String getErrorLocalisation(int javaLine) {
		if (mLinesMapping.isEmpty() && this.filesLines != null && this.filesLines.exists()) {
			try (Stream<String> stream = Files.lines(this.filesLines.toPath())) {
				stream.forEach(l -> {
					var parts = l.split(" ");
					mLinesMapping.put(Integer.parseInt(parts[0]), new LineMapping(Integer.parseInt(parts[2]), Integer.parseInt(parts[1])));
				});
			} catch (IOException e) {}
			thisObject = getAIString();
		}
		var lineMapping = mLinesMapping.get(javaLine);
		if (lineMapping != null) {
			var files = getErrorFiles();
			var f = lineMapping.getAI();
			String file = f < files.length ? files[f] : "?";
			return "\t▶ AI " + file + ", line " + lineMapping.getLeekScriptLine() + "\n"; // + ", java " + line;
		}
		return "";
	}

	public void addSystemLog(int type, Error error) throws LeekRunException {
		addSystemLog(type, error.ordinal(), null);
	}

	public void addSystemLog(int type, int error) throws LeekRunException {
		addSystemLog(type, error, null);
	}

	public void addSystemLog(int type, Error error, Object[] parameters) throws LeekRunException {
		addSystemLog(type, error.ordinal(), parameters);
	}

	public void addSystemLog(int type, int error, Object[] parameters) throws LeekRunException {
		ops(AI.ERROR_LOG_COST);
		if (type == AILog.WARNING)
			type = AILog.SWARNING;
		else if (type == AILog.ERROR)
			type = AILog.SERROR;
		else if (type == AILog.STANDARD)
			type = AILog.SSTANDARD;

		logs.addSystemLog(this, type, getErrorMessage(Thread.currentThread().getStackTrace()), error, parameters);
	}

	protected String[] getErrorFiles() { return null; }

	protected String getAIString() { return ""; }

	public abstract Object runIA() throws LeekRunException;

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
		if (x instanceof ArrayLeekValue) {
			if (y instanceof ArrayLeekValue) {
				return ((ArrayLeekValue) x).eq(this, (ArrayLeekValue) y);
			}
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
				try {
					ops(s.length());
					return n == Double.parseDouble(s);
				} catch (Exception e) {
					return false;
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
			return ((Number) x).doubleValue() < ((Number) y).doubleValue();
		}
		return longint(x) < longint(y);
	}

	public boolean more(Object x, Object y) throws LeekRunException {
		if (x instanceof Number && y instanceof Number) {
			if (x instanceof Long && y instanceof Long) {
				return (Long) x > (Long) y;
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
			return ((Number) x).doubleValue() <= ((Number) y).doubleValue();
		}
		return longint(x) <= longint(y);
	}

	public boolean moreequals(Object x, Object y) throws LeekRunException {
		if (x instanceof Number && y instanceof Number) {
			if (x instanceof Long && y instanceof Long) {
				return (Long) x >= (Long) y;
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
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).size() != 0;
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

	public double real(Object value) throws LeekRunException {
		if (value instanceof Double) {
			return (Double) value;
		} else if (value instanceof Long) {
			return (Long) value;
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
		return integer(value);
	}

	public boolean not(Object value) throws LeekRunException {
		return !bool(value);
	}

	public Object minus(Object value) throws LeekRunException {
		if (value instanceof Double) return -((Double) value);
		return -longint(value);
	}

	public long bnot(Object value) throws LeekRunException {
		return LeekValueManager.bnot(this, value);
	}

	public long bor(Object x, Object y) throws LeekRunException {
		return longint(x) | longint(y);
	}

	public long band(Object x, Object y) throws LeekRunException {
		return longint(x) & longint(y);
	}

	public long bxor(Object x, Object y) throws LeekRunException {
		return longint(x) ^ longint(y);
	}

	public long shl(Object x, Object y) throws LeekRunException {
		return longint(x) << longint(y);
	}

	public long shr(Object x, Object y) throws LeekRunException {
		return longint(x) >> longint(y);
	}

	public long ushr(Object x, Object y) throws LeekRunException {
		return longint(x) >>> longint(y);
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

				var retour = new LegacyArrayLeekValue();
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

			var retour = new LegacyArrayLeekValue();
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

			var retour = new LegacyArrayLeekValue();

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

		if (x instanceof Double || y instanceof Double) {
			return real(x) + real(y);
		}
		return longint(x) + longint(y);
	}

	public Object sub(Object x, Object y) throws LeekRunException {
		if (x instanceof Double || y instanceof Double) {
			return real(x) - real(y);
		}
		return longint(x) - longint(y);
	}

	public Object mul(Object x, Object y) throws LeekRunException {
		if (x instanceof Double || y instanceof Double) {
			return real(x) * real(y);
		}
		return longint(x) * longint(y);
	}

	public Object div(Object x, Object y) throws LeekRunException {
		double real_y = real(y);
		if (version == 1 && real_y == 0) {
			addSystemLog(AILog.ERROR, Error.DIVISION_BY_ZERO);
			return null;
		}
		return real(x) / real_y;
	}

	public long intdiv(Object x, Object y) throws LeekRunException {
		return longint(x) / longint(y);
	}

	public Object mod(Object x, Object y) throws LeekRunException {
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

	public Number pow(Object x, Object y) throws LeekRunException {
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

	public Object div_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).div_eq(y);
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

	public long bor_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).bor_eq(y);
		}
		return 0;
	}

	public long band_eq(Object x, Object y) throws LeekRunException {
		if (x instanceof Box) {
			return ((Box) x).band_eq(y);
		}
		return 0;
	}

	public long bxor_eq(Object x, Object y) throws LeekRunException {
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
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).string(this, new HashSet<Object>());
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.string(new HashSet<Object>());
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).string(this, new HashSet<Object>());
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).getString(this, new HashSet<Object>());
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).getString(this, new HashSet<Object>());
		} else if (value instanceof String) {
			return (String) value;
		} else if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).getString(this);
		} else if (value instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) value).getString(this);
		} else if (value instanceof Box) {
			return string(((Box) value).get());
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
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).string(this, visited);
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.string(visited);
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).string(this, visited);
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).getString(this, visited);
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).getString(this, visited);
		} else if (value instanceof String) {
			return (String) value;
		} else if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).getString(this);
		} else if (value instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) value).getString(this);
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
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).export(this, new HashSet<Object>());
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.export(new HashSet<Object>());
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).export(this, new HashSet<Object>());
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).getString(this, new HashSet<Object>());
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).getString(this, new HashSet<Object>());
		} else if (value instanceof String) {
			return "\"" + value + "\"";
		} else if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).getString(this);
		} else if (value instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) value).getString(this);
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
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else if (value instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) value).export(this, visited);
		} else if (value instanceof NativeObjectLeekValue o) {
			return o.export(visited);
		} else if (value instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) value).export(this, visited);
		} else if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).getString(this, visited);
		} else if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).getString(this, visited);
		} else if (value instanceof String) {
			return "\"" + value + "\"";
		} else if (value instanceof ClassLeekValue) {
			return ((ClassLeekValue) value).getString(this);
		} else if (value instanceof FunctionLeekValue) {
			return ((FunctionLeekValue) value).getString(this);
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
		return !(value instanceof ArrayLeekValue || value instanceof MapLeekValue || value instanceof LegacyArrayLeekValue || value instanceof ObjectLeekValue || value instanceof NativeObjectLeekValue);
	}

	public boolean isIterable(Object value) throws LeekRunException {
		boolean ok = value instanceof LegacyArrayLeekValue || value instanceof ArrayLeekValue || value instanceof MapLeekValue;
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
		if (value instanceof NativeObjectLeekValue) {
			try {
				var f = value.getClass().getField(field);
				f.setAccessible(true);
				return f.get(value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				// Method ?
				try {
					var clazz = (ClassLeekValue) this.getClass().getField(value.getClass().getSimpleName()).get(this);
					var method = clazz.genericMethods.get(field);
					if (method != null) return method;
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {}
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { value, field });
		return null;
	}

	public Object initField(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).initField(field, value);
		}
		try {
			object.getClass().getField(field).set(object, value);
			return value;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object setField(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).setField(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).setField(field, value);
		}
		if (object instanceof NativeObjectLeekValue) {
			try {
				var f = object.getClass().getField(field);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
					return null;
				}
				// f.setAccessible(true);
				f.set(object, value);
				return value;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				e.printStackTrace(System.out);
			}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_inc(Object object, String field) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_inc(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).increment();
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = add(f.get(object), 1l);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}

		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_pre_inc(Object object, String field) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_pre_inc(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).pre_increment();
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = add(f.get(object), 1l);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_dec(Object object, String field) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_dec(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).decrement();
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = sub(f.get(object), 1l);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_pre_dec(Object object, String field) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_pre_dec(field);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).pre_decrement();
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = sub(f.get(object), 1l);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_add_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_add_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).add_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = add(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_sub_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_sub_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).sub_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = sub(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_mul_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_mul_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).mul_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = mul(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_pow_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_pow_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).pow_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = pow(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_div_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_div_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).div_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = div(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_intdiv_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_intdiv_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).intdiv_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = intdiv(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_mod_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_mod_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).mod_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = mod(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_bor_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_bor_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).bor_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = bor(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_band_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_band_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).band_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = band(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_bxor_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_bxor_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).bxor_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = bxor(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_shl_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_shl_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).shl_eq(value);
		}
		try {
			var f = object.getClass().getField(field);
			if (f.isAnnotationPresent(Final.class)) {
				this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
				return null;
			}
			var v = shl(f.get(object), value);
			f.set(object, v);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_shr_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_shr_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).shr_eq(value);
		}
		if (object instanceof NativeObjectLeekValue) {
			try {
				var f = object.getClass().getField(field);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
					return null;
				}
				var v = shr(f.get(object), value);
				f.set(object, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object field_ushr_eq(Object object, String field, Object value) throws LeekRunException {
		if (object instanceof ObjectLeekValue) {
			return ((ObjectLeekValue) object).field_ushr_eq(field, value);
		}
		if (object instanceof ClassLeekValue) {
			return ((ClassLeekValue) object).getFieldL(field).ushr_eq(value);
		}
		if (object instanceof NativeObjectLeekValue) {
			try {
				var f = object.getClass().getField(field);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { object.getClass().getName(), field });
					return null;
				}
				var v = ushr(f.get(object), value);
				f.set(object, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FIELD, new Object[] { object, field });
		return null;
	}

	public Object put(Object array, Object key, Object value) throws LeekRunException {
		if (array instanceof LegacyArrayLeekValue) {
			return ((LegacyArrayLeekValue) array).put(this, key, value);
		}
		if (array instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) array).put(this, key, value);
		}
		if (array instanceof MapLeekValue) {
			return ((MapLeekValue) array).put(this, key, value);
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				f.set(array, value);
				return value;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_inc(Object array, Object key) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = f.get(array);
				f.set(array, add(v, 1l));
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_pre_inc(Object array, Object key) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = add(f.get(array), 1l);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_dec(Object array, Object key) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = f.get(array);
				f.set(array, sub(v, 1l));
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_pre_dec(Object array, Object key) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = sub(f.get(array), 1l);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_add_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = add(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_sub_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = sub(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_mul_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = mul(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_pow_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = pow(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_mod_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = mod(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_div_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = div(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public long put_intdiv_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return 0;
				}
				var v = intdiv(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return 0;
	}

	public Object put_bor_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = bor(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_band_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = band(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_shl_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = shl(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_shr_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = shr(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_ushr_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = ushr(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
		}
		if (version >= 3)
			addSystemLog(AILog.ERROR, Error.VALUE_IS_NOT_AN_ARRAY, new Object[] { array });
		return null;
	}

	public Object put_bxor_eq(Object array, Object key, Object value) throws LeekRunException {
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
				var k = string(key);
				var f = array.getClass().getField(k);
				if (f.isAnnotationPresent(Final.class)) {
					this.addSystemLog(AILog.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD, new String[] { array.getClass().getName(), k });
					return null;
				}
				var v = bxor(f.get(array), value);
				f.set(array, v);
				return v;
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {}
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
			return ((LegacyArrayLeekValue) value).get(this, index);
		}
		if (value instanceof ArrayLeekValue) {
			return ((ArrayLeekValue) value).get(this, index);
		}
		if (value instanceof MapLeekValue) {
			return ((MapLeekValue) value).get(this, index);
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
				return value.getClass().getField(string(index)).get(value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				// Method ?
				try {
					var clazz = (ClassLeekValue) this.getClass().getField(value.getClass().getSimpleName()).get(this);
					var method = clazz.genericMethods.get(string(index));
					if (method != null) return method;
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {}
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
			var m = value.getClass().getMethod("u_" + method);
			m.setAccessible(true);
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
				var types = new Class<?>[args.length];
				for (int i = 0; i < args.length; ++i) {
					types[i] = Object.class;
				}
				var m = value.getClass().getMethod(method, types);
				// m.setAccessible(true);
				return m.invoke(value, args);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				try {
					return execute(value.getClass().getField(field).get(value), args);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {}
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
		String ret = LeekValue.getParamString(arguments);
		addSystemLog(AILog.ERROR, Error.UNKNOWN_FUNCTION, new String[] { functionName + "(" + ret + ")" });
		return false;
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
		return new LegacyArrayLeekValue();
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
			case DOUBLE: return value instanceof Double;
			case STRING: return value instanceof String;
			case NULL: return value == null;
			case LEGACY_ARRAY: return value instanceof LegacyArrayLeekValue;
			case ARRAY: return value instanceof LegacyArrayLeekValue || value instanceof ArrayLeekValue;
			case MAP: return value instanceof MapLeekValue;
			case FUNCTION: return value instanceof FunctionLeekValue;
			case NUMBER: return value instanceof Long || value instanceof Double;
		}
		return true;
	}

	public ClassLeekValue classOf(Object value) {
		if (value == null) return nullClass;
		if (value instanceof Long) return integerClass;
		if (value instanceof Double) return realClass;
		if (value instanceof Boolean) return booleanClass;
		if (value instanceof LegacyArrayLeekValue || value instanceof ArrayLeekValue) return arrayClass;
		if (value instanceof MapLeekValue) return mapClass;
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
}
