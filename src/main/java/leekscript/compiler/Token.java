package leekscript.compiler;

import leekscript.compiler.expression.Expression;

public class Token {

	// Location lazy : sur les ~120k tokens d'une compile, beaucoup ne demandent
	// jamais leur Location (seuls les checks de type/word sont consultés). On
	// stocke les coordonnées brutes et on alloue le Location à la demande.
	private final TokenType type;
	private final String word;
	private final AIFile file;
	private final int startLine;
	private final int startColumn;
	private final int endLine;
	private final int endColumn;
	private Location location;
	private Expression expression;

	public Token(String word) {
		this(TokenType.NOTHING, word, null);
	}

	public Token(TokenType type, String word, AIFile file, int line, int column) {
		this.type = type;
		this.word = word;
		this.file = file;
		this.startLine = line;
		this.startColumn = column - word.length();
		this.endLine = line;
		this.endColumn = column - 1;
	}

	public Token(TokenType type, String word, Location location) {
		this.type = type;
		this.word = word;
		this.location = location;
		this.file = location != null ? location.getFile() : null;
		this.startLine = location != null ? location.getStartLine() : 0;
		this.startColumn = location != null ? location.getStartColumn() : 0;
		this.endLine = location != null ? location.getEndLine() : 0;
		this.endColumn = location != null ? location.getEndColumn() : 0;
	}

	public String getWord() {
		return word;
	}

	public TokenType getType() {
		return type;
	}

	@Override
	public String toString() {
		return word;
	}

	public Location getLocation() {
		if (location == null) {
			location = new Location(file, startLine, startColumn, endLine, endColumn);
		}
		return location;
	}

	// Accès direct au champ — évite d'allouer la Location quand on n'a besoin que
	// de la ligne (cf. WordCompiler.compileWord appelé par token statement-level).
	public int getStartLine() {
		return startLine;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public Expression getExpression() {
		return this.expression;
	}
}
