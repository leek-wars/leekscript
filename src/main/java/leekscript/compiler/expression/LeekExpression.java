package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.runner.values.LeekValue;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.common.Type.CastType;

public class LeekExpression extends Expression {

	protected int mOperator = -1;
	private Token mOperatorToken;
	protected Expression mExpression1 = null;
	protected Expression mExpression2 = null;
	protected LeekExpression mParent = null;
	private Type type = Type.ANY;

	public LeekExpression() {}

	public void setParent(LeekExpression parent) {
		mParent = parent;
	}

	public boolean hasTernaire() {
		if (mExpression1 != null && mExpression1.getNature() == EXPRESSION && ((LeekExpression) mExpression1).hasTernaire())
			return true;
		if (mExpression2 != null && mExpression2.getNature() == EXPRESSION && ((LeekExpression) mExpression2).hasTernaire())
			return true;
		return false;
	}

	public LeekExpression getParent() {
		return mParent;
	}

	public Expression getExpression1() {
		return mExpression1;
	}

	public void setExpression1(Expression e) {
		mExpression1 = e;
	}

	public void setExpression2(Expression e) {
		mExpression2 = e;
	}

	public Expression getExpression2() {
		return mExpression2;
	}

	public int getOperator() {
		return mOperator;
	}

	public Token getOperatorToken() {
		return mOperatorToken;
	}

	public int getLastOperator() {
		if (mExpression2 != null && mExpression2 instanceof LeekExpression) {
			return ((LeekExpression) mExpression2).getLastOperator();
		}
		return mOperator;
	}

	public Expression getLastExpression() {
		if (mExpression2 != null) {
			if (mExpression2 instanceof LeekExpression) {
				return ((LeekExpression) mExpression2).getLastExpression();
			}
			return mExpression2;
		}
		if (mExpression1 != null) {
			if (mExpression1 instanceof LeekExpression) {
				return ((LeekExpression) mExpression1).getLastExpression();
			}
			return mExpression1;
		}
		return mExpression1;
	}

	public void setOperator(int operator, Token operatorToken) {
		mOperator = operator;
		mOperatorToken = operatorToken;
		operatorToken.setExpression(this);
	}

	public boolean needOperator() {
		if (mExpression1 != null) {
			if (mExpression1.getNature() == EXPRESSION && !((LeekExpression) mExpression1).complete())
				return ((LeekExpression) mExpression1).needOperator();
			if (mOperator == -1)
				return true;
		}
		if (mExpression2 != null) {
			if (mExpression2.getNature() == EXPRESSION)
				return ((LeekExpression) mExpression2).needOperator();
			return true;
		}
		return false;
	}

	public LeekExpression lastExpression() {
		if (mExpression2 != null && mExpression2.getNature() == EXPRESSION)
			return ((LeekExpression) mExpression2).lastExpression();
		else
			return this;
	}

	@Override
	public int getNature() {
		return Expression.EXPRESSION;
	}

	@Override
	public Type getType() {
		return type;
	}

	public boolean complete(int operator) {
		return complete();
	}

	public boolean complete() {
		if (mExpression1 == null || mExpression2 == null)
			return false;
		if (mExpression1.getNature() == EXPRESSION && !((LeekExpression) mExpression1).complete())
			return false;
		if (mExpression2.getNature() == EXPRESSION && !((LeekExpression) mExpression2).complete())
			return false;
		return true;
	}

	public void addExpression(Expression expression) {
		if (mExpression1 == null)
			mExpression1 = expression;
		else if (mExpression1.getNature() == EXPRESSION && !((LeekExpression) mExpression1).complete()) {
			((LeekExpression) mExpression1).addExpression(expression);
		}
		else {
			if (mExpression2 == null)
				mExpression2 = expression;
			else
				((LeekExpression) mExpression2).addExpression(expression);
		}
	}

	public void addUnaryPrefix(int operator, Token operatorToken) {
		// On doit trouver à quel endroit de l'arborescence on doit placer le
		// préfix
		// En général c'est un => !
		LeekExpression exp = new LeekExpression();
		exp.setOperator(operator, operatorToken);
		exp.setParent(this);
		exp.setExpression1(new LeekNull(operatorToken));
		addExpression(exp);
	}

