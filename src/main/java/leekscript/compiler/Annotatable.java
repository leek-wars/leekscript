package leekscript.compiler;

import leekscript.common.Annotation;

public interface Annotatable {
	void addAnnotation(Annotation a);

	/** Annotation avec ses arguments (`@skip("raison")`) ; par défaut les arguments sont ignorés. */
	default void addAnnotation(Annotation a, AnnotationArgs args) {
		addAnnotation(a);
	}
}
