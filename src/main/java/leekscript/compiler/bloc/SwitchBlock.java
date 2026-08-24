package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.instruction.LeekBreakInstruction;

public class SwitchBlock extends AbstractLeekBlock {

	private final Token token;
	private Expression mExpression;
	private final ArrayList<SwitchCase> mCases = new ArrayList<>();
	private final int mId;

	public static class SwitchCase {
		public final ArrayList<Expression> values; // empty for default
		public final AbstractLeekBlock body;
		public final boolean isDefault;

		public SwitchCase(ArrayList<Expression> values, AbstractLeekBlock body, boolean isDefault) {
			this.values = values;
			this.body = body;
			this.isDefault = isDefault;
		}
	}

	public SwitchBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
		this.mId = main.getCount();
	}

	public void setExpression(Expression expression) {
		mExpression = expression;
	}

	public void addCase(SwitchCase switchCase) {
		mCases.add(switchCase);
	}

	@Override
	public String getCode() {
		StringBuilder str = new StringBuilder("switch (");
		str.append(mExpression.toString()).append(") {\n");
		for (var c : mCases) {
			if (c.isDefault) {
				str.append("default:\n");
			} else {
				for (var v : c.values) {
					str.append("case ").append(v.toString()).append(":\n");
				}
			}
			str.append(c.body.getCode());
		}
		str.append("}");
		return str.toString();
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.preAnalyze(compiler);
		for (var c : mCases) {
			for (var v : c.values) {
				v.preAnalyze(compiler);
			}
			c.body.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.analyze(compiler);

		// Check if switch expression is a nullable variable for narrowing
		LeekVariable switchVar = null;
		boolean hasNullCase = false;

		var v = mExpression.getVariable();
		if (v != null && v.getType().canBeNull()) {
			switchVar = v;
			// Check if there's a case null anywhere
			for (var c : mCases) {
				for (var val : c.values) {
					if (val instanceof LeekNull) {
						hasNullCase = true;
						break;
					}
				}
				if (hasNullCase) break;
			}
		}

		for (var c : mCases) {
			for (var val : c.values) {
				val.analyze(compiler);
			}

			if (switchVar != null) {
				// Determine narrowed type for this case
				boolean caseHasNull = false;
				boolean caseHasNonNull = false;
				for (var val : c.values) {
					if (val instanceof LeekNull) caseHasNull = true;
					else caseHasNonNull = true;
				}

				Type narrowedType = null;
				if (caseHasNull && !caseHasNonNull) {
					// Pure null case: narrow to null
					narrowedType = Type.NULL;
				} else if (!caseHasNull && !c.isDefault) {
					// Non-null case values: narrow to non-null
					narrowedType = switchVar.getType().assertNotNull();
				} else if (c.isDefault && hasNullCase) {
					// Default with explicit null case elsewhere: narrow to non-null
					narrowedType = switchVar.getType().assertNotNull();
				}

				if (narrowedType != null) {
					var savedType = switchVar.getType();
					switchVar.setType(narrowedType);
					c.body.analyze(compiler);
					switchVar.setType(savedType);
				} else {
					c.body.analyze(compiler);
				}
			} else {
				c.body.analyze(compiler);
			}
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		// getEndBlock() est volontairement conservateur : un case qui fallthrough ne
		// compte pas comme retournant. Mais javac, lui, raisonne par groupes : avec
		// un default et un dernier groupe abrupt (return/continue), il peut prouver
		// que le switch ne se termine jamais normalement et rejeter le code émis
		// après en « unreachable statement » — typiquement le return implicite de la
		// fonction. Le bloc s'ouvre alors en `if (true) {`, exempté de l'analyse
		// d'atteignabilité (JLS 14.21), sans effet sur le bytecode. Surtout pas de
		// bouclier quand getEndBlock() == 1 : le return implicite n'est alors pas
		// émis du tout, et javac réclamerait un « missing return statement ».
		// Les accolades du bloc sont ouvertes/fermées ici pour les trois chemins.
		boolean shield = hasDefault() && getEndBlock() == 0;
		writer.addLine(shield ? "if (true) {" : "{");
		var dispatch = buildConstantDispatch(mainblock);
		if (dispatch == null) {
			writeComparisonChain(mainblock, writer);
		} else if (dispatch.direct) {
			writeDirectDispatch(mainblock, writer, dispatch);
		} else {
			writeGuardedDispatch(mainblock, writer, dispatch);
		}
		writer.addLine("}"); // end block
	}

	/**
	 * Plan de dispatch en O(1).
	 *
	 * Le `switch` de LeekScript compare avec `eq()`, l'égalité lâche (`eq(1, "1")`
	 * est vrai), et ses labels peuvent être des expressions quelconques : d'où la
	 * chaîne de comparaisons linéaire du cas général. Mais dès que tous les labels
	 * sont des constantes d'un même genre et que le sujet est un entier ou une
	 * chaîne, `eq()` se réduit à l'égalité stricte — on peut alors émettre un vrai
	 * `switch` Java (tableswitch / lookupswitch / dispatch par hash sur String) et
	 * ne facturer qu'1 opération au lieu d'une par cas testé.
	 *
	 * Deux formes : {@link #writeDirectDispatch} quand le sujet est typé exactement
	 * `integer`/`string`, {@link #writeGuardedDispatch} quand il est dynamique (le
	 * cas d'un `var`) — un `instanceof` choisit alors entre le dispatch rapide et la
	 * chaîne `eq()`, qui reste seule à même de traiter les valeurs d'un autre type.
	 */
	private static class ConstantDispatch {
		/** Labels chaîne (`case "a":`) plutôt qu'entiers (`case 1:`). */
		final boolean string;
		/** Le sujet est déjà du bon type Java : pas besoin de garde `instanceof`. */
		final boolean direct;
		/** Labels Java déjà formatés, un paquet par case (indexé comme mCases). */
		final ArrayList<ArrayList<String>> labels = new ArrayList<>();
		/** Valeur injectée quand le sujet ne peut correspondre à aucun label (direct seulement). */
		String sentinel;

		ConstantDispatch(boolean string, boolean direct) {
			this.string = string;
			this.direct = direct;
		}
	}

	private static Expression unwrapParenthesis(Expression expression) {
		while (expression instanceof LeekParenthesis parenthesis) {
			expression = parenthesis.getExpression();
		}
		return expression;
	}

	/** Valeur d'un label entier constant, ou null si le label n'est pas une constante entière. */
	private static Long constantInt(Expression expression) {
		expression = unwrapParenthesis(expression);
		if (expression instanceof LeekNumber number) {
			return number.getType() == Type.INT ? number.getLongValue() : null;
		}
		// `case -1:` n'est pas un littéral mais un moins unaire appliqué à un littéral.
		if (expression instanceof LeekExpression operation && operation.getOperator() == Operators.UNARY_MINUS && operation.getExpression2() != null) {
			var operand = unwrapParenthesis(operation.getExpression2());
			if (operand instanceof LeekNumber number && number.getType() == Type.INT) {
				return -number.getLongValue();
			}
		}
		return null;
	}

	private ConstantDispatch buildConstantDispatch(MainLeekBlock mainblock) {
		// Les labels d'un case `default:` sont ignorés partout (le `default` les
		// couvre déjà), exactement comme dans la chaîne de comparaisons.
		var labelled = new ArrayList<SwitchCase>();
		int defaults = 0;
		for (var c : mCases) {
			if (c.isDefault) defaults++;
			else if (!c.values.isEmpty()) labelled.add(c);
		}
		// Deux `default` donneraient un switch Java invalide ; sans label constant
		// il n'y a rien à dispatcher.
		if (defaults > 1 || labelled.isEmpty()) return null;

		// Genre des labels : tous entiers constants, ou tous chaînes littérales.
		boolean string = unwrapParenthesis(labelled.get(0).values.get(0)) instanceof LeekString;

		var type = mExpression.getType();
		boolean direct;
		if (type == (string ? Type.STRING : Type.INT)) {
			direct = true;
		} else if (type == Type.INT || type == Type.STRING || type == Type.REAL || type == Type.BOOL || type == Type.NULL) {
			// Sujet d'un type incompatible avec les labels : `eq()` lâche obligatoire
			// (eq(1.0, 1) est vrai), et une garde `instanceof` serait morte.
			return null;
		} else {
			direct = false;
		}

		var dispatch = new ConstantDispatch(string, direct);
		var seen = new LinkedHashSet<String>();

		for (var c : mCases) {
			var labels = new ArrayList<String>();
			if (!c.isDefault) {
				for (var value : c.values) {
					String label;
					if (string) {
						if (!(unwrapParenthesis(value) instanceof LeekString literal)) return null;
						label = literal.getJavaLiteral(mainblock);
						// Un NUL dans un label rendrait la sentinelle ambiguë (cf. stringSentinel).
						if (label.indexOf('\0') >= 0 || label.contains("\\0") || label.contains("\\u0000")) return null;
					} else {
						var constant = constantInt(value);
						if (constant == null) return null;
						// Java ne sait switcher que sur int : hors plage, on garde la chaîne.
						if (constant < Integer.MIN_VALUE || constant > Integer.MAX_VALUE) return null;
						label = String.valueOf(constant.intValue());
					}
					// Doublon : la chaîne retient le premier cas, alors qu'un switch Java
					// refuserait de compiler. On préserve la sémantique en restant dessus.
					if (!seen.add(label)) return null;
					labels.add(label);
				}
			}
			dispatch.labels.add(labels);
		}

		if (direct) {
			dispatch.sentinel = string ? stringSentinel() : intSentinel(seen);
		}
		return dispatch;
	}

	/** Un int qui n'est label d'aucun case (il en existe toujours un : les labels sont en nombre fini). */
	private static String intSentinel(Set<String> labels) {
		for (long value = Integer.MIN_VALUE; value <= Integer.MAX_VALUE; value++) {
			var candidate = String.valueOf((int) value);
			if (!labels.contains(candidate)) {
				// Émis symboliquement : `-2147483648` en littéral est un cas particulier du JLS.
				return value == Integer.MIN_VALUE ? "Integer.MIN_VALUE" : candidate;
			}
		}
		throw new IllegalStateException("switch: aucune sentinelle entière disponible");
	}

	/** Aucun label ne contient de NUL (vérifié dans buildConstantDispatch), donc "\0" ne peut collisionner. */
	private static String stringSentinel() {
		return "\"\\0\"";
	}

	/**
	 * Sujet déjà typé `integer` ou `string` : un seul `switch` Java, sans variable
	 * d'index intermédiaire et sans boxing du sujet.
	 */
	private void writeDirectDispatch(MainLeekBlock mainblock, JavaWriter writer, ConstantDispatch dispatch) {
		String swVar = "__sw_" + mId;

		if (dispatch.string) {
			writer.addCode("String " + swVar + " = ");
			writer.getString(mainblock, mExpression, false);
		} else {
			writer.addCode("long " + swVar + " = ");
			writer.getInt(mainblock, mExpression, false);
		}
		writer.addLine(";", getLocation());

		if (writer.isOperationsEnabled() && mExpression.getOperations() > 0) {
			writer.addCounter(mExpression.getOperations());
		}
		// Le dispatch est en O(1) : 1 opération, quel que soit le nombre de cas.
		writer.addCounter(1);

		if (dispatch.string) {
			// Un sujet null ne correspond à aucun label (eq(null, "x") est faux) mais
			// ferait lever une NPE au switch Java : on le renvoie sur la sentinelle.
			writer.addLine("switch (" + swVar + " == null ? " + dispatch.sentinel + " : " + swVar + ") {");
		} else {
			String skVar = "__swk_" + mId;
			// Hors plage int la troncature pourrait faire matcher un label par
			// accident : on détecte la perte et on renvoie sur la sentinelle.
			writer.addLine("int " + skVar + " = (int) " + swVar + ";");
			writer.addLine("switch (" + skVar + " == " + swVar + " ? " + skVar + " : " + dispatch.sentinel + ") {");
		}

		int index = 0;
		for (var c : mCases) {
			for (var label : dispatch.labels.get(index)) {
				writer.addLine("case " + label + ":");
			}
			if (c.isDefault) {
				writer.addLine("default:");
			}
			writer.addLine("{");
			writer.addCounter(1);
			c.body.writeJavaCode(mainblock, writer, false);
			writer.addLine("}");
			index++;
		}

		writer.addLine("}"); // end switch
	}

	/**
	 * Sujet dynamique (un `var`) : `instanceof` sur le type des labels. Si ça
	 * correspond, `eq()` se réduit à l'égalité stricte et un switch Java calcule
	 * l'index en O(1) ; sinon on retombe sur la chaîne, seule capable d'appliquer
	 * l'égalité lâche entre types différents. Les corps ne sont émis qu'une fois.
	 */
	private void writeGuardedDispatch(MainLeekBlock mainblock, JavaWriter writer, ConstantDispatch dispatch) {
		String swVar = "__sw_" + mId;
		String siVar = "__si_" + mId;
		String svVar = "__swv_" + mId;

		writer.addCode("Object " + swVar + " = ");
		mExpression.writeJavaCode(mainblock, writer, false);
		writer.addLine(";", getLocation());

		if (writer.isOperationsEnabled() && mExpression.getOperations() > 0) {
			writer.addCounter(mExpression.getOperations());
		}

		writer.addLine("int " + siVar + " = -1;");
		writer.addLine("if (" + swVar + " instanceof " + (dispatch.string ? "String" : "Long") + " " + svVar + ") {");
		writer.addCounter(1);

		String selector;
		if (dispatch.string) {
			selector = svVar;
		} else {
			String skVar = "__swk_" + mId;
			// Hors plage int, aucun label entier ne peut correspondre : on laisse
			// l'index à -1 plutôt que de risquer un match par troncature.
			writer.addLine("int " + skVar + " = (int) (long) " + svVar + ";");
			writer.addLine("if (" + skVar + " == (long) " + svVar + ")");
			selector = skVar;
		}
		writer.addLine("switch (" + selector + ") {");
		int index = 0;
		for (var c : mCases) {
			if (!dispatch.labels.get(index).isEmpty()) {
				for (var label : dispatch.labels.get(index)) {
					writer.addLine("case " + label + ":");
				}
				writer.addLine(siVar + " = " + index + "; break;");
			}
			index++;
		}
		writer.addLine("}");

		writer.addLine("} else {");
		writeIndexChain(mainblock, writer, swVar, siVar);
		writer.addLine("}");

		writeBodies(mainblock, writer, siVar);
	}

	/** Chemin général : labels non constants ou sujet de type incompatible, `eq()` obligatoire. */
	private void writeComparisonChain(MainLeekBlock mainblock, JavaWriter writer) {
		String swVar = "__sw_" + mId;
		String siVar = "__si_" + mId;

		// Store the switch expression in a temp variable
		writer.addCode("Object " + swVar + " = ");
		mExpression.writeJavaCode(mainblock, writer, false);
		writer.addLine(";", getLocation());

		if (writer.isOperationsEnabled() && mExpression.getOperations() > 0) {
			writer.addCounter(mExpression.getOperations());
		}

		writer.addLine("int " + siVar + " = -1;");
		writeIndexChain(mainblock, writer, swVar, siVar);
		writeBodies(mainblock, writer, siVar);
	}

	/**
	 * Chaîne if/else if calculant l'index du case retenu.
	 * Chaque comparaison coûte 1 op + les ops de l'expression du label, comme un if/else if.
	 */
	private void writeIndexChain(MainLeekBlock mainblock, JavaWriter writer, String swVar, String siVar) {
		boolean first = true;
		int index = 0;
		for (var c : mCases) {
			if (!c.isDefault) {
				if (first) {
					writer.addCode("if (");
					first = false;
				} else {
					writer.addCode("else if (");
				}
				// Compute operation cost: 1 per eq() + value expression ops
				int ops = 0;
				for (var v : c.values) {
					ops += 1 + v.getOperations();
				}
				if (writer.isOperationsEnabled() && ops > 0) {
					writer.addCode("ops(");
				}
				boolean firstValue = true;
				for (var v : c.values) {
					if (!firstValue) {
						writer.addCode(" || ");
					}
					writer.addCode("eq(" + swVar + ", ");
					v.writeJavaCode(mainblock, writer, false);
					writer.addCode(")");
					firstValue = false;
				}
				if (writer.isOperationsEnabled() && ops > 0) {
					writer.addCode(", " + ops + ")");
				}
				writer.addLine(") " + siVar + " = " + index + ";");
			}
			index++;
		}
	}

	/** Switch Java sur l'index calculé : le fallthrough et la position du `default` sont préservés. */
	private void writeBodies(MainLeekBlock mainblock, JavaWriter writer, String siVar) {
		writer.addLine("switch (" + siVar + ") {");

		int index = 0;
		for (var c : mCases) {
			if (c.isDefault) {
				writer.addLine("default: {");
			} else {
				writer.addLine("case " + index + ": {");
			}
			writer.addCounter(1);
			c.body.writeJavaCode(mainblock, writer, false);
			writer.addLine("}");
			index++;
		}

		writer.addLine("}"); // end switch
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	@Override
	public int getEndBlock() {
		// Le switch « retourne toujours » si : il a un default, chaque case retourne,
		// et aucun break n'en sort — un `if (x) break` avant un return suffit à le
		// rendre terminable normalement, et javac exigerait alors un return après.
		// Lu aussi par writeJavaCode pour décider du bouclier `if (true)`.
		for (var c : mCases) {
			if (c.body.getEndBlock() != 1 || containsBreak(c.body)) return 0;
		}
		return hasDefault() ? 1 : 0;
	}

	private boolean hasDefault() {
		for (var c : mCases) {
			if (c.isDefault) return true;
		}
		return false;
	}

	/**
	 * Cherche un `break` qui vise ce switch : direct dans le corps du case, ou caché
	 * dans un if/else (les else sont des instructions sœurs du if, donc parcourus).
	 * Seuls les blocs qui capturent le break (boucles, switchs imbriqués) arrêtent
	 * la descente ; tout autre bloc est traversé — un futur bloc inconnu se dégrade
	 * ainsi en bouclier superflu, pas en « missing return statement » javac.
	 */
	private static boolean containsBreak(AbstractLeekBlock block) {
		for (var instruction : block.getInstructions()) {
			if (instruction instanceof LeekBreakInstruction) return true;
			if (instruction instanceof AbstractLeekBlock bloc && !capturesBreak(bloc) && containsBreak(bloc)) return true;
		}
		return false;
	}

	/** Blocs qu'un `break` interne vise eux-mêmes, plutôt que ce switch. */
	private static boolean capturesBreak(AbstractLeekBlock block) {
		return block instanceof WhileBlock || block instanceof DoWhileBlock || block instanceof ForBlock
			|| block instanceof ForeachBlock || block instanceof ForeachKeyBlock || block instanceof SwitchBlock;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		return 0;
	}

	@Override
	public Type getType() {
		return Type.VOID;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return false;
	}
}
