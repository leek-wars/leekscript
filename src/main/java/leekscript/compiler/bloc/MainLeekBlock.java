package leekscript.compiler.bloc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.LeekScript;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.WordParser;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekGlobalDeclarationInstruction;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.runner.LeekFunctions;

public class MainLeekBlock extends AbstractLeekBlock {

	private final ArrayList<String> mGlobales = new ArrayList<>();
	private final ArrayList<LeekGlobalDeclarationInstruction> mGlobalesDeclarations = new ArrayList<>();
	private final HashSet<String> mRedefinedFunctions = new HashSet<String>();
	private final ArrayList<FunctionBlock> mFunctions = new ArrayList<FunctionBlock>();
	private final ArrayList<AnonymousFunctionBlock> mAnonymousFunctions = new ArrayList<AnonymousFunctionBlock>();
	private final Map<String, Integer> mUserFunctions = new TreeMap<String, Integer>();
	private final Map<String, ClassDeclarationInstruction> mUserClasses = new TreeMap<String, ClassDeclarationInstruction>();
	private int mMinLevel = 1;
	private int mAnonymousId = 1;
	private int mFunctionId = 1;

	private final ArrayList<Integer> mIncluded = new ArrayList<Integer>();

	private int mCounter = 0;
	private int mCountInstruction = 0;
	private final IACompiler mCompiler;
	private String mAIName;

	@Override
	public int getCount() {
		return mCounter++;
	}

