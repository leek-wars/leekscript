package leekscript.compiler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import leekscript.common.Annotation;

/**
 * Arguments d'une annotation : `@skip("raison")`, `@test(ops: 50_000_000)`. Des littéraux
 * seulement (chaîne, nombre, booléen), en position ou nommés.
 */
public class AnnotationArgs {

	public static final AnnotationArgs NONE = new AnnotationArgs();

	private final List<Object> positional = new ArrayList<>();
	private final Map<String, Object> named = new LinkedHashMap<>();

	public void add(String name, Object value) {
		if (name == null) positional.add(value);
		else named.put(name, value);
	}

	public Map<String, Object> named() {
		return named;
	}

	/** Premier argument en position s'il est une chaîne, sinon null. */
	public String text() {
		return !positional.isEmpty() && positional.get(0) instanceof String s ? s : null;
	}

	public boolean isEmpty() {
		return positional.isEmpty() && named.isEmpty();
	}

	/** Range les arguments d'une annotation dans la table d'une déclaration (créée au besoin, rendue). */
	public static EnumMap<Annotation, AnnotationArgs> put(EnumMap<Annotation, AnnotationArgs> byAnnotation, Annotation annotation, AnnotationArgs args) {
		if (args.isEmpty()) return byAnnotation;
		if (byAnnotation == null) byAnnotation = new EnumMap<>(Annotation.class);
		byAnnotation.put(annotation, args);
		return byAnnotation;
	}

	/** Les arguments d'une annotation dans la table d'une déclaration, jamais null. */
	public static AnnotationArgs get(EnumMap<Annotation, AnnotationArgs> byAnnotation, Annotation annotation) {
		var args = byAnnotation == null ? null : byAnnotation.get(annotation);
		return args == null ? NONE : args;
	}
}
