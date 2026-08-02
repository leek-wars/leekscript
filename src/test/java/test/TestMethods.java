package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

/**
 * Object-oriented standard library (version 5): calling standard-library functions
 * as methods on the value, `array.sort()` instead of `sort(array)`.
 */
@ExtendWith(SummaryExtension.class)
public class TestMethods extends TestCommon {

	@Test
	public void arrayMethodsVarReceiver() throws Exception {
		section("Methods / array (var receiver, runtime dispatch)");
		// `var` gives the receiver a static type of `any`, so the method is resolved
		// at runtime. In-place mutators must mutate the actual receiver.
		code_v5("var a = [3, 1, 2] a.sort() return a").equals("[1, 2, 3]");
		code_v5("var a = [1, 2] a.push(3) return a").equals("[1, 2, 3]");
		code_v5("var a = [3, 1, 2] a.reverse() return a").equals("[2, 1, 3]");
		code_v5("var a = [1, 2, 3] var x = a.pop() return [a, x]").equals("[[1, 2], 3]");
		// Read-only methods.
		code_v5("var a = [11, 22, 33] return a.count()").equals("3");
		code_v5("var a = [1, 2, 3] return a.sum()").equals("6.0");
	}

	@Test
	public void arrayMethodsTypedReceiver() throws Exception {
		section("Methods / array (typed receiver, static dispatch)");
		// A precise static type resolves the method at compile time.
		code_v5("Array<integer> a = [3, 1, 2] a.sort() return a").equals("[1, 2, 3]");
		code_v5("Array<integer> a = [1, 2] a.push(3) return a").equals("[1, 2, 3]");
		code_strict_v5_("Array<integer> a = [11, 22, 33] integer n = a.count() return n").noWarning();
		// On a literal receiver.
		code_v5("return [5, 1, 9].count()").equals("3");
		code_v5("return [3, 1, 2].sum()").equals("6.0");
	}

	@Test
	public void cleanMethodNames() throws Exception {
		section("Methods / clean names (arrayMap -> map, etc.)");
		// The `arrayXxx` library functions are exposed under clean method names in v5.
		code_v5("return [1, 2, 3].map(x -> x * 2)").equals("[2, 4, 6]");
		code_v5("var a = [1, 2, 3] return a.map(x -> x * 2)").equals("[2, 4, 6]");
		code_v5("Array<integer> a = [1, 2, 3] return a.map(x -> x * 2)").equals("[2, 4, 6]");
		code_v5("return [1, 2, 3, 4].filter(x -> x > 2)").equals("[3, 4]");
		code_v5("return [3, 1, 4, 1, 5].max()").equals("5");
		code_v5("return [3, 1, 4, 1, 5].min()").equals("1");
		code_v5("return [1, 1, 2, 3, 3].unique()").equals("[1, 2, 3]");
		code_v5("return [[1, 2], [3, 4]].flatten()").equals("[1, 2, 3, 4]");
		code_v5("return [1, 2, 3].concat([4, 5])").equals("[1, 2, 3, 4, 5]");
		// Inferred return type of the generic method is usable.
		code_strict_v5_("Array<integer> a = [1, 2, 3] integer m = a.max() return m").noWarning();
	}

	@Test
	public void mapMethods() throws Exception {
		section("Methods / map");
		code_v5("var m = [1: 10, 2: 20] return m.size()").equals("2");
		code_v5("var m = [1: 10, 2: 20] return m.keys()").equals("[1, 2]");
		code_v5("var m = [1: 10, 2: 20] return m.values()").equals("[10, 20]");
		code_v5("var m = [1: 10, 2: 20] return m.contains(10)").equals("true");
		code_v5("var m = [1: 10, 2: 20] return m.containsKey(2)").equals("true");
		code_v5("Map<integer, integer> m = [1: 10, 2: 20] return m.size()").equals("2"); // typed
		// mapMap callback is (value, key, map).
		code_v5("var m = [1: 10, 2: 20] return m.map((value, key) -> value * 2)").equals("[1 : 20, 2 : 40]");
	}

	@Test
	public void setMethods() throws Exception {
		section("Methods / set");
		code_v5("var s = <1, 2, 3> return s.size()").equals("3");
		code_v5("var s = <1, 2, 3> return s.contains(2)").equals("true");
		code_v5("var s = <1, 2, 3> return s.toArray()").equals("[1, 2, 3]");
		code_v5("var s = <1, 2> return s.union(<2, 3>)").equals("<1, 2, 3>");
		code_v5("var s = <1, 2, 3, 4> return s.filter(x -> x > 2)").equals("<3, 4>");
	}

