package leekscript.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import leekscript.common.Error;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;

public class LexicalParser {

	/**
	 * Métadonnées d'un keyword pour le dispatch rapide dans tryParseIdentifier.
	 * minVersion : version minimum à partir de laquelle le mot est un keyword.
	 * emit : valeur passée à addToken (null = utilise le mot original ; non-null
	 *        pour les remappings comme "and" → "&&").
	 */
	private static record KeywordInfo(int minVersion, String emit, TokenType type) {}

	// Map case-sensitive (v3+). Pour v1-2 on lowercase d'abord (alloc) — le
	// fallback d'origine est conservé si tu préfères, mais le HashMap a 70
	// entrées donc le coût est négligeable.
	private static final Map<String, KeywordInfo> KEYWORDS = buildKeywords();

	private static Map<String, KeywordInfo> buildKeywords() {
		var m = new HashMap<String, KeywordInfo>(128);
		m.put("and",         new KeywordInfo(1, "&&", TokenType.OPERATOR));
		m.put("or",          new KeywordInfo(1, "||", TokenType.OPERATOR));
		m.put("xor",         new KeywordInfo(1, null, TokenType.OPERATOR));
		m.put("instanceof",  new KeywordInfo(2, null, TokenType.OPERATOR));
		m.put("as",          new KeywordInfo(1, null, TokenType.AS));
		m.put("var",         new KeywordInfo(1, null, TokenType.VAR));
		m.put("global",      new KeywordInfo(1, null, TokenType.GLOBAL));
		m.put("return",      new KeywordInfo(1, null, TokenType.RETURN));
		m.put("constructor", new KeywordInfo(2, null, TokenType.CONSTRUCTOR));
		m.put("final",       new KeywordInfo(3, null, TokenType.FINAL));
		m.put("for",         new KeywordInfo(1, null, TokenType.FOR));
		m.put("if",          new KeywordInfo(1, null, TokenType.IF));
		m.put("while",       new KeywordInfo(1, null, TokenType.WHILE));
		m.put("static",      new KeywordInfo(2, null, TokenType.STATIC));
		m.put("in",          new KeywordInfo(1, null, TokenType.IN));
		m.put("abstract",    new KeywordInfo(3, null, TokenType.ABSTRACT));
		m.put("await",       new KeywordInfo(3, null, TokenType.AWAIT));
		m.put("break",       new KeywordInfo(1, null, TokenType.BREAK));
		m.put("continue",    new KeywordInfo(1, null, TokenType.CONTINUE));
		m.put("import",      new KeywordInfo(3, null, TokenType.IMPORT));
		m.put("export",      new KeywordInfo(3, null, TokenType.EXPORT));
		m.put("goto",        new KeywordInfo(3, null, TokenType.GOTO));
		m.put("switch",      new KeywordInfo(3, null, TokenType.SWITCH));
		m.put("super",       new KeywordInfo(2, null, TokenType.SUPER));
		m.put("class",       new KeywordInfo(2, null, TokenType.CLASS));
		m.put("catch",       new KeywordInfo(3, null, TokenType.CATCH));
		m.put("extends",     new KeywordInfo(2, null, TokenType.EXTENDS));
		m.put("true",        new KeywordInfo(1, null, TokenType.TRUE));
		m.put("false",       new KeywordInfo(1, null, TokenType.FALSE));
		m.put("const",       new KeywordInfo(3, null, TokenType.CONST));
		m.put("char",        new KeywordInfo(3, null, TokenType.CHAR));
		m.put("enum",        new KeywordInfo(3, null, TokenType.ENUM));
		m.put("eval",        new KeywordInfo(3, null, TokenType.EVAL));
		m.put("case",        new KeywordInfo(3, null, TokenType.CASE));
		m.put("float",       new KeywordInfo(3, null, TokenType.FLOAT));
		m.put("double",      new KeywordInfo(3, null, TokenType.DOUBLE));
		m.put("byte",        new KeywordInfo(3, null, TokenType.BYTE));
		m.put("do",          new KeywordInfo(1, null, TokenType.DO));
		m.put("try",         new KeywordInfo(3, null, TokenType.TRY));
		m.put("void",        new KeywordInfo(3, null, TokenType.VOID));
		m.put("with",        new KeywordInfo(3, null, TokenType.WITH));
		m.put("yield",       new KeywordInfo(3, null, TokenType.YIELD));
		m.put("finally",     new KeywordInfo(3, null, TokenType.FINALLY));
		m.put("interface",   new KeywordInfo(3, null, TokenType.INTERFACE));
		m.put("long",        new KeywordInfo(3, null, TokenType.LONG));
		m.put("let",         new KeywordInfo(3, null, TokenType.LET));
		m.put("native",      new KeywordInfo(3, null, TokenType.NATIVE));
		m.put("new",         new KeywordInfo(2, null, TokenType.NEW));
		m.put("package",     new KeywordInfo(3, null, TokenType.PACKAGE));
		m.put("this",        new KeywordInfo(2, null, TokenType.THIS));
		m.put("function",    new KeywordInfo(1, null, TokenType.FUNCTION));
		m.put("implements",  new KeywordInfo(3, null, TokenType.IMPLEMENTS));
		m.put("int",         new KeywordInfo(3, null, TokenType.INT));
		m.put("not",         new KeywordInfo(1, null, TokenType.NOT));
		m.put("null",        new KeywordInfo(1, null, TokenType.NULL));
		m.put("private",     new KeywordInfo(2, null, TokenType.PRIVATE));
		m.put("protected",   new KeywordInfo(2, null, TokenType.PROTECTED));
		m.put("public",      new KeywordInfo(2, null, TokenType.PUBLIC));
		m.put("short",       new KeywordInfo(3, null, TokenType.SHORT));
		m.put("else",        new KeywordInfo(1, null, TokenType.ELSE));
		m.put("include",     new KeywordInfo(1, null, TokenType.INCLUDE));
		m.put("throws",      new KeywordInfo(3, null, TokenType.THROWS));
		m.put("throw",       new KeywordInfo(3, null, TokenType.THROW));
		m.put("transient",   new KeywordInfo(3, null, TokenType.THROWS));
		m.put("volatile",    new KeywordInfo(3, null, TokenType.VOLATILE));
		m.put("default",     new KeywordInfo(3, null, TokenType.DEFAULT));
		m.put("synchronized",new KeywordInfo(3, null, TokenType.SYNCHRONIZED));
		m.put("typeof",      new KeywordInfo(3, null, TokenType.TYPEOF));
		return m;
	}

