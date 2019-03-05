package test;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.ResolverContext;
import leekscript.compiler.resolver.Resolver;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestWithFile {

	@Test
	public void testBasicFile() throws Exception {
		assertEquals("bonjour", LeekScript.runFile("test/ai/bonjour.leek"));
	}
	
	@Test
	public void testLargeFile() throws Exception {
		assertEquals("cent-vingt-trois millions quatre-cent-cinquante-six-mille-sept-cent-quatre-vingt-neuf", LeekScript.runFile("test/ai/french.leek"));
	}
	
	@Test
	public void testInclude() throws Exception {
		assertEquals("[a, b, KEY]", LeekScript.runFile("test/ai/array_keys.leek"));
	}
	
	@Test
	public void testIncludeMultiple() throws Exception {
		assertEquals("[a, b, KEY]", LeekScript.runFile("test/ai/include_multiple.leek"));
	}
	
	@Test
	public void testCustomResolver() throws Exception {
		class CustomContext extends ResolverContext {} 
		LeekScript.setResolver(new Resolver<CustomContext>() {
			@Override
			public AIFile<CustomContext> resolve(String path, ResolverContext context) {
				return new AIFile<CustomContext>(path, "return 'generated';", new CustomContext());
			}
		});
		assertEquals("generated", LeekScript.runFile("whatever"));
		LeekScript.resetResolver();
	}
	
	@Test
	public void testSubFolder() throws Exception {
		assertEquals("sub", LeekScript.runFile("test/ai/include_sub.leek"));
	}
	
	@Test
	public void testRelativePath() throws Exception {
		assertEquals("cent-vingt-trois millions quatre-cent-cinquante-six-mille-sept-cent-quatre-vingt-neuf", LeekScript.runFile("test/ai/subfolder/include_parent.leek"));
	}
	
	@Test
	public void testMultipleInclude() throws Exception {
		assertEquals("bonjour", LeekScript.runFile("test/ai/multiple_includes.leek"));
	}
}
