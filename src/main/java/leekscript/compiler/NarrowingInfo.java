package leekscript.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import leekscript.common.CompoundType;
import leekscript.common.MapType;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.compiler.expression.ConstantFolder;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekArray;
import leekscript.compiler.expression.LeekBigInteger;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekFunctionCall;
import leekscript.compiler.expression.LeekInterval;
import leekscript.compiler.expression.LeekMap;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekObject;
import leekscript.compiler.expression.LeekSet;
import leekscript.compiler.expression.LeekObjectAccess;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;

public class NarrowingInfo {

	// Maps et ensembles lazy : la grande majorité des conditions n'ont aucune narrowing
	// (juste un boolean simple comme `getFoo()`). On évite ainsi ~5 allocs par
	// condition analysée.
	private static final Map<LeekVariable, Type> EMPTY_VARS = Map.of();
	private static final Map<String, Type> EMPTY_PROPS = Map.of();
	private static final Map<String, LeekVariable> EMPTY_PROP_VARS = Map.of();
	private static final Set<MapEntry> EMPTY_ENTRIES = Set.of();

	private Map<LeekVariable, Type> trueNarrowings = null;
	private Map<LeekVariable, Type> falseNarrowings = null;
	private Map<String, Type> truePropertyNarrowings = null;
	private Map<String, Type> falsePropertyNarrowings = null;
	private Map<String, LeekVariable> propertyVariables = null;
	private Set<MapEntry> trueEntries = null;
	private Set<MapEntry> falseEntries = null;

	/**
	 * Case `map[key]` présente (`mapContainsKey(map, key)`), cf LeekArrayAccess. `map` est la
	 * déclaration de la variable ; `key` celle d'une variable, ou la valeur d'un littéral
	 * (Long, String). Par identité : un paramètre de closure homonyme est une autre variable.
	 */
	public record MapEntry(LeekVariable map, Object key) {}

	public Map<LeekVariable, Type> getTrueNarrowings() { return trueNarrowings != null ? trueNarrowings : EMPTY_VARS; }
	public Map<LeekVariable, Type> getFalseNarrowings() { return falseNarrowings != null ? falseNarrowings : EMPTY_VARS; }
	public Map<String, Type> getTruePropertyNarrowings() { return truePropertyNarrowings != null ? truePropertyNarrowings : EMPTY_PROPS; }
	public Map<String, Type> getFalsePropertyNarrowings() { return falsePropertyNarrowings != null ? falsePropertyNarrowings : EMPTY_PROPS; }
	public Map<String, LeekVariable> getPropertyVariables() { return propertyVariables != null ? propertyVariables : EMPTY_PROP_VARS; }
	public Set<MapEntry> getTrueEntries() { return trueEntries != null ? trueEntries : EMPTY_ENTRIES; }
	public Set<MapEntry> getFalseEntries() { return falseEntries != null ? falseEntries : EMPTY_ENTRIES; }

	public boolean hasTrue() {
		return (trueNarrowings != null && !trueNarrowings.isEmpty())
			|| (truePropertyNarrowings != null && !truePropertyNarrowings.isEmpty());
	}
	public boolean hasFalse() {
		return (falseNarrowings != null && !falseNarrowings.isEmpty())
			|| (falsePropertyNarrowings != null && !falsePropertyNarrowings.isEmpty());
	}

	private Map<LeekVariable, Type> trueOrAlloc() {
		if (trueNarrowings == null) trueNarrowings = new HashMap<>();
		return trueNarrowings;
	}
	private Map<LeekVariable, Type> falseOrAlloc() {
		if (falseNarrowings == null) falseNarrowings = new HashMap<>();
		return falseNarrowings;
	}
	private Map<String, Type> truePropOrAlloc() {
		if (truePropertyNarrowings == null) truePropertyNarrowings = new HashMap<>();
		return truePropertyNarrowings;
	}
	private Map<String, Type> falsePropOrAlloc() {
		if (falsePropertyNarrowings == null) falsePropertyNarrowings = new HashMap<>();
		return falsePropertyNarrowings;
	}
	private Map<String, LeekVariable> propVarsOrAlloc() {
		if (propertyVariables == null) propertyVariables = new HashMap<>();
		return propertyVariables;
	}
	private Set<MapEntry> trueEntriesOrAlloc() {
		if (trueEntries == null) trueEntries = new HashSet<>();
		return trueEntries;
	}
	private Set<MapEntry> falseEntriesOrAlloc() {
		if (falseEntries == null) falseEntries = new HashSet<>();
		return falseEntries;
	}

