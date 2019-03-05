package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import leekscript.compiler.IACompiler;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.LeekScript;
import leekscript.compiler.instruction.LeekInstruction;
import leekscript.runner.AI;

public class MainLeekBlock extends AbstractLeekBlock {

	private final ArrayList<String> mGobales = new ArrayList<String>();
	private final ArrayList<String> mGobalesDeclarations = new ArrayList<String>();
	private final HashSet<String> mRedefinedFunctions = new HashSet<String>();
	private final ArrayList<FunctionBlock> mFunctions = new ArrayList<FunctionBlock>();
	private final ArrayList<AnonymousFunctionBlock> mAnonymousFunctions = new ArrayList<AnonymousFunctionBlock>();
	private final Map<String, Integer> mUserFunctions = new TreeMap<String, Integer>();
	private int mMinLevel = 1;
	private int mAnonymousId = 1;
	private int mFunctionId = 1;

	private final ArrayList<String> mIncluded = new ArrayList<String>();

	private int mCounter = 0;
	private int mCountInstruction = 0;
	private final IACompiler mCompiler = new IACompiler();

	@Override
	public int getCount() {
		return mCounter++;
	}

	public MainLeekBlock(String ai) {
		super(null, null, 0, 0);
		// On ajoute l'IA pour pas pouvoir l'include
		mIncluded.add(ai);
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

	public List<String> getIncludes() {
		return mIncluded;
	}

	public int getMinLevel() {
		return mMinLevel;
	}

	public void setMinLevel(int min_level) {
		this.mMinLevel = min_level;
	}

	public boolean includeAI(String path) throws Exception {
		if (mIncluded.contains(path)) {
			return true;
		}
		AI ai = LeekScript.compile(1213, path, "AI");
		if (ai == null) {
			return false;
		}
		mIncluded.add(path);
		return true;
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

	public void addGlobalDeclaration(String declaration) {
		mGobalesDeclarations.add(declaration);
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
		return mUserFunctions.get(name);
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
		if (mGobalesDeclarations.contains(globale))
			return true;
		return mGobales.contains(globale);
	}

	@Override
	public boolean hasDeclaredGlobal(String globale) {
		return mGobales.contains(globale);
	}

	@Override
	public void addGlobal(String globale) {
		mGobales.add(globale);
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
		writer.addLine("public class " + className + " extends " + AIClass + " {");
		writer.addLine("public " + className + "() throws Exception{ super(); }");
		// Variables globales
		for (String global : mGobales) {
			writer.addLine("private VariableLeekValue globale_" + global + " = null;");
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
		/*
		 * for(LeekInstruction instruction : mAnonymousFunctions){
		 * instruction.writeJavaCode(this, writer); }
		 */
		writer.addLine("public AbstractLeekValue runIA() throws Exception{ resetCounter();");
		super.writeJavaCode(this, writer);
		if (mEndInstruction == 0)
			writer.addLine("return LeekValueManager.NULL;");
		writer.addLine("}");

		TreeMap<Integer, String> ais = new TreeMap<Integer, String>();
		// for (String path : mIncluded) {
		// 	LeekAI ai = mCompiler.getAI(id);
		// 	if (ai == null)
		// 		continue;
		// 	ais.put(ai.getId(), ai.getName());
		// }
		writer.writeErrorFunction(mCompiler, ais);
		printFunctionInformations(writer);

		if (mRedefinedFunctions.size() > 0) {
			writer.addCode("protected void init() throws Exception{");
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
					writer.addCode("new FunctionLeekValue(LeekFunctions.");
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
		writer.addLine("public boolean[] userFunctionReference(int id){");
		writer.addLine("switch(id){");
		for (FunctionBlock f : mFunctions) {
			writer.addLine("case " + f.getId() + ": return new boolean[]" + f.referenceArray() + ";");
		}
		writer.addLine("} return null; }");
		// Execute
		writer.addLine("public AbstractLeekValue userFunctionExecute(int id, AbstractLeekValue[] value) throws Exception{");
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
}
