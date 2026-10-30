package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import leekscript.common.Annotation;
import leekscript.common.Type;
import leekscript.compiler.Annotatable;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.ConstantFolder;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.common.Error;
import leekscript.common.FunctionType;

public class ClassMethodBlock extends AbstractLeekBlock implements Annotatable {

	private final ClassDeclarationInstruction clazz;
	private final boolean isStatic;
	private final ArrayList<Token> mParameters = new ArrayList<>();
	private final ArrayList<Expression> defaultValues = new ArrayList<>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private int mId = 0;
	private final Token token;
	private final boolean isConstructor;
	private int minParameters = 0;
	private int maxParameters = 0;
	private final FunctionType type;
	// Lazy : la grande majorité des méthodes n'ont aucune annotation.
	private EnumSet<Annotation> annotations = null;
	// @tailrec : appels récursifs (this.m() / m() / Class.m() résolvant vers CETTE
	// méthode) collectés pendant l'analyse du corps, validés ensuite — tous doivent
	// être en position terminale. Mirror de FunctionBlock. Lazy : seules les méthodes
	// @tailrec qui s'appellent elles-mêmes en collectent.
	private ArrayList<leekscript.compiler.expression.LeekFunctionCall> recursiveCalls = null;
	// Type guard (#42, v5) : `public f(any x) -> x is Type`. La méthode renvoie un
	// booléen ; quand un appel sert de condition, l'argument à `typeGuardParam` est
	// affiné vers `typeGuardType` dans la branche vraie. Mirror de FunctionBlock.
	private int typeGuardParam = -1;
	private Type typeGuardType = null;

	public ClassMethodBlock(ClassDeclarationInstruction clazz, boolean isConstructor, boolean isStatic, AbstractLeekBlock parent, MainLeekBlock main, Token token, Type returnType) {
		super(parent, main);
		this.clazz = clazz;
		this.isConstructor = isConstructor;
		this.isStatic = isStatic;
		this.token = token;
		this.type = new FunctionType(returnType);
		this.overridesGetVariable = true;  // override polymorphique (cf. AbstractLeekBlock.getVariable)
	}

	public void addAnnotation(Annotation a) {
		if (annotations == null) annotations = EnumSet.noneOf(Annotation.class);
		annotations.add(a);
	}

	public boolean hasAnnotation(Annotation a) {
		return annotations != null && annotations.contains(a);
	}

	// @tailrec : un appel résolu vers cette méthode (récursion directe) s'enregistre
	// ici pendant l'analyse de son propre corps, pour validation a posteriori.
	public void registerRecursiveCall(leekscript.compiler.expression.LeekFunctionCall call) {
		if (recursiveCalls == null) recursiveCalls = new ArrayList<>();
		recursiveCalls.add(call);
	}

	public String getName() {
		return token.getWord();
	}

	public int getParameterIndex(String name) {
		for (int i = 0; i < mParameters.size(); i++) {
			if (mParameters.get(i).getWord().equals(name)) return i;
		}
		return -1;
	}

	// Type guard (#42) : enregistré au parse (`-> p is Type`), lu par NarrowingInfo. La
	// méthode renvoie alors un booléen.
	public void setTypeGuard(int paramIndex, Type type) {
		this.typeGuardParam = paramIndex;
		this.typeGuardType = type;
		this.type.setReturnType(Type.BOOL);
		this.returnTypeWritten = true;
	}

	// Types écrits dans la signature, pour l'API typée d'un package (cf MainLeekBlock.checkPackageApis)
	private final ArrayList<Boolean> parametersTyped = new ArrayList<>();
	private boolean returnTypeWritten = false;

	public void markLastParameterTyped(boolean typed) {
		parametersTyped.add(typed);
	}

	public boolean isParameterTyped(int index) {
		return index < parametersTyped.size() && parametersTyped.get(index);
	}

	public String getParameterName(int index) {
		return mParameters.get(index).getWord();
	}

	public void markReturnTypeWritten() {
		returnTypeWritten = true;
	}

	public boolean isReturnTypeWritten() {
		return returnTypeWritten;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	@Override
	public int getTypeGuardParam() {
		return typeGuardParam;
	}

	@Override
	public Type getTypeGuardType() {
		return typeGuardType;
	}

	public void setId(int id) {
		mId = id;
	}

	public int getId() {
		return mId;
	}

	public int countParameters() {
		return mParameters.size();
	}

	public String referenceArray() {
		String str = "{";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ",";
		}
		return str + "}";
	}

	public void addParameter(WordCompiler compiler, Token token, Token equal, Type type, Expression defaultValue) throws LeekCompilerException {

		// Existe déjà ?
		for (var param : mParameters) {
			if (param.getWord().equals(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.DUPLICATED_ARGUMENT, new String[] { token.getWord() }));
			}
		}

		// Paramètre par défaut pas à la fin
		if (mParameters.size() > 0 && defaultValue == null && defaultValues.get(defaultValues.size() - 1) != null) {
			compiler.addError(new AnalyzeError(mParameters.get(mParameters.size() - 1), AnalyzeErrorLevel.ERROR, Error.DEFAULT_ARGUMENT_NOT_END, new String[] {

			}));
		}

