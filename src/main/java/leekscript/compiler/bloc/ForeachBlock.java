package leekscript.compiler.bloc;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ForeachBlock extends AbstractLeekBlock {

	private final Token token;
	private Token mIterator;
	private Expression mArray;
	private final boolean mIsDeclaration;
	private boolean mReference = false;
	private LeekVariableDeclarationInstruction declaration;
	private LeekVariable iteratorVariable;

	public ForeachBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isDeclaration, Token token, boolean reference) {
		super(parent, main);
		mIsDeclaration = isDeclaration;
		mReference = reference;
		this.token = token;
	}

	public void setIterator(WordCompiler compiler, Token iterator, Type type) {
		mIterator = iterator;
		if (mIsDeclaration) {
			declaration = new LeekVariableDeclarationInstruction(compiler, iterator, compiler.getCurrentFunction(), type);
		}
	}

	public void setArray(Expression exp) {
		mArray = exp;
	}

	@Override
	public String getCode() {
		return "for (" + (mIsDeclaration ? "var " : "") + mIterator + " in " + mArray.toString() + ") {\n" + super.getCode() + "}";
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);

		// On analyse d'abord le container puis la variable
		mArray.preAnalyze(compiler);

		// Si c'est une déclaration on vérifie que le nom est disponible
		if (mIsDeclaration) {
			if ((compiler.getVersion() >= 2 && (compiler.getMainBlock().hasGlobal(mIterator.getWord()) || compiler.getMainBlock().hasUserFunction(mIterator.getWord(), true))) || compiler.getCurrentBlock().hasVariable(mIterator.getWord())) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			} else {
				// this.addVariable(new LeekVariable(mIterator, VariableType.LOCAL, declaration));
			}
			declaration.setFunction(compiler.getCurrentFunction());
			declaration.preAnalyze(compiler);
		} else {
			var v = compiler.getCurrentBlock().getVariable(mIterator.getWord(), true);
			if (v == null) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION, new String[] {
					mIterator.getWord()
				}));
			}
		}
		compiler.setCurrentBlock(initialBlock);

		super.preAnalyze(compiler);
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		mArray.analyze(compiler);
		this.iteratorVariable = compiler.getCurrentBlock().getVariable(mIterator.getWord(), false);

		if (compiler.getMainBlock().isStrict() && !mArray.getType().isIterable()) {
			compiler.addError(new AnalyzeError(mArray.getLocation(), AnalyzeErrorLevel.WARNING, Error.MAY_NOT_BE_ITERABLE, new String[] {
				mArray.toString(),
				mArray.getType().toString()
			} ));
		} else if (!mArray.getType().canBeIterable()) {
			var level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
			compiler.addError(new AnalyzeError(mArray.getLocation(), level, Error.NOT_ITERABLE, new String[] { mArray.getType().toString() } ));
		}

		if (this.iteratorVariable != null) {

			var cast = mArray.getType().element().accepts(iteratorVariable.getType());
			if (cast == CastType.INCOMPATIBLE) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.WARNING, Error.INCOMPATIBLE_TYPE, new String[] {
					mArray.getType().element().toString(),
					iteratorVariable.getType().toString()
				}));
			}
			// LS5+ : Le type est forcé par le conteneur
			if (compiler.getMainBlock().isStrict() && iteratorVariable.getType() == Type.ANY) {
				iteratorVariable.setType(mArray.getType().element());
			}
		}

		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}


	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {

		AbstractLeekBlock initialBlock = mainblock.getWordCompiler().getCurrentBlock();
		mainblock.getWordCompiler().setCurrentBlock(this);

		// On prend un nombre unique pour les noms de variables temporaires
		int block_count = getCount();
		String var = "v" + block_count;
		String it = "i" + block_count;
		String ar = "ar" + block_count;
		String iterator_name = mainblock.hasGlobal(mIterator.getWord()) ? ("g_" + mIterator) : "u_" + mIterator;

		// Container
		writer.addCode("final var " + ar + " = ops(");
		if (mainblock.getCompiler().getCurrentAI().getVersion() >= 2) {
			mArray.writeJavaCode(mainblock, writer, false);
		} else {
			writer.compileLoad(mainblock, mArray, false);
		}
		writer.addCode(", " + mArray.getOperations() + ");");

		writer.addCode("if (isIterable(" + ar + ")) { ");
		if (mIsDeclaration) {
			if (declaration.isCaptured()) {
				writer.addCode("final Wrapper<" + iteratorVariable.getType().getJavaName(mainblock.getVersion()) + "> " + iterator_name + " = new Wrapper<" + iteratorVariable.getType().getJavaName(mainblock.getVersion()) + ">(new Box(" + writer.getAIThis() + ", null));");
			} else if (mainblock.getVersion() >= 2) {
				writer.addCode(declaration.getVariable().getType().getJavaName(mainblock.getVersion()) + " " + iterator_name + " = null;");
				writer.addCounter(1);
			} else {
				writer.addCode("var " + iterator_name + " = new Box(" + writer.getAIThis() + ", null);");
			}
		} else {
			writer.addCounter(1);
		}

		// On fait le parcours
		writer.addCode("var " + it + " = iterator(" + ar + "); while (" + it + ".hasNext()) { var " + var + " = " + it + ".next(); ");

		if (mainblock.getVersion() >= 4) {
			if (iteratorVariable != null && iteratorVariable.getDeclaration() != null && iteratorVariable.getDeclaration().isCaptured()) {
				writer.addCode(iterator_name + ".set(" + var + ".getValue());");
			} else if (mIsDeclaration) {
				writer.addCode(iterator_name + " = (" + iteratorVariable.getType().getJavaName(mainblock.getVersion()) + ") " + var + ".getValue();");
			} else {
				writer.addCode(iterator_name + " = (" + iteratorVariable.getType().getJavaName(mainblock.getVersion()) + ") " + var + ".getValue();");
			}
		} else if (mainblock.getVersion() >= 2) {
			if (iteratorVariable != null && iteratorVariable.getDeclaration() != null && iteratorVariable.getDeclaration().isCaptured()) {
				writer.addCode(iterator_name + ".set(" + var + ".getValue());");
			} else {
				writer.addCode(iterator_name + " = (" + iteratorVariable.getType().getJavaName(mainblock.getVersion()) + ") " + var + ".getValue();");
			}
		} else {
			if (mReference) {
				writer.addCode(iterator_name + ".set(" + var + ".getValue());");
			} else if (iteratorVariable != null && iteratorVariable.getDeclaration() != null && iteratorVariable.getDeclaration().isCaptured()) {
				writer.addCode(iterator_name + ".set(" + var + ".getValue());");
				writer.addCounter(1);
			} else {
				writer.addCode(iterator_name + ".set(" + var + ".getValue());");
				writer.addCounter(1);
			}
		}
		writer.addCounter(1);
		writer.addLine("", mIterator.getLocation());

		mainblock.getWordCompiler().setCurrentBlock(initialBlock);

		super.writeJavaCode(mainblock, writer, false);

		writer.addLine("}}");
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		return 0;
	}

	@Override
	public Type getType() {
		return Type.VOID;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
