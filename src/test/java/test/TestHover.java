package test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.Options;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

/**
 * Survol (hover) de l'éditeur.
 *
 * Le survol ne tourne pas que sur des IA saines : `GeneratorAPI.ensureParsed` se
 * contente de `hasBeenParsed()`, et une IA dont l'analyse a échoué (erreur, ou
 * AI_TIMEOUT sur une grosse IA) reste en cache côté démon avec ses tokens posés
 * mais aucune expression typée. Le survol doit tenir dans cet état — pas planter
 * le thread websocket du démon (#3968).
 */
public class TestHover {

	private static int id = 990000;

	/** Reproduit l'état « tokens posés, jamais analysé » : readCode() sans analyze(). */
	private static void hoverEverywhere(String name, String code) throws Exception {
		var ai = ++id;
		var file = new AIFile(name, code, System.currentTimeMillis(), 4, ai, false);
		file.setJavaClass("AI_" + ai);
		file.setRootClass("AI");
		file.setId(ai);
		var wc = new WordCompiler(file, file.getVersion(), new Options());
		var main = new MainLeekBlock(new IACompiler(), wc, file);
		main.setWordCompiler(wc);
		wc.readCode();

		var lines = code.split("\n", -1);
		for (int l = 1; l <= lines.length; l++) {
			for (int c = 1; c <= lines[l - 1].length() + 1; c++) {
				final int line = l, column = c;
				assertDoesNotThrow(() -> file.hover(line, column), name + " line " + line + " column " + column);
			}
		}
	}

	@Test
	public void hoverOnUnanalyzedCode() throws Exception {
		// Le cas de #3968 : l'objet est un accès indexé, dont le type n'existe qu'après analyse.
		hoverEverywhere("array_access_field", "var tab = [[1]]\ntab[0].foo\n");
		hoverEverywhere("array_access_chain", "var a = [1]\na[0].b.c\n");
		hoverEverywhere("call_field", "var x = getLeek()\nx.life\n");
		hoverEverywhere("class_static", "class A { static var s = 1 }\nA.s\n");
		hoverEverywhere("this_field", "class A {\n  var f = 1\n  fn m() { return this.f }\n}\n");
		hoverEverywhere("map_access_field", "var m = [:]\nm[\"k\"].x\n");
		hoverEverywhere("slice_field", "var a = [1, 2, 3]\na[0:2].length\n");
	}
}
