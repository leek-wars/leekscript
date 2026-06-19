package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.EnumSet;

import leekscript.common.Annotation;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.Annotatable;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.ConstantFolder;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.CallableVersion;

public class FunctionBlock extends AbstractLeekBlock implements Annotatable, leekscript.compiler.TypeParameterScope {

	private Token token;
	private int mId;
	// Module v5 propriétaire (préfixe le nom de codegen). null = module principal
	// (préfixe vide → byte-identique v1-4).
	private Module module;
	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekType> mParametersTypes = new ArrayList<LeekType>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();
	private final ArrayList<Expression> defaultValues = new ArrayList<>();
	private final ArrayList<Type> mTypes = new ArrayList<>();
	private CallableVersion[] versions;
	private FunctionType type = new FunctionType(Type.ANY);
	private int minParameters = 0;
	private int maxParameters = 0;
	private LeekVariable variable;
	// Lazy : la grande majorité des fonctions n'ont aucune annotation.
	private EnumSet<Annotation> annotations = null;
	// Paramètres de type génériques (`function<T> f(...)`, version >= 5). Lazy :
	// la quasi-totalité des fonctions ne sont pas génériques.
	private ArrayList<leekscript.common.TypeVariable> typeParameters = null;
	// @tailrec : appels récursifs collectés pendant l'analyse du corps, validés
	// ensuite (doivent tous être en position terminale). Lazy : seules les
	// fonctions @tailrec qui s'appellent elles-mêmes en collectent.
	private ArrayList<leekscript.compiler.expression.LeekFunctionCall> recursiveCalls = null;
	// Type guard (#42, v5) : `function f(any x) -> p is Type`. La fonction renvoie un
	// booléen ; quand un appel sert de condition, l'argument à `typeGuardParam` est
	// affiné vers `typeGuardType` dans la branche vraie. -1 / null = pas de guard.
	private int typeGuardParam = -1;
	private Type typeGuardType = null;
	// #43 (v5) : le type de retour vient-il d'une annotation explicite `-> Type` (ou d'un
	// type guard) ? Si oui on n'infère pas ; sinon on infère depuis les `return` du corps.
	private boolean returnTypeExplicit = false;
	// #43 : instructions `return` du corps, collectées pendant analyze pour inférer le
	// type de retour (union de leurs types). Lazy ; les fonctions anonymes imbriquées
	// sont exclues (getCurrentFunction y vaut l'anonyme, pas ce FunctionBlock).
	private ArrayList<leekscript.compiler.instruction.LeekReturnInstruction> bodyReturns = null;
	// #43 T2 : drapeau d'analyse idempotente. L'inférence à la demande (LeekFunctionCall
	// déclenche analyze de l'appelé) peut appeler analyze plusieurs fois et en ré-entrance ;
	// on n'analyse qu'une fois.
	private boolean analysisStarted = false;

	public FunctionBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public String getName() {
		return token.getWord();
	}

	public void setModule(Module module) {
		this.module = module;
	}

	public Module getModule() {
		return module;
	}

	/** Nom utilisé dans le code Java généré (`f_`, `ufunction_`). Préfixé par le
	 * module pour éviter les collisions entre modules ; vide pour le module
	 * principal → v1-4 byte-identique. */
	public String getInternalName() {
		return (module == null ? "" : module.prefix()) + token.getWord();
	}

	public int countParameters() {
		return mParameters.size();
	}

	public int getParameterIndex(String name) {
		return mParameters.indexOf(name);
	}

	// Type guard (#42) : enregistré au parse (`-> p is Type`), lu par NarrowingInfo.
	public void setTypeGuard(int paramIndex, Type type) {
		this.typeGuardParam = paramIndex;
		this.typeGuardType = type;
	}

	public int getTypeGuardParam() {
		return typeGuardParam;
	}

	public Type getTypeGuardType() {
		return typeGuardType;
	}

	public int getMinParameters() {
		return minParameters;
	}

	public int getMaxParameters() {
		return maxParameters;
	}

	public ArrayList<LeekVariableDeclarationInstruction> getParametersDeclarations() {
		return mParameterDeclarations;
	}

	public ArrayList<Expression> getDefaultValues() {
		return defaultValues;
	}

