package test;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import leekscript.common.Error;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;

/**
 * Coût en RAM des conteneurs : un coût de base par conteneur, même vide, plus ses éléments.
 * getUsedRAM() rend des octets (une case = 8 octets).
 */
@ExtendWith(SummaryExtension.class)
public class TestRamCost extends TestCommon {

	@Test
	public void run() throws Exception {

		section("Conteneurs vides (v4)");
		code_v4_("var r = getUsedRAM() var a = [] return getUsedRAM() - r").equals("88");
		code_v4_("var r = getUsedRAM() var a = [:] return getUsedRAM() - r").equals("120");
		code_v4_("var r = getUsedRAM() var a = <> return getUsedRAM() - r").equals("144");
		code_v4_("var r = getUsedRAM() var a = {} return getUsedRAM() - r").equals("144");

		section("Conteneurs remplis (v4)");
		code_v4_("var r = getUsedRAM() var a = [1, 2, 3] return getUsedRAM() - r").equals("112");
		code_v4("var a = [] var r = getUsedRAM() push(a, 1) return getUsedRAM() - r").equals("8");
		code_v4_("var r = getUsedRAM() var a = [1: 2] return getUsedRAM() - r").equals("136");
		// Une clé ou une valeur répétée ne compte qu'une fois
		code_v4_("var k = 1 var r = getUsedRAM() var a = [k: 2, k: 3] return getUsedRAM() - r").equals("136");
		code_v4_("var r = getUsedRAM() var a = <1, 1, 1> return getUsedRAM() - r").equals("152");

		section("Instances de classes (v4)");
		code_v4_("class A {} var r = getUsedRAM() var a = new A() return getUsedRAM() - r").equals("72");
		code_v4_("class A { x } var r = getUsedRAM() var a = new A() return getUsedRAM() - r").equals("88");
		// Le coût de base n'est compté qu'une fois, par la classe racine
		code_v4_("class A { x } class B extends A { y } var r = getUsedRAM() var b = new B() return getUsedRAM() - r").equals("104");
		code_v4_("class A { x } var a = new A() var r = getUsedRAM() var b = clone(a) return getUsedRAM() - r").equals("88");

		section("Tableaux v1-v3");
		code_v1_3("var r = getUsedRAM() var a = [] return getUsedRAM() - r").equals("48");
		code_v1_3("var r = getUsedRAM() var a = [1, 2, 3] return getUsedRAM() - r").equals("336");
		code_v1_3("var a = [] var r = getUsedRAM() push(a, 1) return getUsedRAM() - r").equals("96");
		code_v1_3("var a = [1, 2] var r = getUsedRAM() removeElement(a, 1) return r - getUsedRAM()").equals("96");
		code_v1_3("var a = [1, 2, 3] var r = getUsedRAM() remove(a, 0) return r - getUsedRAM()").equals("96");
		code_v1_3("var a = ['x': 1] var r = getUsedRAM() a['y'] = 2 a['x'] = 3 return getUsedRAM() - r").equals("96");

		section("Tables de méthodes des classes");
		// A.methods est compté à sa lecture, comme A.fields (rien à la déclaration : cf. classesDeclaredForFree) :
		// le prix d'un tableau de ses noms, comme [1, 2]
		code_v4_("class A { a() {} b() {} } var r = getUsedRAM() var m = A.methods return getUsedRAM() - r").equals("104");
		code_v2_3("class A { a() {} b() {} } var r = getUsedRAM() var m = A.methods return getUsedRAM() - r").equals("240");
		code_v2_3("class A { a() {} } var m = A.methods var r = getUsedRAM() push(m, 1) return getUsedRAM() - r").equals("96");
		code_v2_4("class A { a() {} b() {} } class B extends A { c() {} } return B.methods").equals("[\"a\", \"b\", \"c\"]");

		section("Limite de RAM");
		// Des conteneurs vides atteignent la limite, ils ne passent plus gratuitement
		code_v4("var a = [] for (var i = 0; i < 1000000; ++i) push(a, [])").max_ram(1_000_000).error(Error.OUT_OF_MEMORY);
		code_v4("var a = [] for (var i = 0; i < 1000000; ++i) push(a, {})").max_ram(1_000_000).error(Error.OUT_OF_MEMORY);
		code_v4("class A {} var a = [] for (var i = 0; i < 1000000; ++i) push(a, new A())").max_ram(1_000_000).error(Error.OUT_OF_MEMORY);
		code_v1_3("var a = [] for (var i = 0; i < 1000000; ++i) push(a, i)").max_ram(1_000_000).error(Error.OUT_OF_MEMORY);
	}

	/**
	 * Déclarer des classes ne compte rien : la RAM de l'IA est la même après sa compilation (qui
	 * compte déjà la taille du code généré) et au début de son code, une fois les classes définies.
	 */
	@Test
	public void classesDeclaredForFree() throws Exception {
		for (int version : new int[] { 3, 4 }) {
			var ai = LeekScript.compileSnippet("class A { a() {} b() {} } class B extends A { c() {} } return 1", "AI", new Options(version, false, false, true, null, true));
			long compiled = ai.getUsedRAM();
			ai.init();
			ai.staticInit();
			assertEquals(compiled, ai.getUsedRAM(), "v" + version);
		}
	}

	/** Le code compilé compte deux octets par octet de bytecode ; sa taille est notée avec la signature du cache. */
	@Test
	public void compiledCodeCharged() throws Exception {
		var ai = LeekScript.compileSnippet("var a = [1, 2, 3] return a", "AI", new Options(4, false, true, true, null, true));
		var signature = Path.of(JavaCompiler.IA_PATH, "AI_" + ai.getId() + ".sig");
		assertTrue(ai.getBytecodeSize() > 0);
		assertEquals(ai.getBytecodeSize() * 2 / 8, ai.getCodeRAM());
		assertEquals(String.valueOf(ai.getBytecodeSize()), Files.readString(signature).split("\n")[1]);
	}
}
