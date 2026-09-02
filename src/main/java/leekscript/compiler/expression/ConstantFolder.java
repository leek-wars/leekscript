package leekscript.compiler.expression;

import java.util.List;

import leekscript.common.Type;
import leekscript.compiler.bloc.ConditionalBloc;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.BlankInstruction;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.compiler.instruction.LeekReturnInstruction;

/**
 * Constantes de compilation et pliage de constantes (feature « DCE moderne »).
 *
 * Trois couches, toutes appliquées au codegen (l'analyse n'est pas modifiée,
 * l'éditeur voit donc les mêmes diagnostics qu'avant) :
 *
 * 1. Un champ `static final` dont l'initialiseur est un LITTÉRAL scalaire
 *    (nombre, chaîne, booléen, null, moins unaire compris) est une constante de
 *    compilation : ses lectures statiquement résolues sont inlinées dans le Java
 *    généré, comme les constantes moteur (LeekConstants / SYSTEM_CONSTANT).
 *    L'idiome runtime (`static final X = tune()`) n'est PAS plié : seul un
 *    initialiseur littéral direct compte, sans chaînage de champ à champ (le
 *    chaînage dépendrait de l'ordre d'initialisation, et pourrait boucler).
 * 2. Une condition de `if`/`else if` qui se réduit à une constante (littéraux et
 *    champs pliés combinés par `!`, `&&`, `||`) est émise telle quelle, sans
 *    compteur d'opérations : un `if` constant ne coûte plus rien, et javac
 *    élimine la branche morte du bytecode.
 * 3. cf {@link LeekFunctionCall#isEliminable} : un appel statiquement résolu vers
 *    une fonction VIDE (corps réduit à rien après pliage) ou CONSTANTE (unique
 *    `return <littéral>`) est supprimé ou substitué par son littéral.
 *
 * Sûreté de la couche 1 : un champ `static final` scalaire ne peut pas changer
 * après son init (setField/compileSet lèvent CANNOT_ASSIGN_FINAL_FIELD, un champ
 * statique ne peut pas être shadowé par une sous-classe — FIELD_ALREADY_EXISTS
 * via hasStaticMember qui remonte la hiérarchie — et en v2+ les arguments passent
 * par valeur, la Box du champ ne s'échappe jamais). Seules les lectures
 * statiquement résolues sont inlinées : le champ runtime reste initialisé et les
 * accès dynamiques voient la même valeur.
 */
public final class ConstantFolder {

	private ConstantFolder() {}

	/**
	 * Réduit une expression à son littéral de compilation, ou null si elle n'est
	 * pas constante. Renvoie un nœud littéral existant (ou synthétisé pour le
	 * moins unaire et la promotion int → real) prêt à être émis via writeJavaCode.
	 */
	public static Expression literal(Expression expression, ClassDeclarationInstruction fromClass) {
		if (expression == null) return null;
		var expr = expression.trim();
		var direct = directLiteral(expr);
		if (direct != null) return direct;
		if (expr instanceof LeekVariable v && v.getVariableType() == VariableType.STATIC_FIELD) {
			return staticFinalLiteral(v.getClassDeclaration(), v.getName(), fromClass);
		}
		if (expr instanceof LeekObjectAccess oa
			&& oa.getVariable() != null && oa.getVariable().getVariableType() == VariableType.STATIC_FIELD
			&& !oa.isOptional()
			&& oa.getObject().trim() instanceof LeekVariable v && v.getVariableType() == VariableType.CLASS) {
			return staticFinalLiteral(v.getClassDeclaration(), oa.getField(), fromClass);
		}
		return null;
	}

	/**
	 * Champ statique de `clazz` (hiérarchie comprise) LISIBLE depuis `fromClass`,
	 * ou null. Même contrôle d'accès que l'analyse (canAccessStaticField) : une
	 * lecture interdite renvoie null + log d'erreur au runtime — elle ne doit
	 * donc jamais être pliée ni jugée pure.
	 */
	private static ClassDeclarationInstruction.ClassDeclarationField readableStaticField(ClassDeclarationInstruction clazz, String fieldName, ClassDeclarationInstruction fromClass) {
		if (clazz.canAccessStaticField(fieldName, fromClass) != null) return null;
		return clazz.getStaticField(fieldName);
	}

