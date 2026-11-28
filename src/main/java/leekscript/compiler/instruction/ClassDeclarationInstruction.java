package leekscript.compiler.instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import leekscript.runner.values.ClassLeekValue;
import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.ClassMethodBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.ConstantFolder;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Annotation;
import leekscript.common.AccessLevel;
import leekscript.common.ClassType;
import leekscript.common.ClassValueType;
import leekscript.common.Error;
import leekscript.common.LanguageFeature;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.runner.AI;
import leekscript.compiler.bytecode.BytecodeWriter;
import leekscript.compiler.bytecode.ClassStubs;
import leekscript.compiler.bytecode.JExpr;
import leekscript.compiler.bytecode.JTypes;
import leekscript.compiler.bytecode.Resolver;
import leekscript.compiler.bytecode.Unsupported;
import java.lang.classfile.ClassFile;
import java.lang.constant.ConstantDescs;
import leekscript.runner.Final;
import leekscript.runner.Private;
import leekscript.runner.Protected;
import leekscript.runner.values.ObjectVariableValue;
import leekscript.runner.values.FunctionLeekValue;

public class ClassDeclarationInstruction extends LeekInstruction implements leekscript.compiler.TypeParameterScope {

	public static class ClassDeclarationField extends LeekExpression {

		String name;
		Expression expression;
		public AccessLevel level;
		boolean isFinal;
		// Type écrit dans la déclaration (`integer x`), cf MainLeekBlock.checkPackageApis
		public boolean typed = true;

		public ClassDeclarationField(String name, Expression expr, AccessLevel level, boolean isFinal, Type type) {
			this.name = name;
			this.expression = expr;
			this.level = level;
			this.isFinal = isFinal;
			this.type = type;
		}

		public Expression getExpression() {
			return expression;
		}

		@Override
		public boolean isFinal() {
			return isFinal;
		}

		@Override
		public Hover hover(Token token) {
			return new Hover(type, token.getLocation(), token.getWord());
		}
	}

	public static class ClassDeclarationMethod {

		public ClassMethodBlock block;
		public AccessLevel level;
		public ClassMethodBlock ppp;

		public ClassDeclarationMethod(ClassMethodBlock block, AccessLevel level) {
			this.block = block;
			this.level = level;
		}
	}

	private final Token token;
	private Token parentToken;
	private ClassDeclarationInstruction parent;
	private MainLeekBlock mainBlock;
	public boolean internal;
	// Classe interne sans constructeur au runtime (Function, Class, Value, JSON, System) : pas de
	// new_<classe>() dans AI, `new X()` y est une erreur comme au runtime (UNKNOWN_CONSTRUCTOR)
	private boolean instantiable = true;
	private LinkedHashMap<String, ClassDeclarationField> fields = new LinkedHashMap<>();
	private LinkedHashMap<String, ClassDeclarationField> staticFields = new LinkedHashMap<>();
	private HashMap<String, LeekVariable> fieldVariables = new HashMap<>();
	private HashMap<String, LeekVariable> staticFieldVariables = new HashMap<>();
	private HashMap<String, LeekVariable> methodVariables = new HashMap<>();
	private HashMap<String, LeekVariable> staticMethodVariables = new HashMap<>();
	private ArrayList<ClassDeclarationMethod> constructorsList = new ArrayList<>();
	private HashMap<Integer, ClassDeclarationMethod> constructors = new HashMap<>();
	private HashMap<String, HashMap<Integer, ClassDeclarationMethod>> methods = new HashMap<>();
	private HashMap<String, HashMap<Integer, ClassDeclarationMethod>> staticMethods = new HashMap<>();
	private ClassMethodBlock staticInitBlock;
	public Type classType;
	public Type emptyType;
	public Type classValueType;

	public ClassDeclarationInstruction(Token token, int line, AIFile ai, boolean internal, MainLeekBlock block) {
		this(token, line, ai, internal, block, null, null);
		this.classType = new ClassType(this);
		this.emptyType = this.classType;
	}

	public ClassDeclarationInstruction(Token token, int line, AIFile ai, boolean internal, MainLeekBlock block, Type type) {
		this(token, line, ai, internal, block, type, type);
	}

	public ClassDeclarationInstruction(Token token, int line, AIFile ai, boolean internal, MainLeekBlock block, Type type, Type emptyType) {
		this.token = token;
		this.internal = internal;
		this.mainBlock = block;
		this.staticInitBlock = new ClassMethodBlock(this, false, true, block, block, null, Type.ANY);
		this.classType = type;
		this.emptyType = emptyType;
	}

	public ClassDeclarationInstruction notInstantiable() {
		this.instantiable = false;
		return this;
	}

	public boolean isInstantiable() {
		return instantiable;
	}

	public HashMap<String, ClassDeclarationField> getFields() {
		return fields;
	}
	public HashMap<String, ClassDeclarationField> getStaticFields() {
		return staticFields;
	}
	public HashMap<String, HashMap<Integer, ClassDeclarationMethod>> getStaticMethods() {
		return staticMethods;
	}

	public HashMap<String, HashMap<Integer, ClassDeclarationMethod>> getMethods() {
		return methods;
	}
	public HashMap<String, LeekVariable> getFieldVariables() {
		return fieldVariables;
	}
	public HashMap<String, LeekVariable> getMethodVariables() {
		return methodVariables;
	}
	public HashMap<String, LeekVariable> getStaticFieldVariables() {
		return staticFieldVariables;
	}
	public HashMap<String, LeekVariable> getStaticMethodVariables() {
		return staticMethodVariables;
	}

	public String getName() {
		return token.getWord();
	}

	// Module v5 propriétaire (préfixe le nom de codegen `u_<Class>`). null = principal.
	private leekscript.compiler.bloc.Module module;

	public void setModule(leekscript.compiler.bloc.Module module) {
		this.module = module;
	}

	public leekscript.compiler.bloc.Module getModule() {
		return module;
	}

	// v5, une classe Java par module : la classe Java qui contient celle-ci (celle de son module,
	// ou celle de l'IA). Null quand le programme tient en une seule classe Java.
	private String javaOwner;

	public void setJavaOwner(String javaOwner) {
		this.javaOwner = javaOwner;
	}

	/** Le type Java de la classe, lisible de n'importe quelle classe du programme. */
	public String getJavaTypeName() {
		return (javaOwner == null ? "" : javaOwner + ".") + "u_" + getInternalName();
	}

	/** Nom de codegen de la classe (`u_<internalName>`), dans la classe Java de son module. */
	public String getInternalName() {
		return token.getWord();
	}

	/** Clé de la classe dans les tables du programme, qui tiennent celles de tous les modules. */
	public String getKey() {
		return (module == null ? "" : module.prefix()) + token.getWord();
	}

	// Paramètres de type génériques (`class Box<T> { ... }`, version >= 5). Lazy :
	// la quasi-totalité des classes ne sont pas génériques.
	private java.util.ArrayList<leekscript.common.TypeVariable> typeParameters = null;

	public void addTypeParameter(leekscript.common.TypeVariable variable) {
		if (typeParameters == null) typeParameters = new java.util.ArrayList<>();
		typeParameters.add(variable);
	}

	@Override
	public leekscript.common.TypeVariable getTypeParameter(String name) {
		if (typeParameters != null) {
			for (var variable : typeParameters) {
				if (variable.getName().equals(name)) return variable;
			}
		}
		return null;
	}

	/** Liste ordonnée des paramètres de type (`class Box<T, U>` -> [T, U]), ou null si non générique. */
	public java.util.ArrayList<leekscript.common.TypeVariable> getTypeParameters() {
		return typeParameters;
	}

	/** True si la classe déclare des paramètres de type (`class Box<T>`). */
	public boolean isGeneric() {
		return typeParameters != null;
	}

	@Override
	public String getCode() {
		String r = "class " + token.getWord();
		if (parentToken != null) {
			r += " extends " + parentToken.getWord();
		}
		r += " {\n";

		for (Entry<String, ClassDeclarationField> field : staticFields.entrySet()) {
			r += "\t" + field.getValue().level.toString().toLowerCase() + " static " + field.getValue().getType() + " " + field.getKey();
			if (field.getValue().expression != null) {
				r += " = " + field.getValue().expression.toString();
			}
			r += "\n";
		}
		r += "\n";

		for (var method : staticMethods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				if (version.getKey() == version.getValue().block.getMaxParameters()) {
					r += "\t" + version.getValue().level.toString().toLowerCase() + " static " + version.getValue().block.getType().returnType() + " " + method.getKey() + version.getValue().block.getCode();
				}
			}
			r += "\n";
		}
		r += "\n";

		for (Entry<String, ClassDeclarationField> field : fields.entrySet()) {
			r += "\t" + field.getValue().level.toString().toLowerCase() + " " + field.getValue().getType() + " " + field.getKey();
			if (field.getValue().expression != null) {
				r += " = " + field.getValue().expression.toString();
			}
			r += "\n";
		}
		r += "\n";

