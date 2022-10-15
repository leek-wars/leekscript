package leekscript.compiler;

import leekscript.compiler.expression.Expression;

public class Token {

	private final Location location;
	private final int type;
	private final String word;
	private Expression expression;

	public Token(String word) {
		this(0, word, null);
	}

	public Token(int type, String word, AIFile file, int line, int column) {
		this.location = new Location(file, line, column - word.length(), line, column - 1);
		// System.out.println("Token type=" + type + " word=" + word + " len=" + word.length() + " line=" + line + " column=" + this.location.getStartColumn() + " to=" + this.location.getEndColumn());
		this.type = type;
		this.word = word;
	}

	public Token(int type, String word, Location location) {
		this.location = location;
		this.type = type;
		this.word = word;
	}

	public String getWord() {
		return word;
	}

	public int getType() {
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
