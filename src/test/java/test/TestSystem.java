package test;

public class TestSystem extends TestCommon {

	public void run() {

		section("debug()");
		code("return debug(null)").equals("null");
	}
}
