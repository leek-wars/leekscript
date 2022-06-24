package leekscript.compiler;


import java.util.ArrayList;

import com.alibaba.fastjson.JSONObject;

import leekscript.common.Type;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.ResolverContext;
import leekscript.runner.AI;

public class AIFile<C extends ResolverContext> {

	private String path;
	private String code;
	private C context;
	private int id;
	private long timestamp;
	private int version;
	private ArrayList<AnalyzeError> errors = new ArrayList<>();
	private AICode compiledCode;
	private String clazz;
	private String rootClazz;
	private ArrayList<Token> tokens = new ArrayList<Token>();

	public AIFile(String path, String code, long timestamp, int version, C context) {
		this(path, code, timestamp, version, context, (context + "/" + path).hashCode() & 0xfffffff);
	}

	public AIFile(String path, String code, long timestamp, int version, C context, int id) {
		this.path = path;
		this.code = code;
		this.context = context;
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
	public C getContext() {
		return context;
	}
	public void setContext(C context) {
		this.context = context;
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

	public String toJson() {
		JSONObject json = new JSONObject();
		json.put("path", path);
		json.put("timestamp", timestamp);
		json.put("version", version);
		context.toJson(json);
		return json.toString();
	}

	public AI compile(boolean use_cache) throws LeekScriptException, LeekCompilerException {

		// System.out.println("LeekScript compile AI " + file.getPath() + " timestamp : " + file.getTimestamp());

		AI ai = JavaCompiler.compile(this, use_cache);

		return ai;
	}

	public void clearErrors() {
		this.errors.clear();
	}

	public ArrayList<Token> getTokens() {
		return tokens;
	}

	public Hover hover(int line, int column) {

		// Find token
		var token = findToken(line, column);

		if (token.getExpression() != null) {
			return token.getExpression().hover(token);
		}

		return new Hover(token.getLocation(), token.getWord());
	}

	public Token findToken(int line, int column) {
		// Find token
		int start = 0;
		int end = tokens.size() - 1;
		while (true) {
			int p = (end + start) / 2;
			var token = tokens.get(p);
			// System.out.println("findToken start=" + start + " end=" + end + " token=" + token);
			if (start >= end) return token;
			var tLine = token.getLocation().getStartLine();
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
}