		for (var constructor : constructorsList) {
			r += "\t" + constructor.level.toString().toLowerCase() + " constructor" + constructor.block.getCode();
		}
		r += "\n";

		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				if (version.getKey() == version.getValue().block.getMaxParameters()) {
					r += "\t" + version.getValue().level.toString().toLowerCase() + " " + version.getValue().block.getType().returnType() + " " + method.getKey() + version.getValue().block.getCode();
				}
			}
			r += "\n";
		}

		r += "}";
		return r;
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	public void setParent(Token userClass) {
		this.parentToken = userClass;
		userClass.setExpression(this);
	}

	public boolean hasConstructor(int param_count) {
		return constructors.containsKey(param_count);
	}

	public ClassDeclarationMethod getConstructor(int param_count) {
		// Search existing constructor
		var constructor = constructors.get(param_count);
		if (constructor != null) return constructor;

		// Constructeurs hérités : la résolution runtime via findMethod descend la hiérarchie Java,
		// donc accepter cet arity au compile-time suffit (pas besoin d'émettre un init(n) sur la sous-classe).
		if (parent != null) {
			var inherited = parent.getConstructor(param_count);
			if (inherited != null) return inherited;
		}

		// If constructor has 0 parameters, return the default implicit one
		if (param_count == 0) {
			return new ClassDeclarationMethod(null, AccessLevel.PUBLIC);
		}
		return null;
	}

	public HashMap<Integer, ClassDeclarationMethod> getConstructors() {
		return constructors;
	}

	public void addConstructor(WordCompiler compiler, ClassMethodBlock constructor, AccessLevel level) throws LeekCompilerException {
		// System.out.println("add constructor " + constructor.getMinParameters() + " " + constructor.getMaxParameters());
		// On regarde si il n'y a pas déjà un constructeur de même arité
		for (int p = constructor.getMinParameters(); p <= constructor.getMaxParameters(); ++p) {
			if (constructors.containsKey(p)) {
				var l = compiler.getVersion() >= 4 ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				compiler.addError(new AnalyzeError(token, l, Error.DUPLICATED_CONSTRUCTOR));
			}
		}
		for (int p = constructor.getMinParameters(); p <= constructor.getMaxParameters(); ++p) {
			constructors.put(p, new ClassDeclarationMethod(constructor, level));
		}

		constructorsList.add(constructors.get(constructor.getMaxParameters()));
	}

	public void addMethod(WordCompiler compiler, Token token, ClassMethodBlock method, AccessLevel level) throws LeekCompilerException {
		// On regarde si il n'y a pas déjà une méthode statique du même nom
		if (staticMethods.containsKey(token.getWord())) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.DUPLICATED_METHOD));
		} else {
			var m = methods.get(token.getWord());
			if (m != null) {
				for (int p = method.getMinParameters(); p <= method.getMaxParameters(); ++p) {
					if (m.containsKey(p)) {
						var l = compiler.getVersion() >= 4 ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
						compiler.addError(new AnalyzeError(token, l, Error.DUPLICATED_METHOD));
						break;
					}
				}
			}
		}
		if (!methods.containsKey(token.getWord())) {
			methods.put(token.getWord(), new HashMap<>());
			methodVariables.put(token.getWord(), new LeekVariable(token, VariableType.METHOD, method.getType(), true));
		} else {
			methodVariables.get(token.getWord()).setType(Type.versions(methodVariables.get(token.getWord()).getType(), method.getType()));
		}
		for (int p = method.getMinParameters(); p <= method.getMaxParameters(); ++p) {
			methods.get(token.getWord()).put(p, new ClassDeclarationMethod(method, level));
		}
	}

	public boolean hasMethod(String name, int paramCount) {
		if (name.equals("constructor")) {
			return hasConstructor(paramCount);
		}
		return methods.containsKey(name + "_" + paramCount);
	}

	public boolean hasMethod(String name) {
		return methods.containsKey(name);
	}

	public void addStaticMethod(WordCompiler compiler, Token token, ClassMethodBlock method, AccessLevel level) throws LeekCompilerException {
		// On regarde si il n'y a pas déjà une méthode du même nom
		if (methods.containsKey(token.getWord())) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.DUPLICATED_METHOD));
		} else {
			var sm = staticMethods.get(token.getWord());
			if (sm != null) {
				for (int p = method.getMinParameters(); p <= method.getMaxParameters(); ++p) {
					if (sm.containsKey(p)) {
						var l = compiler.getVersion() >= 4 ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
						compiler.addError(new AnalyzeError(token, l, Error.DUPLICATED_METHOD));
						break;
					}
				}
			}
		}
		if (!staticMethods.containsKey(token.getWord())) {
			staticMethods.put(token.getWord(), new HashMap<>());
			staticMethodVariables.put(token.getWord(), new LeekVariable(token, VariableType.STATIC_METHOD, method.getType(), true));
		} else {
			staticMethodVariables.get(token.getWord()).setType(Type.versions(staticMethodVariables.get(token.getWord()).getType(), method.getType()));
		}
		for (int p = method.getMinParameters(); p <= method.getMaxParameters(); ++p) {
			staticMethods.get(token.getWord()).put(p, new ClassDeclarationMethod(method, level));
		}
	}

	public boolean hasStaticMethod(String name, int paramCount) {
		if (staticMethods.containsKey(name) && staticMethods.get(name).containsKey(paramCount)) {
			return true;
		}
		if (parent != null) {
			return parent.hasStaticMethod(name, paramCount);
		}
		return false;
	}

	public void addField(WordCompiler compiler, Token word, Expression expr, AccessLevel level, boolean isFinal, Type type) throws LeekCompilerException {
		addField(compiler, word, type, expr, level, isFinal);
	}

	public void addField(WordCompiler compiler, Token word, Type type, Expression expr, AccessLevel level, boolean isFinal) throws LeekCompilerException {
		if (hasMember(word.getWord())) {
			compiler.addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.FIELD_ALREADY_EXISTS, new String[] { word.getWord() }));
			return;
		}
		fields.put(word.getWord(), new ClassDeclarationField(word.getWord(), expr, level, isFinal, type));
		var variable = new LeekVariable(word, VariableType.FIELD, type, isFinal);
		variable.trackNarrowing(mainBlock);
		fieldVariables.put(word.getWord(), variable);
	}

	public void addStaticField(WordCompiler compiler, Token word, Expression expr, AccessLevel level, boolean isFinal, Type type) throws LeekCompilerException {
		addStaticField(compiler, word, type, expr, level, isFinal);
	}

	public void addStaticField(WordCompiler compiler, Token word, Type type, Expression expr, AccessLevel level, boolean isFinal) throws LeekCompilerException {
		if (hasStaticMember(word.getWord())) {
			compiler.addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.FIELD_ALREADY_EXISTS, new String[] { word.getWord() }));
			return;
		}
		staticFields.put(word.getWord(), new ClassDeclarationField(word.getWord(), expr, level, isFinal, type));
		// La classe déclarante est attachée à la variable canonique (et donc copiée
		// sur chaque référence par LeekVariable.preAnalyze) : le codegen peut ainsi
		// retrouver l'initialiseur d'un champ static final à inliner (ConstantFolder).
		var variable = new LeekVariable(word, VariableType.STATIC_FIELD, type, isFinal, this);
		variable.trackNarrowing(mainBlock);
		staticFieldVariables.put(word.getWord(), variable);
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la classe
		this.classValueType = new ClassValueType(this);
		// Le principal (et les classes internes) déclarent la CLASS-var dans le bloc
		// partagé (résolution par chaîne de blocs, byte-identique) ; une classe d'un
		// module importé NON (anti-fuite/collision) → résolue via getDefinedClass(module).
		// Dans les deux cas la variable lie le nom de la classe à sa déclaration : l'éditeur part de
		// ce jeton pour le survol et les références.
		variable = new LeekVariable(token, VariableType.CLASS, this.classValueType, this);
		if (module == null || module.isMain()) {
			compiler.getCurrentBlock().addVariable(variable);
		}
	}

	// La variable du nom de la classe, comme FunctionBlock.getVariable pour une fonction
	private LeekVariable variable;

	public LeekVariable getVariable() {
		return variable;
	}

	// Comme le corps d'une fonction (cf FunctionBlock.preAnalyze), une classe se résout dans
	// le module qui la déclare : ses imports et ses déclarations privées.
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		var initialModule = compiler.getModule();
		if (module != null) compiler.setModule(module);
		try {
			preAnalyzeInModule(compiler);
		} finally {
			compiler.setModule(initialModule);
		}
	}

	private void preAnalyzeInModule(WordCompiler compiler) throws LeekCompilerException {
		compiler.setCurrentClass(this);
		// Parent
		if (parentToken != null) {
			var parentVar = compiler.getCurrentBlock().getVariable(this.parentToken.getWord(), true);
			// Classe d'un module (importée ou privée) : absente des variables du bloc principal
			var parentDeclaration = parentVar == null && leekscript.common.LanguageFeature.MODULES.enabledIn(compiler.getVersion())
				? compiler.getMainBlock().getDefinedClass(parentToken.getWord(), compiler.getModule()) : null;
			if (parentVar == null && parentDeclaration == null) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION, new String[] {
					parentToken.getWord()
				}));
			} else if (parentVar != null && parentVar.getVariableType() != VariableType.CLASS) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION, new String[] {
					parentToken.getWord()
				}));
			} else {
				if (parentDeclaration == null) parentDeclaration = parentVar.getClassDeclaration();
				var current = parentDeclaration;
				boolean ok = true;
				while (current != null) {
					if (current == this) {
						compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.EXTENDS_LOOP));
						ok = false;
						break;
					}
					current = current.getParent();
				}
				if (ok) {
					this.parent = parentDeclaration;
				}
			}
		}
		// Fields
		for (var field : fields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.preAnalyze(compiler);
			}
		}
		// Static fields
		var previousBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(staticInitBlock);
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.preAnalyze(compiler);
			}
		}
		compiler.setCurrentBlock(previousBlock);

		for (var entry : constructors.entrySet()) {
			if (entry.getValue().block != null && entry.getKey() == entry.getValue().block.getMaxParameters()) {
				entry.getValue().block.preAnalyze(compiler);
			}
		}
		for (var method : methods.values()) {
			for (var entry : method.entrySet()) {
				if (entry.getKey() == entry.getValue().block.getMaxParameters()) {
					entry.getValue().block.preAnalyze(compiler);
				}
			}
		}
		for (var method : staticMethods.values()) {
			for (var entry : method.entrySet()) {
				if (entry.getKey() == entry.getValue().block.getMaxParameters()) {
					entry.getValue().block.preAnalyze(compiler);
				}
			}
		}
		compiler.setCurrentClass(null);
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		var initialModule = compiler.getModule();
		if (module != null) compiler.setModule(module);
		try {
			analyzeInModule(compiler);
		} finally {
			compiler.setModule(initialModule);
		}
	}

	private void analyzeInModule(WordCompiler compiler) throws LeekCompilerException {
		compiler.setCurrentClass(this);

		// Fields
		for (var field : fields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.analyze(compiler);

				// Vérification du type de l'expression
				var fieldType = field.getValue().getType();
				var valueType = field.getValue().expression.getType();
				var cast = compiler.getMainBlock().strictContainerCast(fieldType.accepts(valueType), fieldType, valueType);
				if (cast.ordinal() > CastType.UPCAST.ordinal()) {

					var level = compiler.getMainBlock().isStrict() && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					var error = cast == CastType.INCOMPATIBLE ? Error.ASSIGNMENT_INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION_VARIABLE;

					compiler.addError(new AnalyzeError(field.getValue().expression.getLocation(), level, error, new String[] {
						field.getValue().expression.toString(),
						valueType.toString(),
						field.getValue().name,
						fieldType.toString(),
					}));
				}
			}
		}

		// Static fields
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.analyze(compiler);

				var fieldType = field.getValue().getType();
				var valueType = field.getValue().expression.getType();
				var cast = compiler.getMainBlock().strictContainerCast(fieldType.accepts(valueType), fieldType, valueType);
				if (cast.ordinal() > CastType.UPCAST.ordinal()) {

					var level = compiler.getMainBlock().isStrict() && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					var error = cast == CastType.INCOMPATIBLE ? Error.ASSIGNMENT_INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION_VARIABLE;

					compiler.addError(new AnalyzeError(field.getValue().expression.getLocation(), level, error, new String[] {
						field.getValue().expression.toString(),
						valueType.toString(),
						field.getValue().name,
						fieldType.toString(),
					}));
				}
			}
		}

		// Une méthode à paramètres optionnels est enregistrée sous chacune de ses arités avec
		// le même bloc. L'analyse n'est pas idempotente (appels déjà résolus, coût des `if`) :
		// chaque bloc n'est analysé qu'une fois.
		var analyzed = new HashSet<ClassMethodBlock>();
		for (var constructor : constructors.values()) {
			if (constructor.block != null && analyzed.add(constructor.block)) {
				constructor.block.analyze(compiler);
			}
		}
		// Tout constructeur d'une classe fille émet un `super.init()` implicite (cf. codegen),
		// y compris quand la fille n'en déclare aucun. Si le constructeur sans argument du
		// parent est privé, cet appel est illégal : `new Fille()` exécutait le constructeur
		// privé sans un mot, alors que `new Parent()` était bien refusé (#2760). Avertissement
		// hors strict pour ne pas refuser des IA qui compilaient.
		if (parent != null && !parent.internal) {
			// Lookup NON hérité : le `super.init()` vise l'init du parent direct. Passer par
			// getConstructor(0), qui remonte la hiérarchie, signalait aussi les classes
			// intermédiaires qui ne déclarent aucun constructeur — en les nommant à tort, et
			// de façon dépendante de l'ordre de déclaration. Chaque classe est signalée pour
			// son propre appel implicite, une fois, avec le bon nom.
			var parentConstructor = parent.getConstructors().get(0);
			if (parentConstructor != null && parentConstructor.block != null && parentConstructor.level == AccessLevel.PRIVATE) {
				var level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				compiler.addError(new AnalyzeError(token, level, Error.PRIVATE_CONSTRUCTOR, new String[] { parent.getName() }));
			}
		}

		// Ajout du constructeur à 0 argument par défaut en public
		if (!constructors.containsKey(0)) {
			constructors.put(0, new ClassDeclarationMethod(null, AccessLevel.PUBLIC));
		}

		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				var block = version.getValue().block;
				if (analyzed.add(block)) {
					emitMethodAnnotationWarnings(block, method.getKey(), compiler);
					block.analyze(compiler);
				}

				// Méthode surchargée ?
				var current = parent;
				boolean foundInParent = false;
				while (current != null) {
					var parentMethod = current.methods.get(method.getKey());
					if (parentMethod != null) {
						var parentVersion = parentMethod.get(version.getKey());
						if (parentVersion != null) {
							foundInParent = true;
							// Compatibilité de type entre la méthode surchargée et celle du parent,
							// comme en Java :
							//  - le type de retour peut être réduit (covariance) : l'enfant peut
							//    retourner un sous-type de ce que retourne le parent ;
							//  - les types des paramètres restent invariants (doivent être identiques).
							var childType = block.getType();
							var parentType = parentVersion.block.getType();
							var parentReturn = parentType.returnType();
							var childReturn = childType.returnType();

							// Type de retour : identique, ou réduit (covariance). On autorise la
							// réduction vers un type référence, pour rester cohérent avec Java qui
							// autorise la covariance entre types référence mais pas entre primitifs
							// (ex. double -> long). Cela couvre les classes (réduction vers une
							// sous-classe), mais aussi les conteneurs génériques (Array<any> ->
							// Array<integer>, Set, Map...) et les autres types objet (string...) :
							// tous ces cas sont de la covariance saine et déjà reconnus par
							// accepts(). Le parent peut être `any` (le type top) ou un type
							// référence plus large. On interdit seulement de réduire vers un
							// primitif (integer/real/boolean), interdit en Java aussi. Note : les
							// méthodes générées retournent toujours Object, donc cette restriction
							// est purement sémantique et n'est pas imposée par le code généré.
							var returnCast = parentReturn.accepts(childReturn);
							boolean childIsPrimitive = childReturn == Type.INT || childReturn == Type.REAL || childReturn == Type.BOOL;
							boolean compatible = returnCast == CastType.EQUALS
								|| (returnCast == CastType.UPCAST && !childIsPrimitive);

							// Paramètres : chaque type doit rester identique (invariant).
							if (compatible) {
								var parentArgs = parentType.getArguments();
								var childArgs = childType.getArguments();
								for (int a = 0; a < Math.min(parentArgs.size(), childArgs.size()); ++a) {
									if (parentArgs.get(a).accepts(childArgs.get(a)) != CastType.EQUALS) {
										compatible = false;
										break;
									}
								}
							}

							if (!compatible) {
								compiler.addError(new AnalyzeError(block.getLocation(), AnalyzeErrorLevel.ERROR, Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE, new String[] {
									childType.toString(),
									parentType.toString()
								}));
							}
							// Une override ne peut pas réduire la visibilité (LSP : ce qui est public dans le parent doit le rester).
							// Erreur dure en LS5 ; warning en v1-4 pour ne pas casser les IAs existantes (#3781).
							if (version.getValue().level.ordinal() > parentVersion.level.ordinal()) {
								var level = LanguageFeature.STRICT_OVERRIDE_VISIBILITY.enabledIn(compiler.getVersion()) ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
								compiler.addError(new AnalyzeError(block.getLocation(), level, Error.OVERRIDDEN_METHOD_NARROWER_VISIBILITY, new String[] {
									method.getKey(),
									version.getValue().level.toString(),
									parentVersion.level.toString()
								}));
							}
							break;
						}
					}
					current = current.parent;
				}
				// @override : error if no parent method found
				if (!foundInParent && block.hasAnnotation(Annotation.OVERRIDE)) {
					compiler.addError(new AnalyzeError(block.getLocation(), AnalyzeErrorLevel.ERROR, Error.ANNOTATION_OVERRIDE_NO_PARENT, new String[] { method.getKey() }));
				}
			}
		}
		for (var staticMethod : staticMethods.entrySet()) {
			for (var version : staticMethod.getValue().values()) {
				if (analyzed.add(version.block)) {
					emitMethodAnnotationWarnings(version.block, staticMethod.getKey(), compiler);
					version.block.analyze(compiler);
				}
			}
		}
		compiler.setCurrentClass(null);
	}

	private void emitMethodAnnotationWarnings(ClassMethodBlock block, String name, WordCompiler compiler) throws LeekCompilerException {
		if (block.hasAnnotation(Annotation.TODO)) {
			compiler.addError(new AnalyzeError(block.getLocation(), AnalyzeErrorLevel.WARNING, Error.ANNOTATION_TODO, new String[] { name }));
		}
	}

	/**
	 * Dans la classe d'un module, l'instance qui englobe la classe parente, à nommer dans l'appel à
	 * `super` quand elle n'est pas celle qui englobe cette classe : l'IA pour NativeObjectLeekValue
	 * (classe interne d'AI), l'instance d'un autre module pour une classe importée. Null sinon.
	 */
	private String superOuterInstance(JavaWriter writer) {
		if (parent == null) return writer.getModule() == null ? null : writer.getAIThis();
		var field = writer.ownerField(parent.getModule());
		return field == null ? null : writer.getClassName() + ".this." + field;
	}

	public void declareJava(MainLeekBlock mainblock, JavaWriter writer) {
		mainblock.getWordCompiler().setCurrentClass(this);
		String className = "u_" + getInternalName();

		// Declare the actuel class
		var parentName = parent == null ? "NativeObjectLeekValue" : parent.getJavaName(mainblock);
		writer.addLine("public class " + className + " extends " + parentName + " {");

		// Fields
		for (var field : fields.entrySet()) {
			if (field.getValue().isFinal) {
				writer.addCode("@Final ");
			}
			if (field.getValue().level == AccessLevel.PROTECTED) {
				writer.addCode("@Protected ");
			} else if (field.getValue().level == AccessLevel.PRIVATE) {
				writer.addCode("@Private ");
			}
			writer.addCode("public " + field.getValue().getType().getJavaPrimitiveName(mainblock.getVersion()) + " " + field.getKey());
			// if (field.getValue().expression != null) {
			// 	writer.addCode(" = ");
			// 	field.getValue().expression.writeJavaCode(mainblock, writer);
			// }
			writer.addLine(";");
			// writer.addCode(", AccessLevel." + field.getValue().level + ", " + field.getValue().isFinal);
			// writer.addLine(");");
		}

		// Constructeur par défaut
		writer.addLine("public " + className + "() throws LeekRunException {");
		if (parent != null && parent.internal) {
			writer.addLine("super(" + writer.getAIThis() + ");");
		} else {
			var outer = superOuterInstance(writer);
			if (outer != null) writer.addLine(outer + ".super();");
		}
		// Le coût de base de l'instance une seule fois, par la classe racine (une classe interne
		// parente, comme Array, compte le sien dans son constructeur)
		int ram = (parent == null ? AI.INSTANCE_BASE_RAM : 0) + 2 * fieldVariables.size();
		if (ram > 0) {
			writer.addLine("allocateRAM(this, " + ram + ");");
		}
		for (var field : fields.entrySet()) {
			Expression expr = field.getValue().expression;
			if (expr != null) {
				writer.addCode(field.getKey());
				writer.addCode(" = ");
				if (field.getValue().getType() != Type.ANY) {
					writer.compileConvertTyped(mainblock, 0, expr, field.getValue().getType(), false);
				} else {
					expr.writeJavaCode(mainblock, writer, false);
				}
				writer.addLine(";");
			} else {
				// Un champ Java primitif (integer, real, boolean) vaut déjà 0 / 0.0 / false
				var implicit = implicitFieldDefault(mainblock, writer, field.getValue().getType());
				if (implicit != null && !field.getValue().getType().isPrimitive()) {
					writer.addLine(field.getKey() + " = " + implicit + ";");
				}
			}
		}
		writer.addLine("}");

		// Constructeur par copie
		writer.addLine("public " + className + "(" + className + " o, int level) throws LeekRunException {");
		if (parent != null) {
			if (parent.internal) {
				writer.addLine("super(" + writer.getAIThis() + ", o, level);");
			} else {
				var outer = superOuterInstance(writer);
				writer.addLine((outer == null ? "" : outer + ".") + "super(o, level);");
			}
		} else {
			var outer = superOuterInstance(writer);
			if (outer != null) writer.addLine(outer + ".super();");
		}
		for (var field : fields.entrySet()) {
			writer.addLine("this." + field.getKey() + " = level == 1 ? o." + field.getKey() + " : (" + field.getValue().getType().getJavaPrimitiveName(mainblock.getVersion()) + ") copy(o." + field.getKey() + ", level - 1);");
		}
		writer.addLine("}");

		// Vrais constructeurs
		// Le corps est extrait dans une méthode privée <className>$body_<maxArity>
		// pour que la délégation depuis init(args) reste non virtuelle : sinon
		// le fall-through `init(default)` dispatcherait dynamiquement vers une
		// override en sous-classe et boucle via super.init() (issue #3159).
		for (var ctor : constructorsList) {
			final var bodyBlock = ctor.block;

			mainblock.getWordCompiler().setCurrentBlock(bodyBlock);
			writer.currentBlock = bodyBlock;

			writer.addCode("private Object " + className + "$body_" + bodyBlock.getMaxParameters() + "(");
			for (int a = 0; a < bodyBlock.getMaxParameters(); ++a) {
				var arg = bodyBlock.getParametersDeclarations().get(a);
				var letter = arg.isCaptured() ? "p" : "u";
				if (a > 0) writer.addCode(", ");
				writer.addCode(arg.getType().getJavaPrimitiveName(mainblock.getVersion()));
				writer.addCode(" " + letter + "_" + arg.getToken());
			}
			writer.addLine(") throws LeekRunException {");

			// Captures : box les paramètres capturés
			for (int a = 0; a < bodyBlock.getParametersDeclarations().size(); ++a) {
				var arg = bodyBlock.getParametersDeclarations().get(a);
				if (arg.isCaptured()) {
					writer.addCode("final var u_" + arg.getToken() + " = new Box<" + arg.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
					writer.addCode("p_" + arg.getToken());
					writer.addLine(");");
				}
			}

			bodyBlock.writeJavaCode(mainblock, writer, false);
			writer.addLine("}");

			writer.currentBlock = null;
			mainblock.getWordCompiler().setCurrentBlock(null);
		}

		for (var construct : constructors.entrySet()) {

			final var block = construct.getValue().block;

			mainblock.getWordCompiler().setCurrentBlock(block);
			writer.currentBlock = block;

			writer.addCode("public Object init(");
			if (block != null) {
				for (int a = 0; a < construct.getKey(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					var letter = arg.isCaptured() ? "p" : "u";
					if (a > 0) writer.addCode(", ");
					writer.addCode(arg.getType().getJavaPrimitiveName(mainblock.getVersion()));
					writer.addCode(" " + letter + "_" + arg.getToken());
				}
			}
			writer.addLine(") throws LeekRunException {");

			if (parent != null && !parent.internal && (block == null || !block.hasExplicitSuperCall())) {
				writer.addLine("super.init();");
			}

			// Émis dans l'ordre pour qu'une valeur par défaut puisse référencer
			// les paramètres précédents (ex: constructor(x = 1, y = x)).
			if (block != null) {
				for (int a = 0; a < block.getParametersDeclarations().size(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					if (arg.isCaptured()) {
						writer.addCode("final var u_" + arg.getToken() + " = new Box<" + arg.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
						if (a < construct.getKey()) {
							writer.addCode("p_" + arg.getToken());
						} else {
							writer.compileDefaultValue(mainblock, a, block.getDefaultValues().get(a), arg.getType());
							writer.addCode(", " + block.getDefaultValues().get(a).operations);
						}
						writer.addLine(");");
					} else {
						if (a >= construct.getKey()) {
							var defaultValue = block.getDefaultValues().get(a);
							// Nom primitif (real/int/bool) : `double u_x` et non `Double u_x`, que
							// l'initialisation par un `long` ferait crasher (#4703).
							writer.addCode("final " + arg.getType().getJavaPrimitiveName(mainblock.getVersion()) + " u_" + arg.getName() + " = ");
							writer.compileDefaultValue(mainblock, a, defaultValue, arg.getType());
							writer.addLine(";");
							writer.addCounter(defaultValue.operations);
						}
					}
				}
			}

			if (block != null) {
				writer.addCode("return " + className + "$body_" + block.getMaxParameters() + "(");
				for (int a = 0; a < block.getMaxParameters(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					if (a > 0) writer.addCode(", ");
					if (arg.isCaptured()) {
						writer.addCode("u_" + arg.getName() + ".get()");
					} else {
						writer.addCode("u_" + arg.getName());
					}
				}
				writer.addLine(");");
			} else {
				writer.addLine("return null;");
			}
			writer.addLine("}");

			writer.currentBlock = null;
			mainblock.getWordCompiler().setCurrentBlock(null);
		}

		// Déclaration des méthodes
		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				final var block = version.getValue().block;
				mainblock.getWordCompiler().setCurrentBlock(block);
				writer.currentBlock = block;

				String methodName = method.getKey();

				if (version.getValue().level == AccessLevel.PROTECTED) {
					writer.addCode("@Protected ");
				} else if (version.getValue().level == AccessLevel.PRIVATE) {
					writer.addCode("@Private ");
				}
				writer.addCode("public " + block.getType().returnType().getJavaPrimitiveName(mainblock.getVersion()) + " u_" + methodName + "(");
				for (int a = 0; a < version.getKey(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					var letter = arg.isCaptured() ? "p" : "u";
					if (a > 0) writer.addCode(", ");
					writer.addCode(arg.getType().getJavaPrimitiveName(mainblock.getVersion()));
					writer.addCode(" " + letter + "_" + arg.getToken());
				}
				writer.addLine(") throws LeekRunException {");
				if (parent != null) {
					// writer.addLine("final var u_super = u_" + parent.token.getWord() + ";");
				}

				// Captures
				for (int a = 0; a < block.getParametersDeclarations().size(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					if (arg.isCaptured()) {
						writer.addCode("final var u_" + arg.getToken() + " = new Box<" + arg.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
						if (a < version.getKey()) {
							writer.addCode("p_" + arg.getToken());
						} else {
							writer.compileDefaultValue(mainblock, a, block.getDefaultValues().get(a), arg.getType());
							writer.addCode(", " + block.getDefaultValues().get(a).operations);
						}
						writer.addLine(");");
					} else {
						// Valeur par défaut
						if (a >= version.getKey()) {
							var defaultValue = block.getDefaultValues().get(a);
							// Nom primitif (real/int/bool) : `double u_x` et non `Double u_x`, que
							// l'initialisation par un `long` ferait crasher (#4703).
							writer.addCode("final " + arg.getType().getJavaPrimitiveName(mainblock.getVersion()) + " u_" + arg.getName() + " = ");
							writer.compileDefaultValue(mainblock, a, defaultValue, arg.getType());
							writer.addLine(";");
							writer.addCounter(defaultValue.operations);
						}
					}
				}
				// writer.addCounter(1);
				// Sous-version
				if (version.getKey() < block.getMaxParameters()) {
					writer.addCode("return u_" + method.getKey() + "(");
					for (int a = 0; a < block.getParametersDeclarations().size(); ++a) {
						var arg = block.getParametersDeclarations().get(a);
						if (a > 0)
							writer.addCode(", ");
						if (arg.isCaptured()) {
							writer.addCode("u_" + arg.getName() + ".get()");
						} else {
							writer.addCode("u_" + arg.getName());
						}
					}
					writer.addLine(");");
				} else {
					version.getValue().block.writeJavaCode(mainblock, writer, false);
				}
				writer.addLine("}");
				writer.currentBlock = null;
				mainblock.getWordCompiler().setCurrentBlock(null);
			}
		}

		// End of actual class
		writer.addLine("}");

		// Declare the class as a field of the AI
		writer.addLine("public ClassLeekValue " + className + " = new ClassLeekValue(" + writer.getAI() + ", \"" + token.getWord() + "\", null, " + className + ".class)" + (writer.getModule() != null ? ".inModule(this)" : "") + ";");
		for (var field : boxedStaticFields()) {
			writer.addLine("public ObjectVariableValue " + staticFieldBoxName(field) + ";");
		}

		// Constructeur
		writer.addLine("public " + className + " new_" + className + "(Object... args) throws LeekRunException {");
		writer.addLine("return (" + className + ") execute(" + className + ", args);");
		writer.addLine("}");

		// Static methods
		for (Entry<String, HashMap<Integer, ClassDeclarationMethod>> method : staticMethods.entrySet()) {
			for (Entry<Integer, ClassDeclarationMethod> version : method.getValue().entrySet()) {
				final var block = version.getValue().block;
				mainblock.getWordCompiler().setCurrentBlock(block);
				writer.currentBlock = block;
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode(writer.privateMember() + "final " + block.getType().returnType().getJavaPrimitiveName(mainblock.getVersion()) + " " + methodName + "(");
				int i = 0;
				for (int a = 0; a < version.getKey(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					if (i++ > 0) writer.addCode(", ");
					var letter = arg.isCaptured() ? "p" : "u";
					writer.addCode(arg.getType().getJavaPrimitiveName(mainblock.getVersion()));
					writer.addCode(" " + letter + "_" + arg.getToken());
				}
				writer.addLine(") throws LeekRunException {");
				if (parent != null) {
					// writer.addLine("final var u_super = u_" + parent.token.getWord() + ";");
				}

				// Captures
				for (int a = 0; a < block.getParametersDeclarations().size(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					if (arg.isCaptured()) {
						writer.addCode("final var u_" + arg.getToken() + " = new Box<" + arg.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
						if (a < version.getKey()) {
							writer.addCode("p_" + arg.getToken());
						} else {
							writer.compileDefaultValue(mainblock, a, block.getDefaultValues().get(a), arg.getType());
							writer.addCode(", " + block.getDefaultValues().get(a).operations);
						}
						writer.addLine(");");
					} else {
						// Valeur par défaut
						if (a >= version.getKey()) {
							var defaultValue = block.getDefaultValues().get(a);
							// Nom primitif (real/int/bool) : `double u_x` et non `Double u_x`, que
							// l'initialisation par un `long` ferait crasher (#4703).
							writer.addCode("final " + arg.getType().getJavaPrimitiveName(mainblock.getVersion()) + " u_" + arg.getName() + " = ");
							writer.compileDefaultValue(mainblock, a, defaultValue, arg.getType());
							writer.addLine(";");
							writer.addCounter(defaultValue.operations);
						}
					}
				}

				// Sous-version
				if (version.getKey() < block.getMaxParameters()) {
					writer.addCode("return " + className + "_" + method.getKey() + "_" + block.getMaxParameters() + "(");
					for (int a = 0; a < block.getParametersDeclarations().size(); ++a) {
						var arg = block.getParametersDeclarations().get(a);
						if (a > 0) writer.addCode(", ");

						if (arg.isCaptured()) {
							writer.addCode("u_" + arg.getName() + ".get()");
						} else {
							writer.addCode("u_" + arg.getName());
						}
					}
					writer.addLine(");");
				} else {
					// Version complète
					version.getValue().block.writeJavaCode(mainblock, writer, false);
				}
				writer.addLine("}");
				writer.currentBlock = null;
				mainblock.getWordCompiler().setCurrentBlock(null);
			}
		}

		mainblock.getWordCompiler().setCurrentClass(null);
	}

	private String getJavaName(MainLeekBlock block) {
		if (internal) {
			if (block.getVersion() <= 3 && token.getWord().equals("Array")) {
				return "LegacyArrayLeekValue";
			}
			switch (token.getWord()) {
				case "Array": return "ArrayLeekValue";
				case "Map": return "MapLeekValue";
			}
		}
		return getJavaTypeName();
	}

	private String getClassName(MainLeekBlock block) {
		if (internal) {
			if (block.getVersion() <= 3 && token.getWord().equals("Array")) {
				return "legacyArrayClass";
			}
			return token.getWord().toLowerCase() + "Class";
		}
		return "u_" + getInternalName();
	}

	public void defineClass(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addLine(writer.ref(module) + "defineClass_" + getInternalName() + "();");
	}

	public void writeDefineClass(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addLine(writer.privateMember() + "void defineClass_" + getInternalName() + "() throws LeekRunException {");
		createJava(mainblock, writer);
		writer.addLine("}");
	}

	public void createJava(MainLeekBlock mainblock, JavaWriter writer) {

		mainblock.getWordCompiler().setCurrentClass(this);

		// Create the class in the constructor of the AI
		String className = "u_" + getInternalName();

		if (parent != null) {
			writer.addLine(className + ".setParent(" + (parent.internal ? "" : writer.ref(parent.getModule())) + parent.getClassName(mainblock) + ");");
		}

		writer.addCode(className + ".initFields = new FunctionLeekValue(0) {");
		writer.addLine("public Object run(AI ai, Object u_this, Object... values) throws LeekRunException {");
		ClassDeclarationInstruction current = this;
		ArrayList<ClassDeclarationInstruction> classes = new ArrayList<>();
		while (current != null) {
			classes.add(current);
			current = current.parent;
		}
		for (int i = classes.size() - 1; i >= 0; --i) {
			// var clazz = classes.get(i);
			// for (var field : clazz.fields.entrySet()) {
				// writer.addCode("u_this.addField(" + writer.getAIThis() + ", \"" + field.getKey() + "\", ");
				// if (field.getValue().expression != null) {
				// 	field.getValue().expression.writeJavaCode(mainblock, writer);
				// } else {
				// 	writer.addCode("null");
				// }
				// writer.addLine(", AccessLevel." + field.getValue().level + ", " + field.getValue().isFinal + ");");
			// }
		}
		writer.addLine("return null;");
		writer.addLine("}};");

		writeFields(mainblock, writer, className);

		// Static methods
		for (var method : staticMethods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode(className);
				writer.addCode(".addStaticMethod(\"" + method.getKey() + "\", " + version.getKey() + ", new FunctionLeekValue(1, \"#Function " + token.getWord() + "." + method.getKey() + "\") { public Object run(AI ai, Object thiz, Object... args) throws LeekRunException { return " + methodName + "(");
				int i = 0;
				for (var a = 0; a < version.getKey(); ++a) {
					if (i > 0) writer.addCode(", ");
					writer.addCode("(" + version.getValue().block.getParametersDeclarations().get(a).getType().getJavaName(mainblock.getVersion()) + ") ");
					writer.addCode("args[" + i + "]");
					i++;
				}
				writer.addLine("); }}, AccessLevel." + version.getValue().level.name() + ");");
			}
			writer.addCode(className);
			writer.addLine(".addGenericStaticMethod(\"" + method.getKey() + "\");");
		}

		// Méthodes
		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				writer.addCode(className);
				writer.addLine(".addMethod(\"" + method.getKey() + "\", " + version.getKey() + ", new FunctionLeekValue(0, \"#Function " + token.getWord() + "." + method.getKey() + "\") { public Object run(AI ai, Object thiz, Object... args) throws LeekRunException {");
				writer.addCode("return ((" + className + ") thiz).u_" + method.getKey() + "(");
				int i = 0;
				for (var a = 0; a < version.getKey(); ++a) {
					if (a > 0) writer.addCode(", ");
					writer.addCode("(" + version.getValue().block.getParametersDeclarations().get(a).getType().getJavaName(mainblock.getVersion()) + ") ");
					writer.addCode("args[" + i++ + "]");
				}
				writer.addLine("); }}, AccessLevel." + version.getValue().level.name() + ");");
			}
			writer.addCode(className);
			writer.addLine(".addGenericMethod(\"" + method.getKey() + "\");");
		}
		mainblock.getWordCompiler().setCurrentClass(null);
	}

	public void createStaticFields(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addLine(writer.ref(module) + "createStaticClass_" + getInternalName() + "();");
	}

	/** Types dont la valeur par défaut n'est pas null (cf. champs d'instance, qui sont
	 * de vrais champs Java primitifs). */
	private static boolean hasZeroDefault(Type type) {
		return type == Type.INT || type == Type.REAL || type == Type.BOOL || type == Type.BIG_INT;
	}

	/**
	 * v5 : la valeur d'un champ typé sans initialiseur, celle d'une variable locale du même type
	 * (`Set<A> s` → set vide, `string t` → ""), null si ce type n'a pas d'autre valeur par
	 * défaut (classes, fonctions, any, types nullables : `Set<A> | null`, `A?`). v1-4 : null
	 * pour tout ce qui n'est pas un champ Java primitif (#4635).
	 */
	private static String implicitFieldDefault(MainLeekBlock mainblock, JavaWriter writer, Type type) {
		if (!LanguageFeature.FIELD_TYPE_DEFAULT.enabledIn(mainblock.getVersion())) return null;
		var value = type.getDefaultValue(writer, mainblock.getVersion());
		return value.equals("null") ? null : value;
	}

	public void writeCreateStaticFields(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addLine(writer.privateMember() + "void createStaticClass_" + getInternalName() + "() throws LeekRunException {");

		mainblock.getWordCompiler().setCurrentClass(this);
		mainblock.getWordCompiler().setCurrentBlock(staticInitBlock);

		// Create the class in the constructor of the AI
		String className = "u_" + getInternalName();

		// First create all static fields
		for (var field : staticFields.entrySet()) {
			writer.addCode(className);
			writer.addCode(".addStaticField(" + writer.getAIThis() + ", \"" + field.getKey() + "\", ");
			// Champ constante de compilation : créé directement avec son littéral.
			// La phase « null avant initField » disparaît pour ces champs : les
			// lectures inlinées (ConstantFolder) restent donc cohérentes avec le
			// champ runtime même pendant l'init statique, quel que soit l'ordre de
			// déclaration des champs et des classes.
			var constant = ConstantFolder.staticFinalLiteral(this, field.getKey(), this);
			if (constant != null) {
				constant.writeJavaCode(mainblock, writer, false);
			} else {
				writer.addCode("null");
			}
			writer.addCode(", AccessLevel." + field.getValue().level.name() + ", " + field.getValue().isFinal);
			writer.addLine(");");
		}
		// Dans une méthode à part, pour que createStaticClass ne grossisse que d'un appel (64 Ko de bytecode par méthode)
		var boxed = boxedStaticFields();
		if (!boxed.isEmpty()) writer.addLine("linkStaticFields_" + getInternalName() + "();");

		writer.addLine("}");

		if (!boxed.isEmpty()) {
			writer.addLine(writer.privateMember() + "void linkStaticFields_" + getInternalName() + "() {");
			for (var field : boxed) {
				// La map plutôt qu'une méthode nouvelle : le code compilé tourne aussi sur un runtime plus ancien
				writer.addLine(staticFieldBoxName(field) + " = " + className + ".staticFields.get(\"" + field + "\");");
			}
			writer.addLine("}");
		}
		mainblock.getWordCompiler().setCurrentClass(null);
		mainblock.getWordCompiler().setCurrentBlock(null);
	}

	public void initializeStaticFields(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addLine(writer.ref(module) + "initClass_" + getInternalName() + "();");
	}

	public void writeInitializeStaticFields(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addLine(writer.privateMember() + "void initClass_" + getInternalName() + "() throws LeekRunException {");

		mainblock.getWordCompiler().setCurrentClass(this);
		mainblock.getWordCompiler().setCurrentBlock(staticInitBlock);

		// Create the class in the constructor of the AI
		String className = "u_" + getInternalName();

		// Second assign values for fields with values
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				writer.addCode(className);
				writer.addCode(".initField(\"" + field.getKey() + "\", ");
				writer.compileConvert(mainblock, 0, field.getValue().expression, field.getValue().getType(), false);
				writer.addLine(");");
			} else if (hasZeroDefault(field.getValue().getType()) || implicitFieldDefault(mainblock, writer, field.getValue().getType()) != null) {
				// Un champ statique est stocké dans une Box, qui vaut null tant que
				// rien ne l'initialise — alors qu'un champ d'instance est un vrai champ
				// Java et vaut donc 0 / 0.0 / false. Sans ça, `class Z { static integer
				// b; }` puis `Z.b` lit null et le code généré, qui déballe un long,
				// plante (IMPOSSIBLE_CAST). En v1-4, les types qui acceptent null (string,
				// Array, classes…) gardent null, comme les champs d'instance (#4908) ; en v5
				// ils reçoivent la valeur par défaut d'une locale (#4635).
				writer.addCode(className);
				writer.addCode(".initField(\"" + field.getKey() + "\", ");
				writer.addCode(field.getValue().getType().getDefaultValue(writer, mainblock.getVersion()));
				writer.addLine(");");
			}
		}

		mainblock.getWordCompiler().setCurrentClass(null);
		mainblock.getWordCompiler().setCurrentBlock(null);
		writer.addLine("}");
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {

	}


	// ---- Backend bytecode : la classe interne `u_X`, ses membres dans l'IA, sa définition ----

	private String bytecodeName() {
		return "u_" + getInternalName();
	}

	private static List<String> bytecodeParameterNames(MainLeekBlock mainblock, ClassMethodBlock block, int count) {
		var names = new ArrayList<String>();
		for (int a = 0; a < count; a++) names.add(block.getParametersDeclarations().get(a).getType().getJavaPrimitiveName(mainblock.getVersion()));
		return names;
	}

	private static List<Class<?>> bytecodeParameters(MainLeekBlock mainblock, BytecodeWriter writer, ClassMethodBlock block, int count) {
		var types = new ArrayList<Class<?>>();
		for (var name : bytecodeParameterNames(mainblock, block, count)) types.add(writer.typeNamed(name));
		return types;
	}

	/** Le talon de la classe, interne à la classe racine `owner` : ses champs et les signatures de ses méthodes, comme declareJava les écrit. */
	public ClassStubs.ClassSpec bytecodeSpec(MainLeekBlock mainblock, String owner) {
		int version = mainblock.getVersion();
		var name = bytecodeName();
		var fieldSpecs = new ArrayList<ClassStubs.FieldSpec>();
		for (var field : fields.entrySet()) fieldSpecs.add(new ClassStubs.FieldSpec(field.getKey(), field.getValue().getType().getJavaPrimitiveName(version)));
		var methodSpecs = new ArrayList<ClassStubs.MethodSpec>();
		for (var ctor : constructorsList) {
			var block = ctor.block;
			methodSpecs.add(new ClassStubs.MethodSpec(name + "$body_" + block.getMaxParameters(), "Object", bytecodeParameterNames(mainblock, block, block.getMaxParameters()), ClassFile.ACC_PRIVATE));
		}
		for (var construct : constructors.entrySet()) {
			var block = construct.getValue().block;
			methodSpecs.add(new ClassStubs.MethodSpec("init", "Object", block == null ? List.of() : bytecodeParameterNames(mainblock, block, construct.getKey()), ClassFile.ACC_PUBLIC));
		}
		for (var method : methods.entrySet()) {
			for (var v : method.getValue().entrySet()) {
				var block = v.getValue().block;
				methodSpecs.add(new ClassStubs.MethodSpec("u_" + method.getKey(), block.getType().returnType().getJavaPrimitiveName(version), bytecodeParameterNames(mainblock, block, v.getKey()), ClassFile.ACC_PUBLIC));
			}
		}
		var superName = parent == null ? "NativeObjectLeekValue" : parent.getJavaName(mainblock);
		return new ClassStubs.ClassSpec(owner, name, superName, fieldSpecs, methodSpecs, List.of(List.of(), List.of(name, "int")));
	}

	/**
	 * Paramètres d'une méthode : les `count` premiers lus dans leurs emplacements, les suivants
	 * initialisés par leur valeur par défaut (`final T u_x = …;` puis ses opérations).
	 */
	private static void bytecodeArguments(MainLeekBlock mainblock, BytecodeWriter writer, ClassMethodBlock block, int count, List<Class<?>> parameters) {
		var declarations = block.getParametersDeclarations();
		for (int a = 0; a < declarations.size(); a++) {
			var arg = declarations.get(a);
			if (arg.isCaptured()) {
				// `final var u_x = new Box<T>(ai, p_x)` ou `(ai, <défaut>, ops)`
				var javaType = writer.boxGeneric(leekscript.runner.values.Box.class, arg.getType().getJavaName(mainblock.getVersion()));
				List<JExpr> boxArgs;
				if (a < count) {
					var p = writer.alias(new Object(), BytecodeWriter.parameterSlot(parameters, a), parameters.get(a), "p_" + arg.getName());
					boxArgs = List.of(writer.aiThis(), new JExpr.Read(p));
				} else {
					var defaultValue = block.getDefaultValues().get(a);
					boxArgs = List.of(writer.aiThis(), writer.compileDefaultValue(mainblock, a, defaultValue, arg.getType()), JExpr.of(defaultValue.operations));
				}
				var box = writer.declare(arg, leekscript.runner.values.Box.class, arg.getName(), javaType);
				writer.emitStatement(new JExpr.Assign(box, new JExpr.New(leekscript.runner.values.Box.class, boxArgs)));
				continue;
			}
			if (a < count) {
				writer.alias(arg, BytecodeWriter.parameterSlot(parameters, a), parameters.get(a), arg.getName(), writer.genericNamed(arg.getType().getJavaPrimitiveName(mainblock.getVersion())));
			} else {
				var defaultValue = block.getDefaultValues().get(a);
				var local = writer.declare(arg, writer.primitiveClass(arg.getType()), arg.getName());
				writer.emitStatement(new JExpr.Assign(local, writer.compileDefaultValue(mainblock, a, defaultValue, arg.getType())));
				writer.counter(defaultValue.operations);
			}
		}
	}

	/** Les arguments transmis à la version complète (`return u_m(u_a, u_b…)`). */
	private static List<JExpr> bytecodeForward(BytecodeWriter writer, ClassMethodBlock block) {
		var args = new ArrayList<JExpr>();
		for (var arg : block.getParametersDeclarations()) {
			var local = new JExpr.Read(writer.local(arg));
			args.add(arg.isCaptured() ? new JExpr.Call(local, "get", List.of()) : local);
		}
		return args;
	}

	/**
	 * Charge l'instance englobante de la classe parente, premier argument de son constructeur
	 * (superOuterInstance), depuis un constructeur de cette classe (this$0 dans l'emplacement 1) ;
	 * rend son type : l'IA pour NativeObjectLeekValue et les classes internes du runtime, la racine
	 * d'un autre module pour une classe importée.
	 */
	private Class<?> bytecodeSuperOuter(BytecodeWriter writer, java.lang.classfile.CodeBuilder cb) {
		var root = writer.aiContext;
		cb.aload(1);
		if (parent == null || parent.internal) {
			var ai = writer.aiField();
			if (ai == null) return writer.aiStub();
			cb.getfield(root.desc, ai.name(), JTypes.desc(ai.type()));
			return ai.type();
		}
		var parentRoot = writer.rootOf(parent.getModule());
		if (parentRoot == root) return writer.aiStub();
		var reference = writer.reference(parentRoot);
		cb.getfield(root.desc, reference.name(), JTypes.desc(reference.type()));
		return reference.type();
	}

	/** La classe Java `u_X`, interne à la classe racine de son module (cf declareJava). */
	public void writeBytecodeClass(MainLeekBlock mainblock, BytecodeWriter writer) {
		int version = mainblock.getVersion();
		var name = bytecodeName();
		var stub = writer.typeNamed(name);
		var aiStub = writer.aiStub();
		var desc = JTypes.desc(stub);
		var superClass = stub.getSuperclass();
		var superDesc = JTypes.desc(superClass);
		var ai = writer.aiContext;
		var context = new BytecodeWriter.ClassContext(desc, stub, stub, ai);
		mainblock.getWordCompiler().setCurrentClass(this);
		var bytes = writer.classFile().build(desc, clb -> writer.inClass(context, clb, () -> {
			clb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
			clb.withSuperclass(superDesc);
			var fieldVars = new ArrayList<BytecodeWriter.FieldVar>();
			for (var field : fields.entrySet()) {
				var annotations = new ArrayList<Class<?>>();
				if (field.getValue().isFinal) annotations.add(Final.class);
				if (field.getValue().level == AccessLevel.PROTECTED) annotations.add(Protected.class);
				else if (field.getValue().level == AccessLevel.PRIVATE) annotations.add(Private.class);
				var javaName = field.getValue().getType().getJavaPrimitiveName(version);
				fieldVars.add(writer.field(field.getKey(), writer.typeNamed(javaName), ClassFile.ACC_PUBLIC, annotations, writer.genericNamed(javaName)));
			}
			clb.withField("this$0", ai.desc, ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC);

			// Constructeur par défaut : this$0, super(<instance englobante du parent>), la RAM de l'instance, les initialiseurs
			writer.writeMethod(ConstantDescs.INIT_NAME, ClassFile.ACC_PUBLIC, List.of(aiStub), void.class, () -> {
				var cb = writer.code();
				cb.aload(0); cb.aload(1); cb.putfield(desc, "this$0", ai.desc);
				cb.aload(0);
				var superCtor = Resolver.constructor(superClass, List.of(bytecodeSuperOuter(writer, cb)));
				cb.invokespecial(superDesc, ConstantDescs.INIT_NAME, ctorDesc(superCtor));
				int ram = (parent == null ? AI.INSTANCE_BASE_RAM : 0) + 2 * fieldVariables.size();
				if (ram > 0) writer.emitStatement(JExpr.call("allocateRAM", new JExpr.This(), JExpr.of(ram)));
				int i = 0;
				for (var field : fields.entrySet()) {
					var slot = fieldVars.get(i++);
					Expression expr = field.getValue().expression;
					if (expr != null) {
						var value = field.getValue().getType() != Type.ANY ? writer.compileConvertTyped(mainblock, 0, expr, field.getValue().getType()) : writer.expr(mainblock, expr);
						writer.emitStatement(new JExpr.Assign(slot, value));
					} else if (LanguageFeature.FIELD_TYPE_DEFAULT.enabledIn(version) && !field.getValue().getType().isPrimitive()) {
						var value = writer.defaultValue(field.getValue().getType());
						if (!(value instanceof JExpr.Lit lit && lit.value() == null)) writer.emitStatement(new JExpr.Assign(slot, value));
					}
				}
			});

			// Constructeur par copie (o, level)
			var copyParams = List.<Class<?>>of(aiStub, stub, int.class);
			writer.writeMethod(ConstantDescs.INIT_NAME, ClassFile.ACC_PUBLIC, copyParams, void.class, () -> {
				var cb = writer.code();
				cb.aload(0); cb.aload(1); cb.putfield(desc, "this$0", ai.desc);
				cb.aload(0);
				var outer = bytecodeSuperOuter(writer, cb);
				if (parent != null) { cb.aload(2); cb.iload(3); }
				var superCopy = Resolver.constructor(superClass, parent == null ? List.of(outer) : List.of(outer, stub, int.class));
				cb.invokespecial(superDesc, ConstantDescs.INIT_NAME, ctorDesc(superCopy));
				var o = new JExpr.Read(new BytecodeWriter.LocalVar(2, stub, "o"));
				var level = new JExpr.Read(new BytecodeWriter.LocalVar(3, int.class, "level"));
				int i = 0;
				for (var field : fields.entrySet()) {
					var slot = fieldVars.get(i++);
					var other = new JExpr.Read(new BytecodeWriter.MemberVar(o, stub, field.getKey(), slot.type()));
					var copied = new JExpr.Cast(writer.primitiveClass(field.getValue().getType()), JExpr.call("copy", other, new JExpr.Binary("-", level, JExpr.of(1))));
					writer.emitStatement(new JExpr.Assign(slot, new JExpr.Cond(new JExpr.Binary("==", level, JExpr.of(1)), other, copied)));
				}
			});

			// Corps des constructeurs, hors init() pour rester non virtuels (#3159)
			for (var ctor : constructorsList) {
				final var bodyBlock = ctor.block;
				var params = bytecodeParameters(mainblock, writer, bodyBlock, bodyBlock.getMaxParameters());
				writer.writeMethod(name + "$body_" + bodyBlock.getMaxParameters(), ClassFile.ACC_PRIVATE, params, Object.class, () -> {
					mainblock.getWordCompiler().setCurrentBlock(bodyBlock);
					writer.currentBlock = bodyBlock;
					bytecodeArguments(mainblock, writer, bodyBlock, bodyBlock.getMaxParameters(), params);
					bodyBlock.writeBytecodeBody(mainblock, writer);
					mainblock.getWordCompiler().setCurrentBlock(null);
					writer.currentBlock = null;
				});
			}
			for (var construct : constructors.entrySet()) {
				final var block = construct.getValue().block;
				var params = block == null ? List.<Class<?>>of() : bytecodeParameters(mainblock, writer, block, construct.getKey());
				writer.writeMethod("init", ClassFile.ACC_PUBLIC, params, Object.class, () -> {
					mainblock.getWordCompiler().setCurrentBlock(block);
					writer.currentBlock = block;
					if (parent != null && !parent.internal && (block == null || !block.hasExplicitSuperCall())) {
						writer.emitStatement(new JExpr.SuperCall("init", List.of()));
					}
					if (block != null) {
						bytecodeArguments(mainblock, writer, block, construct.getKey(), params);
						writer.emitReturn(new JExpr.Call(null, name + "$body_" + block.getMaxParameters(), bytecodeForward(writer, block)), Object.class);
					} else {
						writer.emitReturn(JExpr.nul(), Object.class);
					}
					mainblock.getWordCompiler().setCurrentBlock(null);
					writer.currentBlock = null;
				});
			}
			for (var method : methods.entrySet()) {
				for (var v : method.getValue().entrySet()) {
					final var block = v.getValue().block;
					var params = bytecodeParameters(mainblock, writer, block, v.getKey());
					var returnClass = writer.primitiveClass(block.getType().returnType());
					var level = v.getValue().level;
					List<Class<?>> annotations = level == AccessLevel.PROTECTED ? List.of(Protected.class) : level == AccessLevel.PRIVATE ? List.of(Private.class) : List.of();
					writer.writeMethod("u_" + method.getKey(), ClassFile.ACC_PUBLIC, params, returnClass, annotations, () -> {
						mainblock.getWordCompiler().setCurrentBlock(block);
						writer.currentBlock = block;
						bytecodeArguments(mainblock, writer, block, v.getKey(), params);
						if (v.getKey() < block.getMaxParameters()) {
							writer.emitReturn(new JExpr.Call(null, "u_" + method.getKey(), bytecodeForward(writer, block)), returnClass);
						} else {
							block.writeBytecodeBody(mainblock, writer);
						}
						mainblock.getWordCompiler().setCurrentBlock(null);
						writer.currentBlock = null;
					});
				}
			}
			writer.finishClass(clb, desc);
		}));
		writer.addClass(desc, bytes);
		mainblock.getWordCompiler().setCurrentClass(null);
	}

	private static java.lang.constant.MethodTypeDesc ctorDesc(Resolver.Resolved ctor) {
		var params = ctor.parameters();
		var descs = new java.lang.constant.ClassDesc[params.length];
		for (int i = 0; i < params.length; i++) descs[i] = JTypes.desc(params[i]);
		return java.lang.constant.MethodTypeDesc.of(ConstantDescs.CD_void, descs);
	}

	/** La classe dans la hiérarchie (stack maps), le nid et les InnerClasses de sa racine, avant que du code la référence. */
	public void registerBytecodeClass(MainLeekBlock mainblock, BytecodeWriter writer) {
		var stub = writer.typeNamed(bytecodeName());
		writer.registerInnerClass(JTypes.desc(stub), JTypes.desc(stub.getSuperclass()), writer.aiContext.desc, bytecodeName(), ClassFile.ACC_PUBLIC);
	}

	/** Les champs de la classe dans sa racine : sa ClassLeekValue (initialisée par le constructeur) et ses boîtes de champs statiques. */
	public void declareBytecodeFields(MainLeekBlock mainblock, BytecodeWriter writer) {
		var name = bytecodeName();
		var classField = writer.field(name, ClassLeekValue.class, ClassFile.ACC_PUBLIC);
		JExpr value = new JExpr.New(ClassLeekValue.class, List.of(writer.aiThis(), JExpr.of(token.getWord()), JExpr.nul(), new JExpr.ClassLit(writer.typeNamed(name))));
		// Une classe de module se rattache à lui (`.inModule(this)`)
		if (writer.module != null) value = new JExpr.Call(value, "inModule", List.of(new JExpr.This()));
		writer.initializeField(classField, value);
		for (var field : boxedStaticFields()) writer.field(staticFieldBoxName(field), ObjectVariableValue.class, ClassFile.ACC_PUBLIC);
	}

	/** Les méthodes de la classe dans l'IA : new_u_X et ses méthodes statiques. */
	public void writeBytecodeMembers(MainLeekBlock mainblock, BytecodeWriter writer) {
		var name = bytecodeName();
		var stub = writer.typeNamed(name);
		mainblock.getWordCompiler().setCurrentClass(this);
		var classField = writer.field(name);
		writer.writeMethod("new_" + name, ClassFile.ACC_PUBLIC | ClassFile.ACC_VARARGS, List.of(Object[].class), stub, () -> {
			var args = new JExpr.Read(new BytecodeWriter.LocalVar(1, Object[].class, "args"));
			writer.emitReturn(new JExpr.Cast(stub, JExpr.call("execute", new JExpr.Read(classField), args)), stub);
		});
		for (var method : staticMethods.entrySet()) {
			for (var v : method.getValue().entrySet()) {
				final var block = v.getValue().block;
				var params = bytecodeParameters(mainblock, writer, block, v.getKey());
				var returnClass = writer.primitiveClass(block.getType().returnType());
				writer.writeMethod(name + "_" + method.getKey() + "_" + v.getKey(), writer.memberFlags() | ClassFile.ACC_FINAL, params, returnClass, () -> {
					mainblock.getWordCompiler().setCurrentBlock(block);
					writer.currentBlock = block;
					bytecodeArguments(mainblock, writer, block, v.getKey(), params);
					if (v.getKey() < block.getMaxParameters()) {
						var full = bytecodeParameters(mainblock, writer, block, block.getMaxParameters());
						writer.emitReturn(new JExpr.Self(name + "_" + method.getKey() + "_" + block.getMaxParameters(), full, returnClass, bytecodeForward(writer, block)), returnClass);
					} else {
						block.writeBytecodeBody(mainblock, writer);
					}
					mainblock.getWordCompiler().setCurrentBlock(null);
					writer.currentBlock = null;
				});
			}
		}
		mainblock.getWordCompiler().setCurrentClass(null);
	}

	/** defineClass_X : le parent, initFields, l'enregistrement des méthodes par des FunctionLeekValue anonymes. */
	public void writeBytecodeDefine(MainLeekBlock mainblock, BytecodeWriter writer) {
		int version = mainblock.getVersion();
		var name = bytecodeName();
		var stub = writer.typeNamed(name);
		mainblock.getWordCompiler().setCurrentClass(this);
		writer.writeMethod("defineClass_" + getInternalName(), writer.memberFlags(), List.of(), void.class, () -> {
			var classValue = new JExpr.Read(writer.field(name));
			if (parent != null) {
				var parentValue = parent.internal ? new JExpr.Read(writer.member(new JExpr.This(), parent.getClassName(mainblock))) : new JExpr.Read(writer.field(parent.getModule(), parent.getClassName(mainblock)));
				writer.emitStatement(new JExpr.Call(classValue, "setParent", List.of(parentValue)));
			}
			var runParams = List.<Class<?>>of(AI.class, Object.class, Object[].class);
			var initFields = writer.anonymous(FunctionLeekValue.class, List.of(JExpr.of(0)), List.of(new BytecodeWriter.AnonymousMethod("run", ClassFile.ACC_PUBLIC | ClassFile.ACC_VARARGS, runParams, Object.class,
				() -> writer.emitReturn(JExpr.nul(), Object.class))));
			writer.emitStatement(new JExpr.Assign(new BytecodeWriter.MemberVar(classValue, ClassLeekValue.class, "initFields", FunctionLeekValue.class), initFields));
			var args = new JExpr.Read(new BytecodeWriter.LocalVar(3, Object[].class, "args"));
			var thiz = new JExpr.Read(new BytecodeWriter.LocalVar(2, Object.class, "thiz"));
			for (var method : staticMethods.entrySet()) {
				for (var v : method.getValue().entrySet()) {
					var block = v.getValue().block;
					var params = bytecodeParameters(mainblock, writer, block, v.getKey());
					var returnClass = writer.primitiveClass(block.getType().returnType());
					var function = writer.anonymous(FunctionLeekValue.class, List.of(JExpr.of(1), JExpr.of("#Function " + token.getWord() + "." + method.getKey())),
						List.of(new BytecodeWriter.AnonymousMethod("run", ClassFile.ACC_PUBLIC | ClassFile.ACC_VARARGS, runParams, Object.class, () -> {
							var callArgs = new ArrayList<JExpr>();
							for (int a = 0; a < v.getKey(); a++) {
								callArgs.add(new JExpr.Cast(writer.typeNamed(block.getParametersDeclarations().get(a).getType().getJavaName(version)), new JExpr.ArrayGet(args, JExpr.of(a))));
							}
							writer.emitReturn(new JExpr.Self(name + "_" + method.getKey() + "_" + v.getKey(), params, returnClass, callArgs), Object.class);
						})));
					writer.emitStatement(new JExpr.Call(classValue, "addStaticMethod", List.of(JExpr.of(method.getKey()), JExpr.of(v.getKey()), function, new JExpr.GetStatic(AccessLevel.class, v.getValue().level.name()))));
				}
				writer.emitStatement(new JExpr.Call(classValue, "addGenericStaticMethod", List.of(JExpr.of(method.getKey()))));
			}
			for (var method : methods.entrySet()) {
				for (var v : method.getValue().entrySet()) {
					var block = v.getValue().block;
					var function = writer.anonymous(FunctionLeekValue.class, List.of(JExpr.of(0), JExpr.of("#Function " + token.getWord() + "." + method.getKey())),
						List.of(new BytecodeWriter.AnonymousMethod("run", ClassFile.ACC_PUBLIC | ClassFile.ACC_VARARGS, runParams, Object.class, () -> {
							var callArgs = new ArrayList<JExpr>();
							for (int a = 0; a < v.getKey(); a++) {
								callArgs.add(new JExpr.Cast(writer.typeNamed(block.getParametersDeclarations().get(a).getType().getJavaName(version)), new JExpr.ArrayGet(args, JExpr.of(a))));
							}
							writer.emitReturn(new JExpr.Call(new JExpr.Cast(stub, thiz), "u_" + method.getKey(), callArgs), Object.class);
						})));
					writer.emitStatement(new JExpr.Call(classValue, "addMethod", List.of(JExpr.of(method.getKey()), JExpr.of(v.getKey()), function, new JExpr.GetStatic(AccessLevel.class, v.getValue().level.name()))));
				}
				writer.emitStatement(new JExpr.Call(classValue, "addGenericMethod", List.of(JExpr.of(method.getKey()))));
			}
		});
		mainblock.getWordCompiler().setCurrentClass(null);
	}

	/** createStaticClass_X, linkStaticFields_X et initClass_X (cf writeCreateStaticFields, writeInitializeStaticFields). */
	public void writeBytecodeStatics(MainLeekBlock mainblock, BytecodeWriter writer) {
		var name = bytecodeName();
		var boxed = boxedStaticFields();
		mainblock.getWordCompiler().setCurrentClass(this);
		mainblock.getWordCompiler().setCurrentBlock(staticInitBlock);
		writer.writeMethod("createStaticClass_" + getInternalName(), writer.memberFlags(), List.of(), void.class, () -> {
			var classValue = new JExpr.Read(writer.field(name));
			for (var field : staticFields.entrySet()) {
				var constant = ConstantFolder.staticFinalLiteral(this, field.getKey(), this);
				var value = constant != null ? writer.expr(mainblock, constant) : JExpr.nul();
				writer.emitStatement(new JExpr.Call(classValue, "addStaticField", List.of(writer.aiThis(), JExpr.of(field.getKey()), value,
					new JExpr.GetStatic(AccessLevel.class, field.getValue().level.name()), JExpr.of(field.getValue().isFinal))));
			}
			if (!boxed.isEmpty()) writer.emitStatement(new JExpr.Self("linkStaticFields_" + getInternalName(), List.of(), void.class, List.of()));
		});
		if (!boxed.isEmpty()) {
			writer.writeMethod("linkStaticFields_" + getInternalName(), writer.memberFlags(), List.of(), void.class, () -> {
				var classValue = new JExpr.Read(writer.field(name));
				var map = new JExpr.Read(new BytecodeWriter.MemberVar(classValue, ClassLeekValue.class, "staticFields", java.util.LinkedHashMap.class));
				for (var field : boxed) {
					var box = new JExpr.Cast(ObjectVariableValue.class, new JExpr.Call(map, "get", List.of(JExpr.of(field))));
					writer.emitStatement(new JExpr.Assign(writer.field(staticFieldBoxName(field)), box));
				}
			});
		}
		writer.writeMethod("initClass_" + getInternalName(), writer.memberFlags(), List.of(), void.class, () -> {
			var classValue = new JExpr.Read(writer.field(name));
			for (var field : staticFields.entrySet()) {
				JExpr value = null;
				if (field.getValue().expression != null) {
					value = writer.compileConvert(mainblock, 0, field.getValue().expression, field.getValue().getType());
				} else if (hasZeroDefault(field.getValue().getType()) || (LanguageFeature.FIELD_TYPE_DEFAULT.enabledIn(mainblock.getVersion()) && !(writer.defaultValue(field.getValue().getType()) instanceof JExpr.Lit lit && lit.value() == null))) {
					value = writer.defaultValue(field.getValue().getType());
				}
				if (value != null) writer.emitStatement(new JExpr.Call(classValue, "initField", List.of(JExpr.of(field.getKey()), value)));
			}
		});
		mainblock.getWordCompiler().setCurrentClass(null);
		mainblock.getWordCompiler().setCurrentBlock(null);
	}

	private void writeFields(MainLeekBlock mainblock, JavaWriter writer, String className) {

		if (parent != null) {
			parent.writeFields(mainblock, writer, className);
		}

		// for (Entry<String, ClassDeclarationField> field : fields.entrySet()) {
			// writer.addCode(className);
			// writer.addCode(".addField(\"" + field.getKey() + "\"");
			// writer.addCode(", AccessLevel." + field.getValue().level + ", " + field.getValue().isFinal);
			// writer.addLine(");");
		// }
	}

	public Token getParentToken() {
		return parentToken;
	}

	public ClassDeclarationInstruction getParent() {
		return parent;
	}

	public boolean hasMember(String field) {
		return getMember(field) != null;
	}

	public boolean hasStaticMember(String field) {
		return getStaticMember(field) != null;
	}

	public LeekVariable getMember(String token) {
		var f = fieldVariables.get(token);
		if (f != null) return f;

		var m = methodVariables.get(token);
		if (m != null) return m;

		if (parent != null) {
			return parent.getMember(token);
		}
		return null;
	}

	// Variante de getMember limitée aux méthodes (ignore les champs). Sert à lever
	// l'ambiguïté quand un champ et une méthode portent le même nom : getMember
	// renvoie le champ, mais un accès de type appel/référence doit viser la méthode.
	public LeekVariable getMethodMember(String token) {
		var m = methodVariables.get(token);
		if (m != null) return m;

		if (parent != null) {
			return parent.getMethodMember(token);
		}
		return null;
	}

	public LeekVariable getStaticMember(String token) {
		var f = staticFieldVariables.get(token);
		if (f != null) return f;

		var m = staticMethodVariables.get(token);
		if (m != null) return m;

		if (parent != null) {
			return parent.getStaticMember(token);
		}
		return null;
	}

	public HashMap<Integer, ClassDeclarationMethod> getMethod(String name) {
		return methods.get(name);
	}

	public String getMethodName(String name, int argumentCount) {
		var versions = methods.get(name);
		if (versions != null) {
			if (versions.containsKey(argumentCount)) return getInternalName() + "_" + name + "_" + argumentCount;
		}
		if (parent != null) {
			return parent.getMethodName(name, argumentCount);
		}
		return null;
	}

	public ClassDeclarationField getStaticField(String name) {
		var staticField = staticFields.get(name);
		if (staticField != null) return staticField;

		if (parent != null) {
			return parent.getStaticField(name);
		}
		return null;
	}

	public HashMap<Integer, ClassDeclarationMethod> getStaticMethod(String name) {
		return staticMethods.get(name);
	}

	public String getStaticMethodName(String name, int argumentCount) {
		var versions = staticMethods.get(name);
		if (versions != null) {
			if (versions.containsKey(argumentCount)) return getInternalName() + "_" + name + "_" + argumentCount;
		}
		if (parent != null) {
			return parent.getStaticMethodName(name, argumentCount);
		}
		return null;
	}

	public ClassDeclarationMethod getStaticMethod(String method, int argumentCount) {
		var versions = staticMethods.get(method);
		if (versions != null) {
			if (versions.containsKey(argumentCount)) return versions.get(argumentCount);
		}
		if (parent != null) {
			return parent.getStaticMethod(method, argumentCount);
		}
		return null;
	}

	@Override
	public int getOperations() {
		return 0;
	}

	public boolean descendsFrom(ClassDeclarationInstruction clazz) {
		var current = this;
		while (current != null) {
			if (current == clazz) return true;
			if (current.parent != null) {
				current = current.parent;
			} else {
				return false;
			}
		}
		return false;
	}

	public boolean hasField(String field) {
		return getField(field) != null;
	}

	public LeekVariable getField(String token) {
		var f = fieldVariables.get(token);
		if (f != null) return f;

		if (parent != null) {
			return parent.getField(token);
		}
		return null;
	}

	public Error canAccessField(String field, ClassDeclarationInstruction fromClass) {
		var f = fields.get(field);
		if (f != null) {
			if (fromClass == this || this.descendsFrom(fromClass)) {
				return null;
			} else {
				// Protected : Access from descendant
				if (fromClass != null && fromClass.descendsFrom(this)) {
					if (f.level == AccessLevel.PRIVATE) {
						return Error.PRIVATE_FIELD;
					}
					return null;
				} else {
					// Public : Access from outside
					if (f.level != AccessLevel.PUBLIC) {
						return f.level == AccessLevel.PROTECTED ? Error.PROTECTED_FIELD : Error.PRIVATE_FIELD;
					}
					return null;
				}
			}
		}
		if (parent != null) {
			return parent.canAccessField(field, fromClass);
		}
		return null;
	}

	/**
	 * Le champ Java qui garde la boîte du champ statique `field`, déclaré par cette classe et lisible
	 * depuis `fromClass`, pour le lire sans le chercher par son nom ; null sinon (champ hérité ou
	 * inliné, nom que getField intercepte, accès refusé). Les boîtes sont créées par staticInit(),
	 * avant le premier code du joueur.
	 */
	public String staticFieldBox(String field, ClassDeclarationInstruction fromClass, JavaWriter writer) {
		if (!hasBox(field) || canAccessStaticField(field, fromClass) != null) return null;
		return writer.ref(module) + staticFieldBoxName(field);
	}

	/** staticFieldBox pour le backend bytecode : le champ `sf_` existe-t-il, accessible depuis `fromClass` ? */
	public boolean hasStaticFieldBox(String field, ClassDeclarationInstruction fromClass) {
		return hasBox(field) && canAccessStaticField(field, fromClass) == null;
	}

	public String staticFieldBoxField(String field) {
		return staticFieldBoxName(field);
	}

	// Un `static final` à littéral est inliné à chaque lecture (cf ConstantFolder) : pas de boîte
	private boolean hasBox(String field) {
		return !internal && staticFields.containsKey(field) && !ClassLeekValue.MEMBERS_BEFORE_STATIC_FIELDS.contains(field)
			&& ConstantFolder.staticFinalLiteral(this, field, this) == null;
	}

	private List<String> boxedStaticFields() {
		return staticFields.keySet().stream().filter(this::hasBox).toList();
	}

	// Le `$` ne peut pas venir d'un nom LeekScript
	private String staticFieldBoxName(String field) {
		return "sf_" + getInternalName() + "$" + field;
	}

	/**
	 * Visibilité d'un champ statique vu depuis `fromClass` (null = hors de toute classe),
	 * en remontant jusqu'à la classe qui le déclare. Mêmes règles que le runtime en
	 * lecture (ClassLeekValue.getField → getStaticField) : privé = classe déclarante
	 * seule, protégé = classe déclarante et ses descendantes ; l'écriture, que le runtime
	 * ne contrôle pas, est refusée de la même façon à l'analyse. Contrairement à
	 * canAccessField, pas de clause « ancêtre » : le runtime n'en a pas pour les statiques.
	 * Null si l'accès est autorisé (ou si le champ n'existe pas).
	 */
	public Error canAccessStaticField(String field, ClassDeclarationInstruction fromClass) {
		var f = staticFields.get(field);
		if (f != null) {
			if (fromClass == this || f.level == AccessLevel.PUBLIC) return null;
			if (fromClass != null && fromClass.descendsFrom(this)) {
				return f.level == AccessLevel.PRIVATE ? Error.PRIVATE_STATIC_FIELD : null;
			}
			return f.level == AccessLevel.PROTECTED ? Error.PROTECTED_STATIC_FIELD : Error.PRIVATE_STATIC_FIELD;
		}
		if (parent != null) {
			return parent.canAccessStaticField(field, fromClass);
		}
		return null;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		return 0;
	}

	@Override
	public Type getType() {
		return this.classType;
	}

	public Type getEmptyType() {
		return this.emptyType;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return false;
	}

	@Override
	public Hover hover(Token token) {
		if (token == parentToken && parent != null) {
			return new Hover(parent.getType(), token.getLocation(), parent.getLocation());
		}
		return super.hover(token);
	}

	public Type getClassValueType() {
		return classValueType;
	}
}
