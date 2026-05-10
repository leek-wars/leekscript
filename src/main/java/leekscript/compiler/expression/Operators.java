package leekscript.compiler.expression;

import java.util.HashMap;
import java.util.Map;

public class Operators {
	public final static int CROCHET = 1;
	public final static int PARENTHESIS = 2;
	public final static int INCREMENT = 3;
	public final static int DECREMENT = 4;
	public final static int NOT = 5;
	public final static int MULTIPLIE = 6;
	public final static int DIVIDE = 7;
	public final static int MODULUS = 8;
	public final static int ADD = 9;
	public final static int MINUS = 10;
	public final static int LESS = 11;
	public final static int LESSEQUALS = 12;
	public final static int MORE = 13;
	public final static int MOREEQUALS = 14;
	public final static int EQUALS = 15;
	public final static int NOTEQUALS = 16;
	public final static int AND = 17;
	public final static int OR = 18;
	public final static int ASSIGN = 19;
	public final static int ADDASSIGN = 20;
	public final static int MINUSASSIGN = 21;
	public final static int MULTIPLIEASSIGN = 22;
	public final static int DIVIDEASSIGN = 23;
	public final static int MODULUSASSIGN = 24;
	public final static int UNARY_MINUS = 25;
	public final static int TERNAIRE = 26;
	public final static int DOUBLE_POINT = 27;
	public final static int POWER = 28;
	public final static int POWERASSIGN = 29;
	public final static int EQUALS_EQUALS = 30;
	public final static int NOT_EQUALS_EQUALS = 31;
	public final static int SHIFT_LEFT = 32;
	public final static int SHIFT_UNSIGNED_RIGHT = 33;
	public final static int SHIFT_RIGHT = 34;
	public final static int BITXOR = 35;
	public final static int BITAND = 36;
	public final static int BITOR = 37;
	public final static int SHIFT_LEFT_ASSIGN = 38;
	public final static int SHIFT_UNSIGNED_RIGHT_ASSIGN = 39;
	public final static int SHIFT_RIGHT_ASSIGN = 40;
	public final static int BITXOR_ASSIGN = 41;
	public final static int BITAND_ASSIGN = 42;
	public final static int BITOR_ASSIGN = 43;
	public final static int PRE_INCREMENT = 44;
	public final static int PRE_DECREMENT = 45;
	public final static int REFERENCE = 46;
	public final static int BITNOT = 47;
	public final static int NEW = 48;
	public final static int DOT = 49;
	public final static int INSTANCEOF = 50;
	public final static int INTEGER_DIVISION = 51;
	public final static int INTEGER_DIVISION_EQ = 52;
	public final static int AS = 53;
	public final static int NON_NULL_ASSERTION = 54;
	public final static int XOR = 55;
	public final static int IN = 56;
	public final static int NOT_IN = 57;
	public final static int COALESCE = 58;
	public final static int COALESCE_ASSIGN = 59;

