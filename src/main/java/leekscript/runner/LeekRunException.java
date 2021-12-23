package leekscript.runner;

public class LeekRunException extends Exception {

	private static final long serialVersionUID = 3760370673897923713L;

	private final int mError;

	public final static int TOO_MUCH_OPERATIONS = 1;
	public final static int ARRAY_EMPTY = 2;
	public final static int INVALID_INDEX = 3;
	public final static int UNKNOWN_FUNCTION = 4;
	public final static int INVALID_LEVEL = 6;
	public final static int OUT_OF_MEMORY = 7;
	public final static int UNKNOWN_FIELD = 8;

	public LeekRunException(int error) {
		mError = error;
	}

	public LeekRunException(LeekRunException e) {
		mError = e.getError();
	}

	public int getError() {
		return mError;
	}

	@Override
	public String getMessage() {
		switch (mError) {
		case TOO_MUCH_OPERATIONS:
			return "Erreur d'exécution : Trop d'opérations exécutées pour ce tour";
		case ARRAY_EMPTY:
			return "Erreur d'exécution : Tableau vide";
		case INVALID_INDEX:
			return "Erreur d'exécution : Indice invalide";
		case UNKNOWN_FUNCTION:
			return "Erreur d'exécution : Fonction inconnue";
		case INVALID_LEVEL:
			return "Erreur d'exécution : Niveau invalide";
		case OUT_OF_MEMORY:
			return "Erreur d'exécution : Trop de RAM utilisée";
		case UNKNOWN_FIELD:
			return "Erreur d'exécution : Champ inconnu";
		}
		return "Erreur d'exécution";
	}
}
