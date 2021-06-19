package test;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import leekscript.compiler.LeekScript;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;
import leekscript.common.Error;

public class TestCommon {

	private static String GREEN_BOLD = "\033[1;32m";
	private static String C_RED = "\033[1;31m";
	private static String END_COLOR = "\033[0m";
	private static String C_PINK = "\033[1;95m";
	private static String C_GREY = "\033[0;90m";

	private static int tests = 0;
	private static int success = 0;
	private static int disabled = 0;
	private static long analyze_time = 0;
	private static long compile_time = 0;
	private static long load_time = 0;
	private static long execution_time = 0;
	private static ArrayList<Long> operations = new ArrayList<>();

	private static List<String> failedTests = new ArrayList<String>();
	private static List<String> disabledTests = new ArrayList<String>();

	public static class Case {
		String code;
		boolean enabled = true;
		int version = -1;

		public Case(String code, boolean enabled) {
			this.code = code;
			this.enabled = enabled;
		}

		public Case(String code, boolean enabled, int version) {
			this.code = code;
			this.enabled = enabled;
			this.version = version;
		}

		public String equals(String expected) {
			return run(version, new Checker() {
				public boolean check(Result result) {
					return result.result.equals(expected);
				}
				public String getExpected() { return expected; }
				public String getResult(Result result) { return result.result; }
			});
		}

		public String error(Error type) {
			return run(version, new Checker() {
				public boolean check(Result result) {
					return result.error == type;
				}
				public String getExpected() { return type.name(); }
				public String getResult(Result result) { return result.error.name(); }
			});
		}

		public void almost(double expected) {
			almost(expected, 1e-10);
		}

		public void almost(double expected, double delta) {
			run(version, new Checker() {
				public boolean check(Result result) {
					try {
						double r = Double.parseDouble(result.result);
						return Math.abs(r - expected) < delta;
					} catch (Exception e) {
						return false;
					}
				}
				public String getExpected() { return String.valueOf(expected); }
				public String getResult(Result result) { return result.result; }
			});
		}

		public String run(int version, Checker checker) {
			if (!enabled) {
				disabled++;
				var s = C_PINK + "[DISA] " + END_COLOR + "[v" + version + "] " + code;
				System.out.println(s);
				disabledTests.add(s);
				return "disabled";
			}
			if (version == -1) {
				run_version(10, checker);
				return run_version(11, checker);
			} else {
				return run_version(version, checker);
			}
		}
		public String run_version(int version, Checker checker) {
			tests++;
			int aiID = 0;
			Result result;
			long compile_time = 0;
			long ops = 0;
			try {
				boolean is_file = code.contains(".leek");

				AI ai = is_file ? LeekScript.compileFile(code, "AI", version) : LeekScript.compileSnippet(code, "AI", version);
				aiID = ai.getId();

				// compile_time = ai.getCompileTime() / 1000000;
				// TestCommon.analyze_time += ai.getAnalyzeTime() / 1000000;
				// TestCommon.compile_time += ai.getCompileTime() / 1000000;
				// TestCommon.load_time += ai.getLoadTime() / 1000000;

				ai.maxOperations = Integer.MAX_VALUE;

				long t = System.nanoTime();
				var v = ai.runIA();
				long exec_time = (System.nanoTime() - t) / 1000;
				TestCommon.execution_time += exec_time / 1000;

				ops = ai.getOperations();

				var vs = v.getString(ai);
				result = new Result(vs, Error.NONE, (int) ai.getOperations(), exec_time);

			} catch (LeekCompilerException e) {
				// e.printStackTrace();
				// System.out.println("Error = " + e.getError());
				result = new Result(e.getError().toString(), e.getError(), 0, 0);
			} catch (Exception e) {
				// e.printStackTrace();
				result = new Result("error", Error.NONE, 0, 0);
			}

			operations.add(ops);

			if (checker.check(result)) {
				System.out.println(GREEN_BOLD + " [OK]  " + END_COLOR + "[v" + version + "] " + code + " === " + checker.getResult(result) + "	" + C_GREY + compile_time + "ms + " + fn(result.exec_time) + "µs" + ", " + fn(result.operations) + " ops" + END_COLOR);
				success++;
			} else {
				var err = C_RED + "[FAIL] " + END_COLOR + "[v" + version + "] " + code + " =/= " + checker.getExpected() + " got " + checker.getResult(result) + "\n" +
				"/home/pierre/dev/leek-wars/server/daemon/generator-v1/leekscript-v1/ai/AI_" + aiID + ".java";
				System.out.println(err);
				failedTests.add(err);
			}
			return result.result;
		}
	}

	public static class Result {
		String result;
		Error error;
		int operations;
		long exec_time;

		public Result(String result, Error error, int operations, long exec_time) {
			this.result = result;
			this.operations = operations;
			this.exec_time = exec_time;
			this.error = error;
		}
	}

	public static interface Checker {
		public boolean check(Result result);

		public String getResult(Result result);

		public String getExpected();
	}

	public Case code(String code) {
		return new Case(code, true);
	}
	public Case file(String code) {
		return new Case(code, true);
	}
	public Case file_v10(String code) {
		return new Case(code, true, 10);
	}
	public Case file_v11(String code) {
		return new Case(code, true, 11);
	}
	public Case DISABLED_file(String code) {
		return new Case(code, false);
	}
	public Case code_v10(String code) {
		return new Case(code, true, 10);
	}
	public Case code_v11(String code) {
		return new Case(code, true, 11);
	}
	public Case DISABLED_code(String code) {
		return new Case(code, false);
	}

	public void section(String title) {
		System.out.println("========== " + title + " ==========");
	}
	public void header(String title) {
		System.out.println("================================================");
		System.out.println("========== " + title + " ==========");
		System.out.println("================================================");
	}

	public static boolean summary() {
		System.out.println("================================================");
		System.out.println(success + " / " + tests + " tests passed, " + (tests - success) + " errors, " + disabled + " disabled");
		System.out.println("Total time: " + fn(analyze_time + compile_time + execution_time) + " ms"
			+ " = Analyze: " + fn(analyze_time) + " ms"
			+ " + Compile: " + fn(compile_time) + " ms"
			+ " + Execution: " + fn(execution_time) + " ms");
		System.out.println("================================================");

		for (String test : disabledTests) {
			System.out.println(test);
		}
		for (String test : failedTests) {
			System.out.println(test);
		}
		return success == tests;
	}

	public static String fn(long n) {
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

		symbols.setGroupingSeparator(' ');
		formatter.setDecimalFormatSymbols(symbols);
		return formatter.format(n);
	}
	public static void ouputOperationsFile() {
		try {
			FileWriter myWriter = new FileWriter("opérations.txt");
			for (Long ops : operations) {
				myWriter.write(String.valueOf(ops) + "\n");
			}
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
}
