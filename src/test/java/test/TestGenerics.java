package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestGenerics extends TestCommon {

	@Test
	public void run() throws Exception {

		section("Interval<integer> and Interval<real> type annotations");
		code_v4_("Interval<integer> x = [1..10] return intervalMin(x)").equals("1");
		code_v4_("Interval<real> x = [1.0..10.0] return intervalMin(x)").almost(1.0);

		section("Generic class with type parameter");
		// Basic constructor + field access
		code_v4_("class Box<T> { public T value constructor(T v) { this.value = v } } Box<integer> b = new Box(42) return b.value").equals("42");
		code_v4_("class Box<T> { public T value constructor(T v) { this.value = v } } Box<string> b = new Box(\"hello\") return b.value").equals("\"hello\"");

		// Method returning T and taking T
		code_v4_("class Box<T> { public T value constructor(T v) { this.value = v } public T get() { return this.value } public void set(T v) { this.value = v } } Box<integer> b = new Box(1) b.set(42) return b.get()").equals("42");

		// Function parameter typed with a generic class
		code_v4_("class Box<T> { public T value constructor(T v) { this.value = v } } function use(Box<integer> b) { return b.value } Box<integer> b = new Box(42) return use(b)").equals("42");

		// Function type using a generic class
		code_v4_("class Box<T> { public T value constructor(T v) { this.value = v } } Function<Box<integer> => integer> f = function(Box<integer> b) => integer { return b.value } Box<integer> b = new Box(42) return f(b)").equals("42");

		section("Generic class with multiple type parameters");
		code_v4_("class Pair<K, V> { public K key public V value constructor(K k, V v) { this.key = k this.value = v } } Pair<integer, string> p = new Pair(1, \"a\") return p.key + p.value").equals("\"1a\"");

		section("Generic functions");
		code_v4_("function id<T>(T x) => T { return x } return id(12)").equals("12");
		code_v4_("function id<T>(T x) => T { return x } return id(\"hello\")").equals("\"hello\"");
		code_v4_("function pickSecond<T, U>(T a, U b) => U { return b } return pickSecond(1, \"a\")").equals("\"a\"");
		code_v4_("function map<T, U>(Array<T> a, Function<T => U> f) => Array<U> { var result = [] for (var i = 0; i < count(a); ++i) push(result, f(a[i])) return result } Array<integer> input = [1, 2, 3] Array<real> output = map(input, function(integer x) => real { return x * 1.5 }) return output").equals("[1.5, 3.0, 4.5]");
	}
}
