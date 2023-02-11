package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import leekscript.compiler.LeekScript;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.common.Error;

public class TestCommon {

	private static String GREEN_BOLD = "\033[1;32m";
	private static String C_RED = "\033[1;31m";
	private static String END_COLOR = "\033[0m";
	private static String C_PINK = "\033[1;95m";
	private static String C_GREY = "\033[0;90m";

	private static int LATEST_VERSION = 4;

	private static int tests = 0;
	private static int success = 0;
	private static int disabled = 0;
	private static long analyze_time = 0;
	private static long compile_time = 0;
	// private static long load_time = 0;
	private static long execution_time = 0;
	private static ArrayList<Long> operationsReference = new ArrayList<>();
	// private static int operationsReferenceIndex = 0;
	private static ArrayList<Long> operations = new ArrayList<>();

	private static List<String> failedTests = new ArrayList<String>();
	private static List<String> disabledTests = new ArrayList<String>();

	public static class Case {
		String code;
		boolean enabled = true;
		int version_min = 1;
		int version_max = LATEST_VERSION;
		long maxOperations = Long.MAX_VALUE;
		long maxRAM = AI.MAX_RAM;

		public Case(String code, boolean enabled) {
			this.code = code;
			this.enabled = enabled;
		}

		public Case(String code, boolean enabled, int version_min, int version_max) {
			this.code = code;
			this.enabled = enabled;
			this.version_min = version_min;
			this.version_max = version_max;
		}

		public String equals(String expected) {
			return run(new Checker() {
				public boolean check(Result result) {
					return result.result.equals(expected);
				}
				public String getExpected() { return expected; }
				public String getResult(Result result) { return result.result; }
			});
		}

		public String error(Error type) {
			return run(new Checker() {
				public boolean check(Result result) {
					if (result.error != null) {
						return result.error == type;
					} else if (result.ai != null) {
						var errors = result.ai.getFile().getErrors();
						return errors.size() > 0 && errors.get(0).level == AnalyzeErrorLevel.ERROR && errors.get(0).error == type;
					}
					return false;
				}
				public String getExpected() { return "error " + type.name(); }
				public String getResult(Result result) {
					if (result.error != null) {
						return "error " + result.error.name() + " " + Arrays.toString(result.parameters);
					} else if (result.ai != null) {
						var errors = result.ai.getFile().getErrors();
						if (errors.size() > 0) return "error " + errors.get(0).error.name();
					}
					if (result.error != Error.NONE) {
						return result.error.name();
					}
					return "no error";
				}
			});
		}

		public String warning(Error type) {
			return run(new Checker() {
				public boolean check(Result result) {
					if (result.ai != null) {
						var errors = result.ai.getFile().getErrors();
						return errors.size() > 0 && errors.get(0).level == AnalyzeErrorLevel.WARNING && errors.get(0).error == type;
					}
					return false;
				}
				public String getExpected() { return "warning " + type.name(); }
				public String getResult(Result result) {
					if (result.ai != null) {
						var errors = result.ai.getFile().getErrors();
						if (errors.size() > 0) return "warning " + errors.get(0).error.name();
					}
					if (result.error != Error.NONE) {
						return result.error.name();
					}
					return "no warning";
				}
			});
		}

		public String any_error() {
			return run(new Checker() {
				public boolean check(Result result) {
					return result.error != Error.NONE;
				}
				public String getExpected() { return "no error"; }
				public String getResult(Result result) { return result.error.name(); }
			});
		}

		public void almost(double expected) {
			almost(expected, 1e-10);
		}

