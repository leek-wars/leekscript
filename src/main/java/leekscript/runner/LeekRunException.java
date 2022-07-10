package leekscript.runner;

import leekscript.common.Error;

public class LeekRunException extends Exception {

	private static final long serialVersionUID = 3760370673897923713L;

	private final Error mError;
	private Object param = null;

	public LeekRunException(Error error) {
		mError = error;
	}

	public LeekRunException(Error error, Object param) {
		mError = error;
		this.param = param;
	}

	public LeekRunException(LeekRunException e) {
		mError = e.getError();
	}

	public Error getError() {
		return mError;
	}

	public Object getParam() {
		return param;
	}

	// @Override
	// public String getMessage() {
	// 	switch (mError) {
	// 	case TOO_MUCH_OPERATIONS:
	// 		return "Erreur d'exécution : Trop d'opérations exécutées pour ce tour";
	// 	case ARRAY_EMPTY:
	// 		return "Erreur d'exécution : Tableau vide";
	// 	case INVALID_INDEX:
	// 		return "Erreur d'exécution : Indice invalide";
	// 	case UNKNOWN_FUNCTION:
	// 		return "Erreur d'exécution : Fonction inconnue";
	// 	case INVALID_LEVEL:
	// 		return "Erreur d'exécution : Niveau invalide";
	// 	case OUT_OF_MEMORY:
	// 		return "Erreur d'exécution : Trop de RAM utilisée";
	// 	case UNKNOWN_FIELD:
	// 		return "Erreur d'exécution : Champ inconnu";
	// 	case INVALID_VALUE:
	// 		return "Erreur d'exécution : Valeur invalide : " + this.param + " (class " + (this.param == null ? "null" : this.param.getClass().getSimpleName()) + ")";
	// 	}
	// 	return "Erreur d'exécution";
	// }
}
