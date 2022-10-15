package leekscript.compiler;

import com.alibaba.fastjson.JSONArray;

public class Location {

	private final AIFile file;
	private final int startLine;
	private final int startColumn;
	private final int endLine;
	private final int endColumn;

	public Location(AIFile file) {
		this.file = file;
		this.startLine = 0;
		this.startColumn = 0;
		this.endLine = 0;
		this.endColumn = 0;
	}

	public Location(AIFile file, int startLine, int startColumn, int endLine, int endColumn) {
		this.file = file;
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	public Location(AIFile file, int line, int column) {
		this.file = file;
		this.startLine = line;
		this.startColumn = column;
		this.endLine = line;
		this.endColumn = column;
	}

	public Location(Location start, Location end) {
		assert start.getFile() == end.getFile();
		assert start.getStartLine() <= end.getStartLine();

		this.file = start.file;
		this.startLine = start.startLine;
		this.startColumn = start.startColumn;
		this.endLine = end.endLine;
		this.endColumn = end.endColumn;
	}

	public AIFile getFile() {
		return this.file;
	}
	public int getStartLine() {
		return this.startLine;
	}
	public int getStartColumn() {
		return this.startColumn;
	}
	public int getEndLine() {
		return this.endLine;
	}
	public int getEndColumn() {
		return this.endColumn;
	}

	public Object toJSON() {
		JSONArray a = new JSONArray();
		a.add(this.file.getId());
		a.add(this.startLine);
		a.add(this.startColumn);
		a.add(this.endLine);
		a.add(this.endColumn);
		return a;
	}
}
