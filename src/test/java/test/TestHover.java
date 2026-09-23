package test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.Options;
import leekscript.compiler.exceptions.LeekCompilerException;

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

	private static AIFile file(String name, String code) {
		var ai = ++id;
		var file = new AIFile(name, code, System.currentTimeMillis(), 4, ai, false);
		file.setJavaClass("AI_" + ai);
		file.setRootClass("AI");
		file.setId(ai);
		return file;
	}

	private static void hoverEverywhere(String name, String code, AIFile file) {
		var lines = code.split("\n", -1);
		for (int l = 1; l <= lines.length; l++) {
			for (int c = 1; c <= lines[l - 1].length() + 1; c++) {
				final int line = l, column = c;
				assertDoesNotThrow(() -> file.hover(line, column), name + " line " + line + " column " + column);
			}
		}
	}

	/**
	 * Reproduit l'état « tokens posés, jamais analysé ». `merge()` est le seul chemin
	 * public qui fasse exactement ça : il arme le chrono du timeout — sans quoi
	 * `WordCompiler.isInterrupted()` serait vrai dès sa première prise et un code un
	 * peu long lèverait AI_TIMEOUT — puis appelle `readCode()` sans `analyze()`.
	 */
	private static void hoverUnanalyzed(String name, String code) throws Exception {
		var file = file(name, code);
		new IACompiler().merge(file);
		hoverEverywhere(name, code, file);
	}

	/**
	 * L'IA est bel et bien passée par l'analyse — les expressions atteintes sont typées et
	 * les classes résolues. Le code n'a pas à compiler pour autant : c'est justement l'état
	 * de production, où l'analyse échoue et laisse l'arbre à moitié renseigné.
	 */
	private static void hoverAnalyzed(String name, String code) throws Exception {
		var file = file(name, code);
		try {
			file.compile(new Options());
		} catch (LeekCompilerException e) {
			// Attendu : un accès inachevé n'est pas du code valide.
		}
		hoverEverywhere(name, code, file);
	}

	@Test
	public void hoverOnUnanalyzedCode() throws Exception {
		// Le cas de #3968 : l'objet est un accès indexé, dont le type n'existe qu'après analyse.
		hoverUnanalyzed("array_access_field", "var tab = [[1]]\ntab[0].foo\n");
		hoverUnanalyzed("array_access_chain", "var a = [1]\na[0].b.c\n");
		hoverUnanalyzed("call_field", "var x = getLeek()\nx.life\n");
		hoverUnanalyzed("class_static", "class A { public static m() { return 1 } }\nA.m()\n");
		hoverUnanalyzed("this_field", "class A {\n  var f = 1\n  fn m() { return this.f }\n}\n");
		hoverUnanalyzed("map_access_field", "var m = [:]\nm[\"k\"].x\n");
		hoverUnanalyzed("slice_field", "var a = [1, 2, 3]\na[0:2].length\n");
	}

	/**
	 * Un accès dont le champ manque encore : c'est l'état de TOUTE frappe de `.`, et le
	 * parseur construit le nœud tel quel (`addObjectAccess(dot, null)`). Le survol tombe
	 * dessus aussi bien sur une IA analysée que non.
	 */
	@Test
	public void hoverOnIncompleteFieldAccess() throws Exception {
		hoverUnanalyzed("dangling_dot_class", "class A { public static m() { return 1 } }\nA.\n");
		hoverUnanalyzed("dangling_dot_var", "var x = [1]\nx.\n");
		hoverAnalyzed("dangling_dot_class_analyzed", "class A { public static m() { return 1 } }\nA.\n");
		hoverAnalyzed("dangling_dot_this_analyzed", "class A {\n  public f = 1\n  public m() { return this. }\n}\n");
	}
}
