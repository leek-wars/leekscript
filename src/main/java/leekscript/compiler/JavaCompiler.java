package leekscript.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.tools.ToolProvider;

public class JavaCompiler {

	public final static int INIT = 0;
	public final static int RUNNING = 1;
	public final static int END = 2;
	public final static int ERROR = 3;
	private final File mInput;
	private int mStatus = 0;

	public JavaCompiler(File input) {
		mInput = input;
	}

	public void compile() throws Exception {
		if (mStatus != INIT)
			return;
		mStatus = RUNNING;
		if (!mInput.exists()) {
			mStatus = ERROR;
			return;
		}

		javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		String classpath = JavaCompiler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

		compiler.run(null, System.out, System.out, "-encoding", "utf8", "-classpath", classpath, mInput.getPath());
		// System.out.println("Javac result = " + result);
	}

	public static int getStatus() {
		return 0;
	}
}
