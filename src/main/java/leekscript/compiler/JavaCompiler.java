package leekscript.compiler;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

public class JavaCompiler {
	
	public final static int INIT = 0;
	public final static int RUNNING = 1;
	public final static int END = 2;
	public final static int ERROR = 3;
	private final File mInput;
	private int mStatus = 0;

	private static class Worker extends Thread {
		private final Process process;
		private Integer exit;

		private Worker(Process process) {
			this.process = process;
		}

		public void run() {
			try {
				exit = process.waitFor();
			} catch (InterruptedException ignore) {
				return;
			}
		}
	}

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

		Process process = Runtime.getRuntime().exec(new String[] { "javac", "-encoding", "utf8", "-nowarn", "-classpath", "leekscript.jar", mInput.getAbsolutePath() });

		Worker worker = new Worker(process);
		worker.start();
		try {
			
			worker.join(10000);
			if (worker.exit == null) {
				throw new CompilationException("too_long_java");
			}
			
			// new
			// File(System.getProperty("user.dir")));
			boolean error = false;
			InputStream iin = process.getErrorStream();
			if (iin != null) {
				byte[] e = new byte[128];
				int nb;
				StringBuilder sb = new StringBuilder();
				while ((nb = iin.read(e)) > 0) {
					sb.append(new String(Arrays.copyOf(e, nb)));
				}
				if (sb.length() > 0)
					throw new CompilationException(sb.toString());
			}
			iin = process.getInputStream();
			if (iin != null) {
				byte[] e = new byte[128];
				int nb;
				while ((nb = iin.read(e)) > 0) {
					System.out.println(new String(Arrays.copyOf(e, nb)));
				}
			}
			process.waitFor();
			if (process.exitValue() != 0)
				error = true;
			if (!error)
				mStatus = END;
			
		} catch (InterruptedException ex) {
			worker.interrupt();
			Thread.currentThread().interrupt();
			throw ex;
		} finally {
			process.destroy();
		}
	}

	public static int getStatus() {
		return 0;
	}
}
