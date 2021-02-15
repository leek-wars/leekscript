package leekscript.compiler;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class JavaWriter {
	private final StringBuilder mCode;
	private int mLine;
	private final ArrayList<Line> mLines;
	private final boolean mWithDebug;

	private class Line {
		private final int mJavaLine;
		private final int mCodeLine;
		private final AIFile<?> mAI;

		public Line(int java_line, int code_line, AIFile<?> ai) {
			mJavaLine = java_line;
			mCodeLine = code_line;
			mAI = ai;
		}
	}

	public JavaWriter(boolean debug) {
		mCode = new StringBuilder();
		mLines = new ArrayList<Line>();
		mLine = 1;
		mWithDebug = debug;
	}

	public boolean hasDebug() {
		return mWithDebug;
	}

	public void addLine(String datas, int line, AIFile<?> ai) {
		mCode.append(datas).append("\n");
		mLines.add(new Line(mLine, line, ai));
		mLine++;
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
		mCode.append("protected String[] getErrorString(){ return new String[]{");

		String aiJson = JSON.toJSONString(ai);

		boolean first = true;
		for (Line l : mLines) {
			if (!first)
				mCode.append(",");
			else
				first = false;
			JSONArray array = new JSONArray();
			array.add(l.mJavaLine);
			array.add(l.mAI.getPath());
			array.add(l.mCodeLine);
			mCode.append(JSON.toJSONString(array.toJSONString()));
		}
		mCode.append("};}\nprotected String getAItring(){ return ");
		mCode.append(aiJson);
		mCode.append(";}\n");
	}

	public void addCounter(int id) {
		addCode("mUAI.addOperations(1);");
	}
}
