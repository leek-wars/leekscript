package leekscript;

import java.util.Scanner;
import java.io.File;

import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.AI;
import leekscript.runner.Session;

public class TopLevel {

	public static void main(String[] args) {
		if (args.length < 1) {
			Scanner input = new Scanner(System.in);
			System.out.print(">>> ");
			String code;
			var session = new Session();
			while ((code = input.nextLine()) != null) {
				executeSnippet(code, session);
				System.out.print(">>> ");
			}
			input.close();
		} else {

			File file = new File(args[0]);
			if (file.exists()) {
				executeFile(file);
			} else {
				executeSnippet(args[0]);
			}
		}
	}

	private static void executeSnippet(String code) {
		executeSnippet(code, null);
	}

	private static void executeSnippet(String code, Session session) {
		try {
			var options = new Options(session);

			long ct = System.currentTimeMillis();
			AI ai = LeekScript.compileSnippet(code, "AI", options);
			long compileTime = System.currentTimeMillis() - ct;

			long et = System.currentTimeMillis();
			ai.init();
			ai.staticInit();
			var v = ai.runIA(session);
			long executionTime = System.currentTimeMillis() - et;

			var result = ai.string(v);
			long ops = ai.operations();

			System.out.println(result);
			System.out.println("(" + ops + " ops, " + compileTime + "ms + " + executionTime + "ms)");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void executeFile(File file) {
		LeekScript.setFileSystem(LeekScript.getNativeFileSystem());
		try {
			var options = new Options();

			long ct = System.currentTimeMillis();
			AI ai = LeekScript.compileFile(file.getPath(), "AI", options);
			long compileTime = System.currentTimeMillis() - ct;

			long et = System.currentTimeMillis();
			var v = ai.runIA();
			long executionTime = System.currentTimeMillis() - et;

			var result = ai.string(v);
			long ops = ai.operations();

			System.out.println(result);
			System.out.println("(" + ops + " ops, " + compileTime + "ms + " + executionTime + "ms)");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
