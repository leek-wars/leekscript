package leekscript.compiler;

import leekscript.compiler.resolver.ResolverContext;

public class AIFile<C extends ResolverContext> {

	private String path;
	private String code;
	private C context;
	private int id;
	private long timestamp;

	public AIFile(String path, String code, long timestamp, C context) {
		this(path, code, timestamp, context, (context + "/" + path).hashCode() & 0xfffffff);
	}
	public AIFile(String path, String code, long timestamp, C context, int id) {
		this.path = path;
		this.code = code;
		this.context = context;
		this.timestamp = timestamp;
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
}
