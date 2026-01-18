package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

/**
 * Tests for edge cases that could cause bugs, based on recent commits:
 * - Memory pressure scenarios (SoftReference cache, OOM in tournaments)
 * - Deep recursion with temporary structures (minimax-like algorithms)
 * - Null/reference edge cases
 * - Operations and RAM limit boundaries
 */
@ExtendWith(SummaryExtension.class)
public class TestEdgeCases extends TestCommon {

	@Test
	public void run() throws Exception {

		/**
		 * Deep recursion with temporary arrays
		 * These simulate minimax-like algorithms that caused OOM in tournaments
		 */
		section("Deep recursion with temporary arrays");

		// Simple recursion creating arrays at each level
		code_v4_("function rec(depth) { if (depth == 0) return [] var a = [1, 2, 3] return rec(depth - 1) } return count(rec(100))").equals("0");

		// Recursion that accumulates array references
		code_v4_("function rec(depth, acc) { if (depth == 0) return acc push(acc, [depth, depth * 2]) return rec(depth - 1, acc) } return count(rec(50, []))").equals("50");

		// Tree-like recursion (simulates minimax)
		code_v4_("function tree(depth) { if (depth == 0) return 1 var left = tree(depth - 1) var right = tree(depth - 1) return left + right } return tree(10)").equals("1024");

		// Tree recursion with array creation at each node - returns nested array
		code_v4_("function tree(depth) { if (depth == 0) return [1] var arr = [] push(arr, tree(depth - 1)) push(arr, tree(depth - 1)) return arr } return count(tree(5))").equals("2");

		/**
		 * Minimax simulation - this pattern caused OOM in tournaments
		 */
		section("Minimax-like patterns");

		// Minimax without pruning - creates many temporary arrays
		code_v4_("function minimax(depth, isMax) { if (depth == 0) return randInt(0, 100) var scores = [] for (var i = 0; i < 3; i++) push(scores, minimax(depth - 1, !isMax)) if (isMax) return arrayMax(scores) else return arrayMin(scores) } return minimax(4, true) >= 0").equals("true");

		// Minimax with move generation - simplified version
		code_v4_("function minimax2(val, depth, isMax) { if (depth == 0) return val var scores = [] for (var i = -1; i <= 1; i++) push(scores, minimax2(val + i, depth - 1, !isMax)) if (isMax) return arrayMax(scores) else return arrayMin(scores) } return minimax2(0, 3, true) >= -3").equals("true");

		/**
		 * Nested data structures - potential memory issues
		 */
		section("Deeply nested structures");

		// Deep array nesting - typeOf returns a number
		code_v4_("var a = [1] for (var i = 0; i < 50; i++) a = [a] return count(a)").equals("1");

		// Deep map nesting - use mapSize instead of count
		code_v4_("any m = ['a': 1] for (var i = 0; i < 50; i++) m = ['nested': m] return mapSize(m)").equals("1");

		// Deep object nesting
		code_v4_("class Node { any value; any next; } var n = new Node() n.value = 1 for (var i = 0; i < 50; i++) { var newN = new Node() newN.value = i newN.next = n n = newN } return n.value").equals("49");

		// Mixed nesting - use mapSize
		code_v4_("any x = 1 for (var i = 0; i < 20; i++) x = ['arr': [x, x]] return mapSize(x)").equals("1");

		/**
		 * Edge cases with null references - related to NPE fixes
		 */
		section("Null reference edge cases");

		// Null in array operations
		code_v4_("var a = [null, null, null] return count(a)").equals("3");
		code_v4_("var a = [1, 2, 3] return arrayFoldLeft(a, (acc, v) -> acc + v, 0)").equals("6");
		code_v4_("var a = [null] push(a, null) return count(a)").equals("2");

		// Null in map operations - use mapSize for maps
		code_v4_("any m = ['a': null, 'b': null] return mapSize(m)").equals("2");
		code_v4_("any m = [:] m['x'] = null return m['x']").equals("null");

		// Null function arguments
		code_v4_("function f(x) { return x == null ? 'null' : 'not null' } return f(null)").equals("\"null\"");
		code_v4_("function f(x, y, z) { return [x, y, z] } return f(null, null, null)").equals("[null, null, null]");

		// Null in conditionals
		code_v4_("var x = null if (x) return 'true' else return 'false'").equals("\"false\"");
		code_v4_("var x = null return x ? 'true' : 'false'").equals("\"false\"");
		code_v4_("var x = null return !x").equals("true");

		// Null coalescing patterns (manual implementation since ?? doesn't exist)
		code_v4_("var x = null var y = x == null ? 42 : x return y").equals("42");
		code_v4_("var x = 0 var y = x == null ? 42 : x return y").equals("0");
		code_v4_("function maybeNull(b) { if (b) return 'value' return null } var r = maybeNull(false) return r == null ? 'default' : r").equals("\"default\"");

		/**
		 * Operations limit edge cases
		 */
		section("Operations limit edge cases");

		// Just under the limit
		code_v4_("var sum = 0 for (var i = 0; i < 1000; i++) sum += i return sum").max_ops(50000).equals("499500");

		// Exceeding the limit
		code_v4_("var sum = 0 for (var i = 0; i < 1000000; i++) sum += i return sum").max_ops(1000).error(Error.TOO_MUCH_OPERATIONS);

		// Recursive operations limit
		code_v4_("function rec(n) { if (n == 0) return 0 return 1 + rec(n - 1) } return rec(1000)").max_ops(50000).equals("1000");
		code_v4_("function rec(n) { if (n == 0) return 0 return 1 + rec(n - 1) } return rec(10000)").max_ops(1000).error(Error.TOO_MUCH_OPERATIONS);

		/**
		 * RAM limit edge cases
		 */
		section("RAM limit edge cases");

		// Array that grows to limit
		code_v4_("var a = [] for (var i = 0; i < 1000; i++) push(a, i) return count(a)").max_ram(50000).equals("1000");
		code_v4_("var a = [] for (var i = 0; i < 100000; i++) push(a, i) return count(a)").max_ram(1000).error(Error.OUT_OF_MEMORY);

		// Map that grows to limit - use any type and mapSize
		code_v4_("any m = [:] for (var i = 0; i < 1000; i++) m[i] = i return mapSize(m)").max_ram(50000).equals("1000");
		code_v4_("any m = [:] for (var i = 0; i < 100000; i++) m[i] = i return mapSize(m)").max_ram(1000).error(Error.OUT_OF_MEMORY);

		// String concatenation memory
		code_v4_("var s = '' for (var i = 0; i < 1000; i++) s += 'x' return length(s)").max_ram(50000).equals("1000");

		/**
		 * Garbage collection scenarios - testing that temporary objects are freed
		 */
		section("GC scenarios - temporary object cleanup");

		// Create and discard many arrays (should not OOM if GC works)
		code_v4_("for (var i = 0; i < 100; i++) { var temp = [] for (var j = 0; j < 1000; j++) push(temp, j) } return 'ok'").max_ram(100000).equals("\"ok\"");

		// Create and discard many maps
		code_v4_("for (var i = 0; i < 100; i++) { any temp = [:] for (var j = 0; j < 1000; j++) temp[j] = j } return 'ok'").max_ram(100000).equals("\"ok\"");

		// Recursive function that creates and discards arrays
		code_v4_("function work(n) { if (n == 0) return 0 var temp = [1, 2, 3, 4, 5] return work(n - 1) } return work(100)").max_ram(10000).equals("0");

		/**
		 * Complex data structure patterns
		 */
		section("Complex data structure patterns");

		// Graph-like structure (adjacency list) - use global
		code_v4_("global graph = [:] for (var i = 0; i < 10; i++) graph[i] = [] for (var i = 0; i < 10; i++) for (var j = i + 1; j < 10; j++) { push(graph[i], j) push(graph[j], i) } return count(graph[0])").equals("9");

		// Matrix operations
		code_v4_("function createMatrix(n) { var m = [] for (var i = 0; i < n; i++) { var row = [] for (var j = 0; j < n; j++) push(row, i * n + j) push(m, row) } return m } var mat = createMatrix(10) return mat[5][5]").equals("55");

		// Priority queue simulation - use global
		code_v4_("global pq = [] function pqPush(val) { push(pq, val) sort(pq) } function pqPop() { return shift(pq) } for (var i = 10; i > 0; i--) pqPush(i) return pqPop()").equals("1");

		/**
		 * Edge cases with closures and memory
		 */
		section("Closure memory edge cases");

		// Closure capturing large array
		code_v4_("var large = [] for (var i = 0; i < 1000; i++) push(large, i) var f = function() { return count(large) } return f()").equals("1000");

		// Multiple closures sharing state
		code_v4_("var shared = [0] var inc = function() { shared[0]++ } var get = function() { return shared[0] } for (var i = 0; i < 100; i++) inc() return get()").equals("100");

		// Nested closures
		code_v4_("function outer(x) { return function(y) { return function(z) { return x + y + z } } } return outer(1)(2)(3)").equals("6");

		/**
		 * Type coercion edge cases
		 */
		section("Type coercion edge cases");

		// Mixed type arrays
		code_v4_("var a = [1, 'two', 3.0, null, true] return count(a)").equals("5");

		// String coercion
		code_v4_("return 'value: ' + 42").equals("\"value: 42\"");
		code_v4_("return 'value: ' + null").equals("\"value: null\"");
		code_v4_("return 'value: ' + [1, 2, 3]").equals("\"value: [1, 2, 3]\"");

		// Numeric operations
		code_v4_("return 1 + 2.5").almost(3.5);
		code_v4_("return 1 * 2.5").almost(2.5);

		/**
		 * Boundary value tests
		 */
		section("Boundary values");

		// Large integers
		code_v4_("return 9223372036854775807").equals("9223372036854775807");
		code_v4_("var x = 9223372036854775807 return x + 1").equals("-9223372036854775808"); // Overflow

		// Empty structures
		code_v4_("var a = [] return count(a)").equals("0");
		code_v4_("any m = [:] return mapSize(m)").equals("0");
		code_v4_("var s = '' return length(s)").equals("0");

		// Single element structures
		code_v4_("var a = [42] return a[0]").equals("42");
		code_v4_("var m = ['key': 'value'] return m['key']").equals("\"value\"");

		/**
		 * Stress test combining multiple patterns
		 */
		section("Combined stress test");

		// Tree with arrays at each node, limited recursion
		code_v4_("function buildTree(depth) { if (depth == 0) return ['leaf': true, 'data': [1, 2, 3]] return ['left': buildTree(depth - 1), 'right': buildTree(depth - 1), 'data': [depth]] } var tree = buildTree(3) return tree['data'][0]").equals("3");

		// Recursive array transformation
		code_v4_("function transform(arr, depth) { if (depth == 0) return arr return transform(arrayMap(arr, x -> x * 2), depth - 1) } return transform([1, 2, 3, 4], 3)").equals("[8, 16, 24, 32]");

		// Dynamic programming pattern with memoization
		code_v4_("function fib(n, memo) { if (n <= 1) return n if (memo[n] != null) return memo[n] memo[n] = fib(n - 1, memo) + fib(n - 2, memo) return memo[n] } any memo = [:] return fib(30, memo)").equals("832040");

		/**
		 * Additional stress tests for OOM scenarios
		 */
		section("Tournament-like stress scenarios");

		// Multiple sequential computations (like tournament rounds)
		code_v4_("var results = [] for (var round = 0; round < 8; round++) { var scores = [] for (var i = 0; i < 100; i++) push(scores, randInt(0, 1000)) push(results, arrayMax(scores)) } return count(results)").equals("8");

		// Simulate 16 fights in first round (tournament pattern)
		code_v4_("function simulateFight() { var moves = [] for (var turn = 0; turn < 64; turn++) { push(moves, [randInt(0, 10), randInt(0, 10)]) } return randInt(0, 2) } var winners = [] for (var i = 0; i < 16; i++) { var winner = simulateFight() push(winners, winner) } return count(winners)").equals("16");

		// Deep tree search with alpha-beta like pruning
		code_v4_("function search(depth, alpha, beta, isMax) { if (depth == 0) return randInt(-100, 100) for (var i = 0; i < 3; i++) { var score = search(depth - 1, alpha, beta, !isMax) if (isMax) { alpha = max(alpha, score) } else { beta = min(beta, score) } if (beta <= alpha) break } return isMax ? alpha : beta } return search(5, -1000, 1000, true) >= -1000").equals("true");

		/**
		 * Value conversions edge cases
		 */
		section("Value conversions");

		// Integer to real
		code_v4_("real x = 42 return x").equals("42.0");
		code_v4_("function f(real r) { return r } return f(42)").equals("42.0");
		code_v4_("var a = [1.0, 2.0] a[0] = 5 return a[0]").equals("5"); // Array doesn't enforce element type

		// Real to integer (truncation)
		code_v4_("integer x = 42.9 return x").equals("42");
		code_v4_("function f(integer i) { return i } return f(42.9)").equals("42");

		// Boolean conversions
		code_v4_("return !!1").equals("true");
		code_v4_("return !!0").equals("false");
		code_v4_("return !!''").equals("false");
		code_v4_("return !!'hello'").equals("true");
		code_v4_("return !![]").equals("false"); // Empty array is falsy in LeekScript
		code_v4_("return !![1]").equals("true"); // Non-empty array is truthy
		code_v4_("return !!null").equals("false");

		// String conversions
		code_v4_("return '' + 42").equals("\"42\"");
		code_v4_("return '' + 3.14").equals("\"3.14\"");
		code_v4_("return '' + true").equals("\"true\"");
		code_v4_("return '' + false").equals("\"false\"");
		code_v4_("return '' + null").equals("\"null\"");

		// Array type declarations
		code_v4_("Array<integer> a = [1, 2, 3] return a").equals("[1, 2, 3]");
		code_v4_("Array<real> a = [1.0, 2.0, 3.0] return a").equals("[1.0, 2.0, 3.0]");
		code_v4_("any a = [1, 2, 3] Array<integer> b = a return b").equals("[1, 2, 3]");

		/**
		 * Complex assignments - nested maps and chained assignments
		 */
		section("Complex assignments");

		// Simple chained assignment
		code_v4_("var a var b a = b = 5 return [a, b]").equals("[5, 5]");
		code_v4_("var a var b var c a = b = c = 10 return [a, b, c]").equals("[10, 10, 10]");

		// Assignment in expression
		code_v4_("var x = 0 return (x = 5) + 3").equals("8");
		code_v4_("var x = 0 var y = (x = 10) return [x, y]").equals("[10, 10]");

		// Nested map assignment (the buggy case)
		code_v4_("global Map x x = [:]").equals("[:]");
		code_v4_("global x global y x = (y = [:])").equals("[:]");
		code_v4_("global x global y = [1:2] x = (y[1] = [:])").equals("[:]");
		code_v4_("var z = 2 global x global y = [1:2] x = (y[z] = [:])").equals("[:]");

		// Complex nested map types
		code_v4_("global Map<integer, Map<integer, boolean>> x x = (x[1] = [:]) x").equals("[:]");
		code_v4_("global Map<integer, Map<integer, Map<integer, boolean>>> y = [:] global Map<integer, Map<integer, boolean>> x x = (y[5] = [:]) x").equals("[:]");

		// Triple nested assignment
		code_v4_("any a = [:] any b = [:] any c = [:] a[1] = b b[2] = c c[3] = 'value' return a[1][2][3]").equals("\"value\"");

		// Assignment with computed index
		code_v4_("var e = 2 var f = 4 global x global y = [1:2] x = (y[e | f << 2] = [:])").equals("[:]");

		// Array element assignment in expression
		code_v4_("var a = [0, 0, 0] var x = (a[1] = 42) return [a, x]").equals("[[0, 42, 0], 42]");

		// Compound assignment in nested structure
		code_v4_("any m = ['a': 10] m['a'] += 5 return m['a']").equals("15");
		code_v4_("var a = [[1, 2], [3, 4]] a[0][1] += 10 return a").equals("[[1, 12], [3, 4]]");

		/**
		 * Type inference and conversion in functions
		 */
		section("Type inference in functions");

		// Return type inference
		code_v4_("function f() { return 42 } integer x = f() return x").equals("42");
		code_v4_("function f() { return [1, 2, 3] } Array<integer> x = f() return x").equals("[1, 2, 3]");

		// Generic function call type handling
		code_v4_("function f(x) { return x } return f(42)").equals("42");
		code_v4_("function f(x) { return x } return f('hello')").equals("\"hello\"");
		code_v4_("function f(x) { return x } return f([1, 2, 3])").equals("[1, 2, 3]");

		// Type checking with typeOf
		code_v4_("any x = 42 if (typeOf(x) == 1) { return x + 1 } return 0").equals("43"); // 1 = integer type
		code_v4_("any x = 'hello' if (typeOf(x) == 3) { return length(x) } return 0").equals("5"); // 3 = string type

		/**
		 * Edge cases with operators and assignments
		 */
		section("Operator assignment edge cases");

		// Increment/decrement with side effects
		code_v4_("var x = 5 var y = x++ return [x, y]").equals("[6, 5]");
		code_v4_("var x = 5 var y = ++x return [x, y]").equals("[6, 6]");
		code_v4_("var a = [1, 2, 3] var i = 0 a[i++] = 10 return [a, i]").equals("[[10, 2, 3], 1]");

		// Compound operators with type promotion
		code_v4_("var x = 5 x += 2.5 return x").almost(7.5);
		code_v4_("var x = 10 x /= 4 return x").almost(2.5);

		// Bitwise compound operators
		code_v4_("var x = 12 x |= 5 return x").equals("13");
		code_v4_("var x = 12 x &= 5 return x").equals("4");
		code_v4_("var x = 12 x <<= 2 return x").equals("48");
		code_v4_("var x = 12 x >>= 2 return x").equals("3");

		/**
		 * Map/Array access patterns that could cause issues
		 */
		section("Access pattern edge cases");

		// Chained access
		code_v4_("var a = [[1, 2], [3, 4], [5, 6]] return a[1][1]").equals("4");
		code_v4_("any m = ['a': ['b': ['c': 42]]] return m['a']['b']['c']").equals("42");

		// Access with function call as index
		code_v4_("function idx() { return 1 } var a = [10, 20, 30] return a[idx()]").equals("20");
		code_v4_("function key() { return 'x' } any m = ['x': 42] return m[key()]").equals("42");

		// Modify through access
		code_v4_("var a = [[1, 2], [3, 4]] a[0][0] = 99 return a").equals("[[99, 2], [3, 4]]");
		code_v4_("any m = ['a': ['b': 1]] m['a']['b'] = 99 return m").equals("[\"a\" : [\"b\" : 99]]");

		// Null-safe access patterns
		code_v4_("any m = [:] var x = m['missing'] return x == null").equals("true");
		code_v4_("var a = [] var x = a[999] return x == null").equals("true");

		/**
		 * Reference and copy semantics
		 */
		section("Reference vs copy semantics");

		// Array reference behavior
		code_v4_("var a = [1, 2, 3] var b = a push(b, 4) return a").equals("[1, 2, 3, 4]");
		code_v4_("var a = [1, 2, 3] var b = clone(a) push(b, 4) return a").equals("[1, 2, 3]");

		// Map reference behavior
		code_v4_("any a = ['x': 1] any b = a b['y'] = 2 return a").equals("[\"x\" : 1, \"y\" : 2]");
		code_v4_("any a = ['x': 1] any b = clone(a) b['y'] = 2 return mapSize(a)").equals("1");

		// Nested structure reference
		code_v4_("var outer = [[1, 2, 3]] var inner = outer[0] inner[0] = 99 return outer").equals("[[99, 2, 3]]");

		// Function parameter reference
		code_v4_("function modify(arr) { push(arr, 99) } var a = [1, 2, 3] modify(a) return a").equals("[1, 2, 3, 99]");

	}
}
