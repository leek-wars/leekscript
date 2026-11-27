package leekscript.compiler.bytecode;

import java.util.List;

/**
 * Expression Java typée, construite depuis l'AST LeekScript à la place du texte de
 * {@link leekscript.compiler.JavaWriter}. Chaque nœud correspond à une construction du Java
 * que JavaWriter écrit (appel par nom, cast, littéral, opérateur natif…) : le backend en
 * déduit les types comme javac, résout les surcharges (cf {@link Resolver}) et émet le bytecode.
 */
public sealed interface JExpr {

	/** Littéral : Long/Double/Integer/Boolean/String, ou null (type {@link JTypes.Null}). */
	record Lit(Object value, Class<?> type) implements JExpr {}

	/** Variable locale de la méthode en cours. */
	record Local(BytecodeWriter.LocalVar var) implements JExpr {}

	/** `this` : l'instance de l'IA. */
	record This() implements JExpr {}

	/** Appel de méthode d'instance par son nom ; receveur null = `this` (méthode de l'IA). */
	record Call(JExpr receiver, String name, List<JExpr> args) implements JExpr {}

	/** Appel de méthode statique par son nom. */
	record StaticCall(Class<?> owner, String name, List<JExpr> args) implements JExpr {}

	/** Champ statique (`Double.NaN`…). */
	record GetStatic(Class<?> owner, String name) implements JExpr {}

	/** `new T(args)`. */
	record New(Class<?> type, List<JExpr> args) implements JExpr {}

	/** Cast Java `(T) e` : conversions de cast du JLS (checkcast, unboxing, conversions primitives). */
	record Cast(Class<?> type, JExpr expr) implements JExpr {}

	/** Opérateur binaire natif : + - * / % & | ^ << >> >>> == != < > <= >= && ||. */
	record Binary(String op, JExpr left, JExpr right) implements JExpr {}

	/** Opérateur unaire natif : `-` ou `!`. */
	record Unary(String op, JExpr expr) implements JExpr {}

	/** Affectation `x = e`, de valeur la valeur affectée. */
	record AssignLocal(BytecodeWriter.LocalVar var, JExpr value) implements JExpr {}

	/** Affectation composée native `x op= e` : `x = (T) (x op e)` (JLS 15.26.2). */
	record CompoundLocal(BytecodeWriter.LocalVar var, String op, JExpr value) implements JExpr {}

	/** `x++`, `x--`, `++x`, `--x` sur une variable locale primitive. */
	record IncLocal(BytecodeWriter.LocalVar var, boolean increment, boolean prefix) implements JExpr {}

	/** Ternaire `c ? a : b`. */
	record Cond(JExpr condition, JExpr then, JExpr otherwise) implements JExpr {}

	static Lit of(long v) { return new Lit(v, long.class); }
	static Lit of(int v) { return new Lit(v, int.class); }
	static Lit of(double v) { return new Lit(v, double.class); }
	static Lit of(boolean v) { return new Lit(v, boolean.class); }
	static Lit of(String v) { return new Lit(v, String.class); }
	static Lit nul() { return new Lit(null, JTypes.Null.class); }

	static Call call(String name, JExpr... args) { return new Call(null, name, List.of(args)); }
}
