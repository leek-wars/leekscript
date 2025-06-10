package leekscript.compiler.bloc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Options;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekBigInteger;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekInteger;
import leekscript.compiler.expression.LeekReal;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekGlobalDeclarationInstruction;
import leekscript.runner.LeekFunctions;

public class MainLeekBlock extends AbstractLeekBlock {

	private final ArrayList<String> mGlobales = new ArrayList<>();
	private final ArrayList<LeekGlobalDeclarationInstruction> mGlobalesDeclarations = new ArrayList<>();
	private final HashSet<String> mRedefinedFunctions = new HashSet<String>();
	private final HashMap<String, FunctionBlock> mFunctions = new HashMap<>();
	private final ArrayList<AnonymousFunctionBlock> mAnonymousFunctions = new ArrayList<AnonymousFunctionBlock>();
	private final Map<String, Integer> mUserFunctions = new TreeMap<String, Integer>();
	private final Map<String, ClassDeclarationInstruction> mDefinedClasses = new TreeMap<String, ClassDeclarationInstruction>();
	private final Map<String, ClassDeclarationInstruction> mUserClasses = new TreeMap<String, ClassDeclarationInstruction>();
	private final List<ClassDeclarationInstruction> mUserClassesList = new ArrayList<>();
	private int mMinLevel = 1;
	private int mAnonymousId = 1;
	private int mFunctionId = 1;
	private final Set<AIFile> mIncluded = new HashSet<AIFile>();
	private final Set<AIFile> mIncludedFirstPass = new HashSet<AIFile>();
	private int mCounter = 0;
	private int mCountInstruction = 0;
	private final IACompiler mCompiler;
	private String mAIName;
	private String className;
	private WordCompiler wordCompiler;

