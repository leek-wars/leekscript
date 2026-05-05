package leekscript.compiler;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.Util;
import leekscript.common.Error;
import leekscript.common.Type;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import leekscript.util.Json;

/**
 * Classe permettant de compiler une IA
 */
public class IACompiler {

	public static class AnalyzeResult {
		public ArrayNode informations;
		public Set<AIFile> includedAIs;
		public boolean success;
		public Throwable tooMuchErrors;
	}

	public static class MultiAnalyzeResult {
		public final AnalyzeResult merged;
		public final Map<AIFile, AnalyzeResult> perEntrypoint;
		public MultiAnalyzeResult(AnalyzeResult merged, Map<AIFile, AnalyzeResult> perEntrypoint) {
			this.merged = merged;
			this.perEntrypoint = perEntrypoint;
		}
	}

	public static final long TIMEOUT_MS = 30 * 1000; // 30 seconds

	private final ArrayNode informations = Json.createArray();
	private AIFile mCurrentAI;
	private long analyzeStart;

	public IACompiler() {}

	public void addError(Location location, Error errorType, String[] parameters) {
		ArrayNode error = Json.createArray();
		error.add(0); // level
		error.add(location.getFile().getPath());
		error.add(location.getStartLine());
		error.add(location.getStartColumn());
		error.add(location.getEndLine());
		error.add(location.getEndColumn());
		error.add(errorType.ordinal());
		if (parameters != null)
			error.addPOJO(parameters);
		informations.add(error);
	}

	/**
	 * Analyse un fichier en tenant compte de ses entrypoints transitifs.
	 * Si le fichier n'a pas d'includers, il est analysé directement.
	 * Sinon, chaque entrypoint est compilé et les résultats sont fusionnés :
	 * - UNUSED_* : intersection (warning seulement si inutilisé dans TOUS les entrypoints)
	 * - Autres erreurs : union dédupliquée par (fichier, ligne, colonne, code)
	 */
	public static MultiAnalyzeResult analyzeWithIncludes(AIFile ai) {
		var fs = LeekScript.getFileSystem();
		var includers = fs.getIncluders(ai);
		if (includers.isEmpty()) {
			fs.loadDependencies(ai);
			var result = new IACompiler().analyze(ai);
			return new MultiAnalyzeResult(result, Map.of(ai, result));
		}
		var perEntrypoint = new LinkedHashMap<AIFile, AnalyzeResult>();
		for (var ep : includers) {
			fs.loadDependencies(ep);
			perEntrypoint.put(ep, new IACompiler().analyze(ep));
		}
		return new MultiAnalyzeResult(mergeResults(perEntrypoint.values()), perEntrypoint);
	}

	/**
	 * Fusionne les résultats de plusieurs compilations d'entrypoints.
	 * UNUSED_VARIABLE / UNUSED_FUNCTION : intersection (seulement si inutilisé dans TOUS).
	 * Autres erreurs : union dédupliquée par (fichier, ligne, colonne, code).
	 */
	public static AnalyzeResult mergeResults(Collection<AnalyzeResult> results) {
		int unusedVariableOrdinal = Error.UNUSED_VARIABLE.ordinal();
		int unusedFunctionOrdinal = Error.UNUSED_FUNCTION.ordinal();
		int count = 0;
		for (var r : results) if (r != null && r.informations != null) count++;
		var unusedFirst = new LinkedHashMap<String, JsonNode>();
		var unusedCounts = new HashMap<String, int[]>();
		var otherProblems = new LinkedHashMap<String, JsonNode>();
		for (var result : results) {
			if (result == null || result.informations == null) continue;
			for (JsonNode problem : result.informations) {
				int ordinal = problem.get(6).intValue();
				String key = problem.get(1).stringValue() + ":"
						+ problem.get(2).intValue() + ":"
						+ problem.get(3).intValue() + ":"
						+ ordinal;
				if (ordinal == unusedVariableOrdinal || ordinal == unusedFunctionOrdinal) {
					unusedFirst.putIfAbsent(key, problem);
					unusedCounts.computeIfAbsent(key, k -> new int[1])[0]++;
				} else {
					otherProblems.putIfAbsent(key, problem);
				}
			}
		}
		var informations = Json.createArray();
		for (var e : unusedFirst.entrySet()) {
			if (unusedCounts.get(e.getKey())[0] == count) informations.add(e.getValue());
		}
		for (var problem : otherProblems.values()) informations.add(problem);
		var merged = new AnalyzeResult();
		merged.informations = informations;
		merged.success = isValid(merged);
		return merged;
	}

