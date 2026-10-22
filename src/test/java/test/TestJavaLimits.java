package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.Options;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekTernaire;
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

	/**
	 * Beaucoup de classes et de méthodes : leur enregistrement tenait dans le seul constructeur de l'IA,
	 * qui dépassait les 64 Ko d'une méthode Java vers 3 000 méthodes. Une méthode par classe.
	 */
	@Test
	public void milliersDeMethodes() throws Exception {
		var code = new StringBuilder();
		for (int c = 0; c < 150; c++) {
			code.append("class C").append(c).append(" {");
			for (int m = 0; m < 20; m++) {
				code.append(" public static m").append(m).append("(x) { return x + ").append(m).append(" }");
			}
			code.append(" } ");
		}
		code.append("return C149.m19(1)");
		var ai = LeekScript.compileSnippet(code.toString(), "AI", new Options(4, false, false, true, null, true));
		ai.init();
		ai.staticInit();
		assertEquals("20", ai.string(ai.runIA()));
	}

	@Test
	public void parametresALaLimite() throws Exception {
		var ai = compileFunctionWithParameters(127);
		ai.init();
		ai.staticInit();
		assertEquals("0", ai.string(ai.runIA()));
	}

	private static String run(String code, boolean strict) throws Exception {
		var ai = LeekScript.compileSnippet(code, "AI", new Options(4, strict, false, true, null, true));
		ai.init();
		ai.staticInit();
		return ai.string(ai.runIA());
	}

	/**
	 * Ternaires imbriqués : l'analyse reparcourait deux fois chaque branche (2^n), et javac
	 * réévaluait chaque ternaire écrit en argument d'ops() pour chacune de ses surcharges (4^n).
	 * 15 niveaux prenaient déjà plus de 20 s.
	 */
	@Test
	@Timeout(value = 120, threadMode = ThreadMode.SEPARATE_THREAD)
	public void ternairesImbriques() throws Exception {
		var nested = "x ? 1 : ".repeat(LeekTernaire.MAX_DEPTH) + "2";
		assertEquals("2", run("var x = false return " + nested, false));
		assertEquals("1", run("var x = true return " + nested, false));
		assertEquals("2", run("var x = false return abs(" + nested + ")", false));
		assertEquals("[2]", run("var x = false return [" + nested + "]", false));
		assertEquals("2", run("boolean x = false integer y = " + nested + " return y", true));
		// Entre parenthèses, la forme la plus courante d'une imbrication
		assertEquals("2", run("var x = false return " + "x ? 1 : (".repeat(LeekTernaire.MAX_DEPTH - 1) + "x ? 1 : 2" + ")".repeat(LeekTernaire.MAX_DEPTH - 1), false));
		// Branches gardées en Object (union rétrécie) : pas de cast possible, ops() sans surcharge
		var chain = IntStream.rangeClosed(1, 40).mapToObj(i -> "x == " + i + " ? this.n : ").collect(Collectors.joining());
		assertEquals("12", run("class A { integer | real n = 12 m(x) { if (this.n instanceof Integer) { return " + chain + "0 } return -1 } } return new A().m(40)", true));
	}

	/** Au-delà de la limite : une erreur de l'IA, plus une compilation sans fin ou un javac planté. */
	@Test
	public void ternairesTropImbriques() throws Exception {
		var nested = "x ? 1 : ".repeat(LeekTernaire.MAX_DEPTH + 1) + "2";
		var e = assertThrows(LeekCompilerException.class, () -> run("var x = false return " + nested, false));
		assertEquals(Error.CODE_TOO_LARGE, e.getError());
		// Imbriqués dans la condition : jamais coûteux, pas limités
		var condition = "(".repeat(150) + "x" + " ? true : false)".repeat(150);
		assertEquals("false", run("var x = false return " + condition, false));
	}

	/** Un ternaire imbriqué en branche comptée garde son type Java (boxé, Object, big_integer…). */
	@Test
	public void ternaireImbriqueTypes() throws Exception {
		assertEquals("[null, 1.5, 3.5]", run("class A { real? f = null m(x, y) { return x ? this.f : (y ? 1.5 : 3.5 + 0) } } var a = new A() return [a.m(true, true), a.m(false, true), a.m(false, false)]", true));
		assertEquals("[12, 2, 13]", run("class A { integer | real n = 12 m(x, y) { if (this.n instanceof Integer) { return x ? this.n : (y ? 2 : this.n + 1) } return 0 } } var a = new A() return [a.m(true, true), a.m(false, true), a.m(false, false)]", true));
		assertEquals("[1, 2, 3]", run("var r = [] for (var c in [0, 1, 2]) { push(r, c == 0 ? 1L : c == 1 ? 2L : 3L + 0L) } return r", false));
		assertEquals("[\"a\", \"b\", \"cd\"]", run("var r = [] for (var c in [0, 1, 2]) { push(r, c == 0 ? 'a' : c == 1 ? 'b' : 'c' + 'd') } return r", false));
	}
}
