package leekscript.compiler;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.common.Error;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;

/**
 * Classe permettant de compiler une IA
 */
public class IACompiler {

	public static class AnalyzeResult {
		public JSONArray informations;
		public Set<AIFile> includedAIs;
		public boolean success;
		public Throwable tooMuchErrors;
	}

	public static final long TIMEOUT_MS = 30 * 1000; // 30 seconds

	private final JSONArray informations = new JSONArray();
	private AIFile mCurrentAI;
	private long analyzeStart;

	public IACompiler() {}

	public void addError(Location location, Error errorType, String[] parameters) {
		JSONArray error = new JSONArray();
		error.add(0); // level
		error.add(location.getFile().getId());
		error.add(location.getStartLine());
		error.add(location.getStartColumn());
		error.add(location.getEndLine());
		error.add(location.getEndColumn());
		error.add(errorType.ordinal());
		if (parameters != null)
			error.add(parameters);
		informations.add(error);
	}

	public AnalyzeResult analyze(AIFile ai) throws LeekCompilerException {
		AnalyzeResult result = new AnalyzeResult();
		this.analyzeStart = System.currentTimeMillis(); // For timeout
		try {
			ai.clearErrors();
			// On lance la compilation du code de l'IA
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			WordCompiler compiler = new WordCompiler(ai, ai.getVersion());
			MainLeekBlock main = new MainLeekBlock(this, compiler, ai);
			main.setWordCompiler(compiler);
			compiler.readCode();
			compiler.analyze();

			result.includedAIs = main.getIncludedAIs();

			// System.out.println("errors " + ai.getPath() + " " + ai.getErrors().size());
			if (ai.getErrors().size() > 0) {
				for (var error : ai.getErrors()) {
					informations.add(error.toJSON());
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
				informations.add(error.toJSON());
			}
			ai.getErrors().add(new AnalyzeError(e.getLocation(), AnalyzeErrorLevel.ERROR, e.getError()));
			addError(e.getLocation(), e.getError(), e.getParameters());
			result.success = false;
		}
		result.informations = informations;
		return result;
	}

	public AICode compile(AIFile ai, String AIClass, boolean enableOperations) throws LeekCompilerException {
		this.analyzeStart = System.currentTimeMillis(); // For timeout
		JavaWriter writer = new JavaWriter(true, ai.getJavaClass(), enableOperations);
		try {
			ai.clearErrors();
			// On lance la compilation du code de l'IA
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			WordCompiler compiler = new WordCompiler(ai, ai.getVersion());
			MainLeekBlock main = new MainLeekBlock(this, compiler, ai);
			main.setWordCompiler(compiler);
			compiler.readCode();
			compiler.analyze();
			// System.out.println("errors " + compiler.getErrors().size());

			if (ai.getErrors().size() > 0) {
				for (var error : ai.getErrors()) {
					if (error.level == AnalyzeErrorLevel.ERROR) {
						throw new LeekCompilerException(error.location, error.error, error.parameters);
					}
				}
			}
			compiler.writeJava(ai.getJavaClass(), writer, ai.getRootClass());

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
		WordCompiler compiler = new WordCompiler(ai, ai.getVersion());
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
