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
import leekscript.runner.LeekFunctions;
import leekscript.compiler.instruction.LeekExpressionInstruction;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.common.Annotation;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.compiler.NarrowingInfo;

public abstract class AbstractLeekBlock extends LeekInstruction {

	protected ArrayList<LeekInstruction> mInstructions = new ArrayList<LeekInstruction>();
	protected AbstractLeekBlock mParent = null;
	// Lazy : la majorité des blocs (if/while/for sans var decl interne) restent
	// vides. ~145 HashMap allouées par compile de Quantum dont la plupart inutiles.
	protected HashMap<String, LeekVariable> mVariables = null;

	protected Token mDeclaringVariable = null;
	protected boolean mDeclaringVariableUsed = false;
	protected boolean mAccolade = true;
	protected MainLeekBlock mMain = null;
	protected int mEndInstruction = 0;
	protected boolean full = false;
	protected Map<String, Type> mNarrowedPropertyTypes = null;
	// Mis à true dans le constructeur des sous-classes qui override getVariable
	// (ClassMethodBlock). Plus rapide qu'un appel virtuel à overridesGetVariable()
	// dans la boucle de getVariable.
	protected boolean overridesGetVariable = false;

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
		if (mVariables == null) mVariables = new HashMap<>();
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
		// Fast path : la majorité des blocs (if/while/for) ont mVariables vide.
		// On évite le HashMap.get + on déroule la récursion en boucle pour les
		// blocs sans override (ClassMethodBlock casse la boucle pour préserver
		// son lookup polymorphe sur la classe ou les arguments).
		AbstractLeekBlock block = this;
		while (block != null) {
			if (block.mVariables != null) {
				var v = block.mVariables.get(variable);
				if (v != null) return v;
			}
			AbstractLeekBlock parent = block.mParent;
			// Si le parent override getVariable (ClassMethodBlock...), on ne peut
			// pas continuer la boucle : il faut dispatcher polymorphement.
			// Field read au lieu d'appel virtuel — appelé par chaque block walk.
			if (parent != null && parent.overridesGetVariable) {
				return parent.getVariable(variable, includeClassMembers);
			}
			block = parent;
		}
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

		// Lazy : la grande majorité des blocs n'ont aucun if avec narrowing → on
		// évite l'alloc de l'ArrayList pour rien sur chaque block.analyze().
		ArrayList<Map<LeekVariable, Type>> pendingRestores = null;

		for (var instruction : mInstructions) {
			instruction.analyze(compiler);

			// Apply false narrowings after a conditional block (if without else):
			// Case 2: body exits (return/break/continue) → inverse narrowings for subsequent code
			// Case 7: body assigns non-null to null-checked variables → both branches guarantee non-null
			if (instruction instanceof ConditionalBloc cb
				&& cb.getParentCondition() == null  // First 'if' in chain (not else-if)
				&& cb.getNarrowingInfo() != null
				&& cb.getNarrowingInfo().hasFalse()
				&& (cb.mEndInstruction != 0 || cb.isAssignmentSatisfiesFalse())) {
				if (pendingRestores == null) pendingRestores = new ArrayList<>();
				pendingRestores.add(cb.getNarrowingInfo().applyFalse());
				// Case 7: also apply property narrowings for subsequent code
				if (cb.mEndInstruction == 0) {
					var falsePropertyNarrowings = cb.getNarrowingInfo().getFalsePropertyNarrowings();
					if (!falsePropertyNarrowings.isEmpty()) {
						if (mNarrowedPropertyTypes == null) mNarrowedPropertyTypes = new HashMap<>();
						mNarrowedPropertyTypes.putAll(falsePropertyNarrowings);
					}
				}
			}
		}

		// Restore all narrowings applied during this block
		if (pendingRestores != null) for (var saved : pendingRestores) {
			NarrowingInfo.restore(saved);
		}

		var strict = mMain != null && mMain.isStrict();
		if (mVariables != null) for (var v : mVariables.values()) {
			if (v.hasAnnotation(Annotation.TODO)) {
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.WARNING, Error.ANNOTATION_TODO, new String[] { v.getName() }));
			}
			if (strict && v.getUsageCount() == 0 && !v.getName().startsWith("_") && !v.hasAnnotation(Annotation.UNUSED)) {
				var vt = v.getVariableType();
				if (vt == LeekVariable.VariableType.LOCAL || vt == LeekVariable.VariableType.ARGUMENT || vt == LeekVariable.VariableType.GLOBAL) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.WARNING, Error.UNUSED_VARIABLE, new String[] { v.getName() }));
				} else if (vt == LeekVariable.VariableType.FUNCTION && !LeekFunctions.isEntryPointFunction(v.getName())) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.WARNING, Error.UNUSED_FUNCTION, new String[] { v.getName() }));
				}
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

	// Est-on dans une méthode statique (donc sans `this`) ? Utilisé pour savoir si
	// le mot-clé `class` doit désigner la classe runtime de l'instance (méthode
	// d'instance / constructeur) ou la classe englobante (méthode statique).
	public boolean isInStaticMethod() {
		return mParent != null && mParent.isInStaticMethod();
	}
}
