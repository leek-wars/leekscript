package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction.ClassDeclarationMethod;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;
import leekscript.common.AccessLevel;
import leekscript.common.ClassType;
import leekscript.common.ClassValueType;
import leekscript.common.Error;
import leekscript.common.FunctionType;
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
	private boolean is_static_method = false;
	private ClassDeclarationMethod method;
	private Type functionType = Type.ANY;

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
		boolean addFinalParenthesis = true;
		FunctionBlock user_function = null;
		boolean convertPrimitive = false;

		if (mExpression instanceof LeekObjectAccess) {
			// Object access : object.field()
			var object = ((LeekObjectAccess) mExpression).getObject();
			var field = ((LeekObjectAccess) mExpression).getField();

			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.SUPER) {
				// super.field()
				// writer.addCode("u_this.callSuperMethod(this, \"" + field + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());

				writer.addCode("super.u_" + field + "(");
				addComma = false;

			} else if (object instanceof LeekVariable v && v.getVariableType() == VariableType.CLASS) {
				// Class.method() : Méthode statique connue
				var method = v.getClassDeclaration().getStaticMethod(field, mParameters.size());
				if (method != null) {
					// Méthode statique
					String methodName = "u_" + v.getClassDeclaration().getStaticMethodName(field, mParameters.size());
					writer.addCode(methodName + "(");
					addComma = false;
				} else {
					// Champ statique
					// writer.addCode("execute(" + v.getClassDeclaration().getName() + "." + field);
					if (type != Type.ANY) {
						writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
					}
					writer.addCode("u_" + v.getClassDeclaration().getName() + ".callStaticField(\"" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
				}
			} else if (object instanceof LeekVariable v && v.getVariableType() == VariableType.THIS) {
				var method = mainblock.getWordCompiler().getCurrentClass().getMethodName(field, mParameters.size());
				if (method != null) {
					// this.method() : Méthode connue
					// writer.addCode("callObjectAccess(u_this, \"" + field + "\", \"" + field + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
					writer.addCode("u_" + field + "(");
					addComma = false;
				} else if (this.functionType instanceof FunctionType ft) {
					writer.addCode("this." + field);
					writer.addCode(".run(");
					writer.addCode(writer.getAIThis());
					writer.addCode(", null");
					convertPrimitive = true;
				} else {
					writer.addCode("execute(this." + field);
				}
			} else if (is_static_method) {
				writer.addCode("u_" + method.block.getClassDeclaration().getName() + "_" + field + "_" + mParameters.size() + "(");
				addComma = false;
			} else if (is_method) {
				object.writeJavaCode(mainblock, writer);
				writer.addCode(".u_" + field + "(");
				addComma = false;
			} else {
				// object.field() : Méthode ou bien appel d'un champ
				if (type != Type.ANY) {
					writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				writer.addCode("callObjectAccess(");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", \"" + field + "\", \"u_" + field + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
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
			// var variable = (LeekVariable) mExpression;
			// writer.addCode("u_" + variable.getClassDeclaration().getParent().getName());
			// writer.addCode(".callConstructor(u_this");

			writer.addCode("super.init(");
			addComma = false;

		} else if (mExpression instanceof LeekVariable v && v.getVariableType() == VariableType.METHOD) {
			// Méthode connue
			// String methodName = "u_" + mainblock.getWordCompiler().getCurrentClass().getMethodName(((LeekVariable) mExpression).getName(), mParameters.size());
			// writer.addCode(methodName + "(u_this");
			writer.addCode("u_" + v.getName() + "(");
			// writer.addCode("callMethod(this, \"" + ((LeekVariable) mExpression).getName() + "_" + mParameters.size() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable());
			addComma = false;

		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.STATIC_METHOD) {
			// Méthode statique connue
			if (this.type != Type.ANY && this.type != Type.VOID) {
				writer.addCode("(" + this.type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
			}
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
				if (mParameters.get(0).getType() != Type.ANY) {
					writer.addCode("((" + mParameters.get(0).getType().getJavaName(mainblock.getVersion()) + ") ");
				}
				mParameters.get(0).writeJavaCode(mainblock, writer);
				if (mParameters.get(0).getType() != Type.ANY) {
					writer.addCode(")");
				}
				writer.addCode("." + system_function.getName() + "(" + writer.getAIThis());
				skipFirstArg = true;
			}

		} else if (mExpression instanceof LeekVariable && ((LeekVariable) mExpression).getVariableType() == VariableType.FUNCTION) {
			writer.addCode("f_");
			writer.addCode(((LeekVariable) mExpression).getName());
			writer.addCode("(");
			addComma = false;
			user_function = mainblock.getUserFunction(((LeekVariable) mExpression).getName());
		} else if (mExpression.getType() instanceof ClassValueType cvt && cvt.getClassDeclaration() != null) {
			writer.addCode("new_");
			mExpression.writeJavaCode(mainblock, writer);
			writer.addCode("(");
			addComma = false;
		} else if (mExpression instanceof LeekVariable v && v.getType() instanceof ClassValueType cvt) {
			if (cvt.getClassDeclaration() != null) {
				if (cvt.getClassDeclaration().getName() == "Integer") {
					writer.addCode("0l");
					addFinalParenthesis = false;
				} else if (cvt.getClassDeclaration().getName() == "BigInteger") {
					writer.addCode("new BigIntegerValue(" + writer.getAIThis() + ", 0)");
					addFinalParenthesis = false;
				} else if (cvt.getClassDeclaration().getName() == "Real" || cvt.getClassDeclaration().getName() == "Number") {
					writer.addCode("0.0");
					addFinalParenthesis = false;
				} else if (cvt.getClassDeclaration().getName() == "Boolean") {
					writer.addCode("false");
					addFinalParenthesis = false;
				}
			} else {
				writer.addCode("execute(");
				mExpression.writeJavaCode(mainblock, writer);
			}
		} else if (this.functionType instanceof FunctionType) {
			mExpression.writeJavaCode(mainblock, writer);
			writer.addCode(".run(");
			writer.addCode(writer.getAIThis());
			writer.addCode(", null");
			convertPrimitive = true;
		} else {
			writer.addCode("execute(");
			mExpression.writeJavaCode(mainblock, writer);
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
				if (argCount == 1 && parameter.getType() == Type.NULL && user_function == null && system_function == null && !unsafe && !is_method && !is_static_method) {
					writer.addCode("new Object[] { null }");
					continue;
				}
				// System.out.println("Argument : " + functionType.getArgument(mParameters.size(), i));
				if (mainblock.getVersion() >= 2) {
					if (system_function != null) {
						parameter.writeJavaCode(mainblock, writer);
					} else {
						writer.compileConvert(mainblock, i, parameter, functionType.getArgument(mParameters.size(), i));
					}
				} else {
					if (user_function != null) {
						parameter.compileL(mainblock, writer);
					} else if (system_function != null) {
						if (unsafe) {
							writer.compileLoad(mainblock, parameter);
						} else {
							writer.compileConvert(mainblock, i, parameter, functionType.getArgument(mParameters.size(), i));
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
		if (addFinalParenthesis) {
			writer.addCode(")");
		}
		if (convertPrimitive) {
			if (this.type == Type.INT) {
				writer.addCode(".longValue()");
			} else if (this.type == Type.REAL) {
				writer.addCode(".doubleValue()");
			} else if (this.type == Type.BOOL) {
				writer.addCode(".booleanValue()");
			}
		}
		writer.addPosition(openParenthesis);
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.preAnalyze(compiler);
		for (Expression parameter : mParameters) {
			parameter.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {

		// System.out.println("[FC] analyze " + this);

		operations = 0;

		mExpression.analyze(compiler);
		operations += mExpression.getOperations();

		// L'expression est appelable ?
		if (compiler.getMainBlock().isStrict() && !mExpression.getType().isCallable()) {
			compiler.addError(new AnalyzeError(mExpression.getLocation(), AnalyzeErrorLevel.WARNING, Error.MAY_NOT_BE_CALLABLE, new String[] {
				mExpression.toString(),
				mExpression.getType().toString()
			} ));
		}
		else if (!mExpression.getType().canBeCallable()) {
			var level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
			compiler.addError(new AnalyzeError(mExpression.getLocation(), level, Error.NOT_CALLABLE, new String[] {
				mExpression.toString(),
				mExpression.getType().toString()
			} ));
		}

		this.functionType = mExpression.getType();
		this.type = functionType.returnType();
		// System.out.println("[FC] function type = " + functionType + " args = " + functionType.getArguments() + " return = " + functionType.returnType());

		for (Expression parameter : mParameters) {
			parameter.analyze(compiler);
			operations += parameter.getOperations();
		}

		if (mExpression instanceof LeekVariable) {
			var v = (LeekVariable) mExpression;

			if (v.getVariableType() == VariableType.METHOD) { // La variable est analysée comme une méthode, mais ça peut être une fonction système,

				// on regarde si le nombre d'arguments est correct
				var current = compiler.getCurrentClass();
				boolean ok = false;
				end:
				while (current != null) {
					var methods = current.getMethod(v.getName());
					if (methods != null) {
						for (var count : methods.keySet()) {
							if (count == mParameters.size()) {
								ok = true;
								functionType = methods.get(count).block.getType();
								break end;
							}
						}
					}
					current = current.getParent();
				}
				if (!ok) {
					// Est-ce que c'est une fonction système ?
					system_function = LeekFunctions.getValue(v.getName(), compiler.getOptions().useExtra());
					if (system_function != null) {
						verifySystemFunction(compiler, v, system_function);

						if (mParameters.size() >= system_function.getArgumentsMin() && mParameters.size() <= system_function.getArguments()) {
							v.setVariableType(VariableType.SYSTEM_FUNCTION);
							return; // OK, fonction système
						}
					}
					// Sinon, erreur de méthode
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
				}

			} else if (v.getVariableType() == VariableType.STATIC_METHOD) {

				// on regarde si le nombre d'arguments est correct
				var current = compiler.getCurrentClass();
				boolean ok = false;
				end:
				while (current != null) {
					var methods = current.getStaticMethod(v.getName());
					if (methods != null) {
						for (var entry : methods.entrySet()) {
							if (entry.getKey() == mParameters.size()) {
								ok = true;
								is_static_method = true;
								method = entry.getValue();
								functionType = method.block.getType();
								break end;
							}
						}
					}
					current = current.getParent();
				}
				if (!ok) {
					// Est-ce que c'est une fonction système ?
					system_function = LeekFunctions.getValue(v.getName(), compiler.getOptions().useExtra());
					if (system_function != null) {
						verifySystemFunction(compiler, v, system_function);
						if (mParameters.size() >= system_function.getArgumentsMin() && mParameters.size() <= system_function.getArguments()) {
							v.setVariableType(VariableType.SYSTEM_FUNCTION);
							return; // OK, fonction système
						}
					}
					// Sinon, erreur de méthode
					compiler.addError(new AnalyzeError(v.getToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT));
				}

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
					system_function = LeekFunctions.getValue(v.getName(), compiler.getOptions().useExtra());
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
				this.type = clazz.getEmptyType();

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
			var o = oa.getObject();

			if (o.getType() instanceof ClassType ct) {

				// on regarde si le nombre d'arguments est correct
				var current = ct.getClassDeclaration();
				end:
				while (current != null) {
					var methods = current.getMethod(oa.getField());
					if (methods != null) {
						is_method = true;
						for (var entry : methods.entrySet()) {
							if (entry.getKey() == mParameters.size()) {
								break end;
							}
						}
					}
					current = current.getParent();
				}

			} else if (o instanceof LeekVariable v && (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS)) {

				var clazz = v.getVariableType() == VariableType.CLASS ? v.getClassDeclaration() : compiler.getCurrentClass();
				operations += 1;
				// on regarde si le nombre d'arguments est correct
				var current = clazz;
				HashMap<Integer, ClassDeclarationMethod> methods = null;
				end:
				while (current != null) {
					methods = current.getStaticMethod(oa.getField());
					if (methods != null) {
						for (var count : methods.keySet()) {
							if (count == mParameters.size()) {
								var staticMethod = methods.get(count);
								if (staticMethod.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
									compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
								} else if (staticMethod.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
									compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_METHOD, new String[] { clazz.getName(), oa.getField() }));
								}
								is_static_method = true;
								method = staticMethod;
								functionType = staticMethod.block.getType();
								// return; // OK
								break end;
							}
						}
					}
					current = current.getParent();
				}

				if (methods != null) { // Trouvée mais mauvais nombre d'arguments
					// compiler.addError(new AnalyzeError(oa.getFieldToken(), AnalyzeErrorLevel.ERROR, Error.INVALID_PARAMETER_COUNT, new String[] { clazz.getName(), oa.getField() }));
					is_static_method = true;
				} else { // Pas trouvée
					// Si c'est un champ statique, on accepte de l'appeler
					var field = clazz.getStaticField(oa.getField());
					if (field != null) {
						if (field.level == AccessLevel.PRIVATE && compiler.getCurrentClass() != clazz) {
							compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PRIVATE_STATIC_FIELD, new String[] { clazz.getName(), oa.getField() }));
						} else if (field.level == AccessLevel.PROTECTED && (compiler.getCurrentClass() == null || !compiler.getCurrentClass().descendsFrom(clazz))) {
							compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.PROTECTED_STATIC_FIELD, new String[] { clazz.getName(), oa.getField() }));
						}
						is_static_method = true;
					} else {
						compiler.addError(new AnalyzeError(oa.getLastToken(), AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), oa.getField() }));
					}
				}
			}
		}

		// System.out.println("[FC] type = " + functionType);

		if (mParameters.size() < functionType.getMinArguments() || mParameters.size() > functionType.getMaxArguments()) {

			// Pas d'erreur pour les fonctions système en LS <= 4
			var level = this.is_static_method || this.is_method || compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;

			compiler.addError(new AnalyzeError(getLocation(), level, Error.INVALID_PARAMETER_COUNT));
		}

		var types = mParameters.stream().map(p -> p.getType()).collect(Collectors.toList());
		var cast = functionType.acceptsArguments(types);
		if (cast.ordinal() > CastType.UPCAST.ordinal()) {

			if (cast == CastType.INCOMPATIBLE || compiler.getMainBlock().isStrict()) {
				// Pas d'erreur pour les fonctions système en LS <= 4
				var level = (this.is_method || compiler.getMainBlock().isStrict()) && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				var error = cast == CastType.INCOMPATIBLE ? Error.INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION;

				compiler.addError(new AnalyzeError(getLocation(), level, error, new String[] {
					"(" + String.join(", ", types.stream().map(t -> t.toString()).collect(Collectors.toList())) + ")",
					"(" + String.join(", ", functionType.getArguments().stream().map(t -> t.toString()).collect(Collectors.toList())) + ")",
				}));
			}
		}
	}

	private void verifySystemFunction(WordCompiler compiler, LeekVariable v, LeekFunctions f) throws LeekCompilerException {

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

	public void verifyVersions(WordCompiler compiler, CallableVersion[] versions) throws LeekCompilerException {

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
					AnalyzeErrorLevel level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					version_errors.add(new AnalyzeError(mParameters.get(i).getLocation(), level, Error.WRONG_ARGUMENT_TYPE, new String[] {
						String.valueOf(i + 1),
						mParameters.get(i).toString(),
						a_type.toString(),
						f_type.toString()
					}));
					version_unsafe = true;
				// } else if (cast_type.ordinal() > CastType.SAFE_DOWNCAST.ordinal()) {
				} else if (cast_type != CastType.EQUALS) {
					version_unsafe = true;
				}
				// System.out.println("cast " + f_type + " accepts " + a_type + " = " + cast_type);
				if (cast_type == CastType.UPCAST) distance += 1;
				else if (cast_type == CastType.SAFE_DOWNCAST) distance += 100;
				else if (cast_type == CastType.UNSAFE_DOWNCAST) distance += 1000;
				else if (cast_type == CastType.INCOMPATIBLE) distance += 10000;
			}
			if (distance > 1000) distance = 1000;
			// System.out.println("version = " + version + " distance = " + distance);
			if (distance == best_distance) {
				best_versions.add(version);
			} else if (distance < best_distance) {
				best_distance = distance;
				best_versions.clear();
				best_versions.add(version);
				unsafe = version_unsafe;
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
				type = Type.compound(type, version.return_type);
			}
			// type = best_versions.get(0).return_type;
			// System.out.println("[FC] version=" + best_versions.get(0) + " type=" + type + " unsafe=" + unsafe);
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
