package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;
import leekscript.common.Error;
import leekscript.common.Type;

public class LeekVariable extends AbstractExpression {

	public static enum VariableType {
		LOCAL, GLOBAL, ARGUMENT, FIELD, STATIC_FIELD, THIS, THIS_CLASS, CLASS, SUPER, METHOD, STATIC_METHOD, SYSTEM_CONSTANT, SYSTEM_FUNCTION, FUNCTION, ITERATOR
	}

	private final IAWord token;
	private VariableType type;
	private Type variableType = Type.ANY;
	private LeekVariableDeclarationInstruction declaration;
	private ClassDeclarationInstruction classDeclaration;
	private boolean box;

	public LeekVariable(IAWord token, VariableType type) {
		this.token = token;
		this.type = type;
		this.declaration = null;
		this.classDeclaration = null;
		this.box = false;
	}

	public LeekVariable(WordCompiler compiler, IAWord token, VariableType type) {
		this.token = token;
		this.type = type;
		this.declaration = null;
		this.classDeclaration = null;
		this.box = compiler.getVersion() <= 1;
	}

	public LeekVariable(IAWord token, VariableType type, boolean box) {
		this.token = token;
		this.type = type;
		this.declaration = null;
		this.classDeclaration = null;
		this.box = box;
	}

	public LeekVariable(IAWord token, VariableType type, LeekVariableDeclarationInstruction declaration) {
		this.token = token;
		this.type = type;
		this.declaration = declaration;
		this.classDeclaration = null;
		this.box = declaration.isCaptured();
	}

