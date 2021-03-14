package leekscript.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class JavaWriter {
	private final StringBuilder mCode;
	private int mLine;
	private final TreeMap<Integer, Line> mLines = new TreeMap<>();
	private final HashMap<AIFile<?>, Integer> mFiles = new HashMap<>();
	private final ArrayList<AIFile<?>> mFilesList = new ArrayList<>();
	private final boolean mWithDebug;

	private class Line {
		private final int mJavaLine;
		private final int mCodeLine;
		private final int mAI;

		public Line(int java_line, int code_line, int ai) {
			mJavaLine = java_line;
			mCodeLine = code_line;
			mAI = ai;
		}
	}

	public JavaWriter(boolean debug) {
		mCode = new StringBuilder();
		mLine = 1;
		mWithDebug = debug;
	}

	public boolean hasDebug() {
		return mWithDebug;
	}

	public void addLine(String datas, int line, AIFile<?> ai) {
		mCode.append(datas).append("\n");
		int fileIndex = getFileIndex(ai);
		mLines.put(mLine, new Line(mLine, line, fileIndex));
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

	public String getJavaCode() {
		return mCode.toString();
	}

	public void writeErrorFunction(IACompiler comp, String ai) {
		mCode.append("protected String[] getErrorString() { return new String[] {");

		String aiJson = JSON.toJSONString(ai);
		for (Line l : mLines.values()) {
			JSONArray array = new JSONArray();
			array.add(l.mJavaLine);
			array.add(l.mAI);
			array.add(l.mCodeLine);
			mCode.append(JSON.toJSONString(array.toJSONString()));
			mCode.append(", ");
			// System.out.println(l.mAI.getPath() + ":" + l.mCodeLine + " -> " + l.mJavaLine);
		}
		mCode.append("};}\nprotected String getAItring() { return ");
		mCode.append(aiJson);
		mCode.append(";}\n");

		mCode.append("protected String[] getErrorFiles() { return new String[] {");
		for (var f : mFilesList) {
			mCode.append("\"" + f.getPath().replaceAll("\\\\/", "/").replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"");
			mCode.append(", ");
		}
		mCode.append("};}\n");
	}

	public void addCounter(int id) {
		addCode("mUAI.addOperations(1);");
	}

	public int getCurrentLine() {
		return mLine;
	}

	public void addPosition(IAWord token) {
		var index = getFileIndex(token.getAI());
		mLines.put(mLine, new Line(mLine, token.getLine(), index));
	}
}
