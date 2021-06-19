package leekscript.compiler.expression;

import java.util.ArrayList;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.ClassMethodBlock;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.runner.CallableVersion;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekFunctions;
import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.Type;

public class LeekExpressionFunction extends AbstractExpression {

	private IAWord openParenthesis = null;
	private final ArrayList<AbstractExpression> mParameters = new ArrayList<AbstractExpression>();
	private AbstractExpression mExpression = null;
	private Type type = Type.ANY;

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
	public int getNature() {
		return FUNCTION;
	}

	@Override
	public Type getType() {
		return type;
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
				writer.addCode("u_this.callSuperMethod(mUAI, u_class, \"" + ((LeekObjectAccess) mExpression).getField() + "_" + mParameters.size() + "\"");
			} else {
				var fromClass = writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null";
				object.writeJavaCode(mainblock, writer);
				writer.addCode(".callMethod(mUAI, \"" + ((LeekObjectAccess) mExpression).getField() + "_" + mParameters.size() + "\", " + fromClass);
			}
		} else if (mExpression instanceof LeekTabularValue) {
			var object = ((LeekTabularValue) mExpression).getTabular();
			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				writer.addCode("u_this.callSuperMethod(mUAI, u_class, ");
				((LeekTabularValue) mExpression).getCase().writeJavaCode(mainblock, writer);
				writer.addCode(".getString(mUAI) + \"_" + mParameters.size() + "\"");
			} else {
				writer.addCode("LeekValueManager.executeArrayAccess(mUAI, ");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				((LeekTabularValue) mExpression).getCase().writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				writer.addCode(writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null");
			}
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SUPER) {
			// Super constructor
			var variable = (LeekVariable) mExpression;
			writer.addCode("user_" + variable.getClassDeclaration().getParent().getName());
			writer.addCode(".callConstructor(mUAI, u_this");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.METHOD) {
			writer.addCode("u_this.callMethod(mUAI, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\", u_class");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.STATIC_METHOD) {
			writer.addCode("u_class.callMethod(mUAI, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\", u_class");
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
		operations = 1;
		mExpression.analyze(compiler);
		operations += mExpression.getOperations();
		for (AbstractExpression parameter : mParameters) {
			parameter.analyze(compiler);
			operations += parameter.getOperations();
		}

		if (mExpression instanceof LeekVariable) {
			var v = (LeekVariable) mExpression;
			if (v.getVariableType() == VariableType.FUNCTION) {
				int nb_params = LeekFunctions.isFunction(v.getName());
				if (nb_params == -1) {
					nb_params = compiler.getMainBlock().getUserFunctionParametersCount(v.getName());
					if (mParameters.size() != nb_params) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					}
				} else {
					var f = LeekFunctions.getValue(v.getName());
					if (mParameters.size() > nb_params || mParameters.size() < f.getArgumentsMin()) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					}
					var version = checkArgumentsStatically(f);
					if (version != null) {
						type = version.return_type;
					}
				}
			} else if (v.getVariableType() == VariableType.SYSTEM_FUNCTION) {
				var system_function = LeekFunctions.getValue(v.getName());
				if (system_function.getReturnType() != null) {
					type = system_function.getReturnType();
				}
			} else if (v.getVariableType() == VariableType.CLASS) {

				var clazz = v.getClassDeclaration();
				var constructor = clazz.getConstructor(mParameters.size());
				if (constructor == null) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
				} else if (constructor.level != AccessLevel.PUBLIC) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, constructor.level == AccessLevel.PROTECTED ? Error.PROTECTED_CONSTRUCTOR : Error.PRIVATE_CONSTRUCTOR, new String[] { clazz.getName() }));
				}

			} else if (v.getVariableType() == VariableType.SUPER) {

				var clazz = v.getClassDeclaration().getParent();
				if (clazz != null) {
					var constructor = clazz.getConstructor(mParameters.size());
					if (constructor == null) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
					} else if (constructor.level == AccessLevel.PRIVATE) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_CONSTRUCTOR, new String[] { clazz.getName() }));
					}
				}
			}
		} else if (mExpression instanceof LeekObjectAccess) {

			var oa = (LeekObjectAccess) mExpression;
			if (oa.getObject() instanceof LeekVariable) {
				var v = (LeekVariable) oa.getObject();
				if (v.getVariableType() == VariableType.CLASS) {
					var clazz = v.getClassDeclaration();
					var staticMethod = clazz.getStaticMethod(oa.getField(), mParameters.size());
					if (staticMethod == null) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
					} else if (staticMethod.level == AccessLevel.PRIVATE && clazz != compiler.getCurrentClass()) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
					} else if (staticMethod.level == AccessLevel.PROTECTED && !compiler.getCurrentClass().descendsFrom(clazz)) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
					}
				}
			}
		}
	}

	CallableVersion checkArgumentsStatically(ILeekFunction function) {
		var versions = function.getVersions();
		if (versions == null) return null;

		for (var version : versions) {
			if (checkArgumentsStatically(version)) {
				return version;
			}
		}
		return null;
	}

	private boolean checkArgumentsStatically(CallableVersion version) {
		for (int i = 0; i < version.arguments.length; ++i) {
			if (!version.arguments[i].accepts(mParameters.get(i).getType())) {
				return false;
			}
		}
		return true;
	}

	private String buildTypesSignature(Type[] types) {
		var s = new StringBuilder();
		for (var type : types) {
			s.append(type.getSignature());
		}
		return s.toString();
	}
}
