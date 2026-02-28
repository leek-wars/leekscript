package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestSystem extends TestCommon {


	@Test
	public void run() throws Exception {

		section("debug()");
		code("return debug(null)").equals("null");

	}
}
