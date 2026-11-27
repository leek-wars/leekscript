package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import leekscript.runner.AI;

/**
 * Argument du mauvais type passé à une fonction système : l'appel rend sa valeur par défaut, et
 * l'exception qui le signale à l'enveloppe générique ne remplit pas de pile d'appels (sinon une
 * boucle sur un tel appel coûte des microsecondes par itération pour une ou deux opérations). Le
 * temps de ces boucles sur le code généré est gardé par le générateur (BenchFightCosts), dont les
 * tests tournent avec les options de la JVM du worker.
 */
@ExtendWith(SummaryExtension.class)
public class TestSystemFunctionArguments extends TestCommon {

	@Test
	public void wrongTypeSignalHasNoStackTrace() {
		assertEquals(0, assertThrows(ClassCastException.class, () -> AI.toLongOrNull("x")).getStackTrace().length);
		assertEquals(0, assertThrows(ClassCastException.class, () -> AI.toLongOrNull(true)).getStackTrace().length);
		assertEquals(0, assertThrows(ClassCastException.class, () -> AI.castArgument("x", Boolean.class)).getStackTrace().length);
		assertEquals(0, assertThrows(ClassCastException.class, () -> AI.castArgument(12L, String.class)).getStackTrace().length);
	}

	@Test
	public void rightTypeAndNullPass() {
		assertEquals(12L, AI.toLongOrNull(12L));
		assertEquals(12L, AI.toLongOrNull(12.9));
		assertNull(AI.toLongOrNull(null));
		assertSame("x", AI.castArgument("x", String.class));
		assertEquals(true, AI.castArgument(true, Boolean.class));
		assertNull(AI.castArgument(null, String.class));
	}

	@Test
	public void wrongTypeArgumentGivesTheDefaultValue() throws Exception {
		section("Ensemble");
		code_v1_4("return setContains('x', 1)").equals("false");
		code_v1_4("return setPut('x', 1)").equals("false");
		code_v1_4("return setSize('x')").equals("0");
		code_v1_4("return setRemove(12, 1)").equals("false");
		code_v1_4("return setContains(<1, 2>, 1)").equals("true");
		code("var f = setContains; return f('x', 1)").equals("false");
		code("var f = setContains; return f(<1>, 1)").equals("true");
		code("var f = setSize; return f([1, 2])").equals("0");

		section("Intervalle");
		code_v2_("return intervalMin('x')").equals("0.0");
		code_v2_("return intervalMax(12)").equals("0.0");
		code_v2_("return intervalSize('x')").equals("0.0");
		code("return intervalToArray('x')").equals("[]");
		code("return intervalMin([1..5])").equals("1");
		code("var f = intervalMin; return f('x')").equals("0");
		code("var f = intervalMin; return f([2..5])").equals("2");

		section("Tableau");
		code("var f = count; return f('x')").equals("0");
		code("var f = count; return f([1, 2])").equals("2");
		code("var f = push; return f('x', 1)").equals("null");
	}
}
