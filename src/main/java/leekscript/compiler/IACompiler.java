package leekscript.compiler;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.common.Error;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;

/**
 * Classe permettant de compiler une IA
 */
public class IACompiler {

	public static class AnalyzeResult {
		public JSONArray informations;
		public List<Integer> includedAIs = new ArrayList<>();
		public boolean success;
	}

	private final JSONArray mInformations = new JSONArray();
	private AIFile<?> mCurrentAI;

	public IACompiler() {}

	public void addError(AIFile<?> ia_context, int line, int pos, String word, Error errorType, String[] parameters) {
		JSONArray error = new JSONArray();
		error.add(0); // level
		error.add(ia_context.getId());
		error.add(line);
		error.add(pos);
		error.add(word);
		error.add(errorType.ordinal());
		if (parameters != null)
			error.add(parameters);
		mInformations.add(error);
	}

	public AnalyzeResult analyze(AIFile<?> ai) throws LeekCompilerException {
		AnalyzeResult result = new AnalyzeResult();
		try {
			// On lance la compilation du code de l'IA
			WordParser parser = new WordParser(ai, ai.getVersion());
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			MainLeekBlock main = new MainLeekBlock(this, ai);
			WordCompiler compiler = new WordCompiler(parser, main, ai, ai.getVersion());
			main.setWordCompiler(compiler);
			compiler.readCode();
			compiler.analyze();

			// System.out.println("errors " + compiler.getErrors().size());
			if (compiler.getErrors().size() > 0) {
				for (var error : compiler.getErrors()) {
					mInformations.add(error.toJSON());
				}
				result.success = false;
			} else {
				result.includedAIs = main.getIncludedAIs();
				result.success = true;
			}
		} catch (LeekCompilerException e) {
			addError(e.getIA(), e.getLine(), e.getChar(), e.getWord(), e.getError(), e.getParameters());
			result.success = false;
		}
		result.informations = mInformations;
		return result;
	}

	public AICode compile(AIFile<?> ai, String javaClassName, String AIClass) throws LeekCompilerException {
		JavaWriter writer = new JavaWriter(true, javaClassName);
		try {
			// On lance la compilation du code de l'IA
			WordParser parser = new WordParser(ai, ai.getVersion());
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			MainLeekBlock main = new MainLeekBlock(this, ai);
			WordCompiler compiler = new WordCompiler(parser, main, ai, ai.getVersion());
			main.setWordCompiler(compiler);
			compiler.readCode();
			compiler.analyze();
			// System.out.println("errors " + compiler.getErrors().size());

			if (compiler.getErrors().size() > 0) {
				for (var error : compiler.getErrors()) {
					if (error.level == AnalyzeErrorLevel.ERROR) {
						throw new LeekCompilerException(error.token, error.error, error.parameters);
					}
				}
			}
			compiler.writeJava(javaClassName, writer, AIClass);

		} catch (LeekCompilerException e) {
			addError(e.getIA(), e.getLine(), e.getChar(), e.getWord(), e.getError(), e.getParameters());
			throw e;
		}
		return writer.getCode();
	}

	public String merge(AIFile<?> ai) throws LeekCompilerException {
		// System.out.println("Merge ai " + ai);
		WordParser parser = new WordParser(ai, ai.getVersion());
		MainLeekBlock main = new MainLeekBlock(this, ai);
		WordCompiler compiler = new WordCompiler(parser, main, ai, ai.getVersion());
		main.setWordCompiler(compiler);
		compiler.readCode();
		String code = main.getCode();
		// System.out.println("Code = " + code);
		return code;
	}

	public String getInformations() {
		return mInformations.toJSONString();
	}

	public AIFile<?> getCurrentAI() {
		return mCurrentAI;
	}

	public void setCurrentAI(AIFile<?> ai) {
		mCurrentAI = ai;
	}
}
