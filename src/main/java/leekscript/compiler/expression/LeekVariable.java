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
		this.box = compiler.getVersion() <= 10;
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

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (type == VariableType.THIS) {
			writer.addCode("u_this");
		} else if (type == VariableType.THIS_CLASS) {
			writer.addCode("u_class");
		} else if (type == VariableType.SUPER) {
			writer.addCode("user_" + classDeclaration.getParent().getName());
		} else if (type == VariableType.FIELD) {
			writer.addCode("u_this.getField(mUAI, \"" + token.getWord() + "\")");
		} else if (type == VariableType.STATIC_FIELD) {
			writer.addCode("u_class.getField(mUAI, \"" + token.getWord() + "\")");
		} else if (type == VariableType.METHOD) {
			writer.addCode("###");
		} else if (mainblock.isRedefinedFunction(token.getWord())) {
			writer.addCode("rfunction_" + token.getWord());
		} else if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
		} else if (type == VariableType.SYSTEM_CONSTANT) {
			var constant = LeekConstants.get(token.getWord());
			if (constant.getType() == Type.INT) writer.addCode("LeekValueManager.getLeekIntValue(" + constant.getIntValue() + ")");
			else if (constant.getType() == Type.REAL) writer.addCode("new DoubleLeekValue(" + constant.getValue() + ")");
			else writer.addCode("LeekValueManager.NULL");
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			if (user_function != null) {
				writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
			} else {
				String namespace = LeekFunctions.getNamespace(token.getWord());
				writer.addCode("LeekValueManager.getFunction(" + namespace + "." + token.getWord() + ")");
			}
		} else if (type == VariableType.GLOBAL) {
			writer.addCode("globale_" + token.getWord());
		} else {
			writer.addCode("user_" + token.getWord());
		}
	}

	@Override
	public boolean isLeftValue() {
		return true;
	}

	public VariableType getVariableType() {
		return type;
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

}
