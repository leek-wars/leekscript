package leekscript.compiler;

import java.util.ArrayList;
import java.util.TreeMap;

public class AICode {
	String javaCode;
	String linesFile;
	TreeMap<Integer, LineMapping> lines;
	ArrayList<AIFile> files;

	public AICode(String javaCode, String linesFile, TreeMap<Integer, LineMapping> lines, ArrayList<AIFile> files) {
		this.javaCode = javaCode;
		this.linesFile = linesFile;
		this.lines = lines;
		this.files = files;
	}

	public String getJavaCode() {
		return javaCode;
	}

	public String getLines() {
		return linesFile;
	}

	public TreeMap<Integer, LineMapping> getLinesMap() {
		return lines;
	}

	public ArrayList<AIFile> getFiles() {
		return files;
	}
}