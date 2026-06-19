package leekscript.compiler.expression;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.NarrowingInfo;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.ClassMethodBlock;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction.ClassDeclarationMethod;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;
import leekscript.runner.classes.SystemClass;
import java.util.Map;

import leekscript.common.AccessLevel;
import leekscript.common.ArrayType;
import leekscript.common.ClassType;
import leekscript.common.ClassValueType;
import leekscript.common.ParameterizedClassType;
import leekscript.common.CompoundType;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.common.IntervalType;
import leekscript.common.LanguageFeature;
import leekscript.common.MapType;
import leekscript.common.SetType;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.common.TypeVariable;

public class LeekFunctionCall extends Expression {

	private Token openParenthesis = null;
	private Token closingParenthesis = null;
	private final ArrayList<Expression> mParameters = new ArrayList<Expression>();
	private Expression mExpression = null;
	private Type type = Type.ANY;
	private boolean unsafe = false;
	private ArrayList<CallableVersion> callable_versions = null;
	private LeekFunctions system_function = null;
	private boolean is_method = false;
	private boolean is_static_method = false;
	// Appel de méthode sur un type natif : `array.sort()`, `array.map(f)` (v5, OO
	// stdlib). Résolu comme `sort(array)` mais codegen `((ArrayLeekValue) array).sort(ai)`.
	private boolean is_builtin_method = false;
	// Appel de fonction de namespace built-in : `Math.sqrt(x)`, `Math.rand()` (v5,
	// stdlib objet). Résolu et généré comme l'ancienne forme libre `sqrt(x)`, juste
	// qualifié par le namespace ; réutilise tout le chemin system_function.
	private boolean is_namespace_function = false;
	// Un cast `((Type) …)` de retour effacé générique a été ouvert sur le callee : il
	// faut le refermer après la parenthèse finale de l'appel (précédence Java : sinon
	// `(Type) x.m()` se lit `(Type)(x.m())` et casse un accès chaîné `x.m().n()`).
	private boolean wrapReturnCast = false;
	private ClassDeclarationMethod method;
	private ClassMethodBlock constructorBlock = null;
	private Type functionType = Type.ANY;
	private FunctionBlock resolvedFunction = null;

	// Méthodes natives exposées en `u_` sur la classe de base des objets générés
	// (NativeObjectLeekValue), ex: keys(). Un `super.method()` vers l'une d'elles
	// compile correctement en `super.u_method()` même sans déclaration utilisateur,
	// donc l'analyse ne doit pas la rejeter. Clés sous la forme "nom_arité".
	private static final java.util.Set<String> NATIVE_OBJECT_METHODS = computeNativeObjectMethods();
	private static java.util.Set<String> computeNativeObjectMethods() {
		var set = new java.util.HashSet<String>();
		for (var m : leekscript.runner.AI.NativeObjectLeekValue.class.getMethods()) {
			if (m.getName().startsWith("u_")) {
				set.add(m.getName().substring(2) + "_" + m.getParameterCount());
			}
		}
		return set;
	}

	// Natifs qui ne font que lire la map passée en premier argument (cf preAnalyze)
	private static final java.util.Set<String> MAP_READERS = java.util.Set.of("mapContainsKey", "mapContains", "mapGet",
		"mapSize", "mapIsEmpty", "mapKeys", "mapKeySet", "mapValues", "mapSearch", "mapMin", "mapMax", "mapSum", "mapAverage", "count");

	public LeekFunctionCall(Token openParenthesis) {
		this.openParenthesis = openParenthesis;
		this.openParenthesis.setExpression(this);
	}

	public void setExpression(Expression expression) {
		mExpression = expression;
	}

	public void setClosingParenthesis(Token closingParenthesis) {
		this.closingParenthesis = closingParenthesis;
		this.closingParenthesis.setExpression(this);
	}

	public void addParameter(Expression param) {
		mParameters.add(param);
	}

	public FunctionBlock getResolvedFunction() {
		return resolvedFunction;
	}

