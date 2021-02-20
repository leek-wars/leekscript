package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.runner.LeekFunctions;

public class LeekVariable extends AbstractExpression {

	public static enum VariableType {
		LOCAL, GLOBAL, ARGUMENT, FIELD, STATIC_FIELD, THIS, THIS_CLASS, CLASS, SUPER, METHOD, STATIC_METHOD, SYSTEM_FUNCTION, FUNCTION
	}

	private final IAWord token;
	private VariableType type;
	private ClassDeclarationInstruction classDeclaration;

	public LeekVariable(IAWord token, VariableType type) {
		this.token = token;
		this.type = type;
		this.classDeclaration = null;
	}

	public LeekVariable(IAWord token, VariableType type, ClassDeclarationInstruction classDeclaration) {
		this.token = token;
		this.type = type;
		this.classDeclaration = classDeclaration;
	}

	@Override
	public int getType() {
		return VARIABLE;
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
		} else
		if (type == VariableType.FUNCTION) {
			FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
			writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
		} else if (type == VariableType.SYSTEM_FUNCTION) {
			if (mainblock.isRedefinedFunction(token.getWord())) {
				writer.addCode("rfunction_" + token.getWord());
			} else {
				FunctionBlock user_function = mainblock.getUserFunction(token.getWord());
				if (user_function != null) {
					writer.addCode("new FunctionLeekValue(" + user_function.getId() + ")");
				} else {
					String namespace = LeekFunctions.getNamespace(token.getWord());
					writer.addCode("LeekValueManager.getFunction(" + namespace + "." + token.getWord() + ")");
				}
			}
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
		if (compiler.getMainBlock().hasUserFunction(token.getWord(), true)) {
			this.type = VariableType.FUNCTION;
			return;
		}
		var v = compiler.getCurrentBlock().getVariable(token.getWord(), true);
		if (v != null) {
			this.type = v.getVariableType();
			this.classDeclaration = v.getClassDeclaration();
			return;
		}
		if (LeekFunctions.isFunction(token.getWord()) != -1) {
			this.type = VariableType.SYSTEM_FUNCTION;
			return;
		}
		compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, LeekCompilerException.UNKNOWN_VARIABLE_OR_FUNCTION));
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return classDeclaration;
	}

	public IAWord getToken() {
		return token;
	}
}
