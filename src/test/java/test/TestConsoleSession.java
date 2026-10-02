package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.Session;

/**
 * Console interactive : chaque ligne est une IA à part, qui partage ses variables par la
 * session. Le coût affiché d'une ligne se calcule comme dans la console du démon.
 */
public class TestConsoleSession {

	/** Coût de chaque ligne jouée dans une même session. */
	private static long[] costs(String... lines) throws Exception {
		var session = new Session(4, false);
		var costs = new long[lines.length];
		for (int i = 0; i < lines.length; i++) {
			var ai = LeekScript.compileSnippet(lines[i], "AI", new Options(4, false, false, true, session, false));
			ai.maxOperations = 1_000_000;
			long before = ai.operations();
			ai.init();
			ai.staticInit();
			ai.runIA(session);
			costs[i] = ai.operations() - before + session.previousOperations();
		}
		return costs;
	}

	@Test
	public void functionDefinedOnAPreviousLineCountsItsOperations() throws Exception {
		var separate = costs("var f = x => x ** 2", "f(42)");
		var together = costs("var f = x => x ** 2 f(42)");
		assertTrue(separate[1] > 0, "appeler une fonction d'une ligne passée a un coût");
		assertEquals(together[0] - separate[0], separate[1], "même coût que sur la même ligne");
	}

	@Test
	public void eachCallIsCountedOnce() throws Exception {
		var c = costs("var f = x => x ** 2", "f(42)", "f(42)", "var y = 1");
		assertEquals(c[1], c[2], "le compteur d'une ligne passée repart de zéro à chaque ligne");
		assertEquals(costs("var y = 1")[0], c[3], "une ligne qui n'appelle rien ne paie pas les appels d'avant");
	}
}
