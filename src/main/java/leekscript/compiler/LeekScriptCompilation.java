package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import leekscript.LSException;
import leekscript.LeekAI;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;

public class LeekScriptCompilation {

	private static class IACompilerTest extends IACompiler {
		public IACompilerTest(LeekAI ai) {
			mAI = ai;
		}

		@Override
		public LeekAI getAI(String name) throws IACompilerException {
			return mAI;
		}

		@Override
		public LeekAI getAI(Integer id) {
			return mAI;
		}
	}

	private static class JavaCompiler {
		public final static int INIT = 0;
		public final static int RUNNING = 1;
		public final static int END = 2;
		public final static int ERROR = 3;
		private final File mInput;
		private int mStatus = 0;

		public JavaCompiler(File input) {
			mInput = input;

		}

		private static boolean isWindows() {
			try {
				return System.getProperty("os.name").contains("Windows");
			} catch (SecurityException ex) {
				return false;
			}
		}

		public void compile() throws Exception {
			if (mStatus != INIT)
				return;
			mStatus = RUNNING;
			if (!mInput.exists()) {
				mStatus = ERROR;
				return;
			}
			String line = "javac";
			if (isWindows())
				line = "C:\\Program Files\\Java\\jdk1.8.0_25\\bin\\javac.exe";

			String[] args = { line, "-encoding", "utf8", "-classpath", "leekscript.jar", mInput.getAbsolutePath() };
			Process process = Runtime.getRuntime().exec(args);
			// , new File(System.getProperty("user.dir")));

			boolean error = false;
			InputStream iin = process.getErrorStream();
			if (iin != null) {
				byte[] e = new byte[128];
				int nb;
				StringBuilder sb = new StringBuilder();
				while ((nb = iin.read(e)) > 0) {
					sb.append(new String(Arrays.copyOf(e, nb)));
				}

				if (sb.length() > 0) {
					mStatus = ERROR;
					error = true;
					throw new Exception(sb.toString());
				}
			}
			iin = process.getInputStream();
			if (iin != null) {
				byte[] e = new byte[128];
				int nb;
				while ((nb = iin.read(e)) > 0) {
					System.out.println(new String(Arrays.copyOf(e, nb)));
				}
			}
			process.waitFor();
			if (process.exitValue() != 0)
				error = true;
			if (!error)
				mStatus = END;
		}

		public static int getStatus() {
			return 0;
		}

	}

	// Classe principale du LeekScript
	private final static String IA_PATH = "ai/";

	public static boolean compileCode(String name, LeekAI ai) {
		// if(ai.getValid() == 0) return false;//Si l'ia est invalide on se
		// fatigue pas

		File compiled = new File(IA_PATH + name + ".class");
		if (compiled.exists())
			compiled.delete();
		File java = new File(IA_PATH + name + ".java");
		if (java.exists())
			java.delete();

		if (ai.getCompiled().isEmpty())
			return false;// Rien ne compile
		// Si on a maintenant du code java
		try {
			FileOutputStream output = new FileOutputStream(java);
			output.write(ai.getCompiled().getBytes());
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static AI getUserAI(LeekAI ai) {

		String name = "IA_" + ai.getClassName();
		// File compiled = new File(IA_PATH + name + ".class");
		File java = new File(IA_PATH + name + ".java");

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
		} catch (Exception e) {
			System.out.println("Erreur de compilation !");
			e.printStackTrace();
			status = JavaCompiler.ERROR;
			return null;
		}
		if (status == JavaCompiler.ERROR) {
			// java.delete();
			return null;
		}
		return IALoader.loadAI(IA_PATH, name);
	}

	public static AI compile(String leekscript) throws Exception {
		LeekAI ai = new LeekAI(1, 1, "Test", 0, leekscript, 0, 0, 0, 0, false);
		IACompiler c = new IACompilerTest(ai);// On lance la compilation du code
												// de l'IA
		WordParser parser = new WordParser(ai);
		// Si on est là c'est qu'on a une liste de words correcte, on peut
		// commencer à lire
		MainLeekBlock main = new MainLeekBlock(c, ai);
		WordCompiler compiler = new WordCompiler(parser, main);
		compiler.readCode();

		JavaWriter writer = new JavaWriter(false);
		compiler.writeJava("IA_" + ai.getClassName(), writer);

		ai.setCompiled(writer.getJavaCode());
		return LeekScriptCompilation.getUserAI(ai);
	}

	public static boolean testScript(String leek, String script, AbstractLeekValue s) throws Exception {
		AI ai = LeekScriptCompilation.compile(script);
		AbstractLeekValue v = ai.runIA();
		if (v.equals(ai, s))
			return true;
		ArrayLeekValue tab1 = v.getArray();
		ArrayLeekValue tab2 = s.getArray();
		if (tab1 != null && tab2 != null && tab1.size() == tab2.size()) {
			int i = 0;
			for (i = 0; i < tab1.size(); i++) {
				if (!tab1.get(ai, i).equals(ai, tab2.get(ai, i))) {
					throw new LSException(i, tab1.get(ai, i), tab2.get(ai, i));
				}
			}
		} else
			System.out.println(v.getString(ai) + " -- " + s.getString(ai));
		return false;
	}

	public static boolean testScript(String script, AbstractLeekValue s) throws Exception {
		AI ai = LeekScriptCompilation.compile(script);
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		return v.equals(ai, s);
	}
}
