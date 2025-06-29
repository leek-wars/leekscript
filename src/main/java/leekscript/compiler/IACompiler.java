package leekscript.compiler;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;

import java.util.Set;

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

	public static final long TIMEOUT_MS = 30 * 1000; // 30 seconds

	private final ArrayNode informations = Json.createArray();
	private AIFile mCurrentAI;
	private long analyzeStart;

	public IACompiler() {}

	public void addError(Location location, Error errorType, String[] parameters) {
		ArrayNode error = Json.createArray();
		error.add(0); // level
		error.add(location.getFile().getId());
		error.addPOJO(location.getStartLine());
		error.addPOJO(location.getStartColumn());
		error.addPOJO(location.getEndLine());
		error.addPOJO(location.getEndColumn());
		error.addPOJO(errorType.ordinal());
		if (parameters != null)
			error.addPOJO(parameters);
		informations.add(error);
	}

	public AnalyzeResult analyze(AIFile ai) throws LeekCompilerException {
		AnalyzeResult result = new AnalyzeResult();
		this.analyzeStart = System.currentTimeMillis(); // For timeout
		try {
			ai.clearErrors();
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

			// System.out.println("Parse time = " + Util.formatDurationNanos(parseTime) + ", analyzeTime = " + Util.formatDurationNanos(analyzeTime));

			// System.out.println("errors " + ai.getPath() + " " + ai.getErrors().size());
			if (ai.getErrors().size() > 0) {
				for (var error : ai.getErrors()) {
					informations.addPOJO(error.toJSON());
				}
				result.success = false;
			} else {
				result.success = true;
			}
		} catch (LeekCompilerException e) {
			if (e.getError() == Error.TOO_MUCH_ERRORS) {
				result.tooMuchErrors = e;
			}
			for (var error : ai.getErrors()) {
				informations.addPOJO(error.toJSON());
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
