package leekscript;

import java.util.Scanner;
import java.io.File;

import leekscript.compiler.LeekScript;
import leekscript.runner.AI;

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
			AI ai = LeekScript.compileSnippet(code, "AI");
			long compileTime = System.currentTimeMillis() - ct;

			long et = System.currentTimeMillis();
			ai.staticInit();
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

	private static void executeFile(File file) {
		try {
			long ct = System.currentTimeMillis();
			AI ai = LeekScript.compileFile(file.getPath(), "AI", false);
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
