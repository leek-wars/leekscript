package leekscript.compiler;

import java.util.ArrayList;

public class LexicalParserTokenStream {
	private ArrayList<Token> tokens;
	private Token EOFToken;
	private int cursor = 0;

	public LexicalParserTokenStream(ArrayList<Token> tokens, Token EOFToken) {
		this.tokens = tokens;
		this.EOFToken = EOFToken;
	}

	public Token eat() {
		cursor++;
		return get(-1);
	}

	public void skip() {
		eat();
	}

	public void unskip() {
		cursor--;

		if (cursor < 0) {
			cursor = 0;
		}
	}

	public void reset() {
		cursor = 0;
	}

	public Token getEndOfFileToken() {
		return EOFToken;
	}

	public Token get() {
		return get(0);
	}

	public Token get(int offset) {
		var index = cursor + offset;
		if (index < 0 || index >= tokens.size()) {
			return EOFToken;
		}
		return tokens.get(index);
	}

	public boolean hasMoreTokens() {
		return cursor < tokens.size();
	}

	public Token atLocation(int line, int column) {
		if (tokens.isEmpty()) {
			return null;
		}

		int start = 0;
		int end = tokens.size() - 1;
		while (true) {
			var middle = (end + start) / 2;
			var token = tokens.get(middle);

			var relativePosition = token.getLocation().compare(line, column);

			if (start >= end && relativePosition != Location.RelativePosition.INSIDE) {
				return null;
			}

			if (relativePosition == Location.RelativePosition.BEFORE) {
				end = middle - 1;
			} else if (relativePosition == Location.RelativePosition.AFTER) {
				start = middle + 1;
			} else {
				return token;
			}
		}
	}

	public int getOffsetToNextClosingParenthesis() {
		int offset = 0;
		int level = 1;

		while (level > 0) {
			var token = get(offset++);

			var tokenType = token.getType();
			switch (tokenType) {
				case END_OF_FILE:
					return -1;
				case PAR_LEFT:
					level++;
					break;
				case PAR_RIGHT:
					level--;
					break;
				default:
					break;
			}
		}
		return offset - 1;
	}

	public int getOffsetToNextArrow() {
		int offset = 0;

		while (true) {
			var token = get(offset++);
			var tokenType = token.getType();

			switch (tokenType) {
				case END_OF_FILE:
					return -1;
				case ARROW:
					return offset - 1;
				default:
					break;
			}
		}
	}

	public LexicalParserTokenStreamPosition getPosition() {
		return new LexicalParserTokenStreamPosition(cursor);
	}

	public void setPosition(LexicalParserTokenStreamPosition position) {
		cursor = position.cursor;
	}

	public record LexicalParserTokenStreamPosition(int cursor) {}
}