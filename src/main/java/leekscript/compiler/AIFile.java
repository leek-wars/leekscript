package leekscript.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;

import leekscript.common.Type;
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
	private boolean strict = false;
	public TreeMap<Integer, LineMapping> mLinesMapping = new TreeMap<>();
	private File filesLines;
	private LexicalParserTokenStream tokens = null;

	public AIFile(String path, String code, long timestamp, int version, int owner, boolean strict) {
		this(path, code, timestamp, version, null, owner, path.hashCode() & 0xfffffff, strict);
	}

	public AIFile(String path, String code, long timestamp, int version, Folder folder, int owner, int id, boolean strict) {
		this.path = path;
		this.code = code;
		this.folder = folder;
		this.owner = owner;
		this.timestamp = timestamp;
		this.version = version;
		this.id = id;
		this.strict = strict;
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
		this.tokens = null;
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
	public void setVersion(int version, boolean strict) {
		this.version = version;
		this.strict = strict;
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
		json.put("strict", strict);
		// context.toJson(json);
		return json.toString();
	}

	public AI compile(Options options) throws LeekScriptException, LeekCompilerException {

		// System.out.println("LeekScript compile AI " + this.getPath() + " timestamp : " + this.getTimestamp() + " options " + options);

		LeekScript.getFileSystem().loadDependencies(this);

		AI ai = JavaCompiler.compile(this, options);

		return ai;
	}

	public void clearErrors() {
		this.errors.clear();
	}

	public LexicalParserTokenStream getTokenStream() {
		return tokens;
	}

	public void setTokenStream(LexicalParserTokenStream tokens) {
		this.tokens = tokens;
	}

	public boolean hasBeenParsed() {
		return tokens != null;
	}

	public Hover hover(int line, int column) {
		var token = tokens.atLocation(line, column);
		if (token == null) {
			return null;
		}

		if (token.getExpression() != null) {
			return token.getExpression().hover(token);
		}

		return new Hover(token.getLocation(), token.getWord());
	}

	public Complete complete(int line, int column) {

		// Find token
		var token = tokens.atLocation(line, column);
		if (token == null) return null;

		if (token.getExpression() != null) {
			return token.getExpression().complete(token);
		}

		return new Complete(Type.VOID);
	}

	@Override
	public String toString() {
		return getName();
	}

	public boolean isStrict() {
		return strict;
	}

	public Options getOptions() {
		return new Options(version, strict, true, true, null, true);
	}
}
