package leekscript.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

	public void compile(String jar) throws Exception {
		if (mStatus != INIT)
			return;
		mStatus = RUNNING;
		if (!mInput.exists()) {
			mStatus = ERROR;
			return;
		}

		String[] args = new String[] { "javac", "-encoding", "utf8", "-classpath", jar, mInput.getPath() };
		// System.out.println(String.join(" ", args));
		ProcessBuilder pb = new ProcessBuilder(args);
		Worker worker = new Worker(pb);
		Process process = worker.begin();

		Gobbler outGobbler = new Gobbler(process.getInputStream());
		Gobbler errGobbler = new Gobbler(process.getErrorStream());
		Thread outThread = new Thread(outGobbler);
		Thread errThread = new Thread(errGobbler);
		outThread.start();
		errThread.start();

		try {
			worker.join(20000);
			if (worker.exit == null) {
				throw new CompilationException("too_long_java");
			}
			outThread.join();
			errThread.join();
			process.waitFor();
			String error = errGobbler.getOuput();
			if (error.length() > 0) {
				mStatus = END;
				throw new CompilationException(error);
			}
			if (process.exitValue() == 0) {
				mStatus = END;
			}
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

	public static class Worker extends Thread {
		private final ProcessBuilder pb;
		private Process p;
		public Integer exit;
		public Worker(ProcessBuilder pb) {
			this.pb = pb;
		}
		public Process begin() {
			try {
				p = pb.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			super.start();
			return p;
		}
		public void run() {
			try {
				exit = p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class Gobbler implements Runnable {
        private BufferedReader reader;
        private StringBuilder output;
        public Gobbler(InputStream inputStream) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream));
        }
        public void run() {
            String line;
            output = new StringBuilder();
            try {
                while ((line = reader.readLine()) != null) {
					// Limit the length of the line to 500 characters
                    output.append(line.substring(0, Math.min(line.length(), 500)) + "\n");
                }
                reader.close();
            } catch (IOException e) {
				System.err.println("ERROR: " + e.getMessage());
				e.printStackTrace();
            }
        }
        public String getOuput() {
            return this.output.toString();
        }
    }
}
