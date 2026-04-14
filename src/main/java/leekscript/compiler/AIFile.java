package leekscript.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import tools.jackson.databind.node.ObjectNode;
import leekscript.util.Json;

import leekscript.common.Type;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekFunctionCall;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.runner.AI;

public class AIFile {

	private String path;
	private String code;
	private Folder folder;
	private int owner;
	private int id;
	private volatile long timestamp;
	private int version;
	private ArrayList<AnalyzeError> errors = new ArrayList<>();
	private AICode compiledCode;
	private String clazz;
	private String rootClazz;
	private boolean strict = false;
	public TreeMap<Integer, LineMapping> mLinesMapping = new TreeMap<>();
	private File filesLines;
	private LexicalParserTokenStream tokens = null;
	private Set<AIFile> includedAIs = null;
	private List<ClassDeclarationInstruction> userClasses = null;

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
		ObjectNode json = Json.createObject();
		json.putPOJO("path", path);
		json.putPOJO("timestamp", timestamp);
		json.putPOJO("version", version);
		json.putPOJO("strict", strict);
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

	public void setIncludedAIs(Set<AIFile> includedAIs) {
		this.includedAIs = includedAIs;
	}

	public Set<AIFile> getIncludedAIs() {
		return includedAIs;
	}

	public void setUserClasses(List<ClassDeclarationInstruction> classes) {
		this.userClasses = classes;
	}

	public List<ClassDeclarationInstruction> getUserClasses() {
		return userClasses;
	}

	public List<Location> references(int line, int column, Set<AIFile> filesToSearch) {
		var token = tokens.atLocation(line, column);
		if (token == null || token.getExpression() == null) {
			return List.of();
		}

		var hover = token.getExpression().hover(token);
		Location definedLocation = hover.defined != null ? hover.defined : token.getLocation();

		var definedLocations = new ArrayList<Location>();
		definedLocations.add(definedLocation);
		collectInheritedMethodLocations(token, definedLocation, filesToSearch, definedLocations);

		String word = token.getWord();
		List<Location> results = new ArrayList<>();

		for (AIFile file : filesToSearch) {
			if (file.tokens == null) continue;
			for (Token t : file.tokens.getTokens()) {
				if (!t.getWord().equals(word)) continue;
				if (t.getExpression() == null) continue;
				if (definedLocations.stream().anyMatch(dl -> t.getLocation().sameStart(dl))) continue;
				var h = t.getExpression().hover(t);
				Location target = h.defined != null ? h.defined : t.getLocation();
				if (definedLocations.stream().anyMatch(dl -> target.sameStart(dl))) {
					results.add(t.getLocation());
				}
			}
		}

		return results;
	}

	private void collectInheritedMethodLocations(Token token, Location definedLocation, Set<AIFile> filesToSearch, List<Location> locations) {
		String methodName = token.getWord();

		if (token.getExpression() instanceof LeekVariable v && v.getClassDeclaration() != null) {
			return;
		}

		var allClasses = new ArrayList<ClassDeclarationInstruction>();
		for (AIFile file : filesToSearch) {
			if (file.userClasses != null) {
				allClasses.addAll(file.userClasses);
			}
		}

		ClassDeclarationInstruction defClass = null;
		for (var cls : allClasses) {
			var mv = cls.getMethodVariables().get(methodName);
			if (mv != null && mv.getLocation() != null && mv.getLocation().sameStart(definedLocation)) {
				defClass = cls;
				break;
			}
			var smv = cls.getStaticMethodVariables().get(methodName);
			if (smv != null && smv.getLocation() != null && smv.getLocation().sameStart(definedLocation)) {
				defClass = cls;
				break;
			}
		}
		if (defClass == null) return;

		var parent = defClass.getParent();
		while (parent != null) {
			var member = parent.getMember(methodName);
			if (member != null) {
				locations.add(member.getLocation());
			}
			parent = parent.getParent();
		}

		var hierarchy = new HashSet<ClassDeclarationInstruction>();
		hierarchy.add(defClass);
		boolean changed = true;
		while (changed) {
			changed = false;
			for (var cls : allClasses) {
				if (cls.getParent() == null) continue;
				if (!hierarchy.contains(cls.getParent())) continue;
				if (hierarchy.contains(cls)) continue;
				hierarchy.add(cls);
				var member = cls.getMember(methodName);
				if (member != null) {
					locations.add(member.getLocation());
				}
				changed = true;
			}
		}
	}

	public List<Location> constructorReferences(int line, int column, int paramCount, Set<AIFile> filesToSearch) {
		var token = tokens.atLocation(line, column);
		if (token == null) return List.of();

		Location classLocation = null;
		if (token.getExpression() instanceof LeekVariable v && v.getClassDeclaration() != null) {
			classLocation = v.getClassDeclaration().getLocation();
		} else if (token.getExpression() != null) {
			classLocation = token.getLocation();
		}
		if (classLocation == null) return List.of();

		List<Location> results = new ArrayList<>();
		var seen = new HashSet<LeekFunctionCall>();
		for (AIFile file : filesToSearch) {
			if (file.tokens == null) continue;
			for (Token t : file.tokens.getTokens()) {
				if (!(t.getExpression() instanceof LeekFunctionCall call)) continue;
				if (!call.isConstructorCall()) continue;
				if (call.getParameterCount() != paramCount) continue;
				if (seen.contains(call)) continue;
				var calledExpr = call.getCallExpression();
				if (calledExpr instanceof LeekVariable v && v.getClassDeclaration() != null) {
					if (v.getClassDeclaration().getLocation() != null && v.getClassDeclaration().getLocation().sameStart(classLocation)) {
						seen.add(call);
						results.add(v.getLocation());
					}
				}
			}
		}
		return results;
	}

	public Options getOptions() {
		return new Options(version, strict, true, true, null, true);
	}
}
