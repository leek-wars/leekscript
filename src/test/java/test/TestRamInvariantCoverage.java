package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import leekscript.common.Error;
import leekscript.runner.LeekFunctions;

/**
 * Chaque fonction de tableau, de map et d'ensemble au moins une fois, et chaque écriture composée sur
 * une case de map, entrée et résultat gardés vivants jusqu'à la fin : TestCommon vérifie alors la RAM
 * comptée de chacun (cf. RamInvariant).
 */
@ExtendWith(SummaryExtension.class)
public class TestRamInvariantCoverage extends TestCommon {

	private static final String F = "function(v) { return v > 1 }";
	private static final String F2 = "function(acc, v) { return acc + v }";

	private static final String[] ARRAY_CALLS = {
		"arrayChunk(a, 2)", "arrayClear(a)", "arrayConcat(a, [4, 5])", "arrayEvery(a, " + F + ")",
		"arrayFilter(a, " + F + ")", "arrayFind(a, " + F + ")", "arrayFlatten([a, [a]])", "arrayFlatten([a, [a]], 2)",
		"arrayFoldLeft(a, " + F2 + ", 0)", "arrayFoldRight(a, " + F2 + ", 0)", "arrayFrequencies([1, 1, 2])",
		"arrayGet(a, 0)", "arrayIter(a, function(v) {})", "arrayMap(a, " + F + ")", "arrayMax(a)", "arrayMin(a)",
		"arrayPartition(a, " + F + ")", "arrayRandom(a)", "arrayRandom(a, 2)", "arrayRemoveAll(a, 2)", "arrayRotate(a, 1)",
		"arraySlice(a, 1)", "arraySlice(a, 0, 2)", "arraySlice(a, 2, 0, -1)", "a[1:]", "a[::2]", "arraySome(a, " + F + ")",
		"arraySort(a)", "arraySort(a, function(x, y) { return y - x })", "arrayToSet(a)", "arrayUnique([1, 1, 2])",
		"average(a)", "count(a)", "fill(a, 0)", "fill(a, 0, 6)", "inArray(a, 2)", "insert(a, 9, 1)", "isEmpty(a)",
		"join(a, ',')", "pop(a)", "push(a, 4)", "pushAll(a, [4, 5])", "remove(a, 0)", "removeElement(a, 2)",
		"reverse(a)", "search(a, 2)", "shift(a)", "shuffle(a)", "sort(a)", "sum(a)",
		"unshift(a, 0)", "a += 4", "a += [4, 5]", "a + [4]", "a + 4", "clone(a)", "clone([a, [a]], 2)",
	};

	private static final String[] MAP_CALLS = {
		"mapAverage(m)", "mapClear(m)", "mapContains(m, 2)", "mapContainsKey(m, 1)",
		"mapEvery(m, " + F + ")", "mapFill(m, 0)", "mapFilter(m, " + F + ")", "mapFold(m, " + F2 + ", 0)",
		"mapGet(m, 1)", "mapIsEmpty(m)", "mapIter(m, function(v) {})", "mapKeys(m)", "mapKeySet(m)", "mapMap(m, " + F + ")",
		"mapMax(m)", "mapMerge(m, [3: 4, 5: 6])", "mapMin(m)", "mapPut(m, 7, 8)", "mapPutAll(m, [3: 4, 9: 9])",
		"mapRemove(m, 1)", "mapRemove(m, 42)", "mapRemoveAll(m, 2)", "mapReplace(m, 1, 5)",
		"mapReplaceAll(m, [1: 5, 42: 0])", "mapSearch(m, 2)", "mapSize(m)", "mapSome(m, " + F + ")", "mapSum(m)",
		"mapValues(m)", "m[7] = 8", "m[1] ??= 9", "m += [7: 8]", "m + [7: 8]", "clone(m)", "clone([1: m], 2)",
	};

