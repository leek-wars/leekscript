package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.compiler.IACompiler;

/**
 * Une expression incomplète (`var a = }`) ne doit pas avaler l'accolade qui ferme le bloc :
 * sinon tout ce qui suit est décalé d'une accolade et l'éditeur souligne la fin du fichier.
 */
public class TestSyntaxRecovery {

	private static List<Error> errors(String code) {
		var file = TestCommon.aiFile("recovery", code);
		new IACompiler().analyze(file);
		return file.getErrors().stream().map(e -> e.error).toList();
	}

	@Test
	public void incompleteExpressionKeepsTheClosingBrace() {
		// Valeur oubliée en cours de frappe, le `}` du bloc juste dessous
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION), errors("if (true) {\n\tvar a = \n}\nvar c = 3\nreturn c"));
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION), errors("if (true) {\n\tvar a = 1 +\n}\nreturn 1"));
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION), errors("while (true) {\n\tvar a = 1 + }\nreturn 2"));
	}

	@Test
	public void incompleteExpressionBeforeSemicolonOrParenthesis() {
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION), errors("var a = ;\nvar b = 2\nreturn b"));
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION, Error.CLOSING_PARENTHESIS_EXPECTED), errors("var a = (1 + }\nreturn 2"));
	}

	@Test
	public void strayClosingParenthesisIsConsumed() {
		// Aucune instruction ne reprend un `)` en trop : non consommé, l'analyse tournerait sur place
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION), errors("debug(1))\nreturn 1"));
		assertEquals(List.of(Error.UNCOMPLETE_EXPRESSION), errors("var a = )\nreturn 1"));
	}

	/**
	 * Un token jamais consommé fait tourner l'analyse sur place jusqu'à la limite de 10 000
	 * erreurs : chaque code cassé doit en produire une poignée, dans toutes les versions.
	 */
	@Test
	public void brokenCodeDoesNotSpinTheParser() {
		String[] codes = { "debug(1))", "var a = )", "f(1, }", "[1, }", "{a: }", "var x = [ }", "for (var i = ; i < 3; i++) {}",
			"for (var i = 0; i < ; i++) {}", "while ( ) {}", "if () {}", "return [1: }", "class A { x = }", "class A { x = ; y = 2 }",
			"function f(a = ) {}", "var a = b ? : 2", "x = }}}}", "(((}", "var a = <1, }", "for (x in ) {}", "for (k : v in ) {}",
			"do { } while ( )", "var f = (a) -> }", "switch (x) { case : }", "var a = 1 +; var b = ; var c = ) ; var d = }",
			"return ;;;", "var s = \"${ }\"", "include(}", "global g = }", "var a = [1, 2][}", "a.b.}", "new }", "var a = -}",
			"var a = !)", "var o = {x: 1, y: }", "var m = [1: 2, 3: ]", "var t = (1, }", "a ? b var x = 1", "]", ")))" };
		for (var code : codes) {
			for (int version = 1; version <= 5; version++) {
				// analyze() repart de la dernière version : seul le pragma la fixe
				var file = TestCommon.aiFile("recovery", "// @version:" + version + "\n" + code);
				new IACompiler().analyze(file);
				int count = file.getErrors().size();
				assertTrue(count < 50, "v" + version + " « " + code + " » : " + count + " erreurs");
			}
		}
	}
}
