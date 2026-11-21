package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import leekscript.common.Type;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;

/** Variantes d'une fonction système propres à une version du langage (LeekFunctions.since). */
public class TestFunctionVersions {

	private static LeekFunctions function() {
		return new LeekFunctions("Fight", "nearest", 25, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }).keepNull(0),
			new CallableVersion(Type.INT),
		});
	}

	@Test
	public void withoutVariantEveryVersionSeesTheDeclaration() {
		var f = function();
		for (int v = 1; v <= 5; v++) assertSame(f, f.forVersion(v));
		assertEquals("nearest", f.getJavaName());
	}

	@Test
	public void variantOnlyFromItsVersion() {
		var f = function().since(5, Type.INT_OR_NULL);
		for (int v = 1; v <= 4; v++) {
			assertSame(f, f.forVersion(v));
			assertEquals(Type.INT, f.forVersion(v).getVersions()[0].return_type);
		}
		var v5 = f.forVersion(5);
		assertEquals("nearest_v5", v5.getJavaName());
		assertEquals(Type.INT_OR_NULL, v5.getVersions()[0].return_type);
		assertEquals(Type.INT_OR_NULL, v5.getVersions()[1].return_type);
		// Mêmes arguments, keepNull compris, et chaque signature renvoie à la variante
		assertEquals(1, v5.getVersions()[0].arguments.length);
		assertTrue(v5.getVersions()[0].keepsNull(0));
		assertSame(v5, v5.getVersions()[0].function);
		assertSame(f, f.getVersions()[0].function);
		assertEquals(0, v5.getArgumentsMin());
		assertEquals(1, v5.getArguments());
	}

	@Test
	public void variantKeepsTheDeclarationProperties() {
		var f = function().since(5);
		var v5 = f.forVersion(5);
		assertEquals(Type.INT, v5.getVersions()[0].return_type);
		assertEquals(25, v5.getOperations());
		// Réglés après since() : la variante suit la déclaration
		f.setOperations(40);
		f.setMaxVersion(4, "other");
		assertEquals(40, v5.getOperations());
		assertEquals(4, v5.getMaxVersion());
		assertEquals("other", v5.getReplacement());
		assertEquals("nearest", v5.getName());
		assertEquals("Fight", v5.getStandardClass());
		assertTrue(v5.isStatic());
		assertFalse(v5.hasFileArgument());
	}

	@Test
	public void latestVariantAtOrBelowTheVersion() {
		var f = function().since(4, Type.INT_OR_NULL).since(5, Type.STRING);
		assertSame(f, f.forVersion(3));
		assertEquals("nearest_v4", f.forVersion(4).getJavaName());
		assertEquals("nearest_v5", f.forVersion(5).getJavaName());
		assertEquals(Type.STRING, f.forVersion(5).getVersions()[0].return_type);
	}

	@Test
	public void methodFormFollowsTheVariants() {
		// alias() avant ou après since() : la forme méthode (`x.m()`) suit la version, comme la forme libre
		var before = new LeekFunctions("TestSinceA", "nearestA", 25, true, new CallableVersion[] { new CallableVersion(Type.INT, new Type[] { Type.INT }) });
		before.alias("nearA");
		before.since(5, Type.INT_OR_NULL);
		var after = new LeekFunctions("TestSinceB", "nearestB", 25, true, new CallableVersion[] { new CallableVersion(Type.INT, new Type[] { Type.INT }) });
		after.since(5, Type.INT_OR_NULL);
		after.alias("nearB");
		for (var m : new LeekFunctions[] { LeekFunctions.getMethod("TestSinceA", "nearA"), LeekFunctions.getMethod("TestSinceB", "nearB") }) {
			assertEquals(Type.INT, m.forVersion(4).getVersions()[0].return_type);
			assertTrue(m.forVersion(5).getJavaName().endsWith("_v5"));
			assertEquals(Type.INT_OR_NULL, m.forVersion(5).getVersions()[0].return_type);
			assertEquals(m.forVersion(5).getJavaName().replace("_v5", ""), m.forVersion(5).getDeclaredJavaName());
		}
	}
}
