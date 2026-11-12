package test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tools.GenerateAIModule;

public class TestAIModule {

	// AIModule relaie chaque méthode non privée d'AI au code des modules v5 : une méthode ajoutée
	// à AI doit y paraître, sinon un module qui l'appelle ne compile plus.
	@Test
	public void mirrorOfAIIsUpToDate() throws Exception {
		var source = Path.of("src/main/java/leekscript/runner/AIModule.java");
		// (pas d'assertEquals : l'écart afficherait les deux fichiers entiers)
		assertTrue(GenerateAIModule.generate().equals(Files.readString(source)),
			"AIModule.java n'est plus à jour : relancer `java tools.GenerateAIModule " + source + "`");
	}
}
