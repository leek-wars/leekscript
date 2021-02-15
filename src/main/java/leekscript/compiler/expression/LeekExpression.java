package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LeekExpression extends AbstractExpression {

	private int mOperator = -1;
	private IAWord mOperatorToken;
	protected AbstractExpression mExpression1 = null;
	protected AbstractExpression mExpression2 = null;

	protected LeekExpression mParent = null;

	public void setParent(LeekExpression parent) {
		mParent = parent;
	}

	public boolean hasTernaire() {
		if (mExpression1 != null && mExpression1.getType() == EXPRESSION && ((LeekExpression) mExpression1).hasTernaire())
			return true;
		if (mExpression2 != null && mExpression2.getType() == EXPRESSION && ((LeekExpression) mExpression2).hasTernaire())
			return true;
		return false;
	}

	public LeekExpression getParent() {
		return mParent;
	}

	public AbstractExpression getExpression1() {
		return mExpression1;
	}

	public void setExpression1(AbstractExpression e) {
		mExpression1 = e;
	}

	public void setExpression2(AbstractExpression e) {
		mExpression2 = e;
	}

	public AbstractExpression getExpression2() {
		return mExpression2;
	}

	public int getOperator() {
		return mOperator;
	}

	public int getLastOperator() {
		if (mExpression2 != null && mExpression2 instanceof LeekExpression) {
			return ((LeekExpression) mExpression2).getLastOperator();
		}
		return mOperator;
	}

	public AbstractExpression getLastExpression() {
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

	public void setOperator(int operator, IAWord operatorToken) {
		mOperator = operator;
		mOperatorToken = operatorToken;
	}

	public boolean needOperator() {
		if (mExpression1 != null) {
			if (mExpression1.getType() == EXPRESSION && !((LeekExpression) mExpression1).complete())
				return ((LeekExpression) mExpression1).needOperator();
			if (mOperator == -1)
				return true;
		}
		if (mExpression2 != null) {
			if (mExpression2.getType() == EXPRESSION)
				return ((LeekExpression) mExpression2).needOperator();
			return true;
		}
		return false;
	}

	public LeekExpression lastExpression() {
		if (mExpression2 != null && mExpression2.getType() == EXPRESSION)
			return ((LeekExpression) mExpression2).lastExpression();
		else
			return this;
	}

	@Override
	public int getType() {
		return AbstractExpression.EXPRESSION;
	}

	public boolean complete(int operator) {
		return complete();
	}

	public boolean complete() {
		if (mExpression1 == null || mExpression2 == null)
			return false;
		if (mExpression1.getType() == EXPRESSION && !((LeekExpression) mExpression1).complete())
			return false;
		if (mExpression2.getType() == EXPRESSION && !((LeekExpression) mExpression2).complete())
			return false;
		return true;
	}

	public void addExpression(AbstractExpression expression) {
		if (mExpression1 == null)
			mExpression1 = expression;
		else if (mExpression1.getType() == EXPRESSION && !((LeekExpression) mExpression1).complete()) {
			((LeekExpression) mExpression1).addExpression(expression);
		}
		else {
			if (mExpression2 == null)
				mExpression2 = expression;
			else
				((LeekExpression) mExpression2).addExpression(expression);
		}
	}

	public void addUnaryPrefix(int operator, IAWord operatorToken) {
		// On doit trouver à quel endroit de l'arborescence on doit placer le
		// préfix
		// En général c'est un => !
		LeekExpression exp = new LeekExpression();
		exp.setOperator(operator, operatorToken);
		exp.setParent(this);
		exp.setExpression1(new LeekNull());
		addExpression(exp);
	}

	public void addBracket(AbstractExpression casevalue) {
		// On doit ajouter ce crochet au dernier élément ajouté
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getType() == EXPRESSION)
				((LeekExpression) mExpression1).addBracket(casevalue);
			else {
				// On doit ajouter à l'élément mExpression1
				LeekTabularValue exp = new LeekTabularValue();
				exp.setCase(casevalue);
				exp.setTabular(mExpression1);
				mExpression1 = exp;
			}
		}
		else if (mExpression2 != null) {
			if (mExpression2.getType() == EXPRESSION)
				((LeekExpression) mExpression2).addBracket(casevalue);
			else {
				// On doit ajouter à l'élément mExpression2
				LeekTabularValue exp = new LeekTabularValue();
				exp.setCase(casevalue);
				exp.setTabular(mExpression2);
				mExpression2 = exp;
			}
		}
	}

	public void addFunction(LeekExpressionFunction function) {
		// On doit ajouter ce crochet au dernier élément ajouté
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getType() == EXPRESSION)
				((LeekExpression) mExpression1).addFunction(function);
			else {
				// On doit ajouter à l'élément mExpression1
				function.setExpression(mExpression1);
				mExpression1 = function;
			}
		}
		else if (mExpression2 != null) {
			if (mExpression2.getType() == EXPRESSION)
				((LeekExpression) mExpression2).addFunction(function);
			else {
				// On doit ajouter à l'élément mExpression2
				function.setExpression(mExpression2);
				mExpression2 = function;
			}
		}
	}

	public void addUnarySuffix(int suffix, IAWord token) {
		// On doit ajouter ce suffix au dernier élément ajouté
		if (mExpression1 != null && mExpression2 == null) {
			if (mExpression1.getType() == EXPRESSION)
				((LeekExpression) mExpression1).addUnarySuffix(suffix, token);
			else {
				// On doit ajouter à l'élément mExpression1
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull());
				exp.setOperator(suffix, token);
				exp.setExpression2(mExpression1);
				mExpression1 = exp;
			}
		}
		else if (mExpression2 != null) {
			if (mExpression2.getType() == EXPRESSION)
				((LeekExpression) mExpression2).addUnarySuffix(suffix, token);
			else {
				// On doit ajouter à l'élément mExpression2
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull());
				exp.setOperator(suffix, token);
				exp.setExpression2(mExpression2);
				mExpression2 = exp;
			}
		}
	}

	public void addTernaire() {
		// On a ajotué un opérateur ?
		if (mOperator == -1) {
			if (mExpression1 != null) {
				LeekTernaire ternaire = new LeekTernaire();
				ternaire.addExpression(mExpression1);
				ternaire.addOperator(Operators.TERNAIRE, null);
				ternaire.setParent(this);
				mExpression1 = ternaire;
			}
		}
		else {
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
				LeekTernaire ternaire = new LeekTernaire();
				ternaire.addExpression(new_e);
				ternaire.addOperator(Operators.TERNAIRE, null);
				ternaire.setParent(this);
				mExpression1 = ternaire;
				mExpression2 = null;
				mOperator = -1;
			}
			else if (mExpression2.getType() != EXPRESSION) {
				// On doit englober l'expression de droite
				LeekTernaire ternaire = new LeekTernaire();
				ternaire.addExpression(mExpression2);
				ternaire.addOperator(Operators.TERNAIRE, null);
				ternaire.setParent(this);
				mExpression2 = ternaire;
			}
			else
				((LeekExpression) mExpression2).addOperator(operator, null);
		}
	}

	public void replaceExpression(AbstractExpression base, AbstractExpression replacement) {
		if (mExpression1 == base) {
			if (replacement.getType() == EXPRESSION)
				((LeekExpression) replacement).setParent(this);
			mExpression1 = replacement;
		}
		else if (mExpression2 == base) {
			if (replacement.getType() == EXPRESSION)
				((LeekExpression) replacement).setParent(this);
			mExpression2 = replacement;
		}
	}

	public void addOperator(int operator, IAWord token) {
		// On doit trouver à quel endroit de l'arborescence on doit placer
		// l'opérateur
		/*
		 * if(operator == Operators.TERNAIRE) addTernaire(); else if(operator ==
		 * Operators.DOUBLE_POINT){ if(mExpression2 != null &&
		 * mExpression2.getType() == EXPRESSION) ((LeekExpression)
		 * mExpression2).addOperator(operator); else if(mExpression1 != null &&
		 * mExpression1.getType() == EXPRESSION) ((LeekExpression)
		 * mExpression1).addOperator(operator); } else
		 */
		if (mExpression1 != null && mExpression1.getType() == EXPRESSION && !((LeekExpression) mExpression1).complete(operator)) {
			((LeekExpression) mExpression1).addOperator(operator, token);
		}
		else if (mOperator == -1) {
			if (operator == Operators.TERNAIRE) {
				LeekTernaire trn = new LeekTernaire();
				if (mExpression1.getType() == EXPRESSION)
					((LeekExpression) mExpression1).setParent(trn);
				trn.addExpression(mExpression1);
				trn.addOperator(operator, token);
				if (mParent != null)
					mParent.replaceExpression(this, trn);
				else {
					trn.setParent(this);
					mExpression1 = trn;
				}
			}
			else
				mOperator = operator;
		}
		else {
			int cur_p = Operators.getPriority(mOperator);
			int p = Operators.getPriority(operator);
			if (cur_p >= p) {
				// On doit englober l'expression actuelle
				LeekExpression new_e = new LeekExpression();
				new_e.setParent(this);
				new_e.setExpression1(mExpression1);
				new_e.setExpression2(mExpression2);
				new_e.setOperator(mOperator, token);
				if (operator == Operators.TERNAIRE) {
					LeekTernaire trn = new LeekTernaire();
					if (mExpression1.getType() == EXPRESSION)
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
				}
				else {
					mExpression1 = new_e;
					mExpression2 = null;
					mOperator = operator;
				}
			}
			else if (mExpression2.getType() != EXPRESSION) {
				// On doit englober l'expression de droite
				if (operator == Operators.TERNAIRE) {
					LeekTernaire trn = new LeekTernaire();
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
			}
			else
				((LeekExpression) mExpression2).addOperator(operator, token);
		}
	}

	@Override
	public String getString() {
		String retour = "(";
		if (Operators.isUnaryPrefix(mOperator)) {
			;
			retour += Operators.getString(mOperator);
			retour += mExpression2 == null ? "null" : mExpression2.getString();
		}
		else if (Operators.isUnarySuffix(mOperator)) {
			retour += mExpression2 == null ? "null" : mExpression2.getString();
			retour += Operators.getString(mOperator);
		}
		else {
			retour += mExpression1 == null ? "null" : mExpression1.getString();
			retour += " " + Operators.getString(mOperator) + " ";
			retour += mExpression2 == null ? "null" : mExpression2.getString();
		}
		return retour + ")";
	}

	public AbstractExpression getAbstractExpression() {
		// Retourner l'AbstractExpression (dans le cas où on n'aurait pas
		// d'expression complete)
		if (mOperator == -1)
			return mExpression1;
		return this;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if (mExpression1 instanceof LeekExpression)
			mExpression1 = ((LeekExpression) mExpression1).getAbstractExpression();
		if (mExpression2 instanceof LeekExpression)
			mExpression2 = ((LeekExpression) mExpression2).getAbstractExpression();
		if (mExpression1 == null || mExpression2 == null || mOperator == -1)
			throw new LeekExpressionException(this, LeekCompilerException.UNCOMPLETE_EXPRESSION);

		// Si on a affaire à une assignation, incrémentation ou autre du genre
		// on doit vérifier qu'on a bien une variable (l-value)
		if (mOperator == Operators.ADDASSIGN || mOperator == Operators.MINUSASSIGN || mOperator == Operators.DIVIDEASSIGN || mOperator == Operators.ASSIGN || mOperator == Operators.MODULUSASSIGN
				|| mOperator == Operators.MULTIPLIEASSIGN || mOperator == Operators.POWERASSIGN) {
			if (!mExpression1.isLeftValue())
				compiler.addError(new AnalyzeError(mOperatorToken, AnalyzeErrorLevel.ERROR, LeekCompilerException.CANT_ASSIGN_VALUE));
				// throw new LeekExpressionException(mExpression1, LeekCompilerException.CANT_ASSIGN_VALUE);
			if (mExpression1 instanceof LeekFunctionValue)
				mainblock.addRedefinedFunction(((LeekFunctionValue) mExpression1).getFunctionName());
			if (mExpression1 instanceof LeekTabularValue)
				((LeekTabularValue) mExpression1).setLeftValue(true);
		}
		if (mOperator == Operators.INCREMENT || mOperator == Operators.DECREMENT || mOperator == Operators.PRE_INCREMENT || mOperator == Operators.PRE_DECREMENT) {
			if (!(mExpression2 instanceof LeekFunctionValue) && !(mExpression2 instanceof LeekVariable) && !(mExpression2 instanceof LeekGlobal) && !(mExpression2 instanceof LeekTabularValue))
				compiler.addError(new AnalyzeError(mOperatorToken, AnalyzeErrorLevel.ERROR, LeekCompilerException.CANT_ASSIGN_VALUE));
				// throw new LeekExpressionException(mExpression2, LeekCompilerException.CANT_ASSIGN_VALUE);
			if (mExpression2 instanceof LeekFunctionValue)
				mainblock.addRedefinedFunction(((LeekFunctionValue) mExpression2).getFunctionName());
			if (mExpression2 instanceof LeekTabularValue)
				((LeekTabularValue) mExpression2).setLeftValue(true);
		}

		return mExpression1.validExpression(compiler, mainblock) && mExpression2.validExpression(compiler, mainblock);
	}

	@Override
	public AbstractExpression trim() {
		if (mExpression2 == null)
			return mExpression1.trim();
		return this;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mExpression1 instanceof LeekExpression)
			mExpression1 = ((LeekExpression) mExpression1).getAbstractExpression();
		if (mExpression2 instanceof LeekExpression)
			mExpression2 = ((LeekExpression) mExpression2).getAbstractExpression();
		// Retourner le code java de l'expression... plein de cas :)
		switch (mOperator) {

		// Les classiques
		case Operators.ADD:// Addition (on commence facile)
			writer.addCode("LeekOperations.add(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MINUS:// Soustraction
			writer.addCode("LeekOperations.minus(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MULTIPLIE:// Multiplication
			writer.addCode("LeekOperations.multiply(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MODULUS:// Modulo
			writer.addCode("LeekOperations.modulus(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.DIVIDE:// Division
			writer.addCode("LeekOperations.divide(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.POWER:// Puissance
			writer.addCode("LeekOperations.power(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
			// Les binaires
		case Operators.BITAND:
			writer.addCode("LeekOperations.band(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.BITOR:
			writer.addCode("LeekOperations.bor(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.BITXOR:
			writer.addCode("LeekOperations.bxor(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.SHIFT_LEFT:
			writer.addCode("LeekOperations.bleft(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.SHIFT_RIGHT:
			writer.addCode("LeekOperations.bright(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.ROTATE_RIGHT:
			writer.addCode("LeekOperations.brotate(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;

			// Les logiques
		case Operators.EQUALS_EQUALS:
			writer.addCode("LeekOperations.equals_equals(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.NOT_EQUALS_EQUALS:
			writer.addCode("LeekOperations.notequals_equals(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.EQUALS:
			writer.addCode("LeekOperations.equals(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MORE:
			writer.addCode("LeekOperations.more(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.LESS:
			writer.addCode("LeekOperations.less(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MOREEQUALS:
			writer.addCode("LeekOperations.moreequals(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.LESSEQUALS:
			writer.addCode("LeekOperations.lessequals(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.NOTEQUALS:
			writer.addCode("LeekOperations.notequals(mUAI, ");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(", ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.AND:
			writer.addCode("LeekValueManager.getLeekBooleanValue(");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".getBoolean() && ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".getBoolean())");
			return;
		case Operators.OR:
			writer.addCode("LeekValueManager.getLeekBooleanValue(");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".getBoolean() || ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".getBoolean())");
			return;

			// Les unaires préfixés (!)
		case Operators.NOT:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".not(mUAI)");
			return;
		case Operators.BITNOT:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".bnot(mUAI)");
			return;
		case Operators.UNARY_MINUS:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".opposite(mUAI)");
			return;
		case Operators.NEW:
			mExpression2.writeJavaCode(mainblock, writer);
			return;
			// Les unaires suffixés (++, --), Il a été vérifié au préalable
			// qu'on avait bien une L-Value
		case Operators.INCREMENT:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".increment(mUAI)");
			return;
		case Operators.DECREMENT:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".decrement(mUAI)");
			return;
		case Operators.PRE_INCREMENT:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".pre_increment(mUAI)");
			return;
		case Operators.PRE_DECREMENT:
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(".pre_decrement(mUAI)");
			return;

			// Les assignations
		case Operators.ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".set(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.ADDASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".add(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MINUSASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".minus(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MODULUSASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".modulus(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.DIVIDEASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".divide(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.MULTIPLIEASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".multiply(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.POWERASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".power(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.BITXOR_ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".bxor(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.BITAND_ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".band(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.BITOR_ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".bor(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.SHIFT_LEFT_ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".bleft(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.SHIFT_RIGHT_ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".bright(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.ROTATE_RIGHT_ASSIGN:
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(".brotate(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.REFERENCE:
			writer.addCode("new ReferenceLeekValue(mUAI, ");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode(")");
			return;
		case Operators.DOT:
			mExpression2.writeJavaCode(mainblock, writer);
			return;
		}
		return;
	}

	@Override
	public boolean isLeftValue() {
		return mOperator == Operators.DOT;
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (mExpression1 != null) mExpression1.analyze(compiler);
		if (mExpression2 != null) mExpression2.analyze(compiler);
	}
}
