package leekscript.compiler;

import java.util.HashSet;

import leekscript.common.AccessLevel;
import leekscript.common.Error;
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
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekFunctionCall;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekObject;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.expression.LeekVariable.VariableType;
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
	private final WordParser mCompiler;
	private int mLine;
	private AIFile<?> mAI = null;
	private final int version;

	public WordCompiler(WordParser cmp, AIFile<?> ai, int version) {
		mCompiler = cmp;
		mAI = ai;
		this.version = version;
	}

	public void readCode() throws LeekCompilerException {
		try {
			mCompiler.compile(this);
			// Recherche des fonctions utilisateur
			while (mCompiler.haveWords()) {
				if (mCompiler.token().getType() == WordParser.T_STRING && mCompiler.token().getWord().equals("global")) {
					mCompiler.skipToken();
					var global = mCompiler.eatToken();
					// System.out.println("global = " + global.getWord() + " " + global.getLine());
					if (!isGlobalAvailable(global) || mMain.hasDeclaredGlobal(global.getWord())) {
						addError(new AnalyzeError(global, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
					} else {
						mMain.addGlobal(global.getWord());
					}
					if (mCompiler.token().getWord().equals("=")) {
						mCompiler.skipToken();
						readExpression();
					}
					while (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_VIRG) {
						mCompiler.skipToken();
						global = mCompiler.eatToken();
						if (!isGlobalAvailable(global) || mMain.hasDeclaredGlobal(global.getWord())) {
							addError(new AnalyzeError(global, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
						} else {
							mMain.addGlobal(global.getWord());
						}
						if (mCompiler.token().getWord().equals("=")) {
							mCompiler.skipToken();
							readExpression();
						}
					}
				} else if (mCompiler.token().getWord().equals("function")) {
					mCompiler.skipToken();
					var funcName = mCompiler.eatToken();
					if (funcName.getWord().equals("(")) {
						continue;
					}
					if (!isAvailable(funcName, false)) {
						throw new LeekCompilerException(mCompiler.token(), Error.FUNCTION_NAME_UNAVAILABLE);
					}
					if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
						addError(new AnalyzeError(mCompiler.lastToken(), AnalyzeErrorLevel.ERROR, Error.OPENING_PARENTHESIS_EXPECTED));
					}
					int param_count = 0;
					var parameters = new HashSet<String>();
					while (mCompiler.haveWords() && mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
						if (mCompiler.token().getType() == WordParser.T_OPERATOR && mCompiler.token().getWord().equals("@")) {
							mCompiler.skipToken();
						}
						if (mCompiler.token().getType() != WordParser.T_STRING) {
							addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
						}
						var parameter = mCompiler.eatToken();
						// if (parameters.contains(parameter.getWord())) {
						// 	throw new LeekCompilerException(parameter, Error.PARAMETER_NAME_UNAVAILABLE);
						// }
						parameters.add(parameter.getWord());
						param_count++;

						if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_VIRG) {
							mCompiler.skipToken();
						}
					}
					if (mCompiler.haveWords() && mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}

					mMain.addFunctionDeclaration(funcName.getWord(), param_count);
				} else {
					mCompiler.skipToken();
				}
			}
			mCompiler.reset();
			// Vraie compilation
			while (mCompiler.haveWords()) {

				// On vérifie les instructions en cours

				if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
					DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
					mCurentBlock = mCurentBlock.endInstruction();
					dowhileendBlock(do_block);
					mCompiler.skipToken();
				} else
					mCurentBlock = mCurentBlock.endInstruction();
				if (!mCompiler.haveWords())
					break;

				// Puis on lit l'instruction
				compileWord();
			}
			while (mCurentBlock.getParent() != null && !mCurentBlock.hasAccolade()) {
				if (mCurentBlock instanceof DoWhileBlock) {
					DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
					mCurentBlock = mCurentBlock.endInstruction();
					dowhileendBlock(do_block);
					mCompiler.skipToken();
				} else {
					if (mCurentBlock.endInstruction() == mCurentBlock) {
						throw new LeekCompilerException(mCompiler.lastToken(), Error.NO_BLOC_TO_CLOSE);
					}
					mCurentBlock = mCurentBlock.endInstruction();
				}
			}
			if (!mMain.equals(mCurentBlock))
				throw new LeekCompilerException(mCompiler.lastToken(), Error.OPEN_BLOC_REMAINING);

		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace(System.out);
			throw new LeekCompilerException(mCompiler.endToken(), Error.END_OF_SCRIPT_UNEXPECTED);
		}
	}

	public void analyze() {
		// Analyse sémantique
		mCurentBlock = mMain;
		setCurrentFunction(mMain);
		mMain.preAnalyze(this);
		mMain.analyze(this);
	}

	private void compileWord() throws LeekCompilerException {
		mLine = mCompiler.token().getLocation().getStartLine();
		mMain.addInstruction();
		Token word = mCompiler.token();
		if (word.getType() == WordParser.T_END_INSTRUCTION) {
			// mCurentBlock.addInstruction(this, new BlankInstruction());
			mCurentBlock.setFull(true);
			mCompiler.skipToken();
			return;
		} else if (word.getType() == WordParser.T_ACCOLADE_RIGHT) {
			// Fermeture de bloc
			if (!mCurentBlock.hasAccolade() || mCurentBlock.getParent() == null) {
				// throw new LeekCompilerException(word, Error.NO_BLOC_TO_CLOSE);
				addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.NO_BLOC_TO_CLOSE));
			} else {
				if (mCurentBlock instanceof DoWhileBlock) {
					DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
					mCurentBlock.checkEndBlock();
					mCurentBlock = mCurentBlock.getParent();
					mCompiler.skipToken();
					dowhileendBlock(do_block);
				} else {
					mCurentBlock.checkEndBlock();
					mCurentBlock = mCurentBlock.getParent();
				}
			}
			mCompiler.skipToken();
			return;
		} else if (word.getType() == WordParser.T_STRING) {
			if (word.getWord().equals("var")) {
				// Déclaration de variable
				mCompiler.skipToken();
				variableDeclaration();
				return;
			} else if (word.getWord().equals("global")) {
				// Déclaration de variable
				globalDeclaration();
				return;
			} else if (version >= 2 && getCurrentBlock() instanceof MainLeekBlock && word.getWord().equals("class")) {
				// Déclaration de classe
				mCompiler.skipToken();
				classDeclaration();
				return;
			} else if (word.getWord().equals("if")) {
				ifBlock();
				return;
			} else if (word.getWord().equals("else")) {
				elseBlock();
				return;
			} else if (word.getWord().equals("while")) {
				whileBlock();
				return;
			} else if (word.getWord().equals("do")) {
				doWhileBlock();
				return;
			} else if (word.getWord().equals("include")) {
				mCompiler.skipToken();
				includeBlock();
				return;
			} else if (word.getWord().equals("for")) {
				forBlock();
				return;
			} else if (word.getWord().equals("break")) {
				if (!mCurentBlock.isBreakable()) {
					addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.ERROR, Error.BREAK_OUT_OF_LOOP));
				}
				mCompiler.skipToken();
				if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
					mCompiler.skipToken();
				}
				mCurentBlock.addInstruction(this, new LeekBreakInstruction(word));

				return;
			} else if (word.getWord().equals("continue")) {
				if (!mCurentBlock.isBreakable()) {
					addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.ERROR, Error.CONTINUE_OUT_OF_LOOP));
				}
				var token = mCompiler.eatToken();
				if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
					mCompiler.skipToken();
				}
				mCurentBlock.addInstruction(this, new LeekContinueInstruction(token));
				return;
			} else if (word.getWord().equals("return")) {
				var token = mCompiler.eatToken();
				Expression exp = null;
				if (mCompiler.token().getType() != WordParser.T_END_INSTRUCTION) {
					exp = readExpression();
				}
				if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
					mCompiler.skipToken();
				}
				mCurentBlock.addInstruction(this, new LeekReturnInstruction(token, exp));
				return;
			} else if (word.getWord().equals("function")) {
				mCompiler.skipToken();
				functionBlock();
				return;
			}

		}
		var exp = readExpression();
		if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipToken();
		}
		mCurentBlock.addInstruction(this, new LeekExpressionInstruction(exp));
	}

	public void writeJava(String className, JavaWriter writer, String AIClass) {
		mMain.writeJavaCode(writer, className, AIClass);
	}

	private void includeBlock() throws LeekCompilerException {
		// On vérifie qu'on est dans le bloc principal
		if (!mCurentBlock.equals(mMain))
			throw new LeekCompilerException(mCompiler.lastToken(), Error.INCLUDE_ONLY_IN_MAIN_BLOCK);
		// On récupere l'ia
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT)
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);

		if (mCompiler.token().getType() != WordParser.T_VAR_STRING)
			throw new LeekCompilerException(mCompiler.token(), Error.AI_NAME_EXPECTED);
		String iaName = mCompiler.eatToken().getWord();
		iaName = iaName.substring(1, iaName.length() - 1);
		if (!mMain.includeAI(this, iaName)) {
			addError(new AnalyzeError(mCompiler.lastToken(), AnalyzeErrorLevel.ERROR, Error.AI_NOT_EXISTING, new String[] { iaName }));
		}

		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT)
			throw new LeekCompilerException(mCompiler.lastToken(), Error.CLOSING_PARENTHESIS_EXPECTED);
	}

	private void functionBlock() throws LeekCompilerException {
		// Déclaration de fonction utilisateur
		if (!mCurentBlock.equals(mMain))
			throw new LeekCompilerException(mCompiler.lastToken(), Error.FUNCTION_ONLY_IN_MAIN_BLOCK);
		// Récupération du nom de la fonction
		if (mCompiler.token().getType() != WordParser.T_STRING)
			throw new LeekCompilerException(mCompiler.token(), Error.FUNCTION_NAME_EXPECTED);
		Token funcName = mCompiler.eatToken();
		if (!isAvailable(funcName, false))
			throw new LeekCompilerException(mCompiler.token(), Error.FUNCTION_NAME_UNAVAILABLE);

		if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);
		}

		var previousFunction = mCurrentFunction;
		FunctionBlock block = new FunctionBlock(mCurentBlock, mMain, funcName);
		mCurentBlock = block;
		setCurrentFunction(block);
		while (mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
			boolean is_reference = false;
			if (mCompiler.token().getType() == WordParser.T_OPERATOR && mCompiler.token().getWord().equals("@")) {
				is_reference = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mCompiler.skipToken();
			}
			if (mCompiler.token().getType() != WordParser.T_STRING) {
				addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
			}

			var parameter = mCompiler.token();
			mCompiler.skipToken();
			Type type = Type.ANY;

			if (mCompiler.token().getType() == WordParser.T_VIRG) {
				mCompiler.skipToken();
			}
			// else if (mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
				// type = parseType(parameter.getWord());
				// parameter = mCompiler.token();
			// 	mCompiler.skipToken();
			// 	if (mCompiler.token().getType() == WordParser.T_VIRG) {
			// 		mCompiler.skipToken();
			// 	}
			// }

			block.addParameter(this, parameter, is_reference, type);
		}
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		// On regarde s'il y a des accolades
		if (mCompiler.eatToken().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		mMain.addFunction(block);
		setCurrentFunction(previousFunction);
	}

	// private Type parseType(String word) {
	// 	if (word.equals("void")) return Type.VOID;
	// 	if (word.equals("bool")) return Type.BOOL;
	// 	if (word.equals("any")) return Type.ANY;
	// 	if (word.equals("int")) return Type.INT;
	// 	if (word.equals("real")) return Type.REAL;
	// 	if (word.equals("number")) return Type.NUMBER;
	// 	if (word.equals("string")) return Type.STRING;
	// 	if (word.equals("array")) return Type.ARRAY;
	// 	if (word.equals("map")) return Type.MAP;
	// 	if (word.equals("function")) return Type.FUNCTION;
	// 	if (word.equals("object")) return Type.OBJECT;
	// 	if (word.equals("class")) return Type.CLASS;
	// 	if (word.equals("null")) return Type.NULL;
	// 	return Type.ANY;
	// }

	private void forBlock() throws LeekCompilerException {
		var token = mCompiler.eatToken();
		// Bloc de type for(i=0;i<5;i++) ou encore for(element in tableau)
		// On peut déclarer une variable pendant l'instruction d'initialisation
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);
		}

		boolean isDeclaration = false;
		AbstractLeekBlock forBlock = null;

		// Là on doit déterminer si y'a déclaration de variable
		if (mCompiler.token().getWord().equals("var")) {// Il y a déclaration
			isDeclaration = true;
			mCompiler.skipToken();
		}
		// Référence ?
		boolean reference1 = false;
		if (mCompiler.token().getWord().equals("@")) {
			reference1 = true;
			if (getVersion() >= 2) {
				addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
			}
			mCompiler.skipToken();
		}
		// On récupère ensuite le nom de la variable
		if (mCompiler.token().getType() != WordParser.T_STRING)
			throw new LeekCompilerException(mCompiler.token(), Error.VARIABLE_NAME_EXPECTED);
		Token varName = mCompiler.eatToken();

		// Maintenant on va savoir si on a affaire à un for (i in array) ou à un for(i=0;i<...
		if (mCompiler.token().getWord().equals(":")) { // C'est un for (key:value in array)
			mCompiler.skipToken();
			boolean isValueDeclaration = false;
			if (mCompiler.token().getWord().equals("var")) { // Il y a déclaration de la valeur
				isValueDeclaration = true;
				mCompiler.skipToken();
			}
			// Référence ?
			boolean reference2 = false;
			if (mCompiler.token().getWord().equals("@")) {
				reference2 = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mCompiler.skipToken();
			}
			// On récupère ensuite le nom de la variable accueillant la valeur
			if (mCompiler.token().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.token(), Error.VARIABLE_NAME_EXPECTED);
			Token valueVarName = mCompiler.eatToken();

			if (!mCompiler.eatToken().getWord().equals("in"))
				throw new LeekCompilerException(mCompiler.token(), Error.KEYWORD_IN_EXPECTED);

			// On déclare notre bloc foreach et on entre dedans
			ForeachKeyBlock block = new ForeachKeyBlock(mCurentBlock, mMain, isDeclaration, isValueDeclaration, token, reference1, reference2);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			var array = readExpression();
			block.setArray(array);
			block.setKeyIterator(this, varName, isDeclaration);
			block.setValueIterator(this, valueVarName, isValueDeclaration);

			forBlock = block;
		} else if (mCompiler.token().getWord().equals("in")) { // C'est un for (i in array)
			mCompiler.skipToken();

			ForeachBlock block = new ForeachBlock(mCurentBlock, mMain, isDeclaration, token, reference1);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			var array = readExpression();
			block.setArray(array);
			block.setIterator(this, varName);

			forBlock = block;
		} else if (mCompiler.token().getWord().equals("=")) { // C'est un for (i=0;i<1;i++)
			mCompiler.skipToken();

			ForBlock block = new ForBlock(mCurentBlock, mMain, token);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On récupère la valeur de base du compteur
			var initValue = readExpression();
			if (mCompiler.eatToken().getType() != WordParser.T_END_INSTRUCTION) {
				// errors.add(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.ERROR, Error.END_OF_INSTRUCTION_EXPECTED));
				throw new LeekCompilerException(mCompiler.lastToken(), Error.END_OF_INSTRUCTION_EXPECTED);
				// return;
			}
			var condition = readExpression();
			if (mCompiler.eatToken().getType() != WordParser.T_END_INSTRUCTION) {
				// errors.add(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.ERROR, Error.END_OF_INSTRUCTION_EXPECTED));
				throw new LeekCompilerException(mCompiler.lastToken(), Error.END_OF_INSTRUCTION_EXPECTED);
				// return;
			}
			// if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			// 	mCompiler.skipWord();
			// }
			var incrementation = readExpression();

			// Attention si l'incrémentation n'est pas une expression Java fait la gueule !
			if (incrementation != null && (incrementation instanceof LeekVariable ||
					(incrementation instanceof LeekExpression && ((LeekExpression) incrementation).getOperator() == -1))) {
				throw new LeekCompilerException(mCompiler.lastToken(), Error.UNCOMPLETE_EXPRESSION);
			}

			block.setInitialisation(this, varName, initValue, isDeclaration, block.hasGlobal(varName.getWord()));
			block.setCondition(condition);
			block.setIncrementation(incrementation);

			forBlock = block;
		} else
			throw new LeekCompilerException(mCompiler.token(), Error.KEYWORD_UNEXPECTED);

		// On vérifie la parenthèse fermante
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		// On regarde s'il y a des accolades
		if (mCompiler.token().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipToken();
		} else
			forBlock.noAccolade();
	}

	private void whileBlock() throws LeekCompilerException {
		var token = mCompiler.eatToken();
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		var exp = readExpression();
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		WhileBlock bloc = new WhileBlock(mCurentBlock, mMain, token);
		bloc.setCondition(exp);
		if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipToken();
		} else if (mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipToken();
			bloc.addInstruction(this, new BlankInstruction());
			bloc.noAccolade();
		} else {
			bloc.noAccolade();
		}
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void doWhileBlock() throws LeekCompilerException {
		var token = mCompiler.eatToken();
		DoWhileBlock bloc = new DoWhileBlock(mCurentBlock, mMain, token);
		if (mCompiler.token().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipToken();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void dowhileendBlock(DoWhileBlock bloc) throws LeekCompilerException {
		if (!mCompiler.eatToken().getWord().equals("while"))
			throw new LeekCompilerException(mCompiler.lastToken(), Error.WHILE_EXPECTED_AFTER_DO);
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		bloc.setCondition(readExpression());
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		// if (mCompiler.getWord().getType() != WordParser.T_END_INSTRUCTION)
		// 	throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
	}

	private void elseBlock() throws LeekCompilerException {
		var token = mCompiler.eatToken();
		// On vérifie qu'on est bien associé à un bloc conditionnel
		ConditionalBloc last = mCurentBlock.getLastOpenedConditionalBlock();
		if (last == null || last.getCondition() == null) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.NO_IF_BLOCK);
		}
		ConditionalBloc bloc = new ConditionalBloc(mCurentBlock, mMain, token);
		bloc.setParentCondition(last);
		if (mCompiler.token().getWord().equals("if")) {
			// On veut un elseif
			mCompiler.skipToken();
			if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
				throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);
			}
			var exp = readExpression();
			if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
				throw new LeekCompilerException(mCompiler.lastToken(), Error.CLOSING_PARENTHESIS_EXPECTED);
			}
			bloc.setCondition(exp);
		}

		if (mCompiler.token().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipToken();
		} else
			bloc.noAccolade();
		last.getParent().addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void ifBlock() throws LeekCompilerException {
		var token = mCompiler.eatToken();
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		var exp = readExpression();
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastToken(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		var bloc = new ConditionalBloc(mCurentBlock, mMain, token);
		bloc.setCondition(exp);
		if (mCompiler.token().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipToken();
		} else if (mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipToken();
			bloc.addInstruction(this, new BlankInstruction());
			bloc.noAccolade();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void globalDeclaration() throws LeekCompilerException {
		// Il y a au moins une premiere variable
		Token token = mCompiler.eatToken();
		Token word = mCompiler.eatToken();
		if (!(mCurentBlock instanceof MainLeekBlock)) {
			throw new LeekCompilerException(word, Error.GLOBAL_ONLY_IN_MAIN_BLOCK);
		}
		if (word.getType() != WordParser.T_STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED_AFTER_GLOBAL);
		}
		var variable = new LeekGlobalDeclarationInstruction(token, word);
		// On regarde si une valeur est assignée
		if (mCompiler.token().getWord().equals("=")) {
			mCompiler.skipToken();
			// Si oui on récupère la valeur en question
			variable.setValue(readExpression());
		}
		// On ajoute la variable
		mMain.addGlobalDeclaration(variable);
		mCurentBlock.addInstruction(this, variable);
		while (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_VIRG) {
			// On regarde si y'en a d'autres
			mCompiler.skipToken();// On passe la virgule
			word = mCompiler.eatToken();
			if (word.getType() != WordParser.T_STRING)
				throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
			variable = new LeekGlobalDeclarationInstruction(token, word);
			// On regarde si une valeur est assignée
			if (mCompiler.token().getWord().equals("=")) {
				mCompiler.skipToken();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression());
			}
			// On ajoute la variable
			mMain.addGlobalDeclaration(variable);
			mCurentBlock.addInstruction(this, variable);
		}
		// word = mCompiler.readWord();
		// if (word.getType() != WordParser.T_END_INSTRUCTION)
			// throw new LeekCompilerException(word, Error.END_OF_INSTRUCTION_EXPECTED);
		if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_END_INSTRUCTION)
			mCompiler.skipToken();
	}

	private void variableDeclaration() throws LeekCompilerException {
		// Il y a au moins une premiere variable
		Token word = mCompiler.eatToken();
		if (word.getType() != WordParser.T_STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
		}
		if (getVersion() >= 3 && isKeyword(word)) {
			addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
		}
		LeekVariableDeclarationInstruction variable = new LeekVariableDeclarationInstruction(this, word, getCurrentFunction());
		// On regarde si une valeur est assignée
		if (mCompiler.haveWords() && mCompiler.token().getWord().equals("=")) {
			mCompiler.skipToken();
			// Si oui on récupère la valeur en question
			variable.setValue(readExpression());
		}
		mCurentBlock.addInstruction(this, variable);
		while (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_VIRG) {
			// On regarde si y'en a d'autres
			mCompiler.skipToken();// On passe la virgule
			word = mCompiler.eatToken();
			if (word.getType() != WordParser.T_STRING)
				throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
			if (getVersion() >= 3 && isKeyword(word)) {
				addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			}
			variable = new LeekVariableDeclarationInstruction(this, word, getCurrentFunction());
			// On regarde si une valeur est assignée
			if (mCompiler.token().getWord().equals("=")) {
				mCompiler.skipToken();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression());
			}
			// On ajoute la variable
			mCurentBlock.addInstruction(this, variable);
		}
		if (mCompiler.haveWords() && mCompiler.token().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipToken();
		}
	}

	public void classDeclaration() throws LeekCompilerException {
		// Read class name
		Token word = mCompiler.eatToken();
		if (word.getType() != WordParser.T_STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
		}
		if (mMain.hasUserClass(word.getWord())) {
			throw new LeekCompilerException(word, Error.VARIABLE_NAME_UNAVAILABLE);
		}
		if (getVersion() >= 3 && isKeyword(word)) {
			addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { word.getWord() }));
		}
		ClassDeclarationInstruction classDeclaration = new ClassDeclarationInstruction(word, mLine, mAI, false, getMainBlock());
		mMain.addClass(classDeclaration);
		mCurrentClass = classDeclaration;

		if (mCompiler.token().getWord().equals("extends")) {
			mCompiler.skipToken();
			Token parent = mCompiler.eatToken();
			classDeclaration.setParent(parent);
		}
		if (mCompiler.token().getType() != WordParser.T_ACCOLADE_LEFT) {
			throw new LeekCompilerException(mCompiler.token(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		}
		mCompiler.skipToken();

		while (mCompiler.token().getType() != WordParser.T_ACCOLADE_RIGHT) {
			word = mCompiler.token();
			switch (word.getWord()) {
				case "public":
				case "private":
				case "protected":
				{
					AccessLevel level = AccessLevel.fromString(word.getWord());
					mCompiler.skipToken();
					classAccessLevelMember(classDeclaration, level);
					break;
				}
				case "static": {
					mCompiler.skipToken();
					classStaticMember(classDeclaration, AccessLevel.PUBLIC);
					break;
				}
				case "final": {
					mCompiler.skipToken();
					endClassMember(classDeclaration, AccessLevel.PUBLIC, false, true);
					break;
				}
				case "constructor": {
					mCompiler.skipToken();
					classConstructor(classDeclaration, AccessLevel.PUBLIC, word);
					break;
				}
				default: {
					endClassMember(classDeclaration, AccessLevel.PUBLIC, false, false);
					// if (word.getType() == WordParser.T_STRING) {
					// 	mCompiler.back();
					// 	classAccessLevelMember(classDeclaration, AccessLevel.PUBLIC);
					// } else {
					// 	throw new LeekCompilerException(word, Error.KEYWORD_UNEXPECTED);
					// }
				}
			}
		}
		if (mCompiler.eatToken().getType() != WordParser.T_ACCOLADE_RIGHT) {
			throw new LeekCompilerException(word, Error.END_OF_CLASS_EXPECTED);
		}
		mCurrentClass = null;
	}

	public void classStaticMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		Token token = mCompiler.token();
		switch (token.getWord()) {
			case "final":
				mCompiler.skipToken();
				endClassMember(classDeclaration, accessLevel, true, true);
				return;
		}
		endClassMember(classDeclaration, accessLevel, true, false);
	}

	public void classAccessLevelMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		Token token = mCompiler.token();
		switch (token.getWord()) {
			case "constructor":
				mCompiler.skipToken();
				classConstructor(classDeclaration, accessLevel, token);
				return;
			case "static":
				mCompiler.skipToken();
				classStaticMember(classDeclaration, accessLevel);
				return;
			case "final":
				mCompiler.skipToken();
				endClassMember(classDeclaration, accessLevel, false, true);
				return;
		}
		endClassMember(classDeclaration, accessLevel, false, false);
	}

	public void endClassMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel, boolean isStatic, boolean isFinal) throws LeekCompilerException {
		Token name = mCompiler.eatToken();

		if (name.getWord().equals("super") || name.getWord().equals("class")) {
			addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { name.getWord() }));
		} else if (getVersion() >= 3 && isKeyword(name)) {
			addError(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { name.getWord() }));
		}

		// Field
		Expression expr = null;
		if (mCompiler.token().getType() == WordParser.T_OPERATOR && mCompiler.token().getWord().equals("=")) {
			mCompiler.skipToken();
			expr = readExpression();
		} else if (mCompiler.token().getType() == WordParser.T_PAR_LEFT) {
			// Méthode
			ClassMethodBlock method = classMethod(classDeclaration, name, false, isStatic);
			if (isStatic) {
				classDeclaration.addStaticMethod(this, name, method, accessLevel);
			} else {
				classDeclaration.addMethod(this, name, method, accessLevel);
			}
			if (mCompiler.token().getType() == WordParser.T_END_INSTRUCTION)
				mCompiler.skipToken();
			return;
		}

		if (isStatic) {
			classDeclaration.addStaticField(this, name, expr, accessLevel, isFinal);
		} else {
			classDeclaration.addField(this, name, expr, accessLevel, isFinal);
		}

		if (mCompiler.token().getType() == WordParser.T_END_INSTRUCTION)
			mCompiler.skipToken();
	}

	public void classConstructor(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel, Token token) throws LeekCompilerException {
		ClassMethodBlock constructor = classMethod(classDeclaration, token, true, false);
		classDeclaration.addConstructor(constructor, accessLevel);
	}

	public ClassMethodBlock classMethod(ClassDeclarationInstruction classDeclaration, Token token, boolean isConstructor, boolean isStatic) throws LeekCompilerException {

		ClassMethodBlock method = new ClassMethodBlock(classDeclaration, isConstructor, isStatic, mCurentBlock, mMain, token);

		Token word = mCompiler.eatToken();
		if (word.getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(word, Error.OPENING_PARENTHESIS_EXPECTED);
		}
		int param_count = 0;
		while (mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
			if (mCompiler.token().getType() == WordParser.T_OPERATOR && mCompiler.token().getWord().equals("@")) {
				addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				mCompiler.skipToken();
			}
			if (mCompiler.token().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.token(), Error.PARAMETER_NAME_EXPECTED);
			method.addParameter(this, mCompiler.token());
			mCompiler.skipToken();
			param_count++;
			if (mCompiler.token().getType() == WordParser.T_VIRG)
				mCompiler.skipToken();
		}
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}
		if (classDeclaration.hasMethod(token.getWord(), param_count)) {
			throw new LeekCompilerException(mCompiler.token(), Error.CONSTRUCTOR_ALREADY_EXISTS);
		}

		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		int initialLine = mLine;
		AIFile<?> initialAI = mAI;
		mCurentBlock = method;

		// Ouverture des accolades
		if (mCompiler.eatToken().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_CURLY_BRACKET_EXPECTED);

		// Lecture du corps de la fonction
		while (mCompiler.haveWords()) {
			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mCompiler.skipToken();
			} else
				mCurentBlock = mCurentBlock.endInstruction();
			if (!mCompiler.haveWords())
				break;

			// On regarde si on veut fermer la fonction anonyme
			if (mCompiler.token().getType() == WordParser.T_ACCOLADE_RIGHT && mCurentBlock == method) {
				mCompiler.skipToken();
				break; // Fermeture de la fonction anonyme
			} else
				compileWord();
		}
		// On remet le bloc initial
		mCurentBlock = initialBlock;
		mLine = initialLine;
		mAI = initialAI;
		return method;
	}

	public Expression readExpression() throws LeekCompilerException {
		var retour = new LeekExpression();
		while (mCompiler.haveWords()) {
			Token word = mCompiler.token();
			if (word.getType() == WordParser.T_PAR_RIGHT || word.getType() == WordParser.T_ACCOLADE_RIGHT || word.getType() == WordParser.T_END_INSTRUCTION) {
				break;
			}
			if (retour.needOperator()) {
				// Si on attend un opérateur mais qu'il vient pas

				if (word.getType() == WordParser.T_BRACKET_LEFT) {
					var bracket = mCompiler.eatToken(); // On avance le curseur pour être au début de l'expression
					Token colon = null;
					Token colon2 = null;
					Expression start = null;
					Expression end = null;
					Expression stride = null;

					if (getVersion() >= 4 && mCompiler.token().getWord().equals(":")) {
						colon = mCompiler.eatToken();
						if (getVersion() >= 4 && mCompiler.token().getWord().equals(":")) {
							colon2 = mCompiler.eatToken();
							if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
								stride = readExpression();
							}
						} else if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
							end = readExpression();
							if (getVersion() >= 4 && mCompiler.token().getWord().equals(":")) {
								colon2 = mCompiler.eatToken();
								if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
									stride = readExpression();
								}
							}
						}
					} else {
						start = readExpression();
						if (getVersion() >= 4 && mCompiler.token().getWord().equals(":")) {
							colon = mCompiler.eatToken();
							if (getVersion() >= 4 && mCompiler.token().getWord().equals(":")) {
								colon2 = mCompiler.eatToken();
								if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
									stride = readExpression();
								}
							} else if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
								end = readExpression();
								if (getVersion() >= 4 && mCompiler.token().getWord().equals(":")) {
									colon2 = mCompiler.eatToken();
									if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
										stride = readExpression();
									}
								}
							}
						}
					}

					if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
						throw new LeekCompilerException(mCompiler.token(), Error.CLOSING_SQUARE_BRACKET_EXPECTED);
					}
					retour.addBracket(bracket, start, colon, end, colon2, stride, mCompiler.token());

				} else if (word.getType() == WordParser.T_PAR_LEFT) {

					LeekFunctionCall function = new LeekFunctionCall(word);
					mCompiler.skipToken(); // On avance le curseur pour être au début de l'expression

					while (mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
						function.addParameter(readExpression());
						if (mCompiler.token().getType() == WordParser.T_VIRG)
							mCompiler.skipToken();
					}
					if (mCompiler.haveWords() && mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					function.setClosingParenthesis(mCompiler.token());
					retour.addFunction(function);

				} else if (word.getType() == WordParser.T_DOT) {
					// Object access
					var dot = mCompiler.eatToken();
					var name = mCompiler.token();
					retour.addObjectAccess(dot, name);

				} else if (word.getType() == WordParser.T_OPERATOR) {
					int operator = Operators.getOperator(word.getWord(), getVersion());

					// Là c'est soit un opérateur (+ - ...) soit un suffix
					// unaire (++ -- ) sinon on sort de l'expression
					if (Operators.isUnaryPrefix(operator))
						break;
					if (operator == Operators.DOUBLE_POINT && !retour.hasTernaire())
						break;

					if (Operators.isUnarySuffix(operator))
						retour.addUnarySuffix(operator, word);
					else
						retour.addOperator(operator, word);
				} else if (word.getType() == WordParser.T_STRING) {
					if (word.getWord().equals("is")) {
						mCompiler.skipToken();
						word = mCompiler.token();
						if (word.getWord().equals("not")) {
							Token token = mCompiler.eatToken();
							retour.addOperator(Operators.NOTEQUALS, token);
						} else {
							retour.addOperator(Operators.EQUALS, word);
						}
						continue;
					}
					break;
				} else
					break;
			} else {
				if (word.getType() == WordParser.T_NUMBER) {
					var s = word.getWord();
					if (s.contains("__")) {
						addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.MULTIPLE_NUMERIC_SEPARATORS));
					}
					try {
						var radix = s.startsWith("0x") ? 16 : s.startsWith("0b") ? 2 : 10;
						s = word.getWord().replace("_", "");
						if (radix != 10) s = s.substring(2);
						retour.addExpression(new LeekNumber(word, 0, Long.parseLong(s, radix), Type.INT));
					} catch (NumberFormatException e) {
						s = word.getWord().replace("_", "");
						try {
							retour.addExpression(new LeekNumber(word, Double.parseDouble(s), 0, Type.REAL));
						} catch (NumberFormatException e2) {
							addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.INVALID_NUMBER));
							retour.addExpression(new LeekNumber(word, 0, 0, Type.INT));
						}
					}
				} else if (word.getType() == WordParser.T_VAR_STRING) {

					retour.addExpression(new LeekString(word, word.getWord()));

				} else if (word.getType() == WordParser.T_BRACKET_LEFT) {
					// Déclaration d'un tableau
					var token = mCompiler.eatToken();
					var array = new LeekArray(token);

					if (mCompiler.token().getWord().equals(":")) {
						// [:] map vide
						array.mIsKeyVal = true;
						array.type = Type.MAP;
						mCompiler.skipToken();
					} else {

						int type = 0;// 0 => A déterminer, 1 => Simple, 2 => Clé:valeur
						while (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
							var exp = readExpression();
							if (mCompiler.token().getWord().equals(":")) {
								if (type == 0)
									type = 2;
								else if (type == 1)
									throw new LeekCompilerException(mCompiler.token(), Error.SIMPLE_ARRAY);
								var colon = mCompiler.token();
								mCompiler.skipToken();
								var value = readExpression();
								array.addValue(this, exp, colon, value);
							} else {
								if (type == 0)
									type = 1;
								else if (type == 2)
									throw new LeekCompilerException(mCompiler.token(), Error.ASSOCIATIVE_ARRAY);
								array.addValue(exp);
							}
							if (mCompiler.token().getType() == WordParser.T_VIRG)
								mCompiler.skipToken();
						}
					}
					if (mCompiler.token().getType() != WordParser.T_BRACKET_RIGHT) {
						throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					array.setClosingBracket(mCompiler.token());
					retour.addExpression(array);

				} else if (getVersion() >= 2 && word.getType() == WordParser.T_ACCOLADE_LEFT) {

					// Déclaration d'un objet
					var token = mCompiler.eatToken();
					var object = new LeekObject(token);

					while (mCompiler.token().getType() != WordParser.T_ACCOLADE_RIGHT) {
						if (mCompiler.token().getType() != WordParser.T_STRING) {
							throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
						}
						String key = mCompiler.token().getWord();
						mCompiler.skipToken();

						if (!mCompiler.token().getWord().equals(":")) {
							throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
						}
						mCompiler.skipToken();

						var value = readExpression();
						object.addEntry(key, value);

						if (mCompiler.token().getType() == WordParser.T_VIRG) {
							mCompiler.skipToken();
						}
					}
					if (mCompiler.token().getType() != WordParser.T_ACCOLADE_RIGHT) {
						throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					object.setClosingBrace(mCompiler.token());
					retour.addExpression(object);

				} else if (word.getType() == WordParser.T_STRING) {

					if (mMain.hasGlobal(word.getWord())) {
						retour.addExpression(new LeekVariable(this, word, VariableType.GLOBAL));
					} else if (wordEquals(word, "function")) {
						retour.addExpression(readAnonymousFunction());
					} else if (wordEquals(word, "true"))
						retour.addExpression(new LeekBoolean(word, true));
					else if (wordEquals(word, "false"))
						retour.addExpression(new LeekBoolean(word, false));
					else if (wordEquals(word, "null"))
						retour.addExpression(new LeekNull(word));
					else if (wordEquals(word, "not"))
						retour.addUnaryPrefix(Operators.NOT, word);
					else if (getVersion() >= 2 && word.getWord().equals("new")) {
						retour.addUnaryPrefix(Operators.NEW, word);
					} else if (getVersion() >= 2 && word.getWord().equals("super")) {
						// super doit être dans une méthode
						if (!(mCurentBlock instanceof ClassMethodBlock)) {
							addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.KEYWORD_MUST_BE_IN_CLASS));
							retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
						} else {
							if (((ClassMethodBlock) mCurentBlock).getClassDeclaration().getParentToken() == null) {
								addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.SUPER_NOT_AVAILABLE_PARENT));
							}
							retour.addExpression(new LeekVariable(word, VariableType.SUPER, Type.CLASS, ((ClassMethodBlock) mCurentBlock).getClassDeclaration()));
						}
					} else {
						retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
						// throw new LeekCompilerException(word, Error.UNKNOWN_VARIABLE_OR_FUNCTION);
					}
				} else if (word.getType() == WordParser.T_PAR_LEFT) {
					mCompiler.skipToken(); // On avance le curseur pour bien être au début de l'expression

					var exp = readExpression();
					if (mCompiler.haveWords() && mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.token(), Error.CLOSING_PARENTHESIS_EXPECTED);
					}
					retour.addExpression(new LeekParenthesis(exp));
				} else if (word.getType() == WordParser.T_OPERATOR) {
					// Si c'est un opérateur (il doit forcément être unaire et
					// de type préfix (! ))
					int operator = Operators.getOperator(word.getWord(), getVersion());
					if (operator == Operators.MINUS)
						operator = Operators.UNARY_MINUS;
					else if (operator == Operators.DECREMENT)
						operator = Operators.PRE_DECREMENT;
					else if (operator == Operators.INCREMENT)
						operator = Operators.PRE_INCREMENT;

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
			mCompiler.skipToken();
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
			throw new LeekCompilerException(mCompiler.lastToken(), Error.UNCOMPLETE_EXPRESSION);
		}
		try {
			result.validExpression(this, mMain);
		} catch (LeekExpressionException e) {
			throw new LeekCompilerException(mCompiler.lastToken(), e.getError(), new String[] { e.getExpression() });
		}
		return result;
	}

	private boolean wordEquals(Token word, String expected) {
		if (getVersion() <= 2) {
			return word.getWord().equalsIgnoreCase(expected);
		}
		return word.getWord().equals(expected);
	}

	private LeekAnonymousFunction readAnonymousFunction() throws LeekCompilerException {
		var token = mCompiler.eatToken();
		if (mCompiler.token().getType() != WordParser.T_PAR_LEFT) {
			addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.ERROR, Error.PARENTHESIS_EXPECTED_AFTER_FUNCTION));
		}
		mCompiler.skipToken(); // Left parenthesis

		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		var previousFunction = mCurrentFunction;
		int initialLine = mLine;
		AIFile<?> initialAI = mAI;
		AnonymousFunctionBlock block = new AnonymousFunctionBlock(mCurentBlock, mMain, token);
		// if (initialBlock.getDeclaringVariable() != null)
		// 	block.addVariable(new LeekVariable(initialBlock.getDeclaringVariable(), VariableType.LOCAL));
		mCurentBlock = block;
		setCurrentFunction(block);

		// Lecture des paramètres
		while (mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
			boolean is_reference = false;
			if (mCompiler.token().getType() == WordParser.T_OPERATOR && mCompiler.token().getWord().equals("@")) {
				is_reference = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mCompiler.skipToken();
			}
			if (mCompiler.token().getType() != WordParser.T_STRING) {
				addError(new AnalyzeError(mCompiler.token(), AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_EXPECTED));
			}

			var parameter = mCompiler.token();
			mCompiler.skipToken();
			Type type = Type.ANY;

			if (mCompiler.token().getType() == WordParser.T_VIRG) {
				mCompiler.skipToken();
			}
			// else if (mCompiler.token().getType() != WordParser.T_PAR_RIGHT) {
			// 	type = parseType(parameter.getWord());
			// 	parameter = mCompiler.token();
			// 	mCompiler.skipToken();
			// 	if (mCompiler.token().getType() == WordParser.T_VIRG) {
			// 		mCompiler.skipToken();
			// 	}
			// }

			block.addParameter(this, parameter, is_reference, type);
		}
		if (mCompiler.eatToken().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.token(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		// Ouverture des accolades
		if (mCompiler.eatToken().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastToken(), Error.OPENING_CURLY_BRACKET_EXPECTED);

		// Lecture du corp de la fonction
		while (mCompiler.haveWords()) {

			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mCompiler.skipToken();
			} else
				mCurentBlock = mCurentBlock.endInstruction();
			if (!mCompiler.haveWords())
				break;

			// On regarde si on veut fermer la fonction anonyme
			if (mCompiler.token().getType() == WordParser.T_ACCOLADE_RIGHT && mCurentBlock == block)
				break;// Fermeture de la fonction anonyme
			else
				compileWord();
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
		for (var w : WordParser.reservedWords) {
			if (wordEquals(word, w)) return true;
		}
		return false;
	}

	public boolean isAvailable(Token word, boolean allFunctions) {
		if (getVersion() >= 3 && isKeyword(word)) return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasGlobal(word) ||
		// mMain.hasUserFunction(word, allFunctions) ||
		// mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasGlobal(word.getWord()) || mMain.hasUserFunction(word.getWord(), allFunctions) || mCurentBlock.hasVariable(word.getWord()))
			return false;
		return true;
	}

	public boolean isGlobalAvailable(Token word) {
		if (getVersion() <= 2) {
			if (word.getWord().equalsIgnoreCase("in") || word.getWord().equalsIgnoreCase("global") || word.getWord().equalsIgnoreCase("var") || word.getWord().equalsIgnoreCase("for") || word.getWord().equalsIgnoreCase("else") || word.getWord().equalsIgnoreCase("if") || word.getWord().equalsIgnoreCase("break") || word.getWord().equalsIgnoreCase("return") || word.getWord().equalsIgnoreCase("do") || word.getWord().equalsIgnoreCase("while") || word.getWord().equalsIgnoreCase("function") || word.getWord().equalsIgnoreCase("true") || word.getWord().equalsIgnoreCase("false") || word.getWord().equalsIgnoreCase("null"))
				return false;
		}
		if (getVersion() >= 3 && isKeyword(word)) return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasUserFunction(word,
		// false) || mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasUserFunction(word.getWord(), false) || mCurentBlock.hasVariable(word.getWord()))
			return false;
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

	public void addError(AnalyzeError error) {
		this.mAI.getErrors().add(error);
	}

	public void setCurrentBlock(AbstractLeekBlock block) {
		mCurentBlock = block;
	}

	public WordParser getParser() {
		return mCompiler;
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

	public AIFile<?> getAI() {
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
}
