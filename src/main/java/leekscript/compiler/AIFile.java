package leekscript.compiler;

import java.util.ArrayList;

import com.alibaba.fastjson.JSONObject;

import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;

public class AIFile {

	private String path;
	private String code;
	private Folder folder;
	private int owner;
	private int id;
	private long timestamp;
	private int version;
	private ArrayList<AnalyzeError> errors = new ArrayList<>();
	private AICode compiledCode;
	private String clazz;
	private String rootClazz;
	private ArrayList<Token> tokens = new ArrayList<Token>();
	private Token endOfFileToken = new Token(WordParser.T_END_OF_FILE, "", new Location(this));

	public AIFile(String path, String code, long timestamp, int version, int owner) {
		this(path, code, timestamp, version, null, owner, path.hashCode() & 0xfffffff);
	}

	public AIFile(String path, String code, long timestamp, int version, Folder folder, int owner, int id) {
		this.path = path;
		this.code = code;
		this.folder = folder;
		this.owner = owner;
		this.timestamp = timestamp;
		this.version = version;
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public Folder getFolder() {
		return folder;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public long getTimestamp() {
		return this.timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public int getVersion() {
		return this.version;
	}
	public void setVersion(int version) {
		this.version = version;
	}

	public ArrayList<AnalyzeError> getErrors() {
		return errors;
	}

	public void setCompiledCode(AICode compiledCode) {
		this.compiledCode = compiledCode;
	}
	public AICode getCompiledCode() {
		return compiledCode;
	}

	public void setJavaClass(String clazz) {
		this.clazz = clazz;
	}
	public String getJavaClass() {
		return this.clazz;
	}

	public void setRootClass(String clazz) {
		this.rootClazz = clazz;
	}
	public String getRootClass() {
		return this.rootClazz;
	}

	public int getOwner() {
		return this.owner;
	}

	public String getName() {
		return path;
	}

	public String toJson() {
		JSONObject json = new JSONObject();
		json.put("path", path);
		json.put("timestamp", timestamp);
		json.put("version", version);
		// context.toJson(json);
		return json.toString();
	}

	public AI compile(boolean use_cache) throws LeekScriptException, LeekCompilerException {

		// System.out.println("LeekScript compile AI " + this.getPath() + " timestamp : " + this.getTimestamp());

		LeekScript.getFileSystem().loadDependencies(this);

		AI ai = JavaCompiler.compile(this, use_cache);

		return ai;
	}

	public void clearErrors() {
		this.errors.clear();
	}

	public ArrayList<Token> getTokens() {
		return tokens;
	}

	public Token getTokenAt(int index) {
		return index < 0 || index < tokens.size() ? tokens.get(index) : endOfFileToken;
	}

	public Token getLastToken() {
		return tokens.size() > 0 ? tokens.get(tokens.size() - 1) : endOfFileToken;
	}

	public Hover hover(int line, int column) {

		// Find token
		var token = findToken(line, column);
		if (token == null) return null;

		if (token.getExpression() != null) {
			return token.getExpression().hover(token);
		}

		return new Hover(token.getLocation(), token.getWord());
	}

	public Token findToken(int line, int column) {
		if (tokens.size() == 0) return null;
		// Find token
		int start = 0;
		int end = tokens.size() - 1;
		while (true) {
			int p = (end + start) / 2;
			var token = tokens.get(p);
			var tLine = token.getLocation().getStartLine();
			// System.out.println("findToken start=" + start + " end=" + end + " token=" + token);
			if (start >= end) {
				if (line == tLine) {
					var startColumn = token.getLocation().getStartColumn();
					var endColumn = token.getLocation().getEndColumn();
					if (column >= startColumn && column <= endColumn) {
						return token;
					}
				}
				return null;
			}
			if (line > tLine) {
				start = p + 1;
			} else if (line < tLine) {
				end = p - 1;
			} else { // Même ligne
				var startColumn = token.getLocation().getStartColumn();
				var endColumn = token.getLocation().getEndColumn();
				if (column >= startColumn && column <= endColumn) {
					return token;
				}
				if (column > endColumn) {
					start = p + 1;
				} else {
					end = p - 1;
				}
			}
		}
	}

	@Override
	public String toString() {
		return getName();
	}

}
