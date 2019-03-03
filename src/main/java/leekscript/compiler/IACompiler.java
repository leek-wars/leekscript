package leekscript.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import leekscript.ErrorManager;
import leekscript.LeekAI;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

import com.alibaba.fastjson.JSONArray;

/**
 * Classe permettant de compiler une IA
 */
public class IACompiler {

	private final TreeMap<String, LeekAI> mAIs = new TreeMap<String, LeekAI>();
	private final TreeMap<Integer, LeekAI> mAIs_ids = new TreeMap<Integer, LeekAI>();
	private final JSONArray mInformations = new JSONArray();
	protected LeekAI mAI;
	private LeekAI mCurrentAI;
	private boolean mErrors = false;

	public IACompiler() {
		mAI = null;
		mCurrentAI = null;
	}

	public IACompiler(LeekAI ai) {

		mAI = ai;
		// On met l'ia en cache
		mAIs.put(ai.getName(), ai);
		// On compile l'ia principale
		compileAI(ai);

		// On va recompiler les IA incluant cette IA
		List<LeekAI> mIncluders = null;
		if (ai.getId() >= 0) {
			// TODO
			// mIncluders = LeekWars.getDB().getIncludingAI(ai.getId());
		} else
			mIncluders = new ArrayList<LeekAI>();

		// On met en cache
		for (LeekAI includer : mIncluders) {
			if (includer.getId() == mAI.getId()) {
				mAIs.remove(includer.getName());
				continue;
			}
			mAIs.put(includer.getName(), includer);
		}
		// On compile
		for (LeekAI includer : mIncluders) {
			// Logger.log("Compilation de l'ia " + includer.getId() +
			// " incluant l'ia " + ai.getId());
			compileAI(includer);
		}
	}

	public void addError(int ia_context, int ia, int line, int pos, String word, String informations,
			String[] parameters) {
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

	private void compileAI(LeekAI ai) {

		mCurrentAI = ai;

		// if (mAI.getCode().isEmpty())
		// return;
		
//		if (mAI.v2) return;

		try {
			ai.setModified(System.currentTimeMillis());
			// On lance la compilation du code de l'IA
			WordParser parser = new WordParser(ai);
			// Si on est là c'est qu'on a une liste de words correcte, on peut
			// commencer à lire
			MainLeekBlock main = new MainLeekBlock(this, ai);
			WordCompiler compiler = new WordCompiler(parser, main);
			compiler.readCode();

			JavaWriter writer = new JavaWriter(ai.getOwner() > 0);
			compiler.writeJava("IA_" + ai.getClassName(), writer, "AI");

			ai.setCompiled(writer.getJavaCode());
			ai.setMinLevel(main.getMinLevel());
			ai.setInstructions(main.getInstructionsCount());
			// On sauvegarde les dépendances
			addInformations(ai.getId(), main.getMinLevel());
		} catch (LeekCompilerException e) {
			addError(ai.getId(), e.getIA(), e.getLine(), e.getChar(), e.getWord(), e.getError(), e.getParameters());
		} catch (Exception e) {
			ErrorManager.exception(e);
			addError(ai.getId(), e.getMessage());
		}
	}

	public LeekAI getAI(String name) throws IACompilerException {
		if (mAIs.containsKey(name)) {
			return mAIs.get(name);
		}
		// TODO
		assert(false);
//		LeekAI ai = LeekWars.getDB().getAIByName(mCurrentAI.getOwner(), mCurrentAI.getFolder(), name);
//		mAIs.put(name, ai);
//
//		if (ai != null) {
//			mAIs_ids.put(ai.getId(), ai);
//		}
//		return ai;
		return null;
	}

	public LeekAI getAI(Integer id) {
		if (mAIs_ids.containsKey(id))
			return mAIs_ids.get(id);
		assert(false);
//		LeekAI ai = LeekWars.getDB().getAI(id);
//		if (ai == null)
//			return null;
//		mAIs.put(ai.getName(), ai);
//		mAIs_ids.put(id, ai);
//		return ai;
		return null;
	}

	public String getInformations() {
		return mInformations.toJSONString();
	}

	public boolean hasError() {
		return mErrors;
	}

	public LeekAI getMainAI() {
		return mAI;
	}

	public LeekAI getCurrentAI() {
		return mCurrentAI;
	}

	public void setCurrentAI(LeekAI ai) {
		mCurrentAI = ai;
	}

}
