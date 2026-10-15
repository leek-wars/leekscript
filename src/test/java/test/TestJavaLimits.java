package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.Options;
import leekscript.runner.AI;

/**
 * Code du joueur qui dépasse une limite de la JVM : javac le refuse, et l'erreur doit
 * être celle de l'IA (CODE_TOO_LARGE, affichée au joueur) et non COMPILE_JAVA, qui
 * est remontée comme une erreur du serveur.
 */
public class TestJavaLimits {

	/** Compile `function f(integer p0, …) { return p0 } return f(0, …)` en LeekScript 4. */
	private static AI compileFunctionWithParameters(int count) throws Exception {
		var parameters = IntStream.range(0, count).mapToObj(i -> "integer p" + i).collect(Collectors.joining(", "));
		var arguments = IntStream.range(0, count).mapToObj(String::valueOf).collect(Collectors.joining(", "));
		var code = "function f(" + parameters + ") { return p0 } return f(" + arguments + ")";
		return LeekScript.compileSnippet(code, "AI", new Options(4, false, false, true, null, true));
	}

	@Test
	public void tropDeParametres() {
		// 128 integer = 256 emplacements, plus `this` : au-delà des 255 de la JVM
		var e = assertThrows(LeekScriptException.class, () -> compileFunctionWithParameters(128));
		assertEquals(Error.CODE_TOO_LARGE, e.getType());
	}

	@Test
	public void parametresALaLimite() throws Exception {
		var ai = compileFunctionWithParameters(127);
		ai.init();
		ai.staticInit();
		assertEquals("0", ai.string(ai.runIA()));
	}
}
