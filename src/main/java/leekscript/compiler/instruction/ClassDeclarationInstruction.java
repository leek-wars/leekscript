package leekscript.compiler.instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

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
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.AccessLevel;
import leekscript.common.ClassType;
import leekscript.common.ClassValueType;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.common.Type.CastType;

public class ClassDeclarationInstruction extends LeekInstruction {

	public static class ClassDeclarationField extends LeekExpression {

		String name;
		Expression expression;
		public AccessLevel level;
		boolean isFinal;

		public ClassDeclarationField(String name, Expression expr, AccessLevel level, boolean isFinal, Type type) {
			this.name = name;
			this.expression = expr;
			this.level = level;
			this.isFinal = isFinal;
			this.type = type;
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

	public HashMap<String, ClassDeclarationField> getFields() {
		return fields;
	}
	public HashMap<String, ClassDeclarationField> getStaticFields() {
		return staticFields;
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
		fieldVariables.put(word.getWord(), new LeekVariable(word, VariableType.FIELD, type, isFinal));
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
		staticFieldVariables.put(word.getWord(), new LeekVariable(word, VariableType.STATIC_FIELD, type, isFinal));
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la classe
		this.classValueType = new ClassValueType(this);
		compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.CLASS, this.classValueType, this));
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		compiler.setCurrentClass(this);
		// Parent
		if (parentToken != null) {
			var parentVar = compiler.getCurrentBlock().getVariable(this.parentToken.getWord(), true);
			if (parentVar == null) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION, new String[] {
					parentToken.getWord()
				}));
			} else if (parentVar.getVariableType() != VariableType.CLASS) {
				compiler.addError(new AnalyzeError(parentToken, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION, new String[] {
					parentToken.getWord()
				}));
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
			if (entry.getKey() == entry.getValue().block.getMaxParameters()) {
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
		compiler.setCurrentClass(this);

		// Fields
		for (var field : fields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.analyze(compiler);

				// Vérification du type de l'expression
				var cast = field.getValue().getType().accepts(field.getValue().expression.getType());
				if (cast.ordinal() > CastType.UPCAST.ordinal()) {

					var level = compiler.getMainBlock().isStrict() && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					var error = cast == CastType.INCOMPATIBLE ? Error.ASSIGNMENT_INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION_VARIABLE;

					compiler.addError(new AnalyzeError(field.getValue().expression.getLocation(), level, error, new String[] {
						field.getValue().expression.toString(),
						field.getValue().expression.getType().toString(),
						field.getValue().name,
						field.getValue().getType().toString(),
					}));
				}
			}
		}

		// Static fields
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				field.getValue().expression.analyze(compiler);

				var cast = field.getValue().getType().accepts(field.getValue().expression.getType());
				if (cast.ordinal() > CastType.UPCAST.ordinal()) {

					var level = compiler.getMainBlock().isStrict() && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					var error = cast == CastType.INCOMPATIBLE ? Error.ASSIGNMENT_INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION_VARIABLE;

					compiler.addError(new AnalyzeError(field.getValue().expression.getLocation(), level, error, new String[] {
						field.getValue().expression.toString(),
						field.getValue().expression.getType().toString(),
						field.getValue().name,
						field.getValue().getType().toString(),
					}));
				}
			}
		}

		for (var constructor : constructors.values()) {
			constructor.block.analyze(compiler);
		}
		// Ajout du constructeur à 0 argument par défaut en public
		if (!constructors.containsKey(0)) {
			constructors.put(0, new ClassDeclarationMethod(null, AccessLevel.PUBLIC));
		}

		for (var method : methods.entrySet()) {
			for (var version : method.getValue().entrySet()) {
				version.getValue().block.analyze(compiler);

				// Méthode surchargée ?
				var current = parent;
				while (current != null) {
					var parentMethod = current.methods.get(method.getKey());
					if (parentMethod != null) {
						var parentVersion = parentMethod.get(version.getKey());
						if (parentVersion != null && version.getValue().block.getType().accepts(parentVersion.block.getType()) != CastType.EQUALS) {
							compiler.addError(new AnalyzeError(version.getValue().block.getLocation(), AnalyzeErrorLevel.ERROR, Error.OVERRIDDEN_METHOD_DIFFERENT_TYPE, new String[] {
								version.getValue().block.getType().toString(),
								parentVersion.block.getType().toString()
							}));
							break;
						}
					}
					current = current.parent;
				}
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
		String className = "u_" + token.getWord();

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
			if (parent.internal) {
				writer.addLine("super(" + writer.getAIThis() + ");");
			}
		}
		writer.addLine("allocateRAM(this, " + (2 * fieldVariables.size()) + ");");
		for (var field : fields.entrySet()) {
			Expression expr = field.getValue().expression;
			if (expr != null) {
				writer.addCode(field.getKey());
				writer.addCode(" = ");
				if (field.getValue().getType() != Type.ANY) {
					writer.compileConvert(mainblock, 0, expr, field.getValue().getType());
				} else {
					expr.writeJavaCode(mainblock, writer);
				}
				writer.addLine(";");
			}
		}
		writer.addLine("}");

		// Constructeur par copie
		writer.addLine("public " + className + "(" + className + " o, int level) throws LeekRunException {");
		if (parent != null) {
			if (parent.internal) {
				writer.addLine("super(" + writer.getAIThis() + ", o, level);");
			} else {
				writer.addLine("super(o, level);");
			}
		}
		for (var field : fields.entrySet()) {
			writer.addLine("this." + field.getKey() + " = level == 1 ? o." + field.getKey() + " : (" + field.getValue().getType().getJavaPrimitiveName(mainblock.getVersion()) + ") copy(o." + field.getKey() + ", level - 1);");
		}
		writer.addLine("}");

		// Vrais constructeurs
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

			if (parent != null && !parent.internal) {
				writer.addLine("super.init();");
			}

			// Init fields
			// ClassDeclarationInstruction current = this;
			// ArrayList<ClassDeclarationInstruction> classes = new ArrayList<>();
			// while (current != null) {
			// 	classes.add(current);
			// 	current = current.parent;
			// }
			// for (int i = classes.size() - 1; i >= 0; --i) {
			// 	var clazz = classes.get(i);
				// for (var field : fields.entrySet()) {
				// 	if (field.getValue().expression != null) {
				// 		writer.addCode(field.getKey());
				// 		writer.addCode(" = ");
				// 		field.getValue().expression.writeJavaCode(mainblock, writer);
				// 		writer.addLine(";");
				// 	}
				// }
			// }

			// Captures
			if (block != null) {
				for (int a = 0; a < block.getParametersDeclarations().size(); ++a) {
					var arg = block.getParametersDeclarations().get(a);
					if (arg.isCaptured()) {
						writer.addCode("final var u_" + arg.getToken() + " = new Box<" + arg.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
						if (a < construct.getKey()) {
							writer.addCode("p_" + arg.getToken());
						} else {
							block.getDefaultValues().get(a).writeJavaCode(mainblock, writer);
							writer.addCode(", " + block.getDefaultValues().get(a).operations);
						}
						writer.addLine(");");
					} else {
						// Valeur par défaut
						if (a >= construct.getKey()) {
							var defaultValue = block.getDefaultValues().get(a);
							writer.addCode("final " + arg.getType().getJavaName(mainblock.getVersion()) + " u_" + arg.getName() + " = ");
							defaultValue.writeJavaCode(mainblock, writer);
							writer.addLine(";");
							writer.addCounter(defaultValue.operations);
						}
					}
				}
			}

			if (block != null) {

				// Sous-version
				if (construct.getKey() < block.getMaxParameters()) {
					writer.addCode("return init(");
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

					// Version normale
					block.writeJavaCode(mainblock, writer);
				}
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
							block.getDefaultValues().get(a).writeJavaCode(mainblock, writer);
							writer.addCode(", " + block.getDefaultValues().get(a).operations);
						}
						writer.addLine(");");
					} else {
						// Valeur par défaut
						if (a >= version.getKey()) {
							var defaultValue = block.getDefaultValues().get(a);
							writer.addCode("final " + arg.getType().getJavaName(mainblock.getVersion()) + " u_" + arg.getName() + " = ");
							defaultValue.writeJavaCode(mainblock, writer);
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
					version.getValue().block.writeJavaCode(mainblock, writer);
				}
				writer.addLine("}");
				writer.currentBlock = null;
				mainblock.getWordCompiler().setCurrentBlock(null);
			}
		}

		// End of actual class
		writer.addLine("}");

		// Declare the class as a field of the AI
		writer.addLine("public ClassLeekValue " + className + " = new ClassLeekValue(this, \"" + token.getWord() + "\", null, " + className + ".class);");

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
				writer.addCode("private final " + block.getType().returnType().getJavaPrimitiveName(mainblock.getVersion()) + " " + methodName + "(");
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
							block.getDefaultValues().get(a).writeJavaCode(mainblock, writer);
							writer.addCode(", " + block.getDefaultValues().get(a).operations);
						}
						writer.addLine(");");
					} else {
						// Valeur par défaut
						if (a >= version.getKey()) {
							var defaultValue = block.getDefaultValues().get(a);
							writer.addCode("final " + arg.getType().getJavaPrimitiveName(mainblock.getVersion()) + " u_" + arg.getName() + " = ");
							defaultValue.writeJavaCode(mainblock, writer);
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
					version.getValue().block.writeJavaCode(mainblock, writer);
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
				case "BigInteger": return "BigIntegerValue";
			}
		}
		return "u_" + token.getWord();
	}

	private String getClassName(MainLeekBlock block) {
		if (internal) {
			if (block.getVersion() <= 3 && token.getWord().equals("Array")) {
				return "legacyArrayClass";
			}
			return token.getWord().toLowerCase() + "Class";
		}
		return "u_" + token.getWord();
	}

	public void createJava(MainLeekBlock mainblock, JavaWriter writer) {

		mainblock.getWordCompiler().setCurrentClass(this);

		// Create the class in the constructor of the AI
		String className = "u_" + token.getWord();

		if (parent != null) {
			writer.addLine(className + ".setParent(" + parent.getClassName(mainblock) + ");");
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
				writer.addCode(".addStaticMethod(\"" + method.getKey() + "\", " + version.getKey() + ", new FunctionLeekValue(1) { public Object run(AI ai, Object thiz, Object... args) throws LeekRunException { return " + methodName + "(");
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
				writer.addLine(".addMethod(\"" + method.getKey() + "\", " + version.getKey() + ", new FunctionLeekValue(0) { public Object run(AI ai, Object thiz, Object... args) throws LeekRunException {");
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
		writer.addLine("createStaticClass_" + token.getWord() + "();");
	}

	public void writeCreateStaticFields(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addLine("private void createStaticClass_" + token.getWord() + "() throws LeekRunException {");

		mainblock.getWordCompiler().setCurrentClass(this);
		mainblock.getWordCompiler().setCurrentBlock(staticInitBlock);

		// Create the class in the constructor of the AI
		String className = "u_" + token.getWord();

		// First create all static fields
		for (var field : staticFields.entrySet()) {
			writer.addCode(className);
			writer.addCode(".addStaticField(" + writer.getAIThis() + ", \"" + field.getKey() + "\", ");
			writer.addCode("null");
			writer.addCode(", AccessLevel." + field.getValue().level.name() + ", " + field.getValue().isFinal);
			writer.addLine(");");
		}

		writer.addLine("}");
		mainblock.getWordCompiler().setCurrentClass(null);
		mainblock.getWordCompiler().setCurrentBlock(null);
	}

	public void initializeStaticFields(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addLine("initClass_" + token.getWord() + "();");
	}

	public void writeInitializeStaticFields(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addLine("private void initClass_" + token.getWord() + "() throws LeekRunException {");

		mainblock.getWordCompiler().setCurrentClass(this);
		mainblock.getWordCompiler().setCurrentBlock(staticInitBlock);

		// Create the class in the constructor of the AI
		String className = "u_" + token.getWord();

		// Second assign values for fields with values
		for (var field : staticFields.entrySet()) {
			if (field.getValue().expression != null) {
				writer.addCode(className);
				writer.addCode(".initField(\"" + field.getKey() + "\", ");
				writer.compileConvert(mainblock, 0, field.getValue().expression, field.getValue().getType());
				writer.addLine(");");
			}
		}

		mainblock.getWordCompiler().setCurrentClass(null);
		mainblock.getWordCompiler().setCurrentBlock(null);
		writer.addLine("}");
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

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
