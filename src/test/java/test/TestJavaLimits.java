package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.Options;

/**
 * Code du joueur qui dépasse une limite de la JVM : javac le refuse, et l'erreur doit
 * être celle de l'IA (CODE_TOO_LARGE, affichée au joueur) et non COMPILE_JAVA, qui
 * est remontée comme une erreur du serveur.
 */
public class TestJavaLimits {

	/** `function f(integer p0, …) { return p0 } return f(0, …)` */
	private static String functionWithParameters(int count) {
		var parameters = new StringBuilder();
		var arguments = new StringBuilder();
		for (int i = 0; i < count; i++) {
			if (i > 0) { parameters.append(", "); arguments.append(", "); }
			parameters.append("integer p").append(i);
			arguments.append(i);
		}
		return "function f(" + parameters + ") { return p0 } return f(" + arguments + ")";
	}

	@Test
	public void tropDeParametres() {
		// 128 integer = 256 emplacements, plus `this` : au-delà des 255 de la JVM
		var e = assertThrows(LeekScriptException.class, () -> LeekScript.compileSnippet(functionWithParameters(128), "AI", new Options(4, false, false, true, null, true)));
		assertEquals(Error.CODE_TOO_LARGE, e.getType());
	}

	@Test
	public void parametresALaLimite() throws Exception {
		var ai = LeekScript.compileSnippet(functionWithParameters(127), "AI", new Options(4, false, false, true, null, true));
		ai.init();
		ai.staticInit();
		assertEquals("0", ai.string(ai.runIA()));
	}
}