	public void addBracket(Token bracket, Expression casevalue, Token colon, Expression endIndex, Token colon2, Expression stride, Token closingBracket) {
		// On doit ajouter ce crochet au dernier élément ajouté
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getNature() == EXPRESSION)
				((LeekExpression) mExpression1).addBracket(bracket, casevalue, colon, endIndex, colon2, stride, closingBracket);
			else {
				// On doit ajouter à l'élément mExpression1
				var exp = new LeekArrayAccess(bracket);
				exp.setTabular(mExpression1);
				exp.setCase(casevalue);
				exp.setColon(colon);
				exp.setEndIndex(endIndex);
				exp.setColon2(colon2);
				exp.setStride(stride);
				exp.setClosingBracket(closingBracket);
				mExpression1 = exp;
			}
		}
		else if (mExpression2 != null) {
			if (mExpression2.getNature() == EXPRESSION)
				((LeekExpression) mExpression2).addBracket(bracket, casevalue, colon, endIndex, colon2, stride, closingBracket);
			else {
				// On doit ajouter à l'élément mExpression2
				var exp = new LeekArrayAccess(bracket);
				exp.setTabular(mExpression2);
				exp.setCase(casevalue);
				exp.setColon(colon);
				exp.setEndIndex(endIndex);
				exp.setColon2(colon2);
				exp.setStride(stride);
				exp.setClosingBracket(closingBracket);
				mExpression2 = exp;
			}
		}
	}

	public void addObjectAccess(Token dot, Token name) {
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getNature() == EXPRESSION)
				((LeekExpression) mExpression1).addObjectAccess(dot, name);
			else {
				mExpression1 = new LeekObjectAccess(mExpression1, dot, name);
			}
		} else if (mExpression2 != null) {
			if (mExpression2.getNature() == EXPRESSION)
				((LeekExpression) mExpression2).addObjectAccess(dot, name);
			else {
				mExpression2 = new LeekObjectAccess(mExpression2, dot, name);
			}
		}
	}

	public void addFunction(LeekFunctionCall function) {
		// On doit ajouter ce crochet au dernier élément ajouté
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getNature() == EXPRESSION)
				((LeekExpression) mExpression1).addFunction(function);
			else {
				// On doit ajouter à l'élément mExpression1
				function.setExpression(mExpression1);
				mExpression1 = function;
			}
		}
		else if (mExpression2 != null) {
			if (mExpression2.getNature() == EXPRESSION)
				((LeekExpression) mExpression2).addFunction(function);
			else {
				// On doit ajouter à l'élément mExpression2
				function.setExpression(mExpression2);
				mExpression2 = function;
			}
		}
	}

	public void addUnarySuffix(int suffix, Token token) {
		// On doit ajouter ce suffix au dernier élément ajouté
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getNature() == EXPRESSION)
				((LeekExpression) mExpression1).addUnarySuffix(suffix, token);
			else {
				// On doit ajouter à l'élément mExpression1
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull(token));
				exp.setOperator(suffix, token);
				exp.setExpression2(mExpression1);
				mExpression1 = exp;
			}
		}
		else if (mExpression2 != null) {
			if (mExpression2.getNature() == EXPRESSION)
				((LeekExpression) mExpression2).addUnarySuffix(suffix, token);
			else {
				// On doit ajouter à l'élément mExpression2
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull(token));
				exp.setOperator(suffix, token);
				exp.setExpression2(mExpression2);
				mExpression2 = exp;
			}
		}
	}

	public void addTernaire(Token token) {
		// On a ajotué un opérateur ?
		if (mOperator == -1) {
			if (mExpression1 != null) {
				LeekTernaire ternaire = new LeekTernaire(token);
				ternaire.addExpression(mExpression1);
				ternaire.addOperator(Operators.TERNAIRE, null);
				ternaire.setParent(this);
				mExpression1 = ternaire;
			}
		} else {
			int operator = Operators.TERNAIRE;
			int cur_p = Operators.getPriority(mOperator);
			int p = Operators.getPriority(operator);
			if (cur_p >= p) {
				// On doit englober l'expression actuelle
				LeekExpression new_e = new LeekExpression();
				new_e.setParent(this);
				new_e.setExpression1(mExpression1);
				new_e.setExpression2(mExpression2);
				new_e.setOperator(mOperator, null);

				// Et la mettre en condition de ternaire
				LeekTernaire ternaire = new LeekTernaire(token);
				ternaire.addExpression(new_e);
				ternaire.addOperator(Operators.TERNAIRE, null);
				ternaire.setParent(this);
				mExpression1 = ternaire;
				mExpression2 = null;
				mOperator = -1;
			} else if (mExpression2.getNature() != EXPRESSION) {
				// On doit englober l'expression de droite
				LeekTernaire ternaire = new LeekTernaire(token);
				ternaire.addExpression(mExpression2);
				ternaire.addOperator(Operators.TERNAIRE, null);
				ternaire.setParent(this);
				mExpression2 = ternaire;
			} else {
				((LeekExpression) mExpression2).addOperator(operator, mOperatorToken);
			}
		}
	}

	public void replaceExpression(Expression base, Expression replacement) {
		if (mExpression1 == base) {
			if (replacement.getNature() == EXPRESSION)
				((LeekExpression) replacement).setParent(this);
			mExpression1 = replacement;
		}
		else if (mExpression2 == base) {
			if (replacement.getNature() == EXPRESSION)
				((LeekExpression) replacement).setParent(this);
			mExpression2 = replacement;
		}
	}

	public void addOperator(int operator, Token token) {
		// On doit trouver à quel endroit de l'arborescence on doit placer l'opérateur
		if (mExpression1 != null && mExpression1.getNature() == EXPRESSION && !((LeekExpression) mExpression1).complete(operator)) {
			((LeekExpression) mExpression1).addOperator(operator, token);
		}
		else if (mOperator == -1) {
			if (operator == Operators.TERNAIRE) {
				LeekTernaire trn = new LeekTernaire(token);
				if (mExpression1.getNature() == EXPRESSION)
					((LeekExpression) mExpression1).setParent(trn);
				trn.addExpression(mExpression1);
				trn.addOperator(operator, token);
				if (mParent != null)
					mParent.replaceExpression(this, trn);
				else {
					trn.setParent(this);
					mExpression1 = trn;
				}
			} else {
				setOperator(operator, token);
			}
		}
		else {
			int cur_p = Operators.getPriority(mOperator);
			int p = Operators.getPriority(operator);
			boolean higher_priority = mOperator == Operators.ASSIGN && operator == Operators.ASSIGN ? false : (mOperator == Operators.OR || mOperator == Operators.AND ? cur_p > p : cur_p >= p);
			if (higher_priority) {
				// On doit englober l'expression actuelle
				LeekExpression new_e = new LeekExpression();
				new_e.setParent(this);
				new_e.setExpression1(mExpression1);
				new_e.setExpression2(mExpression2);
				new_e.setOperator(mOperator, mOperatorToken);
				if (operator == Operators.TERNAIRE) {
					LeekTernaire trn = new LeekTernaire(token);
					if (mExpression1.getNature() == EXPRESSION)
						((LeekExpression) mExpression1).setParent(trn);
					trn.addExpression(new_e);
					trn.addOperator(operator, token);
					if (mParent != null)
						mParent.replaceExpression(this, trn);
					else {
						trn.setParent(this);
						mExpression1 = trn;
						mExpression2 = null;
						mOperator = -1;
					}
				} else {
					mExpression1 = new_e;
					mExpression2 = null;
					setOperator(operator, token);
				}
			}
			else if (mExpression2.getNature() != EXPRESSION) {
				// On doit englober l'expression de droite
				if (operator == Operators.TERNAIRE) {
					LeekTernaire trn = new LeekTernaire(token);
					trn.setParent(this);
					trn.addExpression(mExpression2);
					trn.addOperator(operator, token);
					mExpression2 = trn;
				}
				else {
					LeekExpression new_e = new LeekExpression();
					new_e.setParent(this);
					new_e.setExpression1(mExpression2);
					new_e.setOperator(operator, token);
					mExpression2 = new_e;
				}
			} else {
				((LeekExpression) mExpression2).addOperator(operator, token);
			}
		}
	}

	@Override
	public String toString() {
		String retour = "";
		if (Operators.isUnaryPrefix(mOperator)) {
			retour += Operators.getString(mOperator);
			if (mOperator == Operators.NEW) retour += " ";
			retour += mExpression2 == null ? "null" : mExpression2.toString();
		}
		else if (Operators.isUnarySuffix(mOperator)) {
			retour += mExpression2 == null ? "null" : mExpression2.toString();
			retour += Operators.getString(mOperator);
		}
		else {
			retour += mExpression1 == null ? "null" : mExpression1.toString();
			retour += " " + Operators.getString(mOperator) + " ";
			retour += mExpression2 == null ? "null" : mExpression2.toString();
		}
		return retour + "";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// if (mExpression1 instanceof LeekExpression)
		// 	mExpression1 = ((LeekExpression) mExpression1).getAbstractExpression();
		// if (mExpression2 instanceof LeekExpression)
		// 	mExpression2 = ((LeekExpression) mExpression2).getAbstractExpression();
		if (mExpression1 == null || mExpression2 == null || mOperator == -1)
			throw new LeekExpressionException(this, Error.UNCOMPLETE_EXPRESSION);
		return mExpression1.validExpression(compiler, mainblock) && mExpression2.validExpression(compiler, mainblock);
	}

	@Override
	public Expression trim() {
		if (mExpression2 == null)
			return mExpression1.trim();
		// if (mOperator == Operators.REFERENCE) {
		// 	return mExpression1.trim();
		// }
		return this;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

		// Retourner le code java de l'expression... plein de cas :)
		switch (mOperator) {

		// Les classiques
		case Operators.ADD: // Addition (on commence facile)
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" + ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else if (mExpression1.getType() == Type.STRING || mExpression2.getType() == Type.STRING) {
				writer.addCode("(String) add(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");

			// 	if (mExpression1.getType() == Type.STRING)
			// 		mExpression1.writeJavaCode(mainblock, writer);
			// 	else
			// 		writer.getString(mainblock, mExpression1);
			// 	writer.addCode(" + ");
			// 	if (mExpression2.getType() == Type.STRING || mExpression2.getType() == Type.REAL || mExpression2.getType() == Type.INT || mExpression2.getType() == Type.BOOL)
			// 		mExpression2.writeJavaCode(mainblock, writer);
			// 	else
			// 		writer.getString(mainblock, mExpression2);
			} else {
				writer.addCode("add(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.MINUS: // Soustraction
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" - ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("sub(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.MULTIPLIE: // Multiplication
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" * ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("mul(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.MODULUS:// Modulo
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" % ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("mod(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.DIVIDE:// Division
			// Division by zero is not handled
			// if (mExpression1.getType().isNumber() && mExpression2.getType().isNumber()) {
			// 	mExpression1.writeJavaCode(mainblock, writer);
			// 	writer.addCode(" / ");
			// 	mExpression2.writeJavaCode(mainblock, writer);
			// } else {
				writer.addCode("div(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				writer.compileLoad(mainblock, mExpression2);
				// mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			// }
			return;
		case Operators.INTEGER_DIVISION: // Division entière
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" / ");
			writer.getInt(mainblock, mExpression2);
			return;
		case Operators.POWER: // Puissance
			writer.addCode("pow(");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
			// Les binaires
		case Operators.BITAND:
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" & ");
			writer.getInt(mainblock, mExpression2);
			return;
		case Operators.BITOR:
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" | ");
			writer.getInt(mainblock, mExpression2);
			return;
		case Operators.BITXOR:
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" ^ ");
			writer.getInt(mainblock, mExpression2);
			return;
		case Operators.SHIFT_LEFT:
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" << ");
			writer.getInt(mainblock, mExpression2);
			return;
		case Operators.SHIFT_RIGHT:
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" >> ");
			writer.getInt(mainblock, mExpression2);
			return;
		case Operators.SHIFT_UNSIGNED_RIGHT:
			writer.getInt(mainblock, mExpression1);
			writer.addCode(" >>> ");
			writer.getInt(mainblock, mExpression2);
			return;

		// Les logiques
		case Operators.EQUALS_EQUALS:
			if (mExpression2 instanceof LeekNull) {
				if (mExpression1.getType() == Type.BOOL || mExpression1.getType().isNumber()) {
					writer.addCode("false");
				} else {
					mExpression1.writeJavaCode(mainblock, writer);
					writer.addCode(" == null");
				}
			} else {
				writer.addCode("equals_equals(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.NOT_EQUALS_EQUALS:
			writer.addCode("notequals_equals(");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.EQUALS:
			if (mainblock.getWordCompiler().getVersion() >= 4) {
				writer.addCode("equals_equals(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("eq(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.MORE:
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" > ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("more(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.LESS:
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" < ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("less(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.MOREEQUALS:
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" >= ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("moreequals(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.LESSEQUALS:
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(" <= ");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("lessequals(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.NOTEQUALS:
			if (mainblock.getWordCompiler().getVersion() >= 4) {
				writer.addCode("notequals_equals(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("neq(");
				mExpression1.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.AND:
			writer.addCode("(");
			if (mExpression1.operations > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mExpression1);
			if (mExpression1.operations > 0) {
				writer.addCode(", " + mExpression1.operations + ")");
			}
			writer.addCode(" && ");
			if (mExpression2.operations > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mExpression2);
			if (mExpression2.operations > 0) {
				writer.addCode(", " + mExpression2.operations + ")");
			}
			writer.addCode(")");
			return;
		case Operators.OR:
			writer.addCode("(");
			if (mExpression1.operations > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mExpression1);
			if (mExpression1.operations > 0) {
				writer.addCode(", " + mExpression1.operations + ")");
			}
			writer.addCode(" || ");
			if (mExpression2.operations > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mExpression2);
			if (mExpression2.operations > 0) {
				writer.addCode(", " + mExpression2.operations + ")");
			}
			writer.addCode(")");
			return;

			// Les unaires préfixés (!)
		case Operators.NOT:
			writer.addCode("!");
			writer.getBoolean(mainblock, mExpression2);
			return;
		case Operators.BITNOT:
			writer.addCode("bnot(");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.UNARY_MINUS:
			if (mExpression2.getType().isPrimitiveNumber()) {
				writer.addCode("-");
				mExpression2.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("minus(");
				mExpression2.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
			return;
		case Operators.NEW:
			if (mExpression2 instanceof LeekVariable) {
				if (mainblock.getWordCompiler().getVersion() >= 3 && ((LeekVariable) mExpression2).toString().equals("Array")) {
					if (mainblock.getWordCompiler().getVersion() >= 4) {
						writer.addCode("new LegacyArrayLeekValue()");
					} else {
						writer.addCode("new ArrayLeekValue(" + writer.getAIThis() + ")");
					}
				} else if (mainblock.getWordCompiler().getVersion() >= 4 && ((LeekVariable) mExpression2).toString().equals("Map")) {
					writer.addCode("new MapLeekValue(" + writer.getAIThis() + ")");
				} else {
					writer.addCode("execute(");
					mExpression2.writeJavaCode(mainblock, writer);
					writer.addCode(")");
				}
			} else {
				mExpression2.writeJavaCode(mainblock, writer);
			}
			return;
			// Les unaires suffixés (++, --), Il a été vérifié au préalable
			// qu'on avait bien une L-Value
		case Operators.INCREMENT:
			mExpression2.compileIncrement(mainblock, writer);
			return;
		case Operators.DECREMENT:
			mExpression2.compileDecrement(mainblock, writer);
			return;
		case Operators.PRE_INCREMENT:
			mExpression2.compilePreIncrement(mainblock, writer);
			return;
		case Operators.PRE_DECREMENT:
			mExpression2.compilePreDecrement(mainblock, writer);
			return;

			// Les assignations
		case Operators.ASSIGN:
			// Assign without clone for LS 2 or reference
			if (mainblock.getVersion() >= 2) {
				mExpression1.compileSet(mainblock, writer, mExpression2);
			} else if (mExpression2 instanceof LeekExpression && ((LeekExpression) mExpression2).getOperator() == Operators.REFERENCE) {
				mExpression1.compileSet(mainblock, writer, ((LeekExpression) mExpression2).mExpression2);
			} else {
				mExpression1.compileSetCopy(mainblock, writer, mExpression2);
			}
			return;
		case Operators.ADDASSIGN:
			mExpression1.compileAddEq(mainblock, writer, mExpression2);
			return;
		case Operators.MINUSASSIGN:
			mExpression1.compileSubEq(mainblock, writer, mExpression2);
			return;
		case Operators.MODULUSASSIGN:
			mExpression1.compileModEq(mainblock, writer, mExpression2);
			return;
		case Operators.DIVIDEASSIGN:
			mExpression1.compileDivEq(mainblock, writer, mExpression2);
			return;
		case Operators.INTEGER_DIVISION_EQ:
			mExpression1.compileIntDivEq(mainblock, writer, mExpression2);
			return;
		case Operators.MULTIPLIEASSIGN:
			mExpression1.compileMulEq(mainblock, writer, mExpression2);
			return;
		case Operators.POWERASSIGN:
			mExpression1.compilePowEq(mainblock, writer, mExpression2);
			return;
		case Operators.BITXOR_ASSIGN:
			mExpression1.compileBitXorEq(mainblock, writer, mExpression2);
			return;
		case Operators.BITAND_ASSIGN:
			mExpression1.compileBitAndEq(mainblock, writer, mExpression2);
			return;
		case Operators.BITOR_ASSIGN:
			mExpression1.compileBitOrEq(mainblock, writer, mExpression2);
			return;
		case Operators.SHIFT_LEFT_ASSIGN:
			mExpression1.compileShiftLeftEq(mainblock, writer, mExpression2);
			return;
		case Operators.SHIFT_RIGHT_ASSIGN:
			mExpression1.compileShiftRightEq(mainblock, writer, mExpression2);
			return;
		case Operators.SHIFT_UNSIGNED_RIGHT_ASSIGN:
			mExpression1.compileShiftUnsignedRightEq(mainblock, writer, mExpression2);
			return;
		case Operators.REFERENCE:
			// writer.addCode("new ReferenceLeekValue(" + writer.getAIThis() + ", ");
			mExpression2.writeJavaCode(mainblock, writer);
			// writer.addCode(")");
			return;
		case Operators.INSTANCEOF:
			writer.addCode("instanceOf(");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		}
		return;
	}

	@Override
	public boolean isLeftValue() {
		// return mOperator == Operators.DOT;
		return false;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {

		if (mExpression1 != null) mExpression1.preAnalyze(compiler);
		if (mExpression2 != null) mExpression2.preAnalyze(compiler);

		if (mOperator == Operators.ASSIGN) {
			if (mExpression1 instanceof LeekVariable) {
				var v = (LeekVariable) mExpression1;

				if (v.getVariableType() == VariableType.SYSTEM_FUNCTION || v.getVariableType() == VariableType.FUNCTION) {

					if (compiler.getVersion() <= 3) {
						compiler.getMainBlock().addRedefinedFunction(((LeekVariable) mExpression1).getName());
					} else {
						compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANNOT_REDEFINE_FUNCTION));
					}
				}
			}
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {

		// Opérateur @ déprécié en LS 2+
		if (mOperator == Operators.REFERENCE && compiler.getVersion() >= 2) {
			compiler.addError(new AnalyzeError(mOperatorToken, AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
		}
		// Opérateurs === et !== déprécié en LS 4+
		if ((mOperator == Operators.EQUALS_EQUALS || mOperator == Operators.NOT_EQUALS_EQUALS) && compiler.getVersion() >= 4) {
			compiler.addError(new AnalyzeError(mOperatorToken, AnalyzeErrorLevel.WARNING, Error.TRIPLE_EQUALS_DEPRECATED));
		}

		if (mExpression1 != null) mExpression1.analyze(compiler);
		if (mExpression2 != null) mExpression2.analyze(compiler);

		// Si on a affaire à une assignation, incrémentation ou autre du genre
		// on doit vérifier qu'on a bien une variable (l-value)
		if (mOperator == Operators.ADDASSIGN || mOperator == Operators.MINUSASSIGN || mOperator == Operators.DIVIDEASSIGN || mOperator == Operators.ASSIGN || mOperator == Operators.MODULUSASSIGN || mOperator == Operators.MULTIPLIEASSIGN || mOperator == Operators.POWERASSIGN || mOperator == Operators.BITOR_ASSIGN || mOperator == Operators.BITAND_ASSIGN || mOperator == Operators.BITXOR_ASSIGN || mOperator == Operators.SHIFT_LEFT_ASSIGN || mOperator == Operators.SHIFT_RIGHT_ASSIGN || mOperator == Operators.SHIFT_UNSIGNED_RIGHT_ASSIGN) {
			if (mExpression1.isFinal()) {
				if (mExpression1 instanceof LeekObjectAccess) {
					if (!compiler.isInConstructor()) {
						compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD));
					}
				} else if (mExpression1 instanceof LeekVariable) {
					if (((LeekVariable) mExpression1).getVariableType() == VariableType.FIELD) {
						if (!compiler.isInConstructor()) {
							compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD));
						}
					}
				} else {
					compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANNOT_ASSIGN_FINAL_VALUE));
				}
			}
			if (!mExpression1.isLeftValue())
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANT_ASSIGN_VALUE));

			if (mExpression1 instanceof LeekArrayAccess)
				((LeekArrayAccess) mExpression1).setLeftValue(true);
		}

		if (mOperator == Operators.INCREMENT || mOperator == Operators.DECREMENT || mOperator == Operators.PRE_INCREMENT || mOperator == Operators.PRE_DECREMENT) {
			if (mExpression2.isFinal()) {
				if (mExpression2 instanceof LeekObjectAccess) {
					compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANNOT_ASSIGN_FINAL_FIELD));
				} else {
					compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANNOT_ASSIGN_FINAL_VALUE));
				}
			}
			if (!mExpression2.isLeftValue())
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.ERROR, Error.CANT_ASSIGN_VALUE));
			if (mExpression2 instanceof LeekArrayAccess)
				((LeekArrayAccess) mExpression2).setLeftValue(true);
		}

		// x = x : même variable
		if (mOperator == Operators.ASSIGN) {
			var variable1 = mExpression1.getVariable();
			var variable2 = mExpression2.getVariable();
			if (variable1 != null && variable1 == variable2) {
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.WARNING, Error.ASSIGN_SAME_VARIABLE, new String[] { variable1.getName() } ));
			}
		}

		// x == y : toujours faux si types incompatibles
		if ((compiler.getVersion() == 4 && mOperator == Operators.EQUALS) || mOperator == Operators.EQUALS_EQUALS) {
			if (mExpression1.getType().accepts(mExpression2.getType()) == CastType.INCOMPATIBLE) {
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.WARNING, Error.COMPARISON_ALWAYS_FALSE, new String[] { mExpression1.getType().name, mExpression2.getType().name }));
			}
		}
		// x != y : toujours vrai si types incompatibles
		if ((compiler.getVersion() == 4 && mOperator == Operators.NOTEQUALS) || mOperator == Operators.NOT_EQUALS_EQUALS) {
			if (mExpression1.getType().accepts(mExpression2.getType()) == CastType.INCOMPATIBLE) {
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.WARNING, Error.COMPARISON_ALWAYS_TRUE, new String[] { mExpression1.getType().name, mExpression2.getType().name }));
			}
		}

		// Opérateurs inconnus (mathématiques avec un string par exemple)
		if (mOperator == Operators.MINUS || mOperator == Operators.MULTIPLIE || mOperator == Operators.DIVIDE || mOperator == Operators.POWER || mOperator == Operators.MODULUS || mOperator == Operators.BITAND || mOperator == Operators.BITOR || mOperator == Operators.BITXOR || mOperator == Operators.SHIFT_LEFT || mOperator == Operators.SHIFT_RIGHT || mOperator == Operators.SHIFT_UNSIGNED_RIGHT || mOperator == Operators.MORE || mOperator == Operators.LESS || mOperator == Operators.MOREEQUALS || mOperator == Operators.LESSEQUALS) {
			if (Type.NUMBER.accepts(mExpression1.getType()) == CastType.INCOMPATIBLE || Type.NUMBER.accepts(mExpression2.getType()) == CastType.INCOMPATIBLE) {
				compiler.addError(new AnalyzeError(getLocation(), AnalyzeErrorLevel.WARNING, Error.UNKNOWN_OPERATOR, new String[] { mOperatorToken.getWord(), mExpression1.getType().name, mExpression2.getType().name }));
			}
		}

		// Types

		if (mOperator == Operators.NOT || mOperator == Operators.EQUALS_EQUALS || mOperator == Operators.LESS || mOperator == Operators.MORE || mOperator == Operators.MOREEQUALS || mOperator == Operators.LESSEQUALS || mOperator == Operators.EQUALS || mOperator == Operators.AND || mOperator == Operators.OR || mOperator == Operators.NOTEQUALS || mOperator == Operators.NOT_EQUALS_EQUALS || mOperator == Operators.INSTANCEOF) {
			type = Type.BOOL;
		}
		if (mOperator == Operators.BITAND || mOperator == Operators.BITNOT || mOperator == Operators.BITOR  || mOperator == Operators.BITXOR || mOperator == Operators.SHIFT_LEFT || mOperator == Operators.SHIFT_RIGHT || mOperator == Operators.SHIFT_UNSIGNED_RIGHT || mOperator == Operators.INTEGER_DIVISION) {
			type = Type.INT;
		}
		if (mOperator == Operators.ADD && ((mExpression1.getType() == Type.STRING || mExpression2.getType() == Type.STRING))) {
			type = Type.STRING;
		}
		if (mOperator == Operators.ADD || mOperator == Operators.MINUS) {
			if (mExpression1.getType() == Type.INT) {
				if (mExpression2.getType() == Type.INT) type = Type.INT;
				if (mExpression2.getType() == Type.REAL) type = Type.REAL;
			}
			else if (mExpression1.getType() == Type.REAL) {
				if (mExpression2.getType() == Type.INT || mExpression2.getType() == Type.REAL) type = Type.REAL;
			}
		}
		if (mOperator == Operators.MULTIPLIE) {
			if (mExpression1.getType().isPrimitiveNumber() && mExpression2.getType().isPrimitiveNumber()) {
				if (mExpression1.getType() == Type.INT && mExpression2.getType() == Type.INT) {
					type = Type.INT;
				} else {
					type = Type.REAL;
				}
			}
		}
		// if (mOperator == Operators.DIVIDE) {
		// 	type = Type.REAL;
		// }
		if (mOperator == Operators.UNARY_MINUS) {
			type = mExpression2.getType();
		}
		// if (mOperator == Operators.POWER) {
		// 	type = Type.REAL;
		// }

		// Opérations
		operations = (mExpression1 != null ? mExpression1.getOperations() : 0) + (mExpression2 != null ? mExpression2.getOperations() : 0);
		if (mOperator == Operators.POWER) {
			operations += LeekValue.POW_COST;
		} else if (mOperator == Operators.POWERASSIGN) {
			operations += LeekValue.POW_COST;
		} else if (mOperator == Operators.MULTIPLIE) {
			operations += LeekValue.MUL_COST;
		} else if (mOperator == Operators.MULTIPLIEASSIGN) {
			operations += LeekValue.MUL_COST; //+ 1;
		} else if (mOperator == Operators.DIVIDE || mOperator == Operators.DIVIDEASSIGN || mOperator == Operators.INTEGER_DIVISION || mOperator == Operators.INTEGER_DIVISION_EQ) {
			operations += LeekValue.DIV_COST;
		} else if (mOperator == Operators.MODULUS || mOperator == Operators.MODULUSASSIGN) {
			operations += LeekValue.MOD_COST;
		} else if (mOperator == Operators.REFERENCE || mOperator == Operators.NEW) {
			// 0
		} else if (mOperator == Operators.AND || mOperator == Operators.OR) {
			operations = 1;
		} else {
			operations += 1;
		}
	}

	public boolean needsWrapper() {
		return mOperator == Operators.OR || mOperator == Operators.AND || mOperator == Operators.ADD || mOperator == Operators.MINUS || mOperator == Operators.MULTIPLIE || mOperator == Operators.DIVIDE || mOperator == Operators.MODULUS || mOperator == Operators.POWER || mOperator == Operators.SHIFT_LEFT || mOperator == Operators.SHIFT_RIGHT || mOperator == Operators.BITAND || mOperator == Operators.BITOR || mOperator == Operators.BITXOR || mOperator == Operators.LESS || mOperator == Operators.MORE || mOperator == Operators.LESSEQUALS || mOperator == Operators.MOREEQUALS || mOperator == Operators.EQUALS || mOperator == Operators.EQUALS_EQUALS || mOperator == Operators.NOTEQUALS || mOperator == Operators.NOT_EQUALS_EQUALS;
	}

	@Override
	public Location getLocation() {
		var startLocation = mExpression1 != null ? mExpression1.getLocation() : mOperatorToken.getLocation();
		var endLocation = mExpression2 != null ? mExpression2.getLocation() : mOperatorToken.getLocation();
		return new Location(startLocation, endLocation);
	}
}
