package leekscript.compiler;

import java.util.ArrayList;
import leekscript.common.Error;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LexicalParser {

	public interface ErrorReporter {
		void report(AnalyzeError error) throws LeekCompilerException;
	}

	public static final String[] reservedWords = new String[] { "abstract", "arguments", "as", "await", "break", "byte", "case", "catch", "char", "class", "const", "constructor", "continue", "default", "do", "double", "else", "enum", "eval", "export", "extends", "false", "final", "finally", "float", "for", "function", "global", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface", "let", "long", "native", "new", "not", "null", "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with", "yield" };

	private ArrayList<Token> tokens = new ArrayList<>();
	private AIFile aiFile;
	private int version;
	private CharStream stream = null;

	public LexicalParser(AIFile aiFile, int version) {
		this.aiFile = aiFile;
		this.version = version;
	}

	public LexicalParserTokenStream parse(ErrorReporter error) throws LeekCompilerException {
		for (stream = new CharStream(aiFile.getCode()); stream.hasMore();) {

			if (tryParseWhiteSpaces()) continue;
			if (tryParseString()) continue;
			if (tryParseComments()) continue;
			if (tryParseNumber(error)) continue;
			if (tryParseIdentifier()) continue;
			if (tryParseOperator()) continue;
			if (tryParseBracketLike()) continue;
			if (tryParseCommaLike()) continue;

			error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
			stream.next();
		}
		return new LexicalParserTokenStream(tokens, new Token(TokenType.END_OF_FILE, "", new Location(aiFile)));
	}

	private boolean tryParseBracketLike() {
		if (tryParseExact('[', TokenType.BRACKET_LEFT)) return true;
		if (tryParseExact(']', TokenType.BRACKET_RIGHT)) return true;
		if (tryParseExact('(', TokenType.PAR_LEFT)) return true;
		if (tryParseExact(')', TokenType.PAR_RIGHT)) return true;
		if (tryParseExact('{', TokenType.ACCOLADE_LEFT)) return true;
		if (tryParseExact('}', TokenType.ACCOLADE_RIGHT)) return true;
		return false;
	}

	private boolean tryParseCommaLike() {
		if (tryParseExact(',', TokenType.VIRG)) return true;
		if (tryParseExact(';', TokenType.END_INSTRUCTION)) return true;
		return false;
	}

	private boolean tryParseOperator() {
		// Order is important, the first operator found is returned
		// So operators starting with the same characters must be ordered by length
		var operators = new String[] {
			":",
			"&&", "&=", "&",
			"||", "|=", "|",
			"++", "+=", "+",
			"--", "-=", "-",
			"**=", "**", "*=", "*",
			"/=", "/", "\\=", "\\",
			"%=", "%",
			"===", "==", "=",
			"!=", "!",
			"<<<=", "<<<", "<<=", "<<", "<=", "<",
			">>>=", ">>>", ">>=", ">>", ">=", ">",
			"^=", "^",
			"~", "@",
			"?", "\\"
		};

		if (tryParseExact("=>", TokenType.ARROW)) {
			return true;
		}
		if (tryParseExact("->", TokenType.ARROW)) {
			return true;
		}

		if (version >= 2 && tryParseExact('.', TokenType.DOT)) {
			return true;
		}

		for (var operator : operators) {
			if (tryParseExact(operator, TokenType.OPERATOR)) return true;
		}

		return false;
	}

	private boolean tryParseIdentifier() {
		var startingPoint = stream.getRestorePoint();
		for (char c = stream.peek(); stream.hasMore(); c = stream.next()) {
			if (c >= '0' && c <= '9') continue;
			if (c >= 'A' && c <= 'Z') continue;
			if (c >= 'a' && c <= 'z') continue;
			if (c >= 'À' && c <= 'Ö') continue;
			if (c >= 'à' && c <= 'ö') continue;
			if (c >= 'Ø' && c <= 'Ý') continue;
			if (c >= 'ø' && c <= 'ý') continue;
			if (c >= 'Œ' && c <= 'œ') continue;
			if (c == '_' || c == 'ÿ') continue;

			break;
		}

		if (startingPoint.equals(stream.getRestorePoint())) {
			return false;
		}

		var word = stream.getSubStringSince(startingPoint);

		if (wordEquals(word, "and")) {
			addToken("&&", TokenType.OPERATOR);
		} else if (wordEquals(word, "or")) {
			addToken("||", TokenType.OPERATOR);
		} else if (wordEquals(word, "instanceof")) {
			addToken("instanceof", TokenType.OPERATOR);
		} else if (wordEquals(word, "as")) {
			addToken("as", TokenType.OPERATOR);
		} else {
			addToken(word, TokenType.STRING);
		}

		return true;
	}

	private boolean tryParseNumber(ErrorReporter error) throws LeekCompilerException {
		var startingPoint = stream.getRestorePoint();

		if (stream.peek() < '0' || stream.peek() > '9') {
			return false;
		}

		stream.next();

		for (char c = stream.peek(); stream.hasMore(); c = stream.next()) {
			if (c >= '0' && c <= '9') continue;
			if (c >= 'A' && c <= 'Z') continue;
			if (c >= 'a' && c <= 'z') continue;
			if (c >= 'À' && c <= 'Ö') continue;
			if (c >= 'à' && c <= 'ö') continue;
			if (c >= 'Ø' && c <= 'Ý') continue;
			if (c >= 'ø' && c <= 'ý') continue;
			if (c >= 'Œ' && c <= 'œ') continue;
			if (c == '_' || c == 'ÿ') continue;
			if (c == '-' || c == '+') {
				if (stream.peek(-1) == 'e' || stream.peek(-1) == 'p') {
					continue;
				} else {
					break;
				}
			}

			if (c == '.') {
				if (stream.getSubStringSince(startingPoint).contains(".")) {
					error.report(new AnalyzeError(new Token(TokenType.NOTHING, ".", aiFile, stream.getLineCounter(), stream.getCharCounter() + 1), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
					break;
				}
				continue;
			}

			break;
		}

		addToken(stream.getSubStringSince(startingPoint), TokenType.NUMBER);
		return true;
	}

	private boolean tryParseString() {
		var startingPoint = stream.getRestorePoint();

		var openQuote = stream.peek();
		if (openQuote != '"' && openQuote != '\'') {
			return false;
		}

		stream.next();

		var escaped = false;
		for (char c = stream.peek(); stream.hasMore(); c = stream.next()) {
			if (c == '\\') {
				escaped = !escaped;
				continue;
			}
			if (c == openQuote && !escaped) {
				stream.next();
				break;
			}
			escaped = false;
		}

		addToken(stream.getSubStringSince(startingPoint), TokenType.VAR_STRING);
		return true;
	}

	private boolean tryParseWhiteSpaces() {
		if ("\r\n\t ".contains("" + stream.peek()) || stream.peek() == 160) {
			stream.next();
			return true;
		}
		return false;
	}

	private boolean tryParseComments() {
		if (stream.peek() == '/' && stream.peek(1) == '/') {
			while (stream.hasMore() && stream.peek() != '\n') stream.next();
			stream.next();
			return true;
		}

		if (stream.peek() == '/' && stream.peek(1) == '*') {
			stream.next();
			stream.next();

			if (version < 2 && stream.peek() == '/') {
				stream.next();
				return true;
			}

			while (stream.hasMore() && (stream.peek() != '*' || stream.peek(1) != '/')) stream.next();
			stream.next();
			stream.next();
			return true;
		}

		return false;
	}

	private boolean tryParseExact(String expected, TokenType type) {
		var startingPoint = stream.getRestorePoint();

		for (int i = 0; i < expected.length(); i++) stream.next();

		var streamWord = stream.getSubStringSince(startingPoint);
		if (wordEquals(streamWord, expected)) {
			addToken(expected, type);
			return true;
		}

		stream.restore(startingPoint);
		return false;
	}

	private boolean tryParseExact(char expected, TokenType type) {
		if (stream.peek() == expected) {
			addToken("" + expected, type);
			stream.next();
			return true;
		}
		return false;
	}

	private void addToken(String word, TokenType type) {
		tokens.add(new Token(type, word, aiFile, stream.getLineCounter(), stream.getCharCounter()));
	}

	private boolean wordEquals(String word, String expected) {
		if (version <= 2) {
			return word.equalsIgnoreCase(expected);
		}
		return word.equals(expected);
	}

	private record CharStreamRestorePoint(int index, int lineCounter, int charCounter) {
	}

	private class CharStream {
		private int lineCounter = 1;
		private int charCounter = 1;
		private int index = 0;
		private String content;

		public CharStream(String content) {
			this.content = content;
		}

		public int getLineCounter() {
			return lineCounter;
		}

		public int getCharCounter() {
			return charCounter;
		}

		public boolean hasMore() {
			return index < content.length();
		}

		public char next() {
			if (index >= content.length()) return 0;

			if (peek() == '\n') {
				lineCounter++;
				charCounter = 1;
			} else {
				charCounter++;
			}

			index++;

			return peek();
		}

		public char peek(int offset) {
			if (index + offset >= content.length() || index + offset < 0) return 0;
			return content.charAt(index + offset);
		}

		public char peek() {
			return peek(0);
		}

		public CharStreamRestorePoint getRestorePoint() {
			return new CharStreamRestorePoint(index, lineCounter, charCounter);
		}

		public void restore(CharStreamRestorePoint restorePoint) {
			index = restorePoint.index;
			lineCounter = restorePoint.lineCounter;
			charCounter = restorePoint.charCounter;
		}

		public String getSubStringSince(CharStreamRestorePoint restorePoint) {
			return content.substring(restorePoint.index, index);
		}
	}

}