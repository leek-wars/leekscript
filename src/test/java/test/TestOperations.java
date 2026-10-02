package test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.Session;

@ExtendWith(SummaryExtension.class)
public class TestOperations extends TestCommon {


		@Test
	public void testInit() throws Exception {
		header("Operations");
	}

	@Test
	public void testOperators() throws Exception {
		section("Operators");
		code("1").ops(0);
		code("1 + 1").ops(1);
		code("1 - 1").ops(1);
		code("2 * 2").ops(2);
		code("2 / 2").ops(5);
		code("2 \\ 2").ops(5);
		code("2 % 2").ops(5);
	}

	@Test
	public void testConditions() throws Exception {
		section("Conditions");
		// Une condition constante est pliée au codegen et ne coûte plus rien
		// (cf ConstantFolder / TestConstantFolding).
		code("if (1) {}").ops(0);
		code("var x = 1 if (x) {}").ops(2);
	}

	@Test
	public void testBoolean_operators() throws Exception {
		section("Boolean operators");
		code("1 or 2").ops(1);
		code("1 and 2").ops(1);
		code("(1 + 1) or (2 + 2)").ops(2);
		code("(1 + 1) and (2 + 2)").ops(3);
		code("(1 + 1) or (2 + 2) or (3 + 3)").ops(2);
		code("(1 + 1) and (2 + 2) and (3 + 3)").ops(5);
	}

	record ReplStep(String code, long expectedOps) {}

	/**
	 * https://leekwars.com/forum/category-3/topic-11211 — les valeurs persistées
	 * en session doivent facturer leurs ops sur l'AI courante (pas celle qui
	 * les a créées), sinon les coûts affichés par la console sont sous-évalués.
	 */
	@Test
	public void testSessionRepl() throws Exception {
		section("Console session (REPL)");
		runRepl(
			new ReplStep("Map m = [:]", 1),
			new ReplStep("m['a'] = 1", 4),
			new ReplStep("var a = m['a']", 3),
			new ReplStep("Array t = []", 1),
			new ReplStep("push(t, 2)", 2),
			new ReplStep("t[0] = 1", 3),
			new ReplStep("var b = t[0]", 2)
		);
	}

	/**
	 * https://leekwars.com/forum/category-3/topic-11187 — une fonction définie sur une ligne
	 * passée tourne dans l'IA de cette ligne : ses appels doivent compter sur la ligne courante.
	 */
	@Test
	public void testSessionReplCallsPreviousFunction() throws Exception {
		section("Console session (REPL) : fonction d'une ligne passée");
		var separate = replCosts("var f = x => x ** 2", "f(42)");
		var together = replCosts("var f = x => x ** 2 f(42)");
		assertTrue(separate[1] > 0, "appeler une fonction d'une ligne passée a un coût");
		assertEquals(together[0] - separate[0], separate[1], "même coût que sur la même ligne");
		var c = replCosts("var f = x => x ** 2", "f(42)", "f(42)", "var y = 1");
		assertEquals(c[1], c[2], "le compteur d'une ligne passée repart de zéro à chaque ligne");
		assertEquals(replCosts("var y = 1")[0], c[3], "une ligne qui n'appelle rien ne paie pas les appels d'avant");
	}

	/**
	 * #5226 : une variable de session garde son type déclaré d'une ligne à l'autre. Relue en `any`,
	 * `BigInteger a = 1` puis `a << 64` décalait un long 64 bits et rendait 1.
	 */
	@Test
	public void testSessionKeepsDeclaredType() throws Exception {
		section("Console session (REPL) : type déclaré");
		runReplResults(
			new String[] { "BigInteger a = 1", null },
			new String[] { "a << 64", "18446744073709551616" },
			new String[] { "a <<= 3", "8" },
			new String[] { "[a & 1, ~a, a \\ 2]", "[0, -9, 4]" },
			new String[] { "a = 5", "5" },
			new String[] { "a << 64 == 92233720368547758080L", "true" },
			new String[] { "integer i = 5", null },
			new String[] { "i = 2.5", "2" },
			new String[] { "real r = 1", null },
			new String[] { "r / 4", "0.25" },
			new String[] { "var v = 1", null },
			new String[] { "v = 'str'", "\"str\"" }
		);
	}

	private void runReplResults(String[]... steps) throws Exception {
		var session = new Session(LeekScript.LATEST_VERSION, false);
		for (var step : steps) {
			var ai = LeekScript.compileSnippet(step[0], "AI", new Options(session));
			ai.maxOperations = 1_000_000;
			ai.maxRAM = 1_000_000;
			ai.init();
			ai.staticInit();
			var v = ai.runIA(session);
			if (step[1] != null) {
				assertEquals(step[1], ai.export(v), "résultat pour: " + step[0]);
			}
		}
	}

	private void runRepl(ReplStep... steps) throws Exception {
		var costs = replCosts(Arrays.stream(steps).map(step -> step.code).toArray(String[]::new));
		for (int i = 0; i < steps.length; i++) {
			System.out.println("[REPL] " + steps[i].code + " → " + costs[i] + " ops (attendu " + steps[i].expectedOps + ")");
			assertEquals(steps[i].expectedOps, costs[i], "ops pour: " + steps[i].code);
		}
	}

	/** Coût de chaque ligne jouée dans une même session, tel que la console l'affiche. */
	private static long[] replCosts(String... lines) throws Exception {
		var session = new Session(LeekScript.LATEST_VERSION, false);
		var costs = new long[lines.length];
		for (int i = 0; i < lines.length; i++) {
			var ai = LeekScript.compileSnippet(lines[i], "AI", new Options(session));
			ai.maxOperations = 1_000_000;
			ai.maxRAM = 1_000_000;
			// Départ pris après la construction de l'IA, qui facture déjà quelques ops.
			long before = ai.operations();
			ai.init();
			ai.staticInit();
			ai.runIA(session);
			costs[i] = session.lineOperations(ai, before);
		}
		return costs;
	}

}
