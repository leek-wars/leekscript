package leekscript.compiler.exceptions;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;

public class LeekCompilerException extends Exception {

	public final static String FUNCTION_NAME_UNAVAILABLE = "function_name_unavailable";
	public final static String PARAMETER_NAME_UNAVAILABLE = "parameter_name_unavailable";
	public final static String OPENING_PARENTHESIS_EXPECTED = "opening_parenthesis_expected";
	public final static String OPENING_CURLY_BRACKET_EXPECTED = "opening_curly_bracket_expected";
	public final static String PARAMETER_NAME_EXPECTED = "parameter_name_expected";
	public final static String FUNCTION_NAME_EXPECTED = "function_name_expected";
	public final static String PARENTHESIS_EXPECTED_AFTER_PARAMETERS = "parenthesis_expected_after_parameters";
	public final static String OPEN_BLOC_REMAINING = "open_bloc_remaining";
	public final static String NO_BLOC_TO_CLOSE = "no_bloc_to_close";
	public final static String END_OF_SCRIPT_UNEXPECTED = "end_of_script_unexpected";
	public final static String END_OF_INSTRUCTION_EXPECTED = "end_of_instruction_expected";
	public final static String BREAK_OUT_OF_LOOP = "break_out_of_loop";
	public final static String CONTINUE_OUT_OF_LOOP = "continue_out_of_loop";
	public final static String INCLUDE_ONLY_IN_MAIN_BLOCK = "include_only_in_main_block";
	public final static String AI_NAME_EXPECTED = "ai_name_expected";
	public final static String AI_NOT_EXISTING = "ai_not_existing";
	public final static String CLOSING_PARENTHESIS_EXPECTED = "closing_parenthesis_expected";
	public final static String CLOSING_SQUARE_BRACKET_EXPECTED = "closing_square_bracket_expected";
	public final static String FUNCTION_ONLY_IN_MAIN_BLOCK = "function_only_in_main_block";
	public final static String VARIABLE_NAME_EXPECTED = "variable_name_expected";
	public final static String VARIABLE_NAME_UNAVAILABLE = "variable_name_unavailable";
	public final static String VARIABLE_NOT_EXISTS = "variable_not_exists";
	public final static String KEYWORD_UNEXPECTED = "keyword_unexpected";
	public final static String KEYWORD_IN_EXPECTED = "keyword_in_expected";
	public final static String WHILE_EXPECTED_AFTER_DO = "while_expected_after_do";
	public final static String NO_IF_BLOCK = "no_if_block";
	public final static String GLOBAL_ONLY_IN_MAIN_BLOCK = "global_only_in_main_block";
	public final static String VAR_NAME_EXPECTED_AFTER_GLOBAL = "var_name_expected_after_global";
	public final static String VAR_NAME_EXPECTED = "var_name_expected";
	public final static String SIMPLE_ARRAY = "simple_array";
	public final static String ASSOCIATIVE_ARRAY = "associative_array";
	public final static String PARENTHESIS_EXPECTED_AFTER_FUNCTION = "parenthesis_expected_after_function";
	public final static String UNKNOWN_VARIABLE_OR_FUNCTION = "unknown_variable_or_function";
	public final static String OPERATOR_UNEXPECTED = "operator_unexpected";
	public final static String VALUE_EXPECTED = "value_expected";
	public final static String CANT_ADD_INSTRUCTION_AFTER_BREAK = "cant_add_instruction_after_break";
	public final static String UNCOMPLETE_EXPRESSION = "uncomplete_expression";
	public final static String CANT_ASSIGN_VALUE = "cant_assign_value";
	public final static String FUNCTION_NOT_EXISTS = "function_not_exists";
	public final static String INVALID_PAREMETER_COUNT = "invalid_parameter_count";
	public final static String INVALID_CHAR = "invalid_char";
	public final static String INVALID_NUMBER = "invalid_number";
	public final static String CONSTRUCTOR_ALREADY_EXISTS = "constructor_already_exists";
	public final static String END_OF_CLASS_EXPECTED = "end_of_class_expected";
	public final static String FIELD_ALREADY_EXISTS = "field_already_exists";
	public final static String NO_SUCH_CLASS = "no_such_class";
	public final static String THIS_NOT_ALLOWED_HERE = "this_not_allowed_here";
	public final static String REFERENCE_IGNORED_IN_METHODS = "reference_ignored_in_methods";
	public final static String KEYWORD_MUST_BE_IN_CLASS = "keyword_must_be_in_class";
	public final static String SUPER_NOT_AVAILABLE_PARENT = "super_not_available_parent";
	public final static String CLASS_MEMBER_DOES_NOT_EXIST = "class_member_does_not_exist";
	public final static String CLASS_STATIC_MEMBER_DOES_NOT_EXIST = "class_static_member_does_not_exist";
	public final static String EXTENDS_LOOP = "extends_loop";
	public final static String REFERENCES_DEPRECATED = "reference_deprecated";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	int mLine;
	int mChar;
	String mWord;
	String mType;
	AIFile<?> mIA;
	private String[] mParameters = null;

	public LeekCompilerException(IAWord word) {
		mLine = word.getLine();
		mChar = word.getCharacter();
		mWord = word.getWord();
		mIA = word.getAI();
		mType = "";
	}

	public LeekCompilerException(IAWord word, String informations) {
		mLine = word.getLine();
		mChar = word.getCharacter();
		mWord = word.getWord();
		mIA = word.getAI();
		mType = informations;
	}

	public LeekCompilerException(IAWord word, String informations, String[] parameters) {
		mLine = word.getLine();
		mChar = word.getCharacter();
		mWord = word.getWord();
		mIA = word.getAI();
		mType = informations;
		mParameters = parameters;
	}

	public LeekCompilerException(AIFile<?> ai, int line, int char_pos, String word, String informations) {
		mLine = line;
		mChar = char_pos;
		mWord = word;
		mIA = ai;
		mType = informations;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public String getWord() {
		return mWord;
	}

	public int getLine() {
		return mLine;
	}

	public int getChar() {
		return mChar;
	}

	@Override
	public String getMessage() {
		return mIA.getPath() + ":" + mLine + " : " + mWord + " : " + mType;
	}

	public String getError() {
		return mType;
	}

	public AIFile<?> getIA() {
		return mIA;
	}
}
