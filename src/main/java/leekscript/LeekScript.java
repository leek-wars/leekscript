package leekscript;

import java.util.Scanner;

import leekscript.compiler.LeekScriptCompilation;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;

public class LeekScript {

	public static void main(String[] args) {
		if (args.length < 1) {
			Scanner input = new Scanner(System.in);
		    System.out.print(">>> ");
		   
		    String code;
		    while ((code = input.nextLine()) != null) {
		    	execute(code);
		    	System.out.print(">>> ");
			}
	    	input.close();
		} else {
			String code = args[0];
			execute(code);
		}
	}
	
	private static void execute(String code) {
		try {
			long ct = System.currentTimeMillis();
			AI ai = LeekScriptCompilation.compile(code, "AI");
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
