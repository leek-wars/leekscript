package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import leekscript.ErrorManager;
import leekscript.LSException;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystemContext;
import leekscript.compiler.resolver.FileSystemResolver;
import leekscript.compiler.resolver.Resolver;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;

public class LeekScript {
	
	private final static String IA_PATH = "ai/";
	private static long id = 1;
	
	private static Resolver<FileSystemContext> defaultResolver = new FileSystemResolver();
	private static Resolver<?> customResolver = null;
	
	private static RandomGenerator defaultRandomGenerator = new RandomGenerator() {
		private Random random = new Random();
		@Override
		public void seed(long seed) {
			random.setSeed(seed);
		}
		@Override
		public int getInt(int min, int max) {
			if (max - min + 1 <= 0)
				return 0;
			return min + random.nextInt(max - min + 1);
		}
		@Override
		public double getDouble() {
			return random.nextDouble();
		}
	};
	private static RandomGenerator customRandomGenerator = null;
	
	public static AI compileFile(String filepath, String AIClass, String jar, boolean nocache) throws LeekScriptException, LeekCompilerException {
		AIFile<?> ai = getResolver().resolve(filepath, null);
		int id = (filepath + "_" + ai.getCode()).hashCode() & 0xfffffff;
		ai.setJavaClassName("IA_" + id);
		return compile(ai, AIClass, jar, nocache);
	}
	
	public static AI compileSnippet(String snippet, String AIClass, String jar) throws LeekScriptException, LeekCompilerException {
		AIFile<?> ai = new AIFile<FileSystemContext>("<snippet>", snippet, null);
		ai.setJavaClassName("IA_" + id++);
		return compile(ai, AIClass, jar, false);
	}
	
	public static boolean testScript(String leek, String script, AbstractLeekValue s, String AIClass, String jar, boolean nocache) throws Exception {
		AI ai = LeekScript.compileSnippet(script, AIClass, jar);
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
	
	public static AbstractLeekValue runScript(String script, boolean nocache) throws Exception {
		return LeekScript.compileSnippet(script, "AI", "leekscript.jar").runIA();
	}
	
	public static boolean testScript(String script, AbstractLeekValue s) throws Exception {
		AI ai = LeekScript.compileSnippet(script, "AI", "leekscript.jar");
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		return v.equals(ai, s);
	}
	
	public static String runFile(String filename) throws Exception {
		AI ai = LeekScript.compileFile(filename, "AI", "leekscript.jar", true);
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		return v.getString(ai);
	}
	
	public static void setResolver(Resolver<?> resolver) {
		customResolver = resolver;
	}
	public static void resetResolver() {
		customResolver = null;
	}
	public static Resolver<?> getResolver() {
		return customResolver != null ? customResolver : defaultResolver;
	}
	
	public static void setRandomGenerator(RandomGenerator generator) {
		customRandomGenerator = generator;
	}
	public static RandomGenerator getRandom() {
		return customRandomGenerator != null ? customRandomGenerator : defaultRandomGenerator;
	}

	private static AI compile(AIFile<?> ai, String AIClass, String jar, boolean nocache) throws LeekScriptException, LeekCompilerException {

		new File(IA_PATH).mkdir();
		String javaClassName = ai.getJavaClassName();
		String error = "";
		File compiled = new File(IA_PATH + javaClassName + ".class");
		File java = new File(IA_PATH + javaClassName + ".java");
		
		if (!compiled.exists() || nocache) {
			// On commence par la conversion LS->Java
			if (ai.getCode().isEmpty()) { // Pas de code du tout...
				System.out.println("No code!");
				return null;
			}
			// On compile l'IA
			String compiledJava = new IACompiler().compile(ai, AIClass);
			
			if (compiledJava.isEmpty()) {
				System.out.println("No java generated!");
				return null; // Rien ne compile
			}
			// Si on a maintenant du code java
			try {
				FileOutputStream output = new FileOutputStream(java);
				output.write(compiledJava.getBytes());
				output.close();
			} catch (Exception e) {
				ErrorManager.exception(e);
				System.out.println("Failed to compiled AI: " + ai.getPath());
				return null;
			}
	
			// On va compiler le java maintenant
			JavaCompiler compiler = new JavaCompiler(java);
			int status = JavaCompiler.INIT;
	
			try {
				compiler.compile(jar);
				status = JavaCompiler.getStatus();
			} catch (CompilationException e) {
	
				error = e.getMessage();
				// ErrorManager.registerCompilationError(ai, e.getMessage());
				status = JavaCompiler.ERROR;
	
			} catch (Exception e) {
	
				// ErrorManager.exception(e, ai.getId());
				ErrorManager.exception(e);
				status = JavaCompiler.ERROR;
			}
			if (status == JavaCompiler.ERROR) {
				java.delete();
				throwException(error);
			}
		}
		return IALoader.loadAI(IA_PATH, javaClassName);
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