package leekscript.runner;

public class LeekRunException extends Exception {

	private static final long serialVersionUID = 3760370673897923713L;

	private final int mError;

	public final static int TOO_MUCH_OPERATIONS = 1;
	public final static int ARRAY_EMPTY = 2;
	public final static int INVALID_INDEX = 3;
	public final static int UNKNOWN_FUNCTION = 4;
	public final static int INVALID_OPERATOR = 5;
	public final static int INVALID_LEVEL = 6;
	public final static int OUT_OF_MEMORY = 7;

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
			return "Erreur d'execution : Trop d'opérations éxécutées pour ce tour";
		case ARRAY_EMPTY:
			return "Erreur d'execution : Tableau vide";
		case INVALID_INDEX:
			return "Erreur d'execution : Indice invalide";
		case UNKNOWN_FUNCTION:
			return "Erreur d'execution : Fonction inconnue";
		case INVALID_OPERATOR:
			return "Erreur d'execution : Impossible d'utiliser cet opérateur";
		case INVALID_LEVEL:
			return "Erreur d'execution : Niveau invalide";
		case OUT_OF_MEMORY:
			return "Erreur d'execution : Trop de RAM utilisée";

		}
		return "Erreur d'éxécution";
	}
}