	// Lookup HashMap pour getOperator au lieu de ~50 String.equals séquentiels.
	// Cas spécial : "^=" est version-dépendant (BITXOR_ASSIGN en v≥2, POWERASSIGN
	// en v1) → géré explicitement dans getOperator.
	private static final Map<String, Integer> OPERATOR_MAP = buildOperatorMap();
	private static Map<String, Integer> buildOperatorMap() {
		var m = new HashMap<String, Integer>(96);
		m.put("[", CROCHET);
		m.put("(", PARENTHESIS);
		m.put("++", INCREMENT);
		m.put("--", DECREMENT);
		m.put("!", NON_NULL_ASSERTION);
		m.put("*", MULTIPLIE);
		m.put("/", DIVIDE);
		m.put("%", MODULUS);
		m.put("+", ADD);
		m.put("-", MINUS);
		m.put("<", LESS);
		m.put("<=", LESSEQUALS);
		m.put(">", MORE);
		m.put(">=", MOREEQUALS);
		m.put("==", EQUALS);
		m.put("!=", NOTEQUALS);
		m.put("&&", AND);
		m.put("||", OR);
		m.put("xor", XOR);
		m.put("=", ASSIGN);
		m.put("+=", ADDASSIGN);
		m.put("-=", MINUSASSIGN);
		m.put("*=", MULTIPLIEASSIGN);
		m.put("/=", DIVIDEASSIGN);
		m.put("%=", MODULUSASSIGN);
		m.put("?", TERNAIRE);
		m.put(":", DOUBLE_POINT);
		m.put("**", POWER);
		m.put("**=", POWERASSIGN);
		m.put("===", EQUALS_EQUALS);
		m.put("!==", NOT_EQUALS_EQUALS);
		m.put("^", BITXOR);
		m.put("&", BITAND);
		m.put("|", BITOR);
		m.put("~", BITNOT);
		m.put("<<", SHIFT_LEFT);
		m.put(">>", SHIFT_RIGHT);
		m.put(">>>", SHIFT_UNSIGNED_RIGHT);
		m.put("&=", BITAND_ASSIGN);
		m.put("|=", BITOR_ASSIGN);
		m.put("<<=", SHIFT_LEFT_ASSIGN);
		m.put(">>=", SHIFT_RIGHT_ASSIGN);
		m.put(">>>=", SHIFT_UNSIGNED_RIGHT_ASSIGN);
		m.put("@", REFERENCE);
		m.put("new", NEW);
		m.put(".", DOT);
		m.put("instanceof", INSTANCEOF);
		m.put("\\", INTEGER_DIVISION);
		m.put("\\=", INTEGER_DIVISION_EQ);
		m.put("as", AS);
		m.put("in", IN);
		m.put("not in", NOT_IN);
		m.put("??", COALESCE);
		m.put("??=", COALESCE_ASSIGN);
		return m;
	}

	public final static int getOperator(String operator, int version) {
		// Cas version-dépendant : "^=" est BITXOR_ASSIGN en v≥2, POWERASSIGN en v1.
		if (operator.equals("^=")) {
			return version >= 2 ? BITXOR_ASSIGN : POWERASSIGN;
		}
		Integer op = OPERATOR_MAP.get(operator);
		return op != null ? op : -1;
	}

	public static int getPriority(int operator) {
		switch(operator) {
		case CROCHET:
		case PARENTHESIS:
			return 17;
		case DOT:
			return 16;
		case AS:
		case INCREMENT:
		case DECREMENT:
		case REFERENCE:
		case UNARY_MINUS:
			return 15;
		case NOT:
		case NON_NULL_ASSERTION:
		case BITNOT:
			return 14;
		case NEW:
			return 13;
		case POWER:
			return 12;
		case MULTIPLIE:
		case DIVIDE:
		case INTEGER_DIVISION:
		case MODULUS:
			return 11;
		case ADD:
		case MINUS:
			return 10;
		case SHIFT_LEFT:
		case SHIFT_RIGHT:
		case SHIFT_UNSIGNED_RIGHT:
			return 9;
		case LESS:
		case LESSEQUALS:
		case MORE:
		case MOREEQUALS:
		case INSTANCEOF:
		case IN:
		case NOT_IN:
			return 8;
		case EQUALS:
		case NOTEQUALS:
		case EQUALS_EQUALS:
		case NOT_EQUALS_EQUALS:
			return 7;
		case BITAND:
			return 6;
		case BITXOR:
			return 5;
		case BITOR:
			return 4;
		case AND:
			return 3;
		case OR:
			return 2;
		case COALESCE:
			return 2;
		case TERNAIRE:
		case DOUBLE_POINT:
			return 1;
		case ASSIGN:
		case ADDASSIGN:
		case MINUSASSIGN:
		case MULTIPLIEASSIGN:
		case DIVIDEASSIGN:
		case INTEGER_DIVISION_EQ:
		case MODULUSASSIGN:
		case POWERASSIGN:
		case SHIFT_LEFT_ASSIGN:
		case SHIFT_RIGHT_ASSIGN:
		case SHIFT_UNSIGNED_RIGHT_ASSIGN:
		case BITAND_ASSIGN:
		case BITOR_ASSIGN:
		case BITXOR_ASSIGN:
		case COALESCE_ASSIGN:
			return 0;
		default:
			return -1;
		}
	}

