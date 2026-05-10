package leekscript.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LexicalParserTokenStream {
	private ArrayList<Token> tokens;
	private Token EOFToken;
	private int cursor = 0;

	public LexicalParserTokenStream(ArrayList<Token> tokens, Token EOFToken) {
		this.tokens = tokens;
		this.EOFToken = EOFToken;
	}

	public Token eat() {
		// Lit le token courant puis avance — évite get(-1) qui re-fait l'addition
		// et le bounds check du cursor + (-1).
		Token t;
		if (cursor < tokens.size()) {
			t = tokens.get(cursor);
		} else {
			t = EOFToken;
		}
		cursor++;
		return t;
	}

	public void skip() {
		cursor++;
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
		// Variante sans offset : skip l'addition et le check negative-index.
		// Hot path appelé des centaines de milliers de fois pendant firstPass+secondPass.
		return cursor < tokens.size() ? tokens.get(cursor) : EOFToken;
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

	public List<Token> getTokens() {
		return Collections.unmodifiableList(tokens);
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

	/**
	 * Combine getOffsetToNextArrow + getOffsetToNextClosingParenthesis en un seul
	 * forward scan : true ssi un ARROW apparaît avant un PAR_RIGHT non balancé
	 * (en partant à level=1). Sémantique équivalente à
	 *   a != -1 && (a < p || p == -1)
	 * mais la nouvelle version termine au premier `)` non matché — l'ancienne
	 * scannait toujours jusqu'à EOF si ni arrow ni `)` non matché n'existaient,
	 * ce qui rendait `var x = ...;` au top-level O(taille du fichier) par déclaration.
	 */
	public boolean isArrowFunctionAhead() {
		int level = 1;
		int n = tokens.size();
		var ts = tokens;
		for (int i = cursor; i < n; i++) {
			TokenType type = ts.get(i).getType();
			if (type == TokenType.ARROW) return true;
			if (type == TokenType.PAR_LEFT) level++;
			else if (type == TokenType.PAR_RIGHT) {
				if (--level == 0) return false;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Tokens current=" + get();
	}

	public LexicalParserTokenStreamPosition getPosition() {
		return new LexicalParserTokenStreamPosition(cursor);
	}

	public void setPosition(LexicalParserTokenStreamPosition position) {
		cursor = position.cursor;
	}

	public record LexicalParserTokenStreamPosition(int cursor) {}
}