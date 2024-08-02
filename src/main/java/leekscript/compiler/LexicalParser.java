package leekscript.compiler;

import java.util.ArrayList;
import leekscript.common.Error;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LexicalParser {

	public interface ErrorReporter {
		void report(AnalyzeError error) throws LeekCompilerException;
	}

	public static final String[] reservedWords = new String[] { "abstract", "and", "as", "await", "break", "byte", "case", "catch", "char", "class", "const", "constructor", "continue", "default", "do", "double", "else", "enum", "eval", "export", "extends", "false", "final", "finally", "float", "for", "function", "global", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface", "let", "long", "native", "new", "not", "null", "or", "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with", "xor", "yield" };

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
			if (tryParseString(error)) continue;
			if (tryParseComments()) continue;
			if (tryParseNumber(error)) continue;
			if (tryParseSpecialIdentifier()) continue;
			if (tryParseIdentifier()) continue;
			if (tryParseOperator()) continue;
			if (tryParseBracketLike()) continue;
			if (tryParseCommaLike()) continue;

			error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
			stream.next();
		}
		return new LexicalParserTokenStream(tokens, new Token(TokenType.END_OF_FILE, "", new Location(aiFile, stream.getLineCounter(), stream.getCharCounter())));
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
			"!==", "!=", "!",
			"<<<=", "<<<", "<<=", "<<", "<=", "<",
			">>>=", /* ">>>", ">>=", ">>", */ ">=", ">",
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

		if (tryParseExact("..", TokenType.DOT_DOT)) {
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

	private boolean tryParseSpecialIdentifier() {

		var c = stream.peek();
		if (c == '∞') {
			stream.next();
			addToken("∞", TokenType.LEMNISCATE);
			return true;
		} else if (c == 'π') {
			stream.next();
			addToken("π", TokenType.PI);
			return true;
		}
		return false;
	}

	private boolean tryParseIdentifier() {
		var startingPoint = stream.index;
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

		if (startingPoint == stream.index) {
			return false;
		}

		var word = stream.getSubStringSince(startingPoint);

		if (wordEquals(word, "and")) {
			addToken("&&", TokenType.OPERATOR);
		} else if (wordEquals(word, "or")) {
			addToken("||", TokenType.OPERATOR);
		} else if (wordEquals(word, "xor")) {
			addToken(word, TokenType.OPERATOR);
		} else if (version >= 2 && wordEquals(word, "instanceof")) {
			addToken(word, TokenType.OPERATOR);
		} else if (wordEquals(word, "as")) {
			addToken(word, TokenType.AS);
		} else if (wordEquals(word, "var")) {
			addToken(word, TokenType.VAR);
		} else if (wordEquals(word, "global")) {
			addToken(word, TokenType.GLOBAL);
		} else if (wordEquals(word, "return")) {
			addToken(word, TokenType.RETURN);
		} else if (version >= 2 && wordEquals(word, "constructor")) {
			addToken(word, TokenType.CONSTRUCTOR);
		} else if (version >= 3 && wordEquals(word, "final")) {
			addToken(word, TokenType.FINAL);
		} else if (wordEquals(word, "for")) {
			addToken(word, TokenType.FOR);
		} else if (wordEquals(word, "if")) {
			addToken(word, TokenType.IF);
		} else if (wordEquals(word, "while")) {
			addToken(word, TokenType.WHILE);
		} else if (version >= 2 && wordEquals(word, "static")) {
			addToken(word, TokenType.STATIC);
		} else if (wordEquals(word, "in")) {
			addToken(word, TokenType.IN);
		} else if (version >= 3 && wordEquals(word, "abstract")) {
			addToken(word, TokenType.ABSTRACT);
		} else if (version >= 3 && wordEquals(word, "await")) {
			addToken(word, TokenType.AWAIT);
		} else if (wordEquals(word, "break")) {
			addToken(word, TokenType.BREAK);
		} else if (wordEquals(word, "continue")) {
			addToken(word, TokenType.CONTINUE);
		} else if (version >= 3 && wordEquals(word, "import")) {
			addToken(word, TokenType.IMPORT);
		} else if (version >= 3 && wordEquals(word, "export")) {
			addToken(word, TokenType.EXPORT);
		} else if (version >= 3 && wordEquals(word, "goto")) {
			addToken(word, TokenType.GOTO);
		} else if (version >= 3 && wordEquals(word, "switch")) {
			addToken(word, TokenType.GOTO);
		} else if (version >= 2 && wordEquals(word, "super")) {
			addToken(word, TokenType.SUPER);
		} else if (version >= 2 && word.equals("class")) {
			addToken(word, TokenType.CLASS);
		} else if (version >= 3 && wordEquals(word, "catch")) {
			addToken(word, TokenType.CATCH);
		} else if (version >= 2 && wordEquals(word, "extends")) {
			addToken(word, TokenType.EXTENDS);
		} else if (wordEquals(word, "true")) {
			addToken(word, TokenType.TRUE);
		} else if (wordEquals(word, "false")) {
			addToken(word, TokenType.FALSE);
		} else if (version >= 3 && wordEquals(word, "const")) {
			addToken(word, TokenType.CONST);
		} else if (version >= 3 && wordEquals(word, "char")) {
			addToken(word, TokenType.CHAR);
		} else if (version >= 3 && wordEquals(word, "enum")) {
			addToken(word, TokenType.ENUM);
		} else if (version >= 3 && wordEquals(word, "eval")) {
			addToken(word, TokenType.EVAL);
		} else if (version >= 3 && wordEquals(word, "case")) {
			addToken(word, TokenType.CASE);
		} else if (version >= 3 && wordEquals(word, "float")) {
			addToken(word, TokenType.FLOAT);
		} else if (version >= 3 && wordEquals(word, "double")) {
			addToken(word, TokenType.DOUBLE);
		} else if (version >= 3 && wordEquals(word, "byte")) {
			addToken(word, TokenType.BYTE);
		} else if (wordEquals(word, "do")) {
			addToken(word, TokenType.DO);
		} else if (version >= 3 && wordEquals(word, "try")) {
			addToken(word, TokenType.TRY);
		} else if (version >= 3 && wordEquals(word, "void")) {
			addToken(word, TokenType.VOID);
		} else if (version >= 3 && wordEquals(word, "with")) {
			addToken(word, TokenType.WITH);
		} else if (version >= 3 && wordEquals(word, "yield")) {
			addToken(word, TokenType.YIELD);
		} else if (version >= 3 && wordEquals(word, "finally")) {
			addToken(word, TokenType.FINALLY);
		} else if (version >= 3 && wordEquals(word, "interface")) {
			addToken(word, TokenType.INTERFACE);
		} else if (version >= 3 && wordEquals(word, "long")) {
			addToken(word, TokenType.LONG);
		} else if (version >= 3 && wordEquals(word, "let")) {
			addToken(word, TokenType.LET);
		} else if (version >= 3 && wordEquals(word, "native")) {
			addToken(word, TokenType.NATIVE);
		} else if (version >= 2 && wordEquals(word, "new")) {
			addToken(word, TokenType.NEW);
		} else if (version >= 3 && wordEquals(word, "package")) {
			addToken(word, TokenType.PACKAGE);
		} else if (version >= 2 && wordEquals(word, "this")) {
			addToken(word, TokenType.THIS);
		} else if (wordEquals(word, "function")) {
			addToken(word, TokenType.FUNCTION);
		} else if (version >= 3 && wordEquals(word, "implements")) {
			addToken(word, TokenType.IMPLEMENTS);
		} else if (version >= 3 && wordEquals(word, "int")) {
			addToken(word, TokenType.INT);
		} else if (wordEquals(word, "not")) {
			addToken(word, TokenType.NOT);
		} else if (wordEquals(word, "null")) {
			addToken(word, TokenType.NULL);
		} else if (version >= 2 && wordEquals(word, "private")) {
			addToken(word, TokenType.PRIVATE);
		} else if (version >= 2 && wordEquals(word, "protected")) {
			addToken(word, TokenType.PROTECTED);
		} else if (version >= 2 && wordEquals(word, "public")) {
			addToken(word, TokenType.PUBLIC);
		} else if (version >= 3 && wordEquals(word, "short")) {
			addToken(word, TokenType.SHORT);
		} else if (wordEquals(word, "else")) {
			addToken(word, TokenType.ELSE);
		} else if (wordEquals(word, "include")) {
			addToken(word, TokenType.INCLUDE);
		} else if (version >= 3 && wordEquals(word, "throws")) {
			addToken(word, TokenType.THROWS);
		} else if (version >= 3 && wordEquals(word, "throw")) {
			addToken(word, TokenType.THROW);
		} else if (version >= 3 && wordEquals(word, "transient")) {
			addToken(word, TokenType.THROWS);
		} else if (version >= 3 && wordEquals(word, "volatile")) {
			addToken(word, TokenType.VOLATILE);
		} else if (version >= 3 && wordEquals(word, "default")) {
			addToken(word, TokenType.DEFAULT);
		} else if (version >= 3 && wordEquals(word, "synchronized")) {
			addToken(word, TokenType.SYNCHRONIZED);
		} else if (version >= 3 && wordEquals(word, "typeof")) {
			addToken(word, TokenType.TYPEOF);
		} else {
			addToken(word, TokenType.STRING);
		}

		return true;
	}

	private boolean tryParseNumber(ErrorReporter error) throws LeekCompilerException {

		if (stream.peek() < '0' || stream.peek() > '9') {
			return false;
		}

		var startingPoint = stream.index;

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
				// We don't eat the dot if it's followed by another dot
				if (stream.peek(1) == '.') {
					break;
				}

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

	private boolean tryParseString(ErrorReporter error) throws LeekCompilerException {

		var openQuote = stream.peek();
		if (openQuote != '"' && openQuote != '\'') {
			return false;
		}

		var startingPoint = stream.index;

		stream.next();

		var escaped = false;
		var closed = false;
		for (char c = stream.peek(); stream.hasMore(); c = stream.next()) {
			if (c == '\\') {
				escaped = !escaped;
				continue;
			}
			if (c == openQuote && !escaped) {
				closed = true;
				stream.next();
				break;
			}
			escaped = false;
		}

		if (closed) {
			addToken(stream.getSubStringSince(startingPoint), TokenType.VAR_STRING);
			return true;
		} else {
			error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.STRING_NOT_CLOSED));
			return false;
		}
	}

	private boolean tryParseWhiteSpaces() {
		var c = stream.peek();
		if (c == ' ' || c == '\r' || c == '\n' || c == '\t' || c == 160) {
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
		if (stream.index + expected.length() <= stream.content.length()) {
			boolean allGood = true;
			for (int i = 0; i < expected.length(); i++) {
				if (!charEquals(stream.peek(i), expected.charAt(i))) {
					allGood = false;
					break;
				}
			}
			if (allGood) {
				stream.index += expected.length();
				stream.charCounter += expected.length();
				if (stream.index < stream.content.length()) {
					stream.c = stream.content.charAt(stream.index);
				}
				addToken(expected, type);
				return true;
			}
		}
		return false;
	}

	private boolean tryParseExact(char expected, TokenType type) {
		if (charEquals(stream.peek(), expected)) {
			stream.next();
			addToken("" + expected, type);
			return true;
		}
		return false;
	}

	private void addToken(String word, TokenType type) {
		// System.out.println("addToken " + word + " " + type + " " + stream.getLineCounter() + " " + stream.getCharCounter());
		tokens.add(new Token(type, word, aiFile, stream.getLineCounter(), stream.getCharCounter()));
	}

	private boolean wordEquals(String word, String expected) {
		if (version <= 2) {
			return word.equalsIgnoreCase(expected);
		}
		return word.equals(expected);
	}

	private boolean charEquals(char c, char expected) {
		if (version <= 2) {
			return Character.toLowerCase(c) == Character.toLowerCase(expected);
		}
		return c == expected;
	}

	private class CharStream {

		private int lineCounter = 1;
		private int charCounter = 0;
		private int index = 0;
		private String content;
		private char c;

		public CharStream(String content) {
			this.content = content;
			this.c = content.length() > 0 ? content.charAt(0) : 0;
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

			if (c == '\n') {
				lineCounter++;
				charCounter = 0;
			} else {
				charCounter++;
			}

			index++;
			if (index < content.length()) {
				c = content.charAt(index);
			}
			return c;
		}

		public char peek(int offset) {
			if (index + offset >= content.length()) return 0;
			return content.charAt(index + offset);
		}

		public char peek() {
			return c;
		}

		public String getSubStringSince(int start) {
			return content.substring(start, index);
		}
	}
}
