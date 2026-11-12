package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import leekscript.compiler.AICode;
import leekscript.compiler.JavaCompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

/**
 * v5 : un programme qui importe des modules se compile en une classe Java par module, à côté de
 * celle de l'IA. Ces tests tiennent la forme du Java généré (qui porte quoi), les plafonds JVM par
 * module, et ce que le runtime retrouvait par la classe de l'IA : trace d'erreur, copie d'objet,
 * classe d'un objet, méthode prise comme valeur.
 *
 * @Isolated : LeekScript.setFileSystem est un singleton statique.
 */
@Isolated
public class TestModuleClasses {

	@TempDir Path tmpRoot;
	private RamFileSystem fs;
	private static final AtomicLong UNIQUE = new AtomicLong();
	private String uniqueId;

	@BeforeEach
	void setUp() {
		uniqueId = Long.toHexString(System.nanoTime()) + "_" + UNIQUE.incrementAndGet();
		fs = new RamFileSystem(tmpRoot, LeekScript.MAX_VERSION);
		LeekScript.setFileSystem(fs);
	}

	@AfterEach
	void tearDown() {
		LeekScript.resetFileSystem();
	}

	// =========================================================================
	// Forme du Java généré
	// =========================================================================

	@Test
	public void oneJavaClassPerModule() throws Exception {
		write("geo.leek", "export function area(w, h) { return w * h }\nexport class Vec { public integer x = 1 }");
		write("fmt.leek", "import { area } from \"geo\"\nexport function show(w, h) { return \"aire \" + area(w, h) }");
		String main = writeMain("import { show } from \"fmt\"\nimport { Vec } from \"geo\"\nreturn show(2, 3) + \" \" + new Vec().x");
		assertEquals("aire 6 1", run(main));
		var code = codeOf(main);
		assertEquals(2, code.getModules().size());
		var geo = module(code, "_geo_");
		var fmt = module(code, "_fmt_");
		// Chaque module porte ses déclarations, l'IA n'en porte aucune
		// (dans sa classe, un symbole de module n'a plus besoin de préfixe)
		assertTrue(geo.javaCode().contains(" f_area(") && geo.javaCode().contains("class u_Vec "), geo.javaCode());
		assertTrue(fmt.javaCode().contains(" f_show(") && fmt.javaCode().contains(".f_area("), fmt.javaCode());
		assertFalse(fmt.javaCode().contains("class u_"), fmt.javaCode());
		assertFalse(code.getJavaCode().contains("class u_") || code.getJavaCode().contains(" f_area("), code.getJavaCode());
		// La classe d'un module ne s'appelle pas d'après son rang d'import
		assertTrue(geo.className().matches("AI_\\w+_geo_[0-9a-z]+"), geo.className());
	}

	// Un programme sans import reste en une seule classe Java, comme en v1-4.
	@Test
	public void programWithoutImportKeepsOneClass() throws Exception {
		String main = writeMain("class A { public integer x = 2 }\nfunction f() { return new A().x }\nreturn f()");
		assertEquals("2", run(main));
		assertEquals(0, codeOf(main).getModules().size());
	}

	// Les 65 535 constantes d'une classe Java se comptent par module : deux modules qui en ont
	// 36 000 chacun en demandaient 72 000 dans la classe unique de l'IA (« too many constants »).
	@Test
	public void eachModuleHasItsOwnConstantPool() throws Exception {
		for (var name : new String[] { "big1", "big2" }) {
			var sb = new StringBuilder();
			for (int f = 0; f < 180; f++) {
				sb.append("function f").append(f).append("() { return [");
				for (int i = 0; i < 100; i++) sb.append(i > 0 ? ", " : "").append('"').append(name).append('_').append(f).append('_').append(i).append('"');
				sb.append("] }\n");
			}
			sb.append("export function last() { return f179()[99] }\n");
			write(name + ".leek", sb.toString());
		}
		String main = writeMain("import * as A from \"big1\"\nimport * as B from \"big2\"\nreturn A.last() + \" \" + B.last()");
		assertEquals("big1_179_99 big2_179_99", run(main));
	}

	// =========================================================================
	// Trace d'erreur : la ligne d'un cadre se lit par sa classe Java
	// =========================================================================

	@Test
	public void errorTraceCrossesModules() throws Exception {
		write("deep.leek", "export class Loop {\n\tspin() {\n\t\tvar i = 0\n\t\twhile (true) { i++ }\n\t}\n}");
		write("mid.leek", "import { Loop } from \"deep\"\n\nexport function go() {\n\treturn new Loop().spin()\n}");
		String main = writeMain("import { go } from \"mid\"\n\n\nreturn go()");
		AI ai = load(main);
		String trace = null;
		try {
			ai.runIA();
		} catch (LeekRunException e) {
			trace = ai.getErrorMessage(e);
		}
		assertEquals("\t▶ AI deep, line 4\n\t▶ AI mid, line 4\n\t▶ AI " + main + ", line 4\n", trace);
	}

	// =========================================================================
	// Classes utilisateur d'un module
	// =========================================================================

