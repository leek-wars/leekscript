package leekscript;

import java.time.Duration;

public class Util {


	public static String formatDurationNanos(long t) {
		return (t / 1_000_000) + " ms";
		// return Duration.ofNanos(t).toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").replaceAll("\\.\\d+", "").toLowerCase();
	}
}
