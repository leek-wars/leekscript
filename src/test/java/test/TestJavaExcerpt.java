package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import leekscript.compiler.JavaCompiler;

/**
 * Extrait de Java généré joint aux erreurs COMPILE_JAVA : sans lui, ces erreurs ne
 * sont pas diagnosticables après coup (cf. JavaCompiler.javaExcerpt).
 */
public class TestJavaExcerpt {

	private static String code(int lines) {
		var sb = new StringBuilder();
		for (int i = 1; i <= lines; i++) sb.append("ligne").append(i).append('\n');
		return sb.toString();
	}

	@Test
	public void marqueLaLigneFautive() {
		var excerpt = JavaCompiler.javaExcerpt(code(100), 50);
		assertTrue(excerpt.contains("> 50: ligne50"), excerpt);
		assertTrue(excerpt.contains("  49: ligne49"), excerpt);
		assertTrue(excerpt.contains("  62: ligne62"), excerpt);
		// Fenêtre bornée : rien au-delà du contexte
		assertTrue(!excerpt.contains("ligne37"), excerpt);
		assertTrue(!excerpt.contains("ligne63"), excerpt);
	}

	@Test
	public void bornesDuFichier() {
		assertTrue(JavaCompiler.javaExcerpt(code(5), 1).contains("> 1: ligne1"));
		assertTrue(JavaCompiler.javaExcerpt(code(5), 5).contains("> 5: ligne5"));
		// Ligne hors fichier ou inconnue (javac non parsé) : pas d'extrait plutôt qu'un crash
		assertNull(JavaCompiler.javaExcerpt(code(5), 99));
		assertNull(JavaCompiler.javaExcerpt(code(5), -1));
		assertNull(JavaCompiler.javaExcerpt(null, 3));
	}

	@Test
	public void tronqueLesLignesEnormes() {
		// Le Java généré tient des expressions sur une seule ligne : elles peuvent être
		// gigantesques et noieraient error.info.
		var longue = "x".repeat(10000);
		var excerpt = JavaCompiler.javaExcerpt("a\n" + longue + "\nb\n", 2);
		assertTrue(excerpt.contains("[…]"), "la ligne doit être tronquée");
		assertTrue(excerpt.length() < 6000, "taille = " + excerpt.length());
	}

	@Test
	public void numerotationCoherente() {
		var excerpt = JavaCompiler.javaExcerpt(code(30), 3);
		var first = excerpt.split("\n")[1];
		assertEquals("  1: ligne1", first);
	}
}