	public static boolean isAssign(int operator) {
		switch (operator) {
		case ASSIGN:
		case ADDASSIGN:
		case MINUSASSIGN:
		case MULTIPLIEASSIGN:
		case DIVIDEASSIGN:
		case INTEGER_DIVISION_EQ:
		case MODULUSASSIGN:
		case POWERASSIGN:
		case SHIFT_LEFT_ASSIGN:
		case SHIFT_RIGHT_ASSIGN:
		case SHIFT_UNSIGNED_RIGHT_ASSIGN:
		case BITAND_ASSIGN:
		case BITOR_ASSIGN:
		case BITXOR_ASSIGN:
		case COALESCE_ASSIGN:
			return true;
		}
		return false;
	}

	public static boolean isIncrement(int operator) {
		switch (operator) {
		case INCREMENT:
		case DECREMENT:
		case PRE_INCREMENT:
		case PRE_DECREMENT:
			return true;
		}
		return false;
	}

	public static boolean isUnaryPrefix(int operator) {
		switch(operator) {
		case NOT:
		case UNARY_MINUS:
		case PRE_INCREMENT:
		case PRE_DECREMENT:
		case REFERENCE:
		case BITNOT:
		case NEW:
			return true;
		}
		return false;
	}

	public static boolean isUnary(int operator) {
		return isUnaryPrefix(operator) || isUnarySuffix(operator);
	}

	public static boolean isUnarySuffix(int operator) {
		switch(operator) {
		case INCREMENT:
		case DECREMENT:
		case NON_NULL_ASSERTION:
			return true;
		}
		return false;
	}

	public static String getString(int operator) {
		switch (operator) {
		case ADD:
			return "+";
		case ADDASSIGN:
			return "+=";
		case MINUS:
		case UNARY_MINUS:
			return "-";
		case MINUSASSIGN:
			return "-=";
		case MULTIPLIE:
			return "*";
		case MULTIPLIEASSIGN:
			return "*=";
		case INTEGER_DIVISION:
			return "\\";
		case INTEGER_DIVISION_EQ:
			return "\\=";
		case DIVIDE:
			return "/";
		case DIVIDEASSIGN:
			return "/=";
		case MODULUS:
			return "%";
		case ASSIGN:
			return "=";
		case AND:
			return "&&";
		case OR:
			return "||";
		case LESS:
			return "<";
		case LESSEQUALS:
			return "<=";
		case MORE:
			return ">";
		case MOREEQUALS:
			return ">=";
		case EQUALS:
			return "==";
		case EQUALS_EQUALS:
			return "===";
		case NOTEQUALS:
			return "!=";
		case NOT_EQUALS_EQUALS:
			return "!==";
		case NOT:
		case NON_NULL_ASSERTION:
			return "!";
		case BITAND:
			return "&";
		case BITAND_ASSIGN:
			return "&=";
		case BITOR:
			return "|";
		case BITOR_ASSIGN:
			return "|=";
		case BITXOR:
			return "^";
		case BITXOR_ASSIGN:
			return "^=";
		case SHIFT_LEFT:
			return "<<";
		case SHIFT_LEFT_ASSIGN:
			return "<<=";
		case SHIFT_RIGHT:
			return ">>";
		case SHIFT_RIGHT_ASSIGN:
			return ">>=";
		case SHIFT_UNSIGNED_RIGHT:
			return ">>>";
		case SHIFT_UNSIGNED_RIGHT_ASSIGN:
			return ">>>=";
		case INCREMENT:
		case PRE_INCREMENT:
			return "++";
		case DECREMENT:
		case PRE_DECREMENT:
			return "--";
		case POWER:
			return "**";
		case POWERASSIGN:
			return "**=";
		case NEW:
			return "new";
		case BITNOT:
			return "~";
		case REFERENCE:
			return "@";
		case INSTANCEOF:
			return "instanceof";
		case AS:
			return "as";
		case IN:
			return "in";
		case NOT_IN:
			return "not in";
		case XOR:
			return "xor";
		case COALESCE:
			return "??";
		case COALESCE_ASSIGN:
			return "??=";
		}
		return "null";
	}
}
