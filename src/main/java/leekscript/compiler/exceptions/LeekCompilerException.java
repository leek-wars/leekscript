package leekscript.compiler.exceptions;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.common.Error;

public class LeekCompilerException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	int mLine;
	int mChar;
	IAWord mWord;
	Error mError;
	AIFile<?> mIA;
	private String[] mParameters = null;

	public LeekCompilerException(IAWord word, Error error) {
		mLine = word.getLine();
		mChar = word.getCharacter();
		mWord = word;
		mIA = word.getAI();
		mError = error;
	}

	public LeekCompilerException(IAWord word, Error error, String[] parameters) {
		mLine = word.getLine();
		mChar = word.getCharacter();
		mWord = word;
		mIA = word.getAI();
		mError = error;
		mParameters = parameters;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public String getString() {
		return mWord.getWord();
	}

	public IAWord getWord() {
		return mWord;
	}

	public int getLine() {
		return mLine;
	}

	public int getChar() {
		return mChar;
	}

	@Override
	public String getMessage() {
		return mIA.getPath() + ":" + mLine + " : " + mWord + " : " + mError.name();
	}

	public Error getError() {
		return mError;
	}

	public AIFile<?> getIA() {
		return mIA;
	}
}
