package leekscript.compiler;

import leekscript.compiler.expression.Expression;

public class Token {

	private final Location location;
	private final TokenType type;
	private final String word;
	private Expression expression;

	public Token(String word) {
		this(TokenType.NOTHING, word, null);
	}

	public Token(TokenType type, String word, AIFile file, int line, int column) {
		this.location = new Location(file, line, column - word.length(), line, column - 1);
		this.type = type;
		this.word = word;
	}

	public Token(TokenType type, String word, Location location) {
		this.location = location;
		this.type = type;
		this.word = word;
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
		return location;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public Expression getExpression() {
		return this.expression;
	}
}