		public void almost(double expected, double delta) {
			run(new Checker() {
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

		public void ops(long ops) {
			run(new Checker() {
				public boolean check(Result result) {
					return result.operations == ops;
				}
				public String getExpected() { return String.valueOf(ops); }
				public String getResult(Result result) { return String.valueOf(result.operations); }
			});
		}

		public String run(Checker checker) {
			if (!enabled) {
				disabled++;
				var s = C_PINK + "[DISA] " + END_COLOR + "[v" + version_min + "-" + version_max + "] " + code;
				System.out.println(s);
				disabledTests.add(s);
				return "disabled";
			}
			for (int v = version_min; v <= version_max - 1; ++v) {
				run_version(v, checker);
			}
			return run_version(version_max, checker);
		}

		public String run_version(int version, Checker checker) {
			tests++;
			int aiID = 0;
			Result result;
			long compile_time = 0;
			long ops = 0;
			AI ai = null;
			long t = System.nanoTime();
			try {
				boolean is_file = code.contains(".leek");

				ai = is_file ? LeekScript.compileFile(code, "AI", version) : LeekScript.compileSnippet(code, "AI", version);
				ai.init();
				ai.staticInit();
				aiID = ai.getId();

				compile_time = ai.getCompileTime() / 1000000;
				TestCommon.analyze_time += ai.getAnalyzeTime() / 1000000;
				TestCommon.compile_time += ai.getCompileTime() / 1000000;
				// TestCommon.load_time += ai.getLoadTime() / 1000000;

				ai.maxOperations = this.maxOperations;
				ai.maxRAM = this.maxRAM;

				t = System.nanoTime();
				var v = ai.runIA();
				long exec_time = (System.nanoTime() - t) / 1000;
				TestCommon.execution_time += exec_time / 1000;

				ops = ai.operations();

				var vs = ai.export(v, new HashSet<>());
				result = new Result(vs, ai, Error.NONE, new String[0], ai.getOperations(), exec_time);

			} catch (LeekCompilerException e) {
				// e.printStackTrace();
				// System.out.println("Error = " + e.getError());
				result = new Result(e.getError().toString() + " " + Arrays.toString(e.getParameters()), ai, e.getError(), e.getParameters(), 0, 0);
			} catch (LeekRunException e) {
				long exec_time = (System.nanoTime() - t) / 1000;
				result = new Result(e.getError().toString(), ai, e.getError(), new String[0], ai.getOperations(), exec_time);
			} catch (Exception e) {
				e.printStackTrace(System.out);
				result = new Result("unknown error!", ai, Error.UNKNOWN_ERROR, new String[0], 0, 0);
			}

			operations.add(ops);
			// long referenceOperations = operationsReference.get(operationsReferenceIndex++);

			if (checker.check(result)) {
				int ops_per_ms = (int) Math.round(1000 * (double) result.operations / result.exec_time);
				System.out.println(GREEN_BOLD + " [OK]  " + END_COLOR + "[v" + version + "] " + code + " === " + checker.getResult(result) + "	" + C_GREY + compile_time + "ms + " + fn(result.exec_time) + "µs" + ", " + fn(result.operations) + " ops, " + ops_per_ms + " ops/ms" + END_COLOR);
				success++;
			} else {
				var err = C_RED + "[FAIL] " + END_COLOR + "[v" + version + "] " + code + " =/= " + checker.getExpected() + " got " + checker.getResult(result) + "\n" +
				"/home/pierre/dev/leek-wars/generator/leekscript/ai/AI_" + aiID + ".java";
				System.out.println(err);
				failedTests.add(err);
			}
			return result.result;
		}

		public Case max_ops(long ops) {
			this.maxOperations = ops;
			return this;
		}

		public Case max_ram(long ram) {
			this.maxRAM = ram;
			return this;
		}
	}

	public static class Result {
		String result;
		AI ai;
		Error error;
		String[] parameters;
		long operations;
		long exec_time;

		public Result(String result, AI ai, Error error, String[] parameters, long operations, long exec_time) {
			this.result = result;
			this.ai = ai;
			this.operations = operations;
			this.exec_time = exec_time;
			this.error = error;
			this.parameters = parameters;
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
	public Case file_v1(String code) {
		return new Case(code, true, 1, 1);
	}
	public Case file_v2_(String code) {
		return new Case(code, true, 2, LATEST_VERSION);
	}
	public Case file_v3(String code) {
		return new Case(code, true, 3, 3);
	}
	public Case file_v4_(String code) {
		return new Case(code, true, 4, LATEST_VERSION);
	}
	public Case DISABLED_file(String code) {
		return new Case(code, false);
	}
	public Case DISABLED_file_v2_(String code) {
		return new Case(code, false, 2, LATEST_VERSION);
	}
	public Case code_v1(String code) {
		return new Case(code, true, 1, 1);
	}
	public Case code_v1_2(String code) {
		return new Case(code, true, 1, 2);
	}
	public Case code_v1_3(String code) {
		return new Case(code, true, 1, 3);
	}
	public Case code_v2(String code) {
		return new Case(code, true, 2, 2);
	}
	public Case code_v2_(String code) {
		return new Case(code, true, 2, LATEST_VERSION);
	}
	public Case code_v2_3(String code) {
		return new Case(code, true, 2, 3);
	}
	public Case code_v3(String code) {
		return new Case(code, true, 3, 3);
	}
	public Case code_v3_(String code) {
		return new Case(code, true, 3, LATEST_VERSION);
	}
	public Case code_v1_4(String code) {
		return new Case(code, true, 1, 4);
	}
	public Case code_v4_(String code) {
		return new Case(code, true, 4, LATEST_VERSION);
	}
	public Case DISABLED_code_v4_(String code) {
		return new Case(code, false, 4, LATEST_VERSION);
	}
	public Case DISABLED_code(String code) {
		return new Case(code, false);
	}
	public Case DISABLED_code_v2_(String code) {
		return new Case(code, false, 2, LATEST_VERSION);
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
	public static void loadReferenceOperations() {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("opérations_v1.txt"));
			String line = reader.readLine();
			while (line != null) {
				operationsReference.add(Long.parseLong(line));
				line = reader.readLine();
			}
			System.out.println(operationsReference.size() + " test operations references loaded.");
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