	public MainLeekBlock(IACompiler compiler, AIFile<?> ai) {
		super(null, null, 0, null);
		// On ajoute l'IA pour pas pouvoir l'include
		mIncluded.add(ai.getId());
		mAIName = ai.getPath();
		mCompiler = compiler;
		mCompiler.setCurrentAI(ai);
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

	public boolean includeAI(WordCompiler compiler, String path) throws LeekCompilerException {
		try {
			AIFile<?> ai = LeekScript.getResolver().resolve(path, mCompiler.getCurrentAI().getContext());
			if (mIncluded.contains(ai.getId())) {
				return true;
			}
			// System.out.println("include " + ai.getPath());
			mIncluded.add(ai.getId());
			AIFile<?> previousAI = mCompiler.getCurrentAI();
			mCompiler.setCurrentAI(ai);
			WordParser words = new WordParser(ai, compiler.getVersion());
			WordCompiler newCompiler = new WordCompiler(words, this, ai, compiler.getVersion());
			newCompiler.readCode();
			compiler.addErrors(newCompiler.getErrors());
			mCompiler.setCurrentAI(previousAI);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	public boolean hasUserFunction(String name, boolean use_declarations) {
		for (FunctionBlock block : mFunctions) {
			if (block.getName().equals(name))
				return true;
		}
		if (use_declarations && mUserFunctions.containsKey(name))
			return true;
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

	public int getUserFunctionParametersCount(String name) {

		for (FunctionBlock block : mFunctions) {
			if (block.getName().equals(name))
				return block.countParameters();
		}
		var f = mUserFunctions.get(name);
		if (f != null) return f;
		return -1;
	}

	public FunctionBlock getUserFunction(String name) {
		for (FunctionBlock block : mFunctions) {
			if (block.getName().equals(name))
				return block;
		}
		return null;
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
		mFunctions.add(block);
	}

	@Override
	public String getCode() {
		String str = "";
		for (LeekInstruction instruction : mFunctions) {
			str += instruction.getCode() + "\n";
		}
		for (LeekInstruction instruction : mAnonymousFunctions) {
			str += instruction.getCode() + "\n";
		}
		return str + super.getCode();
	}

	public void writeJavaCode(JavaWriter writer, String className, String AIClass) {
		writer.addLine("import leekscript.runner.*;");
		writer.addLine("import leekscript.runner.values.*;");
		writer.addLine();
		writer.addLine("public class " + className + " extends " + AIClass + " {");
		writer.addLine("public " + className + "() throws LeekRunException {");
		writer.addLine("super();");
		writer.addLine("mInstructions = " + mInstructions.size() + ";");
		writer.addLine("}");

		// Variables globales
		for (String global : mGlobales) {
			writer.addLine("private VariableLeekValue globale_" + global + " = new VariableLeekValue(mUAI);");
			writer.addLine("private boolean globale_init_" + global + " = false;");
		}
		// Fonctions redéfinies
		for (String redefined : mRedefinedFunctions) {
			writer.addCode("private VariableLeekValue rfunction_");
			writer.addCode(redefined);
			writer.addLine(";");
		}
		// Fonctions
		for (LeekInstruction instruction : mFunctions) {
			instruction.writeJavaCode(this, writer);
		}
		for (var clazz : mUserClasses.values()) {
			clazz.declareJava(this, writer);
		}
		/*
		 * for(LeekInstruction instruction : mAnonymousFunctions){
		 * instruction.writeJavaCode(this, writer); }
		 */
		writer.addLine("public AbstractLeekValue runIA() throws LeekRunException { resetCounter();");

		for (var clazz : mUserClasses.values()) {
			clazz.writeJavaCode(this, writer);
		}

		super.writeJavaCode(this, writer);
		if (mEndInstruction == 0)
			writer.addLine("return LeekValueManager.NULL;");
		writer.addLine("}");

		writer.writeErrorFunction(mCompiler, mAIName);
		printFunctionInformations(writer);

		if (mRedefinedFunctions.size() > 0) {
			writer.addCode("protected void init() throws LeekRunException {\n");
			for (String redefined : mRedefinedFunctions) {
				FunctionBlock user_function = getUserFunction(redefined);
				writer.addCode("rfunction_");
				writer.addCode(redefined);
				writer.addCode(" = new VariableLeekValue(mUAI, ");
				if (user_function != null) {
					writer.addCode("new FunctionLeekValue(");
					writer.addCode(String.valueOf(user_function.getId()));
					writer.addCode(")");
				} else {
					String namespace = LeekFunctions.getNamespace(redefined);
					writer.addCode("new FunctionLeekValue(" + namespace + ".");
					writer.addCode(redefined);
					writer.addCode(")");
				}
				writer.addLine(");");
			}
			writer.addCode("}");
		}
		writer.addLine("}");
	}

	public void printFunctionInformations(JavaWriter writer) {
		// Compteur de parametres
		writer.addLine("public int userFunctionCount(int id){");
		writer.addLine("switch(id){");
		for (FunctionBlock f : mFunctions) {
			writer.addLine("case " + f.getId() + ": return " + f.countParameters() + ";");
		}
		writer.addLine("} return -1; }");
		// Références
		writer.addLine("public boolean[] userFunctionReference(int id) {");
		writer.addLine("switch(id) {");
		for (FunctionBlock f : mFunctions) {
			writer.addLine("case " + f.getId() + ": return new boolean[]" + f.referenceArray() + ";");
		}
		writer.addLine("} return null; }");
		// Execute
		writer.addLine("public AbstractLeekValue userFunctionExecute(int id, AbstractLeekValue[] value) throws LeekRunException {");
		writer.addLine("switch(id){");
		for (FunctionBlock f : mFunctions) {
			String params = "";
			for (int i = 0; i < f.countParameters(); i++) {
				if (i != 0)
					params += ",";
				params += "value[" + i + "]";
			}
			writer.addLine("case " + f.getId() + ": return user_function_" + f.getName() + "(" + params + ");");
		}
		writer.addLine("} return null; }");

		writer.addLine("public int anonymousFunctionCount(int id){");
		writer.addLine("switch(id){");
		for (AnonymousFunctionBlock f : mAnonymousFunctions) {
			writer.addLine("case " + f.getId() + ": return " + f.countParameters() + ";");
		}
		writer.addLine("} return -1; }");
		// Références
		writer.addLine("public boolean[] anonymousFunctionReference(int id){");
		writer.addLine("switch(id){");
		for (AnonymousFunctionBlock f : mAnonymousFunctions) {
			writer.addLine("case " + f.getId() + ": return new boolean[]" + f.referenceArray() + ";");
		}
		writer.addLine("} return null; }");
		writer.addLine("public int getVersion() {");
		writer.addLine("return " + mCompiler.getCurrentAI().getVersion() + ";");
		writer.addLine("}");
		// Execute
		/*
		 * writer.addLine(
		 * "public AbstractLeekValue anonymousFunctionExecute(int id, AbstractLeekValue[] value) throws Exception{"
		 * ); writer.addLine("switch(id){"); for(AnonymousFunctionBlock f :
		 * mAnonymousFunctions){ String params = ""; for(int i = 0; i <
		 * f.countParameters(); i++){ if(i != 0) params += ","; params +=
		 * "value[" + i + "]"; } writer.addLine("case " + f.getId() +
		 * ": return anonymous_function_" + f.getId() + "(" + params + ");"); }
		 * writer.addLine("} return null; }");
		 */
		/*
		 * public abstract int userFunctionCount(int id);
		 *
		 * public abstract boolean[] userFunctionReference(int id);
		 *
		 * public abstract AbstractLeekValue userFunctionExecute(int id,
		 * AbstractLeekValue[] value);
		 *
		 * public abstract int anonymousFunctionCount(int id);
		 *
		 * public abstract boolean[] anonymousFunctionReference(int id);
		 *
		 * public abstract AbstractLeekValue anonymousFunctionExecute(int id,
		 * AbstractLeekValue[] value);
		 */
	}

	public List<Integer> getIncludedAIs() {
		return mIncluded;
	}

	public IACompiler getCompiler() {
		return mCompiler;
	}

	public boolean hasUserClass(String name) {
		return mUserClasses.containsKey(name);
	}

	public void addClass(ClassDeclarationInstruction classDeclaration) {
		mUserClasses.put(classDeclaration.getName(), classDeclaration);
	}

	public ClassDeclarationInstruction getUserClass(String name) {
		return mUserClasses.get(name);
	}

	public void analyze(WordCompiler compiler) {
		for (var clazz : mUserClasses.values()) {
			clazz.declare(compiler);
		}
		for (var function : mFunctions) {
			function.declare(compiler);
		}
		for (var global : mGlobalesDeclarations) {
			global.declare(compiler);
		}
		for (var clazz : mUserClasses.values()) {
			clazz.analyze(compiler);
		}
		for (var function : mFunctions) {
			function.analyze(compiler);
		}
		super.analyze(compiler);
	}
}