	@Test
	public void stringMethods() throws Exception {
		section("Methods / string (static helper shape)");
		// String methods live in the static StringClass helper, not as instance methods
		// on the Java String — the codegen branches on isStatic.
		code_v5("return \"hello\".length()").equals("5");
		code_v5("var s = \"hello\" return s.length()").equals("5");
		code_v5("string s = \"hello\" return s.length()").equals("5");
		code_v5("return \"hello\".charAt(1)").equals("\"e\"");
		code_v5("return \"hello world\".split(\" \")").equals("[\"hello\", \"world\"]");
		code_v5("return \"Hello\".toUpper()").equals("\"HELLO\"");
		code_v5("return \"hello\".contains(\"ell\")").equals("true");
		code_v5("return \"  hi  \".trim()").equals("\"hi\"");
		// substring(index, length).
		code_v5("return \"hello\".substring(1, 3)").equals("\"ell\"");
		code_v5("return \"hello\".replace(\"l\", \"L\")").equals("\"heLLo\"");
		code_strict_v5_("string s = \"abc\" integer n = s.length() return n").noWarning();
	}

	@Test
	public void numberMethods() throws Exception {
		section("Methods / number (static helper, overload selection)");
		// Number methods are static NumberClass helpers with int/real/big_integer
		// overloads; the runtime dispatch selects the overload matching the actual type
		// (reflection does no widening). Literal receivers (5.abs()) are avoided: the
		// lexer would read `5.` as a real, so a typed/var receiver is used.
		code_v5("integer x = -5 return x.abs()").equals("5");
		code_v5("var x = -5 return x.abs()").equals("5");
		code_v5("real x = 9.0 return x.sqrt()").equals("3.0");
		code_v5("var x = 9.0 return x.sqrt()").equals("3.0");
		code_v5("integer x = 9 return x.sqrt()").equals("3.0");
		code_v5("var x = 9 return x.sqrt()").equals("3.0");
		code_v5("integer x = 5 return x.min(3)").equals("3");
		code_v5("var x = 7 return x.max(10)").equals("10");
		code_v5("real x = 2.5 return x.floor()").equals("2");
		code_strict_v5_("integer x = -5 integer a = x.abs() return a").noWarning();
	}

	@Test
	public void mathNamespace() throws Exception {
		section("Namespace / Math (Math.sqrt(x), Math.rand()...)");
		// Forme canonique v5 des fonctions mathématiques : `Math.sqrt(x)` au lieu de
		// `sqrt(x)`. Résolue et générée comme l'ancienne forme libre (NumberClass.sqrt).
		code_v5("return Math.sqrt(16)").equals("4.0");
		code_v5("return Math.abs(-5)").equals("5");
		code_v5("return Math.max(3, 7)").equals("7");
		code_v5("return Math.min(3, 7)").equals("3");
		code_v5("return Math.floor(2.7)").equals("2");
		code_v5("return Math.pow(2, 10)").equals("1024.0");
		code_v5("var x = -3 return Math.abs(x)").equals("3");
		// Fonctions sans receveur de valeur : seul le namespace les expose proprement.
		code_v5("var r = Math.randInt(0, 5) return r >= 0 && r <= 5").equals("true");
		code_v5("var r = Math.rand() return r >= 0.0 && r < 1.0").equals("true");
		// Le type de retour est correctement propagé (pas de cast Object).
		code_strict_v5_("real r = Math.sqrt(9.0) return r").noWarning();
		// Le namespace Math n'existe pas avant la v5.
		code_v4("return Math.sqrt(16)").error(leekscript.common.Error.UNKNOWN_VARIABLE_OR_FUNCTION);
	}

