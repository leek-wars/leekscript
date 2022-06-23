package test;

public class TestFiles extends TestCommon {

	public void run() {

		/** Complex codes */
		header("Files");
		section("General");
		file_v2_("ai/code/primes.leek").equals("78498");
		// DISABLED_file("test/code/primes_gmp.leek").equals("9591");
		file("ai/code/gcd.leek").equals("151");
		file("ai/code/strings.leek").equals("true");
		file("ai/code/reachable_cells.leek").equals("383");
		file("ai/code/reachable_cells.leek").equals("383");
		file("ai/code/reachable_cells_variant_1.leek").equals("400");
		file("ai/code/reachable_cells_variant_2.leek").equals("481");
		file("ai/code/reachable_cells_variant_3.leek").equals("16");
		file("ai/code/reachable_cells_variant_4.leek").equals("2");
		file("ai/code/reachable_cells_variant_5.leek").equals("5");
		file("ai/code/reachable_cells_variant_6.leek").equals("4");
		file("ai/code/reachable_cells_variant_7.leek").equals("5");
		file("ai/code/reachable_cells_variant_8.leek").equals("null");
		file("ai/code/reachable_cells_variant_9.leek").equals("null");
		file("ai/code/reachable_cells_variant_10.leek").equals("null");
		file("ai/code/french.leek").equals("\"cent-soixante-huit millions quatre-cent-quatre-vingt-neuf-mille-neuf-cent-quatre-vingt-dix-neuf\"");
		// DISABLED_file("test/code/break_and_continue.leek").equals("2504");
		file("ai/code/french.min.leek").equals("\"neuf-cent-quatre-vingt-sept milliards six-cent-cinquante-quatre millions trois-cent-vingt-et-un-mille-douze\"");
		// file("test/code/quine.leek").quine();
		// file_v1("test/code/quine_zwik.leek").quine();
		// file("test/code/dynamic_operators").works();
		// file("ai/code/euler1.leek").equals("2333316668");
		// DISABLED_file("ai/code/text_analysis.leek").equals("[3, 47, 338]");
		// DISABLED_file("ai/code/divisors.leek").equals("[1, 3, 9, 13, 17, 39]");
		file_v2_("ai/code/two_functions.leek").equals("[{p: 2, v: 5}, [{p: 3, v: 6}]]");
		// file("test/code/product_n.leek").equals("5040");
		// file("test/code/product_n_return.leek").equals("265252859812191058636308480000000");
		// file("test/code/product_n_arrays.leek").equals("[5040]");
		// file("test/code/product_coproduct.leek").equals("171122452428141311372468338881272839092270544893520369393648040923257279754140647424000000000000000");
		file_v2_("ai/code/fold_left.leek").equals("[{w: 1}, {w: 3}, {w: 4}, {w: 2}, {w: 7}, {w: 5}, {w: 8}, {w: 9}, {w: 6}]");
		// file("test/code/fold_left_2.leek").equals("{p: 6, v: {p: 9, v: {p: 8, v: {p: 5, v: {p: 7, v: { ... }}}}}}");
		// file("test/code/fold_right.leek").equals("[{w: 6}, {w: 9}, {w: 8}, {w: 5}, {w: 7}, {w: 2}, {w: 4}, {w: 3}, {w: 1}]");
		// file("test/code/fold_right_2.leek").equals("{p: {p: {p: {p: {p: { ... }, v: 7}, v: 2}, v: 4}, v: 3}, v: 1}");
		file("ai/code/assignments.leek").equals("15");
		file("ai/code/recursive_2_vars.leek").equals("1021");
		file("ai/code/global_functions_1.leek").equals("false");
		file("ai/code/global_functions_2.leek").equals("[false, true]");
		file("ai/code/recursive_2_functions.leek").equals("10");
		// DISABLED_file("test/code/recursive_2_versions.leek").equals("9.5");
		// DISABLED_file("test/code/swap.leek").equals("[{p: 1}, {p: 3}, {p: 4}, {p: 12}, {p: 5}]");
		file_v2_("ai/code/classes_simple.leek").equals("[\"Ferrari\", \"Maserati\", \"Lamborghini\"]");
		file_v2_("ai/code/classes_multiple.leek").equals("[4, 40, 80]");
		// DISABLED_file("test/code/match.leek").output("Yeah!\n");
		file("ai/code/fibonacci.leek").equals("832040");
		// file("ai/code/fibonacci_long.leek").equals("1346269");
		// file("ai/code/pow5.leek").equals("6938893903907228377647697925567626953125");
		file("ai/code/tarai.leek").equals("16");
		file("ai/code/return_in_function.leek").equals("2");
	}
}
