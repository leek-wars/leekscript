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

		// new TestCommon().code_v11("return 5 + 5;").equals("10");
		new TestGeneral().run();
		new TestNumber().run();
		new TestString().run();
		new TestArray().run();
		new TestComments().run();
		new TestOperators().run();
		new TestReference().run();
		new TestGlobals().run();
		new TestIf().run();
		new TestLoops().run();
		new TestFunction().run();
		new TestFiles().run();
		new TestEuler().run();

		Assert.assertTrue(TestCommon.summary());
    }
}
