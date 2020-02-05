package leekscript;

import java.util.Scanner;
import java.io.File;

import leekscript.compiler.LeekScript;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;

public class TopLevel {

	public static void main(String[] args) {
		if (args.length < 1) {
			Scanner input = new Scanner(System.in);
		    System.out.print(">>> ");
		    String code;
		    while ((code = input.nextLine()) != null) {
		    	executeSnippet(code);
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
		try {
			long ct = System.currentTimeMillis();
			AI ai = LeekScript.compileSnippet(code, "AI", "leekscript.jar");
			long compileTime = System.currentTimeMillis() - ct;

			long et = System.currentTimeMillis();
			AbstractLeekValue v = ai.runIA();
			long executionTime = System.currentTimeMillis() - et;

			String result = v.getString(ai);
			long ops = ai.getOperations();

			System.out.println(result);
			System.out.println("(" + ops + " ops, " + compileTime + "ms + " + executionTime + "ms)");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void executeFile(File file) {
		try {
			long ct = System.currentTimeMillis();
			AI ai = LeekScript.compileFile(file.getPath(), "AI", "leekscript.jar", false);
			long compileTime = System.currentTimeMillis() - ct;

			long et = System.currentTimeMillis();
			AbstractLeekValue v = ai.runIA();
			long executionTime = System.currentTimeMillis() - et;

			String result = v.getString(ai);
			long ops = ai.getOperations();

			System.out.println(result);
			System.out.println("(" + ops + " ops, " + compileTime + "ms + " + executionTime + "ms)");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
