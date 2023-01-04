package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction.ClassDeclarationMethod;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;
import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.common.Type.CastType;

public class LeekFunctionCall extends Expression {

	private Token openParenthesis = null;
	private Token closingParenthesis = null;
	private final ArrayList<Expression> mParameters = new ArrayList<Expression>();
	private Expression mExpression = null;
	private Type type = Type.ANY;
	private boolean unsafe = false;
	private ArrayList<CallableVersion> callable_versions = null;
	private LeekFunctions system_function = null;
	private boolean is_method = false;

	public LeekFunctionCall(Token openParenthesis) {
		this.openParenthesis = openParenthesis;
		this.openParenthesis.setExpression(this);
	}

	public void setExpression(Expression expression) {
		mExpression = expression;
	}

	public void setClosingParenthesis(Token closingParenthesis) {
		this.closingParenthesis = closingParenthesis;
		this.closingParenthesis.setExpression(this);
	}

	public void addParameter(Expression param) {
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
	public String toString() {
		String str = mExpression.toString() + "(";
		for(int i = 0; i < mParameters.size(); i++){
			if(i > 0) str += ", ";
			str += mParameters.get(i).toString();
		}
		return str + ")";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		if(mExpression == null || !mExpression.validExpression(compiler, mainblock)) return false;

		//Vérification de chaque paramètre
		for(Expression parameter : mParameters){
			parameter.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mainblock.getVersion() == 1 && type == Type.ANY) {
			writer.addCode("load(");
			compileL(mainblock, writer);
			writer.addCode(")");
		} else {
			compileL(mainblock, writer);
		}
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		boolean addComma = true;
		boolean addBrace = false;
		boolean skipFirstArg = false;
		FunctionBlock user_function = null;

		if (mExpression instanceof LeekObjectAccess) {
			// Object access : object.field()
			var object = ((LeekObjectAccess) mExpression).getObject();
			var field = ((LeekObjectAccess) mExpression).getField();

			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				// super.field()
				writer.addCode("u_this.callSuperMethod(this, \"" + field + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			} else if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.CLASS) {
				// Class.method() : Méthode statique connue
				var v = (LeekVariable) object;
				var method = v.getClassDeclaration().getStaticMethod(field, mParameters.size());
				if (method != null) {
					// Méthode statique
					String methodName = "u_" + v.getClassDeclaration().getStaticMethodName(field, mParameters.size());
					writer.addCode(methodName + "(");
					addComma = false;
				} else {
					// Champ statique
					writer.addCode("u_" + v.getClassDeclaration().getName() + ".callStaticField(\"" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
				}
			} else if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.THIS) {
				// this.method() : Méthode connue
				writer.addCode("callObjectAccess(u_this, \"" + field + "\", \"" + field + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			} else {
				// object.field() : Méthode ou bien appel d'un champ
				writer.addCode("callObjectAccess(");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", \"" + field + "\", \"" + field + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			}
		} else if (mExpression instanceof LeekArrayAccess) {
			var object = ((LeekArrayAccess) mExpression).getTabular();
			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				writer.addCode("u_this.callSuperMethod(mUAI, " + mainblock.getWordCompiler().getCurrentClassVariable() + ", ");
				((LeekArrayAccess) mExpression).getCase().writeJavaCode(mainblock, writer);
				writer.addCode(".getString(mUAI) + \"_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			} else {
				writer.addCode("LeekValueManager.executeArrayAccess(" + writer.getAIThis() + ", ");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", ");
				((LeekArrayAccess) mExpression).getCase().writeJavaCode(mainblock, writer);
				writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable());
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
			writer.addCode("callMethod(u_this, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());

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
			if (unsafe || mainblock.getVersion() <= 3) {
				if (callable_versions != null) {
					var key = writer.generateGenericFunction(callable_versions);
					writer.addCode(system_function.getStandardClass() + "_" + key + "(");
					addComma = false;
				} else {
					var list = new ArrayList<CallableVersion>();
					list.add(system_function.getVersions()[0]);
					writer.generateGenericFunction(list);
					writer.addCode(system_function.getStandardClass() + "_" + system_function.getName() + "_" + system_function.getVersions()[0].getParametersSignature() + "(");
					addComma = false;
				}
			} else if (system_function.isStatic()) {
				writer.addCode(system_function.getStandardClass() + "Class." + system_function.getName() + "(" + writer.getAIThis());
			} else {
				mParameters.get(0).writeJavaCode(mainblock, writer);
				writer.addCode("." + system_function.getName() + "(" + writer.getAIThis());
				skipFirstArg = true;
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
		if (system_function != null) {
			argCount = callable_versions != null ? callable_versions.get(0).arguments.length : system_function.getVersions()[0].arguments.length;
		}
		for (int i = 0; i < argCount; i++) {
			if (i == 0 && skipFirstArg) continue;
			if (i > 0 || addComma) writer.addCode(", ");
			if (i < mParameters.size()) {
				var parameter = mParameters.get(i);
				// Java doesn't like a single null for Object... argument
				if (argCount == 1 && parameter.getType() == Type.NULL && user_function == null && system_function == null && !unsafe && !is_method) {
					writer.addCode("new Object[] { null }");
					continue;
				}
				if (mainblock.getVersion() >= 2) {
					if (system_function != null && callable_versions != null && !unsafe) {
						var type = callable_versions.get(0).arguments[i];
						writer.compileConvert(mainblock, i, parameter, type);
					} else {
						parameter.writeJavaCode(mainblock, writer);
					}
				} else {
					if (user_function != null) {
						parameter.compileL(mainblock, writer);
					} else if (system_function != null) {
						if (unsafe) {
							writer.compileLoad(mainblock, parameter);
						} else {
							if (callable_versions != null) {
								var type = callable_versions.get(0).arguments[i];
								writer.compileConvert(mainblock, i, parameter, type);
							} else {
								parameter.writeJavaCode(mainblock, writer);
							}
						}
					} else {
						parameter.compileL(mainblock, writer);
					}
				}
			} else {
				// Java doesn't like a single null for Object... argument
				if (argCount == 1 && system_function == null && !unsafe) {
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
	public void preAnalyze(WordCompiler compiler) {
		mExpression.preAnalyze(compiler);
		for (Expression parameter : mParameters) {
			parameter.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {

		operations = 0;

		mExpression.analyze(compiler);
		operations += mExpression.getOperations();

		for (Expression parameter : mParameters) {
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
				system_function = LeekFunctions.getValue(v.getName());
				if (system_function != null) {
					verifySystemFunction(compiler, v, system_function);

					if (mParameters.size() >= system_function.getArgumentsMin() && mParameters.size() <= system_function.getArguments()) {
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
				system_function = LeekFunctions.getValue(v.getName());
				if (system_function != null) {
					verifySystemFunction(compiler, v, system_function);
					if (mParameters.size() >= system_function.getArgumentsMin() && mParameters.size() <= system_function.getArguments()) {
						v.setVariableType(VariableType.SYSTEM_FUNCTION);
						return; // OK, fonction système
					}
				}
				// Sinon, erreur de méthode
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));

			} else if (v.getVariableType() == VariableType.FUNCTION) {

				var f = compiler.getMainBlock().getUserFunction(v.getName());
				if (f != null) {
					int nb_params = f.countParameters();
					if (mParameters.size() != nb_params) {
						compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					}
					verifyVersions(compiler, f.getVersions());
				}

			} else if (v.getVariableType() == VariableType.SYSTEM_FUNCTION) {

				if (compiler.getMainBlock().isRedefinedFunction(v.getName())) {
					// Nothing
				} else {
					system_function = LeekFunctions.getValue(v.getName());
					verifySystemFunction(compiler, v, system_function);
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

				if (v.getVariableType() == VariableType.THIS) {

					// on regarde si le nombre d'arguments est correct
					var current = compiler.getCurrentClass();
					while (current != null) {
						var methods = current.getMethod(oa.getField());
						if (methods != null) {
							for (var count : methods.keySet()) {
								if (count == mParameters.size()) {
									return; // OK
								}
							}
						}
						current = current.getParent();
					}
					// Si la classe a un field du même nom, pas d'erreur
					if (!compiler.getCurrentClass().hasField(oa.getField())) {
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
					}

				} else if (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS) {

					var clazz = v.getVariableType() == VariableType.CLASS ? v.getClassDeclaration() : compiler.getCurrentClass();
					operations += 1;
					// on regarde si le nombre d'arguments est correct
					var current = clazz;
					HashMap<Integer, ClassDeclarationMethod> methods = null;
					while (current != null) {
						methods = current.getStaticMethod(oa.getField());
						if (methods != null) {
							for (var count : methods.keySet()) {
								if (count == mParameters.size()) {
									var staticMethod = methods.get(count);

									if (staticMethod.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
										compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
									} else if (staticMethod.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
										compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
									}
									is_method = true;
									return; // OK
								}
							}
						}
						current = current.getParent();
					}

					if (methods != null) { // Trouvée mais mauvais nombre d'arguments
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT, new String[] { clazz.getName(), oa.getField() }));
					} else { // Pas trouvée
						// Si c'est un champ statique, on accepte de l'appeler
						var field = clazz.getStaticField(oa.getField());
						if (field != null) {
							if (field.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
								compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_FIELD, new String[] { clazz.getName(), oa.getField() }));
							} else if (field.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
								compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_FIELD, new String[] { clazz.getName(), oa.getField() }));
							}
							is_method = true;
							return; // OK
						}
						compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), oa.getField() }));
					}
				}
			}
		}
	}

	private void verifySystemFunction(WordCompiler compiler, LeekVariable v, LeekFunctions f) {

		if (f.getOperations() > 0) {
			// System.out.println("cost of " + f.getName() + " : " + f.getOperations());
			operations += f.getOperations();
		}

		if (compiler.getVersion() > f.getMaxVersion()) {
			// Fonction supprimée
			if (f.getReplacement() != null) {
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.REMOVED_FUNCTION_REPLACEMENT, new String[] {
					String.valueOf(f.getMaxVersion()),
					String.valueOf(compiler.getVersion()),
					f.getReplacement()
				}));
			} else {
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.REMOVED_FUNCTION, new String[] {
					String.valueOf(f.getMaxVersion()),
					String.valueOf(compiler.getVersion())
				}));
			}
			return;
		}
		if (compiler.getVersion() < f.getMinVersion()) {
			// Fonction non disponible
			compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.FUNCTION_NOT_AVAILABLE, new String[] {
				String.valueOf(f.getMinVersion()),
				String.valueOf(compiler.getVersion())
			}));
			return;
		}

		// Verify argument count
		if (compiler.getVersion() >= 3) {
			if (mParameters.size() > f.getArguments() || mParameters.size() < f.getArgumentsMin()) {
				compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
			}
		}

		// Verify argument types
		if (f.getVersions() != null) {
			verifyVersions(compiler, f.getVersions());
		}
	}

	public void verifyVersions(WordCompiler compiler, CallableVersion[] versions) {

		var types = new ArrayList<Type>(mParameters.size());
		for (var a : mParameters) types.add(a.getType());
		// System.out.println("verifyVersions types = " + types);

		// Find best version
		ArrayList<CallableVersion> best_versions = new ArrayList<>();
		int best_distance = 9999;
		List<AnalyzeError> errors = null;
		for (var version : versions) {
			if (version.arguments.length != mParameters.size()) continue;
			int distance = 0;
			var version_errors = new ArrayList<AnalyzeError>();
			boolean version_unsafe = false;
			for (int i = 0; i < mParameters.size(); ++i) {
				var f_type = version.arguments[i];
				var a_type = mParameters.get(i).getType();
				var cast_type = f_type.accepts(a_type);
				if (cast_type == CastType.INCOMPATIBLE) {
					AnalyzeErrorLevel level = compiler.getVersion() >= 5 ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					version_errors.add(new AnalyzeError(mParameters.get(i).getLocation(), level, Error.WRONG_ARGUMENT_TYPE, new String[] {
						String.valueOf(i + 1),
						mParameters.get(i).toString(),
						a_type.name,
						f_type.name
					}));
					version_unsafe = true;
				} else if (cast_type == CastType.UNSAFE_DOWNCAST) {
					version_unsafe = true;
				}
				if (cast_type == CastType.UPCAST) distance += 1;
				else if (cast_type == CastType.SAFE_DOWNCAST) distance += 100;
				else if (cast_type == CastType.UNSAFE_DOWNCAST) distance += 1000;
				else if (cast_type == CastType.INCOMPATIBLE) distance += 10000;
			}
			if (distance >= 1000) distance = 1000;
			// System.out.println("version = " + version + " distance = " + distance);
			if (distance == best_distance) {
				best_versions.add(version);
			} else if (distance < best_distance) {
				best_distance = distance;
				best_versions.clear();
				best_versions.add(version);
				unsafe = version_unsafe;
				errors = version_errors;
			} else {
				errors = version_errors;
			}
		}
		if (errors != null) {
			for (var error : errors) {
				compiler.addError(error);
			}
		}
		if (best_versions.size() > 0) {
			callable_versions = best_versions;
			type = Type.VOID;
			for (var version : callable_versions) {
				type = type.union(version.return_type);
			}
			// System.out.println("version = " + best_versions.get(0) + " type = " + type);
		} else {
			unsafe = true;
		}
	}

	@Override
	public Location getLocation() {
		if (mExpression == null) {
			if (closingParenthesis == null) {
				return openParenthesis.getLocation();
			}
			return new Location(openParenthesis.getLocation(), closingParenthesis.getLocation());
		}
		return new Location(mExpression.getLocation(), closingParenthesis.getLocation());
	}
}