	public static boolean isValid(AnalyzeResult result) {
		if (result == null || result.informations == null) return true;
		for (JsonNode problem : result.informations) {
			if (problem.get(0).intValue() == 0) return false;
		}
		return true;
	}

	public AnalyzeResult analyze(AIFile ai) {
		AnalyzeResult result = new AnalyzeResult();
		this.analyzeStart = System.currentTimeMillis(); // For timeout
		try {
			ai.clearErrors();
			// Reset version/strict avant PragmaParser pour éviter qu'un pragma précédent
			// ne subsiste si le fichier a changé (ex: suppression de @version:2).
			ai.setVersion(LeekScript.LATEST_VERSION, false);
			PragmaParser.apply(ai);
			// On lance la compilation du code de l'IA
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			WordCompiler compiler = new WordCompiler(ai, ai.getVersion(), ai.getOptions());
			MainLeekBlock main = new MainLeekBlock(this, compiler, ai);
			main.setWordCompiler(compiler);

			long parseTime = System.nanoTime();
			compiler.readCode();
			parseTime = System.nanoTime() - parseTime;

			long analyzeTime = System.nanoTime();
			compiler.analyze();
			analyzeTime = System.nanoTime() - analyzeTime;

			result.includedAIs = main.getIncludedAIs();
			ai.setIncludedAIs(result.includedAIs);
			ai.setUserClasses(main.getUserClasses());

			// System.out.println("Parse time = " + Util.formatDurationNanos(parseTime) + ", analyzeTime = " + Util.formatDurationNanos(analyzeTime));

			for (var error : ai.getErrors()) {
				informations.add(error.toJSON());
			}
			result.success = ai.getErrors().stream().noneMatch(e -> e.level == AnalyzeErrorLevel.ERROR);
		} catch (LeekCompilerException e) {
			if (e.getError() == Error.TOO_MUCH_ERRORS) {
				result.tooMuchErrors = e;
			}
			for (var error : ai.getErrors()) {
				informations.add(error.toJSON());
			}
			ai.getErrors().add(new AnalyzeError(e.getLocation(), AnalyzeErrorLevel.ERROR, e.getError()));
			addError(e.getLocation(), e.getError(), e.getParameters());
			result.success = false;
		}
		result.informations = informations;
		return result;
	}

	public AICode compile(AIFile ai, String AIClass, Options options) throws LeekCompilerException {
		this.analyzeStart = System.currentTimeMillis(); // For timeout
		JavaWriter writer = new JavaWriter(true, ai.getJavaClass(), options.enableOperations());
		writer.options = options;
		try {
			ai.clearErrors();
			PragmaParser.apply(ai);

			// On lance la compilation du code de l'IA
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			WordCompiler compiler = new WordCompiler(ai, ai.getVersion(), options);
			MainLeekBlock main = new MainLeekBlock(this, compiler, ai);
			main.setWordCompiler(compiler);

			// Ajout des variables de la session
			if (options.session() != null) {
				for (var name : options.session().getVariables().keySet()) {
					var variable = new LeekVariable(new Token(name), VariableType.LOCAL, true);
					main.addVariable(variable);
				}
			}

			compiler.readCode();
			compiler.analyze();
			// System.out.println("errors " + ai.getErrors().size());

			if (ai.getErrors().size() > 0) {
				for (var error : ai.getErrors()) {
					if (error.level == AnalyzeErrorLevel.ERROR) {
						throw new LeekCompilerException(error.location, error.error, error.parameters);
					}
				}
			}
			compiler.writeJava(ai.getJavaClass(), writer, ai.getRootClass(), options);

		} catch (LeekCompilerException e) {
			ai.getErrors().add(new AnalyzeError(e.getLocation(), AnalyzeErrorLevel.ERROR, e.getError()));
			addError(e.getLocation(), e.getError(), e.getParameters());
			throw e;
		}
		return writer.getCode();
	}

	public String merge(AIFile ai) throws LeekCompilerException {
		// System.out.println("Merge ai " + ai);
		this.analyzeStart = System.currentTimeMillis(); // For timeout
		WordCompiler compiler = new WordCompiler(ai, ai.getVersion(), ai.getOptions());
		MainLeekBlock main = new MainLeekBlock(this, compiler, ai);
		main.setWordCompiler(compiler);
		compiler.readCode();
		String code = main.getCode();
		// System.out.println("Code = " + code);
		return code;
	}

	public AIFile getCurrentAI() {
		return mCurrentAI;
	}

	public void setCurrentAI(AIFile ai) {
		mCurrentAI = ai;
	}

	public long getAnalyzeStart() {
		return analyzeStart;
	}
}