	/**
	 * Littéral direct : nombre, chaîne, booléen, null, ou moins unaire sur un
	 * nombre. Ne suit aucun champ (utilisé pour qualifier un initialiseur).
	 */
	private static Expression directLiteral(Expression expr) {
		if (expr instanceof LeekNumber || expr instanceof LeekString || expr instanceof LeekBoolean || expr instanceof LeekNull) {
			return expr;
		}
		if (expr instanceof LeekTernaire) return null;
		if (expr instanceof LeekExpression operation && operation.getOperator() == Operators.UNARY_MINUS && operation.getExpression2() != null) {
			if (operation.getExpression2().trim() instanceof LeekNumber number) {
				if (number.getType() == Type.INT) return new LeekNumber(number.token, 0, -number.getLongValue(), Type.INT);
				if (number.getType() == Type.REAL) return new LeekNumber(number.token, -number.getDoubleValue(), 0, Type.REAL);
			}
		}
		return null;
	}

	/**
	 * Littéral de compilation du champ statique `clazz.fieldName` (hiérarchie
	 * comprise), ou null si le champ n'est pas une constante de compilation :
	 * absent, non final, initialiseur non littéral, ou type déclaré incompatible
	 * avec le littéral (seule la promotion `real R = 5` est acceptée).
	 */
	public static Expression staticFinalLiteral(ClassDeclarationInstruction clazz, String fieldName, ClassDeclarationInstruction fromClass) {
		if (clazz == null) return null;
		var field = readableStaticField(clazz, fieldName, fromClass);
		if (field == null || !field.isFinal() || field.getExpression() == null) return null;
		var literal = directLiteral(field.getExpression().trim());
		if (literal == null) return null;
		var declared = field.getType();
		if (declared == null || declared == Type.ANY || declared == literal.getType()) return literal;
		if (declared == Type.REAL && literal instanceof LeekNumber number && number.getType() == Type.INT) {
			return new LeekNumber(number.token, number.getLongValue(), 0, Type.REAL);
		}
		return null;
	}

	/**
	 * Valeur de vérité d'une condition constante, ou null si la condition n'est
	 * pas pliable. Plie `!`, `&&`, `||` (avec la règle de court-circuit : un
	 * opérande gauche dominant plie l'ensemble même si le droit n'est pas
	 * constant, il n'aurait jamais été évalué) et la truthiness des littéraux
	 * nombre/booléen/null. Les chaînes ne sont pas pliées (conservateur).
	 */
	public static Boolean truthiness(Expression expression, ClassDeclarationInstruction fromClass) {
		if (expression == null) return null;
		var expr = expression.trim();
		if (!(expr instanceof LeekTernaire) && expr instanceof LeekExpression operation) {
			switch (operation.getOperator()) {
				case Operators.NOT: {
					var operand = truthiness(operation.getExpression2(), fromClass);
					return operand == null ? null : !operand;
				}
				case Operators.AND: {
					var left = truthiness(operation.getExpression1(), fromClass);
					if (left == null) return null;
					if (!left) return false;
					return truthiness(operation.getExpression2(), fromClass);
				}
				case Operators.OR: {
					var left = truthiness(operation.getExpression1(), fromClass);
					if (left == null) return null;
					if (left) return true;
					return truthiness(operation.getExpression2(), fromClass);
				}
			}
		}
		var literal = literal(expr, fromClass);
		if (literal instanceof LeekBoolean b) return b.getValue();
		if (literal instanceof LeekNull) return false;
		if (literal instanceof LeekNumber number) {
			return number.getType() == Type.INT ? number.getLongValue() != 0 : number.getDoubleValue() != 0;
		}
		return null;
	}

