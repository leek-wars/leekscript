package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import leekscript.compiler.Complete;
import leekscript.compiler.Options;

/**
 * Complétion de l'éditeur après un point (`objet.`).
 */
public class TestComplete {

	private static Complete completeAt(String code, int line, int column) throws Exception {
		var file = TestCommon.aiFile("complete", code);
		file.compile(new Options());
		return file.complete(line, column);
	}

	private static long count(Complete complete, String name) {
		return complete.completions.stream().filter(c -> c.name.equals(name)).count();
	}

	@Test
	public void overriddenMethodIsProposedOnce() throws Exception {
		var code = """
			class A {
				public toto() {}
				public onlyA() {}
			}
			class B extends A {
				public toto() {}
			}
			class C extends B {
				public toto() {}
			}
			C c = new C();
			c.toto();
			""";
		// Le point de `c.toto()`, ligne 12.
		var complete = completeAt(code, 12, 1);
		assertNotNull(complete);
		assertEquals(1, count(complete, "toto"), "une méthode redéfinie à chaque niveau ne sort qu'une fois");
		assertEquals(1, count(complete, "onlyA"), "une méthode héritée reste proposée");
	}

	@Test
	public void overloadedMethodIsProposedOnce() throws Exception {
		var code = """
			class A {
				public f() {}
				public f(x) {}
			}
			A a = new A();
			a.f();
			""";
		var complete = completeAt(code, 6, 1);
		assertNotNull(complete);
		assertEquals(1, count(complete, "f"), "les surcharges d'une méthode ne font qu'une proposition");
	}

	@Test
	public void redeclaredFieldIsProposedOnce() throws Exception {
		var code = """
			class A {
				public x = 1
			}
			class B extends A {
				public x = 2
			}
			B b = new B();
			b.x;
			""";
		var complete = completeAt(code, 8, 1);
		assertNotNull(complete);
		assertEquals(1, count(complete, "x"), "un champ redéclaré ne sort qu'une fois");
	}

	@Test
	public void unionMemberSharedMethodIsProposedOnce() throws Exception {
		var code = """
			class A {
				public m() {}
			}
			class B extends A {}
			class C extends A {}
			function f(B | C x) {
				x.m();
			}
			""";
		var complete = completeAt(code, 7, 2);
		assertNotNull(complete);
		assertEquals("B | C", complete.type.toString(), "le receveur est bien une union");
		assertEquals(1, count(complete, "m"), "une méthode commune aux membres d'une union ne sort qu'une fois");
	}
}
