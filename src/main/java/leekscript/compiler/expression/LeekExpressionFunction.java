package leekscript.compiler.expression;

import java.util.ArrayList;

import org.graalvm.compiler.word.ObjectAccess;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekFunctions;

public class LeekExpressionFunction extends AbstractExpression {

	private IAWord openParenthesis = null;
	private final ArrayList<AbstractExpression> mParameters = new ArrayList<AbstractExpression>();
	private AbstractExpression mExpression = null;

	public LeekExpressionFunction(IAWord openParenthesis) {
		this.openParenthesis = openParenthesis;
	}

	public void setExpression(AbstractExpression expression) {
		mExpression = expression;
	}

	public void addParameter(AbstractExpression param) {
		mParameters.add(param);
	}

	@Override
	public int getType() {
		return FUNCTION;
	}

	@Override
	public String getString() {
		String str = mExpression.getString() + "(";
		for(int i = 0; i < mParameters.size(); i++){
			if(i > 0) str += ", ";
			str += mParameters.get(i).getString();
		}
		return str + ")";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if(mExpression == null || !mExpression.validExpression(compiler, mainblock)) return false;

		//Vérification de chaque paramètre
		for(AbstractExpression parameter : mParameters){
			parameter.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		boolean addComma = true;
		FunctionBlock user_function = null;
		ILeekFunction system_function = null;
		if (mExpression instanceof LeekObjectAccess) {
			var object = ((LeekObjectAccess) mExpression).getObject();
			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				writer.addCode("u_this.callSuperMethod(mUAI, \"" + ((LeekObjectAccess) mExpression).getField() + "_" + mParameters.size() + "\"");
			} else {
				object.writeJavaCode(mainblock, writer);
				writer.addCode(".callMethod(mUAI, \"" + ((LeekObjectAccess) mExpression).getField() + "_" + mParameters.size() + "\"");
			}
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SUPER) {
			// Super constructor
			var variable = (LeekVariable) mExpression;
			writer.addCode("user_" + variable.getClassDeclaration().getParent().getName());
			writer.addCode(".callConstructor(mUAI, u_this");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.METHOD) {
			writer.addCode("u_this.callMethod(mUAI, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\"");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.STATIC_METHOD) {
			writer.addCode("u_class.callMethod(mUAI, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\"");
		} else if (mExpression instanceof LeekVariable && mainblock.isRedefinedFunction(((LeekVariable) mExpression).getName())) {
			writer.addCode("rfunction_" + ((LeekVariable) mExpression).getName());
			writer.addCode(".executeFunction(mUAI");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SYSTEM_FUNCTION) {
			var variable = (LeekVariable) mExpression;
			system_function = LeekFunctions.getValue(variable.getName());
			String namespace = LeekFunctions.getNamespace(variable.getName());
			// writer.addCode("LeekValueManager.getFunction(" + namespace + "." + variable.getName() + ")");
			writer.addCode("LeekFunctions.executeFunction(mUAI, " + namespace + "." + variable.getName() + ", new AbstractLeekValue[] {");
			addComma = false;
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.FUNCTION) {
			writer.addCode("user_function_");
			writer.addCode(((LeekVariable) mExpression).getName());
			writer.addCode("(");
			addComma = false;
			user_function = mainblock.getUserFunction(((LeekVariable) mExpression).getName());
		} else {
			mExpression.writeJavaCode(mainblock, writer);
			writer.addCode(".executeFunction(mUAI");
		}
		int argCount = mParameters.size();
		if (system_function != null) argCount = Math.max(argCount, system_function.getArguments());
		for (int i = 0; i < argCount; i++) {
			if (i > 0 || addComma) writer.addCode(", ");
			if (i < mParameters.size()) {
				if (mainblock.getCompiler().getCurrentAI().getVersion() >= 11) {
					mParameters.get(i).writeJavaCode(mainblock, writer);
					if (system_function != null) {
						writer.addCode(".getValue()");
					}
				} else {
					if (user_function != null) {
						if (user_function.isReference(i)) {
							mParameters.get(i).writeJavaCode(mainblock, writer);
						} else {
							writer.addCode("LeekOperations.clone(mUAI, ");
							mParameters.get(i).writeJavaCode(mainblock, writer);
							writer.addCode(".getValue())");
						}
					} else if (system_function != null) {
						mParameters.get(i).writeJavaCode(mainblock, writer);
						writer.addCode(".getValue()");
					} else {
						mParameters.get(i).writeJavaCode(mainblock, writer);
					}
				}
			} else {
				writer.addCode("LeekValueManager.NULL");
			}
		}
		if (system_function != null) {
			writer.addCode("}, " + mParameters.size());
		}
		writer.addCode(")");
		writer.addPosition(openParenthesis);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		mExpression.analyze(compiler);
		for (AbstractExpression parameter : mParameters) {
			parameter.analyze(compiler);
		}

		if (mExpression instanceof LeekVariable) {
			var v = (LeekVariable) mExpression;
			if (v.getVariableType() == VariableType.FUNCTION) {
				int nb_params = LeekFunctions.isFunction(v.getName());
				if (nb_params == -1) {
					nb_params = compiler.getMainBlock().getUserFunctionParametersCount(v.getName());
					if (mParameters.size() != nb_params) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, LeekCompilerException.INVALID_PAREMETER_COUNT));
					}
				} else {
					var f = LeekFunctions.getValue(v.getName());
					if (mParameters.size() > nb_params || mParameters.size() < f.getArgumentsMin())
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, LeekCompilerException.INVALID_PAREMETER_COUNT));
				}
			} else if (v.getVariableType() == VariableType.CLASS) {
				System.out.println("Class " + v);

				var clazz = v.getClassDeclaration();
				if (mParameters.size() != 0 && !clazz.hasConstructor(mParameters.size())) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, LeekCompilerException.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
				}
			}
		} else if (mExpression instanceof LeekObjectAccess) {
			System.out.println("OA " + mExpression.getString());

			var oa = (LeekObjectAccess) mExpression;
			if (oa.getObject() instanceof LeekVariable) {
				var v = (LeekVariable) oa.getObject();
				if (v.getVariableType() == VariableType.CLASS) {
					var clazz = v.getClassDeclaration();
					if (!clazz.hasStaticMethod(oa.getField(), mParameters.size())) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, LeekCompilerException.UNKNOWN_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
					}
				}
			}
		}
	}
}