	/**
	 * Merge `source` dans info.trueNarrowings (si toTrue=true) ou info.falseNarrowings
	 * (sinon). Skip si source est null/vide pour éviter d'allouer la map de destination
	 * pour rien — cas hot quand la grande majorité des sub-narrowings sont vides.
	 */
	private static void mergeInto(NarrowingInfo info, Map<LeekVariable, Type> source, boolean toTrue) {
		if (source == null || source.isEmpty()) return;
		if (toTrue) info.trueOrAlloc().putAll(source);
		else info.falseOrAlloc().putAll(source);
	}

	private static void mergePropsInto(NarrowingInfo info, Map<String, Type> source, boolean toTrue) {
		if (source == null || source.isEmpty()) return;
		if (toTrue) info.truePropOrAlloc().putAll(source);
		else info.falsePropOrAlloc().putAll(source);
	}

	private static void mergeEntriesInto(NarrowingInfo info, Set<MapEntry> source, boolean toTrue) {
		if (source == null || source.isEmpty()) return;
		if (toTrue) info.trueEntriesOrAlloc().addAll(source);
		else info.falseEntriesOrAlloc().addAll(source);
	}

	/**
	 * Apply true narrowings and return saved original types for later restore
	 * (null si aucun narrowing à appliquer — restore null-safe).
	 */
	public Map<LeekVariable, Type> applyTrue() {
		return apply(trueNarrowings);
	}

	/**
	 * Apply false narrowings and return saved original types for later restore
	 * (null si aucun narrowing à appliquer — restore null-safe).
	 */
	public Map<LeekVariable, Type> applyFalse() {
		return apply(falseNarrowings);
	}

	private static Map<LeekVariable, Type> apply(Map<LeekVariable, Type> narrowings) {
		if (narrowings == null || narrowings.isEmpty()) return null;
		var saved = new HashMap<LeekVariable, Type>();
		for (var entry : narrowings.entrySet()) {
			saved.put(entry.getKey(), entry.getKey().getType());
			entry.getKey().setType(entry.getValue());
		}
		return saved;
	}

	/**
	 * Narrowings de variables, de propriétés (`this.x`, `A.X`) et de cases de map, le temps
	 * d'analyser une sous-expression (opérande droit d'un && / ||, branche d'un ternaire) : à
	 * rendre par restore(Saved). null quand il n'y a rien à appliquer (cas dominant).
	 */
	public record Saved(Map<LeekVariable, Type> variables, AbstractLeekBlock block, Map<String, Type> properties, Set<MapEntry> entries) {}

	public Saved applyTrue(AbstractLeekBlock block) {
		return apply(trueNarrowings, truePropertyNarrowings, trueEntries, block);
	}

	public Saved applyFalse(AbstractLeekBlock block) {
		return apply(falseNarrowings, falsePropertyNarrowings, falseEntries, block);
	}

	private static Saved apply(Map<LeekVariable, Type> narrowings, Map<String, Type> propertyNarrowings, Set<MapEntry> entries, AbstractLeekBlock block) {
		boolean onBlock = propertyNarrowings != null && !propertyNarrowings.isEmpty() || entries != null && !entries.isEmpty();
		if (!onBlock && (narrowings == null || narrowings.isEmpty())) return null;
		if (!onBlock) return new Saved(apply(narrowings), null, null, null);
		return new Saved(apply(narrowings), block, block.narrowPropertyTypes(propertyNarrowings), block.addPresentEntries(entries));
	}

	public static void restore(Saved saved) {
		if (saved == null) return;
		if (saved.block() != null) {
			saved.block().restoreNarrowedPropertyTypes(saved.properties());
			saved.block().restorePresentEntries(saved.entries());
		}
		restore(saved.variables());
	}

