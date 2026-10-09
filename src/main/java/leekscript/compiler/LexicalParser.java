package leekscript.compiler;

import java.util.ArrayList;
import java.util.Arrays;
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

	// Words à 1 char interned : on évite l'alloc "" + c via String.valueOf à chaque
	// bracket/parens/comma/semicolon dans le main loop.
	private static final String[] SINGLE_CHAR_WORDS = buildSingleCharWords();
	private static String[] buildSingleCharWords() {
		var arr = new String[128];
		for (int i = 0; i < 128; i++) arr[i] = String.valueOf((char) i);
		return arr;
	}

	// Lookup ASCII pour les chars valides dans un identifiant : a-z, A-Z, 0-9, _.
	// Remplace ~4 range checks dans la boucle de tryParseIdentifier par un array
	// lookup (les chars > 127 retombent sur le slow path Latin-1).
	private static final boolean[] IDENT_CHAR_ASCII = buildIdentCharAscii();
	private static boolean[] buildIdentCharAscii() {
		var arr = new boolean[128];
		for (char c = 'a'; c <= 'z'; c++) arr[c] = true;
		for (char c = 'A'; c <= 'Z'; c++) arr[c] = true;
		for (char c = '0'; c <= '9'; c++) arr[c] = true;
		arr['_'] = true;
		return arr;
	}

	// Bitmap des premiers chars possibles d'un keyword (v3+ case-sensitive).
	// Permet de fast-path les identifiants user qui ne peuvent pas être keywords.
	// Calculé à partir de KEYWORDS pour rester synchronisé automatiquement.
	private static final boolean[] KEYWORD_FIRST_CHAR = buildKeywordFirstCharSet();
	private static boolean[] buildKeywordFirstCharSet() {
		var bs = new boolean[128];
		for (var k : KEYWORDS.keySet()) {
			char c = k.charAt(0);
			if (c < 128) bs[c] = true;
		}
		return bs;
	}

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
		stream = new CharStream(aiFile.getCode());
		while (stream.index < stream.content.length()) {
			step(error);
		}
		var result = new LexicalParserTokenStream(tokens, new Token(TokenType.END_OF_FILE, "", new Location(aiFile, stream.getLineCounter(), stream.getCharCounter())), computeMatchingBrackets(tokens));
		if (IACompiler.PHASE_TIMINGS_ENABLED) {
			IACompiler.LEX_NANOS.addAndGet(System.nanoTime() - t0);
		}
		return result;
	}

	/**
	 * Tokenise un seul élément à la position courante du stream (whitespace, mot,
	 * nombre, string, opérateur, bracket…). Extrait de la boucle principale pour
	 * pouvoir être réutilisé par l'interpolation de chaînes ({@link #lexInterpolationExpression}),
	 * qui doit lexer une expression LeekScript arbitraire à l'intérieur d'un {@code ${ … }}.
	 */
	private void step(ErrorReporter error) {
		if (stream.index >= stream.content.length()) return;
		char c = stream.c;

		// Fast paths sur les chars dominants (whitespace + lettres ascii). Le
		// dispatch séquentiel d'avant essayait chaque tryParseXxx en série :
		// 9 méthodes par token au pire, alors qu'une seule peut réussir.
		if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == 160) {
			stream.next();
			return;
		}
		if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
			if (tryParseIdentifier()) return;
		}
		if (c >= '0' && c <= '9') {
			if (tryParseNumber(error)) return;
		}
		if (c == '"' || c == '\'') {
			if (tryParseString(error)) return;
		}
		if (c == '/') {
			// peek(1) pour distinguer commentaire de division
			char c2 = stream.index + 1 < stream.content.length() ? stream.content.charAt(stream.index + 1) : 0;
			if (c2 == '/' || c2 == '*') {
				if (tryParseComments()) return;
			}
			if (tryParseOperator()) return;
		}
		// Opérateurs (premiers chars)
		if (isOperatorStart(c)) {
			if (tryParseOperator()) return;
		}
		// Brackets / accolades / parenthèses : dispatch direct par char.
		// Évite les ~6 tryParseExact séquentiels de tryParseBracketLike, chacun
		// avec un charEquals + une string "" + char allocation.
		TokenType bracketType = null;
		switch (c) {
			case '(': bracketType = TokenType.PAR_LEFT; break;
			case ')': bracketType = TokenType.PAR_RIGHT; break;
			case '[': bracketType = TokenType.BRACKET_LEFT; break;
			case ']': bracketType = TokenType.BRACKET_RIGHT; break;
			case '{': bracketType = TokenType.ACCOLADE_LEFT; break;
			case '}': bracketType = TokenType.ACCOLADE_RIGHT; break;
			case ',': bracketType = TokenType.VIRG; break;
			case ';': bracketType = TokenType.END_INSTRUCTION; break;
			default: break;
		}
		if (bracketType != null) {
			stream.next();
			addToken(SINGLE_CHAR_WORDS[c], bracketType);
			return;
		}
		// Identifiants étendus (lettres accentuées) + lemniscate / pi
		if (tryParseSpecialIdentifier()) return;
		if (tryParseIdentifier()) return;

		error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
		stream.next();
	}

	/**
	 * Pré-calcule en O(n) les paires de brackets matchantes ({↔}, [↔], (↔)).
	 * Permet au syntax pass de skipper un body en O(1) au lieu de re-compter la
	 * profondeur token par token à chaque skip-to-brace.
	 *
	 * Tolérant aux brackets non balancées (code mal formé) : on émet juste -1
	 * pour ceux qui n'ont pas de match, sans erreur — le parser syntax émettra
	 * l'erreur appropriée si besoin.
	 */
	private static int[] computeMatchingBrackets(ArrayList<Token> tokens) {
		int n = tokens.size();
		// On stocke matchIdx+1 (0 = pas de match) pour profiter de l'init zéro du new int[]
		// au lieu de payer un Arrays.fill(-1) sur toute la longueur ; getMatchingBracket
		// fait -1 au reverse. Tolérant aux brackets non balancées : pas d'erreur ici,
		// le parser syntax émettra le diag approprié si besoin.
		int[] match = new int[n];
		// 3 piles séparées (une par type) pour gérer les mismatches genre `{[}`.
		int[] stackBrace = new int[64], stackBracket = new int[64], stackParen = new int[64];
		int spBrace = 0, spBracket = 0, spParen = 0;
		for (int i = 0; i < n; i++) {
			TokenType t = tokens.get(i).getType();
			switch (t) {
				case ACCOLADE_LEFT:
					if (spBrace == stackBrace.length) stackBrace = Arrays.copyOf(stackBrace, spBrace * 2);
					stackBrace[spBrace++] = i;
					break;
				case ACCOLADE_RIGHT:
					if (spBrace > 0) {
						int open = stackBrace[--spBrace];
						match[open] = i + 1;
						match[i] = open + 1;
					}
					break;
				case BRACKET_LEFT:
					if (spBracket == stackBracket.length) stackBracket = Arrays.copyOf(stackBracket, spBracket * 2);
					stackBracket[spBracket++] = i;
					break;
				case BRACKET_RIGHT:
					if (spBracket > 0) {
						int open = stackBracket[--spBracket];
						match[open] = i + 1;
						match[i] = open + 1;
					}
					break;
				case PAR_LEFT:
					if (spParen == stackParen.length) stackParen = Arrays.copyOf(stackParen, spParen * 2);
					stackParen[spParen++] = i;
					break;
				case PAR_RIGHT:
					if (spParen > 0) {
						int open = stackParen[--spParen];
						match[open] = i + 1;
						match[i] = open + 1;
					}
					break;
				default:
					break;
			}
		}
		return match;
	}

	private static boolean isOperatorStart(char c) {
		// Premiers chars possibles d'un opérateur (cf tryParseOperator switch).
		return c == ':' || c == '~' || c == '@' || c == '&' || c == '|'
				|| c == '+' || c == '-' || c == '*' || c == '\\' || c == '%'
				|| c == '=' || c == '!' || c == '<' || c == '>' || c == '^'
				|| c == '?' || c == '.';
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
		// Scan direct sur content + bump du stream à la fin (un identifiant ne
		// contient pas de \n donc lineCounter intact). Ancienne version :
		// stream.next() par char → maj lineCounter/charCounter/c à chaque pas.
		String content = stream.content;
		int len = content.length();
		int start = stream.index;
		int i = start;
		while (i < len) {
			char c = content.charAt(i);
			// Fast path ASCII : un seul lookup au lieu de 4 range checks.
			if (c < 128) {
				if (IDENT_CHAR_ASCII[c]) { i++; continue; }
				break;
			}
			// Slow path Latin-1+ : lettres accentuées et Œ/œ/ÿ.
			if ((c >= 'À' && c <= 'Ö') || (c >= 'à' && c <= 'ö')
				|| (c >= 'Ø' && c <= 'Ý') || (c >= 'ø' && c <= 'ý')
				|| (c >= 'Œ' && c <= 'œ') || c == 'ÿ') { i++; continue; }
			break;
		}
		if (i == start) return false;
		stream.index = i;
		stream.charCounter += i - start;
		if (i < len) stream.c = content.charAt(i);

		var word = content.substring(start, i);

		// Fast path v3+ : si le premier char n'est pas un premier char de keyword,
		// inutile de faire le HashMap.get. La majorité des identifiants user
		// (CamelCase, ou commençant par h/j/k/m/q/u/z) sont exclus tout de suite.
		KeywordInfo info;
		if (version >= 3) {
			char fc = word.charAt(0);
			if (fc >= 128 || !KEYWORD_FIRST_CHAR[fc]) {
				addToken(word, TokenType.STRING);
				return true;
			}
			info = KEYWORDS.get(word);
		} else {
			// v1-2 sont case-insensitive : lookup sur le mot lowercased.
			info = KEYWORDS.get(word.toLowerCase());
		}
		// Exception: 'class' reste case-sensitive même en v1/v2 — un identifiant
		// 'Class' est valide en v2 (cf. test `class A {} Class clazz = A`).
		if (info != null && version >= info.minVersion
			&& !(info.type == TokenType.CLASS && version <= 2 && !word.equals("class"))) {
			addToken(info.emit != null ? info.emit : word, info.type);
		} else {
			addToken(word, TokenType.STRING);
		}

		return true;
	}

	private boolean tryParseNumber(ErrorReporter error) {
		// Scan direct sur content + bump du stream à la fin (un nombre ne contient
		// pas de \n donc lineCounter intact). Ancienne version : stream.next() par
		// char → maj lineCounter/charCounter/c à chaque pas.
		String content = stream.content;
		int len = content.length();
		int start = stream.index;
		if (start >= len) return false;
		char first = content.charAt(start);
		if (first < '0' || first > '9') return false;
		int i = start + 1;
		boolean dotSeen = false;
		while (i < len) {
			char c = content.charAt(i);
			// Fast path ASCII : même lookup que tryParseIdentifier (a-z, A-Z, 0-9, _).
			if (c < 128) {
				if (IDENT_CHAR_ASCII[c]) { i++; continue; }
				// fallthrough to handle '-', '+', '.', etc.
			} else if ((c >= 'À' && c <= 'Ö') || (c >= 'à' && c <= 'ö')
					|| (c >= 'Ø' && c <= 'Ý') || (c >= 'ø' && c <= 'ý')
					|| (c >= 'Œ' && c <= 'œ') || c == 'ÿ') {
				i++; continue;
			}
			if (c == '-' || c == '+') {
				char prev = i > 0 ? content.charAt(i - 1) : 0;
				if (prev == 'e' || prev == 'p') { i++; continue; }
				break;
			}
			if (c == '.') {
				// Pas d'eat si suivi d'un autre point (intervalle 1..10)
				if (i + 1 < len && content.charAt(i + 1) == '.') break;
				if (dotSeen) {
					// Compute char counter at the second dot for error location.
					int charAtDot = stream.charCounter + (i - start) + 1;
					error.report(new AnalyzeError(new Token(TokenType.NOTHING, ".", aiFile, stream.getLineCounter(), charAtDot), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
					break;
				}
				dotSeen = true;
				i++;
				continue;
			}
			break;
		}
		// Bump du stream
		stream.index = i;
		stream.charCounter += i - start;
		if (i < len) stream.c = content.charAt(i);
		addToken(content.substring(start, i), TokenType.NUMBER);
		return true;
	}

	private boolean tryParseString(ErrorReporter error) {
		String content = stream.content;
		int len = content.length();
		int start = stream.index;
		if (start >= len) return false;
		char openQuote = content.charAt(start);
		if (openQuote != '"' && openQuote != '\'') return false;

		// Interpolation de chaînes (sucre syntaxique au-dessus de la concaténation) :
		// uniquement en v4+ et pour les chaînes à guillemets doubles. On ne bascule
		// sur le chemin desugaré que si un `${` non échappé est réellement présent,
		// sinon on garde le chemin rapide (un seul token VAR_STRING) — comportement
		// inchangé pour les chaînes ordinaires.
		if (version >= 4 && openQuote == '"' && hasInterpolation(content, start, len)) {
			return parseInterpolatedString(error, start);
		}

		// Scan direct ; chaque \n rencontré incrémente lineCounter (les strings
		// peuvent contenir des newlines). À la fin : bump charCounter selon la
		// distance depuis le dernier \n.
		int i = start + 1;
		int lineInc = 0;
		int lastNewlineAt = -1;
		boolean escaped = false;
		boolean closed = false;
		while (i < len) {
			char c = content.charAt(i);
			if (c == '\\') {
				escaped = !escaped;
				i++;
				continue;
			}
			if (c == openQuote && !escaped) {
				closed = true;
				i++;
				break;
			}
			if (c == '\n') { lineInc++; lastNewlineAt = i; }
			escaped = false;
			i++;
		}
		// Bump du stream
		stream.index = i;
		if (lineInc > 0) {
			stream.lineCounter += lineInc;
			stream.charCounter = i - lastNewlineAt - 1;
		} else {
			stream.charCounter += i - start;
		}
		if (i < len) stream.c = content.charAt(i);
		if (closed) {
			addToken(content.substring(start, i), TokenType.VAR_STRING);
			return true;
		} else {
			error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.STRING_NOT_CLOSED));
			return false;
		}
	}

	/**
	 * Détecte la présence d'un `${` non échappé dans la chaîne démarrant à `start`
	 * (guillemet ouvrant inclus). Ne modifie pas le stream — simple lookahead pour
	 * décider entre le chemin rapide et le chemin interpolé.
	 */
	private static boolean hasInterpolation(String content, int start, int len) {
		boolean escaped = false;
		for (int i = start + 1; i < len; i++) {
			char c = content.charAt(i);
			if (c == '\\') { escaped = !escaped; continue; }
			if (c == '"' && !escaped) return false; // guillemet fermant, rien trouvé
			if (!escaped && c == '$' && i + 1 < len && content.charAt(i + 1) == '{') return true;
			escaped = false;
		}
		return false; // chaîne non fermée : on laisse le chemin normal reporter l'erreur
	}

	/**
	 * Desugare une chaîne interpolée en la suite de tokens d'une concaténation :
	 * {@code "a${x}b"} devient {@code ( "a" + ( x ) + "b" )}. Le parser et le
	 * générateur de code restent inchangés ; le Java produit est strictement
	 * identique à une concaténation écrite à la main, donc le coût en opérations
	 * est le même.
	 *
	 * Le premier littéral est toujours émis (même vide) pour ancrer le type String,
	 * de sorte que {@code "${x}"} avec x entier produise bien une chaîne. Les
	 * littéraux vides suivants sont omis pour ne pas ajouter de concaténation inutile.
	 */
	private boolean parseInterpolatedString(ErrorReporter error, int start) {
		String content = stream.content;
		int len = content.length();
		stream.next(); // consomme le guillemet ouvrant
		addToken("(", TokenType.PAR_LEFT);
		boolean needPlus = false;    // émettre un `+` avant le prochain élément
		boolean firstLiteral = true; // le premier littéral est toujours émis (ancre le type String)
		int chunkStart = stream.index;

		while (stream.index < len) {
			char c = stream.c;
			if (c == '\\') {
				// Échappement : le backslash et le char suivant restent dans le littéral
				stream.next();
				if (stream.index < len) stream.next();
				continue;
			}
			if (c == '"' || (c == '$' && stream.peek(1) == '{')) {
				// Flush du littéral courant [chunkStart, index)
				int chunkEnd = stream.index;
				boolean empty = chunkEnd == chunkStart;
				if (!empty || firstLiteral) {
					if (needPlus) addToken("+", TokenType.OPERATOR);
					addToken("\"" + content.substring(chunkStart, chunkEnd) + "\"", TokenType.VAR_STRING);
					needPlus = true;
				}
				firstLiteral = false;

				if (c == '"') {
					stream.next(); // consomme le guillemet fermant
					addToken(")", TokenType.PAR_RIGHT);
					return true;
				}
				// Interpolation ${ … }
				stream.next(); // '$'
				stream.next(); // '{'
				if (needPlus) addToken("+", TokenType.OPERATOR);
				addToken("(", TokenType.PAR_LEFT);
				lexInterpolationExpression(error);
				addToken(")", TokenType.PAR_RIGHT);
				needPlus = true;
				chunkStart = stream.index;
				continue;
			}
			stream.next(); // CharStream.next() gère le compteur de lignes pour les \n
		}
		// Chaîne non fermée
		error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.STRING_NOT_CLOSED));
		addToken(")", TokenType.PAR_RIGHT);
		return true;
	}

	/**
	 * Lexe l'expression LeekScript à l'intérieur d'un `${ … }`, jusqu'à l'accolade
	 * fermante correspondante (exclue de l'émission). Réutilise {@link #step} pour
	 * tokeniser n'importe quelle expression ; la profondeur d'accolades est suivie
	 * via les tokens émis, de sorte que les `}` à l'intérieur d'une chaîne imbriquée
	 * ou d'un littéral map ne ferment pas l'interpolation.
	 */
	private void lexInterpolationExpression(ErrorReporter error) {
		int len = stream.content.length();
		int depth = 0;
		boolean emittedExpr = false;
		while (stream.index < len) {
			int before = tokens.size();
			int idxBefore = stream.index;
			step(error);
			if (tokens.size() > before) {
				Token last = tokens.get(tokens.size() - 1);
				TokenType t = last.getType();
				if (t == TokenType.ACCOLADE_LEFT) {
					depth++;
					emittedExpr = true;
				} else if (t == TokenType.ACCOLADE_RIGHT) {
					if (depth == 0) {
						// Accolade fermante de l'interpolation : c'est un délimiteur,
						// pas du code → on la retire des tokens.
						tokens.remove(tokens.size() - 1);
						if (!emittedExpr) {
							error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.STRING_NOT_CLOSED));
						}
						return;
					}
					depth--;
				} else {
					emittedExpr = true;
				}
			} else if (stream.index == idxBefore) {
				break; // aucun progrès (EOF / char invalide) : éviter une boucle infinie
			}
		}
		// `${` non fermé avant la fin du fichier
		error.report(new AnalyzeError(new Location(aiFile, stream.getLineCounter(), stream.getCharCounter()), AnalyzeErrorLevel.ERROR, Error.STRING_NOT_CLOSED));
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

	private void addToken(String word, TokenType type) {
		tokens.add(new Token(type, word, aiFile, stream.getLineCounter(), stream.getCharCounter()));
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
	}
}
