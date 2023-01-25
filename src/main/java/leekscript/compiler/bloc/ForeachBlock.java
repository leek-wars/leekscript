package leekscript.compiler.bloc;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.common.Type;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ForeachBlock extends AbstractLeekBlock {

	private final Token token;
	private Token mIterator;
	private Expression mArray;
	private final boolean mIsDeclaration;
	private boolean mReference = false;
	private LeekVariableDeclarationInstruction declaration;

	public ForeachBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isDeclaration, Token token, boolean reference) {
		super(parent, main);
		mIsDeclaration = isDeclaration;
		mReference = reference;
		this.token = token;
	}

	public void setIterator(WordCompiler compiler, Token iterator) {
		mIterator = iterator;
		if (mIsDeclaration) {
			declaration = new LeekVariableDeclarationInstruction(compiler, iterator, compiler.getCurrentFunction());
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
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		// On prend un nombre unique pour les noms de variables temporaires
		int block_count = getCount();
		String var = "v" + block_count;
		String it = "i" + block_count;
		String ar = "ar" + block_count;
		String iterator_name = mainblock.hasGlobal(mIterator.getWord()) ? ("g_" + mIterator) : "u_" + mIterator;

		// Container
		writer.addCode("final var " + ar + " = ops(");
		if (mainblock.getCompiler().getCurrentAI().getVersion() >= 2) {
			mArray.writeJavaCode(mainblock, writer);
		} else {
			writer.compileLoad(mainblock, mArray);
		}
		writer.addCode(", " + mArray.getOperations() + ");");

		writer.addLine("if (isIterable(" + ar + ")) {", mIterator.getLocation());
		if (mIsDeclaration) {
			if (mIsDeclaration && declaration.isCaptured()) {
				writer.addCode("final Wrapper " + iterator_name + " = new Wrapper(new Box(" + writer.getAIThis() + ", null));");
			} else if (mainblock.getCompiler().getCurrentAI().getVersion() >= 2) {
				writer.addLine("Object " + iterator_name + " = null;");
				writer.addCounter(1);
			} else {
				writer.addLine("var " + iterator_name + " = new Box(" + writer.getAIThis() + ", null);");
			}
		} else {
			writer.addCounter(1);
		}

		// On fait le parcours
		writer.addLine("var " + it + " = iterator(" + ar + "); while (" + it + ".hasNext()) { var " + var + " = " + it + ".next(); ");

		if (mainblock.getVersion() >= 4) {
			if (mIsDeclaration && declaration.isCaptured()) {
				writer.addLine(iterator_name + ".set(" + var + ".getValue());");
			} else {
				writer.addLine(iterator_name + " = " + var + ".getValue();");
			}
		} else if (mainblock.getVersion() >= 2) {
			if (mIsDeclaration && declaration.isCaptured()) {
				writer.addLine(iterator_name + ".set(" + var + ".getValue());");
			} else {
				writer.addLine(iterator_name + " = " + var + ".getValue();");
			}
		} else {
			if (mReference) {
				writer.addCode(iterator_name + ".set(" + var + ".getValue());");
			} else if (mIsDeclaration && declaration.isCaptured()) {
				writer.addLine(iterator_name + ".set(" + var + ".getValue());");
				writer.addCounter(1);
			} else {
				writer.addLine(iterator_name + ".set(" + var + ".getValue());");
				writer.addCounter(1);
			}
		}
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);

		writer.addLine("}}");
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	public void preAnalyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);

		// On analyse d'abord le container puis la variable
		mArray.preAnalyze(compiler);

		// Si c'est une déclaration on vérifie que le nom est disponnible
		if (mIsDeclaration) {
			if ((compiler.getVersion() >= 2 && (compiler.getMainBlock().hasGlobal(mIterator.getWord()) || compiler.getMainBlock().hasUserFunction(mIterator.getWord(), true))) || compiler.getCurrentBlock().hasVariable(mIterator.getWord())) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			} else {
				this.addVariable(new LeekVariable(mIterator, VariableType.LOCAL, declaration));
			}
			declaration.setFunction(compiler.getCurrentFunction());
		} else {
			var v = compiler.getCurrentBlock().getVariable(mIterator.getWord(), true);
			if (v == null) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
			}
		}
		compiler.setCurrentBlock(initialBlock);

		super.preAnalyze(compiler);
	}

	public void analyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		mArray.analyze(compiler);

		if (!mArray.getType().canBeIterable()) {
			compiler.addError(new AnalyzeError(mArray.getLocation(), AnalyzeErrorLevel.WARNING, Error.NOT_ITERABLE, new String[] { mArray.getType().name } ));
		}

		compiler.setCurrentBlock(initialBlock);
		super.analyze(compiler);
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
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
