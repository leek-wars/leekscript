package test;

import java.util.concurrent.atomic.AtomicInteger;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.bytecode.BytecodeBackend;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

/**
 * Mode différentiel (`-Dleekscript.bytecode.diff`) des tests à plusieurs fichiers (modules,
 * packages) : avant le chargement du test, le programme est compilé et joué par javac puis par le
 * backend bytecode, sans cache et sous un nom de classe à lui ; un écart s'affiche en `[DIFF]`.
 */
final class BackendDiff {

	private BackendDiff() {}

	static final boolean ENABLED = System.getProperty("leekscript.bytecode.diff") != null;
	private static final AtomicInteger compared = new AtomicInteger();
	private static final AtomicInteger names = new AtomicInteger();

	static {
		if (ENABLED) Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println(compared.get() + " programmes à plusieurs fichiers comparés javac / bytecode")));
	}

	private record Execution(String result, long operations, String error, boolean bytecode) {}

	/** Compare les deux backends sur le programme d'entrée `file` (de classe racine `rootClass`). */
	static void compare(AIFile file, String rootClass) {
		if (!ENABLED) return;
		var javaClass = file.getJavaClass();
		try {
			BytecodeBackend.override.set(false);
			var javac = execute(file, rootClass);
			BytecodeBackend.override.set(true);
			var bytecode = execute(file, rootClass);
			if (!bytecode.bytecode()) return;
			compared.incrementAndGet();
			if (!javac.equals(new Execution(bytecode.result(), bytecode.operations(), bytecode.error(), false))) {
				System.out.println("[DIFF] " + file.getPath() + " : javac " + javac + " / bytecode " + bytecode);
			}
		} finally {
			BytecodeBackend.override.remove();
			file.setJavaClass(javaClass);
			file.setRootClass(rootClass);
		}
	}

	private static Execution execute(AIFile file, String rootClass) {
		// Un nom neuf : ni le cache en mémoire des classes ni celui du disque ne servent
		file.setJavaClass("AI_diff" + names.incrementAndGet());
		file.setRootClass(rootClass);
		AI ai;
		try {
			ai = JavaCompiler.compile(file, new Options(LeekScript.MAX_VERSION, false, false, true, null, true));
		} catch (Exception e) {
			return new Execution("compilation " + e.getClass().getSimpleName(), 0, String.valueOf(e.getMessage()), false);
		}
		boolean bytecode = file.getCompiledCode().isBytecode();
		try {
			ai.init();
			ai.staticInit();
			ai.resetCounter();
			var value = ai.runIA();
			return new Execution(ai.string(value), ai.getOperations(), null, bytecode);
		} catch (LeekRunException e) {
			return new Execution(e.getError().toString(), ai.getOperations(), ai.getErrorMessage(e), bytecode);
		} catch (Throwable e) {
			return new Execution(ai.throwableToError(e).type.toString(), ai.getOperations(), ai.getErrorMessage(e), bytecode);
		}
	}
}
