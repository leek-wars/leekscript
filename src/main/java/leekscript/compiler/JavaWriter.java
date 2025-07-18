package leekscript.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.common.CompoundType;
import leekscript.common.FunctionType;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.Expression;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;

public class JavaWriter {

	private final StringBuilder mCode;
	private final StringBuilder mLinesFile;
	private int mLine;
	private final TreeMap<Integer, LineMapping> mLines = new TreeMap<>();
	private final HashMap<AIFile, Integer> mFiles = new HashMap<>();
	private final ArrayList<AIFile> mFilesList = new ArrayList<>();
	private final boolean mWithDebug;
	private final String className;
	public AbstractLeekBlock currentBlock = null;
	public HashMap<String, ArrayList<CallableVersion>> genericFunctions = new HashMap<>();
	public HashSet<LeekFunctions> anonymousSystemFunctions = new HashSet<>();
	private boolean operationsEnabled = true;
	public boolean lastInstruction = false;
	public Options options;

	public JavaWriter(boolean debug, String className, boolean enableOperations) {
		mCode = new StringBuilder();
		mLinesFile = new StringBuilder();
		mLine = 1;
		mWithDebug = debug;
		this.className = className;
		this.operationsEnabled = enableOperations;
	}

	public boolean hasDebug() {
		return mWithDebug;
	}

	public void addLine(String datas, int line, AIFile ai) {
		mCode.append(datas).append("\n");
		int fileIndex = getFileIndex(ai);
		mLines.put(mLine, new LineMapping(line, fileIndex));
		mLine++;
	}

	public void addLine(String datas, Location location) {
		mCode.append(datas).append("\n");
		int fileIndex = getFileIndex(location.getFile());
		mLines.put(mLine, new LineMapping(location.getStartLine(), fileIndex));
		mLine++;
	}

	private int getFileIndex(AIFile ai) {
		var index = mFiles.get(ai);
		if (index != null) return index;
		var new_index = mFiles.size();
		mFiles.put(ai, new_index);
		mFilesList.add(ai);
		return new_index;
	}

	public void addLine(String datas) {
		mCode.append(datas).append("\n");
		mLine++;
	}

	public void addLine() {
		mCode.append("\n");
		mLine++;
	}

	public void addCode(String datas) {
		mCode.append(datas);
	}

	public AICode getCode() {
		return new AICode(mCode.toString(), mLinesFile.toString(), mLines, mFilesList);
	}

	public void writeErrorFunction(IACompiler comp, String ai) {
		String aiJson = JSON.toJSONString(ai);
		for (var e : mLines.entrySet()) {
			var line = e.getValue();
			mLinesFile.append(e.getKey() + " " + line.getAI() + " " + line.getLeekScriptLine() + "\n");
			// System.out.println(l.mAI.getPath() + ":" + l.mCodeLine + " -> " + l.mJavaLine);
		}
		mCode.append("protected String getAIString() { return ");
		mCode.append(aiJson);
		mCode.append(";}\n");

		mCode.append("protected String[] getErrorFiles() { return new String[] {");
		for (var f : mFilesList) {
			mCode.append("\"" + f.getPath().replaceAll("\\\\/", "/").replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"");
			mCode.append(", ");
		}
		mCode.append("};}\n\n");

		mCode.append("protected int[] getErrorFilesID() { return new int[] {");
		for (var f : mFilesList) {
			mCode.append(f.getId());
			mCode.append(", ");
		}
		mCode.append("};}\n\n");
	}

	public void addCounter(int count) {
		if (operationsEnabled) {
			addCode("ops(" + count + ");");
		}
	}

	public int getCurrentLine() {
		return mLine;
	}

	public void addPosition(Token token) {
		var index = getFileIndex(token.getLocation().getFile());
		mLines.put(mLine, new LineMapping(token.getLocation().getStartLine(), index));
	}

	public String getAIThis() {
		return className + ".this";
	}

	public String getClassName() {
		return className;
	}