	public MainLeekBlock(IACompiler compiler, WordCompiler wordCompiler, AIFile ai) throws LeekCompilerException {
		super(null, null);

		// On ajoute l'IA pour pas pouvoir l'include
		mIncluded.add(ai);
		mIncludedFirstPass.add(ai);
		mAIName = ai.getPath();
		mCompiler = compiler;
		mCompiler.setCurrentAI(ai);

		if (ai.getVersion() >= 2) {
			var classClass = new ClassDeclarationInstruction(new Token("Class"), 0, ai, true, this);
			classClass.addField(wordCompiler, new Token("name"), Type.STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("super"), Type.CLASS, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("fields"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("staticFields"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("methods"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("staticMethods"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			addClass(classClass);
		}

		if (ai.getVersion() >= 3) {

			var valueClass = new ClassDeclarationInstruction(new Token("Value"), 0, ai, true, this, Type.ANY);
			valueClass.addField(wordCompiler, new Token("class"), Type.CLASS, null, AccessLevel.PUBLIC, true);
			addClass(valueClass);

			addClass(new ClassDeclarationInstruction(new Token("Null"), 0, ai, true, this, Type.NULL));
			addClass(new ClassDeclarationInstruction(new Token("Boolean"), 0, ai, true, this, Type.BOOL));

			var integerClass = new ClassDeclarationInstruction(new Token("Integer"), 0, ai, true, this, Type.INT);
			integerClass.addStaticField(wordCompiler, new Token("MIN_VALUE"), Type.INT, new LeekInteger(new Token(""), Long.MIN_VALUE), AccessLevel.PUBLIC, true);
			integerClass.addStaticField(wordCompiler, new Token("MAX_VALUE"), Type.INT, new LeekInteger(new Token(""), Long.MAX_VALUE), AccessLevel.PUBLIC, true);
			addClass(integerClass);

			var realClass = new ClassDeclarationInstruction(new Token("Real"), 0, ai, true, this, Type.REAL);
			realClass.addStaticField(wordCompiler, new Token("MIN_VALUE"), Type.REAL, new LeekReal(new Token(""), Double.MIN_VALUE), AccessLevel.PUBLIC, true);
			realClass.addStaticField(wordCompiler, new Token("MAX_VALUE"), Type.REAL, new LeekReal(new Token(""), Double.MAX_VALUE), AccessLevel.PUBLIC, true);
			addClass(realClass);
			
			if (ai.getVersion() >= 4) {
				addClass(new ClassDeclarationInstruction(new Token("BigInteger"), 0, ai, true, this, Type.BIG_INT));
			}
			
			addClass(new ClassDeclarationInstruction(new Token("Number"), 0, ai, true, this, Type.INT_OR_REAL));
			addClass(new ClassDeclarationInstruction(new Token("Array"), 0, ai, true, this, Type.ARRAY, Type.EMPTY_ARRAY));
			if (ai.getVersion() >= 4) {
				addClass(new ClassDeclarationInstruction(new Token("Map"), 0, ai, true, this, Type.MAP, Type.EMPTY_MAP));
			}
			addClass(new ClassDeclarationInstruction(new Token("Interval"), 0, ai, true, this, Type.INTERVAL, Type.EMPTY_INTERVAL));
			addClass(new ClassDeclarationInstruction(new Token("Set"), 0, ai, true, this, Type.SET, Type.EMPTY_SET));
			addClass(new ClassDeclarationInstruction(new Token("String"), 0, ai, true, this, Type.STRING));
			var objectClass = new ClassDeclarationInstruction(new Token("Object"), 0, ai, true, this, Type.OBJECT);
			objectClass.addMethod(wordCompiler, new Token("keys"), new ClassMethodBlock(objectClass, false, false, wordCompiler.getCurrentBlock(), this, new Token("keys"), Type.ARRAY), AccessLevel.PUBLIC);
			addClass(objectClass);
			addClass(new ClassDeclarationInstruction(new Token("Function"), 0, ai, true, this, Type.FUNCTION));
			addClass(new ClassDeclarationInstruction(new Token("JSON"), 0, ai, true, this));
			addClass(new ClassDeclarationInstruction(new Token("System"), 0, ai, true, this));
		}
	}

	@Override
	public int getCount() {
		return mCounter++;
	}

	public void addRedefinedFunction(String function) {
		mRedefinedFunctions.add(function);
	}

	public boolean isRedefinedFunction(String function) {
		return mRedefinedFunctions.contains(function);
	}

	public void addInstruction() {
		mCountInstruction++;
	}

	public int getInstructionsCount() {
		return mCountInstruction;
	}

	public int getMinLevel() {
		return mMinLevel;
	}

	public void setMinLevel(int min_level) {
		this.mMinLevel = min_level;
	}

	public boolean includeAIFirstPass(WordCompiler compiler, String path) throws LeekCompilerException {
		try {
			var ai = mCompiler.getCurrentAI().getFolder().resolve(path);
			if (mIncludedFirstPass.contains(ai)) {
				return true;
			}
			// Hack dégueu à retirer, crash du daemon dans un cas précis d'include infini
			if (mIncludedFirstPass.size() > 500) {
				throw new LeekCompilerException(compiler.getTokenStream().get(), Error.UNKNOWN_ERROR);
			}
			ai.clearErrors();
			mIncludedFirstPass.add(ai);
			var previousAI = mCompiler.getCurrentAI();
			mCompiler.setCurrentAI(ai);
			WordCompiler newCompiler = new WordCompiler(ai, compiler.getVersion(), compiler.getOptions());
			newCompiler.setMainBlock(this);
			newCompiler.firstPass();
			compiler.getAI().getErrors().addAll(ai.getErrors());
			mCompiler.setCurrentAI(previousAI);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	public boolean includeAI(WordCompiler compiler, String path) throws LeekCompilerException {
		try {
			var ai = mCompiler.getCurrentAI().getFolder().resolve(path);
			if (mIncluded.contains(ai)) {
				return true;
			}
			// Hack dégueu à retirer, crash du daemon dans un cas précis d'include infini
			if (mIncluded.size() > 500) {
				throw new LeekCompilerException(compiler.getTokenStream().get(), Error.UNKNOWN_ERROR);
			}
			// ai.clearErrors();
			mIncluded.add(ai);
			var previousAI = mCompiler.getCurrentAI();
			mCompiler.setCurrentAI(ai);
			WordCompiler newCompiler = new WordCompiler(ai, compiler.getVersion(), compiler.getOptions());
			newCompiler.setMainBlock(this);
			newCompiler.secondPass();
			compiler.getAI().getErrors().addAll(ai.getErrors());
			mCompiler.setCurrentAI(previousAI);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	public boolean hasUserFunction(String name, boolean use_declarations) {
		if (mFunctions.containsKey(name)) {
			return true;
		}
		if (use_declarations && mUserFunctions.containsKey(name)) {
			return true;
		}
		return false;
	}

	public void addFunctionDeclaration(String name, int count_param) {
		mUserFunctions.put(name, count_param);
	}

	public void addGlobalDeclaration(LeekGlobalDeclarationInstruction declaration) {
		mGlobalesDeclarations.add(declaration);
	}

	public void addAnonymousFunction(AnonymousFunctionBlock block) {
		block.setId(mAnonymousId);
		mAnonymousFunctions.add(block);
		mAnonymousId++;
	}

	public FunctionBlock getUserFunction(String name) {
		return mFunctions.get(name);
	}

	@Override
	public boolean hasGlobal(String globale) {
		return mGlobales.contains(globale);
	}

	@Override
	public boolean hasDeclaredGlobal(String globale) {
		return mGlobales.contains(globale);
	}

	@Override
	public void addGlobal(String variable) {
		mGlobales.add(variable);
	}

	public void addFunction(FunctionBlock block) {
		block.setId(mFunctionId);
		mFunctionId++;
		mFunctions.put(block.getName(), block);
	}

	@Override
	public String getCode() {
		String str = "";
		for (var instruction : mFunctions.values()) {
			str += instruction.getCode() + "\n";
		}
		for (var clazz : mUserClasses.values()) {
			if (clazz.internal) continue;
			str += clazz.getCode() + "\n";
		}
		return str + super.getCode();
	}

	public void writeJavaCode(JavaWriter writer, String className, String AIClass, Options options) {
		this.className = className;

		writer.addLine("import leekscript.runner.*;");
		writer.addLine("import leekscript.runner.values.*;");
		writer.addLine("import leekscript.runner.classes.*;");
		writer.addLine("import leekscript.common.*;");
		if (LeekFunctions.getExtraFunctionsImport() != null) {
			writer.addLine("import " + LeekFunctions.getExtraFunctionsImport() + ";");
		}
		writer.addLine();
		writer.addLine("public class " + className + " extends " + AIClass + " {");

		// Classes
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.declareJava(this, writer);
		}

		// Constructor
		writer.addLine("public " + className + "() throws LeekRunException {");
		writer.addLine("super(" + mInstructions.size() + ", " + mCompiler.getCurrentAI().getVersion() + ");");

		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.createJava(this, writer);
		}
		writer.addLine("}");

		// Classes initialize functions
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.writeCreateStaticFields(this, writer);
			clazz.writeInitializeStaticFields(this, writer);
		}

		// Static init
		writer.addLine("public void staticInit() throws LeekRunException {");

		// Create classes static fields
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.createStaticFields(this, writer);
		}

		// Initialize classes fields
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.initializeStaticFields(this, writer);
		}

		writer.addLine("}"); // Fin staticInit

		// Variables globales
		for (var global : mGlobalesDeclarations) {
			// System.out.println("declare global " + global.getName() + " type " + global.getType());
			if (getWordCompiler().getVersion() >= 2) {
				writer.addLine("private " + global.getType().getJavaPrimitiveName(getVersion()) + " g_" + global.getName() + " = " + global.getType().getDefaultValue(writer, getVersion()) + ";");
			} else {
				writer.addLine("private Box<" + global.getType().getJavaName(getVersion()) + "> g_" + global.getName() + " = new Box<" + global.getType().getJavaName(getVersion()) + ">(" + writer.getAIThis() + ");");
			}
			writer.addLine("private boolean g_init_" + global.getName() + " = false;");
		}
		// Fonctions redéfinies
		for (String redefined : mRedefinedFunctions) {
			writer.addCode("private Box rfunction_");
			writer.addCode(redefined);
			writer.addLine(";");
		}
		// Fonctions
		for (var instruction : mFunctions.values()) {
			instruction.writeJavaCode(this, writer);
		}

		writer.addLine("public Object runIA(Session session) throws LeekRunException {");
		writer.addLine("resetCounter();");

		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.writeJavaCode(this, writer);
		}

		// Import des variables de la session
		if (options.session() != null) {
			for (var variable : options.session().getVariables().keySet()) {
				writer.addLine("var u_" + variable + " = session.getVariable(\"" + variable + "\");");
			}
		}

		super.writeJavaCode(this, writer);

		writer.addLine("}");

		writer.writeErrorFunction(mCompiler, mAIName);
		printFunctionInformations(writer);

		if (mRedefinedFunctions.size() > 0) {
			writer.addCode("public void init() throws LeekRunException {\n");
			for (String redefined : mRedefinedFunctions) {
				FunctionBlock user_function = getUserFunction(redefined);
				writer.addCode("rfunction_");
				writer.addCode(redefined);
				writer.addCode(" = new Box(" + writer.getAIThis() + ", ");
				if (user_function != null) {
					user_function.compileAnonymousFunction(this, writer);
				} else {
					var system_function = LeekFunctions.getValue(redefined, writer.getOptions().useExtra());
					writer.generateAnonymousSystemFunction(system_function);
					writer.addCode(system_function.getStandardClass() + "_" + redefined);
				}
				writer.addLine(");");
			}
			writer.addCode("}");
		}

		writer.writeGenericFunctions(this);
		writer.writeAnonymousSystemFunctions(this);

		writer.addLine("}");
	}

	public void writeBeforeReturn(JavaWriter writer) {
		// Export des variables de la session
		if (writer.options.session() != null) {
			for (var variable : mVariables.entrySet()) {
				if (!writer.options.session().getVariables().containsKey(variable.getKey()) && variable.getValue().getVariableType() == VariableType.LOCAL) {
					writer.addLine("session.setVariable(" + writer.getAIThis() + ", \"" + variable.getKey() + "\", u_" + variable.getKey() + ");");
				}
			}
		}
	}

	public void printFunctionInformations(JavaWriter writer) {

	}

	public Set<AIFile> getIncludedAIs() {
		return mIncluded;
	}

	public IACompiler getCompiler() {
		return mCompiler;
	}

	public void defineClass(ClassDeclarationInstruction classDeclaration) {
		mUserClasses.put(classDeclaration.getName(), classDeclaration);
		mDefinedClasses.put(classDeclaration.getName(), classDeclaration);
	}

	public void addClassList(ClassDeclarationInstruction classDeclaration) {
		mUserClassesList.add(classDeclaration);
	}

	public void addClass(ClassDeclarationInstruction classDeclaration) {
		mUserClasses.put(classDeclaration.getName(), classDeclaration);
		mUserClassesList.add(classDeclaration);
		mDefinedClasses.put(classDeclaration.getName(), classDeclaration);
	}

	public ClassDeclarationInstruction getUserClass(String name) {
		return mUserClasses.get(name);
	}

	public ClassDeclarationInstruction getDefinedClass(String name) {
		return mDefinedClasses.get(name);
	}

	public Map<String, ClassDeclarationInstruction> getDefinedClasses() {
		return mDefinedClasses;
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		for (var clazz : mUserClassesList) {
			clazz.declare(compiler);
		}
		for (var function : mFunctions.values()) {
			function.declare(compiler);
		}
		for (var global : mGlobalesDeclarations) {
			global.declare(compiler);
		}
		for (var clazz : mUserClassesList) {
			clazz.preAnalyze(compiler);
		}
		for (var function : mFunctions.values()) {
			function.preAnalyze(compiler);
		}
		super.preAnalyze(compiler);
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		// for (var global : mGlobalesDeclarations) {
		// 	global.analyze(compiler);
		// }
		for (var clazz : mUserClassesList) {
			clazz.analyze(compiler);
		}
		for (var function : mFunctions.values()) {
			function.analyze(compiler);
		}
		super.analyze(compiler);
	}

	public String getClassName() {
		return className;
	}

	public void setWordCompiler(WordCompiler compiler) {
		this.wordCompiler = compiler;
		compiler.setMainBlock(this);
	}

	public WordCompiler getWordCompiler() {
		return this.wordCompiler;
	}

	public int getVersion() {
		return this.wordCompiler.getVersion();
	}

	@Override
	public Location getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isStrict() {
		return mCompiler.getCurrentAI().isStrict();
	}
}