		type = FunctionBlock.parameterType(type, defaultValue);
		mParameters.add(token);
		defaultValues.add(defaultValue);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, this, type);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, type, declaration));
		maxParameters++;
		if (defaultValue == null) {
			minParameters++;
		}
		this.type.add_argument(type, defaultValue != null);
		// System.out.println("Method " + this.token.getWord() + " type = " + this.type);
	}

	@Override
	public LeekVariable getVariable(String variable, boolean includeClassMembers) {
		// Arguments
		// for (var parameter : mParameters) {
		// 	if (parameter.getWord().equals(variable)) {
		// 		return new LeekVariable(parameter, VariableType.ARGUMENT);
		// 	}
		// }

		var v = super.getVariable(variable, includeClassMembers);
		if (v != null) return v;

		// Search in fields
		if (variable.equals("class")) return new LeekVariable(new Token("class"), VariableType.THIS_CLASS);
		if (!isStatic) {
			if (variable.equals("this")) return new LeekVariable(new Token("this"), VariableType.THIS);
			if (includeClassMembers) {
				v = clazz.getMember(variable);
				if (v != null) return v;
			}
		}
		if (includeClassMembers) {
			v = clazz.getStaticMember(variable);
			if (v != null) return v;
		}
		return null;
	}

	@Override
	public String getCode() {
		String str = "(";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0) str += ", ";
			str += this.type.getArgument(i).getCode() + " ";
			str += mParameters.get(i);
			if (defaultValues.get(i) != null) {
				str += " = " + defaultValues.get(i).toString();
			}
		}
		return str + ") {\n" + super.getCode() + "}\n";
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialFunction = compiler.getCurrentFunction();
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentFunction(this);
		compiler.setCurrentBlock(this);
		for (var value : defaultValues) {
			if (value != null) {
				value.preAnalyze(compiler);
			}
		}
		for (var instruction : mInstructions) {
			instruction.preAnalyze(compiler);
		}
		compiler.setCurrentBlock(initialBlock);
		compiler.setCurrentFunction(initialFunction);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(this);
		for (int i = 0; i < defaultValues.size(); ++i) {
			var value = defaultValues.get(i);
			if (value != null) {
				value.analyze(compiler);
				FunctionBlock.checkDefaultValueType(compiler, mParameters.get(i).getWord(), this.type.getArgument(i), value);
			}
		}
		super.analyze(compiler);
		// @tailrec : après l'analyse du corps, tout appel récursif collecté doit être
		// en position terminale, sinon erreur dure pointant l'appel fautif (mirror
		// FunctionBlock.analyze). Inactif sans l'annotation.
		if (recursiveCalls != null && hasAnnotation(Annotation.TAILREC) && leekscript.common.LanguageFeature.TAILREC_CHECK.enabledIn(compiler.getVersion())) {
			for (var call : recursiveCalls) {
				if (!call.isTailPosition()) {
					compiler.addError(new AnalyzeError(call.getLocation(), AnalyzeErrorLevel.ERROR, Error.ANNOTATION_TAILREC_NOT_TAIL, new String[] { getName() }));
				}
			}
		}
		compiler.setCurrentFunction(initialFunction);
	}

	@Override
	public void checkEndBlock() {

	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {

		// writer.addLine("", getLocation());

		super.writeJavaCode(mainblock, writer, false);
		if (mEndInstruction == 0) {
			writer.addLine("return " + type.returnType().getDefaultValue(writer, mainblock.getVersion()) + ";");
		}
	}

	/**
	 * Classification du corps après pliage des constantes, pour l'élimination des
	 * appels de méthodes STATIQUES en position d'instruction (cf
	 * ConstantFolder.classifyBody). Le corps s'exécute dans le contexte de la
	 * classe : ses champs private/protected sont pliables (parité runtime).
	 */
	private ConstantFolder.ClassifiedBody bodyClassification = null;

	public ConstantFolder.ClassifiedBody getBodyClassification(MainLeekBlock mainblock) {
		if (bodyClassification == null) {
			bodyClassification = mainblock.getVersion() < 2 ? ConstantFolder.ClassifiedBody.NONE
				: ConstantFolder.classifyBody(mInstructions, defaultValues, type.returnType(), clazz);
		}
		return bodyClassification;
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return this.clazz;
	}

	public List<Token> getParameters() {
		return mParameters;
	}

	public List<Expression> getDefaultValues() {
		return defaultValues;
	}

	public ArrayList<LeekVariableDeclarationInstruction> getParametersDeclarations() {
		return mParameterDeclarations;
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
		return type;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	public boolean isInConstructor() {
		if (isConstructor) {
			return true;
		}
		if (mParent != null) {
			return mParent.isInConstructor();
		}
		return false;
	}

	@Override
	public boolean isInStaticMethod() {
		// Le contexte statique s'arrête à la méthode : un constructeur ou une méthode
		// d'instance possède un `this`, une méthode statique non.
		return isStatic;
	}

	public int getMinParameters() {
		return minParameters;
	}

	public int getMaxParameters() {
		return maxParameters;
	}
}
