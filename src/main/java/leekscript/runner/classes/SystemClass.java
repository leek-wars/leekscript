package leekscript.runner.classes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import leekscript.AILog;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class SystemClass {

	// Parcours de la pile Java (~1 µs), au tarif des autres natives en temps par opération
	private static final int LINE_LOOKUP_OPERATIONS = 100;

	public static long getOperations(AI ai) {
		return (long) ai.getOperations();
	}

	public static long getMaxOperations(AI ai) {
		return (long) ai.getMaxOperations();
	}

	public static long getUsedRAM(AI ai) {
		return ai.getUsedRAM() * AI.BYTES_PER_RAM_UNIT;
	}

	public static long getMaxRAM(AI ai) {
		return ai.getMaxRAM() * AI.BYTES_PER_RAM_UNIT;
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
		return df.format(ai.getDate()).toString();
	}

	public static String getTime(AI ai) {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		return df.format(ai.getDate()).toString();
	}

	public static long getTimestamp(AI ai) {
		return (ai.getDate().getTime() / 1000);
	}

	/**
	 * getSourceLine / getSourceFile prises comme valeurs (`var f = getSourceLine`) : la position de
	 * l'instruction qui les appelle, cherchée dans la pile. Appelées directement, elles sont remplacées
	 * à la compilation par leur valeur (cf. LeekFunctions.setCallSite).
	 */
	public static long getSourceLine(AI ai) throws LeekRunException {
		ai.ops(LINE_LOOKUP_OPERATIONS);
		var position = ai.getCurrentLeekScriptPosition();
		return position == null ? 0 : position.line();
	}

	public static String getSourceFile(AI ai) throws LeekRunException {
		ai.ops(LINE_LOOKUP_OPERATIONS);
		var position = ai.getCurrentLeekScriptPosition();
		return position == null ? "" : sourcePath(position.file());
	}

	/**
	 * Chemin absolu d'un fichier d'IA, tel que include() et fileRead() le comprennent. Même forme que
	 * les chemins des erreurs (cf. JavaWriter.getErrorFiles), pour que l'appel direct et l'appel à
	 * travers une variable rendent le même chemin.
	 */
	public static String sourcePath(String path) {
		path = path.replace("\\/", "/");
		return path.startsWith("/") ? path : "/" + path;
	}

	/** Identité : le compilateur passe déjà le contenu du fichier (cf. LeekFunctions.setFileArgument). */
	public static String fileRead(AI ai, String content) {
		return content;
	}
}
