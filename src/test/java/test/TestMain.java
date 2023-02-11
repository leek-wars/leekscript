package test;

import java.util.Locale;

import org.junit.Assert;

public class TestMain {

	public static void main(String[] args) throws Exception {

		Locale.setDefault(Locale.FRENCH);
		Locale currentLocale = Locale.getDefault();
		System.out.println("Locale = ");
		System.out.println(currentLocale.getDisplayLanguage());
		System.out.println(currentLocale.getDisplayCountry());
		System.out.println(currentLocale.getLanguage());
		System.out.println(currentLocale.getCountry());
		System.out.println(System.getProperty("user.country"));
		System.out.println(System.getProperty("user.language"));

        System.out.println("Start tests...");

		// TestCommon.loadReferenceOperations();

		// new TestCommon().code_v4("var a = [1, 2, 3] return count(a)").equals("3");

		new TestGeneral().run();
		new TestNumber().run();
		new TestString().run();
		new TestArray().run();
		new TestArrayStress().run();
		new TestMap().run();
		new TestMapStress().run();
		new TestObject().run();
		new TestObjectStress().run();
		new TestClass().run();
		new TestComments().run();
		new TestOperators().run();
		new TestReference().run();
		new TestGlobals().run();
		new TestIf().run();
		new TestLoops().run();
		new TestFunction().run();
		new TestSystem().run();
		new TestJSON().run();
		new TestOperations().run();
		new TestFiles().run();
		new TestEuler().run();

		// TestCommon.ouputOperationsFile();
		Assert.assertTrue(TestCommon.summary());

    }
}
