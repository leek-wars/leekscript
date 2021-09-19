package leekscript.compiler;

public class LineMapping {

	private final int mLeekScriptLine;
	private final int mAI;

	public LineMapping(int leekScriptLine, int ai) {
		mLeekScriptLine = leekScriptLine;
		mAI = ai;
	}

	public int getLeekScriptLine() {
		return mLeekScriptLine;
	}

	public int getAI() {
		return mAI;
	}
}
