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

	/** Une classe de module (v5) : son source Java et ses lignes, à côté de la classe de l'IA. */
	public record ModuleCode(String className, String javaCode, TreeMap<Integer, LineMapping> lines) {}

	private final ArrayList<ModuleCode> modules = new ArrayList<>();

	public void addModule(String className, String javaCode, TreeMap<Integer, LineMapping> lines) {
		modules.add(new ModuleCode(className, javaCode, lines));
	}

	public ArrayList<ModuleCode> getModules() {
		return modules;
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