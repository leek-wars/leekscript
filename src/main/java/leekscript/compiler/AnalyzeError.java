package leekscript.compiler;

import com.alibaba.fastjson.JSONArray;
import leekscript.common.Error;

public class AnalyzeError implements Comparable<AnalyzeError> {

	public static enum AnalyzeErrorLevel {
		ERROR, // 0
		WARNING, // 1
	}

	// public String file;
	// public int startLine;
	// public int startCharacter;
	// public int endLine;
	// public int endCharacter;
	public IAWord token;
	public Error error;
	public AnalyzeErrorLevel level;
	public String[] parameters;

	public AnalyzeError(IAWord token, AnalyzeErrorLevel level, Error error) {
		this(token, level, error, null);
	}
	public AnalyzeError(IAWord token, AnalyzeErrorLevel level, Error error, String[] parameters) {
		this.token = token;
		this.error = error;
		this.level = level;
		this.parameters = parameters;
	}

	public JSONArray toJSON() {
		JSONArray array = new JSONArray();
		array.add(level.ordinal());
		array.add(token.getAI().getId());
		array.add(token.getLine());
		array.add(token.getCharacter());
		array.add(token.getWord());
		array.add(this.error.ordinal());
		if (parameters != null) {
			array.add(parameters);
		}
		return array;
	}

	@Override
	public int compareTo(AnalyzeError o) {
		if (token.getLine() != o.token.getLine()) {
			return token.getLine() - o.token.getLine();
		}
		if (token.getCharacter() != o.token.getCharacter()) {
			return token.getCharacter() - o.token.getCharacter();
		}
		return 0;
	}

	// public AnalyzeError(String file, int startLine, int startCharacter, int endLine, int endCharacter) {
	// 	this.file = file;
	// 	this.startLine = startLine;
	// 	this.startCharacter = startCharacter;
	// 	this.endLine = endLine;
	// 	this.endCharacter = endCharacter;
	// }
}
