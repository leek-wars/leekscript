package leekscript.compiler.bytecode;

/**
 * Construction que le backend bytecode ne sait pas encore émettre : la compilation retombe
 * sur le chemin javac. Sans pile d'appels, elle sert de contrôle de flux.
 */
public class Unsupported extends RuntimeException {

	/** Diagnostic : `-Dleekscript.bytecode.trace=<motif>` imprime d'où vient un repli dont la raison contient le motif. */
	private static final String TRACE = System.getProperty("leekscript.bytecode.trace");

	static boolean tracing() {
		return TRACE != null;
	}

	public Unsupported(String what) {
		super(what, null, false, false);
		if (TRACE != null && what != null && what.contains(TRACE)) {
			var at = BytecodeWriter.CURRENT_LOCATION.get();
			new Throwable("repli : " + what + (at != null ? " (" + at.getFile().getPath() + ":" + at.getStartLine() + ")" : "")).printStackTrace(System.out);
		}
	}
}