	public interface ErrorReporter {
		void report(AnalyzeError error);
	}

	public static final String[] reservedWords = new String[] { "abstract", "and", "as", "await", "break", "byte", "case", "catch", "char", "class", "const", "constructor", "continue", "default", "do", "double", "else", "enum", "eval", "export", "extends", "false", "final", "finally", "float", "for", "function", "global", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface", "let", "long", "native", "new", "not", "null", "or", "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with", "xor", "yield" };
	public static final Set<String> reservedWordsSet = Set.of(reservedWords);

	private final ArrayList<Token> tokens;
	private AIFile aiFile;
	private int version;
	private CharStream stream = null;

	public LexicalParser(AIFile aiFile, int version) {
		this.aiFile = aiFile;
		this.version = version;
		// Pré-dimensionner ~ 1 token / 5 chars (heuristique : noms d'identifiants
		// ~5 chars, opérateurs/whitespace plus courts). Évite plusieurs resizes
		// d'ArrayList sur les gros fichiers.
		this.tokens = new ArrayList<>(Math.max(16, aiFile.getCode().length() / 5));
	}

	public LexicalParserTokenStream parse(ErrorReporter error) {
		long t0 = IACompiler.PHASE_TIMINGS_ENABLED ? System.nanoTime() : 0L;
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
		var result = new LexicalParserTokenStream(tokens, new Token(TokenType.END_OF_FILE, "", new Location(aiFile, stream.getLineCounter(), stream.getCharCounter())));
		if (IACompiler.PHASE_TIMINGS_ENABLED) {
			IACompiler.LEX_NANOS.addAndGet(System.nanoTime() - t0);
		}
		return result;
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

	// Dispatch sur le premier char + longest-match. Remplace le linear scan sur
	// le tableau OPERATORS de ~30 entrées (chacune validée par boucle char-par-char) :
	// l'ancienne version était dans le top 5 du JFR sur la phase lex.
	private boolean tryParseOperator() {
		int idx = stream.index;
		String content = stream.content;
		int len = content.length();
		if (idx >= len) return false;
		char c1 = stream.c;
		char c2 = idx + 1 < len ? content.charAt(idx + 1) : 0;
		char c3 = idx + 2 < len ? content.charAt(idx + 2) : 0;
		char c4 = idx + 3 < len ? content.charAt(idx + 3) : 0;

		switch (c1) {
			case ':': return emitOp(":", TokenType.OPERATOR, 1);
			case '~': return emitOp("~", TokenType.OPERATOR, 1);
			case '@': return emitOp("@", TokenType.OPERATOR, 1);
			case '&':
				if (c2 == '&') return emitOp("&&", TokenType.OPERATOR, 2);
				if (c2 == '=') return emitOp("&=", TokenType.OPERATOR, 2);
				return emitOp("&", TokenType.OPERATOR, 1);
			case '|':
				if (c2 == '|') return emitOp("||", TokenType.OPERATOR, 2);
				if (c2 == '=') return emitOp("|=", TokenType.OPERATOR, 2);
				return emitOp("|", TokenType.OPERATOR, 1);
			case '+':
				if (c2 == '+') return emitOp("++", TokenType.OPERATOR, 2);
				if (c2 == '=') return emitOp("+=", TokenType.OPERATOR, 2);
				return emitOp("+", TokenType.OPERATOR, 1);
			case '-':
				if (c2 == '>') return emitOp("->", TokenType.ARROW, 2);
				if (c2 == '-') return emitOp("--", TokenType.OPERATOR, 2);
				if (c2 == '=') return emitOp("-=", TokenType.OPERATOR, 2);
				return emitOp("-", TokenType.OPERATOR, 1);
			case '*':
				if (c2 == '*') {
					if (c3 == '=') return emitOp("**=", TokenType.OPERATOR, 3);
					return emitOp("**", TokenType.OPERATOR, 2);
				}
				if (c2 == '=') return emitOp("*=", TokenType.OPERATOR, 2);
				return emitOp("*", TokenType.OPERATOR, 1);
			case '/':
				if (c2 == '=') return emitOp("/=", TokenType.OPERATOR, 2);
				return emitOp("/", TokenType.OPERATOR, 1);
			case '\\':
				if (c2 == '=') return emitOp("\\=", TokenType.OPERATOR, 2);
				return emitOp("\\", TokenType.OPERATOR, 1);
			case '%':
				if (c2 == '=') return emitOp("%=", TokenType.OPERATOR, 2);
				return emitOp("%", TokenType.OPERATOR, 1);
			case '=':
				if (c2 == '>') return emitOp("=>", TokenType.ARROW, 2);
				if (c2 == '=') {
					if (c3 == '=') return emitOp("===", TokenType.OPERATOR, 3);
					return emitOp("==", TokenType.OPERATOR, 2);
				}
				return emitOp("=", TokenType.OPERATOR, 1);
			case '!':
				if (c2 == '=') {
					if (c3 == '=') return emitOp("!==", TokenType.OPERATOR, 3);
					return emitOp("!=", TokenType.OPERATOR, 2);
				}
				return emitOp("!", TokenType.OPERATOR, 1);
			case '<':
				if (c2 == '<') {
					if (c3 == '<') {
						if (c4 == '=') return emitOp("<<<=", TokenType.OPERATOR, 4);
						return emitOp("<<<", TokenType.OPERATOR, 3);
					}
					if (c3 == '=') return emitOp("<<=", TokenType.OPERATOR, 3);
					return emitOp("<<", TokenType.OPERATOR, 2);
				}
				if (c2 == '=') return emitOp("<=", TokenType.OPERATOR, 2);
				return emitOp("<", TokenType.OPERATOR, 1);
			case '>':
				if (c2 == '>' && c3 == '>' && c4 == '=') return emitOp(">>>=", TokenType.OPERATOR, 4);
				if (c2 == '=') return emitOp(">=", TokenType.OPERATOR, 2);
				return emitOp(">", TokenType.OPERATOR, 1);
			case '^':
				if (c2 == '=') return emitOp("^=", TokenType.OPERATOR, 2);
				return emitOp("^", TokenType.OPERATOR, 1);
			case '?':
				if (c2 == '?') {
					if (c3 == '=') return emitOp("??=", TokenType.OPERATOR, 3);
					return emitOp("??", TokenType.OPERATOR, 2);
				}
				return emitOp("?", TokenType.OPERATOR, 1);
			case '.':
				if (c2 == '.') return emitOp("..", TokenType.DOT_DOT, 2);
				if (version >= 2) return emitOp(".", TokenType.DOT, 1);
				return false;
			default:
				return false;
		}
	}

	// Avance le stream de `len` chars (aucun \n possible dans un opérateur) puis émet.
	private boolean emitOp(String word, TokenType type, int len) {
		stream.index += len;
		stream.charCounter += len;
		if (stream.index < stream.content.length()) {
			stream.c = stream.content.charAt(stream.index);
		}
		addToken(word, type);
		return true;
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

		// Lookup keyword via HashMap au lieu de ~70 wordEquals séquentiels.
		// v1-2 sont case-insensitive, v3+ case-sensitive.
		// Exception: 'class' reste case-sensitive même en v1/v2 — un identifiant
		// 'Class' est valide en v2 (cf. test `class A {} Class clazz = A`).
		var info = KEYWORDS.get(version <= 2 ? word.toLowerCase() : word);
		if (info != null && version >= info.minVersion
			&& !(info.type == TokenType.CLASS && version <= 2 && !word.equals("class"))) {
			addToken(info.emit != null ? info.emit : word, info.type);
		} else {
			addToken(word, TokenType.STRING);
		}

		return true;
	}

	private boolean tryParseNumber(ErrorReporter error) {

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

	private boolean tryParseString(ErrorReporter error) {

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
