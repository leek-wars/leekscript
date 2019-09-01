package leekscript.compiler;

import leekscript.compiler.resolver.ResolverContext;

public class AIFile<C extends ResolverContext> {
	
	private String path;
	private String code;
	private C context;
	private String javaClassName;
	private int id;

	public AIFile(String path, String code, C context) {
		this.path = path;
		this.code = code;
		this.context = context;
	}
	public AIFile(String path, String code, C context, int id) {
		this.path = path;
		this.code = code;
		this.context = context;
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
	public String getJavaClassName() {
		return javaClassName;
	}
	public void setJavaClassName(String javaClassName) {
		this.javaClassName = javaClassName;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
}
