package leekscript.compiler;

import java.util.ArrayList;

import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.common.Error;

/**
 * Son but est de convertir le LeekCode en un "bytecode" plus rapide à
 * exécuter Il doit aussi trouver les erreurs dans le LeekCode
 */
public class WordParser {

	public static final String[] reservedWords = new String[] {
		"abstract", "arguments", "await", "break", "byte", "case", "catch",
		"char", "class", "const", "constructor", "continue", "default", "do", "double", "else", "enum", "eval",
		"export", "extends", "false", "final", "finally", "float", "for", "function",
		"goto", "if", "implements", "import", "in", "instanceof", "int", "interface",
		"let", "long", "native", "new", "null", "package", "private", "protected",
		"public", "return", "short", "static", "super", "switch", "synchronized", "this",
		"throw", "throws", "transient", "true", "try", "typeof", "var", "void",
		"volatile", "while", "with", "yield"
	};

	/**
	 * Instructions byte(0) (0-255) byte => instruction
	 *
	 * 1 : Etiquette SmallInt (2) : numéro d'étiquette
	 *
	 * 2 : SetVariable StringUTF : nom variable
	 */

	public final static int T_NOTHING = 0;
	public final static int T_NUMBER = 1;
	public final static int T_STRING = 2;
	public final static int T_VAR_STRING = 3;
	public final static int T_OPERATOR = 4;
	public final static int T_END_INSTRUCTION = 5;
	public final static int T_PAR_LEFT = 6;
	public final static int T_PAR_RIGHT = 7;
	public final static int T_VIRG = 8;
	public final static int T_CONDITION_OPERATOR = 9;

	public final static int T_ACCOLADE_LEFT = 10;
	public final static int T_ACCOLADE_RIGHT = 11;

	public final static int T_BRACKET_LEFT = 12;
	public final static int T_BRACKET_RIGHT = 13;

	public final static int T_DOUBLE_POINT = 14;
	public final static int T_DOT = 15;

	private final AIFile<?> mAI;
	private final ArrayList<IAWord> words = new ArrayList<IAWord>();

	private int instructions = 0;
	private int cursor = 0;
	private int line_counter = 0;
	private int char_counter = 0;
	private final String code;
	private int version;

	public WordParser(AIFile<?> ai, int version) {
		mAI = ai;
		this.code = ai.getCode();
		this.version = version;
	}

