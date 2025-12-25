package leekscript.compiler;

import tools.jackson.databind.node.ArrayNode;
import leekscript.util.Json;
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


	public ArrayNode toJSON() {
		ArrayNode array = Json.createArray();
		array.addPOJO(level.ordinal());
		array.add(location.getFile().getId());
		array.addPOJO(location.getStartLine());
		array.addPOJO(location.getStartColumn());
		array.addPOJO(location.getEndLine());
		array.addPOJO(location.getEndColumn());
		// array.addPOJO(token.getWord());
		array.add(this.error.ordinal());
		if (parameters != null) {
			array.addPOJO(parameters);
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
