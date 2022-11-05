package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekAnonymousFunction extends Expression {

	private final AnonymousFunctionBlock mBlock;
	private final Token token;

	public LeekAnonymousFunction(AnonymousFunctionBlock block, Token token) {
		mBlock = block;
		this.token = token;
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
	public String toString() {
		return mBlock.getCode();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new FunctionLeekValue(" + mBlock.countParameters() + ") {");
		mBlock.writeJavaCode(mainblock, writer);
		writer.addCode("}");
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		var previousFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(mBlock);
		mBlock.preAnalyze(compiler);
		compiler.setCurrentFunction(previousFunction);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		var previousFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(mBlock);
		mBlock.analyze(compiler);
		compiler.setCurrentFunction(previousFunction);
	}

	@Override
	public Location getLocation() {
		return new Location(token.getLocation(), mBlock.getLocation());
	}
}
