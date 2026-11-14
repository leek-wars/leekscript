package leekscript.compiler.bloc;

import leekscript.common.Annotation;
import leekscript.common.Error;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.AnnotationArgs;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;

/**
 * Ce que le lanceur de tests appelle lui-même dans un module de test : une fonction de premier
 * niveau ou une méthode statique annotée `@test`, `@before` ou `@after`.
 */
public interface TestEntry {

	boolean hasAnnotation(Annotation a);

	/** Arguments de l'annotation, jamais null. */
	AnnotationArgs getAnnotationArgs(Annotation a);

	Location getLocation();

	/** Test, ou préparation et clôture d'un test. */
	default boolean isTestEntry() {
		for (var a : Annotation.TEST_ENTRIES) if (hasAnnotation(a)) return true;
		return false;
	}

	/** Contrôles d'une déclaration qui porte une annotation de test, une fois ses paramètres connus. */
	default void checkTestEntry(WordCompiler compiler, Token name, int parameters) throws LeekCompilerException {
		// Le lanceur appelle lui-même un test, sa préparation et sa clôture : sans argument
		if (isTestEntry() && parameters > 0) {
			compiler.addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.TEST_FUNCTION_WITH_PARAMETERS, new String[] { name.getWord() }));
		}
		// Un test vérifie, un bench mesure : pas les deux à la fois
		if (hasAnnotation(Annotation.TEST) && hasAnnotation(Annotation.BENCH)) {
			compiler.addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.ANNOTATION_INVALID_CONTEXT, new String[] { "bench" }));
		}
		// `@skip` et `@fails` qualifient un test ou un bench
		if (!hasAnnotation(Annotation.TEST) && !hasAnnotation(Annotation.BENCH)) {
			if (hasAnnotation(Annotation.SKIP)) compiler.addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.ANNOTATION_INVALID_CONTEXT, new String[] { "skip" }));
			if (hasAnnotation(Annotation.FAILS)) compiler.addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.ANNOTATION_INVALID_CONTEXT, new String[] { "fails" }));
		}
	}
}