	public LeekVariable(IAWord token, VariableType type, ClassDeclarationInstruction classDeclaration) {
		this.token = token;
		this.type = type;
		this.classDeclaration = classDeclaration;
		this.box = false;
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
	public String getString() {
		return token.getWord();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	public boolean isLeftValue() {
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

	@Override
	public void analyze(WordCompiler compiler) {
		if (this.type == VariableType.SUPER) {
			return; // Déjà OK
		}
		// Local variables first
		var v = compiler.getCurrentBlock().getVariable(token.getWord(), true);
		if (v != null) {
			this.type = v.getVariableType();
			this.declaration = v.getDeclaration();
			this.classDeclaration = v.getClassDeclaration();
			this.box = v.box;
			if (v.getDeclaration() != null && v.getDeclaration().getFunction() != compiler.getCurrentFunction()) {
				v.getDeclaration().setCaptured();
			}
			if (this.type == VariableType.FIELD) {
				operations += 1;
			}
			return;
		}
		// Global user functions
		if (compiler.getMainBlock().hasUserFunction(token.getWord(), true)) {
			this.type = VariableType.FUNCTION;
			return;
		}
		// LS constants
		var constant = LeekConstants.get(token.getWord());
		if (constant != null) {
			this.type = VariableType.SYSTEM_CONSTANT;
			this.variableType = constant.getType();
			return;
		}
		// LS functions
		if (LeekFunctions.isFunction(token.getWord()) != -1) {
			this.type = VariableType.SYSTEM_FUNCTION;
			return;
		}
		compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return classDeclaration;
	}

	public LeekVariableDeclarationInstruction getDeclaration() {
		return declaration;
	}

	public IAWord getToken() {
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
			writer.addCode("u_this");
		} else if (type == VariableType.THIS_CLASS) {
			writer.addCode("u_class");
		} else if (type == VariableType.SUPER) {
			writer.addCode("u_" + classDeclaration.getParent().getName());
		} else if (type == VariableType.FIELD) {
			writer.addCode("u_this.getField(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.getField(\"" + token.getWord() + "\")");
		} else if (type == VariableType.METHOD) {
			writer.addCode("u_class.getField(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_METHOD) {
			writer.addCode("u_class.getField(\"" + token.getWord() + "\")");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord());
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			// if (constant.getType() == LeekFunctions.INT) writer.addCode("LeekValueManager.getLeekIntValue(" + constant.getIntValue() + ")");
			if (constant.getType() == Type.INT) writer.addCode(String.valueOf(constant.getIntValue()));
			// else if (constant.getType() == LeekFunctions.DOUBLE) writer.addCode("new DoubleLeekValue(" + constant.getValue() + ")");
			else if (constant.getType() == Type.REAL) writer.addCode(String.valueOf(constant.getValue()));
			else writer.addCode("null");
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			if (user_function != null) {
				writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
			} else {
				String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode("LeekValueManager.getFunction(" + namespace + "." + token.getWord() + ")");
			}
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() <= 1) {
				writer.addCode("g_" + token.getWord() + ".getValue()");
			} else {
				writer.addCode("g_" + token.getWord());
			}
		} else if (type == VariableType.CLASS) {
			if (classDeclaration.internal) {
				writer.addCode(token.getWord().toLowerCase() + "Class");
			} else {
				writer.addCode("u_" + token.getWord());
			}
		} else {
			if (isWrapper()) {
				writer.addCode("u_" + token.getWord() + ".getValue()");
			} else if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".getValue()");
			} else {
				writer.addCode("u_" + token.getWord());
			}
		}
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.THIS) {
			writer.addCode("u_this");
		} else if (type == VariableType.THIS_CLASS) {
			writer.addCode("u_class");
		} else if (type == VariableType.SUPER) {
			writer.addCode("u_" + classDeclaration.getParent().getName());
		} else if (type == VariableType.FIELD) {
			writer.addCode("u_this.getFieldL(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.getFieldL(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			writer.addCode("g_" + token.getWord());
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord());
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			if (user_function != null) {
				writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
			} else {
				String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode("LeekValueManager.getFunction(" + namespace + "." + token.getWord() + ")");
			}
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			if (constant.getType() == Type.INT) writer.addCode(String.valueOf(constant.getIntValue()));
			else if (constant.getType() == Type.REAL) writer.addCode(String.valueOf(constant.getValue()));
			else writer.addCode("null");
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
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
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.setField(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.setField(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord() + ".set(");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (mainblock.getWordCompiler().getVersion() >= 2) {
				writer.addCode("g_" + token.getWord() + " = ");
				expr.writeJavaCode(mainblock, writer);
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
				// 	writer.addCode("u_" + token.getWord() + " = ");
				// 	expr.compileL(mainblock, writer);
				// } else {
					writer.addCode("u_" + token.getWord() + ".set(");
					expr.writeJavaCode(mainblock, writer);
					writer.addCode(")");
				// }
			} else {
				writer.addCode("u_" + token.getWord() + " = ");
				expr.writeJavaCode(mainblock, writer);
			}
		}
	}

	@Override
	public void compileSetCopy(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.setField(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.setField(\"" + token.getWord() + "\", ");
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
				expr.compileL(mainblock, writer);
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
			writer.addCode("u_this.field_inc(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_inc(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".increment()");
			} else {
				writer.addCode("sub(g_" + token.getWord() + " = add(g_" + token.getWord() + ", 1), 1)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".increment()");
			} else {
				writer.addCode("sub(u_" + token.getWord() + " = add(u_" + token.getWord() + ", 1), 1)");
			}
		}
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_dec(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_dec(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".decrement()");
			} else {
				writer.addCode("add(g_" + token.getWord() + " = sub(g_" + token.getWord() + ", 1), 1)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".decrement()");
			} else {
				writer.addCode("add(u_" + token.getWord() + " = sub(u_" + token.getWord() + ", 1), 1)");
			}
		}
	}

	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_pre_inc(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_pre_inc(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".pre_increment()");
			} else {
				writer.addCode("g_" + token.getWord() + " = add(g_" + token.getWord() + ", 1)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".pre_increment()");
			} else {
				writer.addCode("u_" + token.getWord() + " = add(u_" + token.getWord() + ", 1)");
			}
		}
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_pre_dec(\"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_pre_dec(\"" + token.getWord() + "\")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".pre_decrement()");
			} else {
				writer.addCode("g_" + token.getWord() + " = sub(g_" + token.getWord() + ", 1)");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".pre_decrement()");
			} else {
				writer.addCode("u_" + token.getWord() + " = sub(u_" + token.getWord() + ", 1)");
			}
		}
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_add_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_add_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".add_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = add(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".add_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				// writer.addCode("u_" + token.getWord() + " = add(u_" + token.getWord() + ", ");
				// expr.writeJavaCode(mainblock, writer);
				// writer.addCode(")");
				writer.addCode("u_" + token.getWord() + " = add_eq(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_sub_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_sub_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".sub_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = sub(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".sub_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = sub(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_mul_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_mul_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".mul_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = mul(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".mul_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = mul(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}


	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_pow_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_pow_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".pow_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = pow(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".pow_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = pow(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_div_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_div_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".div_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = div(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".div_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = div(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_mod_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_mod_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".mod_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = mod(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".mod_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = mod(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_bor_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_bor_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".bor_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = bor(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".bor_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = bor(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}


	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_band_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_band_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".band_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = band(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".band_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = band(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}


	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_bxor_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_bxor_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".bxor_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = bxor(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".bxor_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = bxor(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_shl_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_shl_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".shl_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = shl(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".shl_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = shl(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_shr_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_shr_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".shr_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = shr(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".shr_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = shr(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		if (type == VariableType.FIELD) {
			writer.addCode("u_this.field_ushr_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.field_ushr_eq(\"" + token.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else if (type == VariableType.GLOBAL) {
			if (isBox()) {
				writer.addCode("g_" + token.getWord() + ".ushr_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("g_" + token.getWord() + " = ushr(g_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		} else {
			if (isBox()) {
				writer.addCode("u_" + token.getWord() + ".ushr_eq(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else {
				writer.addCode("u_" + token.getWord() + " = ushr(u_" + token.getWord() + ", ");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			}
		}
	}
}