	@Test
	public void objectsOfModuleClasses() throws Exception {
		write("shapes.leek", String.join("\n",
			"export class Shape {",
			"	public string name",
			"	public Array<integer> points = []",
			"	static integer count = 0",
			"	constructor(string name) { this.name = name; Shape.count++ }",
			"	area() { return 0 }",
			"	label() { return name + \":\" + area() }",
			"	static make(string name) { return new Shape(name) }",
			"}"));
		write("square.leek", String.join("\n",
			"import { Shape } from \"shapes\"",
			"export class Square extends Shape {",
			"	public integer side",
			"	constructor(integer side) { super(\"carré\"); this.side = side }",
			"	area() { return side * side }",
			"}",
			"export function twin(Square s) { var c = clone(s); c.side = c.side + 1; return c }"));
		String main = writeMain(String.join("\n",
			"import { Shape } from \"shapes\"",
			"import { Square, twin } from \"square\"",
			"var s = new Square(3)",
			"var t = twin(s)",
			"var label = s.label",
			"var made = Shape.make(\"point\")",
			"return [s.label(), t.area(), s.area(), s instanceof Shape, made instanceof Square, t.class == Square,",
			"	string(t.class), label(), Shape.count, string(made), made.class.name]"));
		assertEquals("[\"carré:9\", 16, 9, true, false, true, \"<class Square>\", \"carré:9\", 2, \"Shape {name: \"point\", points: []}\", \"Shape\"]", run(main));
	}

	// Une classe de l'entrée peut hériter d'une classe de module, et un module lire l'entrée.
	@Test
	public void entryAndModuleClassesMix() throws Exception {
		String name = "Main_" + uniqueId;
		write("base.leek", "import { bonus } from \"" + name + "\"\nexport class Base { public integer v = 1; get() { return v + bonus() } }");
		String main = writeMain("import { Base } from \"base\"\nexport function bonus() { return 10 }\nclass Child extends Base { get() { return super.get() * 2 } }\nvar c = clone(new Child())\nreturn [c.get(), c instanceof Base, string(c)]");
		assertEquals("[22, true, \"Child {v: 1}\"]", run(main));
	}

	// =========================================================================
	// Ce que le Java généré range à côté des fonctions : utilitaires génériques, fonctions
	// système et utilisateur prises comme valeurs, temporaires de match, globales
	// =========================================================================

	@Test
	public void moduleCodeUsesEveryGeneratedHelper() throws Exception {
		write("kit.leek", String.join("\n",
			"export global calls = 0",
			"export function twice(x) { return x * 2 }",
			"export function apply(f, x) { calls++; return f(x) }",
			"export function pick(any v) { return match (v) { 1 => \"un\", 2 => \"deux\", _ => \"autre\" } }",
			"export function total(any xs) { var s = 0; for (var x in xs) { s += x } return s + Array.count(xs) }",
			"export function funs() { return [twice, pick] }"));
		String main = writeMain(String.join("\n",
			"import { calls, twice, apply, pick, total, funs } from \"kit\"",
			"var fs = funs()",
			"var r = [apply(twice, 4), apply(x => x + 1, 1), pick(2), pick(7), total([1, 2, 3]), fs[0](5), fs[1](1), twice]",
			"return [r, calls]"));
		assertEquals("[[8, 2, \"deux\", \"autre\", 9, 10, \"un\", #Function twice], 2]", run(main));
	}

	// Les globales d'un module vivent d'un tour à l'autre, dans la classe du module.
	@Test
	public void moduleGlobalsPersistAcrossTurns() throws Exception {
		write("state.leek", "export global turns = 0\nexport function tick() { turns++; return turns }");
		String main = writeMain("import { tick } from \"state\"\nreturn tick()");
		AI ai = load(main);
		assertEquals("1", ai.string(ai.runIA()));
		assertEquals("2", ai.string(ai.runIA()));
	}

	// Rechargée du cache disque, l'IA retrouve les classes de ses modules.
	@Test
	public void modulesReloadFromDiskCache() throws Exception {
		write("lib.leek", "export class P { public integer x = 4 }\nexport function f() { return clone(new P()).x }");
		String main = writeMain("import { f } from \"lib\"\nreturn f()");
		var file = fs.getRoot(0).resolve(main);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		assertEquals("4", run(main));
		// Comme un autre processus : le cache des classes en mémoire ne sert pas
		var cache = JavaCompiler.class.getDeclaredField("aiCache");
		cache.setAccessible(true);
		((java.util.Map<?, ?>) cache.get(null)).remove(file.getJavaClass());
		assertEquals("4", run(main));
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private static AICode.ModuleCode module(AICode code, String part) {
		return code.getModules().stream().filter(m -> m.className().contains(part)).findFirst().orElseThrow();
	}

	private Path write(String relative, String content) throws IOException {
		var p = tmpRoot.resolve(relative);
		Files.createDirectories(p.getParent());
		Files.writeString(p, content);
		return p;
	}

	private String writeMain(String content) throws IOException {
		String name = "Main_" + uniqueId;
		write(name + ".leek", content);
		return name;
	}

	private AICode codeOf(String name) throws Exception {
		return fs.getRoot(0).resolve(name).getCompiledCode();
	}

	private AI load(String name) throws Exception {
		var file = fs.getRoot(0).resolve(name);
		file.setJavaClass("AI_" + file.getId());
		file.setRootClass("AI");
		AI ai = JavaCompiler.compile(file, new Options(LeekScript.MAX_VERSION, false, true, true, null, true));
		ai.init();
		ai.staticInit();
		ai.resetCounter();
		return ai;
	}

	private String run(String name) throws Exception {
		AI ai = load(name);
		return ai.string(ai.runIA());
	}
}
