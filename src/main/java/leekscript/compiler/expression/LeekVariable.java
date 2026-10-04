package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekGlobalDeclarationInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.AI;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

import leekscript.common.Annotation;
import leekscript.common.CompoundType;
import leekscript.common.Error;
import leekscript.common.Type;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public class LeekVariable extends Expression {

	public static enum VariableType {
		LOCAL, GLOBAL, ARGUMENT, FIELD, STATIC_FIELD, THIS, THIS_CLASS, CLASS, SUPER, METHOD, STATIC_METHOD, SYSTEM_CONSTANT, SYSTEM_FUNCTION, FUNCTION, ITERATOR
	}

	private final Token token;
	private VariableType type;
	private Type variableType = Type.ANY;
	private LeekVariableDeclarationInstruction declaration = null;
	private ClassDeclarationInstruction classDeclaration = null;
	private FunctionBlock functionDeclaration = null;
	private boolean box = false;
	private boolean isFinal = false;
	private LeekVariable variable;
	private Type declaredType = null;
	private Type lastAssignedType = null;
	private int usageCount = 0;
	private LeekGlobalDeclarationInstruction globalDeclaration = null; // cf slotType
	// Lazy : la grande majorité des LeekVariable n'ont aucune annotation. Une
	// EnumSet.noneOf alloue un RegularEnumSet pour chaque variable construite,
	// ce qui se voit dans le profile sur Quantum (~milliers de LeekVariable).
	private EnumSet<Annotation> annotations = null;

	public LeekVariable(Token token, VariableType type) {
		this.token = token;
		token.setExpression(this);
		this.type = type;
	}

	public LeekVariable(Token token, VariableType variableType, Type type, boolean isFinal) {
		this(token, variableType);
		this.variableType = type;
		this.declaredType = type;
		this.isFinal = isFinal;
	}

	public LeekVariable(Token token, VariableType variableType, Type type, boolean isFinal, ClassDeclarationInstruction classDeclaration) {
		this(token, variableType, type, isFinal);
		this.classDeclaration = classDeclaration;
	}

	public LeekVariable(WordCompiler compiler, Token token, VariableType variableType) {
		this(token, variableType);
		this.box = compiler.getVersion() <= 1;
	}

	public LeekVariable(WordCompiler compiler, Token token, VariableType variableType, Type type) {
		this(token, variableType);
		this.variableType = type;
		this.declaredType = type;
		this.box = compiler.getVersion() <= 1;
	}

	public LeekVariable(Token token, VariableType type, boolean box) {
		this(token, type);
		this.box = box;
	}

	public LeekVariable(Token token, VariableType type, LeekVariableDeclarationInstruction declaration) {
		this(token, type);
		this.declaration = declaration;
		this.box = declaration.isCaptured();
	}

	public LeekVariable(Token token, VariableType variableType, Type type, LeekVariableDeclarationInstruction declaration) {
		this(token, variableType);
		this.declaration = declaration;
		this.box = declaration.isCaptured();
		this.variableType = type;
	}

	public LeekVariable(Token token, VariableType type, Type variableType, ClassDeclarationInstruction classDeclaration) {
		this(token, type);
		this.classDeclaration = classDeclaration;
		this.variableType = variableType;
	}

	public LeekVariable(Token token, VariableType type, Type variableType, FunctionBlock functionDeclaration) {
		this(token, type);
		this.functionDeclaration = functionDeclaration;
		this.variableType = variableType;
	}

	@Override
	public int getNature() {
		return VARIABLE;
	}

	@Override
	public Type getType() {
		return variableType;
	}

	/**
	 * Returns the Java declaration type: the type used for the Java variable declaration.
	 * For narrowed variables, this may differ from getType() (which returns the narrowed type).
	 * This is needed during code generation to avoid calling methods that don't exist on Object.
	 */
	public Type getJavaDeclarationType() {
		if (this.variable != null) {
			return this.variable.getDeclaredType();
		}
		return variableType;
	}

	@Override
	public String toString() {
		return token.getWord();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	public boolean isLeftValue() {
		if (type == VariableType.CLASS || type == VariableType.THIS || type == VariableType.THIS_CLASS || type == VariableType.SUPER || type == VariableType.SYSTEM_CONSTANT) {
			return false;
		}
		return true;
	}

	@Override
	public boolean nullable() {
		// return type != VariableType.CLASS && type != VariableType.THIS && type != VariableType.THIS_CLASS;
		return false;
	}

	public VariableType getVariableType() {
		return type;
	}

	public void setVariableType(VariableType type) {
		this.type = type;
	}

	public String getName() {
		return token.getWord();
	}

	public void addUsage() {
		usageCount++;
	}

	public int getUsageCount() {
		return usageCount;
	}

	public void addAnnotation(Annotation a) {
		if (annotations == null) annotations = EnumSet.noneOf(Annotation.class);
		annotations.add(a);
	}

	public boolean hasAnnotation(Annotation a) {
		return annotations != null && annotations.contains(a);
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (this.type == VariableType.SUPER) {
			return; // Déjà OK
		}
		// Cache locale du nom : token.getWord() était appelé jusqu'à 5× dans la
		// cascade de lookups, et getMainBlock() 2×.
		final String name = token.getWord();
		// Local variables first
		var v = compiler.getCurrentBlock().getVariable(name, true);
		if (v != null) {
			this.type = v.getVariableType();
			this.variableType = v.getType();
			this.declaration = v.getDeclaration();
			this.classDeclaration = v.getClassDeclaration();
			this.functionDeclaration = v.getFunctionDeclaration();
			this.isFinal = v.isFinal();
			this.box = v.box;
			this.variable = v;
			v.addUsage();
			if (v.hasAnnotation(Annotation.DEPRECATED)) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.WARNING, Error.ANNOTATION_DEPRECATED_CALL, new String[] { v.getName() }));
			}
			if (v.getDeclaration() != null && v.getDeclaration().getFunction() != compiler.getCurrentFunction()) {
				v.getDeclaration().setCaptured();
			}
			if (this.type == VariableType.FIELD) {
				operations += 1;
			}
			return;
		}
		final var mainBlock = compiler.getMainBlock();
		// Global user functions
		var f = mainBlock.getUserFunction(name);
		if (f != null) {
			this.type = VariableType.FUNCTION;
			this.variableType = f.getType();
			f.getVariable().addUsage();
			if (f.hasAnnotation(Annotation.DEPRECATED)) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.WARNING, Error.ANNOTATION_DEPRECATED_CALL, new String[] { f.getName() }));
			}
			return;
		}
		// LS constants
		var constant = LeekConstants.get(name);
		if (constant != null) {
			this.type = VariableType.SYSTEM_CONSTANT;
			this.variableType = constant.getType();
			return;
		}
		// Redefined function — skip carrément si aucune redef dans tout le programme.
		if (mainBlock.hasRedefinedFunctions() && mainBlock.isRedefinedFunction(name)) {
			this.variableType = Type.ANY;
			return;
		}
		// LS functions
		var lf = LeekFunctions.getValue(name, compiler.getOptions().useExtra());
		if (lf != null) {
			this.type = VariableType.SYSTEM_FUNCTION;
			this.variableType = lf.getVersions()[0].getType();
			for (int i = 1; i < lf.getVersions().length; ++i) {
				this.variableType = Type.versions(this.variableType, lf.getVersions()[i].getType());
			}
			return;
		}
		compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION, new String[] {
			name
		}));
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (this.variable != null) {
			this.variableType = this.variable.getType();
		}
		// Court-circuit pour les variables THIS/THIS_CLASS/SUPER : check par type
		// avant d'appeler getCurrentClass() qui n'a de sens que pour ces variantes.
		// Pour SUPER, on cache getCurrentClass() pour éviter 3 appels.
		if (this.type == VariableType.THIS) {
			this.variableType = compiler.getCurrentClass().getType();
		} else if (this.type == VariableType.THIS_CLASS) {
			this.variableType = compiler.getCurrentClass().classValueType;
		} else if (this.type == VariableType.SUPER) {
			var currentClass = compiler.getCurrentClass();
			var parent = currentClass != null ? currentClass.getParent() : null;
			if (parent != null) {
				this.variableType = parent.getClassValueType();
			}
		}
		// Redefined function — skip carrément si aucune redef dans tout le programme
		// (cas majoritaire). Le précédent isEmpty fast-path est dans le call ; ici
		// on évite aussi le compiler.getMainBlock() + l'appel.
		var mainBlock = compiler.getMainBlock();
		if (mainBlock.hasRedefinedFunctions() && mainBlock.isRedefinedFunction(token.getWord())) {
			this.variableType = Type.ANY;
			return;
		}
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return classDeclaration;
	}

	public LeekVariableDeclarationInstruction getDeclaration() {
		return declaration;
	}

	/**
	 * Returns the original declared type, unaffected by narrowing.
	 * For local/argument variables: uses declaration.getType().
	 * For field/static field variables: uses the stored declaredType.
	 */
	public Type getDeclaredType() {
		if (declaration != null) return declaration.getType();
		return declaredType != null ? declaredType : variableType;
	}

	public Type getLastAssignedType() { return lastAssignedType; }
	public void setLastAssignedType(Type type) { this.lastAssignedType = type; }
	public void clearLastAssignedType() { this.lastAssignedType = null; }

	public FunctionBlock getFunctionDeclaration() {
		return functionDeclaration;
	}

	public Token getToken() {
		return token;
	}

	public boolean isBox() {
		return this.box || (declaration != null && declaration.isBox());
	}

	public boolean isWrapper() {
		return declaration != null && declaration.isWrapper();
	}

	/**
	 * Une fonction système redéfinie (`count = 0` en v1, cf. MainLeekBlock) vit dans un
	 * `Box rfunction_<nom>`, pas dans un `u_<nom>` : ce dernier n'est jamais déclaré.
	 * writeJavaCode/compileL/compileSet le savaient déjà, mais AUCUNE des méthodes de
	 * mutation (+=, -=, ++, --, ...) ne le savait, et émettait `u_<nom>` → « cannot find
	 * symbol » à la compilation Java, donc combat ingénérable (remonté en prod 08/2026).
	 * Le Box supportant toutes ces opérations, on route simplement ces variables vers le
	 * chemin Box déjà en place, avec le bon préfixe.
	 */
	private boolean isRedefinedFunction(MainLeekBlock mainblock) {
		return mainblock.hasRedefinedFunctions() && mainblock.isRedefinedFunction(token.getWord());
	}

	private boolean isBoxLike(MainLeekBlock mainblock) {
		return isBox() || isRedefinedFunction(mainblock);
	}

	/** Nom Java de la variable locale : `rfunction_x` si fonction redéfinie, `u_x` sinon. */
	private String localName(MainLeekBlock mainblock) {
		return (isRedefinedFunction(mainblock) ? "rfunction_" : "u_") + token.getWord();
	}

	/** La variable vit dans un Box ou un Wrapper, champs exclus : lue par `.get()`, écrite par ses méthodes. */
	boolean isBoxSlot() {
		return isBox() && type != VariableType.FIELD && type != VariableType.STATIC_FIELD;
	}

	/**
	 * Lue par `u_x.get()` d'un Box ou d'un Wrapper (cf writeJavaCode) : un Long pour un integer.
	 * Idem pour un itérateur de foreach, déclaré boxé (cf hasBoxedSlot). Sauf narrowing, que
	 * writeNarrowed convertit (#5300) ; narrowée depuis `T?` et lue telle quelle (cf
	 * readsNarrowedRaw), la lecture reste boxée.
	 */
	@Override
	public boolean hasBoxedJavaResult() {
		if (this.variable != null) {
			var slot = slotType();
			if (slot != this.variableType) return readsNarrowedRaw() && slot.boxedPrimitive() == this.variableType;
		}
		return isBoxSlot() || declaration != null && declaration.hasBoxedSlot();
	}

	/**
	 * Lecture narrowée laissée telle quelle (cf readsNarrowedRaw) sur un emplacement Object (any,
	 * union) narrowé par `instanceof` : le consommateur la convertit comme une valeur any (cf
	 * JavaWriter.codegenType).
	 */
	@Override
	public boolean hasObjectJavaResult() {
		return this.variable != null && readsNarrowedRaw() && !slotType().assertNotNull().isPrimitive();
	}

	/**
	 * Lecture narrowée vers un primitif que rien ne convertit : une globale (lue sans
	 * writeNarrowed) ou un boolean (writeNarrowed ne convertit qu'integer et real, un champ
	 * statique est casté). Pas de conversion à la lecture : null s'y lirait false ou 0 si la
	 * variable est remise à null sous le narrowing.
	 */
	private boolean readsNarrowedRaw() {
		return this.variableType.isPrimitive() && (type == VariableType.GLOBAL || this.variableType == Type.BOOL && type != VariableType.STATIC_FIELD);
	}

	/**
	 * Type avec lequel l'emplacement Java est déclaré. Il doit rester stable pendant l'analyse,
	 * où le narrowing réécrit en place le type de la variable et où LeekFunctionCall consulte
	 * hasObjectJavaResult : pour une globale, celui de sa déclaration (inféré de sa valeur en
	 * strict, cf MainLeekBlock) ; sinon le type déclaré. Exception : un itérateur, dont
	 * l'emplacement suit le type de sa variable (cf getIteratorJavaName), narrowé pendant
	 * l'analyse. À distinguer de javaSlotType, qui lit pour une globale le type courant de sa
	 * variable (codegen des écritures).
	 */
	private Type slotType() {
		if (this.variable.globalDeclaration != null) return this.variable.globalDeclaration.getType();
		if (declaration != null && declaration.isIterator()) return this.variable.getType();
		return getJavaDeclarationType();
	}

	public void setGlobalDeclaration(LeekGlobalDeclarationInstruction globalDeclaration) {
		this.globalDeclaration = globalDeclaration;
	}

	/**
	 * Primitif dont l'emplacement Java de la variable est la boîte : déclaration `T?` (locale,
	 * paramètre, globale, champ) ou itérateur de foreach typé. Une affectation native
	 * (`u_x = v`, `u_x += v`, `++u_x`) y rend ce type boxé. Null pour un emplacement primitif ou
	 * Object, pour une variable en Box (écrite par ses méthodes) et pour un champ statique.
	 */
	public Type boxedSlotType() {
		if (this.variable == null || isBox() || type == VariableType.STATIC_FIELD) return null;
		var slot = slotType();
		return declaration != null && declaration.hasBoxedSlot() ? slot : slot.boxedPrimitive();
	}

	/**
	 * `x <op>= v`, `x = v` ou `x++` sur une variable en Box passe par une méthode du Box (ou du
	 * Wrapper d'une variable capturée) qui renvoie un Object, quel que soit le type de la variable :
	 * add_eq, set, coalesce_eq… Sauf `/=` hors v1 (div_eq rend un double, mais pas arithmetic_eq,
	 * cf compileConvertedArithmetic) et les incréments d'un Box<T>, qui rendent T (#5300).
	 */
	public boolean writesObjectThroughBox(int operator, int version) {
		if (!isBoxSlot()) return false;
		if (operator == Operators.DIVIDEASSIGN) return version == 1 || JavaWriter.arithmeticConversion(javaSlotType(), Type.REAL, AI.ArithmeticOperation.DIV) != null;
		if (Operators.isIncrement(operator)) return inUntypedBox(version);
		return true;
	}

	/**
	 * La variable vit dans un Wrapper ou un Box brut, dont les incréments rendent un Object (un
	 * Box<T> les rend en T) : locale capturée, argument capturé en v1, argument d'une fonction
	 * anonyme (en Box, il est capturé ou en v1) (cf FunctionBlock, AnonymousFunctionBlock).
	 */
	private boolean inUntypedBox(int version) {
		if (declaration == null) return false;
		if (type == VariableType.ARGUMENT && declaration.getFunction() instanceof AnonymousFunctionBlock) return true;
		return declaration.isCaptured() && (type == VariableType.LOCAL || version == 1);
	}

	// Émet le mot-clé `class`. Dans une méthode d'instance (ou un constructeur), il
	// doit désigner la classe *runtime* de l'objet courant — sinon un `class` hérité
	// d'une classe parente renverrait toujours la classe parente (bug #2619 :
	// class.name affichait "A" au lieu de "B"). On le résout donc comme `this.class`
	// via classOf(...). Dans une méthode statique il n'y a pas de `this` : on garde la
	// classe englobante (compile-time).
	private void writeThisClass(MainLeekBlock mainblock, JavaWriter writer) {
		var classVariable = mainblock.getWordCompiler().getCurrentClassVariable();
		if (writer.currentBlock != null && !writer.currentBlock.isInStaticMethod()) {
			writer.addCode("classOf(" + classVariable + ".this)");
		} else {
			writer.addCode(classVariable);
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (type == VariableType.THIS) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".this");
		} else if (type == VariableType.THIS_CLASS) {
			writeThisClass(mainblock, writer);
		} else if (type == VariableType.SUPER) {
			writer.addCode("u_" + classDeclaration.getParent().getName());
		} else if (type == VariableType.FIELD) {
			// Le champ Java garde son type déclaré : sans cast, `if (m instanceof B) m.doStuff()`
			// cherche la méthode de B sur une variable de type A → « cannot find symbol » (#4933).
			writeNarrowed(mainblock, writer, token.getWord());
		} else if (type == VariableType.STATIC_FIELD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
			// Champ static final à initialiseur littéral : inliné, comme les
			// constantes moteur (SYSTEM_CONSTANT ci-dessous). Cf ConstantFolder.
			var constant = ConstantFolder.staticFinalLiteral(classDeclaration, token.getWord(), mainblock.getWordCompiler().getCurrentClass());
			if (constant != null) {
				writer.addCode("(");
				constant.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
				return;
			}
			// Un champ statique est stocké en Object : une valeur rangée par un chemin
			// dynamique (`/=`, `??=`…) peut être un Long ou un Double. Pour big_integer
			// on convertit au lieu de caster, sinon la lecture jette une
			// ClassCastException (IMPOSSIBLE_CAST). (#bigint #4908)
			if (variableType == Type.BIG_INT) {
				writer.addCode("BigIntegerValue.valueOf(" + writer.getAIThis() + ", " + mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\"))");
				return;
			}
			// Idem pour integer et real : une opération composée peut avoir rangé
			// l'autre type numérique (`b /= 4` range un Double, `b \= 2` un Long).
			// On convertit comme le fait openResultConversion pour les variables
			// locales, au lieu d'un cast Java qui jetterait une ClassCastException.
			if (variableType == Type.INT || variableType == Type.REAL) {
				writer.addCode("((Number) " + mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\"))." + (variableType == Type.INT ? "longValue()" : "doubleValue()"));
				return;
			}
			if (variableType != Type.ANY) {
				if (parenthesis) writer.addCode("(");
				if (variableType.isPrimitive()) {
					writer.addCode("(" + variableType.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				writer.addCode("(" + variableType.getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\")");
			if (variableType != Type.ANY) {
				if (parenthesis) writer.addCode(")");
			}
		} else if (type == VariableType.METHOD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_METHOD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\")");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".get()");
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			writer.generateAnonymousUserFunction(user_function);
			writer.addCode("ufunction_" + token.getWord());
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			if (constant.getType() == Type.INT) writer.addCode(String.valueOf(constant.getIntValue()) + "l");
			else if (constant.getType() == Type.REAL) {
				if (constant == LeekConstants.NaN) {
					writer.addCode("Double.NaN");
				} else if (constant == LeekConstants.Infinity) {
					writer.addCode("Double.POSITIVE_INFINITY");
				} else {
					writer.addCode(String.valueOf(constant.getValue()));
				}
			}
			else writer.addCode("null");
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			if (user_function != null) {
				writer.generateAnonymousUserFunction(user_function);
				writer.addCode("ufunction_" + token.getWord());
			} else {
				var system_function = LeekFunctions.getValue(token.getWord(), writer.getOptions().useExtra());
				writer.generateAnonymousSystemFunction(system_function);
				// String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode(system_function.getStandardClass() + "_" + token.getWord());
				// writer.addCode("LeekValueManager.getFunction(" + namespace + "." + token.getWord() + ")");
			}
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() <= 1) {
				writer.addCode("g_" + token.getWord() + ".get()");
			} else {
				writer.addCode("g_" + token.getWord());
			}
		} else if (type == VariableType.CLASS) {
			if (classDeclaration.internal) {
				if (token.getWord().equals("Array") && mainblock.getVersion() <= 3) {
					writer.addCode("legacyArrayClass");
				} else {
					writer.addCode(token.getWord().toLowerCase() + "Class");
				}
			} else {
				writer.addCode("u_" + token.getWord());
			}
		} else {
			// Check if the variable was narrowed during analysis: if the expression's type
			// differs from the declaration variable's Java type, emit a cast so that
			// methods like .get(), .doubleValue() work on the narrowed type.
			if (isWrapper() || isBox()) {
				writeNarrowed(mainblock, writer, "u_" + token.getWord() + ".get()");
			} else {
				writeNarrowed(mainblock, writer, "u_" + token.getWord());
			}
		}
	}

	/**
	 * Émet la référence Java `base`, en l'adaptant au type narrowé quand la déclaration
	 * Java est plus large que ce que l'analyse a déduit (`if (x instanceof B)`, `x != null`…).
	 * Sans cela le code généré appelle une méthode du type narrowé sur une variable
	 * déclarée avec le type d'origine, et javac refuse.
	 */
	private void writeNarrowed(MainLeekBlock mainblock, JavaWriter writer, String base) {
		// Pas de garde sur `this.variable` : hasNarrowingMismatch, en tête des deux prédicats,
		// répond déjà false quand il est null.
		if (needsNarrowingCast(mainblock.getVersion())) {
			writer.addCode("((" + this.variableType.getJavaPrimitiveName(mainblock.getVersion()) + ") " + base + ")");
		} else if (needsPrimitiveNarrowingConversion(mainblock.getVersion())) {
			// Variable narrowed to a primitive type (e.g., Cell|integer → integer),
			// but declared as Object in Java. Use safe conversion helpers.
			if (this.variableType == Type.INT) {
				writer.addCode("longint(" + base + ")");
			} else if (this.variableType == Type.REAL) {
				writer.addCode("real(" + base + ")");
			} else {
				writer.addCode(base);
			}
		} else {
			writer.addCode(base);
		}
	}

	/**
	 * Returns true if this variable was narrowed during analysis and the Java
	 * declaration type differs from the narrowed type. This means the narrowed
	 * type cannot be used directly in Java code without a cast or safe helper.
	 * For example: a variable declared as Map|integer (Object in Java) narrowed
	 * to integer cannot be used in arithmetic without conversion.
	 */
	public boolean hasNarrowingMismatch(int version) {
		if (this.variable == null || this.variableType == null) return false;
		Type declType = this.variable.getType();
		if (declType == this.variableType) return false;
		String javaType = this.variableType.getJavaPrimitiveName(version);
		String javaDeclType = declType.getJavaPrimitiveName(version);
		return !javaType.equals(javaDeclType);
	}

	/**
	 * Check if this variable reference needs a narrowing cast in Java code.
	 * Only for non-primitive narrowed types (e.g., Map, Array) where a Java
	 * cast like ((MapLSClass) u_var) is valid. For primitive narrowed types
	 * (e.g., integer → long), the caller must use a safe helper instead.
	 *
	 * Dernière condition : le cast doit aussi être LÉGAL depuis le type déclaré. Une condition
	 * toujours fausse (`Array m; if (m instanceof Map) { ... }`) narrowe vers un type sans lien, et
	 * le cast ferait échouer javac (« incompatible types ») sur du code mort qui compilait très
	 * bien avant. Réponse « non » dans ce cas, donc émission sans cast, comme avant le narrowing.
	 */
	private boolean needsNarrowingCast(int version) {
		return hasNarrowingMismatch(version) && !this.variableType.isPrimitive()
			&& this.variable.getType().castableFrom(this.variableType);
	}

	/**
	 * Check if this variable reference needs a primitive narrowing conversion.
	 * For primitive narrowed types (e.g., Cell|integer narrowed to integer),
	 * a Java cast is not valid (can't cast Object to long), so we need
	 * safe conversion helpers like longint() or real().
	 */
	private boolean needsPrimitiveNarrowingConversion(int version) {
		return hasNarrowingMismatch(version) && this.variableType.isPrimitive();
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (type == VariableType.THIS) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".this");
		} else if (type == VariableType.THIS_CLASS) {
			writeThisClass(mainblock, writer);
		} else if (type == VariableType.SUPER) {
			writer.addCode("u_" + classDeclaration.getParent().getName());
		} else if (type == VariableType.FIELD) {
			writer.addCode("this.getFieldL(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getFieldL(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			writer.addCode("g_" + token.getWord());
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord());
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			if (user_function != null) {
				writer.generateAnonymousUserFunction(user_function);
				writer.addCode("ufunction_" + token.getWord());
			} else {
				var system_function = LeekFunctions.getValue(token.getWord(), writer.getOptions().useExtra());
				writer.generateAnonymousSystemFunction(system_function);
				// String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode(system_function.getStandardClass() + "_" + token.getWord());
				// writer.addCode("LeekValueManager.getFunction(" + namespace + "." + token.getWord() + ")");
			}
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			if (constant.getType() == Type.INT) writer.addCode(String.valueOf(constant.getIntValue()) + "l");
			else if (constant.getType() == Type.REAL) {
				if (constant == LeekConstants.NaN) {
					writer.addCode("Double.NaN");
				} else if (constant == LeekConstants.Infinity) {
					writer.addCode("Double.POSITIVE_INFINITY");
				} else {
					writer.addCode(String.valueOf(constant.getValue()));
				}
			}
			else writer.addCode("null");
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			writer.generateAnonymousUserFunction(user_function);
			writer.addCode("ufunction_" + token.getWord());
		} else if (type == VariableType.CLASS) {
			if (classDeclaration.internal) {
				writer.addCode(token.getWord().toLowerCase() + "Class");
			} else {
				writer.addCode("u_" + token.getWord());
			}
		} else {
			if (isWrapper()) {
				writer.addCode("u_" + token.getWord() + ".getVariable()");
			} else {
				writer.addCode("u_" + token.getWord());
			}
		}
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			writer.compileConvertTyped(mainblock, 0, expr, getJavaDeclarationType(), false);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".setField(\"" + token.getWord() + "\", ");
			writeStaticFieldValue(mainblock, writer, expr);
			writer.addCode(")" + close);
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".set(");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() >= 2) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				writer.compileConvertTyped(mainblock, 0, expr, globalAssignType(mainblock, expr), false);
			if (parenthesis) writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + ".set(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			}
		} else {
			// Use declared type for assignments (not narrowed type)
			var assignType = (this.declaration != null) ? this.declaration.getType() : this.variableType;
			if (isWrapper()) {
				if (expr.isLeftValue()) {
					if (mainblock.getWordCompiler().getVersion() <= 1) {
						writer.addCode("u_" + token.getWord() + ".setBox(");
					} else {
						writer.addCode("u_" + token.getWord() + ".set(");
					}
					expr.compileL(mainblock, writer, false);
					writer.addCode(")");
				} else {
					writer.addCode("u_" + token.getWord() + ".setBoxOrValue(");
					expr.compileL(mainblock, writer, false);
					writer.addCode(")");
				}
			} else if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".set(");
				writer.compileConvert(mainblock, 0, expr, assignType, false);
				writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				writer.compileConvertTyped(mainblock, 0, expr, assignType, false);
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	@Override
	public void compileSetCopy(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			expr.writeJavaCode(mainblock, writer, false);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".setField(\"" + token.getWord() + "\", ");
			writeStaticFieldValue(mainblock, writer, expr);
			writer.addCode(")" + close);
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".set(");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() >= 2) {
			if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				expr.writeJavaCode(mainblock, writer, false);
			if (parenthesis) writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + ".set(");
				expr.compileL(mainblock, writer, false);
				writer.addCode(")");
			}
		} else {
			if (isWrapper() || isBox()) {
				writer.addCode("u_" + token.getWord() + ".set(");
				writer.compileConvert(mainblock, 0, expr, this.variableType, false);
				writer.addCode(")");
			} else {
			if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				expr.writeJavaCode(mainblock, writer, false);
			if (parenthesis) writer.addCode(")");
			}
		}
	}

	/**
	 * Valeur écrite dans un champ statique (`setField`, qui stocke un Object sans
	 * connaître le type déclaré du champ). Un champ `big_integer` doit recevoir un
	 * BigIntegerValue : sans conversion, `b = 5` y range un Long et la lecture
	 * suivante, castée en BigIntegerValue par le code généré, jette une
	 * ClassCastException. Limité à big_integer : les autres types ont toujours été
	 * stockés tels quels par ce chemin. (#bigint #4908)
	 */
	private void writeStaticFieldValue(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		var fieldType = getJavaDeclarationType();
		if (fieldType == Type.BIG_INT) {
			writer.compileConvert(mainblock, 0, expr, fieldType, false);
		} else {
			expr.writeJavaCode(mainblock, writer, false);
		}
	}

	/**
	 * `x++`, `x--`, `++x` et `--x` sur un emplacement que Java ne déclare pas comme un
	 * nombre primitif : la réaffectation passe par add()/sub(), qui renvoient un Object.
	 * Sans conversion vers le type déclaré, `integer | real x = 5; x++` émettait
	 * `u_x = add(u_x, 1l)` sur un `Number u_x`, rejeté par javac (« Object cannot be
	 * converted to Number ») et donc COMPILE_JAVA : l'IA ne compilait plus du tout
	 * (issue #5052). On applique la même conversion que `x += 1` (cf compileAddEq).
	 *
	 * La forme suffixe renvoie l'ancienne valeur en défaisant l'opération à l'extérieur
	 * de l'affectation, d'où l'opérateur inverse autour. `parenthesis` ne concerne que la
	 * forme préfixe : la forme suffixe est déjà délimitée par son sub()/add() extérieur.
	 *
	 * La cible de la conversion est le type DÉCLARÉ de l'emplacement (javaSlotType), jamais
	 * le type narrowé de cette référence : `Map | string x` narrowée vers Map reste déclarée
	 * `Object` en Java, et convertir vers MapLeekValue ferait un IMPOSSIBLE_CAST là où le
	 * code tournait.
	 */
	private void writeIncrement(MainLeekBlock mainblock, JavaWriter writer, String name, boolean increment, boolean suffix, boolean parenthesis) {
		var slotType = javaSlotType();
		if (suffix) {
			writer.addCode(increment ? "sub(" : "add(");
		} else if (parenthesis) {
			writer.addCode("(");
		}
		writer.addCode(name + " = ");
		// Sur un emplacement `integer` la conversion serait l'identité : add(long, long) et
		// sub(Long, Long) renvoient déjà un long. On l'économise, sinon chaque incrément boxe
		// le résultat pour le repasser à longint() (+12 % mesuré sur un champ `integer`).
		// `real` en a besoin, lui : il n'existe pas de surcharge add(double, ...), le résultat
		// arriverait en Object.
		var close = slotType == Type.INT ? "" : openOperationResult(mainblock, writer, slotType, Type.INT);
		writer.addCode((increment ? "add(" : "sub(") + name + ", 1l)" + close);
		if (suffix) {
			writer.addCode(", 1l)");
		} else if (parenthesis) {
			writer.addCode(")");
		}
	}

	/**
	 * Nom de la méthode runtime pour une assignation composée de bits
	 * (|=, &=, ^=, <<=, >>=, >>>=, \=). Sur une variable big_integer il faut la
	 * variante bigOr/bigAnd/bigXor/bigShl/bigShr/bigIntdiv qui renvoie un BigIntegerValue :
	 * les variantes long (bor/band/bxor/shl/shr/ushr/intdiv) renvoient un long et
	 * casseraient la réaffectation (« long cannot be converted to
	 * BigIntegerValue »). (#bigint #4477)
	 *
	 * L'emplacement compte autant que la valeur : `big_integer?` est déclaré
	 * BigIntegerValue en Java (cf CompoundType.getJavaName), sans être BIG_INT.
	 */
	private String bitOpMethod(String longMethod) {
		if (this.variableType != Type.BIG_INT && javaSlotType().assertNotNull() != Type.BIG_INT) {
			// Peut-être un big_integer (any, union) : la promotion se décide au runtime,
			// sinon un big_integer rangé dans une globale ou un champ `any` est tronqué
			// à 64 bits par longint(). (#bigint #4908) Pas sur un champ Java integer ou real,
			// qui ne recevrait pas l'Object rendu : en strict, la référence à une globale non
			// typée analysée avant l'inférence de son type, ou après une affectation, est any (#5321).
			var slot = type == VariableType.GLOBAL && this.variable != null ? slotType() : javaSlotType();
			return mayBeBigInt(this.variableType) && !slot.isPrimitiveNumber() ? longMethod + "Any" : longMethod;
		}
		switch (longMethod) {
			case "bor": return "bigOr";
			case "band": return "bigAnd";
			case "bxor": return "bigXor";
			case "shl": return "bigShl";
			case "shr": case "ushr": return "bigShr";
			case "intdiv": return "bigIntdiv";
			default: return longMethod;
		}
	}

	/**
	 * Plus large que la règle des opérateurs simples (LeekExpression.bitResultType) : `x |= v`
	 * sur un any passe par la variante any depuis #4908, `x | v` reste sur 64 bits.
	 */
	private static boolean mayBeBigInt(Type type) {
		return type == Type.ANY || type instanceof CompoundType ct && ct.getTypes().contains(Type.BIG_INT);
	}

	/**
	 * Type cible pour la conversion sur une globale. Une référence à une globale
	 * fige son `variableType` lors de son analyse, qui a lieu AVANT l'inférence du
	 * type de la globale depuis sa valeur d'initialisation (les fonctions sont
	 * analysées avant les globales). Le champ Java de la globale est pourtant
	 * déclaré avec le type inféré : on lit le type canonique (`this.variable`)
	 * pour rester cohérent et éviter un COMPILE_JAVA "Object cannot be converted
	 * to long". (#4339)
	 */
	private Type globalCastType() {
		return this.variable != null ? this.variable.getType() : this.variableType;
	}

	/**
	 * Cible de la conversion de `g = v` (v2+) : le type de la variable de la globale. En strict,
	 * une globale non typée prend le type de sa valeur, avec lequel son champ Java est déclaré
	 * (cf slotType), mais une affectation remet sa variable à any (LeekExpression, ASSIGN) : `v`
	 * s'écrivait alors tel quel, et `g = f()` rangeait un Object dans un `long` (#5322). Le type
	 * du champ devient la cible quand javac n'y accepterait pas `v` : un Object, un autre
	 * primitif. Une valeur que le champ reçoit déjà s'écrit comme avant.
	 */
	private Type globalAssignType(MainLeekBlock mainblock, Expression expr) {
		var type = this.variable.getType();
		if (type != Type.ANY) return type;
		var slot = slotType();
		var slotJava = slot.getJavaPrimitiveName(mainblock.getVersion());
		if (slotJava.equals("Object")) return type;
		var value = writtenType(mainblock, expr);
		if (value == Type.NULL) return slot.isPrimitive() ? slot : type;
		var valueJava = expr.hasObjectJavaResult() ? "Object" : value.getJavaPrimitiveName(mainblock.getVersion());
		var fits = switch (slotJava) {
			case "long", "Long" -> valueJava.equals("long") || valueJava.equals("Long");
			case "double" -> valueJava.equals("double") || valueJava.equals("Double") || valueJava.equals("long") || valueJava.equals("Long");
			case "Double" -> valueJava.equals("double") || valueJava.equals("Double");
			case "boolean", "Boolean" -> valueJava.equals("boolean") || valueJava.equals("Boolean");
			case "Number" -> Set.of("long", "Long", "double", "Double", "Number").contains(valueJava);
			// Autre type référence : un Object, un nombre ou un booléen n'y entrent pas
			default -> valueJava.equals(slotJava) || !Set.of("Object", "Number", "long", "Long", "double", "Double", "boolean", "Boolean").contains(valueJava);
		};
		return fits ? type : slot;
	}

	/**
	 * Type du Java que `expr` émet (v2+) : le sien, sauf un appel remplacé par le littéral rendu
	 * par la fonction (cf LeekFunctionCall.getWrittenType) et une globale, lue dans son champ.
	 */
	private static Type writtenType(MainLeekBlock mainblock, Expression expr) {
		var trimmed = expr.trim();
		if (trimmed instanceof LeekFunctionCall call) return call.getWrittenType(mainblock);
		if (trimmed instanceof LeekVariable v && v.type == VariableType.GLOBAL && v.variable != null && v.variable.globalDeclaration != null) return v.slotType();
		return expr.getType();
	}

	/**
	 * Type avec lequel l'emplacement Java de cette variable est DÉCLARÉ — jamais le type
	 * narrowé de cette référence. Une globale lit son type canonique (cf globalCastType),
	 * un champ et une locale leur type de déclaration (même règle que compileSet).
	 */
	private Type javaSlotType() {
		return type == VariableType.GLOBAL ? globalCastType() : getJavaDeclarationType();
	}

	/**
	 * L'emplacement Java accepte-t-il l'opérateur natif `++` / `--` ? Il y faut un nombre
	 * primitif déclaré avec ce type, ou avec son type boîte (`Long`/`Double`), que javac
	 * incrémente aussi. Une variable narrowée vers integer mais déclarée `Object`
	 * (`Array | integer`) ne l'accepte pas : `ops(u_x++, 1)` donne « bad operand type
	 * Object for unary operator '++' », le même COMPILE_JAVA que #5052.
	 */
	private boolean hasPrimitiveJavaSlot(int version) {
		if (!this.variableType.isPrimitiveNumber()) return false;
		if (!hasNarrowingMismatch(version)) return true;
		return javaSlotType().getJavaPrimitiveName(version).equals(this.variableType.getJavaName(version));
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		compileIncDec(mainblock, writer, true, true, parenthesis, "increment", "field_inc");
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		compileIncDec(mainblock, writer, false, true, parenthesis, "decrement", "field_dec");
	}

	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		compileIncDec(mainblock, writer, true, false, parenthesis, "pre_increment", "field_pre_inc");
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		compileIncDec(mainblock, writer, false, false, parenthesis, "pre_decrement", "field_pre_dec");
	}

	/**
	 * Les quatre incréments ne diffèrent que par deux booléens et le nom du helper runtime
	 * appelé sur un Box (`increment`, `pre_decrement`, ...) ou sur un champ statique
	 * (`field_inc`, `field_pre_dec`, ...) : ces noms restent passés en littéral, pour qu'un
	 * grep les retrouve depuis ClassLeekValue.
	 */
	private void compileIncDec(MainLeekBlock mainblock, JavaWriter writer, boolean increment, boolean suffix, boolean parenthesis, String boxMethod, String fieldHelper) {
		var javaOperator = increment ? "++" : "--";
		if (type == VariableType.FIELD) {
			writeIncrement(mainblock, writer, token.getWord(), increment, suffix, parenthesis);
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + "." + fieldHelper + "(\"" + token.getWord() + "\")" + close);
		} else if (type == VariableType.GLOBAL) {
			var name = "g_" + token.getWord();
			if (isBox()) {
				writer.addCode(name + "." + boxMethod + "()");
			} else if (hasPrimitiveJavaSlot(mainblock.getVersion())) {
				writer.addCode(suffix ? name + javaOperator : javaOperator + name);
			} else {
				writeIncrement(mainblock, writer, name, increment, suffix, parenthesis);
			}
		} else {
			var name = "u_" + token.getWord();
			if (isBoxLike(mainblock)) {
				writer.addCode(localName(mainblock) + "." + boxMethod + "()");
			} else if (hasPrimitiveJavaSlot(mainblock.getVersion())) {
				writer.addCode(suffix ? name + javaOperator : javaOperator + name);
			} else {
				writeIncrement(mainblock, writer, name, increment, suffix, parenthesis);
			}
		}
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t, boolean parenthesis) {
		if (compileConvertedArithmetic(mainblock, writer, expr, AI.ArithmeticOperation.ADD)) return;
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			var close = openOperationResult(mainblock, writer, variableType, expr.getType());
			writer.addCode("add(" + token.getWord() + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_add_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".add_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber() && expr.getType().isPrimitiveNumber() && !hasNarrowingMismatch(mainblock.getVersion()) && !(expr instanceof LeekVariable lve && lve.hasNarrowingMismatch(mainblock.getVersion()))) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " += ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, globalCastType(), expr.getType());
				writer.addCode("add(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (isBoxLike(mainblock)) {
				writer.addCode(localName(mainblock) + ".add_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber() && expr.getType().isPrimitiveNumber() && !hasNarrowingMismatch(mainblock.getVersion()) && !(expr instanceof LeekVariable lve && lve.hasNarrowingMismatch(mainblock.getVersion()))) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " += ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, this.variableType, expr.getType());
				writer.addCode("add_eq(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		if (compileConvertedArithmetic(mainblock, writer, expr, AI.ArithmeticOperation.SUB)) return;
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			var close = openOperationResult(mainblock, writer, variableType, expr.getType());
			writer.addCode("sub(" + token.getWord() + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_sub_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".sub_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber() && expr.getType().isPrimitiveNumber() && !hasNarrowingMismatch(mainblock.getVersion()) && !(expr instanceof LeekVariable lve && lve.hasNarrowingMismatch(mainblock.getVersion()))) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " -= ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, globalCastType(), expr.getType());
				writer.addCode("sub(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (isBoxLike(mainblock)) {
				writer.addCode(localName(mainblock) + ".sub_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber() && expr.getType().isPrimitiveNumber() && !hasNarrowingMismatch(mainblock.getVersion()) && !(expr instanceof LeekVariable lve && lve.hasNarrowingMismatch(mainblock.getVersion()))) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " -= ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, this.variableType, expr.getType());
				writer.addCode("sub(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type resultType, boolean parenthesis) {
		if (compileConvertedArithmetic(mainblock, writer, expr, AI.ArithmeticOperation.MUL)) return;
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			var close = openOperationResult(mainblock, writer, variableType, expr.getType());
			writer.addCode("mul(" + token.getWord() + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_mul_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".mul_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber() && expr.getType().isPrimitiveNumber() && !hasNarrowingMismatch(mainblock.getVersion()) && !(expr instanceof LeekVariable lve && lve.hasNarrowingMismatch(mainblock.getVersion()))) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " *= ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, globalCastType(), expr.getType());
				writer.addCode("mul(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (isBoxLike(mainblock)) {
				writer.addCode(localName(mainblock) + ".mul_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber() && expr.getType().isPrimitiveNumber() && !hasNarrowingMismatch(mainblock.getVersion()) && !(expr instanceof LeekVariable lve && lve.hasNarrowingMismatch(mainblock.getVersion()))) {
			if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " *= ");
				writer.compileTyped(mainblock, expr, false);
			if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, this.variableType, expr.getType());
				writer.addCode("mul(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		}
	}


	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type resultType, boolean parenthesis) {
		if (compileConvertedArithmetic(mainblock, writer, expr, AI.ArithmeticOperation.POW)) return;
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			var close = openOperationResult(mainblock, writer, variableType, expr.getType());
			writer.addCode("pow(" + token.getWord() + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_pow_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".pow_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, globalCastType(), expr.getType());
				writer.addCode("pow(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (isBoxLike(mainblock)) {
				writer.addCode(localName(mainblock) + ".pow_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, this.variableType, expr.getType());
				writer.addCode("pow(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	/**
	 * Cible de la conversion du résultat de `/=`, toujours un réel : sur un `integer?`, le cast
	 * `(Long)` ne compile pas, et le résultat n'étant jamais null, il se convertit en integer.
	 */
	private static Type divTarget(Type slot) {
		return slot.boxedPrimitive() == Type.INT ? Type.INT : slot;
	}

	/**
	 * Ouvre la conversion du résultat d'une opération composée (add, sub, mul, pow, div, mod)
	 * vers l'emplacement, `castType` étant le type visé, `operand` celui de l'opérande :
	 * - un emplacement booléen (non strict : refusé à l'analyse en strict) reçoit la vérité du
	 *   nombre, comme `b = 1` ; narrowé vers boolean sur un autre emplacement, le nombre y est
	 *   rangé tel quel ;
	 * - sur un `integer?` / `real?`, le cast vers la boîte ne tient que si l'opération rend ce
	 *   type, ce que garantit un opérande du même type. Sinon on convertit : `real? r = null;
	 *   r += 5` rend un Long, `integer? h; h += 1.5` un Double.
	 */
	private String openOperationResult(MainLeekBlock mainblock, JavaWriter writer, Type castType, Type operand) {
		if (castType.assertNotNull() == Type.BOOL) {
			var slot = javaSlotType();
			if (slot.assertNotNull() == Type.BOOL) return writer.openFieldResultConversion(Type.BOOL);
			castType = slot;
		}
		var boxed = castType.boxedPrimitive();
		if ((boxed == Type.INT || boxed == Type.REAL) && operand.assertNotNull() != boxed) castType = boxed;
		return writer.openResultConversion(mainblock.getVersion(), castType);
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		if (compileConvertedArithmetic(mainblock, writer, expr, AI.ArithmeticOperation.DIV)) return;
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			var close = openOperationResult(mainblock, writer, divTarget(variableType), expr.getType());
			writer.addCode("div(" + token.getWord() + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_div_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				if (mainblock.getVersion() == 1) {
					writer.addCode("g_" + token.getWord() + ".div_eq_v1(");
				} else {
					writer.addCode("g_" + token.getWord() + ".div_eq(");
				}
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, divTarget(globalCastType()), expr.getType());
				if (mainblock.getVersion() == 1) {
					writer.addCode("div_v1(g_" + token.getWord() + ", ");
				} else {
					writer.addCode("div(g_" + token.getWord() + ", ");
				}
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (isBoxLike(mainblock)) {
				if (mainblock.getVersion() == 1) {
					writer.addCode(localName(mainblock) + ".div_eq_v1(");
				} else {
					writer.addCode(localName(mainblock) + ".div_eq(");
				}
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				if (mainblock.getVersion() == 1) {
					writer.addCode("u_" + token.getWord() + " = div_v1(u_" + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer, false);
					writer.addCode(")");
				} else {
					writer.addCode("u_" + token.getWord() + " = ");
					var close = openOperationResult(mainblock, writer, divTarget(this.variableType), expr.getType());
					writer.addCode("div(u_" + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer, false);
					writer.addCode(")" + close);
				}
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	@Override
	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.INTDIV, "field_intdiv_eq", "intdiv_eq", null);
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		if (compileConvertedArithmetic(mainblock, writer, expr, AI.ArithmeticOperation.MOD)) return;
		if (type == VariableType.FIELD) {
			if (parenthesis) writer.addCode("(");
			writer.addCode(token.getWord() + " = ");
			var close = openOperationResult(mainblock, writer, variableType, expr.getType());
			writer.addCode("mod(" + token.getWord() + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (parenthesis) writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_mod_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".mod_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (hasPrimitiveJavaSlot(mainblock.getVersion()) && expr.getType().isPrimitiveNumber()) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " %= ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("g_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, globalCastType(), expr.getType());
				writer.addCode("mod(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (isBoxLike(mainblock)) {
				writer.addCode(localName(mainblock) + ".mod_eq(");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			} else if (hasPrimitiveJavaSlot(mainblock.getVersion()) && expr.getType().isPrimitiveNumber()) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " %= ");
				writer.compileTyped(mainblock, expr, false);
				if (parenthesis) writer.addCode(")");
			} else {
				if (parenthesis) writer.addCode("(");
				writer.addCode("u_" + token.getWord() + " = ");
				var close = openOperationResult(mainblock, writer, this.variableType, expr.getType());
				writer.addCode("mod(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer, false);
				writer.addCode(")" + close);
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.BOR, "field_bor_eq", "bor_eq", "|");
	}

	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.BAND, "field_band_eq", "band_eq", "&");
	}

	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.BXOR, "field_bxor_eq", "bxor_eq", "^");
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.SHL, "field_shl_eq", "shl_eq", "<<");
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.SHR, "field_shr_eq", "shr_eq", ">>");
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compileBitAssign(mainblock, writer, expr, parenthesis, AI.BitOperation.USHR, "field_ushr_eq", "ushr_eq", ">>>");
	}

	/**
	 * `x <op>= v` pour les six opérateurs de bits et `\=` : `fieldHelper` et `boxMethod` sont
	 * les méthodes runtime d'un champ statique et d'un Box, `operator` l'opérateur Java natif,
	 * null s'il n'y en a pas. Un champ statique ou un Box booléen, réel ou `big_integer?` passe
	 * par field_bit_eq / bit_eq, qui convertissent le résultat (cf JavaWriter.containerBitConversion).
	 */
	private void compileBitAssign(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis, AI.BitOperation operation, String fieldHelper, String boxMethod, String operator) {
		String name = token.getWord();
		String box = boxName(mainblock);
		if (type == VariableType.STATIC_FIELD || box != null) {
			var target = JavaWriter.containerBitConversion(javaSlotType());
			writeContainerOperation(mainblock, writer, expr, box, target != null ? "field_bit_eq" : fieldHelper, target != null ? "bit_eq" : boxMethod, operation, target);
			return;
		}
		String slot = type == VariableType.FIELD ? name : (type == VariableType.GLOBAL ? "g_" : "u_") + name;
		if (parenthesis) writer.addCode("(");
		if (operator != null && type != VariableType.FIELD && hasNativeBitSlot(mainblock.getVersion())) {
			writer.addCode(slot + " " + operator + "= ");
			// Opérande entier (Long compris, que javac déboxe) ou Object converti : Java inchangé.
			// Un any, un real… passent par longint() comme sur une locale (#5321).
			if (type == VariableType.GLOBAL && (expr.hasObjectJavaResult() || writtenType(mainblock, expr).assertNotNull() == Type.INT)) {
				writer.compileTyped(mainblock, expr, Type.INT, false);
			} else {
				writer.getInt(mainblock, expr, false);
			}
		} else {
			writer.addCode(slot + " = ");
			if (type == VariableType.FIELD && operation == AI.BitOperation.BOR) {
				// Cast historique du seul `|=` sur un champ. Narrowé vers null, variableType
				// donnerait `(Object)`, qui ne rentre pas dans un `T?`.
				var castType = variableType == Type.NULL ? javaSlotType() : variableType;
				if (castType != Type.ANY) {
					writer.addCode("(" + castType.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
			}
			// Le résultat est un long, un BigIntegerValue ou un Object : un booléen ou une boîte
			// Double ne le reçoit pas tel quel. Un booléen garde la vérité du résultat, comme
			// `this.f |= v` (setField).
			var converted = javaSlotType().assertNotNull() == Type.BOOL ? Type.BOOL : boxedSlotType() == Type.REAL ? Type.REAL : null;
			var close = converted != null ? writer.openFieldResultConversion(converted) : "";
			writer.addCode(bitOpMethod(operation.name().toLowerCase(Locale.ROOT)) + "(" + slot + ", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
		}
		if (parenthesis) writer.addCode(")");
	}

	/**
	 * L'emplacement accepte-t-il l'opérateur de bits natif (`u_x |= v`) ? Il y faut un integer
	 * déclaré `long` ou `Long` : sur un `double`, un `Number` ou un `Object`, javac refuse.
	 */
	private boolean hasNativeBitSlot(int version) {
		return this.variableType == Type.INT && hasPrimitiveJavaSlot(version);
	}

	/** Nom Java du Box de la variable (globale en Box, variable capturée, fonction redéfinie), null sans Box. */
	private String boxName(MainLeekBlock mainblock) {
		if (type == VariableType.GLOBAL) return isBox() ? "g_" + token.getWord() : null;
		return type != VariableType.FIELD && type != VariableType.STATIC_FIELD && isBoxLike(mainblock) ? localName(mainblock) : null;
	}

	/**
	 * `x <op>= v` (+=…) sur un champ statique ou un Box dont add() & co rangeraient un autre type
	 * que celui déclaré (cf JavaWriter.staticFieldArithmeticConversion / arithmeticConversion) :
	 * par field_arithmetic_eq / arithmetic_eq, qui rangent le résultat converti. Rend false, sans
	 * rien écrire, sinon.
	 */
	private boolean compileConvertedArithmetic(MainLeekBlock mainblock, JavaWriter writer, Expression expr, AI.ArithmeticOperation operation) {
		String box = boxName(mainblock);
		if (type != VariableType.STATIC_FIELD && box == null) return false;
		var target = type == VariableType.STATIC_FIELD
			? JavaWriter.staticFieldArithmeticConversion(javaSlotType(), expr.getType(), operation)
			: JavaWriter.arithmeticConversion(javaSlotType(), expr.getType(), operation);
		if (target == null) return false;
		writeContainerOperation(mainblock, writer, expr, box, "field_arithmetic_eq", "arithmetic_eq", operation, target);
		return true;
	}

	/**
	 * `Classe.fieldMethod("x", v[, opération, cible])` sur un champ statique, `box.boxMethod(v[,
	 * opération, cible])` sinon ; sans cible, les helpers du runtime qui rangent le résultat brut.
	 */
	private void writeContainerOperation(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String box, String fieldMethod, String boxMethod, Enum<?> operation, Type target) {
		if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + "." + fieldMethod + "(\"" + token.getWord() + "\", ");
		} else {
			writer.addCode(box + "." + boxMethod + "(");
		}
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode((target != null ? JavaWriter.operationArguments(operation, target) : "") + ")");
	}

	@Override
	public void compileCoalesceEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// a ??= b  =>  a = (a != null) ? a : b
		// Variable dans un Box (Wrapper si une closure la capture), argument compris comme pour
		// `+=` (#5300) : Box.coalesce_eq. Fonction système redéfinie : Box `rfunction_<nom>`,
		// quel que soit le VariableType (cf. isRedefinedFunction).
		var box = isRedefinedFunction(mainblock) ? localName(mainblock)
			: isBoxSlot() ? (type == VariableType.GLOBAL ? "g_" : "u_") + token.getWord()
			: null;
		if (box != null) {
			// `b` n'est évaluée que si la variable est null (#5300)
			boolean lazy = !ConstantFolder.isHarmlessValue(expr, mainblock.getWordCompiler().getCurrentClass());
			writer.addCode(box + ".coalesce_eq(" + (lazy ? box + ".get() == null ? " : ""));
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode((lazy ? " : null" : "") + ")");
			return;
		}

		// Un champ statique n'est pas un champ Java de la ClassLeekValue : il faut
		// passer par getField/setField. Sans ça le Java généré ne compile pas
		// (« cannot find symbol: variable b »), pour tous les types. (#4908)
		if (type == VariableType.STATIC_FIELD) {
			var clazz = mainblock.getWordCompiler().getCurrentClassVariable();
			var read = clazz + ".getField(\"" + token.getWord() + "\")";
			var close = writer.openFieldResultConversion(this.variableType);
			writer.addCode(clazz + ".setField(\"" + token.getWord() + "\", ");
			if (this.variableType == Type.BIG_INT) {
				writer.addCode("BigIntegerValue.valueOf(" + writer.getAIThis() + ", ");
			}
			writer.addCode("(" + read + " != null ? " + read + " : ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
			if (this.variableType == Type.BIG_INT) {
				writer.addCode(")");
			}
			writer.addCode(")" + close);
			return;
		}

		// Fallback: explicit ternary assignment on the underlying storage
		String varName;
		if (type == VariableType.FIELD) {
			varName = token.getWord();
		} else if (type == VariableType.GLOBAL) {
			varName = "g_" + token.getWord();
		} else {
			// LOCAL or others
			varName = "u_" + token.getWord();
		}

		if (parenthesis) writer.addCode("(");
		if (javaSlotType().isPrimitive()) {
			// Emplacement Java primitif (integer, real, boolean) : jamais null, `b` n'est pas
			// évaluée. `x != null` n'y compilerait pas (#5300) : on garde l'affectation, à l'identique.
			writer.addCode(varName + " = " + varName);
		} else {
			writer.addCode(varName + " = ");
			if (this.variableType != Type.ANY) {
				writer.addCode("(" + this.variableType.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("(" + varName + " != null ? " + varName + " : ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		}
		if (parenthesis) writer.addCode(")");
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public Hover hover(Token token) {
		if (classDeclaration != null) {
			return new Hover(getType(), getLocation(), classDeclaration.getLocation());
		}
		if (declaration != null) {
			return new Hover(getType(), getLocation(), declaration.getLocation());
		}
		if (functionDeclaration != null) {
			return new Hover(getType(), getLocation(), functionDeclaration.getLocation());
		}
		if (this.variable != null) {
			return new Hover(getType(), getLocation(), this.variable.getToken().getLocation());
		}
		return new Hover(getType(), getLocation());
	}

	public boolean isFinal() {
		return isFinal;
	}

	public LeekVariable getVariable() {
		return variable;
	}

	public void setType(Type type) {
		this.variableType = type;
	}
}
