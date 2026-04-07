package leekscript.compiler;

import java.util.HashMap;
import java.util.Map;

import leekscript.common.CompoundType;
import leekscript.common.Type;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekObjectAccess;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;

public class NarrowingInfo {

	private final Map<LeekVariable, Type> trueNarrowings = new HashMap<>();
	private final Map<LeekVariable, Type> falseNarrowings = new HashMap<>();

	// Property access narrowings (case 6): key = "varName.fieldName"
	private final Map<String, Type> truePropertyNarrowings = new HashMap<>();
	private final Map<String, Type> falsePropertyNarrowings = new HashMap<>();
	// Maps property keys to their field variables (for assignment narrowing detection)
	private final Map<String, LeekVariable> propertyVariables = new HashMap<>();

	public Map<LeekVariable, Type> getTrueNarrowings() { return trueNarrowings; }
	public Map<LeekVariable, Type> getFalseNarrowings() { return falseNarrowings; }
	public Map<String, Type> getTruePropertyNarrowings() { return truePropertyNarrowings; }
	public Map<String, Type> getFalsePropertyNarrowings() { return falsePropertyNarrowings; }
	public Map<String, LeekVariable> getPropertyVariables() { return propertyVariables; }

	public boolean hasTrue() { return !trueNarrowings.isEmpty() || !truePropertyNarrowings.isEmpty(); }
	public boolean hasFalse() { return !falseNarrowings.isEmpty() || !falsePropertyNarrowings.isEmpty(); }

	/**
	 * Apply true narrowings and return saved original types for later restore.
	 */
	public Map<LeekVariable, Type> applyTrue() {
		return apply(trueNarrowings);
	}

	/**
	 * Apply false narrowings and return saved original types for later restore.
	 */
	public Map<LeekVariable, Type> applyFalse() {
		return apply(falseNarrowings);
	}

	private static Map<LeekVariable, Type> apply(Map<LeekVariable, Type> narrowings) {
		var saved = new HashMap<LeekVariable, Type>();
		for (var entry : narrowings.entrySet()) {
			saved.put(entry.getKey(), entry.getKey().getType());
			entry.getKey().setType(entry.getValue());
		}
		return saved;
	}

	public static void restore(Map<LeekVariable, Type> saved) {
		for (var entry : saved.entrySet()) {
			entry.getKey().setType(entry.getValue());
		}
	}

	/**
	 * Extract narrowing info from a condition expression.
	 */
	public static NarrowingInfo extract(Expression condition) {
		var info = new NarrowingInfo();
		extractInto(condition, info);
		return info;
	}

	/**
	 * Unwrap parentheses from an expression.
	 */
	private static Expression unwrap(Expression expr) {
		while (expr instanceof LeekParenthesis lp) {
			expr = lp.getExpression();
		}
		return expr;
	}

	private static void extractInto(Expression condition, NarrowingInfo info) {
		// Unwrap parentheses: !(x == null) → the inner (x == null) is a LeekParenthesis
		condition = unwrap(condition);

		// Case: truthy check on a variable (if (x) where x is nullable)
		if (condition instanceof LeekVariable lv) {
			var declVar = lv.getVariable();
			if (declVar != null && declVar.getType().canBeNull()) {
				info.trueNarrowings.put(declVar, declVar.getType().assertNotNull());
				// False branch: can't narrow (x could be null, 0, false, "")
			}
			return;
		}

		if (!(condition instanceof LeekExpression expr)) return;

		int op = expr.getOperator();
		var e1 = expr.getExpression1();
		var e2 = expr.getExpression2();

		// Case: x != null or x !== null
		if (op == Operators.NOTEQUALS || op == Operators.NOT_EQUALS_EQUALS) {
			extractNullCheck(e1, e2, info, false);
		}
		// Case: x == null or x === null
		else if (op == Operators.EQUALS || op == Operators.EQUALS_EQUALS) {
			extractNullCheck(e1, e2, info, true);
		}
		// Case: !expr → invert narrowings
		else if (op == Operators.NOT) {
			var inner = extract(e2);
			info.trueNarrowings.putAll(inner.falseNarrowings);
			info.falseNarrowings.putAll(inner.trueNarrowings);
			info.truePropertyNarrowings.putAll(inner.falsePropertyNarrowings);
			info.falsePropertyNarrowings.putAll(inner.truePropertyNarrowings);
		}
		// Case: a && b → true narrowings combine (both must be true)
		else if (op == Operators.AND) {
			var left = extract(e1);
			var right = extract(e2);
			info.trueNarrowings.putAll(left.trueNarrowings);
			info.trueNarrowings.putAll(right.trueNarrowings);
			info.truePropertyNarrowings.putAll(left.truePropertyNarrowings);
			info.truePropertyNarrowings.putAll(right.truePropertyNarrowings);
			// False: can't narrow (at least one is false, don't know which)
		}
		// Case: a || b → false narrowings combine (both must be false)
		else if (op == Operators.OR) {
			var left = extract(e1);
			var right = extract(e2);
			info.falseNarrowings.putAll(left.falseNarrowings);
			info.falseNarrowings.putAll(right.falseNarrowings);
			info.falsePropertyNarrowings.putAll(left.falsePropertyNarrowings);
			info.falsePropertyNarrowings.putAll(right.falsePropertyNarrowings);
			// True: can't narrow (at least one is true, don't know which)
		}
		// Case: x instanceof Type
		else if (op == Operators.INSTANCEOF) {
			extractInstanceof(e1, e2, info);
		}
	}

