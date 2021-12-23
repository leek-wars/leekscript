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
		writer.addCode("load(");
		compileL(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		boolean addComma = true;
		boolean addBrace = false;
		FunctionBlock user_function = null;
		ILeekFunction system_function = null;

		if (mExpression instanceof LeekObjectAccess) {
			// Object access : object.field()
			var object = ((LeekObjectAccess) mExpression).getObject();
			var field = ((LeekObjectAccess) mExpression).getField();

			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				// super.field()
				var from_class = writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null";
				writer.addCode("u_this.callSuperMethod(this, \"" + field + "_" + mParameters.size() + "\", " + from_class);
			} else if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.CLASS) {
				// Class.method() : Méthode statique connue
				var v = (LeekVariable) object;
				String methodName = "u_" + v.getClassDeclaration().getStaticMethodName(field, mParameters.size());
				writer.addCode(methodName + "(");
				addComma = false;
			} else if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.THIS) {
				// this.method() : Méthode connue
				writer.addCode("callObjectAccess(u_this, \"" + field + "\", \"" + field + "_" + mParameters.size() + "\", u_class");
			} else {
				// object.field() : Méthode ou bien appel d'un champ
				writer.addCode("callObjectAccess(");
				object.writeJavaCode(mainblock, writer);
				var from_class = writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null";
				writer.addCode(", \"" + field + "\", \"" + field + "_" + mParameters.size() + "\", " + from_class);
			}
		} else if (mExpression instanceof LeekTabularValue) {
			var object = ((LeekTabularValue) mExpression).getTabular();
			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				writer.addCode("u_this.callSuperMethod(mUAI, u_class, ");
				((LeekTabularValue) mExpression).getCase().writeJavaCode(mainblock, writer);
				writer.addCode(".getString(mUAI) + \"_" + mParameters.size() + "\", ");
				var from_class = writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null";
				writer.addCode(from_class);
			} else {
				writer.addCode("LeekValueManager.executeArrayAccess(" + writer.getAIThis() + ", ");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				((LeekTabularValue) mExpression).getCase().writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				writer.addCode(writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null");
			}
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SUPER) {
			// Super constructor
			var variable = (LeekVariable) mExpression;
			writer.addCode("u_" + variable.getClassDeclaration().getParent().getName());
			writer.addCode(".callConstructor(u_this");
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.METHOD) {
			// Méthode connue
			// String methodName = "u_" + mainblock.getWordCompiler().getCurrentClass().getMethodName(((LeekVariable) mExpression).getName(), mParameters.size());
			// writer.addCode(methodName + "(u_this");
			writer.addCode("callMethod(u_this, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\", u_class");

		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.STATIC_METHOD) {
			// Méthode statique connue
			String methodName = "u_" + mainblock.getWordCompiler().getCurrentClass().getStaticMethodName(((LeekVariable) mExpression).getName(), mParameters.size());
			writer.addCode(methodName + "(");
			addComma = false;
		} else if (mExpression instanceof LeekVariable && mainblock.isRedefinedFunction(((LeekVariable) mExpression).getName())) {
			writer.addCode("rfunction_" + ((LeekVariable) mExpression).getName());
			writer.addCode(".execute(");
			addComma = false;
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.SYSTEM_FUNCTION) {
			var variable = (LeekVariable) mExpression;
			system_function = LeekFunctions.getValue(variable.getName());

			if (system_function.getVersions() != null) {
				// var version = checkArgumentsStatically(system_function);
				// System.out.println("version = " + version);
				// if (version != null) {
				// 	var signature = buildTypesSignature(version.arguments);
				// 	writer.addCode("" + system_function + "_" + signature + "(");
				// } else {
					writer.addCode("" + system_function + "(");
				// }
				addComma = false;
			} else {
				// writer.addCode("" + system_function + "_(");
				String namespace = LeekFunctions.getNamespace(variable.getName());
				writer.addCode("sysexec(" + namespace + "." + variable.getName());
			}
		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.FUNCTION) {
			writer.addCode("f_");
			writer.addCode(((LeekVariable) mExpression).getName());
			writer.addCode("(");
			addComma = false;
			user_function = mainblock.getUserFunction(((LeekVariable) mExpression).getName());
		} else {
			if (mExpression.isLeftValue() && !mExpression.nullable()) {
				writer.addCode("execute(");
				mExpression.writeJavaCode(mainblock, writer);
				// addComma = false;
			} else {
				writer.addCode("execute(");
				mExpression.writeJavaCode(mainblock, writer);
			}
		}

		int argCount = mParameters.size();
		if (system_function != null) argCount = Math.max(argCount, system_function.getArguments());
		for (int i = 0; i < argCount; i++) {
			if (i > 0 || addComma) writer.addCode(", ");
			if (i < mParameters.size()) {
				var parameter = mParameters.get(i);
				// Java doesn't like a single null for Object... argument
				if (argCount == 1 && parameter.getType() == Type.NULL && user_function == null) {
					writer.addCode("new Object[] { null }");
					continue;
				}
				if (mainblock.getCompiler().getCurrentAI().getVersion() >= 2) {
					parameter.writeJavaCode(mainblock, writer);
				} else {
					if (user_function != null) {
						// if (user_function.isReference(i)) {
							parameter.compileL(mainblock, writer);
						// } else {
						// 	writer.compileClone(mainblock, parameter);
						// }
					} else if (system_function != null) {
						writer.compileLoad(mainblock, parameter);
					} else {
						parameter.compileL(mainblock, writer);
					}
				}
			} else {
				// Java doesn't like a single null for Object... argument
				if (argCount == 1) {
					writer.addCode("new Object[] { null }");
				} else {
					writer.addCode("null");
				}
			}
		}
		if (addBrace) {
			writer.addCode("}");
		}
		writer.addCode(")");
		writer.addPosition(openParenthesis);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		operations = 0;
		mExpression.analyze(compiler);
		operations += mExpression.getOperations();
		for (AbstractExpression parameter : mParameters) {
			parameter.analyze(compiler);
			operations += parameter.getOperations();
		}

		if (mExpression instanceof LeekVariable) {
			var v = (LeekVariable) mExpression;

			if (v.getVariableType() == VariableType.METHOD) { // La variable est analysée comme une méthode, mais ça peut être une fonction système,

				// on regarde si le nombre d'arguments est correct
				var current = compiler.getCurrentClass();
				while (current != null) {
					var methods = current.getMethod(v.getName());
					if (methods != null) {
						for (var count : methods.keySet()) {
							if (count == mParameters.size()) {
								return; // OK
							}
						}
					}
					current = current.getParent();
				}
				// Est-ce que c'est une fonction système ?
				var f = LeekFunctions.getValue(v.getName());
				if (f != null) {
					if (mParameters.size() >= f.getArgumentsMin() && mParameters.size() <= f.getArguments()) {
						v.setVariableType(VariableType.SYSTEM_FUNCTION);
						return; // OK, fonction système
					}
				}
				// Sinon, erreur de méthode
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));

			} else if (v.getVariableType() == VariableType.STATIC_METHOD) {

				// on regarde si le nombre d'arguments est correct
				var current = compiler.getCurrentClass();
				while (current != null) {
					var methods = current.getStaticMethod(v.getName());
					if (methods != null) {
						for (var count : methods.keySet()) {
							if (count == mParameters.size()) {
								return; // OK
							}
						}
					}
					current = current.getParent();
				}
				// Est-ce que c'est une fonction système ?
				var f = LeekFunctions.getValue(v.getName());
				if (f != null) {
					if (mParameters.size() >= f.getArgumentsMin() && mParameters.size() <= f.getArguments()) {
						v.setVariableType(VariableType.SYSTEM_FUNCTION);
						return; // OK, fonction système
					}
				}
				// Sinon, erreur de méthode
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));

			} else if (v.getVariableType() == VariableType.FUNCTION) {
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

				if (compiler.getVersion() >= 3) {
					var f = LeekFunctions.getValue(v.getName());
					if (mParameters.size() > f.getArguments() || mParameters.size() < f.getArgumentsMin()) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					}
				}
				var system_function = LeekFunctions.getValue(v.getName());
				if (system_function.getReturnType() != null) {
					type = system_function.getReturnType();
				}
			} else if (v.getVariableType() == VariableType.CLASS) {

				var clazz = v.getClassDeclaration();
				var constructor = clazz.getConstructor(mParameters.size());
				if (constructor == null) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
				} else if (constructor.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_CONSTRUCTOR, new String[] { clazz.getName() }));
				} else if (constructor.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_CONSTRUCTOR, new String[] { clazz.getName() }));
				}

			} else if (v.getVariableType() == VariableType.SUPER) {

				var clazz = v.getClassDeclaration().getParent();
				if (clazz != null) {
					var constructor = clazz.getConstructor(mParameters.size());
					operations += 1;
					if (constructor == null) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_CONSTRUCTOR, new String[] { clazz.getName() }));
					} else if (constructor.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_CONSTRUCTOR, new String[] { clazz.getName() }));
					} else if (constructor.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_CONSTRUCTOR, new String[] { clazz.getName() }));
					}
				}
			}
		} else if (mExpression instanceof LeekObjectAccess) {

			var oa = (LeekObjectAccess) mExpression;
			if (oa.getObject() instanceof LeekVariable) {
				var v = (LeekVariable) oa.getObject();
				if (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS) {
					var clazz = v.getVariableType() == VariableType.CLASS ? v.getClassDeclaration() : compiler.getCurrentClass();
					var staticMethod = clazz.getStaticMethod(oa.getField(), mParameters.size());
					operations += 1;
					if (staticMethod == null) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.UNKNOWN_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
					} else if (staticMethod.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
					} else if (staticMethod.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
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
}