	@Override
	public int getNature() {
		return FUNCTION;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Hover hover(Token token) {
		if (constructorBlock != null) {
			return new Hover(type, token.getLocation(), constructorBlock.getLocation());
		}
		return super.hover(token);
	}

	public boolean isConstructorCall() {
		return constructorBlock != null;
	}

	public int getParameterCount() {
		return mParameters.size();
	}

	public Expression getParameter(int index) {
		return mParameters.get(index);
	}

	public Expression getCallExpression() {
		return mExpression;
	}

	/** Nom de la fonction native appelée (connu dès le preAnalyze), ou null. */
	public String systemFunctionName() {
		return mExpression instanceof LeekVariable function && function.getVariableType() == VariableType.SYSTEM_FUNCTION ? function.getName() : null;
	}

	@Override
	public String toString() {
		String str = mExpression.toString() + "(";
		for(int i = 0; i < mParameters.size(); i++){
			if(i > 0) str += ", ";
			str += mParameters.get(i).toString();
		}
		return str + ")";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if(mExpression == null || !mExpression.validExpression(compiler, mainblock)) return false;

		//Vérification de chaque paramètre
		for(Expression parameter : mParameters){
			parameter.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (mainblock.getVersion() == 1 && type == Type.ANY) {
			writer.addCode("load(");
			compileL(mainblock, writer, false);
			writer.addCode(")");
		} else {
			compileL(mainblock, writer, parenthesis);
		}
	}

	/**
	 * Appel substituable (couche 3 du pliage de constantes, cf ConstantFolder) :
	 * fonction utilisateur globale résolue statiquement, non redéfinie, dont le
	 * corps est VIDE ou CONSTANT après pliage, et dont tous les arguments sont
	 * purs (ils ne seront jamais évalués puisque l'appel disparaît). Le coût de
	 * l'appel (1 op côté callee) disparaît avec lui. Dispatch statique seulement :
	 * les méthodes (virtuelles) et les valeurs de fonction ne sont pas touchées.
	 */
	public boolean isEliminable(MainLeekBlock mainblock) {
		if (mainblock.getVersion() < 2 || resolvedFunction == null) return false;
		if (!(mExpression instanceof LeekVariable v) || v.getVariableType() != VariableType.FUNCTION) return false;
		if (mainblock.isRedefinedFunction(v.getName())) return false;
		if (resolvedFunction.getBodyClassification(mainblock).kind() == ConstantFolder.BodyKind.NONE) return false;
		return hasEliminableArguments(mainblock);
	}

	/**
	 * Élimination en POSITION D'INSTRUCTION seulement : en plus des fonctions
	 * globales, les appels de méthodes STATIQUES résolues (dispatch figé à la
	 * compilation, `private final` en Java — pas de virtualité) dont le corps est
	 * VIDE ou CONSTANT. Restreint aux instructions : en expression, l'op d'analyse
	 * du call (+1 pour Class.m()) resterait facturée par l'instruction englobante
	 * sans être émise — en instruction, tout disparaît, comptage cohérent (0 op).
	 */
	public boolean isEliminableStatement(MainLeekBlock mainblock) {
		if (isEliminable(mainblock)) return true;
		if (mainblock.getVersion() < 2 || !is_static_method || method == null || method.block == null) return false;
		if (method.block.getBodyClassification(mainblock).kind() == ConstantFolder.BodyKind.NONE) return false;
		return hasEliminableArguments(mainblock);
	}

	private boolean hasEliminableArguments(MainLeekBlock mainblock) {
		var fromClass = mainblock.getWordCompiler().getCurrentClass();
		for (int i = 0; i < mParameters.size(); i++) {
			var parameter = mParameters.get(i);
			if (!ConstantFolder.isPureSimpleArgument(parameter, fromClass)) return false;
			// Un argument pur mais à coût non nul (moins unaire, `-(5)`) serait facturé sans
			// être émis dans certains contextes : l'appel éliminé doit être 0 op partout.
			if (parameter.getOperations() != 0) return false;
			// La conversion vers le type déclaré du paramètre est émise côté appelant
			// et peut lever (toArray/toMap…) : ne l'éliminer que si elle est sûre.
			var target = functionType.getArgument(mParameters.size(), i);
			if (target != Type.ANY && target.accepts(parameter.getType()).ordinal() > Type.CastType.UPCAST.ordinal()) return false;
		}
		return true;
	}

	/**
	 * Appel direct de getSourceLine / getSourceFile (cf LeekFunctions.setCallSite) : il s'écrit comme un
	 * littéral. Null pour tout autre appel.
	 */
	private LeekFunctions.CallSite callSite(MainLeekBlock mainblock) {
		// Le cas courant d'abord : la recherche des redéfinitions ne coûte que pour getSource*
		if (system_function == null || system_function.getCallSite() == null) return null;
		var name = systemFunctionName();
		if (name == null || mainblock.isRedefinedFunction(name)) return null;
		return system_function.getCallSite();
	}

	/** Aucun argument dont l'évaluation se voit (cf ConstantFolder.isHarmlessValue). */
	private boolean hasHarmlessArguments(MainLeekBlock mainblock) {
		var fromClass = mainblock.getWordCompiler().getCurrentClass();
		for (var parameter : mParameters) {
			if (!ConstantFolder.isHarmlessValue(parameter, fromClass)) return false;
		}
		return true;
	}

	/**
	 * Valeur émise à la place d'un appel éliminé dont le résultat est utilisé :
	 * le littéral du `return` (converti vers le type de retour déclaré) pour une
	 * fonction CONSTANTE, la valeur par défaut du type de retour (celle du
	 * `return` implicite du callee) pour une fonction VIDE.
	 */
	private void writeSubstitutedValue(MainLeekBlock mainblock, JavaWriter writer) {
		var classification = resolvedFunction.getBodyClassification(mainblock);
		if (classification.kind() == ConstantFolder.BodyKind.CONSTANT) {
			writer.addCode("(");
			writer.compileConvert(mainblock, 0, classification.literal(), this.type, false);
			writer.addCode(")");
		} else {
			writer.addCode(this.type.getDefaultValue(writer, mainblock.getVersion()));
		}
	}

	/**
	 * La classe appelée (`new A()`) est-elle nommée, donc son constructeur Java `new_<classe>`
	 * connu à la compilation ? `class` aussi dans une méthode statique, écrit comme la classe
	 * englobante (cf LeekVariable.writeThisClass).
	 */
	private boolean isStaticClassReference(JavaWriter writer) {
		if (!(mExpression.trim() instanceof LeekVariable v)) return false;
		if (v.getVariableType() == VariableType.CLASS) return true;
		return v.getVariableType() == VariableType.THIS_CLASS && !writer.inInstanceContext();
	}

	/**
	 * Type du Java émis pour cet appel : celui du littéral substitué à un appel éliminé vers une
	 * fonction sans type de retour (cf writeSubstitutedValue), que getType() annonce any.
	 */
	public Type getWrittenType(MainLeekBlock mainblock) {
		if (this.type != Type.ANY || !isEliminable(mainblock)) return this.type;
		var classification = resolvedFunction.getBodyClassification(mainblock);
		return classification.kind() == ConstantFolder.BodyKind.CONSTANT ? classification.literal().getType() : Type.NULL;
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (isEliminable(mainblock)) {
			writeSubstitutedValue(mainblock, writer);
			return;
		}
		var callSite = callSite(mainblock);
		if (callSite != null) {
			// Ligne ou chemin du fichier qui contient l'appel, include compris
			var location = ((LeekVariable) mExpression).getToken().getLocation();
			if (callSite == LeekFunctions.CallSite.LINE) {
				writer.addCode(location.getStartLine() + "l");
			} else {
				var path = SystemClass.sourcePath(location.getFile().getPath());
				writer.addCode(LeekFileContent.javaString(path));
			}
			return;
		}
		boolean addComma = true;
		boolean addBrace = false;
		boolean skipFirstArg = false;
		FunctionBlock user_function = null;
		boolean convertPrimitive = false;
		// Un cast en préfixe (`(u_Entity) execute(...)`) a une priorité plus faible que
		// l'accès membre : si l'appelant demande des parenthèses (typiquement parce que
		// l'appel est le receveur d'un `.u_method()`), il faut envelopper cast compris,
		// sinon Java lit `(u_Entity) (execute(...).u_method())` et le receveur reste
		// Object → « cannot find symbol » à la compilation Java.
		boolean castParenthesis = false;
		// Appel Java direct d'une fonction ou méthode utilisateur : ses paramètres ont le type déclaré
		boolean typedArguments = false;
		// Appel optionnel `obj?.m(...)` émis en `(obj instanceof Object $o ? callObjectAccess($o, ...)`, à refermer
		boolean closeOptionalCall = false;

		if (mExpression instanceof LeekObjectAccess) {
			// Object access : object.field()
			var object = ((LeekObjectAccess) mExpression).getObject();
			var field = ((LeekObjectAccess) mExpression).getField();

			if (((LeekObjectAccess) mExpression).isOptional()) {
				// Appel de méthode optionnel `obj?.method()` : null si l'objet est null,
				// sinon appel dynamique. L'objet n'est évalué qu'une fois (en argument).
				// Un objet de type primitif n'est jamais null : rien à court-circuiter, Java inchangé.
				if (object.getType().isPrimitive() || hasHarmlessArguments(mainblock)) {
					writer.addCode("callObjectAccessNullSafe(");
					object.writeJavaCode(mainblock, writer, false);
				} else {
					// Les arguments ne sont évalués que si l'objet n'est pas null (#5300). L'objet est
					// lié par le motif, sous un nom unique : un argument peut contenir un autre `?.`,
					// et `$` n'entre dans aucun nom LeekScript, donc dans aucun champ qu'il masquerait.
					var receiver = "$o" + mainblock.getCount();
					writer.addCode("(");
					object.writeJavaCode(mainblock, writer, true);
					writer.addCode(" instanceof Object " + receiver + " ? callObjectAccess(" + receiver);
					closeOptionalCall = true;
				}
				writer.addCode(", \"" + field + "\", \"u_" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());

			} else if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				// super.field()
				// Super qualifié (u_Class.super) : dans une fonction anonyme, le code est
				// généré dans une classe interne Java (FunctionLeekValue) où le `super` nu
				// désignerait la classe de base de la closure et non le parent de la classe
				// utilisateur (issue #4070).
				writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".super.u_" + field + "(");
				addComma = false;
				typedArguments = true;

			} else if (object instanceof LeekVariable v && v.getVariableType() == VariableType.CLASS) {
				if (is_namespace_function) {
					// Math.sqrt(x) -> NumberClass.sqrt(mUAI, x) : même émission que la forme libre.
					var state = writeSystemFunctionCallee(mainblock, writer);
					addComma = state[0];
					skipFirstArg = state[1];
				} else {
				// Class.method() : Méthode statique connue
				var method = v.getClassDeclaration().getStaticMethod(field, mParameters.size());
				if (method != null) {
					// Méthode statique
					String methodName = "u_" + v.getClassDeclaration().getStaticMethodName(field, mParameters.size());
					writer.addCode(methodName + "(");
					addComma = false;
					typedArguments = true;
				} else {
					// Champ statique
					// writer.addCode("execute(" + v.getClassDeclaration().getName() + "." + field);
					if (type != Type.ANY) {
						if (parenthesis) { writer.addCode("("); castParenthesis = true; }
						writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
					}
					writer.addCode("u_" + v.getClassDeclaration().getInternalName() + ".callStaticField(\"" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
				}
				}
			} else if (object instanceof LeekVariable v && v.getVariableType() == VariableType.THIS) {
				var method = mainblock.getWordCompiler().getCurrentClass().getMethodName(field, mParameters.size());
				if (method != null) {
					// this.method() : Méthode connue
					// writer.addCode("callObjectAccess(u_this, \"" + field + "\", \"" + field + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
					writer.addCode("u_" + field + "(");
					addComma = false;
					typedArguments = true;
				} else if (this.functionType instanceof FunctionType ft) {
					// run() retourne Object : on caste vers le wrapper avant .doubleValue()/etc.
					writeConvertPrimitiveCast(writer);
					writer.addCode("this." + field);
					writer.addCode(".run(");
					writer.addCode(writer.getAIThis());
					writer.addCode(", null");
					convertPrimitive = true;
				} else if (((LeekObjectAccess) mExpression).getVariable() != null) {
					// Declared class field: exists as a real Java field on the inner class
					writer.addCode("execute(this." + field);
				} else {
					// Dynamic field: look it up on the ObjectLeekValue backing map
					writer.addCode("callObjectAccess(" + mainblock.getWordCompiler().getCurrentClassVariable() + ".this, \"" + field + "\", \"u_" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
				}
			} else if (is_static_method) {
				writer.addCode("u_" + method.block.getClassDeclaration().getInternalName() + "_" + field + "_" + mParameters.size() + "(");
				addComma = false;
				typedArguments = true;
			} else if (is_method) {
				// Méthode d'une classe générique instanciée (receveur `Box<integer>`) : la
				// méthode Java est effacée (retour `Object`). On caste vers le type de retour
				// concret inféré, sinon un `ops((Object)…, n)` affecté à un primitif casse au
				// javac (« inference variable T has incompatible bounds »).
				if (object.getType() instanceof ParameterizedClassType
						&& functionType instanceof FunctionType ft && ft.returnType().containsTypeVariable()
						&& this.type != Type.ANY && this.type != Type.VOID) {
					openGenericReturnCast(mainblock, writer);
				}
				object.writeJavaCode(mainblock, writer, true);
				writer.addCode(".u_" + field + "(");
				addComma = false;
				typedArguments = true;
			} else if (is_builtin_method) {
				// Le receveur est l'objet ; les arguments restent dans mParameters.
				// Retour effacé d'une méthode générique (ex. array.max() -> Object) :
				// caste vers le type inféré.
				if (systemReturnContainsTypeVariable() && this.type != Type.ANY && this.type != Type.VOID) {
					openGenericReturnCast(mainblock, writer);
				}
				if (system_function.isStatic()) {
					// "hello".length() -> StringClass.length(ai, "hello", ...) : méthode
					// statique du helper, le receveur est le premier argument après l'AI.
					writer.addCode(system_function.getStandardClass() + "Class." + system_function.getJavaName() + "(" + writer.getAIThis() + ", ");
					object.writeJavaCode(mainblock, writer, false);
					addComma = true;
				} else {
					// array.sort() -> ((ArrayLeekValue) array).sort(ai, ...). getJavaName() :
					// le nom leekscript (ex. `map`) peut différer de la méthode Java (`arrayMap`).
					writer.addCode("((" + object.getType().getJavaName(mainblock.getVersion()) + ") ");
					object.writeJavaCode(mainblock, writer, true);
					writer.addCode(")." + system_function.getJavaName() + "(" + writer.getAIThis());
					addComma = true;
				}
			} else {
				// object.field() : Méthode ou bien appel d'un champ
				if (type != Type.ANY) {
					if (parenthesis) { writer.addCode("("); castParenthesis = true; }
					writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				writer.addCode("callObjectAccess(");
				object.writeJavaCode(mainblock, writer, false);
				writer.addCode(", \"" + field + "\", \"u_" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			}
		} else if (mExpression instanceof LeekArrayAccess) {
			var object = ((LeekArrayAccess) mExpression).getTabular();
			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {

				writer.addCode("u_this.callSuperMethod(mUAI, " + mainblock.getWordCompiler().getCurrentClassVariable() + ", ");
				((LeekArrayAccess) mExpression).getCase().writeJavaCode(mainblock, writer, true);
				writer.addCode(".getString(mUAI) + \"_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			} else {
				writer.addCode("LeekValueManager.executeArrayAccess(" + writer.getAIThis() + ", ");
				object.writeJavaCode(mainblock, writer, false);
				writer.addCode(", ");
				((LeekArrayAccess) mExpression).getCase().writeJavaCode(mainblock, writer, false);
				writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable());
			}
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SUPER) {
			// Super constructor
			// var variable = (LeekVariable) mExpression;
			// writer.addCode("u_" + variable.getClassDeclaration().getParent().getName());
			// writer.addCode(".callConstructor(u_this");

			// Qualifié pour la même raison que super.method() : valable aussi depuis
			// une fonction anonyme (classe interne Java) du constructeur (issue #4070).
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".super.init(");
			addComma = false;
			typedArguments = true;

		} else if (mExpression instanceof LeekVariable v && v.getVariableType() == VariableType.METHOD) {
			// Méthode connue
			// String methodName = "u_" + mainblock.getWordCompiler().getCurrentClass().getMethodName(((LeekVariable) mExpression).getName(), mParameters.size());
			// writer.addCode(methodName + "(u_this");
			writer.addCode("u_" + v.getName() + "(");
			typedArguments = true;
			// writer.addCode("callMethod(this, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			addComma = false;

		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.STATIC_METHOD) {
			// Méthode statique connue
			if (this.type != Type.ANY && this.type != Type.VOID) {
				writer.addCode("(" + this.type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
			}
			String methodName = "u_" + mainblock.getWordCompiler().getCurrentClass().getStaticMethodName(((LeekVariable) mExpression).getName(), mParameters.size());
			writer.addCode(methodName + "(");
			addComma = false;
			typedArguments = true;
		} else if (mExpression instanceof LeekVariable && mainblock.isRedefinedFunction(((LeekVariable) mExpression).getName())) {
			writer.addCode("rfunction_" + ((LeekVariable) mExpression).getName());
			writer.addCode(".execute(");
			addComma = false;
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SYSTEM_FUNCTION) {
			var state = writeSystemFunctionCallee(mainblock, writer);
			addComma = state[0];
			skipFirstArg = state[1];

		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.FUNCTION) {
			// Bloc résolu en preAnalyze (module-aware) ; fallback main-context.
			user_function = ((LeekVariable) mExpression).getResolvedFunction(mainblock);
			// Fonction générique : la méthode Java est effacée (retour `Object`). On
			// caste vers le type de retour concret inféré pour que le code appelant
			// reste bien typé (l'affectation déballe le wrapper si primitif).
			if (user_function != null && user_function.getType().returnType().containsTypeVariable()
					&& this.type != Type.ANY && this.type != Type.VOID) {
				openGenericReturnCast(mainblock, writer);
			}
			writer.addCode("f_");
			writer.addCode(user_function != null ? user_function.getInternalName() : ((LeekVariable) mExpression).getName());
			writer.addCode("(");
			addComma = false;
			typedArguments = true;
		} else if (mExpression.getType() instanceof ClassValueType cvt && cvt.getClassDeclaration() != null) {
			if (isStaticClassReference(writer)) {
				writer.addCode("new_");
				mExpression.trim().writeJavaCode(mainblock, writer, false);
				writer.addCode("(");
				addComma = false;
			} else {
				// Classe portée par une valeur (variable, élément, `class` d'une méthode d'instance) :
				// construite au runtime comme hors strict, `new_u_x()` n'existant pas
				writeConvertPrimitiveCast(writer);
				convertPrimitive = true;
				castParenthesis = writeDynamicExecute(mainblock, writer, parenthesis);
			}
		} else if (mExpression instanceof LeekVariable v && v.getType() instanceof ClassValueType) {
			// Variable de type `Class`, classe inconnue à la compilation
			castParenthesis = writeDynamicExecute(mainblock, writer, parenthesis);
		} else if (this.functionType instanceof FunctionType) {
			// run() retourne Object : on caste vers le wrapper avant .doubleValue()/etc.
			writeConvertPrimitiveCast(writer);
			mExpression.writeJavaCode(mainblock, writer, true);
			writer.addCode(".run(");
			writer.addCode(writer.getAIThis());
			writer.addCode(", null");
			convertPrimitive = true;
		} else {
			// Appel dynamique aussi, par exemple un ternaire de références de fonctions
			// `cond ? f1 : f2`, dont le type est un CompoundType non géré par la branche
			// FunctionType ci-dessus.
			castParenthesis = writeDynamicExecute(mainblock, writer, parenthesis);
		}

		int argCount = mParameters.size();
		if (system_function != null && !is_builtin_method) {
			// Pour une méthode native, le receveur est déjà émis (objet), pas un
			// paramètre : argCount reste mParameters.size().
			argCount = callable_versions != null ? callable_versions.get(0).arguments.length : system_function.getVersions()[0].arguments.length;
		}
		for (int i = 0; i < argCount; i++) {
			if (i == 0 && skipFirstArg) continue;
			if (i > 0 || addComma) writer.addCode(", ");
			if (i < mParameters.size()) {
				var parameter = mParameters.get(i);
				// Java doesn't like a single null for Object... argument
				if (argCount == 1 && parameter.getType() == Type.NULL && user_function == null && system_function == null && !unsafe && !is_method && !is_static_method) {
					writer.addCode("new Object[] { null }");
					continue;
				}
				// System.out.println("Argument : " + functionType.getArgument(mParameters.size(), i));
				if (mainblock.getVersion() >= 2) {
					if (system_function != null) {
						parameter.writeJavaCode(mainblock, writer, false);
					} else if (typedArguments) {
						writer.compileConvertTyped(mainblock, i, parameter, functionType.getArgument(mParameters.size(), i), false);
					} else {
						writer.compileConvert(mainblock, i, parameter, functionType.getArgument(mParameters.size(), i), false);
					}
				} else {
					if (user_function != null) {
						// Paramètre typé (ex. `Array x`) : la signature Java générée utilise le
						// type concret, donc un argument ANY (`var` -> Box<Object>) doit être
						// converti comme en V2+, sinon « incompatible types » à la compilation
						// Java (#2743). Les paramètres non typés gardent le chargement L-value.
						var argType = functionType.getArgument(mParameters.size(), i);
						if (argType != Type.ANY) {
							writer.compileConvertTyped(mainblock, i, parameter, argType, false);
						} else {
							parameter.compileL(mainblock, writer, false);
						}
					} else if (system_function != null) {
						if (unsafe) {
							writer.compileLoad(mainblock, parameter, false);
						} else {
							writer.compileConvert(mainblock, i, parameter, functionType.getArgument(mParameters.size(), i), false);
						}
					} else {
						parameter.compileL(mainblock, writer, false);
					}
				}
			} else {
				// Java doesn't like a single null for Object... argument
				if (argCount == 1 && system_function == null && !unsafe && !is_method && !is_static_method) {
					writer.addCode("new Object[] { null }");
				} else {
					writer.addCode("null");
				}
			}
		}
		if (addBrace) {
			writer.addCode("}");
		}
		writer.addCode(")");
		if (closeOptionalCall) {
			writer.addCode(" : null)");
		}
		if (wrapReturnCast) {
			// Ferme le cast `((Type) <appel>)` ouvert sur le callee (retour effacé générique).
			writer.addCode(")");
		}
		if (convertPrimitive) {
			// Ferme le cast ouvert par writeConvertPrimitiveCast() : ((Number) X.run(...)).doubleValue()
			if (this.type == Type.INT) {
				writer.addCode(").longValue()");
			} else if (this.type == Type.REAL) {
				writer.addCode(").doubleValue()");
			} else if (this.type == Type.BOOL) {
				writer.addCode(").booleanValue()");
			}
		}
		if (castParenthesis) {
			writer.addCode(")");
		}
		writer.addPosition(openParenthesis);
	}

	/**
	 * Appel dynamique `execute(f, …)`, qui rend un Object : casté vers le type de retour concret
	 * quand il est connu, pour que le Java respecte le type annoncé par getType() (sinon
	 * `Object cannot be converted to SetLeekValue`). Ouvre `execute(` et écrit la fonction
	 * appelée ; rend true si une parenthèse englobant le cast est à refermer (cf castParenthesis).
	 */
	private boolean writeDynamicExecute(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		boolean castParenthesis = false;
		if (this.type != Type.ANY && this.type != Type.VOID && !this.type.isPrimitive()) {
			if (parenthesis) { writer.addCode("("); castParenthesis = true; }
			writer.addCode("(" + this.type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
		}
		writer.addCode("execute(");
		mExpression.writeJavaCode(mainblock, writer, false);
		return castParenthesis;
	}

	/**
	 * Émet le callee d'un appel de fonction système et renvoie l'état
	 * {addComma, skipFirstArg} pour la boucle d'arguments. Trois formes : wrapper
	 * générique (v<=3 ou appel unsafe), helper statique `NumberClass.sqrt(mUAI`,
	 * ou méthode d'instance `((ArrayLeekValue) x).sort(mUAI`. Partagé par l'appel
	 * libre `sqrt(x)` et l'appel qualifié de namespace `Math.sqrt(x)` (v5).
	 */
	/**
	 * Ouvre un cast `((Type) …)` du retour effacé d'un appel générique vers le type
	 * concret inféré (`this.type`). Le cast est refermé après la parenthèse finale de
	 * l'appel via {@link #wrapReturnCast} (précédence Java : un cast bare `(Type) x.m()`
	 * se lirait `(Type)(x.m())` et casserait un accès chaîné `x.m().n()`). Les gardes
	 * « le retour déclaré contient une variable de type » diffèrent par site d'appel et
	 * restent donc chez l'appelant.
	 */
	private void openGenericReturnCast(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("((" + this.type.getJavaName(mainblock.getVersion()) + ") ");
		wrapReturnCast = true;
	}

	private boolean[] writeSystemFunctionCallee(MainLeekBlock mainblock, JavaWriter writer) {
		boolean addComma = true;
		boolean skipFirstArg = false;
		// Fonction système générique dont le retour a été inféré vers un type concret :
		// la méthode Java est effacée (Object), on caste vers le type inféré.
		if (systemReturnContainsTypeVariable() && this.type != Type.ANY && this.type != Type.VOID) {
			openGenericReturnCast(mainblock, writer);
		}
		if (unsafe || mainblock.getVersion() <= 3) {
			if (callable_versions != null) {
				var key = writer.generateGenericFunction(callable_versions);
				writer.addCode(system_function.getStandardClass() + "_" + key + "(");
				addComma = false;
			} else {
				var list = new ArrayList<CallableVersion>();
				list.add(system_function.getVersions()[0]);
				writer.generateGenericFunction(list);
				writer.addCode(system_function.getStandardClass() + "_" + system_function.getName() + "_" + system_function.getVersions()[0].getParametersSignature() + "(");
				addComma = false;
			}
		} else if (system_function.isStatic()) {
			writer.addCode(system_function.getStandardClass() + "Class." + system_function.getJavaName() + "(" + writer.getAIThis());
		} else {
			if (mParameters.get(0).getType() != Type.ANY) {
				writer.addCode("((" + mParameters.get(0).getType().getJavaName(mainblock.getVersion()) + ") ");
			}
			mParameters.get(0).writeJavaCode(mainblock, writer, true);
			if (mParameters.get(0).getType() != Type.ANY) {
				writer.addCode(")");
			}
			writer.addCode("." + system_function.getJavaName() + "(" + writer.getAIThis());
			skipFirstArg = true;
		}
		return new boolean[] { addComma, skipFirstArg };
	}

	/**
	 * Le résultat d'un appel de valeur fonction (`FunctionLeekValue.run(...)`) est
	 * typé `Object`. Quand le type de retour inféré est primitif, on doit caster
	 * vers le wrapper Java correspondant avant d'appeler `.doubleValue()` /
	 * `.longValue()` / `.booleanValue()` (sinon `Object` n'a pas ces méthodes).
	 * Émet le préfixe `((Number) ` / `((Boolean) ` ; le suffixe `)` est écrit par
	 * le bloc convertPrimitive. No-op si le type n'est pas primitif.
	 */
	private void writeConvertPrimitiveCast(JavaWriter writer) {
		if (this.type == Type.INT || this.type == Type.REAL) {
			writer.addCode("((Number) ");
		} else if (this.type == Type.BOOL) {
			writer.addCode("((Boolean) ");
		}
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		// #2861 : cet accès est appelé, donc un nom partagé champ/méthode doit viser la
		// méthode (et non le champ, que getMember privilégierait par défaut).
		if (mExpression instanceof LeekObjectAccess oa) {
			oa.setCalledAsMethod(true);
		}
		if (mExpression instanceof LeekVariable v) v.setCalled();
		mExpression.preAnalyze(compiler);
		for (Expression parameter : mParameters) {
			parameter.preAnalyze(compiler);
		}
		if (!mParameters.isEmpty()) {
			var name = systemFunctionName();
			if (name != null && MAP_READERS.contains(name)) LeekVariable.addEntryPreservingUse(mParameters.get(0));
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {

		// System.out.println("[FC] analyze " + this);

		operations = 0;

		// #2861 : cf. preAnalyze — l'accès appelé vise la méthode homonyme, pas le champ.
		if (mExpression instanceof LeekObjectAccess oa) {
			oa.setCalledAsMethod(true);
		}
		mExpression.analyze(compiler);
		operations += mExpression.getOperations();

		// L'expression est appelable ? Cache isStrict + getType — chacun était
		// appelé 2× dans le chemin (et appel de fonction = hot path).
		var exprType = mExpression.getType();
		boolean strict = compiler.getMainBlock().isStrict();
		// Appel optionnel `obj?.method()` : l'accès optionnel a un type statique ANY
		// (résultat nullable) et passe par le chemin dynamique null-safe
		// (callObjectAccessNullSafe). On ne vérifie donc pas la callabilité, sinon faux
		// positif "pourrait ne pas être appelable" en mode strict (#4204).
		boolean optionalAccess = mExpression instanceof LeekObjectAccess oa && oa.isOptional();
		if (!optionalAccess) {
			if (strict && !exprType.isCallable()) {
				if (!mExpression.getWarningType().isCallable()) {
					compiler.addError(new AnalyzeError(mExpression.getLocation(), AnalyzeErrorLevel.WARNING, Error.MAY_NOT_BE_CALLABLE, new String[] {
						mExpression.toString(),
						exprType.toString()
					} ));
				}
			}
			else if (!exprType.canBeCallable()) {
				var level = strict ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				compiler.addError(new AnalyzeError(mExpression.getLocation(), level, Error.NOT_CALLABLE, new String[] {
					mExpression.toString(),
					exprType.toString()
				} ));
			}
		}

		this.functionType = mExpression.getType();
		this.type = functionType.returnType();
		// System.out.println("[FC] function type = " + functionType + " args = " + functionType.getArguments() + " return = " + functionType.returnType());

		for (Expression parameter : mParameters) {
			parameter.analyze(compiler);
			operations += parameter.getOperations();
		}

		if (mExpression instanceof LeekVariable) {
			var v = (LeekVariable) mExpression;

			if (v.getVariableType() == VariableType.METHOD) { // La variable est analysée comme une méthode, mais ça peut être une fonction système,

				// on regarde si le nombre d'arguments est correct
				var current = compiler.getCurrentClass();
				boolean ok = false;
				end:
				while (current != null) {
					var methods = current.getMethod(v.getName());
					if (methods != null) {
						for (var count : methods.keySet()) {
							if (count == mParameters.size()) {
								ok = true;
								is_method = true;
								var m = methods.get(count);
								if (m.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != current) {
									compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_METHOD, new String[] { current.getName(), v.getName() }));
								}
								functionType = m.block.getType();
								break end;
							}
						}
					}
					current = current.getParent();
				}
				if (!ok) {
					// Est-ce que c'est une fonction système ?
					system_function = LeekFunctions.getValue(v.getName(), compiler.getOptions().useExtra());
					if (system_function != null) {
						verifySystemFunction(compiler, v.getToken(), system_function);

						if (mParameters.size() >= system_function.getArgumentsMin() && mParameters.size() <= system_function.getArguments()) {
							v.setVariableType(VariableType.SYSTEM_FUNCTION);
							return; // OK, fonction système
						}
					}
					// Sinon, erreur de méthode
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
				}

			} else if (v.getVariableType() == VariableType.STATIC_METHOD) {

				// on regarde si le nombre d'arguments est correct
				var current = compiler.getCurrentClass();
				boolean ok = false;
				end:
				while (current != null) {
					var methods = current.getStaticMethod(v.getName());
					if (methods != null) {
						for (var entry : methods.entrySet()) {
							if (entry.getKey() == mParameters.size()) {
								ok = true;
								is_static_method = true;
								method = entry.getValue();
								if (method.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != current) {
									compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { current.getName(), v.getName() }));
								}
								functionType = method.block.getType();
								break end;
							}
						}
					}
					current = current.getParent();
				}
				if (!ok) {
					// Est-ce que c'est une fonction système ?
					system_function = LeekFunctions.getValue(v.getName(), compiler.getOptions().useExtra());
					if (system_function != null) {
						verifySystemFunction(compiler, v.getToken(), system_function);
						if (mParameters.size() >= system_function.getArgumentsMin() && mParameters.size() <= system_function.getArguments()) {
							v.setVariableType(VariableType.SYSTEM_FUNCTION);
							return; // OK, fonction système
						}
					}
					// Sinon, erreur de méthode
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
				}

			} else if (v.getVariableType() == VariableType.FUNCTION) {

				var f = v.getFunctionDeclaration() != null ? v.getFunctionDeclaration() : compiler.getMainBlock().getUserFunction(v.getName());
				if (f != null) {
					resolvedFunction = f;
					if (mParameters.size() < f.getMinParameters() || mParameters.size() > f.getMaxParameters()) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					}
					verifyVersions(compiler, f.getVersions());
				}

			} else if (v.getVariableType() == VariableType.SYSTEM_FUNCTION) {

				if (compiler.getMainBlock().isRedefinedFunction(v.getName())) {
					// Nothing
				} else {
					system_function = LeekFunctions.getValue(v.getName(), compiler.getOptions().useExtra());
					verifySystemFunction(compiler, v.getToken(), system_function);
				}

			} else if (v.getVariableType() == VariableType.CLASS) {

				var clazz = v.getClassDeclaration();
				var constructor = clazz.isInstantiable() ? clazz.getConstructor(mParameters.size()) : null;
				if (constructor == null) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
				} else if (constructor.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_CONSTRUCTOR, new String[] { clazz.getName() }));
				} else if (constructor.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_CONSTRUCTOR, new String[] { clazz.getName() }));
				}
				if (constructor != null && constructor.block != null) {
					this.constructorBlock = constructor.block;
					v.getToken().setExpression(this);
				}
				// Instanciation de classe générique (v5) : on infère les arguments de type
				// depuis les arguments du constructeur (même mécanique que les fonctions
				// génériques) pour produire un `Box<integer>` au lieu d'un `Box` brut où T
				// est effacé à any. No-op pour les classes non génériques / v1-4 (gelé).
				if (LanguageFeature.GENERICS.enabledIn(compiler.getVersion()) && clazz.isGeneric()
						&& constructor != null && constructor.block != null) {
					Map<TypeVariable, Type> bindings = new HashMap<>();
					var ctorArgs = constructor.block.getType().getArguments();
					for (int i = 0; i < mParameters.size() && i < ctorArgs.size(); ++i) {
						inferTypeVariables(ctorArgs.get(i), mParameters.get(i).getType(), bindings);
					}
					// Toute variable de classe non inférée est effacée vers sa borne (ANY) :
					// un `T` brut ne doit jamais fuir dans le type résultat.
					for (var tp : clazz.getTypeParameters()) bindings.putIfAbsent(tp, Type.ANY);
					this.type = new ParameterizedClassType(clazz, bindings);
				} else {
					this.type = clazz.getEmptyType();
				}

			} else if (v.getVariableType() == VariableType.SUPER) {

				var clazz = v.getClassDeclaration().getParent();
				if (clazz != null) {
					// super(...) appelle le constructeur parent. Dans une méthode statique il n'y a
					// pas d'objet (le Java généré ne compilait pas) ; dans une méthode d'instance il
					// relance le constructeur parent sur un objet déjà construit : un avertissement,
					// pour ne pas refuser les IA qui le font déjà.
					if (compiler.getCurrentBlock().isInStaticMethod()) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.SUPER_NOT_AVAILABLE_PARENT));
					} else if (!compiler.isInConstructor()) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.WARNING, Error.SUPER_NOT_AVAILABLE_PARENT));
					}
					var constructor = clazz.getConstructor(mParameters.size());
					operations += 1;
					if (constructor == null) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
					} else if (constructor.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_CONSTRUCTOR, new String[] { clazz.getName() }));
					} else if (constructor.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_CONSTRUCTOR, new String[] { clazz.getName() }));
					}
				}
			}
		} else if (mExpression instanceof LeekObjectAccess && !((LeekObjectAccess) mExpression).isOptional()) {
			// (les accès optionnels `obj?.method()` passent par le chemin dynamique
			//  null-safe : on ne résout pas la méthode statiquement)

			var oa = (LeekObjectAccess) mExpression;
			var o = oa.getObject();

			// Méthode sur un type natif (v5, OO stdlib) : array.sort(), array.map(f)...
			// Résolue comme `name(receiver, ...)`. Un type natif ne matche aucun des
			// checks ClassType ci-dessous, donc on traite ce cas en premier.
			if (LanguageFeature.OO_STDLIB.enabledIn(compiler.getVersion())) {
				var builtinClass = standardClassOf(o.getType());
				if (builtinClass != null) {
					var builtinMethod = LeekFunctions.getMethod(builtinClass, oa.getField());
					if (builtinMethod != null && resolveBuiltinMethod(compiler, o.getType(), builtinMethod)) {
						return;
					}
				}
				// Fonction de namespace built-in : `Math.sqrt(x)`, `Math.rand()`. Le
				// receveur est un namespace (classe interne), pas une valeur ; les
				// arguments restent les arguments réels (pas d'injection de receveur).
				// Résolue et vérifiée comme l'ancienne forme libre `sqrt(x)`.
				if (o instanceof LeekVariable nv && nv.getVariableType() == VariableType.CLASS
						&& LanguageFeature.OO_STDLIB.enabledIn(compiler.getVersion())) {
					var nsFunction = LeekFunctions.namespaceFunction(nv.getName(), oa.getField());
					if (nsFunction != null) {
						this.system_function = nsFunction;
						this.is_namespace_function = true;
						verifySystemFunction(compiler, oa.getFieldToken(), nsFunction);
						return;
					}
				}
			}

			if (o.getType() instanceof ClassType ct || (o instanceof LeekVariable lv && lv.getVariableType() == VariableType.SUPER)) {

				// on regarde si le nombre d'arguments est correct
				var current = o.getType() instanceof ClassType ct2 ? ct2.getClassDeclaration() : ((LeekVariable) o).getClassDeclaration().getParent();
				boolean resolved = false;
				end:
				while (current != null) {
					var methods = current.getMethod(oa.getField());
					if (methods != null) {
						is_method = true;
						for (var entry : methods.entrySet()) {
							if (entry.getKey() == mParameters.size()) {
								// Check method access level
								var method = entry.getValue();
								if (method.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != current) {
									compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_METHOD, new String[] { current.getName(), oa.getField() }));
								} else if (method.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(current))) {
									compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_METHOD, new String[] { current.getName(), oa.getField() }));
								}
								// Résout l'overload concrète pour que compileConvert utilise les types
								// déclarés au lieu du compound (Versions) qui dégrade en (Object).
								functionType = method.block.getType();
								resolved = true;
								break end;
							}
						}
					}
					current = current.getParent();
				}

				// super.method() : la méthode doit être résolue statiquement sur un ancêtre.
				// Le code généré est `super.u_method(...)`, qui ne compile pas en Java si
				// la méthode n'existe pas (ou existe avec un autre nombre d'arguments).
				// Sans cette vérification, l'erreur n'était détectée qu'au moment de la
				// compilation Java côté worker (issue #4010). On épargne les méthodes
				// natives de la classe de base (ex: super.keys()), dont le `super.u_method()`
				// compile et fonctionne réellement.
				if (!resolved && o instanceof LeekVariable sv && sv.getVariableType() == VariableType.SUPER
						&& !NATIVE_OBJECT_METHODS.contains(oa.getField() + "_" + mParameters.size())) {
					if (is_method) {
						// La méthode existe sur un ancêtre mais pas avec ce nombre d'arguments
						compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					} else {
						var parent = sv.getClassDeclaration().getParent();
						var className = parent != null ? parent.getName() : sv.getClassDeclaration().getName();
						compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_METHOD, new String[] { className, oa.getField() }));
					}
				}

			} else if (o instanceof LeekVariable v && (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS)) {

				var clazz = v.getVariableType() == VariableType.CLASS ? v.getClassDeclaration() : compiler.getCurrentClass();
				operations += 1;
				// on regarde si le nombre d'arguments est correct
				var current = clazz;
				HashMap<Integer, ClassDeclarationMethod> methods = null;
				end:
				while (current != null) {
					methods = current.getStaticMethod(oa.getField());
					if (methods != null) {
						for (var count : methods.keySet()) {
							if (count == mParameters.size()) {
								var staticMethod = methods.get(count);
								if (staticMethod.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
									compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
								} else if (staticMethod.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
									compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
								}
								is_static_method = true;
								method = staticMethod;
								functionType = staticMethod.block.getType();
								// return; // OK
								break end;
							}
						}
					}
					current = current.getParent();
				}

				if (methods != null) { // Trouvée mais mauvais nombre d'arguments
					// compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT, new String[] { clazz.getName(), oa.getField() }));
					is_static_method = true;
				} else { // Pas trouvée
					// Si c'est un champ statique, on accepte de l'appeler
					var field = clazz.getStaticField(oa.getField());
					if (field != null) {
						if (field.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
							compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_FIELD, new String[] { clazz.getName(), oa.getField() }));
						} else if (field.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
							compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_FIELD, new String[] { clazz.getName(), oa.getField() }));
						}
						is_static_method = true;
					} else {
						compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), oa.getField() }));
					}
				}
			}
		}

		// System.out.println("[FC] type = " + functionType);

		if (mParameters.size() < functionType.getMinArguments() || mParameters.size() > functionType.getMaxArguments()) {

			// Pas d'erreur pour les fonctions système en LS <= 4
			var level = this.is_static_method || this.is_method || compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;

			compiler.addError(new AnalyzeError(getLocation(), level, Error.INVALID_PARAMETER_COUNT));
		}

		// Boucle au lieu de stream+collect — appelé pour chaque function call analyzed.
		var types = new ArrayList<Type>(mParameters.size());
		for (var p : mParameters) types.add(p.getType());
		var cast = NarrowingInfo.warningArgumentsCast(functionType, mParameters, types, functionType.acceptsArguments(types));
		if (cast.ordinal() > CastType.UPCAST.ordinal()) {

			if (cast == CastType.INCOMPATIBLE || compiler.getMainBlock().isStrict()) {
				// Pas d'erreur pour les fonctions système en LS <= 4
				var level = (this.is_method || compiler.getMainBlock().isStrict()) && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				var error = cast == CastType.INCOMPATIBLE ? Error.INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION;

				compiler.addError(new AnalyzeError(getLocation(), level, error, new String[] {
					"(" + String.join(", ", types.stream().map(t -> t.toString()).collect(Collectors.toList())) + ")",
					"(" + String.join(", ", functionType.getArguments().stream().map(t -> t.toString()).collect(Collectors.toList())) + ")",
				}));
			}
		}
	}

	/**
	 * Fonction à argument fichier : le chemin, un littéral, est résolu comme un include
	 * (relatif au fichier appelant) et remplacé par le contenu du fichier.
	 */
	private void inlineFileArgument(WordCompiler compiler) throws LeekCompilerException {
		if (mParameters.isEmpty() || mParameters.get(0) instanceof LeekFileContent) return;
		if (!(mParameters.get(0) instanceof LeekString path)) {
			compiler.addError(new AnalyzeError(mParameters.get(0).getLocation(), AnalyzeErrorLevel.ERROR, Error.FILE_PATH_EXPECTED));
			return;
		}
		// Le fichier du jeton, pas celui du compilateur : une fonction incluse s'analyse avec le
		// compilateur du fichier racine. Pas de dossier : code compilé seul (extrait).
		var folder = path.getLocation().getFile().getFolder();
		AIFile file = null;
		try {
			if (folder != null) file = folder.resolve(path.getText());
		} catch (FileNotFoundException e) {}
		if (file == null) {
			compiler.addError(new AnalyzeError(path.getLocation(), AnalyzeErrorLevel.ERROR, Error.FILE_NOT_FOUND, new String[] { path.getText() }));
			return;
		}
		// Le contenu est compilé dans l'IA, porté par un champ statique par fichier.
		var field = compiler.getMainBlock().addFileContent(file);
		if (field == null) {
			compiler.addError(new AnalyzeError(path.getLocation(), AnalyzeErrorLevel.ERROR, Error.FILE_TOO_LARGE, new String[] { path.getText() }));
			return;
		}
		mParameters.set(0, new LeekFileContent(path, field));
	}

	private void verifySystemFunction(WordCompiler compiler, Token token, LeekFunctions f) throws LeekCompilerException {

		if (f.getOperations() > 0) {
			// System.out.println("cost of " + f.getName() + " : " + f.getOperations());
			operations += f.getOperations();
		}

		// La suppression de version (maxVersion) ne concerne que la FORME LIBRE
		// (`sort(a)` retiré en v5) : la fonction existe toujours en méthode et en
		// namespace. Un appel qualifié `Array.sort(a)` n'est donc pas soumis au retrait.
		if (compiler.getVersion() > f.getMaxVersion() && !is_namespace_function) {
			// Fonction supprimée
			if (f.getReplacement() != null) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.REMOVED_FUNCTION_REPLACEMENT, new String[] {
					String.valueOf(f.getMaxVersion()),
					String.valueOf(compiler.getVersion()),
					f.getReplacement()
				}));
			} else {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.REMOVED_FUNCTION, new String[] {
					String.valueOf(f.getMaxVersion()),
					String.valueOf(compiler.getVersion())
				}));
			}
			return;
		}
		if (compiler.getVersion() < f.getMinVersion()) {
			// Fonction non disponible
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.FUNCTION_NOT_AVAILABLE, new String[] {
				String.valueOf(f.getMinVersion()),
				String.valueOf(compiler.getVersion())
			}));
			return;
		}

		// Verify argument count
		if (compiler.getVersion() >= 3) {
			if (mParameters.size() > f.getArguments() || mParameters.size() < f.getArgumentsMin()) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
			}
		}

		if (f.hasFileArgument()) inlineFileArgument(compiler);

		// Verify argument types
		if (f.getVersions() != null) {
			verifyVersions(compiler, f.getVersions());
		}
	}

	public void verifyVersions(WordCompiler compiler, CallableVersion[] versions) throws LeekCompilerException {

		var types = new ArrayList<Type>(mParameters.size());
		for (var a : mParameters) types.add(a.getType());
		// System.out.println("verifyVersions types = " + types);

		// Find best version
		ArrayList<CallableVersion> best_versions = new ArrayList<>();
		// Type de retour accumulé sur le meilleur palier (compound des versions à
		// égalité). Accumulé en place pour éviter une liste parallèle à best_versions.
		Type best_return = Type.VOID;
		int best_distance = 9999;
		List<AnalyzeError> errors = null;
		for (var version : versions) {
			if (version.arguments.length != mParameters.size()) continue;
			// Versions génériques : on infère les variables de type à partir des
			// arguments effectifs, puis on substitue dans les types des paramètres
			// (pour le check de compatibilité) et dans le type de retour. No-op pour
			// les versions non génériques (cas dominant), donc zéro impact existant.
			Map<TypeVariable, Type> bindings = null;
			if (version.isGeneric()) {
				bindings = new HashMap<>();
				// En v5, on infère les variables de type depuis les arguments effectifs.
				if (LanguageFeature.GENERICS.enabledIn(compiler.getVersion())) {
					for (int i = 0; i < mParameters.size(); ++i) {
						inferTypeVariables(version.arguments[i], mParameters.get(i).getType(), bindings);
					}
				}
				// Toute variable non liée (non inférée, ou générics désactivés en v1-4)
				// est effacée vers sa borne (ANY) : un `T` brut ne doit jamais fuir dans
				// le type résultat (il casserait l'assignabilité). Ainsi v1-4 voit la
				// signature exactement comme l'ancienne version non générique.
				var vars = new java.util.HashSet<TypeVariable>();
				for (var arg : version.arguments) collectTypeVariables(arg, vars);
				collectTypeVariables(version.return_type, vars);
				for (var v : vars) bindings.putIfAbsent(v, Type.ANY);
			}
			int distance = 0;
			var version_errors = new ArrayList<AnalyzeError>();
			boolean version_unsafe = false;
			for (int i = 0; i < mParameters.size(); ++i) {
				var f_type = bindings != null ? version.arguments[i].substitute(bindings) : version.arguments[i];
				var a_type = mParameters.get(i).getType();
				var cast_type = f_type.accepts(a_type);
				if (cast_type == CastType.INCOMPATIBLE) {
					AnalyzeErrorLevel level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					version_errors.add(new AnalyzeError(mParameters.get(i).getLocation(), level, Error.WRONG_ARGUMENT_TYPE, new String[] {
						String.valueOf(i + 1),
						mParameters.get(i).toString(),
						a_type.toString(),
						f_type.toString()
					}));
					version_unsafe = true;
				// } else if (cast_type.ordinal() > CastType.SAFE_DOWNCAST.ordinal()) {
				} else if (cast_type != CastType.EQUALS) {
					version_unsafe = true;
				} else if (compiler.getVersion() >= 4 && !f_type.getJavaPrimitiveName(compiler.getVersion()).equals("Object") && mParameters.get(i).hasObjectJavaResult()) {
					// Un Object vers un paramètre Java typé (long, Number, String…) : l'appel direct
					// d'une native (v4+) ne trouverait pas sa surcharge, l'appel générique le reçoit
					// comme un any (#5300).
					version_unsafe = true;
				}
				// System.out.println("cast " + f_type + " accepts " + a_type + " = " + cast_type);
				if (cast_type == CastType.UPCAST) distance += 1;
				else if (cast_type == CastType.SAFE_DOWNCAST) distance += 100;
				else if (cast_type == CastType.UNSAFE_DOWNCAST) distance += 1000;
				else if (cast_type == CastType.INCOMPATIBLE) distance += 10000;
			}
			if (distance > 1000) distance = 1000;
			// Type de retour de cette version, substitué si générique.
			var returnType = bindings != null ? version.return_type.substitute(bindings) : version.return_type;
			// System.out.println("version = " + version + " distance = " + distance);
			if (distance == best_distance) {
				best_versions.add(version);
				best_return = Type.compound(best_return, returnType);
			} else if (distance < best_distance) {
				best_distance = distance;
				best_versions.clear();
				best_versions.add(version);
				best_return = returnType;
				unsafe = version_unsafe;
				errors = version_errors;
			}
		}
		if (errors != null) {
			for (var error : errors) {
				compiler.addError(error);
			}
		}
		if (best_versions.size() > 0) {
			callable_versions = best_versions;
			type = best_return;
			// System.out.println("[FC] version=" + best_versions.get(0) + " type=" + type + " unsafe=" + unsafe);
		} else {
			unsafe = true;
		}
	}

	/**
	 * Infers generic type-variable bindings by structurally matching a (possibly
	 * generic) formal parameter type against the concrete actual argument type.
	 * Multiple occurrences of the same variable are joined into a compound type.
	 */
	private void inferTypeVariables(Type formal, Type actual, Map<TypeVariable, Type> bindings) {
		if (formal instanceof TypeVariable variable) {
			var existing = bindings.get(variable);
			bindings.put(variable, existing == null ? actual : Type.compound(existing, actual));
		} else if (formal instanceof ArrayType && actual instanceof ArrayType) {
			inferTypeVariables(formal.element(), actual.element(), bindings);
		} else if (formal instanceof SetType && actual instanceof SetType) {
			inferTypeVariables(formal.element(), actual.element(), bindings);
		} else if (formal instanceof MapType && actual instanceof MapType) {
			inferTypeVariables(formal.key(), actual.key(), bindings);
			inferTypeVariables(formal.element(), actual.element(), bindings);
		} else if (formal instanceof FunctionType formalFn && actual instanceof FunctionType actualFn) {
			// Le type de retour d'un callback est covariant : on en infère R. Les types
			// de paramètres sont contravariants — T *entre* dans le callback (il vient
			// d'ailleurs, ex. l'élément du tableau), on ne l'infère donc PAS depuis eux,
			// ce qui polluerait T avec le `any` d'un lambda non typé. Ex :
			// arrayMap(Array<T>, Function<(T,int,Array<T>) => R>) -> Array<R> : T se lie
			// depuis le tableau, R depuis le retour du callback.
			inferTypeVariables(formalFn.returnType(), actualFn.returnType(), bindings);
		} else if (formal instanceof CompoundType formalCompound) {
			// Formal like `T?` (T | null) : infer against each branch.
			for (var t : formalCompound.getTypes()) {
				inferTypeVariables(t, actual, bindings);
			}
		}
	}

	/**
	 * True if the selected system-function version(s) declare a generic return type
	 * (the Java method is erased, so the call result needs a cast to the inferred type).
	 */
	private boolean systemReturnContainsTypeVariable() {
		if (callable_versions == null) return false;
		for (var version : callable_versions) {
			if (version.return_type.containsTypeVariable()) return true;
		}
		return false;
	}

	/**
	 * The standard-library class name of a built-in receiver type that exposes
	 * methods. Array/Map/Set methods are instance methods on the value class
	 * (`((ArrayLeekValue) x).sort(ai)`); String methods are static helpers
	 * (`StringClass.length(ai, s)`) — the codegen branches on {@code isStatic()}.
	 */
	private static String standardClassOf(Type type) {
		if (type instanceof ArrayType) return "Array";
		if (type instanceof MapType) return "Map";
		if (type instanceof SetType) return "Set";
		if (type == Type.STRING) return "String";
		if (type.isNumber()) return "Number";
		return null;
	}

	/**
	 * Resolves `receiver.name(params...)` as the built-in method `name` on the
	 * receiver's type, treating the receiver as the implicit first argument (so
	 * `array.sort()` == `sort(array)`). Picks the best overload (generic inference
	 * included), sets {@code type}/{@code callable_versions}, and returns true on
	 * success. Used for the OO standard library (version 5).
	 */
	private boolean resolveBuiltinMethod(WordCompiler compiler, Type receiverType, LeekFunctions method) {
		int arity = mParameters.size() + 1; // + le receveur
		var argTypes = new ArrayList<Type>(arity);
		argTypes.add(receiverType);
		for (var p : mParameters) argTypes.add(p.getType());

		CallableVersion best = null;
		int bestDistance = Integer.MAX_VALUE;
		Type bestReturn = Type.VOID;
		boolean bestUnsafe = false;
		for (var version : method.getVersions()) {
			if (version.arguments.length != arity) continue;
			Map<TypeVariable, Type> bindings = null;
			if (version.isGeneric()) {
				bindings = new HashMap<>();
				if (LanguageFeature.GENERICS.enabledIn(compiler.getVersion())) {
					for (int i = 0; i < arity; ++i) inferTypeVariables(version.arguments[i], argTypes.get(i), bindings);
				}
				var vars = new java.util.HashSet<TypeVariable>();
				for (var arg : version.arguments) collectTypeVariables(arg, vars);
				collectTypeVariables(version.return_type, vars);
				for (var v : vars) bindings.putIfAbsent(v, Type.ANY);
			}
			int distance = 0;
			boolean unsafe = false;
			for (int i = 0; i < arity; ++i) {
				var f_type = bindings != null ? version.arguments[i].substitute(bindings) : version.arguments[i];
				var cast = f_type.accepts(argTypes.get(i));
				if (cast != CastType.EQUALS) unsafe = true;
				if (cast == CastType.UPCAST) distance += 1;
				else if (cast == CastType.SAFE_DOWNCAST) distance += 100;
				else if (cast == CastType.UNSAFE_DOWNCAST) distance += 1000;
				else if (cast == CastType.INCOMPATIBLE) distance += 10000;
			}
			if (distance > 1000) distance = 1000;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = version;
				bestReturn = bindings != null ? version.return_type.substitute(bindings) : version.return_type;
				bestUnsafe = unsafe;
			}
		}
		if (best == null) return false;
		this.system_function = method;
		this.is_builtin_method = true;
		this.unsafe = bestUnsafe;
		this.callable_versions = new ArrayList<>();
		this.callable_versions.add(best);
		this.functionType = best.getType();
		this.type = bestReturn;
		return true;
	}

	/** Collects every {@link TypeVariable} occurring in a (possibly nested) type. */
	private void collectTypeVariables(Type type, java.util.Set<TypeVariable> out) {
		if (type instanceof TypeVariable variable) {
			out.add(variable);
		} else if (type instanceof ArrayType || type instanceof SetType || type instanceof IntervalType) {
			collectTypeVariables(type.element(), out);
		} else if (type instanceof MapType) {
			collectTypeVariables(type.key(), out);
			collectTypeVariables(type.element(), out);
		} else if (type instanceof FunctionType function) {
			collectTypeVariables(function.returnType(), out);
			for (var a : function.getArguments()) collectTypeVariables(a, out);
		} else if (type instanceof CompoundType compound) {
			for (var t : compound.getTypes()) collectTypeVariables(t, out);
		}
	}

	@Override
	public Location getLocation() {
		if (mExpression == null) {
			if (closingParenthesis == null) {
				return openParenthesis.getLocation();
			}
			return new Location(openParenthesis.getLocation(), closingParenthesis.getLocation());
		}
		return new Location(mExpression.getLocation(), closingParenthesis.getLocation());
	}
}
