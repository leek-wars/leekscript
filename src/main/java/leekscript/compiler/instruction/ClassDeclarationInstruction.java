package leekscript.compiler.instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import leekscript.common.AccessLevel;
import leekscript.common.Error;

public class ClassDeclarationInstruction implements LeekInstruction {

	public static class ClassDeclarationField {

		AbstractExpression expression;
		AccessLevel level;

		public ClassDeclarationField(AbstractExpression expr, AccessLevel level) {
			this.expression = expr;
			this.level = level;
		}
	}

	public static class ClassDeclarationMethod {

		public ClassMethodBlock block;
		public AccessLevel level;

		public ClassDeclarationMethod(ClassMethodBlock block, AccessLevel level) {
			this.block = block;
			this.level = level;
		}
	}

	private final IAWord token;
	private IAWord parentToken;
	private ClassDeclarationInstruction parent;
	public boolean internal;
	private LinkedHashMap<String, ClassDeclarationField> fields = new LinkedHashMap<>();
	private LinkedHashMap<String, ClassDeclarationField> staticFields = new LinkedHashMap<>();
	private HashMap<String, LeekVariable> fieldVariables = new HashMap<>();
	private HashMap<String, LeekVariable> staticFieldVariables = new HashMap<>();
	private HashMap<String, LeekVariable> methodVariables = new HashMap<>();
	private HashMap<String, LeekVariable> staticMethodVariables = new HashMap<>();
	private HashMap<Integer, ClassDeclarationMethod> constructors = new HashMap<>();
	private HashMap<String, HashMap<Integer, ClassDeclarationMethod>> methods = new HashMap<>();
	private HashMap<String, HashMap<Integer, ClassDeclarationMethod>> staticMethods = new HashMap<>();

	public ClassDeclarationInstruction(IAWord token, int line, AIFile<?> ai, boolean internal) {
		this.token = token;
		this.internal = internal;
	}

	public HashMap<String, ClassDeclarationField> getFields() {
		return fields;
	}
	public HashMap<String, ClassDeclarationField> getStaticFields() {
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
		String r = "class " + token.getWord();
		if (parentToken != null) {
			r += " extends " + parentToken.getWord();
		}
		r += " {\n";

		for (Entry<String, ClassDeclarationField> field : staticFields.entrySet()) {
			r += "\t" + field.getValue().level.toString().toLowerCase() + " static " + field.getKey();
			if (field.getValue().expression != null) {
				r += " = " + field.getValue().expression.getString();
			}
			r += "\n";
		}
		r += "\n";

		for (var method : staticMethods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				r += "\t" + version.getValue().level.toString().toLowerCase() + " static " + method.getKey() + version.getValue().block.getCode();
			}
			r += "\n";
		}
		r += "\n";

		for (Entry<String, ClassDeclarationField> field : fields.entrySet()) {
			r += "\t" + field.getValue().level.toString().toLowerCase() + " " + field.getKey();
			if (field.getValue().expression != null) {
				r += " = " + field.getValue().expression.getString();
			}
			r += "\n";
		}
		r += "\n";

		for (var constructor : constructors.entrySet()) {
			r += "\t" + constructor.getValue().level.toString().toLowerCase() + " constructor" + constructor.getValue().block.getCode();
		}
		r += "\n";

		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				r += "\t" + version.getValue().level.toString().toLowerCase() + " " + method.getKey() + version.getValue().block.getCode();
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

	public void setParent(IAWord userClass) {
		this.parentToken = userClass;
	}

	public boolean hasConstructor(int param_count) {
		return constructors.containsKey(param_count);
	}

	public ClassDeclarationMethod getConstructor(int param_count) {
		// Search existing constructor
		var constructor = constructors.get(param_count);
		if (constructor != null) return constructor;

		// If constructor has 0 parameters, return the default implicit one
		if (param_count == 0) {
			return new ClassDeclarationMethod(null, AccessLevel.PUBLIC);
		}
		return null;
	}

