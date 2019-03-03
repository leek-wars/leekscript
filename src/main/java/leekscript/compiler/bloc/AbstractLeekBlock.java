package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.exceptions.LeekInstructionException;
import leekscript.compiler.instruction.LeekInstruction;

public abstract class AbstractLeekBlock implements LeekInstruction {
	protected ArrayList<LeekInstruction> mInstructions = new ArrayList<LeekInstruction>();
	protected AbstractLeekBlock mParent = null;
	protected ArrayList<String> mVariables = new ArrayList<String>();

	protected String mDeclaringVariable = null;
	protected boolean mDeclaringVariableUsed = false;
	protected boolean mAccolade = true;
	protected MainLeekBlock mMain = null;
	protected int mEndInstruction = 0;
	protected int mLine = 0;
	protected int mAI = 0;

	public AbstractLeekBlock getParent() {
		return mParent;
	}

	public ConditionalBloc getLastOpenedConditionalBlock() {
		LeekInstruction block = lastInstruction();
		if (block instanceof ConditionalBloc) {
			// C'est une condition
			ConditionalBloc condition = (ConditionalBloc) block;
			if (condition.hasAccolade())
				return condition;
			if (condition.getCondition() == null)
				return null;
			ConditionalBloc new_block = condition.getLastOpenedConditionalBlock();
			if (new_block != null)
				return new_block;
			return condition;
		}
		return null;
	}

	public int getCount() {
		if (mParent != null)
			return mParent.getCount();
		return 1;
	}

	public AbstractLeekBlock(AbstractLeekBlock parent, MainLeekBlock main, int line, int ai) {
		mParent = parent;
		mMain = main;
		mLine = line;
		mAI = ai;
	}

	public void addInstruction(LeekInstruction instruction) throws LeekInstructionException {
		if (mEndInstruction != 0) {
			throw new LeekInstructionException(LeekCompilerException.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		}
		mEndInstruction = instruction.getEndBlock();
		mInstructions.add(instruction);
	}

	public void noAccolade() {
		mAccolade = false;
	}

	public boolean hasAccolade() {
		return mAccolade;
	}

	public void setDeclaringVariable(String variable) {
		mDeclaringVariable = variable;
		mDeclaringVariableUsed = false;
	}

	public String getDeclaringVariable() {
		mDeclaringVariableUsed = true;
		return mDeclaringVariable;
	}

	public boolean isDeclaringBariableUsed() {
		return mDeclaringVariableUsed;
	}

	public void checkEndBlock() {
		if (mParent != null)
			mParent.mEndInstruction = getEndBlock();
	}

	public AbstractLeekBlock endInstruction() {
		if (!mAccolade && mParent != null && mInstructions.size() == 1) {
			// if(this instanceof ConditionalBloc){
			// ConditionalBloc bloc = (ConditionalBloc) this;
			// do{
			// bloc = bloc.getParentCondition();
			// } while(bloc != null);
			// }
			checkEndBlock();
			return mParent.endInstruction();
		}
		return this;
	}

	public void addVariable(String variable) {
		mVariables.add(variable);
	}

	public LeekInstruction lastInstruction() {
		return mInstructions.size() == 0 ? null : mInstructions.get(mInstructions.size() - 1);
	}

	public boolean hasVariable(String variable) {
		if (mVariables.contains(variable))
			return true;
		if (mParent != null)
			return mParent.hasVariable(variable);
		return false;
	}

	public boolean hasGlobal(String globale) {
		return mMain.hasGlobal(globale);
	}

	public boolean hasDeclaredGlobal(String globale) {
		return mMain.hasDeclaredGlobal(globale);
	}

	public void addGlobal(String globale) {
		mMain.addGlobal(globale);
	}

	@Override
	public String getCode() {
		String retour = "";
		for (LeekInstruction instruction : mInstructions) {
			retour += instruction.getCode() + "\n";
		}
		return retour;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		for (LeekInstruction instruction : mInstructions) {
			instruction.writeJavaCode(mainblock, writer);
		}
	}

	public int countInstructions() {
		return mInstructions.size() == 0 ? 1 : mInstructions.size();
	}

	public boolean isBreakable() {
		if (mParent == null)
			return false;
		return mParent.isBreakable();
	}

	@Override
	public int getEndBlock() {
		return mEndInstruction == 1 ? 1 : 0;
	}

	@Override
	public boolean putCounterBefore() {
		return mEndInstruction != 0;
	}
}
