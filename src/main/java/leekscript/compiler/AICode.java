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
	/** `group` : le package du module, {@link PackageCache#PROGRAM_GROUP} hors de tout package. */
	public record ModuleCode(String className, String javaCode, TreeMap<Integer, LineMapping> lines, String group) {}

	private final ArrayList<ModuleCode> modules = new ArrayList<>();

	public void addModule(String className, String javaCode, TreeMap<Integer, LineMapping> lines, String group) {
		modules.add(new ModuleCode(className, javaCode, lines, group));
	}

	/** Vrai si un module du programme est dans un package : la compilation Java se fait par package. */
	public boolean hasPackages() {
		return modules.stream().anyMatch(m -> !m.group().equals(PackageCache.PROGRAM_GROUP));
	}

	// Les classes de premier niveau passées à javac à la dernière compilation (les autres
	// viennent du cache des packages). Point d'observation des tests : rien d'autre ne le lit.
	private java.util.List<String> javacUnits = java.util.List.of();

	public void setJavacUnits(java.util.List<String> javacUnits) {
		this.javacUnits = javacUnits;
	}

	public java.util.List<String> getJavacUnits() {
		return javacUnits;
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