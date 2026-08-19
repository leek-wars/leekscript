package leekscript.compiler;

import leekscript.common.Error;

public class LeekScriptException extends Exception {

	private static final long serialVersionUID = -5149423928011355230L;

	private final Error mType;
	private String mMessage = null;
	private String mLocation = null;
	/**
	 * Contexte de diagnostic joint à l'erreur (extrait du Java généré autour de la
	 * ligne fautive pour COMPILE_JAVA). Le message javac seul ne suffit pas : il donne
	 * la ligne cassée mais pas le code alentour, et le `ai_code` stocké avec l'erreur
	 * est RE-TÉLÉCHARGÉ après coup (cf. ErrorManager.exceptionFightAI côté worker),
	 * donc il ne correspond pas forcément au programme réellement compilé — deux bugs
	 * de codegen d'août 2026 se sont révélés non reproductibles pour cette raison.
	 */
	private String mDetails = null;

	public LeekScriptException(Error type) {
		mType = type;
	}

	public LeekScriptException(Error type, String message) {
		mType = type;
		mMessage = message;
	}

	public LeekScriptException(Error type, String message, String location) {
		mType = type;
		mMessage = message;
		mLocation = location;
	}

	public Error getType() {
		return mType;
	}

	@Override
	public String getMessage() {
		return mType.name() + " : " + mMessage;
	}

	public String getLocation() {
		return mLocation;
	}

	public String getDetails() {
		return mDetails;
	}

	public LeekScriptException details(String details) {
		mDetails = details;
		return this;
	}
}