	/**
	 * Argument « pur » au sens de l'élimination d'appel : jamais évalué si l'appel
	 * disparaît, il ne doit donc porter ni effet de bord ni erreur possible.
	 * v1 conservatrice : littéraux, identifiants simples (locale, argument,
	 * globale, constante moteur) et champs statiques — pas d'accès tableau/map,
	 * pas de déréférencement d'instance, pas d'opération.
	 */
	public static boolean isPureSimpleArgument(Expression expression, ClassDeclarationInstruction fromClass) {
		var expr = expression.trim();
		if (directLiteral(expr) != null) return true;
		if (expr instanceof LeekVariable v) {
			switch (v.getVariableType()) {
				case LOCAL:
				case ARGUMENT:
				case GLOBAL:
				case SYSTEM_CONSTANT:
					return true;
				case STATIC_FIELD:
					// Lecture interdite = null + log d'erreur au runtime : pas pure
					return readableStaticField(v.getClassDeclaration(), v.getName(), fromClass) != null;
				default:
					return false;
			}
		}
		if (expr instanceof LeekObjectAccess oa) {
			return oa.getVariable() != null && oa.getVariable().getVariableType() == VariableType.STATIC_FIELD
				&& !oa.isOptional()
				&& oa.getObject().trim() instanceof LeekVariable v && v.getVariableType() == VariableType.CLASS
				&& readableStaticField(v.getClassDeclaration(), oa.getField(), fromClass) != null;
		}
		return false;
	}

	/**
	 * Condition de boucle qui peut s'émettre en CONSTANTE Java : condition pliée
	 * (truthiness) OU appel substitué par son littéral (fonction CONSTANTE, dont
	 * l'émission `(false)` typée bool serait une constante javac). Les boucles
	 * doivent alors envelopper dans bool(...) pour éviter « unreachable
	 * statement » quand le compteur d'opérations n'enveloppe pas déjà (ops
	 * désactivées : chemin CLI/embarqué).
	 */
	public static boolean isConstantEmission(Expression condition, leekscript.compiler.bloc.MainLeekBlock mainblock, ClassDeclarationInstruction fromClass) {
		if (truthiness(condition, fromClass) != null) return true;
		return condition != null && condition.trim() instanceof LeekFunctionCall call && call.isEliminable(mainblock);
	}

	/**
	 * Instruction morte après pliage : n'exécute rien et n'évalue rien à coût ou
	 * effet observable. Une chaîne if/else if/else est stockée en instructions
	 * sœurs : chacune est jugée indépendamment (une condition non pliable rend
	 * l'instruction vivante, ses effets de bord doivent rester).
	 */
	public static boolean isDeadInstruction(LeekInstruction instruction, ClassDeclarationInstruction fromClass) {
		if (instruction instanceof BlankInstruction) return true;
		if (instruction instanceof ConditionalBloc bloc) {
			if (bloc.getCondition() != null) {
				var constant = truthiness(bloc.getCondition(), fromClass);
				if (constant == null) return false;
				if (!constant) return true; // branche jamais prise, condition pliée donc pure
				return isDeadBody(bloc.getInstructions(), fromClass);
			}
			return isDeadBody(bloc.getInstructions(), fromClass);
		}
		return false;
	}

	/** Vrai si toutes les instructions du corps sont mortes après pliage. */
	public static boolean isDeadBody(List<LeekInstruction> instructions, ClassDeclarationInstruction fromClass) {
		for (var instruction : instructions) {
			if (!isDeadInstruction(instruction, fromClass)) return false;
		}
		return true;
	}

	public enum BodyKind { NONE, EMPTY, CONSTANT }

	/** Résultat de {@link #classifyBody} ; literal non-null pour CONSTANT seulement. */
	public record ClassifiedBody(BodyKind kind, Expression literal) {
		public static final ClassifiedBody NONE = new ClassifiedBody(BodyKind.NONE, null);
		public static final ClassifiedBody EMPTY = new ClassifiedBody(BodyKind.EMPTY, null);
	}

