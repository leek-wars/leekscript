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

	public IACompiler() {}

	public void addError(int ia_context, int ia, int line, int pos, String word, String informations, String[] parameters) {
		mErrors = true;
		JSONArray error = new JSONArray();
		error.add(0);
		error.add(ia_context);
		error.add(ia);
		error.add(line);
		error.add(pos);
		error.add(word);
		error.add(informations);
		if (parameters != null)
			error.add(parameters);
		mInformations.add(error);
	}

	public void addError(int ia_context, String informations) {
		mErrors = true;
		JSONArray error = new JSONArray();
		error.add(1);
		error.add(ia_context);
		error.add(informations);
		mInformations.add(error);
	}

	public void addInformations(int ia_context, int level) {
		JSONArray error = new JSONArray();
		error.add(2);
		error.add(ia_context);
		error.add(level);
		mInformations.add(error);
	}

	public String compile(int id, String name, String code, String AIClass) throws LeekCompilerException {
		JavaWriter writer = new JavaWriter(false);
		try {
			// On lance la compilation du code de l'IA
			WordParser parser = new WordParser(id, code);
			// Si on est là c'est qu'on a une liste de words correcte, on peut commencer à lire
			MainLeekBlock main = new MainLeekBlock(name);
			WordCompiler compiler = new WordCompiler(parser, main);
			compiler.readCode();

			compiler.writeJava("IA_" + id, writer, AIClass);

			// On sauvegarde les dépendances
			addInformations(id, main.getMinLevel());

		} catch (LeekCompilerException e) {
			addError(id, e.getIA(), e.getLine(), e.getChar(), e.getWord(), e.getError(), e.getParameters());
			throw e;
		} catch (Exception e) {
			ErrorManager.exception(e);
			addError(id, e.getMessage());
		}
		return writer.getJavaCode();
	}

	public String getInformations() {
		return mInformations.toJSONString();
	}

	public boolean hasError() {
		return mErrors;
	}
}
