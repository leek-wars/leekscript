package leekscript.compiler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import leekscript.ErrorManager;
import leekscript.LeekAI;
import leekscript.runner.AI;

public class LeekScript {
	// Classe principale du LeekScript
	private final static String IA_PATH = "ai/";

	private final static Map<Integer, Long> sIA_modified = new TreeMap<Integer, Long>();
	private final static Map<Integer, Long> sIA_tournament_modified = new TreeMap<Integer, Long>();

	public static void init() {
		try {
			File f = new File("data/modified.sv");
			if (f.exists()) {
				DataInputStream data_input = new DataInputStream(new FileInputStream(f));
				int len = data_input.readInt();
				for (int i = 0; i < len; i++) {
					int id = data_input.readInt();
					long modified = data_input.readLong();
					sIA_modified.put(id, modified);
				}
				data_input.close();
			}
			f = new File("data/modified_ai.sv");
			if (f.exists()) {
				DataInputStream data_input = new DataInputStream(new FileInputStream(f));
				int len = data_input.readInt();
				for (int i = 0; i < len; i++) {
					int id = data_input.readInt();
					long modified = data_input.readLong();
					sIA_tournament_modified.put(id, modified);
				}
				data_input.close();
			}
		} catch (Exception e) {

		}
	}

	public static void writeAIModified(DataOutputStream output) throws IOException {
		output.writeInt(sIA_modified.size());
		for (Entry<Integer, Long> e : sIA_modified.entrySet()) {
			output.writeInt(e.getKey());
			output.writeLong(e.getValue());
		}
	}

	public static void save() {
		try {
			File f = new File("data/modified.sv");
			DataOutputStream output = new DataOutputStream(new FileOutputStream(f));
			output.writeInt(sIA_modified.size());
			for (Entry<Integer, Long> e : sIA_modified.entrySet()) {
				output.writeInt(e.getKey());
				output.writeLong(e.getValue());
			}
			output.close();
			f = new File("data/modified_ai.sv");
			output = new DataOutputStream(new FileOutputStream(f));
			output.writeInt(sIA_tournament_modified.size());
			for (Entry<Integer, Long> e : sIA_tournament_modified.entrySet()) {
				output.writeInt(e.getKey());
				output.writeLong(e.getValue());
			}
			output.close();
		} catch (Exception e) {

		}
	}

	public static boolean compileCode(String name, LeekAI ai) {
		// if(ai.getValid() == 0) return false;//Si l'ia est invalide on se
		// fatigue pas

		File compiled = new File(IA_PATH + name + ".class");
		if (compiled.exists())
			compiled.delete();
		File java = new File(IA_PATH + name + ".java");
		if (java.exists())
			java.delete();

		if (ai.getCompiled() == null || ai.getCompiled().isEmpty()) {// Pas de
																		// code
			// java
			if (ai.v2) {
				return true;
			}
			if (ai.getCode().isEmpty()) {// Pas de code du tout...
				return false;
			} else {
				// On compile l'IA
				new IACompiler(ai);
			}
		}
		if (ai.getCompiled().isEmpty())
			return false;// Rien ne compile
		// Si on a maintenant du code java
		try {
			FileOutputStream output = new FileOutputStream(java);
			output.write(ai.getCompiled().getBytes());
			output.close();
		} catch (Exception e) {
			ErrorManager.exception(e);
			return false;
		}
		return true;
	}

	public static boolean isValid(LeekAI ai) {
		if ((ai.getAITournament() ? sIA_tournament_modified : sIA_modified).containsKey(ai.getId())) {
			if ((ai.getAITournament() ? sIA_tournament_modified : sIA_modified).get(ai.getId()) == ai.getModified())
				return true;
		}
		return false;
		// return f.lastModified() > ai.getModified() && (!isJava || f.length()
		// != ai.getCompiled().length());
	}

	public static AI getUserAI(LeekAI ai) throws LeekScriptException {

		String name = "IA_" + ai.getClassName();
		String error = "";
		File compiled = new File(IA_PATH + name + ".class");
		File java = new File(IA_PATH + name + ".java");

		if (!compiled.exists() || !isValid(ai)) {

			// On commence par la conversion LS->Java
			if (!compileCode(name, ai)) {
				return null;
			}

			// On va compiler le java maintenant
			JavaCompiler compiler = new JavaCompiler(java);
			int status = JavaCompiler.INIT;

			try {
				compiler.compile();
				status = JavaCompiler.getStatus();
			} catch (CompilationException e) {

				error = e.getMessage();
//				ErrorManager.registerCompilationError(ai, e.getMessage());
				status = JavaCompiler.ERROR;

			} catch (Exception e) {

				ErrorManager.exception(e, ai.getId());
				ErrorManager.exception(e);
				status = JavaCompiler.ERROR;
			}
			(ai.getAITournament() ? sIA_tournament_modified : sIA_modified).put(ai.getId(), ai.getModified());

			if (status == JavaCompiler.ERROR) {
				java.delete();
				throwException(error);
			}
		}
		AI loaded_ai = IALoader.loadAI(IA_PATH, name);
		if (loaded_ai == null) {
			throwException("compilation_error");
		}
		return loaded_ai;
	}

	public static void throwException(String error) throws LeekScriptException {
		if (error != null && !error.isEmpty()) {
			if (error.contains("code too large")) {
				String[] lines = error.split("\n", 3);
				if (lines.length >= 2 && lines[1].split(" ").length > 4) {
					String l = lines[1].split(" ")[2];
					if (l.length() > 4 && !l.startsWith("runIA")) {
						throw new LeekScriptException(LeekScriptException.CODE_TOO_LARGE_FUNCTION, l.substring(14, l.length() - 2));
					}
				}
				throw new LeekScriptException(LeekScriptException.CODE_TOO_LARGE);
			}
		}
		throw new LeekScriptException(LeekScriptException.CANT_COMPILE);
	}
}