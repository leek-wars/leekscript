package leekscript.testing;

import java.util.List;

import leekscript.runner.AI;

/**
 * Un test à lancer : le module, sa table, le test choisi, et la liste où l'appelant recopie le
 * journal de l'IA (jointe au résultat ; null : aucun journal).
 */
public record TestRun(String module, List<TestCase> table, TestCase test, List<String> logs) {

	/** Lance le test sur une IA neuve, prête à tourner (cf. {@link TestRunner#run}). */
	public TestResult on(AI ai) {
		return TestRunner.run(ai, this);
	}

	public TestResult skipped() {
		return new TestResult(module, test.name(), test.line(), TestResult.Status.SKIPPED, test.skip(), 0, 0, 0, List.of(), List.of(), null, List.of(), test.isBench(), test.tolerance());
	}

	/** Le test n'a pas pu être lancé, ou son résultat est perdu : une erreur à son nom. */
	public TestResult error(String message) {
		return TestResult.error(module, test.name(), test.line(), message, logs);
	}
}
