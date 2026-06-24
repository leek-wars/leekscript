package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

/**
 * Expression `match` (#2404) : pattern matching par valeur, renvoie une valeur.
 *
 * <pre>
 * var label = match (x) {
 *     1, 2 -> "petit",
 *     3    -> "trois",
 *     _    -> "autre"
 * }
 * </pre>
 *
 * Désugaré en chaîne de ternaires Java. Le sujet est évalué UNE SEULE FOIS : il est
 * assigné à un champ de classe {@code __match_<id>} dans la première comparaison
 * (l'assignation est une expression Java), les comparaisons suivantes relisent le champ.
 * Un champ par match (et non un champ partagé) pour rester correct quand une valeur de
 * cas contient elle-même un match imbriqué, ou lors d'appels récursifs.
 *
 * v1 : motifs littéraux + jokers {@code _}, type du résultat = ANY. Affinage de type,
 * motifs de type ({@code is T}) et gardes ({@code if}) = évolutions ultérieures.
 */
public class LeekMatch extends Expression {

	public static class Branch {
		public final ArrayList<Expression> values; // vide pour le cas par défaut
		public final Expression result;
		public final boolean isDefault;

		public Branch(ArrayList<Expression> values, Expression result, boolean isDefault) {
			this.values = values;
			this.result = result;
			this.isDefault = isDefault;
		}
	}

	private final Token token;
	private final int id;
	private Expression subject;
	private final ArrayList<Branch> branches = new ArrayList<>();
	private Token closingBrace;

	public LeekMatch(Token token, int id) {
		this.token = token;
		this.id = id;
	}

	public void setSubject(Expression subject) {
		this.subject = subject;
	}

	public void addBranch(Branch branch) {
		branches.add(branch);
	}

	public void setClosingBrace(Token brace) {
		this.closingBrace = brace;
	}

	public boolean hasDefault() {
		for (var b : branches) if (b.isDefault) return true;
		return false;
	}

	@Override
	public int getNature() {
		return EXPRESSION;
	}

	@Override
	public Type getType() {
		return Type.ANY;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (subject != null) subject.preAnalyze(compiler);
		for (var b : branches) {
			for (var v : b.values) v.preAnalyze(compiler);
			if (b.result != null) b.result.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		operations = 0;
		if (subject != null) {
			subject.analyze(compiler);
			operations += subject.getOperations();
		}
		for (var b : branches) {
			for (var v : b.values) {
				v.analyze(compiler);
				operations += 1 + v.getOperations(); // coût d'une comparaison eq (conservateur : tous comptés)
			}
			if (b.result != null) b.result.analyze(compiler);
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.registerMatchTemp(id);
		String tmp = "__match_" + id;

		if (parenthesis) writer.addCode("(");

		boolean subjectEmitted = false;
		// Branches de valeur, dans l'ordre : cond_0 ? r_0 : cond_1 ? r_1 : ... : default
		// (le ternaire Java est associatif à droite, donc pas de parenthèses imbriquées nécessaires)
		for (var b : branches) {
			if (b.isDefault) continue;
			writer.addCode("(");
			boolean firstValue = true;
			for (var v : b.values) {
				if (!firstValue) writer.addCode(" || ");
				writer.addCode("eq(");
				if (!subjectEmitted) {
					writer.addCode(tmp + " = ");
					subject.writeJavaCode(mainblock, writer, false);
					subjectEmitted = true;
				} else {
					writer.addCode(tmp);
				}
				writer.addCode(", ");
				v.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
				firstValue = false;
			}
			writer.addCode(") ? ");
			writeResult(mainblock, writer, b.result);
			writer.addCode(" : ");
		}

		// Cas par défaut (ou null si absent)
		Expression def = null;
		for (var b : branches) if (b.isDefault) { def = b.result; break; }
		if (def != null) {
			writeResult(mainblock, writer, def);
		} else {
			writer.addCode("null");
		}

		if (parenthesis) writer.addCode(")");
	}

	// Chaque résultat est boxé en Object (type du match = ANY) ; ops du résultat comptées
	// inline pour ne facturer que la branche réellement exécutée.
	private void writeResult(MainLeekBlock mainblock, JavaWriter writer, Expression result) {
		boolean countOps = writer.isOperationsEnabled() && result.getOperations() > 0;
		if (countOps) writer.addCode("ops(");
		writer.addCode("(Object) (");
		result.writeJavaCode(mainblock, writer, false);
		writer.addCode(")");
		if (countOps) writer.addCode(", " + result.getOperations() + ")");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if (subject == null || branches.isEmpty()) {
			throw new LeekExpressionException(this, Error.UNCOMPLETE_EXPRESSION);
		}
		return true;
	}

	@Override
	public Location getLocation() {
		if (closingBrace != null) return new Location(token.getLocation(), closingBrace.getLocation());
		return token.getLocation();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("match (");
		sb.append(subject == null ? "?" : subject.toString()).append(") {");
		for (var b : branches) {
			if (b.isDefault) {
				sb.append(" _");
			} else {
				boolean first = true;
				sb.append(" ");
				for (var v : b.values) {
					if (!first) sb.append(", ");
					sb.append(v.toString());
					first = false;
				}
			}
			sb.append(" -> ").append(b.result == null ? "?" : b.result.toString());
		}
		sb.append(" }");
		return sb.toString();
	}
}
