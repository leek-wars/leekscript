package leekscript.compiler;

import com.alibaba.fastjson.JSONObject;

import leekscript.compiler.resolver.ResolverContext;

public class AIFile<C extends ResolverContext> {

	private String path;
	private String code;
	private C context;
	private int id;
	private long timestamp;
	private int version;

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

	public String toJson() {
		JSONObject json = new JSONObject();
		json.put("path", path);
		json.put("timestamp", timestamp);
		json.put("version", version);
		context.toJson(json);
		return json.toJSONString();
	}
}
