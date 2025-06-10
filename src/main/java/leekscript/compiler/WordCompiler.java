package leekscript.compiler;

import java.math.BigInteger;
import java.util.HashSet;

import leekscript.common.AccessLevel;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.common.Type;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.ClassMethodBlock;
import leekscript.compiler.bloc.ConditionalBloc;
import leekscript.compiler.bloc.DoWhileBlock;
import leekscript.compiler.bloc.ForBlock;
import leekscript.compiler.bloc.ForeachBlock;
import leekscript.compiler.bloc.ForeachKeyBlock;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.bloc.WhileBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekArray;
import leekscript.compiler.expression.LeekBigInteger;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekCompoundType;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekFunctionCall;
import leekscript.compiler.expression.LeekInteger;
import leekscript.compiler.expression.LeekInterval;
import leekscript.compiler.expression.LeekMap;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekObject;
import leekscript.compiler.expression.LeekParameterType;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekReal;
import leekscript.compiler.expression.LeekSet;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.expression.LegacyLeekArray;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.instruction.BlankInstruction;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekBreakInstruction;
import leekscript.compiler.instruction.LeekContinueInstruction;
import leekscript.compiler.instruction.LeekExpressionInstruction;
import leekscript.compiler.instruction.LeekGlobalDeclarationInstruction;
import leekscript.compiler.instruction.LeekReturnInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class WordCompiler {

	private MainLeekBlock mMain;
	private AbstractLeekBlock mCurentBlock;
	private AbstractLeekBlock mCurrentFunction;
	private ClassDeclarationInstruction mCurrentClass;
	private LexicalParserTokenStream mTokens;
	private int mLine;
	private AIFile mAI = null;
	private final int version;
	private final Options options;

	public WordCompiler(AIFile ai, int version, Options options) {
		mAI = ai;
		this.version = version;
		this.options = options;
	}

	private void parse() throws LeekCompilerException {
		if (!mAI.hasBeenParsed()) {
			var parser = new LexicalParser(mAI, version);
			mAI.setTokenStream(parser.parse(error -> addError(error)));
		}
		mTokens = mAI.getTokenStream();
	}

	public boolean isInterrupted() {
		return System.currentTimeMillis() - mMain.getCompiler().getAnalyzeStart() > IACompiler.TIMEOUT_MS;
	}

	public void checkInterrupted() throws LeekCompilerException {
		if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
	}

	public void readCode() throws LeekCompilerException {

		firstPass();

		// Classes pré-définies :
		// System.out.println(mMain.getDefinedClasses());

		secondPass();
	}

	/**
	 * Recherche des includes, globales, classes et fonctions utilisateur
	 */
	public void firstPass() throws LeekCompilerException {
		try {
			parse();
			mTokens.reset();

			while (mTokens.hasMoreTokens()) {

				if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

				if (mTokens.get().getWord().equals("include")) {
					var token = mTokens.eat();
					// On vérifie qu'on est dans le bloc principal
					if (!mCurentBlock.equals(mMain)) throw new LeekCompilerException(mTokens.get(), Error.INCLUDE_ONLY_IN_MAIN_BLOCK);
					// On récupere l'ia
					if (mTokens.eat().getType() != TokenType.PAR_LEFT) throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);

					if (mTokens.get().getType() != TokenType.VAR_STRING) throw new LeekCompilerException(mTokens.get(), Error.AI_NAME_EXPECTED);

					String iaName = mTokens.eat().getWord();
					iaName = iaName.substring(1, iaName.length() - 1);

					if (!mMain.includeAIFirstPass(this, iaName)) {
						var location = new Location(token.getLocation(), mTokens.get().getLocation());
						addError(new AnalyzeError(location, AnalyzeErrorLevel.ERROR, Error.AI_NOT_EXISTING, new String[] { iaName }));
					}

					if (mTokens.eat().getType() != TokenType.PAR_RIGHT) throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);

				} else if (mTokens.get().getType() == TokenType.GLOBAL) {
					mTokens.skip();
					eatType(true, false);
					var global = mTokens.eat();
					// System.out.println("global = " + global.getWord() + " " + global.getLine());
					if (!isGlobalAvailable(global) || mMain.hasDeclaredGlobal(global.getWord())) {
						addError(new AnalyzeError(global, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
					} else {
						mMain.addGlobal(global.getWord());
					}
					if (mTokens.get().getWord().equals("=")) {
						mTokens.skip();
						readExpression(true);
					}
					while (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.VIRG) {
						mTokens.skip();
						global = mTokens.eat();
						if (!isGlobalAvailable(global) || mMain.hasDeclaredGlobal(global.getWord())) {
							addError(new AnalyzeError(global, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
						} else {
							mMain.addGlobal(global.getWord());
						}
						if (mTokens.get().getWord().equals("=")) {
							mTokens.skip();
							readExpression(true);
						}
					}
				} else if (mTokens.get().getType() == TokenType.FUNCTION) {
					var functionToken = mTokens.eat();
					var funcName = mTokens.eat();
					if (funcName.getWord().equals("(") || funcName.getWord().equals("<")) {
						continue;
					}
					if (!isAvailable(funcName, false)) {
						throw new LeekCompilerException(mTokens.get(), Error.FUNCTION_NAME_UNAVAILABLE);
					}
					if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
						if (functionToken.getWord().equals("Function")) {
							// Déclaration de type
							mTokens.unskip();
							continue;
						} else {
							addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.OPENING_PARENTHESIS_EXPECTED));
						}
					}
					int param_count = 0;
					var parameters = new HashSet<String>();
					while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {

						if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

						if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("@")) {
							mTokens.skip();
						}
						// if (mTokens.get().getType() != TokenType.STRING) {
						// addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
						// }
						var parameter = mTokens.eat();
						// if (parameters.contains(parameter.getWord())) {
						// 	throw new LeekCompilerException(parameter, Error.PARAMETER_NAME_UNAVAILABLE);
						// }
						parameters.add(parameter.getWord());
						param_count++;

						if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.VIRG) {
							mTokens.skip();
						}
					}
					if (mTokens.hasMoreTokens() && mTokens.eat().getType() != TokenType.PAR_RIGHT) {
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
					}

					mMain.addFunctionDeclaration(funcName.getWord(), param_count);

				} else if (mTokens.get().getWord().equals("class")) {

					mTokens.skip();
					if (mTokens.hasMoreTokens()) {
						var className = mTokens.eat();

						if (className.getType() == TokenType.STRING) {

							if (mMain.getDefinedClass(className.getWord()) != null) {
								throw new LeekCompilerException(className, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { className.getWord() });
							}

							var clazz = new ClassDeclarationInstruction(className, mLine, mAI, false, getMainBlock());
							mMain.defineClass(clazz);
							// System.out.println("Define class " + clazz.getName());
						}
					}
				} else {
					mTokens.skip();
				}
			}

		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace(System.out);
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.END_OF_SCRIPT_UNEXPECTED));
		}
	}

	public void secondPass() throws LeekCompilerException {
		mTokens = this.mAI.getTokenStream();
		assert mTokens != null : "tokens are null";
		mTokens.reset();

		// Vraie compilation
		while (mTokens.hasMoreTokens()) {

			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			// On vérifie les instructions en cours
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mTokens.skip();
			} else mCurentBlock = mCurentBlock.endInstruction();
			if (!mTokens.hasMoreTokens()) break;

			// Puis on lit l'instruction
			compileWord();
		}
		while (mCurentBlock.getParent() != null && !mCurentBlock.hasAccolade()) {

			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			if (mCurentBlock instanceof DoWhileBlock) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mTokens.skip();
			} else {
				if (mCurentBlock.endInstruction() == mCurentBlock) {
					throw new LeekCompilerException(mTokens.get(), Error.NO_BLOC_TO_CLOSE);
				}
				mCurentBlock = mCurentBlock.endInstruction();
			}
		}
		if (!mMain.equals(mCurentBlock)) throw new LeekCompilerException(mTokens.get(), Error.OPEN_BLOC_REMAINING);

		// } catch (IndexOutOfBoundsException e) {
		// 	e.printStackTrace(System.out);
		// 	addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.END_OF_SCRIPT_UNEXPECTED));
		// }
	}

	public void analyze() throws LeekCompilerException {
		// Analyse sémantique
		mCurentBlock = mMain;
		setCurrentFunction(mMain);
		mMain.preAnalyze(this);
		mMain.analyze(this);
	}

	private void compileWord() throws LeekCompilerException {
		mLine = mTokens.get().getLocation().getStartLine();
		mMain.addInstruction();
		Token word = mTokens.get();
		if (word.getType() == TokenType.END_INSTRUCTION) {
			// mCurentBlock.addInstruction(this, new BlankInstruction());
			mCurentBlock.setFull(true);
			mTokens.skip();
			return;
		} else if (word.getType() == TokenType.ACCOLADE_RIGHT) {
			// Fermeture de bloc
			if (!mCurentBlock.hasAccolade() || mCurentBlock.getParent() == null) {
				// throw new LeekCompilerException(word, Error.NO_BLOC_TO_CLOSE);
				addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.NO_BLOC_TO_CLOSE));
			} else {
				if (mCurentBlock instanceof DoWhileBlock) {
					DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
					mCurentBlock.checkEndBlock();
					mCurentBlock = mCurentBlock.getParent();
					mTokens.skip();
					dowhileendBlock(do_block);
				} else {
					mCurentBlock.checkEndBlock();
					mCurentBlock = mCurentBlock.getParent();
				}
			}
			mTokens.skip();
			return;

		} else if (word.getType() == TokenType.VAR) {

			// Déclaration de variable
			mTokens.skip();
			variableDeclaration(null);
			return;

		} else if (word.getType() == TokenType.GLOBAL) {

			// Déclaration de variable
			globalDeclaration();
			return;

		} else if (word.getType() == TokenType.RETURN) {

			var token = mTokens.eat();
			var optional = false;
			if (mTokens.get().getWord().equals("?")) {
				optional = true;
				mTokens.eat();
			}
			Expression exp = null;
			if (mTokens.get().getType() != TokenType.END_INSTRUCTION && mTokens.get().getType() != TokenType.ACCOLADE_RIGHT) {
				exp = readExpression();
			}
			if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) {
				mTokens.skip();
			}
			mCurentBlock.addInstruction(this, new LeekReturnInstruction(token, exp, optional));
			return;

		} else if (word.getType() == TokenType.FOR) {

			forBlock();
			return;

		} else if (word.getType() == TokenType.WHILE) {

			whileBlock();
			return;

		} else if (word.getType() == TokenType.IF) {

			ifBlock();
			return;

		} else if (version >= 2 && getCurrentBlock() instanceof MainLeekBlock && word.getType() == TokenType.CLASS) {

			// Déclaration de classe
			mTokens.skip();
			classDeclaration();
			return;

		} else if (word.getType() == TokenType.BREAK) {

			if (!mCurentBlock.isBreakable()) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.BREAK_OUT_OF_LOOP));
			}
			mTokens.skip();
			if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) {
				mTokens.skip();
			}
			mCurentBlock.addInstruction(this, new LeekBreakInstruction(word));

			return;

		} else if (word.getType() == TokenType.CONTINUE) {

			if (!mCurentBlock.isBreakable()) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.CONTINUE_OUT_OF_LOOP));
			}
			var token = mTokens.eat();
			if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) {
				mTokens.skip();
			}
			mCurentBlock.addInstruction(this, new LeekContinueInstruction(token));
			return;

 		} else if (word.getType() == TokenType.FUNCTION) {

			var functionToken = mTokens.eat();
			if (mTokens.get().getWord().equals("<")) { // Début d'un type
				mTokens.unskip();
			} else { // Vraie fonction
				functionBlock(functionToken);
				return;
			}

		} else if (word.getType() == TokenType.ELSE) {

			elseBlock();
			return;

		} else if (word.getType() == TokenType.DO) {

			doWhileBlock();
			return;

		} else if (word.getType() == TokenType.INCLUDE) {

			var token = mTokens.eat(); // include
			includeBlock(token);
			return;

		}

		var save = mTokens.getPosition();
		var type = eatType(true, false);
		if (type != null) {
			// Déclaration de variable ou expression ?
			if (mTokens.get().getType() == TokenType.STRING) {
				// Déclaration de variable Class a = ...
				variableDeclaration(type);
			} else {
				// Class.toto, on revient d'un token et on parse une expression
				mTokens.setPosition(save);
				var exp = readExpression();
				if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) {
					mTokens.skip();
				}
				mCurentBlock.addInstruction(this, new LeekExpressionInstruction(exp));
			}
			return;
		} else {
			var exp = readExpression();
			if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) {
				mTokens.skip();
			}
			mCurentBlock.addInstruction(this, new LeekExpressionInstruction(exp));
		}
	}

	public void writeJava(String className, JavaWriter writer, String AIClass, Options options) {
		mMain.writeJavaCode(writer, className, AIClass, options);
	}

	private void includeBlock(Token token) throws LeekCompilerException {

		if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

		// On vérifie qu'on est dans le bloc principal
		if (!mCurentBlock.equals(mMain)) throw new LeekCompilerException(mTokens.get(), Error.INCLUDE_ONLY_IN_MAIN_BLOCK);
		// On récupere l'ia
		if (mTokens.eat().getType() != TokenType.PAR_LEFT) throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);

		if (mTokens.get().getType() != TokenType.VAR_STRING) throw new LeekCompilerException(mTokens.get(), Error.AI_NAME_EXPECTED);

		String iaName = mTokens.eat().getWord();
		iaName = iaName.substring(1, iaName.length() - 1);
		if (!mMain.includeAI(this, iaName)) {
			var location = new Location(token.getLocation(), mTokens.get().getLocation());
			addError(new AnalyzeError(location, AnalyzeErrorLevel.ERROR, Error.AI_NOT_EXISTING, new String[] { iaName }));
		}

		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
	}

	private void functionBlock(Token functionToken) throws LeekCompilerException {
		// Déclaration de fonction utilisateur
		if (!mCurentBlock.equals(mMain)) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.FUNCTION_ONLY_IN_MAIN_BLOCK));
		}
		// Récupération du nom de la fonction
		if (mTokens.get().getType() != TokenType.STRING) {
			throw new LeekCompilerException(mTokens.get(), Error.FUNCTION_NAME_EXPECTED);
		}
		Token funcName = mTokens.eat();
		if (!isAvailable(funcName, false)) {
			throw new LeekCompilerException(mTokens.get(), Error.FUNCTION_NAME_UNAVAILABLE);
		}
		if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
			if (functionToken.getWord().equals("Function")) {
				// Déclaration de type
				mTokens.unskip();
				return;
			} else {
				throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);
			}
		}

		var previousFunction = mCurrentFunction;
		FunctionBlock block = new FunctionBlock(mCurentBlock, mMain, funcName);
		mCurentBlock = block;
		setCurrentFunction(block);
		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {

			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			var type = eatType(false, false);

			boolean is_reference = false;
			if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("@")) {
				is_reference = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mTokens.skip();
			}

			Token parameter = null;
			if (mTokens.get().getType() != TokenType.STRING) {
				if (type != null && type.getClass() == LeekType.class) {
					parameter = type.token;
					type = null;
				} else {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
					mTokens.skip();
				}
			} else {
				parameter = mTokens.get();
				mTokens.skip();
			}

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
			if (parameter != null) {
				block.addParameter(this, parameter, is_reference, type);
			}
		}
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
		}

		if (mTokens.get().getType() == TokenType.ARROW) {
			mTokens.skip();

			var returnType = eatType(false, true);
			if (returnType == null) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
			} else {
				block.setReturnType(returnType.getType());
			}
		}

		// On regarde s'il y a des accolades
		if (mTokens.eat().getType() != TokenType.ACCOLADE_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		}
		mMain.addFunction(block);
		setCurrentFunction(previousFunction);
	}

	private LeekType eatType(boolean first, boolean mandatory) throws LeekCompilerException {

		var type = eatOptionalType(first, mandatory);
		if (type == null) return null;

		while (mTokens.get().getWord().equals("|")) {

			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			var pipe = mTokens.eat();

			var type2 = eatOptionalType(false, true);
			if (type2 == null) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
			} else {
				type = new LeekCompoundType(type, type2, pipe);
			}
			pipe.setExpression(type);
		}
		return type;
	}

	private LeekType eatOptionalType(boolean first, boolean mandatory) throws LeekCompilerException {
		var type = eatPrimaryType(first, mandatory);
		if (type == null) return null;

		if (mTokens.get().getWord().equals("?")) {
			var question = mTokens.eat();
			type = new LeekCompoundType(type, new LeekType(question, Type.NULL), question);
		}
		return type;
	}

	private LeekType eatPrimaryType(boolean first, boolean mandatory) throws LeekCompilerException {
		var word = mTokens.get().getWord();
		if (word.equals("void")) return new LeekType(mTokens.eat(), Type.VOID);
		if (!first && word.equals("null")) return new LeekType(mTokens.eat(), Type.NULL);
		if (word.equals("boolean")) return new LeekType(mTokens.eat(), Type.BOOL);
		if (word.equals("any")) return new LeekType(mTokens.eat(), Type.ANY);
		if (word.equals("integer")) return new LeekType(mTokens.eat(), Type.INT);
		if (word.equals("big_integer")) return new LeekType(mTokens.eat(), Type.BIG_INT);
		if (word.equals("real")) return new LeekType(mTokens.eat(), Type.REAL);
		if (word.equals("string")) return new LeekType(mTokens.eat(), Type.STRING);
		if (word.equals("Class")) return new LeekType(mTokens.eat(), Type.CLASS);
		if (word.equals("Object")) return new LeekType(mTokens.eat(), Type.OBJECT);
		if (word.equals("Array") || word.equals("Set")) {
			boolean isArray = word.equals("Array");

			var array = mTokens.eat();
			LeekType arrayOrSetType;
			if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("<")) {
				arrayOrSetType = new LeekParameterType(array, mTokens.eat());
				var value = eatType(false, true);
				Type valueType = Type.ANY;
				if (value != null) valueType = value.getType();

				if (!mTokens.get().getWord().startsWith(">")) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.CLOSING_CHEVRON_EXPECTED));
				}
				((LeekParameterType) arrayOrSetType).close(mTokens.eat());
				arrayOrSetType.setType(isArray ? Type.array(valueType) : Type.set(valueType));
			} else {
				arrayOrSetType = new LeekType(array, isArray ? Type.ARRAY : Type.SET);
			}
			return arrayOrSetType;
		}
		if (word.equals("Map")) {
			var map = mTokens.eat();
			Type keyType = Type.ANY, valueType = Type.ANY;
			LeekType mapType;
			if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("<")) {
				mapType = new LeekParameterType(map, mTokens.eat());
				var key = eatType(false, true);
				if (key != null) keyType = key.getType();

				if (mTokens.get().getWord().equals(",")) {
					mTokens.eat().setExpression(mapType);
				} else {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.COMMA_EXPECTED));
				}
				var value = eatType(false, true);
				if (value != null) valueType = value.getType();
				else {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
				}

				if (!mTokens.get().getWord().startsWith(">")) addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.CLOSING_CHEVRON_EXPECTED));
				((LeekParameterType) mapType).close(mTokens.eat());

				mapType.setType(Type.map(keyType, valueType));
			} else {
				mapType = new LeekType(map, Type.MAP);
			}
			return mapType;
		}
		if (word.equals("Function")) {
			var token = mTokens.eat();
			LeekType functionType;
			if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("<")) {
				functionType = new LeekParameterType(token, mTokens.eat());

				var value = eatType(false, false);
				var function = new FunctionType(Type.ANY);
				if (value != null) function.add_argument(value.getType(), false);

				while (mTokens.get().getWord().equals(",")) {

					if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

					mTokens.eat().setExpression(functionType);

					var parameter = eatType(false, true);
					if (parameter != null) {
						function.add_argument(parameter.getType(), false);
					} else {
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
					}
				}

				if (mTokens.get().getType() == TokenType.ARROW) {
					mTokens.eat().setExpression(functionType);

					var type = eatType(false, true);
					if (type == null) {
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
					} else {
						function.setReturnType(type.getType());
					}
				}

				if (!mTokens.get().getWord().startsWith(">")) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.CLOSING_CHEVRON_EXPECTED));
				}
				((LeekParameterType) functionType).close(mTokens.eat());
				functionType.setType(function);
			} else {
				functionType = new LeekType(token, Type.FUNCTION);
			}
			return functionType;
		}

		var clazz = mMain.getDefinedClass(word);
		if (clazz != null) {
			return new LeekType(mTokens.eat(), clazz.getType());
		}

		if (mandatory) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
		}

		return null;
	}

	private void forBlock() throws LeekCompilerException {
		var token = mTokens.eat();
		// Bloc de type for(i=0;i<5;i++) ou encore for(element in tableau)
		// On peut déclarer une variable pendant l'instruction d'initialisation
		if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);
		}

		boolean isDeclaration = false;
		AbstractLeekBlock forBlock = null;

		// Là on doit déterminer si y'a déclaration de variable
		var type = eatType(true, false);
		if (mTokens.get().getWord().equals("var")) { // Il y a déclaration
			isDeclaration = true;
			mTokens.skip();
		} else if (type != null) {
			isDeclaration = true;
		}
		// Référence ?
		boolean reference1 = false;
		if (mTokens.get().getWord().equals("@")) {
			reference1 = true;
			if (getVersion() >= 2) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
			}
			mTokens.skip();
		}
		// On récupère ensuite le nom de la variable
		if (mTokens.get().getType() != TokenType.STRING) throw new LeekCompilerException(mTokens.get(), Error.VARIABLE_NAME_EXPECTED);
		Token varName = mTokens.eat();

		// Maintenant on va savoir si on a affaire à un for (i in array) ou à un for(i=0;i<...
		if (mTokens.get().getWord().equals(":")) { // C'est un for (key:value in array)
			mTokens.skip();
			boolean isValueDeclaration = false;

			var valueType = eatType(true, false);
			if (mTokens.get().getWord().equals("var")) { // Il y a déclaration de la valeur
				isValueDeclaration = true;
				mTokens.skip();
			} else if (valueType != null) {
				isValueDeclaration = true;
			}
			// Référence ?
			boolean reference2 = false;
			if (mTokens.get().getWord().equals("@")) {
				reference2 = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mTokens.skip();
			}
			// On récupère ensuite le nom de la variable accueillant la valeur
			if (mTokens.get().getType() != TokenType.STRING) throw new LeekCompilerException(mTokens.get(), Error.VARIABLE_NAME_EXPECTED);
			Token valueVarName = mTokens.eat();

			if (!mTokens.eat().getWord().equals("in")) throw new LeekCompilerException(mTokens.get(), Error.KEYWORD_IN_EXPECTED);

			// On déclare notre bloc foreach et on entre dedans
			ForeachKeyBlock block = new ForeachKeyBlock(mCurentBlock, mMain, isDeclaration, isValueDeclaration, token, reference1, reference2);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			var array = readExpression();
			block.setArray(array);
			block.setKeyIterator(this, varName, isDeclaration, type);
			block.setValueIterator(this, valueVarName, isValueDeclaration, valueType);

			forBlock = block;
		} else if (mTokens.get().getWord().equals("in")) { // C'est un for (i in array)
			mTokens.skip();

			ForeachBlock block = new ForeachBlock(mCurentBlock, mMain, isDeclaration, token, reference1);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			var array = readExpression();
			block.setArray(array);
			block.setIterator(this, varName, type == null ? Type.ANY : type.getType());

			forBlock = block;
		} else if (mTokens.get().getWord().equals("=")) { // C'est un for (i=0;i<1;i++)
			mTokens.skip();

			ForBlock block = new ForBlock(mCurentBlock, mMain, token);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On récupère la valeur de base du compteur
			var initValue = readExpression();
			if (mTokens.eat().getType() != TokenType.END_INSTRUCTION) {
				// errors.add(new AnalyzeError(mTokens.getWord(), AnalyzeErrorLevel.ERROR, Error.END_OF_INSTRUCTION_EXPECTED));
				throw new LeekCompilerException(mTokens.get(), Error.END_OF_INSTRUCTION_EXPECTED);
				// return;
			}
			var condition = readExpression();
			if (mTokens.eat().getType() != TokenType.END_INSTRUCTION) {
				// errors.add(new AnalyzeError(mTokens.getWord(), AnalyzeErrorLevel.ERROR, Error.END_OF_INSTRUCTION_EXPECTED));
				throw new LeekCompilerException(mTokens.get(), Error.END_OF_INSTRUCTION_EXPECTED);
				// return;
			}
			// if (mTokens.getWord().getType() == TokenType.END_INSTRUCTION) {
			// 	mTokens.skipWord();
			// }
			var incrementation = readExpression();

			// Attention si l'incrémentation n'est pas une expression Java fait la gueule !
			if (incrementation != null && (incrementation instanceof LeekVariable || (incrementation instanceof LeekExpression && ((LeekExpression) incrementation).getOperator() == -1))) {
				throw new LeekCompilerException(mTokens.get(), Error.UNCOMPLETE_EXPRESSION);
			}

			block.setInitialisation(this, varName, initValue, isDeclaration, block.hasGlobal(varName.getWord()), type == null ? Type.ANY : type.getType());
			block.setCondition(condition);
			block.setIncrementation(incrementation);

			forBlock = block;
		} else throw new LeekCompilerException(mTokens.get(), Error.KEYWORD_UNEXPECTED);

		// On vérifie la parenthèse fermante
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		// On regarde s'il y a des accolades
		if (mTokens.get().getType() == TokenType.ACCOLADE_LEFT) {
			mTokens.skip();
		} else forBlock.noAccolade();
	}

	private void whileBlock() throws LeekCompilerException {
		var token = mTokens.eat();
		if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		var exp = readExpression();
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		WhileBlock bloc = new WhileBlock(mCurentBlock, mMain, token);
		bloc.setCondition(exp);
		if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.ACCOLADE_LEFT) {
			mTokens.skip();
		} else if (mTokens.get().getType() == TokenType.END_INSTRUCTION) {
			mTokens.skip();
			bloc.addInstruction(this, new BlankInstruction());
			bloc.noAccolade();
		} else {
			bloc.noAccolade();
		}
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void doWhileBlock() throws LeekCompilerException {
		var token = mTokens.eat();
		DoWhileBlock bloc = new DoWhileBlock(mCurentBlock, mMain, token);
		if (mTokens.get().getType() == TokenType.ACCOLADE_LEFT) {
			mTokens.skip();
		} else bloc.noAccolade();
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void dowhileendBlock(DoWhileBlock bloc) throws LeekCompilerException {
		if (!mTokens.eat().getWord().equals("while")) throw new LeekCompilerException(mTokens.get(), Error.WHILE_EXPECTED_AFTER_DO);
		if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		bloc.setCondition(readExpression());
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		// if (mTokens.getWord().getType() != TokenType.END_INSTRUCTION)
		// 	throw new LeekCompilerException(mTokens.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
	}

	private void elseBlock() throws LeekCompilerException {
		var token = mTokens.eat();
		// On vérifie qu'on est bien associé à un bloc conditionnel
		ConditionalBloc last = mCurentBlock.getLastOpenedConditionalBlock();
		if (last == null || last.getCondition() == null) {
			throw new LeekCompilerException(mTokens.get(), Error.NO_IF_BLOCK);
		}
		ConditionalBloc bloc = new ConditionalBloc(mCurentBlock, mMain, token);
		bloc.setParentCondition(last);
		if (mTokens.get().getWord().equals("if")) {
			// On veut un elseif
			mTokens.skip();
			if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
				throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);
			}
			var exp = readExpression();
			if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
				throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
			}
			bloc.setCondition(exp);
		}

		if (mTokens.get().getType() == TokenType.ACCOLADE_LEFT) {
			mTokens.skip();
		} else bloc.noAccolade();
		last.getParent().addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void ifBlock() throws LeekCompilerException {
		var token = mTokens.eat();
		if (mTokens.eat().getType() != TokenType.PAR_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		var exp = readExpression();
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		var bloc = new ConditionalBloc(mCurentBlock, mMain, token);
		bloc.setCondition(exp);
		if (mTokens.get().getType() == TokenType.ACCOLADE_LEFT) {
			mTokens.skip();
		} else if (mTokens.get().getType() == TokenType.END_INSTRUCTION) {
			mTokens.skip();
			bloc.addInstruction(this, new BlankInstruction());
			bloc.noAccolade();
		} else bloc.noAccolade();
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void globalDeclaration() throws LeekCompilerException {
		// Il y a au moins une premiere variable
		Token token = mTokens.eat();

		var type = eatType(true, false);

		Token word = mTokens.eat();
		if (!(mCurentBlock instanceof MainLeekBlock)) {
			throw new LeekCompilerException(word, Error.GLOBAL_ONLY_IN_MAIN_BLOCK);
		}
		if (word.getType() != TokenType.STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED_AFTER_GLOBAL);
		}
		var variable = new LeekGlobalDeclarationInstruction(token, word, type);
		// On regarde si une valeur est assignée
		if (mTokens.get().getWord().equals("=")) {
			mTokens.skip();
			// Si oui on récupère la valeur en question
			variable.setValue(readExpression(true));
		}
		// On ajoute la variable
		mMain.addGlobalDeclaration(variable);
		mCurentBlock.addInstruction(this, variable);
		while (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.VIRG) {

			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			// On regarde si y'en a d'autres
			mTokens.skip();// On passe la virgule
			word = mTokens.eat();
			if (word.getType() != TokenType.STRING) throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
			variable = new LeekGlobalDeclarationInstruction(token, word, type);
			// On regarde si une valeur est assignée
			if (mTokens.get().getWord().equals("=")) {
				mTokens.skip();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression(true));
			}
			// On ajoute la variable
			mMain.addGlobalDeclaration(variable);
			mCurentBlock.addInstruction(this, variable);
		}
		// word = mTokens.readWord();
		// if (word.getType() != TokenType.END_INSTRUCTION)
		// throw new LeekCompilerException(word, Error.END_OF_INSTRUCTION_EXPECTED);
		if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) mTokens.skip();
	}

	private void variableDeclaration(LeekType type) throws LeekCompilerException {
		// Il y a au moins une premiere variable
		Token word = mTokens.eat();
		if (word.getType() != TokenType.STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
		}

		if (getVersion() >= 3 && isKeyword(word)) {
			addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
		}
		LeekVariableDeclarationInstruction variable = new LeekVariableDeclarationInstruction(this, word, getCurrentFunction(), type);
		// On regarde si une valeur est assignée
		if (mTokens.hasMoreTokens() && mTokens.get().getWord().equals("=")) {
			mTokens.skip();

			// Arrow function?
			int p = mTokens.getOffsetToNextClosingParenthesis();
			int a = mTokens.getOffsetToNextArrow();
			boolean isArrowFunction = a != -1 && (a < p || p == -1);

			// Si oui on récupère la valeur en question
			variable.setValue(readExpression(!isArrowFunction));
		}
		mCurentBlock.addInstruction(this, variable);

		while (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.VIRG) {

			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			// On regarde si y'en a d'autres
			mTokens.skip();// On passe la virgule
			word = mTokens.eat();
			if (word.getType() != TokenType.STRING) throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
			if (getVersion() >= 3 && isKeyword(word)) {
				addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			}
			variable = new LeekVariableDeclarationInstruction(this, word, getCurrentFunction(), type);
			// On regarde si une valeur est assignée
			if (mTokens.get().getWord().equals("=")) {
				mTokens.skip();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression(true));
			}
			// On ajoute la variable
			mCurentBlock.addInstruction(this, variable);
		}
		if (mTokens.hasMoreTokens() && mTokens.get().getType() == TokenType.END_INSTRUCTION) {
			mTokens.skip();
		}
	}

	public void classDeclaration() throws LeekCompilerException {
		// Read class name
		Token word = mTokens.eat();
		if (word.getType() != TokenType.STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
		}
		if (isKeyword(word)) {
			addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { word.getWord() }));
		}
		ClassDeclarationInstruction classDeclaration = mMain.getDefinedClass(word.getWord());
		assert classDeclaration != null : "Class " + word.getWord() + " not declared (" + mMain.getDefinedClasses().size() + " classes)";
		mMain.addClassList(classDeclaration);
		mCurrentClass = classDeclaration;

		if (mTokens.get().getType() == TokenType.EXTENDS) {
			mTokens.skip();
			Token parent = mTokens.eat();
			classDeclaration.setParent(parent);
		}
		if (mTokens.get().getType() != TokenType.ACCOLADE_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		}
		mTokens.skip();

		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.ACCOLADE_RIGHT) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			word = mTokens.get();
			switch (word.getWord()) {
				case "public":
				case "private":
				case "protected":
				{
					AccessLevel level = AccessLevel.fromString(word.getWord());
					mTokens.skip();
					classAccessLevelMember(classDeclaration, level);
					break;
				}
				case "static":
				{
					mTokens.skip();
					classStaticMember(classDeclaration, AccessLevel.PUBLIC);
					break;
				}
				case "final":
				{
					mTokens.skip();
					endClassMember(classDeclaration, AccessLevel.PUBLIC, false, true);
					break;
				}
				case "constructor":
				{
					mTokens.skip();
					classConstructor(classDeclaration, AccessLevel.PUBLIC, word);
					break;
				}
				default:
				{
					endClassMember(classDeclaration, AccessLevel.PUBLIC, false, false);
				}
			}
		}
		if (mTokens.get().getType() != TokenType.ACCOLADE_RIGHT) {
			throw new LeekCompilerException(mTokens.get(), Error.END_OF_CLASS_EXPECTED);
		}
		mTokens.skip();
		mCurrentClass = null;
	}

	public void classStaticMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		Token token = mTokens.get();
		switch (token.getWord()) {
			case "final":
				mTokens.skip();
				endClassMember(classDeclaration, accessLevel, true, true);
				return;
		}
		endClassMember(classDeclaration, accessLevel, true, false);
	}

	public void classAccessLevelMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		Token token = mTokens.get();
		switch (token.getWord()) {
			case "constructor":
				mTokens.skip();
				classConstructor(classDeclaration, accessLevel, token);
				return;
			case "static":
				mTokens.skip();
				classStaticMember(classDeclaration, accessLevel);
				return;
			case "final":
				mTokens.skip();
				endClassMember(classDeclaration, accessLevel, false, true);
				return;
		}
		endClassMember(classDeclaration, accessLevel, false, false);
	}

	public void endClassMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel, boolean isStatic, boolean isFinal) throws LeekCompilerException {

		var isStringMethod = mTokens.get().getWord().equals("string") && mTokens.get(1).getType() == TokenType.PAR_LEFT;

		var typeExpression = isStringMethod ? null : eatType(false, false);

		Token name = mTokens.eat();
		if (name.getType() != TokenType.STRING) {
			addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_EXPECTED, new String[] { name.getWord() }));
			return;
		}

		if (name.getWord().equals("super") || name.getWord().equals("class")) {
			addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { name.getWord() }));
		} else if (isKeyword(name)) {
			addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { name.getWord() }));
		}

		// Field
		Expression expr = null;
		if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("=")) {
			mTokens.skip();
			expr = readExpression();
		} else if (mTokens.get().getType() == TokenType.PAR_LEFT) {
			// Méthode
			ClassMethodBlock method = classMethod(classDeclaration, name, false, isStatic, typeExpression == null ? Type.ANY : typeExpression.getType());
			if (isStatic) {
				classDeclaration.addStaticMethod(this, name, method, accessLevel);
			} else {
				classDeclaration.addMethod(this, name, method, accessLevel);
			}
			if (mTokens.get().getType() == TokenType.END_INSTRUCTION) mTokens.skip();
			return;
		}

		if (isStatic) {
			// System.out.println(classDeclaration);
			assert classDeclaration != null;
			classDeclaration.addStaticField(this, name, expr, accessLevel, isFinal, typeExpression != null ? typeExpression.getType() : Type.ANY);
		} else {
			classDeclaration.addField(this, name, expr, accessLevel, isFinal, typeExpression != null ? typeExpression.getType() : Type.ANY);
		}

		if (mTokens.get().getType() == TokenType.END_INSTRUCTION) mTokens.skip();
	}

	public void classConstructor(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel, Token token) throws LeekCompilerException {
		ClassMethodBlock constructor = classMethod(classDeclaration, token, true, false, Type.VOID);
		classDeclaration.addConstructor(this, constructor, accessLevel);
	}

	public ClassMethodBlock classMethod(ClassDeclarationInstruction classDeclaration, Token token, boolean isConstructor, boolean isStatic, Type returnType) throws LeekCompilerException {

		ClassMethodBlock method = new ClassMethodBlock(classDeclaration, isConstructor, isStatic, mCurentBlock, mMain, token, returnType);

		Token word = mTokens.eat();
		if (word.getType() != TokenType.PAR_LEFT) {
			throw new LeekCompilerException(word, Error.OPENING_PARENTHESIS_EXPECTED);
		}
		int param_count = 0;
		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("@")) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				mTokens.skip();
			}
			var type = eatType(false, false);

			if (mTokens.get().getType() != TokenType.STRING) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
			}
			var param = mTokens.eat();
			Token equal = null;
			Expression defaultValue = null;

			// Default param
			if (mTokens.get().getWord().equals("=")) {
				equal = mTokens.eat();
				defaultValue = readExpression(true);
			}

			method.addParameter(this, param, equal, type == null ? Type.ANY : type.getType(), defaultValue);
			param_count++;

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
		}
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
		}
		if (classDeclaration.hasMethod(token.getWord(), param_count)) {
			throw new LeekCompilerException(mTokens.get(), Error.CONSTRUCTOR_ALREADY_EXISTS);
		}

		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		int initialLine = mLine;
		AIFile initialAI = mAI;
		mCurentBlock = method;

		// Ouverture des accolades
		if (mTokens.eat().getType() != TokenType.ACCOLADE_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		}

		// Lecture du corps de la fonction
		while (mTokens.hasMoreTokens()) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mTokens.skip();
			} else mCurentBlock = mCurentBlock.endInstruction();
			if (!mTokens.hasMoreTokens()) break;

			// On regarde si on veut fermer la fonction anonyme
			if (mTokens.get().getType() == TokenType.ACCOLADE_RIGHT && mCurentBlock == method) {
				mTokens.skip();
				break; // Fermeture de la fonction anonyme
			} else compileWord();
		}
		// On remet le bloc initial
		mCurentBlock = initialBlock;
		mLine = initialLine;
		mAI = initialAI;
		return method;
	}

	public Expression readExpression() throws LeekCompilerException {
		return readExpression(false, false, false);
	}

	public Expression readExpression(boolean inList) throws LeekCompilerException {
		return readExpression(inList, false, false);
	}

	public Expression readExpression(boolean inList, boolean inSet) throws LeekCompilerException {
		return readExpression(inList, inSet, false);
	}

	public Expression readExpression(boolean inList, boolean inSet, boolean inInterval) throws LeekCompilerException {

		var retour = new LeekExpression();

		// Lambda
		boolean parenthesis = false;
		Token lambdaToken = null;
		LeekType type1 = null;
		var pos = mTokens.getPosition();
		if (mTokens.get().getType() == TokenType.PAR_LEFT) {
			lambdaToken = mTokens.eat();
			parenthesis = true;
		}
		type1 = eatType(true, false);
		var t1 = mTokens.get().getType();
		var t2 = mTokens.get(1).getType();
		var t3 = mTokens.get(2).getType();
		// var t4 = mTokens.get(3).getType();
		if (t1 == TokenType.ARROW // =>
				|| ((!inList || parenthesis) && t1 == TokenType.STRING && t2 == TokenType.VIRG) // x,
				//  || (t1 == TokenType.PAR_LEFT && t2 == TokenType.STRING && t3 == TokenType.VIRG) // (x,
				|| (t1 == TokenType.STRING && t2 == TokenType.ARROW) // x =>
				|| (parenthesis && t1 == TokenType.STRING && t2 == TokenType.PAR_RIGHT && t3 == TokenType.ARROW) // (x) =>
		//  || (t1 == TokenType.PAR_LEFT && t2 == TokenType.STRING && t3 == TokenType.PAR_RIGHT && t4 == TokenType.ARROW) // (x) =>
		) {
			if (!parenthesis) {
				lambdaToken = t1 == TokenType.ARROW ? mTokens.get() : mTokens.get(1); // , ou =>
			}

			var block = new AnonymousFunctionBlock(getCurrentBlock(), getMainBlock(), lambdaToken);
			AbstractLeekBlock initialBlock = mCurentBlock;
			setCurrentBlock(block);

			boolean first = true;
			while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT && mTokens.get().getType() != TokenType.ARROW) {
				if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

				if (mTokens.get().getType() != TokenType.STRING) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
					break;
				}
				var type = first && type1 != null ? type1 : eatType(false, false);
				var parameter = mTokens.get();
				mTokens.skip();

				if (mTokens.get().getType() == TokenType.VIRG) {
					mTokens.skip();
				}
				block.addParameter(this, parameter, false, type == null ? Type.ANY : type.getType());
				first = false;
			}

			boolean surroudingParenthesis = false;
			if (parenthesis) {
				if (mTokens.get().getType() == TokenType.ARROW) {
					surroudingParenthesis = true;
				} else {
					if (mTokens.get().getType() != TokenType.PAR_RIGHT) {
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
					}
					mTokens.skip();
				}
			}
			if (mTokens.get().getType() != TokenType.ARROW) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.ARROW_EXPECTED));
			}
			mTokens.skip();

			// Type de retour
			pos = mTokens.getPosition();
			var returnType = eatType(false, false);
			if (returnType != null) {
				if (mTokens.get().getWord().equals(".")) {
					mTokens.setPosition(pos); // On prend pas le type si "Type.[...]"
				} else {
					block.setReturnType(returnType.getType());
				}
			}

			// boolean surroudingCurlyBracket = false;
			if (mTokens.get().getType() == TokenType.ACCOLADE_LEFT) {
				// surroudingCurlyBracket = true;
				mTokens.skip();

				// Lecture du corps de la fonction
				while (mTokens.hasMoreTokens()) {
					if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
					// Fermeture des blocs ouverts
					if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
						DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
						mCurentBlock = mCurentBlock.endInstruction();
						dowhileendBlock(do_block);
						mTokens.skip();
					} else mCurentBlock = mCurentBlock.endInstruction();
					if (!mTokens.hasMoreTokens()) break;

					// On regarde si on veut fermer la fonction anonyme
					if (mTokens.get().getType() == TokenType.ACCOLADE_RIGHT && mCurentBlock == block) {
						mTokens.skip();
						break; // Fermeture de la fonction anonyme
					} else compileWord();
				}
			} else {

				// Expression seule
				var body = readExpression();
				block.addInstruction(this, new LeekReturnInstruction(lambdaToken, body, false));
			}

			if (surroudingParenthesis) {
				if (mTokens.get().getType() != TokenType.PAR_RIGHT) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
				}
				mTokens.skip();
			}

			setCurrentBlock(initialBlock);
			var f = new LeekAnonymousFunction(block, lambdaToken);
			retour.addExpression(f);

		} else {
			// Pas une lambda, on revient au début
			mTokens.setPosition(pos);
		}

		while (mTokens.hasMoreTokens()) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			Token word = mTokens.get();
			if (word.getType() == TokenType.PAR_RIGHT || word.getType() == TokenType.ACCOLADE_RIGHT || word.getType() == TokenType.END_INSTRUCTION) {
				break;
			}
			if (retour.needOperator()) {
				// Si on attend un opérateur mais qu'il vient pas

				if (word.getType() == TokenType.BRACKET_LEFT) {

					var save = mTokens.getPosition();

					var bracket = mTokens.eat(); // On avance le curseur pour être au début de l'expression
					Token colon = null;
					Token colon2 = null;
					Expression start = null;
					Expression end = null;
					Expression stride = null;

					if (mTokens.get().getType() == TokenType.BRACKET_RIGHT) {
						// Crochet fermant direct
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.VALUE_EXPECTED));
					} else if (getVersion() >= 4 && mTokens.get().getWord().equals(":")) {
						colon = mTokens.eat();
						if (getVersion() >= 4 && mTokens.get().getWord().equals(":")) {
							colon2 = mTokens.eat();
							if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
								stride = readExpression();
							}
						} else if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
							end = readExpression();
							if (getVersion() >= 4 && mTokens.get().getWord().equals(":")) {
								colon2 = mTokens.eat();
								if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
									stride = readExpression();
								}
							}
						}
					} else if (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT && mTokens.get().getType() != TokenType.VIRG) {
						start = readExpression();
						if (getVersion() >= 4 && mTokens.get().getWord().equals(":")) {
							colon = mTokens.eat();
							if (getVersion() >= 4 && mTokens.get().getWord().equals(":")) {
								colon2 = mTokens.eat();
								if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
									stride = readExpression();
								}
							} else if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
								end = readExpression();
								if (getVersion() >= 4 && mTokens.get().getWord().equals(":")) {
									colon2 = mTokens.eat();
									if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
										stride = readExpression();
									}
								}
							}
						}
					}

					if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
						if (inInterval) {
							mTokens.setPosition(save);
							break;
						} else {
							throw new LeekCompilerException(mTokens.get(), Error.CLOSING_SQUARE_BRACKET_EXPECTED);
						}
					}
					retour.addBracket(bracket, start, colon, end, colon2, stride, mTokens.get());

				} else if (word.getType() == TokenType.PAR_LEFT) {

					LeekFunctionCall function = new LeekFunctionCall(word);
					mTokens.skip(); // On avance le curseur pour être au début de l'expression

					while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {
						if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
						function.addParameter(readExpression(true));
						if (mTokens.get().getType() == TokenType.VIRG) mTokens.skip();
					}
					if (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
					}
					function.setClosingParenthesis(mTokens.get());
					retour.addFunction(function);

				} else if (word.getType() == TokenType.DOT) {
					// Object access
					var dot = mTokens.eat();
					if (mTokens.get().getType() == TokenType.STRING || mTokens.get().getType() == TokenType.CLASS || mTokens.get().getType() == TokenType.SUPER) {
						var name = mTokens.get();
						retour.addObjectAccess(dot, name);
					} else {
						addError(new AnalyzeError(dot, AnalyzeErrorLevel.ERROR, Error.VALUE_EXPECTED));
						retour.addObjectAccess(dot, null);
						mTokens.unskip();
					}
				} else if (word.getType() == TokenType.IN) {

					mTokens.skip();
					retour.addOperator(Operators.IN, word);
					continue;

				} else if (word.getType() == TokenType.AS) {

					mTokens.skip();
					var type = eatType(false, true);
					if (type != null) {
						retour.addOperator(Operators.AS, word);
						retour.addExpression(type);
					}
					continue;

				} else if (word.getType() == TokenType.OPERATOR && (!word.getWord().equals(">") || !inSet)) {

					int operator = Operators.getOperator(word.getWord(), getVersion());

					// Handle ">>", ">>=", ">>>", ">>>=" operator
					if (word.getWord().equals(">")) {
						var nextToken = mTokens.get(1);
						if (nextToken.getType() == TokenType.OPERATOR && nextToken.getWord().equals(">")) {
							mTokens.skip();
							operator = Operators.SHIFT_RIGHT;
							if (mTokens.get(1).getType() == TokenType.OPERATOR && mTokens.get(1).getWord().equals(">=")) {
								mTokens.skip();
								operator = Operators.SHIFT_UNSIGNED_RIGHT_ASSIGN;
							} else if (mTokens.get(1).getType() == TokenType.OPERATOR && mTokens.get(1).getWord().equals(">")) {
								mTokens.skip();
								operator = Operators.SHIFT_UNSIGNED_RIGHT;
							}
						} else if (nextToken.getType() == TokenType.OPERATOR && nextToken.getWord().equals(">=")) {
							operator = Operators.SHIFT_RIGHT_ASSIGN;
							mTokens.skip();
						}
						retour.addOperator(operator, word);

					} else {
						if (operator == Operators.SHIFT_RIGHT) {
							var nextToken = mTokens.get(1);
							if (nextToken.getType() == TokenType.OPERATOR && nextToken.getWord().equals(">")) {
								operator = Operators.SHIFT_UNSIGNED_RIGHT;
								mTokens.skip();
							} else if (nextToken.getType() == TokenType.OPERATOR && nextToken.getWord().equals(">=")) {
								operator = Operators.SHIFT_UNSIGNED_RIGHT_ASSIGN;
								mTokens.skip();
							}
						}

						// Là c'est soit un opérateur (+ - ...) soit un suffix
						// unaire (++ -- ) sinon on sort de l'expression
						if (Operators.isUnaryPrefix(operator)) break;
						if (operator == Operators.DOUBLE_POINT && !retour.hasTernaire()) break;

						if (Operators.isUnarySuffix(operator)) retour.addUnarySuffix(operator, word);
						else retour.addOperator(operator, word);
					}
				} else if (word.getType() == TokenType.STRING) {
					if (word.getWord().equals("is")) {
						mTokens.skip();
						word = mTokens.get();
						if (word.getWord().equals("not")) {
							Token token = mTokens.eat();
							retour.addOperator(Operators.NOTEQUALS, token);
						} else {
							retour.addOperator(Operators.EQUALS, word);
						}
						continue;
					}
					break;
				} else break;
			} else {
				if (word.getType() == TokenType.NUMBER) {
					var s = word.getWord();
					if (s.contains("__")) {
						addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.MULTIPLE_NUMERIC_SEPARATORS));
					}
					s = word.getWord().replace("_", "");
					var radix = s.startsWith("0x") ? 16 : s.startsWith("0b") ? 2 : 10;
					if (radix != 10) s = s.substring(2);
					if (s.endsWith("L")) {
						try {
							s = s.substring(0, s.length() - 1);
							retour.addExpression(new LeekBigInteger(word, new BigInteger(s, radix)));
						} catch (NumberFormatException e) {
							addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.INVALID_NUMBER));
							retour.addExpression(new LeekBigInteger(word, BigInteger.ZERO));
						}
					} else {
						try {
							try {
								retour.addExpression(new LeekInteger(word, Long.parseLong(s, radix)));
							} catch (NumberFormatException e2) {
								if (s.contains(".")) throw e2;
								// if number is too big, try to parse it as a BigInteger
								else retour.addExpression(new LeekBigInteger(word, new BigInteger(s, radix)));
							}
						} catch (NumberFormatException e) {
							s = word.getWord().replace("_", "");
							try {
								retour.addExpression(new LeekReal(word, Double.parseDouble(s)));
							} catch (NumberFormatException e2) {
								addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.INVALID_NUMBER));
								retour.addExpression(new LeekInteger(word, 0));
							}
						}
					}
				} else if (word.getType() == TokenType.LEMNISCATE) {

					retour.addExpression(new LeekReal(word, Double.POSITIVE_INFINITY));

				} else if (word.getType() == TokenType.PI) {

					retour.addExpression(new LeekReal(word, Math.PI));

				} else if (word.getType() == TokenType.VAR_STRING) {

					retour.addExpression(new LeekString(word, word.getWord()));

				} else if (word.getType() == TokenType.BRACKET_LEFT) {

					retour.addExpression(readArrayOrMapOrInterval(mTokens.eat()));

				} else if (word.getType() == TokenType.BRACKET_RIGHT) {

					var token = mTokens.eat();
					if (mTokens.get().getType() == TokenType.DOT_DOT) {
						// interval `]..`
						mTokens.skip();
						retour.addExpression(readInterval(token, null));
					} else {
						// interval `]x..`
						var expression = readExpression(true);
						var dot_dot = mTokens.eat();
						if (dot_dot.getType() != TokenType.DOT_DOT) {
							addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.DOT_DOT_EXPECTED));
						}
						retour.addExpression(readInterval(token, expression));
					}

				} else if (word.getType() == TokenType.OPERATOR && word.getWord().equals("<")) {

					retour.addExpression(readSet(mTokens.eat()));

				} else if (getVersion() >= 2 && word.getType() == TokenType.ACCOLADE_LEFT) {

					// Déclaration d'un objet
					var token = mTokens.eat();
					var object = new LeekObject(token);

					while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.ACCOLADE_RIGHT) {
						if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
						if (mTokens.get().getType() != TokenType.STRING) {
							addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
						}
						String key = mTokens.get().getWord();
						mTokens.skip();

						if (!mTokens.get().getWord().equals(":")) {
							addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
						}
						mTokens.skip();

						var value = readExpression(true);
						object.addEntry(key, value);

						if (mTokens.get().getType() == TokenType.VIRG) {
							mTokens.skip();
						}
					}
					if (mTokens.get().getType() != TokenType.ACCOLADE_RIGHT) {
						addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.CLOSING_PARENTHESIS_EXPECTED));
					}
					object.setClosingBrace(mTokens.get());
					retour.addExpression(object);

				} else if (word.getType() == TokenType.CLASS) {
					retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
				} else if (word.getType() == TokenType.THIS) {
					retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
				} else if (word.getType() == TokenType.TRUE) {
					retour.addExpression(new LeekBoolean(word, true));
				} else if (word.getType() == TokenType.FALSE) {
					retour.addExpression(new LeekBoolean(word, false));
				} else if (word.getType() == TokenType.FUNCTION) {
					retour.addExpression(readAnonymousFunction());
				} else if (word.getType() == TokenType.NULL) {
					retour.addExpression(new LeekNull(word));
				} else if (getVersion() >= 2 && word.getType() == TokenType.NEW) {
					retour.addUnaryPrefix(Operators.NEW, word);
				} else if (word.getType() == TokenType.NOT) {
					retour.addUnaryPrefix(Operators.NOT, word);
				} else if (word.getType() == TokenType.SUPER) {
					// super doit être dans une méthode
					if (mCurrentClass == null) {
						addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.KEYWORD_MUST_BE_IN_CLASS));
						retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
					} else {
						if (mCurrentClass.getParentToken() == null) {
							addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.SUPER_NOT_AVAILABLE_PARENT));
						}
						retour.addExpression(new LeekVariable(word, VariableType.SUPER, Type.CLASS, mCurrentClass));
					}
				} else if (word.getType() == TokenType.STRING) {

					if (mMain.hasGlobal(word.getWord())) {
						retour.addExpression(new LeekVariable(this, word, VariableType.GLOBAL));
					} else {
						retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
					}
				} else if (word.getType() == TokenType.PAR_LEFT) {
					var leftParenthesis = mTokens.eat(); // On avance le curseur pour bien être au début de l'expression

					var exp = readExpression();
					if (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {
						throw new LeekCompilerException(mTokens.get(), Error.CLOSING_PARENTHESIS_EXPECTED);
					}
					var rightParenthesis = mTokens.get();
					retour.addExpression(new LeekParenthesis(exp, leftParenthesis, rightParenthesis));

				} else if (word.getType() == TokenType.OPERATOR) {
					// Si c'est un opérateur (il doit forcément être unaire et de type préfix (! ))
					int operator = Operators.getOperator(word.getWord(), getVersion());
					if (operator == Operators.MINUS) operator = Operators.UNARY_MINUS;
					else if (operator == Operators.DECREMENT) operator = Operators.PRE_DECREMENT;
					else if (operator == Operators.INCREMENT) operator = Operators.PRE_INCREMENT;
					else if (operator == Operators.NON_NULL_ASSERTION) operator = Operators.NOT;

					if (Operators.isUnaryPrefix(operator)) {
						// Si oui on l'ajoute
						retour.addUnaryPrefix(operator, word);
					} else {
						addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.OPERATOR_UNEXPECTED));
					}
				} else {
					addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VALUE_EXPECTED));
				}
			}
			mTokens.skip();
		}
		// Avant de retourner, on valide l'expression
		Expression result = retour;
		if (retour.getOperator() == -1) {
			result = retour.getExpression1();
		}
		if (getVersion() == 1 && result instanceof LeekExpression) {
			var expr = (LeekExpression) result;
			if (expr.getOperator() == Operators.NOT && expr.getExpression2() == null) {
				// Un "not" tout seul est valide en LS 1.0
				result = new LeekVariable(this, expr.getOperatorToken(), VariableType.LOCAL);
			}
		}
		if (result == null) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.UNCOMPLETE_EXPRESSION));
			return new LeekNull(mTokens.eat());
		}
		try {
			result.validExpression(this, mMain);
		} catch (LeekExpressionException e) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, e.getError(), new String[] { e.getExpression() }));
			return new LeekNull(mTokens.eat());
		}
		return result;
	}

	private boolean wordEquals(Token word, String expected) {
		if (getVersion() <= 2) {
			return word.getWord().equalsIgnoreCase(expected);
		}
		return word.getWord().equals(expected);
	}

	private Expression readSet(Token openingToken) throws LeekCompilerException {
		var set = new LeekSet(openingToken);

		while (mTokens.hasMoreTokens() && (mTokens.get().getType() != TokenType.OPERATOR || !mTokens.get().getWord().equals(">"))) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			set.addValue(readExpression(true, true));

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
		}

		if (!mTokens.get().getWord().equals(">")) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.CLOSING_CHEVRON_EXPECTED));
		}

		set.setClosingToken(mTokens.get());

		return set;
	}

	private Expression readArrayOrMapOrInterval(Token openingBracket) throws LeekCompilerException {

		// Empty map `[:]`
		if (mTokens.get().getWord().equals(":")) {
			mTokens.skip();

			if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
				throw new LeekCompilerException(mTokens.get(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
			}

			if (version >= 4) {
				var container = new LeekMap(openingBracket);
				container.setClosingBracket(mTokens.get());
				return container;
			} else {
				var container = new LegacyLeekArray(openingBracket);
				container.mIsKeyVal = true;
				container.setClosingBracket(mTokens.get());
				return container;
			}
		}

		// Empty array `[]`
		if (mTokens.get().getType() == TokenType.BRACKET_RIGHT) {
			if (version >= 4) {
				var container = new LeekArray(openingBracket);
				container.setClosingBracket(mTokens.get());
				return container;
			} else {
				var container = new LegacyLeekArray(openingBracket);
				container.setClosingBracket(mTokens.get());
				return container;
			}
		}

		// Empty interval [..]
		if (mTokens.get().getType() == TokenType.DOT_DOT) {
			mTokens.skip();
			return readInterval(openingBracket, null);
		}

		var firstExpression = readExpression(true);
		if (mTokens.get().getWord().equals(":")) {
			mTokens.skip();
			if (version >= 4) {
				return readMap(openingBracket, firstExpression);
			} else {
				return readLegacyArray(openingBracket, firstExpression, true);
			}
		} else if (mTokens.get().getType() == TokenType.DOT_DOT) {
			mTokens.skip();
			return readInterval(openingBracket, firstExpression);
		} else {
			if (version >= 4) {
				return readArray(openingBracket, firstExpression);
			} else {
				return readLegacyArray(openingBracket, firstExpression, false);
			}
		}
	}

	private Expression readMap(Token openingBracket, Expression firstExpression) throws LeekCompilerException {
		var container = new LeekMap(openingBracket);

		var secondExpression = readExpression(true);
		container.addValue(this, firstExpression, secondExpression);

		if (mTokens.get().getType() == TokenType.VIRG) {
			mTokens.skip();
		}

		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			var key = readExpression(true);

			if (!mTokens.get().getWord().equals(":")) {
				throw new LeekCompilerException(mTokens.get(), Error.SIMPLE_ARRAY);
			}
			mTokens.skip();

			var value = readExpression(true);
			container.addValue(this, key, value);

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
		}

		container.setClosingBracket(mTokens.get());
		return container;
	}

	private Expression readArray(Token openingBracket, Expression firstExpression) throws LeekCompilerException {
		var container = new LeekArray(openingBracket);

		container.addValue(firstExpression);

		if (mTokens.get().getType() == TokenType.VIRG) {
			mTokens.skip();
		}

		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			var value = readExpression(true);
			container.addValue(value);

			if (mTokens.get().getWord().equals(":")) {
				throw new LeekCompilerException(mTokens.get(), Error.ASSOCIATIVE_ARRAY);
			}

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
		}

		container.setClosingBracket(mTokens.get());
		return container;
	}

	private Expression readLegacyArray(Token openingBracket, Expression firstExpression, boolean isKeyVal) throws LeekCompilerException {

		var container = new LegacyLeekArray(openingBracket);

		// Empty array `[:]`
		if (mTokens.get().getWord().equals(":")) {
			container.mIsKeyVal = true;
			container.type = Type.MAP;
			mTokens.skip();

			if (mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
				throw new LeekCompilerException(mTokens.get(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
			}
			container.setClosingBracket(mTokens.get());
			return container;
		}

		// Empty array `[]`
		if (mTokens.get().getType() == TokenType.BRACKET_RIGHT) {
			container.addValue(firstExpression);
			container.setClosingBracket(mTokens.get());
			return container;
		}

		if (isKeyVal) {
			var secondExpression = readExpression(true);
			container.addValue(this, firstExpression, secondExpression);
		} else {
			container.addValue(firstExpression);
		}

		if (mTokens.get().getType() == TokenType.VIRG) {
			mTokens.skip();
		}

		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.BRACKET_RIGHT) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			if (isKeyVal) {
				var key = readExpression(true);
				if (!mTokens.get().getWord().equals(":")) {
					throw new LeekCompilerException(mTokens.get(), Error.SIMPLE_ARRAY);
				}
				mTokens.skip();

				var value = readExpression(true);
				container.addValue(this, key, value);
			} else {
				var value = readExpression(true);
				container.addValue(value);

				if (mTokens.get().getWord().equals(":")) {
					throw new LeekCompilerException(mTokens.get(), Error.ASSOCIATIVE_ARRAY);
				}
			}

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
		}

		container.setClosingBracket(mTokens.get());
		return container;
	}

	private Expression readInterval(Token openingBracket, Expression fromExpression) throws LeekCompilerException {

		if (mTokens.get().getType() == TokenType.BRACKET_RIGHT || mTokens.get().getType() == TokenType.BRACKET_LEFT) {
			return new LeekInterval(openingBracket, fromExpression, null, mTokens.get());
		}

		// Although an interval is not comma separated, we still parse the second
		// expression as if we did. This is in order to be more consitent as the first
		// expression is parsed as if it was comma separated
		var toExpression = readExpression(true, false, true);

		var nextToken = mTokens.get();
		if (nextToken.getWord().equals(":")) {
			throw new LeekCompilerException(mTokens.get(), Error.ASSOCIATIVE_ARRAY);
		} else if (nextToken.getType() == TokenType.VIRG) {
			throw new LeekCompilerException(mTokens.get(), Error.SIMPLE_ARRAY);
		} else if (nextToken.getType() != TokenType.BRACKET_RIGHT && nextToken.getType() != TokenType.BRACKET_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		return new LeekInterval(openingBracket, fromExpression, toExpression, mTokens.get());
	}

	private LeekAnonymousFunction readAnonymousFunction() throws LeekCompilerException {
		var token = mTokens.eat();
		if (mTokens.get().getType() != TokenType.PAR_LEFT) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_FUNCTION));
		}
		mTokens.skip(); // Left parenthesis

		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		var previousFunction = mCurrentFunction;
		int initialLine = mLine;
		AIFile initialAI = mAI;
		AnonymousFunctionBlock block = new AnonymousFunctionBlock(mCurentBlock, mMain, token);
		// if (initialBlock.getDeclaringVariable() != null)
		// 	block.addVariable(new LeekVariable(initialBlock.getDeclaringVariable(), VariableType.LOCAL));
		mCurentBlock = block;
		setCurrentFunction(block);

		// Lecture des paramètres
		while (mTokens.hasMoreTokens() && mTokens.get().getType() != TokenType.PAR_RIGHT) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);
			boolean is_reference = false;
			if (mTokens.get().getType() == TokenType.OPERATOR && mTokens.get().getWord().equals("@")) {
				is_reference = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mTokens.skip();
			}
			if (mTokens.get().getType() != TokenType.STRING) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
			}

			var type = eatType(false, false);
			var parameter = mTokens.get();
			mTokens.skip();

			if (mTokens.get().getType() == TokenType.VIRG) {
				mTokens.skip();
			}
			// else if (mTokens.get().getType() != TokenType.PAR_RIGHT) {
			// 	type = parseType(parameter.getWord());
			// 	parameter = mTokens.get();
			// 	mTokens.skip();
			// 	if (mTokens.get().getType() == TokenType.VIRG) {
			// 		mTokens.skip();
			// 	}
			// }

			block.addParameter(this, parameter, is_reference, type == null ? Type.ANY : type.getType());
		}
		if (mTokens.eat().getType() != TokenType.PAR_RIGHT) {
			addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS));
		}

		// Type de retour
		if (mTokens.get().getType() == TokenType.ARROW) {
			mTokens.skip();

			var returnType = eatType(false, true);
			if (returnType == null) {
				addError(new AnalyzeError(mTokens.get(), AnalyzeErrorLevel.ERROR, Error.TYPE_EXPECTED));
			} else {
				block.setReturnType(returnType.getType());
			}
		}

		// Ouverture des accolades
		if (mTokens.eat().getType() != TokenType.ACCOLADE_LEFT) {
			throw new LeekCompilerException(mTokens.get(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		}

		// Lecture du corp de la fonction
		while (mTokens.hasMoreTokens()) {
			if (isInterrupted()) throw new LeekCompilerException(mTokens.get(), Error.AI_TIMEOUT);

			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mTokens.skip();
			} else mCurentBlock = mCurentBlock.endInstruction();
			if (!mTokens.hasMoreTokens()) break;

			// On regarde si on veut fermer la fonction anonyme
			if (mTokens.get().getType() == TokenType.ACCOLADE_RIGHT && mCurentBlock == block) {
				block.setEndToken(mTokens.get());
				break;// Fermeture de la fonction anonyme
			} else {
				compileWord();
			}
		}

		// Ajout de la fonction
		mMain.addAnonymousFunction(block);

		// On remet le bloc initial
		mCurentBlock = initialBlock;
		mLine = initialLine;
		mAI = initialAI;
		setCurrentFunction(previousFunction);

		return new LeekAnonymousFunction(block, token);
	}

	public boolean isKeyword(Token word) {
		for (var w : LexicalParser.reservedWords) {
			if (wordEquals(word, w)) return true;
		}
		return false;
	}

	public boolean isAvailable(Token word, boolean allFunctions) {
		if (getVersion() >= 3 && isKeyword(word)) return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasGlobal(word) ||
		// mMain.hasUserFunction(word, allFunctions) ||
		// mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasGlobal(word.getWord()) || mMain.hasUserFunction(word.getWord(), allFunctions) || mCurentBlock.hasVariable(word.getWord())) return false;
		return true;
	}

	public boolean isGlobalAvailable(Token word) {
		if (getVersion() <= 2) {
			if (word.getWord().equalsIgnoreCase("in") || word.getWord().equalsIgnoreCase("global") || word.getWord().equalsIgnoreCase("var") || word.getWord().equalsIgnoreCase("for") || word.getWord().equalsIgnoreCase("else") || word.getWord().equalsIgnoreCase("if") || word.getWord().equalsIgnoreCase("break") || word.getWord().equalsIgnoreCase("return") || word.getWord().equalsIgnoreCase("do") || word.getWord().equalsIgnoreCase("while") || word.getWord().equalsIgnoreCase("function") || word.getWord().equalsIgnoreCase("true") || word.getWord().equalsIgnoreCase("false") || word.getWord().equalsIgnoreCase("null")) return false;
		}
		if (getVersion() >= 3 && isKeyword(word)) return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasUserFunction(word,
		// false) || mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasUserFunction(word.getWord(), false) || mCurentBlock.hasVariable(word.getWord())) return false;
		return true;
	}

	public String getString() {
		return mMain.getCode();
	}

	public AbstractLeekBlock getCurrentBlock() {
		return mCurentBlock;
	}

	public AbstractLeekBlock getCurrentFunction() {
		return mCurrentFunction;
	}

	public void addError(AnalyzeError analyzeError) throws LeekCompilerException {
		this.mAI.getErrors().add(analyzeError);
		if (this.mAI.getErrors().size() > 10000) {
			throw new LeekCompilerException(mTokens.getEndOfFileToken(), Error.TOO_MUCH_ERRORS);
		}
	}

	public void setCurrentBlock(AbstractLeekBlock block) {
		mCurentBlock = block;
	}

	public LexicalParserTokenStream getTokenStream() {
		return mTokens;
	}

	public MainLeekBlock getMainBlock() {
		return mMain;
	}

	public int getVersion() {
		return this.version;
	}

	public ClassDeclarationInstruction getCurrentClass() {
		return mCurrentClass;
	}

	public void setCurrentClass(ClassDeclarationInstruction clazz) {
		this.mCurrentClass = clazz;
	}

	public void setCurrentFunction(AbstractLeekBlock block) {
		// System.out.println("setCurrentFunction " + block);
		this.mCurrentFunction = block;
	}

	public AIFile getAI() {
		return mAI;
	}

	public void setMainBlock(MainLeekBlock main) {
		this.mMain = main;
		mCurentBlock = main;
		mCurrentFunction = main;
	}

	public boolean isInConstructor() {
		if (mCurrentClass != null) {
			return mCurentBlock.isInConstructor();
		}
		return false;
	}

	public String getCurrentClassVariable() {
		if (mCurrentClass != null) {
			return "u_" + mCurrentClass.getName();
		}
		return null;
	}

	public Options getOptions() {
		return options;
	}
}