	/**
	 * Extract narrowing from a null comparison (x == null or x != null).
	 * Also handles property access (obj.field == null).
	 * @param isEquals true for == null (narrow to null in true branch), false for != null
	 */
	private static void extractNullCheck(Expression e1, Expression e2, NarrowingInfo info, boolean isEquals) {
		// Determine which side is null and which is the expression
		Expression nonNullSide = null;
		if (e2 instanceof LeekNull && e1 != null) nonNullSide = e1;
		else if (e1 instanceof LeekNull && e2 != null) nonNullSide = e2;
		if (nonNullSide == null) return;

		// Case 6: property access narrowing (obj.field != null)
		if (nonNullSide instanceof LeekObjectAccess oa) {
			extractPropertyNullCheck(oa, info, isEquals);
			return;
		}

		// Variable narrowing (cases 1-5)
		var declVar = extractVariable(nonNullSide);
		if (declVar != null && declVar.getType().canBeNull()) {
			var nonNullType = declVar.getType().assertNotNull();
			if (isEquals) {
				// x == null: true → null, false → non-null
				info.trueNarrowings.put(declVar, Type.NULL);
				info.falseNarrowings.put(declVar, nonNullType);
			} else {
				// x != null: true → non-null, false → null
				info.trueNarrowings.put(declVar, nonNullType);
				info.falseNarrowings.put(declVar, Type.NULL);
			}
		}
	}

	/**
	 * Extract the target type from the right-hand side of an instanceof expression.
	 */
	private static Type extractInstanceofTargetType(Expression e2) {
		if (e2 instanceof LeekType lt) {
			return lt.type;
		} else if (e2 instanceof LeekVariable lv && lv.getVariableType() == LeekVariable.VariableType.CLASS) {
			var classDecl = lv.getClassDeclaration();
			if (classDecl != null) {
				return classDecl.classType;
			}
		}
		return null;
	}

	/**
	 * Extract narrowing from an instanceof check (x instanceof Type).
	 * Supports both direct variables and property accesses (obj.field instanceof Type).
	 */
	private static void extractInstanceof(Expression e1, Expression e2, NarrowingInfo info) {
		if (e1 == null) return;

		Type targetType = extractInstanceofTargetType(e2);
		if (targetType == null) return;

		// Try direct variable narrowing first (handles simple vars, this.field, class.field)
		var declVar = extractVariable(e1);
		if (declVar != null) {
			info.trueNarrowings.put(declVar, targetType);
			Type currentType = declVar.getType();
			if (currentType instanceof CompoundType ct) {
				Type remaining = ct.removeType(targetType);
				if (remaining != currentType) {
					info.falseNarrowings.put(declVar, remaining);
				}
			}
			return;
		}

		// Fallback: property access narrowing (obj.field instanceof Type)
		String key = extractPropertyKey(e1);
		if (key == null) return;
		var oa = (LeekObjectAccess) unwrap(e1);
		info.truePropertyNarrowings.put(key, targetType);
		var fieldType = oa.getType();
		if (fieldType instanceof CompoundType ct) {
			Type remaining = ct.removeType(targetType);
			if (remaining != fieldType) {
				info.falsePropertyNarrowings.put(key, remaining);
			}
		}
	}

	/**
	 * Extract narrowing for property access (obj.field != null).
	 */
	private static void extractPropertyNullCheck(LeekObjectAccess oa, NarrowingInfo info, boolean isEquals) {
		String key = extractPropertyKey(oa);
		if (key == null) return;
		if (oa.getVariable() != null) info.propertyVariables.put(key, oa.getVariable());

		var fieldType = oa.getType();
		if (fieldType.canBeNull()) {
			var nonNullType = fieldType.assertNotNull();
			if (isEquals) {
				info.truePropertyNarrowings.put(key, Type.NULL);
				info.falsePropertyNarrowings.put(key, nonNullType);
			} else {
				info.truePropertyNarrowings.put(key, nonNullType);
				info.falsePropertyNarrowings.put(key, Type.NULL);
			}
		}
	}

	/**
	 * Extract a property narrowing key ("varName.fieldName") from a property access expression.
	 * Returns null if the expression is not a valid property access for narrowing.
	 */
	private static String extractPropertyKey(Expression expr) {
		expr = unwrap(expr);
		if (!(expr instanceof LeekObjectAccess oa)) return null;
		var obj = oa.getObject();
		if (!(obj instanceof LeekVariable lv)) return null;
		var declVar = lv.getVariable();
		if (declVar == null) return null;
		String fieldName = oa.getField();
		if (fieldName == null || fieldName.isEmpty()) return null;
		return declVar.getName() + "." + fieldName;
	}

	/**
	 * Extract the declaration variable from an expression.
	 * Handles direct variables and assignment expressions (case 5).
	 */
	private static LeekVariable extractVariable(Expression expr) {
		// Unwrap parentheses: (x = getValue()) → x = getValue()
		expr = unwrap(expr);

		// Direct variable reference
		var v = expr.getVariable();
		if (v != null) return v;

		// Assignment expression: (x = getValue()) → extract x
		if (expr instanceof LeekExpression le && le.getOperator() == Operators.ASSIGN) {
			return extractVariable(le.getExpression1());
		}

		return null;
	}
}
