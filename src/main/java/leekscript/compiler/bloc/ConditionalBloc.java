package leekscript.compiler.bloc;

import java.util.HashMap;
import java.util.Map;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.NarrowingInfo;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;

public class ConditionalBloc extends AbstractLeekBlock {

	private ConditionalBloc mParentCondition = null;
	private Expression mCondition = null;
	private final Token token;
	private boolean mPutCounterBefore = false;
	private NarrowingInfo narrowingInfo = null;
	private boolean assignmentSatisfiesFalse = false;

	public ConditionalBloc(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
	}

	public void setParentCondition(ConditionalBloc parent) {
		mParentCondition = parent;
	}

	public ConditionalBloc getParentCondition() {
		return mParentCondition;
	}

	public void setCondition(Expression condition) {
		mCondition = condition;
	}

	public Expression getCondition() {
		return mCondition;
	}

	public NarrowingInfo getNarrowingInfo() {
		return narrowingInfo;
	}

	public boolean isAssignmentSatisfiesFalse() {
		return assignmentSatisfiesFalse;
	}

	@Override
	public String getCode() {
		String str = "";
		if(mParentCondition == null) str = "if (" + mCondition.toString() + ") {";
		else if(mCondition != null) str = "else if (" + mCondition.toString() + ") {";
		else str = "else {";
		str += "\n" + super.getCode();
		return str + "}";
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (mCondition != null) {
			mCondition.preAnalyze(compiler);
		}
		super.preAnalyze(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {

		// Step 1: Apply false narrowings from parent conditions (for else-if and else blocks)
		var parentSaved = applyParentFalseNarrowings();

		if (mCondition != null) {
			// Analyze the condition (benefits from parent false narrowings if else-if)
			mCondition.analyze(compiler);
			mCondition.operations++; // On rajoute une opé pour le if sur la condition

			// Step 2: Extract narrowing info from this condition
			narrowingInfo = NarrowingInfo.extract(mCondition);

			// Step 3: Apply true narrowings for the body
			var saved = narrowingInfo.applyTrue();

			// Step 3b: Clear lastAssignedType on narrowed variables to avoid stale data
			// from assignments before the if block
			for (var v : narrowingInfo.getFalseNarrowings().keySet()) v.clearLastAssignedType();
			for (var key : narrowingInfo.getFalsePropertyNarrowings().keySet()) {
				var v = narrowingInfo.getPropertyVariables().get(key);
				if (v != null) v.clearLastAssignedType();
			}

			// Step 4: Apply property narrowings to the block
			applyPropertyNarrowings(narrowingInfo.getTruePropertyNarrowings());

			super.analyze(compiler);

			// Step 4b: Check assignment narrowing pattern (if body doesn't exit)
			// Pattern: if (x == null) { x = nonNull } → x is non-null after the if
			if (mEndInstruction == 0 && narrowingInfo.hasFalse()) {
				assignmentSatisfiesFalse = checkAssignmentSatisfiesFalse();
			}

			// Step 5: Restore narrowings
			clearPropertyNarrowings();
			NarrowingInfo.restore(saved);
		} else {
			// This is an 'else' block - parent false narrowings are already applied
			// Also apply property narrowings from parent false branch
			if (mParentCondition != null && mParentCondition.narrowingInfo != null) {
				applyPropertyNarrowings(mParentCondition.narrowingInfo.getFalsePropertyNarrowings());
			}

			super.analyze(compiler);

			clearPropertyNarrowings();
		}

		// Step 6: Restore parent false narrowings
		NarrowingInfo.restore(parentSaved);
	}

	/**
	 * Apply false narrowings from the parent condition chain (for else-if / else blocks).
	 * Returns saved original types for later restore.
	 */
	private Map<LeekVariable, Type> applyParentFalseNarrowings() {
		var saved = new HashMap<LeekVariable, Type>();
		if (mParentCondition != null) {
			collectParentFalseNarrowings(mParentCondition, saved);
		}
		return saved;
	}

	private void collectParentFalseNarrowings(ConditionalBloc parent, Map<LeekVariable, Type> saved) {
		// Recurse to get earlier conditions first
		if (parent.mParentCondition != null) {
			collectParentFalseNarrowings(parent.mParentCondition, saved);
		}
		// Apply this parent's false narrowings
		if (parent.narrowingInfo != null) {
			for (var entry : parent.narrowingInfo.getFalseNarrowings().entrySet()) {
				if (!saved.containsKey(entry.getKey())) {
					saved.put(entry.getKey(), entry.getKey().getType());
				}
				entry.getKey().setType(entry.getValue());
			}
		}
	}

	/**
	 * Store property narrowings on the block for LeekObjectAccess to consult.
	 */
	private void applyPropertyNarrowings(Map<String, Type> propertyNarrowings) {
		if (propertyNarrowings != null && !propertyNarrowings.isEmpty()) {
			mNarrowedPropertyTypes = new HashMap<>(propertyNarrowings);
		}
	}

	/**
	 * Check if the body assigned non-null values to all variables that the condition
	 * null-checked. This detects the pattern: if (x == null) { x = nonNull }
	 * For field variables (property narrowings), the body must have a single
	 * instruction to avoid side effects from subsequent method calls.
	 */
	private boolean checkAssignmentSatisfiesFalse() {
		// Check variable narrowings (local variables — safe from side effects)
		for (var entry : narrowingInfo.getFalseNarrowings().entrySet()) {
			var variable = entry.getKey();
			// Skip if the false narrowing itself is null — applying it would
			// narrow the variable to null after the if, which is never useful.
			// This happens for `if (x != null) { x = nonNull }` where the
			// false branch means x was null. The assignment narrowing pattern
			// only makes sense for `if (x == null) { x = nonNull }`.
			if (entry.getValue().canBeNull()) return false;
			var assignedType = variable.getLastAssignedType();
			if (assignedType == null || assignedType.canBeNull()) return false;
		}
		// Check property narrowings (field variables — single instruction only)
		if (!narrowingInfo.getFalsePropertyNarrowings().isEmpty()) {
			if (mInstructions.size() != 1) return false;
			for (var entry : narrowingInfo.getFalsePropertyNarrowings().entrySet()) {
				var fieldVar = narrowingInfo.getPropertyVariables().get(entry.getKey());
				if (fieldVar == null) return false;
				var assignedType = fieldVar.getLastAssignedType();
				if (assignedType == null || assignedType.canBeNull()) return false;
			}
		}
		return true;
	}

	private void clearPropertyNarrowings() {
		mNarrowedPropertyTypes = null;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (mParentCondition == null) {
			writer.addCode("if (");
			if (writer.isOperationsEnabled() && mCondition.getOperations() > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mCondition, false);
			if (writer.isOperationsEnabled() && mCondition.getOperations() > 0) {
				writer.addCode(", " + mCondition.getOperations() + ")");
			}
			writer.addLine(") {", getLocation());
		} else if (mCondition != null) {
			writer.addCode("else if (");
			if (writer.isOperationsEnabled() && mCondition.getOperations() > 0) {
				writer.addCode("ops(");
			}
			writer.getBoolean(mainblock, mCondition, false);
			if (writer.isOperationsEnabled() && mCondition.getOperations() > 0) {
				writer.addCode(", " + mCondition.getOperations() + ")");
			}
			writer.addLine(") {", getLocation());
		}
		else writer.addLine("else {", getLocation());
		super.writeJavaCode(mainblock, writer, false);
		writer.addLine("}");
	}

	public int getConditionEndBlock() {
		if (mEndInstruction == 0) return 0;
		if (mParentCondition != null) {
			int parent = mParentCondition.getConditionEndBlock();
			if (parent == 0) return 0;
			return parent | mEndInstruction;
		}
		return mEndInstruction;
	}

	@Override
	public int getEndBlock() {
		if (mCondition == null) {
			int r = getConditionEndBlock();
			if (r != 0) setPutCounterBefore(true);
			return r;
		}
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return mPutCounterBefore;
	}

	private void setPutCounterBefore(boolean value) {
		if (mParentCondition != null) mParentCondition.setPutCounterBefore(value);
		else mPutCounterBefore = value;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		return Type.VOID;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
