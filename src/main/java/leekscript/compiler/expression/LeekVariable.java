package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

import leekscript.common.Error;
import leekscript.common.Type;

public class LeekVariable extends Expression {

	public static enum VariableType {
		LOCAL, GLOBAL, ARGUMENT, FIELD, STATIC_FIELD, THIS, THIS_CLASS, CLASS, SUPER, METHOD, STATIC_METHOD,
		SYSTEM_CONSTANT, SYSTEM_FUNCTION, FUNCTION, ITERATOR
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

	public LeekVariable(Token token, VariableType type) {
		this.token = token;
		token.setExpression(this);
		this.type = type;
	}

	public LeekVariable(Token token, VariableType variableType, Type type, boolean isFinal) {
		this(token, variableType);
		this.variableType = type;
		this.isFinal = isFinal;
	}

	public LeekVariable(WordCompiler compiler, Token token, VariableType variableType) {
		this(token, variableType);
		this.box = compiler.getVersion() <= 1;
	}

	public LeekVariable(WordCompiler compiler, Token token, VariableType variableType, Type type) {
		this(token, variableType);
		this.variableType = type;
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

	public LeekVariable(Token token, VariableType variableType, Type type,
			LeekVariableDeclarationInstruction declaration) {
		this(token, variableType);
		this.declaration = declaration;
		this.box = declaration.isCaptured();
		this.variableType = type;
	}

	public LeekVariable(Token token, VariableType type, Type variableType,
			ClassDeclarationInstruction classDeclaration) {
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

	@Override
	public String toString() {
		return token.getWord();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	public boolean isLeftValue() {
		if (type == VariableType.CLASS || type == VariableType.THIS || type == VariableType.THIS_CLASS
				|| type == VariableType.SUPER || type == VariableType.SYSTEM_CONSTANT) {
			return false;
		}
		return true;
	}

	@Override
	public boolean nullable() {
		// return type != VariableType.CLASS && type != VariableType.THIS && type !=
		// VariableType.THIS_CLASS;
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

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (this.type == VariableType.SUPER) {
			return; // Déjà OK
		}
		// Local variables first
		var v = compiler.getCurrentBlock().getVariable(token.getWord(), true);
		if (v != null) {
			this.type = v.getVariableType();
			this.variableType = v.getType();
			this.declaration = v.getDeclaration();
			this.classDeclaration = v.getClassDeclaration();
			this.functionDeclaration = v.getFunctionDeclaration();
			this.isFinal = v.isFinal();
			this.box = v.box;
			this.variable = v;
			if (v.getDeclaration() != null && v.getDeclaration().getFunction() != compiler.getCurrentFunction()) {
				v.getDeclaration().setCaptured();
			}
			if (this.type == VariableType.FIELD) {
				operations += 1;
			}
			return;
		}
		// Global user functions
		var f = compiler.getMainBlock().getUserFunction(token.getWord());
		if (f != null) {
			this.type = VariableType.FUNCTION;
			this.variableType = f.getType();
			return;
		}
		// LS constants
		var constant = LeekConstants.get(token.getWord());
		if (constant != null) {
			this.type = VariableType.SYSTEM_CONSTANT;
			this.variableType = constant.getType();
			return;
		}
		// Redefined function
		if (compiler.getMainBlock().isRedefinedFunction(token.getWord())) {
			this.variableType = Type.ANY;
			return;
		}
		// LS functions
		var lf = LeekFunctions.getValue(token.getWord(), compiler.getOptions().useExtra());
		if (lf != null) {
			this.type = VariableType.SYSTEM_FUNCTION;
			this.variableType = lf.getVersions()[0].getType();
			for (int i = 1; i < lf.getVersions().length; ++i) {
				this.variableType = Type.versions(this.variableType, lf.getVersions()[i].getType());
			}
			return;
		}
		compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION,
				new String[] { token.getWord() }));
	}

	@Override
	public void analyze(WordCompiler compiler) {
		if (this.variable != null) {
			this.variableType = this.variable.getType();
		}
		if (this.type == VariableType.THIS) {
			this.variableType = compiler.getCurrentClass().getType();
		} else if (this.type == VariableType.THIS_CLASS) {
			this.variableType = compiler.getCurrentClass().classValueType;
		} else if (this.type == VariableType.SUPER && compiler.getCurrentClass().getParent() != null) {
			this.variableType = compiler.getCurrentClass().getParent().getClassValueType();
		}
		// Redefined function
		if (compiler.getMainBlock().isRedefinedFunction(token.getWord())) {
			this.variableType = Type.ANY;
			return;
		}
		// System.out.println("[Variable] " + token.getWord() + " type=" + this.variableType);
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return classDeclaration;
	}

	public LeekVariableDeclarationInstruction getDeclaration() {
		return declaration;
	}

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

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.THIS) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".this");
		} else if (type == VariableType.THIS_CLASS) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable());
		} else if (type == VariableType.SUPER) {
			writer.addCode("u_" + classDeclaration.getParent().getName());
		} else if (type == VariableType.FIELD) {
			writer.addCode(token.getWord());
		} else if (type == VariableType.STATIC_FIELD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
			if (variableType != Type.ANY) {
				writer.addCode("((" + variableType.getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\")");
			if (variableType != Type.ANY) {
				writer.addCode(")");
			}
		} else if (type == VariableType.METHOD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_METHOD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + token.getWord() + "\")");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".get()");
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			user_function.compileAnonymousFunction(mainblock, writer);
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			if (constant.getType() == Type.INT)
				writer.addCode(String.valueOf(constant.getIntValue()) + "l");
			else if (constant.getType() == Type.REAL) {
				if (constant == LeekConstants.NaN) {
					writer.addCode("Double.NaN");
				} else if (constant == LeekConstants.Infinity) {
					writer.addCode("Double.POSITIVE_INFINITY");
				} else {
					writer.addCode(String.valueOf(constant.getValue()));
				}
			} else writer.addCode("null");
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			if (user_function != null) {
				user_function.compileAnonymousFunction(mainblock, writer);
			} else {
				var system_function = LeekFunctions.getValue(token.getWord(), writer.getOptions().useExtra());
				writer.generateAnonymousSystemFunction(system_function);
				// String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode(system_function.getStandardClass() + "_" + token.getWord());
				// writer.addCode("LeekValueManager.getFunction(" + namespace + "." +
				// token.getWord() + ")");
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
				} else if (token.getWord().equals("BigInteger")) {
					writer.addCode("bigIntegerClass");
				} else {
					writer.addCode(token.getWord().toLowerCase() + "Class");
				}
			} else {
				writer.addCode("u_" + token.getWord());
			}
		} else {
			if (isWrapper() || isBox()) {
				if (this.variable.getType().isPrimitive()) {
					writer.addCode("(" + this.variable.getType().getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				writer.addCode("u_" + token.getWord() + ".get()");
			} else {
				writer.addCode("u_" + token.getWord());
			}
		}
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.THIS) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".this");
		} else if (type == VariableType.THIS_CLASS) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable());
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
				user_function.compileAnonymousFunction(mainblock, writer);
			} else {
				var system_function = LeekFunctions.getValue(token.getWord(), writer.getOptions().useExtra());
				writer.generateAnonymousSystemFunction(system_function);
				// String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode(system_function.getStandardClass() + "_" + token.getWord());
				// writer.addCode("LeekValueManager.getFunction(" + namespace + "." +
				// token.getWord() + ")");
			}
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			if (constant.getType() == Type.INT)
				writer.addCode(String.valueOf(constant.getIntValue()) + "l");
			else if (constant.getType() == Type.REAL) {
				if (constant == LeekConstants.NaN) {
					writer.addCode("Double.NaN");
				} else if (constant == LeekConstants.Infinity) {
					writer.addCode("Double.POSITIVE_INFINITY");
				} else {
					writer.addCode(String.valueOf(constant.getValue()));
				}
			} else writer.addCode("null");
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			user_function.compileAnonymousFunction(mainblock, writer);
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
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode(token.getWord() + " = ");
			writer.compileConvert(mainblock, 0, expr, variableType);
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(
					mainblock.getWordCompiler().getCurrentClassVariable() + ".setField(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".set(");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() >= 2) {
				writer.addCode("g_" + token.getWord() + " = ");
				if (this.variable.getType() != Type.ANY && this.variable.getType() != expr.getType()) {
					writer.compileConvert(mainblock, 0, expr, variableType);
				} else {
					expr.writeJavaCode(mainblock, writer);
				}
			} else {
				writer.addCode("g_" + token.getWord() + ".set(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isWrapper()) {
				if (expr.isLeftValue()) {
					if (mainblock.getWordCompiler().getVersion() <= 1) {
						writer.addCode("u_" + token.getWord() + ".setBox(");
					} else {
						writer.addCode("u_" + token.getWord() + ".set(");
					}
					expr.compileL(mainblock, writer);
					writer.addCode(")");
				} else {
					writer.addCode("u_" + token.getWord() + ".setBoxOrValue(");
					expr.compileL(mainblock, writer);
					writer.addCode(")");
				}
			} else if (isBox()) {
				// if (expr.isLeftValue()) {
				// writer.addCode("u_" + token.getWord() + " = ");
				// expr.compileL(mainblock, writer);
				// } else {
				writer.addCode("u_" + token.getWord() + ".set(");
				writer.compileConvert(mainblock, 0, expr, this.variableType);
				writer.addCode(")");
				// }
			} else {
				writer.addCode("u_" + token.getWord() + " = ");
				writer.compileConvert(mainblock, 0, expr, this.variableType);
			}
		}
	}

	@Override
	public void compileSetCopy(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode(token.getWord() + " = ");
			expr.writeJavaCode(mainblock, writer);
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".setField(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".set(");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() >= 2) {
				writer.addCode("g_" + token.getWord() + " = ");
				// writer.compileClone(mainblock, expr);
				expr.writeJavaCode(mainblock, writer);
			} else {
				writer.addCode("g_" + token.getWord() + ".set(");
				// writer.compileClone(mainblock, expr);
				expr.compileL(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isWrapper() || isBox()) {
				writer.addCode("u_" + token.getWord() + ".set(");
				// writer.compileClone(mainblock, expr);
				// expr.writeJavaCode(mainblock, writer);
				// expr.compileL(mainblock, writer);
				writer.compileConvert(mainblock, 0, expr, this.variableType);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = ");
				// writer.compileClone(mainblock, expr);
				expr.writeJavaCode(mainblock, writer);
			}
		}
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode("sub(" + token.getWord() + " = ");
			if (!this.variableType.isPrimitiveNumber()) {
				writer.addCode("(" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("add(" + token.getWord() + ", 1l), 1l)");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_inc(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".increment()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("g_" + token.getWord() + "++");
			} else {
				writer.addCode("sub(g_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") add(g_" + token.getWord() + ", 1l), 1l)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".increment()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("u_" + token.getWord() + "++");
			} else {
				writer.addCode("sub(u_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") add(u_" + token.getWord() + ", 1l), 1l)");
			}
		}
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode("add(" + token.getWord() + " = ");
			if (!this.variableType.isPrimitiveNumber()) {
				writer.addCode("(" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("sub(" + token.getWord() + ", 1l), 1l)");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_dec(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".decrement()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("g_" + token.getWord() + "--");
			} else {
				writer.addCode("add(g_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") sub(g_" + token.getWord() + ", 1l), 1l)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".decrement()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("u_" + token.getWord() + "--");
			} else {
				writer.addCode("add(u_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") sub(u_" + token.getWord() + ", 1l), 1l)");
			}
		}
	}

	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode(token.getWord() + " = ");
			if (!this.variableType.isPrimitiveNumber()) {
				writer.addCode("(" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("add(" + token.getWord() + ", 1l)");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_pre_inc(\"" + token.getWord()
					+ "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".pre_increment()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("++g_" + token.getWord());
			} else {
				writer.addCode("g_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") add(g_" + token.getWord() + ", 1l)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".pre_increment()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("++u_" + token.getWord());
			} else {
				writer.addCode("u_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") add(u_" + token.getWord() + ", 1l)");
			}
		}
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode(token.getWord() + " = ");
			if (!this.variableType.isPrimitiveNumber()) {
				writer.addCode("(" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("sub(" + token.getWord() + ", 1l)");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_pre_dec(\"" + token.getWord()
					+ "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".pre_decrement()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("--g_" + token.getWord());
			} else {
				writer.addCode("g_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") sub(g_" + token.getWord() + ", 1l)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".pre_decrement()");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode("--u_" + token.getWord());
			} else {
				writer.addCode("u_" + token.getWord() + " = (" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") sub(u_" + token.getWord() + ", 1l)");
			}
		}
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t) {
		if (type == VariableType.FIELD) {
			writer.addCode(token.getWord() + " = ");
			if (this.variable.getType() != Type.ANY) {
				writer.addCode("((" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
				writer.addCode("add(" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode("))");
			} else {
				writer.addCode("add(" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_add_eq(\""
					+ token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			String prefix = (type == VariableType.GLOBAL ? "g_": "u_");
			if (isBox()) {
				writer.addCode(prefix + token.getWord() + ".add_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else if (this.variableType.isPrimitiveNumber()) {
				writer.addCode(prefix + token.getWord() + " += ");
				if (expr.getType().isPrimitiveNumber()) {
					expr.writeJavaCode(mainblock, writer);
				} else {
					if (this.variableType == Type.INT) {
						writer.addCode("longint(");
						expr.writeJavaCode(mainblock, writer);
						writer.addCode(")");
					} else if (this.variableType == Type.REAL) {
						writer.addCode("real(");
						expr.writeJavaCode(mainblock, writer);
						writer.addCode(")");						
					}
				}
			} else {
				writer.addCode(prefix + token.getWord() + " = ");
				if (this.variable.getType() != Type.ANY) {
					writer.addCode("((" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
					writer.addCode("add_eq(" + prefix + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode("))");
				} else {
					writer.addCode("add_eq(" + prefix + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode(")");
				}
			}
		}
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "sub", "-=");
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type resultType) {
		compileEq(mainblock, writer, expr, "mul", "*=");
	}

	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type resultType) {
		compileEq(mainblock, writer, expr, "pow");
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		if (mainblock.getVersion() == 1) {
			compileEq(mainblock, writer, expr, "div_v1", "/=");
		} else {
			compileEq(mainblock, writer, expr, "div", "/=");
		}
	}

	@Override
	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "intdiv");
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "mod", "%=");
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "bor", "|=");
	}

	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "band", "&=");
	}

	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "bxor", "^=");
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "shl", "<<=");
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "shr", ">>=");
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "ushr", ">>>=");
	}

	private void compileEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String function) {
		compileEq(mainblock, writer, expr, function, null);
	}
	
	/**
	 * Wrapper to write assignment functions of type "a **= b"
	 * */
	private void compileEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String function,
			String primitiveFunction) {
		if (type == VariableType.FIELD) {
			writer.addCode(token.getWord() + " = ");
			if (this.variable.getType() != Type.ANY) {
				if (this.variableType == Type.INT) {
					writer.addCode("longint(");
					writer.addCode(function + "(" + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode("))");
				} else if (this.variableType == Type.REAL) {
					writer.addCode("real(");
					writer.addCode(function + "(" + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode("))");						
				}
				else {
					writer.addCode("((" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
					writer.addCode(function + "(" + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode("))");
				}
			} else {
				writer.addCode(function + "(" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".field_" + function + "_eq(\""
					+ token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			String prefix = (type == VariableType.GLOBAL ? "g_": "u_");
			if (isBox()) {
				writer.addCode(prefix + token.getWord() + "." + function + "_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else if (primitiveFunction != null && this.variableType.isPrimitiveNumber()) {
				writer.addCode(prefix + token.getWord() + " " + primitiveFunction + " ");
				if (expr.getType().isPrimitiveNumber()) {
					expr.writeJavaCode(mainblock, writer);
				} else {
					if (this.variableType == Type.INT) {
						writer.addCode("longint(");
						expr.writeJavaCode(mainblock, writer);
						writer.addCode(")");
					} else if (this.variableType == Type.REAL) {
						writer.addCode("real(");
						expr.writeJavaCode(mainblock, writer);
						writer.addCode(")");						
					}
				}
			} else {
				writer.addCode(prefix + token.getWord() + " = ");
				if (this.variable.getType() != Type.ANY) {
					writer.addCode("((" + this.variable.getType().getJavaName(mainblock.getVersion()) + ") ");
					writer.addCode(function + "(" + prefix + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode("))");
				} else {
					writer.addCode(function + "(" + prefix + token.getWord() + ", ");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode(")");
				}
			}
		}
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
