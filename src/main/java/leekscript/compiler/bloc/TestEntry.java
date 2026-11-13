package leekscript.compiler.bloc;

import leekscript.common.Annotation;
import leekscript.compiler.AnnotationArgs;
import leekscript.compiler.Location;

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
		return hasAnnotation(Annotation.TEST) || hasAnnotation(Annotation.BEFORE) || hasAnnotation(Annotation.AFTER);
	}
}
