package test;

import leekscript.compiler.LeekScript;

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
}
