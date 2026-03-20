package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestOperations extends TestCommon {


		@Test
	public void testInit() throws Exception {
		header("Operations");
	}

	@Test
	public void testOperators() throws Exception {
		section("Operators");
		code("1").ops(0);
		code("1 + 1").ops(1);
		code("1 - 1").ops(1);
		code("2 * 2").ops(2);
		code("2 / 2").ops(5);
		code("2 \\ 2").ops(5);
		code("2 % 2").ops(5);
	}

	@Test
	public void testConditions() throws Exception {
		section("Conditions");
		code("if (1) {}").ops(1);
	}

	@Test
	public void testBoolean_operators() throws Exception {
		section("Boolean operators");
		code("1 or 2").ops(1);
		code("1 and 2").ops(1);
		code("(1 + 1) or (2 + 2)").ops(2);
		code("(1 + 1) and (2 + 2)").ops(3);
		code("(1 + 1) or (2 + 2) or (3 + 3)").ops(2);
		code("(1 + 1) and (2 + 2) and (3 + 3)").ops(5);
	}

}
