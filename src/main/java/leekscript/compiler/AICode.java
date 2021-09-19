package leekscript.compiler;

public class AICode {
	String javaCode;
	String linesFile;

	public AICode(String javaCode, String linesFile) {
		this.javaCode = javaCode;
		this.linesFile = linesFile;
	}

	public String getJavaCode() {
		return javaCode;
	}

	public String getLines() {
		return linesFile;
	}
}