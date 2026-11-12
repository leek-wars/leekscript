package tools;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import leekscript.runner.AI;
import leekscript.runner.values.ClassLeekValue;

/**
 * Écrit {@code leekscript/runner/AIModule.java} : la classe de base des classes de module v5, qui
 * relaie vers l'IA chaque méthode non privée d'{@link AI}. Le Java généré appelle ces méthodes sans
 * les qualifier (`ops(1)`, `add(a, b)`) ; dans la classe d'une IA elles sont héritées, dans celle
 * d'un module elles passent par ce relais.
 *
 * <p>À relancer quand une méthode d'AI appelée par le code généré change :
 * {@code java tools.GenerateAIModule src/main/java/leekscript/runner/AIModule.java}
 * (TestAIModule échoue tant que le fichier n'est pas à jour).
 */
public class GenerateAIModule {

	public static void main(String[] args) throws Exception {
		Files.writeString(Path.of(args[0]), generate());
	}

	public static String generate() {
		var sb = new StringBuilder();
		sb.append("package leekscript.runner;\n\n");
		sb.append("// Fichier GÉNÉRÉ par tools.GenerateAIModule (src/test/java) : ne pas le modifier à la main.\n\n");
		sb.append("/**\n * Base de la classe Java d'un module v5. Un module n'est pas une IA : il garde celle qui\n");
		sb.append(" * l'exécute et lui relaie les méthodes que le code généré appelle sans les qualifier.\n */\n");
		sb.append("@SuppressWarnings({ \"rawtypes\", \"unchecked\" })\n");
		sb.append("public abstract class AIModule {\n\n");
		sb.append("\tprotected final AI ai;\n");
		var fields = Arrays.stream(AI.class.getDeclaredFields())
			.filter(f -> f.getType() == ClassLeekValue.class && Modifier.isFinal(f.getModifiers()) && Modifier.isPublic(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()))
			.map(f -> f.getName()).sorted().collect(Collectors.toList());
		for (var f : fields) sb.append("\tpublic final leekscript.runner.values.ClassLeekValue ").append(f).append(";\n");
		sb.append("\n\tprotected AIModule(AI ai) {\n\t\tthis.ai = ai;\n");
		for (var f : fields) sb.append("\t\tthis.").append(f).append(" = ai.").append(f).append(";\n");
		sb.append("\t}\n");

		var methods = new ArrayList<Method>();
		for (var m : AI.class.getDeclaredMethods()) {
			int mod = m.getModifiers();
			if (Modifier.isPrivate(mod) || Modifier.isAbstract(mod) || m.isSynthetic() || m.isBridge()) continue;
			if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) continue;
			methods.add(m);
		}
		methods.sort(Comparator.comparing(Method::getName).thenComparing(GenerateAIModule::parameters));
		for (var m : methods) {
			int mod = m.getModifiers();
			boolean isStatic = Modifier.isStatic(mod);
			sb.append("\n\t").append(Modifier.isPublic(mod) ? "public" : "protected").append(isStatic ? " static" : " final").append(' ');
			var tps = m.getTypeParameters();
			if (tps.length > 0) {
				sb.append('<').append(Arrays.stream(tps).map(GenerateAIModule::declare).collect(Collectors.joining(", "))).append("> ");
			}
			sb.append(name(m.getGenericReturnType())).append(' ').append(m.getName()).append('(').append(parameters(m)).append(')');
			var thrown = m.getGenericExceptionTypes();
			if (thrown.length > 0) {
				sb.append(" throws ").append(Arrays.stream(thrown).map(GenerateAIModule::name).collect(Collectors.joining(", ")));
			}
			sb.append(" {\n\t\t");
			if (m.getReturnType() != void.class) sb.append("return ");
			sb.append(isStatic ? "AI" : "ai").append('.').append(m.getName()).append('(');
			for (int i = 0; i < m.getParameterCount(); i++) sb.append(i > 0 ? ", " : "").append("a").append(i);
			sb.append(");\n\t}\n");
		}
		sb.append("}\n");
		return sb.toString();
	}

	private static String parameters(Method m) {
		var types = m.getGenericParameterTypes();
		var sb = new StringBuilder();
		for (int i = 0; i < types.length; i++) {
			var type = name(types[i]);
			if (m.isVarArgs() && i == types.length - 1) type = type.substring(0, type.length() - 2) + "...";
			sb.append(i > 0 ? ", " : "").append(type).append(" a").append(i);
		}
		return sb.toString();
	}

	private static String declare(TypeVariable<?> variable) {
		var bounds = Arrays.stream(variable.getBounds()).filter(b -> b != Object.class).map(GenerateAIModule::name).collect(Collectors.joining(" & "));
		return variable.getName() + (bounds.isEmpty() ? "" : " extends " + bounds);
	}

	private static String name(Type type) {
		if (type instanceof Class<?> c) return c.getCanonicalName();
		if (type instanceof GenericArrayType a) return name(a.getGenericComponentType()) + "[]";
		if (type instanceof TypeVariable<?> v) return v.getName();
		if (type instanceof ParameterizedType p) {
			var owner = p.getOwnerType() != null ? name(p.getOwnerType()) + "." + ((Class<?>) p.getRawType()).getSimpleName() : name(p.getRawType());
			return owner + '<' + Arrays.stream(p.getActualTypeArguments()).map(GenerateAIModule::name).collect(Collectors.joining(", ")) + '>';
		}
		if (type instanceof WildcardType w) {
			if (w.getLowerBounds().length > 0) return "? super " + name(w.getLowerBounds()[0]);
			var upper = w.getUpperBounds();
			return upper.length == 0 || upper[0] == Object.class ? "?" : "? extends " + name(upper[0]);
		}
		return type.getTypeName();
	}
}
