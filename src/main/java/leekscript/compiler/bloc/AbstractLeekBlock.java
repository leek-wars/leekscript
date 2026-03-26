package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import leekscript.compiler.Token;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.instruction.LeekExpressionInstruction;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.compiler.NarrowingInfo;

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
	protected Map<String, Type> mNarrowedPropertyTypes = null;

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
			compiler.addError(new AnalyzeError(instruction.getLocation(), AnalyzeErrorLevel.ERROR, Error.CANT_ADD_INSTRUCTION_AFTER_BREAK));
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

	/**
	 * Look up a narrowed property type (e.g. "x.field") in this block or parent blocks.
	 * Used by LeekObjectAccess during type narrowing.
	 */
	public Type getNarrowedPropertyType(String key) {
		if (mNarrowedPropertyTypes != null) {
			var type = mNarrowedPropertyTypes.get(key);
			if (type != null) return type;
		}
		if (mParent != null) return mParent.getNarrowedPropertyType(key);
		return null;
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
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		int i = 0;
		writer.lastInstruction = false;
		for (LeekInstruction instruction : mInstructions) {
			i++;
			var last = i == mInstructions.size();
			if (last && this instanceof MainLeekBlock) {
				if (instruction instanceof LeekExpressionInstruction) {
					mainblock.writeBeforeReturn(writer);
					writer.addCode("return ");
					writer.lastInstruction = true;
					instruction.writeJavaCode(mainblock, writer, false);
				} else {
					instruction.writeJavaCode(mainblock, writer, false);
					if (mEndInstruction == 0) {
						mainblock.writeBeforeReturn(writer);
						writer.addLine("return null;");
					}
				}
			} else {
				instruction.writeJavaCode(mainblock, writer, false);
			}
		}
		if (this instanceof MainLeekBlock && mInstructions.size() == 0) {
			writer.addLine("return null;");
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

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		for (var instruction : mInstructions) {
			// if (instruction instanceof LeekGlobalDeclarationInstruction) {
			// 	continue; // Analysé avant
			// }
			instruction.preAnalyze(compiler);
		}
		compiler.setCurrentBlock(initialBlock);
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);

		// Track narrowings to restore at end of block (from early returns)
		var pendingRestores = new ArrayList<Map<LeekVariable, Type>>();

		for (var instruction : mInstructions) {
			instruction.analyze(compiler);

			// Case 2: Early return narrowing
			// If a conditional block (if without else, or if where body exits)
			// exits via return/break/continue, apply inverse narrowings for subsequent code
			if (instruction instanceof ConditionalBloc cb
				&& cb.getParentCondition() == null  // First 'if' in chain (not else-if)
				&& cb.mEndInstruction != 0           // Body exits (return/break/continue)
				&& cb.getNarrowingInfo() != null
				&& cb.getNarrowingInfo().hasFalse()) {
				pendingRestores.add(cb.getNarrowingInfo().applyFalse());
			}
		}

		// Restore all narrowings applied during this block
		for (var saved : pendingRestores) {
			NarrowingInfo.restore(saved);
		}

		// Check for unused variables (strict mode only)
		if (mMain != null && mMain.isStrict()) {
			for (var entry : mVariables.entrySet()) {
				var v = entry.getValue();
				if (v.getUsageCount() > 0) continue;
				if (v.getName().startsWith("_")) continue;
				var vt = v.getVariableType();
				if (vt != LeekVariable.VariableType.LOCAL && vt != LeekVariable.VariableType.ARGUMENT) continue;
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.WARNING, Error.UNUSED_VARIABLE, new String[] { v.getName() }));
			}
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
