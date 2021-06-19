package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekAnonymousFunction extends AbstractExpression {

	private final AnonymousFunctionBlock mBlock;

	public LeekAnonymousFunction(AnonymousFunctionBlock block) {
		mBlock = block;
	}

	@Override
	public int getNature() {
		return FUNCTION;
	}

	@Override
	public Type getType() {
		return Type.FUNCTION;
	}

	@Override
	public String getString() {
		return mBlock.getCode();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new FunctionLeekValue(" + mBlock.getId() + ", ");
		mBlock.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void analyze(WordCompiler compiler) {
		var previousFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(mBlock);
		mBlock.analyze(compiler);
		compiler.setCurrentFunction(previousFunction);
	}
}