	public String referenceArray() {
		String str = "{";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ",";
			str += mReferences.get(i) ? "true" : "false";
		}
		return str + "}";
	}

	public void addParameter(WordCompiler compiler, Token token, boolean is_reference, LeekType leekType, Expression defaultValue) throws LeekCompilerException {

		for (var parameter : mParameters) {
			if (parameter.equals(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_UNAVAILABLE));
			}
		}

		// Paramètre par défaut pas à la fin
		if (mParameters.size() > 0 && defaultValue == null && defaultValues.get(defaultValues.size() - 1) != null) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.DEFAULT_ARGUMENT_NOT_END));
		}

		// Un paramètre par référence (@) est implémenté par une Box (objet mutable
		// partagé avec l'appelant) : son type Java ne peut donc pas être un primitif.
		// La signature ferait `long p_x` alors que le corps fait `p_x instanceof Box`
		// et `(Box) p_x`, ce qui ne compile pas (cf. #3991). On force le type à ANY.
		var type = is_reference ? Type.ANY : parameterType(leekType != null ? leekType.getType() : Type.ANY, defaultValue);
		mParameters.add(token.getWord());
		mParametersTypes.add(leekType);
		mReferences.add(is_reference);
		mTypes.add(type);
		defaultValues.add(defaultValue);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, this, type);
		if (is_reference) {
			declaration.setBox();
		}
		// Box brut : paramètre par référence, et en v1 l'arité qui prend la valeur par défaut
		// (cf writeJavaCodeForArity)
		if (is_reference || defaultValue != null && compiler.getVersion() == 1) {
			declaration.setUntypedBox();
		}
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, type, declaration));
		maxParameters++;
		if (defaultValue == null) {
			minParameters++;
		}
		this.type.add_argument(type, defaultValue != null);
	}

	/**
	 * Type effectif d'un paramètre déclaré `declared` avec la valeur par défaut `defaultValue`.
	 *
	 * Une valeur par défaut n'est jamais rejetée pour son type (cf. #4703 ; un défaut incompatible
	 * n'est qu'un warning, cf. checkDefaultValueType) ; un défaut `null` sur un type primitif (`integer to = null`) donnait `long u_to = null`, qui
	 * ne compile pas en Java : l'IA plantait à chaque combat (erreur prod #11872155). Le paramètre
	 * devient non typé, `null` garde sa sémantique dynamique ; les types référence (`A a = null`)
	 * sont inchangés. Partagé avec les méthodes de classe (ClassMethodBlock).
	 */
	public static Type parameterType(Type declared, Expression defaultValue) {
		if (declared.isPrimitive() && defaultValue != null && defaultValue.trim() instanceof LeekNull) {
			return Type.ANY;
		}
		return declared;
	}

	/**
	 * Avertit quand une valeur par défaut est d'un type incompatible avec son paramètre
	 * (`A a = []`) : sans cela, rien ne l'annonce dans l'éditeur, alors que l'appel qui
	 * utilise le défaut échoue au runtime. Toujours un warning, même en mode strict : les
	 * défauts n'ont jamais été type-checkés, une erreur ferait échouer à la compilation des
	 * IA existantes. `null` est exclu comme dans parameterType(). Partagé avec les méthodes
	 * de classe (ClassMethodBlock).
	 */
	public static void checkDefaultValueType(WordCompiler compiler, String parameter, Type type, Expression defaultValue) throws LeekCompilerException {
		if (defaultValue.trim() instanceof LeekNull) return;
		if (type.accepts(defaultValue.getType()) == CastType.INCOMPATIBLE) {
			compiler.addError(new AnalyzeError(defaultValue.getLocation(), AnalyzeErrorLevel.WARNING, Error.ASSIGNMENT_INCOMPATIBLE_TYPE, new String[] {
				defaultValue.toString(),
				defaultValue.getType().toString(),
				parameter,
				type.toString(),
			}));
		}
	}

	public void setReturnType(Type type) {
		this.type.setReturnType(type);
		this.returnTypeExplicit = true;
	}

	public void addTypeParameter(leekscript.common.TypeVariable variable) {
		if (typeParameters == null) typeParameters = new ArrayList<>();
		typeParameters.add(variable);
	}

	/** Resolves a type-parameter name (e.g. {@code T}) declared on this function, or null. */
	public leekscript.common.TypeVariable getTypeParameter(String name) {
		if (typeParameters != null) {
			for (var variable : typeParameters) {
				if (variable.getName().equals(name)) return variable;
			}
		}
		return null;
	}

	@Override
	public boolean hasVariable(String variable) {
		return mVariables != null && mVariables.containsKey(variable);
	}

	@Override
	public String getCode() {
		String str = "function " + token.getWord() + "(";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ", ";
			if (mParametersTypes.get(i) != null) {
				str += mParametersTypes.get(i).getType().getCode() + " ";
			}
			str += mParameters.get(i);
			if (defaultValues.get(i) != null) {
				str += " = " + defaultValues.get(i).toString();
			}
		}
		return str + ") {\n" + super.getCode() + "}\n";
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {

		// Créer une version pour chaque arité possible
		buildVersions();

		var initialFunction = compiler.getCurrentFunction();
		// Le corps d'une fonction se résout dans le module qui la déclare (imports +
		// fonctions privées du module), pas dans celui de l'appelant. La phase analyze
		// traverse tout via le compilateur principal, d'où ce save/restore du module.
		var initialModule = compiler.getModule();
		compiler.setCurrentFunction(this);
		if (module != null) compiler.setModule(module);
		for (var value : defaultValues) {
			if (value != null) {
				value.preAnalyze(compiler);
			}
		}
		super.preAnalyze(compiler);
		compiler.setCurrentFunction(initialFunction);
		compiler.setModule(initialModule);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		// #43 T2 : analyse idempotente. L'inférence à la demande peut déclencher analyze
		// d'un appelé avant la boucle principale, et un cycle se ré-entre. On n'analyse
		// qu'une fois ; un appel ré-entrant retourne aussitôt → le type de l'appelé en cours
		// d'analyse vaut ANY (récursion sûre, pas de point fixe).
		if (analysisStarted) return;
		analysisStarted = true;
		var initialFunction = compiler.getCurrentFunction();
		var initialModule = compiler.getModule();
		compiler.setCurrentFunction(this);
		if (module != null) compiler.setModule(module);
		for (int i = 0; i < defaultValues.size(); ++i) {
			var value = defaultValues.get(i);
			if (value != null) {
				value.analyze(compiler);
				checkDefaultValueType(compiler, mParameters.get(i), mTypes.get(i), value);
			}
		}
		super.analyze(compiler);
		// @tailrec : après l'analyse du corps, tout appel récursif collecté doit être
		// en position terminale (expression directe d'un `return` non optionnel).
		// Sinon, erreur dure pointant l'appel fautif. Inactif sans l'annotation.
		if (recursiveCalls != null && hasAnnotation(Annotation.TAILREC)) {
			for (var call : recursiveCalls) {
				if (!call.isTailPosition()) {
					compiler.addError(new AnalyzeError(call.getLocation(), AnalyzeErrorLevel.ERROR, Error.ANNOTATION_TAILREC_NOT_TAIL, new String[] { getName() }));
				}
			}
		}
		// #43 (v5) : inférer le type de retour depuis les `return` du corps si pas
		// d'annotation explicite. Union des types retournés ; + NULL si le corps peut
		// tomber en fin (retour implicite null) ou n'a aucun return. Un appel récursif
		// vaut ANY au moment de l'analyse → l'union retombe sur ANY (pas de point fixe).
		// On reconstruit les versions ET on aligne le type de chaque `return` sur le type
		// inféré, sinon la signature Java inférée et la conversion des `return` divergent.
		// `mEndInstruction == 0` = même condition que le retour implicite du codegen.
		if (!returnTypeExplicit && leekscript.common.LanguageFeature.RETURN_TYPE_INFERENCE.enabledIn(compiler.getVersion())) {
			Type inferred = null;
			if (bodyReturns != null) {
				for (var ret : bodyReturns) {
					inferred = inferred == null ? ret.getReturnedType() : Type.compound(inferred, ret.getReturnedType());
				}
			}
			if (inferred == null) {
				inferred = Type.NULL;
			} else if (mEndInstruction == 0) {
				inferred = Type.compound(inferred, Type.NULL);
			}
			this.type.setReturnType(inferred);
			buildVersions();
			if (bodyReturns != null) {
				for (var ret : bodyReturns) ret.setInferredReturnType(inferred);
			}
		}
		compiler.setCurrentFunction(initialFunction);
		compiler.setModule(initialModule);
	}

	@Override
	public void checkEndBlock() {

	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		for (int paramCount = minParameters; paramCount <= maxParameters; paramCount++) {
			writeJavaCodeForArity(mainblock, writer, paramCount);
		}
	}

	private void writeJavaCodeForArity(MainLeekBlock mainblock, JavaWriter writer, int paramCount) {
		final boolean v1 = mainblock.getVersion() <= 1;
		StringBuilder sb = new StringBuilder();
		sb.append("private " + this.type.returnType().getJavaPrimitiveName(mainblock.getVersion()) + " f_").append(getInternalName()).append("(");
		for (int i = 0; i < paramCount; i++) {
			if (i != 0)
				sb.append(", ");
			sb.append(mParameterDeclarations.get(i).getType().getJavaPrimitiveName(mainblock.getVersion()));
			sb.append(" p_").append(mParameters.get(i));
		}
		sb.append(") throws LeekRunException {");

		// Paramètres passés en argument
		for (int i = 0; i < paramCount; i++) {
			var parameter = mParameters.get(i);
			var declaration = mParameterDeclarations.get(i);
			if (declaration.isCaptured()) {
				if (v1) {
					sb.append("final var u_").append(parameter).append(" = new Wrapper<" + declaration.getType().getJavaName(mainblock.getVersion()) + ">(");
					if (mReferences.get(i)) {
						sb.append("(p_").append(parameter).append(" instanceof Box) ? (Box) p_").append(parameter).append(" : new Box(" + writer.getAIThis() + ", ").append("p_").append(parameter).append("));");
					} else {
						sb.append("new Box(").append(writer.getAIThis()).append(", copy(p_").append(parameter).append(")));");
					}
				} else {
					sb.append("final var u_").append(parameter).append(" = new Box<" + declaration.getType().getJavaName(mainblock.getVersion()) + ">(").append(writer.getAIThis()).append(", p_").append(parameter).append(");");
				}
			} else {
				sb.append("var u_").append(parameter).append(" = ");
				if (mReferences.get(i)) {
					sb.append("(p_").append(parameter).append(" instanceof Box) ? (Box) p_").append(parameter).append(" : new Box(" + writer.getAIThis());
					if (v1) {
						sb.append(", p_").append(parameter).append(");");
					} else {
						sb.append(", copy(p_").append(parameter).append("));");
					}
				} else {
					if (v1) {
						// Box typée (`Box<Type>`) et non brute : sinon `u_x.get()` est typé
						// Object et un accès indexé sur un paramètre `Array x` (`x[i]`)
						// génère `u_x.get().get(i)` = « cannot find symbol method get(long)
						// on class Object ». Cf. la déclaration des locales typées et des
						// paramètres capturés, déjà en `Box<Type>`. (#4510)
						sb.append("new Box<" + declaration.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", copy(p_").append(parameter).append("));");
					} else {
						sb.append("p_").append(parameter).append(";");
					}
				}
			}
		}
		writer.addLine(sb.toString(), getLocation());

		// Paramètres avec valeur par défaut (non passés en argument)
		for (int i = paramCount; i < maxParameters; i++) {
			var declaration = mParameterDeclarations.get(i);
			var defaultValue = defaultValues.get(i);
			var parameter = mParameters.get(i);
			if (declaration.isCaptured()) {
				// v1 : un Wrapper, comme l'arité qui reçoit l'argument : le corps, commun aux arités,
				// y passe par getVariable() (cf LeekVariable.isWrapper) (#5325)
				var javaType = declaration.getType().getJavaName(mainblock.getVersion());
				writer.addCode("final var u_" + parameter + " = " + (v1 ? "new Wrapper<" + javaType + ">(new Box(" : "new Box<" + javaType + ">(") + writer.getAIThis() + ", ");
				writer.compileDefaultValue(mainblock, i, defaultValue, declaration.getType());
				writer.addLine(v1 ? "));" : ");");
				writer.addCounter(defaultValue.operations);
			} else {
				if (v1) {
					writer.addCode("var u_" + parameter + " = new Box(" + writer.getAIThis() + ", ");
					defaultValue.writeJavaCode(mainblock, writer, false);
					writer.addLine(");");
				} else {
					// Type explicite : `var u_x = null;` n'est pas inférable par javac.
					writer.addCode(declaration.getType().getJavaPrimitiveName(mainblock.getVersion()) + " u_" + parameter + " = ");
					writer.compileDefaultValue(mainblock, i, defaultValue, declaration.getType());
					writer.addLine(";");
				}
				writer.addCounter(defaultValue.operations);
			}
		}

		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer, false);
		if (mEndInstruction == 0) {
			writer.addLine("return " + type.returnType().getDefaultValue(writer, mainblock.getVersion()) + ";");
		}
		writer.addLine("}");
	}

	/**
	 * Classification du corps après pliage des constantes, pour l'élimination au
	 * site d'appel (cf ConstantFolder.classifyBody). Une fonction globale
	 * s'exécute sans contexte de classe : fromClass = null (seuls les champs
	 * publics sont pliables, parité avec le runtime).
	 */
	private ConstantFolder.ClassifiedBody bodyClassification = null;

	public ConstantFolder.ClassifiedBody getBodyClassification(MainLeekBlock mainblock) {
		if (bodyClassification == null) {
			bodyClassification = mainblock.getVersion() < 2 ? ConstantFolder.ClassifiedBody.NONE
				: ConstantFolder.classifyBody(mInstructions, defaultValues, type.returnType(), null);
		}
		return bodyClassification;
	}

	public void compileAnonymousFunction(MainLeekBlock mainblock, JavaWriter writer) {
		// Une fonction déclarée a un nom : on le transmet à la valeur fonction pour que
		// `string(f)` affiche `#Function f` et non `#Anonymous Function` (#5042).
		writer.addCode("new FunctionLeekValue(" + this.countParameters() + ", \"#Function " + this.getName() + "\") { public Object run(AI ai, Object thiz, Object... values) throws LeekRunException {");
		writer.addCode("return f_" + getInternalName() + "(");
		for (int a = 0; a < this.countParameters(); ++a) {
			if (a > 0) writer.addCode(", ");
			writer.addCode("values.length > " + a + " ? " + (type != Type.ANY ? "(" + type.getArgument(a).getJavaName(mainblock.getVersion()) + ")" : "") + " values[" + a + "] : ");
			if (defaultValues.get(a) != null) {
				writer.compileDefaultValue(mainblock, a, defaultValues.get(a), this.type.getArgument(a));
			} else {
				writer.addCode(this.type.getArgument(a).getDefaultValue(writer, mainblock.getVersion()));
			}
		}
		writer.addLine(");");
		writer.addLine("}}");
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}

	public void addAnnotation(Annotation a) {
		if (annotations == null) annotations = EnumSet.noneOf(Annotation.class);
		annotations.add(a);
	}

	public boolean hasAnnotation(Annotation a) {
		return annotations != null && annotations.contains(a);
	}

	// @tailrec : un appel résolu vers cette fonction (récursion directe) signale sa
	// présence ici pendant l'analyse de son propre corps, pour validation a posteriori.
	public void registerRecursiveCall(leekscript.compiler.expression.LeekFunctionCall call) {
		if (recursiveCalls == null) recursiveCalls = new ArrayList<>();
		recursiveCalls.add(call);
	}

	// #43 : un `return` du corps s'enregistre ici pendant l'analyse, pour l'inférence du
	// type de retour (cf. analyze).
	public void registerReturn(leekscript.compiler.instruction.LeekReturnInstruction ret) {
		if (bodyReturns == null) bodyReturns = new ArrayList<>();
		bodyReturns.add(ret);
	}

	// Construit les CallableVersion (une par arité) depuis le type de retour courant.
	// Appelé en preAnalyze, puis reconstruit après inférence du type de retour (#43).
	private void buildVersions() {
		var versionsList = new ArrayList<CallableVersion>();
		for (int p = minParameters; p <= maxParameters; ++p) {
			var versionTypes = new Type[p];
			for (int t = 0; t < p; ++t) versionTypes[t] = mTypes.get(t);
			versionsList.add(new CallableVersion(this.type.getReturnType(), versionTypes));
		}
		this.versions = versionsList.toArray(new CallableVersion[0]);
	}

	public void declare(WordCompiler compiler) {
		variable = new LeekVariable(token, VariableType.FUNCTION, type, this);
		if (annotations != null) {
			for (var a : annotations) variable.addAnnotation(a);
		}
		// Les fonctions du module principal (entrypoint + include) sont déclarées comme
		// variables du bloc partagé : c'est le chemin de résolution historique (et la
		// forme « fonction-valeur »). Les fonctions d'un module IMPORTÉ ne le sont pas :
		// elles ne se résolvent que via getUserFunction(name, module) module-aware, pour
		// ne pas polluer le namespace principal ni casser la privacy.
		if (module == null || module.isMain()) {
			compiler.getCurrentBlock().addVariable(variable);
		}
	}

	public LeekVariable getVariable() {
		return variable;
	}

	public String toString() {
		return token.getWord();
	}

	public CallableVersion[] getVersions() {
		return versions;
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
		return type;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
