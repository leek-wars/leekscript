package leekscript.compiler;

import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.ConditionalBloc;
import leekscript.compiler.bloc.DoWhileBlock;
import leekscript.compiler.bloc.ForBlock;
import leekscript.compiler.bloc.ForeachBlock;
import leekscript.compiler.bloc.ForeachKeyBlock;
import leekscript.compiler.bloc.FunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.bloc.WhileBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.exceptions.LeekInstructionException;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekArray;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekConstant;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekExpressionFunction;
import leekscript.compiler.expression.LeekFunction;
import leekscript.compiler.expression.LeekFunctionValue;
import leekscript.compiler.expression.LeekGlobal;
import leekscript.compiler.expression.LeekNull;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekParenthesis;
import leekscript.compiler.expression.LeekString;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.instruction.BlankInstruction;
import leekscript.compiler.instruction.LeekBreakInstruction;
import leekscript.compiler.instruction.LeekContinueInstruction;
import leekscript.compiler.instruction.LeekExpressionInstruction;
import leekscript.compiler.instruction.LeekGlobalDeclarationInstruction;
import leekscript.compiler.instruction.LeekReturnInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class WordCompiler {

	private final MainLeekBlock mMain;
	private AbstractLeekBlock mCurentBlock;
	private final WordParser mCompiler;
	private int mLine;
	private int mAI;

	// private final LeekVariableDeclarationInstruction mDeclaringVariable =
	// null;

	public WordCompiler(WordParser cmp, MainLeekBlock main) {
		mCompiler = cmp;
		mMain = main;
		mCurentBlock = main;
	}

	public void readCode() throws Exception {
		try {
			mCompiler.compile();
			// Receherche des fonctions utilisateur
			while (mCompiler.haveWords()) {
				if (mCompiler.getWord().getWord().equals("global")) {
					mCompiler.skipWord();
					String globalName = mCompiler.readWord().getWord();
					mMain.addGlobalDeclaration(globalName);
					while (mCompiler.getWord().getType() == WordParser.T_VIRG && mCompiler.haveWords()) {
						mCompiler.skipWord();
						globalName = mCompiler.readWord().getWord();
						mMain.addGlobalDeclaration(globalName);
					}
				} else if (mCompiler.getWord().getWord().equals("function")) {
					mCompiler.skipWord();
					String funcName = mCompiler.readWord().getWord();
					if (funcName.equals("("))
						continue;
					if (!isAvailable(funcName, false))
						throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.FUNCTION_NAME_UNAVAILABLE);

					if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
						throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
					}
					int param_count = 0;
					while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
							mCompiler.skipWord();
						}
						if (mCompiler.getWord().getType() != WordParser.T_STRING)
							throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARAMETER_NAME_EXPECTED);
						if (!isAvailable(mCompiler.getWord().getWord(), true))
							throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARAMETER_NAME_UNAVAILABLE);
						mCompiler.skipWord();
						param_count++;

						if (mCompiler.getWord().getType() == WordParser.T_VIRG)
							mCompiler.skipWord();
					}
					if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}

					mMain.addFunctionDeclaration(funcName, param_count);
				} else
					mCompiler.skipWord();
			}
			mCompiler.reset();
			// Vraie compilation
			while (mCompiler.haveWords()) {

				// On vérifie les instructions en cours

				if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.lastInstruction() != null) {
					DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
					mCurentBlock = mCurentBlock.endInstruction();
					dowhileendBlock(do_block);
					mCompiler.skipWord();
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
					mCompiler.skipWord();
				} else
					mCurentBlock = mCurentBlock.endInstruction();
			}
			if (!mMain.equals(mCurentBlock))
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPEN_BLOC_REMAINING);
		} catch (LeekInstructionException e) {
			throw new LeekCompilerException(mCompiler.lastWord(), e.getMessage());
		} catch (IndexOutOfBoundsException e) {
			throw new LeekCompilerException(mCompiler.endWord(), LeekCompilerException.END_OF_SCRIPT_UNEXPECTED);
		}
	}

	private void compileWord() throws Exception {
		mLine = mCompiler.getWord().getLine();
		mMain.addInstruction();
		IAWord word = mCompiler.getWord();
		if (word.getType() == WordParser.T_END_INSTRUCTION) {
			mCurentBlock.addInstruction(new BlankInstruction());
			mCompiler.skipWord();
			return;
		} else if (word.getType() == WordParser.T_ACCOLADE_RIGHT) {
			// Fermeture de bloc
			if (!mCurentBlock.hasAccolade() || mCurentBlock.getParent() == null)
				throw new LeekCompilerException(word, LeekCompilerException.NO_BLOC_TO_CLOSE);
			else {
				if (mCurentBlock instanceof DoWhileBlock) {
					DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
					mCurentBlock.checkEndBlock();
					mCurentBlock = mCurentBlock.getParent();
					mCompiler.skipWord();
					dowhileendBlock(do_block);
				} else {
					mCurentBlock.checkEndBlock();
					mCurentBlock = mCurentBlock.getParent();
				}
			}
			mCompiler.skipWord();
			return;
		} else if (word.getType() == WordParser.T_STRING) {
			if (word.getWord().equals("var")) {
				// Déclaration de variable
				mCompiler.skipWord();
				variableDeclaration();
				return;
			} else if (word.getWord().equals("global")) {
				// Déclaration de variable
				mCompiler.skipWord();
				globalDeclaration();
				return;
			} else if (word.getWord().equals("if")) {
				mCompiler.skipWord();
				ifBlock();
				return;
			} else if (word.getWord().equals("else")) {
				mCompiler.skipWord();
				elseBlock();
				return;
			} else if (word.getWord().equals("while")) {
				mCompiler.skipWord();
				whileBlock();
				return;
			} else if (word.getWord().equals("do")) {
				mCompiler.skipWord();
				dowhileBlock();
				return;
			} else if (word.getWord().equals("include")) {
				mCompiler.skipWord();
				includeBlock();
				return;
			} else if (word.getWord().equals("for")) {
				mCompiler.skipWord();
				forBlock();
				return;
			} else if (word.getWord().equals("break")) {
				if (!mCurentBlock.isBreakable()) {
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.BREAK_OUT_OF_LOOP);
				}
				mCompiler.skipWord();
				if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
				mCurentBlock.addInstruction(new LeekBreakInstruction(mCurentBlock.countInstructions(), mCompiler.lastWord().getLine(), mCompiler.lastWord().getAI()));

				return;
			} else if (word.getWord().equals("continue")) {
				if (!mCurentBlock.isBreakable()) {
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CONTINUE_OUT_OF_LOOP);
				}
				mCompiler.skipWord();
				if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
				mCurentBlock.addInstruction(new LeekContinueInstruction(mCurentBlock.countInstructions(), mLine, mAI));
				return;
			} else if (word.getWord().equals("return")) {
				mCompiler.skipWord();
				AbstractExpression exp = null;
				if (mCompiler.getWord().getType() != WordParser.T_END_INSTRUCTION) {
					exp = readExpression().getAbstractExpression();
				}
				if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
				mCurentBlock.addInstruction(new LeekReturnInstruction(mCurentBlock.countInstructions(), exp, mLine, mAI));
				return;
			} else if (word.getWord().equals("function")) {
				mCompiler.skipWord();
				functionBlock();
				return;
			}

		}
		AbstractExpression exp = readExpression().getAbstractExpression();
		if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
		mCurentBlock.addInstruction(new LeekExpressionInstruction(exp, mLine, mAI));
	}

	public void writeJava(String className, JavaWriter writer, String AIClass) {
		mMain.writeJavaCode(writer, className, AIClass);
	}

	private void includeBlock() throws Exception {
		// On vérifie qu'on est dans le bloc principal
		if (!mCurentBlock.equals(mMain))
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.INCLUDE_ONLY_IN_MAIN_BLOCK);
		// On récupere l'ia
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);

		if (mCompiler.getWord().getType() != WordParser.T_VAR_STRING)
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.AI_NAME_EXPECTED);
		String iaName = mCompiler.readWord().getWord();

		if (!mMain.includeAI(iaName))
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.AI_NOT_EXISTING, new String[] { iaName });

		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);

		if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
	}

	private void functionBlock() throws LeekCompilerException {
		// Déclaration de fonction utilisateur
		if (!mCurentBlock.equals(mMain))
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.FUNCTION_ONLY_IN_MAIN_BLOCK);
		// Récupération du nom de la fonction
		if (mCompiler.getWord().getType() != WordParser.T_STRING)
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.FUNCTION_NAME_EXPECTED);
		String funcName = mCompiler.readWord().getWord();
		if (!isAvailable(funcName, false))
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.FUNCTION_NAME_UNAVAILABLE);

		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
		}

		FunctionBlock block = new FunctionBlock(mCurentBlock, mMain, mLine, mAI);
		block.setName(funcName);
		mCurentBlock = block;
		while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
			boolean is_reference = false;
			if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
				is_reference = true;
				mCompiler.skipWord();
			}
			if (mCompiler.getWord().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARAMETER_NAME_EXPECTED);
			if (!isAvailable(mCompiler.getWord().getWord(), true))
				throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARAMETER_NAME_UNAVAILABLE);
			block.addParameter(mCompiler.readWord().getWord(), is_reference);
			if (mCompiler.getWord().getType() == WordParser.T_VIRG)
				mCompiler.skipWord();
		}
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		// On regarde s'il y a des accolades
		if (mCompiler.readWord().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_CURLY_BRACKET_EXPECTED);
		mMain.addFunction(block);
	}

	private void forBlock() throws Exception {
		// Bloc de type for(i=0;i<5;i++) ou encore for(element in tableau)
		// On peut déclarer une variable pendant l'instruction d'initialisation
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
		}

		boolean isDeclaration = false;
		String varName = "";
		AbstractLeekBlock forBlock = null;

		// Là on doit déterminer si y'a déclaration de variable
		if (mCompiler.getWord().getWord().equals("var")) {// Il y a déclaration
			isDeclaration = true;
			mCompiler.skipWord();
		}
		// On récupère ensuite le nom de la variable
		if (mCompiler.getWord().getType() != WordParser.T_STRING)
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.VARIABLE_NAME_EXPECTED);
		varName = mCompiler.readWord().getWord();
		// Si c'est une déclaration on vérifie que le nom est disponnible
		if (isDeclaration) {
			if (!isAvailable(varName, true))
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.VARIABLE_NAME_UNAVAILABLE);
		} else {
			// Sinon on vérifie que la variable existe
			if (!mCurentBlock.hasVariable(varName) && !mMain.hasGlobal(varName))
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.VARIABLE_NOT_EXISTS);
		}
		// Maintenant on va savoir si on a affaire à un for(i in array) ou à un
		// for(i=0;i<...
		if (mCompiler.getWord().getWord().equals(":")) {// C'est un
														// for(key:value in
														// array)
			mCompiler.skipWord();
			boolean isValueDeclaration = false;
			if (mCompiler.getWord().getWord().equals("var")) {// Il y a
																// déclaration
																// de la valeur
				isValueDeclaration = true;
				mCompiler.skipWord();
			}
			// On récupère ensuite le nom de la variable accueillant la valeur
			if (mCompiler.getWord().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.VARIABLE_NAME_EXPECTED);
			String valueVarName = mCompiler.readWord().getWord();

			if (isValueDeclaration) {
				if (!isAvailable(valueVarName, true))
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.VARIABLE_NAME_UNAVAILABLE);
			} else {
				// Sinon on vérifie que la variable existe
				if (!mCurentBlock.hasVariable(valueVarName) && !mMain.hasGlobal(valueVarName))
					throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.VARIABLE_NOT_EXISTS);
			}

			if (!mCompiler.readWord().getWord().equals("in"))
				throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.KEYWORD_IN_EXPECTED);

			// On déclare notre bloc foreach et on entre dedans
			ForeachKeyBlock block = new ForeachKeyBlock(mCurentBlock, mMain, isDeclaration, isValueDeclaration, mLine, mAI);
			mCurentBlock.addInstruction(block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			AbstractExpression array = readExpression().getAbstractExpression();
			block.setArray(array);
			block.setKeyIterator(varName, isDeclaration);
			block.setValueIterator(valueVarName, isValueDeclaration);

			forBlock = block;
		} else if (mCompiler.getWord().getWord().equals("in")) {// C'est un
																// for(i in
																// array)
			mCompiler.skipWord();

			ForeachBlock block = new ForeachBlock(mCurentBlock, mMain, isDeclaration, mLine, mAI);
			mCurentBlock.addInstruction(block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			AbstractExpression array = readExpression().getAbstractExpression();
			block.setArray(array);
			block.setIterator(varName, isDeclaration);

			forBlock = block;
		} else if (mCompiler.getWord().getWord().equals("=")) {// C'est un
																// for(i=0;i<1;i++)
			mCompiler.skipWord();

			ForBlock block = new ForBlock(mCurentBlock, mMain, mLine, mAI);
			mCurentBlock.addInstruction(block);
			mCurentBlock = block;

			// On récupère la valeur de base du compteur
			AbstractExpression initValue = readExpression().getAbstractExpression();
			if (isDeclaration)
				block.addVariable(varName);
			if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
			AbstractExpression condition = readExpression().getAbstractExpression();
			if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
			AbstractExpression incrementation = readExpression().getAbstractExpression();

			// Attention si l'incrémentation n'est pas une expression Java fait
			// la gueule !
			if (incrementation != null && (incrementation instanceof LeekVariable ||
					(incrementation instanceof LeekExpression && ((LeekExpression) incrementation).getOperator() == -1))) {
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.UNCOMPLETE_EXPRESSION);
			}

			block.setInitialisation(varName, initValue, isDeclaration, block.hasGlobal(varName));
			block.setCondition(condition);
			block.setIncrementation(incrementation);

			forBlock = block;
		} else
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.KEYWORD_UNEXPECTED);

		// On vérifie la parenthèse fermante
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);
		}
		// On regarde s'il y a des accolades
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			forBlock.noAccolade();
	}

	private void whileBlock() throws Exception {
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
		}
		AbstractExpression exp = readExpression().getAbstractExpression();
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);
		}
		WhileBlock bloc = new WhileBlock(mCurentBlock, mMain, mLine, mAI);
		bloc.setCondition(exp);
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(bloc);
		mCurentBlock = bloc;
	}

	private void dowhileBlock() throws LeekCompilerException, LeekInstructionException {
		DoWhileBlock bloc = new DoWhileBlock(mCurentBlock, mMain);
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(bloc);
		mCurentBlock = bloc;
	}

	private void dowhileendBlock(DoWhileBlock bloc) throws Exception {
		if (!mCompiler.readWord().getWord().equals("while"))
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.WHILE_EXPECTED_AFTER_DO);
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
		}
		bloc.setCondition(readExpression().getAbstractExpression());
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);
		}
		if (mCompiler.getWord().getType() != WordParser.T_END_INSTRUCTION)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
	}

	private void elseBlock() throws Exception {
		// On vérifie qu'on est bien associé à un bloc conditionnel
		ConditionalBloc last = mCurentBlock.getLastOpenedConditionalBlock();
		if (last == null || last.getCondition() == null) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.NO_IF_BLOCK);
		}
		ConditionalBloc bloc = new ConditionalBloc(mCurentBlock, mMain, mLine, mAI);
		bloc.setParentCondition(last);
		if (mCompiler.getWord().getWord().equals("if")) {
			// On veut un elseif
			mCompiler.skipWord();
			if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
			}
			AbstractExpression exp = readExpression().getAbstractExpression();
			if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
				throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);
			}
			bloc.setCondition(exp);
		}

		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			bloc.noAccolade();
		last.getParent().addInstruction(bloc);
		mCurentBlock = bloc;
	}

	private void ifBlock() throws Exception {
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_PARENTHESIS_EXPECTED);
		}
		AbstractExpression exp = readExpression().getAbstractExpression();
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);
		}
		ConditionalBloc bloc = new ConditionalBloc(mCurentBlock, mMain, mLine, mAI);
		bloc.setCondition(exp);
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(bloc);
		mCurentBlock = bloc;
	}

	private void globalDeclaration() throws Exception {
		// Il y a au moins une premiere variable
		IAWord word = mCompiler.readWord();
		if (!(mCurentBlock instanceof MainLeekBlock))
			throw new LeekCompilerException(word, LeekCompilerException.GLOBAL_ONLY_IN_MAIN_BLOCK);
		if (word.getType() != WordParser.T_STRING)
			throw new LeekCompilerException(word, LeekCompilerException.VAR_NAME_EXPECTED_AFTER_GLOBAL);
		if (!isGlobalAvailable(word.getWord()) || mMain.hasDeclaredGlobal(word.getWord()))
			throw new LeekCompilerException(word, LeekCompilerException.VARIABLE_NAME_UNAVAILABLE);
		LeekGlobalDeclarationInstruction variable = new LeekGlobalDeclarationInstruction(word.getWord(), mLine, mAI);
		// On regarde si une valeur est assignée
		if (mCompiler.getWord().getWord().equals("=")) {
			mCompiler.skipWord();
			// Si oui on récupère la valeur en question
			variable.setValue(readExpression().getAbstractExpression());
		}
		// On ajoute la variable
		mMain.addGlobal(variable.getName());
		mCurentBlock.addInstruction(variable);
		while (mCompiler.getWord().getType() == WordParser.T_VIRG) {
			// On regarde si y'en a d'autres
			mCompiler.skipWord();// On passe la virgule
			word = mCompiler.readWord();
			if (word.getType() != WordParser.T_STRING)
				throw new LeekCompilerException(word, LeekCompilerException.VAR_NAME_EXPECTED);
			if (!isGlobalAvailable(word.getWord()) || mMain.hasDeclaredGlobal(word.getWord()))
				throw new LeekCompilerException(word, LeekCompilerException.VARIABLE_NAME_UNAVAILABLE);
			variable = new LeekGlobalDeclarationInstruction(word.getWord(), mLine, mAI);
			// On regarde si une valeur est assign�e
			if (mCompiler.getWord().getWord().equals("=")) {
				mCompiler.skipWord();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression().getAbstractExpression());
			}
			// On ajoute la variable
			mMain.addGlobal(variable.getName());
			mCurentBlock.addInstruction(variable);
		}
		word = mCompiler.readWord();
		if (word.getType() != WordParser.T_END_INSTRUCTION)
			throw new LeekCompilerException(word, LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
	}

	private void variableDeclaration() throws Exception {
		// Il y a au moins une premiere variable
		IAWord word = mCompiler.readWord();
		if (word.getType() != WordParser.T_STRING)
			throw new LeekCompilerException(word, LeekCompilerException.VAR_NAME_EXPECTED);
		if (!isAvailable(word.getWord(), true))
			throw new LeekCompilerException(word, LeekCompilerException.VARIABLE_NAME_UNAVAILABLE);
		LeekVariableDeclarationInstruction variable = new LeekVariableDeclarationInstruction(word.getWord(), mLine, mAI);
		// On regarde si une valeur est assignée
		if (mCompiler.getWord().getWord().equals("=")) {
			mCompiler.skipWord();
			// Si oui on récupère la valeur en question
			mCurentBlock.setDeclaringVariable(variable.getName());
			variable.setValue(readExpression().getAbstractExpression());
			if (mCurentBlock.isDeclaringBariableUsed())
				variable.mustSepare();
			mCurentBlock.setDeclaringVariable(null);
		}
		// On ajoute la variable
		mCurentBlock.addVariable(variable.getName());
		mCurentBlock.addInstruction(variable);
		while (mCompiler.getWord().getType() == WordParser.T_VIRG) {
			// On regarde si y'en a d'autres
			mCompiler.skipWord();// On passe la virgule
			word = mCompiler.readWord();
			if (word.getType() != WordParser.T_STRING)
				throw new LeekCompilerException(word, LeekCompilerException.VAR_NAME_EXPECTED);
			if (!isAvailable(word.getWord(), true))
				throw new LeekCompilerException(word, LeekCompilerException.VARIABLE_NAME_UNAVAILABLE);
			variable = new LeekVariableDeclarationInstruction(word.getWord(), mLine, mAI);
			// On regarde si une valeur est assign�e
			if (mCompiler.getWord().getWord().equals("=")) {
				mCompiler.skipWord();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression().getAbstractExpression());
			}
			// On ajoute la variable
			mCurentBlock.addVariable(variable.getName());
			mCurentBlock.addInstruction(variable);
		}
		word = mCompiler.readWord();
		if (word.getType() != WordParser.T_END_INSTRUCTION)
			throw new LeekCompilerException(word, LeekCompilerException.END_OF_INSTRUCTION_EXPECTED);
	}

	public LeekExpression readExpression() throws Exception {
		LeekExpression retour = new LeekExpression();
		while (mCompiler.haveWords()) {
			IAWord word = mCompiler.getWord();
			if (retour.needOperator()) {
				// Si on attend un opérateur mais qu'il vient pas

				if (word.getType() == WordParser.T_BRACKET_LEFT) {
					mCompiler.skipWord();// On avance le curseur pour être au
											// début de l'expression

					LeekExpression exp = readExpression();
					if (mCompiler.getWord().getType() != WordParser.T_BRACKET_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.CLOSING_SQUARE_BRACKET_EXPECTED);
					}
					retour.addBracket(exp.getAbstractExpression());
				} else if (word.getType() == WordParser.T_PAR_LEFT) {
					mCompiler.skipWord();// On avance le curseur pour être au
											// début de l'expression
					LeekExpressionFunction function = new LeekExpressionFunction();

					while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						function.addParameter(readExpression().getAbstractExpression());
						if (mCompiler.getWord().getType() == WordParser.T_VIRG)
							mCompiler.skipWord();
					}
					if (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}

					retour.addFunction(function);
				} else if (word.getType() == WordParser.T_OPERATOR) {
					int operator = Operators.getOperator(word.getWord());

					// Là c'est soit un opérateur (+ - ...) soit un suffix
					// unaire (++ -- ) sinon on sort de l'expression
					if (Operators.isUnaryPrefix(operator))
						break;
					if (operator == Operators.DOUBLE_POINT && !retour.hasTernaire())
						break;

					if (Operators.isUnarySuffix(operator))
						retour.addUnarySuffix(operator);
					else
						retour.addOperator(operator);
				} else if (word.getType() == WordParser.T_STRING) {
					if (word.getWord().equals("is")) {
						mCompiler.skipWord();
						word = mCompiler.getWord();
						if (word.getWord().equals("not")) {
							mCompiler.skipWord();
							retour.addOperator(Operators.NOTEQUALS);
						} else {
							retour.addOperator(Operators.EQUALS);
						}
						continue;
					}
					break;
				} else
					break;
			} else {
				if (word.getType() == WordParser.T_NUMBER)
					retour.addExpression(new LeekNumber(Double.parseDouble(word.getWord())));
				else if (word.getType() == WordParser.T_VAR_STRING) {
					retour.addExpression(new LeekString(word.getWord()));
				} else if (word.getType() == WordParser.T_BRACKET_LEFT) {
					// Déclaration d'un tableau
					mCompiler.skipWord();
					LeekArray array = new LeekArray();
					int type = 0;// 0 => A déterminer, 1=> Simple, 2 =>
									// Clé:valeur
					while (mCompiler.getWord().getType() != WordParser.T_BRACKET_RIGHT) {
						AbstractExpression exp = readExpression().getAbstractExpression();
						if (mCompiler.getWord().getWord().equals(":")) {
							if (type == 0)
								type = 2;
							else if (type == 1)
								throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.SIMPLE_ARRAY);
							mCompiler.skipWord();
							AbstractExpression value = readExpression().getAbstractExpression();
							array.addValue(exp, value);
						} else {
							if (type == 0)
								type = 1;
							else if (type == 2)
								throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.ASSOCIATIVE_ARRAY);
							array.addValue(exp);
						}
						if (mCompiler.getWord().getType() == WordParser.T_VIRG)
							mCompiler.skipWord();
					}
					if (mCompiler.getWord().getType() != WordParser.T_BRACKET_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					retour.addExpression(array);
				} else if (word.getType() == WordParser.T_STRING) {
					if (mCurentBlock.hasVariable(word.getWord()))
						retour.addExpression(new LeekVariable(word.getWord()));
					else if (mMain.hasGlobal(word.getWord()))
						retour.addExpression(new LeekGlobal(word.getWord()));
					else if (LeekFunctions.isFunction(word.getWord()) > -1 || mMain.hasUserFunction(word.getWord(), true)) {
						LeekFunction function = new LeekFunction(word.getWord());
						String fname = word.getWord();
						// On doit lire la fonction
						mCompiler.skipWord();
						if (mCompiler.getWord().getType() != WordParser.T_PAR_LEFT) {
							// On utilise le nom de la fonction comme une
							// variable
							String namespace = LeekFunctions.getNamespace(fname);
							retour.addExpression(new LeekFunctionValue(fname, namespace));
							continue;
						} else {
							mCompiler.skipWord();
							while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
								function.addParameter(readExpression().getAbstractExpression());
								if (mCompiler.getWord().getType() == WordParser.T_VIRG)
									mCompiler.skipWord();
							}
							if (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
								throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
							}
							retour.addExpression(function);
						}
					} else if (word.getWord().equalsIgnoreCase("function")) {
						retour.addExpression(readAnonymousFunction());

					} else if (word.getWord().equalsIgnoreCase("true"))
						retour.addExpression(new LeekBoolean(true));
					else if (word.getWord().equalsIgnoreCase("false"))
						retour.addExpression(new LeekBoolean(false));
					else if (word.getWord().equalsIgnoreCase("null"))
						retour.addExpression(new LeekNull());
					else if (word.getWord().equalsIgnoreCase("not"))
						retour.addUnaryPrefix(Operators.NOT);
					else if (LeekConstants.getType(word.getWord()) != 0)
						retour.addExpression(new LeekConstant(word.getWord()));
					else {
						throw new LeekCompilerException(word, LeekCompilerException.UNKNOWN_VARIABLE_OR_FUNCTION);
					}
				} else if (word.getType() == WordParser.T_PAR_LEFT) {
					mCompiler.skipWord();// On avance le curseur pour bien être
											// au début de l'expression

					LeekExpression exp = readExpression();
					if (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.CLOSING_PARENTHESIS_EXPECTED);
					}
					retour.addExpression(new LeekParenthesis(exp.getAbstractExpression()));
				} else if (word.getType() == WordParser.T_OPERATOR) {
					// Si c'est un opérateur (il doit forcément être unaire et
					// de type préfix (! ))
					int operator = Operators.getOperator(word.getWord());
					if (operator == Operators.MINUS)
						operator = Operators.UNARY_MINUS;
					else if (operator == Operators.DECREMENT)
						operator = Operators.PRE_DECREMENT;
					else if (operator == Operators.INCREMENT)
						operator = Operators.PRE_INCREMENT;

					if (Operators.isUnaryPrefix(operator)) {
						// Si oui on l'ajoute
						retour.addUnaryPrefix(operator);
					} else {
						throw new LeekCompilerException(word, LeekCompilerException.OPERATOR_UNEXPECTED);
					}
				} else {
					throw new LeekCompilerException(word, LeekCompilerException.VALUE_EXPECTED);
				}
			}
			mCompiler.skipWord();
		}
		// Avant de retourner, on valide l'expression
		try {
			retour.getAbstractExpression().validExpression(mMain);
		} catch (LeekExpressionException e) {
			throw new LeekCompilerException(mCompiler.lastWord(), e.getMessage(), new String[] { e.getExpression() });
		}
		return retour;
	}

	private LeekAnonymousFunction readAnonymousFunction() throws Exception {
		mCompiler.skipWord();
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_FUNCTION);
		}
		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		int initialLine = mLine;
		int initialAI = mAI;
		AnonymousFunctionBlock block = new AnonymousFunctionBlock(mCurentBlock, mMain, mLine, mAI);
		if (initialBlock.getDeclaringVariable() != null)
			block.addVariable(initialBlock.getDeclaringVariable());
		mCurentBlock = block;

		// Lecture des paramètres
		while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
			boolean is_reference = false;
			if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
				is_reference = true;
				mCompiler.skipWord();
			}
			if (mCompiler.getWord().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARAMETER_NAME_EXPECTED);
			if (!isAvailable(mCompiler.getWord().getWord(), true))
				throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARAMETER_NAME_UNAVAILABLE);
			block.addParameter(mCompiler.readWord().getWord(), is_reference);
			if (mCompiler.getWord().getType() == WordParser.T_VIRG)
				mCompiler.skipWord();
		}
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.getWord(), LeekCompilerException.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		// Ouverture des accolades
		if (mCompiler.readWord().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), LeekCompilerException.OPENING_CURLY_BRACKET_EXPECTED);

		// Lecture du corp de la fonction
		while (mCompiler.haveWords()) {

			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.lastInstruction() != null) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mCompiler.skipWord();
			} else
				mCurentBlock = mCurentBlock.endInstruction();
			if (!mCompiler.haveWords())
				break;

			// On regarde si on veut fermer la fonction anonyme
			if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_RIGHT && mCurentBlock == block)
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

		return new LeekAnonymousFunction(block);
	}

	public boolean isAvailable(String word, boolean allFunctions) {
		if (word.equalsIgnoreCase("in") || word.equalsIgnoreCase("global") || word.equalsIgnoreCase("var") || word.equalsIgnoreCase("for") || word.equalsIgnoreCase("else")
				|| word.equalsIgnoreCase("if") || word.equalsIgnoreCase("break") || word.equalsIgnoreCase("return") || word.equalsIgnoreCase("do") || word.equalsIgnoreCase("while")
				|| word.equalsIgnoreCase("function") || word.equalsIgnoreCase("true") || word.equalsIgnoreCase("false") || word.equalsIgnoreCase("null"))
			return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasGlobal(word) ||
		// mMain.hasUserFunction(word, allFunctions) ||
		// mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasGlobal(word) || mMain.hasUserFunction(word, allFunctions) || mCurentBlock.hasVariable(word))
			return false;
		return true;
	}

	public boolean isGlobalAvailable(String word) {
		if (word.equalsIgnoreCase("in") || word.equalsIgnoreCase("global") || word.equalsIgnoreCase("var") || word.equalsIgnoreCase("for") || word.equalsIgnoreCase("else")
				|| word.equalsIgnoreCase("if") || word.equalsIgnoreCase("break") || word.equalsIgnoreCase("return") || word.equalsIgnoreCase("do") || word.equalsIgnoreCase("while")
				|| word.equalsIgnoreCase("function") || word.equalsIgnoreCase("true") || word.equalsIgnoreCase("false") || word.equalsIgnoreCase("null"))
			return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasUserFunction(word,
		// false) || mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasUserFunction(word, false) || mCurentBlock.hasVariable(word))
			return false;
		return true;
	}

	public String getString() {
		return mMain.getCode();
	}
}