	// Écritures composées sur une clé neuve (put_*), en dynamique puis typées (put_arithmetic_eq, put_bit_eq)
	private static final String[] MAP_WRITES = {
		"m[7]++", "++m[7]", "m[7]--", "--m[7]", "m[7] ??= 8", "m[7] += 1", "m[7] -= 1", "m[7] *= 2", "m[7] /= 2",
		"m[7] \\= 2", "m[7] %= 2", "m[7] **= 2", "m[7] &= 1", "m[7] |= 1", "m[7] ^= 1", "m[7] <<= 1", "m[7] >>= 1",
		"m[7] >>>= 1",
	};

	private static final String[] SET_CALLS = {
		"setClear(s)", "setContains(s, 2)", "setDifference(s, <2, 4>)", "setDisjunction(s, <2, 4>)",
		"setFilter(s, " + F + ")", "setIntersection(s, <2, 4>)", "setIsEmpty(s)", "setIsSubsetOf(s, <1, 2, 3, 4>)",
		"setPut(s, 4)", "setPutAll(s, <2, 4>)", "setRemove(s, 1)", "setSize(s)", "setToArray(s)", "setUnion(s, <2, 4>)",
		"setAverage(s)", "setEvery(s, " + F + ")", "setFold(s, " + F2 + ", 0)", "setIter(s, function(v) {})",
		"setMax(s)", "setMin(s)", "setSome(s, " + F + ")", "setSum(s)", "setToMap(s)",
		"s += 4", "s += <4, 5>", "s + 4", "s + <4>", "clone(s)", "clone(<s>, 2)",
	};

	/** `decl` déclare `name`, le cas rend [name, r] : l'entrée et le résultat de chaque appel restent vivants. */
	private void each(String decl, String name, String[] calls) {
		for (var call : calls) {
			// Formes libres : retirées en v5 (forme méthode), le runtime mesuré est le même
			code_v4(decl + " var r = " + call + " return [" + name + ", r]").error(Error.NONE);
		}
	}

	@Test
	public void arrays() throws Exception {
		section("RAM des fonctions de tableaux");
		each("var a = [1, 2, 3]", "a", ARRAY_CALLS);
	}

	@Test
	public void maps() throws Exception {
		section("RAM des fonctions de maps");
		each("var m = [1: 2, 3: 1]", "m", MAP_CALLS);
		each("var m = [1: 2, 3: 1]", "m", MAP_WRITES);
		for (var write : MAP_WRITES) {
			code_strict_v4_("Map<integer, integer> m = [1: 2, 3: 1] var r = " + write + " return [m, r]").error(Error.NONE);
		}
	}

	@Test
	public void sets() throws Exception {
		section("RAM des fonctions d'ensembles");
		each("var s = <1, 2, 3>", "s", SET_CALLS);
	}

	@Test
	public void classes() throws Exception {
		section("RAM des tableaux de réflexion des classes");
		for (var member : new String[] { "fields", "staticFields", "methods", "staticMethods" }) {
			code_v4_("class A { x static y m() {} static n() {} } return A." + member).error(Error.NONE);
		}
		// Une classe qui étend un conteneur garde le compteur du conteneur et un second pour ses champs
		code_v4("class A extends Array { x } var a = new A() push(a, 1) return a").error(Error.NONE);
		code_v4_("class A extends Map { x } var a = new A() a[1] = 2 return a").error(Error.NONE);
	}

	/** Une fonction v4 de tableau, de map ou d'ensemble ajoutée au registre doit rejoindre les listes ci-dessus. */
	@Test
	public void everyFunctionIsCovered() {
		var calls = Stream.of(ARRAY_CALLS, MAP_CALLS, SET_CALLS).flatMap(Stream::of).toList();
		var missing = new ArrayList<String>();
		for (var entry : LeekFunctions.getStandardFunctions().entrySet()) {
			var function = entry.getValue();
			if (!List.of("Array", "Map", "Set").contains(function.getStandardClass())) continue;
			if (function.getMinVersion() > 4 || function.getMaxVersion() < 4) continue;
			if (calls.stream().noneMatch(call -> call.startsWith(entry.getKey() + "("))) missing.add(entry.getKey());
		}
		assertEquals(List.of(), missing);
	}
}
