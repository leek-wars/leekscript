package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestPragmas extends TestCommon {

	@Test
	public void run() throws Exception {

		section("Pragmas");

		// Valid pragmas should not produce errors
		code("// @version:4\nreturn 1;").equals("1");
		code("// @strict\nvar x = 1; return x;").equals("1");
		code("// @version:4\n// @strict\nvar x = 1; return x;").equals("1");

		// Pragma anywhere in file
		code("var x = 1;\n// @strict\nreturn x;").equals("1");

		// Duplicate pragmas are errors
		code("// @version:4\n// @version:3\nreturn 1;").error(Error.PRAGMA_DUPLICATE);
		code("// @strict\n// @strict\nreturn 1;").error(Error.PRAGMA_DUPLICATE);

		// Unknown pragma is a warning
		code("// @foo\nreturn 1;").warning(Error.PRAGMA_UNKNOWN);
		code("// @bar:42\nreturn 1;").warning(Error.PRAGMA_UNKNOWN);

		// Invalid values are errors
		code("// @version:abc\nreturn 1;").error(Error.PRAGMA_INVALID_VALUE);
		code("// @version:99\nreturn 1;").error(Error.PRAGMA_INVALID_VALUE);
		code("// @version:0\nreturn 1;").error(Error.PRAGMA_INVALID_VALUE);
		code("// @version\nreturn 1;").error(Error.PRAGMA_INVALID_VALUE);
		code("// @strict:true\nreturn 1;").error(Error.PRAGMA_INVALID_VALUE);

		// Non-pragma comments should not interfere
		code("// just a comment\nreturn 1;").equals("1");
		code("// @ not a pragma\nreturn 1;").equals("1");
		code("/* @version:3 */ return 1;").equals("1");
	}
}