	@Test
	public void otherNamespaces() throws Exception {
		section("Namespaces / JSON, String, Array, Map, Set, System, Number");
		// JSON : noms propres via alias (jsonEncode -> JSON.encode).
		code_v5("return JSON.encode([1, 2, 3])").equals("\"[1,2,3]\"");
		code_v5("return JSON.decode(\"[1,2,3]\")").equals("[1, 2, 3]");
		// String : helpers statiques (StringClass.charAt...).
		code_v5("return String.charAt(\"hello\", 1)").equals("\"e\"");
		code_v5("return String.length(\"hello\")").equals("5");
		code_v5("return String.split(\"a,b,c\", \",\")").equals("[\"a\", \"b\", \"c\"]");
		// Array : la forme namespace reste valide même si la forme libre est retirée v5.
		code_v5("var a = [3, 1, 2] Array.sort(a) return a").equals("[1, 2, 3]");
		code_v5("return Array.count([5, 1, 9])").equals("3");
		code_v5("return Array.map([1, 2, 3], x -> x * 2)").equals("[2, 4, 6]");
		// Map / Set.
		code_v5("var m = [1: 10, 2: 20] return Map.size(m)").equals("2");
		code_v5("var s = <1, 2, 3> return Set.contains(s, 2)").equals("true");
		// Number namespace (équivalent de Math pour les fonctions Number).
		code_v5("return Number.sqrt(9.0)").equals("3.0");
		// System namespace (fonctions sans receveur : debug, getOperations...).
		code_v5("System.debug(42) return 1").equals("1");
		// La forme libre retirée reste une erreur (le namespace ne la ressuscite pas).
		code_v5("var a = [3, 1, 2] sort(a) return a").error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT);
		// Avant la v5 ces namespaces de fonctions ne sont pas exposés.
		code_v4("return JSON.encode([1])").error(leekscript.common.Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);
	}

	@Test
	public void onlyInV5() throws Exception {
		section("Methods / version gate");
		// In v1-4 the method form does not resolve (the value-method OO library is a
		// version-5 feature): the call is a silent no-op, the array is unchanged.
		code_v4("var a = [3, 1, 2] a.sort() return a").equals("[3, 1, 2]");
		// The clean name is method-only: it does not become a free global function.
		code_v5("return map([1, 2, 3], x -> x * 2)").error(leekscript.common.Error.UNKNOWN_VARIABLE_OR_FUNCTION);
	}

	@Test
	public void intervalAndValueNamespaces() throws Exception {
		section("Namespaces / Interval, Value");
		// Interval : la forme namespace complète la forme méthode (i.min()), utile quand le
		// receveur est un intervalle abstrait, non dispatché en méthode à la compilation.
		code_v5("var i = [1..10] return Interval.min(i)").equals("1");
		code_v5("var i = [1..10] return Interval.max(i)").equals("10");
		code_v5("var i = [1..10] return Interval.contains(i, 5)").equals("true");
		// Value : fonctions sans receveur de valeur naturel, la forme libre reste tolérée.
		code_v5("return Value.string(42)").equals("\"42\"");
		code_v5("return Value.typeOf(\"a\")").equals("3");
		code_v5("return string(42)").equals("\"42\"");
		// Aucun des deux namespaces n'existe avant la v5.
		code_v4("var i = [1..10] return Interval.min(i)").error(leekscript.common.Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST);
	}

	@Test
	public void arraySortHasCleanName() throws Exception {
		section("Methods / arraySort -> sorted");
		// sorted() renvoie un NOUVEAU tableau ; sort() trie en place. Les deux coexistent.
		code_v5("var a = [3, 1, 2] var b = a.sorted() return [a, b]").equals("[[3, 1, 2], [1, 2, 3]]");
		code_v5("var a = [3, 1, 2] return a.sorted((x, y) -> y - x)").equals("[3, 2, 1]");
		code_v5("var a = [3, 1, 2] return Array.sorted(a)").equals("[1, 2, 3]");
		// La forme libre est retirée en v5 et pointe désormais vers un nom propre.
		code_v5("var a = [3, 1, 2] return arraySort(a)")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "sorted()");
	}

	@Test
	public void removedBeforeV5StaysRemoved() throws Exception {
		section("Methods / removed before v5");
		// Capping maxVersion at 4 for the OO migration only closes the FREE form: the method
		// and namespace forms are the announced replacement and must keep resolving.
		code_v5("var a = [3, 1, 2] a.sort() return a").equals("[1, 2, 3]");
		code_v5("return \"abc\".length()").equals("3");
		code_v5("var a = [1, 2] return Array.count(a)").equals("2");
		// But a function genuinely removed from the language in v3 must not come back through
		// the method or namespace door.
		code_v5("Array<integer> a = [1, 2, 3] return a.subArray(0, 2)").error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "slice()");
		// Receveur `var` : statiquement `any`, donc resolu au RUNTIME. Le filtre doit y etre aussi,
		// sinon la fonction retiree revient silencieusement (erreur de champ inconnu attendue ici).
		// Un champ inconnu sur receveur `var` est journalise sans faire echouer l'IA : la valeur
		// vaut null. Avant le filtre runtime, ces deux appels renvoyaient tranquillement [1, 2] et
		// la map amputee, ressuscitant une fonction retiree depuis la v3.
		code_v5("var a = [1, 2, 3] return a.subArray(0, 2)").logsError(leekscript.common.Error.UNKNOWN_FIELD);
		code_v5("var a = [1, 2, 3] return a.subArray(0, 2)").equals("null");
		code_v5("var m = [1 : 2] m.removeKey(1) return m").logsError(leekscript.common.Error.UNKNOWN_FIELD);
		code_v5("Map<integer, integer> m = [1 : 2] m.removeKey(1) return m").error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "remove()");
		code_v5("var a = [1, 2, 3] return Array.subArray(a, 0, 2)").error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT);
		code_v5("return Math.randFloat(0, 1)").error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT);
	}

	@Test
	public void removalReplacementChain() throws Exception {
		section("Methods / replacement chain");
		// A function removed in v3 points at whatever replaced it back then, but that name
		// may itself have lost its free form in v5. The suggestion must be resolved for the
		// version being compiled, otherwise the player is sent to an identifier that does
		// not compile either.
		code_v4("var a = [1, 2, 3] return subArray(a, 0, 2)")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "arraySlice");
		code_v5("var a = [1, 2, 3] return subArray(a, 0, 2)")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "slice()");
		code_v4("var m = [1 : 2] removeKey(m, 1) return m")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "mapRemove");
		code_v5("var m = [1 : 2] removeKey(m, 1) return m")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "remove()");
		code_v4("return randFloat(0, 1)")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "randReal");
		code_v5("return randFloat(0, 1)")
			.error(leekscript.common.Error.REMOVED_FUNCTION_REPLACEMENT, "Math.randReal");
	}
}
