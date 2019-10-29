package leekscript.compiler;

import leekscript.ErrorManager;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

import com.alibaba.fastjson.JSONArray;

/**
 * Classe permettant de compiler une IA
 */
public class IACompiler {

	private final JSONArray mInformations = new JSONArray();
	private boolean mErrors = false;
	private AIFile<?> mCurrentAI;

	public IACompiler() {}

	public void addError(AIFile<?> ia_context, int line, int pos, String word, String informations, String[] parameters) {
		mErrors = true;
		JSONArray error = new JSONArray();
		error.add(0);
		error.add(ia_context.getId());
		error.add(line);
		error.add(pos);
		error.add(word);
		error.add(informations);
		if (parameters != null)
			error.add(parameters);
		mInformations.add(error);
	}

	public void addError(AIFile<?> ia_context, String informations) {
		mErrors = true;
		JSONArray error = new JSONArray();
		error.add(1);
		error.add(ia_context.getId());
		error.add(informations);
		mInformations.add(error);
	}

	public void addInformations(AIFile<?> ia_context, int level) {
		JSONArray error = new JSONArray();
		error.add(2);
		error.add(ia_context.getId());
		error.add(level);
		mInformations.add(error);
	}

	public String analyze(AIFile<?> ai) throws LeekCompilerException {
		try {
			// On lance la compilation du code de l'IA
			WordParser parser = new WordParser(ai);
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			MainLeekBlock main = new MainLeekBlock(this, ai);
			WordCompiler compiler = new WordCompiler(parser, main, ai);
			compiler.readCode();
			// On sauvegarde les dépendances
			addInformations(ai, main.getMinLevel());
		} catch (LeekCompilerException e) {
			addError(e.getIA(), e.getLine(), e.getChar(), e.getWord(), e.getError(), e.getParameters());
		} catch (Exception e) {
			addError(ai, e.getMessage());
		}
		return mInformations.toJSONString();
	}

	public String compile(AIFile<?> ai, String javaClassName, String AIClass) throws LeekCompilerException {
		JavaWriter writer = new JavaWriter(true);
		try {
			// On lance la compilation du code de l'IA
			WordParser parser = new WordParser(ai);
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			MainLeekBlock main = new MainLeekBlock(this, ai);
			WordCompiler compiler = new WordCompiler(parser, main, ai);
			compiler.readCode();

			compiler.writeJava(javaClassName, writer, AIClass);
			// On sauvegarde les dépendances
			addInformations(ai, main.getMinLevel());

		} catch (LeekCompilerException e) {
			addError(e.getIA(), e.getLine(), e.getChar(), e.getWord(), e.getError(), e.getParameters());
			throw e;
		} catch (Exception e) {
			ErrorManager.exception(e);
			addError(ai, e.getMessage());
		}
		return writer.getJavaCode();
	}

	public String getInformations() {
		return mInformations.toJSONString();
	}

	public boolean hasError() {
		return mErrors;
	}

	public AIFile<?> getCurrentAI() {
		return mCurrentAI;
	}

	public void setCurrentAI(AIFile<?> ai) {
		mCurrentAI = ai;
	}
}
