package leekscript.compiler.instruction;

import java.util.HashMap;
import java.util.Map.Entry;

import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.ClassMethodBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;

public class ClassDeclarationInstruction implements LeekInstruction {

	private final IAWord token;
	private IAWord parentToken;
	private ClassDeclarationInstruction parent;
	private HashMap<String, AbstractExpression> fields = new HashMap<>();
	private HashMap<String, AbstractExpression> staticFields = new HashMap<>();
	private HashMap<String, LeekVariable> fieldVariables = new HashMap<>();
	private HashMap<String, LeekVariable> staticFieldVariables = new HashMap<>();
	private HashMap<String, LeekVariable> methodVariables = new HashMap<>();
	private HashMap<String, LeekVariable> staticMethodVariables = new HashMap<>();
	private HashMap<Integer, ClassMethodBlock> constructors = new HashMap<>();
	private HashMap<String, HashMap<Integer, ClassMethodBlock>> methods = new HashMap<>();
	private HashMap<String, HashMap<Integer, ClassMethodBlock>> staticMethods = new HashMap<>();

	public ClassDeclarationInstruction(IAWord token, int line, AIFile<?> ai) {
		this.token = token;
	}

	public HashMap<String, AbstractExpression> getFields() {
		return fields;
	}
	public HashMap<String, AbstractExpression> getStaticFields() {
		return staticFields;
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

	@Override
	public String getCode() {
		return "class";
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	public void setParent(IAWord userClass) {
		this.parentToken = userClass;
	}

	public boolean hasConstructor(int param_count) {
		return constructors.containsKey(param_count);
	}

	public void addConstructor(ClassMethodBlock block) {
		constructors.put(block.countParameters(), block);
	}

	public void addMethod(IAWord token, ClassMethodBlock method) {
		if (!methods.containsKey(token.getWord())) {
			methods.put(token.getWord(), new HashMap<>());
			methodVariables.put(token.getWord(), new LeekVariable(token, VariableType.METHOD));
		}
		methods.get(token.getWord()).put(method.countParameters(), method);
	}

	public boolean hasMethod(String name, int param_count) {
		if (name.equals("constructor")) {
			return hasConstructor(param_count);
		}
		return methods.containsKey(name + "_" + param_count);
	}

	public void addStaticMethod(String name, ClassMethodBlock method) {
		if (!staticMethods.containsKey(name)) {
			staticMethods.put(name, new HashMap<>());
			staticMethodVariables.put(name, new LeekVariable(token, VariableType.STATIC_METHOD));
		}
		staticMethods.get(name).put(method.countParameters(), method);
	}

	public void addField(WordCompiler compiler, IAWord word, AbstractExpression expr) throws LeekCompilerException {
		if (fields.containsKey(word.getWord()) || staticFields.containsKey(word.getWord())) {
			compiler.addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, LeekCompilerException.FIELD_ALREADY_EXISTS));
			return;
		}
		fields.put(word.getWord(), expr);
		fieldVariables.put(word.getWord(), new LeekVariable(word, VariableType.FIELD));
	}

