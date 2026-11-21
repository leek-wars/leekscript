package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

/**
 * Règles de typage propres à LeekScript 5, gelées en v1-4 parce qu'elles casseraient des IA
 * existantes : chaque section fige aussi le comportement v4.
 */
@ExtendWith(SummaryExtension.class)
public class TestV5Types extends TestCommon {

	@Test
	public void nullToNumber() throws Exception {
		section("v5 : null converti en integer / real = IMPOSSIBLE_CAST (#5389)");
		// Déclarations
		code_v5_("Map m = [:] real r = m[0] return r").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map m = [:] integer i = m[0] return i").error(Error.IMPOSSIBLE_CAST);
		code_strict_v5_("Map m = [:] real r = m[0] return r").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var m = [:] integer n = m['k'] n++ return n").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var x = null integer i = x return i").error(Error.IMPOSSIBLE_CAST);
		// Affectations, globales, cases de tableau typé
		code_v5_("var m = [:] integer i = 2 i = m[0] return i").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var m = [:] real r = 2.5 r = m[0] return r").error(Error.IMPOSSIBLE_CAST);
		code_v5_("global integer g = 1 var m = [:] g = m[0] return g").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<integer> a = [1] var m = [:] a[0] = m[0] return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, real> a = ['a': 1.5] var m = [:] a['a'] = m[0] return a").error(Error.IMPOSSIBLE_CAST);
		// Paramètres et retours typés
		code_v5_("function f(integer x) { return x } var m = [:] return f(m[0])").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f(real x) { return x } var m = [:] return f(m[0])").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f() => integer { var m = [:] return m[0] } return f()").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f() => real { var m = [:] return m[0] } return f()").error(Error.IMPOSSIBLE_CAST);
		// `return?` écarte null avant de convertir
		code_strict_v5_("function f(integer? x) => integer { return? x return 12 } return [f(5), f(null), f(0)]").equals("[5, 12, 12]");
		code_v5_("function f(any x) => real { return? x return 1.5 } return [f(2.5), f(null)]").equals("[2.5, 1.5]");
		// Champs typés, chemin statique et chemin dynamique (cf TestFieldTypeRepro)
		code_v5_("class A { integer x } var m = [:] A a = new A() a.x = m[0] return a.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { real x } var m = [:] A a = new A() a.x = m[0] return a.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { integer x } var m = [:] var a = new A() a.x = m[0] return a.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { static integer x } var m = [:] A.x = m[0] return A.x").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { integer x constructor() { var m = [:] this.x = m[0] } } return new A().x").error(Error.IMPOSSIBLE_CAST);
		// Conversion explicite
		code_v5_("var m = [:] return m[0] as integer").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var m = [:] return m[0] as real").error(Error.IMPOSSIBLE_CAST);

		section("v5 : le reste de la conversion vers integer / real ne change pas (#5389)");
		code_v5_("var x = 2.7 integer i = x return i").equals("2");
		code_v5_("var x = true integer i = x return i").equals("1");
		code_v5_("var x = 12 real r = x return r").equals("12.0");
		code_v5_("var x = [1, 2, 3] integer i = x return i").equals("3");
		code_v5_("class A { integer x } var a = new A() a.x = 1.5 return a.x").equals("1");
		// Un type nullable garde null
		code_v5_("Map m = [:] real? r = m[0] return r").equals("null");
		code_v5_("Map m = [:] integer? i = m[0] return i").equals("null");
		code_v5_("class A { integer? x = 2 } var a = new A() a.x = null return a.x").equals("null");
		code_v5_("function f(integer? x) { return x } var m = [:] return f(m[0])").equals("null");
		// null comme opérande arithmétique vaut toujours 0 : ce n'est pas une conversion
		code_v5_("integer i = 1 var x = null i += x return i").equals("1");
		code_v5_("real r = 1.5 var x = null r *= x return r").equals("0.0");

		section("v1-4 : null converti en integer / real vaut toujours 0 / 0.0 (#5389)");
		code_v2_4("var m = [:] real r = m[0] return r").equals("0.0");
		code_v2_4("var m = [:] integer i = m[0] return i").equals("0");
		code_strict_v4("Map m = [:] real r = m[0] return r").equals("0.0");
		code_v4("var m = [:] integer n = m['k'] n++ return n").equals("1");
		code_v4("Array<integer> a = [1] var m = [:] a[0] = m[0] return a").equals("[0]");
		code_v2_4("function f(integer x) { return x } var m = [:] return f(m[0])").equals("0");
		code_v2_4("function f() => real { var m = [:] return m[0] } return f()").equals("0.0");
		code_v4("class A { integer x } var m = [:] A a = new A() a.x = m[0] return a.x").equals("0");
		code_v4("class A { static integer x } var m = [:] A.x = m[0] return A.x").equals("0");
		code_v4("var m = [:] return m[0] as integer").equals("0");
	}

	@Test
	public void fieldTypeDefault() throws Exception {
		section("v5 : un champ typé sans initialiseur vaut la valeur par défaut de son type (#4635)");
		code_v5_("class A { static Set<A> aSet } A.aSet.put(new A()) return A.aSet.size()").equals("1");
		code_v5_("class A { Set<integer> s integer x real r string t boolean b big_integer g Array<integer> a Map<string, integer> m } var a = new A() return [a.s, a.x, a.r, a.t, a.b, a.g, a.a, a.m]").equals("[<>, 0, 0.0, \"\", false, 0, [], [:]]");
		code_v5_("class A { static Set<integer> s static integer x static real r static string t static boolean b static big_integer g static Array<integer> a static Map<string, integer> m } return [A.s, A.x, A.r, A.t, A.b, A.g, A.a, A.m]").equals("[<>, 0, 0.0, \"\", false, 0, [], [:]]");
		code_strict_v5_("class A { Array<integer> a } A a = new A() a.a.push(2) return a.a").equals("[2]");
		// Un type nullable explicite, any, une classe ou une fonction gardent null
		code_v5_("class A { Set<A> | null s A? a Array<integer>? n integer? x any z A c Function f } var a = new A() return [a.s, a.a, a.n, a.x, a.z, a.c, a.f]").equals("[null, null, null, null, null, null, null]");
		code_v5_("class A { static Set<A> | null s static A? a static any z } return [A.s, A.a, A.z]").equals("[null, null, null]");
		// Un conteneur par instance, champs hérités compris, l'initialiseur explicite l'emporte
		code_v5_("class A { Array<integer> a } var x = new A() var y = new A() x.a.push(1) return [x.a, y.a]").equals("[[1], []]");
		code_v5_("class P { Array<integer> a } class C extends P { Set<integer> s } var c = new C() return [c.a, c.s]").equals("[[], <>]");
		code_v5_("class A { Array<integer> a = [1] static Map<string, integer> m = ['a': 1] } return [new A().a, A.m]").equals("[[1], [\"a\" : 1]]");
		code_v5_("class A { Array<integer> a constructor() { this.a.push(3) } } return new A().a").equals("[3]");
		code_v5_("class B<T> { T x Array<T> a } var b = new B() return [b.x, b.a]").equals("[null, []]");
		// L'init paresseuse sur `== null` ne se déclenche plus : le champ n'est jamais null
		code_v5_("class A { static Array<integer> a static f() { if (A.a == null) A.a = [1] return A.a } } return A.f()").equals("[]");
		// Coût : celui d'un initialiseur explicite `= []` (aucune opération ; l'allocation compte en RAM)
		code_v5_("class A { Array<integer> a } new A() return 0").ops(1);
		code_v5_("class A { Array<integer> a = [] } new A() return 0").ops(1);

		section("v1-4 : un champ typé sans initialiseur reste null (#4635)");
		code_v4("class A { static Set<A> s } return A.s").equals("null");
		code_v4("class A { Set<integer> s integer x real r string t boolean b big_integer g Array<integer> a Map<string, integer> m } var a = new A() return [a.s, a.x, a.r, a.t, a.b, a.g, a.a, a.m]").equals("[null, 0, 0.0, null, false, null, null, null]");
		code_v4("class A { static Set<integer> s static integer x static string t static Array<integer> a } return [A.s, A.x, A.t, A.a]").equals("[null, 0, null, null]");
		code_v4("class A { static Array<integer> a static f() { if (A.a == null) A.a = [1] return A.a } } return A.f()").equals("[1]");
		code_v4("class A { Array<integer> a } new A() return 0").ops(1);
	}

	@Test
	public void nonNullElements() throws Exception {
		section("v5 : insérer null dans un conteneur typé non nullable = IMPOSSIBLE_CAST (#5349)");
		// Le cas du forum : `!` ne retire null que du type statique, l'insertion le refuse
		code_v5_("class Test { public helloWorld() { return 1 } } Array<Test> aTests = [] Test? nullable = null aTests.push(nullable!) return aTests").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class Test {} Array<Test> a = [] Test? n = null a.unshift(n!) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class Test {} Array<Test> a = [] Test? n = null a.insert(n!, 0) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class Test {} Array<Test> a = [new Test()] Test? n = null a.fill(n!) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class Test {} Array<Test> a = [new Test()] Test? n = null a[0] = n! return a").error(Error.IMPOSSIBLE_CAST);
		code_strict_v5_("class Test {} Array<Test> a = [new Test()] Test? n = null a[0] = n! return a").error(Error.IMPOSSIBLE_CAST);
		// Valeur venue d'any, autres types d'élément
		code_v5_("Array<string> a = [] var x = null a.push(x) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<integer> a = [] var x = null a.push(x) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<boolean> a = [] var x = null a.insert(x, 0) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<Array<integer>> a = [] var x = null a.push(x) return a").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<string> a = ['a'] var x = null a[0] = x return a").error(Error.IMPOSSIBLE_CAST);
		// Set : l'élément ; Map : la valeur et la clé
		code_v5_("Set<string> s = <> var x = null s.put(x) return s").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class Test {} Map<string, Test> m = [:] Test? n = null m.put('a', n!) return m").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, string> m = [:] var x = null m['a'] = x return m").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, integer> m = ['a': 1] var x = null m.replace('a', x) return m").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, integer> m = ['a': 1] var x = null m.fill(x) return m").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, integer> m = [:] var k = null m.put(k, 1) return m").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, integer> m = [:] var k = null m[k] = 1 return m").error(Error.IMPOSSIBLE_CAST);

		section("v5 : sans contrôle quand l'élément accepte null ou que la valeur ne l'est pas (#5349)");
		code_v5_("Array a = [] a.push(null) return a").equals("[null]");
		code_v5_("Array<any> a = [] var x = null a.push(x) return a").equals("[null]");
		code_v5_("class Test {} Array<Test?> a = [] Test? n = null a.push(n) a.push(n!) return a").equals("[null, null]");
		code_v5_("Array<string | null> a = ['a'] var x = null a[0] = x return a").equals("[null]");
		code_v5_("Map<string, integer?> m = [:] var x = null m.put('a', x) m['b'] = x return m").equals("[\"a\" : null, \"b\" : null]");
		code_v5_("Set<string?> s = <> var x = null s.put(x) return s").equals("<null>");
		code_v5_("class B<T> { Array<T> a } var b = new B() var x = null b.a.push(x) return b.a").equals("[null]");
		code_v5_("class Test {} Array<Test> a = [] a.push(new Test()) a.unshift(new Test()) a[0] = new Test() return a.count()").equals("2");
		code_v5_("Array<string> a = [] var x = 'b' a.push('a') a.push(x) a.push(x + 1) return a").equals("[\"a\", \"b\", \"b1\"]");
		code_v5_("Map<string, Array<integer>> m = [:] var k = 'a' m[k] = [1] m.put(k + 'b', [2]) return m").equals("[\"a\" : [1], \"ab\" : [2]]");
		// L'affectation d'une variable typée n'est pas contrôlée, seule l'insertion l'est
		code_v5_("class Test {} Test? n = null Test t = n! return t").equals("null");
		// Gratuit en opérations
		code_v4("Array<string> a = ['b'] var x = 'a' a[0] = x return a").ops(7);
		code_v5_("Array<string> a = ['b'] var x = 'a' a[0] = x return a").ops(7);

		section("v1-4 : null entre dans un conteneur typé non nullable (#5349)");
		code_v4("class Test { public helloWorld() { return 1 } } Array<Test> aTests = [] Test? nullable = null push(aTests, nullable!) return aTests").equals("[null]");
		code_v4("class Test {} Array<Test> a = [new Test()] Test? n = null a[0] = n! return a").equals("[null]");
		code_v4("Array<string> a = [] var x = null push(a, x) return a").equals("[null]");
		code_v4("Set<string> s = <> var x = null setPut(s, x) return s").equals("<null>");
		code_v4("Map<string, string> m = [:] var x = null m['a'] = x mapPut(m, 'b', x) return m").equals("[\"a\" : null, \"b\" : null]");
	}

	@Test
	public void typedContentCheck() throws Exception {
		section("v5 : contenu d'un Array / Map / Set typé vérifié à la conversion depuis any (#5384)");
		// Le cas du forum : l'erreur tombe à l'affectation, plus à la lecture de a[0]
		code_v5_("Array<integer> a = (=> ['a'])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_strict_v5_("Array<integer> a = (=> ['a'])() return 1").error(Error.IMPOSSIBLE_CAST);
		// Un élément que la lecture typée refuserait : réel ou booléen dans un Array<integer>,
		// entier dans un Array<big_integer>, null hors d'un type nullable
		code_v5_("Array<integer> a = (=> [1, 1.5])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<integer> a = (=> [true])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<big_integer> a = (=> [1])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<string> a = (=> ['a', null])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A {} Array<A> a = (=> [new A(), 12])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<integer | string> a = (=> [1, 'a', true])() return 1").error(Error.IMPOSSIBLE_CAST);
		// Conteneurs imbriqués, Map (clés et valeurs), Set
		code_v5_("Array<Array<integer>> a = (=> [[1], [2, 'x']])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Array<Array> a = (=> [[1], 2])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, integer> m = (=> ['a': 1, 'b': true])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, integer> m = (=> [1: 1])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Map<string, Array<real>> m = (=> ['a': [1.5], 'b': ['x']])() return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("Set<string> s = (=> <'a', 2>)() return 1").error(Error.IMPOSSIBLE_CAST);
		// Tous les chemins de conversion : affectation, paramètre, retour, champ, `as`, nullable
		code_v5_("var x = [1, 'b'] Array<integer> a = [] a = x return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f(Array<integer> a) { return a } var x = [1, 'b'] return f(x)").error(Error.IMPOSSIBLE_CAST);
		code_v5_("function f(any x) => Array<integer> { return x } return f([1, 'b'])").error(Error.IMPOSSIBLE_CAST);
		code_v5_("class A { Array<integer> a } var x = [1, 'b'] A o = new A() o.a = x return 1").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var x = [1, 'b'] return x as Array<integer>").error(Error.IMPOSSIBLE_CAST);
		code_v5_("var x = [1, 'b'] Array<integer>? a = x return 1").error(Error.IMPOSSIBLE_CAST);

		section("v5 : contenu conforme, ou rien à vérifier (#5384)");
		code_v5_("Array<integer> a = (=> [1, 2])() return a").equals("[1, 2]");
		code_v5_("Array<real> a = (=> [1, 2.5])() return a").equals("[1, 2.5]");
		code_v5_("class A {} class B extends A {} Array<A> a = (=> [new A(), new B()])() return a.count()").equals("2");
		code_v5_("class A {} Array<A?> a = (=> [new A(), null])() return a.count()").equals("2");
		code_v5_("Map<string, Array<integer>> m = (=> ['a': [1, 2], 'b': []])() return m").equals("[\"a\" : [1, 2], \"b\" : []]");
		code_v5_("Set<integer> s = (=> <1, 2>)() return s").equals("<1, 2>");
		code_v5_("var x = null Array<integer>? a = x return a").equals("null");
		code_v5_("Array<integer> a = (=> [])() return a").equals("[]");
		// Contenu any : aucun contrôle
		code_v5_("Array a = (=> [1, 'a'])() return a").equals("[1, \"a\"]");
		code_v5_("Array<any> a = (=> [1, 'a'])() return a").equals("[1, \"a\"]");
		code_v5_("Map<any, any> m = (=> [1: 'a'])() return m").equals("[1 : \"a\"]");
		// Une affectation déjà typée n'est pas parcourue
		code_v5_("Array a = [1, 'a'] Array<integer> b = a return b").equals("[1, \"a\"]");
		// Coût : une opération par élément parcouru, conteneurs imbriqués compris
		code_v4("Array<integer> a = (=> [1, 2])() return a").ops(6);
		code_v5_("Array<integer> a = (=> [1, 2])() return a").ops(8);
		code_v4("Map<string, Array<integer>> a = (=> ['a': [1, 2], 'b': []])() return a").ops(10);
		code_v5_("Map<string, Array<integer>> a = (=> ['a': [1, 2], 'b': []])() return a").ops(14);

		section("v1-4 : le contenu n'est vérifié qu'à la lecture (#5384)");
		code_v4("Array<integer> a = (=> ['a'])() return a").equals("[\"a\"]");
		code_v4("Array<integer> a = (=> ['a'])() return a[0]").error(Error.IMPOSSIBLE_CAST);
		code_v4("Map<string, integer> m = (=> ['a': true])() return m").equals("[\"a\" : true]");
		code_v4("function f(Array<integer> a) { return a } var x = [1, 'b'] return f(x)").equals("[1, \"b\"]");
	}
}
