package leekscript.compiler.expression;

import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LeekTernaire extends LeekExpression {

	private AbstractExpression mCondition;

	private int mOperator = 0;

	public LeekTernaire() {
		mCondition = null;
		mExpression1 = null;
		mExpression2 = null;
	}

	@Override
	public boolean needOperator() {
		if(mCondition != null && mOperator == 0){
			if(mCondition.getType() == EXPRESSION){
				return ((LeekExpression) mCondition).needOperator();
			}
			return true;
		}
		if(mExpression1 != null && mOperator == 1){
			if(mExpression1.getType() == EXPRESSION){
				return ((LeekExpression) mExpression1).needOperator();
			}
			return true;
		}
		if(mExpression2 != null && mOperator == 2){
			if(mExpression2.getType() == EXPRESSION){
				return ((LeekExpression) mExpression2).needOperator();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean hasTernaire() {
		return true;
	}

	@Override
	public int getType() {
		return EXPRESSION;
	}

	@Override
	public String getString() {
		String retour = "(";
		retour += mCondition == null ? "null" : mCondition.getString();
		retour += " ? ";
		retour += mExpression1 == null ? "null" : mExpression1.getString();
		retour += " : ";
		retour += mExpression2 == null ? "null" : mExpression2.getString();
		return retour + ")";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		// if(mCondition instanceof LeekExpression) mCondition = ((LeekExpression) mCondition).getAbstractExpression();
		// if(mExpression1 instanceof LeekExpression) mExpression1 = ((LeekExpression) mExpression1).getAbstractExpression();
		// if(mExpression2 instanceof LeekExpression) mExpression2 = ((LeekExpression) mExpression2).getAbstractExpression();
		if (!complete()) writer.addCode("/* " + getString() + " */");
		else{
			writer.addCode("(");
			mCondition.writeJavaCode(mainblock, writer);
			writer.addCode(".getBooleanTernary(mUAI) ? (");
			mExpression1.writeJavaCode(mainblock, writer);
			writer.addCode(") : (");
			mExpression2.writeJavaCode(mainblock, writer);
			writer.addCode("))");
		}
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if(!complete()) throw new LeekExpressionException(this, LeekCompilerException.UNCOMPLETE_EXPRESSION);
		if(!(mCondition.validExpression(compiler, mainblock) && mExpression1.validExpression(compiler, mainblock) && mExpression2.validExpression(compiler, mainblock))) throw new LeekExpressionException(this, LeekCompilerException.UNCOMPLETE_EXPRESSION);
		return true;
	}

	@Override
	public void addExpression(AbstractExpression expression) {
		if(mCondition == null) mCondition = expression;
		else if(mCondition.getType() == EXPRESSION && !((LeekExpression) mCondition).complete()){
			((LeekExpression) mCondition).addExpression(expression);
		}
		else if(mOperator == 1){
			if(mExpression1 == null) mExpression1 = expression;
			else ((LeekExpression) mExpression1).addExpression(expression);
		}
		else if(mOperator == 2){
			if(mExpression2 == null) mExpression2 = expression;
			else ((LeekExpression) mExpression2).addExpression(expression);
		}

	}

	@Override
	public void addUnarySuffix(int suffix, IAWord token) {
		//On doit ajouter ce suffix au dernier élément ajouté
		if(mCondition != null && mExpression1 == null && mExpression2 == null){
			if(mCondition.getType() == EXPRESSION) ((LeekExpression) mCondition).addUnarySuffix(suffix, token);
			else{
				//On doit ajouter à l'élément mExpression1
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull());
				exp.setOperator(suffix, token);
				exp.setExpression2(mCondition);
				mCondition = exp;
			}
		}
		else if(mExpression1 != null && mExpression2 == null){
			if(mExpression1.getType() == EXPRESSION) ((LeekExpression) mExpression1).addUnarySuffix(suffix, token);
			else{
				//On doit ajouter à l'élément mExpression1
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull());
				exp.setOperator(suffix, token);
				exp.setExpression2(mExpression1);
				mExpression1 = exp;
			}
		}
		else if(mExpression2 != null){
			if(mExpression2.getType() == EXPRESSION) ((LeekExpression) mExpression2).addUnarySuffix(suffix, token);
			else{
				//On doit ajouter à l'élément mExpression2
				LeekExpression exp = new LeekExpression();
				exp.setParent(this);
				exp.setExpression1(new LeekNull());
				exp.setOperator(suffix, token);
				exp.setExpression2(mExpression2);
				mExpression2 = exp;
			}
		}
	}

	@Override
	public boolean complete(int operator) {
		if(!complete()) return false;
		if(operator >= Operators.getPriority(Operators.DOUBLE_POINT)) return false;
		return true;
	}

	@Override
	public boolean complete() {
		if(!super.complete()) return false;
		if(mCondition == null) return false;
		if(mCondition.getType() == EXPRESSION && !((LeekExpression) mCondition).complete()) return false;
		return true;
	}

	@Override
	public void addOperator(int operator, IAWord token) {
		//On doit trouver à quel endroit de l'arborescence on doit placer l'opérateur
		if(mOperator == 0 && operator == Operators.TERNAIRE){
			mOperator = 1;
		}
		else if(mExpression1.getType() == EXPRESSION && !((LeekExpression) mExpression1).complete()) ((LeekExpression) mExpression1).addOperator(operator, token);
		else if(mOperator == 1 && operator == Operators.DOUBLE_POINT){
			mOperator = 2;
		}
		else{
			if(mOperator == 0){
				if(mCondition.getType() == EXPRESSION) ((LeekExpression) mCondition).addOperator(operator, token);
				else{
					LeekExpression new_e = new LeekExpression();
					new_e.setParent(this);
					new_e.setExpression1(mCondition);
					new_e.setOperator(operator, token);
					mCondition = new_e;
				}
			}
			else if(mOperator == 1){
				if(mExpression1.getType() == EXPRESSION) ((LeekExpression) mExpression1).addOperator(operator, token);
				else{
					if(operator == Operators.TERNAIRE){
						LeekTernaire new_e = new LeekTernaire();
						new_e.setParent(this);
						new_e.addExpression(mExpression1);
						new_e.addOperator(operator, token);
						mExpression1 = new_e;
					}
					else{
						LeekExpression new_e = new LeekExpression();
						new_e.setParent(this);
						new_e.setExpression1(mExpression1);
						new_e.setOperator(operator, token);
						mExpression1 = new_e;
					}
				}
			}
			else{
				if(mExpression2.getType() == EXPRESSION) ((LeekExpression) mExpression2).addOperator(operator, token);
				else{
					if(operator == Operators.TERNAIRE){
						LeekTernaire new_e = new LeekTernaire();
						new_e.setParent(this);
						new_e.addExpression(mExpression2);
						new_e.addOperator(operator, token);
						mExpression2 = new_e;
					}
					else{
						LeekExpression new_e = new LeekExpression();
						new_e.setParent(this);
						new_e.setExpression1(mExpression2);
						new_e.setOperator(operator, token);
						mExpression2 = new_e;
					}
				}
			}
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (mCondition != null) mCondition.analyze(compiler);
		if (mExpression1 != null) mExpression1.analyze(compiler);
		if (mExpression2 != null) mExpression2.analyze(compiler);
	}
}
