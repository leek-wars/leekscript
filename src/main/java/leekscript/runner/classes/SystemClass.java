package leekscript.runner.classes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class SystemClass {

	// Leek Wars game time is canonically Europe/Paris. Pinning the TZ here
	// ensures getDate/getTime return identical strings regardless of the
	// worker's host TZ, so replays don't diverge cross-machine.
	private static final TimeZone GAME_TZ = TimeZone.getTimeZone("Europe/Paris");

	public static long getOperations(AI ai) {
		return (long) ai.getOperations();
	}

	public static long getMaxOperations(AI ai) {
		return (long) ai.getMaxOperations();
	}

	public static long getUsedRAM(AI ai) {
		return (long) ai.getUsedRAM() * 8;
	}

	public static long getMaxRAM(AI ai) {
		return (long) ai.getMaxRAM() * 8;
	}

	public static long getInstructionsCount(AI ai) {
		return 0l;
	}

	public static Object debug(AI ai, Object value) throws LeekRunException {
		String message = ai.string(value);
		ai.getLogs().addLog(AILog.STANDARD, message);
		ai.ops(message.length());
		return null;
	}

	public static Object debugW(AI ai, Object value) throws LeekRunException {
		String message = ai.string(value);
		ai.getLogs().addLog(AILog.WARNING, message);
		ai.ops(message.length());
		return null;
	}

	public static Object debugE(AI ai, Object value) throws LeekRunException {
		String message = ai.string(value);
		ai.getLogs().addLog(AILog.ERROR, message);
		ai.ops(message.length());
		return null;
	}

	public static Object debugC(AI ai, Object value, long color) throws LeekRunException {
		String message = ai.string(value);
		ai.getLogs().addLog(AILog.STANDARD, message, (int) color);
		ai.ops(message.length());
		return null;
	}

	public static String getDate(AI ai) {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		df.setTimeZone(GAME_TZ);
		return df.format(ai.getDate()).toString();
	}

	public static String getTime(AI ai) {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		df.setTimeZone(GAME_TZ);
		return df.format(ai.getDate()).toString();
	}

	public static long getTimestamp(AI ai) {
		return (ai.getDate().getTime() / 1000);
	}
}
