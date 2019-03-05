package leekscript.compiler;

public class IAWord {
	private final int type;
	private final String word;
	private final int line;
	private final int character;
	private final int ai;

	public IAWord(int ai, int type, String word, int line, int character) {
		this.ai = ai;
		this.type = type;
		this.word = word;
		this.line = line;
		this.character = character;
	}

	public int getAI() {
		return ai;
	}

	public String getWord() {
		return word;
	}

	public int getType() {
		return type;
	}

	public int getLine() {
		return line;
	}

	public int getCharacter() {
		return character;
	}
}