	public void addConstructor(ClassMethodBlock block, AccessLevel level) {
		constructors.put(block.countParameters(), new ClassDeclarationMethod(block, level));
	}

	public void addMethod(WordCompiler compiler, IAWord token, ClassMethodBlock method, AccessLevel level) {
		// On regarde si il n'y a pas déjà une méthode statique du même nom
		if (staticMethods.containsKey(token.getWord())) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.DUPLICATED_METHOD));
		}
		if (!methods.containsKey(token.getWord())) {
			methods.put(token.getWord(), new HashMap<>());
			methodVariables.put(token.getWord(), new LeekVariable(token, VariableType.METHOD));
		}
		methods.get(token.getWord()).put(method.countParameters(), new ClassDeclarationMethod(method, level));
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

	public void addStaticMethod(WordCompiler compiler, IAWord token, ClassMethodBlock method, AccessLevel level) {
		// On regarde si il n'y a pas déjà une méthode du même nom
		if (methods.containsKey(token.getWord())) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.DUPLICATED_METHOD));
		}
		if (!staticMethods.containsKey(token.getWord())) {
			staticMethods.put(token.getWord(), new HashMap<>());
			staticMethodVariables.put(token.getWord(), new LeekVariable(token, VariableType.STATIC_METHOD));
		}
		staticMethods.get(token.getWord()).put(method.countParameters(), new ClassDeclarationMethod(method, level));
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

	public void addField(WordCompiler compiler, IAWord word, AbstractExpression expr, AccessLevel level) throws LeekCompilerException {
		if (fields.containsKey(word.getWord()) || staticFields.containsKey(word.getWord())) {
			compiler.addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.FIELD_ALREADY_EXISTS));
			return;
		}
		fields.put(word.getWord(), new ClassDeclarationField(expr, level));
		fieldVariables.put(word.getWord(), new LeekVariable(word, VariableType.FIELD));
	}

	public void addStaticField(IAWord word, AbstractExpression expr, AccessLevel level) throws LeekCompilerException {
		if (staticFields.containsKey(word.getWord()) || fields.containsKey(word.getWord())) {
			throw new LeekCompilerException(word, Error.FIELD_ALREADY_EXISTS);
		}
		staticFields.put(word.getWord(), new ClassDeclarationField(expr, level));
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
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
			} else if (parentVar.getVariableType() != VariableType.CLASS) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
			} else {
				var current = parentVar.getClassDeclaration();
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
					this.parent = parentVar.getClassDeclaration();
				}
			}
			if (parent == null) {
				System.out.println("issue");
			}
		}

		// Fields
		for (var field : fields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.analyze(compiler);
			}
		}
		// Static fields
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.analyze(compiler);
			}
		}

		for (var constructor : constructors.values()) {
			constructor.block.analyze(compiler);
		}
		// Ajout du constructeur à 0 argument par défaut en public
		if (!constructors.containsKey(0)) {
			constructors.put(0, new ClassDeclarationMethod(null, AccessLevel.PUBLIC));
		}

		for (var method : methods.values()) {
			for (var version : method.values()) {
				version.block.analyze(compiler);
			}
		}
		for (var method : staticMethods.values()) {
			for (var version : method.values()) {
				version.block.analyze(compiler);
			}
		}
		compiler.setCurrentClass(null);
	}

	public void declareJava(MainLeekBlock mainblock, JavaWriter writer) {
		mainblock.getWordCompiler().setCurrentClass(this);

		// Declare the class as a field of the AI
		String className = "u_" + token.getWord();
		writer.addLine("private ClassLeekValue " + className + " = new ClassLeekValue(this, \"" + token.getWord() + "\");");

		// Static methods
		for (Entry<String, HashMap<Integer, ClassDeclarationMethod>> method : staticMethods.entrySet()) {
			for (Entry<Integer, ClassDeclarationMethod> version : method.getValue().entrySet()) {
				writer.currentBlock = version.getValue().block;
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode("private final Object " + methodName + "(");
				int i = 0;
				for (var arg : version.getValue().block.getParameters()) {
					if (i++ > 0) writer.addCode(", ");
					writer.addCode("Object u_" + arg);
				}
				writer.addCode(") throws LeekRunException {");
				writer.addLine("final var u_class = " + className + ";", version.getValue().block.getLine(), version.getValue().block.getFile());
				if (parent != null) {
					writer.addLine("final var u_super = u_" + parent.token.getWord() + ";");
				}
				version.getValue().block.writeJavaCode(mainblock, writer);
				writer.addLine("}");
				writer.currentBlock = null;
			}
		}

		// Methods
		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				writer.currentBlock = version.getValue().block;
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode("private final Object " + methodName + "(ObjectLeekValue u_this");
				for (var arg : version.getValue().block.getParameters()) {
					writer.addCode(", Object u_" + arg);
				}
				// if (version.getValue().getParameters().size() == 0) {
				// 	writer.addCode(", Object __");
				// }
				writer.addCode(") throws LeekRunException {");
				writer.addLine("final var u_class = " + className + ";", version.getValue().block.getLine(), version.getValue().block.getFile());
				if (parent != null) {
					writer.addLine("final var u_super = u_" + parent.token.getWord() + ";");
				}
				writer.addCounter(1);
				version.getValue().block.writeJavaCode(mainblock, writer);
				writer.addLine("}");
				writer.currentBlock = null;
			}
		}

		// Constructeurs
		for (Entry<Integer, ClassDeclarationMethod> construct : constructors.entrySet()) {
			writer.currentBlock = construct.getValue().block;
			String methodName = className + "_" + construct.getKey();
			writer.addCode("private final Object " + methodName + "(ObjectLeekValue u_this");
			if (construct.getValue().block != null) {
				for (var arg : construct.getValue().block.getParameters()) {
					writer.addCode(", Object u_" + arg);
				}
			}
			writer.addCode(") throws LeekRunException {");
			writer.addLine("final var u_class = " + className + ";");
			if (parent != null) {
				writer.addLine("final var u_super = u_" + parent.token.getWord() + ";");
			}
			if (construct.getValue().block != null) {
				construct.getValue().block.writeJavaCode(mainblock, writer);
			} else {
				writer.addLine("return null;");
			}
			writer.addLine("}");
			writer.currentBlock = null;
		}
	}

	public void createJava(MainLeekBlock mainblock, JavaWriter writer) {

		mainblock.getWordCompiler().setCurrentClass(this);

		// Create the class in the constructor of the AI
		String className = "u_" + token.getWord();

		if (parent != null) {
			writer.addLine(className + ".setParent(u_" + parent.getName() + ");");
		}

		writer.addCode(className + ".initFields = new LeekAnonymousFunction() {");
		writer.addLine("public Object run(ObjectLeekValue u_this, Object... values) throws LeekRunException {");
		ClassDeclarationInstruction current = this;
		ArrayList<ClassDeclarationInstruction> classes = new ArrayList<>();
		while (current != null) {
			classes.add(current);
			current = current.parent;
		}
		for (int i = classes.size() - 1; i >= 0; --i) {
			var clazz = classes.get(i);
			for (var field : clazz.fields.entrySet()) {
				writer.addCode("u_this.addField(" + writer.getAIThis() + ", \"" + field.getKey() + "\", ");
				if (field.getValue().expression != null) {
					field.getValue().expression.writeJavaCode(mainblock, writer);
				} else {
					writer.addCode("null");
				}
				writer.addLine(", AccessLevel." + field.getValue().level + ");");
			}
		}
		writer.addLine("return null;");
		writer.addLine("}};");

		writeFields(mainblock, writer, className);

		// Static methods
		for (var method : staticMethods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode(className);
				writer.addCode(".addStaticMethod(\"" + method.getKey() + "\", " + version.getKey() + ", new LeekFunction() { public Object run(Object... args) throws LeekRunException { return " + methodName + "(");
				int i = 0;
				for (var a = 0; a < version.getValue().block.getParameters().size(); ++a) {
					if (i > 0) writer.addCode(", ");
					writer.addCode("args[" + i + "]");
					i++;
				}
				writer.addLine("); }}, AccessLevel." + version.getValue().level + ");");
			}
			writer.addCode(className);
			writer.addLine(".addGenericStaticMethod(\"" + method.getKey() + "\");");
		}

		for (var construct : constructors.entrySet()) {
			String methodName = className + "_" + construct.getKey();
			writer.addCode(className);
			writer.addCode(".addConstructor(" + construct.getKey() + ", new LeekAnonymousFunction() { public Object run(ObjectLeekValue thiz, Object... args) throws LeekRunException { " + methodName + "(thiz");
			int i = 0;
			if (construct.getValue().block != null) {
				for (var a = 0; a < construct.getValue().block.getParameters().size(); ++a) {
					writer.addCode(", args[" + i++ + "]");
				}
			}
			writer.addLine("); return thiz; }}, AccessLevel." + construct.getValue().level + ");");
		}

		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				String methodName = className + "_" + method.getKey() + "_" + version.getKey();
				writer.addCode(className);
				writer.addCode(".addMethod(\"" + method.getKey() + "\", " + version.getKey() + ", new LeekAnonymousFunction() { public Object run(ObjectLeekValue thiz, Object... args) throws LeekRunException { return " + methodName + "(thiz");
				int i = 0;
				for (var a = 0; a < version.getValue().block.getParameters().size(); ++a) {
					writer.addCode(", args[" + i++ + "]");
				}
				writer.addLine("); }}, AccessLevel." + version.getValue().level + ");");
			}
			writer.addCode(className);
			writer.addLine(".addGenericMethod(\"" + method.getKey() + "\");");
		}
	}

	public void initializeStaticFields(MainLeekBlock mainblock, JavaWriter writer) {

		mainblock.getWordCompiler().setCurrentClass(this);

		// Create the class in the constructor of the AI
		String className = "u_" + token.getWord();

		// First declare all static fields
		for (var field : staticFields.entrySet()) {
			writer.addCode(className);
			writer.addCode(".addStaticField(" + writer.getAIThis() + ", \"" + field.getKey() + "\", ");
			writer.addCode("null");
			writer.addCode(", AccessLevel." + field.getValue().level);
			writer.addLine(");");
		}

		// Second assign values for fields with values
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				writer.addCode(className);
				writer.addCode(".setField(\"" + field.getKey() + "\", ");
				field.getValue().expression.writeJavaCode(mainblock, writer);
				writer.addLine(");");
			}
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

	}

	private void writeFields(MainLeekBlock mainblock, JavaWriter writer, String className) {

		if (parent != null) {
			parent.writeFields(mainblock, writer, className);
		}

		for (Entry<String, ClassDeclarationField> field : fields.entrySet()) {
			writer.addCode(className);
			writer.addCode(".addField(\"" + field.getKey() + "\"");
			writer.addCode(", AccessLevel." + field.getValue().level);
			writer.addLine(");");
		}
	}

	public IAWord getParentToken() {
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
			if (versions.containsKey(argumentCount)) return getName() + "_" + name + "_" + argumentCount;
		}
		if (parent != null) {
			return parent.getMethodName(name, argumentCount);
		}
		return null;
	}

	public HashMap<Integer, ClassDeclarationMethod> getStaticMethod(String name) {
		return staticMethods.get(name);
	}

	public String getStaticMethodName(String name, int argumentCount) {
		var versions = staticMethods.get(name);
		if (versions != null) {
			if (versions.containsKey(argumentCount)) return getName() + "_" + name + "_" + argumentCount;
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
}