	public void compile(WordCompiler compiler) throws LeekCompilerException {
		words.clear();
		line_counter = 1;
		char_counter = 0;
		char opener = 0;
		char c;
		int type = 0;
		boolean comment_line = false;
		boolean comment_block = false;
		String word = "";

		// Boucle qui détecte les mots clés, valeurs, opérateurs... utilisés
		int length = code.length();
		for(int i = 0; i < code.length(); i++){
			c = code.charAt(i);
			if (c == '\r') continue;
			// Compteur caractères/lignes
			if (c == '\n') {
				if (type != T_NOTHING) {
					newWord(word, type);
					type = T_NOTHING;
					word = "";
				}
				char_counter = 0;
				line_counter++;
				comment_line = false;
			}
			else char_counter++;
			if ((c == '"' || c == '\'') && !comment_block && !comment_line) {
				if (type == T_NOTHING) {
					word = "";
					type = T_VAR_STRING;
					opener = c;
				}
				else if (type == T_VAR_STRING && opener == c) {
					boolean isEscaped = false;
					for (int j = word.length() - 1; j >= 0; j--) {
						if(word.charAt(j) == '\\') isEscaped = !isEscaped;
						else break;
					}
					if (isEscaped) {
						word = word.substring(0, word.length() - 1);
						word += c;
					} else {
						newWord(word, type);
						word = "";
						type = T_NOTHING;
					}
				}
				else if (type == T_VAR_STRING) word += c;
				else {
					newWord(word, type);
					word = "";
					opener = c;
					type = T_VAR_STRING;
				}
				continue;
			}
			if (type == T_VAR_STRING) {
				word += c;
				continue;
			}

			if (comment_block && c == '*' && length > i + 1 && code.charAt(i + 1) == '/') {
				comment_block = false;
				i++;
				continue;
			}
			if (comment_line || comment_block) continue;
			if (c == '/' && length > i + 1 && code.charAt(i + 1) == '/') {
				comment_line = true;
				if (version >= 2) i++;
				continue;
			}
			else if (c == '/' && length > i + 1 && code.charAt(i + 1) == '*') {
				comment_block = true;
				if (version >= 2) i++;
				continue;
			}
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_') {
				if (type == T_NOTHING) {
					word += c;
					type = T_STRING;
				} else if (type == T_STRING || type == T_VAR_STRING) {
					word += c;
				} else {
					if (type == T_OPERATOR) {
						newWord(word, type);
						word = "" + c;
						type = T_STRING;
					} else if (type == T_NUMBER) {
						throw new LeekCompilerException(mAI, line_counter, char_counter, word, Error.INVALID_NUMBER);
					}
				}
			}
			else if(c >= '0' && c <= '9'){
				if(type == T_NOTHING){
					word += c;
					type = T_NUMBER;
				}
				else if(type == T_NUMBER || type == T_STRING || type == T_VAR_STRING){
					word += c;
				}
				else{
					if(type != T_NOTHING) newWord(word, type);
					word = "" + c;
					type = T_NUMBER;
				}
			}
			else if(c == ':'){
				if(type != T_NOTHING) newWord(word, type);
				newWord(":", T_OPERATOR);
				word = "";
				type = T_NOTHING;
			}
			else if(c == '.') {
				if (type == T_VAR_STRING) {
					word += c;
				} else if (type == T_NUMBER) {
					if (word.contains(".")) {
						compiler.addError(new AnalyzeError(new IAWord(mAI, 0, ".", line_counter, char_counter + 1), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
					} else {
						word += c;
					}
				}
				else if (version >= 2) {
					if (type == T_STRING) {
						newWord(word, type);
					}
					newWord(".", T_DOT);
					word = "";
					type = T_NOTHING;
				} else {
					compiler.addError(new AnalyzeError(new IAWord(mAI, 0, ".", line_counter, char_counter + 1), AnalyzeErrorLevel.ERROR, Error.INVALID_CHAR));
					// throw new LeekCompilerException(mAI, line_counter, char_counter + 1, ".", Error.INVALID_CHAR);
				}
			}
			else if(c == '@' || c == '+' || c == '=' || c == '<' || c == '>' || c == '|' || c == '&' || c == '-' || c == '/' || c == '*' || c == '%' || c == '!' || c == '?' || c == '^' || c == '~' || c == '.'){
				if(type == T_VAR_STRING){
					word += c;
				}
				else if(type == T_OPERATOR){
					if(word.equals("&")){
						if(c == '&' || c == '=') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("|")){
						if(c == '|' || c == '=') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("+")){
						if(c == '=' || c == '+') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("-")){
						if(c == '=' || c == '-') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("*")){
						if(c == '=' || c == '*') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals(">")){
						if(c == '=' || c == '>') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("<")){
						if(c == '=' || c == '<') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals(">>")){
						if(c == '=' || c == '>') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals(">>>")){
						if(c == '=') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("<<")){
						if(c == '=') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else if(word.equals("*") || word.equals("**") || word.equals("/") || word.equals("%") || word.equals("=") || word.equals("!") || word.equals("<") || word.equals(">") || word.equals("^") || word.equals("==") || word.equals("!=")){
						if(c == '=') word += c;
						else{
							newWord(word, type);
							word = "" + c;
						}
					}
					else{
						newWord(word, type);
						word = "" + c;
					}
				}
				//else if(type == T_NUMBER || type == T_STRING){
				else{
					if(type != T_NOTHING) newWord(word, type);
					word = "" + c;
					type = T_OPERATOR;
				}
			}
			else if(c == '(' || c == ')'){
				if(type != T_NOTHING){
					newWord(word, type);
					word = "";
					type = T_NOTHING;
				}
				if(c == '(') newWord("(", T_PAR_LEFT);
				else newWord(")", T_PAR_RIGHT);
			}
			else if(c == '[' || c == ']'){
				if(type != T_NOTHING){
					newWord(word, type);
					word = "";
					type = T_NOTHING;
				}
				if(c == '[') newWord("[", T_BRACKET_LEFT);
				else newWord("]", T_BRACKET_RIGHT);
			}
			else if(c == '{' || c == '}'){
				if(type != T_NOTHING){
					newWord(word, type);
					word = "";
					type = T_NOTHING;
				}
				if(c == '{') newWord("{", T_ACCOLADE_LEFT);
				else newWord("}", T_ACCOLADE_RIGHT);
			}
			else if(c == ' ' || c == '\n' || c == '\t' || c == 160 /* NBSP */){
				if(type == T_VAR_STRING){
					word += c;
				}
				else if(type != T_NOTHING){
					newWord(word, type);
					word = "";
					type = T_NOTHING;
				}
			}
			else if(c == ';'){
				if(type != T_NOTHING){
					newWord(word, type);
					word = "";
					type = T_NOTHING;
				}
				newWord(";", T_END_INSTRUCTION);
			}
			else if(c == ','){
				if(type != T_NOTHING){
					newWord(word, type);
					word = "";
					type = T_NOTHING;
				}
				newWord(",", T_VIRG);
			} else {
				// Tout autre caractère
				if (type == T_VAR_STRING) {
					word += c;
				} else {
					throw new LeekCompilerException(mAI, line_counter, char_counter, "" + c, Error.INVALID_CHAR);
				}
			}
		}

		if(type != T_NOTHING){
			newWord(word, type);
		}

		// for(int i=0;i<words.size();i++){
		// 	System.out.println(words.get(i).getType()+" => "+words.get(i).getWord());
		// }
	}

	private boolean wordEquals(String word, String expected) {
		if (version <= 2) {
			return word.equalsIgnoreCase(expected);
		}
		return word.equals(expected);
	}

	private void newWord(String word, int type) {
		if (type == T_STRING) {
			if (wordEquals(word, "or")) {
				type = T_OPERATOR;
				word = "||";
			}
			else if (wordEquals(word, "and")) {
				type = T_OPERATOR;
				word = "&&";
			}
			else if (wordEquals(word, "instanceof")) {
				type = T_OPERATOR;
				word = "instanceof";
			}
			/*
			 * else if(word.equalsIgnoreCase("not")){
			 * type = T_OPERATOR;
			 * word = "!";
			 * }
			 */
		}
		else if (type == T_OPERATOR) {
			if (word.equals("=!")) {
				words.add(new IAWord(mAI, type, "=", line_counter, char_counter));
				words.add(new IAWord(mAI, type, "!", line_counter, char_counter));
				return;
			}
		}
		words.add(new IAWord(mAI, type, word, line_counter, char_counter));
	}

	public IAWord getWord() {
		return words.get(cursor);
	}

	public IAWord lastWord() {
		return words.get(cursor - 1);
	}

	public IAWord endWord() {
		return words.isEmpty() ? null : words.get(words.size() - 1);
	}

	public IAWord readWord() {
		cursor++;
		return words.get(cursor - 1);
	}

	public void skipWord() {
		cursor++;
	}

	public void back() {
		cursor--;
	}

	public boolean haveWords() {
		return cursor < words.size();
	}

	public void reset() {
		cursor = 0;
	}

	public void setInstructionCount(int count) {
		instructions = count;
	}

	public int getInstructionCount() {
		return instructions;
	}

	public int getPosition() {
		return cursor;
	}

	public void setPosition(int position) {
		this.cursor = position;
	}
}
