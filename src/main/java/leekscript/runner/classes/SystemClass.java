package leekscript.runner.classes;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class SystemClass {

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
}