	public void addStaticField(IAWord word, AbstractExpression expr) throws LeekCompilerException {
		if (staticFields.containsKey(word.getWord()) || fields.containsKey(word.getWord())) {
			throw new LeekCompilerException(word, LeekCompilerException.FIELD_ALREADY_EXISTS);
		}
		staticFields.put(word.getWord(), expr);
		staticFieldVariables.put(word.getWord(), new LeekVariable(word, VariableType.STATIC_FIELD));
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la classe
		compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.CLASS, this));
	}

	public void analyze(WordCompiler compiler) {
		compiler.setCurrentClass(this);
		// Parent
		if (parentToken != null) {
			var parentVar = compiler.getCurrentBlock().getVariable(this.parentToken.getWord(), true);
			if (parentVar == null) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, LeekCompilerException.UNKNOWN_VARIABLE_OR_FUNCTION));
			} else if (parentVar.getVariableType() != VariableType.CLASS) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, LeekCompilerException.UNKNOWN_VARIABLE_OR_FUNCTION));
			} else {
				this.parent = parentVar.getClassDeclaration();
			}
		}

		for (var constructor : constructors.values()) {
			constructor.analyze(compiler);
		}
		for (var method : methods.values()) {
			for (var version : method.values()) {
				version.analyze(compiler);
			}
		}
		for (var method : staticMethods.values()) {
			for (var version : method.values()) {
				version.analyze(compiler);
			}
		}
		compiler.setCurrentClass(null);
	}

	public void declareJava(MainLeekBlock mainblock, JavaWriter writer) {
		// Declare the class as a field of the AI
		String className = "user_" + token.getWord();
		writer.addLine("private ClassLeekValue " + className + " = new ClassLeekValue(\"" + token.getWord() + "\");");
	}

	public void createJava(MainLeekBlock mainblock, JavaWriter writer) {
		// Create the class in the constructor of the AI
		String className = "user_" + token.getWord();

		if (parent != null) {
			writer.addLine(className + ".setParent(user_" + parent.getName() + ");");
		}

		for (Entry<String, HashMap<Integer, ClassMethodBlock>> method : staticMethods.entrySet()) {
			for (Entry<Integer, ClassMethodBlock> version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode("final LeekAnonymousFunction " + methodName + " = new LeekAnonymousFunction() {");
				writer.addLine("public AbstractLeekValue run(AI mUAI, AbstractLeekValue u_this, AbstractLeekValue... values) throws LeekRunException {");
				writer.addLine("final var u_class = " + className + ";", version.getValue().getLine(), version.getValue().getFile());
				if (parent != null) {
					writer.addLine("final var u_super = user_" + parent.token.getWord() + ";");
				}
				version.getValue().writeJavaCode(mainblock, writer);
				writer.addLine("}};");
			}
		}

		for (Entry<String, HashMap<Integer, ClassMethodBlock>> method : methods.entrySet()) {
			for (Entry<Integer, ClassMethodBlock> version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode("final LeekAnonymousFunction " + methodName + " = new LeekAnonymousFunction() {");
				writer.addLine("public AbstractLeekValue run(AI mUAI, AbstractLeekValue u_this, AbstractLeekValue... values) throws LeekRunException {");
				writer.addLine("final var u_class = " + className + ";", version.getValue().getLine(), version.getValue().getFile());
				if (parent != null) {
					writer.addLine("final var u_super = user_" + parent.token.getWord() + ";");
				}
				writer.addCounter(1);
				version.getValue().writeJavaCode(mainblock, writer);
				writer.addLine("}};");
			}
		}

		for (Entry<String, AbstractExpression> field : staticFields.entrySet()) {
			writer.addCode(className);
			writer.addCode(".addStaticField(mUAI, \"" + field.getKey() + "\", ");
			if (field.getValue() != null) {
				field.getValue().writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("LeekValueManager.NULL");
			}
			writer.addLine(");");
		}

		writeFields(mainblock, writer, className);

		for (Entry<String, HashMap<Integer, ClassMethodBlock>> method : staticMethods.entrySet()) {
			for (Entry<Integer, ClassMethodBlock> version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode(className);
				writer.addLine(".addStaticMethod(\"" + method.getKey() + "\", " + version.getKey() + ", " + methodName + ");");
			}
			writer.addCode(className);
			writer.addLine(".addGenericStaticMethod(\"" + method.getKey() + "\");");
		}

		for (Entry<Integer, ClassMethodBlock> construct : constructors.entrySet()) {
			writer.addCode(className);
			writer.addLine(".addConstructor(" + construct.getKey() + ", new LeekAnonymousFunction() {");
			writer.addLine("public AbstractLeekValue run(AI mUAI, AbstractLeekValue u_this, AbstractLeekValue... values) throws LeekRunException {");
			writer.addLine("final var u_class = " + className + ";");
			if (parent != null) {
				writer.addLine("final var u_super = user_" + parent.token.getWord() + ";");
			}
			construct.getValue().writeJavaCode(mainblock, writer);
			writer.addLine("}});");
		}

		for (Entry<String, HashMap<Integer, ClassMethodBlock>> method : methods.entrySet()) {
			for (Entry<Integer, ClassMethodBlock> version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode(className);
				writer.addLine(".addMethod(\"" + method.getKey() + "\", " + version.getKey() + ", " + methodName + ");");
			}
			writer.addCode(className);
			writer.addLine(".addGenericMethod(\"" + method.getKey() + "\");");
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

	}

	private void writeFields(MainLeekBlock mainblock, JavaWriter writer, String className) {

		if (parent != null) {
			parent.writeFields(mainblock, writer, className);
		}

		for (Entry<String, AbstractExpression> field : fields.entrySet()) {
			writer.addCode(className);
			writer.addCode(".addField(mUAI, \"" + field.getKey() + "\", ");
			if (field.getValue() != null) {
				field.getValue().writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("LeekValueManager.NULL");
			}
			writer.addLine(");");
		}
	}

	public IAWord getParentToken() {
		return parentToken;
	}

	public ClassDeclarationInstruction getParent() {
		return parent;
	}

	public boolean hasMember(IAWord field) {
		return getMember(field) != null;
	}

	public boolean hasStaticMember(IAWord field) {
		return getStaticMember(field) != null;
	}

	public LeekVariable getMember(IAWord token) {
		var f = fieldVariables.get(token.getWord());
		if (f != null) return f;

		var m = methodVariables.get(token.getWord());
		if (m != null) return m;

		if (parent != null) {
			return parent.getMember(token);
		}
		return null;
	}

	public LeekVariable getStaticMember(IAWord token) {
		var f = staticFieldVariables.get(token.getWord());
		if (f != null) return f;

		var m = staticMethodVariables.get(token.getWord());
		if (m != null) return m;

		if (parent != null) {
			return parent.getStaticMember(token);
		}
		return null;
	}
}
