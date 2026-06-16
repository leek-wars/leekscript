package leekscript.compiler;

import java.util.Set;

import leekscript.common.Error;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekArrayAccess;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekFunctionCall;
import leekscript.compiler.expression.LeekObjectAccess;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;

/**
 * Static purity analysis for the {@code @pure} annotation. A function annotated
 * {@code @pure} is expected to be free of observable side effects; this checker
 * detects the side effects that can be determined statically and emits an
 * {@link Error#ANNOTATION_NOT_PURE} warning otherwise.
 *
 * The checks are performed during the normal {@code analyze()} traversal: each
 * relevant node asks {@link WordCompiler#getCurrentPureFunction()} whether it is
 * inside a {@code @pure} function and, if so, reports any side effect it
 * introduces. A function is considered impure when its body, directly:
 * <ul>
 *   <li>assigns to / increments a variable that lives outside the function:
 *       a global, an object field, a static field, {@code this}, or a variable
 *       captured from an enclosing scope (mutating the function's own locals and
 *       parameters is pure);</li>
 *   <li>calls a built-in system function that mutates its arguments or performs
 *       I/O (see {@link #IMPURE_SYSTEM_FUNCTIONS});</li>
 *   <li>calls another user function that is not itself {@code @pure}.</li>
 * </ul>
 * Side effects hidden behind dynamic/method calls or aliasing are not detected,
 * so the check is intentionally conservative (it may miss impurities but does not
 * flag pure code).
 */
public class PurityChecker {

	// Built-in functions that mutate one of their arguments in place or perform
	// I/O. LeekFunctions carries no purity metadata, so the side-effecting
	// builtins are listed explicitly here (return type is not a reliable signal:
	// e.g. pop/setPut mutate but return a value).
	private static final Set<String> IMPURE_SYSTEM_FUNCTIONS = Set.of(
		// I/O
		"debug", "debugW", "debugE", "debugC",
		// Array mutators
		"push", "pushAll", "pop", "shift", "unshift", "insert",
		"remove", "removeElement", "removeKey", "fill", "sort", "arraySort",
		"shuffle", "reverse", "arrayClear", "arrayRemoveAll",
		"assocSort", "assocReverse", "keySort",
		// Map mutators
		"mapClear", "mapPut", "mapPutAll", "mapRemove", "mapRemoveAll",
		"mapReplace", "mapReplaceAll", "mapFill", "mapMerge",
		// Set mutators
		"setPut", "setRemove", "setClear"
	);

	public static boolean isImpureSystemFunction(String name) {
		return IMPURE_SYSTEM_FUNCTIONS.contains(name);
	}

	/**
	 * If an assignment or increment whose target is {@code target} mutates state
	 * visible outside {@code pureFunction}, returns the name of the mutated
	 * symbol; otherwise returns null. Mutating the function's own locals or
	 * parameters is pure and returns null.
	 */
	public static String externalMutation(Expression target, AbstractLeekBlock pureFunction) {
		var root = rootVariable(target);
		if (root == null) return null;
		switch (root.getVariableType()) {
			case GLOBAL:
			case FIELD:
			case STATIC_FIELD:
			case THIS:
				return root.getName();
			case LOCAL:
			case ARGUMENT:
			case ITERATOR:
				// Pure if the variable belongs to this function; impure if it is
				// captured from an enclosing scope (incl. top-level variables).
				var declaration = root.getDeclaration();
				if (declaration != null && declaration.getFunction() != pureFunction) {
					return root.getName();
				}
				return null;
			default:
				return null;
		}
	}

	/**
	 * Walks an l-value down to the variable whose state it ultimately mutates:
	 * {@code arr[i]} → {@code arr}, {@code obj.field} → {@code obj}, etc. Returns
	 * null when no underlying variable can be determined.
	 */
	private static LeekVariable rootVariable(Expression e) {
		while (true) {
			if (e instanceof LeekVariable v) return v;
			if (e instanceof LeekParenthesis p) { e = p.getExpression(); continue; }
			if (e instanceof LeekArrayAccess a) { e = a.getTabular(); continue; }
			if (e instanceof LeekObjectAccess o) { e = o.getObject(); continue; }
			if (e instanceof LeekFunctionCall fc) { e = fc.getCallExpression(); continue; }
			if (e instanceof LeekExpression x && x.getOperator() == Operators.NON_NULL_ASSERTION) { e = x.getExpression2(); continue; }
			return null;
		}
	}

	public static void reportNotPure(WordCompiler compiler, Location location, String detail) throws LeekCompilerException {
		compiler.addError(new AnalyzeError(location, AnalyzeErrorLevel.WARNING, Error.ANNOTATION_NOT_PURE, new String[] { detail }));
	}
}
