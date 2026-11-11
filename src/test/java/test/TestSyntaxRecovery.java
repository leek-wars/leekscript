package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;

/**
 * Une expression incomplète (`var a = }`) ne doit pas avaler l'accolade qui ferme le bloc :
 * sinon tout ce qui suit est décalé d'une accolade et l'éditeur souligne la fin du fichier.
 */
public class TestSyntaxRecovery {

	private static final AtomicInteger ids = new AtomicInteger(2_430_000);

	private static List<Error> errors(String code) {
		int id = ids.incrementAndGet();
		var file = new AIFile("recovery", code, System.currentTimeMillis(), 4, id, false);
		file.setJavaClass("AI_" + id);
		file.setRootClass("AI");
		file.setId(id);
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
}
