package leekscript.compiler;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekArray;
import leekscript.compiler.expression.LeekBoolean;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekExpressionFunction;
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

	private final MainLeekBlock mMain;
	private AbstractLeekBlock mCurentBlock;
	private AbstractLeekBlock mCurrentFunction;
	private ClassDeclarationInstruction mCurrentClass;
	private final WordParser mCompiler;
	private int mLine;
	private AIFile<?> mAI = null;
	private Set<AnalyzeError> errors = new TreeSet<>();
	private final int version;

	public WordCompiler(WordParser cmp, MainLeekBlock main, AIFile<?> ai, int version) {
		mCompiler = cmp;
		mMain = main;
		mCurentBlock = main;
		mCurrentFunction = main;
		mAI = ai;
		this.version = version;
	}

	public void readCode() throws LeekCompilerException {
		try {
			mCompiler.compile(this);
			// Recherche des fonctions utilisateur
			while (mCompiler.haveWords()) {
				if (mCompiler.getWord().getType() == WordParser.T_STRING && mCompiler.getWord().getWord().equals("global")) {
					mCompiler.skipWord();
					var global = mCompiler.readWord();
					// System.out.println("global = " + global.getWord() + " " + global.getLine());
					if (!isGlobalAvailable(global) || mMain.hasDeclaredGlobal(global.getWord())) {
						addError(new AnalyzeError(global, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
					} else {
						mMain.addGlobal(global.getWord());
					}
					if (mCompiler.getWord().getWord().equals("=")) {
						mCompiler.skipWord();
						readExpression();
					}
					while (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_VIRG) {
						mCompiler.skipWord();
						global = mCompiler.readWord();
						if (!isGlobalAvailable(global) || mMain.hasDeclaredGlobal(global.getWord())) {
							addError(new AnalyzeError(global, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
						} else {
							mMain.addGlobal(global.getWord());
						}
						if (mCompiler.getWord().getWord().equals("=")) {
							mCompiler.skipWord();
							readExpression();
						}
					}
				} else if (mCompiler.getWord().getWord().equals("function")) {
					mCompiler.skipWord();
					var funcName = mCompiler.readWord();
					if (funcName.getWord().equals("("))
						continue;
					if (!isAvailable(funcName, false))
						throw new LeekCompilerException(mCompiler.getWord(), Error.FUNCTION_NAME_UNAVAILABLE);

					if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
						throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
					}
					int param_count = 0;
					var parameters = new HashSet<String>();
					while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
							mCompiler.skipWord();
						}
						if (mCompiler.getWord().getType() != WordParser.T_STRING) {
							throw new LeekCompilerException(mCompiler.getWord(), Error.PARAMETER_NAME_EXPECTED);
						}
						var parameter = mCompiler.readWord();
						if (parameters.contains(parameter.getWord())) {
							throw new LeekCompilerException(parameter, Error.PARAMETER_NAME_UNAVAILABLE);
						}
						parameters.add(parameter.getWord());
						param_count++;

						if (mCompiler.getWord().getType() == WordParser.T_VIRG)
							mCompiler.skipWord();
					}
					if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}

					mMain.addFunctionDeclaration(funcName.getWord(), param_count);
				} else
					mCompiler.skipWord();
			}
			mCompiler.reset();
			// Vraie compilation
			while (mCompiler.haveWords()) {

				// On vérifie les instructions en cours

				if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
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
				} else {
					if (mCurentBlock.endInstruction() == mCurentBlock) {
						throw new LeekCompilerException(mCompiler.lastWord(), Error.NO_BLOC_TO_CLOSE);
					}
					mCurentBlock = mCurentBlock.endInstruction();
				}
			}
			if (!mMain.equals(mCurentBlock))
				throw new LeekCompilerException(mCompiler.lastWord(), Error.OPEN_BLOC_REMAINING);

		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace(System.out);
			throw new LeekCompilerException(mCompiler.endWord(), Error.END_OF_SCRIPT_UNEXPECTED);
		}
	}

	public void analyze() {
		// Analyse sémantique
		mCurentBlock = mMain;
		mCurrentFunction = mMain;
		mMain.analyze(this);
	}

	private void compileWord() throws LeekCompilerException {
		mLine = mCompiler.getWord().getLine();
		mMain.addInstruction();
		IAWord word = mCompiler.getWord();
		if (word.getType() == WordParser.T_END_INSTRUCTION) {
			// mCurentBlock.addInstruction(this, new BlankInstruction());
			mCurentBlock.setFull(true);
			mCompiler.skipWord();
			return;
		} else if (word.getType() == WordParser.T_ACCOLADE_RIGHT) {
			// Fermeture de bloc
			if (!mCurentBlock.hasAccolade() || mCurentBlock.getParent() == null) {
				// throw new LeekCompilerException(word, Error.NO_BLOC_TO_CLOSE);
				errors.add(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.NO_BLOC_TO_CLOSE));
			} else {
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
			} else if (version >= 2 && getCurrentBlock() instanceof MainLeekBlock && word.getWord().equals("class")) {
				// Déclaration de classe
				mCompiler.skipWord();
				classDeclaration();
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
					throw new LeekCompilerException(mCompiler.lastWord(), Error.BREAK_OUT_OF_LOOP);
				}
				mCompiler.skipWord();
				if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
					mCompiler.skipWord();
					// throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
				mCurentBlock.addInstruction(this, new LeekBreakInstruction(mCurentBlock.countInstructions(), mCompiler.lastWord().getLine(), mCompiler.lastWord().getAI()));

				return;
			} else if (word.getWord().equals("continue")) {
				if (!mCurentBlock.isBreakable()) {
					throw new LeekCompilerException(mCompiler.lastWord(), Error.CONTINUE_OUT_OF_LOOP);
				}
				mCompiler.skipWord();
				if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
					mCompiler.skipWord();
				// if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
				// 	throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
				mCurentBlock.addInstruction(this, new LeekContinueInstruction(mCurentBlock.countInstructions(), mLine, mAI));
				return;
			} else if (word.getWord().equals("return")) {
				mCompiler.skipWord();
				AbstractExpression exp = null;
				if (mCompiler.getWord().getType() != WordParser.T_END_INSTRUCTION) {
					exp = readExpression();
				}
				if (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
					mCompiler.skipWord();
				mCurentBlock.addInstruction(this, new LeekReturnInstruction(mCurentBlock.countInstructions(), exp, mLine, mAI));
				return;
			} else if (word.getWord().equals("function")) {
				mCompiler.skipWord();
				functionBlock();
				return;
			}

		}
		AbstractExpression exp = readExpression();
		if (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipWord();
		}
		// if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION)
		// 	throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
		mCurentBlock.addInstruction(this, new LeekExpressionInstruction(exp, mLine, mAI));
	}

	public void writeJava(String className, JavaWriter writer, String AIClass) {
		mMain.writeJavaCode(writer, className, AIClass);
	}

	private void includeBlock() throws LeekCompilerException {
		// On vérifie qu'on est dans le bloc principal
		if (!mCurentBlock.equals(mMain))
			throw new LeekCompilerException(mCompiler.lastWord(), Error.INCLUDE_ONLY_IN_MAIN_BLOCK);
		// On récupere l'ia
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);

		if (mCompiler.getWord().getType() != WordParser.T_VAR_STRING)
			throw new LeekCompilerException(mCompiler.getWord(), Error.AI_NAME_EXPECTED);
		String iaName = mCompiler.readWord().getWord();

		if (!mMain.includeAI(this, iaName)) {
			errors.add(new AnalyzeError(mCompiler.lastWord(), AnalyzeErrorLevel.ERROR, Error.AI_NOT_EXISTING, new String[] { iaName }));
		}

		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT)
			throw new LeekCompilerException(mCompiler.lastWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
	}

	private void functionBlock() throws LeekCompilerException {
		// Déclaration de fonction utilisateur
		if (!mCurentBlock.equals(mMain))
			throw new LeekCompilerException(mCompiler.lastWord(), Error.FUNCTION_ONLY_IN_MAIN_BLOCK);
		// Récupération du nom de la fonction
		if (mCompiler.getWord().getType() != WordParser.T_STRING)
			throw new LeekCompilerException(mCompiler.getWord(), Error.FUNCTION_NAME_EXPECTED);
		IAWord funcName = mCompiler.readWord();
		if (!isAvailable(funcName, false))
			throw new LeekCompilerException(mCompiler.getWord(), Error.FUNCTION_NAME_UNAVAILABLE);

		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
		}

		var previousFunction = mCurrentFunction;
		FunctionBlock block = new FunctionBlock(mCurentBlock, mMain, mLine, mAI, funcName);
		mCurentBlock = block;
		mCurrentFunction = block;
		while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
			boolean is_reference = false;
			if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
				is_reference = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mCompiler.skipWord();
			}
			if (mCompiler.getWord().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.getWord(), Error.PARAMETER_NAME_EXPECTED);
			// if (!isAvailable(mCompiler.getWord().getWord(), true))
			// 	throw new LeekCompilerException(mCompiler.getWord(), Error.PARAMETER_NAME_UNAVAILABLE);
			block.addParameter(this, mCompiler.readWord(), is_reference);
			if (mCompiler.getWord().getType() == WordParser.T_VIRG)
				mCompiler.skipWord();
		}
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		// On regarde s'il y a des accolades
		if (mCompiler.readWord().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		mMain.addFunction(block);
		mCurrentFunction = previousFunction;
	}

	private void forBlock() throws LeekCompilerException {
		// Bloc de type for(i=0;i<5;i++) ou encore for(element in tableau)
		// On peut déclarer une variable pendant l'instruction d'initialisation
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
		}

		boolean isDeclaration = false;
		AbstractLeekBlock forBlock = null;

		// Là on doit déterminer si y'a déclaration de variable
		if (mCompiler.getWord().getWord().equals("var")) {// Il y a déclaration
			isDeclaration = true;
			mCompiler.skipWord();
		}
		// Référence ?
		boolean reference1 = false;
		if (mCompiler.getWord().getWord().equals("@")) {
			reference1 = true;
			if (getVersion() >= 2) {
				addError(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
			}
			mCompiler.skipWord();
		}
		// On récupère ensuite le nom de la variable
		if (mCompiler.getWord().getType() != WordParser.T_STRING)
			throw new LeekCompilerException(mCompiler.getWord(), Error.VARIABLE_NAME_EXPECTED);
		IAWord varName = mCompiler.readWord();

		// Maintenant on va savoir si on a affaire à un for (i in array) ou à un for(i=0;i<...
		if (mCompiler.getWord().getWord().equals(":")) { // C'est un for (key:value in array)
			mCompiler.skipWord();
			boolean isValueDeclaration = false;
			if (mCompiler.getWord().getWord().equals("var")) { // Il y a déclaration de la valeur
				isValueDeclaration = true;
				mCompiler.skipWord();
			}
			// Référence ?
			boolean reference2 = false;
			if (mCompiler.getWord().getWord().equals("@")) {
				reference2 = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mCompiler.skipWord();
			}
			// On récupère ensuite le nom de la variable accueillant la valeur
			if (mCompiler.getWord().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.getWord(), Error.VARIABLE_NAME_EXPECTED);
			IAWord valueVarName = mCompiler.readWord();

			if (!mCompiler.readWord().getWord().equals("in"))
				throw new LeekCompilerException(mCompiler.getWord(), Error.KEYWORD_IN_EXPECTED);

			// On déclare notre bloc foreach et on entre dedans
			ForeachKeyBlock block = new ForeachKeyBlock(mCurentBlock, mMain, isDeclaration, isValueDeclaration, mLine, mAI, reference1, reference2);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			AbstractExpression array = readExpression();
			block.setArray(array);
			block.setKeyIterator(this, varName, isDeclaration);
			block.setValueIterator(this, valueVarName, isValueDeclaration);

			forBlock = block;
		} else if (mCompiler.getWord().getWord().equals("in")) { // C'est un for (i in array)
			mCompiler.skipWord();

			ForeachBlock block = new ForeachBlock(mCurentBlock, mMain, isDeclaration, mLine, mAI, reference1);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On lit le array (ou liste de valeurs)
			AbstractExpression array = readExpression();
			block.setArray(array);
			block.setIterator(this, varName);

			forBlock = block;
		} else if (mCompiler.getWord().getWord().equals("=")) { // C'est un for (i=0;i<1;i++)
			mCompiler.skipWord();

			ForBlock block = new ForBlock(mCurentBlock, mMain, mLine, mAI);
			mCurentBlock.addInstruction(this, block);
			mCurentBlock = block;

			// On récupère la valeur de base du compteur
			AbstractExpression initValue = readExpression();
			if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION) {
				// errors.add(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.ERROR, Error.END_OF_INSTRUCTION_EXPECTED));
				throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
				// return;
			}
			// if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			// 	mCompiler.skipWord();
			// }
			AbstractExpression condition = readExpression();
			if (mCompiler.readWord().getType() != WordParser.T_END_INSTRUCTION) {
				// errors.add(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.ERROR, Error.END_OF_INSTRUCTION_EXPECTED));
				throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
				// return;
			}
			// if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			// 	mCompiler.skipWord();
			// }
			AbstractExpression incrementation = readExpression();

			// Attention si l'incrémentation n'est pas une expression Java fait la gueule !
			if (incrementation != null && (incrementation instanceof LeekVariable ||
					(incrementation instanceof LeekExpression && ((LeekExpression) incrementation).getOperator() == -1))) {
				throw new LeekCompilerException(mCompiler.lastWord(), Error.UNCOMPLETE_EXPRESSION);
			}

			block.setInitialisation(this, varName, initValue, isDeclaration, block.hasGlobal(varName.getWord()));
			block.setCondition(condition);
			block.setIncrementation(incrementation);

			forBlock = block;
		} else
			throw new LeekCompilerException(mCompiler.getWord(), Error.KEYWORD_UNEXPECTED);

		// On vérifie la parenthèse fermante
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		// On regarde s'il y a des accolades
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			forBlock.noAccolade();
	}

	private void whileBlock() throws LeekCompilerException {
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		AbstractExpression exp = readExpression();
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		WhileBlock bloc = new WhileBlock(mCurentBlock, mMain, mLine, mAI);
		bloc.setCondition(exp);
		if (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipWord();
			bloc.addInstruction(this, new BlankInstruction());
			bloc.noAccolade();
		} else {
			bloc.noAccolade();
		}
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void dowhileBlock() throws LeekCompilerException {
		DoWhileBlock bloc = new DoWhileBlock(mCurentBlock, mMain, mAI);
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void dowhileendBlock(DoWhileBlock bloc) throws LeekCompilerException {
		if (!mCompiler.readWord().getWord().equals("while"))
			throw new LeekCompilerException(mCompiler.lastWord(), Error.WHILE_EXPECTED_AFTER_DO);
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		bloc.setCondition(readExpression());
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		// if (mCompiler.getWord().getType() != WordParser.T_END_INSTRUCTION)
		// 	throw new LeekCompilerException(mCompiler.lastWord(), Error.END_OF_INSTRUCTION_EXPECTED);
	}

	private void elseBlock() throws LeekCompilerException {
		// On vérifie qu'on est bien associé à un bloc conditionnel
		ConditionalBloc last = mCurentBlock.getLastOpenedConditionalBlock();
		if (last == null || last.getCondition() == null) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.NO_IF_BLOCK);
		}
		ConditionalBloc bloc = new ConditionalBloc(mCurentBlock, mMain, mLine, mAI);
		bloc.setParentCondition(last);
		if (mCompiler.getWord().getWord().equals("if")) {
			// On veut un elseif
			mCompiler.skipWord();
			if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
				throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
			}
			AbstractExpression exp = readExpression();
			if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
				throw new LeekCompilerException(mCompiler.lastWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
			}
			bloc.setCondition(exp);
		}

		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else
			bloc.noAccolade();
		last.getParent().addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void ifBlock() throws LeekCompilerException {
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_PARENTHESIS_EXPECTED);
		}
		AbstractExpression exp = readExpression();
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.lastWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
		}
		ConditionalBloc bloc = new ConditionalBloc(mCurentBlock, mMain, mLine, mAI);
		bloc.setCondition(exp);
		if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_LEFT) {
			mCompiler.skipWord();
		} else if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipWord();
			bloc.addInstruction(this, new BlankInstruction());
			bloc.noAccolade();
		} else
			bloc.noAccolade();
		mCurentBlock.addInstruction(this, bloc);
		mCurentBlock = bloc;
	}

	private void globalDeclaration() throws LeekCompilerException {
		// Il y a au moins une premiere variable
		IAWord word = mCompiler.readWord();
		if (!(mCurentBlock instanceof MainLeekBlock))
			throw new LeekCompilerException(word, Error.GLOBAL_ONLY_IN_MAIN_BLOCK);
		if (word.getType() != WordParser.T_STRING)
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED_AFTER_GLOBAL);
		LeekGlobalDeclarationInstruction variable = new LeekGlobalDeclarationInstruction(word, mLine, mAI);
		// On regarde si une valeur est assignée
		if (mCompiler.getWord().getWord().equals("=")) {
			mCompiler.skipWord();
			// Si oui on récupère la valeur en question
			variable.setValue(readExpression());
		}
		// On ajoute la variable
		mMain.addGlobalDeclaration(variable);
		mCurentBlock.addInstruction(this, variable);
		while (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_VIRG) {
			// On regarde si y'en a d'autres
			mCompiler.skipWord();// On passe la virgule
			word = mCompiler.readWord();
			if (word.getType() != WordParser.T_STRING)
				throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
			variable = new LeekGlobalDeclarationInstruction(word, mLine, mAI);
			// On regarde si une valeur est assign�e
			if (mCompiler.getWord().getWord().equals("=")) {
				mCompiler.skipWord();
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
		if (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
			mCompiler.skipWord();
	}

	private void variableDeclaration() throws LeekCompilerException {
		// Il y a au moins une premiere variable
		IAWord word = mCompiler.readWord();
		if (word.getType() != WordParser.T_STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
		}
		if (getVersion() >= 3 && isKeyword(word)) {
			addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
		}
		LeekVariableDeclarationInstruction variable = new LeekVariableDeclarationInstruction(this, word, mLine, mAI, getCurrentFunction());
		// On regarde si une valeur est assignée
		if (mCompiler.getWord().getWord().equals("=")) {
			mCompiler.skipWord();
			// Si oui on récupère la valeur en question
			variable.setValue(readExpression());
		}
		mCurentBlock.addInstruction(this, variable);
		while (mCompiler.getWord().getType() == WordParser.T_VIRG) {
			// On regarde si y'en a d'autres
			mCompiler.skipWord();// On passe la virgule
			word = mCompiler.readWord();
			if (word.getType() != WordParser.T_STRING)
				throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
			if (getVersion() >= 3 && isKeyword(word)) {
				addError(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			}
			variable = new LeekVariableDeclarationInstruction(this, word, mLine, mAI, getCurrentFunction());
			// On regarde si une valeur est assignée
			if (mCompiler.getWord().getWord().equals("=")) {
				mCompiler.skipWord();
				// Si oui on récupère la valeur en question
				variable.setValue(readExpression());
			}
			// On ajoute la variable
			mCurentBlock.addInstruction(this, variable);
		}
		if (mCompiler.haveWords() && mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION) {
			mCompiler.skipWord();
		}
	}

	public void classDeclaration() throws LeekCompilerException {
		// Read class name
		IAWord word = mCompiler.readWord();
		if (word.getType() != WordParser.T_STRING) {
			throw new LeekCompilerException(word, Error.VAR_NAME_EXPECTED);
		}
		if (mMain.hasUserClass(word.getWord())) {
			throw new LeekCompilerException(word, Error.VARIABLE_NAME_UNAVAILABLE);
		}
		if (getVersion() >= 3 && isKeyword(word)) {
			errors.add(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { word.getWord() }));
		}
		ClassDeclarationInstruction classDeclaration = new ClassDeclarationInstruction(word, mLine, mAI, false);
		mMain.addClass(classDeclaration);
		mCurrentClass = classDeclaration;

		if (mCompiler.getWord().getWord().equals("extends")) {
			mCompiler.skipWord();
			IAWord parent = mCompiler.readWord();
			classDeclaration.setParent(parent);
		}
		if (mCompiler.getWord().getType() != WordParser.T_ACCOLADE_LEFT) {
			throw new LeekCompilerException(mCompiler.getWord(), Error.OPENING_CURLY_BRACKET_EXPECTED);
		}
		mCompiler.skipWord();

		word = mCompiler.readWord();
		while (word.getType() != WordParser.T_ACCOLADE_RIGHT) {
			switch (word.getWord()) {
				case "public":
				case "private":
				case "protected":
				{
					AccessLevel level = AccessLevel.fromString(word.getWord());
					classAccessLevelMember(classDeclaration, level);
					break;
				}
				case "constructor": {
					classConstructor(classDeclaration, AccessLevel.PUBLIC);
					break;
				}
				default: {
					if (word.getType() == WordParser.T_STRING) {
						mCompiler.back();
						classAccessLevelMember(classDeclaration, AccessLevel.PUBLIC);
					} else {
						throw new LeekCompilerException(word, Error.KEYWORD_UNEXPECTED);
					}
				}
			}
			word = mCompiler.readWord();
		}
		if (word.getType() != WordParser.T_ACCOLADE_RIGHT) {
			throw new LeekCompilerException(word, Error.END_OF_CLASS_EXPECTED);
		}
		mCurrentClass = null;
		// mMain.addInstruction(this, classDeclaration);
	}

	public void classAccessLevelMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		IAWord name = mCompiler.readWord();
		switch (name.getWord()) {
			case "constructor":
				classConstructor(classDeclaration, accessLevel);
				return;
			case "static":
				classStaticMember(classDeclaration, accessLevel);
				return;
		}
		if (name.getWord().equals("class") || name.getWord().equals("super")) {
			errors.add(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.RESERVED_FIELD, new String[] { name.getWord() }));
		} else if (getVersion() >= 3 && isKeyword(name)) {
			errors.add(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { name.getWord() }));
		}
		IAWord word2 = mCompiler.getWord();
		if (word2.getType() == WordParser.T_PAR_LEFT) {
			// Méthode
			ClassMethodBlock method = classMethod(classDeclaration, name.getWord(), false);
			classDeclaration.addMethod(this, name, method, accessLevel);
		} else {
			// Field
			AbstractExpression expr = null;
			if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("=")) {
				mCompiler.skipWord();
				expr = readExpression();
			}
			classDeclaration.addField(this, name, expr, accessLevel);
			if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
				mCompiler.skipWord();
		}
	}

	public void classStaticMember(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		IAWord name = mCompiler.readWord();

		// Static field
		AbstractExpression expr = null;
		if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("=")) {
			mCompiler.skipWord();
			expr = readExpression();
		} else if (mCompiler.getWord().getType() == WordParser.T_PAR_LEFT) {
			// Méthode
			ClassMethodBlock method = classMethod(classDeclaration, name.getWord(), true);
			classDeclaration.addStaticMethod(this, name, method, accessLevel);
			if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
				mCompiler.skipWord();
			return;
		}
		if (name.getWord().equals("name") || name.getWord().equals("super") || name.getWord().equals("fields") || name.getWord().equals("staticFields") || name.getWord().equals("methods") || name.getWord().equals("staticMethods")) {
			errors.add(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.RESERVED_FIELD, new String[] { name.getWord() }));
		} else if (getVersion() >= 3 && isKeyword(name)) {
			errors.add(new AnalyzeError(name, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] { name.getWord() }));
		}
		classDeclaration.addStaticField(name, expr, accessLevel);

		if (mCompiler.getWord().getType() == WordParser.T_END_INSTRUCTION)
			mCompiler.skipWord();
	}

	public void classConstructor(ClassDeclarationInstruction classDeclaration, AccessLevel accessLevel) throws LeekCompilerException {
		ClassMethodBlock constructor = classMethod(classDeclaration, "constructor", false);
		classDeclaration.addConstructor(constructor, accessLevel);
	}

	public ClassMethodBlock classMethod(ClassDeclarationInstruction classDeclaration, String name, boolean isStatic) throws LeekCompilerException {

		ClassMethodBlock method = new ClassMethodBlock(classDeclaration, isStatic, mCurentBlock, mMain, mLine, mAI);

		IAWord word = mCompiler.readWord();
		if (word.getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(word, Error.OPENING_PARENTHESIS_EXPECTED);
		}
		int param_count = 0;
		while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
			if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
				errors.add(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				mCompiler.skipWord();
			}
			if (mCompiler.getWord().getType() != WordParser.T_STRING)
				throw new LeekCompilerException(mCompiler.getWord(), Error.PARAMETER_NAME_EXPECTED);
			method.addParameter(mCompiler.getWord());
			mCompiler.skipWord();
			param_count++;
			if (mCompiler.getWord().getType() == WordParser.T_VIRG)
				mCompiler.skipWord();
		}
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}
		if (classDeclaration.hasMethod(name, param_count)) {
			throw new LeekCompilerException(mCompiler.getWord(), Error.CONSTRUCTOR_ALREADY_EXISTS);
		}

		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		int initialLine = mLine;
		AIFile<?> initialAI = mAI;
		mCurentBlock = method;

		// Ouverture des accolades
		if (mCompiler.readWord().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_CURLY_BRACKET_EXPECTED);

		// Lecture du corps de la fonction
		while (mCompiler.haveWords()) {
			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
				DoWhileBlock do_block = (DoWhileBlock) mCurentBlock;
				mCurentBlock = mCurentBlock.endInstruction();
				dowhileendBlock(do_block);
				mCompiler.skipWord();
			} else
				mCurentBlock = mCurentBlock.endInstruction();
			if (!mCompiler.haveWords())
				break;

			// On regarde si on veut fermer la fonction anonyme
			if (mCompiler.getWord().getType() == WordParser.T_ACCOLADE_RIGHT && mCurentBlock == method) {
				mCompiler.skipWord();
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

	public AbstractExpression readExpression() throws LeekCompilerException {
		LeekExpression retour = new LeekExpression();
		while (mCompiler.haveWords()) {
			IAWord word = mCompiler.getWord();
			if (word.getType() == WordParser.T_PAR_RIGHT || word.getType() == WordParser.T_ACCOLADE_RIGHT || word.getType() == WordParser.T_END_INSTRUCTION) {
				break;
			}
			if (retour.needOperator()) {
				// Si on attend un opérateur mais qu'il vient pas

				if (word.getType() == WordParser.T_BRACKET_LEFT) {
					mCompiler.skipWord(); // On avance le curseur pour être au début de l'expression

					AbstractExpression exp = readExpression();
					if (mCompiler.getWord().getType() != WordParser.T_BRACKET_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), Error.CLOSING_SQUARE_BRACKET_EXPECTED);
					}
					retour.addBracket(exp);
				} else if (word.getType() == WordParser.T_PAR_LEFT) {

					LeekExpressionFunction function = new LeekExpressionFunction(word);
					mCompiler.skipWord();// On avance le curseur pour être au début de l'expression

					while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						function.addParameter(readExpression());
						if (mCompiler.getWord().getType() == WordParser.T_VIRG)
							mCompiler.skipWord();
					}
					if (mCompiler.haveWords() && mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					retour.addFunction(function);
				} else if (word.getType() == WordParser.T_DOT) {
					// Object access
					mCompiler.skipWord();
					IAWord name = mCompiler.getWord();
					retour.addObjectAccess(name);

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
						mCompiler.skipWord();
						word = mCompiler.getWord();
						if (word.getWord().equals("not")) {
							IAWord token = mCompiler.readWord();
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
					var type = s.contains(".") ? Type.REAL : Type.INT;
					try {
						Integer.parseInt(s);
					} catch (NumberFormatException e) {
						type = Type.REAL;
					}
					retour.addExpression(new LeekNumber(Double.parseDouble(word.getWord()), type));
				} else if (word.getType() == WordParser.T_VAR_STRING) {
					retour.addExpression(new LeekString(word.getWord()));
				} else if (word.getType() == WordParser.T_BRACKET_LEFT) {
					// Déclaration d'un tableau
					mCompiler.skipWord();
					LeekArray array = new LeekArray();
					int type = 0;// 0 => A déterminer, 1=> Simple, 2 =>
									// Clé:valeur
					while (mCompiler.getWord().getType() != WordParser.T_BRACKET_RIGHT) {
						AbstractExpression exp = readExpression();
						if (mCompiler.getWord().getWord().equals(":")) {
							if (type == 0)
								type = 2;
							else if (type == 1)
								throw new LeekCompilerException(mCompiler.getWord(), Error.SIMPLE_ARRAY);
							mCompiler.skipWord();
							AbstractExpression value = readExpression();
							array.addValue(exp, value);
						} else {
							if (type == 0)
								type = 1;
							else if (type == 2)
								throw new LeekCompilerException(mCompiler.getWord(), Error.ASSOCIATIVE_ARRAY);
							array.addValue(exp);
						}
						if (mCompiler.getWord().getType() == WordParser.T_VIRG)
							mCompiler.skipWord();
					}
					if (mCompiler.getWord().getType() != WordParser.T_BRACKET_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					retour.addExpression(array);

				} else if (getVersion() >= 2 && word.getType() == WordParser.T_ACCOLADE_LEFT) {

					// Déclaration d'un objet
					mCompiler.skipWord();
					var object = new LeekObject();

					while (mCompiler.getWord().getType() != WordParser.T_ACCOLADE_RIGHT) {
						if (mCompiler.getWord().getType() != WordParser.T_STRING) {
							throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
						}
						String key = mCompiler.getWord().getWord();
						mCompiler.skipWord();

						if (!mCompiler.getWord().getWord().equals(":")) {
							throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
						}
						mCompiler.skipWord();

						AbstractExpression value = readExpression();
						object.addEntry(key, value);

						if (mCompiler.getWord().getType() == WordParser.T_VIRG) {
							mCompiler.skipWord();
						}
					}
					if (mCompiler.getWord().getType() != WordParser.T_ACCOLADE_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
					}
					retour.addExpression(object);

				} else if (word.getType() == WordParser.T_STRING) {

					if (mMain.hasGlobal(word.getWord())) {
						retour.addExpression(new LeekVariable(this, word, VariableType.GLOBAL));
					} else if (wordEquals(word, "function")) {
						retour.addExpression(readAnonymousFunction());
					} else if (wordEquals(word, "true"))
						retour.addExpression(new LeekBoolean(true));
					else if (wordEquals(word, "false"))
						retour.addExpression(new LeekBoolean(false));
					else if (wordEquals(word, "null"))
						retour.addExpression(new LeekNull());
					else if (wordEquals(word, "not"))
						retour.addUnaryPrefix(Operators.NOT, word);
					else if (getVersion() >= 2 && word.getWord().equals("new")) {
						retour.addUnaryPrefix(Operators.NEW, word);
					} else if (getVersion() >= 2 && word.getWord().equals("super")) {
						// super doit être dans une méthode
						if (!(mCurentBlock instanceof ClassMethodBlock)) {
							errors.add(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.KEYWORD_MUST_BE_IN_CLASS));
							retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
						} else {
							if (((ClassMethodBlock) mCurentBlock).getClassDeclaration().getParentToken() == null) {
								errors.add(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.SUPER_NOT_AVAILABLE_PARENT));
							}
							retour.addExpression(new LeekVariable(word, VariableType.SUPER, ((ClassMethodBlock) mCurentBlock).getClassDeclaration()));
						}
					} else {
						retour.addExpression(new LeekVariable(this, word, VariableType.LOCAL));
						// throw new LeekCompilerException(word, Error.UNKNOWN_VARIABLE_OR_FUNCTION);
					}
				} else if (word.getType() == WordParser.T_PAR_LEFT) {
					mCompiler.skipWord(); // On avance le curseur pour bien être au début de l'expression

					AbstractExpression exp = readExpression();
					if (mCompiler.haveWords() && mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
						throw new LeekCompilerException(mCompiler.getWord(), Error.CLOSING_PARENTHESIS_EXPECTED);
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
						errors.add(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.OPERATOR_UNEXPECTED));
						// throw new LeekCompilerException(word, Error.OPERATOR_UNEXPECTED);
					}
				} else {
					errors.add(new AnalyzeError(word, AnalyzeErrorLevel.ERROR, Error.VALUE_EXPECTED));
					// throw new LeekCompilerException(word, Error.VALUE_EXPECTED);
				}
			}
			mCompiler.skipWord();
		}
		// Avant de retourner, on valide l'expression
		AbstractExpression result = retour;
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
			throw new LeekCompilerException(mCompiler.lastWord(), Error.UNCOMPLETE_EXPRESSION);
		}
		try {
			result.validExpression(this, mMain);
		} catch (LeekExpressionException e) {
			throw new LeekCompilerException(mCompiler.lastWord(), e.getError(), new String[] { e.getExpression() });
		}
		return result;
	}

	private boolean wordEquals(IAWord word, String expected) {
		if (getVersion() <= 2) {
			return word.getWord().equalsIgnoreCase(expected);
		}
		return word.getWord().equals(expected);
	}

	private LeekAnonymousFunction readAnonymousFunction() throws LeekCompilerException {
		mCompiler.skipWord();
		if (mCompiler.readWord().getType() != WordParser.T_PAR_LEFT) {
			throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_FUNCTION);
		}
		// On enregistre les block actuels
		AbstractLeekBlock initialBlock = mCurentBlock;
		var previousFunction = mCurrentFunction;
		int initialLine = mLine;
		AIFile<?> initialAI = mAI;
		AnonymousFunctionBlock block = new AnonymousFunctionBlock(mCurentBlock, mMain, mLine, mAI);
		// if (initialBlock.getDeclaringVariable() != null)
		// 	block.addVariable(new LeekVariable(initialBlock.getDeclaringVariable(), VariableType.LOCAL));
		mCurentBlock = block;
		mCurrentFunction = block;

		// Lecture des paramètres
		while (mCompiler.getWord().getType() != WordParser.T_PAR_RIGHT) {
			boolean is_reference = false;
			if (mCompiler.getWord().getType() == WordParser.T_OPERATOR && mCompiler.getWord().getWord().equals("@")) {
				is_reference = true;
				if (getVersion() >= 2) {
					addError(new AnalyzeError(mCompiler.getWord(), AnalyzeErrorLevel.WARNING, Error.REFERENCE_DEPRECATED));
				}
				mCompiler.skipWord();
			}
			if (mCompiler.getWord().getType() != WordParser.T_STRING) {
				throw new LeekCompilerException(mCompiler.getWord(), Error.PARAMETER_NAME_EXPECTED);
			}
			var parameter = mCompiler.readWord();
			if (block.hasParameter(parameter.getWord())) {
				throw new LeekCompilerException(parameter, Error.PARAMETER_NAME_UNAVAILABLE);
			}
			block.addParameter(this, parameter, is_reference);
			if (mCompiler.getWord().getType() == WordParser.T_VIRG)
				mCompiler.skipWord();
		}
		if (mCompiler.readWord().getType() != WordParser.T_PAR_RIGHT) {
			throw new LeekCompilerException(mCompiler.getWord(), Error.PARENTHESIS_EXPECTED_AFTER_PARAMETERS);
		}

		// Ouverture des accolades
		if (mCompiler.readWord().getType() != WordParser.T_ACCOLADE_LEFT)
			throw new LeekCompilerException(mCompiler.lastWord(), Error.OPENING_CURLY_BRACKET_EXPECTED);

		// Lecture du corp de la fonction
		while (mCompiler.haveWords()) {

			// Fermeture des blocs ouverts
			if (mCurentBlock instanceof DoWhileBlock && !((DoWhileBlock) mCurentBlock).hasAccolade() && mCurentBlock.isFull()) {
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
		mCurrentFunction = previousFunction;

		return new LeekAnonymousFunction(block);
	}

	public boolean isKeyword(IAWord word) {
		for (var w : WordParser.reservedWords) {
			if (wordEquals(word, w)) return true;
		}
		return false;
	}

	public boolean isAvailable(IAWord word, boolean allFunctions) {
		if (getVersion() >= 3 && isKeyword(word)) return false;
		// if(LeekFunctions.isFunction(word) >= 0 || mMain.hasGlobal(word) ||
		// mMain.hasUserFunction(word, allFunctions) ||
		// mCurentBlock.hasVariable(word)) return false;
		if (mMain.hasGlobal(word.getWord()) || mMain.hasUserFunction(word.getWord(), allFunctions) || mCurentBlock.hasVariable(word.getWord()))
			return false;
		return true;
	}

	public boolean isGlobalAvailable(IAWord word) {
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

	public Set<AnalyzeError> getErrors() {
		return this.errors;
	}

	public AbstractLeekBlock getCurrentBlock() {
		return mCurentBlock;
	}

	public AbstractLeekBlock getCurrentFunction() {
		return mCurrentFunction;
	}

	public void addError(AnalyzeError error) {
		this.errors.add(error);
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

	public void addErrors(Set<AnalyzeError> errors) {
		this.errors.addAll(errors);
	}

	public ClassDeclarationInstruction getCurrentClass() {
		return mCurrentClass;
	}

	public void setCurrentClass(ClassDeclarationInstruction clazz) {
		this.mCurrentClass = clazz;
	}

	public void setCurrentFunction(AbstractLeekBlock block) {
		this.mCurrentFunction = block;
	}
}
