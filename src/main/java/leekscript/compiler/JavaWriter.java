package leekscript.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;

import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.common.Type;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.AbstractExpression;

public class JavaWriter {
	private final StringBuilder mCode;
	private final StringBuilder mLinesFile;
	private int mLine;
	private final TreeMap<Integer, LineMapping> mLines = new TreeMap<>();
	private final HashMap<AIFile<?>, Integer> mFiles = new HashMap<>();
	private final ArrayList<AIFile<?>> mFilesList = new ArrayList<>();
	private final boolean mWithDebug;
	private final String className;
	public AbstractLeekBlock currentBlock = null;

	public JavaWriter(boolean debug, String className) {
		mCode = new StringBuilder();
		mLinesFile = new StringBuilder();
		mLine = 1;
		mWithDebug = debug;
		this.className = className;
	}

	public boolean hasDebug() {
		return mWithDebug;
	}

	public void addLine(String datas, int line, AIFile<?> ai) {
		mCode.append(datas).append("\n");
		int fileIndex = getFileIndex(ai);
		mLines.put(mLine, new LineMapping(line, fileIndex));
		mLine++;
	}

	private int getFileIndex(AIFile<?> ai) {
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
		return new AICode(mCode.toString(), mLinesFile.toString());
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
		mCode.append("};}\n");
	}

	public void addCounter(int count) {
		addCode("ops(" + count + ");");
	}

	public int getCurrentLine() {
		return mLine;
	}

	public void addPosition(IAWord token) {
		var index = getFileIndex(token.getAI());
		mLines.put(mLine, new LineMapping(token.getLine(), index));
	}

	public String getAIThis() {
		return className + ".this";
	}

	public String getClassName() {
		return className;
	}

	public void getBoolean(MainLeekBlock mainblock, AbstractExpression expression) {
		if (expression.getType() == Type.BOOL) {
			expression.writeJavaCode(mainblock, this);
		} else if (expression.getType() == Type.INT) {
			addCode("((");
			expression.writeJavaCode(mainblock, this);
			addCode(") != 0)");
		} else {
			addCode("bool(");
			expression.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void getString(MainLeekBlock mainblock, AbstractExpression expression) {
		if (expression.getType() == Type.STRING) {
			expression.writeJavaCode(mainblock, this);
		} else {
			addCode("string(");
			expression.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void getInt(MainLeekBlock mainblock, AbstractExpression expression) {
		if (expression.getType() == Type.INT) {
			expression.writeJavaCode(mainblock, this);
		} else {
			addCode("integer(");
			expression.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void compileLoad(MainLeekBlock mainblock, AbstractExpression expr) {
		if (expr.getType() == Type.NULL || expr.getType() == Type.BOOL || expr.getType().isNumber() || expr.getType() == Type.STRING || expr.getType() == Type.ARRAY) {
			expr.writeJavaCode(mainblock, this);
		} else {
			addCode("load(");
			expr.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}

	public void compileClone(MainLeekBlock mainblock, AbstractExpression expr) {
		if (expr.getType() == Type.NULL || expr.getType() == Type.BOOL || expr.getType().isNumber() || expr.getType() == Type.STRING) {
			expr.writeJavaCode(mainblock, this);
		} else {
			addCode("copy(");
			expr.writeJavaCode(mainblock, this);
			addCode(")");
		}
	}
}
