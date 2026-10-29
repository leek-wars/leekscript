package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import leekscript.compiler.AIFile;

/** Nom de la classe compilée par défaut : tiré de l'identité du fichier, pas de son seul id sur 28 bits. */
public class TestAIFile {

	private static AIFile file(int owner, int id, String path) {
		return new AIFile(path, "", 1, 4, null, owner, id, false);
	}

	@Test
	public void deuxProprietairesAvecLeMemeIdOntDeuxClasses() {
		assertNotEquals(file(1, 42, "ia").getJavaClass(), file(2, 42, "ia").getJavaClass());
		assertNotEquals(file(1, 42, "a/ia").getJavaClass(), file(1, 42, "b/ia").getJavaClass());
	}

	@Test
	public void unMemeFichierGardeSaClasse() {
		assertEquals(file(1, 42, "ia").getJavaClass(), file(1, 42, "ia").getJavaClass());
	}

	@Test
	public void unNomDeClasseJavaValide() {
		var name = file(Integer.MAX_VALUE, -1, "dossier/é ü/ia.leek").getJavaClass();
		assertTrue(name.matches("AI_[0-9a-z]+"), name);
	}

	@Test
	public void unNomChoisiParLAppelantEstGarde() {
		var f = file(1, 42, "ia");
		f.setJavaClass("AI_42");
		assertEquals("AI_42", f.getJavaClass());
	}
}
