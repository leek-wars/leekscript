package leekscript.compiler.bytecode;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ConvertInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.OperatorInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Mode `compare` du backend (cf {@link BytecodeBackend}) : les classes qu'il écrit pour un programme,
 * contrôlées par celles de javac. On ne peut pas jouer deux fois un combat : on compare ce que font les
 * méthodes. L'empreinte d'une méthode est le multi-ensemble de ce qui a un sens dans son code : appels
 * (surcharge comprise), champs lus et écrits, constantes chargées (dont celles d'ops()), opérateurs et
 * conversions, casts, tests de type et allocations. Le reste (locales, pile, sauts, lignes) tient à la
 * façon d'écrire du compilateur, pas au programme.
 *
 * Ce qui diffère sans changer le programme est normalisé : les classes anonymes (javac les numérote et
 * les range sous la classe utilisateur qui les écrit, le backend sous la classe racine) comptent
 * ensemble par classe de premier niveau ; un appel de méthode d'accès `access$N` compte pour ce qu'elle
 * fait (javac en écrit une par site d'appel, et pour un `C.super.m()` dans C) ; `$link` ne compte pas
 * (javac y câble aussi les modules que seul du code mort lit) ; un membre hérité de
 * l'IA ou d'AIModule se lit quel que soit le type qui le qualifie ; les contrôles de nullité que javac
 * ajoute (`Objects.requireNonNull`) et le code mort qu'il écrit quand même (classe anonyme jamais
 * instanciée, méthode privée jamais appelée) ne comptent pas ; un cast compte une fois par type.
 */
public final class BytecodeCompare {

	private BytecodeCompare() {}

	// Une classe anonyme et ce qui la précède jusqu'à la classe racine : `AI_N$u_X$1`, `AI_N$3$1`, `AI_N$7`
	private static final Pattern ANONYMOUS = Pattern.compile("(\\$(u_[^$;\\s.()/]+|\\d+))*\\$\\d+(?=[$;\\s.()/]|$)");
	private static final Pattern ACCESSOR = Pattern.compile("access\\$\\d+");
	private static final Pattern SYNTHETIC_FIELD = Pattern.compile("this\\$\\d+|val\\$.*");

	/** La première différence entre les classes de javac et celles du backend, null si leurs empreintes sont les mêmes. */
	public static String difference(Map<String, byte[]> javac, Map<String, byte[]> bytecode) {
		var all = differences(javac, bytecode, 1);
		return all.isEmpty() ? null : all.get(0);
	}

	/** Les différences (au plus `max`), méthode par méthode. */
	public static java.util.List<String> differences(Map<String, byte[]> javac, Map<String, byte[]> bytecode, int max) {
		var a = fingerprint(javac);
		var b = fingerprint(bytecode);
		var keys = new java.util.TreeSet<String>();
		keys.addAll(a.keySet());
		keys.addAll(b.keySet());
		var result = new java.util.ArrayList<String>();
		for (var key : keys) {
			var x = a.getOrDefault(key, Map.of());
			var y = b.getOrDefault(key, Map.of());
			if (!x.equals(y)) result.add(key + " : javac " + only(x, y) + " / bytecode " + only(y, x));
			if (result.size() >= max) break;
		}
		return result;
	}

	/** Ce que `x` a de plus que `y` (au plus quelques éléments, pour le message). */
	private static String only(Map<String, Integer> x, Map<String, Integer> y) {
		var extra = new TreeMap<String, Integer>();
		for (var e : x.entrySet()) {
			int n = e.getValue() - y.getOrDefault(e.getKey(), 0);
			if (n > 0) extra.put(e.getKey(), n);
		}
		var s = extra.toString();
		return s.length() > 600 ? s.substring(0, 600) + "…" : s;
	}

	/** Par méthode (les classes anonymes ensemble par classe de premier niveau) : le multi-ensemble de son code. */
	private static Map<String, Map<String, Integer>> fingerprint(Map<String, byte[]> classes) {
		return new Fingerprint(classes).result;
	}

	/** Le nom d'une classe anonyme (`AI_N$7`, `AI_N$u_X$1`) : il finit par `$` et des chiffres. */
	private static boolean isAnonymous(String name) {
		int dollar = name.lastIndexOf('$');
		if (dollar < 0 || dollar == name.length() - 1) return false;
		for (int i = dollar + 1; i < name.length(); i++) if (!Character.isDigit(name.charAt(i))) return false;
		return true;
	}

	/** La clé d'un appel : propriétaire (nom interne), nom et descripteur, comme le nom d'une méthode déclarée. */
	private static String callKey(InvokeInstruction i) {
		return i.owner().asInternalName() + "." + i.name().stringValue() + i.typeSymbol().descriptorString();
	}

	/** L'empreinte des classes d'un compilateur, et ce qu'il faut connaître d'elles pour la normaliser. */
	private static final class Fingerprint {

		final Map<String, Map<String, Integer>> result = new HashMap<>();
		// Les membres que déclare chaque classe générée et sa super-classe : un membre se rapporte à la
		// classe générée qui le déclare (javac le qualifie par une sous-classe), et un membre qu'aucune ne
		// déclare est hérité du runtime (l'IA, AIModule, NativeObjectLeekValue)
		private final Map<String, Set<String>> declared = new HashMap<>();
		private final Map<String, String> supers = new HashMap<>();
		// Les noms et descripteurs déjà normalisés : peu de valeurs distinctes pour beaucoup d'instructions
		private final Map<String, String> normalized = new HashMap<>();

		Fingerprint(Map<String, byte[]> classes) {
			var classFile = ClassFile.of();
			var models = new HashMap<String, ClassModel>();
			for (var e : classes.entrySet()) models.put(e.getKey(), classFile.parse(e.getValue()));
			// Les méthodes appelées et les classes instanciées quelque part : une méthode privée que rien
			// n'appelle, une classe anonyme que rien n'instancie, sont du code mort que javac écrit quand même
			// (enveloppe générique, fonction anonyme d'une branche `if (false)`)
			var called = new HashSet<String>();
			var instantiated = new HashSet<String>();
			for (var e : models.entrySet()) {
				var model = e.getValue();
				model.superclass().ifPresent(c -> supers.put(e.getKey(), c.asInternalName().replace('/', '.')));
				var members = new HashSet<String>();
				for (var m : model.methods()) members.add(m.methodName().stringValue() + m.methodType().stringValue());
				for (var f : model.fields()) members.add(f.fieldName().stringValue());
				declared.put(e.getKey(), members);
				for (var m : model.methods()) {
					m.code().ifPresent(code -> {
						for (var element : code) {
							if (element instanceof NewObjectInstruction n) instantiated.add(n.className().asInternalName());
							if (element instanceof InvokeInstruction i) called.add(callKey(i));
						}
					});
				}
			}
			// Ce que fait chaque méthode d'accès, compté là où elle est appelée
			var accessors = new HashMap<String, java.util.List<String>>();
			for (var e : models.entrySet()) {
				for (var m : e.getValue().methods()) {
					if (!ACCESSOR.matcher(m.methodName().stringValue()).matches()) continue;
					var items = new java.util.ArrayList<String>();
					m.code().ifPresent(code -> {
						for (var element : instructions(code)) {
							var item = item(element);
							if (item != null) items.add(item);
						}
					});
					accessors.put(e.getKey() + "." + m.methodName().stringValue() + m.methodType().stringValue(), items);
				}
			}
			for (var e : models.entrySet()) {
				var name = e.getKey();
				boolean anonymous = isAnonymous(name);
				if (anonymous && !instantiated.contains(name)) continue;
				for (var m : e.getValue().methods()) {
					var methodName = m.methodName().stringValue();
					// La table des fichiers : chaque compilateur la remplit dans son ordre, cohérent avec ses lignes
					if (!anonymous && (methodName.equals("getErrorFiles") || methodName.equals("getErrorFilesID") || methodName.equals("$link"))) continue;
					if (ACCESSOR.matcher(methodName).matches()) continue;
					if (m.flags().has(java.lang.reflect.AccessFlag.PRIVATE) && !methodName.startsWith("<") && !called.contains(name + "." + methodName + m.methodType().stringValue())) continue;
					var key = anonymous ? name.substring(0, name.indexOf('$')) + "$anonymes" : name + "." + methodName + normalize(m.methodType().stringValue());
					var bag = result.computeIfAbsent(key, k -> new HashMap<>());
					m.code().ifPresent(code -> {
						for (var element : instructions(code)) {
							if (element instanceof InvokeInstruction call && ACCESSOR.matcher(call.name().stringValue()).matches()) {
								for (var item : accessors.getOrDefault(callKey(call), java.util.List.of())) bag.merge(item, 1, Integer::sum);
								continue;
							}
							var item = item(element);
							if (item == null) continue;
							// Un cast compte une fois par type : les deux compilateurs n'en écrivent pas autant (javac
							// en ajoute pour la variable d'un `instanceof T x`, et en omet que le backend écrit)
							if (item.startsWith("checkcast ")) bag.put(item, 1);
							else bag.merge(item, 1, Integer::sum);
						}
					});
				}
			}
		}

		private static java.util.List<java.lang.classfile.Instruction> instructions(java.lang.classfile.CodeModel code) {
			var list = new java.util.ArrayList<java.lang.classfile.Instruction>();
			for (var element : code) if (element instanceof java.lang.classfile.Instruction i) list.add(i);
			return list;
		}

		private String normalize(String s) {
			return normalized.computeIfAbsent(s, v -> ANONYMOUS.matcher(ACCESSOR.matcher(v).replaceAll("access\\$")).replaceAll("\\$anonyme"));
		}

		/** Le type qui qualifie un membre : la classe générée qui le déclare, ou le runtime dont il est hérité. */
		private String owner(String owner, String member) {
			// Une classe anonyme se lit par sa super-classe (javac type `new S() { … }` par l'anonyme, le backend par S)
			if (isAnonymous(owner) && supers.containsKey(owner)) owner = supers.get(owner);
			if (declared.containsKey(owner)) {
				for (var c = owner; c != null && declared.containsKey(c); c = supers.get(c)) {
					if (declared.get(c).contains(member)) return normalize(c);
				}
				return "<hérité>";
			}
			if (owner.equals("leekscript.runner.AI") || owner.equals("leekscript.runner.AIModule")) return "<hérité>";
			return normalize(owner);
		}

		private String item(java.lang.classfile.Instruction element) {
			return switch (element) {
				case InvokeInstruction i -> {
					var owner = i.owner().asInternalName();
					var name = i.name().stringValue();
					var type = i.typeSymbol().descriptorString();
					if (owner.equals("java/util/Objects") && name.equals("requireNonNull")) yield null;
					yield "appel " + owner(owner.replace('/', '.'), name + type) + "." + normalize(name) + normalize(type);
				}
				case FieldInstruction f -> {
					var name = f.name().stringValue();
					// L'instance englobante et les locales capturées d'une classe interne : sa plomberie, que
					// javac et le backend n'imbriquent pas de la même façon (et javac omet un this$0 inutile)
					if (SYNTHETIC_FIELD.matcher(name).matches()) yield null;
					yield f.opcode().name().toLowerCase() + " " + owner(f.owner().asInternalName().replace('/', '.'), name) + "." + name + ":" + normalize(f.typeSymbol().descriptorString());
				}
				case ConstantInstruction c -> c.opcode() == Opcode.ACONST_NULL ? null : "constante " + c.constantValue();
				case OperatorInstruction o -> o.opcode().name().toLowerCase();
				case ConvertInstruction c -> c.opcode().name().toLowerCase();
				case TypeCheckInstruction t -> t.opcode().name().toLowerCase() + " " + normalize(t.type().asInternalName());
				case NewObjectInstruction n -> "new " + normalize(n.className().asInternalName());
				case NewReferenceArrayInstruction n -> "anewarray " + normalize(n.componentType().asInternalName());
				case NewMultiArrayInstruction n -> "multianewarray " + normalize(n.arrayType().asInternalName());
				default -> null;
			};
		}
	}
}