	public static void restore(Map<LeekVariable, Type> saved) {
		// null-safe : applyParentFalseNarrowings retourne null quand il n'y a pas
		// de parent (cas hot : premiers if/while/for).
		if (saved == null) return;
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
				info.trueOrAlloc().put(declVar, declVar.getType().assertNotNull());
				// False branch: can't narrow (x could be null, 0, false, "")
			}
			return;
		}

		// Case: truthy check on a property (if (this.field) where this.field is nullable)
		if (condition instanceof LeekObjectAccess oa) {
			String key = extractPropertyKey(oa);
			if (key != null) {
				if (oa.getVariable() != null) info.propVarsOrAlloc().put(key, oa.getVariable());
				var fieldType = oa.getType();
				if (fieldType.canBeNull()) {
					info.truePropOrAlloc().put(key, fieldType.assertNotNull());
				}
			}
			return;
		}

		if (condition instanceof LeekFunctionCall call) {
			extractMapContainsKey(call, info);
			// Case: type guard call (if (isString(x)) where isString a un prédicat `-> x is T`).
			// On affine l'argument à l'index gardé vers le type du guard, comme un `instanceof`.
			// Le callable porteur du guard est une fonction libre OU une méthode (this.isB(x),
			// isB(x), Class.isB(x)). Naturellement inerte hors v5 (aucun guard n'est jamais posé).
			var callable = call.getResolvedCallable();
			if (callable != null && callable.getTypeGuardType() != null) {
				int idx = callable.getTypeGuardParam();
				if (idx >= 0 && idx < call.getParameterCount()) {
					var declVar = extractVariable(call.getParameter(idx));
					if (declVar != null) {
						Type guardType = callable.getTypeGuardType();
						info.trueOrAlloc().put(declVar, guardType);
						Type currentType = declVar.getType();
						if (currentType instanceof CompoundType ct) {
							Type remaining = ct.removeType(guardType);
							if (remaining != currentType) {
								info.falseOrAlloc().put(declVar, remaining);
							}
						}
					}
				}
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
			mergeInto(info, inner.falseNarrowings, true);
			mergeInto(info, inner.trueNarrowings, false);
			mergePropsInto(info, inner.falsePropertyNarrowings, true);
			mergePropsInto(info, inner.truePropertyNarrowings, false);
			mergeEntriesInto(info, inner.falseEntries, true);
			mergeEntriesInto(info, inner.trueEntries, false);
		}
		// Case: a && b → true narrowings combine (both must be true)
		else if (op == Operators.AND) {
			var left = extract(e1);
			var right = extract(e2);
			mergeInto(info, left.trueNarrowings, true);
			mergeInto(info, right.trueNarrowings, true);
			mergePropsInto(info, left.truePropertyNarrowings, true);
			mergePropsInto(info, right.truePropertyNarrowings, true);
			mergeEntriesInto(info, left.trueEntries, true);
			mergeEntriesInto(info, right.trueEntries, true);
			// False: can't narrow (at least one is false, don't know which)
		}
		// Case: a || b → false narrowings combine (both must be false)
		else if (op == Operators.OR) {
			var left = extract(e1);
			var right = extract(e2);
			mergeInto(info, left.falseNarrowings, false);
			mergeInto(info, right.falseNarrowings, false);
			mergePropsInto(info, left.falsePropertyNarrowings, false);
			mergePropsInto(info, right.falsePropertyNarrowings, false);
			mergeEntriesInto(info, left.falseEntries, false);
			mergeEntriesInto(info, right.falseEntries, false);
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
				info.trueOrAlloc().put(declVar, Type.NULL);
				info.falseOrAlloc().put(declVar, nonNullType);
			} else {
				// x != null: true → non-null, false → null
				info.trueOrAlloc().put(declVar, nonNullType);
				info.falseOrAlloc().put(declVar, Type.NULL);
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
			info.trueOrAlloc().put(declVar, targetType);
			Type currentType = declVar.getType();
			if (currentType instanceof CompoundType ct) {
				Type remaining = ct.removeType(targetType);
				if (remaining != currentType) {
					info.falseOrAlloc().put(declVar, remaining);
				}
			}
			return;
		}

		// Fallback: property access narrowing (obj.field instanceof Type)
		String key = extractPropertyKey(e1);
		if (key == null) return;
		var oa = (LeekObjectAccess) unwrap(e1);
		info.truePropOrAlloc().put(key, targetType);
		var fieldType = oa.getType();
		if (fieldType instanceof CompoundType ct) {
			Type remaining = ct.removeType(targetType);
			if (remaining != fieldType) {
				info.falsePropOrAlloc().put(key, remaining);
			}
		}
	}

	/**
	 * Extract narrowing for property access (obj.field != null).
	 */
	private static void extractPropertyNullCheck(LeekObjectAccess oa, NarrowingInfo info, boolean isEquals) {
		String key = extractPropertyKey(oa);
		if (key == null) return;
		if (oa.getVariable() != null) info.propVarsOrAlloc().put(key, oa.getVariable());

		var fieldType = oa.getType();
		if (fieldType.canBeNull()) {
			var nonNullType = fieldType.assertNotNull();
			if (isEquals) {
				info.truePropOrAlloc().put(key, Type.NULL);
				info.falsePropOrAlloc().put(key, nonNullType);
			} else {
				info.truePropOrAlloc().put(key, nonNullType);
				info.falsePropOrAlloc().put(key, Type.NULL);
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

	/** `mapContainsKey(m, k)` vrai : la case `m[k]` existe. */
	private static void extractMapContainsKey(LeekFunctionCall call, NarrowingInfo info) {
		if (!"mapContainsKey".equals(call.systemFunctionName()) || call.getParameterCount() != 2) return;
		var entry = mapEntry(call.getParameter(0), call.getParameter(1));
		if (entry != null) info.trueEntriesOrAlloc().add(entry);
	}

	/**
	 * Type des valeurs de la map pour la lecture `map[key]` quand mapContainsKey garantit ici la
	 * présence de la case (AbstractLeekBlock.isPresentEntry), null sinon. Il tait les avertissements
	 * de la lecture (LeekArrayAccess.getWarningType), et devient son type si narrowsEntryType.
	 */
	public static Type presentEntryType(AbstractLeekBlock block, Expression map, Expression key) {
		if (!(map.getType() instanceof MapType type) || type.element() == Type.VOID || !block.hasPresentEntries()) return null;
		var entry = mapEntry(map, key);
		return entry != null && block.isPresentEntry(entry) ? type.element() : null;
	}

	/**
	 * Une case présente de `map` (cf presentEntryType) prend-elle le type des valeurs, comme le
	 * narrowing de `x != null` ? Si elle ne peut pas valoir null (LeekVariable.cellsMayBeNull) :
	 * sinon, une map reçue en paramètre par exemple, seuls les avertissements se taisent.
	 */
	public static boolean narrowsEntryType(Expression map) {
		var variable = unmodifiedVariable(map);
		return variable != null && !variable.cellsMayBeNull();
	}

	/** L'écriture `m[k] <operator> value` ne range jamais null : `=` d'une valeur isNullFree, ou un calcul (`+=`, `++`…), pas `??=`. */
	public static boolean writesNullFree(int operator, Expression value) {
		return operator == Operators.ASSIGN ? isNullFree(value) : operator != Operators.COALESCE_ASSIGN;
	}

	/**
	 * `value` ne vaut jamais null, quoi qu'en dise son type : en strict, un champ non initialisé,
	 * une case de tableau hors bornes ou un `x!` sont typés non nullables mais peuvent valoir null.
	 * Un littéral, `new` d'une classe dont les instances ne sont pas null, ou un calcul où null
	 * compte pour 0 ou faux (LS4+ : en v1, `/` et `%` par zéro valent null). Sur la forme de
	 * l'expression, ses variables résolues (preAnalyze).
	 */
	public static boolean isNullFree(Expression value) {
		value = value.trim();
		if (value instanceof LeekNumber || value instanceof LeekBigInteger || value instanceof LeekString || value instanceof LeekBoolean
			|| value instanceof LeekArray || value instanceof LeekMap || value instanceof LeekSet || value instanceof LeekInterval || value instanceof LeekObject) {
			return true;
		}
		if (!(value instanceof LeekExpression expression)) return false;
		return switch (expression.getOperator()) {
			case Operators.NEW -> constructsNonNull(expression.getExpression2());
			case Operators.ADD, Operators.MINUS, Operators.MULTIPLIE, Operators.DIVIDE, Operators.INTEGER_DIVISION, Operators.MODULUS, Operators.POWER, Operators.UNARY_MINUS, Operators.NOT,
				Operators.LESS, Operators.LESSEQUALS, Operators.MORE, Operators.MOREEQUALS, Operators.EQUALS, Operators.NOTEQUALS,
				Operators.EQUALS_EQUALS, Operators.NOT_EQUALS_EQUALS, Operators.INSTANCEOF, Operators.IN, Operators.NOT_IN,
				Operators.AND, Operators.OR, Operators.XOR, Operators.BITAND, Operators.BITOR, Operators.BITXOR, Operators.BITNOT,
				Operators.SHIFT_LEFT, Operators.SHIFT_RIGHT, Operators.SHIFT_UNSIGNED_RIGHT -> true;
			default -> false;
		};
	}

	/** `new constructed` crée une instance non null : pas `new f()` d'une fonction, qui rend son résultat, ni `new Null()`. */
	private static boolean constructsNonNull(Expression constructed) {
		var callee = constructed instanceof LeekFunctionCall call ? call.getCallExpression() : constructed;
		if (!(callee.trim() instanceof LeekVariable variable) || variable.getVariableType() != LeekVariable.VariableType.CLASS) return false;
		var clazz = variable.getClassDeclaration();
		return clazz != null && !clazz.getEmptyType().canBeNull();
	}

	/**
	 * Conversion de `value` vers `target` pour un avertissement : celle de son type d'avertissement
	 * (Expression.getWarningType) si elle est moins risquée que `cast`. Sans avertissement
	 * (≤ UPCAST) ou incompatible (erreur en strict), `cast` reste tel quel : on ne fait que taire.
	 */
	public static CastType warningCast(Type target, Expression value, CastType cast) {
		if (!softens(cast)) return cast;
		var type = value.getWarningType();
		if (type == value.getType()) return cast;
		var narrowed = target.accepts(type);
		return narrowed.ordinal() < cast.ordinal() ? narrowed : cast;
	}

	public static CastType warningCast(Type target, Expression value) {
		return warningCast(target, value, target.accepts(value.getType()));
	}

	private static boolean softens(CastType cast) {
		return cast.ordinal() > CastType.UPCAST.ordinal() && cast != CastType.INCOMPATIBLE;
	}

	/** Comme warningCast, pour les arguments `arguments` (de types `types`) d'un appel à `function`. */
	public static CastType warningArgumentsCast(Type function, List<Expression> arguments, List<Type> types, CastType cast) {
		if (!softens(cast)) return cast;
		List<Type> warningTypes = null;
		for (int i = 0; i < arguments.size(); i++) {
			var type = arguments.get(i).getWarningType();
			if (type == types.get(i)) continue;
			if (warningTypes == null) warningTypes = new ArrayList<>(types);
			warningTypes.set(i, type);
		}
		if (warningTypes == null) return cast;
		var narrowed = function.acceptsArguments(warningTypes);
		return narrowed.ordinal() < cast.ordinal() ? narrowed : cast;
	}

	/**
	 * Case `map[key]` du test mapContainsKey ou d'une lecture, si elle désigne la même case
	 * partout où elle est dite présente (le preAnalyze a vu tout le programme, boucles et autres
	 * fonctions comprises) :
	 * - `map`, une variable jamais réécrite, dont la map ne perd pas de clé par elle
	 *   (mayLoseEntries) ;
	 * - `key`, un littéral entier ou chaîne (constantes statiques comprises), ou une variable
	 *   jamais réécrite, de type immuable : un tableau ou un objet en clé change sans affectation.
	 * null sinon.
	 */
	private static MapEntry mapEntry(Expression map, Expression key) {
		var mapVariable = unmodifiedVariable(map);
		if (mapVariable == null || mapVariable.mayLoseEntries()) return null;
		var literal = ConstantFolder.literal(key, null);
		Object keyValue = literal instanceof LeekNumber number && number.getType() == Type.INT ? (Object) number.getLongValue()
			: literal instanceof LeekString string ? string.getText()
			: unmodifiedVariable(key);
		if (keyValue == null || keyValue instanceof LeekVariable && !isImmutable(key.getType())) return null;
		return new MapEntry(mapVariable, keyValue);
	}

	private static boolean isImmutable(Type type) {
		var value = type.assertNotNull();
		return value.isPrimitive() || value == Type.STRING || value == Type.BIG_INT;
	}

	private static LeekVariable unmodifiedVariable(Expression expression) {
		if (!(unwrap(expression) instanceof LeekVariable reference)) return null;
		var variable = reference.getVariable();
		if (variable == null || variable.isReassigned()) return null;
		// Un champ s'écrit aussi par `this.x = …`, que setReassigned ne voit pas
		var kind = variable.getVariableType();
		if (kind != LeekVariable.VariableType.LOCAL && kind != LeekVariable.VariableType.GLOBAL && kind != LeekVariable.VariableType.ARGUMENT) return null;
		// Un itérateur de foreach est réécrit à chaque tour : une closure qui le capture lit le dernier
		var declaration = variable.getDeclaration();
		return declaration != null && declaration.isIterator() && declaration.isCaptured() ? null : variable;
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
