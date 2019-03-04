package leekscript.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import leekscript.ErrorManager;
import leekscript.LSException;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystemResolver;
import leekscript.compiler.resolver.Resolver;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;

public class LeekScript {
	private final static String IA_PATH = "ai/";
	
	private static Resolver defaultResolver = new FileSystemResolver();
	private static Resolver customResolver = null;
	
	public static AI compileFile(int id, String filepath, String AIClass) throws LeekScriptException, LeekCompilerException {
		String code = "";
		try {
			code = new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return compile(id, code, AIClass);
	}

	public static AI compile(int id, String code, String AIClass) throws LeekScriptException, LeekCompilerException {

		String name = "IA_" + id;
		String error = "";
		File compiled = new File(IA_PATH + name + ".class");
		if (compiled.exists()) {
			compiled.delete();
		}
		File java = new File(IA_PATH + name + ".java");
		if (java.exists()) {
			java.delete();
		}

		// On commence par la conversion LS->Java
		if (code.isEmpty()) { // Pas de code du tout...
			System.out.println("No code!");
			return null;
		}
		// On compile l'IA
		String compiledJava = new IACompiler().compile(id, name, code, AIClass);
		
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
			System.out.println("Failed to compiled AI: " + code);
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

			// ErrorManager.exception(e, ai.getId());
			ErrorManager.exception(e);
			status = JavaCompiler.ERROR;
		}

		if (status == JavaCompiler.ERROR) {
			java.delete();
			throwException(error);
		}
		return IALoader.loadAI(IA_PATH, name);
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
	
	public static boolean testScript(String leek, String script, AbstractLeekValue s, String AIClass) throws Exception {
		AI ai = LeekScript.compile(1212, script, AIClass);
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
		AI ai = LeekScript.compile(1212, script, "AI");
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		return v.equals(ai, s);
	}
	
	public static String runFile(String filename) throws Exception  {
		AI ai = LeekScript.compileFile(1212, filename, "AI");
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		return v.getString(ai);
	}
	
	public static void setResolver(Resolver resolver) {
		customResolver = resolver;
	}

	public static Resolver getResolver() {
		return customResolver != null ? customResolver : defaultResolver;
	}
}