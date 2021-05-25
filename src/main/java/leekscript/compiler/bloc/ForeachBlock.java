package leekscript.compiler.bloc;

import leekscript.compiler.AIFile;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.expression.AbstractExpression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ForeachBlock extends AbstractLeekBlock {

	private IAWord mIterator;
	private AbstractExpression mArray;
	private final boolean mIsDeclaration;
	private boolean mReference = false;
	private LeekVariableDeclarationInstruction declaration;

	public ForeachBlock(AbstractLeekBlock parent, MainLeekBlock main, boolean isDeclaration, int line, AIFile<?> ai, boolean reference) {
		super(parent, main, line, ai);
		mIsDeclaration = isDeclaration;
		mReference = reference;
	}

	public void setIterator(WordCompiler compiler, IAWord iterator) {
		mIterator = iterator;
		if (mIsDeclaration) {
			declaration = new LeekVariableDeclarationInstruction(compiler, iterator, iterator.getLine(), iterator.getAI(), compiler.getCurrentFunction());
		}
	}

	public void setArray(AbstractExpression exp) {
		mArray = exp;
	}

	@Override
	public String getCode() {
		return "for(" + mIterator + " in " + mArray.getString() + "){\n" + super.getCode() + "}";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		//On prend un nombre unique pour les noms de variables temporaires
		int block_count = getCount();
		String var = "i" + block_count;
		String ar = "ar" + block_count;
		String iterator_name = mainblock.hasGlobal(mIterator.getWord()) ? ("globale_" + mIterator) : "user_" + mIterator;

		writer.addCode("final AbstractLeekValue " + ar + " = ");
		mArray.writeJavaCode(mainblock, writer);
		writer.addLine(".getValue();", mLine, mAI);
		writer.addLine("if(" + ar + ".isArrayForIteration(mUAI)){");
		if(mIsDeclaration) writer.addLine("final VariableLeekValue " + iterator_name + " = new VariableLeekValue(mUAI, LeekValueManager.NULL);");
		else writer.addLine(iterator_name + ".set(mUAI, LeekValueManager.NULL);");
		if (mReference || mainblock.getCompiler().getCurrentAI().getVersion() >= 11) {
			writer.addLine("for(AbstractLeekValue " + var + " : " + ar + ".getArray()){ " + iterator_name + ".setRef(mUAI, " + var + ");");
		} else {
			writer.addLine("for(AbstractLeekValue " + var + " : " + ar + ".getArray()){ " + iterator_name + ".set(mUAI, " + var + ".getValue());");
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

	public void analyze(WordCompiler compiler) {
		AbstractLeekBlock initialBlock = compiler.getCurrentBlock();
		compiler.setCurrentBlock(this);
		// Si c'est une déclaration on vérifie que le nom est disponnible
		if (mIsDeclaration) {
			if ((compiler.getVersion() >= 11 && (compiler.getMainBlock().hasGlobal(mIterator.getWord()) || compiler.getMainBlock().hasUserFunction(mIterator.getWord(), true))) || compiler.getCurrentBlock().hasVariable(mIterator.getWord())) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE));
			} else {
				this.addVariable(new LeekVariable(mIterator, VariableType.LOCAL));
			}
		} else {
			var v = compiler.getCurrentBlock().getVariable(mIterator.getWord(), true);
			if (v == null) {
				compiler.addError(new AnalyzeError(mIterator, AnalyzeErrorLevel.ERROR, Error.UNKNOWN_VARIABLE_OR_FUNCTION));
			}
		}
		mArray.analyze(compiler);
		compiler.setCurrentBlock(initialBlock);

		super.analyze(compiler);
	}
}
