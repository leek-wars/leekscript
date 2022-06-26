package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.HashMap;

import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.common.Error;

public abstract class AbstractLeekBlock extends LeekInstruction {

	protected ArrayList<LeekInstruction> mInstructions = new ArrayList<LeekInstruction>();
	protected AbstractLeekBlock mParent = null;
	protected HashMap<String, LeekVariable> mVariables = new HashMap<>();

	protected Token mDeclaringVariable = null;
	protected boolean mDeclaringVariableUsed = false;
	protected boolean mAccolade = true;
	protected MainLeekBlock mMain = null;
	protected int mEndInstruction = 0;
	protected boolean full = false;

	public AbstractLeekBlock(AbstractLeekBlock parent, MainLeekBlock main) {
		mParent = parent;
		mMain = main;
	}

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

	public void addInstruction(WordCompiler compiler, LeekInstruction instruction) throws LeekCompilerException {
		if (mEndInstruction != 0) {
			throw new LeekCompilerException(instruction.getLocation(), Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
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

	public void setDeclaringVariable(Token variable) {
		mDeclaringVariable = variable;
		mDeclaringVariableUsed = false;
	}

	public Token getDeclaringVariable() {
		mDeclaringVariableUsed = true;
		return mDeclaringVariable;
	}

	public boolean isDeclaringVariableUsed() {
		return mDeclaringVariableUsed;
	}

	public void checkEndBlock() {
		if (mParent != null)
			mParent.mEndInstruction = getEndBlock();
	}

	public AbstractLeekBlock endInstruction() {
		if (!mAccolade && mParent != null && isFull()) {
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

	public void addVariable(LeekVariable variable) {
		mVariables.put(variable.getName(), variable);
	}

	public LeekInstruction lastInstruction() {
		return mInstructions.size() == 0 ? null : mInstructions.get(mInstructions.size() - 1);
	}

	public boolean hasVariable(String variable) {
		return getVariable(variable, false) != null;
	}

	public LeekVariable getVariable(String variable, boolean includeClassMembers) {
		var v = mVariables.get(variable);
		if (v != null) return v;
		if (mParent != null)
			return mParent.getVariable(variable, includeClassMembers);
		return null;
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

	@Override
	public int getOperations() {
		return 0;
	}

	public int countInstructions() {
		return mInstructions.size() == 0 ? 1 : mInstructions.size();
	}

	public boolean isBreakable() {
		if (mParent == null || this instanceof AnonymousFunctionBlock)
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

	public void preAnalyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		for (var instruction : mInstructions) {
			instruction.preAnalyze(compiler);
		}
		compiler.setCurrentBlock(initialBlock);
	}

	public void analyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		for (var instruction : mInstructions) {
			instruction.analyze(compiler);
		}
		compiler.setCurrentBlock(initialBlock);
	}

	public void setFull(boolean full) {
		this.full = full;
	}

	public boolean isFull() {
		return lastInstruction() != null || this.full;
	}

	public boolean isInConstructor() {
		return mParent != null && mParent.isInConstructor();
	}
}
