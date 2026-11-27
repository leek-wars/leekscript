package leekscript.compiler.bytecode;

/**
 * Construction que le backend bytecode ne sait pas encore émettre : la compilation retombe
 * sur le chemin javac. Sans pile d'appels, elle sert de contrôle de flux.
 */
public class Unsupported extends RuntimeException {

	public Unsupported(String what) {
		super(what, null, false, false);
	}
}