	/**
	 * Classification d'un corps de fonction globale ou de méthode statique après
	 * pliage, pour l'élimination au site d'appel :
	 * - EMPTY : rien à exécuter — instructions toutes mortes, éventuellement
	 *   terminées par un `return` nu (accepté seulement si le retour est any) ou
	 *   par une garde à sortie anticipée `if (<constant vrai>) return` ;
	 * - CONSTANT : se réduit à un unique `return <littéral>` dont la conversion
	 *   vers le type de retour est sûre ;
	 * - NONE : tout le reste.
	 * Les valeurs par défaut des paramètres sont évaluées côté callee et
	 * disparaissent avec l'appel : elles doivent être littérales. `fromClass` est
	 * le contexte d'exécution du CORPS (null pour une fonction globale, la classe
	 * pour une méthode statique) — il gouverne les champs pliables.
	 */
	public static ClassifiedBody classifyBody(List<LeekInstruction> instructions, List<Expression> defaultValues, Type returnType, ClassDeclarationInstruction fromClass) {
		for (var value : defaultValues) {
			if (value != null && literal(value, fromClass) == null) return ClassifiedBody.NONE;
		}
		LeekReturnInstruction returnInstruction = null;
		for (var instruction : instructions) {
			if (isDeadInstruction(instruction, fromClass)) continue;
			if (returnInstruction != null) return ClassifiedBody.NONE;
			if (instruction instanceof LeekReturnInstruction r && !r.isOptional()) {
				returnInstruction = r;
				continue;
			}
			// Idiome `if (!DEBUG) return` : la fonction se termine toujours là,
			// le reste du corps est inatteignable.
			if (instruction instanceof ConditionalBloc bloc) {
				var terminating = terminatingReturn(bloc, fromClass);
				if (terminating != null) {
					returnInstruction = terminating;
					break;
				}
			}
			return ClassifiedBody.NONE;
		}
		if (returnInstruction == null) return ClassifiedBody.EMPTY;
		if (returnInstruction.getExpression() == null) {
			// `return` nu = `return null` : équivalent au défaut seulement en any
			return returnType == Type.ANY ? ClassifiedBody.EMPTY : ClassifiedBody.NONE;
		}
		var literal = literal(returnInstruction.getExpression(), fromClass);
		if (literal == null) return ClassifiedBody.NONE;
		// La conversion du littéral vers le type de retour peut lever (toArray…) :
		// refuser si elle n'est pas sûre (l'élimination la sauterait).
		if (returnType != Type.ANY && returnType.accepts(literal.getType()).ordinal() > Type.CastType.UPCAST.ordinal()) {
			return ClassifiedBody.NONE;
		}
		return new ClassifiedBody(BodyKind.CONSTANT, literal);
	}

	/**
	 * Garde à sortie anticipée : le bloc est un `if` (tête de chaîne) à condition
	 * constante VRAIE (donc pure) dont le corps se réduit, après pliage, à un
	 * unique `return` nu ou littéral — idiome `if (!DEBUG) return`. La fonction se
	 * termine alors toujours sur ce return : la suite (branches else comprises)
	 * est inatteignable. Renvoie ce return, ou null si le motif ne correspond pas.
	 */
	public static LeekReturnInstruction terminatingReturn(ConditionalBloc bloc, ClassDeclarationInstruction fromClass) {
		if (bloc.getParentCondition() != null || bloc.getCondition() == null) return null;
		var constant = truthiness(bloc.getCondition(), fromClass);
		if (constant == null || !constant) return null;
		LeekReturnInstruction result = null;
		for (var instruction : bloc.getInstructions()) {
			if (isDeadInstruction(instruction, fromClass)) continue;
			if (result == null && instruction instanceof LeekReturnInstruction r && !r.isOptional()) {
				result = r;
			} else {
				return null;
			}
		}
		return result;
	}
}
