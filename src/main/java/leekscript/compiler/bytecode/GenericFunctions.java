package leekscript.compiler.bytecode;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import leekscript.common.CompoundType;
import leekscript.common.FunctionType;
import leekscript.common.LanguageFeature;
import leekscript.common.Type;
import leekscript.runner.CallableVersion;
import leekscript.runner.values.BigIntegerValue;

/**
 * Les enveloppes génériques des fonctions système (JavaWriter.writeGenericFunctions) : une
 * méthode `Std_<clé>(Object a0, …)` par jeu de versions, qui choisit la version par instanceof
 * puis convertit ses arguments, la valeur par défaut du retour sur un ClassCastException.
 */
public final class GenericFunctions {

	private GenericFunctions() {}

	static boolean convertsNumberArgument(CallableVersion version) {
		for (int a = 0; a < version.arguments.length; a++) {
			if ((version.arguments[a] == Type.INT || version.arguments[a] == Type.REAL) && !version.keepsNull(a)) return true;
		}
		return false;
	}

	static String methodName(List<CallableVersion> versions, String key) {
		return versions.get(0).function.getStandardClass() + "_" + key;
	}

	static Class<?> returnClass(BytecodeWriter writer, List<CallableVersion> versions) {
		var type = Type.compound(versions.stream().map(v -> v.return_type).collect(Collectors.toCollection(HashSet::new)));
		return writer.primitiveClass(type);
	}

	/** L'appel de l'enveloppe, depuis le site d'appel. */
	public static JExpr call(BytecodeWriter writer, List<CallableVersion> versions, String key, List<JExpr> arguments) {
		var parameters = Collections.<Class<?>>nCopies(versions.get(0).arguments.length, Object.class);
		return new JExpr.Self(methodName(versions, key), parameters, returnClass(writer, versions), arguments);
	}

	public static void write(ClassBuilder clb, BytecodeWriter writer) {
		int version = writer.version;
		for (var entry : writer.getGenericFunctions().entrySet()) {
			var key = entry.getKey();
			var versions = entry.getValue();
			var first = versions.get(0);
			int n = first.arguments.length;
			var returnClass = returnClass(writer, versions);
			var params = new ClassDesc[n];
			java.util.Arrays.fill(params, JTypes.desc(Object.class));
			boolean strict = writer.isStrictGenericFunction(key);

			clb.withMethodBody(methodName(versions, key), MethodTypeDesc.of(JTypes.desc(returnClass), params), ClassFile.ACC_PRIVATE, cb -> {
				writer.begin(cb, 1 + n, returnClass);
				var a = new ArrayList<JExpr>();
				for (int i = 0; i < n; i++) a.add(new JExpr.Read(new BytecodeWriter.LocalVar(1 + i, Object.class, "a" + i)));

				// Versions en conflit : chacune (sauf la première, le repli) testée par instanceof
				for (int vi = 1; vi < versions.size(); vi++) {
					var other = versions.get(vi);
					JExpr condition = null;
					for (int i = 0; i < other.arguments.length; i++) {
						JExpr test = new JExpr.InstanceOf(a.get(i), writer.javaClass(other.arguments[i]));
						condition = condition == null ? test : new JExpr.Binary("&&", condition, test);
					}
					var next = writer.newLabel();
					if (condition != null) writer.jump(condition, false, next);
					writer.emitReturn(functionCall(writer, other, a, null), returnClass);
					writer.bind(next);
				}

				var x = new ArrayList<JExpr>(Collections.nCopies(n, null));
				for (int i = 0; i < n; i++) {
					var argument = first.arguments[i];
					if (argument == Type.ANY || first.keepsNull(i)) continue;
					var local = writer.declare(new Object(), writer.primitiveClass(argument), "x" + i);
					x.set(i, new JExpr.Read(local));
					// try { x = convert(a); } catch (ClassCastException e) { return <défaut>; }
					var start = writer.newLabel();
					var end = writer.newLabel();
					var handler = writer.newLabel();
					var after = writer.newLabel();
					writer.bind(start);
					writer.emitStatement(new JExpr.Assign(local, convert(writer, i + 1, a.get(i), argument, version, strict)));
					writer.bind(end);
					writer.goto_(after);
					writer.bind(handler);
					cb.pop();
					writer.emitReturn(writer.defaultValue(first.return_type), returnClass);
					writer.bind(after);
					cb.exceptionCatch(start, end, handler, JTypes.desc(ClassCastException.class));
				}
				writer.emitReturn(functionCall(writer, first, a, x), returnClass);
			});
		}
	}

	/** writeFunctionCall : la fonction Java de la version, arguments castés (`x` null) ou convertis. */
	private static JExpr functionCall(BytecodeWriter writer, CallableVersion version, List<JExpr> a, List<JExpr> x) {
		var function = version.function;
		var name = function.getJavaName();
		if ((version.return_type.isArray() || version.return_type.isArrayOrNull()) && writer.version <= 3) name += "_v1_3";
		var args = new ArrayList<JExpr>();
		args.add(writer.aiThis());
		int start = function.isStatic() ? 0 : 1;
		for (int i = start; i < version.arguments.length; i++) {
			if (x == null) args.add(new JExpr.Cast(writer.javaClass(version.arguments[i]), a.get(i)));
			else if (x.get(i) != null) args.add(x.get(i));
			else args.add(a.get(i));
		}
		if (function.isStatic()) return new JExpr.StaticCall(JTypes.forName(function.getStandardClass() + "Class"), name, args);
		JExpr receiver;
		if (x == null) receiver = new JExpr.Cast(writer.javaClass(version.arguments[0]), a.get(0));
		else if (x.get(0) != null) receiver = x.get(0);
		else throw new Unsupported("receveur non converti");
		return new JExpr.Call(receiver, name, args);
	}

	/** JavaWriter.convert : un argument Object vers le type d'un paramètre de fonction système. */
	private static JExpr convert(BytecodeWriter writer, int index, JExpr v, Type type, int version, boolean strictNull) {
		if (type.isArray()) return JExpr.call(version >= 4 ? "toArray" : "toLegacyArray", JExpr.of(index), v);
		if (type.isMap()) return JExpr.call("toMap", JExpr.of(index), v);
		if (type instanceof FunctionType) return JExpr.call("toFunction", JExpr.of(index), v);
		boolean nullNotZero = LanguageFeature.NULL_ARGUMENT_NOT_ZERO.enabledIn(version);
		if (type == Type.INT) return nullNotZero ? JExpr.call("longintArgument", v, JExpr.of(index), JExpr.of(strictNull)) : JExpr.call("longint", v);
		if (type == Type.BIG_INT) return new JExpr.StaticCall(BigIntegerValue.class, "valueOf", List.of(writer.aiThis(), v));
		if (type == Type.REAL) return nullNotZero ? JExpr.call("realArgument", v, JExpr.of(index), JExpr.of(strictNull)) : JExpr.call("real", v);
		if (type == Type.STRING) return JExpr.call("string", v);
		if (type instanceof CompoundType ct && ct.getTypes().size() == 2 && ct.containsNull()) {
			for (var t : ct.getTypes()) {
				if (t != Type.NULL && t == Type.INT) return JExpr.call("toLongOrNull", v);
			}
		}
		var javaName = type.getJavaName(version);
		if (!javaName.equals("Object") && javaName.matches("[A-Z]\\w*")) {
			return JExpr.call("castArgument", v, new JExpr.ClassLit(writer.typeNamed(javaName)));
		}
		return new JExpr.Cast(writer.typeNamed(javaName), v);
	}
}