	public void getBoolean(MainLeekBlock mainblock, Expression expression) {
		if (expression.getType() == Type.BOOL) {
			expression.writeJavaCode(mainblock, this);
		} else if (expression.getType() == Type.INT) {
			addCode("((");
			expression.writeJavaCode(mainblock, this);
			addCode(") != 0l)");
		} else {
			addCode("bool(");
			expression.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void getString(MainLeekBlock mainblock, Expression expression) {
		if (expression.getType() == Type.STRING) {
			expression.writeJavaCode(mainblock, this);
		} else {
			addCode("string(");
			expression.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void getInt(MainLeekBlock mainblock, Expression expression) {
		if (expression.getType() == Type.INT) {
			expression.writeJavaCode(mainblock, this);
		} else {
			addCode("longint(");
			expression.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void compileLoad(MainLeekBlock mainblock, Expression expr) {
		if (expr.getType() == Type.NULL || expr.getType() == Type.BOOL || expr.getType().isNumber() || expr.getType() == Type.STRING || expr.getType().isArray()) {
			expr.writeJavaCode(mainblock, this);
		} else {
			addCode("load(");
			expr.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void compileClone(MainLeekBlock mainblock, Expression expr) {
		if (expr.getType() == Type.NULL || expr.getType() == Type.BOOL || expr.getType().isNumber() || expr.getType() == Type.STRING) {
			expr.writeJavaCode(mainblock, this);
		} else {
			addCode("copy(");
			expr.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void compileConvert(MainLeekBlock mainblock, int index, Expression value, Type type) {

//		 System.out.println("convert " + value.getType().getJavaName(4) + " to " + type.getJavaName(4));
		if (type == Type.REAL && value.getType().isCompoundNumber()) {
			addCode("(");
			value.writeJavaCode(mainblock, this);
			addCode(").doubleValue()");
			return;
		}
		if (type == Type.INT && value.getType().isCompoundNumber()) {
			addCode("(");
			value.writeJavaCode(mainblock, this);
			addCode(").longValue()");
			return;
		}
		if (type == Type.BIG_INT && value.getType().isNumber()) {
			addCode("BigIntegerValue.valueOf(" + getAIThis() + ", ");
			value.writeJavaCode(mainblock, this);
			addCode(")");
			return;
		}
		var cast = type.accepts(value.getType());
		// System.out.println("cast = " + cast);
		if (cast.ordinal() <= CastType.EQUALS.ordinal()) {
			value.writeJavaCode(mainblock, this);
			return;
		}
		if (type.isArray()) {
			addCode(mainblock.getVersion() >= 4 ? "toArray(" : "toLegacyArray(");
			addCode(index + ", ");
			value.writeJavaCode(mainblock, this);
			addCode(")");
			return;
		}
		else if (type.isMap()) {
			addCode("toMap(");
			addCode(index + ", ");
			value.writeJavaCode(mainblock, this);
			addCode(")");
			return;
		}
		else if (type == Type.INT) {
			if (value.getType() == Type.REAL) {
				addCode("(long) (");
				value.writeJavaCode(mainblock, this);
				addCode(")");
				return;
			} else {
				addCode("longint(");
				value.writeJavaCode(mainblock, this);
				addCode(")");
				return;
			}
		} else if (type == Type.REAL) {
			if (value.getType() == Type.INT) {
				addCode("(double) (");
				value.writeJavaCode(mainblock, this);
				addCode(")");
				return;
			} else {
				addCode("real(");
				value.writeJavaCode(mainblock, this);
				addCode(")");
				return;
			}
		} else if (type == Type.BIG_INT) {
			addCode("bigint(");
			value.writeJavaCode(mainblock, this);
			addCode(")");
			return;
		}
		// int?, real?, big_integer?
		if (type instanceof CompoundType ct) {
			if (ct.getTypes().size() == 2) {
				if (ct.getTypes().stream().anyMatch(t -> t == Type.NULL)) {
					for (var t : ct.getTypes()) {
						if (t != Type.NULL) {
							if (t == Type.REAL && value.getType() == Type.INT) { // int -> Double
								addCode("((double) (");
								value.writeJavaCode(mainblock, this);
								addCode("))");
								return;
							}
							if (t == Type.INT && value.getType() == Type.REAL) { // double -> Integer
								addCode("((long) (");
								value.writeJavaCode(mainblock, this);
								addCode("))");
								return;
							}
							if (t == Type.INT && value.getType() == Type.BIG_INT) { // bigint -> Integer
								addCode("((");
								value.writeJavaCode(mainblock, this);
								addCode(").longValue())");
								return;
							}
							if (t == Type.REAL && value.getType() == Type.BIG_INT) { // bigint -> double
								addCode("((");
								value.writeJavaCode(mainblock, this);
								addCode(").doubleValue())");
								return;
							}
							if (t == Type.BIG_INT && value.getType().isNumber()) { // int/double -> bigint
								addCode("(BigIntegerValue.valueOf(" + getAIThis() + ", ");
								value.writeJavaCode(mainblock, this);
								addCode("))");
								return;
							}
						}
					}
				}
			}
		}
		if (type instanceof FunctionType ft1 && value.getType() instanceof FunctionType ft2) {
			addCode("((" + type.getJavaPrimitiveName(mainblock.getVersion()) + ")");
			addCode(" (Object) (");
			value.writeJavaCode(mainblock, this);
			addCode("))");
			return;
		}
		addCode("((" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") (");
		value.writeJavaCode(mainblock, this);
		addCode("))");
	}

	public String generateGenericFunction(ArrayList<CallableVersion> versions) {
		String key = versions.get(0).function.getName();
		for (var version : versions) key += "_" + version.getParametersSignature();
		genericFunctions.put(key, versions);
		return key;
	}

	public void generateAnonymousSystemFunction(LeekFunctions system_function) {
		anonymousSystemFunctions.add(system_function);
		for (var version : system_function.getVersions()) {
			var list = new ArrayList<CallableVersion>();
			list.add(version);
			generateGenericFunction(list);
		}
	}

	/**
	 * Writes generic functions (generate checks), example :
	 * var x = unknown(...)
	 * cos(x)
	 * Generates cos_1(Object x)
	 */
	public void writeGenericFunctions(MainLeekBlock block) {

		for (var entry : genericFunctions.entrySet()) {

			var signature = entry.getKey();
			var versions = entry.getValue();
			var first_version = versions.get(0);
			var function = first_version.function;
			var return_type = Type.compound(versions.stream().map(v -> v.return_type).collect(Collectors.toCollection(HashSet::new)));

			addCode("private " + return_type.getJavaPrimitiveName(block.getVersion()) + " " + function.getStandardClass() + "_" + signature + "(");
			for (int a = 0; a < first_version.arguments.length; ++a) {
				if (a > 0) addCode(", ");
				addCode("Object a" + a);
			}
			addLine(") throws LeekRunException {");
			
			// Conflicting versions ?
			if (versions.size() > 1) {
				for (int i = 1; i < versions.size(); ++i) {
					var other_version = versions.get(i);
					addCode("if (");
					for (int a = 0; a < other_version.arguments.length; ++a) {
						if (a > 0) addCode(" && ");
						addCode("a" + a + " instanceof " + other_version.arguments[a].getJavaName(block.getVersion()) + " x" + a);
					}
					addLine(") {");
					writeFunctionCall(block, other_version, false);
					addLine("}");
				}
			}

			int a = 0;
			for (var argument : first_version.arguments) {
				if (argument != Type.ANY) {
					addLine(argument.getJavaPrimitiveName(block.getVersion()) + " x" + a + "; try { x" + a + " = " + convert(a + 1, "a" + a, argument, block.getVersion()) + "; } catch (ClassCastException e) { return " + first_version.return_type.getDefaultValue(this, block.getVersion()) + "; }");
				}
				a++;
			}
			writeFunctionCall(block, first_version, false);
			addLine("}");
			addLine();
		}
	}

	private void writeFunctionCall(MainLeekBlock block, CallableVersion version, boolean cast) {
		var function_name = version.function.getName();
		if ((version.return_type.isArray() || version.return_type.isArrayOrNull()) && block.getVersion() <= 3) {
			function_name += "_v1_3";
		}
		if (version.function.isStatic()) {
			addCode("return " + version.function.getStandardClass() + "Class." + function_name + "(");
		} else {
			addCode("return x0." + function_name + "(");
		}
		ArrayList<String> args = new ArrayList<>();
		args.add("this");
		int start_index = version.function.isStatic() ? 0 : 1;
		for (int a = start_index; a < version.arguments.length; ++a) {
			if (cast) {
				args.add("(" + version.arguments[a].getJavaName(block.getVersion()) + ") a" + a);
			} else if (version.arguments[a] != Type.ANY) {
				args.add("x" + a);
			} else {
				args.add("a" + a);
			}
		}
		addCode(String.join(", ", args));
		addLine(");");
	}

	/**
	 * Write anonymous functions, example :
	 * var a = [ sin, cos ]
	 * a[x](y)
	 */
	public void writeAnonymousSystemFunctions(MainLeekBlock block) {

		for (var function : anonymousSystemFunctions) {
			addLine("private FunctionLeekValue " + function.getStandardClass() + "_" + function.getName() + " = new FunctionLeekValue(" + function.getVersions()[0].arguments.length + ", \"#Function " + function.getName() + "\") { public Object run(AI ai, Object thiz, Object... values) throws LeekRunException {");
			if (operationsEnabled && function.getOperations() >= 0) {
				addLine("ops(" + function.getOperations() + ");");
			}
			if (function.getVersions().length > 1) {
				for (var version : function.getVersions()) {
					addCode("if (values.length == " + version.arguments.length + ") return " + function.getStandardClass() + "_" + function.getName() + "_" + version.getParametersSignature() + "(");
					for (var a = 0; a < version.arguments.length; ++a) {
						if (a > 0) addCode(", ");
						if (block.getVersion() == 1) {
							addCode("load(values[" + a + "])");
						} else {
							addCode("values[" + a + "]");
						}
					}
					addLine(");");
				}
			}
			addCode("if (values.length < " + function.getVersions()[0].arguments.length + ") return null;");
			addCode("return " + function.getStandardClass() + "_" + function.getName() + "_" + function.getVersions()[0].getParametersSignature() + "(");
			for (var a = 0; a < function.getVersions()[0].arguments.length; ++a) {
				if (a > 0) addCode(", ");
				if (block.getVersion() == 1) {
					addCode("load(values[" + a + "])");
				} else {
					addCode("values[" + a + "]");
				}
			}
			addLine(");");
			addLine("}};");
			addLine();
		}
	}

	private String convert(int index, String v, Type type, int version) {
		if (type.isArray()) {
			if (version >= 4) return "toArray(" + index + ", " + v + ")";
			else return "toLegacyArray(" + index + ", " + v + ")";
		}
		if (type.isMap()) {
			return "toMap(" + index + ", " + v + ")";
		}
		if (type instanceof FunctionType) {
			return "toFunction(" + index + ", " + v + ")";
		}
		if (type == Type.INT) {
			return "longint(" + v + ")";
		}
		if (type == Type.BIG_INT) {
			return "bigint(" + v + ")";
		}
		if (type == Type.REAL) {
			return "real(" + v + ")";
		}
		if (type == Type.STRING) {
			return "string(" + v + ")";
		}
		if (type instanceof CompoundType ct) {
			if (ct.getTypes().size() == 2 && ct.getTypes().stream().anyMatch(t -> t == Type.NULL)) {
				for (var t : ct.getTypes()) {
					if (t != Type.NULL) {
						if (t == Type.BIG_INT) return "BigIntegerValue.valueOf(" + getAIThis() + ", " + v + ")";
						if (t == Type.INT) return "(Long) " + v;
						if (t == Type.REAL) return "(Double) " + v;
						if (t == Type.BOOL) return "(Boolean) " + v;
						if (t == Type.STRING) return "(String) " + v;
					}
				}
			}
		}
		return "(" + type.getJavaName(version) + ") " + v;
	}

	public void cast(MainLeekBlock mainblock, Expression expr, Type type) {
		var castType = type.accepts(expr.getType());
		if (castType.ordinal() > CastType.EQUALS.ordinal()) {
			addCode("((" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") (");
		}
		expr.writeJavaCode(mainblock, this);
		if (castType.ordinal() > CastType.EQUALS.ordinal()) {
			addCode("))");
		}
	}

	public boolean isInConstructor() {
		return currentBlock != null && currentBlock.isInConstructor();
	}

	public boolean isOperationsEnabled() {
		return operationsEnabled;
	}

	public Options getOptions() {
		return options;
	}
}
