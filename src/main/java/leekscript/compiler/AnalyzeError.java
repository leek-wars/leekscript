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
	public Location location;
	public Error error;
	public AnalyzeErrorLevel level;
	public String[] parameters;

	public AnalyzeError(Token token, AnalyzeErrorLevel level, Error error) {
		this(token.getLocation(), level, error, null);
	}

	public AnalyzeError(Token token, AnalyzeErrorLevel level, Error error, String[] parameters) {
		this(token.getLocation(), level, error, parameters);
	}

	public AnalyzeError(Location location, AnalyzeErrorLevel level, Error error) {
		this(location, level, error, null);
	}

	public AnalyzeError(Location location, AnalyzeErrorLevel level, Error error, String[] parameters) {
		this.location = location;
		this.error = error;
		this.level = level;
		this.parameters = parameters;
	}


	public JSONArray toJSON() {
		JSONArray array = new JSONArray();
		array.add(level.ordinal());
		array.add(location.getFile().getId());
		array.add(location.getStartLine());
		array.add(location.getStartColumn());
		array.add(location.getEndLine());
		array.add(location.getEndColumn());
		// array.add(token.getWord());
		array.add(this.error.ordinal());
		if (parameters != null) {
			array.add(parameters);
		}
		return array;
	}

	@Override
	public int compareTo(AnalyzeError o) {
		if (location.getStartLine() != o.location.getStartLine()) {
			return location.getStartLine() - o.location.getStartLine();
		}
		if (location.getStartColumn() != o.location.getStartColumn()) {
			return location.getStartColumn() - o.location.getStartColumn();
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
